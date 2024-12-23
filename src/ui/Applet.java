package src.ui;

import src.crypto.PRNG128BitsInsecure;
import src.crypto.SecureCrypto;
import src.hashops.AttackIndices;
import src.hashops.AvgDistRunnable;
import src.hashops.HashDrawer;
import src.hashops.SimpleHashRunnable;
import src.types.Distance;
import src.types.DrawMode;
import src.types.DrawParams;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static src.hashops.HashTransform.*;

public class Applet extends JFrame {

    public final static Logger LOGGER = Logger.getLogger(Applet.class.getName());
    public final static int WINDOW_W = 1100;
    public final static int WINDOW_H = 650;
    public final static int BANNER_H = 160;
    public final static int HASH_W = 256;
    public final static int HASH_H = 256;
    public final static int HASH_X_0 = (WINDOW_W / 2 - HASH_W) / 2;
    public final static int HASH_X_1 = (HASH_X_0 * 3) / 2 + WINDOW_W / 2;
    private final static int[] SHIFTS_X = {0, HASH_X_0, HASH_X_1};
    public final static int HASH_Y = (WINDOW_H - HASH_H - BANNER_H) / 2;
    public final static int[] SHIFTS_Y = {0, HASH_Y, HASH_Y * 3 / 2};
    public final static String DEFAULT_HASH = "75b9bce343839d7faa6c3e0c9152fa0c75b9bce343839d7faa6c3e0c9152fa0c";
    public DrawParams params;
    private HashDrawer canvas;
    private JTextArea psiDisplay;
    private JComboBox<DrawMode> modeSelector;

    public Applet() {
        initUI();
    }

    public static int getShiftX(int shift) {
        if (0 <= shift && shift < 3)
            return SHIFTS_X[shift];
        if (shift < 0) return 40 + 600 * (((-shift - 1) % 4) / 2) + (int) (HASH_W * 1.1 * ((-shift - 1) % 2));

        int ind = shift - 3;
        return 20 + 150 * (ind % 8);
    }

    public static int getShiftY(int shift) {
        if (0 <= shift && shift < 3)
            return SHIFTS_Y[shift];
        if (shift < 0) return 40 + 300 * ((-shift - 1) / 4);
        int ind = shift - 3;
        if (shift == 1000) {
            return 10;
        }
        return 50 + 150 * (ind / 8);
    }

    public static void main(String[] args) {
        System.out.println(SecureCrypto.getHash("hello"));
        EventQueue.invokeLater(() -> {
            Applet app = new Applet();
            app.setVisible(true);
        });
    }

