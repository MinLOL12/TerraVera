/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Works out what actually comes off a carcass at a given stage.
 * <p>
 * This is pure arithmetic with no Minecraft types in it, so the balance can be tested directly and so that the
 * mechanic is legible: three inputs (the tool, the butcher, the state of the animal) produce two outputs (what you
 * get, and what you destroyed getting it). Nothing here is random beyond a single small variance term, because a
 * system whose whole point is that skill and equipment matter should not be dominated by dice.
 */
public final class ButcheryYield
{
    /** Product identifiers. These map one-to-one onto the items registered in {@code TerraVeraItems}. */
    public static final String MEAT_SHOULDER = "meat_shoulder";
    public static final String MEAT_RIBS = "meat_ribs";
    public static final String MEAT_LOIN = "meat_loin";
    public static final String MEAT_LEG = "meat_leg";
    public static final String TRIM_MEAT = "trim_meat";
    public static final String BONE = "bone";
    public static final String MARROW_BONE = "marrow_bone";
    public static final String ANIMAL_FAT = "animal_fat";
    public static final String SUET = "suet";
    public static final String SINEW = "sinew";
    public static final String TENDON = "tendon";
    public static final String BLOOD = "blood";
    public static final String HEART = "heart";
    public static final String LIVER = "liver";
    public static final String KIDNEYS = "kidneys";
    public static final String STOMACH = "stomach";
    public static final String HIDE = "hide";
    public static final String FLEECE = "fleece";

    /**
     * The quality with which a single stage was performed, and everything that follows from it.
     *
     * @param products    product id to count, already reduced by waste
     * @param cutQuality  how well this particular cut went, 0..1
     * @param wasted      fraction of this stage's potential yield that was destroyed
     * @param workTicks   how long the stage takes with this tool and this butcher
     */
    public record Result(Map<String, Integer> products, float cutQuality, float wasted, int workTicks)
    {
        public boolean isEmpty() { return products.isEmpty(); }
    }

    /**
     * The single number the whole system turns on.
     * <p>
     * A good tool in unpracticed hands and a poor tool in expert hands both land in the middle, which is the
     * intended shape: neither buying your way past the skill nor grinding past the equipment fully works. The
     * floor of 0.08 means even the worst attempt recovers something rather than producing an unrecoverable
     * dead-end, and the carcass's own prior workmanship caps how well a late stage can possibly go.
     */
    public static float cutQuality(ButcheryTool tool, float proficiency, CarcassData carcass, ButcheryStage stage)
    {
        final float toolTerm = tool.cutQuality();
        final float skillTerm = proficiency;

        // Difficulty scales how much a bad tool or an unpracticed hand costs you. Bleeding is nearly foolproof;
        // drawing the guts without opening them is not.
        final float difficulty = stage.difficulty();
        final float base = 0.35f + 0.40f * toolTerm + 0.35f * skillTerm + (tool.isButchersKnife() ? 0.15f : 0f);
        final float penalty = difficulty * 0.22f * (1f - toolTerm) * (1.3f - skillTerm);

        // A carcass that was already mangled cannot be recovered by careful later work, only partly compensated.
        final float inherited = 0.55f + 0.45f * carcass.workmanship();

        return clamp((base - penalty) * inherited, 0.08f, 1f);
    }

    /**
     * Fraction of this stage's yield destroyed. Beginners waste; dull knives waste more; a spoiling carcass is
     * partly lost before the knife touches it.
     */
    public static float wastedFraction(float cutQuality, Freshness freshness)
    {
        final float knifeWaste = (1f - cutQuality) * 0.55f;
        final float spoilWaste = 1f - freshness.meatYield();
        return clamp(knifeWaste + spoilWaste * 0.25f, 0f, 0.95f);
    }

    /** Work time in ticks: dull tools genuinely take longer, and practice genuinely speeds you up. */
    public static int workTicks(ButcheryTool tool, float proficiency, CarcassSpecies species, ButcheryStage stage)
    {
        final float sizeTerm = 20f + species.carcassMass() * 0.35f;
        final float stageTerm = 0.6f + stage.difficulty() * 0.7f;
        // A saw or cleaver is the wrong tool for skinning and drawing, but it is the right one for going through
        // bone, so it pays for its clumsiness on exactly the stage that needs it.
        final float toolFit = tool.isSaw() && stage == ButcheryStage.RENDERED ? 0.65f : 1f;
        final float speed = tool.workTimeMultiplier() * (1.35f - 0.45f * proficiency) * toolFit * (tool.isButchersKnife() ? 0.70f : 1f);
        return Math.max(10, Math.round(sizeTerm * stageTerm * speed));
    }

