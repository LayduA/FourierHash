package src.hashops;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import src.crypto.PRNG128BitsInsecure;
import src.fourier.ComplexNumber;
import src.fourier.FFT;
import src.fourier.InverseFFT;
import src.fourier.TwoDArray;
import src.types.DrawMode;
import src.types.DrawParams;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static src.fourier.ImgMod30.shiftOrigin;
import static src.hashops.HashTransform.*;
import static src.types.DrawParams.Deter.*;
import static src.ui.Applet.*;

public class HashDrawer extends JPanel {

    private static final int DEPTH = 4;
    public static int[][] horribleMagic = new int[][]{
            {-1, 10, -1, -1, -1},
            {-1, 6, 11, -1, -1},
            {-1, 5, 7, 12, -1},
            {20, 1, 4, 8, 13},
            {0, 3, 9, 14, -1},
            {2, 19, 15, -1, -1},
            {18, 16, -1, -1, -1},
            {17, -1, -1, -1, -1}
    };
    public static int[][] horribleMagicIndex = new int[][]{
            //i,j
            {0, 1}, {1, 0}, {0, 2}, {1, 1}, {2, 0}, {HASH_W - 1, 1}, {HASH_W - 1, 2}, {HASH_W - 2, 1},
            {3, 0}, {2, 1}, {HASH_W - 1, 3}, {HASH_W - 2, 2}, {HASH_W - 3, 1}, {4, 0},
            {3, 1}, {2, 2}, {1, 3}, {0, 4}, {0, 3}, {1, 2}, {0, 0}
    };
    private final double[] reals = new double[]{-0.5, -0.5, 1.0, 1.0, 0.5, 0.5, -1.0, -1.0, 1.0, 1.0, -0.5, -0.5, -1.0, -1.0, 0.5, 0.5};
    private final double[] ims = new double[]{0.5, -1.0, -0.5, 1.0, -1.0, 0.5, 1.0, -0.5, 0.5, -1.0, -0.5, 1.0, -1.0, 0.5, 1.0, 0.5};
    private int spectrumHeight;

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

    public void drawHash(Graphics g, String hash, int shift, DrawParams params) {
        Graphics2D g2d = (Graphics2D) g;
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHints(rh);
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(0, 0, 0));

        DrawMode drawMode = params.getMode();

