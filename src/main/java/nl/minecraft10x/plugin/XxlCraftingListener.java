package nl.minecraft10x.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Afhandeling van de XXL Crafting Table:
 *  - Crafting recept: 3x3 vol crafting tables → XXL Crafting Table
 *  - Rechts-klikken op een XXL Crafting Table opent de 9x10 GUI
 */
public class XxlCraftingListener implements Listener {

    private final Minecraft10xPlugin plugin;
    public static final String XXL_TABLE_NAME = "§6XXL Crafting Table";

    public XxlCraftingListener(Minecraft10xPlugin plugin) {
        this.plugin = plugin;
        registerRecipe();
    }

    private void registerRecipe() {
        ItemStack xxlTable = createXxlTable();
        NamespacedKey key = new NamespacedKey(plugin, "xxl_crafting_table");

        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, xxlTable);
        recipe.shape("CCC", "CCC", "CCC");
        recipe.setIngredient('C', Material.CRAFTING_TABLE);

        plugin.getServer().addRecipe(recipe);
    }

    public static ItemStack createXxlTable() {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(XXL_TABLE_NAME);
        meta.setLore(Arrays.asList(
                "§7Craft items met 10x meer slots!",
                "§7Elke output is §6x10§7.",
                "§eRechts-klik om te openen."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isXxlTable(ItemStack item) {
        if (item == null || item.getType() != Material.CRAFTING_TABLE) return false;
        if (!item.hasItemMeta()) return false;
        return XXL_TABLE_NAME.equals(item.getItemMeta().getDisplayName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        // Controleer of de speler een XXL Table in de hand heeft
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (!isXxlTable(inHand)) {
            // Controleer ook of er op een geplaatste XXL Table geklikt wordt (via block)
            if (event.getClickedBlock() == null) return;
            // Blokken worden standaard geplaatst; we detecteren via GUI opening bij crafting table block
            return;
        }

        event.setCancelled(true);
        XxlCraftingGui.openGui(player, plugin);
    }
}
