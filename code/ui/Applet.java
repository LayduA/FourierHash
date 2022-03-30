package code.ui;

import code.Crypto;
import code.transform.Blockies;
import code.transform.CompressPairRunnable;
import code.transform.TransformHash;
import code.types.Distance;
import code.types.DrawMode;
import code.types.Result;

import javax.imageio.ImageIO;
import javax.sound.midi.SysexMessage;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static code.transform.TransformHash.*;

public class Applet extends JFrame {

    private Blockies prng;

    public static boolean filtered = true;
    public final static Logger LOGGER = Logger.getLogger(Applet.class.getName());


    private int getHashLength(DrawMode m) {
        String digits = m.name().substring(m.name().length() - 3);
        if (digits.substring(1).equals("32") || digits.substring(1).equals("64"))
            return Integer.parseInt(digits.substring(1));
        return Integer.parseInt(digits);
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
        if (shift < 0) return 40 + 600 * (((-shift - 1) % 4) / 2) + (int) (HASH_W * 1.1 * ((-shift - 1) % 2));
        int ind = shift - 3;
        return 20 + 150 * (ind % 8);
    }

    public static int getShiftY(int shift) {
        if (0 < shift && shift < 3)
            return SHIFTS_Y[shift];
        if (shift < 0) return 40 + 300 * ((-shift - 1) / 4);
        int ind = shift - 3;
        return 50 + 150 * (ind / 8);
    }

    public final static int VERT_JITTER = (WINDOW_H - BANNER_H - HASH_H / 2) / 3;

    private DrawMode MODE;

    //private JTextField inputL;
    //private JTextField inputR;
    private Surface canvas;
    private JTextArea psiDisplay;
    private JComboBox<DrawMode> modeSelector;

