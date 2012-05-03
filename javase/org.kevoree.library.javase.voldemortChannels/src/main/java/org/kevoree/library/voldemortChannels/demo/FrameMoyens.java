package org.kevoree.library.voldemortChannels.demo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 26/04/12
 * Time: 16:00
 */
public class FrameMoyens extends JFrame {
    private static final int FRAME_WIDTH = 300;
    private static final int FRAME_HEIGHT = 600;
    private JTable table;
    String rowData[][] = { { "BLS", "RENNES", "LIBRE" },
            { "FPT", "RENNES", "LIBRE" } };

    Object columnNames[] = { "Type de Moyen", "Caserne", "Etat" };

    public FrameMoyens(String nodeName) {

        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setLayout(new BorderLayout());
        setTitle(nodeName);
        setVisible(true);

        table = new JTable(rowData, columnNames);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        pack();
        setVisible(true);
    }


    public void update(String rowData[][]){
        this.rowData = rowData;
        DefaultTableModel model = new DefaultTableModel( rowData, columnNames);
        table.setModel(model);
    }


}
