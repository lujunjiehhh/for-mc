package ltd.opens.mg.mc.network.payloads;

import ltd.opens.mg.mc.MaingraphforMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RenameBlueprintPayload(String oldName, String newName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RenameBlueprintPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse(MaingraphforMC.MODID + ":rename_blueprint"));
    
    public static final StreamCodec<FriendlyByteBuf, RenameBlueprintPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RenameBlueprintPayload::oldName,
            ByteBufCodecs.STRING_UTF8,
            RenameBlueprintPayload::newName,
            RenameBlueprintPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
