package xyz.holocons.mc.waypoints;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;

public class Exporter {

    public static final String TRAVELER_FILENAME = "traveler.json";
    public static final String WAYPOINT_FILENAME = "waypoint.json";

    private final JavaPlugin plugin;
    private final Gson gson;
    private final HashMap<UUID, Traveler> travelers;
    private final HashMap<Long, Waypoint> waypoints;

    public Exporter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.travelers = new HashMap<>();
        this.waypoints = new HashMap<>();
    }

    private void convertTravelers(final FileConfiguration playerConfig) {
        for (final var uniqueIdString : playerConfig.getKeys(false)) {
            final var waypointsString = playerConfig.getString(uniqueIdString + ".waypoints");
            if (waypointsString == null || waypointsString.trim().isEmpty()) {
                continue;
            }

            final var registeredWaypoints = new BitSet();
            for (final var waypointId : waypointsString.split(" ")) {
                try {
                    registeredWaypoints.set(Integer.parseInt(waypointId));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            final var pointString = playerConfig.getString(uniqueIdString + ".point");
            var tokens = 0;
            try {
                tokens = Integer.parseInt(pointString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            final var uniqueId = UUID.fromString(uniqueIdString);

            travelers.put(uniqueId, new Traveler(1, tokens, null, null, registeredWaypoints));
        }
    }

    private void convertWaypoints(final FileConfiguration waypointConfig) {
        for (final var waypointIdString : waypointConfig.getKeys(false)) {
            final var active = waypointConfig.getString(waypointIdString + ".status").equalsIgnoreCase("finished");
            final var locationString = waypointConfig.getString(waypointIdString + ".cords").split(" ");

            try {
                final var waypointId = Integer.parseInt(waypointIdString);
                final var x = Integer.parseInt(locationString[0]);
                final var y = Integer.parseInt(locationString[1]);
                final var z = Integer.parseInt(locationString[2]);
                final var world = Bukkit.getWorld(locationString[3]);
                final var location = new Location(world, x, y, z);
                waypoints.put(location.getChunk().getChunkKey(), new Waypoint(waypointId, location, null, active));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public void exportTravelers(final FileConfiguration playerConfig) throws IOException {
        convertTravelers(playerConfig);

        if (travelers.isEmpty()) {
            return;
        }

        final var file = new File(plugin.getDataFolder(), TRAVELER_FILENAME);

        final var writer = new GsonWriter(gson, file);
        writer.beginObject();
        for (final var traveler : travelers.entrySet()) {
            writer.name(traveler.getKey().toString());
            writer.value(traveler.getValue());
        }
        writer.endObject();
        writer.close();

        travelers.clear();
    }

    public void exportWaypoints(final FileConfiguration waypointConfig) throws IOException {
        convertWaypoints(waypointConfig);

        if (waypoints.isEmpty()) {
            return;
        }

        final var file = new File(plugin.getDataFolder(), WAYPOINT_FILENAME);

        final var writer = new GsonWriter(gson, file);
        writer.beginArray();
        for (var waypoint : waypoints.values()) {
            writer.value(waypoint);
        }
        writer.endArray();
        writer.close();

        waypoints.clear();
    }
}
