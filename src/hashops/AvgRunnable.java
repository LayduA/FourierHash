package src.hashops;

import src.types.DrawParams;
import src.ui.Applet;

import java.awt.*;
import java.awt.image.BufferedImage;

import static src.hashops.TransformHash.*;

public class AvgRunnable implements Runnable {
    double[] out;
    int from;
    int size;
    String refHash;
    int[] refHashPixels;
    Surface drawer;
    DrawParams params;

    public AvgRunnable(double[] out, int from, int size, String refHash, int[] refHashPixels, Surface drawer, DrawParams params) {
        this.out = out;
        this.refHash = refHash;
        this.refHashPixels = refHashPixels;
        this.from = from;
        this.size = size;
        this.drawer = drawer;
        this.params = params;
    }

    @Override
    public void run() {
        BufferedImage res = new BufferedImage(Applet.HASH_W, Applet.HASH_H, BufferedImage.TYPE_INT_RGB);
        Graphics g = res.createGraphics();
        for (int i = 0; i < size; i++) {
            double d = psiDist(refHashPixels, drawer.drawFourierHash(g, flipBit(refHash, from + i), 0, params), "imageThread" + hashCode());

            out[from + i] += d;
        }

        //out[index] += psiDist(refHashPixels, drawer.drawFourierHash(g, flipBit(refHash, index), 0, mode) , "imageThread" + index);
    }
}