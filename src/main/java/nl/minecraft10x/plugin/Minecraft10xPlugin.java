package nl.minecraft10x.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Minecraft10xPlugin extends JavaPlugin {

    private static Minecraft10xPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Minecraft10x plugin gestart!");

        // Register listeners
        getServer().getPluginManager().registerEvents(new BlockDropListener(this), this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new AreaMiningListener(this), this);
        XxlCraftingGui xxlGui = new XxlCraftingGui();
        getServer().getPluginManager().registerEvents(new XxlCraftingListener(this), this);
        getServer().getPluginManager().registerEvents(xxlGui, this);
        getServer().getPluginManager().registerEvents(new XpMultiplierListener(this), this);

        // Register commands
        getCommand("xxlcraft").setExecutor(new XxlCraftCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("Minecraft10x plugin gestopt.");
    }

    public static Minecraft10xPlugin getInstance() {
        return instance;
    }
}
