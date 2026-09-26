package com.tony.deeprealmtheforth.platform.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;

public final class WorldgenCodecs {
    private WorldgenCodecs() {}

    public static final Codec<SpiralParameters> PARAMETERS = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("center_x", 8.0).forGetter(SpiralParameters::centerX),
            Codec.DOUBLE.optionalFieldOf("center_z", 8.0).forGetter(SpiralParameters::centerZ),
            Codec.doubleRange(4, 4096).optionalFieldOf("core_radius", SpiralParameters.DEFAULT.coreRadius()).forGetter(SpiralParameters::coreRadius),
            Codec.doubleRange(32, 8192).optionalFieldOf("plunge_radius", SpiralParameters.DEFAULT.plungeRadius()).forGetter(SpiralParameters::plungeRadius),
            Codec.doubleRange(0.01, 4).optionalFieldOf("twist", SpiralParameters.DEFAULT.twist()).forGetter(SpiralParameters::twist),
            Codec.DOUBLE.optionalFieldOf("rotation", 0.0).forGetter(SpiralParameters::rotation),
            Codec.doubleRange(16, 65536).optionalFieldOf("reference_radius", 256.0).forGetter(SpiralParameters::referenceRadius),
            Codec.doubleRange(64, 65536).optionalFieldOf("radial_scale", 430.0).forGetter(SpiralParameters::radialScale),
            Codec.doubleRange(0.1, 0.9).optionalFieldOf("land_fraction", SpiralParameters.DEFAULT.landFraction()).forGetter(SpiralParameters::landFraction)
    ).apply(i, SpiralParameters::new));
}
