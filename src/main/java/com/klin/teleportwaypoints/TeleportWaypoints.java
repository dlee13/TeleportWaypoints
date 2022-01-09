package com.klin.teleportwaypoints;

import com.klin.teleportwaypoints.utility.WaypointConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TeleportWaypoints extends JavaPlugin {
    private static TeleportWaypoints instance;

    @Override
    public void onEnable(){
        instance = this;
        WaypointConfig.setUp("general");
        WaypointConfig.setUp("player");
        WaypointConfig.setUp("waypoint");

        getServer().getPluginManager().registerEvents(new TeleportWaypointEvents(), this);

        TeleportWaypointCommands wc = new TeleportWaypointCommands();
        getCommand("worldhome").setExecutor(wc);
        getCommand("worldcamp").setExecutor(wc);
        getCommand("packup").setExecutor(wc);
        getCommand("setspawn").setExecutor(wc);
//        getCommand("sethub").setExecutor(wc);
        getCommand("fake").setExecutor(wc);
        getCommand("givepoint").setExecutor(wc);
        getCommand("readwaypoints").setExecutor(wc);
        getCommand("clearwaitlist").setExecutor(wc);
        getCommand("setpointsrequired").setExecutor(wc);
        getCommand("setcamp").setExecutor(wc);
        getCommand("camp").setExecutor(wc);
        getCommand("waypoint").setExecutor(wc);
        getCommand("sethome").setExecutor(wc);
        getCommand("home").setExecutor(wc);
        getCommand("spawn").setExecutor(wc);
//        getCommand("hub").setExecutor(wc);

        refillPoints();
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "TeleportWaypoints [ON]");
    }

    @Override
    public void onDisable(){
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "TeleportWaypoints [OFF]");
    }

    public static TeleportWaypoints getInstance() {
        return instance;
    }

    private static void refillPoints(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy MM dd HH mm");
        LocalDateTime ldt = LocalDateTime.now();
        String time = dtf.format(ldt);
        time = time.substring(0,8) + (Integer.parseInt(time.substring(8,10))-0.25) + time.substring(10);

        ConfigurationSection section =
                WaypointConfig.get("player").getConfigurationSection("");
        for (String key : section.getKeys(false))
            WaypointConfig.get("player").set(key + ".charge", time);
        WaypointConfig.save("player");
    }
}
