package com.terravera.common.blocks;

import com.terravera.common.paper.PaperContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.Tag;

/**
 * Stores the drawing / writing displayed on a posted paper.
 */
public class PostedPaperBlockEntity extends BlockEntity
{
    private PaperContent content = PaperContent.EMPTY;

    public PostedPaperBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.POSTED_PAPER.get(), pos, state);
    }

    public PaperContent getContent() { return content; }

    public void setContent(PaperContent content)
    {
        this.content = content == null ? PaperContent.EMPTY : content;
        setChanged();
        if (level != null && !level.isClientSide())
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        try
        {
            var encoded = PaperContent.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, content).getOrThrow();
            if (encoded instanceof CompoundTag ct)
            {
                tag.put("paper_content", ct);
            }
            else
            {
                tag.putString("paper_text", content.text());
            }
        }
        catch (Exception e)
        {
            tag.putString("paper_text", content.text());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        if (tag.contains("paper_content", Tag.TAG_COMPOUND))
        {
            try
            {
                CompoundTag ct = tag.getCompound("paper_content");
                var result = PaperContent.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, ct).result();
                if (result.isPresent())
                {
                    content = result.get();
                    return;
                }
            }
            catch (Exception ignored) {}
        }
        String text = tag.contains("paper_text") ? tag.getString("paper_text") : "";
        content = new PaperContent(text, java.util.List.of());
    }
}
