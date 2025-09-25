package me.agnes.agnesesle.commands;

import me.agnes.agnesesle.AgnesEsle;
import me.agnes.agnesesle.data.EslestirmeManager;
import me.agnes.agnesesle.util.MessageUtil;
import me.agnes.agnesesle.util.LuckPermsUtil;
import me.agnes.agnesesle.discord.DiscordBot;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EsleCommand implements CommandExecutor, TabCompleter {

    private final int PAGE_SIZE = 10;
    private final List<String> ALT_KOMUTLAR = Arrays.asList(
            "eşle", "onayla", "iptal", "kaldir", "liste", "sifirla", "yenile", "2fa"
    );

    private final List<String> IKI_FA_ALT_KOMUTLARI = Arrays.asList("aç", "kapat");

    private void playSuccess(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void playError(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyuncular için.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 1) {
            playError(p);
            MessageUtil.sendTitle(p, "hatalı-kullanim");
            return true;
        }

        String altKomut = args[0].toLowerCase();

        if (altKomut.equals("2fa")) {
            if (!EslestirmeManager.eslesmeVar(p.getUniqueId())) {
                playError(p);
                MessageUtil.sendTitle(p, "hesap-eslesmemis");
                return true;
            }
            if (args.length < 2) {
                playError(p);
                p.sendMessage("§cLütfen /hesapeşle 2fa aç veya /hesapeşle 2fa kapat yazın.");
                return true;
            }

            String durum = args[1].toLowerCase();
            if (durum.equals("aç") || durum.equals("ac")) {
                if (EslestirmeManager.isIkiFAOpen(p.getUniqueId())) {
                    p.sendMessage("§e2FA zaten açık.");
                } else {
                    EslestirmeManager.setIkiFA(p.getUniqueId(), true);
                    p.sendMessage("§a2FA başarıyla açıldı.");
                    playSuccess(p);
                }
            } else if (durum.equals("kapat") || durum.equals("kapa")) {
                if (!EslestirmeManager.isIkiFAOpen(p.getUniqueId())) {
                    p.sendMessage("§e2FA zaten kapalı.");
                } else {
                    EslestirmeManager.setIkiFA(p.getUniqueId(), false);
                    p.sendMessage("§c2FA kapatıldı.");
                    playSuccess(p);
                }
            } else {
                playError(p);
                p.sendMessage("§cGeçersiz kullanım! /hesapeşle 2fa aç veya /hesapeşle 2fa kapat");
            }
            return true;
        }

        // --- Mevcut komutlar ---
        switch (altKomut) {
            case "eşle":
                if (EslestirmeManager.eslesmeVar(p.getUniqueId())) {
                    playError(p);
                    MessageUtil.sendTitle(p, "hesap-zaten-eslesmis");
                    return true;
                }
                String kod = EslestirmeManager.uretKod(p.getUniqueId());
                playSuccess(p);
                Map<String, String> vars = new HashMap<>();
                vars.put("kod", kod);
                MessageUtil.sendTitle(p, "kod-verildi", vars);
                break;

            case "iptal":
                if (!EslestirmeManager.beklemeVar(p.getUniqueId())) {
                    playError(p);
                    MessageUtil.sendTitle(p, "eslesme-onayi-beklemiyor");
                    return true;
                }
                EslestirmeManager.iptalEt(p.getUniqueId());
                playSuccess(p);
                MessageUtil.sendTitle(p, "hesap-esle-kodiptal");
                break;

            case "onayla":
                if (!EslestirmeManager.beklemeVar(p.getUniqueId())) {
                    playError(p);
                    MessageUtil.sendTitle(p, "eslesme-onayi-beklemiyor");
                    return true;
                }
                boolean ilkEslesme = !EslestirmeManager.eslesmeVar(p.getUniqueId());
                boolean onaylandi = EslestirmeManager.onaylaEslesme(p.getUniqueId(), null);
                if (!onaylandi) {
                    playError(p);
                    MessageUtil.sendTitle(p, "eslesme-onaylanamadi");
                    return true;
                }
                playSuccess(p);
                MessageUtil.sendTitle(p, "eslesme-basariyla-tamamlandi");

                if (ilkEslesme) {
                    Bukkit.getScheduler().runTask(AgnesEsle.getInstance(), () -> {

                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        p.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, p.getLocation().add(0,1,0), 15,0.5,0.5,0.5);

                        String discordId = EslestirmeManager.discordId(p.getUniqueId());
                        if (discordId != null) {
                            DiscordBot bot = AgnesEsle.getInstance().getDiscordBot();
                            bot.changeNickname(discordId, p.getName());

                            LuckPermsUtil lpUtil = new LuckPermsUtil(AgnesEsle.getInstance().getLuckPerms(), AgnesEsle.getInstance().getLogger());
                            String group = lpUtil.getPrimaryGroup(p.getUniqueId());
                            if (group != null) {
                                String rolePath;
                                switch (group.toLowerCase()) {
                                    case "vip":
                                        rolePath = "roller.vip-rol-id";
                                        break;
                                    case "vipplus":
                                        rolePath = "roller.vipplus-rol-id";
                                        break;
                                    case "mvip":
                                        rolePath = "roller.mvip-rol-id";
                                        break;
                                    case "mvipplus":
                                        rolePath = "roller.mvipplus-rol-id";
                                        break;
                                    default:
                                        rolePath = null;
                                        break;
                                }
                                if (rolePath != null) {
                                    String roleId = AgnesEsle.getInstance().getConfig().getString(rolePath);
                                    if (roleId != null && !roleId.isEmpty()) {
                                        bot.addRoleToMember(discordId, roleId);
                                    }
                                }
                            }
                        }
                    });
                }
                break;

            case "kaldir":
                if (!EslestirmeManager.eslesmeVar(p.getUniqueId())) {
                    playError(p);
                    MessageUtil.sendTitle(p, "eslesme-yok");
                    return true;
                }
                EslestirmeManager.kaldirEslesme(p.getUniqueId());
                playSuccess(p);
                MessageUtil.sendTitle(p, "kaldirildi");
                break;

            case "liste":
                if (!p.hasPermission("agnesesle.admin")) {
                    playError(p);
                    MessageUtil.sendTitle(p, "yetki-yok");
                    return true;
                }
                Map<UUID,String> eslesmeler = EslestirmeManager.getTumEslesmeler();
                if (eslesmeler.isEmpty()) {
                    playError(p);
                    p.sendMessage("§cHenüz eşleşme yok.");
                    return true;
                }

                int toplam = eslesmeler.size();
                int maxPage = (toplam + PAGE_SIZE - 1) / PAGE_SIZE;
                int page = 1;
                if (args.length > 1) {
                    try { page = Math.max(Integer.parseInt(args[1]),1); } catch (NumberFormatException ignored){}
                }
                if (page > maxPage) page = maxPage;

                p.sendMessage("§6---- [Eşleşme Listesi Sayfa "+page+"/"+maxPage+"] ----");
                UUID[] uuids = eslesmeler.keySet().toArray(new UUID[0]);
                int start = (page-1)*PAGE_SIZE;
                int end = Math.min(start+PAGE_SIZE, toplam);
                for(int i=start; i<end; i++) {
                    UUID uuid = uuids[i];
                    String id = eslesmeler.get(uuid);
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String ad = op.getName() != null ? op.getName() : "Bilinmiyor";
                    p.sendMessage("§e"+ad+" §7- Discord ID: §a"+id);
                }
                if(maxPage > 1) p.sendMessage("§7Sayfa değiştirmek için: /hesapeşle liste <sayfa>");
                playSuccess(p);
                break;

            case "sifirla":
                if (!p.hasPermission("agnesesle.admin")) {
                    playError(p);
                    MessageUtil.sendTitle(p, "yetki-yok");
                    return true;
                }
                if (args.length < 2) {
                    playError(p);
                    MessageUtil.sendTitle(p, "hatalı-kullanim");
                    return true;
                }
                String playerName = args[1];
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                if (!target.hasPlayedBefore()) {
                    playError(p);
                    MessageUtil.sendTitle(p, "oyuncu-bulunamadi");
                    return true;
                }
                UUID targetUUID = target.getUniqueId();
                if (!EslestirmeManager.eslesmeVar(targetUUID)) {
                    playError(p);
                    MessageUtil.sendTitle(p, "eslesme-yok");
                    return true;
                }
                EslestirmeManager.kaldirEslesme(targetUUID);
                playSuccess(p);
                p.sendMessage("§a" + target.getName() + " isimli oyuncunun eşleşmesi sıfırlandı.");
                break;

            case "yenile":
                MessageUtil.yenile();
                playSuccess(p);
                MessageUtil.sendTitle(p, "yenilendi");
                break;

            default:
                playError(p);
                MessageUtil.sendTitle(p, "bilinmeyen-komut");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> tamamla = new ArrayList<>();
            for (String alt : ALT_KOMUTLAR) {
                if (alt.startsWith(args[0].toLowerCase())) {
                    tamamla.add(alt);
                }
            }
            return tamamla;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("2fa")) {
            List<String> tamamla = new ArrayList<>();
            for (String alt : IKI_FA_ALT_KOMUTLARI) {
                if (alt.startsWith(args[1].toLowerCase())) {
                    tamamla.add(alt);
                }
            }
            return tamamla;
        }
        return Collections.emptyList();
    }
}
