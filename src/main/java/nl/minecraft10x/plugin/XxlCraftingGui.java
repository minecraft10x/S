package nl.minecraft10x.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * XXL Crafting Table GUI
 *
 * Layout (6 rijen × 9 kolommen = 54 slots):
 *
 *  Rij 0: [I][I][I][I][I][I][I][I][I]   ← input slots 0-8   (kolom 0-8)
 *  Rij 1: [I][I][I][I][I][I][I][I][I]   ← input slots 9-17  (kolom 0-8)
 *  Rij 2: [I][I][I][I][I][I][I][I][I]   ← input slots 18-26 (kolom 0-8)
 *  Rij 3: [I][I][I][I][I][I][I][I][I]   ← input slots 27-35 (kolom 0-8)
 *  Rij 4: [I][I][I][I][I][I][I][I][I]   ← input slots 36-44 (kolom 0-8)
 *  Rij 5: [G][G][G][G][O][G][G][G][G]   ← output midden (slot 49), grijs rondom
 *
 * 9x9 input grid = 81 items nodig in een normaal 3x3 recept van 9 items → je hebt 9x zoveel nodig.
 * Output is 10x de normale output.
 *
 * We simuleren een 9x9 grid door te mappen naar een 3x3 grid:
 *  - Input wordt 9 blokken gelezen (3x3 van de XXL grid)
 *  - Maar elke "cel" in het normale 3x3 recept correspondeert met een 3x3 blok in onze 9x9
 *  - Je hebt dus 9x zoveel van elk ingredient nodig
 */
public class XxlCraftingGui implements Listener {

    private static final Set<UUID> openGuiPlayers = new HashSet<>();
    private static final Map<UUID, Inventory> playerInventories = new HashMap<>();

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9; // 54 slots
    private static final int OUTPUT_SLOT = 49;

    // Input slots: rij 0-4, alle kolommen = slots 0..44
    private static final int INPUT_COUNT = 45; // 5 rijen × 9 = 45 slots
    // We mappen dit naar een 9x9 grid (maar Bukkit kan max 3x3 crafting)

    public static void openGui(Player player, Minecraft10xPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE, "§6XXL Crafting Table");

        // Vul rij 5 met grijs glas behalve output slot
        fillRow5(inv);

