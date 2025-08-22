package org.Emp.VoidForge;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class VoidForge extends JavaPlugin implements TabCompleter {

    private List<String> loadedVoidWorlds = new ArrayList<>();
    private FileConfiguration messages;

    @Override
    public void onEnable() {

        saveDefaultConfig();


        loadLanguageFile();

        getLogger().info(getMessage("plugin_enabled"));


        loadAllVoidWorlds();

        getCommand("createvoid").setExecutor(this);
        getCommand("gotovoid").setExecutor(this);
        getCommand("gotovoid").setTabCompleter(this);
        getCommand("renamevoid").setExecutor(this);
        getCommand("deletevoid").setExecutor(this);
        getCommand("deletevoid").setTabCompleter(this);
        getCommand("renamevoid").setTabCompleter(this);
        getCommand("listvoids").setExecutor(this);
        getCommand("loadvoid").setExecutor(this);
        getCommand("loadvoid").setTabCompleter(this);
        getCommand("voiddebug").setExecutor(this); // Debug komutu eklendi
    }

    @Override
    public void onDisable() {
        getLogger().info(getMessage("plugin_disabled"));

        // ⭐ All World Save
        for (String worldName : loadedVoidWorlds) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.save();
                getLogger().info("Dünya kaydedildi: " + worldName);
            }
        }


        loadedVoidWorlds.clear();
    }

    private void loadLanguageFile() {

        reloadConfig();
        String language = getConfig().getString("language", "en_US");
        File languageFile = new File(getDataFolder(), language + ".yml");


        if (!languageFile.exists()) {
            saveResource(language + ".yml", false);
        }


        messages = YamlConfiguration.loadConfiguration(languageFile);


        try {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(getResource("en_US.yml"), StandardCharsets.UTF_8));
            messages.setDefaults(defaultMessages);
        } catch (Exception e) {
            getLogger().warning("Failed to load default language file: " + e.getMessage());
        }
    }

    private String getMessage(String key) {
        return messages.getString(key, "Message not found: " + key);
    }

    private String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        return message;
    }

    private void loadAllVoidWorlds() {
        File serverDirectory = Bukkit.getWorldContainer();
        File[] worldDirectories = serverDirectory.listFiles(File::isDirectory);

        if (worldDirectories != null) {
            for (File worldDir : worldDirectories) {
                String worldName = worldDir.getName();


                File levelDat = new File(worldDir, "level.dat");
                if (levelDat.exists()) {

                    if (Bukkit.getWorld(worldName) == null) {
                        try {

                            WorldCreator creator = new WorldCreator(worldName);
                            creator.generator(new EmptyWorldGenerator());
                            creator.environment(World.Environment.NORMAL);
                            creator.type(WorldType.FLAT);
                            creator.generateStructures(false);
                            creator.generatorSettings("{\"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"minecraft:the_void\"}");

                            World world = Bukkit.createWorld(creator);
                            if (world != null) {

                                configureWorld(world);
                                loadedVoidWorlds.add(worldName);
                                getLogger().info(getMessage("world_loaded", "world", worldName));
                            }
                        } catch (Exception e) {
                            getLogger().log(Level.WARNING, getMessage("world_load_error", "world", worldName), e);

                            // Eğer dünya zaten varsa ama yüklenemiyorsa, listede işaretle
                            if (!loadedVoidWorlds.contains(worldName)) {
                                loadedVoidWorlds.add(worldName);
                            }
                        }
                    } else {

                        if (!loadedVoidWorlds.contains(worldName)) {
                            loadedVoidWorlds.add(worldName);
                        }
                    }
                }
            }
        }
        getLogger().info(getMessage("worlds_loaded", "count", String.valueOf(loadedVoidWorlds.size())));
    }

    private void configureWorld(World world) {
        world.setSpawnFlags(false, false);
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, false);
        world.setGameRule(org.bukkit.GameRule.DO_INSOMNIA, false);
        world.setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
        world.setGameRule(org.bukkit.GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(org.bukkit.GameRule.DO_TRADER_SPAWNING, false);
        world.setSpawnLocation(8, 66, 8);

        // ⭐ Auto Save
        world.setAutoSave(true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("gotovoid") ||
                command.getName().equalsIgnoreCase("deletevoid") ||
                command.getName().equalsIgnoreCase("renamevoid") ||
                command.getName().equalsIgnoreCase("loadvoid")) {
            if (args.length == 1) {
                // Tüm void dünya isimlerini tab completion'a ekle
                for (String worldName : loadedVoidWorlds) {
                    if (worldName.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(worldName);
                    }
                }
            } else if (command.getName().equalsIgnoreCase("renamevoid") && args.length == 2) {

                completions.add("<yeni-isim>");
                completions.add("<new-name>");
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("createvoid")) {
            return handleCreateVoid(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("gotovoid")) {
            return handleGotoVoid(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("renamevoid")) {
            return handleRenameVoid(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("deletevoid")) {
            return handleDeleteVoid(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("listvoids")) {
            return handleListVoids(sender);
        }
        else if (command.getName().equalsIgnoreCase("loadvoid")) {
            return handleLoadVoidWorld(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("voiddebug")) {
            return handleVoidDebug(sender); // Debug komutu eklendi
        }
        return false;
    }

    // ⭐ DEBUG
    private boolean handleVoidDebug(CommandSender sender) {
        sender.sendMessage(getMessage("commands.voiddebug.header"));
        sender.sendMessage(getMessage("commands.voiddebug.loaded_worlds", "count", String.valueOf(loadedVoidWorlds.size())));

        for (String worldName : loadedVoidWorlds) {
            World world = Bukkit.getWorld(worldName);
            File worldDir = new File(Bukkit.getWorldContainer(), worldName);
            File levelDat = new File(worldDir, "level.dat");

            if (world != null && levelDat.exists()) {
                sender.sendMessage(getMessage("commands.voiddebug.world_entry", "world", worldName));
            } else if (levelDat.exists()) {
                sender.sendMessage(getMessage("commands.voiddebug.world_entry_unloaded", "world", worldName));
            } else {
                sender.sendMessage(getMessage("commands.voiddebug.world_no_file", "world", worldName));
            }
        }
        return true;
    }

    private boolean handleListVoids(CommandSender sender) {
        if (loadedVoidWorlds.isEmpty()) {
            sender.sendMessage(getMessage("commands.listvoids.no_worlds"));
        } else {
            sender.sendMessage(getMessage("commands.listvoids.header"));
            for (String worldName : loadedVoidWorlds) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    int playerCount = world.getPlayers().size();
                    sender.sendMessage(getMessage("commands.listvoids.world_entry", "world", worldName, "players", String.valueOf(playerCount)));
                } else {
                    sender.sendMessage(getMessage("commands.listvoids.world_unloaded", "world", worldName));
                }
            }
        }
        return true;
    }

    private boolean handleCreateVoid(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(getMessage("commands.createvoid.usage"));
            return true;
        }

        String worldName = args[0];


        if (!isValidWorldName(worldName)) {
            sender.sendMessage(getMessage("commands.createvoid.invalid_name"));
            return true;
        }


        if (loadedVoidWorlds.contains(worldName) || worldExistsInFileSystem(worldName)) {
            sender.sendMessage(getMessage("commands.createvoid.exists"));
            return true;
        }

        try {

            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new EmptyWorldGenerator());
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generatorSettings("{\"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"minecraft:the_void\"}");

            World world = getServer().createWorld(creator);

            if (world != null) {

                configureWorld(world);

                // ⭐ World Fast Save
                world.save();
                getLogger().info("Dünya kaydedildi: " + worldName);


                loadedVoidWorlds.add(worldName);

                sender.sendMessage(getMessage("commands.createvoid.success", "world", worldName));
                sender.sendMessage(getMessage("commands.createvoid.teleported"));


                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.teleport(world.getSpawnLocation());
                }
            } else {
                sender.sendMessage(getMessage("commands.createvoid.failed"));
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Dünya oluşturulurken hata oluştu: " + worldName, e);
            sender.sendMessage("§cDünya oluşturulurken beklenmeyen bir hata oluştu!");
        }

        return true;
    }


    private boolean worldExistsInFileSystem(String worldName) {
        File worldDir = new File(getServer().getWorldContainer(), worldName);
        File levelDat = new File(worldDir, "level.dat");
        return levelDat.exists();
    }

    private boolean handleGotoVoid(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("commands.gotovoid.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(getMessage("commands.gotovoid.usage"));
            player.sendMessage(getMessage("available_worlds", "worlds", getWorldList()));
            return true;
        }

        String worldName = args[0];


        if (!loadedVoidWorlds.contains(worldName)) {
            player.sendMessage(getMessage("commands.gotovoid.not_found"));
            player.sendMessage(getMessage("available_worlds", "worlds", getWorldList()));
            return true;
        }

        World world = getServer().getWorld(worldName);


        if (world == null) {
            try {
                WorldCreator creator = new WorldCreator(worldName);
                creator.generator(new EmptyWorldGenerator());
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                creator.generatorSettings("{\"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"minecraft:the_void\"}");

                world = getServer().createWorld(creator);
                if (world != null) {
                    configureWorld(world);
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, getMessage("world_load_error", "world", worldName), e);
            }
        }

        if (world == null) {
            player.sendMessage(getMessage("commands.gotovoid.load_failed"));
            return true;
        }


        player.teleport(world.getSpawnLocation());
        player.sendMessage(getMessage("commands.gotovoid.teleported", "world", worldName));

        return true;
    }

    private boolean handleRenameVoid(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getMessage("commands.renamevoid.usage"));
            sender.sendMessage(getMessage("available_worlds", "worlds", getWorldList()));
            return true;
        }

        String oldName = args[0];
        String newName = args[1];


        if (!isValidWorldName(newName)) {
            sender.sendMessage(getMessage("commands.createvoid.invalid_name"));
            return true;
        }


        if (!loadedVoidWorlds.contains(oldName)) {
            sender.sendMessage(getMessage("commands.renamevoid.old_not_found", "world", oldName));
            sender.sendMessage(getMessage("available_worlds", "worlds", getWorldList()));
            return true;
        }


        if (loadedVoidWorlds.contains(newName)) {
            sender.sendMessage(getMessage("commands.renamevoid.new_exists", "world", newName));
            return true;
        }

        World world = getServer().getWorld(oldName);
        if (world == null) {

            File worldDir = new File(getServer().getWorldContainer(), oldName);
            if (!worldDir.exists()) {
                sender.sendMessage(getMessage("commands.renamevoid.old_not_found", "world", oldName));
                return true;
            }
        }


        World defaultWorld = getServer().getWorlds().get(0);
        if (world != null && world.equals(defaultWorld)) {
            sender.sendMessage(getMessage("commands.renamevoid.main_world"));
            return true;
        }


        if (world != null) {
            for (Player player : world.getPlayers()) {
                player.teleport(defaultWorld.getSpawnLocation());
                player.sendMessage(getMessage("commands.renamevoid.teleport_players"));
            }


            if (!getServer().unloadWorld(world, true)) {
                sender.sendMessage(getMessage("commands.renamevoid.unload_failed"));
                return true;
            }
        }


        File oldDir = new File(getServer().getWorldContainer(), oldName);
        File newDir = new File(getServer().getWorldContainer(), newName);

        if (oldDir.renameTo(newDir)) {

            loadedVoidWorlds.remove(oldName);
            loadedVoidWorlds.add(newName);


            try {
                WorldCreator creator = new WorldCreator(newName);
                creator.generator(new EmptyWorldGenerator());
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                creator.generatorSettings("{\"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"minecraft:the_void\"}");

                World newWorld = getServer().createWorld(creator);
                if (newWorld != null) {
                    configureWorld(newWorld);
                    sender.sendMessage(getMessage("commands.renamevoid.success", "old", oldName, "new", newName));
                }
            } catch (Exception e) {
                sender.sendMessage(getMessage("commands.renamevoid.load_error"));
                getLogger().log(Level.WARNING, getMessage("world_load_error", "world", newName), e);
            }
        } else {
            sender.sendMessage(getMessage("commands.renamevoid.rename_failed"));

            if (oldDir.exists()) {
                WorldCreator creator = new WorldCreator(oldName);
                creator.generator(new EmptyWorldGenerator());
                getServer().createWorld(creator);
                loadedVoidWorlds.add(oldName);
            }
        }

        return true;
    }

    private boolean handleDeleteVoid(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(getMessage("commands.deletevoid.usage"));
            sender.sendMessage(getMessage("available_worlds", "worlds", getWorldList()));
            return true;
        }

        String worldName = args[0];


        if (!loadedVoidWorlds.contains(worldName)) {
            sender.sendMessage(getMessage("commands.deletevoid.not_found", "world", worldName));
            sender.sendMessage(getMessage("available_worlds", "worlds", getWorldList()));
            return true;
        }

        World world = getServer().getWorld(worldName);


        World defaultWorld = getServer().getWorlds().get(0);
        if (world != null && world.equals(defaultWorld)) {
            sender.sendMessage(getMessage("commands.deletevoid.main_world"));
            return true;
        }


        if (world != null) {
            for (Player player : world.getPlayers()) {
                player.teleport(defaultWorld.getSpawnLocation());
                player.sendMessage(getMessage("commands.deletevoid.teleport_players"));
            }


            if (!getServer().unloadWorld(world, false)) {
                sender.sendMessage(getMessage("commands.deletevoid.unload_failed"));
                return true;
            }
        }


        File worldDir = new File(getServer().getWorldContainer(), worldName);
        if (deleteDirectory(worldDir)) {

            loadedVoidWorlds.remove(worldName);
            sender.sendMessage(getMessage("commands.deletevoid.success", "world", worldName));
        } else {
            sender.sendMessage(getMessage("commands.deletevoid.delete_failed"));
        }

        return true;
    }


    private boolean handleLoadVoidWorld(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cKullanım: /loadvoid <dünya-ismi>");
            return true;
        }

        String worldName = args[0];

        if (loadedVoidWorlds.contains(worldName)) {
            sender.sendMessage("§aBu dünya zaten yüklü!");
            return true;
        }

        File worldDir = new File(getServer().getWorldContainer(), worldName);
        File levelDat = new File(worldDir, "level.dat");

        if (!levelDat.exists()) {
            sender.sendMessage("§cBu isimde bir dünya bulunamadı!");
            return true;
        }

        try {
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new EmptyWorldGenerator());
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generatorSettings("{\"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"minecraft:the_void\"}");

            World world = getServer().createWorld(creator);
            if (world != null) {
                configureWorld(world);
                loadedVoidWorlds.add(worldName);
                sender.sendMessage("§aDünya başarıyla yüklendi: §e" + worldName);
            } else {
                sender.sendMessage("§cDünya yüklenemedi!");
            }
        } catch (Exception e) {
            sender.sendMessage("§cDünya yüklenirken hata oluştu: " + e.getMessage());
        }

        return true;
    }

    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    private String getWorldList() {
        if (loadedVoidWorlds.isEmpty()) {
            return getMessage("no_worlds_available");
        }
        List<String> worldNames = new ArrayList<>();
        for (String worldName : loadedVoidWorlds) {
            worldNames.add("§e" + worldName + "§f");
        }
        return String.join(", ", worldNames);
    }

    // Geçerli dünya ismi kontrol metodu
    private boolean isValidWorldName(String name) {
        return name.matches("^[a-zA-Z0-9_-]+$");
    }
}