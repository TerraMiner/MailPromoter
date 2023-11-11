package ua.terra;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.Image.SCALE_DEFAULT;
import static ua.terra.Main.sessionPassword;
import static ua.terra.Main.sessionUser;

public class Window extends JFrame {
    private JTextField hostField, portField, userField, baseField, passwordField;
    private Logger logger;
    private JComboBox<String> fileComboBox;
    private JScrollPane scrollPane;
    private JButton connectButton;
    private JButton startSendButton;
    private JButton stopSendButton;
    private JButton updateFilesButton;
    private JProgressBar progressBar;
    private Image image;
    private DatabaseConnection connection;
    private EmailSender sender;
    private final String resourceFolder = "res";
    private final String promotesFolder = resourceFolder + "/promotes";
    private final String fileExtension = "html";

    private boolean isStarted = false;
    private boolean isWaitStop = false;

    public Window() {
        setTitle("Mail Promoter");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initButtons();
        setupFields();
        loadAppIcon();
        setResizable(false);
        setIconImage(image);
        setVisible(true);
    }

    private void initButtons() {
        hostField = new JTextField(10);
        portField = new JTextField(10);
        userField = new JTextField(10);
        baseField = new JTextField(10);
        passwordField = new JTextField(10);

        hostField.setText("localhost");
        portField.setText("3306");
        userField.setText("root");
        baseField.setText("test");
        passwordField.setText("");

        logger = new Logger();
        scrollPane = new JScrollPane(logger);
        fileComboBox = new JComboBox<>();
        connectButton = new JButton("Connect");
        startSendButton = new JButton("Start sending");
        stopSendButton = new JButton("Stop sending");
        updateFilesButton = new JButton("Update");
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        updateFileComboBox();
        connectButton.addActionListener(e -> connectToDatabase());
        startSendButton.addActionListener(e -> startSending());
        stopSendButton.addActionListener(e -> stopSending());
        updateFilesButton.addActionListener(e -> updateFileComboBox());
    }

    private void setupFields() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Host:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("User:"));
        panel.add(userField);
        panel.add(new JLabel("Base:"));
        panel.add(baseField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        panel.add(connectButton);

        JPanel comboBoxPanel = new JPanel(new GridLayout(1, 2));

        panel.add(comboBoxPanel);
        comboBoxPanel.add(new JLabel("Select File:"));
        comboBoxPanel.add(fileComboBox);
        comboBoxPanel.add(updateFilesButton);

        panel.add(progressBar);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

        panel.add(buttonPanel);
        buttonPanel.add(startSendButton);
        buttonPanel.add(stopSendButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }


    private void connectToDatabase() {
        String host = hostField.getText();
        String port = portField.getText();
        String user = userField.getText();
        String base = baseField.getText();
        String password = passwordField.getText();

        logger.print("Connecting to database...\n");

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.printStackTrace(e);
                return;
            }
        }

        try {
            connection = new DatabaseConnection(host, port, user, password, base);
        } catch (SQLException e) {
            logger.printStackTrace(e);
            return;
        }

        createSenderSession();

        logger.print("Host: " + host + "\n");
        logger.print("Port: " + port + "\n");
        logger.print("User: " + user + "\n");
        logger.print("Base: " + base + "\n");
        logger.print("Password: " + password + "\n");
        logger.print("Connection successful!\n");
    }

    private void createSenderSession() {
        if (sessionUser == null || sessionPassword == null) {
            logger.print("It is necessary to pass the “login” and “password” properties through the -D parameters of the JVM.");
            logger.print("Example: java -Dlogin=myLogin -Dpassword=myPassword MyApp");
            throw new IllegalArgumentException("login or password is null!");
        }
        sender = new EmailSender(sessionUser, sessionPassword);
        logger.print("Created Sender Session for" + sessionUser);
    }

    private void updateFileComboBox() {
        fileComboBox.removeAllItems();
        File folder = new File(promotesFolder);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
                if (!extension.equals(fileExtension)) continue;
                String fileName = file.getName()
                        .replace(".","")
                        .replace(fileExtension, "");
                fileComboBox.addItem(fileName);
            }
        }
    }

    private void stopSending() {
        if (isConnected() || !isStarted) return;
        isWaitStop = true;
    }

    private void tryCancel(List<Thread> threads, Timer timer, AtomicInteger progress) {
        if (isWaitStop) {
            progress.set(0);
            threads.forEach(Thread::interrupt);
            timer.cancel();
            isWaitStop = false;
            isStarted = false;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.print("Process was finished!");
                }
            }, 250);
        }
    }

    private void startSending() {
        if (isConnected() || isStarted) return;

        File resource = getSelectedFile();

        if (!resource.exists()) {
            logger.print("File not exists!");
            return;
        }

        List<List<String>> list;

        try {
            list = Util.divideList(connection.getMails(), 32);
        } catch (SQLException e) {
            logger.printStackTrace(e);
            return;
        }

        List<Thread> threads = new ArrayList<>();

        AtomicInteger progress = new AtomicInteger(0);

        list.forEach(chunk ->
                chunk.forEach(mail ->
                        threads.add(new Thread(() -> {
                            try {
                                sender.send(mail, "Promotion", resource);
                            } catch (Exception e) {
                                logger.printStackTrace(e);
                            }
                            progress.incrementAndGet();
                        }
                        ))));


        threads.forEach(Thread::start);

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                tryCancel(threads,timer, progress);

                double percent = (((double) progress.get() / (double) list.size()) * 100.0);
                progressBar.setValue((int) percent);

                if (percent >= 100) {
                    isWaitStop = true;
                    logger.print("Sended:" + progress.get());
                }
            }
        }, 0, 50);
    }

    private boolean isConnected() {
        boolean statement = hasConnection();
        if (!statement) {
            logger.print("Not connected!");
        }
        return !statement;
    }


    private boolean hasConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            logger.printStackTrace(e);
            return false;
        }
    }

    private File getSelectedFile() {
        String selected = (String) fileComboBox.getSelectedItem();
        return new File(promotesFolder + "/" + selected);
    }

    private void loadAppIcon() {
        try {
            image = ImageIO.read(new File("res/icon.png")).getScaledInstance(32, 32, SCALE_DEFAULT);
        } catch (IOException e) {
            logger.printStackTrace(e);
        }
    }

    public static void open() {
        SwingUtilities.invokeLater(Window::new);
    }
}