        if (drawMode == DrawMode.Antoine256 || drawMode == DrawMode.AntoineShift256 || drawMode == DrawMode.Adjacency1_256 || drawMode == DrawMode.Adjacency2_256 || drawMode == DrawMode.Blockies128) {
            drawAntoineHash(g, hash, shift, params);
        } else if (drawMode.toString().startsWith("GridLines")) {
            drawGridLinesHash(g, hash, shift, params);
        } else if (drawMode == DrawMode.Landscape32) {
            drawLandscapeHash(g, hash, shift, params);
        } else if (drawMode == DrawMode.Random128) {
            drawFuncHash(g, hash, shift, DEPTH);
        } else if (drawMode.name().startsWith("Fourier")) {
            drawFourierHashClean(g, hash, shift, params);
        }

    }

    public void drawFuncHash(Graphics g, String hash, int shift, int depth) {

        BufferedImage bi = new BufferedImage(getHashWidth(shift), getHashHeight(shift), BufferedImage.TYPE_INT_RGB);
        GenFunc fr = GenFunc.randomFunc(hash, depth);
        GenFunc fg = GenFunc.randomFunc(hash.substring(1) + hash.charAt(0), depth);
        GenFunc fb = GenFunc.randomFunc(hash.substring(2) + hash.substring(0, 2), depth);

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
                rgb = (int) Math.floor(doubleG * 127.5 + 127.5) << 16 | (int) Math.floor(doubleG * 127.5 + 127.5) << 8 | (int) Math.floor(doubleG * 127.5 + 127.5);

                bi.setRGB(i, j, (int) rgb);
                flatArrayR[i * bi.getWidth() + j] = (int) Math.floor(doubleR * 127.5 + 127.5);
                flatArrayG[i * bi.getWidth() + j] = (int) Math.floor(doubleG * 127.5 + 127.5);
                flatArrayB[i * bi.getWidth() + j] = (int) Math.floor(doubleB * 127.5 + 127.5);
            }
        }


        // Spectral analysis
        double[] amplitudeR = new FFT(flatArrayR, bi.getWidth(), bi.getHeight()).output.getMagnitude();
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


        outputR = shiftOrigin(amplitude2dR);
        outputG = shiftOrigin(amplitude2dG);
        outputB = shiftOrigin(amplitude2dB);

        int rVal, gVal, bVal;
        for (int i = 0; i < outputR.length; i++) {
            for (int j = 0; j < outputR[0].length; j++) {
                rVal = (int) Math.floor(outputR[i][j] * 255);
                gVal = (int) Math.floor(outputG[i][j] * 255);
                bVal = (int) Math.floor(outputB[i][j] * 255);
                spectrum.setRGB(i, j, gVal << 8 | gVal << 16 | gVal);//rVal << 16 | gVal << 8 | bVal);
            }
        }
        // null);
        g.drawImage(bi, getShiftX(shift), getShiftY(shift), null);// getShiftX(shift, 5, 15, 985), getShiftY(shift, 5,
        if (shift == 1) {
            g.drawImage(spectrum, getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);
        }
    }

    private double[] normalizeMean(double[] in) {
        double mean = Arrays.stream(in).average().isPresent() ? Arrays.stream(in).average().getAsDouble() : -1;
        return Arrays.stream(in).map(d -> d / mean).toArray();
    }

    private void setValues(int i, int j, TwoDArray target, double modulus, double phi, int size) {
        target.values[i][j] = ComplexNumber.rPhi(modulus, phi);

        if (j == 0 && i != 0) {
            target.values[size - i][j] = ComplexNumber.rPhi(modulus, -phi);
        } else if (i == 0 && j != 0) {
            target.values[i][size - j] = ComplexNumber.rPhi(modulus, -phi);
        } else if (i != 0) {
            target.values[size - i][size - j] = ComplexNumber.rPhi(modulus, -phi);
        }
    }

    private void setValues(int x, int y, TwoDArray[] targetArr, double[] moduli, double[] phis, int size) {
        for (int i = 0; i < targetArr.length; i++) {
            setValues(x, y, targetArr[i], moduli[i], phis[i], size);
        }
    }

    private void setValues(int x, int y, TwoDArray[] targetArr, double modulus, double phi, int size) {
        double[] moduli = new double[targetArr.length];
        double[] phis = new double[targetArr.length];
        Arrays.fill(moduli, modulus);
        Arrays.fill(phis, phi);
        setValues(x, y, targetArr, moduli, phis, size);
    }

    private void setValues(int x, int y, Complex[][] targetArr, Complex value) {
        targetArr[x][y] = new Complex(value.getReal(), value.getImaginary());
        int size = targetArr.length;
        if (x != 0 && y == 0) {
            targetArr[size - x][y] = new Complex(value.getReal(), -value.getImaginary());
        } else if (x == 0 && y != 0) {
            targetArr[x][size - y] = new Complex(value.getReal(), -value.getImaginary());
        } else if (x != 0) {
            targetArr[size - x][size - y] = new Complex(value.getReal(), -value.getImaginary());
        }
    }

    private double[] mapToComplex(int bits) {
        return new double[]{reals[bits], ims[bits]};
    }

    private Complex mapToComplexClean(int bits) {
        return new Complex(reals[bits], ims[bits]);
    }

    public int[] drawFourierHash(Graphics g, String hash, int shift, DrawParams params) {

        int spectrumWidth = HASH_W;
        int spectrumHeight = HASH_H;
        int nFunc = params.getNFunc();
        int nBitsPerElement;
        String binHash = hexToBin(hash);
        assert binHash.length() == spectrumWidth;
        String binTemp = binHash;

        BufferedImage spectrum = new BufferedImage(spectrumWidth, spectrumHeight, BufferedImage.TYPE_INT_RGB);

        double[] moduli = new double[nFunc];
        double[] phis = new double[nFunc];

        double frequencyCoeff;

        double[][] complexes = new double[nFunc][2];
        for (int i = 0; i < complexes.length; i++) {
            complexes[i] = new double[2];
        }
        int ind = 0;
        PRNG128BitsInsecure randomPhase = new PRNG128BitsInsecure();
        randomPhase.seedrand(hash);
        PRNG128BitsInsecure randomMod = new PRNG128BitsInsecure();
        randomMod.seedrand(new StringBuilder(hash).reverse().toString());

        TwoDArray[] dataSpectrums = new TwoDArray[nFunc];
        for (int i = 0; i < dataSpectrums.length; i++) {
            dataSpectrums[i] = new TwoDArray(spectrumWidth, spectrumHeight);
        }

        int[] colors;
        switch (nFunc) {
            case 1:
                colors = new int[]{0, 0xffffff};
                break;
            case 2:
                colors = new int[]{0, 0xff, 0xff00, 0xffff};
                break;
            case 3:
                // RGB basic
                colors = new int[]{0, 0xc5e4, 0xff00, 0xffff, 0xff0000, 0xffc5e4, 0xffff00, 0xffffff};
                // RED TO BLUE
                // colors = new int[]{0xd73027, 0xf46d43, 0xfdae61, 0xfee090, 0xe0f3f8, 0xabd9e9, 0x74add1, 0x4575b4};
                //HERE TO PUT IN BLACK AND WHITE
                //colors = new int[]{0, 0xffffff, 0, 0xffffff, 0, 0xffffff, 0, 0xffffff};
                break;
            case 4:
                colors = new int[]{0, 0x80, 0xff, 0x8000, 0xff00, 0x8080, 0xffff, 0x800000, 0xff0000, 0x800080, 0xff00ff, 0x808000, 0xffff00, 0x808080, 0xffffff, 0x123456};
                break;
            default:
                colors = new int[]{};
        }
        if (!params.isClassicRGB()) {
            colors = sampleColors(hash, params);
        }
        if (params.getMode().name().contains("Cartesian")) {
            setValues(0, 0, dataSpectrums, params.dist(0, 0), 0, spectrumWidth);

            nBitsPerElement = 4;

            int groupSize = nBitsPerElement * params.getNFunc();
            int nGroups = params.getMode().length() / groupSize;
            int maxInd = nGroups * groupSize;
            int remainder = params.getMode().length() - maxInd;
            while (ind < 2 * maxInd) {
                if (ind + groupSize > binTemp.length()) binTemp = binTemp + binTemp;
                if (ind == maxInd) ind += remainder;
                //MAGIC BEGINS
                int x = horribleMagicIndex[ind / groupSize][0];
                int y = horribleMagicIndex[ind / groupSize][1];
                //MAGIC ENDS

                //Going from bits to complex numbers (real in [-1,1], im in [-1,1]) according to encoding
                for (int k = 0; k < complexes.length; k++) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < nBitsPerElement; i++) {
                        sb.append(binTemp.charAt(ind + ((nBitsPerElement * k) % groupSize + i)));
                    }
                    complexes[k] = mapToComplex(Integer.parseInt(sb.toString(), 2));
                }
//                for (int k = 0; k < complexes.length; k++) {
//                    complexes[k] = mapToComplex(Integer.parseInt(binTemp.substring(ind + nBitsPerElement * k, ind + nBitsPerElement * (k + 1)), 2));
//                }

                //Translating cartesian coords into polar coords
                moduli = Arrays.stream(complexes).mapToDouble(c -> params.dist(x, y) * Math.sqrt(c[0] * c[0] + c[1] * c[1])).toArray();
                phis = Arrays.stream(complexes).mapToDouble(c -> Math.atan2(c[1], c[0])).toArray();

                setValues(x, y, dataSpectrums, moduli, phis, spectrumWidth);

                ind += groupSize;
            }

            if (params.getMode() == DrawMode.FourierCartesian128) {
                ind = maxInd;
                int x = horribleMagicIndex[ind * 2 / groupSize][0];
                int y = horribleMagicIndex[ind * 2 / groupSize][1];
                //BEGIN FREQUENCES SAMPLING
                for (int k = 0; k < remainder / nBitsPerElement; k++) {
                    complexes[k] = mapToComplex(Integer.parseInt(binTemp.substring(ind + nBitsPerElement * k, ind + nBitsPerElement * (k + 1)), 2));
                    moduli[k] = params.dist(x, y) * Math.sqrt(complexes[k][0] * complexes[k][0] + complexes[k][1] * complexes[k][1]);
                    phis[k] = Math.atan2(complexes[k][1], complexes[k][0]);
                }
//                for (int i = remainder / nBitsPerElement; i < complexes.length; i++) {
//                    moduli[i] = params.dist(x, y);
//                    phis[i] = 0;
//                }
                setValues(x, y, dataSpectrums, moduli, phis, spectrumWidth);
                //END FREQUENCES SAMPLING
            }

        } else {
            for (int i = 0; i < HASH_W; i++) {
                for (int j = 0; j < HASH_H / 2; j++) {

                    frequencyCoeff = params.dist(i, j);

                    if (i == 0 && j == 0) {
                        setValues(i, j, dataSpectrums, frequencyCoeff, 0, spectrumWidth);
                    } else {
                        nBitsPerElement = 0;
                        for (int k = 0; k < nFunc; k++) {

                            if (params.getModDet() == RAND) {
                                moduli[k] = frequencyCoeff * randomMod.rand();
                            } else if (params.getModDet() == FIXED) {
                                moduli[k] = frequencyCoeff;
                            } else if (params.getModDet() == DET) {
                                moduli[k] = frequencyCoeff * 0.5 * (binHash.charAt(ind) - '0' + 1);
                                if ((frequencyCoeff > 0 && !(0 < i && i < spectrumWidth / 2 && j == 0)))
                                    ind = (ind + 1) % binHash.length();
                            } else if (params.getModDet() == DOUBLE) {
                                System.out.println("Erhm modulus should not really be double");
                            }
                            if (params.getPhaseDet() == RAND) {
                                phis[k] = (randomPhase.rand() - 0.5) * Math.PI;
                            } else if (params.getPhaseDet() == DOUBLE) {
                                nBitsPerElement += 2;
                                phis[k] = Math.PI / 2.0 * Integer.parseInt(binHash.charAt((ind + 2 * k) % binHash.length()) + Character.toString(binHash.charAt((ind + 2 * k + 1) % binHash.length())), 2);
                            } else {
                                phis[k] = Math.PI * binHash.charAt(ind) - '0';
                                if ((frequencyCoeff > 0 && !(0 < i && i < spectrumWidth / 2 && j == 0)))
                                    ind = (ind + 1) % binHash.length();
                            }
                        }
                        setValues(i, j, dataSpectrums, moduli, phis, spectrumWidth);

                    }
                }
            }
        }
        Object[] inverseFFTsO = Arrays.stream(dataSpectrums).map(spec -> new InverseFFT().transform(spec)).toArray();
        TwoDArray[] inverseFFTs = new TwoDArray[inverseFFTsO.length];
        double[][] imageValues = new double[inverseFFTs.length][];
        double[][] mags = new double[inverseFFTs.length][];

        for (int i = 0; i < inverseFFTsO.length; i++) {
            inverseFFTs[i] = (TwoDArray) inverseFFTsO[i];
            imageValues[i] = normalizeMean(inverseFFTs[i].getMagnitude());//inverseFFTs[i].DCToCentre(normalizeMean(inverseFFTs[i].getMagnitude()));
            mags[i] = normalize(dataSpectrums[i].getMagnitude());
        }

        double[] phase = dataSpectrums[0].getPhase();
        double[] bigPhase = new double[phase.length];

        BufferedImage res = new BufferedImage(spectrumWidth, spectrumHeight, BufferedImage.TYPE_INT_RGB);
        int[] fingerprintInt = new int[res.getWidth() * res.getHeight() + 1];
        int coordY;
        int pixelLoc, pixelLocMod;

        double[][] bigMags = new double[mags.length][mags[0].length];

        //Store somewhere the number of bits we read
        fingerprintInt[fingerprintInt.length - 1] = ind;


        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrum.getHeight(); j++) {

                coordY = j;

                pixelLoc = j * spectrumWidth + i;
                pixelLocMod = getPixelLocSpectrum(i, j, spectrumWidth, params);
                spectrum.setRGB(i, j, (Math.min(255, (int) (mags[0][pixelLocMod] * 255)) << 16) | (Math.min(255, (int) (mags[Math.min(1, nFunc - 1)][pixelLocMod] * 255)) << 8) | Math.min(255, (int) (mags[Math.min(2, nFunc - 1)][pixelLocMod] * 255)));
                //res.setRGB(i, i > spectrumWidth / 2 ? j == 0 ? j : spectrum.getHeight() - j : j, (int) (255 * corrCoeff * imageValuesR[j * spectrumWidth + i] % 255) << 16 | (int) (255 * corrCoeff * imageValuesG[j * spectrumWidth + i] % 255) << 8 | (int) (255 * corrCoeff * imageValuesB[j * spectrumWidth + i] % 255));

                for (int k = 0; k < mags.length; k++) {
                    bigMags[k][i + j * spectrumWidth] = mags[k][pixelLocMod];
                }

                bigPhase[i + j * spectrumWidth] = phase[pixelLocMod];

                res.setRGB(i, coordY, getRGBCorr(params, colors, pixelLoc, imageValues));
            }
        }

        if (params.getContour() != DrawParams.Contour.NONE) {
            contour(res, params);
        }

        if (params.isSymmetric()) {
            symmetrify(res, params.getSymmetry() == DrawParams.SymMode.FROM_HASH ? DrawParams.SymMode.values()[HashTransform.getSymmetry(hash)] : params.getSymmetry());
        }
        //Here to ensure the return int array is what we see on the screen
        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrum.getHeight(); j++) {
                fingerprintInt[i + j * spectrumWidth] = res.getRGB(i, j);
            }
        }

        for (int i = 0; i < bigMags.length; i++) {
            bigMags[i] = inverseFFTs[i].DCToCentre(bigMags[i]);
        }

        bigPhase = inverseFFTs[0].DCToCentre(bigPhase);

        BufferedImage centerSpectrum = new BufferedImage(spectrumWidth, spectrumHeight, BufferedImage.TYPE_INT_RGB);
        BufferedImage centerPhase = new BufferedImage(spectrumWidth, spectrumHeight, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrumWidth; j++) {
                centerSpectrum.setRGB(i, j, (Math.min(255, (int) (bigMags[0][i + j * spectrumWidth] * 255)) << 16) | (Math.min(255, (int) (bigMags[Math.min(1, nFunc - 1)][i + j * spectrumWidth] * 255)) << 8) | Math.min(255, (int) (bigMags[Math.min(2, nFunc - 1)][i + j * spectrumWidth] * 255)));
                if (bigPhase[i + j * spectrumWidth] == 0) {
                    centerPhase.setRGB(i, j, 0);
                } else if (bigPhase[i + j * spectrumWidth] > 0) {
                    centerPhase.setRGB(i, j, (int) (bigPhase[i + j * spectrumWidth] * 255 / Math.PI) << 16);
                } else {
                    centerPhase.setRGB(i, j, (int) (-bigPhase[i + j * spectrumWidth] * 255 / Math.PI));
                }
            }
        }
        if (!g.drawImage(scale(res, getHashWidth(params.dontScale ? 0 : shift), getHashHeight(params.dontScale ? 0 : shift)),
                getShiftX(shift) - (params.dontScale ? 100 : 0), getShiftY(params.dontScale ? 1 : shift), null)) {
            System.out.println("error drawing image");
        }

