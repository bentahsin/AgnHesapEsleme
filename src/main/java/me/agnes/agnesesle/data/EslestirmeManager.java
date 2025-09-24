package me.agnes.agnesesle.data;

import me.agnes.agnesesle.AgnesEsle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EslestirmeManager {

    private static final Map<String, UUID> kodlar = new ConcurrentHashMap<>();
    private static final Map<UUID, String> eslesmeler = new ConcurrentHashMap<>();
    private static final Map<UUID, String> bekleyenEslesmeler = new ConcurrentHashMap<>();

    private static final Map<String, UUID> bekleyenKodlar = new ConcurrentHashMap<>();
    private static final Map<String, Long> kodZamanları = new ConcurrentHashMap<>();

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
            e.printStackTrace();
        }
    }

    public static void saveOdulVerilenler() {
        try (Writer writer = new FileWriter(odulFile)) {
            gson.toJson(odulVerilenler, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static String uretKod(UUID uuid) {
        String kod = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        kodlar.put(kod, uuid);
        bekleyenKodlar.put(kod, uuid);
        kodZamanları.put(kod, System.currentTimeMillis());
        return kod;
    }

    public static UUID koduKontrolEt(String kod) {
        if (kod == null) return null;
        return kodlar.get(kod.toUpperCase());
    }

    public static boolean eslestir(UUID uuid, String discordId) {
        if (discordId == null || uuid == null) return false;
        if (eslesmeler.containsValue(discordId) || bekleyenEslesmeler.containsValue(discordId)) {
            return false;
        }
        bekleyenEslesmeler.put(uuid, discordId);
        return true;
    }

    public static boolean onaylaEslesme(UUID uuid, String ip) {
        AgnesEsle.getInstance().getLogger().info("onaylaEslesme çağrıldı: " + uuid);
        String discordId = bekleyenEslesmeler.remove(uuid);
        if (discordId == null) return false;



        eslesmeler.put(uuid, discordId);

        if (!odulVerilenler.contains(uuid)) {
            odulVerilenler.add(uuid); // Ödül verildi olarak işaretle
            AgnesEsle.getInstance().odulVer(uuid); // Ödül ver
            saveOdulVerilenler(); // Kaydet (bu metod senin yazman lazım)
        }

        kodlar.values().removeIf(u -> u.equals(uuid));
        bekleyenKodlar.values().removeIf(u -> u.equals(uuid));
        kodZamanları.entrySet().removeIf(entry -> {
            String k = entry.getKey();
            UUID val = bekleyenKodlar.get(k);
            return val != null && val.equals(uuid);
        });

        if (ip != null) {
            kayitliIPler.put(uuid, ip);
        }

        saveEslesmeler();
        saveIPler();

        // Burada ödül verilip verilmediğini kontrol ediyoruz
        if (!odulVerilenler.contains(uuid)) {
            odulVerilenler.add(uuid); // Ödül verildi olarak işaretle
            // Burada ödül komutlarını çalıştırmak için bir metodu çağırabilirsin
            AgnesEsle.getInstance().odulVer(uuid); // Örnek metod, sen eklemen lazım
        }

        return true;
    }


    public static boolean beklemeVar(UUID uuid) {
        return bekleyenEslesmeler.containsKey(uuid);
    }

    public static String bekleyenDiscordId(UUID uuid) {
        return bekleyenEslesmeler.get(uuid);
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

    public static String discordId(UUID uuid) {
        return eslesmeler.get(uuid);
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
                kodZamanları.remove(kod);
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


    private static void loadEslesmeler() {
        if (!dataFile.exists()) return;

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, String>>() {}.getType();
            Map<UUID, String> veriler = gson.fromJson(reader, type);
            if (veriler != null) eslesmeler.putAll(veriler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveEslesmeler() {
        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(eslesmeler, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
    }

    private static void saveIkiFA() {
        Map<String, Boolean> data = new HashMap<>();
        ikiFADurumu.forEach((k,v) -> data.put(k.toString(), v));
        try (Writer writer = new FileWriter(ikiFAFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
    }

    private static void saveIPler() {
        Map<String, String> data = new HashMap<>();
        kayitliIPler.forEach((k,v) -> data.put(k.toString(), v));
        try (Writer writer = new FileWriter(ipFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
