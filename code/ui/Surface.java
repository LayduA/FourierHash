package code.ui;

import code.*;
import code.types.Distance;
import code.types.DrawMode;
import code.fourier.ComplexNumber;
import code.fourier.FFT;
import code.fourier.InverseFFT;
import code.fourier.TwoDArray;
import code.transform.Blockies;
import code.transform.TransformHash;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static code.fourier.ImgMod30.shiftOrigin;
import static code.ui.Applet.*;

public class Surface extends JPanel {

    private static final int DEPTH = 4;

    public static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int x, y;
        int ww = src.getWidth();
        int hh = src.getHeight();
        int[] ys = new int[h];
        for (y = 0; y < h; y++)
            ys[y] = y * hh / h;
        for (x = 0; x < w; x++) {
            int newX = x * ww / w;
            for (y = 0; y < h; y++) {
                int col = src.getRGB(newX, ys[y]);
                img.setRGB(x, y, col);
            }
        }
        return img;
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    private void drawLineHash(Graphics g, int startX, int startY, int endX, int endY, Color color, int shift) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(shift == 2 ? 2 : 3));
        g2d.setColor(color);
        g.drawLine(startX + getShiftX(shift), startY + getShiftY(shift), endX + getShiftX(shift), endY + getShiftY(shift));
    }

    private void drawRectHash(Graphics g, Color c, int x, int y, int w, int h, int shift) {
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setColor(c);
        g2d.fillRect(x + getShiftX(shift), y + getShiftY(shift), w, h);
        g2d.dispose();
    }

    public void drawHash(Graphics g, String hash, int shift, DrawMode drawMode, Blockies prng) {
        Graphics2D g2d = (Graphics2D) g;
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHints(rh);
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(0, 0, 0));

        if (drawMode == DrawMode.Antoine256 || drawMode == DrawMode.AntoineShift256 || drawMode == DrawMode.Adjacency1_256 || drawMode == DrawMode.Adjacency2_256 || drawMode == DrawMode.Blockies128) {
            drawAntoineHash(g, hash, shift, drawMode, prng);
        } else if (drawMode.toString().startsWith("GridLines")) {
            drawGridLinesHash(g, hash, shift, drawMode);
        } else if (drawMode == DrawMode.Landscape32) {
            drawLandscapeHash(g, hash, shift);
        } else if (drawMode == DrawMode.Random128) {
            drawFuncHash(g, hash, shift, DEPTH);
        } else if (drawMode.name().startsWith("Fourier")) {
            drawFourierHash(g, hash, shift, drawMode);
        }
        if (shift == 2) resetShiftY();

    }

    public void drawFuncHash(Graphics g, String hash, int shift, int depth) {

        BufferedImage bi = new BufferedImage(getHashWidth(shift), getHashHeight(shift), BufferedImage.TYPE_INT_RGB);
        GenFunc fr = GenFunc.randomFunc(hash, depth);
        GenFunc fg = GenFunc.randomFunc(hash.substring(1) + hash.charAt(0), depth);
        GenFunc fb = GenFunc.randomFunc(hash.substring(2) + hash.substring(0, 2), depth);
        // System.out.println(fb);
        int[] flatArrayR = new int[bi.getWidth() * bi.getHeight()];
        int[] flatArrayG = new int[bi.getWidth() * bi.getHeight()];
        int[] flatArrayB = new int[bi.getWidth() * bi.getHeight()];
        double rgb;
        double doubleR, doubleG, doubleB;
        for (int i = 0; i < bi.getWidth(); i++) {
            for (int j = 0; j < bi.getHeight(); j++) {
                doubleR = fr.eval((i * 2.0) / bi.getWidth() - 1, (j * 2.0) / bi.getHeight() - 1);
                doubleG = fg.eval((i * 2.0) / bi.getWidth() - 1, (j * 2.0) / bi.getHeight() - 1);
                doubleB = fb.eval((i * 2.0) / bi.getWidth() - 1, (j * 2.0) / bi.getHeight() - 1);
                rgb = (int) Math.floor(doubleR * 127.5 + 127.5) << 16 | (int) Math.floor(doubleG * 127.5 + 127.5) << 8 | (int) Math.floor(doubleB * 127.5 + 127.5);

                bi.setRGB(i, j, (int) rgb);
                flatArrayR[i * bi.getWidth() + j] = (int) Math.floor(doubleR * 127.5 + 127.5);
                flatArrayG[i * bi.getWidth() + j] = (int) Math.floor(doubleG * 127.5 + 127.5);
                flatArrayB[i * bi.getWidth() + j] = (int) Math.floor(doubleB * 127.5 + 127.5);
            }
        }

        g.drawImage(bi, getShiftX(shift), getShiftY(shift), null);// getShiftX(shift, 5, 15, 985), getShiftY(shift, 5,

        // Spectral analysis
        double[] amplitudeR = new FFT(flatArrayR, bi.getWidth(), bi.getHeight()).output.getMagnitude();
        // System.out.println(Arrays.stream(amplitudeR).summaryStatistics());
        double[][] amplitude2dR = new double[bi.getWidth()][bi.getHeight()];
        double[] amplitudeG = new FFT(flatArrayG, bi.getWidth(), bi.getHeight()).output.getMagnitude();
        double[][] amplitude2dG = new double[bi.getWidth()][bi.getHeight()];
        TwoDArray blue = new FFT(flatArrayB, bi.getWidth(), bi.getHeight()).output;
        double[] amplitudeB = blue.getMagnitude();
        // double[] phaseB = blue.getPhase();
        double[][] amplitude2dB = new double[bi.getWidth()][bi.getHeight()];
        double[][] outputR;
        double[][] outputG;
        double[][] outputB;
        BufferedImage spectrum = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < amplitudeR.length; i++) {

            // acc += GenFunc.cap(amplitude[i], 0, 1.0);

            amplitude2dR[i / bi.getHeight()][i % bi.getHeight()] = GenFunc.cap(amplitudeR[i], 0, 1.0);
            amplitude2dG[i / bi.getHeight()][i % bi.getHeight()] = GenFunc.cap(amplitudeG[i], 0, 1.0);
            amplitude2dB[i / bi.getHeight()][i % bi.getHeight()] = GenFunc.cap(amplitudeB[i], 0, 1.0);
        }

        // System.out.println(Arrays.stream(amplitudeB).summaryStatistics());

        outputR = shiftOrigin(amplitude2dR);
        outputG = shiftOrigin(amplitude2dG);
        outputB = shiftOrigin(amplitude2dB);

        int rVal, gVal, bVal;
        // System.out.println(Arrays.stream(phaseB).summaryStatistics());
        for (int i = 0; i < outputR.length; i++) {
            for (int j = 0; j < outputR[0].length; j++) {
                rVal = (int) Math.floor(outputR[i][j] * 255);
                gVal = (int) Math.floor(outputG[i][j] * 255);
                bVal = (int) Math.floor(outputB[i][j] * 255); // (int) (360 * (Math.PI/2 + B[i * outputB[0].length + j])
                // / Math.PI);
                // rVal = (int)(200 * 255 * inverseFFTR[i* outputR[0].length + j]);
                // gVal = (int)(200 * 255 * inverseFFTG[i* outputR[0].length + j]);
                // bVal = (int)(200 * 255 * inverseFFTB[i* outputR[0].length + j]);
                spectrum.setRGB(i, j, rVal << 16 | gVal << 8 | bVal);
            }
        }
        // null);
        if (shift == 1)
            g.drawImage(spectrum, getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);
    }

    private double[] normalizeMean(double[] in) {
        double mean = Arrays.stream(in).average().isPresent() ? Arrays.stream(in).average().getAsDouble() : -1;
        return Arrays.stream(in).map(d -> d / mean).toArray();
    }

    private void setValues(int i, int j, TwoDArray target, double modulus, double phi, int size) {
        //if(i == 1 && j == 0) System.out.println("Set " + modulus + " at 1 0 ");
        target.values[i][j] = ComplexNumber.rPhi(modulus, phi);

        if (j == 0 && i != 0) {
            target.values[size - i][j] = ComplexNumber.rPhi(modulus, phi);
        } else if (i == 0 && j != 0) {
            target.values[i][size - j] = ComplexNumber.rPhi(modulus, phi);
        } else if (i != 0) {
            target.values[size - i][size - j] = ComplexNumber.rPhi(modulus, phi);
        }
    }

    public int[] drawFourierHash(Graphics g, String hash, int shift, DrawMode drawMode){
        return drawFourierHash(g, hash, shift, drawMode, drawMode.getDist(), drawMode.corr());
    }
    public int[] drawFourierHash(Graphics g, String hash, int shift, DrawMode drawMode, Distance dist, double corr) {
        int spectrumWidth = Integer.parseInt(drawMode.name().substring(drawMode.name().length() - 3), 10);
        String binHash = TransformHash.hexToBin(hash);
        assert binHash.length() == spectrumWidth;
        long moduloMod = new BigInteger(hash, 16).mod(new BigInteger("ffffffffffffffad", 16)).longValue();
        long moduloPhase = new BigInteger(hash, 16).mod(new BigInteger("ffffffffffffffc5", 16)).longValue();

        BufferedImage spectrum = new BufferedImage(spectrumWidth, spectrumWidth, BufferedImage.TYPE_INT_RGB);

        double modulusR, modulusG, modulusB;
        double phiR, phiG, phiB;
        double frequencyCoeff;

        int ind = 0;

        Random randomMod = new Random(moduloMod);
        Random randomPhase = new Random(moduloPhase);

        TwoDArray datSpecB = new TwoDArray(spectrumWidth, spectrum.getHeight());
        TwoDArray datSpecG = new TwoDArray(spectrumWidth, spectrum.getHeight());
        TwoDArray datSpecR = new TwoDArray(spectrumWidth, spectrum.getHeight());

        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrumWidth / 2; j++) {

                frequencyCoeff = dist.dist(i, j, drawMode.cut());
                if (drawMode == DrawMode.FourierRModDPhase256 || drawMode == DrawMode.FourierRModRPhase256) {
                    modulusR = frequencyCoeff * randomMod.nextDouble();
                    modulusG = frequencyCoeff * randomMod.nextDouble();
                    modulusB = frequencyCoeff * randomMod.nextDouble();
                } else if (drawMode == DrawMode.FourierDModRPhase256){
                    modulusR = frequencyCoeff * (binHash.charAt(ind % spectrumWidth) - '0');
                    modulusG = frequencyCoeff * (binHash.charAt((ind + 1) % spectrumWidth) - '0');
                    modulusB = frequencyCoeff * (binHash.charAt((ind + 2) % spectrumWidth) - '0');
                    if (frequencyCoeff > 0 && !(0 < i && i < spectrumWidth / 2 && j == 0)) ind += 3;
                } else {
                    modulusR = frequencyCoeff * 0.5 * (binHash.charAt(ind % spectrumWidth) - '0' + 1);
                    modulusG = frequencyCoeff * 0.5 * (binHash.charAt((ind + 1) % spectrumWidth) - '0' + 1);
                    modulusB = frequencyCoeff * 0.5 * (binHash.charAt((ind + 2) % spectrumWidth) - '0' + 1);
                    if (frequencyCoeff > 0 && !(0 < i && i < spectrumWidth / 2 && j == 0)) ind += 3;
                }

                if (drawMode == DrawMode.FourierDModRPhase256 || drawMode == DrawMode.FourierRModRPhase256) {
                    phiR = (randomPhase.nextDouble() - 0.5) * Math.PI;
                    phiG = (randomPhase.nextDouble() - 0.5) * Math.PI;
                    phiB = (randomPhase.nextDouble() - 0.5) * Math.PI;
                } else {
                    phiR = Math.PI / 2.0 * (1 - 2 * binHash.charAt(ind % spectrumWidth) - '0');
                    phiG = Math.PI / 2.0 * (1 - 2 * binHash.charAt((ind + 1) % spectrumWidth) - '0');
                    phiB = Math.PI / 2.0 * (1 - 2 * binHash.charAt((ind + 2) % spectrumWidth) - '0');
                    //if ((drawMode == DrawMode.FourierDModDPhase256 || frequencyCoeff > 0) && !(0 < i && i < spectrumWidth / 2 && j == 0)) ind += 4;
                }

                setValues(i, j, datSpecR, modulusR, phiR, spectrumWidth);
                setValues(i, j, datSpecG, modulusG, phiG, spectrumWidth);
                setValues(i, j, datSpecB, modulusB, phiB, spectrumWidth);

            }
        }
        if (ind < 256 && drawMode != DrawMode.FourierRModRPhase256) {
            //System.out.println("ind = " + ind + ", MUST BE >= 256");
        }
        TwoDArray inverseFFTR = new InverseFFT().transform(datSpecR);
        TwoDArray inverseFFTG = new InverseFFT().transform(datSpecG);
        TwoDArray inverseFFTB = new InverseFFT().transform(datSpecB);

        double[] imageValuesR = inverseFFTR.getMagnitude();
        double[] imageValuesG = inverseFFTG.getMagnitude();
        double[] imageValuesB = inverseFFTB.getMagnitude();

        double[] magR = datSpecR.getMagnitude();
        double[] magG = datSpecG.getMagnitude();
        double[] magB = datSpecB.getMagnitude();

        magR = normalize(magR);
        magG = normalize(magG);
        magB = normalize(magB);

        imageValuesR = inverseFFTR.DCToCentre(normalizeMean(imageValuesR));
        imageValuesG = inverseFFTG.DCToCentre(normalizeMean(imageValuesG));
        imageValuesB = inverseFFTB.DCToCentre(normalizeMean(imageValuesB));

        BufferedImage res = new BufferedImage(spectrumWidth, spectrumWidth, BufferedImage.TYPE_INT_RGB);
        int[] resint = new int[res.getWidth() * res.getHeight()];
        int coordY;
        int pixelLoc, pixelLocMod;

        double[] bigMagR = new double[magR.length];
        double[] bigMagG = new double[magG.length];
        double[] bigMagB = new double[magB.length];
        int sum = binHash.chars().map(c -> c - '0').sum() % 2;

        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrum.getHeight(); j++) {
                coordY = i > spectrumWidth / 2 ? j == 0 || sum == 1 ? j : spectrum.getHeight() - j : j;
                pixelLoc = j * spectrumWidth + i;
                pixelLocMod = getPixelLocSpectrum(i, j, spectrumWidth, drawMode);
                spectrum.setRGB(i, j, (Math.min(255, (int) (magR[pixelLocMod] * 255)) << 16) | (Math.min(255, (int) (magG[pixelLocMod] * 255)) << 8) | Math.min(255, (int) (magB[pixelLocMod] * 255)));
                //res.setRGB(i, i > spectrumWidth / 2 ? j == 0 ? j : spectrum.getHeight() - j : j, (int) (255 * corrCoeff * imageValuesR[j * spectrumWidth + i] % 255) << 16 | (int) (255 * corrCoeff * imageValuesG[j * spectrumWidth + i] % 255) << 8 | (int) (255 * corrCoeff * imageValuesB[j * spectrumWidth + i] % 255));

                bigMagR[i + j * spectrumWidth] = magR[pixelLocMod];
                bigMagG[i + j * spectrumWidth] = magG[pixelLocMod];
                bigMagB[i + j * spectrumWidth] = magB[pixelLocMod];

                res.setRGB(i, coordY, getRGBCorr(imageValuesR[pixelLoc], imageValuesG[pixelLoc], imageValuesB[pixelLoc], corr, filtered));
            }
        }
        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrum.getHeight(); j++) {
                resint[i + j * spectrumWidth] = res.getRGB(i, j) & (1 << 24) - 1;
            }
        }

        bigMagR = inverseFFTR.DCToCentre(bigMagR);
        bigMagG = inverseFFTG.DCToCentre(bigMagG);
        bigMagB = inverseFFTB.DCToCentre(bigMagB);

        BufferedImage centerSpectrum = new BufferedImage(spectrumWidth, spectrumWidth, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrumWidth; j++) {
                centerSpectrum.setRGB(i, j, (Math.min(255, (int) (bigMagR[i + j * spectrumWidth] * 255)) << 16) | (Math.min(255, (int) (bigMagG[i + j * spectrumWidth] * 255)) << 8) | Math.min(255, (int) (bigMagB[i + j * spectrumWidth] * 255)));
            }
        }
        g.drawImage(scale(res, getHashWidth(shift), getHashHeight(shift)), getShiftX(shift), getShiftY(shift), null);
        if (shift == 1 || shift == 2) {
            //g.drawImage(spectrum, getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);
            g.drawImage(scale(centerSpectrum, getHashWidth(shift), getHashHeight(shift)), getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);
        }
        return resint;

    }

    private int getRGBCorr(double red, double green, double blue, double corr, boolean filtered) {
        if (filtered) return (corr * red > 0.5 ? 255 : 0) << 16
                | (corr * green > 0.5 ? 255 : 0) << 8
                | (corr * blue > 0.5 ? 0xc5e4 : 0);
        else
            return (int) (255 * corr * red) % 255 << 16 | (int) (255 * corr * green) % 255 << 8 | (int) (255 * corr * blue) % 255;
    }

    private int getPixelLocSpectrum(int i, int j, int spectrumWidth, DrawMode mode) {
        int spectrumWidthPixels = spectrumWidth / (2 * (mode.cut() + 1));
        int x = i > spectrumWidth / 2 ? (i - spectrumWidth / 2) / spectrumWidthPixels + spectrumWidth - (mode.cut() + 1) : i / spectrumWidthPixels;
        int y = j > spectrumWidth / 2 ? (j - spectrumWidth / 2) / spectrumWidthPixels + spectrumWidth - (mode.cut() + 1) : j / spectrumWidthPixels;
        if (x > spectrumWidth - 1) return 0;
        int out = x + y * spectrumWidth;
        return out > spectrumWidth * spectrumWidth - 1 ? 0 : out;
    }

    public double[] normalize(double[] arr) {

        double mean = Arrays.stream(arr).average().getAsDouble();
        double[] temp = Arrays.stream(arr).map(d -> d / (2 * mean)).toArray();
        double min = Arrays.stream(temp).min().getAsDouble();
        double max = Arrays.stream(temp).max().getAsDouble();
        return Arrays.stream(temp).map(d -> (d - min) / (max - min)).toArray();
    }

    public void drawGridHashRGB(Graphics g, int[][] values, int shift, DrawMode drawMode) {

        int sideLength = getHashWidth(shift) / values.length;
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                drawRectHash(g, drawMode.palette()[values[i][j]], j * sideLength, i * sideLength, sideLength, sideLength, shift);
            }
        }
    }

    public void drawAntoineHash(Graphics g, String s, int shift, DrawMode drawMode, Blockies prng) {

        String bin = TransformHash.hexToBin(s);

        int[][] values = TransformHash.extractValues(bin, drawMode, prng);

        drawGridHashRGB(g, values, shift, drawMode);
    }

    private void resetShiftY() {
        SHIFTS_Y[2] = HASH_Y * 3 / 2 + ThreadLocalRandom.current().nextInt(VERT_JITTER) - VERT_JITTER / 2;
    }

    public void drawLandscapeHash(Graphics g, String hex, int shift) {
        long info = Long.parseLong(hex, 16);

        Color[] palette = TransformHash.buildHSVWheel(16);
        Graphics2D g2d = (Graphics2D) g.create();
        int obj, curr_ind;
        // Obj = sky
        curr_ind = 32;
        obj = extract(info, curr_ind, 2);
        curr_ind -= 2;
        GradientPaint gp = new GradientPaint(getShiftX(shift), getShiftY(shift), new Color(0, 0, 0), getShiftX(shift), getShiftY(shift) + 3 * getHashHeight(shift) / 4f, palette[obj + 11]);
        g2d.setPaint(gp);
        drawRectHash(g2d, null, 0, 0, getHashWidth(shift), 2 * getHashHeight(shift) / 3, shift);

        // Obj = sun
        obj = extract(info, curr_ind, 3);
        curr_ind -= 3;
        int SUN_RADIUS = 2 * getHashWidth(shift) / 3 - getHashHeight(shift) / 30;
        float[] hsb = Color.RGBtoHSB(palette[obj].getRed(), palette[obj].getGreen(), palette[obj].getBlue(), null);
        gp = new GradientPaint(getShiftX(shift), getShiftY(shift) + getHashHeight(shift) / 3f, palette[obj], getShiftX(shift), getShiftY(shift) + getHashHeight(shift), Color.getHSBColor(hsb[0], hsb[1], 0));
        g2d.setPaint(gp);
        obj = extract(info, curr_ind, 1);
        curr_ind -= 1;
        Ellipse2D sun = new Ellipse2D.Double(getShiftX(shift) + getHashWidth(shift) / 6.0 + (-1 + 2 * obj) * SUN_RADIUS / 4f, getShiftY(shift) + getHashHeight(shift) / 3.0, SUN_RADIUS, SUN_RADIUS);
        g2d.fill(sun);

        // Obj = ground
        obj = extract(info, curr_ind, 2);
        curr_ind -= 2;
        gp = new GradientPaint(getShiftX(shift), getShiftY(shift) + getHashHeight(shift) * 2 / 3f, new Color(0, 0, 0), getShiftX(shift), getShiftY(shift) + getHashHeight(shift), palette[obj + 5]);
        g2d.setPaint(gp);
        drawRectHash(g2d, null, 0, 2 * getHashHeight(shift) / 3, getHashWidth(shift), getHashHeight(shift) / 3, shift);

        // Obj = mountainsSize
        int sizes = extract(info, curr_ind, 4);
        curr_ind -= 4;
        int shapes = extract(info, curr_ind, 4);
        curr_ind -= 4;
        int colors = extract(info, curr_ind, 8);
        curr_ind -= 8;

        for (int i = 3; i >= 0; i--) {
            // System.out.println((((colors >> (2 * i)) & 0b11) + 4));
            drawMountain(g2d, i, (sizes >> i) & 1, (shapes >> i) & 1, palette[((colors >> (2 * i)) & 0b11) + 4], shift);
        }

        int moonPos = extract(info, curr_ind, 2);
        curr_ind -= 2;
        int moonPat = extract(info, curr_ind, 2);
        curr_ind -= 2;
        int moonCol = extract(info, curr_ind, 2);
        curr_ind -= 2;
        int MOON_RADIUS = getHashHeight(shift) / 6;
        g2d.setColor(palette[moonCol]);

        if (moonPat < 2) {
            Arc2D moon = new Arc2D.Double(getShiftX(shift) + (moonPos) * getHashWidth(shift) / 5.0 + MOON_RADIUS / 2f, getShiftY(shift) + MOON_RADIUS / 2.0, MOON_RADIUS, MOON_RADIUS, 90, -180 + moonPat * 360, Arc2D.OPEN);
            g2d.fill(moon);
        } else {
            Ellipse2D moon = new Ellipse2D.Double(getShiftX(shift) + (moonPos) * getHashWidth(shift) / 5.0 + MOON_RADIUS / 2f, getShiftY(shift) + MOON_RADIUS / 2.0, MOON_RADIUS, MOON_RADIUS);
            g2d.draw(moon);
            if (moonPat == 2) g2d.fill(moon);
        }

        obj = extract(info, curr_ind, 1);
        if (obj == 0)
            drawLineHash(g, getHashWidth(shift) / 10, getHashHeight(shift) / 3, getHashWidth(shift) / 10, 5 * getHashHeight(shift) / 6, Color.WHITE, shift);
        curr_ind -= 1;
        obj = extract(info, curr_ind, 1);
        if (obj == 0)
            drawLineHash(g, 9 * getHashWidth(shift) / 10, getHashHeight(shift) / 3, 9 * getHashWidth(shift) / 10, 5 * getHashHeight(shift) / 6, Color.WHITE, shift);

    }

    private void drawMountain(Graphics2D g, int ind, int size, int shape, Color color, int shift) {
        int height = (1 + size) * getHashHeight(shift) / 6;
        int width = getHashWidth(shift) / 8;
        int posY = getHashHeight(shift) * 6 / 7 - height;

        g.setPaint(color);
        if (shape == 0) {
            drawRectHash(g, null, getHashWidth(shift) / 6 * (ind + 1), posY, width, height, shift);
        } else {
            Path2D path = new Path2D.Double();
            path.moveTo(getShiftX(shift) + getHashWidth(shift) / 6f * (ind + 1), getShiftY(shift) + posY + height);
            path.lineTo(getShiftX(shift) + getHashWidth(shift) / 6f * (ind + 1) + width / 2f, getShiftY(shift) + posY);
            path.lineTo(getShiftX(shift) + getHashWidth(shift) / 6f * (ind + 1) + width, getShiftY(shift) + posY + height);
            path.closePath();
            g.fill(path);
        }
    }

    private int getHashWidth(int shift) {
        if (shift == 2) return HASH_W / 2;
        if (shift > 2) return (HASH_W) / 2;
        return HASH_W;
    }

    private int getHashHeight(int shift) {
        if (shift == 2) return HASH_H / 2;
        if (shift > 2) return (HASH_H / 2);
        return HASH_H;
    }

    private int extract(long source, int index, int length) {
        return (int) ((source & (((1L << length) - 1) << (index - length))) >>> (index - length));
    }

    public void drawGridLinesHash(Graphics g, String hex, int shift, DrawMode drawMode) {
        int length = hex.length() * 4;
        StringBuilder hex256 = new StringBuilder(hex);
        while (length < 256) {
            hex256.append(hex256);
            length = hex256.length() * 4;
        }

        int tileSide = (drawMode == DrawMode.GridLines256 || drawMode == DrawMode.GridLines64 ? 4 : 3);
        BigInteger bin = new BigInteger(hex256.toString(), 16);

        int colorsmaskLength = 4 * tileSide * tileSide - ((4 * tileSide * tileSide) % 8);
        BigInteger maskColors = BigInteger.ONE.shiftLeft(colorsmaskLength).subtract(BigInteger.ONE);
        BigInteger colors = maskColors.shiftLeft(length - colorsmaskLength).and(bin).shiftRight(length - colorsmaskLength);

        int lineMaskLength = length - colorsmaskLength;
        BigInteger maskLines = BigInteger.ONE.shiftLeft(lineMaskLength).subtract(BigInteger.ONE);
        BigInteger lines = maskLines.and(bin);

        int sideLength = getHashWidth(shift) / tileSide;
        int linesInfo;
        int cornerX;
        int cornerY;

        // (FOR NOW) EACH TILE HAS 16 BITS OF INFO : 4 FOR PRIMARY COLOR, 3 FOR CORNER
        // 1, 3 FOR CORNER 2, 3 FOR CORNER 3, 3 FOR CORNER 4

        for (int i = 0; i < tileSide * tileSide; i++) {
            cornerX = (i % tileSide) * sideLength;
            cornerY = (i / tileSide) * sideLength;

            drawRectHash(g, TransformHash.buildHSVWheel(16)[colors.and(BigInteger.valueOf(0b1111).shiftLeft(colorsmaskLength - (i + 1) * 4)).shiftRight(colorsmaskLength - (i + 1) * 4).intValue()], cornerX, cornerY, sideLength, sideLength, shift);

            linesInfo = lines.and(BigInteger.valueOf(0b111_111_111_111).shiftLeft(lineMaskLength - 12 * (i + 1))).shiftRight(lineMaskLength - 12 * (i + 1)).intValue();
            drawLineHashIndices(g, (linesInfo & (0b111 << 9)) >> 9, (linesInfo & (0b111 << 6)) >> 6, cornerX, cornerY, sideLength, shift);
            drawLineHashIndices(g, (linesInfo & (0b111 << 3)) >>> 3, linesInfo & 0b111, (i % tileSide) * sideLength, (i / tileSide) * sideLength, sideLength, shift);

            if ((drawMode == DrawMode.GridLines128 || drawMode == DrawMode.GridLines32) && i == tileSide * tileSide - 1)
                drawRectHash(g, new Color(100, 100, 100), cornerX, cornerY, sideLength, sideLength, shift);
        }

    }

    public void drawLineHashIndices(Graphics g, int index1, int index2, int cornerX, int cornerY, int sideLength, int shift) {
        int[] startCoords = getCoordFromIndex(index1, cornerX, cornerY, sideLength, sideLength / 6);
        if (index1 == index2) {
            drawLineHash(g, startCoords[0], startCoords[1], cornerX + sideLength / 2, cornerY + sideLength / 2, new Color(0, 0, 0) , shift);
        } else {

            int[] endCoords = getCoordFromIndex(index2, cornerX, cornerY, sideLength, sideLength / 6);
            drawLineHash(g, startCoords[0], startCoords[1], endCoords[0], endCoords[1], index1 <= index2 ? new Color(0, 0, 0) : new Color(255, 255, 255), shift);
        }
    }

    private int[] getCoordFromIndex(int index, int cornerX, int cornerY, int sideLength, int margin) {
        int[] coords = new int[]{0, 0};
        switch (index) {
            case 0:
                coords[0] = cornerX + margin;
                coords[1] = cornerY + margin;
                break;
            case 1:
                coords[0] = cornerX + sideLength / 2;
                coords[1] = cornerY + margin;
                break;
            case 2:
                coords[0] = cornerX + sideLength - margin;
                coords[1] = cornerY + margin;
                break;
            case 3:
                coords[0] = cornerX + margin;
                coords[1] = cornerY + sideLength / 2;
                break;
            case 4:
                coords[0] = cornerX + sideLength - margin;
                coords[1] = cornerY + sideLength / 2;
                break;
            case 5:
                coords[0] = cornerX + margin;
                coords[1] = cornerY + sideLength - margin;
                break;
            case 6:
                coords[0] = cornerX + sideLength / 2;
                coords[1] = cornerY + sideLength - margin;
                break;
            case 7:
                coords[0] = cornerX + sideLength - margin;
                coords[1] = cornerY + sideLength - margin;
                break;
            default:
                break;
        }
        return coords;
    }

}
