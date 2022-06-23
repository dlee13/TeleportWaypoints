package com.klin.teleportwaypoints;

import com.klin.teleportwaypoints.utility.Teleport;
import com.klin.teleportwaypoints.utility.WaypointConfig;

import xyz.holocons.mc.waypoints.Exporter;

import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class TeleportWaypointCommands implements CommandExecutor {
    private static final NamespacedKey KEY = new NamespacedKey
            (TeleportWaypoints.getInstance(), "teleportWaypoints");
    private static ArrayList<String> waitlist = new ArrayList<>();
    public static ArrayList<String> getWaitlist(){
        return waitlist;
    }
    public static boolean checkPlayer(String UId){
        return waitlist!=null && waitlist.contains(UId);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!(sender instanceof Player player)) {
            if (cmd.getName().equalsIgnoreCase("waypoint") && args.length > 0 && args[0].equalsIgnoreCase("export")) {
                final var exporter = new Exporter(TeleportWaypoints.getInstance());
                try {
                    exporter.exportTravelers(WaypointConfig.get("player"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    exporter.exportWaypoints(WaypointConfig.get("waypoint"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        if(player.isOp()) {
            switch (cmd.getName().toLowerCase()) {
                case "worldhome":
                    WaypointConfig.get("general").set("worldhome", player.getWorld().getName());
                    WaypointConfig.save("general");
                    player.sendMessage("Home world set");
                    return true;

                case "worldcamp":
                    WaypointConfig.get("general").set("worldcamp", player.getWorld().getName());
                    WaypointConfig.save("general");
                    player.sendMessage("Camp world set");
                    return true;

                case "packup":
                    ConfigurationSection sec =
                            WaypointConfig.get("player").getConfigurationSection("");
                    for (String key : sec.getKeys(false))
                        WaypointConfig.get("player").set(key + ".camp", "");
                    WaypointConfig.save("player");
                    player.sendMessage("All camps packed up");
                    return true;

                case "setspawn":
                    Location spawnLoc = player.getLocation();
                    String world = spawnLoc.getWorld().getName();
                    if (world.contains("_"))
                        world = world.substring(0, world.indexOf("_"));
                    String spawnStr = spawnLoc.getBlockX() + " " + spawnLoc.getBlockY() + " " +
                            spawnLoc.getBlockZ() + " " + spawnLoc.getWorld().getName();
                    WaypointConfig.get("general").set("spawn." + world, spawnStr);
                    WaypointConfig.save("general");
                    player.sendMessage("Spawn for "+world+" set");
                    return true;

//                case "sethub":
//                    Location hubLoc = player.getLocation();
//                    String hubStr = hubLoc.getBlockX() + " " + hubLoc.getBlockY() + " " +
//                            hubLoc.getBlockZ() + " " + hubLoc.getWorld().getName();
//                    WaypointConfig.get("general").set("hub", hubStr);
//                    WaypointConfig.save("general");
//                    player.sendMessage("Universe hub set");
//                    return true;
                    
                case "fake":
                    if (args.length < 1) {
                        player.sendMessage("Fake what exactly");
                        return true;
                    }
                    try{
                        Integer.parseInt(args[0]);
                    }catch(NumberFormatException e){
                        player.sendMessage("Invalid index");
                        return true;
                    }
                    String cordStr = WaypointConfig.get("waypoint").
                            getString(args[0] + ".cords");
                    if (cordStr == null) {
                        player.sendMessage("Waypoint at this index doesn't exist");
                        return true;
                    }
                    String [] cords = cordStr.split(" ");
                    String name =
                            WaypointConfig.get("waypoint").getString(args[0] + ".name");

                    Location playerLoc = player.getLocation();
                    Location loc = new Location(player.
                            getWorld(), Integer.parseInt(cords[0]), Integer.parseInt(cords[1]),
                            Integer.parseInt(cords[2]));
                    if(Math.abs(playerLoc.getBlockX())-Math.abs(loc.getBlockX())>2 ||
                            Math.abs(playerLoc.getBlockY())-Math.abs(loc.getBlockY())>2 ||
                            Math.abs(playerLoc.getBlockY())-Math.abs(loc.getBlockY())>2){
                        player.sendMessage("Too far from waypoint to fake");
                        return true;
                    }
                    String bannerName = args[0] + "@" + name;
                    Banner banner = (Banner) player.getWorld().getBlockAt(loc).getState();
                    String requirement = WaypointConfig.get("general").getString("pointsrequired");
                    if(requirement==null){
                        player.sendMessage("No points required set");
                        return true;
                    }
                    int pointsRequired = Integer.parseInt(requirement) - 1;
                    String append = "";
                    for (int i=0; i<pointsRequired; i++) {
                        append += " fake";
                    }
                    banner.getPersistentDataContainer().
                            set(KEY, PersistentDataType.STRING, bannerName + append);
                    banner.update();
                    player.sendMessage(bannerName.substring(bannerName.indexOf("@")+1).
                            replace("_"," ")+" faked");
                    return true;

                case "givepoint":
                    Player point = player;
                    if(args.length>=1 && args[0].equals("all")){
                        ConfigurationSection section =
                                WaypointConfig.get("player").getConfigurationSection("");
                        for (String key : section.getKeys(false))
                            WaypointConfig.get("player").set(key + ".point", "1");
                        WaypointConfig.save("player");
                        player.sendMessage("Waypoint points given to all");
                        return true;
                    }
                    if(args.length>=1)
                        point = Bukkit.getPlayer(args[0]);
                    if(point==null) {
                        player.sendMessage("This player isn't present");
                        return true;
                    }
                    WaypointConfig.get("player").set(point.getUniqueId() + ".point", "1");
                    WaypointConfig.save("player");
                    player.sendMessage("Waypoint point given");
                    return true;

                case "readwaypoints":
                    WaypointConfig.setUp("waypoint");
                    WaypointConfig.setUp("player");
                    WaypointConfig.setUp("general");
                    player.sendMessage("Config read");
                    return true;

                case "clearwaitlist":
                    while (!waitlist.isEmpty())
                        waitlist.remove(0);
                    TeleportWaypointEvents.clearPlayers();
                    player.sendMessage("Waitlist cleared");
                    return true;

                case "setpointsrequired":
                    if (args.length < 1) {
                        player.sendMessage("Invalid quantity");
                        return true;
                    }
                    try{
                        Integer.parseInt(args[0]);
                    }catch(NumberFormatException e){
                        player.sendMessage("Invalid quantity");
                        return true;
                    }
                    WaypointConfig.get("general").set("pointsrequired", args[0]);
                    WaypointConfig.save("general");
                    player.sendMessage("Points required set to " + args[0]);
                    return true;
            }
        }

        switch(cmd.getName().toLowerCase()) {
            case "sethome":
                String worldhome = WaypointConfig.get("general").getString("worldhome");
                if (worldhome==null || !player.getWorld().getName().contains(worldhome)) {
                    player.sendMessage("Invalid home");
                    return true;
                }
                String home = (player.getLocation().getX()-0.5) + " " +
                        player.getLocation().getY() + " " +
                        (player.getLocation().getZ()-0.5) + " " +
                        player.getWorld().getName();
                WaypointConfig.get("player").set(player.getUniqueId() + ".home", home);
                WaypointConfig.save("player");
                player.sendMessage("Successfully set home");
                return true;

            case "home":
                if (waitlist != null && waitlist.contains("" + player.getUniqueId())) {
                    player.sendMessage("Try again shortly");
                    return true;
                }
                String homeStr =
                        WaypointConfig.get("player").getString(player.getUniqueId() + ".home");
                if (homeStr == null || homeStr.trim().isEmpty()) {
                    player.sendMessage("No home found");
                    return true;
                }
                Teleport.teleportPlayer(
                        player, homeStr.split(" "), waitlist, "Welcome home", null);
                return true;

            case "setcamp":
                String worldcamp = WaypointConfig.get("general").getString("worldcamp");
                if (worldcamp==null || !player.getWorld().getName().contains(worldcamp)) {
                    player.sendMessage("Invalid camp");
                    return true;
                }
                String camp = (player.getLocation().getX()-0.5) + " " +
                        player.getLocation().getY() + " " +
                        (player.getLocation().getZ()-0.5) + " " +
                        player.getWorld().getName();
                WaypointConfig.get("player").set(player.getUniqueId() + ".camp", camp);
                WaypointConfig.save("player");
                player.sendMessage("Successfully set camp");
                return true;

            case "camp":
                if (waitlist != null && waitlist.contains(player.getUniqueId())) {
                    player.sendMessage("Try again shortly");
                    return true;
                }
                String campStr = WaypointConfig.get("player").getString(player.getUniqueId() + ".camp");
                if (campStr == null || campStr.trim().isEmpty()) {
                    player.sendMessage("No camp found");
                    return true;
                }
                Teleport.teleportPlayer(
                        player, campStr.split(" "), waitlist, "Back to mining", null);
                return true;

            case "waypoint":
                int page = 0;
                if(args.length >= 1){
                    try{
                        page = Integer.parseInt(args[0]);
                    }catch(NumberFormatException e){
                        player.sendMessage("Invalid page number");
                        return true;
                    }
                    /*
                    if(args[0].equalsIgnoreCase("help")) {
                        String cmdHome = WaypointConfig.get("general").getString("worldhome")+" ";
                        if(cmdHome==null)
                            cmdHome = "";
                        String creation =
                            "- Players start with 1 waypoint token." +"\n"+
                            "- This token may be added by right-clicking any renamed" +"\n"+
                            "banner placed in the main world." +"\n"+
                            "- When a banner obtains 8 tokens, it becomes a waypoint." +"\n"+
                            "- Completed waypoints are indestructible, while incomplete" +"\n"+
                            "ones may be broken by destroying the banner. This refunds" +"\n"+
                            "the tokens to respective contributors." +"\n"+
                            "- Waypoints are permanent." +"\n"+
                            "If you need to remove one, send us a @ModMail.";
                        String usage =
                            "- Left click a completed waypoint to register it to your" +"\n"+
                            "waypoint menu." +"\n"+
                            "- /waypoint, alias /wa, opens up your waypoint menu if you" +"\n"+
                            "have registered at least one waypoint." +"\n"+
                            "- Clicking on any waypoint in this menu will teleport you to it." +"\n"+
                            "- Teleporting to a waypoint consumes 1 waypoint charge. " +"\n"+
                            "You gain 1 charge an hour, and can store up to 12 charges." +"\n"+
                            "- You may also click the lantern in the waypoint menu to enter" +"\n"+
                            "rearrange mode, allowing you to reorder your waypoints.";
                        String arg = "";
                        if(args.length>=2)
                            arg = args[1];
                        switch(arg){
                            case"creation":
                                player.sendMessage("§6Teleport Waypoints");
                                player.sendMessage(creation);
                                return true;
                            case"usage":
                                player.sendMessage("§6Teleport Waypoints");
                                player.sendMessage(usage);
                                return true;
                            default:
                                player.sendMessage("§6Teleport Waypoints");
                                player.sendMessage("Players can create, register, and use waypoints to travel\n" +
                                        "to various locations in the world.");
                                player.sendMessage("Specify creation or usage for more details.");
                                return true;
                        }
                    }
                    return true;
                    */
                }

                String[] former = WaypointConfig.get("player").getString(
                        player.getUniqueId() + ".charge").split(" ");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy MM dd HH mm");
                String[] curr = dtf.format(LocalDateTime.now()).split(" ");
                double diff = (Double.parseDouble(curr[0])-Double.parseDouble(former[0]))*535680 +
                        (Double.parseDouble(curr[1])-Double.parseDouble(former[1]))*44640 +
                        (Double.parseDouble(curr[2])-Double.parseDouble(former[2]))*1440 +
                        (Double.parseDouble(curr[3])-Double.parseDouble(former[3]))*60 +
                        (Double.parseDouble(curr[4])-Double.parseDouble(former[4]));
                if(diff<60) {
                    player.sendMessage("No charges available. Refill in " + (int) (60-diff) + " minutes");
                    return true;
                }
                player.sendMessage("§7Charges remaining: " + (int) Math.min(12, diff/60));
                String time = "";
                if(diff<=720) {
                    former[4] = Double.parseDouble(former[4]) + 60 + "";
                    time = former[0]+" "+former[1]+" "+former[2]+" "+former[3]+" "+former[4];
                }
                else{
                    curr[2] = Double.parseDouble(curr[2]) - 0.5 + "";
                    curr[4] = Double.parseDouble(curr[4]) + 60 + "";
                    time = curr[0]+" "+curr[1]+" "+curr[2]+" "+curr[3]+" "+curr[4];
                }
                player.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, time);

                String waypointString =
                        WaypointConfig.get("player").getString(player.getUniqueId() +
                        ".waypoints");
                if (waypointString == null || waypointString.trim().isEmpty()) {
                    player.sendMessage("No waypoints found");
                    return true;
                }
                String[] waypointIndexes =
                        WaypointConfig.get("player").getString(player.getUniqueId() +
                        ".waypoints").split(" ");

                if (page < 0 || page > ((waypointIndexes.length - 1) / 54)) {
                    player.sendMessage("Invalid page number");
                    return true;
                }

                Inventory inv = Bukkit.createInventory(
                        null, Math.min(((waypointIndexes.length - 1) / 9 + 1) * 9, 54),
                        "Waypoints");
                
                for (int i = page * 54; i < waypointIndexes.length; i++) {
                    if (!inv.addItem(Teleport.createShield(waypointIndexes[i], false)).isEmpty()) {
                        break;
                    }
                }
                ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                ItemMeta paneMeta = pane.getItemMeta();
                paneMeta.setDisplayName(" ");
                pane.setItemMeta(paneMeta);
                while (inv.firstEmpty() >= 0) {
                    inv.setItem(inv.firstEmpty(), pane);
                }
                player.openInventory(inv);
                return true;

            case"spawn":
                if (waitlist != null &&
                        waitlist.contains(""+player.getUniqueId())) {
                    player.sendMessage("Try again shortly");
                    return true;
                }
                String worldName = player.getWorld().getName();
                if(worldName.contains("_"))
                    worldName = worldName.substring(0, worldName.indexOf("_"));
                String cordStr = WaypointConfig.get("general").getString("spawn."+worldName);
                if (cordStr == null || cordStr.trim().isEmpty()){
                    player.sendMessage("Spawn hasn't been set");
                    return true;
                }
                String[] cords = cordStr.split(" ");
                Teleport.teleportPlayer(player, cords, waitlist,
                        "Welcome to "+worldName+" spawn!", null);
                return true;

//            case"hub":
//                if (waitlist != null &&
//                        waitlist.contains(""+player.getUniqueId())) {
//                    player.sendMessage("Try again shortly");
//                    return true;
//                }
//                String hub = WaypointConfig.get("general").getString("hub");
//                if (hub == null || hub.trim().isEmpty()) {
//                    player.sendMessage("Hub hasn't been set");
//                    return true;
//                }
//                String[] hubCords = hub.split(" ");
//                Teleport.teleportPlayer(player, hubCords, waitlist,
//                        "Welcome to the hub!", null);

            default:
                return true;
        }
    }
}
