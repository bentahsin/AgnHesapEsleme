package me.agnes.agnesesle.util;

import me.agnes.agnesesle.AgnesEsle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    private static final Map<String, FileConfiguration> messagesByLang = new HashMap<>();
    private static String lang = "tr"; // default dil

    public static void load() {
        String[] langs = {"tr", "en", "es", "fr", "de", "zh"};
        File pluginFolder = AgnesEsle.getInstance().getDataFolder();

        for (String l : langs) {
            File file = new File(pluginFolder, "langs/messages_" + l + ".yml");
            if (!file.exists()) {
                AgnesEsle.getInstance().saveResource("langs/messages_" + l + ".yml", false);
            }
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            messagesByLang.put(l, cfg);
        }
    }

    public static void setLang(String newLang) {
        newLang = newLang.toLowerCase();
        if (messagesByLang.containsKey(newLang)) {
            lang = newLang;
        } else {
            AgnesEsle.getInstance().getLogger().warning("Dil bulunamadı: " + newLang + ", tr kullanılacak.");
            lang = "tr";
        }
    }

    private static FileConfiguration getMessages() {
        FileConfiguration config = messagesByLang.get(lang);
        if (config == null) {
            config = messagesByLang.get("tr"); // fallback Türkçe
        }
        return config;
    }

    public static void sendTitle(Player p, String path, Map<String, String> vars) {
        FileConfiguration messages = getMessages();

        String title = messages.getString(path + ".title", "§cBaşlık Bulunamadı");
        String subtitle = messages.getString(path + ".subtitle", "§7Alt başlık bulunamadı");

        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                title = title.replace("%" + entry.getKey() + "%", entry.getValue());
                subtitle = subtitle.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        p.sendTitle(title, subtitle, 10, 70, 20);
    }

    public static void sendTitle(Player p, String path) {
        sendTitle(p, path, null);
    }

    // Mesaj dosyalarını yenilemek için Türkçe metod
    public static void yenile() {
        messagesByLang.clear();
        load();
        AgnesEsle plugin = AgnesEsle.getInstance();
        plugin.reloadConfig();
    }
}
