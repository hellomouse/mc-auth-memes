package net.hellomouse.authmemes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = AuthMemes.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
        .comment("Global enable for selective auth bypass functionality")
        .define("enable", false);

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> OFFLINE_PLAYERS = BUILDER
        .comment("Offline players list in the form 'username ip[/cidr][,ip[/cidr],...]")
        .defineListAllowEmpty(
            "offline_players",
            Collections.emptyList(),
            () -> "username 10.0.0.1",
            Config::validateOfflinePlayerEntry
        );

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enable = false;
    public static HashMap<String, OfflinePlayer> offlinePlayers = new HashMap<>();

    private static boolean validateOfflinePlayerEntry(final Object obj) {
        if (obj instanceof String entry) {
            try {
                OfflinePlayer.parseConfigLine(entry);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    @SubscribeEvent
    static synchronized void onLoad(final ModConfigEvent event)
    {
        enable = ENABLED.get();
        offlinePlayers.clear();
        for (var entryStr : OFFLINE_PLAYERS.get()) {
            try {
                var entry = OfflinePlayer.parseConfigLine(entryStr);
                var key = entry.username().toLowerCase();
                if (offlinePlayers.containsKey(key)) {
                    LOGGER.warn("duplicate username {}, ignoring", entry.username());
                    continue;
                }
                offlinePlayers.put(key, entry);
                // TODO: might need to update the profile cache
            } catch (IllegalArgumentException e) {
                LOGGER.warn("invalid entry in offline_players, ignoring", e);
            }
        }

        LOGGER.info("configuration reloaded");
    }

    public static synchronized Optional<OfflinePlayer> lookupUsername(String username) {
        if (!enable) {
            return Optional.empty();
        }

        var key = username.toLowerCase();
        var entry = offlinePlayers.get(key);
        if (entry == null) {
            return Optional.empty();
        } else {
            return Optional.of(entry);
        }
    }
}
