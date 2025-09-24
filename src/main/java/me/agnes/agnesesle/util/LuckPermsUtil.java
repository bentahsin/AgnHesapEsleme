package me.agnes.agnesesle.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsUtil {
    private final LuckPerms luckPerms;

    public LuckPermsUtil(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public boolean isPlayerInGroup(UUID uuid, String groupName) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
            // Kullanıcı yoksa, yükle (senkron olmayan, .get() ile bekleniyor)
            try {
                user = luckPerms.getUserManager().loadUser(uuid).get();
            } catch (Exception e) {
                e.printStackTrace();
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
                e.printStackTrace();
                return null;
            }
        }
        return user.getPrimaryGroup();


    }



}