        player.openInventory(inv);
        openGuiPlayers.add(player.getUniqueId());
        playerInventories.put(player.getUniqueId(), inv);
    }

    private static void fillRow5(Inventory inv) {
        ItemStack gray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = gray.getItemMeta();
        meta.setDisplayName("§8");
        gray.setItemMeta(meta);

        for (int col = 0; col < 9; col++) {
            int slot = 45 + col;
            if (slot == OUTPUT_SLOT) {
                inv.setItem(slot, null); // output slot leeg laten
            } else {
                inv.setItem(slot, gray);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!openGuiPlayers.contains(player.getUniqueId())) return;
        Inventory inv = event.getInventory();
        if (!inv.equals(playerInventories.get(player.getUniqueId()))) return;

        int slot = event.getRawSlot();

        // Klik op grijs glas = cancel
        if (slot >= 45 && slot < 54 && slot != OUTPUT_SLOT) {
            event.setCancelled(true);
            return;
        }

        // Klik op output slot
        if (slot == OUTPUT_SLOT) {
            event.setCancelled(true);
            ItemStack output = inv.getItem(OUTPUT_SLOT);
            if (output == null || output.getType() == Material.AIR) return;

            // Geef output aan speler
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(output.clone());
            leftover.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));

            // Verwijder ingrediënten (9x zoveel als normaal)
            consumeIngredients(inv);
            inv.setItem(OUTPUT_SLOT, null);
            // Herbereken output
            updateOutput(inv, player);
            return;
        }

        // Na een tick, herbereken output
        Bukkit.getScheduler().runTaskLater(Minecraft10xPlugin.getInstance(), () -> {
            updateOutput(inv, player);
        }, 1L);
    }

    /**
     * Bereken output op basis van de items in de 9x9 grid.
     * We mappen elke 3x3 cel van de 9x9 naar één ingredient in een standaard 3x3 recept.
     * Elke cel in het normale recept = 9 items in onze grid (3x3 blok van 9 slots).
     */
    private static void updateOutput(Inventory inv, Player player) {
        // Lees de 9x9 grid (slots 0-44 = 5 rijen, maar we gebruiken alleen 0-44)
        // Map naar 3x3 grid: elke cel is een 3x3 blok van 3 rijen en 3 kolommen
        // XXL grid: 9 kolommen, 5 rijen → we nemen een 9x9 (de bovenste 45 slots als 5×9)
        // We mappen dit naar een 3x3 door de grid in 3 horizontale banden en 3 verticale banden te splitsen

        // Simpelere aanpak: pak de eerste 9 unieke items die voorkomen in slots 0-44
        // en match tegen Bukkit recepten

        // Bouw een 3x3 matrix van de XXL input
        // Elke "super-cel" (3 cols × 3 rows in onze 9×5 grid) moet hetzelfde item bevatten
        // Super-cel mapping voor 9 kolommen in 3 groepen: (0,1,2), (3,4,5), (6,7,8)
        // Super-cel mapping voor 5 rijen in ... we nemen de eerste 3 rijen als rij 0,1,2 van recept
        // en de onderste 2 rijen + rij 3 worden ook gebruikt

        // Eenvoudig: we mappen 9 "zones" elk 5 slots (5 rijen x 9 kolommen / 9 zones)
        // Maar dat klopt niet. We gebruiken de simpelste aanpak:
        // De 9x9 = slots 0..80, maar we hebben maar 45 slots (5 rijen).
        // We behandelen het als: 3x3 recept, elke ingredient-cel = 5 slots (of meer).

        // PRAKTISCHE AANPAK: We lezen gewoon de grid als 3 rijen × 3 kolommen
        // waarbij elke rij en kolom van de 9x9 wordt gegroepeerd in 3:
        // Kolommen: 0-2 = links, 3-5 = midden, 6-8 = rechts
        // Rijen: 0-1 = boven, 2-3 = midden, 4 = onder (of 0-1, 2-3, 4-? voor 3 rijen)
        // Maar we hebben 5 rijen → splitsen in 3 groepen: rij 0, rij 1-2, rij 3-4

        // EENVOUDIGE DEFINITIEVE AANPAK:
        // - Slots 0..44 = 5 rijen × 9 kolommen = onze 9×5 grid
        // - We mappen naar 3×3 (9 cellen):
        //   Rij-band 0 = rijen 0-1, Rij-band 1 = rijen 2-3, Rij-band 2 = rij 4
        //   Kolom-band 0 = kolommen 0-2, Kolom-band 1 = kolommen 3-5, Kolom-band 2 = kolommen 6-8
        // - Elke cel in het recept moet hetzelfde item bevatten als alle slots in die band

        ItemStack[] craftGrid = new ItemStack[9];
        boolean validGrid = true;

        for (int cellRow = 0; cellRow < 3; cellRow++) {
            for (int cellCol = 0; cellCol < 3; cellCol++) {
                int cell = cellRow * 3 + cellCol;

                // Rij-bereik
                int rowStart, rowEnd;
                if (cellRow == 0) { rowStart = 0; rowEnd = 1; }
                else if (cellRow == 1) { rowStart = 2; rowEnd = 3; }
                else { rowStart = 4; rowEnd = 4; }

                // Kolom-bereik
                int colStart = cellCol * 3;
                int colEnd = colStart + 2;

                // Verzamel alle items in deze cel-zone
                ItemStack representative = null;
                for (int r = rowStart; r <= rowEnd; r++) {
                    for (int c = colStart; c <= colEnd; c++) {
                        int slotIndex = r * 9 + c;
                        if (slotIndex >= INPUT_COUNT) continue;
                        ItemStack item = inv.getItem(slotIndex);
                        if (item != null && item.getType() != Material.AIR) {
                            if (representative == null) {
                                representative = item;
                            }
                            // Alle items in deze zone moeten hetzelfde type zijn
                            if (item.getType() != representative.getType()) {
                                validGrid = false;
                                break;
                            }
                        } else {
                            // Als er een leeg slot is maar ook gevulde, is het ongelijk
                            if (representative != null) {
                                // Leeg slot in dezelfde zone als gevuld slot — dat mag
                                // (want niet elke zone hoeft vol te zijn)
                            }
                        }
                    }
                    if (!validGrid) break;
                }
                craftGrid[cell] = representative;
            }
        }

        if (!validGrid) {
            inv.setItem(OUTPUT_SLOT, null);
            return;
        }

        // Zoek een passend recept in Bukkit
        ItemStack result = findMatchingRecipe(craftGrid, player);

        if (result != null) {
            // x10 de output
            ItemStack output = result.clone();
            output.setAmount(Math.min(result.getAmount() * 10, result.getMaxStackSize()));
            inv.setItem(OUTPUT_SLOT, output);
        } else {
            inv.setItem(OUTPUT_SLOT, null);
        }
    }

    private static ItemStack findMatchingRecipe(ItemStack[] grid, Player player) {
        Iterator<Recipe> iter = Bukkit.recipeIterator();
        while (iter.hasNext()) {
            Recipe recipe = iter.next();

            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shaped = (ShapedRecipe) recipe;
                if (matchesShaped(grid, shaped)) {
                    return recipe.getResult();
                }
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
                if (matchesShapeless(grid, shapeless)) {
                    return recipe.getResult();
                }
            }
        }
        return null;
    }

    private static boolean matchesShaped(ItemStack[] grid, ShapedRecipe recipe) {
        // Probeer alle rotaties / verschuivingen
        String[] shape = recipe.getShape();
        Map<Character, org.bukkit.inventory.RecipeChoice> choiceMap = recipe.getChoiceMap();
        Map<Character, Material> ingredientMap = new HashMap<>();
        for (Map.Entry<Character, org.bukkit.inventory.RecipeChoice> entry : choiceMap.entrySet()) {
            if (entry.getValue() instanceof org.bukkit.inventory.RecipeChoice.MaterialChoice) {
                org.bukkit.inventory.RecipeChoice.MaterialChoice mc =
                        (org.bukkit.inventory.RecipeChoice.MaterialChoice) entry.getValue();
                if (!mc.getChoices().isEmpty()) {
                    ingredientMap.put(entry.getKey(), mc.getChoices().get(0));
                }
            }
        }

        int recipeRows = shape.length;
        int recipeCols = shape[0].length();

        // Probeer alle offsets in een 3×3 grid
        for (int rowOffset = 0; rowOffset <= 3 - recipeRows; rowOffset++) {
            for (int colOffset = 0; colOffset <= 3 - recipeCols; colOffset++) {
                if (matchesShapedAt(grid, shape, ingredientMap, rowOffset, colOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesShapedAt(ItemStack[] grid, String[] shape,
                                            Map<Character, Material> ingredientMap,
                                            int rowOffset, int colOffset) {
        // Bouw verwacht 3x3 grid
        Material[] expected = new Material[9];
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length(); c++) {
                char ch = shape[r].charAt(c);
                Material mat = ch == ' ' ? Material.AIR : ingredientMap.getOrDefault(ch, Material.AIR);
                expected[(r + rowOffset) * 3 + (c + colOffset)] = mat;
            }
        }

        // Vergelijk met onze grid
        for (int i = 0; i < 9; i++) {
            Material expectedMat = expected[i] != null ? expected[i] : Material.AIR;
            Material actualMat = (grid[i] != null) ? grid[i].getType() : Material.AIR;
            if (expectedMat != actualMat) return false;
        }
        return true;
    }

    private static boolean matchesShapeless(ItemStack[] grid, ShapelessRecipe recipe) {
        List<Material> required = new ArrayList<>();
        for (org.bukkit.inventory.RecipeChoice choice : recipe.getChoiceList()) {
            if (choice instanceof org.bukkit.inventory.RecipeChoice.MaterialChoice) {
                org.bukkit.inventory.RecipeChoice.MaterialChoice mc =
                        (org.bukkit.inventory.RecipeChoice.MaterialChoice) choice;
                if (!mc.getChoices().isEmpty()) {
                    required.add(mc.getChoices().get(0));
                }
            }
        }

        List<Material> available = new ArrayList<>();
        for (ItemStack item : grid) {
            if (item != null && item.getType() != Material.AIR) {
                available.add(item.getType());
            }
        }

        if (available.size() != required.size()) return false;

        List<Material> availableCopy = new ArrayList<>(available);
        for (Material mat : required) {
            if (!availableCopy.remove(mat)) return false;
        }
        return true;
    }

    /**
     * Verwijder de ingrediënten. Elke ingrediënt-cel verbruikt 9x zo veel items
     * (want je hebt een 9x9 zone gevuld in plaats van 1 slot).
     */
    private static void consumeIngredients(Inventory inv) {
        // Verwijder items per zone (zelfde logica als updateOutput)
        for (int cellRow = 0; cellRow < 3; cellRow++) {
            for (int cellCol = 0; cellCol < 3; cellCol++) {
                int rowStart, rowEnd;
                if (cellRow == 0) { rowStart = 0; rowEnd = 1; }
                else if (cellRow == 1) { rowStart = 2; rowEnd = 3; }
                else { rowStart = 4; rowEnd = 4; }

                int colStart = cellCol * 3;
                int colEnd = colStart + 2;

                for (int r = rowStart; r <= rowEnd; r++) {
                    for (int c = colStart; c <= colEnd; c++) {
                        int slotIndex = r * 9 + c;
                        if (slotIndex >= INPUT_COUNT) continue;
                        ItemStack item = inv.getItem(slotIndex);
                        if (item != null && item.getType() != Material.AIR) {
                            if (item.getAmount() > 1) {
                                item.setAmount(item.getAmount() - 1);
                            } else {
                                inv.setItem(slotIndex, null);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (!openGuiPlayers.contains(player.getUniqueId())) return;
        openGuiPlayers.remove(player.getUniqueId());

        Inventory inv = playerInventories.remove(player.getUniqueId());
        if (inv == null) return;

        // Geef items terug aan speler
        for (int i = 0; i < INPUT_COUNT; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                leftover.values().forEach(leftItem ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftItem));
            }
        }
    }
}
