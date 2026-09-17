package com.soundvault.ui.views;

import com.soundvault.config.AppConfig;
import com.soundvault.service.LibraryService;
import com.soundvault.service.SpotifyService;
import com.soundvault.service.TaggingService;
import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibraryView extends VBox {

    private final LibraryService libraryService;
    private final TaggingService taggingService = new TaggingService();
    private final SpotifyService spotifyService;

    private final VBox cardsContainer;
    private final ProgressBar progressBar;
    private final Button btnRescan;
    private String currentTab = "tags";

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LibraryView(LibraryService libraryService, SpotifyService spotifyService) {
        this.libraryService = libraryService;
        this.spotifyService = spotifyService;

        this.setSpacing(15);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Biblioteca Local");
        header.getStyleClass().add("header-label");

        Label countBadge = new Label(libraryService.getAlbumCount() + " álbuns");
        countBadge.getStyleClass().add("badge-pill");

        HBox titleBox = new HBox(14, header, countBadge);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Barra do Caminho da Biblioteca
        Label pathLabel = new Label("Pasta: " + AppConfig.getInstance().getLocalLibraryPath());
        pathLabel.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 12px;");

        Button btnChangePath = new Button("Alterar Pasta...");
        btnChangePath.setStyle("-fx-background-color: transparent; -fx-border-color: -theme-btn-hover; -fx-border-radius: 12px; -fx-text-fill: -theme-text-accent; -fx-font-size: 11px; -fx-padding: 3 10; -fx-cursor: hand;");
        btnChangePath.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Selecionar Pasta da Biblioteca Local");
            File initialDir = new File(AppConfig.getInstance().getLocalLibraryPath());
            if (initialDir.exists()) dc.setInitialDirectory(initialDir);
            File chosen = dc.showDialog(getScene().getWindow());
            if (chosen != null) {
                AppConfig.getInstance().setLocalLibraryPath(chosen.getAbsolutePath());
                try {
                    AppConfig.getInstance().save();
                } catch (Exception ignored) {}
                pathLabel.setText("Pasta: " + chosen.getAbsolutePath());
                rescanLibrary();
            }
        });

        HBox pathBar = new HBox(10, pathLabel, btnChangePath);
        pathBar.setAlignment(Pos.CENTER_LEFT);

        Button btnTags = new Button("Tags FLAC");
        btnTags.getStyleClass().add("action-button");
        btnTags.setOnAction(e -> showTagsTab());

        Button btnCovers = new Button("Scanner de Capas");
        btnCovers.getStyleClass().add("action-button");
        btnCovers.setOnAction(e -> showCoversTab());

        btnRescan = new Button("Re-indexar");
        btnRescan.setStyle("-fx-background-color: #3a3a40; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 14px; -fx-cursor: hand;");
        btnRescan.setOnAction(e -> rescanLibrary());

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(140);
        progressBar.setVisible(false);

        HBox navBar = new HBox(10, btnTags, btnCovers, btnRescan, progressBar);
        navBar.setAlignment(Pos.CENTER_LEFT);

        cardsContainer = new VBox(8);
        cardsContainer.setPadding(new Insets(5));

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(titleBox, pathBar, navBar, scrollPane);

        showTagsTab();
    }

    private void rescanLibrary() {
        progressBar.setVisible(true);
        btnRescan.setDisable(true);
        executor.submit(() -> {
            libraryService.updateCache(true);
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                btnRescan.setDisable(false);
                if ("tags".equals(currentTab)) showTagsTab();
                else showCoversTab();
            });
        });
    }

    public void showTagsTab() {
        this.currentTab = "tags";
        cardsContainer.getChildren().clear();

        List<LibraryService.FolderTagIssue> issues = libraryService.getMissingTagFolders();
        if (issues.isEmpty()) {
            Label okLbl = new Label("Todos os álbuns FLAC têm metadados de Artista e Álbum corretos!");
            okLbl.setStyle("-fx-text-fill: #1DB954; -fx-font-size: 14px; -fx-font-weight: bold;");
            cardsContainer.getChildren().add(okLbl);
            return;
        }

        Label hint = new Label("Formato obrigatório para aplicar tags: 'Nome do Álbum - Artista'");
        hint.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 12px;");
        cardsContainer.getChildren().add(hint);

        for (LibraryService.FolderTagIssue issue : issues) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("card-frame");

            Label nameLbl = new Label(issue.folderName());
            nameLbl.setPrefWidth(300);
            nameLbl.setStyle("-fx-font-weight: bold;");

            TextField input = new TextField();
            input.setPromptText("Ex: Rumours - Fleetwood Mac");
            input.setPrefWidth(280);

            Button btnApply = new Button("Aplicar Tags");
            btnApply.getStyleClass().add("action-button");
            btnApply.setOnAction(e -> applyTags(issue.folderPath(), input.getText().trim(), row));

            row.getChildren().addAll(nameLbl, input, btnApply);
            cardsContainer.getChildren().add(row);
        }
    }

    private void applyTags(String folderPath, String text, HBox rowWidget) {
        if (!text.contains(" - ")) {
            AppLogger.log("ERRO: O texto deve conter ' - ' a separar o álbum do artista.");
            return;
        }

        String[] parts = text.split(" - ", 2);
        String album = parts[0].trim();
        String artist = parts[1].trim();

        executor.submit(() -> {
            File folder = new File(folderPath);
            File[] flacs = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".flac"));
            int count = 0;
            if (flacs != null) {
                for (File f : flacs) {
                    if (taggingService.updateAlbumArtistTags(f, album, artist)) {
                        count++;
                    }
                }
            }
            AppLogger.log("Tags atualizadas em " + count + " ficheiros na pasta '" + folder.getName() + "'.");
            Platform.runLater(() -> cardsContainer.getChildren().remove(rowWidget));
        });
    }

    public void showCoversTab() {
        this.currentTab = "covers";
        cardsContainer.getChildren().clear();

        List<LibraryService.FolderCoverIssue> issues = libraryService.getMissingCoverFolders();
        if (issues.isEmpty()) {
            Label okLbl = new Label("Perfeito! Todos os teus álbuns em disco têm capa embebida.");
            okLbl.setStyle("-fx-text-fill: #1DB954; -fx-font-size: 14px; -fx-font-weight: bold;");
            cardsContainer.getChildren().add(okLbl);
            return;
        }

        for (LibraryService.FolderCoverIssue issue : issues) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("card-frame");

            Label nameLbl = new Label(issue.folderName());
            nameLbl.setPrefWidth(350);
            nameLbl.setStyle("-fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnManual = new Button("Procurar Ficheiro");
            btnManual.setStyle("-fx-background-color: #3a3a40; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 12;");
            btnManual.setOnAction(e -> applyManualCover(issue.folderPath(), row));

            Button btnAuto = new Button("Auto-Fix (Spotify)");
            btnAuto.setStyle("-fx-background-color: #1DB954; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 12;");
            btnAuto.setOnAction(e -> applyAutoCover(issue.folderName(), issue.folderPath(), row));

            row.getChildren().addAll(nameLbl, spacer, btnManual, btnAuto);
            cardsContainer.getChildren().add(row);
        }
    }

    private void applyManualCover(String folderPath, HBox rowWidget) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleciona a imagem da capa");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens (*.jpg, *.png)", "*.jpg", "*.jpeg", "*.png"));

        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;

        executor.submit(() -> {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String mime = file.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

                File folder = new File(folderPath);
                File[] flacs = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".flac"));
                int count = 0;
                if (flacs != null) {
                    for (File f : flacs) {
                        if (taggingService.embedCoverArt(f, bytes, mime)) count++;
                    }
                }
                AppLogger.log("Capa manual aplicada a " + count + " ficheiros em '" + folder.getName() + "'.");
                Platform.runLater(() -> cardsContainer.getChildren().remove(rowWidget));
            } catch (Exception e) {
                AppLogger.log("Erro ao aplicar capa manual: " + e.getMessage());
            }
        });
    }

    private void applyAutoCover(String folderName, String folderPath, HBox rowWidget) {
        executor.submit(() -> {
            AppLogger.log("A procurar capa no Spotify para '" + folderName + "'...");
            Optional<String> coverUrlOpt = spotifyService.searchAlbumCoverUrl(folderName);
            if (coverUrlOpt.isEmpty()) {
                AppLogger.log("Nenhuma capa encontrada no Spotify para: " + folderName);
                return;
            }

            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(coverUrlOpt.get())).GET().build();
                HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

                if (res.statusCode() == 200) {
                    byte[] bytes = res.body();
                    File folder = new File(folderPath);
                    File[] flacs = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".flac"));
                    int count = 0;
                    if (flacs != null) {
                        for (File f : flacs) {
                            if (taggingService.embedCoverArt(f, bytes, "image/jpeg")) count++;
                        }
                    }
                    AppLogger.log("Capa (Spotify) aplicada com sucesso a " + count + " faixas em '" + folder.getName() + "'.");
                    Platform.runLater(() -> cardsContainer.getChildren().remove(rowWidget));
                }
            } catch (Exception e) {
                AppLogger.log("Erro no Auto-Fix Spotify: " + e.getMessage());
            }
        });
    }
}
