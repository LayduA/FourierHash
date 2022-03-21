package code.transform;

import java.awt.Color;
import java.util.Arrays;

public class Blockies {
    
    private int[] randseed;

    public Blockies(String seed){
        randseed = new int[4];
        seedrand(seed);
    }

    public Blockies(){
        randseed = new int[4];
     }

    public void seedrand(String seed) {
        for (var i = 0; i < randseed.length; i++) {
            randseed[i] = 0;
        }
        for (var i = 0; i < seed.length(); i++) {
            randseed[i%4] = ((randseed[i%4] << 5) - randseed[i%4]) + seed.charAt(i);
        }
    }

    public double rand() {
        // based on Java's String.hashCode(), expanded to 4 32bit values
        var t = randseed[0] ^ (randseed[0] << 11);
    
        randseed[0] = randseed[1];
        randseed[1] = randseed[2];
        randseed[2] = randseed[3];
        randseed[3] = (randseed[3] ^ (randseed[3] >> 19) ^ t ^ (t >> 8));
    
        return (double)(randseed[3]>>>0) / ((1L << 31)>>>0);
    }

    public Color createColor() {
        //saturation is the whole color spectrum
        float h = (float)(rand() * 360);
        //saturation goes from 40 to 100, it avoids greyish colors
        float s = (float)((rand() * 40) + 60) + '%';
        //lightness can be anything from 0 to 100, but probabilities are a bell curve around 50%
        float l = (float)((rand()+rand()+rand()+rand()) * 25) + '%';
        return Color.getHSBColor(h, s, l);
    }

    @Override
    public String toString() {
        return "PRNG,seed: " + Arrays.toString(randseed);
    }

}
