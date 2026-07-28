package com.terravera.common.network;

import com.terravera.TerraVera;
import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.blocks.PostedPaperBlockEntity;
import com.terravera.common.paper.PaperContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network handling for paper editing.
 */
public final class TerraVeraNetwork
{
    // ----- Save paper in hand -----
    public record SavePaperPayload(PaperContent content, InteractionHand hand) implements CustomPacketPayload
    {
        public static final Type<SavePaperPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, "save_paper"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SavePaperPayload> STREAM_CODEC = StreamCodec.composite(
            PaperContent.STREAM_CODEC, SavePaperPayload::content,
            ByteBufCodecs.BOOL.map(
                (Boolean b) -> b ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                h -> h == InteractionHand.MAIN_HAND
            ), SavePaperPayload::hand,
            SavePaperPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(SavePaperPayload payload, IPayloadContext ctx)
        {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    ItemStack stack = player.getItemInHand(payload.hand);
                    if (stack.isEmpty()) return;
                    // Only allow if it's a paper item (has PAPER_CONTENT component support)
                    if (!stack.has(TerraVeraDataComponents.PAPER_CONTENT.get()) && !(stack.getItem() instanceof com.terravera.common.paper.PaperItem))
                    {
                        // Still allow if it's plant_fiber_cloth acting as paper
                        if (!stack.is(com.terravera.common.items.TerraVeraItems.PAPER_SHEET.get()) &&
                            !stack.is(com.terravera.common.items.TerraVeraItems.WRITTEN_PAPER.get()) &&
                            !stack.is(com.terravera.common.items.TerraVeraItems.PLANT_FIBER_CLOTH.get()))
                            return;
                    }
                    stack.set(TerraVeraDataComponents.PAPER_CONTENT.get(), payload.content);
                    // Consume ink for realism – 1 ink per save if not creative and content not empty
                    if (!player.isCreative() && !payload.content.isEmpty())
                    {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                        {
                            var s = player.getInventory().getItem(i);
                            if (s.is(com.terravera.common.items.TerraVeraItems.IRON_GALL_INK.get()) || s.is(com.terravera.common.items.TerraVeraItems.CHARCOAL_INK.get()))
                            {
                                s.shrink(1);
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    // ----- Save posted paper block -----
    public record SavePostedPaperPayload(BlockPos pos, PaperContent content) implements CustomPacketPayload
    {
        public static final Type<SavePostedPaperPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, "save_posted_paper"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SavePostedPaperPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SavePostedPaperPayload::pos,
            PaperContent.STREAM_CODEC, SavePostedPaperPayload::content,
            SavePostedPaperPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(SavePostedPaperPayload payload, IPayloadContext ctx)
        {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    var level = player.level();
                    if (!level.isLoaded(payload.pos)) return;
                    if (level.getBlockEntity(payload.pos) instanceof PostedPaperBlockEntity be)
                    {
                        // Simple distance check
                        if (player.distanceToSqr(payload.pos.getX()+0.5, payload.pos.getY()+0.5, payload.pos.getZ()+0.5) > 64) return;
                        be.setContent(payload.content);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        var registrar = event.registrar(TerraVera.MOD_ID);
        registrar.playToServer(SavePaperPayload.TYPE, SavePaperPayload.STREAM_CODEC, SavePaperPayload::handle);
        registrar.playToServer(SavePostedPaperPayload.TYPE, SavePostedPaperPayload.STREAM_CODEC, SavePostedPaperPayload::handle);
    }

    private TerraVeraNetwork() {}
}
