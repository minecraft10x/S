package nl.minecraft10x.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Spawnt 10x zoveel mobs en geeft 10x meer drops bij dood.
 */
public class MobSpawnListener implements Listener {

    private final Minecraft10xPlugin plugin;

    public MobSpawnListener(Minecraft10xPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Alleen bij natuurlijke spawns, niet bij plugin spawns (voorkomt oneindige loop)
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) return;

        Location loc = event.getLocation();
        EntityType type = event.getEntityType();

        // Spawn 9 extra (samen met de originele = 10)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 9; i++) {
                loc.getWorld().spawnEntity(loc, type);
            }
        }, 1L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Vermenigvuldig mob drops x10
        List<ItemStack> drops = event.getDrops();
        for (ItemStack drop : drops) {
            drop.setAmount(Math.min(drop.getAmount() * 10, drop.getMaxStackSize()));
        }

        // XP ook x10
        event.setDroppedExp(event.getDroppedExp() * 10);
    }
}
