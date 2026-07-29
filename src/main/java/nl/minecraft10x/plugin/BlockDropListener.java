package nl.minecraft10x.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * Vermenigvuldigt alle blok drops met 10.
 */
public class BlockDropListener implements Listener {

    private final Minecraft10xPlugin plugin;

    public BlockDropListener(Minecraft10xPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Haal de default drops op
        Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand());

        if (drops.isEmpty()) return;

        // Annuleer default drops zodat we zelf kunnen droppen
        event.setDropItems(false);

        // Drop elke stack x10
        for (ItemStack drop : drops) {
            ItemStack multiplied = drop.clone();
            int newAmount = Math.min(drop.getAmount() * 10, drop.getMaxStackSize() * 10);
            multiplied.setAmount(newAmount);

            // Drop in meerdere stacks als het de max overschrijdt
            int remaining = newAmount;
            while (remaining > 0) {
                ItemStack stack = multiplied.clone();
                int give = Math.min(remaining, multiplied.getMaxStackSize());
                stack.setAmount(give);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
                remaining -= give;
            }
        }

        // XP ook x10
        event.setExpToDrop(event.getExpToDrop() * 10);
    }
}
