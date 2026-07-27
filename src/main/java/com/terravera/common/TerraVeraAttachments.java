/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.terravera.TerraVera;
import com.terravera.common.health.PlayerHealth;
import com.terravera.common.skill.PlayerSkills;

/**
 * Data attachments TerraVera puts on the player.
 * <p>
 * TerraFirmaCraft replaces the player's {@code FoodData} wholesale with its own {@code PlayerInfo}, so an addon cannot
 * safely bolt extra state onto that. NeoForge attachments are the clean seam: they serialise with the player, survive
 * logout, and can opt into being copied across death.
 */
public final class TerraVeraAttachments
{
    public static final DeferredRegister<AttachmentType<?>> TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerraVera.MOD_ID);

    /**
     * Illnesses, immunities, and hygiene. Copied on death so that acquired immunity is not thrown away by a fall,
     * while {@link PlayerHealth#onDeath()} still clears the acute infections the player was carrying.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerHealth>> PLAYER_HEALTH =
        register("player_health", () -> AttachmentType
            .builder(() -> PlayerHealth.EMPTY)
            .serialize(PlayerHealth.CODEC)
            .copyOnDeath()
            .build());

    /** Practical knowledge gained by doing work. It is copied on death because knowledge is not a body stat. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSkills>> PLAYER_SKILLS =
        register("player_skills", () -> AttachmentType
            .builder(() -> PlayerSkills.EMPTY)
            .serialize(PlayerSkills.CODEC)
            .copyOnDeath()
            .build());

    private static <T> DeferredHolder<AttachmentType<?>, AttachmentType<T>> register(String name, Supplier<AttachmentType<T>> type)
    {
        return TYPES.register(name, type);
    }

    private TerraVeraAttachments() {}
}
