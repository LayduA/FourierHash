package code.transform;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import code.ui.Applet.DMODE;

import java.awt.Color;

public interface TransformHash {

    public static String flipBit(String hex, int index){
        HashSet<Integer> singleton = new HashSet<>();
        singleton.add(index);
        return flipBits(hex, singleton);
    }

    public static String flipBits(String hexString, Collection<Integer> indices) {
        String flipped = hexToBin(hexString);
        for (int i : indices) {
            if (i < 0 || i >= flipped.length())
                continue;
            flipped = flipped.substring(0, i) + (flipped.charAt(i) == '0' ? "1" : "0") + flipped.substring(i + 1);
        }
        return binToHex(flipped);
    }

    public static String flipBitsRandom(String hexString, int n, int hashLength) {
        // We use a set to avoid duplicate indices
        if (n > hashLength)
            return hexString;
        HashSet<Integer> values = new HashSet<Integer>();
        while (values.size() < n) {
            values.add(ThreadLocalRandom.current().nextInt(hashLength));
        }
        return flipBits(hexString, values);
    }

    public static int hamDistHex(String hexHash1, String hexHash2) {
        if (hexHash1.length() != hexHash2.length())
            return -1;
        String bin1 = hexToBin(hexHash1);
        String bin2 = hexToBin(hexHash2);
        int acc = 0;
        // System.out.println(bin1 + " " + bin2);
        for (int i = 0; i < bin1.length(); i++) {
            if (bin1.charAt(i) != bin2.charAt(i))
                acc++;
        }
        return acc;

    }

    public static String hexToBin(String hex) {
        return String.format("%" + hex.length() * 4 + "s", new BigInteger(hex, 16).toString(2)).replace(" ", "0");
    }

    public static String shiftRBits(String hexString) {
        String bin = hexToBin(hexString);

        int lengthLine = getsqrtBin(bin);
        if (lengthLine == -1)
            return "";
        for (int i = 0; i < lengthLine; i++) {
            if (i < 5)
                continue;
            bin = bin.substring(0, i * lengthLine)
                    + shiftString(bin.substring(i * lengthLine, (i + 1) * lengthLine), i < 9 ? 1 : 1)
                    + bin.substring((i + 1) * lengthLine);
        }

        return binToHex(bin);
    }

    private static String shiftString(String s, int r) {
        String x = "";
        for (int i = 0; i < r; i++) {
            x = x + s.charAt(0);
        }
        return x + s.substring(0, s.length() - r);
    }

    public static String binToHex(String bin) {
        if (bin.length() % 4 != 0) {
            System.out.println("Something went wrong");
        }
        return String.format("%" + bin.length() / 4 + "s", new BigInteger(bin, 2).toString(16)).replace(" ", "0");
    }

    public static String rotateHex(String hex, int n) {
        String bin = hexToBin(hex);
        bin = bin.substring(n) + bin.substring(0, n);
        return binToHex(bin);
    }

    public static int getsqrtHex(String hex) {
        double sqrt = Math.floor(Math.sqrt(hex.length() * 4));
        return (int) sqrt;
    }

    public static int getsqrtBin(String bin) {
        double sqrt = Math.floor(Math.sqrt(bin.length()));
        return (int) sqrt;
    }

