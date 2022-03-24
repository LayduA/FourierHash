package code.ui;

import code.Crypto;
import code.transform.Blockies;
import code.transform.TransformHash;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static code.transform.TransformHash.*;

public class Applet extends JFrame {

    private Blockies prng;

    public static boolean filtered = true;

    public enum DMODE {
        Antoine256, AntoineShift256, Adjacency1_256, Adjacency2_256, GridLines256, GridLines128, GridLines64,
        GridLines32, Landscape32,
        Blockies128, Random128, FourierDModRPhase256, FourierRModDPhase256, FourierDModDPhase256, FourierRModRPhase256;

        public int modeDist() {
            switch (this) {
                case FourierDModRPhase256:
                    return Surface.CUBIC;
                case FourierDModDPhase256:
                    return Surface.SIGMOID;
                case FourierRModRPhase256:
                    return Surface.BELL;
                default:
                    return Surface.SQUARE;
            }
        }

        public int worstBit(){
            switch (this){
                case FourierDModRPhase256:
                case FourierRModDPhase256:
                case FourierDModDPhase256:
                    return 147;
                default: return 0;
            }
        }

        public boolean getFiltered(){
            return filtered;
        }

        public double modeCorr() {
            switch (this) {
                case FourierDModRPhase256:
                    return 0.505;
                case FourierDModDPhase256:
                    return 0.451;
                default:
                    return 0.5;
            }
        }

        public int modeCut() {
            switch (this) {
                case FourierDModRPhase256:
                case FourierRModDPhase256:
                case FourierDModDPhase256:
                    return 6;
                case FourierRModRPhase256:
                    return 127;
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

    private static Color back;
    private static Color front;
    private static Color spots;

    public static Color[] getPalette(DMODE m) {
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

    public static int getShiftX(int shift) {
        if (0 < shift && shift < 3)
            return SHIFTS_X[shift];
        if (shift < 0) return 40 + 600 * (((-shift - 1) % 4) / 2) + (int)(HASH_W * 1.1 * ((-shift - 1) % 2));
        int ind = shift - 3;
        return 20 + 150 * (ind % 8);
    }

    public static int getShiftY(int shift) {
        if (0 < shift && shift < 3)
            return SHIFTS_Y[shift];
        if (shift < 0) return 40 + 300 * ((-shift -1) / 4);
        int ind = shift - 3;
        return 50 + 150 * (ind / 8);
    }

    public final static int VERT_JITTER = (WINDOW_H - BANNER_H - HASH_H / 2) / 3;

    private DMODE MODE;

    private JTextField inputL;
    private JTextField inputR;
    private Surface canvas1;
    private Surface canvas2;
    private JTextArea psiDisplay;
    private JComboBox<DMODE> modeSelector;

    public void setMode(DMODE mode){
        MODE = mode;
        modeSelector.setSelectedIndex(mode.ordinal());
    }

    public Applet() {

        initUI();
    }


    private void initUI() {
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
        distButtonOnce.addActionListener(l -> psiDist(inputL.getText(), inputR.getText()));

        JButton distButton = new JButton("Comp");
        distButton.addActionListener(l -> {
            String path = "/Users/laya/Documents/VisualHashApplet/applet_env/Scripts/python";
            ProcessBuilder pB = new ProcessBuilder(path, ("code/haar_psi.py"));
            pB.redirectErrorStream(true);

            JFrame frame = new JFrame("Test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLayeredPane pane = new JLayeredPane();

            Surface canvasComp = new Surface();
            canvasComp.setPreferredSize(new Dimension(1200, 600));
            //pane.add(canvasComp, Integer.valueOf(1));
            pane.setPreferredSize(canvasComp.getPreferredSize());
            //pane.add(jaj);

            JButton runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                        String in;
                        for (int i = 1; i < 9; i++) {
                            setMode(DMODE.values()[DMODE.FourierDModRPhase256.ordinal() + (i - 1)/2]);
                            in = i % 2 == 1 ? inputL.getText() : TransformHash.flipBits(inputL.getText(), Arrays.asList(MODE.worstBit(), 255));
                            canvasComp.drawHash(frame.getGraphics(), in, -i, MODE, prng);
                            JLabel jaj = new JLabel(i % 2 == 1 ? MODE.toString() : psiDist(inputL.getText(), TransformHash.flipBits(inputL.getText(), Arrays.asList(MODE.worstBit(), 255))));
                            jaj.setLocation(getShiftX(-i),getShiftY(-i) + HASH_H - 20);
                            jaj.setPreferredSize(new Dimension(HASH_W,20));
                            jaj.setSize(jaj.getPreferredSize());
                            jaj.setVisible(true);
                            pane.add(jaj);
                        }
                    }
            );

            frame.getContentPane().add(BorderLayout.CENTER, pane);
            frame.getContentPane().add(BorderLayout.SOUTH, runButton);
            frame.pack();
            frame.setLocation(0,0);
            frame.setVisible(true);

        });

        JCheckBox checkBoxFiltered = new JCheckBox();
        checkBoxFiltered.setSelected(filtered);
        checkBoxFiltered.addActionListener(l -> filtered = checkBoxFiltered.isSelected());

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

        modeSelector = new JComboBox<>(DMODE.values());
        modeSelector.addActionListener(l -> {
            DMODE nextMode = (DMODE) modeSelector.getSelectedItem();
            if(nextMode == null) return;
            if (getHashLength(MODE) != getHashLength(nextMode)) {
                inputL.setText(newHash(getHashLength(nextMode) / 4));
                updateHashes(canvas1, inputL, 1, hamDistDisplay);
            }
            setMode(nextMode);

        });

        topRowL.add(newButtonL, BorderLayout.WEST);
        topRowL.add(inputL, BorderLayout.CENTER);
        topRowL.add(valButtonL, BorderLayout.EAST);
        topRowL.add(checkBoxFiltered, BorderLayout.EAST);
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
        inputR = new JTextField(25);
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
                    System.out.println("error " + err);
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
            Surface canvasDemo = new Surface();
            canvasDemo.setPreferredSize(new Dimension(1200, 600));

            JButton runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                // canv.setLocation(200, 200);
                for (int i = 3; i < 35; i++) {
                    canvasDemo.drawHash(frame.getGraphics(),
                            TransformHash.flipBit(inputL.getText(), (i - 3)),
                            i, MODE, prng);
                }
            });

            // frame.add(panel);
            // panel.add(canv);
            // panel.add(runButton);

            // frame.getContentPane().add(BorderLayout.CENTER, panel);
            frame.getContentPane().add(BorderLayout.CENTER, canvasDemo);
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


        setMode(DMODE.FourierDModRPhase256);

    }

    public String psiDist(String in1, String in2) {
        String path = "/Users/laya/Documents/VisualHashApplet/applet_env/Scripts/python";
        ProcessBuilder pB = new ProcessBuilder(path, ("code/haar_psi.py"));
        pB.redirectErrorStream(true);
        StringBuilder sb = new StringBuilder();
        BufferedImage res = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
        int[] pixels1, pixels2;

        pixels1 = canvas1.drawFourierHash(res.createGraphics(), in1, 0,
                MODE);
        pixels2 = canvas1.drawFourierHash(
                res.createGraphics(), in2, 0,
                MODE);

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
            for (byte result : results) {
                sb.append((char) result);
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

        EventQueue.invokeLater(() -> {
            Applet app = new Applet();
            app.setVisible(true);
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
                output.append(fileName).append(" -> ").append(input.getText()).append("\r\n");
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
