package code.ui;

import static code.transform.TransformHash.buildHSVWheel;
import static code.transform.TransformHash.flipBitsRandom;
import static code.transform.TransformHash.hamDistHex;
import static code.transform.TransformHash.shiftRBits;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import code.Crypto;
import code.transform.Blockies;
import code.transform.TransformHash;

public class Applet extends JFrame {

    private Blockies prng;

    public enum DMODE {
        Antoine256, AntoineShift256, Adjacency1_256, Adjacency2_256, GridLines256, GridLines128, GridLines64,
        GridLines32, Landscape32,
        Blockies128, Random128, Fourier256, FourierB256;

        public int modeDist() {
            switch (this) {
                case Fourier256:
                    return Surface.CUBIC;
                case FourierB256:
                    return Surface.CUBIC;
                default:
                    return 0;
            }
        }
        }

    private int getHashLength(DMODE m) {
        String digits = m.name().substring(m.name().length() - 3);
        if (digits.substring(1).equals("32") || digits.substring(1).equals("64"))
            return Integer.parseInt(digits.substring(1));
        return Integer.parseInt(digits);
    }

    public static Color[] getPalette(DMODE m) {
        return getPalette(m, null);
    }

    private static Color back;
    private static Color front;
    private static Color spots;

    public static Color[] getPalette(DMODE m, Blockies prng) {
        switch (m) {
            case Antoine256:
                return new Color[]{new Color(0, 0, 0), new Color(255, 255, 255)};
            case Adjacency1_256:
            case Adjacency2_256:
                return new Color[]{new Color(255, 255, 255), new Color(255, 0, 0), new Color(0, 255, 0),
                        new Color(0, 0, 255), new Color(255, 255, 0), new Color(0, 255, 255), new Color(255, 0, 255)};
            case Blockies128:
                return new Color[]{back, front, spots};
            default:
                return buildHSVWheel(16);
        }
    }

    ;

    public final static int WINDOW_W = 1100;
    public final static int WINDOW_H = 650;
    public final static int BANNER_H = 160;
    public final static int HASH_W = 256;
    public final static int HASH_H = 256;

    public final static int HASH_X_0 = (WINDOW_W / 2 - HASH_W) / 2;
    public final static int HASH_X_1 = (HASH_X_0 * 3) / 2 + WINDOW_W / 2;
    public final static int HASH_Y = (WINDOW_H - HASH_H - BANNER_H) / 2;
    public final static String DEFAULT_HASH = "e7e6bda1152ee0ec0f2082cd041e2cdc02f4b390b01bde55ae0abbb7cc99bc2c";
    private final static int[] SHIFTS_X = {1, HASH_X_0, HASH_X_1};
    public final static int[] SHIFTS_Y = {1, HASH_Y, HASH_Y * 3 / 2};

    public final static int getShiftX(int shift) {
        if (shift < 3)
            return SHIFTS_X[shift];
        int ind = shift - 3;
        return 20 + (int) Math.floor(150 * (ind % 8));
    }

    public final static int getShiftY(int shift) {
        if (shift < 3)
            return SHIFTS_Y[shift];
        int ind = shift - 3;
        return 50 + (int) Math.floor(150 * (ind / 8));
    }

    public final static int VERT_JITTER = (WINDOW_H - BANNER_H - HASH_H / 2) / 3;

    private DMODE MODE;

    private JTextField inputL;
    private JTextField inputR;
    private Surface canvas1;
    private Surface canvas2;
    private JTextArea psiDisplay;

    public Applet() {

        initUI();
    }


