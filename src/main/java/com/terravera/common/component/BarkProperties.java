/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Physical properties carried by every harvested sheet of bark.
 *
 * <p>Keeping these values on the stack, rather than deriving them only from the item id, means that drying changes an
 * actual material property. It also leaves room for recipes and integrations to preserve partially dried bark instead
 * of silently treating a wet sheet as tinder.</p>
 */
public record BarkProperties(
    String species,
    float moisture,
    float tannin,
    float flexibility,
    float flammability,
    float thicknessMm
)
{
    public static final float DRY_THRESHOLD = 0.15f;

    public static final Codec<BarkProperties> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("species").forGetter(BarkProperties::species),
        Codec.FLOAT.fieldOf("moisture").forGetter(BarkProperties::moisture),
        Codec.FLOAT.fieldOf("tannin").forGetter(BarkProperties::tannin),
        Codec.FLOAT.fieldOf("flexibility").forGetter(BarkProperties::flexibility),
        Codec.FLOAT.fieldOf("flammability").forGetter(BarkProperties::flammability),
        Codec.FLOAT.fieldOf("thickness_mm").forGetter(BarkProperties::thicknessMm)
    ).apply(i, BarkProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BarkProperties> STREAM_CODEC = StreamCodec.of(
        (buf, bark) -> {
            buf.writeUtf(bark.species);
            buf.writeFloat(bark.moisture);
            buf.writeFloat(bark.tannin);
            buf.writeFloat(bark.flexibility);
            buf.writeFloat(bark.flammability);
            buf.writeFloat(bark.thicknessMm);
        },
        buf -> new BarkProperties(buf.readUtf(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
    );

    public BarkProperties
    {
        moisture = Mth.clamp(moisture, 0f, 1f);
        tannin = Mth.clamp(tannin, 0f, 1f);
        flexibility = Mth.clamp(flexibility, 0f, 1f);
        flammability = Mth.clamp(flammability, 0f, 1f);
        thicknessMm = Math.max(0.1f, thicknessMm);
    }

    public boolean isDry()
    {
        return moisture <= DRY_THRESHOLD;
    }

    public BarkProperties withMoisture(float value)
    {
        return new BarkProperties(species, value, tannin, flexibility, flammability, thicknessMm);
    }

    public BarkProperties dried()
    {
        // Air-dried bark retains a little bound moisture; it is not kiln-dried lumber.
        return withMoisture(0.08f);
    }

    public int percentageMoisture()
    {
        return Math.round(moisture * 100f);
    }
}