    public void setMode(DrawMode mode) {
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
        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));

        JTextField inputL = new JTextField(DEFAULT_HASH, 25);
        JTextField inputR = new JTextField(DEFAULT_HASH, 25);

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
                updateHashes(canvas, inputL, inputR, 1, hamDistDisplay);
            }
        });
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
                updateHashes(canvas, inputR, inputL, 2, hamDistDisplay);
            }
        });
        canvas = new Surface();
        canvas.setPreferredSize(new Dimension(HASH_W, HASH_H));

        JPanel southL = new JPanel();
        JPanel topRowL = new JPanel();
        JPanel botRowL = new JPanel();

        JButton valButtonL = new JButton("See hash");
        valButtonL.addActionListener(l -> {
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputL.getText()) : null;
            DrawMode.sampleColors(prng);
            canvas.drawHash(this.getGraphics(), inputL.getText(), 1, MODE, prng);
        });

        JButton newButtonL = new JButton("New hash");
        newButtonL.addActionListener(l -> {
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputL.getText()) : null;
            DrawMode.sampleColors(prng);
            inputL.setText(newHash(getHashLength(MODE) / 4));
        });
        psiDisplay = new JTextArea("psi = ");
        JButton distButtonOnce = new JButton("Psi");
        distButtonOnce.addActionListener(l ->
                psiDisplay.setText(Double.toString(psiDist(canvas, inputL.getText(), inputR.getText(), MODE, "image"))));

        JButton distButton = new JButton("Comp");
        distButton.addActionListener(l -> {

            JFrame frame = new JFrame("Mode comparison");
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
                            setMode(DrawMode.values()[DrawMode.FourierDModRPhase256.ordinal() + (i - 1) / 2]);
                            in = i % 2 == 1 ? inputL.getText() : TransformHash.flipBits(inputL.getText(), Arrays.asList(MODE.worstBit(), 255));
                            canvasComp.drawHash(frame.getGraphics(), in, -i, MODE, prng);
                            JLabel jaj = new JLabel(i % 2 == 1 ? MODE.toString() :
                                    Double.toString(psiDist(canvas, inputL.getText(), TransformHash.flipBits(inputL.getText(), Arrays.asList(MODE.worstBit(), 255)), MODE, "image")));
                            jaj.setLocation(getShiftX(-i), getShiftY(-i) + HASH_H - 20);
                            jaj.setPreferredSize(new Dimension(HASH_W, 20));
                            jaj.setSize(jaj.getPreferredSize());
                            jaj.setVisible(true);
                            pane.add(jaj);
                        }
                    }
            );

            frame.getContentPane().add(BorderLayout.CENTER, pane);
            frame.getContentPane().add(BorderLayout.SOUTH, runButton);
            frame.pack();
            frame.setLocation(0, 0);
            frame.setVisible(true);

        });

        JButton plotButton = new JButton("Plot");
        plotButton.addActionListener(l -> plotHashes(inputL.getText()));

        JCheckBox checkBoxFiltered = new JCheckBox();
        checkBoxFiltered.setSelected(filtered);
        checkBoxFiltered.addActionListener(l -> filtered = checkBoxFiltered.isSelected());

        JButton saveButtonL = new JButton("Save");
        saveButtonL.addActionListener(l -> save(inputL));

        modeSelector = new JComboBox<>(DrawMode.values());
        modeSelector.addActionListener(l -> {
            DrawMode nextMode = (DrawMode) modeSelector.getSelectedItem();
            if (nextMode == null) return;
            if (getHashLength(MODE) != getHashLength(nextMode)) {
                inputL.setText(newHash(getHashLength(nextMode) / 4));
                updateHashes(canvas, inputL, inputR, 1, hamDistDisplay);
            }
            setMode(nextMode);

        });
        Distance[] distances = new Distance[Distance.values().length + 1];
        for (int i = 1; i < distances.length; i++) {
            distances[i] = Distance.values()[i - 1];
        }
        JComboBox<Distance> distSelector = new JComboBox<>(distances);
        distSelector.addActionListener(l -> {
            if (distSelector.getSelectedItem() != null) MODE.setDist((Distance) distSelector.getSelectedItem());
        });
        Double[] corrs = new Double[19];
        for (int i = 0; i < 19; i++) corrs[i] = (double)Math.round((i * 0.1 + 0.1) * 10d) / 10d ;
        JComboBox<Double> corrSelector = new JComboBox<>(corrs);
        corrSelector.addActionListener(l -> MODE.setCorr((Double) corrSelector.getSelectedItem()));

        topRowL.add(newButtonL, BorderLayout.WEST);
        topRowL.add(inputL, BorderLayout.CENTER);
        topRowL.add(valButtonL, BorderLayout.EAST);
        topRowL.add(checkBoxFiltered, BorderLayout.EAST);
        botRowL.add(saveButtonL);
        botRowL.add(modeSelector);
        botRowL.add(distSelector);
        botRowL.add(corrSelector);

        southL.add(topRowL, BorderLayout.NORTH);
        southL.add(distButtonOnce, BorderLayout.CENTER);
        southL.add(psiDisplay, BorderLayout.CENTER);
        southL.add(distButton, BorderLayout.CENTER);
        southL.add(plotButton, BorderLayout.CENTER);
        southL.add(botRowL, BorderLayout.SOUTH);
        southL.setPreferredSize(new Dimension(WINDOW_W, BANNER_H));
        topRowL.setPreferredSize(new Dimension(southL.getPreferredSize().width, southL.getPreferredSize().height / 2));
        botRowL.setPreferredSize(topRowL.getPreferredSize());
        left.add(southL, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);

        // -----------RIGHT HALF------------------------


        canvas = new Surface();
        canvas.setPreferredSize(new Dimension(HASH_W, HASH_H));

        JPanel southR = new JPanel();
        JPanel topRowR = new JPanel();
        JPanel botRowR = new JPanel();

        JButton valButtonR = new JButton("See hash");
        valButtonR.addActionListener(l -> {
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputR.getText()) : null;
            DrawMode.sampleColors(prng);
            reset(right, southR.getHeight());
            canvas.drawHash(this.getGraphics(), inputR.getText(), 2, MODE, prng);
        });
        JButton newButtonR = new JButton("New hash");
        newButtonR.addActionListener(l -> {
            inputR.setText(newHash(getHashLength(MODE) / 4));
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputR.getText()) : null;
            DrawMode.sampleColors(prng);
        });

        JButton saveButtonR = new JButton("Save");
        saveButtonR.addActionListener(l -> save(inputR));

        hamDistDisplay.setBackground(southR.getBackground());
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
                            ? flipBits(inputL.getText(), Integer.parseInt(flipIndex.getText(), 10), 255)
                            : flipBitsRandom(inputL.getText(), Integer.parseInt(flipValue.getText(), 10),
                            getHashLength(MODE)));

            reset(right, southR.getHeight());
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputR.getText()) : null;
            DrawMode.sampleColors(prng);
            canvas.drawHash(getGraphics(), inputR.getText(), 2, MODE, prng);

        });

        JButton shiftButton = new JButton("Shift");
        shiftButton.addActionListener(e -> {
            inputR.setText(shiftRBits(inputL.getText()));

            reset(right, southR.getHeight());
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputR.getText()) : null;
            DrawMode.sampleColors(prng);
            canvas.drawHash(getGraphics(), inputR.getText(), 2, MODE, prng);
        });

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            inputR.setText(inputL.getText());

            reset(right, southR.getHeight());
            prng = MODE == DrawMode.Blockies128 ? new Blockies(inputR.getText()) : null;
            DrawMode.sampleColors(prng);
            canvas.drawHash(getGraphics(), inputR.getText(), 2, MODE, prng);
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


        setMode(DrawMode.FourierDModDPhase256);

    }

    private void plotHashes(String refHash) {
        double[] averages = new double[255];
        int nHash = 1;
        //int[] ref;

        Path path = Paths.get("/Users/laya/Documents/VisualHashApplet/code/temp/");

        // read java doc, Files.walk need close the resources.
        // try-with-resources to ensure that the stream's open directories are closed
        try (Stream<Path> walk = Files.walk(path)) {
            walk
                    .sorted(Comparator.reverseOrder())
                    .forEach(l -> {
                        try {
                            Files.delete(l);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    });
        } catch (Exception e) {
            System.out.println(e);
        }
        new File("/Users/laya/Documents/VisualHashApplet/code/temp").mkdirs();
        CompressPairRunnable[] tasks = new CompressPairRunnable[Distance.values().length];
        Thread[] threads = new Thread[tasks.length];
        BufferedImage res = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
        String newRefHash;
        Result[][] results = new Result[tasks.length][19];
        for (int hashItr = 0; hashItr < nHash; hashItr++) {
            System.out.println("Iteration " + hashItr + " started....");
            //newRefHash = newHash(256 / 4);
            //ref = canvas.drawFourierHash(res.createGraphics(), newRefHash, 0, MODE);

            for (int i = 0; i < threads.length; i++) {
                //tasks[i] = new AvgRunnable(averages, i * 25, i == threads.length - 1 ? 5 : 25, newRefHash, ref, canvas, MODE);
                //tasks[i] = new AvgRunnable(averages, 0, i, newRefHash, ref, canvas, MODE);
                tasks[i] = new CompressPairRunnable(results[i], Distance.values()[i], refHash, canvas, MODE);
                threads[i] = new Thread(tasks[i], "Thread " + tasks[i].hashCode());
                threads[i].start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            System.out.println("...done.");
        }

        try {
//            File csvOutputFile = new File("code/data/" + MODE + "2bitsPhaseSimVSSize.csv");
//            PrintWriter pw = new PrintWriter(csvOutputFile);
//            DoubleStream.of(averages).map(d -> d / nHash).forEach(pw::println);
//            pw.close();
            File csvOutputFile = new File("code/data/paramsSearch" + MODE + ".json");
            PrintWriter pw = new PrintWriter(csvOutputFile);
            pw.print("{ \n \t \"values\": \n");
            Stream.of(results).map(Arrays::deepToString).forEach(pw::print);
            pw.print("\n}");
            pw.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Data ready.");
    }

    private void updateHashes(Surface canvas, JTextField inputChanged, JTextField inputOther, int shift, JTextArea display) {
        // int[] colors = new int[PALETTES[Math.min(MODE.ordinal(), 3)].length];
        // for (int i = 0; i < colors.length; i++) {
        // colors[i] = i;
        // }
        if (inputChanged.getText().length() != 0 && inputChanged.getText().length() == getHashLength(MODE) / 4) {
            prng = new Blockies(inputChanged.getText());
            DrawMode.sampleColors(prng);
            canvas.drawHash(getGraphics(), inputChanged.getText(), shift, MODE, prng);
            display.setText("Hamming distance = " + hamDistHex(inputChanged.getText(), inputOther.getText()));
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

    /**
     * Saves the visual hash corresponding to given input in a file.
     *
     * @param input the text field from which the visual hash is generated
     */
    public void save(JTextField input) {
        BufferedImage bImg = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);

        Graphics2D cg = bImg.createGraphics();
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        cg.setRenderingHints(rh);
        prng = MODE == DrawMode.Blockies128 ? new Blockies(input.getText()) : null;
        DrawMode.sampleColors(prng);
        canvas.drawHash(cg, input.getText(), 0, MODE, prng);
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
