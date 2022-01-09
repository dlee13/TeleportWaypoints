package com.klin.teleportwaypoints.utility;

import com.klin.teleportwaypoints.TeleportWaypoints;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class WaypointConfig {
    private static File waypointConfigFile;
    private static FileConfiguration waypointConfig;

    private static File playerConfigFile;
    private static FileConfiguration playerConfig;

    private static File generalConfigFile;
    private static FileConfiguration generalConfig;

    public static void save(String name){
        try {
            switch (name) {
                case "waypoint":
                    waypointConfig.save(waypointConfigFile);
                    break;
                case "player":
                    playerConfig.save(playerConfigFile);
                    break;
                case "general":
                    generalConfig.save(generalConfigFile);
                    break;
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void setUp(String name) {
        switch(name) {
            case "waypoint":
                waypointConfigFile =
                        new File(TeleportWaypoints.getInstance().getDataFolder(), name+".yml");
                waypointConfig = new YamlConfiguration();
                setUpProcedure(waypointConfigFile, waypointConfig);
                break;
            case "player":
                playerConfigFile =
                        new File(TeleportWaypoints.getInstance().getDataFolder(), name+".yml");
                playerConfig = new YamlConfiguration();
                setUpProcedure(playerConfigFile, playerConfig);
                break;
            case "general":
                generalConfigFile =
                        new File(TeleportWaypoints.getInstance().getDataFolder(), name+".yml");
                generalConfig = new YamlConfiguration();
                setUpProcedure(generalConfigFile, generalConfig);
                break;
        }
    }

    private static void setUpProcedure(File file, FileConfiguration fileConfig){
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        try {
            fileConfig.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static FileConfiguration get(String name) {
        switch (name) {
            case "waypoint":
                return waypointConfig;
            case "player":
                return playerConfig;
            case "general":
                return generalConfig;
        }
        return null;
    }
}
