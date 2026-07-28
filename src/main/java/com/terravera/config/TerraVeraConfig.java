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

        // ----- Farming, soil, seeds, greenhouses, and crop disease ----------------------------------------

        /** Master switch for the farming overhaul: soil preparation, seed quality, crop disease, greenhouses. */
        public final Supplier<Boolean> enableFarming;
        /** Whether natural material quality affects crafting results. */
        public final Supplier<Boolean> enableMaterialQuality;
        // ----- Butchering ------------------------------------------------------------------------------------

        /** Master switch for the butchering system. If false, animals drop their normal loot. */
        public final Supplier<Boolean> enableButchery;
        /** Whether carcasses lose condition over time and with heat. */
        public final Supplier<Boolean> enableCarcassFreshness;
        /** Multiplier on how fast a carcass spoils. Lower means longer to get it processed. */
        public final Supplier<Double> carcassSpoilageMultiplier;
        /** Whether crops can develop fungal infections, pest problems, and nutrient deficiencies. */
        public final Supplier<Boolean> enableCropDisease;
        /** Global multiplier on crop disease pressure. Lower means diseases develop more slowly. */
        public final Supplier<Double> cropDiseaseMultiplier;
        /** Whether greenhouses simulate interior temperature and humidity. */
        public final Supplier<Boolean> enableGreenhouseClimate;
        /** Whether modern greenhouses can automatically regulate ventilation and climate. */
        public final Supplier<Boolean> enableGreenhouseAutomation;
        /** Multiplier on how quickly soil quality decays. Lower means prepared soil lasts longer. */
        public final Supplier<Double> soilDecayMultiplier;
        /** Whether the hygiene system is active. */
        public final Supplier<Boolean> enableHygiene;
        /** Multiplier on how fast hygiene decays from work and filth. */
        public final Supplier<Double> hygieneDecayMultiplier;
        /** Whether item tooltips show water quality and treatment state. */
        public final Supplier<Boolean> showWaterTooltip;
        /** Whether player-built masonry, roofs, foundations, and support beams are checked for structural support. */
        public final Supplier<Boolean> enableStructuralIntegrity;
        /** Whether eating undercooked meat can transmit parasites. */
        public final Supplier<Boolean> enableFoodborneIllness;
        /** Whether dirty wounds can become infected. */
        public final Supplier<Boolean> enableWoundInfection;

        // ----- Body temperature -----------------------------------------------------------------------------

        /** Master switch for the body temperature system. */
        public final Supplier<Boolean> enableBodyTemperature;
        /** Multiplier on how fast core temperature responds to an imbalance. Lower is more forgiving. */
        public final Supplier<Double> temperatureRateMultiplier;
        /** Whether the extreme hypothermia and heat stroke bands can do direct damage at all. */
        public final Supplier<Boolean> enableTemperatureDamage;
        /** Whether symptom messages appear on the action bar. Turn off for a completely unprompted experience. */
        public final Supplier<Boolean> showTemperatureSymptoms;
        /** Whether rain and water soak clothing, and whether soaked clothing loses insulation. */
        public final Supplier<Boolean> enableWetClothing;

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

            builder.push("building");
            enableStructuralIntegrity = builder
                .comment("If true, player-built masonry, roofs, foundations, and support beams need a valid load path. Invalid work has a short grace period so it can be braced before it fails.")
                .define("enableStructuralIntegrity", true);
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

            builder.push("temperature");
            enableBodyTemperature = builder
                .comment("If true, the player has a core body temperature affected by climate, weather, wind, water, clothing, shelter, fire, and activity. Symptoms appear gradually; nothing does instant damage.")
                .define("enableBodyTemperature", true);
            temperatureRateMultiplier = builder
                .comment("Multiplier on how fast core temperature moves. Below 1.0 gives you longer to react; above 1.0 makes exposure more urgent.")
                .defineInRange("temperatureRateMultiplier", 1.0, 0.1, 5.0);
            enableTemperatureDamage = builder
                .comment("If true, the extreme hypothermia and heat stroke bands do slow direct damage. Even when false, the impairment effects still apply.")
                .define("enableTemperatureDamage", true);
            showTemperatureSymptoms = builder
                .comment("If true, the player is told what their body is doing ('Your hands feel stiff.') on the action bar. There is deliberately never a numeric readout or a cold bar.")
                .define("showTemperatureSymptoms", true);
            enableWetClothing = builder
                .comment("If true, rain and water soak clothing, soaked clothing loses most of its insulation, and clothes have to be dried by a fire.")
                .define("enableWetClothing", true);
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

            builder.push("butchery");
            enableButchery = builder
                .comment("If true, killed animals drop a carcass that must be skinned, drawn, and broken down by hand instead of dropping finished meat and hides. Products feed TFC's leather, food, and bone chains.")
                .define("enableButchery", true);
            enableCarcassFreshness = builder
                .comment("If true, a carcass passes from fresh through cool, aging, and spoiling to rotten. Warm weather is much faster than cold. Organs are lost first, then the meat; hide and bone last longest.")
                .define("enableCarcassFreshness", true);
            carcassSpoilageMultiplier = builder
                .comment("Multiplier on carcass spoilage speed. 0.5 gives twice as long to process an animal; 2.0 halves it.")
                .defineInRange("carcassSpoilageMultiplier", 1.0, 0.0, 5.0);
            builder.pop();

            builder.push("farming");
            enableFarming = builder
                .comment("Master switch for the farming system: soil preparation, seed quality, crop disease, and greenhouse climate. If false, all farming runs on vanilla rules.")
                .define("enableFarming", true);
            enableMaterialQuality = builder
                .comment("If true, natural materials vary in quality. Straight sticks make better shafts, long fibres make stronger cordage, dry wood burns better than wet wood.")
                .define("enableMaterialQuality", true);
            enableCropDisease = builder
                .comment("If true, unhealthy fields develop fungal infections, insect problems, or nutrient deficiencies. Good sanitation, crop rotation, and healthy soil reduce risk.")
                .define("enableCropDisease", true);
            cropDiseaseMultiplier = builder
                .comment("Global multiplier on crop disease pressure. 0.5 halves how fast diseases develop; 2.0 doubles it.")
                .defineInRange("cropDiseaseMultiplier", 1.0, 0.0, 5.0);
            enableGreenhouseClimate = builder
                .comment("If true, greenhouses simulate interior temperature and humidity. Temperature depends on glass coverage, orientation, ventilation, insulation, and sunlight.")
                .define("enableGreenhouseClimate", true);
            enableGreenhouseAutomation = builder
                .comment("If true, modern greenhouses can automatically regulate ventilation and climate when powered.")
                .define("enableGreenhouseAutomation", true);
            soilDecayMultiplier = builder
                .comment("Multiplier on how quickly prepared soil quality decays. Lower means cleared, loosened, and fertile soil lasts longer.")
                .defineInRange("soilDecayMultiplier", 1.0, 0.0, 5.0);
            builder.pop();
        }

        public ModConfigSpec spec()
        {
            return spec;
        }
    }

    private TerraVeraConfig() {}
}
