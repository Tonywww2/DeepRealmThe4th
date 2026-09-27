package com.tony.deeprealmtheforth.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tony.deeprealmtheforth.platform.worldgen.VersionedChunkGenerator;
import com.tony.deeprealmtheforth.platform.worldgen.BiomeClimateAdapter;
import com.tony.deeprealmtheforth.worldgen.biome.SpiralBiomeSource;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.surface.BiomeDecoration;
import com.tony.deeprealmtheforth.worldgen.surface.TerrainMaterials;
import com.tony.deeprealmtheforth.worldgen.surface.StructureDecoration;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class SpiralChunkGenerator extends VersionedChunkGenerator {
    public static final MapCodec<SpiralChunkGenerator> MAP_CODEC = BiomeSource.CODEC.fieldOf("biome_source")
            .xmap(source -> {
                if (!(source instanceof SpiralBiomeSource spiral)) throw new IllegalArgumentException("Expected spiral biome source");
                return new SpiralChunkGenerator(spiral);
            }, SpiralChunkGenerator::getBiomeSource);
    // Forge's codec registry is identity-based. Registration and saving must use this same object.
    public static final Codec<SpiralChunkGenerator> CODEC = MAP_CODEC.codec();

    private final SpiralBiomeSource source;
    private final BiomeDecoration decoration = new BiomeDecoration();
    private static final net.minecraft.tags.TagKey<Biome> GRASS_SUBSTRATE=net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BIOME,com.tony.deeprealmtheforth.platform.PlatformIds.id("surface/grass"));
    private static final net.minecraft.tags.TagKey<Biome> WHITE_SUBSTRATE=net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BIOME,com.tony.deeprealmtheforth.platform.PlatformIds.id("surface/white_terracotta"));
    private static final net.minecraft.tags.TagKey<Biome> BADLANDS_SUBSTRATE=net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BIOME,com.tony.deeprealmtheforth.platform.PlatformIds.id("surface/badlands"));

    public SpiralChunkGenerator(SpiralBiomeSource source) { super(source); this.source = source; }
    public TerrainProfile terrain() { return source.terrain(); }
    public SpiralBiomeSource spiralBiomes() { return source; }
    public BiomeDecoration decoration() { return decoration; }

    /** Same material path for fill, height, base-column queries and runtime verification. */
    public BlockState terrainBlock(TerrainProfile.Column c, int y, int x, int z) {
        return terrainBlock(c,y,x,z,clayMountain(c,x,z));
    }

    private boolean clayMountain(TerrainProfile.Column c,int x,int z) {
        return TerrainMaterials.supportsClayMountain(c)
                &&source.biomeAt(x,z).is(BADLANDS_SUBSTRATE);
    }

    private BlockState terrainBlock(TerrainProfile.Column c,int y,int x,int z,boolean clayMountain) {
        BlockState block = TerrainMaterials.at(c, y, x, z, terrain().seed(),clayMountain);
        if (c.land()) {
            if (c.wet() && c.fluid() == TerrainProfile.Fluid.WATER && y == c.fluidLevel()
                    && BiomeClimateAdapter.snowClimate(source.biomeAt(x, z).value(), new BlockPos(x, y, z)))
                return net.minecraft.world.level.block.Blocks.ICE.defaultBlockState();
            if (!c.wet() && c.theme() == TerrainProfile.Theme.MOUNTAIN && y == c.top())
                return (BiomeClimateAdapter.snowClimate(source.biomeAt(x, z).value(), new BlockPos(x, y, z))
                        ? net.minecraft.world.level.block.Blocks.SNOW_BLOCK : net.minecraft.world.level.block.Blocks.STONE).defaultBlockState();
        }
        return block;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() { return MAP_CODEC; }

    @Override
    protected Codec<? extends ChunkGenerator> forgeCodec() { return CODEC; }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures, RandomState random, long seed) {
        source.bindSeed(seed);
        return ChunkGeneratorStructureState.createForNormal(random, seed, source, structures);
    }

    @Override
    public void createStructures(RegistryAccess registries, ChunkGeneratorStructureState state,
            StructureManager structures, ChunkAccess chunk, StructureTemplateManager templates) {
        super.createStructures(registries, state, structures, chunk, templates);
        // Keep native start selection/biome/spacing rules; reject a whole start instead of
        // cutting individual buildings at a void boundary. References and /locate see this too.
        // getAllStarts() is read-only on Forge; use the native setter so the chunk is dirtied.
        new java.util.HashMap<>(chunk.getAllStarts()).forEach((structure, start) -> {
            if (start.isValid() && !StructureDecoration.supported(start, terrain()))
                structures.setStartForStructure(SectionPos.bottomOf(chunk), structure, StructureStart.INVALID_START, chunk);
        });
    }

    @Override
    protected CompletableFuture<ChunkAccess> generateTerrain(ChunkAccess chunk) {
        TerrainProfile terrain = terrain();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunk.getPos().getMinBlockX() + lx, z = chunk.getPos().getMinBlockZ() + lz;
                TerrainProfile.Column c = terrain.sample(x, z);
                if (!c.land()) continue;
                boolean clayMountain=clayMountain(c,x,z);
                for (int y = Math.max(c.bottom(), chunk.getMinBuildHeight()); y <= c.surface() && y < chunk.getMaxBuildHeight(); y++) {
                    BlockState block = terrainBlock(c, y, x, z,clayMountain);
                    chunk.setBlockState(pos.set(x, y, z), block, false);
                    // Like vanilla aquifers: defer fluid simulation until the chunk and its
                    // neighbours are ready. setBlockState on a ProtoChunk does not run onPlace.
                    // Mark actual river/lake water only (not ice or sealed ocean interiors).
                    if ((c.arm() == 1 || c.arm() == 5)
                            && block.getFluidState().is(net.minecraft.tags.FluidTags.WATER))
                        chunk.markPosForPostprocessing(pos);
                    floor.update(lx, y, lz, block);
                    surface.update(lx, y, lz, block);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
        // Biome-specific ecology substrates; geometry and all query heights are unchanged.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                TerrainProfile.Column c = terrain().sample(x, z);
                if (!c.land() || c.wet() || c.shore()) continue;
                var holder=source.biomeAt(x,z);
                String biome = holder.unwrapKey().map(k -> k.location().getPath()).orElse("");
                net.minecraft.world.level.block.Block substrate = switch (biome) {
                    case "soul_sand_valley" -> net.minecraft.world.level.block.Blocks.SOUL_SAND;
                    case "crimson_forest" -> net.minecraft.world.level.block.Blocks.CRIMSON_NYLIUM;
                    case "warped_forest" -> net.minecraft.world.level.block.Blocks.WARPED_NYLIUM;
                    case "mushroom_fields" -> net.minecraft.world.level.block.Blocks.MYCELIUM;
                    case "old_growth_pine_taiga", "old_growth_spruce_taiga" -> net.minecraft.world.level.block.Blocks.PODZOL;
                    default -> null;
                };
                // Optional substrate adapters, not a replacement for another mod's surface-rule graph.
                if(holder.is(GRASS_SUBSTRATE)&&com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise.fractal(
                        terrain().seed()^831791,x,z,47,2)>-.20)
                    substrate=net.minecraft.world.level.block.Blocks.GRASS_BLOCK;
                if(holder.is(WHITE_SUBSTRATE)) {
                    substrate=net.minecraft.world.level.block.Blocks.WHITE_TERRACOTTA;
                    for(int d=1;d<=3&&c.top()-d>c.bottom()+3;d++)
                        chunk.setBlockState(pos.set(x,c.top()-d,z),substrate.defaultBlockState(),false);
                }
                if (substrate != null) chunk.setBlockState(pos.set(x, c.top(), z), substrate.defaultBlockState(), false);
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
            StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {}

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        decoration.decorate(level, chunk, this, structures);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos pos = region.getCenter();
        TerrainProfile.Column center = terrain().sample(pos.getMiddleBlockX(), pos.getMiddleBlockZ());
        if (center.land() && center.edgeDistance() > 20) {
            NaturalSpawner.spawnMobsForChunkGeneration(region,
                    source.biomeAt(pos.getMiddleBlockX(), pos.getMiddleBlockZ()), pos,
                    RandomSource.create(com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise.hash(terrain().seed(), pos.x, pos.z)));
        }
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome, StructureManager structures,
            MobCategory category, BlockPos pos) {
        return terrain().sample(pos.getX(), pos.getZ()).land()
                ? super.getMobsAt(biome, structures, category, pos) : WeightedRandomList.create();
    }

    @Override public int getMinY() { return SpiralParameters.MIN_Y; }
    @Override public int getGenDepth() { return SpiralParameters.HEIGHT; }
    @Override public int getSeaLevel() { return TerrainProfile.SEA_LEVEL; }
    @Override public int getSpawnHeight(LevelHeightAccessor height) { return 128; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor height, RandomState random) {
        TerrainProfile.Column c = terrain().sample(x, z);
        boolean clayMountain=clayMountain(c,x,z);
        for (int y = Math.min(c.surface(), height.getMaxBuildHeight() - 1); c.land() && y >= Math.max(c.bottom(), height.getMinBuildHeight()); y--) {
            if (type.isOpaque().test(terrainBlock(c, y, x, z,clayMountain))) return y + 1;
        }
        return height.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        TerrainProfile.Column c = terrain().sample(x, z);
        boolean clayMountain=clayMountain(c,x,z);
        BlockState[] blocks = new BlockState[height.getHeight()];
        for (int i = 0; i < blocks.length; i++) blocks[i] = terrainBlock(c, height.getMinBuildHeight() + i, x, z,clayMountain);
        return new NoiseColumn(height.getMinBuildHeight(), blocks);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
        TerrainProfile.Column c = terrain().sample(pos.getX(), pos.getZ());
        lines.add("Fourth layer | arm=" + c.arm() + " | " + c.theme() + " | top=" + c.top()
                + " | bottom=" + c.bottom() + " | water=" + c.fluidLevel());
    }
}
