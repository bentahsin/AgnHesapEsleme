package me.agnes.agnesesle.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.UUID;
import java.util.logging.Logger;

public class LuckPermsUtil {
    private final LuckPerms luckPerms;
    private final Logger logger;

    public LuckPermsUtil(LuckPerms luckPerms, Logger logger) {
        this.luckPerms = luckPerms;
        this.logger = logger;
    }

    public boolean isPlayerInGroup(UUID uuid, String groupName) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
            // Kullanıcı yoksa, yükle (senkron olmayan, .get() ile bekleniyor)
            try {
                user = luckPerms.getUserManager().loadUser(uuid).get();
            } catch (Exception e) {
                logger.warning("Failed to load user " + uuid);
                logger.warning(e.getMessage());
                return false;
            }
        }
        

        return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .anyMatch(name -> name.equalsIgnoreCase(groupName));
    }
    public String getPrimaryGroup(UUID uuid) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
            try {
                user = luckPerms.getUserManager().loadUser(uuid).get();
            } catch (Exception e) {
                logger.warning("Failed to load user " + uuid);
                logger.warning(e.getMessage());
                return null;
            }
        }
        return user.getPrimaryGroup();


    }



}