    public static int[][] extractValues(String bin, DMODE drawMode) {
        return extractValues(bin, drawMode, null);
    }
    public static int[][] extractValues(String bin, DMODE drawMode, Blockies prng) {
        double sqrt = Math.floor(Math.sqrt(bin.length()));
        int width = sqrt - Math.sqrt(bin.length()) == 0 ? (int) sqrt : (int) sqrt + 1;
        int height = width;
        int[][] values = new int[height][width];
        int i, j;
        if (drawMode == DMODE.AntoineShift256){
            return shiftMatrix(bin);
        }
        for (int p = 0; p < width * height; p++) {
            i = p / width;
            j = p % width;
            if (p >= bin.length()) {
                values[i][j] = 0;
                //continue;
            }
            if (drawMode == DMODE.Antoine256)
                values[i][j] = bin.charAt(p) - '0';
            else if (drawMode == DMODE.Adjacency1_256) {
                int c = bin.charAt(p) - '0';
                values[i][j] = 0;
                if (c == 0) {
                    values[i][j]++;
                    if (j > 0)
                        values[i][j] += '1' - bin.charAt(p - 1);
                    if (j < width - 1)
                        values[i][j] += '1' - bin.charAt(p + 1);
                }

            } else if (drawMode == DMODE.Adjacency2_256) {
                int c = bin.charAt(p) - '0';
                if (c == 1) {
                    if (j > 0)
                        c += '1' - bin.charAt(p - 1);
                    if (j < width - 1)
                        c += '1' - bin.charAt(p + 1);
                    if (i > 0)
                        c += '1' - bin.charAt(p - width);
                    if (i < height - 1)
                        c += '1' - bin.charAt(p + width);
                    values[i][j] = c;
                }
            } else if (drawMode == DMODE.Blockies128) {
                if (j >= width / 2) {
                    values[i][j] = values[i][width - 1 - j];
                }else{
                    double val = prng.rand() * 2.3;
                    values[i][j] = val < 1 ? 0 : (val < 2 ? 1 : 2);
                }
            }
        }

        return values;
    }

    public static int[][] shiftMatrix(String bin){
        int[][] mat = new int[bin.length()][bin.length()];
        for (int i = 0; i < mat.length; i++) {
            mat[i] = rotateHex(bin, i).chars().map(n -> n - '0').toArray();
        }
        return mat;
    }


    public static double psi(int[][] pixels0, int[][] pixels1, int[] colors) {
        if (pixels0.length != pixels1.length || pixels0[0].length != pixels1[0].length)
            return -1;
        float dist = 0;

        for (int c : colors) {
            dist += partialDist(pixels0, pixels1, c);
            dist += partialDist(pixels1, pixels0, c);
        }
        return dist;
    }

    private static int[][] dmapc(int[][] pixels, int color) {

        int[][] dmap = new int[pixels.length][pixels[0].length];
        // Array initialization
        for (int i = 0; i < dmap.length; i++) {
            for (int j = 0; j < dmap[0].length; j++) {
                dmap[i][j] = pixels[i][j] == color ? 0 : Integer.MAX_VALUE / 2;
            }
        }
        // Forward pass
        for (int i = 0; i < dmap.length; i++) {
            for (int j = 0; j < dmap[0].length; j++) {
                if (i > 0)
                    dmap[i][j] = Math.min(dmap[i - 1][j] + 1, dmap[i][j]);
                if (j > 0)
                    dmap[i][j] = Math.min(dmap[i][j - 1] + 1, dmap[i][j]);
            }
        }
        // Backward pass
        for (int i = dmap.length - 2; i >= 0; i--) {
            for (int j = dmap[0].length - 2; j >= 0; j--) {
                if (i < dmap.length - 1)
                    dmap[i][j] = Math.min(dmap[i + 1][j] + 1, dmap[i][j]);
                if (j < dmap[0].length - 1)
                    dmap[i][j] = Math.min(dmap[i][j + 1] + 1, dmap[i][j]);
            }
        }

        return dmap;

    }

    private static float partialDist(int[][] pixels0, int[][] pixels1, int color) {
        int totalPixels = 0;
        float sumDists = 0f;
        int[][] dmapc = dmapc(pixels1, color);
        for (int i = 0; i < dmapc.length; i++) {
            for (int j = 0; j < dmapc[0].length; j++) {
                if (pixels0[i][j] == color) {
                    sumDists += dmapc[i][j];
                    totalPixels++;
                }
            }
        }

        return totalPixels == 0 ? 0 : sumDists / totalPixels;
    }

    public static Color[] buildHSVWheel(int n) {
        Color[] wheel = new Color[n];
        int ind;
        for (int i = 0; i < n; i++) {
            // [0 1 2 3 4 5 6 7 8 9 a b c d e f] -> [0 3 6 9 c f 1 4 7 a d 2 5 8 b e]
            ind = i < 6 ? 3 * i : (i < 11 ? (3 * i + 1) % 18 : (3 * i) % 31);
            wheel[ind] = new Color(Color.HSBtoRGB((float) i * 1f / n, 1, 1));
        }
        return wheel;
    }

}
