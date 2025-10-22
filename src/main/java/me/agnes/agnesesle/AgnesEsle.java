package me.agnes.agnesesle;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.PaperCommandManager;
import com.bentahsin.benthpapimanager.BenthPAPIManager;
import com.sun.crypto.provider.HmacSHA1KeyGenerator;
import me.agnes.agnesesle.commands.EsleCommandACF;
import me.agnes.agnesesle.discord.DiscordBot;
import me.agnes.agnesesle.data.EslestirmeManager;
import me.agnes.agnesesle.listener.PlayerLoginListener;
import me.agnes.agnesesle.placeholders.PlayerPlaceholders;
import me.agnes.agnesesle.placeholders.ServerPlaceholders;
import me.agnes.agnesesle.util.LuckPermsUtil;
import me.agnes.agnesesle.util.MessageUtil;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AgnesEsle extends JavaPlugin {

    private static AgnesEsle instance;
    private DiscordBot discordBot;
    private LuckPerms luckPerms;
    private BenthPAPIManager papiMgr;
    private LuckPermsUtil luckPermsUtil;

    // ------------- BURAYA EKLENDİ -----------------
    private File rewardsDataFile;
    private FileConfiguration rewardsDataConfig;
    // ---------------------------------------------

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        try {
            this.luckPerms = LuckPermsProvider.get();
            getLogger().info("[AgnHesapEsle] LuckPerms API başarıyla yüklendi.");
            this.luckPermsUtil = new LuckPermsUtil(this.luckPerms, getLogger());
        } catch (IllegalStateException e) {
            getLogger().warning("[AgnHesapEsle] LuckPerms API yüklenemedi! Plugin düzgün çalışmayabilir.");
            getLogger().severe(e.getMessage());
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

        commandManager.registerCommand(new EsleCommandACF(this));

        EslestirmeManager.init();

        getServer().getPluginManager().registerEvents(new PlayerLoginListener(discordBot), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                this.papiMgr = BenthPAPIManager.create(this)
                        .withInjectable(DiscordBot.class, this.discordBot)
                        .withInjectable(LuckPerms.class, this.luckPerms)
                        .withDebugMode()
                        .register(
                                PlayerPlaceholders.class,
                                ServerPlaceholders.class
                        );
                getLogger().info("PlaceholderAPI desteği başarıyla etkinleştirildi.");
            } catch (Exception e) {
                getLogger().severe("BenthPAPIManager başlatılırken bir hata oluştu!");
                getLogger().warning(e.getMessage());
            }
        } else {
            getLogger().warning("PlaceholderAPI bulunamadı, placeholder'lar yüklenemedi.");
        }

        createRewardsDataFile();
        getLogger().info("Data Dosyaları Yüklendi!");

        getLogger().info("[AgnHesapEsle] Plugin başarıyla yüklendi!");
    }

    @Override
    public void onDisable() {
        if (this.papiMgr != null) {
            this.papiMgr.unregisterAll();
        }
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

    public LuckPermsUtil getLuckPermsUtil() {
        return luckPermsUtil;
    }

    public void handleRewardCheck(String discordId) {
        // ...
    }



    private void createRewardsDataFile() {
        rewardsDataFile = new File(getDataFolder(), "rewards-data.yml");
        if (!rewardsDataFile.exists()) {
            rewardsDataFile.getParentFile().mkdirs();
            saveResource("rewards-data.yml", false);
        }
        rewardsDataConfig = YamlConfiguration.loadConfiguration(rewardsDataFile);
    }

    public FileConfiguration getRewardsDataConfig() {
        return rewardsDataConfig;
    }

    public void saveRewardsDataConfig() {
        try {
            rewardsDataConfig.save(rewardsDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ödül kontrolü ve verisi
     * @param playerUUID Ödül kontrolü yapılacak oyuncunun UUID'si
     * @return true eğer ödül verildiyse, false ise henüz verilmemiş veya zaman dolmamış
     */
    public void handleRewardCheck(UUID playerUUID, String discordId, InteractionHook hook) {
        Bukkit.getScheduler().runTask(AgnesEsle.getInstance(), () -> {
            if (playerUUID == null) {
                hook.sendMessage("⚠️ Minecraft hesabınız eşlenmemiş!").setEphemeral(true).queue();
                return;
            }

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                hook.sendMessage("⚠️ Minecraft oyuncunuz şu anda çevrimiçi değil. Ödül verilemiyor.").setEphemeral(true).queue();
                return;
            }

            FileConfiguration rewardsData = AgnesEsle.getInstance().getRewardsDataConfig();
            long lastClaim = rewardsData.getLong(playerUUID.toString() + ".lastClaim", 0);
            long cooldown = AgnesEsle.getInstance().getConfig().getLong("reward-cooldown", 86400000L); // 24 saat default

            long now = System.currentTimeMillis();
            if (now - lastClaim < cooldown) {
                long remainingMillis = cooldown - (now - lastClaim);

                long remainingSeconds = remainingMillis / 1000 % 60;
                long remainingMinutes = (remainingMillis / (1000 * 60)) % 60;
                long remainingHours = (remainingMillis / (1000 * 60 * 60));

                String timeLeft = String.format("%02d saat %02d dakika %02d saniye", remainingHours, remainingMinutes, remainingSeconds);

                hook.sendMessage("⏳ Ödül almak için lütfen " + timeLeft + " bekleyin!").setEphemeral(true).queue();
                return;
            }

            List<String> rewardCommands = AgnesEsle.getInstance().getConfig().getStringList("daily-rewards");
            for (String cmd : rewardCommands) {
                String command = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }

            rewardsData.set(playerUUID.toString() + ".lastClaim", now);
            AgnesEsle.getInstance().saveRewardsDataConfig();

            hook.sendMessage("🎉 Günlük ödülünüz başarıyla teslim edildi!").setEphemeral(true).queue();
        });
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
