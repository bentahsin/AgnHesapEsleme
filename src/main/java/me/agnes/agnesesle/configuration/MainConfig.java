package me.agnes.agnesesle.configuration;

import com.bentahsin.configuration.annotation.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigHeader({
        "##################################################",
        "#                                                #",
        "#             Account Linking Config             #",
        "#                                                #",
        "##################################################"
})
@ConfigVersion(1)
@Backup(enabled = true, onFailure = true, onMigration = true)
public class MainConfig {

    @Comment({
            "==========================================",
            "DYNAMIC COMMAND SYSTEM",
            "You can add multiple aliases by separating commands with '|'.",
            "Example: 'hesapesle|sync|link'",
            "=========================================="
    })
    public CommandSection commands = new CommandSection();

    @Comment("Bot Settings")
    @Validate(notNull = true)
    public String token = "DISCORD_BOT_TOKEN";

    @Comment("Bot Status Messages")
    @ConfigPath("status-messages")
    public List<String> statusMessages = Arrays.asList(
            "ServerName.net",
            "Account Linking System",
            "{aktifkullanici} Active Players"
    );

    @Comment("Channels")
    @ConfigPath("log-channel-id")
    public String logChannelId = "log-channel-id";

    @ConfigPath("log-system")
    public boolean logSystem = true;

    @Comment("Role İd")
    @ConfigPath("admin-role-id")
    public String adminRoleId = "admin-role-id";

    @ConfigPath("verified-role-id")
    public String verifiedRoleId = "verified-role-id";

    @Comment({
            "Role System",
            "Kontrol Edilmesini İstediğiniz Grup İsimlerini Roles Adı Altında Yazınız. (TR)",
            "Write the Group Names you want to be checked under the Roles Name. (EN)"
    })
    public Map<String, String> roles = new HashMap<String, String>() {{
        put("vip", "");
        put("vipplus", "");
        put("mvip", "");
        put("mvipplus", "");
    }};

    @Comment("Information Settings")
    @ConfigPath("information-sent")
    public boolean informationSent = false;

    @ConfigPath("information-channel-id")
    public String informationChannelId = "information-channel-id";

    @Comment("Reward Commands (First Time)")
    public List<String> rewards = Arrays.asList(
            "give %player% minecraft:diamond 5",
            "say Congratulations %player% linked their account!",
            "experience add %player% 100 points"
    );

    @Comment("DailyRewards System")
    @ConfigPath("reward-cooldown")
    @Validate(min = 0)
    public long rewardCooldown = 86400000L;

    @ConfigPath("daily-rewards")
    public List<String> dailyRewards = Arrays.asList(
            "give %player% diamond 1",
            "xp %player% add 100"
    );

    @Comment("Server Settings")
    @ConfigPath("guild-id")
    public String guildId = "";

    @Comment("Language options: tr, en, es, fr, de, zh")
    public String lang = "en";

    public static class CommandSection {
        public String main = "hesapesle|hesapeşle|sync|link";
        public SubCommandSection subs = new SubCommandSection();

        public static class SubCommandSection {
            public String esle = "eşle|esle|link";
            public String onayla = "onayla|confirm";
            public String iptal = "iptal|cancel";
            public String kaldir = "kaldır|kaldir|unlink";
            public String ikifa = "2fa|guvenlik";
            public String liste = "liste|list";
            public String sifirla = "sıfırla|sifirla|reset";
            public String odul = "ödül|odul|reward";
            public String yenile = "yenile|reload";
        }
    }
}