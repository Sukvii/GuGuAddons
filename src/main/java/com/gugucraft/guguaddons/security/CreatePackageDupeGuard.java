package com.gugucraft.guguaddons.security;

import com.gugucraft.guguaddons.GuGuAddons;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.compat.computercraft.events.RepackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageItem.PackageOrderData;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CreatePackageDupeGuard {
    private static final int MAX_ORDER_LINKS = 1000;
    private static final int MAX_ORDER_FRAGMENTS = 1000;

    private CreatePackageDupeGuard() {
    }

    public static void attemptToRepackage(RepackagerBlockEntity repackager, IItemHandler inventory) {
        Level level = repackager.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        repackager.repackageHelper.clear();

        List<CandidatePackage> candidates = scanPackages(inventory);
        if (candidates.isEmpty()) {
            return;
        }

        CandidatePackage firstSinglePackage = findFirstSinglePackage(candidates);
        CompleteOrder completeOrder = findFirstCompleteOrder(candidates);
        CompleteOrder crossNetworkCraftingOrder = findFirstCompleteCrossNetworkCraftingOrder(candidates);

        if (crossNetworkCraftingOrder != null
                && (completeOrder == null
                || crossNetworkCraftingOrder.completionScanIndex() < completeOrder.completionScanIndex())) {
            completeOrder = crossNetworkCraftingOrder;
        }

        if (completeOrder != null
                && (firstSinglePackage == null
                || completeOrder.completionScanIndex() < firstSinglePackage.scanIndex())) {
            processFragmentedOrder(repackager, inventory, completeOrder);
            return;
        }

        if (firstSinglePackage != null) {
            processSinglePackage(repackager, inventory, firstSinglePackage);
        }
    }

    private static List<CandidatePackage> scanPackages(IItemHandler inventory) {
        List<CandidatePackage> candidates = new ArrayList<>();
        int scanIndex = 0;

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack simulated = inventory.extractItem(slot, 1, true);
            if (simulated.isEmpty() || !PackageItem.isPackage(simulated)) {
                continue;
            }

            ItemStack copy = simulated.copy();
            candidates.add(new CandidatePackage(slot, scanIndex++, copy, PackageItem.getOrderId(copy),
                    copy.get(AllDataComponents.PACKAGE_ORDER_DATA), PackageItem.getOrderContext(copy),
                    PackageItem.getAddress(copy)));
        }

        return candidates;
    }

    private static CandidatePackage findFirstSinglePackage(List<CandidatePackage> candidates) {
        return candidates.stream()
                .filter(candidate -> !candidate.isFragmented())
                .findFirst()
                .orElse(null);
    }

    private static CompleteOrder findFirstCompleteOrder(List<CandidatePackage> candidates) {
        Map<Integer, List<CandidatePackage>> packagesByOrder = new LinkedHashMap<>();

        for (CandidatePackage candidate : candidates) {
            if (!candidate.isFragmented()) {
                continue;
            }
            packagesByOrder.computeIfAbsent(candidate.orderId(), $ -> new ArrayList<>()).add(candidate);
        }

        return packagesByOrder.values().stream()
                .map(CreatePackageDupeGuard::validateCompleteOrder)
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(CompleteOrder::completionScanIndex)
                        .thenComparingInt(CompleteOrder::firstScanIndex))
                .orElse(null);
    }

    private static CompleteOrder validateCompleteOrder(List<CandidatePackage> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        int orderId = candidates.getFirst().orderId();
        if (orderId == -1) {
            return null;
        }

        Map<OrderPosition, CandidatePackage> packagesByPosition = new HashMap<>();
        Map<Integer, Boolean> finalLinkByLink = new HashMap<>();
        PackageOrderWithCrafts orderContext = candidates.getFirst().orderContext();
        String address = null;

        for (CandidatePackage candidate : candidates) {
            PackageOrderData orderData = candidate.orderData();
            if (orderData == null || candidate.orderId() != orderId || orderData.orderId() != orderId) {
                return null;
            }
            if (orderData.linkIndex() < 0 || orderData.fragmentIndex() < 0) {
                return null;
            }
            if (address == null) {
                address = candidate.address();
            } else if (!Objects.equals(address, candidate.address())) {
                return null;
            }

            OrderPosition position = new OrderPosition(orderData.linkIndex(), orderData.fragmentIndex());
            if (packagesByPosition.put(position, candidate) != null) {
                return null;
            }

            Boolean previousFinalLink = finalLinkByLink.putIfAbsent(orderData.linkIndex(), orderData.isFinalLink());
            if (previousFinalLink != null && previousFinalLink != orderData.isFinalLink()) {
                return null;
            }

            if (!Objects.equals(orderContext, candidate.orderContext())) {
                return null;
            }
        }

        List<CandidatePackage> orderedPackages = new ArrayList<>();
        for (int linkIndex = 0; linkIndex < MAX_ORDER_LINKS; linkIndex++) {
            for (int fragmentIndex = 0; fragmentIndex < MAX_ORDER_FRAGMENTS; fragmentIndex++) {
                CandidatePackage candidate = packagesByPosition.get(new OrderPosition(linkIndex, fragmentIndex));
                if (candidate == null) {
                    return null;
                }

                orderedPackages.add(candidate);
                PackageOrderData orderData = candidate.orderData();
                if (!orderData.isFinal()) {
                    continue;
                }

                if (!orderData.isFinalLink()) {
                    break;
                }

                if (orderedPackages.size() != candidates.size()) {
                    return null;
                }

                int firstScanIndex = orderedPackages.stream()
                        .mapToInt(CandidatePackage::scanIndex)
                        .min()
                        .orElse(Integer.MAX_VALUE);
                int completionScanIndex = orderedPackages.stream()
                        .mapToInt(CandidatePackage::scanIndex)
                        .max()
                        .orElse(Integer.MAX_VALUE);
                if (!satisfiesCraftingContext(orderedPackages, orderContext)) {
                    return null;
                }
                return new CompleteOrder(orderId, List.copyOf(orderedPackages), firstScanIndex,
                        completionScanIndex, null);
            }
        }

        return null;
    }

    private static CompleteOrder findFirstCompleteCrossNetworkCraftingOrder(List<CandidatePackage> candidates) {
        Map<CrossCraftingKey, List<CandidatePackage>> packagesByCraftingContext = new LinkedHashMap<>();

        for (CandidatePackage candidate : candidates) {
            PackageOrderWithCrafts context = candidate.orderContext();
            if (!candidate.isFragmented() || candidate.orderId() == -1 || !hasCraftingContext(context)) {
                continue;
            }
            packagesByCraftingContext.computeIfAbsent(new CrossCraftingKey(candidate.address(),
                    context.orderedCrafts()), $ -> new ArrayList<>()).add(candidate);
        }

        return packagesByCraftingContext.values().stream()
                .map(CreatePackageDupeGuard::validateCompleteCrossNetworkCraftingOrder)
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(CompleteOrder::completionScanIndex)
                        .thenComparingInt(CompleteOrder::firstScanIndex))
                .orElse(null);
    }

    private static CompleteOrder validateCompleteCrossNetworkCraftingOrder(List<CandidatePackage> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<CandidatePackage> selectedPackages = new ArrayList<>();
        PackageOrderWithCrafts mergedContext = mergeCraftingContext(candidates.getFirst().orderContext());
        InventorySummary requiredContents = requiredCraftingContents(mergedContext);

        for (CandidatePackage candidate : candidates.stream()
                .sorted(Comparator.comparingInt(CandidatePackage::scanIndex))
                .toList()) {
            if (candidate.orderData() == null || candidate.orderId() == -1) {
                return null;
            }

            selectedPackages.add(candidate);
            InventorySummary selectedContents = packageContents(selectedPackages);
            if (!containsAtLeast(selectedContents, requiredContents)) {
                continue;
            }

            int firstScanIndex = selectedPackages.stream()
                    .mapToInt(CandidatePackage::scanIndex)
                    .min()
                    .orElse(Integer.MAX_VALUE);
            int completionScanIndex = selectedPackages.stream()
                    .mapToInt(CandidatePackage::scanIndex)
                    .max()
                    .orElse(Integer.MAX_VALUE);
            return new CompleteOrder(selectedPackages.getFirst().orderId(), List.copyOf(selectedPackages),
                    firstScanIndex, completionScanIndex, mergedContext);
        }

        return null;
    }

    private static void processSinglePackage(RepackagerBlockEntity repackager, IItemHandler inventory,
            CandidatePackage candidate) {
        List<ExtractedPackage> extracted = new ArrayList<>();
        ItemStack actual = extractExpectedPackage(inventory, candidate, extracted);

        if (!isExpectedExtraction(actual, candidate.stack())) {
            rollbackOrDrop(repackager, inventory, extracted, "single package changed before extraction");
            return;
        }

        repackager.heldBox = actual.copy();
        repackager.animationInward = false;
        repackager.animationTicks = 20;
        repackager.notifyUpdate();
    }

    private static void processFragmentedOrder(RepackagerBlockEntity repackager, IItemHandler inventory,
            CompleteOrder order) {
        List<ExtractedPackage> extracted = new ArrayList<>();
        List<BigItemStack> outputs;

        try {
            for (CandidatePackage candidate : order.packages()) {
                ItemStack actual = extractExpectedPackage(inventory, candidate, extracted);
                if (!isExpectedExtraction(actual, candidate.stack())) {
                    rollbackOrDrop(repackager, inventory, extracted, "fragment package changed before extraction");
                    return;
                }
            }

            repackager.repackageHelper.clear();
            for (int packageIndex = 0; packageIndex < extracted.size(); packageIndex++) {
                ItemStack fragment = extracted.get(packageIndex).stack().copy();
                if (order.normalizedContext() != null) {
                    PackageItem.setOrder(fragment, order.orderId(), 0, true, packageIndex,
                            packageIndex == extracted.size() - 1, order.normalizedContext());
                }
                repackager.repackageHelper.addPackageFragment(fragment);
            }

            outputs = repackager.repackageHelper.repack(order.orderId(), repackager.getLevel().getRandom());
        } catch (RuntimeException exception) {
            rollbackOrDrop(repackager, inventory, extracted, "exception while repackaging fragments");
            GuGuAddons.LOGGER.error("Create repackager dupe guard aborted repackaging at {}",
                    repackager.getBlockPos(), exception);
            return;
        }

        if (outputs.isEmpty()) {
            rollbackOrDrop(repackager, inventory, extracted, "repackaging produced no output");
            GuGuAddons.LOGGER.warn("Create repackager dupe guard rolled back order {} at {} because repackaging "
                    + "produced no output", order.orderId(), repackager.getBlockPos());
            return;
        }

        if (!outputContentsMatchExtractedPackages(extracted, outputs)) {
            rollbackOrDrop(repackager, inventory, extracted, "repackaging output did not match extracted contents");
            GuGuAddons.LOGGER.warn("Create repackager dupe guard blocked mismatched output for order {} at {}",
                    order.orderId(), repackager.getBlockPos());
            return;
        }

        if (repackager.computerBehaviour != null && repackager.computerBehaviour.hasAttachedComputer()) {
            for (BigItemStack output : outputs) {
                repackager.computerBehaviour.prepareComputerEvent(new RepackageEvent(output.stack, output.count));
            }
        }

        repackager.queuedExitingPackages.addAll(outputs);
        repackager.notifyUpdate();
    }

    private static ItemStack extractExpectedPackage(IItemHandler inventory, CandidatePackage candidate,
            List<ExtractedPackage> extracted) {
        if (candidate.slot() >= inventory.getSlots()) {
            return ItemStack.EMPTY;
        }

        ItemStack actual = inventory.extractItem(candidate.slot(), candidate.stack().getCount(), false);
        if (!actual.isEmpty()) {
            extracted.add(new ExtractedPackage(candidate.slot(), actual.copy()));
        }
        return actual;
    }

    private static boolean isExpectedExtraction(ItemStack actual, ItemStack expected) {
        return !actual.isEmpty()
                && actual.getCount() == expected.getCount()
                && ItemStack.isSameItemSameComponents(actual, expected);
    }

    private static boolean satisfiesCraftingContext(List<CandidatePackage> packages, PackageOrderWithCrafts context) {
        return !hasCraftingContext(context)
                || containsAtLeast(packageContents(packages), requiredCraftingContents(context));
    }

    private static boolean hasCraftingContext(PackageOrderWithCrafts context) {
        return context != null && !context.orderedCrafts().isEmpty();
    }

    private static PackageOrderWithCrafts mergeCraftingContext(PackageOrderWithCrafts context) {
        InventorySummary requiredContents = requiredCraftingContents(context);
        return new PackageOrderWithCrafts(new PackageOrder(copyBigItemStacks(requiredContents.getStacks())),
                context.orderedCrafts());
    }

    private static InventorySummary requiredCraftingContents(PackageOrderWithCrafts context) {
        InventorySummary summary = new InventorySummary();
        if (!hasCraftingContext(context)) {
            return summary;
        }

        for (CraftingEntry entry : context.orderedCrafts()) {
            for (BigItemStack stack : entry.pattern().stacks()) {
                if (stack.stack.isEmpty() || stack.count <= 0 || entry.count() <= 0) {
                    continue;
                }
                long count = (long) stack.count * entry.count();
                if (count > Integer.MAX_VALUE) {
                    summary.add(stack.stack, Integer.MAX_VALUE);
                    continue;
                }
                summary.add(stack.stack, (int) count);
            }
        }
        return summary;
    }

    private static InventorySummary packageContents(List<CandidatePackage> packages) {
        InventorySummary summary = new InventorySummary();
        for (CandidatePackage candidate : packages) {
            addPackageContents(summary, candidate.stack(), 1);
        }
        return summary;
    }

    private static boolean outputContentsMatchExtractedPackages(List<ExtractedPackage> extracted,
            List<BigItemStack> outputs) {
        InventorySummary expected = new InventorySummary();
        for (ExtractedPackage extractedPackage : extracted) {
            if (!addPackageContents(expected, extractedPackage.stack(), 1)) {
                return false;
            }
        }

        InventorySummary actual = new InventorySummary();
        for (BigItemStack output : outputs) {
            if (output.count <= 0 || !addPackageContents(actual, output.stack, output.count)) {
                return false;
            }
        }

        return summariesEqual(expected, actual);
    }

    private static boolean addPackageContents(InventorySummary summary, ItemStack packageStack, int packageCount) {
        if (!PackageItem.isPackage(packageStack) || packageCount <= 0) {
            return false;
        }

        ItemStackHandler contents = PackageItem.getContents(packageStack);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack stack = contents.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            long count = (long) stack.getCount() * packageCount;
            if (count > Integer.MAX_VALUE) {
                return false;
            }
            summary.add(stack, (int) count);
        }
        return true;
    }

    private static boolean containsAtLeast(InventorySummary actual, InventorySummary required) {
        for (BigItemStack requiredStack : required.getStacks()) {
            if (actual.getCountOf(requiredStack.stack) < requiredStack.count) {
                return false;
            }
        }
        return true;
    }

    private static boolean summariesEqual(InventorySummary left, InventorySummary right) {
        List<BigItemStack> leftStacks = left.getStacks();
        List<BigItemStack> rightStacks = right.getStacks();
        if (leftStacks.size() != rightStacks.size()) {
            return false;
        }

        for (BigItemStack stack : leftStacks) {
            if (right.getCountOf(stack.stack) != stack.count) {
                return false;
            }
        }
        return true;
    }

    private static List<BigItemStack> copyBigItemStacks(List<BigItemStack> stacks) {
        return stacks.stream()
                .map(stack -> new BigItemStack(stack.stack.copy(), stack.count))
                .toList();
    }

    private static void rollbackOrDrop(RepackagerBlockEntity repackager, IItemHandler inventory,
            List<ExtractedPackage> extracted, String reason) {
        repackager.repackageHelper.clear();

        for (int i = extracted.size() - 1; i >= 0; i--) {
            ExtractedPackage extractedPackage = extracted.get(i);
            ItemStack remainder = insertBack(inventory, extractedPackage);
            if (!remainder.isEmpty()) {
                dropRemainder(repackager, remainder, reason);
            }
        }
    }

    private static ItemStack insertBack(IItemHandler inventory, ExtractedPackage extractedPackage) {
        ItemStack remainder = extractedPackage.stack().copy();
        try {
            if (extractedPackage.slot() < inventory.getSlots()) {
                remainder = inventory.insertItem(extractedPackage.slot(), remainder, false);
            }
            if (!remainder.isEmpty()) {
                remainder = ItemHandlerHelper.insertItemStacked(inventory, remainder, false);
            }
        } catch (RuntimeException exception) {
            GuGuAddons.LOGGER.error("Create repackager dupe guard failed to roll back package into target inventory",
                    exception);
        }
        return remainder;
    }

    private static void dropRemainder(RepackagerBlockEntity repackager, ItemStack remainder, String reason) {
        Level level = repackager.getLevel();
        BlockPos pos = repackager.getBlockPos();
        if (level == null || level.isClientSide()) {
            GuGuAddons.LOGGER.error("Create repackager dupe guard could not return {} at {} after {}",
                    remainder, pos, reason);
            return;
        }

        GuGuAddons.LOGGER.warn("Create repackager dupe guard dropped rollback remainder {} at {} after {}",
                remainder, pos, reason);
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                remainder.copy());
    }

    private record CandidatePackage(int slot, int scanIndex, ItemStack stack, int orderId,
            PackageOrderData orderData, PackageOrderWithCrafts orderContext, String address) {
        private boolean isFragmented() {
            return orderData != null;
        }
    }

    private record OrderPosition(int linkIndex, int fragmentIndex) {
    }

    private record CrossCraftingKey(String address, List<CraftingEntry> orderedCrafts) {
    }

    private record CompleteOrder(int orderId, List<CandidatePackage> packages, int firstScanIndex,
            int completionScanIndex, PackageOrderWithCrafts normalizedContext) {
    }

    private record ExtractedPackage(int slot, ItemStack stack) {
    }
}
