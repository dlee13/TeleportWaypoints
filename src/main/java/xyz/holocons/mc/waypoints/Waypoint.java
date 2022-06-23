package xyz.holocons.mc.waypoints;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Location;

public class Waypoint {

    private int id;
    private Location location;
    private ArrayList<UUID> contributors;
    private boolean active;

    public Waypoint(int id, Location location, ArrayList<UUID> contributors, boolean active) {
        this.id = id;
        this.location = location;
        this.contributors = contributors == null ? new ArrayList<>() : contributors;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location.clone();
    }

    public ArrayList<UUID> getContributors() {
        return contributors;
    }

    public boolean isActive() {
        return active;
    }
}
