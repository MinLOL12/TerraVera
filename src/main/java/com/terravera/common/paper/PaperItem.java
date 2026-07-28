package com.terravera.common.paper;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.blocks.TerraVeraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A writable sheet of paper. Right-click in air opens the drawing / writing screen.
 * Right-click a wall while holding tape or glue in offhand consumes adhesive and posts the sheet.
 *
 * Real paper in TFC is plant_fiber_cloth – a mat of beaten bast fibers, screened and dried.
 * This item represents a finished sheet ready for ink. It holds PaperContent.
 */
public class PaperItem extends Item
{
    public PaperItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        // Writing requires a quill and ink somewhere in inventory (real dependency)
        if (!player.isCreative())
        {
            boolean hasQuill = false;
            boolean hasInk = false;
            // Check both hands and inventory
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(com.terravera.common.items.TerraVeraItems.QUILL.get())) hasQuill = true;
                if (s.is(com.terravera.common.items.TerraVeraItems.IRON_GALL_INK.get()) || s.is(com.terravera.common.items.TerraVeraItems.CHARCOAL_INK.get())) hasInk = true;
            }
            // Also check hands (inventory includes them but be explicit)
            if (stack.is(com.terravera.common.items.TerraVeraItems.QUILL.get())) hasQuill = true;

            if (!hasQuill || !hasInk)
            {
                if (level.isClientSide())
                {
                    player.displayClientMessage(Component.translatable("terravera.paper.need_ink"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }

        if (level.isClientSide())
        {
            // Open screen client-side. The screen will send SavePaperPayload back to server.
            com.terravera.client.paper.PaperDrawingScreen.open(stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx)
    {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        Player player = ctx.getPlayer();
        ItemStack paperStack = ctx.getItemInHand();
        if (player == null) return InteractionResult.PASS;

        // Only allow posting on vertical faces
        if (face == Direction.UP || face == Direction.DOWN) return InteractionResult.PASS;

        BlockPos placeAt = pos.relative(face);
        if (!level.getBlockState(placeAt).canBeReplaced()) return InteractionResult.PASS;

        // Need adhesive in other hand or in inventory
        InteractionHand otherHand = ctx.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack adhesive = player.getItemInHand(otherHand);
        boolean hasAdhesive = isAdhesive(adhesive);
        if (!hasAdhesive)
        {
            // Search inventory for any adhesive (tape or glue)
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            {
                ItemStack s = player.getInventory().getItem(i);
                if (isAdhesive(s)) { adhesive = s; hasAdhesive = true; break; }
            }
        }
        if (!hasAdhesive)
        {
            if (level.isClientSide())
            {
                player.displayClientMessage(Component.translatable("terravera.paper.need_tape"), true);
            }
            return InteractionResult.FAIL;
        }

        BlockState state = TerraVeraBlocks.POSTED_PAPER.get().defaultBlockState()
            .setValue(com.terravera.common.blocks.PostedPaperBlock.FACING, face);

        if (!state.canSurvive(level, placeAt)) return InteractionResult.FAIL;

        if (!level.isClientSide())
        {
            level.setBlock(placeAt, state, 3);
            if (level.getBlockEntity(placeAt) instanceof com.terravera.common.blocks.PostedPaperBlockEntity be)
            {
                PaperContent content = paperStack.get(TerraVeraDataComponents.PAPER_CONTENT.get());
                if (content == null) content = PaperContent.EMPTY;
                be.setContent(content);
                be.setChanged();
                level.sendBlockUpdated(placeAt, state, state, 3);
            }
            // consume paper and one adhesive
            if (!player.isCreative())
            {
                paperStack.shrink(1);
                adhesive.shrink(1);
                if (adhesive.isEmpty() && player.getItemInHand(otherHand) == adhesive)
                {
                    // handled
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static boolean isAdhesive(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        // Tags defined in data: terravera:adhesives, terravera:tape
        // Fallback to item id check for safety
        return stack.is(com.terravera.common.TerraVeraTags.Items.ADHESIVES) || stack.is(com.terravera.common.TerraVeraTags.Items.TAPE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        PaperContent content = stack.get(TerraVeraDataComponents.PAPER_CONTENT.get());
        if (content != null && !content.isEmpty())
        {
            if (!content.text().isBlank())
            {
                String preview = content.text().length() > 30 ? content.text().substring(0, 30) + "..." : content.text();
                tooltip.add(Component.literal("\"" + preview + "\"").withStyle(net.minecraft.ChatFormatting.ITALIC).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (!content.strokes().isEmpty())
            {
                tooltip.add(Component.translatable("terravera.paper.has_drawing", content.strokes().size()).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
        else
        {
            tooltip.add(Component.translatable("terravera.paper.blank").withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip.add(Component.translatable("terravera.paper.hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
