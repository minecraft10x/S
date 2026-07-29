package nl.minecraft10x.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

/**
 * Vermenigvuldigt XP en overige events x10.
 */
public class XpMultiplierListener implements Listener {

    private final Minecraft10xPlugin plugin;

    public XpMultiplierListener(Minecraft10xPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        // Alleen positieve XP x10 (voorkom negatieve loop)
        if (event.getAmount() > 0) {
            event.setAmount(event.getAmount() * 10);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        // Smelt output x10
        if (event.getResult() != null) {
            org.bukkit.inventory.ItemStack result = event.getResult().clone();
            result.setAmount(Math.min(result.getAmount() * 10, result.getMaxStackSize()));
            event.setResult(result);
        }
    }
}
