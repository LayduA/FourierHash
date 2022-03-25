package code.transform;

import code.enums.DrawMode;
import code.ui.Applet;
import code.ui.Surface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.logging.Logger;

import static code.transform.TransformHash.flipBits;
import static code.transform.TransformHash.psiDist;

public class AvgRunnable implements Runnable {
    double[] out;
    int from;
    int size;
    String refHash;
    int[] refHashPixels;
    Surface drawer;
    DrawMode mode;

    public AvgRunnable(double[] out, int from, int size, String refHash, int[] refHashPixels, Surface drawer, DrawMode mode) {
        this.out = out;
        this.refHash = refHash;
        this.refHashPixels = refHashPixels;
        this.from = from;
        this.size = size;
        this.drawer = drawer;
        this.mode = mode;
    }

    @Override
    public void run() {
        BufferedImage res = new BufferedImage(Applet.HASH_W, Applet.HASH_H, BufferedImage.TYPE_INT_RGB);
        Graphics g = res.createGraphics();
        for (int i = 0; i < size; i++) {
            double d = psiDist(refHashPixels, drawer.drawFourierHash(g, flipBits(refHash, Arrays.asList(from + i, 255)), 0, mode), "imageThread" + from/10);
            out[from + i] += d;
        }
    }
}
