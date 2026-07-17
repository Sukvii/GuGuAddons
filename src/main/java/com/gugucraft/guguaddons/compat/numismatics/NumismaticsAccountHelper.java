package com.gugucraft.guguaddons.compat.numismatics;

import com.gugucraft.guguaddons.GuGuAddons;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;

public final class NumismaticsAccountHelper {
    private NumismaticsAccountHelper() {
    }

    public static int repairNegativeBalance(BankAccount account) {
        int balance = account.getBalance();
        if (balance < 0) {
            GuGuAddons.LOGGER.warn("Repairing negative Numismatics balance for account {} ({}) from {}",
                    account.id, account.type, balance);
            account.setBalance(Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
        return balance;
    }

    public static boolean canDeposit(BankAccount account, long amount) {
        if (amount < 0L) {
            return false;
        }
        long capacity = (long) Integer.MAX_VALUE - repairNegativeBalance(account);
        return amount <= capacity;
    }

    public static boolean deposit(BankAccount account, long amount) {
        if (!canDeposit(account, amount)) {
            return false;
        }
        account.deposit((int) amount);
        return true;
    }

    public static int depositUpToCapacity(BankAccount account, long amount) {
        int balance = repairNegativeBalance(account);
        if (amount < 0L) {
            return 0;
        }

        long capacity = (long) Integer.MAX_VALUE - balance;
        int deposited = (int) Math.min(amount, capacity);
        if (deposited > 0) {
            account.deposit(deposited);
        }
        return deposited;
    }
}
