package com.klin.teleportwaypoints;

import com.klin.teleportwaypoints.utility.Teleport;
import com.klin.teleportwaypoints.utility.WaypointConfig;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class TeleportWaypointEvents implements Listener {
    private static final NamespacedKey key = new NamespacedKey
            (TeleportWaypoints.getInstance(), "teleportWaypoints");
    private static ArrayList<String> players = new ArrayList<>();
    public static void clearPlayers(){
        while(!players.isEmpty())
            players.remove(0);
    }

    @EventHandler
    public static void initiateNewPlayer(PlayerJoinEvent event){
        Player player = event.getPlayer();
        String check = WaypointConfig.get("player").
                getString(player.getUniqueId().toString());
        if(check == null) {
            WaypointConfig.get("player").set(player.getUniqueId() + ".point", "1");
            WaypointConfig.get("player").set(player.getUniqueId() + ".home", "");
            WaypointConfig.get("player").set(player.getUniqueId() + ".camp", "");
            WaypointConfig.get("player").set(player.getUniqueId() + ".waypoints", "");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy MM dd HH mm");
            LocalDateTime ldt = LocalDateTime.now();
            String time = dtf.format(ldt);
            time = time.substring(0,8) + (Integer.parseInt(time.substring(8,10))-0.25) + time.substring(10);
            WaypointConfig.get("player").set(player.getUniqueId() + ".charge", time);
            WaypointConfig.save("player");
        }
    }

    @EventHandler
    public static void nameBannerMeta(InventoryClickEvent event){
        if(event.isCancelled())
            return;
        if(event.getInventory().getType() != InventoryType.ANVIL ||
                event.getCurrentItem() == null ||
                event.getSlot() != 2 ||
                !event.getCurrentItem().getType().toString().contains("BANNER"))
            return;

        String rename = ((AnvilInventory) event.getView().getTopInventory()).getRenameText();
        if(rename==null)
            return;
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        if(meta==null)
            return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, rename);
        event.getCurrentItem().setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public static void nameBannerState(BlockPlaceEvent event){
        if(event.isCancelled())
            return;
        if(!(event.getBlockPlaced().getState() instanceof Banner) ||
                event.getBlockPlaced().getType().toString().contains("WALL"))
            return;
        Player player = event.getPlayer();
        String worldhome = WaypointConfig.get("general").getString("worldhome");
        if(worldhome==null || !player.getWorld().getName().contains(worldhome))
            return;
        String under = event.getBlockPlaced().getWorld().getBlockAt(event.getBlockPlaced().
                getLocation().clone().add(0,-1,0)).getType().toString();
        if(under.contains("SAND")||under.contains("CONCRETE_POWDER")||under.equals("GRAVEL")||
                under.equals("ANVIL")||under.contains("PISTON")||under.contains("LEAVES"))
            return;
        int index = 0;
        while(WaypointConfig.get("waypoint").getString(""+index)!=null &&
        !WaypointConfig.get("waypoint").getString(index+".status").equals("new"))
            index++;

        if(WaypointConfig.get("waypoint").getString(""+index)!=null &&
                WaypointConfig.get("waypoint").getString(index+".status").equals("new")){
            String[] cords =
                    WaypointConfig.get("waypoint").getString(index+".cords").split(" ");

            BlockState state = player.getWorld().getBlockAt(new Location(player.
                    getWorld(), Integer.parseInt(cords[0]), Integer.parseInt(cords[1]),
                    Integer.parseInt(cords[2]))).getState();
            if(state instanceof Banner) {
                Banner banner = (Banner) state;
                smite(banner, "" + index);
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        String bannerName = null;
        if(item.hasItemMeta())
            bannerName = item.getItemMeta().getPersistentDataContainer().
                    get(key, PersistentDataType.STRING);
        if(bannerName==null)
            return;
        bannerName = bannerName.replace(" ","_");
        Banner banner = (Banner) event.getBlockPlaced().getState();
        List<Pattern> patterns = banner.getPatterns();
        String patternString = "base";
        String colorString =
                player.getInventory().getItemInMainHand().getType().toString();
        colorString = colorString.substring(0, colorString.indexOf("_BANNER"));
        for(Pattern pattern : patterns){
            if(pattern != null) {
                patternString += " " + pattern.getPattern().toString();
                colorString += " " + pattern.getColor().toString();
            }
        }
        Location loc = event.getBlockPlaced().getLocation();
        WaypointConfig.get("waypoint").set(index + ".name", bannerName);
        WaypointConfig.get("waypoint").set(index + ".patterns", patternString);
        WaypointConfig.get("waypoint").set(index + ".colors", colorString);
        WaypointConfig.get("waypoint").set(index + ".cords", loc.getBlockX() + " " +
                loc.getBlockY() + " " + loc.getBlockZ() + " " +loc.getWorld().getName());
        WaypointConfig.get("waypoint").set(index+".status", "new");
        WaypointConfig.save("waypoint");

        banner.getPersistentDataContainer().
                set(key, PersistentDataType.STRING, index+"@"+bannerName);
        banner.update();
    }

    @EventHandler
    public static void contributePoint(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                !(block.getState() instanceof Banner) ||
                block.getType().toString().contains("WALL"))
            return;
        Player player = event.getPlayer();
        String requirement = WaypointConfig.get("general").getString("pointsrequired");
        if(requirement==null){
            player.sendMessage("No required points set");
            return;
        }

        if(players!=null && players.contains(""+player.getUniqueId())) {
            return;
        }
        players.add(""+player.getUniqueId());

        Banner banner = (Banner) block.getState();
        String contributors = banner.getPersistentDataContainer().
                get(key,PersistentDataType.STRING);

        if(contributors == null){
            player.sendMessage("Looks to be an ordinary banner");

            new BukkitRunnable() {
                @Override
                public void run() {
                    players.remove(""+player.getUniqueId());
                }
            }.runTaskLater(TeleportWaypoints.getInstance(), 10L);
            return;
        }

        String bannerName = "";
        if(count(contributors, " ")>=1)
            bannerName = contributors.substring(0, contributors.indexOf(" "));
        else
            bannerName = contributors;
        String index = bannerName.substring(0,bannerName.indexOf("@"));
        bannerName = bannerName.substring(bannerName.indexOf("@")+1);

        if(WaypointConfig.get("waypoint").getString(index+".status").equals("finished")){
            player.sendMessage("Waypoint already complete");

            new BukkitRunnable() {
                @Override
                public void run() {
                    players.remove(""+player.getUniqueId());
                }
            }.runTaskLater(TeleportWaypoints.getInstance(), 10L);
            return;
        }

        World world = block.getWorld();
        if(contributors.contains(" "+player.getUniqueId())) {
            contributors = contributors.
                    substring(0,contributors.indexOf(" "+player.getUniqueId())) +
                    contributors.
                            substring(contributors.indexOf(" "+player.getUniqueId())+37);

            banner.getPersistentDataContainer().set(key, PersistentDataType.STRING,contributors);
            banner.update();

            WaypointConfig.get("player").set(player.getUniqueId() + ".point", "1");
            WaypointConfig.save("player");

            ArmorStand stand = (ArmorStand)
                    world.getNearbyEntities(block.getLocation().clone().
                            add(0.5, 1.6, 0.5), 0.1, 0.1, 0.1).iterator().next();
            if(count(contributors, " ") == 0)
                stand.remove();
            else
                stand.setCustomName(bannerName.replace("_", " ") + " " +
                        (count(contributors, " ") + "/" + requirement));
            player.sendMessage("Point retrieved");

            new BukkitRunnable() {
                @Override
                public void run() {
                    players.remove(""+player.getUniqueId());
                }
            }.runTaskLater(TeleportWaypoints.getInstance(), 10L);
            return;
        }

        if(WaypointConfig.get("player").
                getString(player.getUniqueId()+".point").equals("0")) {
            player.sendMessage("No points in possession");
            new BukkitRunnable() {
                @Override
                public void run() {
                    players.remove(""+player.getUniqueId());
                }
            }.runTaskLater(TeleportWaypoints.getInstance(), 10L);
            return;
        }
        contributors = contributors + " " + player.getUniqueId();
        banner.getPersistentDataContainer().set(key, PersistentDataType.STRING, contributors);
        banner.update();

        WaypointConfig.get("player").set(player.getUniqueId() + ".point", "0");
        WaypointConfig.save("player");

        player.sendMessage("Point contributed");
        world.playSound(block.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.1F, 0.3F);
        if (count(contributors, " ") == 1) {
            ArmorStand stand = world.spawn(block.
                    getLocation().clone().add(0.5, 1.6, 0.5), ArmorStand.class);
            stand.setMarker(true);
            stand.setCustomName(bannerName.replace("_", " ") + " 1/"+requirement);
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setCustomNameVisible(true);

            WaypointConfig.get("waypoint").set(index+".status", "in progress");
            WaypointConfig.save("waypoint");
        }
        else {
            ArmorStand stand =
                    (ArmorStand) world.getNearbyEntities(event.
                    getClickedBlock().getLocation().clone().
                            add(0.5, 1.6, 0.5), 0.1, 0.1, 0.1).iterator().next();
            if(count(contributors, " ") >= Integer.parseInt(requirement)) {
                stand.setCustomName("§6" + bannerName.replace("_", " "));
                WaypointConfig.get("waypoint").set(index + ".status", "finished");
                WaypointConfig.save("waypoint");
                player.sendMessage(
                        "The world opens itself before those with noble hearts");
                world.playSound(block.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,0.6F, 0.3F);
            }
            else
                stand.setCustomName(bannerName.replace("_", " ") +
                        " " + count(contributors, " ") + "/" + requirement);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                players.remove(""+player.getUniqueId());
            }
        }.runTaskLater(TeleportWaypoints.getInstance(), 10L);
    }

    @EventHandler
    public static void registerWaypoint(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getAction() != Action.LEFT_CLICK_BLOCK ||
                !(event.getClickedBlock().getState() instanceof Banner) ||
                event.getClickedBlock().getType().toString().contains("WALL"))
            return;

        String bannerName =
                ((Banner)event.getClickedBlock().getState()).getPersistentDataContainer().
                get(key, PersistentDataType.STRING);

        if(bannerName == null)
            return;
        Player player = event.getPlayer();
        String requirement = WaypointConfig.get("general").getString("pointsrequired");
        if(requirement==null){
            event.getPlayer().sendMessage("No required points set");
            return;
        }
        if(count(bannerName, " ") < Integer.parseInt(requirement)) {
            player.sendMessage("Charging...");
            return;
        }
        int index = Integer.parseInt(bannerName.substring(0,bannerName.indexOf("@")));
        bannerName = bannerName.substring(bannerName.indexOf("@")+1, bannerName.indexOf(" "));

        String collectedWaypoints = WaypointConfig.get("player").getString(
                player.getUniqueId()+".waypoints");
        ConfigurationSection section =
                WaypointConfig.get("waypoint").getConfigurationSection("");
        boolean found = false;
        for (String key : section.getKeys(false)) {
            if(bannerName.equals(WaypointConfig.get("waypoint").getString(key+".name"))){
                index = Integer.parseInt(key);
                found = true;
                break;
            }
        }
        if(!found){
            player.sendMessage("This waypoint shouldn't exist");
            return;
        }

        if(collectedWaypoints != null && (" "+collectedWaypoints).contains(" "+index+" ")) {
            player.sendMessage("Waypoint already registered");
            return;
        }

        WaypointConfig.get("player").set(player.getUniqueId()+".waypoints",
                collectedWaypoints+index+" ");
        WaypointConfig.save("player");

        player.sendMessage("Waypoint registered");
    }

    @EventHandler
    public static void smiteProgress(BlockBreakEvent event){
        if(event.isCancelled())
            return;
        int y = event.getBlock().getY();
        if(!(event.getBlock().getState() instanceof Banner) ||
                event.getBlock().getType().toString().contains("WALL")) {
            if (!(event.getBlock().getWorld().getBlockAt(event.getBlock().getX(),
                    y+1, event.getBlock().getZ()).getState() instanceof Banner) ||
                    event.getBlock().getType().toString().contains("WALL"))
                return;
            else
                y++;
        }
        Location loc = new Location(event.getBlock().getWorld(),
                event.getBlock().getX(), y, event.getBlock().getZ());
        World world = event.getBlock().getWorld();
        Block block = world.getBlockAt(loc);
        Banner banner = (Banner) block.getState();

        String bannerData = banner.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if(bannerData == null)
            return;

        String requirement = WaypointConfig.get("general").getString("pointsrequired");
        if(requirement==null){
            event.getPlayer().sendMessage("No required points set");
            return;
        }
        if(bannerData.split(" ").length< Integer.parseInt(requirement)) {
            smite(banner, bannerData.substring(0, bannerData.indexOf("@")));
            event.setDropItems(false);
            ItemStack bannerItem = block.getDrops().iterator().next();
            ItemMeta bannerMeta = bannerItem.getItemMeta();
            String strData = bannerData.substring(bannerData.indexOf("@")+1);
            if(strData.contains(" "))
                strData = strData.substring(0, strData.indexOf(" "));
            bannerMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, strData);
            bannerItem.setItemMeta(bannerMeta);
            world.dropItemNaturally(loc, bannerItem);
        }
        else if(event.getPlayer().isOp() &&
                event.getPlayer().getInventory().getItemInMainHand().getType().
                equals(Material.STONE_SHOVEL)) {
            String index = bannerData.substring(0, bannerData.indexOf("@"));
            smite(banner, index);
            ConfigurationSection section =
                    WaypointConfig.get("player").getConfigurationSection("");
            for (String key : section.getKeys(false)) {
                String wayStr = WaypointConfig.get("player").getString(key + ".waypoints");
                if(wayStr!=null && (" "+wayStr).contains(" "+index+" ")){
                    List<String> waypoints = new ArrayList<>(Arrays.asList(wayStr.split(" ")));
                    waypoints.remove(index);
                    String update = "";
                    for(String str : waypoints)
                        update += str + " ";
                    WaypointConfig.get("player").set(key + ".waypoints", update);
                }
            }
            WaypointConfig.save("player");
        }
        else
            event.setCancelled(true);
    }

    @EventHandler
    public static void teleportWaypoint(InventoryClickEvent event){
        if(event.isCancelled())
            return;
        if(event.getRawSlot()>event.getView().getTopInventory().getSize() ||
                event.getCurrentItem() == null)
            return;
        if(!event.getView().getTitle().equals("Waypoints") ||
                event.getInventory().getHolder()!=null)
            return;

        if(!event.getCurrentItem().getType().equals(Material.LIGHT_GRAY_STAINED_GLASS_PANE) &&
                !event.getCurrentItem().getType().equals(Material.LANTERN)) {
            Player player = (Player) event.getWhoClicked();
            if(TeleportWaypointCommands.checkPlayer(""+player.getUniqueId())){
                player.sendMessage("Try again shortly");
                event.setCancelled(true);
                return;
            }

            String name = event.getCurrentItem().getItemMeta().getDisplayName();
            String cord = event.getCurrentItem().getItemMeta().getLore().toString();
            String[] cords = cord.substring(3, cord.length() - 1).split(" ");

            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getView().close();
                }
            }.runTask(TeleportWaypoints.getInstance());

            Teleport.teleportPlayer(player, cords, TeleportWaypointCommands.getWaitlist(),
                    "Welcome to " + name,
                    player.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        }
        event.setCancelled(true);
    }

    @EventHandler
    public static void switchMenu(InventoryClickEvent event) {
        if(event.isCancelled())
            return;
        if(event.getRawSlot()>event.getView().getTopInventory().getSize() ||
                event.getCurrentItem() == null)
            return;
        if (!event.getView().getTitle().equals("Waypoints") ||
                event.getInventory().getHolder() != null)
            return;
        if (!event.getCurrentItem().getType().equals(Material.LANTERN))
            return;

        Player player = (Player) event.getWhoClicked();
        String waypointString = WaypointConfig.get("player").getString(player.getUniqueId()+
                ".waypoints");
        String[] waypointIndexes = WaypointConfig.get("player").getString(player.getUniqueId()+
                ".waypoints").split(" ");

        Inventory inv = Bukkit.createInventory(null,(waypointIndexes.length/9+1)*9,
                "Rearranging");
        for(String index : waypointIndexes){
            inv.addItem(Teleport.createShield(index, true));
        }
        ItemStack lantern = new ItemStack(Material.SOUL_LANTERN);
        ItemMeta lanternMeta = lantern.getItemMeta();
        lanternMeta.setDisplayName("§6Confirm");
        lantern.setItemMeta(lanternMeta);
        inv.setItem(inv.getSize()-1, lantern);
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        pane.setItemMeta(paneMeta);
        while(inv.firstEmpty()>=0){
            inv.setItem(inv.firstEmpty(), pane);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                event.getView().close();
                player.openInventory(inv);
            }
        }.runTask(TeleportWaypoints.getInstance());

        event.setCancelled(true);
    }

    @EventHandler
    public static void rearrangeWaypoint(InventoryClickEvent event) {
        if(event.isCancelled())
            return;
        if(event.getRawSlot()>event.getView().getTopInventory().getSize() ||
                event.getCurrentItem() == null)
            return;
        if (!event.getView().getTitle().equals("Rearranging") || event.getInventory().getHolder() != null)
            return;

        ItemStack item = event.getCurrentItem();
        if(item != null && item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().contains("§0")){
            int enchanted = -1;
            ListIterator<ItemStack> items = event.getView().getTopInventory().iterator();
            ItemStack curr = null;
            int max = event.getView().getTopInventory().getSize();
            for(int i=0; i<max; i++){
                curr = items.next();
                if(curr.getItemMeta().hasEnchants()) {
                    enchanted = i;
                    break;
                }
            }
            if(enchanted>=0){
                event.getInventory().setItem(enchanted, event.getCurrentItem());
                ItemMeta meta = curr.getItemMeta();
                meta.removeEnchant(Enchantment.LUCK);
                curr.setItemMeta(meta);
                event.getInventory().setItem(event.getSlot(), curr);
            }
            else{
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.LUCK, 1, false);
                item.setItemMeta(meta);
            }
        }
        else if (event.getCurrentItem().getType().equals(Material.SOUL_LANTERN)) {
            Inventory inv = event.getInventory();
            String indexes = "";
            Player player = (Player) event.getWhoClicked();
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i).getType().equals(Material.LIGHT_GRAY_STAINED_GLASS_PANE) ||
                        inv.getItem(i).getType().equals(Material.SOUL_LANTERN))
                    break;
                indexes += inv.getItem(i).getItemMeta().getDisplayName().substring(
                        inv.getItem(i).getItemMeta().getDisplayName().indexOf("§0") + 2) + " ";
            }
            WaypointConfig.get("player").set(player.getUniqueId() + ".waypoints",
                    indexes);
            WaypointConfig.save("player");

            String[] waypointIndexes = indexes.split(" ");
            Inventory newInv =
                    Bukkit.createInventory(null, (waypointIndexes.length / 9 + 1) * 9,
                    "Waypoints");
            for (String index : waypointIndexes) {
                newInv.addItem(Teleport.createShield(index, false));
            }
            ItemStack lantern = new ItemStack(Material.LANTERN);
            ItemMeta lanternMeta = lantern.getItemMeta();
            lanternMeta.setDisplayName("§6Rearrange");
            lantern.setItemMeta(lanternMeta);
            newInv.setItem(newInv.getSize() - 1, lantern);
            ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta paneMeta = pane.getItemMeta();
            paneMeta.setDisplayName(" ");
            pane.setItemMeta(paneMeta);
            while (newInv.firstEmpty() >= 0) {
                newInv.setItem(newInv.firstEmpty(), pane);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getView().close();
                    player.openInventory(newInv);
                }
            }.runTask(TeleportWaypoints.getInstance());
        }

        event.setCancelled(true);
    }

    @EventHandler
    public static void protectBannerExplode(EntityExplodeEvent event) {
        if(event.isCancelled())
            return;
        List<Block> blocks = event.blockList();
        for(Block block : blocks){
            int y = block.getY();
            if(!(block.getState() instanceof Banner) ||
                    block.getType().toString().contains("WALL")) {
                if (!(block.getWorld().getBlockAt(block.getX(),
                        y+1, block.getZ()).getState() instanceof Banner) ||
                        block.getType().toString().contains("WALL"))
                    continue;
                else
                    y++;
            }
            Location loc = new Location(block.getWorld(),
                    block.getX(), y, block.getZ());
            Banner banner = (Banner) block.getWorld().getBlockAt(loc).getState();

            String bannerData =
                    banner.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if(bannerData == null)
                continue;

            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public static void protectPistonPush(BlockPistonExtendEvent event){
        if(event.isCancelled())
            return;
        int x = 0;
        int z = 0;
        BlockFace face = event.getDirection();
        switch(face){
            case WEST:
                x = -1;
                break;
            case EAST:
                x = 1;
                break;
            case NORTH:
                z = -1;
                break;
            case SOUTH:
                z = 1;
                break;
            default:
                return;
        }
        Location loc = new Location(event.getBlock().getWorld(), event.getBlock().getX()+x,
                event.getBlock().getY()+1, event.getBlock().getZ()+z);
        if (!(event.getBlock().getWorld().getBlockAt(loc).getState() instanceof Banner) ||
                event.getBlock().getType().toString().contains("WALL")) {
            return;
        }
        Banner banner = (Banner) event.getBlock().getWorld().getBlockAt(loc).getState();

        String bannerData = banner.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if(bannerData == null)
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public static void protectPistonPull(BlockPistonRetractEvent event){
        if(event.isCancelled())
            return;
        if(!event.getBlock().getType().toString().contains("STICKY"))
            return;
        int x = 0;
        int y = 1;
        int z = 0;
        BlockFace face = event.getDirection();
        switch(face){
            case WEST:
                x = -1;
                break;
            case EAST:
                x = 1;
                break;
            case NORTH:
                z = -1;
                break;
            case SOUTH:
                z = 1;
                break;
            case UP:
                y = 3;
                break;
            default:
                return;
        }
        Location loc = new Location(event.getBlock().getWorld(), event.getBlock().getX()+x,
                event.getBlock().getY()+y, event.getBlock().getZ()+z);
        if (!(event.getBlock().getWorld().getBlockAt(loc).getState() instanceof Banner) ||
                event.getBlock().getType().toString().contains("WALL")) {
            return;
        }
        Banner banner = (Banner) event.getBlock().getWorld().getBlockAt(loc).getState();

        String bannerData = banner.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if(bannerData == null)
            return;

        event.setCancelled(true);
    }

    private static int count(String string, String regex){
        String temp = string;
        return (temp.length() - temp.replace(" ", "").length());
    }

    private static void smite(Banner banner, String index){
        String re = banner.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        String[] refund = re.substring(re.indexOf("@")+1).split(" ");
        for(int i=1; i<refund.length; i++){
            if(!refund[i].equals("fake")) {
                WaypointConfig.get("player").set(refund[i] + ".point", "1");
                System.out.println(refund[i]);
            }
        }
        WaypointConfig.get("waypoint").set(index, null);
        WaypointConfig.save("player");
        WaypointConfig.save("waypoint");
        banner.getPersistentDataContainer().remove(key);
        banner.update();

        if(!banner.getWorld().getNearbyEntities(banner.getLocation().clone().
                add(0.5, 1.6, 0.5), 0.1, 0.1, 0.1).isEmpty())
            (banner.getWorld().getNearbyEntities(banner.getLocation().clone().
                    add(0.5, 1.6, 0.5), 0.1, 0.1, 0.1).iterator().next()).remove();
    }

    @EventHandler
    public static void respawn(PlayerRespawnEvent event){
        if(event.isBedSpawn() || event.isAnchorSpawn())
            return;
        String worldName = event.getPlayer().getWorld().getName();
        if(worldName.contains("_"))
            worldName = worldName.substring(0, worldName.indexOf("_"));
        String cordStr = WaypointConfig.get("general").getString("spawn."+worldName);
        if (cordStr == null || cordStr.trim().isEmpty())
            return;
        String[] cords = cordStr.split(" ");
        event.setRespawnLocation(new Location(Bukkit.getWorld(cords[3]),
                Double.parseDouble(cords[0]) + .5, Double.parseDouble(cords[1]),
                Double.parseDouble(cords[2]) + .5));
    }
}
