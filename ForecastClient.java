import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An entry point for client application
 */
public class ForecastClient {

    private static int DEFAULT_CITY_ID = 6167865;

    private static ForecastService findForecastService(final String host, final int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            return (ForecastService) registry.lookup("ForecastService");
        } catch (NotBoundException | RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        final String host = args.length < 2 ? "localhost" : args[0];
        final int port = args.length < 2 ? 1099 : Integer.parseInt(args[1]);

        UIManager.getCrossPlatformLookAndFeelClassName();

        final ForecastService forecastService = findForecastService(host, port);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final MainFrame frame = new MainFrame(forecastService);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setStatusText("Connected to " + host + ":" + port);
                frame.setVisible(true);
            }
        });
    }

    /**
     * Main panel
     */
    static class MainFrame extends JFrame {
        private final ForecastPanel forecastPanel = new ForecastPanel();
        private final ForecastService forecastService;
        private JLabel statusText = new JLabel();

        MainFrame(ForecastService forecastService) throws HeadlessException {
            this.forecastService = forecastService;

            setSize(492, 300);
            setLocationRelativeTo(null);

            setLayout(new BorderLayout());

            try {
                List<Date> dates = forecastService.queryForecast(DEFAULT_CITY_ID);
                final DaySelectionPanel daySelection = new DaySelectionPanel(dates);
                daySelection.onSelect(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            forecastPanel.update(MainFrame.this.forecastService.makeForecast(daySelection.getCity(), daySelection.getDate()));
                            AutoUpdater updater = new AutoUpdater(daySelection);
                            new Thread(updater).start();
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                add(daySelection, BorderLayout.NORTH);
                add(forecastPanel, BorderLayout.CENTER);

                final JPanel status = new JPanel();
                status.add(statusText);
                add(status, BorderLayout.SOUTH);
            } catch (RemoteException re) {
                re.printStackTrace();
            }

        }

        void setStatusText(String text) {
            this.statusText.setText(text);
        }
    }

    /**
     * Panel with rendered forecast data in table
     */
    static class ForecastPanel extends JPanel {

        private final JTable table = new JTable(new DefaultTableModel(0, 2));
        private final JButton saveButton = new JButton("Save");

        private final List<String> quotes = Arrays.asList(
                "\"You Learn More From Failure Than From Success. Donâ€™t Let It Stop You. Failure Builds Character.\" - Unknown",
                "\"Its Not Whether You Get Knocked Down, It's Whether You Get Up.\" - “ Vince Lombardi",
                "\"People Who Are Crazy Enough To Think They Can Change The World, Are The Ones Who Do.\" - Rob Siltanen",
                "\"Failure Will Never Overtake Me If My Determination To Succeed Is Strong Enough.\" - Og Mandino",
                "\"The Only Limit To Our Realization Of Tomorrow Will Be Our Doubts Of Today.\" - Franklin D. Roosevelt"
        );
        private final TitledBorder title = BorderFactory.createTitledBorder("Forecast");

        ForecastPanel() {
            setLayout(new BorderLayout());

            setBorder(
                    BorderFactory.createCompoundBorder(
                            title,
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)));


            table.setTableHeader(null);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel southPanel = new JPanel();
            southPanel.setLayout(new FlowLayout());
            add(southPanel, BorderLayout.SOUTH);
            southPanel.add(saveButton);
            JButton motivateMe = new JButton("Motivate me");
            motivateMe.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SecureRandom random = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());
                    int anInt = random.nextInt(quotes.size());
                    JOptionPane.showMessageDialog(ForecastPanel.this, quotes.get(anInt));
                }
            });
            southPanel.add(motivateMe);

            saveButton.setEnabled(false);

            // saving forecast to a plain text file
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final JFileChooser fileChooser = new JFileChooser();
                    if (fileChooser.showSaveDialog(ForecastPanel.this) == JFileChooser.APPROVE_OPTION) {
                        try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                            for (int row = 0; row < table.getRowCount(); row++) {
                                writer.print(table.getValueAt(row, 0));
                                writer.print(": ");
                                writer.println(table.getValueAt(row, 1));
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }


        private Vector row(Object... cols) {
            final Vector vector = new Vector();
            Collections.addAll(vector, cols);
            return vector;
        }

        void update(final Forecast forecast) {
            final DefaultTableModel tm = new DefaultTableModel(0, 2);
            tm.addRow(row("Temp", forecast.getTemperature()));
            tm.addRow(row("WindSpeed", forecast.getWindSpeed()));
            tm.addRow(row("Humidity", forecast.getHumidity()));
            tm.addRow(row("Pressure", forecast.getPressure()));
            tm.addRow(row("Clouds", forecast.getClouds()));
            title.setTitle(forecast.getLocation());
            table.setModel(tm);
            saveButton.setEnabled(true);
            repaint();
        }
    }

    /**
     * Topbar panel
     */
    static class DaySelectionPanel extends JPanel {
        private final JButton makeForecastButton = new JButton("Forecast");
        private final JComboBox<Date> datesComboBox = new JComboBox<>();
        private final JComboBox<Location> locationComboBox = new JComboBox<>();
        private final AtomicBoolean updateEnabled = new AtomicBoolean(false);

        DaySelectionPanel(List<Date> dates) {
            setLayout(new FlowLayout());

            setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder("Select a day"),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)));


            locationComboBox.addItem(new Location(6167865, "Toronto"));
            locationComboBox.addItem(new Location(6075357, "Mississauga"));
            locationComboBox.addItem(new Location(6094578, "Oshawa"));
            add(locationComboBox);

            for (Date date : dates) {
                datesComboBox.addItem(date);
            }
            add(datesComboBox);


            datesComboBox.setRenderer(new DefaultListCellRenderer() {
                private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof Date) {
                        return super.getListCellRendererComponent(list, df.format(value), index, isSelected, cellHasFocus);
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            add(makeForecastButton);
            final JToggleButton auto = new JToggleButton("A");
            auto.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateEnabled.set(!updateEnabled.get());
                }
            });
            add(auto);
        }


        void onSelect(ActionListener listener) {
            makeForecastButton.addActionListener(listener);
        }

        void updateForecast() {
            makeForecastButton.doClick();
        }

        Date getDate() {
            return (Date) datesComboBox.getSelectedItem();
        }

        int getCity() {
            return ((Location) locationComboBox.getSelectedItem()).getId();
        }

        static final class Location {
            private final int id;
            private final String name;

            public Location(int id, String name) {
                this.id = id;
                this.name = name;
            }

            public int getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            @Override
            public String toString() {
                return this.name;
            }
        }
    }

    static class AutoUpdater implements Runnable {

        private final DaySelectionPanel selectionPanel;

        AutoUpdater(DaySelectionPanel selectionPanel) {
            this.selectionPanel = selectionPanel;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000);
                    if (selectionPanel.updateEnabled.get())
                        selectionPanel.updateForecast();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }
    }
}
