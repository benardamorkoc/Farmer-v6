package xyz.geik.farmer;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;
import xyz.geik.farmer.api.FarmerAPI;
import xyz.geik.farmer.api.managers.FarmerManager;
import xyz.geik.farmer.commands.FarmerCommand;
import xyz.geik.farmer.configuration.ConfigFile;
import xyz.geik.farmer.configuration.LangFile;
import xyz.geik.farmer.database.MySQL;
import xyz.geik.farmer.database.SQL;
import xyz.geik.farmer.database.SQLite;
import xyz.geik.farmer.helpers.*;
import xyz.geik.farmer.integrations.Integrations;
import xyz.geik.farmer.listeners.ListenerRegister;
import xyz.geik.farmer.modules.FarmerModule;
import xyz.geik.farmer.shades.storage.Config;
import xyz.geik.glib.GLib;
import xyz.geik.glib.chat.ChatUtils;
import xyz.geik.glib.database.Database;
import xyz.geik.glib.database.DatabaseType;
import xyz.geik.glib.economy.Economy;
import xyz.geik.glib.economy.EconomyAPI;
import xyz.geik.glib.module.ModuleManager;
import xyz.geik.glib.shades.okaeri.configs.ConfigManager;
import xyz.geik.glib.shades.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import xyz.geik.glib.shades.triumphteam.cmd.bukkit.BukkitCommandManager;
import xyz.geik.glib.shades.triumphteam.cmd.bukkit.message.BukkitMessageKey;
import xyz.geik.glib.shades.triumphteam.cmd.core.message.MessageKey;
import xyz.geik.glib.simplixstorage.SimplixStorageAPI;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class of farmer
 * There is only loads, apis and
 * startup task codes.
 */
@Getter
public class Main extends JavaPlugin {

    /**
     * Listener list of modules
     */
    public Map<FarmerModule, Listener> listenerList = new HashMap<>();

    @Getter @Setter
    private SimplixStorageAPI simplixStorageAPI;

    @Getter
    private ModuleManager moduleManager;

    @Getter @Setter
    private static Database database;

    @Getter
    private static SQL sql = null;

    @Getter @Setter
    private static Economy economy;

    /**
     * Instance of this class
     */
    @Getter
    private static Main instance;

    @Getter
    private static MorePaperLib morePaperLib;

    /**
     * Config files which using SimplixStorage API for it.
     * Also, you can find usage code of API on helpers#StorageAPI
     */
    @Getter
    private static Config itemsFile, levelFile;

    @Getter
    private static LangFile langFile;

    @Getter
    private static ConfigFile configFile;

    /**
     * Main integration of plugin integrations#Integrations
     */
    @Getter
    @Setter
    private static Integrations integration;

    /**
     * CommandManager
     */
    @Getter
    private static BukkitCommandManager<CommandSender> commandManager;


    /**
     * Loading files before enable
     */
    public void onLoad() {
        instance = this;
        simplixStorageAPI = new SimplixStorageAPI(this);
        setupFiles();
        setupDatabase();
    }

    /**
     * onEnable method calls from spigot api.
     * This is sort of the main(String... args) method.
     */
    public void onEnable() {
        morePaperLib = new MorePaperLib(this);
        FarmerAPI.getFarmerManager();
        Integrations.registerIntegrations();
        new GLib(this, getLangFile().getMessages().getPrefix());
        // API Installer
        PlaceholderHelper.initialize();
        CacheLoader.loadAllItems();
        CacheLoader.loadAllLevels();
        registerEconomy();
        setupCommands();
        sendEnableMessage();
        getSql().loadAllFarmers();
        new ListenerRegister();
        loadMetrics();
        registerModules();
    }

    /**
     * disable method calls from spigot api.
     * executing it right before close.
     * async tasks can be fail because server
     * can't handle async tasks while shutting down
     */
    public void onDisable() {
        getSql().updateAllFarmers();
        CommandHelper.unregisterCommands();
        ModuleHelper moduleHelper = ModuleHelper.getInstance();
        moduleHelper.unloadModules();
    }

    /**
     * Setups config, lang and modules file file
     */
    public void setupFiles() {
        try {
            configFile = ConfigManager.create(ConfigFile.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(getDataFolder(), "config.yml"));
                it.saveDefaults();
                it.load(true);
            });
            String langName = configFile.getSettings().getLang();
            Class langClass = Class.forName("xyz.geik.farmer.configuration.lang." + langName);
            Class<LangFile> languageClass = langClass;
            this.langFile = ConfigManager.create(languageClass, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(getDataFolder() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
            itemsFile = getSimplixStorageAPI().initConfig("items");
            levelFile = getSimplixStorageAPI().initConfig("levels");
            WorldHelper.loadAllowedWorlds();
        } catch (Exception exception) {
            getPluginLoader().disablePlugin(this);
            throw new RuntimeException("Error loading configuration file");
        }
    }

    /**
     * Setups database
     */
    private void setupDatabase() {
        DatabaseType type = DatabaseType.getDatabaseType(getConfigFile().getDatabase().getDatabaseType());
        if (type.equals(DatabaseType.SQLite))
            this.sql = new SQLite();
        else
            this.sql = new MySQL();
    }

    /**
     * Registers economy
     */
    private void registerEconomy() {
        Main.economy = new EconomyAPI(this, getConfigFile().getSettings().getEconomy()).getEconomy();
    }

    /**
     * Register modules to this plugin
     */
    private void registerModules() {
        this.moduleManager = new ModuleManager();
        //getModuleManager().enableModules();
        //FarmerModule.calculateModulesUseGui();

        ModuleHelper helper = ModuleHelper.getInstance();
        helper.loadModules();
        FarmerModule.calculateModulesUseGui();
    }

    /**
     * Sends enable message to console.
     */
    private static void sendEnableMessage() {
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&6&l		FARMER 		&b"));
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&aDeveloped by &2Geik"));
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&aContributors &2" + Arrays.toString(Main.getInstance().getDescription().getAuthors().toArray())));
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&aDiscord: &2https://discord.geik.xyz"));
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&aWeb: &2https://geik.xyz"));
    }

    /**
     * Custom charted metrics loader
     */
    private void loadMetrics() {
        Metrics metrics = new Metrics(Main.instance, 9646);
        metrics.addCustomChart(new Metrics.SingleLineChart("ciftci_sayisi", () -> FarmerManager.getFarmers().size()));
        metrics.addCustomChart(new Metrics.SimplePie("api_eklentisi", () -> {
            String[] data = getIntegration().getClass().getName().split(".");
            return data[data.length-1];
        }));
    }

    /**
     * Setups commands
     */
    private void setupCommands() {
        commandManager = BukkitCommandManager.create(this);
        commandManager.registerCommand(new FarmerCommand());
        commandManager.registerMessage(MessageKey.INVALID_ARGUMENT, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getInvalidArgument()));
        commandManager.registerMessage(MessageKey.UNKNOWN_COMMAND, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getUnknownCommand()));
        commandManager.registerMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getNotEnoughArguments()));
        commandManager.registerMessage(MessageKey.TOO_MANY_ARGUMENTS, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getTooManyArguments()));
        commandManager.registerMessage(BukkitMessageKey.NO_PERMISSION, (sender, invalidArgumentContext) ->
                ChatUtils.sendMessage(sender, getLangFile().getMessages().getNoPerm()));
    }

    /**
     * Constructor of class
     */
    public Main() {}
}
