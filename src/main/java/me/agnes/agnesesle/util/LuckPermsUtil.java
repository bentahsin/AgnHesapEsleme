package me.agnes.agnesesle.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LuckPermsUtil {
    private final Logger logger;

    private final LoadingCache<UUID, String> userGroupCache;

    public LuckPermsUtil(LuckPerms luckPerms, Logger logger) {
        this.logger = logger;
        this.userGroupCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(uuid -> {
                    logger.info(uuid + " için LuckPerms verisi cache'leniyor...");
                    User user = luckPerms.getUserManager().loadUser(uuid).join();
                    return user.getPrimaryGroup();
                });
    }

    public String getPrimaryGroup(UUID uuid) {
        try {
            return userGroupCache.get(uuid);
        } catch (Exception e) {
            logger.warning("LuckPerms'ten birincil grup alınamadı: " + uuid);
            logger.warning(e.getMessage());
            return null;
        }
    }
}