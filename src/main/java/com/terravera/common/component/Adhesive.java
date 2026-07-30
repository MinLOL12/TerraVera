package com.terravera.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Physical behaviour of a prepared natural adhesive. Values are intentionally material-specific:
 * pitch survives damp weather, protein glues make very rigid dry joints, and casein is strong but brittle. */
public record Adhesive(float strength, float moistureResistance, float flexibility, String application)
{
    public static final Codec<Adhesive> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("strength").forGetter(Adhesive::strength),
        Codec.FLOAT.fieldOf("moisture_resistance").forGetter(Adhesive::moistureResistance),
        Codec.FLOAT.fieldOf("flexibility").forGetter(Adhesive::flexibility),
        Codec.STRING.fieldOf("application").forGetter(Adhesive::application)
    ).apply(i, Adhesive::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Adhesive> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Adhesive::strength,
        ByteBufCodecs.FLOAT, Adhesive::moistureResistance,
        ByteBufCodecs.FLOAT, Adhesive::flexibility,
        ByteBufCodecs.stringUtf8(48), Adhesive::application,
        Adhesive::new);
}
