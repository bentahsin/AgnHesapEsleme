package me.agnes.agnesesle;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.PaperCommandManager;
import me.agnes.agnesesle.commands.EsleCommandACF;
import me.agnes.agnesesle.discord.DiscordBot;
import me.agnes.agnesesle.data.EslestirmeManager;
import me.agnes.agnesesle.util.MessageUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.stream.Collectors;

public class AgnesEsle extends JavaPlugin {

    private static AgnesEsle instance;
    private DiscordBot discordBot;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        try {
            this.luckPerms = LuckPermsProvider.get();
            getLogger().info("[AgnHesapEsle] LuckPerms API başarıyla yüklendi.");
        } catch (IllegalStateException e) {
            getLogger().warning("[AgnHesapEsle] LuckPerms API yüklenemedi! Plugin düzgün çalışmayabilir.");
            this.luckPerms = null;
        }

        discordBot = new DiscordBot(getConfig().getString("token"));
        discordBot.start();

        MessageUtil.load();
        MessageUtil.setLang(getConfig().getString("lang", "tr"));

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandContexts().registerContext(BukkitCommandIssuer.class, c -> {
            return commandManager.getCommandIssuer(c.getSender());
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("@players", c -> {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        });

        commandManager.registerCommand(new EsleCommandACF());

        EslestirmeManager.init();

        getServer().getPluginManager().registerEvents(new me.agnes.agnesesle.listener.PlayerLoginListener(), this);

        getLogger().info("[AgnHesapEsle] Plugin başarıyla yüklendi!");
    }

    @Override
    public void onDisable() {
        if (discordBot != null) discordBot.shutdown();
        getLogger().info("[AgnHesapEsle] Plugin kapatıldı!");
    }

    public static AgnesEsle getInstance() {
        return instance;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public void odulVer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            getLogger().info("Ödül veriliyor: " + player.getName());
            player.sendMessage(MessageUtil.getMessage("reward-message"));

            for (String cmd : getConfig().getStringList("oduller")) {
                String command = cmd.replace("%player%", player.getName());
                getLogger().info("Komut çalıştırılıyor: " + command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }
}