    public static String newHash(int length) {
        return SecureCrypto.getHash(
                        Integer.toHexString(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)))
                .substring(0, length);
    }

    public void setMode(DrawMode mode) {
        params.setMode(mode);
        modeSelector.setSelectedIndex(mode.ordinal());
    }

    private void initUI() {
        params = new DrawParams(DrawMode.FourierCartesian128);
        setLayout(new BorderLayout());
        // This has to be defined here to have element from left half update elements on
        // right half
        JTextArea hamDistDisplay = new JTextArea("Hamming distance = ");

        // --------------LEFT HALF--------------

        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));
        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));

        JTextField inputL = new JTextField(DEFAULT_HASH.substring(0, params.getMode().length() / 4), 25);
        JTextField inputR = new JTextField(DEFAULT_HASH.substring(0, params.getMode().length() / 4), 25);

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
                updateHashes(canvas, inputL, inputR, 1, hamDistDisplay, false);
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
                updateHashes(canvas, inputR, inputL, 2, hamDistDisplay, true);
            }
        });
        canvas = new HashDrawer();
        canvas.setPreferredSize(new Dimension(HASH_W, HASH_H));

        JPanel southL = new JPanel();
        JPanel topRowL = new JPanel();
        JPanel botRowL = new JPanel();

        JButton valButtonL = new JButton("See");
        valButtonL.addActionListener(l -> {
            params.sampleColors();
            canvas.drawHash(this.getGraphics(), inputL.getText(), 1, params);
        });

        JButton newButtonL = new JButton("New");
        newButtonL.addActionListener(l -> {
            params.sampleColors();
            inputL.setText(newHash(params.getMode().length() / 4));
        });

        JComboBox<Distance.Shape> distModeSelector = new JComboBox<>(Distance.Shape.values());
        distModeSelector.addActionListener(l -> params.setDistMode(Distance.Shape.values()[distModeSelector.getSelectedIndex()]));
        distModeSelector.setSelectedItem(params.getDistMode());

        psiDisplay = new JTextArea("psi = ");
        JButton distButtonOnce = new JButton("Psi");
        distButtonOnce.addActionListener(l ->
                psiDisplay.setText(Double.toString(psiDist(canvas, inputL.getText(), inputR.getText(), params, "image")).substring(0, 6)));

        JButton compButton = new JButton("Comp");
        compButton.addActionListener(l -> {

            JFrame frame = new JFrame("Mode comparison");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLayeredPane pane = new JLayeredPane();

            HashDrawer canvasComp = new HashDrawer();
            canvasComp.setPreferredSize(new Dimension(1200, 600));
            //pane.add(canvasComp, Integer.valueOf(1));
            pane.setPreferredSize(canvasComp.getPreferredSize());
            //pane.add(jaj);

            JButton runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                        String in;

                        for (int i = 1; i < 9; i++) {
                            params.setModDet((i - 1) % 4 < 2 ? DrawParams.Deter.DET : DrawParams.Deter.RAND);
                            params.setPhaseDet(i <= 4 ? DrawParams.Deter.DET : DrawParams.Deter.RAND);
                            in = i % 2 == 1 ? inputL.getText() : flipBit(inputL.getText(), params.worstBit());
                            canvasComp.drawHash(frame.getGraphics(), in, -i, params);
                            JLabel jaj = new JLabel(i % 2 == 1 ? "Modulus : " + params.getModDet() + ", Phase : " + params.getPhaseDet() :
                                    Double.toString(psiDist(canvas, inputL.getText(), flipBit(inputL.getText(), params.worstBit()), params, "image")));
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

        JButton plotButton = new JButton("Get Data");
        plotButton.addActionListener(l -> getHashesDataDists());

        JCheckBox checkBoxClassicRGB = new JCheckBox("RGB");
        checkBoxClassicRGB.setSelected(params.isClassicRGB());
        checkBoxClassicRGB.addActionListener(l -> params.setClassicRGB(checkBoxClassicRGB.isSelected()));

        JCheckBox checkBoxFiltered = new JCheckBox("Filt");
        checkBoxFiltered.setSelected(params.isFiltered());
        checkBoxFiltered.addActionListener(l -> params.setFiltered(checkBoxFiltered.isSelected()));

        JCheckBox checkBoxSymmetry = new JCheckBox("Sym");
        checkBoxSymmetry.setSelected(params.isSymmetric());
        checkBoxSymmetry.addActionListener(l -> {
            params.setSymmetric(checkBoxSymmetry.isSelected());
            updateHashes(canvas, inputL, inputR, 1, hamDistDisplay, false);
        });

        JCheckBox checkBoxModPhase = new JCheckBox("Phase");
        checkBoxModPhase.addActionListener(l -> {
            params.setSeePhase(checkBoxModPhase.isSelected());
        });

        JButton saveButtonL = new JButton("Save");
        saveButtonL.addActionListener(l -> save(inputL));

        modeSelector = new JComboBox<>(DrawMode.values());
        modeSelector.addActionListener(l -> {
            DrawMode nextMode = (DrawMode) modeSelector.getSelectedItem();
            if (nextMode == null) return;
            if (params.getMode().length() != nextMode.length()) {
                inputL.setText(newHash(nextMode.length() / 4));
                updateHashes(canvas, inputL, inputR, 1, hamDistDisplay, false);
            }
            setMode(nextMode);

        });
        modeSelector.setSelectedItem(params.getMode());

        JComboBox<Distance> distSelector = new JComboBox<>(Distance.values());
        distSelector.addActionListener(l -> {
            if (distSelector.getSelectedItem() != null) params.setDist((Distance) distSelector.getSelectedItem());
        });
        distSelector.setSelectedItem(params.getDist());

        JTextField corrSelector = new JTextField(4);
        corrSelector.addActionListener(l -> {
            params.setCorr(Double.parseDouble(corrSelector.getText()));
        });
        corrSelector.setText(Double.toString(params.getCorr()));

        DrawParams.Deter[] moduli = new DrawParams.Deter[]{DrawParams.Deter.DET, DrawParams.Deter.RAND, DrawParams.Deter.FIXED};
        JComboBox<DrawParams.Deter> modulusSelector = new JComboBox<>(moduli);
        modulusSelector.addActionListener(l -> params.setModDet((DrawParams.Deter) modulusSelector.getSelectedItem()));
        modulusSelector.setSelectedItem(params.getModDet());

        DrawParams.Deter[] phases = new DrawParams.Deter[]{DrawParams.Deter.DET, DrawParams.Deter.RAND, DrawParams.Deter.DOUBLE};
        JComboBox<DrawParams.Deter> phaseSelector = new JComboBox<>(phases);
        phaseSelector.addActionListener(l -> params.setPhaseDet((DrawParams.Deter) phaseSelector.getSelectedItem()));
        phaseSelector.setSelectedItem(params.getPhaseDet());

        JLabel cutLabel = new JLabel("Cut threshold");
        JTextField cutValue = new JTextField(3);
        cutValue.setText(params.cut() + "");
        cutValue.getDocument().addDocumentListener(new DocumentListener() {
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
                if (cutValue.getText().length() > 0) params.setCut(Integer.parseInt(cutValue.getText()));
            }
        });

        JLabel bitsUsed = new JLabel();
        JButton checkButton = new JButton("Check bits");
        checkButton.addActionListener(l -> {
            BufferedImage b = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
            bitsUsed.setText(Integer.toString(canvas.drawFourierHash(b.createGraphics(), inputL.getText(), 0, params)[256 * 256]));
        });

        JTextField thresholdField = new JTextField("0.5", 3);
        thresholdField.addActionListener(l -> {
            params.setThreshold(Double.parseDouble(thresholdField.getText()));
        });

        JButton paramsButton = new JButton("Params");
        paramsButton.addActionListener(l -> {
            JFrame frame = new JFrame("Params selection");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel pane = new JPanel();
            pane.setPreferredSize(new Dimension(200, 400));
            pane.add(new JLabel("Distance"));
            pane.add(distSelector);
            pane.add(new JLabel("Correction factor"));
            pane.add(corrSelector);
            pane.add(new JLabel("Spectrum shape"));
            pane.add(distModeSelector);
            pane.add(new JLabel("Modulus computation"));
            pane.add(modulusSelector);
            pane.add(new JLabel("Phase computation"));
            pane.add(phaseSelector);
            pane.add(cutLabel);
            pane.add(cutValue);
            pane.add(checkButton);
            pane.add(bitsUsed);


            JPanel base = new JPanel();
            base.setPreferredSize(new Dimension(200, 80));
            base.add(new JLabel("Draw mode"));
            base.add(modeSelector);
            modeSelector.addActionListener(el -> {
                pane.setVisible(modeSelector.getSelectedItem().toString().startsWith("Fourier"));
                frame.pack();
            });
            base.add(new JLabel("Threshold"));
            base.add(thresholdField);
            frame.getContentPane().add(BorderLayout.NORTH, base);
            frame.getContentPane().add(BorderLayout.CENTER, pane);
            frame.pack();
            frame.setLocation(200, 200);
            frame.setVisible(true);
        });

        JComboBox<DrawParams.Contour> contourBox = new JComboBox<>(DrawParams.Contour.values());
        contourBox.addActionListener(l -> params.setContour((DrawParams.Contour) contourBox.getSelectedItem()));
        contourBox.setSelectedItem(params.getContour());

        JComboBox<Integer> thicknessBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        thicknessBox.addActionListener(l -> params.setThickness((int) thicknessBox.getSelectedItem()));
        JComboBox<DrawParams.SymMode> symmetryIndex = new JComboBox<>(DrawParams.SymMode.values());
        symmetryIndex.addActionListener(l -> params.setSymmetry((DrawParams.SymMode) symmetryIndex.getSelectedItem()));
        symmetryIndex.setSelectedItem(params.getSymmetry());

        JCheckBox colorBlindBox = new JCheckBox("ColBlind");
        colorBlindBox.addActionListener(l -> {
            params.colorBlind = colorBlindBox.isSelected();
            updateHashes(canvas, inputL, inputR, 1, hamDistDisplay, false);
        });

        JCheckBox pal32 = new JCheckBox("Pal32");
        pal32.addActionListener(l -> {
            params.palette32 = pal32.isSelected();
            updateHashes(canvas, inputL, inputR, 1, hamDistDisplay, false);
        });

        topRowL.add(newButtonL, BorderLayout.WEST);
        topRowL.add(inputL, BorderLayout.CENTER);
        topRowL.add(valButtonL, BorderLayout.EAST);
        topRowL.add(checkBoxClassicRGB, BorderLayout.SOUTH);
        topRowL.add(colorBlindBox, BorderLayout.SOUTH);
        topRowL.add(checkBoxSymmetry, BorderLayout.SOUTH);
        botRowL.add(paramsButton);
        //botRowL.add(checkBoxModPhase);
        botRowL.add(pal32);
        botRowL.add(checkBoxFiltered);
        botRowL.add(modeSelector);
        JComboBox<Integer> comboBoxNFunc = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        comboBoxNFunc.addActionListener(l -> {
            Integer n = (Integer) comboBoxNFunc.getSelectedItem();
            if (n != null) {
                params.setNFunc(n);
            }
        });
        botRowL.add(comboBoxNFunc);

        southL.add(topRowL, BorderLayout.NORTH);
        southL.add(distButtonOnce, BorderLayout.CENTER);
        southL.add(psiDisplay, BorderLayout.CENTER);
        //southL.add(compButton, BorderLayout.CENTER);
        //southL.add(plotButton, BorderLayout.CENTER);
        southL.add(saveButtonL, BorderLayout.CENTER);
        southL.add(contourBox, BorderLayout.CENTER);
        southL.add(thicknessBox, BorderLayout.CENTER);
        southL.add(symmetryIndex, BorderLayout.CENTER);
        southL.add(botRowL, BorderLayout.SOUTH);
        southL.setPreferredSize(new Dimension(WINDOW_W, BANNER_H));
        topRowL.setPreferredSize(new Dimension(southL.getPreferredSize().width, southL.getPreferredSize().height / 2));
        botRowL.setPreferredSize(topRowL.getPreferredSize());
        left.add(southL, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);

        // -----------RIGHT HALF------------------------


        canvas = new HashDrawer();
        canvas.setPreferredSize(new Dimension(HASH_W, HASH_H));

        JPanel southR = new JPanel();
        JPanel topRowR = new JPanel();
        JPanel botRowR = new JPanel();

        JButton valButtonR = new JButton("See hash");
        valButtonR.addActionListener(l -> {
            params.sampleColors();
            canvas.drawHash(this.getGraphics(), inputR.getText(), 2, params);
        });
        JButton newButtonR = new JButton("New hash");
        newButtonR.addActionListener(l -> {
            inputR.setText(newHash(params.getMode().length() / 4));
            params.sampleColors();
        });

        JButton saveButtonR = new JButton("Save");
        saveButtonR.addActionListener(l -> save(inputR));

        hamDistDisplay.setBackground(southR.getBackground());
        JTextField flipIndex = new JTextField("67", 3);
        JTextField flipValue = new JTextField("1", 3);

        JButton flipButton = new JButton("Flip");
        flipButton.addActionListener(e -> {
            String in = inputL.getText();
            int nBitsToFlip = Integer.parseInt(flipValue.getText());
            String indexString = flipIndex.getText();
            int randIndex = (int) Math.floor(Math.random() * params.getMode().length());
            params.sampleColors();
            if (indexString.contains("r")) {
                inputR.setText(flipBits(in, IntStream.range(randIndex, randIndex + nBitsToFlip).toArray()));
                //Integer.parseInt(params.getMode().toString().substring(params.getMode().toString().length() - 3), 10) - 1
            } else if (indexString.contains("+")) {
                int index = Integer.parseInt(indexString, 10);
                inputR.setText(flipBits(in, index, index + 12));
            } else if (indexString.contains("c")) {
                JTextField[] indicesField = new JTextField[nBitsToFlip];
                JLabel[] prevValues = new JLabel[nBitsToFlip];
                JFrame frame = new JFrame("Test");
                JPanel pane = new JPanel();
                pane.setSize(150, 300);
                pane.setPreferredSize(new Dimension(150, 300));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                for (int i = 0; i < indicesField.length; i++) {
                    indicesField[i] = new JTextField(3);
                    indicesField[i].setSize(100, 30);
                    indicesField[i].setPreferredSize(new Dimension(100, 30));
                    indicesField[i].setLocation(0, 30 * i);
                    prevValues[i] = new JLabel("was ?");
                    pane.add(new JLabel("Index " + i));
                    pane.add(indicesField[i]);
                    pane.add(prevValues[i]);
                }
                JButton validateButton = new JButton("Validate");
                validateButton.addActionListener(l -> {
                    int[] indices = Arrays.stream(indicesField).mapToInt(field -> Integer.parseInt(field.getText())).toArray();
                    for (int i = 0; i < prevValues.length; i++) {
                        prevValues[i].setText(" was " + (hexToBin(inputL.getText()).charAt(Integer.parseInt(indicesField[i].getText())) - '0'));
                    }
                    inputR.setText(flipBits(inputL.getText(), indices));
                });
                pane.add(validateButton);
                frame.getContentPane().add(pane);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setVisible(true);
                frame.setResizable(true);

            } else {
                int index = Integer.parseInt(indexString, 10);
                inputR.setText(flipBits(in, IntStream.range(index, index + nBitsToFlip).toArray()));
            }

        });

        JButton shiftButton = new JButton("Shift");
        shiftButton.addActionListener(e -> {
            inputR.setText(shiftRBits(inputL.getText()));

            params.sampleColors();
            canvas.drawHash(getGraphics(), inputR.getText(), 2, params);
        });

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            inputR.setText(inputL.getText());

            params.sampleColors();
            canvas.drawHash(getGraphics(), inputR.getText(), 2, params);
        });

        JButton demoButton = new JButton("Demo");
        demoButton.addActionListener(l -> {
            JFrame frame = new JFrame("Test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(1200, 50));
            HashDrawer canvasDemo = new HashDrawer();
            canvasDemo.setPreferredSize(new Dimension(1200, 600));
            JLabel[] jlabs = new JLabel[32];
            for (int i = 0; i < jlabs.length; i++) {
                jlabs[i] = new JLabel();
                jlabs[i].setPreferredSize(new Dimension(120, 20));
                jlabs[i].setSize(new Dimension(120, 20));
                jlabs[i].setLocation(getShiftX(i + 3) + 15, getShiftY(i + 3) + 100);
                jlabs[i].setOpaque(false);
                frame.getContentPane().add(jlabs[i], BorderLayout.CENTER);
            }

            JButton worstCasesButton = new JButton("Worst cases");
            worstCasesButton.setPreferredSize(new Dimension(300, 20));
            JButton symButton = new JButton("Symmetries");
            symButton.setPreferredSize(new Dimension(300, 20));
            JButton permButton = new JButton("Permutations");
            permButton.setPreferredSize(new Dimension(300, 20));

            worstCasesButton.addActionListener(e -> {
                // canv.setLocation(200, 200);
                String input = inputL.getText();
                String inputBin = hexToBin(input);
                int count = 3;
                if (params.palette32) {

                    jlabs[count - 3].setText("original");
                    canvasDemo.drawHash(frame.getGraphics(),
                            input, count++, params);
                    int[][][] allAttacks = {AttackIndices.sameParity4bits, AttackIndices.sameParity6bits, AttackIndices.sameParity8bits, AttackIndices.sameParity10bits};
                    for (int[][] attacks : allAttacks) {
                        for (int[] indices : attacks) {
                            boolean toDraw = true;
                            for (int index : indices) {
                                toDraw &= ((index >= 0 && inputBin.charAt(index) == '0') || (index < 0 && inputBin.charAt(-index) == '1'));
                            }
                            if (toDraw && count < 35) {

                                int[] indicesAbs = Arrays.stream(indices).map(Math::abs).toArray();
                                jlabs[count - 3].setText(Arrays.toString(indicesAbs));

                                String flipped = flipBits(input, indicesAbs);

                                canvasDemo.drawHash(frame.getGraphics(),
                                        flipped, count++, params);
                            }
                        }
                        count++;
                    }
                } else {
                    for (int i = 1; i < 4; i++) {
                        int gap = 33 * i;
                        if (count >= 35) break;
                        for (int j = 0; j < inputBin.length() - gap; j++) {
                            if (count >= 35) break;
                            jlabs[count - 3].setText(j + ", " + (gap + j));

                            if (inputBin.charAt(j) != inputBin.charAt(j + gap)) {
                                canvasDemo.drawHash(frame.getGraphics(),
                                        flipBits(input, j, j + gap), count++, params);
                            }
                        }
                    }
                }
            });

            symButton.addActionListener(e -> {
                DrawParams.SymMode symPush = params.getSymmetry();
                if (params.getMode() == DrawMode.FourierCartesian128) {
                    String input = inputL.getText();
                    int count = 3;

                    while (count - 3 < DrawParams.SymMode.FROM_HASH.ordinal()) {
                        params.setSymmetry(DrawParams.SymMode.values()[count - 3]);
                        canvasDemo.drawHash(frame.getGraphics(),
                                input, count++, params);
                    }
                    params.setSymmetry(symPush);
                }
            });

            permButton.addActionListener(e -> {
                int permPush = params.permut;
                if (params.getMode() == DrawMode.FourierCartesian128) {
                    String input = inputL.getText();
                    int count = 3;

                    while (count - 3 < 32) {
                        params.permut = count - 3;
                        canvasDemo.drawHash(frame.getGraphics(),
                                input, count++, params);
                    }
                }
                params.permut = permPush;
            });

            // frame.add(panel);
            // panel.add(canv);
            // panel.add(worstCasesButton);

            // frame.getContentPane().add(BorderLayout.CENTER, panel);

            frame.getContentPane().add(BorderLayout.CENTER, canvasDemo);
            panel.add(BorderLayout.CENTER, worstCasesButton);
            panel.add(BorderLayout.SOUTH, symButton);
            panel.add(BorderLayout.SOUTH, permButton);
            frame.getContentPane().add(BorderLayout.SOUTH, panel);
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

    private void getHashesDataPairs() {
        int nHash = params.getMode().length() * 10;
        int[][][] images = new int[nHash][2][params.getMode().length() * params.getMode().length()];

        int nThreads = 4;
        SimpleHashRunnable[] tasks = new SimpleHashRunnable[nThreads];//[Distance.values().length];
        Thread[] threads = new Thread[nThreads];
        String[] hashes = new String[nHash];
        //Result[][] results = new Result[tasks.length][15];


        for (int hashItr = 0; hashItr < nHash; hashItr++) {
            hashes[hashItr] = newHash(params.getMode().length() / 4);
        }
        System.out.println("Threads running...");
        long start = System.currentTimeMillis();
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new SimpleHashRunnable(canvas, hashes, params, images, i * nHash / nThreads, nHash / nThreads);
            //tasks[i] = new CompressPairRunnable(results[i], Distance.values()[i], newRefHash, canvas, params.getMode(), hashItr);
            threads[i] = new Thread(tasks[i], "Thread " + tasks[i].hashCode());
            threads[i].start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
        System.out.println("...done (Time elapsed : " + (System.currentTimeMillis() - start) / 1000.0 + "s)");

        try {
            File csvOutputFile = new File("src/data/" + params + "_hashesPairDistVar.csv");
            PrintWriter pw = new PrintWriter(csvOutputFile);
            IntStream.range(0, nHash).mapToObj(i -> hashes[i] + "," + Arrays.deepToString(images[i])).forEach(pw::println);
            pw.close();
            //File csvOutputFile = new File("src/data/paramsSearch" + params.getMode() + ".json");
//            PrintWriter pw = new PrintWriter(csvOutputFile);
//            pw.print("{ \n \t \"values\": \n");
//            Stream.of().map(Arrays::deepToString).forEach(pw::print);
//            pw.print("\n}");
//            pw.close();
        } catch (
                Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        System.out.println("Data ready.");
    }

    private void getHashesDataDists() {

        int nHash = 10;
        double[][] averages = new double[nHash][params.getMode().length()];

        //int[] ref;

        Path path = Paths.get("/Users/laya/Documents/VisualHashApplet/src/temp/");

        // reset temp folder
        try (Stream<Path> walk = Files.walk(path)) {
            walk
                    .sorted(Comparator.reverseOrder())
                    .forEach(l -> {
                        try {
                            Files.delete(l);
                        } catch (Exception e) {
                            System.out.println(e.getLocalizedMessage());
                        }
                    });
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        if (!new File("/Users/laya/Documents/VisualHashApplet/src/temp").mkdirs())
            System.out.println("couldnt makedir");
        AvgDistRunnable[] tasks = new AvgDistRunnable[4];//[Distance.values().length];
        Thread[] threads = new Thread[tasks.length];
        BufferedImage res = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);
        String newRefHash;
        //Result[][] results = new Result[tasks.length][15];
        try {
            File csvOutputFile = new File("src/data/" + params + "_changing1bit.csv");
            PrintWriter pw = new PrintWriter(csvOutputFile);

            for (int hashItr = 0; hashItr < nHash; hashItr++) {
                System.out.println("Iteration " + hashItr + " started....");
                newRefHash = newHash(params.getMode().length() / 4);
                pw.print(newRefHash + ", ");
                int[] ref = canvas.drawFourierHash(res.createGraphics(), newRefHash, 0, params);

                for (int i = 0; i < tasks.length; i++) {
                    tasks[i] = new AvgDistRunnable(averages[hashItr], i * 120 / tasks.length, 120 / tasks.length, newRefHash, ref, canvas, params);
                    tasks[i] = new AvgDistRunnable(averages[hashItr], i * 120 / tasks.length, 120 / tasks.length, newRefHash, ref, canvas, params);
                    //tasks[i] = new CompressPairRunnable(results[i], Distance.values()[i], newRefHash, canvas, params.getMode(), hashItr);
                    threads[i] = new Thread(tasks[i], "Thread " + tasks[i].hashCode());
                    threads[i].start();
                }
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (Exception e) {
                        System.out.println(e.getLocalizedMessage());
                    }
                }
                System.out.println("...done.");
            }

            Stream.of(averages).map(Arrays::toString).forEach(pw::println);
            pw.close();
            //File csvOutputFile = new File("src/data/paramsSearch" + params.getMode() + ".json");
//            PrintWriter pw = new PrintWriter(csvOutputFile);
//            pw.print("{ \n \t \"values\": \n");
//            Stream.of().map(Arrays::deepToString).forEach(pw::print);
//            pw.print("\n}");
//            pw.close();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        System.out.println("Data ready.");
    }

    private void updateHashes(HashDrawer canvas, JTextField inputChanged, JTextField inputOther, int shift, JTextArea display, boolean isChangedRight) {
        // int[] colors = new int[PALETTES[Math.min(mode.getName().ordinal(), 3)].length];
        // for (int i = 0; i < colors.length; i++) {
        // colors[i] = i;
        // }
        if (inputChanged.getText().length() != 0 && inputChanged.getText().length() == params.getMode().length() / 4) {
            params.setPrng(new PRNG128BitsInsecure(inputChanged.getText()));
            params.sampleColors();
            canvas.drawHash(getGraphics(), inputChanged.getText(), shift, params);
            String leftText = (isChangedRight ? inputOther : inputChanged).getText();
            String rightText = (isChangedRight ? inputChanged : inputOther).getText();

            display.setText("Hamming dist = " + hamDistHex(inputChanged.getText(), inputOther.getText())
//            + ", parity changed = " + Arrays.toString(parityDist(inputChanged.getText(), inputOther.getText(), params))
//            + " mods = " + getPaletteShift((isChangedRight ? inputOther : inputChanged).getText()) + ", "
//            + getPaletteShift((isChangedRight ? inputChanged : inputOther).getText())
                    + "/ pal =" + (!params.palette32 ? (weight(leftText) + ", " + weight(rightText)) : (getPaletteShift(leftText) + ", " +
                    getPaletteShift(rightText)))
                    + "/ perm =" + getPerm(leftText) + ", " +
                    getPerm(rightText)
                    + "/ syms = " + getSymmetry(leftText) + ", "
                    + getSymmetry(rightText));
        }
    }

    /**
     * Saves the visual hash corresponding to given input in a file.
     *
     * @param input the text field from which the visual hash is generated
     */
    public void save(JTextField input) {
        BufferedImage bImg = new BufferedImage(HASH_W, HASH_H, BufferedImage.TYPE_INT_RGB);

        Graphics2D cg = bImg.createGraphics();

        params.sampleColors();
        canvas.drawHash(cg, input.getText(), 0, params);

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

}
