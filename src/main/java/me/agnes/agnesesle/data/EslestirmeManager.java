package me.agnes.agnesesle.data;

import me.agnes.agnesesle.AgnesEsle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EslestirmeManager {

    private static final Logger logger = AgnesEsle.getInstance().getLogger();

    private static final Map<String, UUID> kodlar = new ConcurrentHashMap<>();
    private static final Map<UUID, String> eslesmeler = new ConcurrentHashMap<>();
    private static final Map<UUID, String> bekleyenEslesmeler = new ConcurrentHashMap<>();

    private static final Map<String, UUID> bekleyenKodlar = new ConcurrentHashMap<>();
    private static final Map<String, Long> kodZamanlari = new ConcurrentHashMap<>();

    private static final Set<UUID> odulVerilenler = ConcurrentHashMap.newKeySet();

    // 2FA Durumu (Eklenen kısım)
    private static final Map<UUID, Boolean> ikiFADurumu = new ConcurrentHashMap<>();

    // IP Adresleri (2FA için)
    private static final Map<UUID, String> kayitliIPler = new ConcurrentHashMap<>();

    private static final File dataFile = new File(AgnesEsle.getInstance().getDataFolder(), "data.json");
    private static final File ikiFAFile = new File(AgnesEsle.getInstance().getDataFolder(), "ikiFA.json");
    private static final File ipFile = new File(AgnesEsle.getInstance().getDataFolder(), "ipData.json");
    private static final File odulFile = new File(AgnesEsle.getInstance().getDataFolder(), "odulVerilenler.json");  // buraya ekle

    private static final Gson gson = new Gson();

    public static void init() {
        loadEslesmeler();
        loadIkiFA();
        loadIPler();
        loadOdulVerilenler();
    }

    public static void loadOdulVerilenler() {
        if (!odulFile.exists()) return;
        try (Reader reader = new FileReader(odulFile)) {
            Type type = new TypeToken<Set<UUID>>(){}.getType();
            Set<UUID> loaded = gson.fromJson(reader, type);
            if (loaded != null) odulVerilenler.addAll(loaded);
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }
    // Ödül Verilenleri Kaydedilme İşlevi
    public static void saveOdulVerilenler() {
        try (Writer writer = new FileWriter(odulFile)) {
            gson.toJson(odulVerilenler, writer);
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }


    // Kod Üretme İşlevi
    public static String uretKod(UUID uuid) {
        String kod = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        kodlar.put(kod, uuid);
        bekleyenKodlar.put(kod, uuid);
        kodZamanlari.put(kod, System.currentTimeMillis());
        return kod;
    }
    // Kod Kontrol
    public static UUID koduKontrolEt(String kod) {
        if (kod == null) return null;
        return kodlar.get(kod.toUpperCase());
    }
    // Eşleştirme
    public static boolean eslestir(UUID uuid, String discordId) {
        if (discordId == null || uuid == null) return false;
        if (eslesmeler.containsValue(discordId) || bekleyenEslesmeler.containsValue(discordId)) {
            return false;
        }
        bekleyenEslesmeler.put(uuid, discordId);
        return true;
    }
     // Onay Kısmı
    public static boolean onaylaEslesme(UUID uuid, String ip) {
        AgnesEsle.getInstance().getLogger().info("onaylaEslesme çağrıldı: " + uuid);
        String discordId = bekleyenEslesmeler.remove(uuid);
        if (discordId == null) return false;

        eslesmeler.put(uuid, discordId);

        if (!odulVerilenler.contains(uuid)) {
            odulVerilenler.add(uuid);
            AgnesEsle.getInstance().odulVer(uuid);
            saveOdulVerilenler();
        }

        String roleId = AgnesEsle.getInstance().getConfig().getString("verified-role-id");
        if (roleId != null && !roleId.isEmpty()) {
            AgnesEsle.getInstance().getDiscordBot().addRoleToMember(discordId, roleId);
        } else {
            AgnesEsle.getInstance().getLogger().warning("Verified role ID config'de ayarlanmamış.");
        }

        kodlar.values().removeIf(u -> u.equals(uuid));
        bekleyenKodlar.values().removeIf(u -> u.equals(uuid));
        kodZamanlari.entrySet().removeIf(entry -> {
            String k = entry.getKey();
            UUID val = bekleyenKodlar.get(k);
            return val != null && val.equals(uuid);
        });

        if (ip != null) {
            kayitliIPler.put(uuid, ip);
        }

        saveEslesmeler();
        saveIPler();

        if (AgnesEsle.getInstance().getConfig().getBoolean("log-system", false)) {
            AgnesEsle.getInstance().getDiscordBot().sendEslestirmeEmbed(uuid, discordId);
        }


        return true;
    }





    public static boolean beklemeVar(UUID uuid) {
        return bekleyenEslesmeler.containsKey(uuid);
    }

    public static void kaldirEslesme(UUID uuid) {
        eslesmeler.remove(uuid);
        ikiFADurumu.remove(uuid);
        kayitliIPler.remove(uuid);


        saveEslesmeler();
        saveIkiFA();
        saveIPler();

        saveOdulVerilenler();
    }


    public static Map<UUID, String> getTumEslesmeler() {
        return Collections.unmodifiableMap(eslesmeler);
    }

    public static boolean eslesmeVar(UUID uuid) {
        return eslesmeler.containsKey(uuid);
    }

    public static boolean discordZatenEslesmis(String discordId) {
        return eslesmeler.containsValue(discordId) || bekleyenEslesmeler.containsValue(discordId);
    }

    public static UUID getUUIDByDiscordId(String discordId) {
        for (Map.Entry<UUID, String> entry : eslesmeler.entrySet()) {
            if (entry.getValue().equals(discordId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean iptalEt(UUID uuid) {
        if (bekleyenKodlar.containsValue(uuid)) {
            String kod = null;
            for (Map.Entry<String, UUID> entry : bekleyenKodlar.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    kod = entry.getKey();
                    break;
                }
            }
            if (kod != null) {
                kodlar.remove(kod);
                bekleyenKodlar.remove(kod);
                kodZamanlari.remove(kod);
                return true;
            }
        }
        return false;
    }

    public static boolean odulVerildiMi(UUID uuid) {
        return odulVerilenler.contains(uuid);
    }

    public static void odulVerildi(UUID uuid) {
        odulVerilenler.add(uuid);
    }

    public static String getDiscordId(UUID uuid) {
        return eslesmeler.get(uuid);
    }


    public static boolean isIkiFAOpen(UUID uuid) {
        return ikiFADurumu.getOrDefault(uuid, false);
    }

    public static void setIkiFA(UUID uuid, boolean durum) {
        ikiFADurumu.put(uuid, durum);
        saveIkiFA();
    }


    public static String getKayitliIP(UUID uuid) {
        return kayitliIPler.get(uuid);
    }

    public static void setKayitliIP(UUID uuid, String ip) {
        if (ip != null) {
            kayitliIPler.put(uuid, ip);
            saveIPler();
        }
    }

    public static boolean ipDegisti(UUID uuid, String yeniIP) {
        String eskiIP = kayitliIPler.get(uuid);
        if (eskiIP == null) return false;
        return !eskiIP.equals(yeniIP);
    }


    public static void loadEslesmeler() {
        Map<UUID, String> veriler = loadData(new TypeToken<Map<UUID, String>>() {}.getType());
        if (veriler != null) {
            eslesmeler.putAll(veriler);
        }
    }

    private static void saveEslesmeler() {
        saveData(dataFile, eslesmeler);
    }

    private static void loadIkiFA() {
        if (!ikiFAFile.exists()) return;
        try (Reader reader = new FileReader(ikiFAFile)) {
            Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> data = gson.fromJson(reader, type);
            if (data != null) {
                ikiFADurumu.clear();
                data.forEach((k,v) -> ikiFADurumu.put(UUID.fromString(k), v));
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    private static void saveIkiFA() {
        Map<String, Boolean> data = new HashMap<>();
        ikiFADurumu.forEach((k,v) -> data.put(k.toString(), v));
        saveData(ikiFAFile, data);
    }

    private static void loadIPler() {
        if (!ipFile.exists()) return;
        try (Reader reader = new FileReader(ipFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> data = gson.fromJson(reader, type);
            if (data != null) {
                kayitliIPler.clear();
                data.forEach((k,v) -> kayitliIPler.put(UUID.fromString(k), v));
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    private static void saveIPler() {
        Map<String, String> data = new HashMap<>();
        kayitliIPler.forEach((k,v) -> data.put(k.toString(), v));
        try (Writer writer = new FileWriter(ipFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }
     // Data'ya Kaydetme işlevi
    private static <T> void saveData(File file, T data) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            logger.warning("Veri kaydedilemedi: " + file.getName() + " -> " + e.getMessage());
        }
    }

    private static <T> T loadData(Type type) {
        if (!EslestirmeManager.dataFile.exists()) {
            return null;
        }
        try (Reader reader = new FileReader(EslestirmeManager.dataFile)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            logger.warning("Veri okunamadı: " + EslestirmeManager.dataFile.getName() + " -> " + e.getMessage());
            return null;
        }
    }
}
