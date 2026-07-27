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

        /** Whether TerraVera's custom stone heads require cordage to be lashed to a haft. */
        public final Supplier<Boolean> requireCordageForHafting;
        /** Whether plants and grass drop fibre when broken by hand. */
        public final Supplier<Boolean> plantsDropFibre;
        /** Base chance for a grass or plant block to yield fibre when broken. */
        public final Supplier<Double> fibreDropChance;
        /** Extra chance per level of a knife-like tool. Cutting fibre with a blade is much more productive. */
        public final Supplier<Double> fibreKnifeBonus;
        /** Mining-speed multiplier for grass pulled out with an empty hand. */
        public final Supplier<Double> handGatheringSpeed;
        /** Whether stone tool durability is scaled by knapping quality, cordage strength, and binding quality. */
        public final Supplier<Boolean> scaleDurabilityByCraftsmanship;
        /** The lowest multiplier a truly awful head + lashing can produce. */
        public final Supplier<Double> minimumDurabilityMultiplier;
        /** The highest multiplier a masterful head + strong lashing can produce. */
        public final Supplier<Double> maximumDurabilityMultiplier;
        /** How many ticks a bundle of fibre takes to ret in a barrel of water. Default is one in-game day. */
        public final Supplier<Integer> rettingTicks;
        /** Whether the knapping screen tells you which property failed, rather than silently producing nothing. */
        public final Supplier<Boolean> showKnappingFeedback;
        /** Whether cordage binding quality (based on length) affects tool speed. */
        public final Supplier<Boolean> applyBindingBonusToSpeed;
        /** Whether cordage binding quality (based on length) affects tool damage. */
        public final Supplier<Boolean> applyBindingBonusToDamage;
        /** Whether food eating time is scaled by the item's TFC size/weight, instead of vanilla's flat 1.6 seconds. */
        public final Supplier<Boolean> scaleFoodEatTimeBySize;
        /** Multiplier applied on top of the size-derived eating duration, to tune overall pacing without retuning every band. */
        public final Supplier<Double> foodEatTimeMultiplier;
        /** Whether food item tooltips show a flavor line (e.g. "Delicious.") derived from the taste system. */
        public final Supplier<Boolean> showFlavorTooltip;

        // ----- Disease, water, and sanitation ---------------------------------------------------------------

        /** Master switch for the whole disease system. */
        public final Supplier<Boolean> enableDisease;
        /** Global multiplier on every infection chance in the mod. */
        public final Supplier<Double> diseaseChanceMultiplier;
        /** Global multiplier on how long illnesses take to incubate and run their course. */
        public final Supplier<Double> diseaseDurationMultiplier;
        /** Whether contagious illnesses can spread between nearby players. */
        public final Supplier<Boolean> enableContagion;
        /** How far, in blocks, a contagious illness can spread by close contact. */
        public final Supplier<Double> contagionRange;
        /** Whether untreated water sources carry disease risk at all. */
        public final Supplier<Boolean> enableWaterContamination;
        /** Multiplier on the contamination of every natural water source. */
        public final Supplier<Double> waterContaminationMultiplier;
        /** Whether the player is warned before drinking from visibly risky water. */
        public final Supplier<Boolean> warnBeforeDrinkingUnsafeWater;
        /** Whether the hygiene system is active. */
        public final Supplier<Boolean> enableHygiene;
        /** Multiplier on how fast hygiene decays from work and filth. */
        public final Supplier<Double> hygieneDecayMultiplier;
        /** Whether item tooltips show water quality and treatment state. */
        public final Supplier<Boolean> showWaterTooltip;
        /** Whether eating undercooked meat can transmit parasites. */
        public final Supplier<Boolean> enableFoodborneIllness;
        /** Whether dirty wounds can become infected. */
        public final Supplier<Boolean> enableWoundInfection;

        ServerConfig(ModConfigSpec.Builder builder)
        {
            builder.push("hafting");
            requireCordageForHafting = builder
                .comment("If true, TerraVera stone heads must be lashed to a haft with cordage. If false, only TerraVera heads may be hafted without it.")
                .define("requireCordageForHafting", true);
            scaleDurabilityByCraftsmanship = builder
                .comment("If true, the durability of a hafted stone tool is scaled by how well the head was knapped, how strong the cordage is, and how good the binding quality is.")
                .define("scaleDurabilityByCraftsmanship", true);
            minimumDurabilityMultiplier = builder
                .comment("Durability multiplier for the worst possible head and lashing.")
                .defineInRange("minimumDurabilityMultiplier", 0.45, 0.05, 1.0);
            maximumDurabilityMultiplier = builder
                .comment("Durability multiplier for a masterful head and a strong lashing.")
                .defineInRange("maximumDurabilityMultiplier", 1.35, 1.0, 4.0);
            applyBindingBonusToSpeed = builder
                .comment("If true, longer cordage (better binding quality) increases tool speed slightly.")
                .define("applyBindingBonusToSpeed", true);
            applyBindingBonusToDamage = builder
                .comment("If true, longer cordage (better binding quality) increases tool damage slightly.")
                .define("applyBindingBonusToDamage", true);
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
            handGatheringSpeed = builder
                .comment("Mining-speed multiplier when pulling grass with an empty hand. Lower values make hand gathering take longer.")
                .defineInRange("handGatheringSpeed", 0.2, 0.01, 1.0);
            rettingTicks = builder
                .comment("How long a bundle of fibre takes to ret in a sealed barrel of water, in ticks.")
                .defineInRange("rettingTicks", 24000, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("knapping");
            showKnappingFeedback = builder
                .comment("If true, the knapping screen explains why the current shape is not usable yet, e.g. 'the tip is too blunt'.")
                .define("showKnappingFeedback", true);
            builder.pop();

            builder.push("food");
            scaleFoodEatTimeBySize = builder
                .comment("If true, how long it takes to eat a food item scales with its TFC size/weight (tiny bites are ~4s, huge portions ~35-50s) instead of vanilla's flat 1.6s.")
                .define("scaleFoodEatTimeBySize", true);
            foodEatTimeMultiplier = builder
                .comment("Overall multiplier on the size-derived eating duration. 1.0 = the documented 4s/10-20s/20-50s bands. Lower to speed all eating up, raise to slow it down further.")
                .defineInRange("foodEatTimeMultiplier", 1.0, 0.1, 5.0);
            showFlavorTooltip = builder
                .comment("If true, food item tooltips show a flavor line (e.g. 'Delicious.') based on the taste system.")
                .define("showFlavorTooltip", true);
            builder.pop();

            builder.push("disease");
            enableDisease = builder
                .comment("Master switch for the disease system. If false, no illness is ever contracted and existing infections stop progressing.")
                .define("enableDisease", true);
            diseaseChanceMultiplier = builder
                .comment("Global multiplier on every infection chance. 0.5 halves how often you get ill, 2.0 doubles it.")
                .defineInRange("diseaseChanceMultiplier", 1.0, 0.0, 10.0);
            diseaseDurationMultiplier = builder
                .comment("Global multiplier on incubation periods and illness durations. Raise for a slower, more drawn out disease game.")
                .defineInRange("diseaseDurationMultiplier", 1.0, 0.1, 10.0);
            enableContagion = builder
                .comment("If true, colds and influenza can spread from an ill player to nearby healthy ones.")
                .define("enableContagion", true);
            contagionRange = builder
                .comment("How close, in blocks, players have to be for a contagious illness to spread between them.")
                .defineInRange("contagionRange", 4.0, 1.0, 32.0);
            enableFoodborneIllness = builder
                .comment("If true, rotten food, undercooked meat, and food handled with filthy hands can transmit illness.")
                .define("enableFoodborneIllness", true);
            enableWoundInfection = builder
                .comment("If true, taking damage while filthy - or from something out of the soil - can cause an infected wound or tetanus.")
                .define("enableWoundInfection", true);
            builder.pop();

            builder.push("water");
            enableWaterContamination = builder
                .comment("If true, natural water sources have varying contamination and drinking untreated water can make you ill.")
                .define("enableWaterContamination", true);
            waterContaminationMultiplier = builder
                .comment("Multiplier on the contamination of every natural water source. Lower makes the world's water safer overall.")
                .defineInRange("waterContaminationMultiplier", 1.0, 0.0, 5.0);
            warnBeforeDrinkingUnsafeWater = builder
                .comment("If true, a warning is shown when you are about to drink visibly risky water, and the first such drink each session asks for confirmation by drinking again.")
                .define("warnBeforeDrinkingUnsafeWater", true);
            showWaterTooltip = builder
                .comment("If true, water containers show the quality and treatment state of the water they hold.")
                .define("showWaterTooltip", true);
            builder.pop();

            builder.push("hygiene");
            enableHygiene = builder
                .comment("If true, the player accumulates grime from work and filth, which raises the chance of food- and wound-borne infection until they wash.")
                .define("enableHygiene", true);
            hygieneDecayMultiplier = builder
                .comment("Multiplier on how fast hygiene is lost. Lower means you stay clean longer.")
                .defineInRange("hygieneDecayMultiplier", 1.0, 0.0, 5.0);
            builder.pop();
        }

        public ModConfigSpec spec()
        {
            return spec;
        }
    }

    private TerraVeraConfig() {}
}
