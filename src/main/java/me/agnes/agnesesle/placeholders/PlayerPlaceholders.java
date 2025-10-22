package me.agnes.agnesesle.placeholders;

import com.bentahsin.benthpapimanager.annotations.Inject;
import com.bentahsin.benthpapimanager.annotations.Placeholder;
import com.bentahsin.benthpapimanager.annotations.PlaceholderIdentifier;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.agnes.agnesesle.data.EslestirmeManager;
import me.agnes.agnesesle.discord.DiscordBot;
import net.dv8tion.jda.api.JDA;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Placeholder(identifier = "agnesesle", author = "Agnes & bentahsin", version = "1.2")
public class PlayerPlaceholders {

    @Inject
    private DiscordBot discordBot;

    private final LoadingCache<String, String> discordNameCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(discordId -> {
                JDA jda = discordBot.getJda();
                if (jda == null) return "Bilinmiyor";

                try {
                    return jda.retrieveUserById(discordId).complete().getName();
                } catch (Exception e) {
                    return "Bilinmiyor";
                }
            });

    /**
     * Oyuncunun hesap eşleşme durumunu döndürür.
     * Kullanım: %agnesesle_durum%
     */
    @PlaceholderIdentifier(identifier = "durum", onError = "§cHata")
    public String getDurum(OfflinePlayer player) {
        if (player == null) return "Oyuncu Yok";
        return EslestirmeManager.eslesmeVar(player.getUniqueId()) ? "§aEşleşmiş" : "§cEşleşmemiş";
    }

    /**
     * Oyuncunun eşleştiği Discord hesabının ID'sini döndürür.
     * Kullanım: %agnesesle_discord_id%
     */
    @PlaceholderIdentifier(identifier = "discord_id", onError = "-")
    public String getDiscordId(OfflinePlayer player) {
        if (player == null || !EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            return "Yok";
        }
        return EslestirmeManager.getDiscordId(player.getUniqueId());
    }

    /**
     * Oyuncunun eşleştiği Discord hesabının adını (cache'li olarak) döndürür.
     * Kullanım: %agnesesle_discord_adi%
     */
    @PlaceholderIdentifier(identifier = "discord_adi", onError = "Bilinmiyor")
    public String getDiscordAdi(OfflinePlayer player) {
        if (player == null || !EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            return "Yok";
        }
        String discordId = EslestirmeManager.getDiscordId(player.getUniqueId());
        if (discordId == null) return "Bilinmiyor";

        return discordNameCache.get(discordId);
    }

    /**
     * Oyuncunun 2FA durumunu döndürür.
     * Kullanım: %agnesesle_2fa_durum%
     */
    @PlaceholderIdentifier(identifier = "2fa_durum", onError = "§cHata")
    public String get2faDurum(OfflinePlayer player) {
        if (player == null || !EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            return "§7-";
        }
        return EslestirmeManager.isIkiFAOpen(player.getUniqueId()) ? "§aAktif" : "§cPasif";
    }
}