package me.agnes.agnesesle.util;

import me.agnes.agnesesle.AgnesEsle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private static FileConfiguration messages;
    private static String lang = "tr"; // default dil
    private static final Map<String, FileConfiguration> loadedLanguages = new HashMap<>();

    // Yüklü dilleri önceden yükle
    public static void load() {
        File pluginFolder = AgnesEsle.getInstance().getDataFolder();

        String[] langs = {"tr", "en", "es", "fr", "de", "zh"};
        for (String l : langs) {
            File file = new File(pluginFolder, "langs/messages_" + l + ".yml");
            if (!file.exists()) {
                AgnesEsle.getInstance().saveResource("langs/messages_" + l + ".yml", false);
            }
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            loadedLanguages.put(l, cfg);
        }

        messages = loadedLanguages.get(lang);
    }

    public static void setLang(String newLang) {
        newLang = newLang.toLowerCase();
        if (loadedLanguages.containsKey(newLang)) {
            lang = newLang;
            messages = loadedLanguages.get(newLang);
        } else {
            AgnesEsle.getInstance().getLogger().warning("Dil bulunamadı: " + newLang + ". Türkçe kullanılacak.");
            lang = "tr";
            messages = loadedLanguages.get("tr");
        }
    }

    public static String getTitle(String key) {
        return messages.getString(key + ".title", "§cMesaj bulunamadı!");
    }

    public static String getSubtitle(String key) {
        return messages.getString(key + ".subtitle", "");
    }
}
