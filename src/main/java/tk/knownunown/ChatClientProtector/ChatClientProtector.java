package tk.knownunown.ChatClientProtector;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ChatClientProtector extends JavaPlugin implements Listener {

    private static HashMap<Player, Location> victims = new HashMap<Player, Location>();

    private static Location location;
    private static Integer delay;
    private static ArrayList<Player> confirmedNotVictims = new ArrayList<Player>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        World world = getServer().getWorld(getConfig().getString("world"));
        List<Double> coords = getConfig().getDoubleList("location");

        location = new Location(world, coords.get(0), coords.get(1), coords.get(2));
        delay = getConfig().getInt("delay");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("chatonly")){
            if(!(sender instanceof Player)) {
                sender.sendMessage("This command is only available in-game.");
                return false;
            }

            if(isVictim((Player) sender)){
                sender.sendMessage("You're already in chat only mode!");
                return false;
            }
            
            if (confirmedNotVictims.contains((Player) sender)) {
                sender.sendMessage("You have confirmed not being in chat only capacity, denied.");
                return false;
            }
            
            sender.sendMessage(getConfig().getString("message"));
            sender.sendMessage("Teleportation will commence in " + delay + " seconds.");

            final Player player = (Player) sender;

            new BukkitRunnable() {
                @Override
                public void run() {
                    ChatClientProtector.addVictim(player);
                }
            }.runTaskLater(this, delay * 20); // seconds -> ticks (hopefully)
            getLogger().info(((Player) sender).getDisplayName() + " turned on Chat Only Mode.");
            return true;
        }
        return false;
    }

    public static void addVictim(Player player) {
        Location current = player.getLocation();

        player.teleport(location);
        victims.put(player, current);
    }

    public static void removeVictim(Player player) {
        player.teleport(victims.get(player));
        victims.remove(player);
    }

    public static boolean isVictim(Player player) {
        if (confirmedNotVictims.contains(Player)) {
            return false;
        }
        
        return victims.containsKey(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event){
        if(isVictim(event.getPlayer())){
            event.getPlayer().teleport(location);
            confirmedNotVictims.add(event.getPlayer());
        
            removeVictim(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event){
        onCommand(event.getPlayer(), "chatonly");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event){
        if(isVictim(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player){
            if(isVictim((Player) event.getEntity())){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamageOther(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Player){
            if(isVictim((Player) event.getDamager())){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        if(victims.containsKey(event.getPlayer())) {
            removeVictim(event.getPlayer());
            confirmedNotVictims.remove(event.getPlayer());
        }
    }

    @Override
    public void onDisable() {
        for (Player victim : victims.keySet()){
            removeVictim(victim);
            confirmedNotVictims.add(event.getPlayer());
        }
    }
}
