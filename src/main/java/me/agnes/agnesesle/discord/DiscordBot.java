package me.agnes.agnesesle.discord;

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
import java.util.List;


import me.agnes.agnesesle.AgnesEsle;
import me.agnes.agnesesle.data.EslestirmeManager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class DiscordBot extends ListenerAdapter {

    private final Logger logger;
    private final String token;
    private JDA jda;
    private ScheduledExecutorService scheduler;
    private boolean isListenerAdded = false;

    // Timeout schedulerlar
    private final ConcurrentHashMap<String, Long> oneriCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> reportCooldowns = new ConcurrentHashMap<>();

    private String parsePlaceholders(String mesaj) {
        int aktifKullanici = Bukkit.getOnlinePlayers().size();
        return mesaj.replace("{aktifkullanici}", String.valueOf(aktifKullanici));
    }


    public DiscordBot(String token) {
        this.logger = AgnesEsle.getInstance().getLogger();
        this.token = token;
    }

    public void start() {
        try {
            jda = JDABuilder.createDefault(token)
                    .setActivity(net.dv8tion.jda.api.entities.Activity.playing("Başlatılıyor..."))
                    .addEventListeners(this)
                    .build()
                    .awaitReady();

            jda.upsertCommand("eşle", "Minecraft hesabını eşleştir")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "kod", "Eşleştirme kodun", true)
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
                            .setTitle("📘 AgnEşle - Hesap Eşleştirme")
                            .setColor(new Color(0x2F3136))
                            .setDescription(String.join("\n",
                                    "Merhaba! Minecraft hesabını Discord ile eşleştirerek özel ödüller kazanabilirsin!",
                                    "",
                                    "➤ Sunucuya gir ve `/hesapeşle eşle` yazarak kodunu al.",
                                    "➤ Aşağıdaki butona tıkla ve kodunu gir.",
                                    "➤ Son olarak Minecraft'ta `/hesapeşle onayla` yazarak işlemi tamamla!"
                            ))
                            .setThumbnail(sunucuIkonURL)
                            .setFooter("AgnHesapEşle Sistemi", null);

                    kanal.sendMessageEmbeds(embed.build())
                            .addActionRow(Button.primary("esles_modal", "🔗 Eşleş"))
                            .queue();

                    AgnesEsle.getInstance().getConfig().set("discord.bilgilendirme-kanal-id", true);
                    AgnesEsle.getInstance().saveConfig();
                }
            }

            isListenerAdded = true;

        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("esles_modal")) {
            Modal modal = Modal.create("kod_gir_modal", "Eşleşme Kodunu Gir")
                    .addActionRow(
                            TextInput.create("kod", "Eşleştirme Kodu", TextInputStyle.SHORT)
                                    .setPlaceholder("Örn: X7Y2-Z9")
                                    .setRequired(true)
                                    .build()
                    )
                    .build();
            event.replyModal(modal).queue();
        }

        else if (id.startsWith("report_kontrol_")) {
            String[] parts = id.split("_");
            if (parts.length < 4) {
                event.reply("Hatalı buton ID!").setEphemeral(true).queue();
                return;
            }
            String discordId = parts[2];
            String uuidStr = parts[3];

            if (!event.getUser().getId().equals(event.getUser().getId())) {
                event.reply("Bu butonu sadece yetkililer kullanabilir.").setEphemeral(true).queue();
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (target == null) {
                event.reply("Oyuncu bulunamadı!").setEphemeral(true).queue();
                return;
            }

            Player player = Bukkit.getPlayer(target.getUniqueId());
            if (player != null) {
                player.kickPlayer("Lütfen kontrol için Discord’a gel!");
            }
            event.reply("Oyuncu kontrol için sunucudan atıldı.").setEphemeral(true).queue();
        }

        else if (id.startsWith("report_ban_")) {
            String[] parts = id.split("_");
            if (parts.length < 4) {
                event.reply("Hatalı buton ID!").setEphemeral(true).queue();
                return;
            }
            String discordId = parts[2];
            String uuidStr = parts[3];

            if (!event.getUser().getId().equals(event.getUser().getId())) {
                event.reply("Bu butonu sadece yetkililer kullanabilir.").setEphemeral(true).queue();
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));

            Bukkit.getScheduler().runTask(AgnesEsle.getInstance(), () -> {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(Objects.requireNonNull(target.getName()), "Discord üzerinden banlandı.", null, null);

                Player player = Bukkit.getPlayer(target.getUniqueId());
                if (player != null) {
                    player.kickPlayer("Discord Üzerinden Yetkili Tarafından Banlandınız.");
                }
            });

            event.reply("Oyuncu banlandı.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        if (event.getName().equals("eşle")) {
            String kod = Objects.requireNonNull(event.getOption("kod")).getAsString().toUpperCase();

            event.deferReply(true).queue();

            Bukkit.getScheduler().runTaskAsynchronously(AgnesEsle.getInstance(), () -> {
                UUID uuid = EslestirmeManager.koduKontrolEt(kod);
                if (uuid == null) {
                    event.getHook().sendMessage("❌ Geçersiz kod.").queue();
                    return;
                }

                if (EslestirmeManager.eslesmeVar(uuid)) {
                    event.getHook().sendMessage("❌ Bu hesap zaten eşleşmiş.").queue();
                    return;
                }

                boolean basarili = EslestirmeManager.eslestir(uuid, event.getUser().getId());
                if (!basarili) {
                    event.getHook().sendMessage("❌ Bu Discord hesabı zaten başka bir Minecraft hesabıyla eşleşmiş veya onay bekliyor.").queue();
                    return;
                }

                event.getHook().sendMessage("✅ Kod doğru! Şimdi Minecraft'ta `/hesapeşle onayla` yazarak eşleşmeyi tamamla.").queue();
            });
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
            System.out.println("HesapEşleme: Kullanıcı bulunamadı veya nickname değiştirilemedi.");
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
        }, error -> System.out.println("HesapEşleme: Kullanıcı bulunamadı veya rol atanamadı."));
    }


    public void shutdown() {
        if (jda != null) jda.shutdownNow();
        if (scheduler != null) scheduler.shutdownNow();
    }

    public JDA getJda() {
        return jda;
    }
}
