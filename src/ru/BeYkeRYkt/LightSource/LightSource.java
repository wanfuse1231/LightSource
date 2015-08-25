package ru.BeYkeRYkt.LightSource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import ru.BeYkeRYkt.LightAPI.LightAPI;
import ru.BeYkeRYkt.LightAPI.LightRegistry;
import ru.BeYkeRYkt.LightSource.gui.GUIManager;
import ru.BeYkeRYkt.LightSource.gui.Menu;
import ru.BeYkeRYkt.LightSource.gui.editor.EditorManager;
import ru.BeYkeRYkt.LightSource.items.ItemManager;
import ru.BeYkeRYkt.LightSource.sources.Source;
import ru.BeYkeRYkt.LightSource.sources.SourceManager;
import ru.BeYkeRYkt.LightSource.task.TaskManager;

public class LightSource extends JavaPlugin {

	private static LightSource plugin;
	private LSConfig db;
	private ItemManager manager;
	private GUIManager gui;
	private SourceManager source;
	public CommandSender BUKKIT_SENDER = Bukkit.getConsoleSender();
	private EditorManager editor;
	private TaskManager task;
	private LightRegistry registry;

	@Override
	public void onEnable() {
		LightSource.plugin = this;
		PluginDescriptionFile pdfFile = getDescription();
		try {
			FileConfiguration fc = getConfig();
			if (!new File(getDataFolder(), "config.yml").exists()) {
				fc.options().header("LightSource v" + pdfFile.getVersion() + " Configuration" + "\nHave fun :3" + "\nby BeYkeRYkt" + "\nUpdate modes can be: SAVE, MAXIMUM and USER" + "\nTask modes can be: ONE_FOR_ALL and ONE_FOR_ONE");
				fc.addDefault("LightUpdateMode", "USER");
				fc.addDefault("TaskMode", "ONE_FOR_ALL");

				fc.addDefault("PlayerLight", true);
				fc.addDefault("EntityLight", false);
				fc.addDefault("ItemLight", false);
				fc.addDefault("BurnLight", false);
				fc.addDefault("LightSourceDamage", true);
				fc.addDefault("Ignore-save-update-light", false);

				fc.addDefault("Task-delay-ticks", 10);
				fc.addDefault("max-iterations-per-tick", 10);
				fc.addDefault("Damage-fire-ticks-sec", 5);

				List<World> worlds = getServer().getWorlds();
				for (World world : worlds) {
					fc.addDefault("Worlds." + world.getName(), true);
				}
				fc.options().copyDefaults(true);
				saveConfig();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.db = new LSConfig(this);
		manager = new ItemManager();
		source = new SourceManager();
		task = new TaskManager();
		gui = new GUIManager();
		editor = new EditorManager();

		manager.loadItems();
		editor.init();
		task.init();
		gui.load();
		registry = LightAPI.getRegistry(this);
		registry.startAutoSend(getDB().getTaskTicks());
		Bukkit.getPluginManager().registerEvents(new LightListener(), this);

		createExampleItems();

		// mcstats
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit the stats :-(
		}
	}

	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		getEditorManager().save();
		ItemManager.getList().clear();
		HandlerList.unregisterAll(this);
		int index;
		for (index = getSourceManager().getSourceList().size() - 1; index >= 0; --index) {
			Source light = getSourceManager().getSourceList().get(index);
			getLightRegistry().deleteLight(light.getLocation());
			getSourceManager().removeSource(light);
		}
		getDB().save();
		db = null;
	}

	public static LightSource getInstance() {
		return plugin;
	}

	public LSConfig getDB() {
		return db;
	}

	public ItemManager getItemManager() {
		return manager;
	}

	public GUIManager getGUIManager() {
		return gui;
	}

	public SourceManager getSourceManager() {
		return source;
	}

	public void log(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.AQUA + "<LightSource>: " + ChatColor.WHITE + message);
	}

	public EditorManager getEditorManager() {
		return editor;
	}

	public TaskManager getTaskManager() {
		return task;
	}

