package me.agnes.agnesesle.listener;

import me.agnes.agnesesle.AgnesEsle;
import me.agnes.agnesesle.data.EslestirmeManager;
import me.agnes.agnesesle.discord.DiscordBot;
import me.agnes.agnesesle.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class PlayerLoginListener implements Listener {

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        String currentIP = event.getAddress().getHostAddress();

        if (EslestirmeManager.eslesmeVar(playerUUID) && EslestirmeManager.isIkiFAOpen(playerUUID)) {
            String savedIP = EslestirmeManager.getKayitliIP(playerUUID);

            if (savedIP == null) {
                EslestirmeManager.setKayitliIP(playerUUID, currentIP);
                return;
            }

            if (EslestirmeManager.ipDegisti(playerUUID, currentIP)) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, MessageUtil.getMessage("kick-2fa-ip-changed"));

                DiscordBot bot = AgnesEsle.getInstance().getDiscordBot();
                String discordId = EslestirmeManager.getDiscordId(playerUUID);

                if (discordId != null && bot != null) {
                    bot.send2FAConfirmationMessage(discordId, event.getPlayer().getName(), currentIP);
                }
            }
        }
    }
}