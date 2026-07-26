/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.config;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class TerraVeraConfig
{
    public static final ServerConfig SERVER;

    static
    {
        final Pair<ServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = pair.getKey();
        SERVER.spec = pair.getValue();
    }

    public static final class ServerConfig
    {
        private ModConfigSpec spec;

        /** Whether hafting a stone head requires cordage at all. Turning this off restores vanilla TFC hafting. */
        public final Supplier<Boolean> requireCordageForHafting;
        /** Whether plants and grass drop fibre when broken by hand. */
        public final Supplier<Boolean> plantsDropFibre;
        /** Base chance for a grass or plant block to yield fibre when broken. */
        public final Supplier<Double> fibreDropChance;
        /** Extra chance per level of a knife-like tool. Cutting fibre with a blade is much more productive. */
        public final Supplier<Double> fibreKnifeBonus;
        /** Whether stone tool durability is scaled by knapping quality and cordage strength. */
        public final Supplier<Boolean> scaleDurabilityByCraftsmanship;
        /** The lowest multiplier a truly awful head + lashing can produce. */
        public final Supplier<Double> minimumDurabilityMultiplier;
        /** The highest multiplier a masterful head + strong lashing can produce. */
        public final Supplier<Double> maximumDurabilityMultiplier;
        /** How many ticks a bundle of fibre takes to ret in a barrel of water. Default is one in-game day. */
        public final Supplier<Integer> rettingTicks;

        ServerConfig(ModConfigSpec.Builder builder)
        {
            builder.push("hafting");
            requireCordageForHafting = builder
                .comment("If true, stone tool heads must be lashed to a haft with cordage. If false, TFC's vanilla stick-only hafting recipes apply.")
                .define("requireCordageForHafting", true);
            scaleDurabilityByCraftsmanship = builder
                .comment("If true, the durability of a hafted stone tool is scaled by how well the head was knapped and how strong the cordage is.")
                .define("scaleDurabilityByCraftsmanship", true);
            minimumDurabilityMultiplier = builder
                .comment("Durability multiplier for the worst possible head and lashing.")
                .defineInRange("minimumDurabilityMultiplier", 0.45, 0.05, 1.0);
            maximumDurabilityMultiplier = builder
                .comment("Durability multiplier for a masterful head and a strong lashing.")
                .defineInRange("maximumDurabilityMultiplier", 1.35, 1.0, 4.0);
            builder.pop();

            builder.push("fibre");
            plantsDropFibre = builder
                .comment("If true, breaking grass and plants can yield plant fibre.")
                .define("plantsDropFibre", true);
            fibreDropChance = builder
                .comment("Chance for a grass or plant block to drop fibre when broken bare-handed.")
                .defineInRange("fibreDropChance", 0.35, 0.0, 1.0);
            fibreKnifeBonus = builder
                .comment("Additional fibre drop chance when harvesting with a knife or other bladed tool.")
                .defineInRange("fibreKnifeBonus", 0.5, 0.0, 1.0);
            rettingTicks = builder
                .comment("How long a bundle of fibre takes to ret in a sealed barrel of water, in ticks.")
                .defineInRange("rettingTicks", 24000, 1, Integer.MAX_VALUE);
            builder.pop();
        }

        public ModConfigSpec spec()
        {
            return spec;
        }
    }

    private TerraVeraConfig() {}
}
