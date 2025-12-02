package me.agnes.agnesesle.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.annotation.*;
import me.agnes.agnesesle.AgnesEsle;
import me.agnes.agnesesle.data.EslestirmeManager;
import me.agnes.agnesesle.discord.DiscordBot;
import me.agnes.agnesesle.util.LuckPermsUtil;
import me.agnes.agnesesle.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CommandAlias("%main_cmd")
@Description("AgnHesapEsle ana komutu.")
public class EsleCommandACF extends BaseCommand {
    private final AgnesEsle plugin;

    public EsleCommandACF(AgnesEsle plugin) {
        this.plugin = plugin;
    }

    private void playSuccess(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void playError(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_esle")
    @Description("Discord hesabınla eşleşmek için kod üretir.")
    public void onEsle(Player player) {
        if (EslestirmeManager.beklemeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-zaten-bekliyor");
            return;
        }
        if (EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "hesap-zaten-eslesmis");
            return;
        }

        String kod = EslestirmeManager.uretKod(player.getUniqueId());
        playSuccess(player);
        Map<String, String> vars = new HashMap<>();
        vars.put("kod", kod);
        MessageUtil.sendTitle(player, "kod-verildi", vars);

        player.sendMessage(MessageUtil.getMessage("kod-verildi.message", vars));
    }


    @SuppressWarnings("unused")
    @Subcommand("%sub_iptal")
    @Description("Bekleyen eşleşme kodunu iptal eder.")
    public void onIptal(Player player) {
        if (!EslestirmeManager.beklemeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-onayi-beklemiyor");
            return;
        }
        if (!EslestirmeManager.iptalEt(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-iptal-edilemedi");
            return;
        }
        playSuccess(player);
        MessageUtil.sendTitle(player, "hesap-esle-kodiptal");
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_onayla")
    @Description("Discord'dan gelen eşleşme talebini onaylar.")
    public void onOnayla(BukkitCommandIssuer issuer) {
        Player player = issuer.getPlayer();
        if (player == null) return;

        if (!EslestirmeManager.beklemeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-onayi-beklemiyor");
            return;
        }

        boolean ilkEslesme = !EslestirmeManager.eslesmeVar(player.getUniqueId());
        String ipAdresi = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        boolean onaylandi = EslestirmeManager.onaylaEslesme(player.getUniqueId(), ipAdresi);

        if (!onaylandi) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-onaylanamadi");
            return;
        }

        playSuccess(player);
        MessageUtil.sendTitle(player, "eslesme-basariyla-tamamlandi");

        if (ilkEslesme) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5);

            Bukkit.getScheduler().runTaskAsynchronously(AgnesEsle.getInstance(), () -> {
                String discordId = EslestirmeManager.getDiscordId(player.getUniqueId());
                if (discordId != null) {
                    DiscordBot bot = AgnesEsle.getInstance().getDiscordBot();
                    bot.changeNickname(discordId, player.getName());

                    LuckPermsUtil lpUtil = plugin.getLuckPermsUtil();
                    if (lpUtil != null) {
                        String group = lpUtil.getPrimaryGroup(player.getUniqueId());
                        if (group != null) {
                            Map<String, String> rolesMap = AgnesEsle.getInstance().getMainConfig().roles;

                            if (rolesMap != null) {
                                for (Map.Entry<String, String> entry : rolesMap.entrySet()) {
                                    String roleName = entry.getKey();
                                    String roleId = entry.getValue();

                                    if (roleId == null || roleId.isEmpty()) continue;

                                    if (group.equalsIgnoreCase(roleName)) {
                                        bot.addRoleToMember(discordId, roleId);
                                        plugin.getLogger().info(player.getName() + " oyuncusuna " + roleName + " Discord rolü verildi.");
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_kaldir")
    @Description("Mevcut hesap eşleşmesini kaldırır.")
    public void onKaldir(Player player) {
        if (!EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-yok");
            return;
        }
        EslestirmeManager.kaldirEslesme(player.getUniqueId());
        playSuccess(player);
        MessageUtil.sendTitle(player, "kaldirildi");
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_2fa")
    @CommandCompletion("aç|kapat")
    @Description("İki faktörlü kimlik doğrulamayı yönetir.")
    public void on2fa(Player player, String durum) {
        if (!EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            playError(player);
            player.sendMessage(MessageUtil.getMessage("2fa-not-linked"));
            return;
        }

        durum = durum.toLowerCase();
        if (durum.equals("aç") || durum.equals("ac")) {
            if (EslestirmeManager.isIkiFAOpen(player.getUniqueId())) {
                player.sendMessage(MessageUtil.getMessage("2fa-already-enabled"));
            } else {
                EslestirmeManager.setIkiFA(player.getUniqueId(), true);
                player.sendMessage(MessageUtil.getMessage("2fa-successfully-enabled"));
                playSuccess(player);
            }
        } else if (durum.equals("kapat") || durum.equals("kapa")) {
            if (!EslestirmeManager.isIkiFAOpen(player.getUniqueId())) {
                player.sendMessage(MessageUtil.getMessage("2fa-already-disabled"));
            } else {
                EslestirmeManager.setIkiFA(player.getUniqueId(), false);
                player.sendMessage(MessageUtil.getMessage("2fa-successfully-disabled"));
                playSuccess(player);
            }
        } else {
            playError(player);
            player.sendMessage(MessageUtil.getMessage("2fa-invalid-subcommand"));
        }
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_liste")
    @CommandPermission("agnesesle.admin")
    @Description("Eşleşmiş tüm oyuncuları listeler.")
    @Syntax("[sayfa]")
    @CommandCompletion("@nothing")
    public void onListe(Player sender, @Default("1") int page) {
        final int PAGE_SIZE = 10;
        Map<UUID, String> eslesmeler = EslestirmeManager.getTumEslesmeler();
        if (eslesmeler.isEmpty()) {
            playError(sender);
            sender.sendMessage(MessageUtil.getMessage("no-matches-yet"));
            return;
        }

        int toplam = eslesmeler.size();
        int maxPage = (toplam + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(1, Math.min(page, maxPage));

        Map<String, String> vars = new HashMap<>();
        vars.put("page", String.valueOf(page));
        vars.put("maxPage", String.valueOf(maxPage));
        sender.sendMessage(MessageUtil.getMessage("list-header", vars));

        UUID[] uuids = eslesmeler.keySet().toArray(new UUID[0]);
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, toplam);
        for (int i = start; i < end; i++) {
            UUID uuid = uuids[i];
            String id = eslesmeler.get(uuid);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String ad = op.getName() != null ? op.getName() : MessageUtil.getMessage("unknown-player");

            vars.clear();
            vars.put("player", ad);
            vars.put("discordId", id);
            sender.sendMessage(MessageUtil.getMessage("list-entry", vars));
        }
        if (maxPage > 1) sender.sendMessage(MessageUtil.getMessage("list-footer"));
        playSuccess(sender);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_sifirla")
    @CommandPermission("agnesesle.admin")
    @Description("Bir oyuncunun hesap eşleşmesini sıfırlar.")
    @CommandCompletion("@players")
    public void onSifirla(Player sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore()) {
            playError(sender);
            MessageUtil.sendTitle(sender, "oyuncu-bulunamadi");
            return;
        }
        if (!EslestirmeManager.eslesmeVar(target.getUniqueId())) {
            playError(sender);
            MessageUtil.sendTitle(sender, "eslesme-yok");
            return;
        }

        EslestirmeManager.kaldirEslesme(target.getUniqueId());
        playSuccess(sender);
        Map<String, String> vars = new HashMap<>();
        vars.put("player", target.getName() != null ? target.getName() : MessageUtil.getMessage("unknown-player"));
        sender.sendMessage(MessageUtil.getMessage("player-match-reset", vars));
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_odul")
    @CommandPermission("agnesesle.admin")
    @Description("Bir oyuncuya manuel olarak eşleşme ödülünü verir.")
    @CommandCompletion("@players")
    public void onOdul(Player sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore()) {
            playError(sender);
            MessageUtil.sendTitle(sender, "oyuncu-bulunamadi");
            return;
        }
        UUID targetUUID = target.getUniqueId();
        if (!EslestirmeManager.eslesmeVar(targetUUID)) {
            sender.sendMessage(MessageUtil.getMessage("odul-not-linked"));
            return;
        }
        if (EslestirmeManager.odulVerildiMi(targetUUID)) {
            sender.sendMessage(MessageUtil.getMessage("odul-already-given"));
            return;
        }

        AgnesEsle.getInstance().odulVer(targetUUID);
        EslestirmeManager.odulVerildi(targetUUID);
        EslestirmeManager.saveOdulVerilenler();

        Map<String, String> vars = new HashMap<>();
        vars.put("player", target.getName());
        sender.sendMessage(MessageUtil.getMessage("odul-given-success", vars));
        playSuccess(sender);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_yenile")
    @CommandPermission("agnesesle.admin")
    @Description("Konfigürasyon ve dil dosyalarını yeniden yükler.")
    public void onYenile(Player sender) {
        MessageUtil.yenile();
        playSuccess(sender);
        MessageUtil.sendTitle(sender, "yenilendi");
    }

    @SuppressWarnings("unused")
    @HelpCommand
    @Syntax("[yardım]")
    public void onHelp(Player sender) {
        sender.sendMessage(MessageUtil.getMessage("help-header"));

        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
            put("command", "eşle"); put("syntax", ""); put("description", "Eşleşme kodu üretir.");
        }}));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
            put("command", "onayla"); put("syntax", ""); put("description", "Eşleşmeyi onaylar.");
        }}));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
            put("command", "iptal"); put("syntax", ""); put("description", "Bekleyen kodu iptal eder.");
        }}));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
            put("command", "kaldır"); put("syntax", ""); put("description", "Eşleşmeyi kaldırır.");
        }}));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
            put("command", "2fa"); put("syntax", "<aç|kapat>"); put("description", "İki faktörlü doğrulamayı yönetir.");
        }}));

        if (sender.hasPermission("agnesesle.admin")) {
            sender.sendMessage(" ");
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
                put("command", "liste"); put("syntax", "[sayfa]"); put("description", "Tüm eşleşmeleri listeler.");
            }}));
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
                put("command", "sıfırla"); put("syntax", "<oyuncu>"); put("description", "Bir oyuncunun eşleşmesini sıfırlar.");
            }}));
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
                put("command", "ödül"); put("syntax", "<oyuncu>"); put("description", "Oyuncuya manuel ödül verir.");
            }}));
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {{
                put("command", "yenile"); put("syntax", ""); put("description", "Eklentiyi yeniden yükler.");
            }}));
        }

        sender.sendMessage(MessageUtil.getMessage("help-footer"));
    }
}