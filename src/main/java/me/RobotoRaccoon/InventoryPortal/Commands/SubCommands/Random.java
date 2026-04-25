package me.RobotoRaccoon.InventoryPortal.Commands.SubCommands;

import me.RobotoRaccoon.InventoryPortal.Commands.CommandInfo;
import me.RobotoRaccoon.InventoryPortal.Commands.SubCommand;
import me.RobotoRaccoon.InventoryPortal.Helper.EssentialsHelper;
import me.RobotoRaccoon.InventoryPortal.Helper.SoundHelper;
import me.RobotoRaccoon.InventoryPortal.Helper.WorldGuardHelper;
import me.RobotoRaccoon.InventoryPortal.InventoryPortal;
import me.RobotoRaccoon.InventoryPortal.LangString;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Random extends SubCommand {

    private static final java.util.Random RAND = new java.util.Random();
    private Map<Player, Long> lastUsed;

    public Random() {
        setName("random");
        lastUsed = new HashMap<>();
    }

    public void handle(CommandInfo commandInfo) {
        warp((Player) commandInfo.getSender());
    }

    public void warp(Player player) {
        ConfigurationSection config = InventoryPortal.getConfiguration().getConfig().getConfigurationSection("command.random");
        World world = player.getWorld();

        // Make sure the player is in a valid world
        if (!isWorldEnabled(world)) {
            new LangString("command.random.world-not-enabled").send(player);
            return;
        }

        // Deny if the player is in cool-down
        if (lastUsed.containsKey(player) && !player.hasPermission("inventoryportal.random.cool-down")) {
            long timeRemaining = lastUsed.get(player) + config.getInt("cool-down") - System.currentTimeMillis() / 1000;
            if (timeRemaining > 0) {
                new LangString("command.random.cool-down", timeRemaining).send(player);
                return;
            }
        }

        Location loc = getRandomLocation(world);
        List<String> blacklist = getConfig().getStringList("avoid-blocks");

        // Find suitable random location, with a limit on attempts
        for (int i = 0; i < 10; i++) {
            Block beneath = world.getBlockAt(loc).getRelative(BlockFace.DOWN);

            Boolean canBuild = !getConfig().getBoolean("avoid-regions") || WorldGuardHelper.canBuild(player, loc);
            Boolean isSafe = !blacklist.contains(beneath.getType().toString());

            if (canBuild && isSafe) {
                break;
            }

            // Inappropriate location, so get a new location and try again
            loc = getRandomLocation(world);
        }

        // Apply potion effect
        applyDamageResistance(player);

        // Update Essentials /back
        if (config.getBoolean("use-essentials-back")) {
            new EssentialsHelper().updateBackLocation(player);
        }

        // Update cool-down
        lastUsed.put(player, System.currentTimeMillis() / 1000);

        // Send player to the random location
        new LangString("command.random.warping").send(player);
        player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new SoundHelper("random").play(player);
    }

    private Location getRandomLocation(World world) {
        WorldBorder border = world.getWorldBorder();

        int x = (int) ((RAND.nextDouble() - 0.5) * border.getSize() + border.getCenter().getX());
        int z = (int) ((RAND.nextDouble() - 0.5) * border.getSize() + border.getCenter().getZ());
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    private boolean isWorldEnabled(World world) {
        return getConfig().getStringList("enabled-worlds").contains(world.getName());
    }

    private void applyDamageResistance(Player player) {
        int duration = getConfig().getInt("resistance-duration");
        if (duration <= 0) {
            return;
        }

        PotionEffect effect = new PotionEffect(PotionEffectType.RESISTANCE, duration, 1, true, false);
        player.addPotionEffect(effect);
    }
}
