package nl.minecraft10x.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * XXL Pickaxe en XXL Shovel — alleen deze doen 10x10 mining.
 * Gecraften via de XXL Crafting Table (of normaal 3x3 met diamond tools).
 */
public class XxlTools {

    public static final String XXL_PICKAXE_NAME = "§bXXL Pickaxe";
    public static final String XXL_SHOVEL_NAME  = "§bXXL Shovel";

    public static ItemStack createXxlPickaxe() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(XXL_PICKAXE_NAME);
        meta.setLore(Arrays.asList(
                "§7Hakt een §610x10§7 gebied!",
                "§eCraft via de XXL Crafting Table."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createXxlShovel() {
        ItemStack item = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(XXL_SHOVEL_NAME);
        meta.setLore(Arrays.asList(
                "§7Graaft een §610x10§7 gebied!",
                "§eCraft via de XXL Crafting Table."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isXxlPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE) return false;
        if (!item.hasItemMeta()) return false;
        return XXL_PICKAXE_NAME.equals(item.getItemMeta().getDisplayName());
    }

    public static boolean isXxlShovel(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SHOVEL) return false;
        if (!item.hasItemMeta()) return false;
        return XXL_SHOVEL_NAME.equals(item.getItemMeta().getDisplayName());
    }

    /**
     * Registreer recepten: 9x diamond pickaxe in XXL table = 1 XXL Pickaxe
     * (wordt aangeroepen vanuit XxlCraftingListener)
     */
    public static void registerRecipes(Minecraft10xPlugin plugin) {
        // XXL Pickaxe: 3x3 vol diamond pickaxes
        NamespacedKey pickKey = new NamespacedKey(plugin, "xxl_pickaxe");
        ShapedRecipe pickRecipe = new ShapedRecipe(pickKey, createXxlPickaxe());
        pickRecipe.shape("PPP", "PPP", "PPP");
        pickRecipe.setIngredient('P', Material.DIAMOND_PICKAXE);
        plugin.getServer().addRecipe(pickRecipe);

        // XXL Shovel: 3x3 vol diamond shovels
        NamespacedKey shovelKey = new NamespacedKey(plugin, "xxl_shovel");
        ShapedRecipe shovelRecipe = new ShapedRecipe(shovelKey, createXxlShovel());
        shovelRecipe.shape("SSS", "SSS", "SSS");
        shovelRecipe.setIngredient('S', Material.DIAMOND_SHOVEL);
        plugin.getServer().addRecipe(shovelRecipe);
    }
}
