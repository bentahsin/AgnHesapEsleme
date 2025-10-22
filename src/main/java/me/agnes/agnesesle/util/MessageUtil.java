package me.agnes.agnesesle.util;

import me.agnes.agnesesle.AgnesEsle;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MessageUtil {

    private static final Map<String, FileConfiguration> messagesByLang = new HashMap<>();
    private static String lang = "tr"; // default dil

    // Dil dosyalarını yükler, yoksa kaydeder ve log atar
    public static void load() {
        String[] langs = {"tr", "en", "es", "fr", "de", "zh"};
        File pluginFolder = AgnesEsle.getInstance().getDataFolder();

        for (String l : langs) {
            File file = new File(pluginFolder, "langs/messages_" + l + ".yml");

            if (!file.exists()) {
                AgnesEsle.getInstance().saveResource("langs/messages_" + l + ".yml", false);
                AgnesEsle.getInstance().getLogger().info("[MessageUtil] " + file.getName() + " kaydedildi.");
            } else {
                AgnesEsle.getInstance().getLogger().info("[MessageUtil] " + file.getName() + " zaten mevcut.");
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            messagesByLang.put(l, cfg);
            AgnesEsle.getInstance().getLogger().info("[MessageUtil] " + l + " dili yüklendi, " + cfg.getKeys(false).size() + " mesaj bulundu.");
        }
    }

    // Dil değiştirir, yoksa tr kullanır ve log atar
    public static void setLang(String newLang) {
        if (newLang == null) {
            AgnesEsle.getInstance().getLogger().warning("[MessageUtil] setLang null değer aldı, tr kullanılıyor.");
            lang = "tr";
            return;
        }

        newLang = newLang.toLowerCase();

        if (messagesByLang.containsKey(newLang)) {
            lang = newLang;
            AgnesEsle.getInstance().getLogger().info("[MessageUtil] Dil değiştirildi: " + lang);
        } else {
            AgnesEsle.getInstance().getLogger().warning("[MessageUtil] Dil bulunamadı: " + newLang + ", tr kullanılacak.");
            lang = "tr";
        }
    }

    // Mevcut dil dosyasını döner, yoksa tr veya boş config döner ve log atar
    private static FileConfiguration getMessages() {
        FileConfiguration config = messagesByLang.get(lang);
        if (config == null) {
            AgnesEsle.getInstance().getLogger().warning("[MessageUtil] " + lang + " dil dosyası bulunamadı!");
            config = messagesByLang.get("tr");
            if (config == null) {
                AgnesEsle.getInstance().getLogger().warning("[MessageUtil] Türkçe dil dosyası da bulunamadı!");
                return new YamlConfiguration();
            }
        }
        return config;
    }

    // Mesajı path'e göre alır, değişkenleri yerleştirir, renklendirir
    public static String getMessage(String path, Map<String, String> vars) {
        FileConfiguration messages = getMessages();
        String message = messages.getString(path);

        if (message == null) {
            message = "§cMesaj bulunamadı: " + path;
            AgnesEsle.getInstance().getLogger().warning("[MessageUtil] Mesaj bulunamadı: " + path + " (Dil: " + lang + ")");
        } else if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Varsız getMessage overload
    public static String getMessage(String path) {
        return getMessage(path, null);
    }

    // Mesajdan renk kodlarını temizler
    public static String stripColors(String message) {
        return net.md_5.bungee.api.ChatColor.stripColor(message);
    }

    // Başlık gönderir, değişkenleri yerleştirir
    public static void sendTitle(Player p, String path, Map<String, String> vars) {
        FileConfiguration messages = getMessages();

        String title = messages.getString(path + ".title");
        String subtitle = messages.getString(path + ".subtitle");

        if (title == null) title = "§cBaşlık Bulunamadı";
        if (subtitle == null) subtitle = "§7Alt başlık bulunamadı";

        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                title = title.replace("%" + entry.getKey() + "%", entry.getValue());
                subtitle = subtitle.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        p.sendTitle(ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 70, 20);
    }

    public static void sendTitle(Player p, String path) {
        sendTitle(p, path, null);
    }

    // Mesaj dosyalarını yeniler
    public static void yenile() {
        messagesByLang.clear();
        load();
        AgnesEsle plugin = AgnesEsle.getInstance();
        plugin.reloadConfig();
        AgnesEsle.getInstance().getLogger().info("[MessageUtil] Mesaj dosyaları yenilendi.");
    }
}
