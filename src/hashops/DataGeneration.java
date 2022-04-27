package src.hashops;

import src.crypto.SecureCrypto;
import src.types.DrawMode;
import src.types.DrawParams;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static src.hashops.HashTransform.*;

public class DataGeneration {

    public static void main(String[] args) {
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
