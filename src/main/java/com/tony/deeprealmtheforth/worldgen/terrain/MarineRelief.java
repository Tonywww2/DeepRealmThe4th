package com.tony.deeprealmtheforth.worldgen.terrain;

import static com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout.smooth;
import static com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile.SEA_LEVEL;

/** V5 marine H0: correlated geology, not a constant-depth basin around one island contour. */
public final class MarineRelief {
    private final long seed;
    public MarineRelief(long seed) { this.seed=seed; }
    public record Sample(double height,double beach) {}
    public Sample sample(double x,double z) {
        double wx=x+90*n(61397,x,z,470,2)+24*n(75199,x,z,91,2);
        double wz=z+90*n(39173,x,z,470,2)+24*n(91871,x,z,91,2);
        double continental=n(59173,wx,wz,710,3);
        double islands=n(13179,wx,wz,205,3);
        // Small island groups favor the continental margins; deep basins stay open water.
        double archipelago=smooth(-.48,.12,continental);
        double coast=195*(.74*continental+.48*islands*archipelago-.045)
                +9*n(21979,wx,wz,49,3)+3*n(39871,x,z,17,2);
        double rocky=smooth(-.17,.29,n(91813,wx,wz,310,2));
        double width=24+30*(1-rocky)+17*n(33191,wx,wz,190,2);
        double inland=Math.max(0,coast);
        // Broad low sandy coastal plains alternate with steeper weathered headlands.
        double shoreSlope=.11+.44*rocky;
        double land=SEA_LEVEL+shoreSlope*inland+.72*Math.max(0,inland-width)
                *smooth(width,width+36,inland);
        land+=smooth(4,33,inland)*(3.5*n(14897,wx,wz,58,3)+1.2*n(39571,x,z,19,2));
        double offshore=Math.max(0,-coast);
        double shelf=18+11*n(67831,wx,wz,240,2)+8*(1-rocky);
        double basin=smooth(shelf,shelf+34,offshore);
        double seabed=SEA_LEVEL-(.42+.36*rocky)*offshore-32*basin;
        double trench=1-smooth(.025,.24,Math.abs(n(87917,wx,wz,360,2)));
        seabed+=smooth(3,24,offshore)*(5*n(18391,wx,wz,73,3)+1.7*n(59519,x,z,23,2))
                -basin*trench*(16+7*n(17891,wx,wz,148,2));
        // Bounded sea floor leaves room for a supported bed above the void floor.
        seabed=Math.max(-40,seabed);
        double beach=(1-smooth(width-12,width+16,inland+6*n(83197,x,z,37,2)))
                *(1-.83*rocky);
        return new Sample(coast>=0?land:Math.min(SEA_LEVEL-.001,seabed),beach);
    }
    private double n(long salt,double x,double z,double scale,int octaves) {
        return SeededNoise.fractal(seed^salt,x,z,scale,octaves);
    }
}
