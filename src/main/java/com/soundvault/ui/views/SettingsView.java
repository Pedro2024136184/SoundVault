package com.soundvault.ui.views;

import com.soundvault.config.AppConfig;
import com.soundvault.config.SlskdYamlConfig;
import com.soundvault.service.SlskdService;
import com.soundvault.service.SpotifyService;
import com.soundvault.service.ThemeService;
import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsView extends VBox {

    private final ThemeService themeService;
    private final SlskdService slskdService;
    private final SpotifyService spotifyService;
    private final SlskdYamlConfig yamlConfig = new SlskdYamlConfig();

    // Color Pickers
    private final ColorPicker cpBg;
    private final ColorPicker cpSidebar;
    private final ColorPicker cpCard;
    private final ColorPicker cpBtnPrimary;
    private final ColorPicker cpBtnHover;
    private final ColorPicker cpTextPrimary;
    private final ColorPicker cpTextAccent;

    // .env Fields
    private final TextField tfLibPath;
    private final TextField tfExePath;
    private final TextField tfSpotifyClientId;
    private final TextField tfSpotifyClientSecret;
    private final TextField tfSlskdApiKey;
    private final TextField tfSlskdHost;
    private final TextField tfSlskdPort;

    // slskd.yml Fields
    private final TextField tfSlskUser;
    private final TextField tfSlskPass;
    private final TextField tfSlskPort;
    private final TextField tfSlskDlPath;
    private final TextField tfSlskIncPath;

    private final Label statusLabel;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SettingsView(ThemeService themeService, SlskdService slskdService, SpotifyService spotifyService) {
        this.themeService = themeService;
        this.slskdService = slskdService;
        this.spotifyService = spotifyService;

        this.setSpacing(20);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Configurações do Sistema e Personalização");
        header.getStyleClass().add("header-label");

        VBox contentBox = new VBox(25);
        contentBox.setPadding(new Insets(10));

        // ==========================================
        // 1. SECÇÃO: TEMA E CORES AO VIVO
        // ==========================================
        VBox themeSection = new VBox(15);
        themeSection.getStyleClass().add("card-frame");
        Label lblThemeTitle = new Label("Personalização Visual e Temas (Hot-Reload Imediato)");
        lblThemeTitle.getStyleClass().add("section-label");

        HBox presetRow = new HBox(12);
        presetRow.setAlignment(Pos.CENTER_LEFT);
        Label lblPreset = new Label("Tema Predefinido (25 Estilos):");
        ComboBox<String> themePresetCombo = new ComboBox<>();
        List<String> allThemeNames = ThemeService.ThemeProperties.getAllThemes().stream()
                .map(ThemeService.ThemeProperties::name)
                .toList();
        themePresetCombo.getItems().addAll(allThemeNames);
        themePresetCombo.setValue(themeService.getCurrentTheme().name());
        themePresetCombo.setOnAction(e -> {
            if (themePresetCombo.getValue() != null) {
                applyPreset(themePresetCombo.getValue());
            }
        });
        presetRow.getChildren().addAll(lblPreset, themePresetCombo);

        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(15);
        colorGrid.setVgap(10);

        ThemeService.ThemeProperties cur = themeService.getCurrentTheme();
        cpBg = new ColorPicker(Color.web(cur.bgDark()));
        cpSidebar = new ColorPicker(Color.web(cur.sidebarDark()));
        cpCard = new ColorPicker(Color.web(cur.cardBg()));
        cpBtnPrimary = new ColorPicker(Color.web(cur.buttonPrimary()));
        cpBtnHover = new ColorPicker(Color.web(cur.buttonHover()));
        cpTextPrimary = new ColorPicker(Color.web(cur.textPrimary()));
        cpTextAccent = new ColorPicker(Color.web(cur.textAccent()));

        colorGrid.addRow(0, new Label("Fundo Principal:"), cpBg, new Label("Barra Lateral:"), cpSidebar);
        colorGrid.addRow(1, new Label("Cartões / Painéis:"), cpCard, new Label("Botão Primário:"), cpBtnPrimary);
        colorGrid.addRow(2, new Label("Botão Hover:"), cpBtnHover, new Label("Texto Principal:"), cpTextPrimary);
        colorGrid.addRow(3, new Label("Cor de Destaque:"), cpTextAccent);

        HBox themeButtons = new HBox(12);
        Button btnApplyTheme = new Button("Aplicar Tema");
        btnApplyTheme.getStyleClass().add("action-button");
        btnApplyTheme.setOnAction(e -> applyCustomThemeLive());

        Button btnSaveTheme = new Button("Guardar Tema Padrão");
        btnSaveTheme.setStyle("-fx-background-color: #3a3a40; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 14px; -fx-cursor: hand;");
        btnSaveTheme.setOnAction(e -> {
            applyCustomThemeLive();
            showStatus("Tema gravado com sucesso!", "#1DB954");
        });

        themeButtons.getChildren().addAll(btnApplyTheme, btnSaveTheme);
        themeSection.getChildren().addAll(lblThemeTitle, presetRow, colorGrid, themeButtons);

        // ==========================================
        // 2. SECÇÃO: SETUP DE SISTEMA (.env)
        // ==========================================
        VBox envSection = new VBox(15);
        envSection.getStyleClass().add("card-frame");
        Label lblEnvTitle = new Label("Setup de Sistema e Variáveis (.env)");
        lblEnvTitle.getStyleClass().add("section-label");

        GridPane envGrid = new GridPane();
        envGrid.setHgap(12);
        envGrid.setVgap(10);

        AppConfig cfg = AppConfig.getInstance();

        tfLibPath = new TextField(cfg.getLocalLibraryPath());
        tfLibPath.setPrefWidth(380);
        Button btnBrowseLib = new Button("Procurar...");
        btnBrowseLib.setOnAction(e -> browseDirectory("Selecionar Pasta da Biblioteca", tfLibPath));

        tfExePath = new TextField(cfg.getSlskdExePath());
        tfExePath.setPrefWidth(380);
        Button btnBrowseExe = new Button("Procurar...");
        btnBrowseExe.setOnAction(e -> browseFile("Selecionar Executável slskd.exe", "*.exe", tfExePath));

        tfSpotifyClientId = new TextField(cfg.getSpotifyClientId());
        tfSpotifyClientId.setPrefWidth(380);

        tfSpotifyClientSecret = new TextField(cfg.getSpotifyClientSecret());
        tfSpotifyClientSecret.setPrefWidth(380);

        tfSlskdApiKey = new TextField(cfg.getSlskdApiKey());
        tfSlskdApiKey.setPrefWidth(380);

        tfSlskdHost = new TextField(cfg.getSlskdHost());
        tfSlskdPort = new TextField(String.valueOf(cfg.getSlskdPort()));

        envGrid.addRow(0, new Label("Pasta da Biblioteca Local:"), tfLibPath, btnBrowseLib);
        envGrid.addRow(1, new Label("Executável Soulseek (.exe):"), tfExePath, btnBrowseExe);
        envGrid.addRow(2, new Label("Spotify Client ID:"), tfSpotifyClientId);
        envGrid.addRow(3, new Label("Spotify Client Secret:"), tfSpotifyClientSecret);
        envGrid.addRow(4, new Label("Soulseek API Key:"), tfSlskdApiKey);

        HBox hostPortRow = new HBox(8, new Label("Host:"), tfSlskdHost, new Label("Porta:"), tfSlskdPort);
        hostPortRow.setAlignment(Pos.CENTER_LEFT);
        envGrid.addRow(5, new Label("Servidor slskd:"), hostPortRow);

        HBox testButtons = new HBox(12);
        Button btnTestSpotify = new Button("Testar Spotify");
        btnTestSpotify.getStyleClass().add("action-button");
        btnTestSpotify.setOnAction(e -> testSpotify());

        Button btnTestSlskd = new Button("Testar Soulseek API");
        btnTestSlskd.getStyleClass().add("action-button");
        btnTestSlskd.setOnAction(e -> testSlskd());

        testButtons.getChildren().addAll(btnTestSpotify, btnTestSlskd);
        envSection.getChildren().addAll(lblEnvTitle, envGrid, testButtons);

        // ==========================================
        // 3. SECÇÃO: MOTOR SOULSEEK (slskd.yml)
        // ==========================================
        VBox slskSection = new VBox(15);
        slskSection.getStyleClass().add("card-frame");
        Label lblSlskTitle = new Label("Motor Soulseek (slskd.yml)");
        lblSlskTitle.getStyleClass().add("section-label");

        GridPane slskGrid = new GridPane();
        slskGrid.setHgap(12);
        slskGrid.setVgap(10);

        tfSlskUser = new TextField(yamlConfig.getProperty("soulseek.username"));
        tfSlskPass = new TextField(yamlConfig.getProperty("soulseek.password"));
        tfSlskPort = new TextField(yamlConfig.getProperty("soulseek.listen_port"));

        tfSlskDlPath = new TextField(yamlConfig.getProperty("directories.downloads"));
        tfSlskDlPath.setPrefWidth(380);
        Button btnBrowseDl = new Button("Procurar...");
        btnBrowseDl.setOnAction(e -> browseDirectory("Pasta de Downloads do Soulseek", tfSlskDlPath));

        tfSlskIncPath = new TextField(yamlConfig.getProperty("directories.incomplete"));
        tfSlskIncPath.setPrefWidth(380);
        Button btnBrowseInc = new Button("Procurar...");
        btnBrowseInc.setOnAction(e -> browseDirectory("Pasta de Ficheiros Incompletos", tfSlskIncPath));

        slskGrid.addRow(0, new Label("Username Soulseek:"), tfSlskUser);
        slskGrid.addRow(1, new Label("Password Soulseek:"), tfSlskPass);
        slskGrid.addRow(2, new Label("Listen Port:"), tfSlskPort);
        slskGrid.addRow(3, new Label("Pasta Downloads:"), tfSlskDlPath, btnBrowseDl);
        slskGrid.addRow(4, new Label("Pasta Incompletos:"), tfSlskIncPath, btnBrowseInc);

        slskSection.getChildren().addAll(lblSlskTitle, slskGrid);

        // ==========================================
        // BOTÃO SAVE FINAL
        // ==========================================
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-weight: bold;");

        Button btnSaveAll = new Button("Guardar Todas as Configurações (.env e slskd.yml)");
        btnSaveAll.setStyle("-fx-background-color: -theme-btn-primary; -fx-text-fill: -theme-text-primary; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 14px; -fx-cursor: hand;");
        btnSaveAll.setOnAction(e -> saveAllSettings());

        contentBox.getChildren().addAll(themeSection, envSection, slskSection, statusLabel, btnSaveAll);

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        this.getChildren().addAll(header, scroll);
    }

    private void applyPreset(String presetName) {
        ThemeService.ThemeProperties p = ThemeService.ThemeProperties.getThemeByName(presetName);
        themeService.setTheme(p);
        if (this.getScene() != null) {
            themeService.applyTheme(this.getScene());
        }

        cpBg.setValue(Color.web(p.bgDark()));
        cpSidebar.setValue(Color.web(p.sidebarDark()));
        cpCard.setValue(Color.web(p.cardBg()));
        cpBtnPrimary.setValue(Color.web(p.buttonPrimary()));
        cpBtnHover.setValue(Color.web(p.buttonHover()));
        cpTextPrimary.setValue(Color.web(p.textPrimary()));
        cpTextAccent.setValue(Color.web(p.textAccent()));
    }

    private void applyCustomThemeLive() {
        ThemeService.ThemeProperties custom = new ThemeService.ThemeProperties(
                "Personalizado",
                toHex(cpBg.getValue()),
                toHex(cpSidebar.getValue()),
                toHex(cpBtnPrimary.getValue()),
                toHex(cpBtnHover.getValue()),
                "#ffffff",
                toHex(cpTextPrimary.getValue()),
                toHex(cpTextAccent.getValue()),
                "#0d0d0d",
                toHex(cpCard.getValue()),
                14,
                themeService.getCurrentTheme().fontFamily()
        );

        themeService.setTheme(custom);
        Scene scene = getScene();
        if (scene != null) {
            themeService.applyTheme(scene);
        }
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private void browseDirectory(String title, TextField target) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(title);
        File init = new File(target.getText().trim());
        if (init.exists() && init.isDirectory()) dc.setInitialDirectory(init);

        File chosen = dc.showDialog(getScene().getWindow());
        if (chosen != null) target.setText(chosen.getAbsolutePath());
    }

    private void browseFile(String title, String extFilter, TextField target) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executável", extFilter));

        File init = new File(target.getText().trim());
        if (init.exists() && init.getParentFile() != null && init.getParentFile().isDirectory()) {
            fc.setInitialDirectory(init.getParentFile());
        }

        File chosen = fc.showOpenDialog(getScene().getWindow());
        if (chosen != null) target.setText(chosen.getAbsolutePath());
    }

    private void testSpotify() {
        showStatus("A testar autenticação Spotify...", "#ffaa00");
        executor.submit(() -> {
            boolean ok = spotifyService.authenticate();
            Platform.runLater(() -> {
                if (ok) showStatus("Conexão ao Spotify bem-sucedida! Utilizador: " + spotifyService.getCurrentUserId(), "#1DB954");
                else showStatus("Falha na autenticação Spotify.", "#ff5555");
            });
        });
    }

    private void testSlskd() {
        showStatus("A testar conexão à API do Soulseek (slskd)...", "#ffaa00");
        executor.submit(() -> {
            boolean ok = slskdService.testConnection();
            Platform.runLater(() -> {
                if (ok) showStatus("Conexão ao slskd bem-sucedida! API a responder.", "#1DB954");
                else showStatus("Não foi possível ligar ao slskd. Verifica se o daemon está a correr.", "#ff5555");
            });
        });
    }

    private void saveAllSettings() {
        try {
            AppConfig cfg = AppConfig.getInstance();
            cfg.setLocalLibraryPath(tfLibPath.getText().trim());
            cfg.setSlskdExePath(tfExePath.getText().trim());
            cfg.setSpotifyClientId(tfSpotifyClientId.getText().trim());
            cfg.setSpotifyClientSecret(tfSpotifyClientSecret.getText().trim());
            cfg.setSlskdApiKey(tfSlskdApiKey.getText().trim());
            cfg.setSlskdHost(tfSlskdHost.getText().trim());

            try {
                cfg.setSlskdPort(Integer.parseInt(tfSlskdPort.getText().trim()));
            } catch (NumberFormatException ignored) {}

            cfg.save();

            // Sincroniza slskd.yml
            yamlConfig.setProperty("soulseek.username", tfSlskUser.getText().trim());
            yamlConfig.setProperty("soulseek.password", tfSlskPass.getText().trim());
            try {
                yamlConfig.setProperty("soulseek.listen_port", Integer.parseInt(tfSlskPort.getText().trim()));
            } catch (Exception ignored) {}
            yamlConfig.setProperty("directories.downloads", tfSlskDlPath.getText().trim());
            yamlConfig.setProperty("directories.incomplete", tfSlskIncPath.getText().trim());
            yamlConfig.syncApiKey(tfSlskdApiKey.getText().trim());
            yamlConfig.save();

            showStatus("Todas as configurações (.env e slskd.yml) foram gravadas com sucesso!", "#1DB954");
            AppLogger.log("Configurações atualizadas com sucesso pelo utilizador.");
        } catch (Exception e) {
            showStatus("Erro ao guardar configurações: " + e.getMessage(), "#ff5555");
        }
    }

    private void showStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
    }
}