//        if (shift == 1 || shift == 2) {
//            g.drawImage(scale((params.isSeePhase() ? centerPhase : centerSpectrum), getHashWidth(shift), getHashHeight(shift)), getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);
//        }
        return fingerprintInt;

    }

    public Complex[][] generateSpectrum(DrawParams params, String hash, int funcIndex) {

        int nBitsPerElement = 4;

        Complex[][] spectrum = new Complex[256][256];

        int groupSize = nBitsPerElement * params.getNFunc();
        int nGroups = params.getMode().length() / groupSize;
        int maxIndex = nGroups * groupSize;
        int curr4Bits;
        Complex complex;
        int[] coords;

        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                spectrum[i][j] = new Complex(0, 0);
            }
        }

        for (int currentIndex = 0; currentIndex <= maxIndex; currentIndex += groupSize) {

            if (currentIndex + funcIndex * nBitsPerElement >= params.getMode().length()) continue;

            curr4Bits = extract4Bits(hash, currentIndex / 4 + funcIndex);

            complex = mapToComplexClean(curr4Bits);

            if (currentIndex < maxIndex) {
                coords = horribleMagicIndex[currentIndex / groupSize];
                setValues(coords[0], coords[1], spectrum, complex.multiply(params.dist(coords[0], coords[1])));
            }
            if (params.getMode().length() == 128) {
                coords = horribleMagicIndex[(currentIndex + maxIndex) / groupSize];
                setValues(coords[0], coords[1], spectrum, complex.multiply(params.dist(coords[0], coords[1])));
            }
        }
        return spectrum;
    }

    public Complex[][] ifft2D(Complex[][] source) {
        Complex[][] out = new Complex[source.length][source[0].length];
        Complex[][] intermediate = new Complex[source.length][source[0].length];
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        for (int i = 0; i < source.length; i++) {
            Complex[] newCol = fft.transform(source[i], TransformType.INVERSE);
            Complex acc = new Complex(0, 0);
            for (int j = 0; j < out[0].length; j++) {
                acc = acc.add(source[i][j]);
            }
            for (int j = 0; j < source.length; j++) {
                intermediate[i][j] = new Complex(newCol[j].getReal(), newCol[j].getImaginary());
            }
        }


        for (int i = 0; i < source.length; i++) {
            Complex[] column = getColumn(intermediate, i);
            Complex[] newRow = fft.transform(column, TransformType.INVERSE);
            for (int j = 0; j < source.length; j++) {
                out[j][i] = new Complex(newRow[j].getReal(), newRow[i].getImaginary());
            }
        }

        Complex acc = new Complex(0, 0);
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[0].length; j++) {
                acc = acc.add(out[i][j]);
            }
        }
        return out;
    }

    private Complex[] getColumn(Complex[][] arr, int ind) {
        Complex[] col = new Complex[arr.length];
        for (int i = 0; i < col.length; i++) {
            col[i] = new Complex(arr[i][ind].getReal(), arr[i][ind].getImaginary());
        }
        return col;
    }

    public Complex[][] normalizeMean(Complex[][] arr) {
        double mean = 0;
        int size = arr.length * arr[0].length;

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                mean += arr[i][j].abs() / size;
            }
        }

        Complex[][] normalized = new Complex[arr.length][arr[0].length];
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                normalized[i][j] = arr[i][j].divide(mean).multiply(0.4);
            }
        }

        return normalized;
    }

    public int[][] drawFourierHashClean(Graphics g, String hash, int shift, DrawParams params) {
        int spectrumWidth = HASH_W;
        int spectrumHeight = HASH_H;
        Complex[][][] dataSpectrums = new Complex[params.getNFunc()][spectrumWidth][spectrumHeight];

        for (int i = 0; i < params.getNFunc(); i++) {
            dataSpectrums[i] = generateSpectrum(params, hash, i);
        }
        int[] colors = sampleColors(hash, params);

        Complex[][][] inverses = new Complex[dataSpectrums.length][256][256];
        for (int i = 0; i < inverses.length; i++) {
            inverses[i] = ifft2D(dataSpectrums[i]);
        }

        Complex[][][] normalized = new Complex[dataSpectrums.length][256][256];
        for (int i = 0; i < inverses.length; i++) {
            normalized[i] = normalizeMean(inverses[i]);
        }


        BufferedImage res = new BufferedImage(spectrumWidth, spectrumHeight, BufferedImage.TYPE_INT_RGB);
        int[][] fingerprintInt = new int[res.getWidth()][res.getHeight()];


        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrumHeight; j++) {
                res.setRGB(i, j, getRGBCorr(params, colors, i, j, normalized));
            }
        }

        if (params.getContour() != DrawParams.Contour.NONE) {
            contour(res, params);
        }

        if (params.isSymmetric()) {
            symmetrify(res, params.getSymmetry() == DrawParams.SymMode.FROM_HASH ? DrawParams.SymMode.values()[HashTransform.getSymmetry(hash)] : params.getSymmetry());
        }
        //Here to ensure the return int array is what we see on the screen
        for (int i = 0; i < spectrumWidth; i++) {
            for (int j = 0; j < spectrumHeight; j++) {
                fingerprintInt[i][j] = res.getRGB(i, j);
            }
        }

        if (!g.drawImage(scale(res, getHashWidth(params.dontScale ? 0 : shift), getHashHeight(params.dontScale ? 0 : shift)),
                getShiftX(shift) - (params.dontScale ? 100 : 0), getShiftY(params.dontScale ? 1 : shift), null)) {
            System.out.println("error drawing image");
        }

        return fingerprintInt;
    }

    //        if (shift == 1 || shift == 2) {
