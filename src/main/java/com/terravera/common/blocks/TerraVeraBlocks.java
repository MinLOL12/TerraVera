/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import com.terravera.common.power.CopperWireBlock;
import com.terravera.common.power.GeneratorBlock;
import com.terravera.common.power.HandCrankBlock;
import com.terravera.common.power.SingleWireBlock;
import com.terravera.common.power.WindTurbineBlock;
import com.terravera.common.power.WireIntersectionBlock;
import com.terravera.common.water.CollectorType;
import com.terravera.common.water.WaterCollectorBlock;
import com.terravera.common.farming.DripIrrigationBlock;
import com.terravera.common.farming.PreparedFarmlandBlock;
import com.terravera.common.greenhouse.GreenhouseBlock;
import com.terravera.common.greenhouse.GreenhouseTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;

public final class TerraVeraBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TerraVera.MOD_ID);

    /**
     * A heavy, flat iron striking surface that can be set down beside a charcoal forge. It is intentionally weaker
     * than a true anvil: it can maintain and correct worn tools, but it does not replace TFC's anvil progression.
     */
    public static final DeferredHolder<Block, WorkplateBlock> WORKPLATE = BLOCKS.register("workplate",
        () -> new WorkplateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(4.5f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** Late-industrial vapor-compression unit; its controller circuit and electrical supply are required separately. */
    public static final DeferredHolder<Block, AirConditionerBlock> AIR_CONDITIONER = BLOCKS.register("air_conditioner",
        () -> new AirConditionerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops().strength(5.0f, 8.0f).sound(SoundType.METAL).noOcclusion()));

    /** Constant-output generator, intended for a fuel/progression pack or a dedicated generator room. */
    public static final DeferredHolder<Block, GeneratorBlock> GENERATOR = BLOCKS.register("generator",
        () -> new GeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
            .strength(4.5f, 7f).sound(SoundType.METAL)));

    /** Right-click to turn it; each full revolution of the handle supplies a short burst of emergency power. */
    public static final DeferredHolder<Block, HandCrankBlock> HAND_CRANK = BLOCKS.register("hand_crank",
        () -> new HandCrankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f, 3f)
            .sound(SoundType.WOOD).noOcclusion()));

    /** Outdoor wind generator. It produces only when its rotor has clear sky above it. */
    public static final DeferredHolder<Block, WindTurbineBlock> WIND_TURBINE = BLOCKS.register("wind_turbine",
        () -> new WindTurbineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
            .strength(3.5f, 5f).sound(SoundType.METAL).noOcclusion()));

    /** A bare single copper conductor. Cheap and thin - 100 W rating, horizontal runs only. */
    public static final DeferredHolder<Block, SingleWireBlock> SINGLE_WIRE = BLOCKS.register("single_wire",
        () -> new SingleWireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(.3f)
            .sound(SoundType.COPPER).noCollission().noOcclusion()));

    /** Insulated low-voltage cable, 200 W rating. The workhorse of a wiring run. */
    public static final DeferredHolder<Block, CopperWireBlock> COPPER_WIRE = BLOCKS.register("copper_wire",
        () -> new CopperWireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(.35f)
            .sound(SoundType.WOOL).noCollission().noOcclusion()));

    /** A six-way cast junction rated for 400 W. Used for vertical drops, climbs and crossing runs. */
    public static final DeferredHolder<Block, WireIntersectionBlock> WIRE_INTERSECTION = BLOCKS.register("wire_intersection",
        () -> new WireIntersectionBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(.5f)
            .sound(SoundType.COPPER).noCollission().noOcclusion()));

    // ----- Passive water collection -------------------------------------------------------------------------

    public static final DeferredHolder<Block, WaterCollectorBlock> RAIN_CATCHER = BLOCKS.register("rain_catcher",
        () -> new WaterCollectorBlock(CollectorType.RAIN_CATCHER, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(1.2f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredHolder<Block, WaterCollectorBlock> DEW_COLLECTOR = BLOCKS.register("dew_collector",
        () -> new WaterCollectorBlock(CollectorType.DEW_COLLECTOR, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.8f).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredHolder<Block, WaterCollectorBlock> ROCK_BASIN = BLOCKS.register("rock_basin",
        () -> new WaterCollectorBlock(CollectorType.ROCK_BASIN, BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.5f, 5f).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredHolder<Block, WaterCollectorBlock> SOLAR_STILL = BLOCKS.register("solar_still",
        () -> new WaterCollectorBlock(CollectorType.SOLAR_STILL, BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(1.5f, 2f).sound(SoundType.GLASS).noOcclusion()));

    /** Overlapping bark sheets used as a temporary rain skin, roof covering, or primitive insulation. */
    public static final DeferredHolder<Block, BarkRoofBlock> BARK_ROOFING = BLOCKS.register("bark_roofing",
        () -> new BarkRoofBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(0.7f, 1f).sound(SoundType.WOOD).noOcclusion()));

    /** Compact laid-stone footing. It distributes a column's load into soil or rock beneath it. */
    public static final DeferredHolder<Block, Block> RUBBLE_FOUNDATION = BLOCKS.register("rubble_foundation",
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(3.0f, 9.0f)
            .sound(SoundType.STONE)));

    /** A pegged timber post or lintel. Flexible and forgiving, but not suitable for long spans of masonry. */
    public static final DeferredHolder<Block, SupportBeamBlock> WOODEN_SUPPORT_BEAM = BLOCKS.register("wooden_support_beam",
        () -> new SupportBeamBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5f, 4.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    /** Forge-welded wrought-iron I-section. It carries much heavier masonry and roof spans than timber. */
    public static final DeferredHolder<Block, SupportBeamBlock> WROUGHT_IRON_SUPPORT_BEAM = BLOCKS.register("wrought_iron_support_beam",
        () -> new SupportBeamBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0f, 12.0f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /**
     * A frame for drying wet clothes on. It is not itself a heat source - the point is that you have to put it
     * somewhere warm, which makes "keep a fire going" the actual mechanic rather than "own the right block".
     */
    public static final DeferredHolder<Block, DryingRackBlock> DRYING_RACK = BLOCKS.register("drying_rack",
        () -> new DryingRackBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(1.2f, 1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    // ----- Greenhouse structures ----------------------------------------------------------------------------

    /** A small glazed box that protects seedlings from frost. The simplest greenhouse: minimal climate buffer. */
    public static final DeferredHolder<Block, GreenhouseBlock> COLD_FRAME = BLOCKS.register("cold_frame",
        () -> new GreenhouseBlock(GreenhouseTier.COLD_FRAME, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(1.0f, 2.0f)
            .sound(SoundType.GLASS)
            .noOcclusion()));

    /** Wood-and-fabric hoop covering. Extends the growing season modestly without glass. */
    public static final DeferredHolder<Block, GreenhouseBlock> HOOP_HOUSE = BLOCKS.register("hoop_house",
        () -> new GreenhouseBlock(GreenhouseTier.HOOP_HOUSE, BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(1.5f, 3.0f)
            .sound(SoundType.WOOL)
            .noOcclusion()));

    /** A proper glass greenhouse with ventilation. Good solar capture and temperature control. */
    public static final DeferredHolder<Block, GreenhouseBlock> GLASS_GREENHOUSE = BLOCKS.register("glass_greenhouse",
        () -> new GreenhouseBlock(GreenhouseTier.GLASS_GREENHOUSE, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(2.5f, 5.0f)
            .sound(SoundType.GLASS)
            .noOcclusion()));

    /** The pinnacle: powered ventilation, heating, irrigation, and automated climate control. */
    public static final DeferredHolder<Block, GreenhouseBlock> MODERN_GREENHOUSE = BLOCKS.register("modern_greenhouse",
        () -> new GreenhouseBlock(GreenhouseTier.MODERN_GREENHOUSE, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(4.0f, 8.0f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    // ----- Soil preparation and farming ----------------------------------------------------------------------

    /** Farmland that has been cleared of stones, loosened, and optionally amended. Quality varies by preparation. */
    public static final DeferredHolder<Block, PreparedFarmlandBlock> PREPARED_FARMLAND = BLOCKS.register("prepared_farmland",
        () -> new PreparedFarmlandBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT)
            .strength(0.6f)
            .sound(SoundType.GRAVEL)));

    // ----- Irrigation ---------------------------------------------------------------------------------------

    /** Drip irrigation pipe that distributes water from storage tanks to nearby prepared farmland. */
    public static final DeferredHolder<Block, DripIrrigationBlock> DRIP_IRRIGATION = BLOCKS.register("drip_irrigation",
        () -> new DripIrrigationBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion()));

    /** Rainwater storage tank. Collects rainwater and feeds drip irrigation. Has a block entity for tick-based water collection. */
    public static final DeferredHolder<Block, com.terravera.common.farming.IrrigationTankBlock> IRRIGATION_TANK = BLOCKS.register("irrigation_tank",
        () -> new com.terravera.common.farming.IrrigationTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 4.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    private TerraVeraBlocks() {}
}
