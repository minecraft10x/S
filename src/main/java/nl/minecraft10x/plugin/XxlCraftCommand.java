package nl.minecraft10x.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XxlCraftCommand implements CommandExecutor {

    private final Minecraft10xPlugin plugin;

    public XxlCraftCommand(Minecraft10xPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Alleen spelers kunnen dit commando gebruiken!");
            return true;
        }

        Player player = (Player) sender;
        XxlCraftingGui.openGui(player, plugin);
        player.sendMessage("§6[10x] §eXXL Crafting Table geopend!");
        return true;
    }
}
