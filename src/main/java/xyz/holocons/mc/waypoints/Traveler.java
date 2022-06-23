package xyz.holocons.mc.waypoints;

import java.util.BitSet;

import org.bukkit.Location;

public class Traveler {

    private int charges;
    private int tokens;
    private Location home;
    private Location camp;
    private BitSet waypoints;

    public Traveler(int charges, int tokens, Location home, Location camp, BitSet waypoints) {
        this.charges = charges;
        this.tokens = tokens;
        this.home = home;
        this.camp = camp;
        this.waypoints = waypoints != null ? waypoints : new BitSet();
    }

    public int getCharges() {
        return charges;
    }

    public int getTokens() {
        return tokens;
    }

    public Location getHome() {
        return home;
    }

    public Location getCamp() {
        return camp;
    }

    public BitSet getWaypoints() {
        return waypoints;
    }
}
