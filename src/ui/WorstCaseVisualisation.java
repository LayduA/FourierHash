package src.ui;

import src.hashops.DataGeneration;
import src.hashops.HashDrawer;
import src.types.DrawMode;
import src.types.DrawParams;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Comparator;

public class WorstCaseVisualisation extends JFrame {
    public final static int WINDOW_W = 1100;
    public final static int WINDOW_H = 650;
    public final static int BANNER_H = 160;
    public final static int HASH_W = 256;
    public final static int HASH_H = 256;
    public final static int HASH_X_0 = (WINDOW_W / 2 - HASH_W) / 2;
    public final static int HASH_X_1 = (HASH_X_0 * 3) / 2 + WINDOW_W / 2;
    public final static int HASH_Y = (WINDOW_H - HASH_H - BANNER_H) / 2;

    public DrawParams params;
    private HashDrawer canvas;
    private int currHash;
    private DataGeneration.DataElem[] elems;

    public WorstCaseVisualisation() {
        initUI();
    }

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            WorstCaseVisualisation app = new WorstCaseVisualisation();
            app.setVisible(true);
        });


    }

    private void initUI() {
        elems = new DataGeneration.DataElem[100];
        try {
            String file = "src/data/collisions100.csv";

            BufferedReader reader = new BufferedReader(new FileReader(file));
            Object[] lines = reader.lines().toArray();
            for (int i = 1; i < lines.length; i++) {
                elems[i - 1] = DataGeneration.DataElem.fromString((String) lines[i]);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Mais quelle erreur !" + e.getMessage());
        }

        java.util.List<DataGeneration.DataElem> elemsList = Arrays.asList(elems);
        elemsList.sort(Comparator.comparingDouble(DataGeneration.DataElem::getDist));
        elemsList.toArray(elems);

        params = new DrawParams(DrawMode.FourierCartesian128);
        setLayout(new BorderLayout());
        params.palette32 = true;
        params.colorBlind = false;

        // --------------LEFT HALF--------------

        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));
        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(WINDOW_W / 2, WINDOW_H));


        canvas = new HashDrawer();
        canvas.setPreferredSize(new Dimension(HASH_W, HASH_H));
        JLabel text = new JLabel();

        JButton nextButtonL = new JButton("Next hash");
        nextButtonL.addActionListener(l -> {
            currHash = (currHash + 1) % elems.length;
            drawHashes(text);
        });

        JButton prevButtonL = new JButton("Prev hash");
        prevButtonL.addActionListener(l -> {
            currHash = (currHash - 1) + (currHash == 0 ? elems.length : 0);
            drawHashes(text);
        });

        JPanel pane = new JPanel();
        pane.setPreferredSize(new Dimension(50, 50));
        pane.setSize(new Dimension(50, 50));
        //pane.setBackground(Color.RED);
        pane.setLayout(new GridBagLayout());
        pane.add(text);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel botRow = new JPanel();
        botRow.add(prevButtonL, BorderLayout.WEST);
        botRow.add(nextButtonL, BorderLayout.EAST);

        add(botRow, BorderLayout.SOUTH);
        //add(text, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);


        // -----------RIGHT HALF------------------------


        // ----------------------------------------
        pack();
        setTitle("Worst cases");
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public void drawHashes(JLabel text) {
        canvas.drawHash(this.getGraphics(), elems[currHash].getOriginalHash(), 1, params);
        params.dontScale = true;
        canvas.drawHash(this.getGraphics(), elems[currHash].getDist() == 0.0 ? elems[currHash].getOriginalHash() : elems[currHash].getFlippedHash(), 2, params);
        params.dontScale = false;
        text.setText("<html>" + elems[currHash].getOriginalHash() + "<br/>" + elems[currHash].getFlippedHash() + "<br/>" + elems[currHash].getDist() + "</html>");
    }

}
