package src.hashops;

import src.crypto.SecureCrypto;
import src.fourier.ComplexNumber;
import src.fourier.InverseFFT;
import src.fourier.TwoDArray;
import src.types.DrawMode;
import src.types.DrawParams;
import src.ui.Applet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static src.hashops.HashTransform.*;

public class DataGeneration {

    public static void main2(String[] args) {
        HashDrawer drawer = new HashDrawer();
        DrawParams params = new DrawParams(DrawMode.FourierCartesian128);
        ArrayList<DataElem> distances = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            String input = SecureCrypto.getHash(
                            Integer.toHexString(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)))
                    .substring(0, 128 / 4);
            String inputBin = hexToBin(input);
            int[][][] allAttacks = {AttackIndices.sameParity4bits, AttackIndices.sameParity6bits, AttackIndices.sameParity8bits, AttackIndices.sameParity10bits};
            DataElem max = new DataElem(input, "", 0.0, 0);
            for (int j = 0; j < allAttacks.length; j++) {
                for (int[] indices : allAttacks[j]) {
                    boolean toDraw = true;

                    for (int index : indices) {
                        toDraw &= ((index > 0 && inputBin.charAt(index) == '0') || (index < 0 && inputBin.charAt(-index) == '1'));
                    }
                    if (toDraw) {

                        int[] indicesAbs = Arrays.stream(indices).map(Math::abs).toArray();
                        String flipped = flipBits(input, indicesAbs);
                        double dist = psiDist(drawer, input, flipped, params, "imageData");
                        if (dist > max.getDist()){
                            max.setDist(dist);
                            max.setFlippedHash(flipped);
                            max.setN(2 * j + 4);
                        }

                    }
                }
            }
            distances.add(max);
        }
        try {
            File csvOutputFile = new File("src/data/collisions100.csv");
            PrintWriter pw = new PrintWriter(csvOutputFile);
            Stream.of(distances).forEach(pw::println);
            pw.close();

        } catch (Exception e) {
            System.out.println("Something went wrong");
        }
        System.out.println("Finished in " + (System.currentTimeMillis() - start) / 1000.0 + "s");
    }

    public static void main(String[] args){
//        TwoDArray pixels = new TwoDArray(256,256);
//        for (int i = 0; i < 256; i++) {
//            for (int j = 0; j < 256; j++) {
//                pixels.values[i][j] = new ComplexNumber((i / 60f), j % 2);
//            }
//        }
//
//        TwoDArray result = new InverseFFT().transform(pixels);
//        System.out.println(Arrays.deepToString(result.values[1]));

        int mod = PERM_PRIME * SHIFT_PRIME * SYMMETRY_PRIME;
        int hashLength = 256;
        long[] vals = new long[hashLength];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = powerMod(2L, i, mod);
        }

//        long[][] sums2 = new long[hashLength][hashLength];
//        for (int i = 0; i < hashLength; i++) {
//            for (int j = 0; j < hashLength; j++) {
//                sums2[i][j] = (vals[i] + vals[j]) % mod;
//            }
//        }

        long[][][] sums3 = new long[hashLength][hashLength][hashLength];
        for (int i = 0; i < hashLength; i++) {
            for (int j = 0; j < hashLength; j++) {
                for (int k = 0; k < hashLength; k++) {
                    sums3[i][j][k] = (vals[i] + vals[j] + vals[k]) % mod;
                    if(sums3[i][j][k] == 0) System.out.println("(3,0): " + i + " " + j + " " + k);
                    if(sums3[i][j][k] == mod - 1) System.out.println("(3, -1): " + i + " " + j + " " + k);
                }
            }
        }
//        HashSet<Set<Integer>> combs40 = new HashSet<>();
//        HashSet<Set<Integer>> combs41 = new HashSet<>();
//        long[][][][] sums4 = new long[hashLength][hashLength][hashLength][hashLength];
//        for (int i = 0; i < hashLength; i++) {
//            for (int j = 0; j < hashLength; j++) {
//                for (int k = 0; k < hashLength; k++) {
//                    for (int l = 0; l < hashLength; l++) {
//                        sums4[i][j][k][l] = (vals[i] + vals[j] + vals[k] + vals[l]) % mod;
//                        if (sums4[i][j][k][l] == 0) combs40.add(new HashSet<>(Arrays.asList(i, j, k, l)));//System.out.println("(4,0): " + i + " " + j + " " + k + " " + l);
//                        if (sums4[i][j][k][l] == mod - 1) combs41.add(new HashSet<>(Arrays.asList(i, j, k, l)));//System.out.println("(4,-1): " + i + " " + j + " " + k +  " "  + l);
//                    }
//                }
//            }
//        }
//        System.out.println(combs40.size() + " " + combs40);
//        System.out.println(combs41.size() + " " + combs41);
//
//        DrawParams params = new DrawParams(DrawMode.FourierCartesian128);
//        HashDrawer drawer = new HashDrawer();
//        int[] pixels = drawer.drawFourierHash(new BufferedImage(256,256, BufferedImage.TYPE_INT_RGB).createGraphics(), Applet.DEFAULT_HASH.substring(0, 32), 0, params);
//        System.out.println(Arrays.toString(pixels));
    }

    public static long powerMod(long base, long exponent, long modulus){
        if (modulus == 1) return 0;
        long result = 1;
        base = base % modulus;
        while (exponent > 0) {
            if (exponent % 2 == 1)  //odd number
                result = (result * base) % modulus;
            exponent = exponent >> 1; //divide by 2
            base = (base * base) % modulus;
        }
        return result;
    }

    public static class DataElem {
        String originalHash;
        String flippedHash;
        double dist;
        int n;

        public DataElem(String s1, String s2, double dist, int n) {
            originalHash = s1;
            flippedHash = s2;
            this.dist = dist;
            this.n = n;
        }

        public double getDist() {
            return dist;
        }

        public void setDist(double dist) {
            this.dist = dist;
        }

        public void setN(int n) {
            this.n = n;
        }

        public void setFlippedHash(String flippedHash) {
            this.flippedHash = flippedHash;
        }

        public String getFlippedHash() {
            return flippedHash;
        }
        public String getOriginalHash() {
            return originalHash;
        }

        @Override
        public String toString() {
            return "\n(" + originalHash + ", " + flippedHash + ", " + dist + ", " + n + ")";
        }

        public static DataElem fromString(String s){
            String[] elems = s.replace("(", "").replace(")", "").replace("]", "").split(",");
            return new DataElem(elems[0].strip(), elems[1].strip(), Double.parseDouble(elems[2].strip()), Integer.parseInt(elems[3].strip()));
        }
    }
}
