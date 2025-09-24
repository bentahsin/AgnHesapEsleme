package me.agnes.agnesesle.discord.listener;

import me.agnes.agnesesle.data.EslestirmeManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.UUID;

public class EslestirmeKomutu extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("eşle")) return;

        event.deferReply(true).queue();

        try {
            String kod = event.getOption("kod").getAsString().toUpperCase();
            UUID uuid = EslestirmeManager.koduKontrolEt(kod);

            if (uuid == null) {
                event.getHook().sendMessage("❌ Kod geçersiz.").queue();
                return;
            }

            if (EslestirmeManager.eslesmeVar(uuid)) {
                event.getHook().sendMessage("❌ Bu hesap zaten eşleştirilmiş.").queue();
                return;
            }

            boolean basarili = EslestirmeManager.eslestir(uuid, event.getUser().getId());
            if (!basarili) {
                event.getHook().sendMessage("❌ Bu Discord hesabı zaten başka bir Minecraft hesabı ile eşleşmiş veya onay bekliyor.").queue();
                return;
            }

            event.getHook().sendMessage("✅ Kod doğrulandı! Minecraft'ta `/hesapeşle onayla` yazarak eşleşmeyi tamamla.").queue();

        } catch (Exception e) {
            event.getHook().sendMessage("❌ Bir hata oluştu: " + e.getMessage()).queue();
            e.printStackTrace();
        }
    }
}