    private void initUI() {
        MODE = DMODE.FourierB256;
        setLayout(new BorderLayout());
        // This has to be defined here to have element from left half update elements on
        // right half
        JTextArea hamDistDisplay = new JTextArea("Hamming distance = ");

        // --------------LEFT HALF--------------

        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));

        canvas1 = new Surface();
        canvas1.setPreferredSize(new Dimension(HASH_W, HASH_H));

        JPanel southL = new JPanel();
        JPanel topRowL = new JPanel();
        JPanel botRowL = new JPanel();

        JButton valButtonL = new JButton("See hash");
        valButtonL.addActionListener(l -> {
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputL.getText()) : null;
            sampleColors();
            canvas1.drawHash(this.getGraphics(), inputL.getText(), 1, MODE, prng);
        });

        JButton newButtonL = new JButton("New hash");
        newButtonL.addActionListener(l -> {
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputL.getText()) : null;
            sampleColors();
            inputL.setText(newHash(getHashLength(MODE) / 4));
        });
        psiDisplay = new JTextArea("psi = ");
        JButton distButtonOnce = new JButton("Psi");
        distButtonOnce.addActionListener(l -> {
            psiDist(inputL.getText(), inputR.getText());

//        String dist;
//        double maxdist = 0;
//            for (int i = 0; i < 256; i++) {
//                dist = psiDist(inputL.getText(), TransformHash.flipBit(inputL.getText(), i));
//                //System.out.println(dist);
//                int jaj = Integer.parseInt(dist, 2, 5, 10);
//                if (jaj != 999 && jaj > 500) {
//                    System.out.println("bit " + i + ", dist = " + dist);
//                    if(jaj > maxdist) maxdist = jaj;
//                }
//            }
        });

        JButton distButton = new JButton("Plot");
        distButton.addActionListener(l -> {
            String path = "/Users/laya/Documents/VisualHashApplet/applet_env/Scripts/python";
            ProcessBuilder pB = new ProcessBuilder(path, ("code/haar_psi.py"));
            pB.redirectErrorStream(true);
            StringBuilder sb = new StringBuilder();
            int NB_CORR = 10;
            String[][] similarities = new String[7][NB_CORR];
            BufferedImage res = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
            int[] pixels1, pixels2;
            int bitMin = 0;
            int sim;
            for (int dist = 0; dist < similarities.length; dist++) {
                System.out.println("dist =" + dist);
                for (int corr = 0; corr < NB_CORR; corr++) {

                    sim = 0;
                    double corrD = corr / 10.0;
                    for (int iter = 0; iter < 10; iter++) {
                        sb = new StringBuilder();
                        pixels1 = canvas1.drawFourierHashRandomPhase(res.createGraphics(), Crypto.getHash(corr + "" + iter), 0,
                                MODE, prng, dist, corrD);
                        pixels2 = canvas2.drawFourierHashRandomPhase(
                                res.createGraphics(), Crypto.getHash(corr + "" + iter + "" + "prime"), 0,
                                MODE, prng, dist, corrD);
                        // PSI DISTANCE
                        try {

                            Process proc = pB.start();
                            byte[] results = proc.getInputStream().readAllBytes();
                            for (int i = 0; i < results.length; i++) {
                                sb.append((char) results[i]);
                            }
                            String out = sb.toString();
                            // phiDisplay.setText(out);
                            sim += Integer.parseInt(out, 2, 7, 10); // 5 first decimals
                            // System.out.println(similarities[dist][bit]);
                        } catch (Exception e) {
                            System.out.println("ERROR OULALA PANIC" + e);
                        }
                    }
                    similarities[dist][corr] = dist + "," + corrD + "," + (sim / 100_000.0);
                    // COMPRESSION
                    // try {
                    // int[] bitMasks = new int[]{0xFF0000, 0xFF00, 0xFF};
                    // SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
                    // DataBuffer.TYPE_INT, HASH_W, HASH_H, bitMasks);
                    // DataBufferInt db = new DataBufferInt(pixels1, pixels1.length);
                    // WritableRaster wr = Raster.createWritableRaster(sm, db, new Point());
                    // //System.out.println(ColorModel.getRGBdefault().hasAlpha());
                    // res = new BufferedImage(new DirectColorModel(24,bitMasks[0], bitMasks[1],
                    // bitMasks[2]), wr, false, null);

                    // File compressedImageFile = new File("compress.jpg");
                    // OutputStream os = new FileOutputStream(compressedImageFile);
                    // Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                    // ImageWriter writer = (ImageWriter) writers.next();

                    // File notcompressedImageFile = new File("notcompress.jpg");
                    // OutputStream notos = new FileOutputStream(notcompressedImageFile);

                    // ImageOutputStream nios = ImageIO.createImageOutputStream(notos);
                    // writer.setOutput(nios);
                    // writer.write(null, new IIOImage(res, null, null), null);

                    // ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                    // writer.setOutput(ios);

                    // ImageWriteParam param = writer.getDefaultWriteParam();

                    // param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    // param.setCompressionQuality(0.05f);
                    // writer.write(null, new IIOImage(res, null, null), param);
                    // ios.close();

                    // //System.out.println((double) compressedImageFile.length() /
                    // notcompressedImageFile.length());

                    // } catch (FileNotFoundException e) {
                    // System.out.println(e);
                    // } catch (IOException e) {
                    // System.out.println(e);
                    // }
                }
            }

            try {
                File csvOutputFile = new File("code/simvsCorr" + bitMin + "" + (bitMin + 5) + ".csv");
                PrintWriter pw = new PrintWriter(csvOutputFile);
                for (int i = 0; i < similarities.length; i++) {
                    for (int k = 0; k < similarities[0].length; k++) {
                        pw.println(similarities[i][k]);
                    }

                }

                pw.close();
            } catch (Exception e) {
                System.err.println("final output error \n" + e);
            }

        });
        JButton saveButtonL = new JButton("Save");
        saveButtonL.addActionListener(l -> save(inputL));

        inputL = new JTextField(DEFAULT_HASH, 25);
        inputL.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void changed() {
                reset(left, BANNER_H);
                updateHashes(canvas1, inputL, 1, hamDistDisplay);
            }
        });

        JComboBox<DMODE> modeSelector = new JComboBox<DMODE>(DMODE.values());
        modeSelector.addActionListener(l -> {
            DMODE nextMode = (DMODE) modeSelector.getSelectedItem();
            if (getHashLength(MODE) != getHashLength(nextMode)) {
                inputL.setText(newHash(getHashLength(nextMode) / 4));
                updateHashes(canvas1, inputL, 1, hamDistDisplay);
                ;
            }
            MODE = nextMode;

        });

        topRowL.add(newButtonL, BorderLayout.WEST);
        topRowL.add(inputL, BorderLayout.CENTER);
        topRowL.add(valButtonL, BorderLayout.EAST);
        botRowL.add(saveButtonL);
        botRowL.add(modeSelector);

        southL.add(topRowL, BorderLayout.NORTH);
        southL.add(distButtonOnce, BorderLayout.CENTER);
        southL.add(psiDisplay, BorderLayout.CENTER);
        southL.add(distButton, BorderLayout.CENTER);
        southL.add(botRowL, BorderLayout.SOUTH);
        southL.setPreferredSize(new Dimension(WINDOW_W, BANNER_H));
        topRowL.setPreferredSize(new Dimension(southL.getPreferredSize().width, southL.getPreferredSize().height / 2));
        botRowL.setPreferredSize(topRowL.getPreferredSize());
        left.add(southL, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);

        // -----------RIGHT HALF------------------------

        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));

        canvas2 = new Surface();
        canvas2.setPreferredSize(new Dimension(HASH_W, HASH_H));

        JPanel southR = new JPanel();
        JPanel topRowR = new JPanel();
        JPanel botRowR = new JPanel();

        JButton valButtonR = new JButton("See hash");
        valButtonR.addActionListener(l -> {
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputR.getText()) : null;
            sampleColors();
            reset(right, southR.getHeight());
            canvas2.drawHash(this.getGraphics(), inputR.getText(), 2, MODE, prng);
        });
        JButton newButtonR = new JButton("New hash");
        newButtonR.addActionListener(l -> {
            inputR.setText(newHash(getHashLength(MODE) / 4));
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputR.getText()) : null;
            sampleColors();
        });

        JButton saveButtonR = new JButton("Save");
        saveButtonR.addActionListener(l -> save(inputR));

        hamDistDisplay.setBackground(southR.getBackground());
        inputR = new JTextField(newHash(getHashLength(MODE) / 4), 25);
        inputR.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void changed() {
                reset(right, BANNER_H);
                updateHashes(canvas2, inputR, 2, hamDistDisplay);
            }
        });
        JTextField flipIndex = new JTextField("27", 3);
        JTextField flipValue = new JTextField("1", 3);
        flipValue.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed(e);
            }

            public void removeUpdate(DocumentEvent e) {
                changed(e);
            }

            public void insertUpdate(DocumentEvent e) {
                changed(e);
            }

            public void changed(DocumentEvent e) {
                try {
                    flipIndex.setVisible(e.getDocument().getText(0, 1).equals("1") && e.getDocument().getLength() == 1);
                    botRowR.repaint();
                } catch (BadLocationException err) {
                }
            }
        });
        JButton flipButton = new JButton("Flip");
        flipButton.addActionListener(e -> {
            reset(right, southR.getHeight());
            inputR.setText(
                    flipIndex.isVisible()
                            ? TransformHash.flipBit(inputL.getText(), Integer.parseInt(flipIndex.getText(), 10))
                            : flipBitsRandom(inputL.getText(), Integer.parseInt(flipValue.getText(), 10),
                            getHashLength(MODE)));

            reset(right, southR.getHeight());
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputR.getText()) : null;
            sampleColors();
            canvas2.drawHash(getGraphics(), inputR.getText(), 2, MODE, prng);

        });

        JButton shiftButton = new JButton("Shift");
        shiftButton.addActionListener(e -> {
            inputR.setText(shiftRBits(inputL.getText()));

            reset(right, southR.getHeight());
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputR.getText()) : null;
            sampleColors();
            canvas2.drawHash(getGraphics(), inputR.getText(), 2, MODE, prng);
        });

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            inputR.setText(inputL.getText());

            reset(right, southR.getHeight());
            prng = MODE == DMODE.Blockies128 ? new Blockies(inputR.getText()) : null;
            sampleColors();
            canvas2.drawHash(getGraphics(), inputR.getText(), 2, MODE, prng);
        });

        JButton demoButton = new JButton("Demo");
        demoButton.addActionListener(l -> {
            JFrame frame = new JFrame("Test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(1200, 600));
            Surface canv = new Surface();
            canv.setPreferredSize(new Dimension(1200, 600));

            JButton runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                // canv.setLocation(200, 200);
                for (int i = 3; i < 35; i++) {
                    canv.drawHash(frame.getGraphics(),
                            TransformHash.flipBit(inputL.getText(), (MODE == DMODE.Fourier256 ? 3 : 1) * (i - 3) + 16),
                            i, MODE, prng);
                }
            });

            // frame.add(panel);
            // panel.add(canv);
            // panel.add(runButton);

            // frame.getContentPane().add(BorderLayout.CENTER, panel);
            frame.getContentPane().add(BorderLayout.CENTER, canv);
            frame.getContentPane().add(BorderLayout.SOUTH, runButton);
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
            frame.setResizable(false);

        });

        topRowR.add(newButtonR, BorderLayout.WEST);
        topRowR.add(inputR, BorderLayout.CENTER);
        topRowR.add(valButtonR, BorderLayout.EAST);
        botRowR.add(copyButton);
        botRowR.add(shiftButton);
        botRowR.add(saveButtonR);
        botRowR.add(flipButton);
        botRowR.add(flipValue);
        botRowR.add(flipIndex);
        botRowR.add(demoButton);

        southR.add(topRowR, BorderLayout.NORTH);
        southR.add(hamDistDisplay, BorderLayout.CENTER);
        southR.add(botRowR, BorderLayout.SOUTH);
        southR.setPreferredSize(new Dimension(WINDOW_W, BANNER_H));
        topRowR.setPreferredSize(new Dimension(southR.getPreferredSize().width, southR.getPreferredSize().height / 2));
        botRowR.setPreferredSize(topRowR.getPreferredSize());
        right.add(southR, BorderLayout.SOUTH);

        right.setBackground(left.getBackground());

        add(right, BorderLayout.EAST);

        // ----------------------------------------

        setTitle("Visual Hash Applet");
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public String psiDist(String in1, String in2) {
        String path = "/Users/laya/Documents/VisualHashApplet/applet_env/Scripts/python";
        ProcessBuilder pB = new ProcessBuilder(path, ("code/haar_psi.py"));
        pB.redirectErrorStream(true);
        StringBuilder sb = new StringBuilder();
        BufferedImage res = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
        int[] pixels1, pixels2;
        if (MODE == DMODE.FourierB256) {
            pixels1 = canvas1.drawFourierHashDetPhase(res.createGraphics(), in1, 0,
                    MODE, prng, Surface.CUBIC, 1.7);
            pixels2 = canvas2.drawFourierHashDetPhase(
                    res.createGraphics(), in2, 0,
                    MODE, prng, Surface.CUBIC, 1.7);
        } else if (MODE == DMODE.Fourier256) {
            pixels1 = canvas1.drawFourierHashRandomPhase(res.createGraphics(), in1, 0,
                    MODE, prng, Surface.SIGMOID, 1.0);
            pixels2 = canvas2.drawFourierHashRandomPhase(
                    res.createGraphics(), in2, 0,
                    MODE, prng, Surface.SIGMOID, 1.0);
        } else {
            return null;
        }
        try {
            File csvOutputFile = new File("code/image1.csv");
            PrintWriter pw = new PrintWriter(csvOutputFile);
            IntStream.of(pixels1).forEach(pw::println);
            csvOutputFile = new File("code/image2.csv");
            pw.close();
            pw = new PrintWriter(csvOutputFile);
            IntStream.of(pixels2).forEach(pw::println);
            pw.close();
            Process proc = pB.start();
            byte[] results = proc.getInputStream().readAllBytes();
            for (int i = 0; i < results.length; i++) {
                sb.append((char) results[i]);
            }
        } catch (Exception e) {
            System.out.println("Ah non pas une erreur oh non " + e);
        }
        String out = sb.toString();
        psiDisplay.setText(out);

        return out;
    }

    private void sampleColors() {
        if (prng == null)
            return;
        front = prng.createColor();
        back = prng.createColor();
        spots = prng.createColor();
    }

    private void updateHashes(Surface canvas, JTextField input, int shift, JTextArea display) {
        // int[] colors = new int[PALETTES[Math.min(MODE.ordinal(), 3)].length];
        // for (int i = 0; i < colors.length; i++) {
        // colors[i] = i;
        // }
        if (input.getText().length() != 0 && input.getText().length() == getHashLength(MODE) / 4) {
            prng = new Blockies(input.getText());
            sampleColors();
            canvas.drawHash(getGraphics(), input.getText(), shift, MODE, prng);
            display.setText("Hamming distance = " + hamDistHex(inputL.getText(), inputR.getText()));
        }
    }

    public static void main(String[] args) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Applet app = new Applet();
                app.setVisible(true);
            }
        });
    }

    private String newHash(int length) {
        return Crypto.getHash(
                        Integer.toHexString(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)))
                .substring(0, length);
    }

    public void save(JTextField input) {
        BufferedImage bImg = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);

        Graphics2D cg = bImg.createGraphics();
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        cg.setRenderingHints(rh);
        prng = MODE == DMODE.Blockies128 ? new Blockies(input.getText()) : null;
        sampleColors();
        canvas1.drawHash(cg, input.getText(), 0, MODE, prng);
        // cg.setColor(new Color (20, 254, 100));
        // cg.fillRect(0, 0, HASH_W, HASH_H);

        try {
            BigInteger big = new BigInteger(input.getText(), 16);
            int mod = big.mod(new BigInteger("1337", 10)).intValue();
            String fileName = (input.getText().substring(0, 8) + String.format("%4s", mod).replace(" ", "0"));
            if (ImageIO.write(bImg, "png", new File("outputs/" + fileName + ".png"))) {
                Writer output;
                output = new BufferedWriter(new FileWriter("outputs/names.txt", true)); // clears file every time
                output.append(fileName + " -> " + input.getText() + "\r\n");
                output.close();
                System.out.println("-- saved");
            }

        } catch (IOException e) {
            System.err.println("Ayo");
            e.printStackTrace();
        }
    }

    private void reset(JPanel pan, int heightLim) {
        Graphics g = pan.getGraphics();
        g.setColor(pan.getBackground());
        g.fillRect(0, 0, pan.getWidth(), pan.getHeight() - heightLim);
    }
}
