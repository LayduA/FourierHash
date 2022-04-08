package src.hashops;

import src.types.DrawParams;

import java.awt.image.BufferedImage;

import static src.hashops.HashTransform.flipBit;
import static src.ui.Applet.HASH_H;
import static src.ui.Applet.HASH_W;

public class SimpleHashRunnable implements Runnable {

    private final HashDrawer drawer;
    private final String[] hashes;
    private final DrawParams params;
    private final int[][][] out;
    private final int from;
    private final int size;
    //private final int bitsFlipped;

    public SimpleHashRunnable(HashDrawer drawer, String[] hashes, DrawParams params, int[][][] out, int from, int size){
        this.drawer = drawer;
        this.hashes = hashes;
        this.params = params;
        this.out = out;
        this.from = from;
        this.size = size;
    }

    @Override
    public void run(){
        if(out[0].length != 2) throw new IllegalArgumentException("The size of output array must have last dimension equal to 2");
        BufferedImage bi = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
        for (int i = from; i < from + size; i++) {
            out[i][0] = drawer.drawFourierHash(bi.createGraphics(), hashes[i], 0, params);
            out[i][1] = drawer.drawFourierHash(bi.createGraphics(), flipBit(hashes[i], i % params.getMode().length()), 0, params);
        }
    }
}
