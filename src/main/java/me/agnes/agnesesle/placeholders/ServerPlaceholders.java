package me.agnes.agnesesle.placeholders;

import com.bentahsin.benthpapimanager.annotations.Placeholder;
import com.bentahsin.benthpapimanager.annotations.PlaceholderIdentifier;
import me.agnes.agnesesle.data.EslestirmeManager;
import org.bukkit.OfflinePlayer;

@Placeholder(identifier = "agnesesle_server", author = "Agnes & bentahsin", version = "1.2")
public class ServerPlaceholders {

    /**
     * Sunucudaki toplam hesap eşleşmesi sayısını döndürür.
     * Kullanım: %agnesesle_server_toplam_eslesme%
     */
    @PlaceholderIdentifier(identifier = "toplam_eslesme", onError = "0")
    public String getToplamEslesme(OfflinePlayer player) {
        return String.valueOf(EslestirmeManager.getTumEslesmeler().size());
    }
}