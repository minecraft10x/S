package nl.minecraft10x.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Pickaxe hakt een 10x10 gebied, Schep graaft een 10x10 gebied.
 */
public class AreaMiningListener implements Listener {

    private final Minecraft10xPlugin plugin;

    // Voorkomt oneindige loop
    private final Set<Block> processingBlocks = new HashSet<>();

    // Schop-materialen
    private static final Set<Material> SHOVEL_MATERIALS = new HashSet<>();
    // Houweel-materialen
    private static final Set<Material> PICKAXE_MATERIALS = new HashSet<>();

    static {
        // Schop blokken
        SHOVEL_MATERIALS.add(Material.DIRT);
        SHOVEL_MATERIALS.add(Material.GRASS_BLOCK);
        SHOVEL_MATERIALS.add(Material.SAND);
        SHOVEL_MATERIALS.add(Material.GRAVEL);
        SHOVEL_MATERIALS.add(Material.CLAY);
        SHOVEL_MATERIALS.add(Material.COARSE_DIRT);
        SHOVEL_MATERIALS.add(Material.PODZOL);
        SHOVEL_MATERIALS.add(Material.MYCELIUM);
        SHOVEL_MATERIALS.add(Material.SOUL_SAND);
        SHOVEL_MATERIALS.add(Material.SOUL_SOIL);
        SHOVEL_MATERIALS.add(Material.SNOW_BLOCK);
        SHOVEL_MATERIALS.add(Material.SNOW);

        // Houweel blokken
        PICKAXE_MATERIALS.add(Material.STONE);
        PICKAXE_MATERIALS.add(Material.COBBLESTONE);
        PICKAXE_MATERIALS.add(Material.GRANITE);
        PICKAXE_MATERIALS.add(Material.DIORITE);
        PICKAXE_MATERIALS.add(Material.ANDESITE);
        PICKAXE_MATERIALS.add(Material.IRON_ORE);
        PICKAXE_MATERIALS.add(Material.GOLD_ORE);
        PICKAXE_MATERIALS.add(Material.DIAMOND_ORE);
        PICKAXE_MATERIALS.add(Material.COAL_ORE);
        PICKAXE_MATERIALS.add(Material.DEEPSLATE);
        PICKAXE_MATERIALS.add(Material.DEEPSLATE_IRON_ORE);
        PICKAXE_MATERIALS.add(Material.DEEPSLATE_GOLD_ORE);
        PICKAXE_MATERIALS.add(Material.DEEPSLATE_DIAMOND_ORE);
        PICKAXE_MATERIALS.add(Material.DEEPSLATE_COAL_ORE);
        PICKAXE_MATERIALS.add(Material.NETHERRACK);
        PICKAXE_MATERIALS.add(Material.NETHER_QUARTZ_ORE);
        PICKAXE_MATERIALS.add(Material.NETHER_GOLD_ORE);
        PICKAXE_MATERIALS.add(Material.SANDSTONE);
        PICKAXE_MATERIALS.add(Material.OBSIDIAN);
        PICKAXE_MATERIALS.add(Material.TERRACOTTA);
    }

    public AreaMiningListener(Minecraft10xPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Voorkom oneindige loop
        if (processingBlocks.contains(brokenBlock)) return;

        boolean isPickaxe = isPickaxe(tool.getType());
        boolean isShovel = isShovel(tool.getType());

        if (!isPickaxe && !isShovel) return;

        // Controleer of het blok van het juiste type is
        Material blockMat = brokenBlock.getType();
        if (isPickaxe && !PICKAXE_MATERIALS.contains(blockMat)) return;
        if (isShovel && !SHOVEL_MATERIALS.contains(blockMat)) return;

        // Bepaal richtingsvectoren voor 10x10 gebied
        // We graven een 10x10 vlak loodrecht op de kijkrichting van de speler
        org.bukkit.util.Vector dir = player.getLocation().getDirection().normalize();

        int bx = brokenBlock.getX();
        int by = brokenBlock.getY();
        int bz = brokenBlock.getZ();

        // Detecteer of de speler meer omhoog/omlaag of horizontaal kijkt
        // Voor vereenvoudiging: 10x10 in een vlak bepaald door welke as het meest loodrecht staat
        double ax = Math.abs(dir.getX());
        double az = Math.abs(dir.getZ());

        // 10x10 area center is het geslagen blok
        // We maken een 10x10 raster (5 naar elke kant)
        processingBlocks.add(brokenBlock);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (int dx = -4; dx <= 5; dx++) {
                for (int dy = -4; dy <= 5; dy++) {
                    int tx, ty, tz;

                    if (ax < az) {
                        // Kijkt meer noord/zuid → vlak in X/Y richting
                        tx = bx + dx;
                        ty = by + dy;
                        tz = bz;
                    } else {
                        // Kijkt meer oost/west → vlak in Z/Y richting
                        tx = bx;
                        ty = by + dy;
                        tz = bz + dx;
                    }

                    Block target = brokenBlock.getWorld().getBlockAt(tx, ty, tz);
                    if (target.equals(brokenBlock)) continue;
                    if (target.getType() == Material.AIR) continue;
                    if (target.getType() == Material.BEDROCK) continue;

                    processingBlocks.add(target);

                    // Drop items x10
                    target.getDrops(tool).forEach(drop -> {
                        ItemStack multiplied = drop.clone();
                        int amount = Math.min(drop.getAmount() * 10, drop.getMaxStackSize() * 10);
                        multiplied.setAmount(amount);

                        int remaining = amount;
                        while (remaining > 0) {
                            ItemStack stack = multiplied.clone();
                            int give = Math.min(remaining, multiplied.getMaxStackSize());
                            stack.setAmount(give);
                            target.getWorld().dropItemNaturally(target.getLocation(), stack);
                            remaining -= give;
                        }
                    });

                    target.setType(Material.AIR);
                    processingBlocks.remove(target);
                }
            }

            processingBlocks.remove(brokenBlock);
        }, 1L);
    }

    private boolean isPickaxe(Material mat) {
        return mat == Material.WOODEN_PICKAXE || mat == Material.STONE_PICKAXE
                || mat == Material.IRON_PICKAXE || mat == Material.GOLDEN_PICKAXE
                || mat == Material.DIAMOND_PICKAXE || mat == Material.NETHERITE_PICKAXE;
    }

    private boolean isShovel(Material mat) {
        return mat == Material.WOODEN_SHOVEL || mat == Material.STONE_SHOVEL
                || mat == Material.IRON_SHOVEL || mat == Material.GOLDEN_SHOVEL
                || mat == Material.DIAMOND_SHOVEL || mat == Material.NETHERITE_SHOVEL;
    }
}
