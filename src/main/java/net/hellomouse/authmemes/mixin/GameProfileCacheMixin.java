package net.hellomouse.authmemes.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import net.hellomouse.authmemes.Config;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(GameProfileCache.class)
public class GameProfileCacheMixin {
    @Inject(method = "lookupGameProfile", at = @At("HEAD"), cancellable = true)
    private static void lookupGameProfileHead(GameProfileRepository pProfileRepo, String pName, CallbackInfoReturnable<Optional<GameProfile>> cir) {
        var entry = Config.lookupUsername(pName);
        if (entry.isEmpty()) {
            return;
        }

        var offlineEntry = entry.get();
        // return offline profile if username exists in config
        cir.setReturnValue(Optional.of(UUIDUtil.createOfflineProfile(offlineEntry.username())));
    }
}
