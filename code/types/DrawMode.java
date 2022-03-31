package code.types;

import code.transform.Blockies;

import java.awt.*;

import static code.transform.TransformHash.buildHSVWheel;
import static code.types.Distance.*;

public enum DrawMode {
    Antoine256, AntoineShift256,
    Adjacency1_256, Adjacency2_256,
    GridLines256, GridLines128, GridLines64, GridLines32,
    Landscape32,
    Blockies128,
    Random128,
    FourierDModRPhase256, FourierRModDPhase256, FourierDModDPhase256, FourierRModRPhase256,
    FourierDModRPhase128, FourierRModDPhase128, FourierDModDPhase128, FourierRModRPhase128,
    FourierCartesian256;

    private final static Distance[] DISTS = {
            null, null,
            null, null,
            null, null, null, null,
            null,
            null,
            null,
            CUBIC, CUBIC, CUBIC, SQUARE,
            CUBIC, CUBIC, SQUARE, SQUARE,
            CUBIC
    };
    private final static double[] CORRS = {
            1, 1,
            1, 1,
            1, 1, 1, 1,
            1,
            1,
            1,
            0.5, 0.5, 0.5, 0.5,
            0.5, 0.5, 0.5, 0.5,
            0.6
    };

    public double dist(int x, int y) {
        return getDist().dist(x, y, cut());
    }

    public Distance getDist() {
        return DISTS[ordinal()];
    }

    public void setDist(Distance dist) {
        DISTS[ordinal()] = dist;
    }

    public int worstBit() {
        switch (this) {
            case FourierDModRPhase256:
            case FourierRModDPhase256:
            case FourierDModDPhase256:
                return 147;
            case FourierDModDPhase128:
                return 72;
            default:
                return 0;
        }
    }

    public double corr() {
        return CORRS[ordinal()];
    }

    public void setCorr(double newCorr) {
        CORRS[ordinal()] = newCorr;
    }

    public int cut() {
        switch (this) {
            case FourierRModRPhase256:
                return 127;
            case FourierRModRPhase128:
                return 63;
            case FourierDModDPhase128:
                return 3;
            default:
                return 6;
        }
    }

    private static Color back;
    private static Color front;
    private static Color spots;


    public static void sampleColors(Blockies prng) {
        if (prng == null)
            return;
        front = prng.createColor();
        back = prng.createColor();
        spots = prng.createColor();
    }

    public int length(){
        return Integer.parseInt(toString().substring(toString().length()-3));
    }

    public Color[] palette() {
        switch (this) {
            case Antoine256:
                return new Color[]{new Color(0, 0, 0), new Color(255, 255, 255)};
            case Adjacency1_256:
            case Adjacency2_256:
                return new Color[]{new Color(255, 255, 255), new Color(255, 0, 0), new Color(0, 255, 0),
                        new Color(0, 0, 255), new Color(255, 255, 0), new Color(0, 255, 255), new Color(255, 0, 255)};
            case Blockies128:
                return new Color[]{back, front, spots};
            default:
                return buildHSVWheel(16);
        }
    }
}
