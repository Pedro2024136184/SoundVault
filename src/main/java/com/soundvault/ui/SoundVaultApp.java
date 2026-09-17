package com.soundvault.ui;

import com.soundvault.config.AppConfig;
import com.soundvault.service.LibraryService;
import com.soundvault.service.PhysicalLibraryService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.SpotifyService;
import com.soundvault.service.ThemeService;
import com.soundvault.service.WishlistService;
import com.soundvault.ui.components.DownloadManagerBar;
import com.soundvault.ui.views.*;
import com.soundvault.util.AppLogger;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundVaultApp extends Application {

    private final ThemeService themeService = new ThemeService();
    private final SpotifyService spotifyService = new SpotifyService();
    private final SlskdService slskdService = new SlskdService();
    private final LibraryService libraryService = new LibraryService();
    private final PhysicalLibraryService physicalLibraryService = new PhysicalLibraryService();
    private final WishlistService wishlistService = new WishlistService();

    private final Map<String, Button> sidebarButtons = new HashMap<>();
    private final Map<String, Node> viewCache = new HashMap<>();
    private BorderPane rootLayout;
    private StackPane contentArea;
    private String activeViewName = "";

    private final ExecutorService backgroundExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void start(Stage stage) {
        AppLogger.log("A iniciar SoundVault Java Edition...");

        // Inicia serviços assíncronos
        backgroundExecutor.submit(() -> libraryService.updateCache(false));
        backgroundExecutor.submit(spotifyService::authenticate);

        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("main-background");

        // 1. Sidebar à esquerda
        VBox sidebar = buildSidebar();
        rootLayout.setLeft(sidebar);

        // 2. Área central de conteúdo
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(10));
        rootLayout.setCenter(contentArea);

        // 3. Barra de Gestão de Downloads em Tempo Real ao fundo
        DownloadManagerBar downloadBar = new DownloadManagerBar(slskdService, () -> switchView("Soulseek"));
        rootLayout.setBottom(downloadBar);

        Scene scene = new Scene(rootLayout, 1320, 840);
        themeService.applyTheme(scene);

        // Ícone da janela
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/icon.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        stage.setTitle("The SoundVault - Ultimate Manager");
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();

        // Aba inicial padrão
        switchView("LikedSongs");
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(24, 14, 20, 14));
        sidebar.getStyleClass().add("sidebar");

        // Brand Header (High-End Isometric Vault Logo + Typographic Identity)
        HBox brandHeader = new HBox(12);
        brandHeader.setAlignment(Pos.CENTER_LEFT);
        brandHeader.setPadding(new Insets(0, 4, 18, 4));

        ImageView logoView = null;
        try (InputStream is = getClass().getResourceAsStream("/icons/logo.png")) {
            if (is != null) {
                Image logoImg = new Image(is, 40, 40, true, true);
                logoView = new ImageView(logoImg);
                logoView.setFitWidth(40);
                logoView.setFitHeight(40);
                Rectangle clip = new Rectangle(40, 40);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                logoView.setClip(clip);
            }
        } catch (Exception ignored) {}

        VBox brandTextBox = new VBox(2);
        brandTextBox.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label("SOUNDVAULT");
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-letter-spacing: 1.5px;");

        Label badge = new Label("PRO");
        badge.setStyle("-fx-font-size: 8.5px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent; -fx-background-color: derive(-theme-card-bg, 15%); -fx-background-radius: 4px; -fx-padding: 1 5; -fx-border-color: derive(-theme-text-accent, -40%); -fx-border-radius: 4px;");

        titleRow.getChildren().addAll(titleLbl, badge);

        Label subtitle = new Label("HI-RES VAULT ARCHIVE");
        subtitle.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: derive(-theme-text-primary, -45%); -fx-letter-spacing: 1.2px;");

        brandTextBox.getChildren().addAll(titleRow, subtitle);

        if (logoView != null) {
            StackPane logoContainer = new StackPane(logoView);
            logoContainer.setStyle("-fx-background-color: derive(-theme-card-bg, 10%); -fx-background-radius: 10px; -fx-border-color: derive(-theme-text-accent, -50%); -fx-border-radius: 10px; -fx-border-width: 1px;");
            brandHeader.getChildren().add(logoContainer);
        }
        brandHeader.getChildren().add(brandTextBox);

        sidebar.getChildren().add(brandHeader);

        // SECÇÃO 1: BIBLIOTECA
        sidebar.getChildren().add(createSectionLabel("COLEÇÃO"));
        addNavButton(sidebar, "Liked Songs", "LikedSongs");
        addNavButton(sidebar, "Biblioteca Local", "Library");
        addNavButton(sidebar, "Biblioteca Física", "PhysicalLibrary");
        addNavButton(sidebar, "Lista de Desejos", "Wishlist");
        addNavButton(sidebar, "Minhas Playlists", "Playlists");

        sidebar.getChildren().add(createSpacer(10));

        // SECÇÃO 2: DESCOBERTA & P2P
        sidebar.getChildren().add(createSectionLabel("DESCOBERTA & REDE"));
        addNavButton(sidebar, "Monitor Soulseek", "Soulseek");
        addNavButton(sidebar, "Relic Explorer", "RelicExplorer");
        addNavButton(sidebar, "Deep Dive Artistas", "DeepDive");
        addNavButton(sidebar, "Análise Musical", "Analyses");

        // Espaçador para empurrar o sistema para o fundo
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        sidebar.getChildren().add(bottomSpacer);

        // SECÇÃO 3: SISTEMA
        sidebar.getChildren().add(createSectionLabel("SISTEMA"));

        // Botão Ligar Soulseek
        Button btnLaunchSlsk = new Button("Ligar Soulseek (slskd)");
        btnLaunchSlsk.setMaxWidth(Double.MAX_VALUE);
        btnLaunchSlsk.setStyle("-fx-background-color: linear-gradient(to right, #ea580c, #f97316); -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 14; -fx-background-radius: 8px; -fx-cursor: hand; -fx-alignment: BASELINE_LEFT; -fx-effect: dropshadow(gaussian, rgba(234, 88, 12, 0.3), 8, 0, 0, 2);");
        btnLaunchSlsk.setOnAction(e -> launchSlskdProcess());
        sidebar.getChildren().add(btnLaunchSlsk);

        addNavButton(sidebar, "System Logs", "Logs");
        addNavButton(sidebar, "Configurações", "Settings");

        // SECÇÃO 4: SELETOR DE TEMAS RÁPIDO (25 ESTILOS)
        VBox themeBox = new VBox(6);
        themeBox.setPadding(new Insets(10, 0, 0, 0));
        Label lblTheme = createSectionLabel("TEMA VISUAL");

        ComboBox<String> themeSelect = new ComboBox<>();
        themeSelect.setMaxWidth(Double.MAX_VALUE);
        List<String> themeNames = ThemeService.ThemeProperties.getAllThemes().stream()
                .map(ThemeService.ThemeProperties::name)
                .toList();
        themeSelect.getItems().addAll(themeNames);
        themeSelect.setValue(themeService.getCurrentTheme().name());
        themeSelect.setOnAction(e -> {
            String val = themeSelect.getValue();
            if (val != null && rootLayout.getScene() != null) {
                themeService.setTheme(ThemeService.ThemeProperties.getThemeByName(val));
                themeService.applyTheme(rootLayout.getScene());
            }
        });

        Button btnRandomTheme = new Button("Tema Aleatório");
        btnRandomTheme.setMaxWidth(Double.MAX_VALUE);
        btnRandomTheme.setStyle("-fx-background-color: transparent; -fx-border-color: derive(-theme-text-primary, -50%); -fx-border-radius: 6px; -fx-text-fill: derive(-theme-text-primary, -20%); -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
        btnRandomTheme.setOnAction(e -> {
            ThemeService.ThemeProperties rnd = ThemeService.ThemeProperties.getRandomTheme();
            themeService.setTheme(rnd);
            themeSelect.setValue(rnd.name());
            if (rootLayout.getScene() != null) {
                themeService.applyTheme(rootLayout.getScene());
            }
        });

        themeBox.getChildren().addAll(lblTheme, themeSelect, btnRandomTheme);
        sidebar.getChildren().add(themeBox);

        return sidebar;
    }

    private Label createSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-padding: 8 8 3 8; -fx-letter-spacing: 1px;");
        return lbl;
    }

    private void addNavButton(VBox container, String labelText, String viewKey) {
        Button btn = new Button(labelText);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("sidebar-button");
        btn.setOnAction(e -> switchView(viewKey));
        sidebarButtons.put(viewKey, btn);
        container.getChildren().add(btn);
    }

    private void switchView(String viewKey) {
        if (viewKey.equals(activeViewName) && contentArea.getChildren().contains(viewCache.get(viewKey))) {
            return;
        }

        Node view = viewCache.computeIfAbsent(viewKey, this::createView);
        contentArea.getChildren().setAll(view);
        activeViewName = viewKey;

        // Atualiza estilo dos botões da sidebar
        for (Map.Entry<String, Button> entry : sidebarButtons.entrySet()) {
            Button b = entry.getValue();
            b.getStyleClass().removeAll("sidebar-button", "sidebar-button-active");
            if (entry.getKey().equals(viewKey)) {
                b.getStyleClass().add("sidebar-button-active");
            } else {
                b.getStyleClass().add("sidebar-button");
            }
        }
    }

    private Node createView(String key) {
        return switch (key) {
            case "LikedSongs" -> new LikedSongsView(spotifyService, libraryService, slskdService, wishlistService);
            case "Library" -> new LibraryView(libraryService, spotifyService);
            case "PhysicalLibrary" -> new PhysicalLibraryView(physicalLibraryService, spotifyService, libraryService, slskdService, wishlistService);
            case "Wishlist" -> new WishlistView(wishlistService, physicalLibraryService, spotifyService, slskdService);
            case "Soulseek" -> new SoulseekView(slskdService);
            case "Playlists" -> new PlaylistsView(spotifyService, libraryService, slskdService, wishlistService);
            case "Analyses" -> new AnalysesView(spotifyService);
            case "RelicExplorer" -> new RelicExplorerView(slskdService, wishlistService);
            case "DeepDive" -> new DeepDiveView(spotifyService, libraryService, slskdService);
            case "Settings" -> new SettingsView(themeService, slskdService, spotifyService);
            case "Logs" -> new LogsView();
            default -> new Label("Vista não encontrada.");
        };
    }

    private void launchSlskdProcess() {
        String exePath = AppConfig.getInstance().getSlskdExePath();
        if (exePath == null || exePath.isBlank() || !new File(exePath).exists()) {
            AppLogger.log("Aviso: Caminho do executável slskd.exe não configurado ou ficheiro inexistente.");
            return;
        }

        try {
            new ProcessBuilder(exePath).start();
            AppLogger.log("Daemon do Soulseek (slskd) iniciado.");
        } catch (Exception e) {
            AppLogger.log("Erro ao iniciar executável do slskd: " + e.getMessage());
        }
    }

    private Region createSpacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        return r;
    }
}
