package src.types;

import src.crypto.PRNG128BitsInsecure;

import java.awt.*;
import java.util.Random;
import java.util.stream.Stream;

import static src.hashops.HashTransform.buildHSVWheel;

public class DrawParams {

    private DrawMode drawMode;
    private Distance dist;
    private PRNG128BitsInsecure prng;
    private double corr;
    private Distance.Shape distMode;
    private Deter modDet;
    private Deter phaseDet;
    private boolean filtered;
    private int cut;
    private boolean seePhase;

    private static Color back;
    private static Color front;
    private static Color spots;

    public enum Deter{
        DET, RAND, DOUBLE, FIXED
    }

    public DrawParams(DrawMode mode, Distance distance, PRNG128BitsInsecure prng, double corr, Distance.Shape distMode){
        drawMode = mode;
        dist = distance;
        this.prng = prng;
        this.corr = corr;
        this.distMode = distMode;
        modDet = Deter.DET;
        phaseDet = Deter.DET;
        filtered = true;
        cut = default_cut(mode);
        seePhase = false;
    }

    public DrawParams(DrawParams other){
        drawMode = other.getMode();
        dist = other.dist;
        this.prng = other.prng.copy();
        this.corr = other.corr;
        this.distMode = other.getDistMode();
        modDet = other.modDet;
        phaseDet = other.phaseDet;
        filtered = other.filtered;
        cut = other.cut;
        seePhase = other.seePhase;
    }

    public DrawParams(DrawMode mode){
        this(mode, getDefaultDist(mode, Deter.DET, Deter.DET), new PRNG128BitsInsecure(), getDefaultCorr(mode, Deter.DET, Deter.DET), Distance.Shape.RHOMBUS);
    }

    public static Distance getDefaultDist(DrawMode mode, Deter modDet, Deter phaseDet){
        if (modDet == Deter.RAND && phaseDet == Deter.RAND) return Distance.SQUARE;
        else if(mode.toString().contains("Cart") || mode == DrawMode.Fourier128 && (modDet == Deter.FIXED || modDet == Deter.DET) && (phaseDet == Deter.DET || phaseDet == Deter.DOUBLE)){
            return Distance.MANHATTAN;
        }
        else{
            return Distance.CUBIC;
        }
    }
    public static double getDefaultCorr(DrawMode mode, Deter modDet, Deter phaseDet){
        if(mode == DrawMode.Fourier128 && (modDet == Deter.FIXED || modDet == Deter.DET) && (phaseDet == Deter.DET || phaseDet == Deter.DOUBLE)){
            return 0.4;
        }
        return 0.4;
    }
    public static int worstBit(DrawMode mode){
        if(mode == DrawMode.Fourier256) return 147;
        return 54;
    }

    public int worstBit(){
        return worstBit(getMode());
    }

    public int cut(){
        return cut;
    }
    public void setCut(int newCut){
        cut = newCut;
    }

    public int default_cut(DrawMode drawMode){
        if(drawMode == DrawMode.Fourier128){
            if(modDet == Deter.DET && phaseDet == Deter.RAND){
                return distMode == Distance.Shape.RHOMBUS ? 7 : 6;
            }
            if(modDet == Deter.DET && phaseDet == Deter.DET){
                return distMode == Distance.Shape.RHOMBUS ? 4 : 3;
            }
            if(modDet == Deter.DET && phaseDet == Deter.DOUBLE){
                return distMode == Distance.Shape.RHOMBUS ? 3 : 2;
            }
            if(modDet == Deter.RAND && phaseDet == Deter.RAND){
                return 63;
            }
            else{
                return 6;
            }
        } else if (drawMode == DrawMode.Fourier256){
            if(modDet == Deter.RAND && phaseDet == Deter.RAND){
                return 127;
            }else{
                return 6;
            }
        } else {
            return 4;
        }
    }

    @Override
    public String toString() {
        if(drawMode.toString().startsWith("Fourier")){
            if(drawMode.toString().contains("Cartesian")){
                return drawMode + "_" + dist.toString().substring(0,3) + distMode.toString().substring(0,4) + "_" + (corr + "000").replace(".", "").substring(0,3);
            }else{
                return drawMode + "_" + modDet + "Mod_" + phaseDet + "_Phase_" + dist.toString().substring(0,3) + "_" + distMode.toString().substring(0,4) + "_" + (corr + "000").replace(".", "").substring(0,3);
            }
        }else{
            return drawMode.toString();
        }
    }

    public Distance getDist() {
        return dist;
    }

    public double dist(int x,  int y){
        return dist.dist(x, y, this);
    }

    public void setModDet(Deter det){
        modDet = det;
    }
    public void setPhaseDet(Deter phaseDet) {
        this.phaseDet = phaseDet;
    }
    public Deter getModDet() {
        return modDet;
    }
    public Deter getPhaseDet() {
        return phaseDet;
    }


    public void setDist(Distance newDist){
        this.dist = newDist;
    }

    public void setMode(DrawMode mode){
        drawMode = mode;
    }

    public DrawMode getMode(){
        return drawMode;
    }

    public void setCorr(double newCorr){
        corr = newCorr;
    }

    public double getCorr(){
        return corr;
    }

    public boolean isFiltered(){
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public Distance.Shape getDistMode(){
        return distMode;
    }
    public void setDistMode(Distance.Shape newDistMode){
        distMode = newDistMode;
    }

    public boolean isSeePhase() {
        return seePhase;
    }

    public void setSeePhase(boolean seePhase) {
        this.seePhase = seePhase;
    }

    public PRNG128BitsInsecure prng(){
        return prng;
    }

    public void setPrng(PRNG128BitsInsecure b){
        prng = b;
    }

    public void sampleColors() {
        if (prng == null)
            return;
        front = prng.createColor();
        back = prng.createColor();
        spots = prng.createColor();
    }

    public int[] paletteRGB(int length, Random r){
        Color[] palette = buildHSVWheel(length, this, r);
        return Stream.of(palette).mapToInt(col -> (col.getRGB() & 0xffffff)).toArray();
    }
    public int[] paletteRGB(int length){
        Color[] palette = buildHSVWheel(length, this);
        return Stream.of(palette).mapToInt(col -> (col.getRGB() & 0xffffff)).toArray();
    }

    public Color[] palette() {
        switch (drawMode) {
            case Antoine256:
                return new Color[]{new Color(0, 0, 0), new Color(255, 255, 255)};
            case Adjacency1_256:
            case Adjacency2_256:
                return new Color[]{new Color(255, 255, 255), new Color(255, 0, 0), new Color(0, 255, 0),
                        new Color(0, 0, 255), new Color(255, 255, 0), new Color(0, 255, 255), new Color(255, 0, 255)};
            case Blockies128:
                return new Color[]{back, front, spots};
            default:
                return buildHSVWheel(16, this);
        }
    }
}
