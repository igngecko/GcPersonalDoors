package me.personaldoors;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class GcPersonalDoors extends JavaPlugin implements Listener {

    private NamespacedKey ownerKey;

    @Override
    public void onEnable() {
        ownerKey = new NamespacedKey(this, "owner");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PersonalDoors enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("personaldoors.give")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /personaldoor <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        ItemStack door = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta meta = door.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Personal Door");
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, target.getUniqueId().toString());
        door.setItemMeta(meta);

        target.getInventory().addItem(door);
        sender.sendMessage(ChatColor.GREEN + "Gave a personal door to " + target.getName());
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !block.getType().name().contains("DOOR")) return;

        Location loc = block.getLocation();
        String key = "pd-" + loc.getWorld().getName() + "-" + loc.getBlockX() + "-" + loc.getBlockY() + "-" + loc.getBlockZ();

        if (getConfig().contains(key)) {
            UUID owner = UUID.fromString(getConfig().getString(key));
            if (!event.getPlayer().getUniqueId().equals(owner) && !event.getPlayer().isOp()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "This door isn't yours.");
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getType().name().contains("DOOR")) return;

        Location loc = block.getLocation();
        String key = "pd-" + loc.getWorld().getName() + "-" + loc.getBlockX() + "-" + loc.getBlockY() + "-" + loc.getBlockZ();

        if (getConfig().contains(key)) {
            UUID owner = UUID.fromString(getConfig().getString(key));
            if (!event.getPlayer().getUniqueId().equals(owner) && !event.getPlayer().isOp()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You can't break someone else's personal door.");
            } else {
                getConfig().set(key, null); // remove from config
                saveConfig();
            }
        }
    }

    @EventHandler
    public void onPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                String ownerUUID = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                Location loc = event.getBlock().getLocation();
                String key = "pd-" + loc.getWorld().getName() + "-" + loc.getBlockX() + "-" + loc.getBlockY() + "-" + loc.getBlockZ();
                getConfig().set(key, ownerUUID);
                saveConfig();
                event.getPlayer().sendMessage(ChatColor.GREEN + "Placed a personal door.");
            }
        }
    }
}