	public LightRegistry getLightRegistry() {
		return registry;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (cmd.getName().equalsIgnoreCase("ls")) {
				if (player.hasPermission("ls.admin") || player.isOp()) {
					getGUIManager().openMenu(player, getGUIManager().getMenuFromId("mainMenu"));
				} else {
					log(player, "Nope :)");
				}
			} else if (cmd.getName().equalsIgnoreCase("light")) {
				if (player.hasPermission("ls.lightcreator") || player.isOp()) {
					if (args.length == 0) {
						Menu menu = getGUIManager().getMenuFromId("lc_mainMenu");
						getGUIManager().openMenu(player, menu);
					} else if (args.length == 1) {
						if (args[0].equalsIgnoreCase("create")) {
							log(player, ChatColor.RED + "Need more arguments!");
							log(player, ChatColor.RED + "/light create [level 1-15]");
						} else if (args[0].equalsIgnoreCase("delete")) {
							getLightRegistry().deleteLight(player.getLocation());
							log(player, ChatColor.GREEN + "Light successfully deleted!");
						} else {
							Menu menu = getGUIManager().getMenuFromId("lc_mainMenu");
							getGUIManager().openMenu(player, menu);
						}
					} else if (args.length == 2) {
						if (args[0].equalsIgnoreCase("create")) {
							int lightlevel = Integer.parseInt(args[1]);
							if (lightlevel <= 15) {
								getLightRegistry().deleteLight(player.getLocation());
								player.getLocation().getChunk().unload(true);
								player.getLocation().getChunk().load(true);
								log(player, ChatColor.GREEN + "Light successfully created!");
							} else {
								log(player, ChatColor.RED + "Maximum 15 level!");
								log(player, ChatColor.RED + "/light create [level 1-15]");
							}
						} else if (args[0].equalsIgnoreCase("delete")) {
							getLightRegistry().deleteLight(player.getLocation());
							log(player, ChatColor.GREEN + "Light successfully deleted!");
						} else {
							Menu menu = getGUIManager().getMenuFromId("lc_mainMenu");
							getGUIManager().openMenu(player, menu);
						}
					} else {
						Menu menu = getGUIManager().getMenuFromId("lc_mainMenu");
						getGUIManager().openMenu(player, menu);
					}
				} else {
					log(player, "Nope :)");
				}
			}
		}
		return true;
	}

	public void createExampleItems() {
		try {
			FileConfiguration fc = getItemManager().getConfig();

			if (!new File(getDataFolder(), "Items.yml").exists()) {

				fc.addDefault("Lava.material", "LAVA");
				fc.addDefault("Lava.lightlevel", 15);

				fc.addDefault("StationLava.material", "STATIONARY_LAVA");
				fc.addDefault("StationLava.lightlevel", 15);

				fc.addDefault("Fire.material", "FIRE");
				fc.addDefault("Fire.lightlevel", 115);

				fc.addDefault("Jack.material", "JACK_O_LANTERN");
				fc.addDefault("Jack.lightlevel", 15);

				fc.addDefault("LavaBucket.material", "LAVA_BUCKET");
				fc.addDefault("LavaBucket.lightlevel", 15);

				fc.addDefault("Torch.material", "TORCH");
				fc.addDefault("Torch.lightlevel", 14);

				fc.addDefault("Glowstone.material", "GLOWSTONE");
				fc.addDefault("Glowstone.lightlevel", 14);

				fc.addDefault("BlazeRod.material", "BLAZE_ROD");
				fc.addDefault("BlazeRod.lightlevel", 5);

				fc.addDefault("Redstone.material", "REDSTONE_TORCH_ON");
				fc.addDefault("Redstone.lightlevel", 9);

				// fc.addDefault("SeaLatern.material", "SEA_LANTERN");
				// fc.addDefault("SeaLatern.lightlevel", 15);
				// fc.addDefault("SeaLatern.burnTime", -1); - error for 1.7.10

				fc.addDefault("RedstoneLamp.material", "REDSTONE_LAMP_ON");
				fc.addDefault("RedstoneLamp.lightlevel", 15);

				fc.addDefault("Furnace.material", "BURNING_FURNACE");
				fc.addDefault("Furnace.lightlevel", 13);

				fc.addDefault("RedstoneOre.material", "REDSTONE_ORE");
				fc.addDefault("RedstoneOre.lightlevel", 9);

				fc.addDefault("EnderChest.material", "ENDER_CHEST");
				fc.addDefault("EnderChest.lightlevel", 6);

				fc.options().copyDefaults(true);
				getItemManager().saveConfig();
				fc.options().copyDefaults(false);
				getItemManager().reloadConfig();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
