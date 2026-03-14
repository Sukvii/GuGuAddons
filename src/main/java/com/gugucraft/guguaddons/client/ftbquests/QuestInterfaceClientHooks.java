package com.gugucraft.guguaddons.client.ftbquests;

import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.TeamData;

public final class QuestInterfaceClientHooks {
    private QuestInterfaceClientHooks() {
    }

    public static boolean canEditTaskSelection(TeamData data) {
        return data.getCanEdit(FTBQuestsClient.getClientPlayer());
    }
}
