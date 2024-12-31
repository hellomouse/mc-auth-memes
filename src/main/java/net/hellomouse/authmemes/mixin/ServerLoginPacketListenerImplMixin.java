package net.hellomouse.authmemes.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.hellomouse.authmemes.AuthMemes;
import net.hellomouse.authmemes.Config;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.core.UUIDUtil;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow String requestedUsername;
    @Shadow @Final Connection connection;

    @Shadow void startClientVerification(GameProfile pAuthenticatedProfile) {}
    @Shadow public void disconnect(Component pReason) {}

    @Inject(method = "handleHello", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"
    ), cancellable = true)
    public void handleHelloAuth(ServerboundHelloPacket pPacket, CallbackInfo ci) {
        var maybeOfflineEntry = Config.lookupUsername(this.requestedUsername);
        if (maybeOfflineEntry.isEmpty()) {
            return;
        }

        var allowed = false;
        var offlineEntry = maybeOfflineEntry.get();
        var peerAddress = connection.getRemoteAddress();
        for (var allowedSubnet : offlineEntry.allowedIps()) {
            if (allowedSubnet.matches(peerAddress)) {
                LOGGER.info(
                    "using offline mode for connection {}, username {} using subnet match {}",
                    peerAddress,
                    offlineEntry.username(),
                    allowedSubnet
                );
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            ci.cancel();
            this.disconnect(Component.literal("IP address mismatch"));
        } else {
            ci.cancel();
            this.startClientVerification(UUIDUtil.createOfflineProfile(offlineEntry.username()));
        }
    }
}