//            g.drawImage(scale((params.isSeePhase() ? centerPhase : centerSpectrum), getHashWidth(shift), getHashHeight(shift)), getShiftX(shift) + (int) (getHashWidth(shift) * 1.1), getShiftY(shift), null);
//        }

    private int[] sampleColors(String hash, DrawParams params) {
        //BEGIN COLOR MAPPING
        if (params.isClassicRGB()) {
            return new int[]{0x000000, 0x0000ff, 0x00ff00, 0x00ffff, 0xff0000, 0xff00ff, 0xffff00, 0xffffff};
        }
        if (!params.palette32 || params.colorBlind) {
            int[][][] palettes = new int[3][2][8];
            // RED TO BLUE
            // RED TO BLUE COLOR-BLIND
            palettes[0] = new int[][]{
                    new int[]{0xd73027, 0xf46d43, 0xfdae61, 0xfee090, 0xe0f3f8, 0xabd9e9, 0x74add1, 0x4575b4},
                    new int[]{0xb2182b, 0xd6604d, 0xf4a582, 0xfddbc7, 0xd1e5f0, 0x92c5de, 0x4393c3, 0x2166ac}
            };
            // NEON
            // RED TO BLACK
            // JAJ
            // OTHER JAJ with params http://vrl.cs.brown.edu/color -> GREEN TO PURPLE BUT FLASHY
            // BROWN TO PURPLE COLOR-BLIND
            palettes[1] = new int[][]{
                    //new int[]{0x00062e, 0x00ffff, 0x0900bd, 0x990bb5, 0xc90ab6, 0xd4203e, 0xff3700, 0xffd500},
                    //new int[]{0xb2182b, 0xd6604d, 0xf4a582, 0xfddbc7, 0xe0e0e0, 0xbababa, 0x878787, 0x4d4d4d},
                    //new int[]{0xa40038, 0xd43e1f, 0xdf892e, 0x4e7026, 0x09177e, 0x330043, 0x01000e, 0xf5c1c5},
                    new int[]{0x3588d1, 0xe5f642, 0x715cb6, 0xb6eee2, 0x387836, 0xf27ff5, 0x2cf52b, 0xb00bd9},
                    new int[]{0xb35806, 0xe08214, 0xfdb863, 0xfee0b6, 0xd8daeb, 0xb2abd2, 0x8073ac, 0x542788}
            };

            // LIL MIX
            // PURPLE TO GREEN COLOR-BLIND
            palettes[2] = new int[][]{
                    new int[]{0xa6cee3, 0x1f78b4, 0xb2df8a, 0x33a02c, 0xfb9a99, 0xe31a1c, 0xfdbf6f, 0xff7f00},
                    new int[]{0x762a83, 0x9970ab, 0xc2a5cf, 0xe7d4e8, 0xd9f0d3, 0xa6dba0, 0x5aae61, 0x1b7837}
            };

            int paletteInd = params.palette32 ? sumIndex(hash, 3) : (1 + weight(hash) % 2);
            int[] palette = palettes[paletteInd][params.colorBlind ? 1 : 0];

            int permutInd = getPerm(hash);//sumIndex(hash, 616);
            palette = permute(palette, Combinatorial.codewords[permutInd]);

            //int shiftPalette = sumIndex(hash, 7);
            //palette = rotate(palette, shiftPalette, 0);

            if (params.palette32 && weight(hash) % 2 == 1) {
                List<Object> li = Arrays.asList(Arrays.stream(palette).boxed().toArray());
                Collections.reverse(li);
                palette = li.stream().mapToInt(I -> (Integer) I).toArray();
            }

            return palette;
        }
        // BEGIN 32-PALETTE SAMPLING


        Boolean[] groupParities = HashTransform.getParities(hash.substring(0, 32), params);
        if (params.getMode().length() == 256) {
            Boolean[] secondHalfParities = getParities(hash.substring(32), params);
            for (int i = 0; i < groupParities.length; i++) {
                groupParities[i] ^= secondHalfParities[i];
            }
        }

        int[] palette = new int[1 << params.getNFunc()];
        int[] palette32 = params.paletteRGB(32, null);
        int shiftPalette = HashTransform.getPaletteShift(hash);
        palette32 = rotate(palette32, shiftPalette, 0);

        int indexColor;
        for (int i = 0; i < palette.length; i++) {
            if (palette.length == 8) {
                indexColor = 4 * i + (!groupParities[2 * i] ? 2 : 0) + (!groupParities[2 * i + 1] ? 1 : 0);
                palette[i] = palette32[indexColor];
            } else if (palette.length == 16) {
                palette[i] = palette32[2 * i + (groupParities[i] ? 1 : 0)];
            }
        }

        int permutInd = getPerm(hash);
        int[] permutation = new int[palette.length];
        if (PERM_PRIME == 613) permutation = Combinatorial.codewords[permutInd];
        else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("src/code8_4.txt"), StandardCharsets.UTF_8));
                String[] line = new String[palette.length];

                for (int i = 0; i < permutInd; i++) {
                    line = reader.readLine().strip().split(",");
                }
                permutation = new int[line.length];
                for (int i = 0; i < line.length; i++) {
                    permutation[i] = Integer.parseInt(line[i].strip());
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        palette = permute(palette, permutation);//Combinatorial.codewords[params.permut == -1 ? permutInd : params.permut]);
        //END COLOR MAPPING
        return palette;
        //END 32-PALETTE SAMPLING

    }

    private int[] permute(int[] original, int[] indArr) {
        int[] perm = new int[indArr.length];
        for (int i = 0; i < indArr.length; i++) {
            perm[i] = original[indArr[i]];
        }
        return perm;
    }

    private void symmetrify(BufferedImage res, DrawParams.SymMode symMode) {
        int spectrumWidth = res.getWidth();
        int spectrumHeight = res.getHeight();
        //Giga yikes
        for (int i = 0; i < res.getWidth(); i++) {
            for (int j = 0; j < res.getHeight(); j++) {

                if (symMode == DrawParams.SymMode.ROWS_BOTTOMEST && j >= spectrumWidth / 2) {
                    if (j <= 3 * spectrumWidth / 4) {
                        res.setRGB(i, j, res.getRGB(i, Math.min(6 * spectrumWidth / 4 - j, 255)));
                    }
                    res.setRGB(i, spectrumHeight - 1 - j, res.getRGB(i, j));
                }
                if (symMode.toString().contains("ROWS_TOP") && j <= spectrumHeight / 2) {
                    int sign = (symMode.toString().contains("EST") ? -1 : 1);

                    if (sign * j <= sign * spectrumHeight / 4) {
                        res.setRGB(i, j, res.getRGB(i, spectrumWidth / 2 - j));
                    }
                    res.setRGB(i, spectrumHeight - 1 - j, res.getRGB(i, j));

                }
                if (symMode.toString().contains("COLUMNS_LEFT") && i <= spectrumWidth / 2) {
                    int sign = (symMode.toString().contains("EST") ? -1 : 1);
                    if (sign * i <= sign * spectrumWidth / 4) {
                        res.setRGB(i, j, res.getRGB(spectrumWidth / 2 - i, j));
                    }
                }
                if (symMode.toString().contains("COLUMNS_RIGHT") && i >= spectrumWidth / 2) {
                    int sign = (symMode.toString().contains("EST") ? 1 : -1);
                    if (sign * i <= sign * 3 * spectrumWidth / 4) {
                        res.setRGB(i, j, res.getRGB(Math.min(6 * spectrumWidth / 4 - i, 255), j));
                    }
                    res.setRGB(spectrumWidth - 1 - i, j, res.getRGB(i, j));
                }

                if ((symMode == DrawParams.SymMode.HORIZONTAL_LEFT || symMode.toString().contains("COLUMNS_LEFT")) && i > spectrumWidth / 2) {
                    res.setRGB(i, j, res.getRGB(spectrumWidth - 1 - i, j));
                }
                if ((symMode == DrawParams.SymMode.HORIZONTAL_RIGHT) && i < spectrumWidth / 2) {
                    res.setRGB(i, j, res.getRGB(spectrumWidth - 1 - i, j));
                }
                if ((symMode == DrawParams.SymMode.VERTICAL_LEFT || symMode == DrawParams.SymMode.ROWS_TOPPEST) && j > spectrumHeight / 2) {
                    res.setRGB(i, j, res.getRGB(i, spectrumHeight - 1 - j));
                }
                if (symMode == DrawParams.SymMode.VERTICAL_RIGHT && j < spectrumHeight / 2) {
                    res.setRGB(i, j, res.getRGB(i, spectrumHeight - 1 - j));
                }
                if (symMode == DrawParams.SymMode.DIAGONAL_LEFT && i + j > spectrumWidth) {
                    res.setRGB(i, j, res.getRGB(spectrumWidth - 1 - j, spectrumHeight - 1 - i));
                }
                if (symMode == DrawParams.SymMode.DIAGONAL_RIGHT && i + j < spectrumWidth) {
                    res.setRGB(i, j, res.getRGB(spectrumWidth - 1 - j, spectrumHeight - 1 - i));
                }
                if (symMode == DrawParams.SymMode.ANTIDIAGONAL_LEFT && i > j) {
                    res.setRGB(i, j, res.getRGB(j, i));
                }
                if (symMode == DrawParams.SymMode.ANTIDIAGONAL_RIGHT && i < j) {
                    res.setRGB(i, j, res.getRGB(j, i));
                }
                if (symMode == DrawParams.SymMode.CROSS_TOPLEFT && (i <= spectrumWidth / 2 && j <= spectrumHeight / 2)
                        || (symMode == DrawParams.SymMode.CROSS_BOTLEFT && (i <= spectrumWidth / 2 && j >= spectrumHeight / 2))
                        || (symMode == DrawParams.SymMode.CROSS_BOTRIGHT && (i >= spectrumWidth / 2 && j >= spectrumHeight / 2))
                        || (symMode == DrawParams.SymMode.CROSS_TOPRIGHT && (i >= spectrumWidth / 2 && j <= spectrumHeight / 2))) {
                    int col = res.getRGB(i, j);
                    res.setRGB(spectrumWidth - 1 - i, j, col);
                    res.setRGB(i, spectrumHeight - 1 - j, col);
                    res.setRGB(spectrumWidth - 1 - i, spectrumHeight - 1 - j, col);
                }

                if ((symMode == DrawParams.SymMode.CROSSX_LEFT && (i + j <= spectrumWidth && i <= j))
                        || (symMode == DrawParams.SymMode.CROSSX_BOT && (i + j >= spectrumWidth - 1 && i <= j))
                        || (symMode == DrawParams.SymMode.CROSSX_RIGHT && (i + j >= spectrumWidth - 1 && i >= j))
                        || (symMode == DrawParams.SymMode.CROSSX_TOP && (i + j <= spectrumWidth && i >= j))) {
                    int col = res.getRGB(i, j);
                    res.setRGB(j, i, col);
                    res.setRGB(spectrumHeight - 1 - j, spectrumWidth - 1 - i, col);
                    res.setRGB(spectrumHeight - 1 - i, spectrumWidth - 1 - j, col);
                }


            }
        }
    }

    private int[] rotate(int[] arr, int n, int from) {
        if (from == 0) {
            int[] out = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = arr[(i + n) % (arr.length)];
            }
            return out;
        } else {
            return IntStream.concat(Arrays.stream(Arrays.copyOf(arr, from)),
                    Arrays.stream(rotate(Arrays.copyOfRange(arr, from, arr.length), n, 0))).toArray();
        }
    }

    private int getRGBCorr(DrawParams params, int[] colors, int index, double[]... funcs) {
        if ((1 << funcs.length) != colors.length)
            throw new IllegalArgumentException("number of functions vs number of colors");
        int color = 0;
        for (int i = 0; i < funcs.length; i++) {
            color += ((funcs[i][index] > params.getThreshold()) ? 1 : 0) << (funcs.length - 1 - i);
        }
        if (params.isFiltered()) {
            return colors[color];
        } else {
            int n = params.getNFunc();
            return (int) (255 * 0.5 * funcs[0][index]) % 255 << 16 | (int) (255 * 0.5 * funcs[Math.min(0, n - 1)][index]) % 255 << 8 | (int) (255 * 0.5 * funcs[Math.min(0, n - 1)][index]) % 255;
        }
    }

    private int getRGBCorr(DrawParams params, int[] colors, int x, int y, Complex[][][] funcs) {
        if ((1 << funcs.length) != colors.length)
            throw new IllegalArgumentException("number of functions vs number of colors");
        int color = 0;
        for (int i = 0; i < funcs.length; i++) {
            color += ((funcs[i][x][y].abs() > params.getThreshold()) ? 1 : 0) << (funcs.length - 1 - i);
        }
        if (params.isFiltered()) {
            return colors[color];
        } else {
            int n = params.getNFunc();
            return (int) (255 * funcs[0][x][y].abs()) % 255 << 16 | (int) (255 * funcs[Math.min(0, n - 1)][x][y].abs()) % 255 << 8 | (int) (255 * funcs[Math.min(0, n - 1)][x][y].abs()) % 255;
        }
    }

    private int[] getNeighbours(BufferedImage img, int x, int y) {
        ArrayList<Integer> neighb = new ArrayList<>();
        neighb.add(img.getRGB(x, y));
        neighb.add(img.getRGB(x - 1, y));
        neighb.add(img.getRGB(x - 1, y - 1));
        neighb.add(img.getRGB(x, y - 1));
        return neighb.stream().mapToInt(i -> i).toArray();
    }

    private void contour(BufferedImage img, DrawParams params) {
        for (int iteration = 0; iteration < params.getThickness(); iteration++) {

            int[] colors;
            //BEGIN COPYING IMAGE
            ColorModel cm = img.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = img.copyData(null);
            BufferedImage temp = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
            //END COPYING IMAGE
            for (int x = 1; x < img.getWidth() - 1; x++) {
                for (int y = 1; y < img.getHeight() - 1; y++) {
                    colors = getNeighbours(temp, x, y);

                    if (Arrays.stream(colors).distinct().toArray().length > 1) {
                        int red = (int) Arrays.stream(colors).map(i -> (i >> 16) & 255).average().orElseThrow();
                        int green = (int) Arrays.stream(colors).map(i -> (i >> 8) & 255).average().orElseThrow();
                        int blue = (int) Arrays.stream(colors).map(i -> i & 255).average().orElseThrow();
                        img.setRGB(x, y, params.getContour().toString().contains("SMOOTH") ? red << 16 | green << 8 | blue : 0);
                    } else if (params.getContour().toString().contains("ONLY")) {
                        img.setRGB(x, y, 0xffffff);
                    }
                }
            }
        }
    }

    private int getPixelLocSpectrum(int i, int j, int spectrumWidth, DrawParams params) {
        int spectrumWidthPixels = spectrumWidth / (2 * (params.cut() + 1));
        int x = i > spectrumWidth / 2 ? (i - spectrumWidth / 2) / spectrumWidthPixels + spectrumWidth - (params.cut() + 1) : i / spectrumWidthPixels;
        int y = j > spectrumWidth / 2 ? (j - spectrumWidth / 2) / spectrumWidthPixels + spectrumWidth - (params.cut() + 1) : j / spectrumWidthPixels;
        if (x > spectrumWidth - 1) return 0;
        int out = x + y * spectrumWidth;
        return out > spectrumWidth * spectrumWidth - 1 ? 0 : out;
    }

    public double[] normalize(double[] arr) {

        double mean = Arrays.stream(arr).average().isPresent() ? Arrays.stream(arr).average().getAsDouble() : -1.0;
        double[] temp = Arrays.stream(arr).map(d -> d / (2 * mean)).toArray();
        double min = Arrays.stream(temp).min().isPresent() ? Arrays.stream(temp).min().getAsDouble() : -1.0;
        double max = Arrays.stream(temp).max().isPresent() ? Arrays.stream(temp).max().getAsDouble() : -1.0;
        return Arrays.stream(temp).map(d -> (d - min) / (max - min)).toArray();
    }

    public void drawGridHashRGB(Graphics g, int[][] values, int shift, DrawParams params) {

        int sideLength = getHashWidth(shift) / values.length;
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                drawRectHash(g, params.palette()[values[i][j]], j * sideLength, i * sideLength, sideLength, sideLength, shift);
            }
        }
    }

    public void drawAntoineHash(Graphics g, String s, int shift, DrawParams params) {

        String bin = hexToBin(s);

        int[][] values = HashTransform.extractValues(bin, params);

        drawGridHashRGB(g, values, shift, params);
    }


    public void drawLandscapeHash(Graphics g, String hex, int shift, DrawParams params) {
        long info = Long.parseLong(hex, 16);

        Color[] palette = HashTransform.buildHSVWheel(16, params);
        Graphics2D g2d = (Graphics2D) g.create();
        int obj, curr_ind;
        // Obj = sky
        curr_ind = 32;
        obj = extractBits(info, curr_ind, 2);
        curr_ind -= 2;
        GradientPaint gp = new GradientPaint(getShiftX(shift), getShiftY(shift), new Color(0, 0, 0), getShiftX(shift), getShiftY(shift) + 3 * getHashHeight(shift) / 4f, palette[obj + 11]);
        g2d.setPaint(gp);
        drawRectHash(g2d, null, 0, 0, getHashWidth(shift), 2 * getHashHeight(shift) / 3, shift);

        // Obj = sun
        obj = extractBits(info, curr_ind, 3);
        curr_ind -= 3;
        int SUN_RADIUS = 2 * getHashWidth(shift) / 3 - getHashHeight(shift) / 30;
        float[] hsb = Color.RGBtoHSB(palette[obj].getRed(), palette[obj].getGreen(), palette[obj].getBlue(), null);
        gp = new GradientPaint(getShiftX(shift), getShiftY(shift) + getHashHeight(shift) / 3f, palette[obj], getShiftX(shift), getShiftY(shift) + getHashHeight(shift), Color.getHSBColor(hsb[0], hsb[1], 0));
        g2d.setPaint(gp);
        obj = extractBits(info, curr_ind, 1);
        curr_ind -= 1;
        Ellipse2D sun = new Ellipse2D.Double(getShiftX(shift) + getHashWidth(shift) / 6.0 + (-1 + 2 * obj) * SUN_RADIUS / 4f, getShiftY(shift) + getHashHeight(shift) / 3.0, SUN_RADIUS, SUN_RADIUS);
        g2d.fill(sun);

        // Obj = ground
        obj = extractBits(info, curr_ind, 2);
        curr_ind -= 2;
        gp = new GradientPaint(getShiftX(shift), getShiftY(shift) + getHashHeight(shift) * 2 / 3f, new Color(0, 0, 0), getShiftX(shift), getShiftY(shift) + getHashHeight(shift), palette[obj + 5]);
        g2d.setPaint(gp);
        drawRectHash(g2d, null, 0, 2 * getHashHeight(shift) / 3, getHashWidth(shift), getHashHeight(shift) / 3, shift);

        // Obj = mountainsSize
        int sizes = extractBits(info, curr_ind, 4);
        curr_ind -= 4;
        int shapes = extractBits(info, curr_ind, 4);
        curr_ind -= 4;
        int colors = extractBits(info, curr_ind, 8);
        curr_ind -= 8;

        for (int i = 3; i >= 0; i--) {
            drawMountain(g2d, i, (sizes >> i) & 1, (shapes >> i) & 1, palette[((colors >> (2 * i)) & 0b11) + 4], shift);
        }

        int moonPos = extractBits(info, curr_ind, 2);
        curr_ind -= 2;
        int moonPat = extractBits(info, curr_ind, 2);
        curr_ind -= 2;
        int moonCol = extractBits(info, curr_ind, 2);
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

        obj = extractBits(info, curr_ind, 1);
        if (obj == 0)
            drawLineHash(g, getHashWidth(shift) / 10, getHashHeight(shift) / 3, getHashWidth(shift) / 10, 5 * getHashHeight(shift) / 6, Color.WHITE, shift);
        curr_ind -= 1;
        obj = extractBits(info, curr_ind, 1);
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
        if (shift > 2 && shift < 100) return (HASH_W) / 2;
        return HASH_W;
    }

    private int getHashHeight(int shift) {
        if (shift == 2) return HASH_H / 2;
        if (shift > 2 && shift < 100) return (HASH_W) / 2;
        return HASH_H;
    }

    private int extractBits(long source, int index, int length) {
        return (int) ((source & (((1L << length) - 1) << (index - length))) >>> (index - length));
    }

    private int extract4Bits(String source, int index) {
        return Integer.parseInt(Character.toString(source.charAt(index)), 16);
    }

    public void drawGridLinesHash(Graphics g, String hex, int shift, DrawParams params) {
        int length = hex.length() * 4;
        StringBuilder hex256 = new StringBuilder(hex);
        while (length < 256) {
            hex256.append(hex256);
            length = hex256.length() * 4;
        }

        int tileSide = (params.getMode() == DrawMode.GridLines256 || params.getMode() == DrawMode.GridLines64 ? 4 : 3);
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

            drawRectHash(g, HashTransform.buildHSVWheel(16, params)[colors.and(BigInteger.valueOf(0b1111).shiftLeft(colorsmaskLength - (i + 1) * 4)).shiftRight(colorsmaskLength - (i + 1) * 4).intValue()], cornerX, cornerY, sideLength, sideLength, shift);

            linesInfo = lines.and(BigInteger.valueOf(0b111_111_111_111).shiftLeft(lineMaskLength - 12 * (i + 1))).shiftRight(lineMaskLength - 12 * (i + 1)).intValue();
            drawLineHashIndices(g, (linesInfo & (0b111 << 9)) >> 9, (linesInfo & (0b111 << 6)) >> 6, cornerX, cornerY, sideLength, shift);
            drawLineHashIndices(g, (linesInfo & (0b111 << 3)) >>> 3, linesInfo & 0b111, (i % tileSide) * sideLength, (i / tileSide) * sideLength, sideLength, shift);

            if ((params.getMode() == DrawMode.GridLines128 || params.getMode() == DrawMode.GridLines32) && i == tileSide * tileSide - 1)
                drawRectHash(g, new Color(100, 100, 100), cornerX, cornerY, sideLength, sideLength, shift);
        }

    }

    public void drawLineHashIndices(Graphics g, int index1, int index2, int cornerX, int cornerY, int sideLength, int shift) {
        int[] startCoords = getCoordFromIndex(index1, cornerX, cornerY, sideLength, sideLength / 6);
        if (index1 == index2) {
            drawLineHash(g, startCoords[0], startCoords[1], cornerX + sideLength / 2, cornerY + sideLength / 2, new Color(0, 0, 0), shift);
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
