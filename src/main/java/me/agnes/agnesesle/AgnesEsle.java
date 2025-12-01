package me.agnes.agnesesle;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.PaperCommandManager;
import com.bentahsin.benthpapimanager.BenthPAPIManager;
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
import org.bukkit.command.ConsoleCommandSender;
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

    // ------------- Günlük Ödül Sistemi Data -----------------
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
            if (this.luckPerms != null) {
                this.luckPermsUtil = new LuckPermsUtil(this.luckPerms, getLogger());
            }
        } catch (IllegalStateException e) {
            getLogger().warning("[AgnHesapEsle] LuckPerms API bulunamadı! Plugin rütbe özellikleri olmadan çalışacak.");
            getLogger().severe(e.getMessage());
            this.luckPerms = null;
            this.luckPermsUtil = null;
        }

        String token = getConfig().getString("token");

        if (token == null || token.isEmpty() || token.equals("DISCORD_BOT_TOKEN")) {
            getLogger().severe("---------------------------------------------------");
            getLogger().severe("HATA: Discord Bot Tokeni girilmemiş!");
            getLogger().severe("Lütfen config.yml dosyasını düzenleyin ve sunucuyu yeniden başlatın.");
            getLogger().severe("Plugin devre dışı bırakılıyor...");
            getLogger().severe("---------------------------------------------------");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.discordBot = new DiscordBot(token);
        this.discordBot.start();

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
            try {
                this.papiMgr.unregisterAll();
            } catch (Exception ignored) { }
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

    private void createRewardsDataFile() {
        rewardsDataFile = new File(getDataFolder(), "rewards-data.yml");
        if (!rewardsDataFile.exists()) {
            boolean ignored = rewardsDataFile.getParentFile().mkdirs();
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
            getLogger().severe(e.getMessage());
        }
    }

    /**
     * Oyuncunun günlük ödülünü kontrol eder ve verir.
     * Sonuç Discord üzerinden InteractionHook ile bildirilir.
     *
     * @param playerUUID Ödül kontrolü yapılacak oyuncunun UUID'si
     * @param hook       Discord etkileşim kancası (Cevap vermek için)
     */
    public void handleRewardCheck(UUID playerUUID, InteractionHook hook) {
        Bukkit.getScheduler().runTask(this, () -> {

            if (playerUUID == null) {
                hook.sendMessage("⚠️ Minecraft hesabınız eşlenmemiş!").setEphemeral(true).queue();
                return;
            }

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                hook.sendMessage("⚠️ Minecraft oyuncunuz şu anda çevrimiçi değil. Ödül verilemiyor.").setEphemeral(true).queue();
                return;
            }

            FileConfiguration rewardsData = getRewardsDataConfig();
            String path = playerUUID.toString() + ".lastClaim";

            long lastClaim = rewardsData.getLong(path, 0);
            long cooldown = getConfig().getLong("reward-cooldown", 86400000L);
            long now = System.currentTimeMillis();
            long timeDiff = now - lastClaim;

            if (timeDiff < cooldown) {
                long remainingMillis = cooldown - timeDiff;
                long hours = remainingMillis / 3600000;
                long minutes = (remainingMillis % 3600000) / 60000;
                long seconds = (remainingMillis % 60000) / 1000;

                String timeLeft = String.format("%02d saat %02d dakika %02d saniye", hours, minutes, seconds);
                hook.sendMessage("⏳ Ödül almak için lütfen **" + timeLeft + "** bekleyin!").setEphemeral(true).queue();
                return;
            }

            List<String> rewardCommands = getConfig().getStringList("daily-rewards");
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            for (String cmd : rewardCommands) {
                Bukkit.dispatchCommand(console, cmd.replace("%player%", player.getName()));
            }

            rewardsData.set(path, now);
            Bukkit.getScheduler().runTaskAsynchronously(this, this::saveRewardsDataConfig);

            hook.sendMessage("🎉 Günlük ödülünüz başarıyla teslim edildi!").setEphemeral(true).queue();
        });
    }


     // ödül Verme İşlevi
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