    /**
     * Perform one stage and report what came off.
     *
     * @param carcass     the carcass being worked, at the stage about to be performed
     * @param tool        the blade in hand
     * @param proficiency the butcher's practical proficiency, 0..1
     * @param freshness   the carcass's current freshness band
     * @param variance    a value in [-1, 1] for the small random element; pass 0 for deterministic results
     */
    public static Result perform(CarcassData carcass, ButcheryTool tool, float proficiency,
                                 Freshness freshness, float variance)
    {
        final ButcheryStage stage = carcass.stage();
        final CarcassSpecies species = carcass.species();
        final float quality = clamp(cutQuality(tool, proficiency, carcass, stage) + variance * 0.05f, 0.05f, 1f);
        final float wasted = wastedFraction(quality, freshness);
        final float keep = 1f - wasted;

        final Map<String, Integer> products = new LinkedHashMap<>();
        final float mass = species.carcassMass();

        switch (stage)
        {
            case BLED ->
            {
                // Blood settles and clots quickly; only a fresh, promptly bled animal gives usable blood.
                final int blood = scaled(mass * 0.03f, keep * freshness.organYield());
                if (blood > 0) products.put(BLOOD, blood);
            }
            case SKINNED ->
            {
                // Hide is all or nothing per piece: a nicked hide is a smaller hide, not a worse one.
                final int pieces = scaled(species.hidePieces(), keep * freshness.durableYield());
                if (pieces > 0) products.put(species.woolBearing() ? FLEECE : HIDE, pieces);
            }
            case EVISCERATED ->
            {
                final float organKeep = keep * freshness.organYield();
                addIf(products, HEART, scaled(mass >= 20f ? 1f : 0.6f, organKeep));
                addIf(products, LIVER, scaled(mass >= 20f ? 2f : 1f, organKeep));
                addIf(products, KIDNEYS, scaled(mass >= 20f ? 2f : 1f, organKeep));
                addIf(products, STOMACH, scaled(mass >= 20f ? 1f : 0f, organKeep));
            }
            case PRIMALS ->
            {
                final float meatKeep = keep * freshness.meatYield();
                final int primals = species.primalCount();
                // Primals come off in a fixed anatomical order, so a small animal gives a leg and nothing else
                // rather than a random subset. Trim is the offcuts - always some, more if the work was rough.
                final String[] order = { MEAT_LEG, MEAT_SHOULDER, MEAT_LOIN, MEAT_RIBS, MEAT_LEG, MEAT_SHOULDER };
                for (int i = 0; i < primals; i++)
                {
                    // Each primal is worth more on a bigger animal. The mass divisor is small enough that the
                    // difference between a well-timed and a late butchering survives integer rounding rather than
                    // being flattened to "one cut either way".
                    final int count = scaled(1.5f + mass / 45f, meatKeep);
                    if (count > 0) products.merge(order[i], count, Integer::sum);
                }
                final int trim = scaled(mass * 0.02f + wasted * mass * 0.03f, freshness.meatYield());
                addIf(products, TRIM_MEAT, trim);
            }
            case RENDERED ->
            {
                final float durable = keep * freshness.durableYield();
                final float fatKeep = keep * freshness.meatYield();
                addIf(products, ANIMAL_FAT, scaled(mass * species.fatness() * 0.25f, fatKeep));
                // Suet is the hard kidney fat specifically, so it only exists on animals big enough to have it.
                addIf(products, SUET, mass >= 20f ? scaled(mass * species.fatness() * 0.08f, fatKeep) : 0);
                addIf(products, BONE, scaled(1f + mass * 0.05f, durable));
                addIf(products, MARROW_BONE, scaled(mass >= 20f ? mass * 0.012f : 0f, durable));
                addIf(products, SINEW, scaled(mass * 0.02f, durable));
                addIf(products, TENDON, scaled(mass * 0.015f, durable));
            }
            default -> { }
        }

        return new Result(products, quality, wasted, workTicks(tool, proficiency, species, stage));
    }

    /**
     * Skill awarded for a stage. Difficult stages teach more, and a stage botched badly still teaches something -
     * arguably more than one that went perfectly.
     */
    public static float experienceFor(ButcheryStage stage, CarcassSpecies species, float cutQuality)
    {
        final float sizeTerm = 0.5f + Math.min(2.0f, species.carcassMass() / 80f);
        return (0.6f + stage.difficulty()) * sizeTerm * (0.7f + 0.6f * (1f - cutQuality));
    }

    private static void addIf(Map<String, Integer> products, String key, int count)
    {
        if (count > 0) products.put(key, count);
    }

    /** Round a fractional amount down, but never let a positive expected yield round to literally nothing. */
    private static int scaled(float amount, float multiplier)
    {
        final float value = amount * multiplier;
        if (value <= 0f) return 0;
        final int floor = (int) value;
        return floor > 0 ? floor : (value >= 0.5f ? 1 : 0);
    }

    private static float clamp(float value, float min, float max)
    {
        return value < min ? min : value > max ? max : value;
    }

    private ButcheryYield() {}
}
