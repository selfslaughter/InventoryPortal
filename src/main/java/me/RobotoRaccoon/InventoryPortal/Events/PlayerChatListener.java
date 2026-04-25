package me.RobotoRaccoon.InventoryPortal.Events;

import me.RobotoRaccoon.InventoryPortal.Helper.SoundHelper;
import me.RobotoRaccoon.InventoryPortal.InventoryPortal;
import me.RobotoRaccoon.InventoryPortal.Menu.EditMenu;
import me.RobotoRaccoon.InventoryPortal.Menu.EditOption;
import me.RobotoRaccoon.InventoryPortal.Warp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerChatListener implements Listener {

    public static final Map<Player, EditOption> EDIT_MAP = new HashMap<>();

    private static final String CANCEL = "cancel";
    private static final int SPLIT_COLUMN = 45;

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!EDIT_MAP.containsKey(player)) {
            return;
        }

        event.setCancelled(true);
        EditOption option = EDIT_MAP.get(player);
        EDIT_MAP.remove(player);

        String message = event.getMessage();
        String cancel = InventoryPortal.getConfiguration().getConfig().getString("command.edit.cancel");
        // If message is CANCEL or the one defined in the config, do not edit.
        if (message.equalsIgnoreCase(CANCEL) || message.equalsIgnoreCase(cancel)) {
            new SoundHelper("edit-cancel").play(player);
            openMenu(player, option.getWarp());
            return;
        }

        // Update appropriate setting
        switch (option.getSetting()) {
        case DISPLAYNAME:
            option.getWarp().setDisplayName(message);
            break;
        case DESCRIPTION:
            option.getWarp().setDescription(warpString(message));
            break;
        }

        new SoundHelper("edit-success").play(player);
        openMenu(player, option.getWarp());
    }

    private void openMenu(Player player, Warp warp) {
        Bukkit.getScheduler().runTask(InventoryPortal.getPlugin(), () ->
            InventoryPortal.getHandler().openMenu(player, new EditMenu(player, warp))
        );
    }

    private List<String> warpString(String message) {
        List<String> words = Arrays.asList(message.split(" "));
        return words;
    }
}
