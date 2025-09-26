package me.agnes.agnesesle.discord;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.agnes.agnesesle.util.MessageUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.*;


import me.agnes.agnesesle.AgnesEsle;
import me.agnes.agnesesle.data.EslestirmeManager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class DiscordBot extends ListenerAdapter {

    private static final long ESLE_COOLDOWN_SECONDS = 60; // 1 dakika
    private static final long REPORT_COOLDOWN_SECONDS = 300; // 5 dakika

    private final Logger logger;
    private final String token;
    private JDA jda;
    private ScheduledExecutorService scheduler;

    // Cache'ler
    private final Cache<String, Long> esleCooldowns;
    private final Cache<String, Long> reportCooldowns;

    private String parsePlaceholders(String mesaj) {
        int aktifKullanici = Bukkit.getOnlinePlayers().size();
        return mesaj.replace("{aktifkullanici}", String.valueOf(aktifKullanici));
    }


    public DiscordBot(String token) {
        this.logger = AgnesEsle.getInstance().getLogger();
        this.token = token;

        this.esleCooldowns = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();

        this.reportCooldowns = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    public void start() {
        try {
            jda = JDABuilder.createDefault(token)
                    .setActivity(net.dv8tion.jda.api.entities.Activity.playing(MessageUtil.getMessage("discord-bot-status-starting")))
                    .addEventListeners(this)
                    .build()
                    .awaitReady();

            jda.upsertCommand("eşle", MessageUtil.getMessage("discord-command-description-esle"))
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "kod", MessageUtil.getMessage("discord-command-option-code"), true)
                    .queue();
            jda.upsertCommand("raporla", MessageUtil.getMessage("discord-command-description-raporla"))
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "oyuncu", MessageUtil.getMessage("discord-command-option-player"), true)
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "sebep", MessageUtil.getMessage("discord-command-option-reason"), true)
                    .queue();
            jda.upsertCommand("bilgi", MessageUtil.getMessage("discord-command-description-info"))
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.USER, "kullanıcı", MessageUtil.getMessage("discord-command-option-user"), true)
                    .queue();

            List<String> durumlar = AgnesEsle.getInstance().getConfig().getStringList("durum-mesajlari");
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                if (durumlar.isEmpty()) return;

                int index = (int) ((System.currentTimeMillis() / 5000) % durumlar.size());
                String mesaj = durumlar.get(index);
                String islenmisMesaj = parsePlaceholders(mesaj);

                jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.playing(islenmisMesaj));
            }, 0, 5, TimeUnit.SECONDS);


            if (!AgnesEsle.getInstance().getConfig().getBoolean("discord.bilgilendirme-gonderildi", false)) {
                String kanalId = AgnesEsle.getInstance().getConfig().getString("discord.bilgilendirme-kanal-id");
                if (kanalId == null || kanalId.isEmpty()) {
                    System.out.println("AgnHesapEşle: Bilgilendirme kanalı ID'si ayarlanmamış.");
                    return;
                }
                TextChannel kanal = jda.getTextChannelById(kanalId);
                if (kanal != null) {
                    Guild guild = kanal.getGuild();
                    String sunucuIkonURL = guild.getIconUrl();

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(MessageUtil.getMessage("discord-info-embed-title"))
                            .setColor(new Color(0x2F3136))
                            .setDescription(MessageUtil.getMessage("discord-info-embed-description"))
                            .setThumbnail(sunucuIkonURL)
                            .setFooter(MessageUtil.getMessage("discord-info-embed-footer"), null);

                    kanal.sendMessageEmbeds(embed.build())
                            .addActionRow(Button.primary("esles_modal", MessageUtil.getMessage("discord-info-button-label")))
                            .queue();

                    AgnesEsle.getInstance().getConfig().set("discord.bilgilendirme-kanal-id", true);
                    AgnesEsle.getInstance().saveConfig();
                }
            }

        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("esles_modal")) {
            Modal modal = Modal.create("kod_gir_modal", MessageUtil.getMessage("discord-modal-title"))
                    .addActionRow(
                            TextInput.create("kod", MessageUtil.getMessage("discord-modal-textfield-label"), TextInputStyle.SHORT)
                                    .setPlaceholder(MessageUtil.getMessage("discord-modal-textfield-placeholder"))
                                    .setRequired(true)
                                    .build()
                    )
                    .build();
            event.replyModal(modal).queue();
        }

        else if (id.startsWith("2fa_confirm_")) {
            String[] parts = id.split("_");
            UUID playerUUID = UUID.fromString(parts[2]);
            String newIP = parts[3];

            EslestirmeManager.setKayitliIP(playerUUID, newIP);

            event.reply(MessageUtil.getMessage("discord-2fa-confirm-reply")).setEphemeral(true).queue();
            event.getMessage().editMessage(MessageUtil.getMessage("discord-2fa-confirm-message-edit")).setComponents().queue();
        }

        else if (id.startsWith("2fa_deny_")) {
            event.reply(MessageUtil.getMessage("discord-2fa-deny-reply")).setEphemeral(true).queue();
            event.getMessage().editMessage(MessageUtil.getMessage("discord-2fa-deny-message-edit")).setComponents().queue();
        }

        else if (id.startsWith("report_kontrol_")) {
            String[] parts = id.split("_");
            if (parts.length < 4) {
                event.reply(MessageUtil.getMessage("discord-button-invalid-id")).setEphemeral(true).queue();
                return;
            }
            String raporlananDiscordId = parts[2];
            String uuidStr = parts[3];
            String yetkiliRolId = AgnesEsle.getInstance().getConfig().getString("yetkili-rol-id");

            if (yetkiliRolId == null || Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(role -> role.getId().equals(yetkiliRolId))) {
                event.reply(MessageUtil.getMessage("discord-button-no-permission")).setEphemeral(true).queue();
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (!target.hasPlayedBefore()) {
                event.reply(MessageUtil.getMessage("discord-button-player-not-found")).setEphemeral(true).queue();
                return;
            }

            Player player = Bukkit.getPlayer(target.getUniqueId());
            if (player != null) {
                player.kickPlayer(MessageUtil.getMessage("discord-button-control-kick-reason"));
            }

            Map<String, String> vars = new HashMap<>();
            vars.put("player", target.getName());
            vars.put("discordId", raporlananDiscordId);
            event.reply(MessageUtil.getMessage("discord-button-control-reply", vars)).setEphemeral(true).queue();
        }

        else if (id.startsWith("report_ban_")) {
            String[] parts = id.split("_");
            if (parts.length < 4) {
                event.reply(MessageUtil.getMessage("discord-button-invalid-id")).setEphemeral(true).queue();
                return;
            }
            String uuidStr = parts[3];
            String yetkiliRolId = AgnesEsle.getInstance().getConfig().getString("yetkili-rol-id");

            if (yetkiliRolId == null || Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(role -> role.getId().equals(yetkiliRolId))) {
                event.reply(MessageUtil.getMessage("discord-button-no-permission")).setEphemeral(true).queue();
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (!target.hasPlayedBefore()) {
                event.reply(MessageUtil.getMessage("discord-button-player-not-found")).setEphemeral(true).queue();
                return;
            }

            Bukkit.getScheduler().runTask(AgnesEsle.getInstance(), () -> {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(Objects.requireNonNull(target.getName()), MessageUtil.getMessage("discord-button-ban-reason"), null, event.getUser().getAsTag());
                Player player = Bukkit.getPlayer(target.getUniqueId());
                if (player != null) {
                    player.kickPlayer(MessageUtil.getMessage("discord-button-ban-kick-reason"));
                }
            });

            Map<String, String> vars = new HashMap<>();
            vars.put("player", target.getName());
            event.reply(MessageUtil.getMessage("discord-button-ban-reply", vars)).setEphemeral(true).queue();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSlashCommandInteraction(net.dv8tion.jda.api.events.interaction.command.@NotNull SlashCommandInteractionEvent event) {
        try {
            if (event.getName().equals("eşle")) {
                String userId = event.getUser().getId();
                if (isUserOnCooldown(userId, esleCooldowns, ESLE_COOLDOWN_SECONDS, event)) {
                    return;
                }

                String kod = Objects.requireNonNull(event.getOption("kod")).getAsString().toUpperCase();

                event.deferReply(true).queue();

                Bukkit.getScheduler().runTaskAsynchronously(AgnesEsle.getInstance(), () -> {
                    UUID uuid = EslestirmeManager.koduKontrolEt(kod);
                    if (uuid == null) {
                        event.getHook().sendMessage(MessageUtil.stripColors(MessageUtil.getMessage("discord-invalid-code"))).queue();
                        return;
                    }

                    if (EslestirmeManager.eslesmeVar(uuid)) {
                        event.getHook().sendMessage(MessageUtil.getMessage("discord-already-linked-mc")).queue();
                        return;
                    }

                    if (EslestirmeManager.discordZatenEslesmis(event.getUser().getId())) {
                        event.getHook().sendMessage(MessageUtil.getMessage("discord-already-linked-discord")).queue();
                        return;
                    }

                    boolean basarili = EslestirmeManager.eslestir(uuid, event.getUser().getId());
                    if (!basarili) {
                        event.getHook().sendMessage(MessageUtil.getMessage("discord-generic-error")).queue();
                        return;
                    }

                    setUserCooldown(userId, esleCooldowns);

                    event.getHook().sendMessage(MessageUtil.getMessage("discord-success")).queue();
                });
            }
            else if (event.getName().equals("raporla")) {
                String userId = event.getUser().getId();

                if (isUserOnCooldown(userId, reportCooldowns, REPORT_COOLDOWN_SECONDS, event)) {
                    return;
                }

                String raporlananOyuncu = Objects.requireNonNull(event.getOption("oyuncu")).getAsString();
                String sebep = Objects.requireNonNull(event.getOption("sebep")).getAsString();
                String raporlayanKullanici = Objects.requireNonNull(event.getUser()).getAsTag();

                String logKanalId = AgnesEsle.getInstance().getConfig().getString("log-kanal-id");
                if (logKanalId == null || logKanalId.isEmpty()) {
                    event.reply(MessageUtil.getMessage("discord-report-channel-not-set")).setEphemeral(true).queue();
                    return;
                }

                TextChannel logKanali = jda.getTextChannelById(logKanalId);
                if (logKanali == null) {
                    event.reply(MessageUtil.getMessage("discord-report-channel-not-found")).setEphemeral(true).queue();
                    return;
                }

                Map<String, String> reportVars = new HashMap<>();
                reportVars.put("reporter", raporlayanKullanici);
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(MessageUtil.getMessage("discord-report-embed-title"))
                        .setColor(Color.RED)
                        .addField(MessageUtil.getMessage("discord-report-embed-field-player"), raporlananOyuncu, false)
                        .addField(MessageUtil.getMessage("discord-report-embed-field-reason"), sebep, false)
                        .setFooter(MessageUtil.getMessage("discord-report-embed-footer", reportVars));

                logKanali.sendMessageEmbeds(embed.build()).queue();

                setUserCooldown(userId, reportCooldowns); // Cooldown'ı başlat
                event.reply(MessageUtil.getMessage("discord-report-success")).setEphemeral(true).queue();
            }
            else if (event.getName().equals("bilgi")) {
                net.dv8tion.jda.api.entities.User targetUser = Objects.requireNonNull(event.getOption("kullanıcı")).getAsUser();
                String discordId = targetUser.getId();

                UUID playerUUID = EslestirmeManager.getUUIDByDiscordId(discordId);

                if (playerUUID == null) {
                    event.reply(MessageUtil.getMessage("discord-info-reply-no-match")).setEphemeral(true).queue();
                } else {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                    Map<String, String> vars = new HashMap<>();
                    vars.put("player", player.getName() != null ? player.getName() : "Bilinmiyor");
                    event.reply(MessageUtil.getMessage("discord-info-reply-success", vars)).setEphemeral(true).queue();
                }
            }
        } catch (Exception e) {
            logger.severe("Slash komutu işlenirken bir hata oluştu: " + event.getName());
            logger.severe(e.getMessage());
        }
    }


    public void changeNickname(String discordId, String newNickname) {
        String guildId = AgnesEsle.getInstance().getConfig().getString("discord.guild-id");
        if (guildId == null || guildId.isEmpty()) {
            System.out.println("AgnHesapEşle: Guild ID ayarlanmamış.");
            return;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(member -> {
            guild.modifyNickname(member, newNickname).queue();
        }, error -> {
            logger.warning("Kullanıcı bulunamadı veya nickname değiştirilemedi: " + error.getMessage());
        });
    }

    public void addRoleToMember(String discordId, String roleId) {
        String guildId = AgnesEsle.getInstance().getConfig().getString("discord.guild-id");
        if (guildId == null || guildId.isEmpty()) {
            System.out.println("AgnHesapEşle: Guild ID ayarlanmamış.");
            return;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (guild.getRoleById(roleId) != null) {
                guild.addRoleToMember(member, Objects.requireNonNull(guild.getRoleById(roleId))).queue();
            }
        }, error -> logger.warning("Kullanıcı bulunamadı veya rol atanamadı: " + error.getMessage()));
    }

    public void send2FAConfirmationMessage(String discordId, String playerName, String newIpAddress) {
        if (jda == null) return;

        jda.retrieveUserById(discordId).queue(user -> {
            if (user == null) return;

            Map<String, String> vars = new HashMap<>();
            vars.put("player", playerName);
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(MessageUtil.getMessage("discord-2fa-embed-title"))
                    .setDescription(MessageUtil.getMessage("discord-2fa-embed-description", vars))
                    .addField(MessageUtil.getMessage("discord-2fa-embed-field-ip"), "||" + newIpAddress + "||", false)
                    .setColor(Color.ORANGE)
                    .setFooter(MessageUtil.getMessage("discord-2fa-embed-footer"));

            Button confirmButton = Button.success("2fa_confirm_", MessageUtil.getMessage("discord-2fa-button-confirm"));
            Button denyButton = Button.danger("2fa_deny_", MessageUtil.getMessage("discord-2fa-button-deny"));

            user.openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(embed.build()).setActionRow(confirmButton, denyButton).queue();
            });
        }, throwable -> {
            logger.warning("2FA onayı için " + discordId + " ID'li kullanıcıya DM gönderilemedi.");
        });
    }

    private boolean isUserOnCooldown(String userId, Cache<String, Long> cooldowns, long cooldownTimeSeconds, net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        Long lastUsed = cooldowns.getIfPresent(userId);

        if (lastUsed != null) {
            long secondsSinceLastUse = (System.currentTimeMillis() - lastUsed) / 1000;

            if (secondsSinceLastUse < cooldownTimeSeconds) {
                long timeLeft = cooldownTimeSeconds - secondsSinceLastUse;

                Map<String, String> vars = new HashMap<>();
                vars.put("timeLeft", String.valueOf(timeLeft));
                event.reply(MessageUtil.getMessage("discord-cooldown-message", vars)).setEphemeral(true).queue();

                return true;
            }
        }

        return false;
    }

    private void setUserCooldown(String userId, Cache<String, Long> cooldowns) {
        cooldowns.put(userId, System.currentTimeMillis());
    }

    public void shutdown() {
        if (jda != null) jda.shutdownNow();
        if (scheduler != null) scheduler.shutdownNow();
    }

    public JDA getJda() {
        return jda;
    }
}
