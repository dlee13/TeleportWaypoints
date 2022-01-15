package com.klin.teleportwaypoints.utility;

import com.klin.teleportwaypoints.TeleportWaypoints;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class Teleport {
    final static NamespacedKey KEY =
            new NamespacedKey(TeleportWaypoints.getInstance(), "teleportWaypoints");

    public static void teleportPlayer(Player player, String[] cords,
                                      ArrayList<String> waitlist, String message, String time){
        if(player.getGameMode().equals(GameMode.CREATIVE) ||
                player.getGameMode().equals(GameMode.SPECTATOR)){
            teleport(player, cords, message);
            return;
        }
        waitlist.add(""+player.getUniqueId());
        BossBar bossbar = Bukkit.createBossBar(
                KEY, "teleporting. . .", BarColor.GREEN, BarStyle.SEGMENTED_20);
        bossbar.addPlayer(player);
        bossbar.setProgress(1);
        final double health = player.getHealth();
        final int playerX = player.getLocation().getBlockX();
        final int playerZ = player.getLocation().getBlockZ();

        new Task(TeleportWaypoints.getInstance(), 0, 7) {
            double progress = 1;
            @Override
            public void run() {
                if (player.getHealth() < health ||
                        !(player.getLocation().getBlockX() == playerX &&
                                player.getLocation().getBlockZ() == playerZ)) {
                    player.sendMessage("Cancelling teleportation");
                    bossbar.removePlayer(player);
                    Bukkit.removeBossBar(KEY);
                    waitlist.remove(""+player.getUniqueId());
                    cancel();
                    return;
                }

                if (progress > 0.05) {
                    progress -= 0.05;
                    bossbar.setProgress(progress);
                } else {
                    teleport(player, cords, message);
                    if(time!=null) {
                        WaypointConfig.get("player").set(player.getUniqueId() + ".charge", time);
                        WaypointConfig.save("player");
                        player.getPersistentDataContainer().remove(KEY);
                    }
                    bossbar.removePlayer(player);
                    Bukkit.removeBossBar(KEY);
                    waitlist.remove(""+player.getUniqueId());
                    cancel();
                }
            }
        };
    }

    private static void teleport(Player player, String[] cords, String message){
        player.teleport(new Location(Bukkit.getWorld(cords[3]),
                Double.parseDouble(cords[0]) + .5, Double.parseDouble(cords[1]),
                Double.parseDouble(cords[2]) + .5,
                player.getLocation().getYaw(), player.getLocation().getPitch()));
        player.sendMessage(message);
    }

    public static ItemStack createShield(String index, boolean black){
        String[] patterns = WaypointConfig.get("waypoint").getString(index+".patterns").
                split(" ");
        String[] colors = WaypointConfig.get("waypoint").getString(index+".colors").split(" ");

        ItemStack shieldItem = new ItemStack(Material.SHIELD);
        ItemMeta meta = shieldItem.getItemMeta();
        BlockStateMeta bmeta = (BlockStateMeta) meta;

        Banner banner = (Banner) bmeta.getBlockState();
        banner.setBaseColor(DyeColor.valueOf(colors[0]));

        for(int i=1; i<patterns.length; i++) {
            banner.addPattern(new Pattern(DyeColor.valueOf(colors[i]),
                    PatternType.valueOf(patterns[i])));
        }
        String blackIndex = "";
        if(black)
            blackIndex = "§0" + index;
        bmeta.setDisplayName("§6"+WaypointConfig.get("waypoint").getString(index+".name").
                replace("_", " ") + blackIndex);
        bmeta.setLore(
                Arrays.asList("§7"+WaypointConfig.get("waypoint").getString(index+".cords")));
        bmeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bmeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        banner.update();

        bmeta.setBlockState(banner);
        shieldItem.setItemMeta(bmeta);

        return shieldItem;
    }
}
