package net.ncguy.subnet.display;

import net.ncguy.subnet.Launcher;
import net.ncguy.subnet.data.IPAddress;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Created by Guy on 23/03/2016.
 */
public class SubnetFrame {
    private JTable subnetTable;
    private JFormattedTextField subnetMaskTextField;
    private JFormattedTextField sharedBitsTextField;
    private JButton btnMask;
    private JButton btnBits;
    private JTextField addrNetField;
    private JTextField addrFirstField;
    private JTextField addrLastField;
    private JTextField addrBroadField;
    private JButton calculateButton;
    private JTextField ipAddrField;
    private JTabbedPane parentTab;
    private JTextField addrSubField;
    private JTextField addrHostField;
    private JTextField addrNetBitsField;
    private JTextField addrHostBitsField;
    private JTextField addrNetField_bin;
    private JTextField addrFirstField_bin;
    private JTextField addrLastField_bin;
    private JTextField addrBroadField_bin;
    private JTextField addrSubField_bin;

    private static JFrame frame;
    private static Dimension size;

    public static JFrame getFrame(String... args) {
        if(frame == null) {
            frame = new JFrame("SubnetFrame");
            frame.setContentPane(new SubnetFrame(args).parentTab);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            size = frame.getSize();

            JMenuBar menuBar = new JMenuBar();
            JMenu filMenu = new JMenu("File");
            filMenu.add(new JMenuItem(new AbstractAction("Exit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                    frame.dispose();
                    frame = null;
                    Launcher.kill();
                }
            }));
            JMenu optMenu = new JMenu("Options");
            optMenu.add(new JMenuItem(new AbstractAction("Pack") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.pack();
                }
            }));
            optMenu.add(new JMenuItem(new AbstractAction("Refresh") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                    Launcher.postRunnable(() -> Launcher.openFrame(args));
                    frame = null;
                }
            }));
            JMenu lafMenu = new JMenu("Look and feel");
            for(UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
                JMenuItem item = new JMenuItem(new LaFAction(lafInfo.getName(), lafInfo));
                lafMenu.add(item);
            }
            menuBar.add(filMenu);
            menuBar.add(optMenu);
            menuBar.add(lafMenu);
            frame.setJMenuBar(menuBar);
        }
        return frame;
    }

    private SubnetFrame(String... args) {
        addColumns("Network", "First host", "Last host", "Broadcast");
        btnMask.addActionListener(e -> {
            String text = subnetMaskTextField.getText();
            ValidationError valid = validateSubnetMask(text);
            if(!valid.pass) {
                showDialog("Subnet mask is not valid, "+valid.errorText());
                return;
            }
            calculateDataFromMask(text);
        });
        btnBits.addActionListener(e -> {
            String text = sharedBitsTextField.getText();
            text = text.replaceAll("[^0-9]", "");
            int nBits = Integer.parseInt(text);
            if(nBits >= 1 && nBits <= 30) {
                nBits = Math.max(0, Math.min(nBits, 32));
                sharedBitsTextField.setText("/" + nBits);
                calculateMaskFromBits("/" + nBits);
            }
        });
        calculateButton.addActionListener(e -> {
            String text = ipAddrField.getText();
            ValidationError err = validateSubnetMask(text);
            if(!err.pass) {
                showDialog("Invalid address entered. "+err.errorText);
                return;
            }
//            if(!text.substring(text.indexOf("/")).matches("[0-9]")) return;
            calculateDataFromFullAddr(text);
        });
        ipAddrField.addActionListener(e -> {
            String text = ipAddrField.getText();
            ValidationError err = validateSubnetMask(text);
            if(!err.pass) {
                showDialog("Invalid address entered. "+err.errorText);
                return;
            }
            calculateDataFromFullAddr(text);
        });
    }

    private void calculateMaskFromBits(String bits) {
        bits = bits.replaceAll("[^0-9]", "");
        int bBits = Integer.parseInt(bits);
        String[] binBits = new String[4];
        for(int i = 0; i < binBits.length; i++) {
            binBits[i] = "";
        }
        int max = 32;
        for(int i = 0, j = 0; i < max; i++) {
            if(i > 0 && i % 8 == 0) j++;
            if(i < bBits) binBits[j] += "1";
            else binBits[j] += "0";
        }
        int[] bytes = new int[binBits.length];
        for (int i = 0; i < binBits.length; i++) {
            bytes[i] = Integer.parseInt(binBits[i], 2);
        }
        calculateDataFromAddr(new IPAddress(bytes));
    }

    private void calculateDataFromMask(String mask) {
        calculateDataFromAddr(new IPAddress(mask));
    }

    private void calculateDataFromAddr(IPAddress mask) {
        IPAddress flipped = mask.flip();
        int octetSplit = 3;
        String bits = mask.toBinaryOctetString();
        char[] bitsArr = bits.toCharArray();
        for (int i = 0, j = 0; i < bitsArr.length; i++) {
            if(i != 1 && i % 8 == 1) j++;
            if(i < bitsArr.length-1) {
                if((bitsArr[i]+"").equals("1") && (bitsArr[i+1]+"").equals("0")) {
                    octetSplit = j;
                    i = bitsArr.length;
                }
            }
        }

        if(tablePopulationThread != null) {
            if(tablePopulationThread.getState() != Thread.State.TERMINATED) {
                tablePopulationThread.interrupt();
            }
        }
        resetTableData();
        final int finalOctetSplit = octetSplit;
        tablePopulationThread = new Thread(() -> {
            IPAddress masterAddr = new IPAddress(0, 0, 0, 0);
            IPAddress parentAddr = masterAddr.copy();
            boolean valid = true;
            while(valid) {
                RowData row = new RowData();
                row.shared = mask.getPositiveBits();
                row.network = parentAddr.mask(mask);
                row.first = row.network.add(1);
                row.broadcast = row.network.add(flipped);
                row.last = row.broadcast.sub(1);
                row.mask = mask;
                row.hostCount = row.network.diff(row.broadcast);
                addRowToTable(row);
                parentAddr = parentAddr.add(flipped.add(1));
                if(parentAddr.getOctet(finalOctetSplit-1) >= 1) valid = false;
            }
            subnetTable.invalidate();
        });
        tablePopulationThread.start();
    }

    Thread tablePopulationThread;

    private void resetTableData() {
        DefaultTableModel model = (DefaultTableModel)subnetTable.getModel();
        model.setNumRows(0);
        model.setColumnCount(0);
        model.addColumn("Network");
        model.addColumn("First");
        model.addColumn("Last");
        model.addColumn("Broadcast");
    }
    private void addRowToTable(RowData row) {
        Object[] rowObj = new Object[] {
                row.network,
                row.first,
                row.last,
                row.broadcast
        };
        ((DefaultTableModel)subnetTable.getModel()).addRow(rowObj);
    }

    private void calculateDataFromFullAddr(String fullAddr) {
        String[] parts = fullAddr.split("/");
        IPAddress addr = new IPAddress(parts[0]);
        int shared = Integer.parseInt(parts[1]);
        IPAddress mask = getSubnetMask(shared);
        IPAddress flipped = mask.flip();
//        System.out.println("Flipped: "+flipped);
        RowData row = new RowData();
        row.shared = shared;
        row.network = addr.mask(mask);
        row.first = row.network.add(new IPAddress("0.0.0.1"));
        row.broadcast = row.network.add(flipped);
        row.last = row.broadcast.sub(new IPAddress("0.0.0.1"));
        row.mask = mask;
        row.hostCount = row.network.diff(row.broadcast);
        setAddrFields(row);
    }

    private void setAddrFields(RowData data) {
        addrNetField.setText(data.network.toString());
        addrFirstField.setText(data.first.toString());
        addrLastField.setText(data.last.toString());
        addrBroadField.setText(data.broadcast.toString());
        addrSubField.setText(data.mask.toString());
        addrHostField.setText(data.hostCount+"");
        addrNetBitsField.setText(data.shared+"");
        addrHostBitsField.setText((32-data.shared)+"");

        addrNetField_bin.setText(data.network.toBinaryOctetString());
        addrFirstField_bin.setText(data.first.toBinaryOctetString());
        addrLastField_bin.setText(data.last.toBinaryOctetString());
        addrBroadField_bin.setText(data.broadcast.toBinaryOctetString());
        addrSubField_bin.setText(data.mask.toBinaryOctetString());
    }

    private IPAddress getSubnetMask(int shared) {
        String addr = "";
        for(int i = 0; i < 32; i++) {
            if(addr.length() > 0) if(i % 8 == 0) addr += ".";
            if(i < shared) addr += "1";
            else addr += "0";
        }
        String[] parts = addr.split("\\.");
        addr = "";
        for (String part : parts) addr += Integer.parseInt(part, 2) + ".";
        addr = addr.substring(0, addr.length()-1);
        return new IPAddress(addr);
    }

    private IPAddress getSubnetSize(int shared) {
        IPAddress mask = getSubnetMask(shared);
        mask.mFlip();
        return mask;
    }

    private ValidationError validateSubnetMask(String mask) {
        if(countOccurrence(mask, '.') != 3) return ValidationError.OCTET;
        if(!IPAddress.isValid(mask)) return ValidationError.RANGE;
        if(!mask.matches("^\\d{1,3}(\\.\\d{1,3}){3}\\/\\d{1,2}$")) return ValidationError.MALFORMED;
        return ValidationError.NONE;
    }

    private int countOccurrence(String string, char target) {
        char[] strArr = string.toCharArray();
        int count = 0;
        for(char c : strArr) {
            if((c+"").equalsIgnoreCase(target+""))
                count++;
        }
        return count;
    }

    private void showDialog(String text) {
        JDialog d = new JDialog(SubnetFrame.getFrame());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(new JLabel(text), BorderLayout.LINE_START);
        JButton btn = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                d.setModal(false);
                d.setVisible(false);
            }
        });
        btn.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    btn.getAction().actionPerformed(null);
            }
        });
        p.add(btn, BorderLayout.SOUTH);
        d.add(p);
        d.pack();
        d.setModal(true);
        d.setVisible(true);
        d.requestFocus();
    }

    private void addColumn(String header) {
        TableColumn c = new TableColumn();
        c.setHeaderValue(header);
        subnetTable.addColumn(c);
    }
    private void addColumns(String... headers) {
        for(String s : headers)
            addColumn(s);
    }

    private enum ValidationError {
        NONE(true),
        OCTET(false, "Not enough octets"),
        RANGE(false, "Octet not in valid range"),
        MALFORMED(false, "IP must match the pattern: [x.x.x.x/x]"),
        ;

        ValidationError(boolean pass) { this(pass, "No error text"); }
        ValidationError(boolean pass, String errorText) {
            this.pass = pass;
            this.errorText = errorText;
        }
        private boolean pass;
        private String errorText;
        public boolean pass() { return pass; }
        public String errorText() { return errorText; }
    }

    private static class RowData {
        IPAddress network, first, last, broadcast, mask;
        long hostCount = 0L;
        int shared = 24;

        public RowData() {
            this.network = new IPAddress();
            this.first = new IPAddress();
            this.last = new IPAddress();
            this.broadcast = new IPAddress();
            this.mask = new IPAddress();
        }

        public RowData(IPAddress network, IPAddress first, IPAddress last, IPAddress broadcast, IPAddress mask) {
            this.network = network;
            this.first = first;
            this.last = last;
            this.broadcast = broadcast;
            this.mask = mask;
        }

        @Override
        public String toString() {
            return String.format("Row data: [%s] \n" +
                    "\tNetwork: %s\n" +
                    "\tFirst host: %s\n" +
                    "\tLast host: %s\n" +
                    "\tBroadcast: %s\n" +
                    "\tSubnet mask: %s",
                    hostCount, network, first, last, broadcast, mask);
        }
    }

    public static class LaFAction extends AbstractAction {

        UIManager.LookAndFeelInfo info;

        public LaFAction(String name, UIManager.LookAndFeelInfo info) {
            super(name);
            this.info = info;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try{
                UIManager.setLookAndFeel(info.getClassName());
                SwingUtilities.updateComponentTreeUI(frame);
            }catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }
}
