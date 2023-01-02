package de.vendettagroup.rightclickharvest;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;

import com.gmail.nossr50.api.ExperienceAPI;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class RightClickHarvest implements Listener {

    @EventHandler
    public void rightClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || b == null ||
                !checkBlock(b.getType()) || !checkForAxe(b.getType(), p) ||
                !(b.getBlockData() instanceof Ageable ageable)) {
            return;
        }
        int actualAge = ageable.getAge();
        if (actualAge != ageable.getMaximumAge()) {
            return;
        }
        harvest(b, p);
        if (checkForMcMMO()) {
            getLogger().info("checkForMcMMO positive");
            ExperienceAPI.addRawXP(p, "Herbalism", 50, "UNKNOWN");
        }
    }

    private void harvest(Block b, Player p) {
        Material setToBlock = b.getType();
        p.swingMainHand();
        changeOutputAndBreak(b, p);
        b.setType(setToBlock);
        changeItemDurability(b.getType(), p);
        changeCocaDirection(b);
        playSound(p, b.getType());
    }

    private boolean checkBlock(Material m) {
        return switch (m) {
            case WHEAT, POTATOES, CARROTS, BEETROOTS, NETHER_WART, COCOA -> true;
            default -> false;
        };
    }

    private boolean isNotCocoa(Material m) {
        return m != Material.COCOA;
    }

    private boolean checkForAxe(Material m, Player p) {
        if (isNotCocoa(m)) {
            return true;
        }
        return switch (p.getInventory().getItemInMainHand().getType()) {
            case NETHERITE_AXE, DIAMOND_AXE, GOLDEN_AXE, IRON_AXE, STONE_AXE, WOODEN_AXE -> true;
            default -> false;
        };
    }

    private void changeItemDurability(Material m, Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || isNotCocoa(m)) {
            return;
        }
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null || !item.getItemMeta().isUnbreakable() ||
                !(item.getItemMeta() instanceof Damageable itemdmg)) {
            return;
        }
        itemdmg.setDamage(itemdmg.getDamage() + 1);
        item.setItemMeta(item.getItemMeta());
    }

    private boolean checkForJungleLog(Material m) {
        return switch (m) {
            case JUNGLE_LOG, STRIPPED_JUNGLE_LOG -> true;
            default -> false;
        };
    }

    //Normally CoCpaBeansFace North so i ask for that first
    private void changeCocaDirection(Block b) {
        if (isNotCocoa(b.getType())) {
            return;
        }
        BlockData blockData = b.getBlockData();
        Location cocoaBean = b.getLocation();
        var facings = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

        for (var facing : facings) {
            var testLocation = cocoaBean.clone();
            testLocation.setX(testLocation.getX() + facing.getModX());
            testLocation.setZ(testLocation.getZ() + facing.getModZ());
            if (checkForJungleLog(testLocation.getBlock().getType()) &&
                    blockData instanceof Directional directional) {
                directional.setFacing(facing);
                b.setBlockData(blockData);
                return;
            }
        }
    }

    private Material getSeed(Material m) {
        return switch (m) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            default -> Material.AIR;
        };
    }

    private void playSound(Player p, Material m) {
        var isNetherWart = m.equals(Material.NETHER_WART);
        p.playSound(p.getLocation(),
                isNetherWart ? Sound.BLOCK_NETHER_WART_BREAK : Sound.BLOCK_CROP_BREAK, 10, 1);
        p.playSound(p.getLocation(),
                isNetherWart ? Sound.ITEM_NETHER_WART_PLANT : Sound.ITEM_CROP_PLANT, 8, 1);
    }

    private void changeOutputAndBreak(Block b, Player p) {
        Location location = b.getLocation();
        for (var item : b.getDrops(p.getInventory().getItemInMainHand())) {
            if (item.getType() == getSeed(b.getType())) {
                item.setAmount(item.getAmount() - 1);
            }
            if (location.getWorld() != null && item.getAmount() != 0) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }

    private boolean checkForMcMMO() {
        Plugin mcmmo = getServer().getPluginManager().getPlugin("mcMMo");
        return mcmmo != null && mcmmo.isEnabled();
    }
}
