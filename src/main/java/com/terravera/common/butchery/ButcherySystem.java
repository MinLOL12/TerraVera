/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.skill.SkillSystem;
import com.terravera.common.skill.SkillType;
import com.terravera.common.temperature.TemperatureSystem;
import com.terravera.config.TerraVeraConfig;

/**
 * The runtime half of butchering: turning a killed animal into a carcass, and turning a carcass into parts.
 * <p>
 * The design constraint that shapes everything here is that TerraFirmaCraft must keep working. TFC's own hide,
 * meat, and bone drops are what feed its tanning, food preservation, and tool chains, so this system does not
 * bypass them - it inserts a step in front of them. Killing an animal now drops a {@link CarcassItem} instead of a
 * pile of finished goods, and the products that come off that carcass are TFC's own items wherever TFC has one
 * (raw meat, hides, bones) and TerraVera's only where it does not (organs, sinew, suet, blood).
 * <p>
 * That is why {@link #onLivingDrops} suppresses rather than replaces: the original drop list is discarded, but the
 * carcass is sized from the same entity, so a TFC cow still ultimately gives cow-sized quantities of TFC beef.
 */
public final class ButcherySystem
{
    public static void init()
    {
        NeoForge.EVENT_BUS.register(ButcherySystem.class);
    }

    /**
     * Replace an animal's drops with a carcass.
     * <p>
     * Runs at low priority so that other mods have already contributed their drops and we are deciding against the
     * final list. Non-animals, player kills configured off, and creative-mode kills all fall through untouched -
     * this should never be the reason a skeleton stops dropping bones.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event)
    {
        if (!TerraVeraConfig.SERVER.enableButchery.get()) return;

        final LivingEntity entity = event.getEntity();
        final Level level = entity.level();
        if (level.isClientSide()) return;
        if (!(entity instanceof Animal)) return;

        final ItemStack carcass = createCarcass(entity);
        if (carcass.isEmpty()) return;

        // Anything that is not a body part - saddles, wool sheared onto the ground, addon trophies - is kept.
        event.getDrops().removeIf(drop -> isBodyPart(drop.getItem()));
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(level,
            entity.getX(), entity.getY(), entity.getZ(), carcass));
    }

    /** Build a carcass item for a freshly killed animal, sized and dated from the entity itself. */
    public static ItemStack createCarcass(LivingEntity entity)
    {
        final var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        final var box = entity.getBoundingBox();
        final float volume = (float) (box.getXsize() * box.getYsize() * box.getZsize());
        final CarcassSpecies species = CarcassSpecies.fromEntityId(id, volume);

        final ItemStack stack = new ItemStack(TerraVeraItems.CARCASS.get());
        stack.set(TerraVeraDataComponents.CARCASS.get(), new CarcassData(
            species, ButcheryStage.INTACT, entity.level().getGameTime(), 0.5f, 0f));
        return stack;
    }

    /**
     * Perform the next stage of butchering on a carcass.
     *
     * @return true if work was done, false if the player was told why it could not be
     */
    public static boolean butcher(Player player, ItemStack carcassStack, ItemStack toolStack, InteractionHand toolHand)
    {
        final Level level = player.level();
        final CarcassData data = carcassStack.get(TerraVeraDataComponents.CARCASS.get());
        if (data == null) return false;

        final ButcheryStage stage = data.stage();
        if (stage.complete())
        {
            player.displayClientMessage(Component.translatable("terravera.butchery.finished"), true);
            return false;
        }

        final ButcheryTool tool = ButcheryTool.of(toolStack);
        if (!tool.canPerform(stage))
        {
            player.displayClientMessage(Component.translatable("terravera.butchery.need_blade"), true);
            return false;
        }

        // Freshness can be switched off entirely, in which case a carcass is treated as though it were just cooled:
        // the best working state, so disabling the mechanic is a simplification rather than a buff or a nerf.
        final Freshness freshness;
        if (TerraVeraConfig.SERVER.enableCarcassFreshness.get())
        {
            final float ambient = TemperatureSystem.ambientTemperature(level, player.blockPosition());
            final float hours = data.hoursDead(level.getGameTime())
                * TerraVeraConfig.SERVER.carcassSpoilageMultiplier.get().floatValue();
            freshness = Freshness.of(hours, ambient);
        }
        else
        {
            freshness = Freshness.COOL;
        }

        final float proficiency = SkillSystem.proficiency(player, SkillType.BUTCHERY);
        final float variance = level.getRandom().nextFloat() * 2f - 1f;

        final ButcheryYield.Result result = ButcheryYield.perform(data, tool, proficiency, freshness, variance);

        // Hand out the products. An empty result is still progress - a rotten carcass genuinely has no organs left
        // in it, and the player needs to be able to get past that stage to reach the hide and bone underneath.
        int given = 0;
        for (Map.Entry<String, Integer> entry : result.products().entrySet())
        {
            final ItemStack product = ButcheryProducts.stackFor(entry.getKey(), entry.getValue(), data.species());
            if (product.isEmpty()) continue;
            given += product.getCount();
            if (!player.getInventory().add(product))
            {
                player.drop(product, false);
            }
        }

        // Advance the carcass. The stack itself becomes the next stage rather than being consumed, so a
        // half-butchered animal is a real object the player can put down and come back to.
        final CarcassData advanced = data.advanced(result.cutQuality(), result.wasted() * 0.2f);
        carcassStack.set(TerraVeraDataComponents.CARCASS.get(), advanced);

        if (advanced.stage().complete())
        {
            carcassStack.shrink(1);
        }

        // Blade wear scales with the size of the animal: breaking down a bison blunts a knife, gutting a rabbit
        // barely touches it. This is the loop that makes maintaining your tools part of butchering.
        if (!player.isCreative() && tool.isBlade() && !toolStack.isEmpty())
        {
            final int wear = Math.max(1, Math.round(data.species().carcassMass() / 40f));
            final EquipmentSlot slot = toolHand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            toolStack.hurtAndBreak(wear, player, slot);
        }

        SkillSystem.award(player, SkillType.BUTCHERY,
            ButcheryYield.experienceFor(stage, data.species(), result.cutQuality()));

        player.getCooldowns().addCooldown(carcassStack.getItem(), result.workTicks());
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);

        player.displayClientMessage(Component.translatable("terravera.butchery.performed",
            stage.displayName(), given, Math.round(result.wasted() * 100f)), true);
        return true;
    }

    /**
     * Whether a drop is a body part that the carcass now supersedes. Matched by id so that TFC's food, hides, and
     * bones are caught alongside vanilla's, and so that a mod adding {@code somemod:raw_venison} works too.
     */
    private static boolean isBodyPart(ItemStack stack)
    {
        final String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        final String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        if (namespace.equals("terravera")) return false;

        return path.contains("hide") || path.contains("leather") || path.contains("pelt")
            || path.contains("bone") || path.contains("feather")
            || path.contains("beef") || path.contains("pork") || path.contains("mutton")
            || path.contains("chicken") || path.contains("rabbit") || path.contains("venison")
            || path.contains("bison") || path.contains("horse_meat") || path.contains("bear")
            || path.contains("meat") || path.contains("blubber")
            || path.contains("wolf") || path.contains("hyena") || path.contains("gran_feline")
            || path.contains("camelidae") || path.contains("mutton") || path.contains("duck")
            || path.contains("quail") || path.contains("grouse") || path.contains("pheasant")
            || path.contains("turkey") || path.contains("calamari");
    }

    private ButcherySystem() {}
}
