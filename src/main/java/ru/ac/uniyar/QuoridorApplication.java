package ru.ac.uniyar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class QuoridorApplication {

    public static void main(String[] args) {
        boolean desktopMode = Arrays.asList(args).contains("--quoridor.desktop=true");
        if (desktopMode) {
            startDesktopApplication(args);
            return;
        }

        SpringApplication.run(QuoridorApplication.class, args);
    }

    private static void startDesktopApplication(String[] args) {
        System.setProperty("java.awt.headless", "false");
        System.setProperty("spring.devtools.livereload.port", "35729");

        SpringApplication application = new SpringApplication(QuoridorApplication.class);
        application.setHeadless(false);

        ConfigurableApplicationContext context = application.run(prepareDesktopArguments(args));
        int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port", "8080"));
        String url = "http://127.0.0.1:" + port + "/start";

        SwingUtilities.invokeLater(() -> {
            showControlWindow(context, url);
            openBrowser(url);
        });
    }

    private static String[] prepareDesktopArguments(String[] args) {
        List<String> prepared = new ArrayList<>(Arrays.asList(args));
        prepared.remove("--quoridor.desktop=true");
        prepared.add("--server.port=0");
        prepared.add("--server.address=127.0.0.1");
        prepared.add("--vaadin.productionMode=true");
        prepared.add("--vaadin.devmode.liveReload.enabled=false");
        return prepared.toArray(String[]::new);
    }

    private static void showControlWindow(ConfigurableApplicationContext context, String url) {
        JFrame frame = new JFrame("Коридор");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(420, 170);
        frame.setLocationRelativeTo(null);

        JLabel title = new JLabel("Игра \"Коридор\" запущена", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel hint = new JLabel("<html><div style='text-align:center;'>Окно игры открыто в браузере.<br>Закройте это окно, чтобы завершить приложение.</div></html>", SwingConstants.CENTER);
        JButton openButton = new JButton("Открыть игру");
        openButton.addActionListener(event -> openBrowser(url));

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        panel.add(title, BorderLayout.NORTH);
        panel.add(hint, BorderLayout.CENTER);
        panel.add(openButton, BorderLayout.SOUTH);
        frame.setContentPane(panel);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                context.close();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // Если браузер не открылся автоматически, пользователь может нажать кнопку в окне запуска.
        }
    }
}
