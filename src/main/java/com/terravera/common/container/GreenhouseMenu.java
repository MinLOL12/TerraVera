/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.container;

import com.terravera.common.greenhouse.GreenhouseBlock;
import com.terravera.common.greenhouse.GreenhouseBlockEntity;
import com.terravera.common.greenhouse.GreenhouseClimate;
import com.terravera.common.greenhouse.GreenhouseTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Read/write control panel for placed greenhouse blocks.
 * <p>
 * The menu syncs live climate values to the client and sends button clicks back to the server for the few controls
 * the player can operate directly: vents, irrigation, and modern powered heating/cooling.
 */
public class GreenhouseMenu extends AbstractContainerMenu
{
    public static final int TOGGLE_VENT = 0;
    public static final int TOGGLE_IRRIGATION = 1;
    public static final int TOGGLE_HEATING = 2;
    public static final int TOGGLE_COOLING = 3;

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    private final DataSlot tier = DataSlot.standalone();
    private final DataSlot temperatureTenths = DataSlot.standalone();
    private final DataSlot humidityPercent = DataSlot.standalone();
    private final DataSlot soilMoisturePercent = DataSlot.standalone();
    private final DataSlot growthPercent = DataSlot.standalone();
    private final DataSlot sunlightPercent = DataSlot.standalone();
    private final DataSlot glassPercent = DataSlot.standalone();
    private final DataSlot plants = DataSlot.standalone();
    private final DataSlot capacity = DataSlot.standalone();
    private final DataSlot flags = DataSlot.standalone();

    public GreenhouseMenu(int id, Inventory inv, BlockPos pos)
    {
        super(TerraVeraContainers.GREENHOUSE.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        sync(inv.player.level());
        addDataSlot(tier);
        addDataSlot(temperatureTenths);
        addDataSlot(humidityPercent);
        addDataSlot(soilMoisturePercent);
        addDataSlot(growthPercent);
        addDataSlot(sunlightPercent);
        addDataSlot(glassPercent);
        addDataSlot(plants);
        addDataSlot(capacity);
        addDataSlot(flags);

        addPlayerSlots(inv);
    }

    public static GreenhouseMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf)
    {
        return new GreenhouseMenu(id, inv, buf.readBlockPos());
    }

    @Override
    public void broadcastChanges()
    {
        access.execute((level, blockPos) -> sync(level));
        super.broadcastChanges();
    }

    private void sync(Level level)
    {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GreenhouseBlockEntity greenhouse)) return;

        GreenhouseClimate climate = greenhouse.climate();
        tier.set(climate.tier());
        temperatureTenths.set(Math.round(climate.temperatureC() * 10.0f));
        humidityPercent.set(Math.round(climate.humidity() * 100.0f));
        soilMoisturePercent.set(Math.round(climate.soilMoisture() * 100.0f));
        growthPercent.set(Math.round(climate.growthModifier() * 100.0f));
        sunlightPercent.set(Math.round(greenhouse.sunlightExposure() * 100.0f));
        glassPercent.set(Math.round(climate.glassCoverage() * 100.0f));
        plants.set(greenhouse.plantCount());
        capacity.set(greenhouse.trayCapacity());

        int bitFlags = 0;
        if (greenhouse.ventilationOpen()) bitFlags |= 1;
        if (climate.irrigationActive()) bitFlags |= 2;
        if (climate.heatingOn()) bitFlags |= 4;
        if (climate.coolingOn()) bitFlags |= 8;
        flags.set(bitFlags);
    }

    public int tier() { return tier.get(); }
    public String tierName() { return GreenhouseTier.byLevel(tier()).id(); }
    public float temperatureC() { return temperatureTenths.get() / 10.0f; }
    public int humidityPercent() { return humidityPercent.get(); }
    public int soilMoisturePercent() { return soilMoisturePercent.get(); }
    public int growthPercent() { return growthPercent.get(); }
    public int sunlightPercent() { return sunlightPercent.get(); }
    public int glassPercent() { return glassPercent.get(); }
    public int plants() { return plants.get(); }
    public int capacity() { return capacity.get(); }
    public boolean ventOpen() { return (flags.get() & 1) != 0; }
    public boolean irrigationActive() { return (flags.get() & 2) != 0; }
    public boolean heatingOn() { return (flags.get() & 4) != 0; }
    public boolean coolingOn() { return (flags.get() & 8) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (!(player.level().getBlockEntity(pos) instanceof GreenhouseBlockEntity greenhouse)) return false;
        if (!(player.level().getBlockState(pos).getBlock() instanceof GreenhouseBlock)) return false;

        GreenhouseTier greenhouseTier = GreenhouseBlock.tierFromState(player.level().getBlockState(pos));
        switch (id)
        {
            case TOGGLE_VENT -> greenhouse.setVentilationOpen(!greenhouse.ventilationOpen());
            case TOGGLE_IRRIGATION -> greenhouse.setIrrigation(!greenhouse.climate().irrigationActive());
            case TOGGLE_HEATING ->
            {
                if (!greenhouseTier.supportsAutomation())
                {
                    player.displayClientMessage(Component.translatable("terravera.greenhouse.automation_required"), true);
                    return false;
                }
                greenhouse.setHeating(!greenhouse.climate().heatingOn());
            }
            case TOGGLE_COOLING ->
            {
                if (!greenhouseTier.supportsAutomation())
                {
                    player.displayClientMessage(Component.translatable("terravera.greenhouse.automation_required"), true);
                    return false;
                }
                greenhouse.setCooling(!greenhouse.climate().coolingOn());
            }
            default -> { return false; }
        }
        sync(player.level());
        return true;
    }

    private void addPlayerSlots(Inventory inv)
    {
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(inv, col, 8 + col * 18, 170));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return access.evaluate((level, blockPos) ->
            level.getBlockState(blockPos).getBlock() instanceof GreenhouseBlock
                && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D,
            true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
