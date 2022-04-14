package src.hashops;

import src.types.DrawMode;
import src.types.DrawParams;
import src.ui.Applet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface HashTransform {

    /**
     * Flips the bit of given index in the input hash and outputs the result as a hex string.
     *
     * @param hex   the hex hash
     * @param index the index of bit change
     * @return the hex hash with flipped bit
     */
    static String flipBit(String hex, int index) {
        HashSet<Integer> singleton = new HashSet<>();
        singleton.add(index);
        return flipBits(hex, singleton);
    }

    /**
     * Flip bits of given indices in the given hash and output the result
     *
     * @param hexString the input hash
     * @param indices   a collection of integer, the indices
     * @return the hash with flipped bits
     */
    static String flipBits(String hexString, Collection<Integer> indices) {
        String flipped = hexToBin(hexString);
        for (int i : indices) {
            if (i < 0 || i >= flipped.length()) continue;
            flipped = flipped.substring(0, i) + (flipped.charAt(i) == '0' ? "1" : "0") + flipped.substring(i + 1);
        }
        return binToHex(flipped);
    }

    static String flipBits(String hexString, int... indices) {
        List<Integer> l = new ArrayList<>();
        for (int ind : indices) {
            l.add(ind);
        }
        return flipBits(hexString, l);
    }

    /**
     * Flip n bits of a hex hash at randomly chosen indices.
     *
     * @param hexString  the input hash
     * @param n          the number of bits to flip
     * @param hashLength the length of the hash
     * @return the hash with n bits flipped
     */
    static String flipBitsRandom(String hexString, int n, int hashLength) {
        // We use a set to avoid duplicate indices
        if (n > hashLength) return hexString;
        HashSet<Integer> values = new HashSet<Integer>();
        while (values.size() < n) {
            values.add(ThreadLocalRandom.current().nextInt(hashLength));
        }
        return flipBits(hexString, values);
    }

    /**
     * Computes the Hamming distance of 2 hex hashes, that is, the number of bits at which they differ
     *
     * @param hexHash1 first hash, in hex string form
     * @param hexHash2 second hash, in hex string form
     * @return the Hamming distance
     */
    static int hamDistHex(String hexHash1, String hexHash2) {
        if (hexHash1.length() != hexHash2.length()) return -1;
        String bin1 = hexToBin(hexHash1);
        String bin2 = hexToBin(hexHash2);
        int acc = 0;
        // System.out.println(bin1 + " " + bin2);
        for (int i = 0; i < bin1.length(); i++) {
            if (bin1.charAt(i) != bin2.charAt(i)) acc++;
        }
        return acc;

    }

    /**
     * Converts a hex string hash to a bin string hash
     *
     * @param hex the hash as a hex string
     * @return the hash as a bin string
     */
    static String hexToBin(String hex) {
        return String.format("%" + hex.length() * 4 + "s", new BigInteger(hex, 16).toString(2)).replace(" ", "0");
    }


    static String shiftRBits(String hexString) {
        String bin = hexToBin(hexString);

        int lengthLine = getsqrtBin(bin);
        if (lengthLine == -1) return "";
        for (int i = 0; i < lengthLine; i++) {
            if (i < 5) continue;
            bin = bin.substring(0, i * lengthLine) + shiftString(bin.substring(i * lengthLine, (i + 1) * lengthLine), 1) + bin.substring((i + 1) * lengthLine);
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

    /**
     * Converts a bin string hash to a hex string hash
     *
     * @param bin the hash in bin string form
     * @return the hash in hex string form
     */
    static String binToHex(String bin) {
        if (bin.length() % 4 != 0) {
            System.out.println("Something went wrong");
        }
        return String.format("%" + bin.length() / 4 + "s", new BigInteger(bin, 2).toString(16)).replace(" ", "0");
    }

    static String rotateHex(String hex, int n) {
        String bin = hexToBin(hex);
        bin = bin.substring(n) + bin.substring(0, n);
        return binToHex(bin);
    }

    static int getsqrtBin(String bin) {
        double sqrt = Math.floor(Math.sqrt(bin.length()));
        return (int) sqrt;
    }


    /**
     * Computes the matrix of pixel values given a binary string hash and a draw mode.
     *
     * @param bin    the hash in binary string form
     * @param params the drawing parameters
     * @return a square matrices of values depending on the drawmode.
     */
    static int[][] extractValues(String bin, DrawParams params) {
        double sqrt = Math.floor(Math.sqrt(bin.length()));
        int width = sqrt - Math.sqrt(bin.length()) == 0 ? (int) sqrt : (int) sqrt + 1;
        int height = width;
        int[][] values = new int[height][width];
        int i, j;
        if (params.getMode() == DrawMode.AntoineShift256) {
            return shiftMatrix(bin);
        }
        for (int p = 0; p < width * height; p++) {
            i = p / width;
            j = p % width;
            if (p >= bin.length()) {
                values[i][j] = 0;
                //continue;
            }
            if (params.getMode() == DrawMode.Antoine256) values[i][j] = bin.charAt(p) - '0';
            else if (params.getMode() == DrawMode.Adjacency1_256) {
                int c = bin.charAt(p) - '0';
                values[i][j] = 0;
                if (c == 0) {
                    values[i][j]++;
                    if (j > 0) values[i][j] += '1' - bin.charAt(p - 1);
                    if (j < width - 1) values[i][j] += '1' - bin.charAt(p + 1);
                }

            } else if (params.getMode() == DrawMode.Adjacency2_256) {
                int c = bin.charAt(p) - '0';
                if (c == 1) {
                    if (j > 0) c += '1' - bin.charAt(p - 1);
                    if (j < width - 1) c += '1' - bin.charAt(p + 1);
                    if (i > 0) c += '1' - bin.charAt(p - width);
                    if (i < height - 1) c += '1' - bin.charAt(p + width);
                    values[i][j] = c;
                }
            } else if (params.getMode() == DrawMode.Blockies128) {
                if (j >= width / 2) {
                    values[i][j] = values[i][width - 1 - j];
                } else {
                    double val = params.prng().rand() * 2.3;
                    values[i][j] = val < 1 ? 0 : (val < 2 ? 1 : 2);
                }
            }
        }

        return values;
    }

    static int[][] shiftMatrix(String bin) {
        int[][] mat = new int[bin.length()][bin.length()];
        for (int i = 0; i < mat.length; i++) {
            mat[i] = rotateHex(bin, i).chars().map(n -> n - '0').toArray();
        }
        return mat;
    }


    static Boolean[] getParities(String hash, DrawParams params){
        String binHash = hexToBin(hash);
        int nBitsPerElement = 4;

        int groupSize = params.getNFunc() * nBitsPerElement;
        int nGroups = params.getMode().length() / groupSize;
        int maxInd = nGroups * groupSize;
        int remainder = params.getMode().length() - maxInd;
        Boolean[] groupParities = new Boolean[groupSize + remainder / 2];
        for (int i = 0; i < groupSize; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < maxInd; j += groupSize) {
                sb.append(binHash.charAt(j));
            }
            groupParities[i] = sb.toString().chars().map(c -> c - '0').sum() % 2 == 0;
        }
        for (int i = 0; i < remainder / 2; i++) {
            groupParities[groupSize + i] = ((binHash.charAt(maxInd + i) - '0') + (binHash.charAt(maxInd + remainder / 2 + i) - '0')) % 2 == 0;
        }

        return groupParities;
    }

    static int[] parityDist(String hash1, String hash2, DrawParams params){
        Boolean[] par1 = getParities(hash1, params);
        Boolean[] par2 = getParities(hash2, params);
        IntStream diff = IntStream.of();
        for (int i = 0; i < par1.length; i++) {
            if (par1[i] != par2[i]) diff = IntStream.concat(diff, IntStream.of(i));
        }
        return diff.toArray();
    }

    static int getPaletteShift(String hash){
        return new BigInteger(new StringBuilder(hexToBin(hash)).reverse().toString(), 2).mod(BigInteger.valueOf(23)).intValue();
    }

    static int getSymmetry(String hash){
        //TODO -> Incorporate that mod 13
        //TODO -> see combinatorial2.py
        //TODO -> prove thingies
        String temp = new StringBuilder(hexToBin(hash)).toString();
        int count = 0;
        for (int i = 0; i < temp.length(); i++) {
            if (temp.charAt(i) == '1'){
                count += i == 0 ? 5 : i;
            }
        }
        System.out.println(count);
        return count % 8;
    }


    /**
     * Computes the Haar-PSI distance between two hashes in the current drawing mode.
     *
     * @param pixels1 the first hash's pixels
     * @param pixels2 the second hash's pixels
     * @return the Haar-PSI distance, a double between 0 and 1
     */
    static double psiDist(int[] pixels1, int[] pixels2, String fileName) {
        String path = "/Users/laya/Documents/VisualHashApplet/applet_env/Scripts/python";
        ProcessBuilder pB;
        StringBuilder sb = new StringBuilder();
        assert (pixels1.length == 256 * 256 && pixels2.length == 256 * 256);
        try {
            long time = System.currentTimeMillis();
            String path1 = "src/temp/" + fileName + "_1.csv";
            String path2 = "src/temp/" + fileName + "_2.csv";
            File csvOutputFile = new File(path1);
            PrintWriter pw = new PrintWriter(csvOutputFile);
            IntStream.of(pixels1).forEach(pw::println);
            csvOutputFile = new File(path2);
            pw.close();
            pw = new PrintWriter(csvOutputFile);
            IntStream.of(pixels2).forEach(pw::println);
            pw.close();

            pB = new ProcessBuilder(path, "-u", "src/haar_psi.py", path1, path2, Integer.toString((int) Math.floor(Math.sqrt(pixels1.length))));
            pB.redirectErrorStream(true);
            Process proc = pB.start();
            byte[] results = proc.getInputStream().readAllBytes();
            for (byte result : results) {
                sb.append((char) result);
            }
            //System.out.println((System.currentTimeMillis() - time) / 1000.0 + "s elapsed.");
        } catch (Exception e) {
            System.out.println("Ah non pas une error oh non " + e);
        }
        String result = sb.toString();

        return Double.parseDouble(result);
    }

    static double psiDist(HashDrawer canvas, String input1, String input2, DrawParams params, String fileName) {
        BufferedImage res = new BufferedImage(Applet.HASH_W, Applet.HASH_H, BufferedImage.TYPE_INT_RGB);
        int[] pixels1 = canvas.drawFourierHash(res.createGraphics(), input1, 0, params);
        int[] pixels2 = canvas.drawFourierHash(res.createGraphics(), input2, 0, params);
        return psiDist(pixels1, pixels2, fileName);
    }

    static Color[] buildHSVWheel(int n, DrawParams params) {
        return buildHSVWheel(n, params, null);
    }

    /**
     * Build a wheel of n colors such that all colors are evenly spaced in the hsv space.
     *
     * @param n the number of colors
     * @return an array of evenly spaced colors
     */
    static Color[] buildHSVWheel(int n, DrawParams params, Random r) {
        Color[] wheel = new Color[n];
        if (n == 32) {
            return new Color[]{
                    new Color(0xdf7126),
                    new Color(0xffffff),
                    new Color(0x000000),
                    new Color(0x696a6a),
                    new Color(0x639bff),
                    new Color(0x663931),
                    new Color(0x99e550),
                    new Color(0xeec39a),
                    new Color(0x9badb7),
                    new Color(0x222034),
                    new Color(0x5b6ee1),
                    new Color(0x45283c),
                    new Color(0x4b692f),
                    new Color(0xd95763),
                    new Color(0xfbf236),
                    new Color(0x8f563b),
                    new Color(0x3f3f74),
                    new Color(0x76428a),
                    new Color(0xd9a066),
                    new Color(0x524b24),
                    new Color(0x847e87),
                    new Color(0x323c39),
                    new Color(0x306082),
                    new Color(0x8f974a),
                    new Color(0x37946e),
                    new Color(0xcbdbfc),
                    new Color(0x595652),
                    new Color(0x6abe30),
                    new Color(0xd77bba),
                    new Color(0xac3232),
                    new Color(0x8a6f30),
                    new Color(0x5fcde4)};

        }
        int[] ref = new int[0];
        double shiftCoeff = r == null ? params.prng().rand() : r.nextDouble();
        float shift = (360f / n) * (float) shiftCoeff;
        //System.out.println(shift + " " + shiftCoeff + " " + (float)shiftCoeff);
        for (int i = 0; i < n; i++) {
            if (n == 16) {
                // [0 1 2 3 4 5 6 7 8 9 a b c d e f] -> [0 3 6 9 c f 1 4 7 a d 2 5 8 b e]
                ref = new int[]{0, 3, 6, 9, 0xc, 0xf, 1, 4, 7, 0xa, 0xd, 2, 5, 8, 0xb, 0xe};
            } else if (n == 8) {
                // [0 1 2 3 4 5 6 7] -> [0 3 6 1 4 7 2 5]
                ref = new int[]{0, 3, 6, 1, 4, 7, 2, 5};
            }

            wheel[ref[i]] = new Color(Color.HSBtoRGB(shift + (float) i / n, 0.5f + 0.5f * (ref[i] % 2), 1));
        }
        return wheel;
    }

}
