package code.enums;

import code.transform.Blockies;

import java.awt.*;

import static code.transform.TransformHash.buildHSVWheel;

public enum DrawMode {
    Antoine256, AntoineShift256, Adjacency1_256, Adjacency2_256, GridLines256, GridLines128, GridLines64,
    GridLines32, Landscape32,
    Blockies128, Random128, FourierDModRPhase256, FourierRModDPhase256, FourierDModDPhase256, FourierRModRPhase256;

    public double dist(int x, int y) {
        switch (this) {
            case FourierDModRPhase256:
                return Distance.CUBIC.dist(x,y,cut());
            case FourierDModDPhase256:
                return Distance.SIGMOID.dist(x,y,cut());
            case FourierRModRPhase256:
                return Distance.BELL.dist(x,y,cut());
            default:
                return Distance.SQUARE.dist(x,y,cut());
        }
    }


    public int worstBit(){
        switch (this){
            case FourierDModRPhase256:
            case FourierRModDPhase256:
            case FourierDModDPhase256:
                return 147;
            default: return 0;
        }
    }

    public double corr() {
        switch (this) {
            case FourierDModRPhase256:
                return 0.505;
            case FourierDModDPhase256:
                return 0.451;
            default:
                return 0.5;
        }
    }

    public int cut() {
        switch (this) {
            case FourierDModRPhase256:
            case FourierRModDPhase256:
            case FourierDModDPhase256:
                return 6;
            case FourierRModRPhase256:
                return 127;
            default:
                return 0;
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
