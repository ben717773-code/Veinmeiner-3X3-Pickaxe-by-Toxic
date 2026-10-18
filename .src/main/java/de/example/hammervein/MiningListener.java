package de.example.hammervein;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class MiningListener implements Listener {

    private final HammerVeinPlugin plugin;
    private final Set<UUID> busy = new HashSet<>();

    public MiningListener(HammerVeinPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (busy.contains(player.getUniqueId())) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;

        Block origin = event.getBlock();
        boolean requireSneak = plugin.getConfig().getBoolean("require-sneak", true);
        boolean sneaking = player.isSneaking();

        List<Block> targets;
        if (isOre(origin.getType())
                && plugin.isVeinEnabled(player.getUniqueId())
                && (sneaking || !requireSneak)) {
            targets = findVein(origin);
        } else if (plugin.isHammer(tool) && !sneaking) {
            targets = findArea(player, origin);
        } else {
            return;
        }

        busy.add(player.getUniqueId());
        try {
            for (Block block : targets) {
                ItemStack current = player.getInventory().getItemInMainHand();
                if (current.getType() == Material.AIR) break;
                if (!canBreak(block, current)) continue;
                player.breakBlock(block);
            }
        } finally {
            busy.remove(player.getUniqueId());
        }
    }

    private boolean canBreak(Block block, ItemStack tool) {
        Material type = block.getType();
        if (type.isAir() || block.isLiquid()) return false;
        if (type.getHardness() < 0) return false;
        return block.isPreferredTool(tool);
    }

    private boolean isOre(Material material) {
        return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    private List<Block> findVein(Block origin) {
        int max = Math.max(1, plugin.getConfig().getInt("max-vein-blocks", 64));
        Material type = origin.getType();
        List<Block> result = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() < max) {
            Block current = queue.poll();
            if (!current.equals(origin)) result.add(current);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block next = current.getRelative(x, y, z);
                        if (next.getType() == type && visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<Block> findArea(Player player, Block origin) {
        BlockFace face = BlockFace.UP;
        RayTraceResult ray = player.rayTraceBlocks(6.0);
        if (ray != null && ray.getHitBlockFace() != null) {
            face = ray.getHitBlockFace();
        }

        List<Block> result = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                Block block = switch (face) {
                    case UP, DOWN -> origin.getRelative(a, 0, b);
                    case NORTH, SOUTH -> origin.getRelative(a, b, 0);
                    default -> origin.getRelative(0, a, b);
                };
                result.add(block);
            }
        }
        return result;
    }
}
