package src.hashops;

import src.crypto.SecureCrypto;
import src.types.DrawMode;
import src.types.DrawParams;

import java.awt.image.BufferedImage;
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
        for (int i = 0; i < 100; i++) {
            String input = SecureCrypto.getHash(
                            Integer.toHexString(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)))
                    .substring(0, 128 / 4);
            String inputBin = hexToBin(input);
            int[][][] allAttacks = {AttackIndices.sameParity4bits, AttackIndices.sameParity6bits, AttackIndices.sameParity8bits, AttackIndices.sameParity10bits};
            boolean attackFound = false;
            for (int j = 0; j < allAttacks.length; j++) {
                if (attackFound) break;
                for (int[] indices : allAttacks[j]) {
                    if (attackFound) break;
                    boolean toDraw = true;
                    for (int index : indices) {
                        toDraw &= ((index > 0 && inputBin.charAt(index) == '0') || (index < 0 && inputBin.charAt(-index) == '1'));
                    }
                    if (toDraw) {

                        int[] indicesAbs = Arrays.stream(indices).map(Math::abs).toArray();
                        String flipped = flipBits(input, indicesAbs);

                        distances.add(new DataElem(input, flipped, psiDist(drawer, input, flipped, params, "imageData"), j * 2 + 4));

                        attackFound = true;
                    }
                }
            }
        }
        try {
            File csvOutputFile = new File("src/data/collisions.csv");
            PrintWriter pw = new PrintWriter(csvOutputFile);
            Stream.of(distances).forEach(pw::println);
            pw.close();

        } catch (Exception e) {
            System.out.println("Something went wrong");
        }
    }

    private static class DataElem {
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

        @Override
        public String toString() {
            return "\n(" + originalHash + ", " + flippedHash + ", " + dist + ")";
        }
    }
}
