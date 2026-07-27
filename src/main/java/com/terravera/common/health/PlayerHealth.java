/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Everything TerraVera tracks about one player's health: what they are currently incubating or suffering from, what
 * they are now immune to, and how clean they are.
 * <p>
 * <strong>Hygiene</strong> is the connective tissue of the whole system. It is a single {@code [0, 1]} value that
 * decays as you work, get muddy, butcher animals, and handle waste, and is restored by washing. It does not do anything
 * by itself - it is a multiplier on the infection chance of every food-, wound-, and sanitation-borne illness in the
 * mod. That is what makes "improved hygiene" a real, gradual, unlockable defence rather than a checkbox: a player with
 * a habit of washing before eating catches meaningfully less than one without, long before they can boil water at
 * scale.
 * <p>
 * Stored as a serialised data attachment on the player, so it survives logout and (deliberately) is copied across
 * death - dying does not cure your tapeworm, but it does reset the acute infections you were carrying, which keeps
 * death from being an exploit-free cure while not being absurdly punishing.
 *
 * @param infections  active infections, incubating or symptomatic
 * @param immunities  illness id to the calendar tick at which acquired immunity lapses
 * @param hygiene     cleanliness in {@code [0, 1]}. 1 is freshly washed, 0 is filthy
 * @param lastWashTick the tick the player last washed, used to rate-limit washing
 */
public record PlayerHealth(
    List<Infection> infections,
    Map<ResourceLocation, Long> immunities,
    float hygiene,
    long lastWashTick
) {
    /** A player who has never been ill and has just spawned: clean, healthy, immune to nothing. */
    public static final PlayerHealth EMPTY = new PlayerHealth(List.of(), Map.of(), 1.0f, Long.MIN_VALUE);

    public static final Codec<PlayerHealth> CODEC = RecordCodecBuilder.create(i -> i.group(
        Infection.CODEC.listOf().optionalFieldOf("infections", List.of()).forGetter(PlayerHealth::infections),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("immunities", Map.of()).forGetter(PlayerHealth::immunities),
        Codec.FLOAT.optionalFieldOf("hygiene", 1.0f).forGetter(PlayerHealth::hygiene),
        Codec.LONG.optionalFieldOf("last_wash", Long.MIN_VALUE).forGetter(PlayerHealth::lastWashTick)
    ).apply(i, PlayerHealth::new));

    public PlayerHealth
    {
        infections = List.copyOf(infections);
        immunities = Map.copyOf(immunities);
        hygiene = Mth.clamp(hygiene, 0f, 1f);
    }

    // ----- Infections ---------------------------------------------------------------------------------------

    public boolean hasInfection(ResourceLocation illnessId)
    {
        for (Infection infection : infections)
        {
            if (infection.illnessId().equals(illnessId)) return true;
        }
        return false;
    }

    @Nullable
    public Infection infection(ResourceLocation illnessId)
    {
        for (Infection infection : infections)
        {
            if (infection.illnessId().equals(illnessId)) return infection;
        }
        return null;
    }

    /** @return {@code true} if immunity to {@code illnessId} is still in force at {@code now}. */
    public boolean isImmune(ResourceLocation illnessId, long now)
    {
        final Long until = immunities.get(illnessId);
        return until != null && until > now;
    }

    /**
     * @return {@code true} if this player can currently be infected with {@code illnessId} - not already carrying it,
     * and not immune.
     */
    public boolean isSusceptibleTo(ResourceLocation illnessId, long now)
    {
        return !hasInfection(illnessId) && !isImmune(illnessId, now);
    }

    public PlayerHealth withInfection(Infection infection)
    {
        final List<Infection> next = new ArrayList<>(infections);
        next.removeIf(existing -> existing.illnessId().equals(infection.illnessId()));
        next.add(infection);
        return new PlayerHealth(next, immunities, hygiene, lastWashTick);
    }

    public PlayerHealth withoutInfection(ResourceLocation illnessId)
    {
        final List<Infection> next = new ArrayList<>(infections);
        next.removeIf(existing -> existing.illnessId().equals(illnessId));
        return new PlayerHealth(next, immunities, hygiene, lastWashTick);
    }

    public PlayerHealth withInfections(List<Infection> next)
    {
        return new PlayerHealth(next, immunities, hygiene, lastWashTick);
    }

    /** Grants immunity to an illness until {@code until}. Used on recovery for illnesses that confer it. */
    public PlayerHealth withImmunity(ResourceLocation illnessId, long until)
    {
        final Map<ResourceLocation, Long> next = new HashMap<>(immunities);
        next.put(illnessId, until);
        return new PlayerHealth(infections, next, hygiene, lastWashTick);
    }

    /** Drops immunities that have lapsed, so the map does not grow without bound over a long game. */
    public PlayerHealth prunedImmunities(long now)
    {
        if (immunities.isEmpty()) return this;
        final Map<ResourceLocation, Long> next = new HashMap<>();
        immunities.forEach((id, until) -> {
            if (until > now) next.put(id, until);
        });
        return next.size() == immunities.size() ? this : new PlayerHealth(infections, next, hygiene, lastWashTick);
    }

    // ----- Hygiene ------------------------------------------------------------------------------------------

    public PlayerHealth withHygiene(float value)
    {
        return new PlayerHealth(infections, immunities, Mth.clamp(value, 0f, 1f), lastWashTick);
    }

    public PlayerHealth soiled(float amount)
    {
        return withHygiene(hygiene - amount);
    }

    public PlayerHealth washed(float amount, long tick)
    {
        return new PlayerHealth(infections, immunities, Mth.clamp(hygiene + amount, 0f, 1f), tick);
    }

    /**
     * The infection-chance multiplier from cleanliness. A freshly washed player takes roughly half the risk of a
     * filthy one on any vector that hygiene affects.
     * <p>
     * Deliberately not a cliff: there is no threshold at which you are "clean enough". It slides, so improving hygiene
     * is always worth something.
     */
    public float hygieneRiskMultiplier()
    {
        return Mth.clampedMap(hygiene, 0f, 1f, 1.6f, 0.55f);
    }

    /**
     * What survives death. Acute infections clear - you died, the game gave you a new body - but acquired immunity and
     * a baseline of hygiene carry over, so death is not a strategy for skipping the disease system entirely and also
     * not a total reset of everything you learned.
     */
    public PlayerHealth onDeath()
    {
        return new PlayerHealth(List.of(), immunities, 0.75f, lastWashTick);
    }

    public boolean isHealthy()
    {
        return infections.isEmpty();
    }
}
