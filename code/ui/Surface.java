package code.ui;

import static code.ui.Applet.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JPanel;

import code.ComplexNumber;
import code.Crypto;
import code.FFT;
import code.GenFunc;
import code.InverseFFT;
import code.TwoDArray;
import code.transform.Blockies;
import code.transform.TransformHash;

import static code.ImgMod30.*;

class Surface extends JPanel {

    private static final int DEPTH = 4;

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    private void drawLineHash(Graphics g, int startx, int starty, int endx, int endy, Color color, int shift) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(shift == 2 ? 2 : 3));
        g2d.setColor(color);
        g.drawLine(startx + getShiftX(shift), starty + getShiftY(shift), endx +
                getShiftX(shift), endy + getShiftY(shift));
    }

    private void drawRectHash(Graphics g, Color c, int x, int y, int w, int h, int shift) {
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setColor(c);
        g2d.fillRect(x + getShiftX(shift), y + getShiftY(shift), w, h);
        g2d.dispose();
    }
    // private void drawRectHash(Graphics g, Color c, int x, int y, int w, int h,
    // int shift, int gradientDir) {
    // Graphics2D g2d = (Graphics2D) g.create();
    // GradientPaint gp = new GradientPaint(x + SHIFTS[shift],y + SHIFTY[shift], c,
    // x + SHIFTS[shift]+ w, x + SHIFTS[shift] +h, new Color(0,0,0));
    // g2d.setPaint(gp);
    // g2d.fillRect(x + SHIFTS[shift], y + SHIFTY[shift], w, h);
    // g2d.dispose();
    // }

    public void drawHash(Graphics g, String hash, int shift, DMODE drawMode, Blockies prng) {
        Graphics2D g2d = (Graphics2D) g;
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHints(rh);
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(0, 0, 0));
        // g2d.drawRect(shift == 0 ? 0 : SHIFTS[shift], shift == 0 ? 0 : SHIFTY[shift],
        // shift == 2 ? HASH_W / 2 : HASH_W, shift == 2 ? HASH_H / 2 : HASH_H);
        if (drawMode == DMODE.Antoine256 || drawMode == DMODE.AntoineShift256 || drawMode == DMODE.Adjacency1_256
                || drawMode == DMODE.Adjacency2_256 || drawMode == DMODE.Blockies128) {
            drawAntoineHash(g, hash, shift, drawMode, prng);
        } else if (drawMode.toString().substring(0, 9).equals("GridLines")) {
            drawGridLinesHash(g, hash, shift, drawMode);
        } else if (drawMode == DMODE.Landscape32) {
            drawLandscapeHash(g, hash, shift, drawMode);
        } else if (drawMode == DMODE.Random128) {
            drawFuncHash(g, hash, shift, DEPTH);
        } else if (drawMode.toString().substring(0, 7).equals("Fourier")) {
            drawFourierHashDet(g, hash, shift, drawMode, prng, CUBIC, 2.0);
        }
        if (shift == 2)
            resetShiftY();

    }

    public void drawGridHash(Graphics g, int[][] values, int shift) {
        int sideLength = getHashWidth(shift) / values.length;
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                drawRectHash(g, values[i][j] == 1 ? new Color(255, 255, 255) : new Color(0, 0, 0), j * sideLength,
                        i * sideLength, sideLength, sideLength, shift);
            }
        }
    }

    public int mathToRGB(double d) {
        // From [-1, 1] to [0, 255]
        return (int) Math.floor((d + 1) / 2.0 * 255);
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
                rgb = (int) Math.floor(doubleR * 127.5 + 127.5) << 16 | (int) Math.floor(doubleG *
                        127.5 + 127.5) << 8
                        | (int) Math.floor(doubleB * 127.5 + 127.5);

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

    public final static int BELL = 0;
    public final static int MANHATTAN = 1;
    public final static int EUCLIDEAN = 2;
    public final static int CUBIC = 3;
    public final static int MULT = 4;
    public final static int SQUARE = 5;
    public final static int XY = 6;

    private double dist(double x, double y, int func) {
        if (func == MULT)
            return x * y == 0 ? 4 : 2/Math.abs(x * y);
        if (func == MANHATTAN)
            return (Math.abs(x) + Math.abs(y)) == 0 ? 4 : 2 / (Math.abs(x) + Math.abs(y));
        double dist = Math.sqrt(x * x + y * y);
        if (func == EUCLIDEAN)
            return dist == 0 ? 4 : 2/dist;
        if (func == BELL)
            return Math.exp(-0.5 * dist * dist / 1.5);
        if (func == CUBIC)
            return (Math.abs(x) * x * x + Math.abs(y) * y * y) == 0 ? 4 : 2 / (Math.abs(x) * x * x + Math.abs(y) * y * y);
        if (func == SQUARE)
            return (x * x + y * y) == 0 ? 4 : 2 /(x * x + y * y);
        if (func == XY)
            return (x * x + y * y) == 0 ? 4 : x * y * 2.0/ (x*x + y*y);  
        return 0;
    }

    private double[] normalizeMean(double[] in) {
        double mean = Arrays.stream(in).average().getAsDouble();
        return Arrays.stream(in).map(d -> d / mean).toArray();
    }

    private void setValues(int i, int j, TwoDArray target, double modulus, double phi, int size) {
        target.values[i][j] = ComplexNumber.rPhi(modulus, phi);
        if (j == 0 && i != 0) {
            target.values[size - i][j] = ComplexNumber.rPhi(modulus, phi);
        } else if (i == 0 && j != 0)
            target.values[i][size - j] = ComplexNumber.rPhi(modulus, phi);
        else if (i != 0 && j != 0)
            target.values[size - i][size - j] = ComplexNumber.rPhi(modulus,
                    phi);
    }

    public int[] drawFourierHashDet(Graphics g, String hash, int shift, DMODE drawmode, Blockies prng, int dist, double corr) {
        int spectrumWidth = Integer.parseInt(drawmode.name().substring(7), 10);
        int width = 256;
        String binHash = TransformHash.hexToBin(hash);
        BufferedImage spectrum = new BufferedImage(spectrumWidth, spectrumWidth, BufferedImage.TYPE_INT_RGB);

        double valueR, valueG, valueB;
        double temp_val;

        TwoDArray datSpecB = new TwoDArray(spectrum.getWidth(), spectrum.getHeight());
        TwoDArray datSpecG = new TwoDArray(spectrum.getWidth(), spectrum.getHeight());
        TwoDArray datSpecR = new TwoDArray(spectrum.getWidth(), spectrum.getHeight());
        int[][] phases = TransformHash.shiftMatrix(binHash);
        int ind = 0;
        double phi;
        BigInteger bigint = new BigInteger(hash, 16);
        int mod = bigint.mod(BigInteger.valueOf(1999)).intValue();
        Random random = new Random(mod);
        // Random random = new Random(hash.chars().map(c -> c - '0').sum());
        int jMax = 8;
        // RHOMBUS
        int iMax = 8;
        // for (int dist = 0; dist < 6; dist++) {
        // System.out.println("dist = " + dist);
        // for (double corr = 0.7; corr < 2.5; corr += 0.1) {
        // System.out.println("corr = "+ corr);
        // double ratio = 0;
        // for (int epoch = 1; epoch < 10; epoch++) {
        // binHash = TransformHash.hexToBin(Crypto.getHash("Bonjour " + epoch));

        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < jMax; j++) {

                // setValues((int) (40 * Math.random()) , (int) (40 * Math.random()), datSpecR,
                // 0.2, Math.PI / 2, bi.getWidth());

                // for (int i = -iMax / 2; i <= iMax / 2; i++) {
                // for (int j = -(iMax / 2 - Math.abs(i)); j <= iMax / 2 - Math.abs(i); j++) {

                // RECTANGLE
                if ((i == iMax - 1 && j != 0) || (j == iMax - 1 && i != 0)) {
                    continue;
                }

                phi = Math.PI;//(random.nextDouble() - 0.5) * Math.PI;
                temp_val = dist(i, j, dist);
                temp_val = GenFunc.cap(temp_val, 0.01, 100);

                valueB = temp_val * (binHash.charAt(ind * 3 % spectrum.getWidth()) - '0');
                valueR = temp_val * (binHash.charAt((ind * 3 + 1) % spectrum.getWidth()) - '0');
                valueG = temp_val * (binHash.charAt((ind * 3 + 2) % spectrum.getWidth()) - '0');

                // setValues(0, i * jMax + j, datSpecR, valueR, phi, bi.getWidth());
                // setValues(0, i * jMax + j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(0, i * jMax + j, datSpecB, valueB, phi, bi.getWidth());

                ind++;

                setValues(i, j, datSpecR, valueR, phi, spectrum.getWidth());
                setValues(i, j, datSpecG, valueG, phi, spectrum.getWidth());
                setValues(i, j, datSpecB, valueB, phi, spectrum.getWidth());
                if (i * j > 0) {
                    ind++;
                    valueB = temp_val * (binHash.charAt(ind * 3 % spectrum.getWidth()) - '0');

                    valueR = (temp_val) * (binHash.charAt(ind % spectrum.getWidth() + 1) - '0');
                    random.nextDouble();

                    valueG = (temp_val) * (binHash.charAt(ind % spectrum.getWidth() + 2) - '0');
                    random.nextDouble();
                    setValues(spectrum.getWidth() - i, j, datSpecR, valueR, phi, spectrum.getWidth());
                    setValues(spectrum.getWidth() - i, j, datSpecG, valueG, phi, spectrum.getWidth());
                    setValues(spectrum.getWidth() - i, j, datSpecB, valueB, phi, spectrum.getWidth());
                }

                // // RHOMBUS
                // phi = 2*Math.PI / 1999.0 * new BigInteger(binHash,
                // 2).mod(BigInteger.valueOf(1999)).intValue();//(phases[(i % 256 + 256) %
                // 256][(j % 256 + 256) % 256] - 0.5) * Math.PI;// (random.nextDouble() -
                // // 0.5) * Math.PI * 2;
                // temp_val = dist(i, j, MANHATTAN);
                // temp_val = temp_val < 0.01 ? 4 : 1 / temp_val;
                // temp_val = GenFunc.cap(temp_val, 0.05, 100);

                // valueR = temp_val * (binHash.charAt((ind * 3 + 1) % 256) - '0');//
                // random.nextDouble();///16)
                // valueG = temp_val * (binHash.charAt((ind * 3 + 2) % 256) - '0');
                // valueB = temp_val * (binHash.charAt(ind * 3 % 256) - '0');
                // ind++;
                // if (i >= 0 && j >= 0) {
                // setValues(i, j, datSpecR, valueR, phi, bi.getWidth());
                // setValues(i, j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(i, j, datSpecB, valueB, phi, bi.getWidth());
                // } else if (i < 0 && j >= 0) {
                // setValues(bi.getWidth() + i, Math.abs(j), datSpecR, valueR, phi,
                // bi.getWidth());
                // setValues(bi.getWidth() + i, Math.abs(j), datSpecG, valueG, phi,
                // bi.getWidth());
                // setValues(bi.getWidth() + i, Math.abs(j), datSpecB, valueB, phi,
                // bi.getWidth());
                // }
                // setValues(i, j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(i, j, datSpecB, valueB, phi, bi.getWidth());

                // //CROSS
                // setValues(i * 7 + j, 0, datSpecR, valueR, phi, bi.getWidth());
                // setValues(i * 7 + j, 0, datSpecG, valueG, phi, bi.getWidth());
                // setValues(i * 7 + j, 0, datSpecB, valueB, phi, bi.getWidth());
                // setValues(0, i * 7 + j, datSpecR, valueR, phi, bi.getWidth());
                // setValues(0, i * 7 + j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(0, i * 7 + j, datSpecB, valueB, phi, bi.getWidth());
                // if (i > 0 && j > 0) {
                // ind++;
                // valueB = temp_val * (binHash.charAt(ind * 3 % 256) - '0');

                // valueR = (temp_val) * (binHash.charAt(ind % 120 + 1) - '0');//
                // random.nextDouble();///16)

                // valueG = (temp_val) * (binHash.charAt(ind % 120 + 2) - '0');//
                // random.nextDouble();///16)

                // CROSS
                // setValues(bi.getWidth() - 1, ((i - 1) * 7 + j - 1), datSpecR, valueR, phi,
                // bi.getWidth());
                // setValues(bi.getWidth() - 1, ((i - 1) * 7 + j - 1), datSpecG, valueG, phi,
                // bi.getWidth());
                // setValues(bi.getWidth() - 1, ((i - 1) * 7 + j - 1), datSpecB, valueB, phi,
                // bi.getWidth());
                // }

                // // SNOWFLAKE
                // setValues(i * jMax + j, 0, datSpecR, valueR, phi, bi.getWidth());
                // setValues(i * jMax + j, 0, datSpecG, valueG, phi, bi.getWidth());
                // setValues(i * jMax + j, 0, datSpecB, valueB, phi, bi.getWidth());
                // ind++;

                // valueB = temp_val * (binHash.charAt(ind * 3 % 256) - '0');

                // valueR = temp_val * (binHash.charAt((ind * 3 + 1) % 256) - '0');//
                // random.nextDouble();///16)

                // valueG = temp_val * (binHash.charAt((ind * 3 + 2) % 256) - '0');//
                // random.nextDouble();///16)
                // setValues(0, i * jMax + j, datSpecR, valueR, phi, bi.getWidth());
                // setValues(0, i * jMax + j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(0, i * jMax + j, datSpecB, valueB, phi, bi.getWidth());

                // ind++;

                // valueB = temp_val * (binHash.charAt(ind * 3 % 256) - '0');

                // valueR = temp_val * (binHash.charAt((ind * 3 + 1) % 256) - '0');//
                // random.nextDouble();///16)

                // valueG = temp_val * (binHash.charAt((ind * 3 + 2) % 256) - '0');//
                // random.nextDouble();///16)
                // setValues(i * jMax + j, i * jMax + j, datSpecR, valueR, phi, bi.getWidth());
                // setValues(i * jMax + j, i * jMax + j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(i * jMax + j, i * jMax + j, datSpecB, valueB, phi, bi.getWidth());

                // ind++;

                // valueB = temp_val * (binHash.charAt(ind * 3 % 256) - '0');

                // valueR = temp_val * (binHash.charAt((ind * 3 + 1) % 256) - '0');//
                // random.nextDouble();///16)

                // valueG = temp_val * (binHash.charAt((ind * 3 + 2) % 256) - '0');//
                // random.nextDouble();///16)
                // setValues(i * jMax + j, i * jMax + j, datSpecR, valueR, phi, bi.getWidth());
                // setValues(i * jMax + j, i * jMax + j, datSpecG, valueG, phi, bi.getWidth());
                // setValues(i * jMax + j, i * jMax + j, datSpecB, valueB, phi, bi.getWidth());

            }
        }
        // System.out.println("ATTENTION : ind = " + (3 * ind + 2) + ", MUST BE >=
        // 256");
        TwoDArray inverseFFTB = new InverseFFT().transform(datSpecB);
        TwoDArray inverseFFTG = new InverseFFT().transform(datSpecG);
        TwoDArray inverseFFTR = new InverseFFT().transform(datSpecR);

        // visuSpectrum = inverseFFTB.DCToCentre(datSpecB.getMagnitude());

        double[] imageValuesB = inverseFFTB.getMagnitude();
        double[] imageValuesG = inverseFFTG.getMagnitude();
        double[] imageValuesR = inverseFFTR.getMagnitude();

        double[] magB = datSpecB.getMagnitude();
        double[] magR = datSpecR.getMagnitude();
        magB = normalize(magB);
        magB = inverseFFTB.DCToCentre(magB);
        magR = inverseFFTB.DCToCentre(normalize(magR));

        imageValuesR = inverseFFTB.DCToCentre(normalizeMean(imageValuesR));// normalize(normalizeMean(imageValuesR));
        imageValuesG = inverseFFTB.DCToCentre(normalizeMean(imageValuesG));
        imageValuesB = inverseFFTB.DCToCentre(normalizeMean(imageValuesB));

        // System.out.println(Arrays.stream(imageValuesR).summaryStatistics());
        double corrCoeff;

        // RECTANGLE
        // corrcoeff = 1.8

        // RHOMBUS
        corrCoeff = corr;
        BufferedImage res = new BufferedImage(spectrum.getWidth(), spectrum.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] resint = new int[res.getWidth() * res.getHeight()];
        for (int i = 0; i < spectrum.getWidth(); i++) {
            for (int j = 0; j < spectrum.getHeight(); j++) {
                spectrum.setRGB(i, j, ((magR[j * spectrum.getWidth() + i] != 0 ? 255 : 0) << 16)
                        | (magB[j * spectrum.getWidth() + i] != 0 ? 255 : 0));
                res.setRGB(i, i > spectrum.getWidth() / 2 ? j == 0 ? j : spectrum.getHeight() - j : j,
                        (int) (255 * corrCoeff * imageValuesR[j * spectrum.getWidth() + i] % 255) << 16
                                | (int) (255 * corrCoeff * imageValuesG[j * spectrum.getWidth() + i] % 255) << 8
                                | (int) (255 * corrCoeff * imageValuesB[j * spectrum.getWidth() + i] % 255));
                resint[i + (i > spectrum.getWidth() / 2 ? j == 0 ? j : spectrum.getHeight() - j : j) * res
                        .getWidth()] = (int) (255 * corrCoeff * imageValuesR[j * spectrum.getWidth() + i] % 255) << 16
                                | (int) (255 * corrCoeff * imageValuesG[j * spectrum.getWidth() + i] % 255) << 8
                                | (int) (255 * corrCoeff * imageValuesB[j * spectrum.getWidth() + i] % 255);
            }
        }
        g.drawImage(scale(res, getHashWidth(shift), getHashHeight(shift)), getShiftX(shift),
                getShiftY(shift),
                null);
        if (shift == 1)
            g.drawImage(spectrum, getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);

        
        // }
        // ratio = ratio / 10;
        // ratios = DoubleStream.concat(ratios, DoubleStream.of(ratio));
        // }
        // }
        // double max = Double.MIN_VALUE;
        // double [] ratiosarr = ratios.toArray();
        // for (int j = 0; j < ratiosarr.length; j++) {
        // if(ratiosarr[j] > max){
        // max = ratiosarr[j];
        // System.out.println(j);
        // }
        // }
        return resint;

    }

    public double[] normalize(double[] arr) {

        double mean = Arrays.stream(arr).average().getAsDouble();
        double[] temp = Arrays.stream(arr).map(d -> d / (2 * mean)).toArray();
        double min = Arrays.stream(temp).min().getAsDouble();
        double max = Arrays.stream(temp).max().getAsDouble();
        return Arrays.stream(temp).map(d -> (d - min) / (max - min)).toArray();
    }

    public void drawGridHashRGB(Graphics g, int[][] values, int shift, DMODE drawMode, Blockies prng) {

        int sideLength = getHashWidth(shift) / values.length;
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                drawRectHash(g, getPalette(drawMode, prng)[values[i][j]], j * sideLength,
                        i * sideLength, sideLength, sideLength, shift);
            }
        }
    }

    public void drawAntoineHash(Graphics g, String s, int shift, DMODE drawMode, Blockies prng) {

        String bin = TransformHash.hexToBin(s);

        int[][] values = TransformHash.extractValues(bin, drawMode, prng);

        drawGridHashRGB(g, values, shift, drawMode, prng);
        return;
    }

    private void resetShiftY() {
        SHIFTS_Y[2] = HASH_Y * 3 / 2 + ThreadLocalRandom.current().nextInt(VERT_JITTER) - VERT_JITTER / 2;
    }

    public void drawLandscapeHash(Graphics g, String hex, int shift, DMODE drawMode) {
        long info = Long.parseLong(hex, 16);

        Color[] palette = TransformHash.buildHSVWheel(16);
        Graphics2D g2d = (Graphics2D) g.create();
        int obj, curr_ind;
        // Obj = sky
        curr_ind = 32;
        obj = extract(info, curr_ind, 2);
        curr_ind -= 2;
        GradientPaint gp = new GradientPaint(getShiftX(shift), getShiftY(shift), new Color(0, 0, 0), getShiftX(shift),
                getShiftY(shift) + 3 * getHashHeight(shift) / 4, palette[obj + 11]);
        g2d.setPaint(gp);
        drawRectHash(g2d, null, 0, 0, getHashWidth(shift), 2 * getHashHeight(shift) / 3, shift);

        // Obj = sun
        obj = extract(info, curr_ind, 3);
        curr_ind -= 3;
        int SUN_RADIUS = 2 * getHashWidth(shift) / 3 - getHashHeight(shift) / 30;
        float[] hsb = Color.RGBtoHSB(palette[obj].getRed(), palette[obj].getGreen(), palette[obj].getBlue(),
                null);
        gp = new GradientPaint(getShiftX(shift), getShiftY(shift) + getHashHeight(shift) / 3, palette[obj],
                getShiftX(shift),
                getShiftY(shift) + getHashHeight(shift), Color.getHSBColor(hsb[0], hsb[1], 0));
        g2d.setPaint(gp);
        obj = extract(info, curr_ind, 1);
        curr_ind -= 1;
        Ellipse2D sun = new Ellipse2D.Double(
                getShiftX(shift) + getHashWidth(shift) / 6.0 + (-1 + 2 * obj) * SUN_RADIUS / 4,
                getShiftY(shift) + getHashHeight(shift) / 3.0, SUN_RADIUS, SUN_RADIUS);
        g2d.fill(sun);

        // Obj = ground
        obj = extract(info, curr_ind, 2);
        curr_ind -= 2;
        gp = new GradientPaint(getShiftX(shift), getShiftY(shift) + getHashHeight(shift) * 2 / 3, new Color(0, 0, 0),
                getShiftX(shift),
                getShiftY(shift) + getHashHeight(shift), palette[obj + 5]);
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
            Arc2D moon = new Arc2D.Double(getShiftX(shift) + (moonPos) * getHashWidth(shift) / 5.0 + MOON_RADIUS / 2,
                    getShiftY(shift) + MOON_RADIUS / 2.0, MOON_RADIUS, MOON_RADIUS, 90, -180 + moonPat * 360,
                    Arc2D.OPEN);
            g2d.fill(moon);
        } else {
            Ellipse2D moon = new Ellipse2D.Double(
                    getShiftX(shift) + (moonPos) * getHashWidth(shift) / 5.0 + MOON_RADIUS / 2,
                    getShiftY(shift) + MOON_RADIUS / 2.0, MOON_RADIUS, MOON_RADIUS);
            g2d.draw(moon);
            if (moonPat == 2)
                g2d.fill(moon);
        }

        obj = extract(info, curr_ind, 1);
        if (obj == 0)
            drawLineHash(g, getHashWidth(shift) / 10, getHashHeight(shift) / 3, getHashWidth(shift) / 10,
                    5 * getHashHeight(shift) / 6, Color.WHITE, shift);
        curr_ind -= 1;
        obj = extract(info, curr_ind, 1);
        if (obj == 0)
            drawLineHash(g, 9 * getHashWidth(shift) / 10, getHashHeight(shift) / 3, 9 * getHashWidth(shift) / 10,
                    5 * getHashHeight(shift) / 6, Color.WHITE, shift);

    }

    private void drawMountain(Graphics2D g, int ind, int size, int shape, Color color, int shift) {
        int height = (1 + size) * getHashHeight(shift) / 6;
        int width = getHashWidth(shift) / 8;
        int posY = getHashHeight(shift) * 6 / 7 - height;

        // float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(),
        // color.getBlue(),
        // null);
        // GradientPaint gp = new GradientPaint(getShiftX(shift), getShiftY(shift) +
        // posY,
        // Color.getHSBColor(hsb[0], hsb[1], hsb[2] - 0.5f),
        // getShiftX(shift),
        // getShiftY(shift) + posY + height, color);
        // g.setPaint(gp);
        g.setPaint(color);
        if (shape == 0) {
            drawRectHash(g, null, getHashWidth(shift) / 6 * (ind + 1), posY, width,
                    height, shift);
        } else {
            Path2D path = new Path2D.Double();
            path.moveTo(getShiftX(shift) + getHashWidth(shift) / 6 * (ind + 1), getShiftY(shift) + posY + height);
            path.lineTo(getShiftX(shift) + getHashWidth(shift) / 6 * (ind + 1) + width / 2, getShiftY(shift) + posY);
            path.lineTo(getShiftX(shift) + getHashWidth(shift) / 6 * (ind + 1) + width,
                    getShiftY(shift) + posY + height);
            path.closePath();
            g.fill(path);
        }
    }

    private int getHashWidth(int shift) {
        if (shift == 2)
            return HASH_W / 2;
        if (shift > 2)
            return (HASH_W) / 2;
        return HASH_W;
    }

    private int getHashHeight(int shift) {
        if (shift == 2)
            return HASH_H / 2;
        if (shift > 2)
            return (HASH_H / 2);
        return HASH_H;
    }

    private int extract(long source, int index, int length) {
        return (int) ((source & (((1L << length) - 1) << (index - length))) >>> (index - length));
    }

    public void drawGridLinesHash(Graphics g, String hex, int shift, DMODE drawMode) {
        int length = hex.length() * 4;
        String hex256 = hex;
        while (length < 256) {
            hex256 = hex256 + hex256;
            length = hex256.length() * 4;
        }

        int tileSide = (drawMode == DMODE.GridLines256 || drawMode == DMODE.GridLines64 ? 4 : 3);
        BigInteger bin = new BigInteger(hex256, 16);

        int colorsmaskLength = 4 * tileSide * tileSide - ((4 * tileSide * tileSide) % 8);
        BigInteger maskColors = BigInteger.ONE.shiftLeft(colorsmaskLength).subtract(BigInteger.ONE);
        BigInteger colors = maskColors.shiftLeft(length - colorsmaskLength).and(bin)
                .shiftRight(length - colorsmaskLength);

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

            drawRectHash(g,
                    TransformHash.buildHSVWheel(16)[colors
                            .and(BigInteger.valueOf(0b1111).shiftLeft(colorsmaskLength - (i + 1) * 4))
                            .shiftRight(colorsmaskLength - (i + 1) * 4).intValue()],
                    cornerX,
                    cornerY, sideLength, sideLength, shift);

            linesInfo = lines.and(BigInteger.valueOf(0b111_111_111_111).shiftLeft(lineMaskLength - 12 * (i + 1)))
                    .shiftRight(lineMaskLength - 12 * (i + 1)).intValue();
            drawLineHashIndices(g, (linesInfo & (0b111 << 9)) >> 9, (linesInfo & (0b111 << 6)) >> 6, cornerX,
                    cornerY, sideLength, shift);
            drawLineHashIndices(g, (linesInfo & (0b111 << 3)) >>> 3, linesInfo & 0b111, (i %
                    tileSide) * sideLength,
                    (i / tileSide) * sideLength, sideLength, shift);

            if ((drawMode == DMODE.GridLines128 || drawMode == DMODE.GridLines32) && i == tileSide * tileSide - 1)
                drawRectHash(g, new Color(100, 100, 100), cornerX, cornerY, sideLength, sideLength, shift);
        }

    }

    public void drawLineHashIndices(Graphics g, int index1, int index2, int cornerX, int cornerY, int sideLength,
            int shift) {
        int[] startCoords = getCoordFromIndex(index1, cornerX, cornerY, sideLength, sideLength / 6);
        if (index1 == index2) {
            drawLineHash(g, startCoords[0], startCoords[1], cornerX + sideLength / 2, cornerY + sideLength / 2,
                    index1 <= index2 ? new Color(0, 0, 0) : new Color(255, 255, 255),
                    shift);
        } else {

            int[] endCoords = getCoordFromIndex(index2, cornerX, cornerY, sideLength, sideLength / 6);
            drawLineHash(g, startCoords[0], startCoords[1], endCoords[0], endCoords[1],
                    index1 <= index2 ? new Color(0, 0, 0) : new Color(255, 255, 255),
                    shift);
        }
    }

    private int[] getCoordFromIndex(int index, int cornerX, int cornerY, int sideLength, int margin) {
        int[] coords = new int[] { 0, 0 };
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
