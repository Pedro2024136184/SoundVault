package com.soundvault.ui.views;

import com.soundvault.model.Artist;
import com.soundvault.service.LibraryService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.SpotifyService;
import com.soundvault.ui.components.ImageCache;
import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DeepDiveView extends VBox {

    private final SpotifyService spotifyService;
    private final LibraryService libraryService;
    private final SlskdService slskdService;

    private final VBox cardsContainer;
    private final ProgressBar progressBar;
    private final AtomicInteger currentTaskId = new AtomicInteger(0);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DeepDiveView(SpotifyService spotifyService, LibraryService libraryService, SlskdService slskdService) {
        this.spotifyService = spotifyService;
        this.libraryService = libraryService;
        this.slskdService = slskdService;

        this.setSpacing(15);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Deep Dive - Teus Top Artistas");
        header.getStyleClass().add("header-label");

        // Botões de período
        Button btnMonth = new Button("Último Mês");
        btnMonth.getStyleClass().add("action-button");
        btnMonth.setOnAction(e -> triggerDive("short_term"));

        Button btnSixMonths = new Button("Últimos 6 Meses");
        btnSixMonths.getStyleClass().add("action-button");
        btnSixMonths.setOnAction(e -> triggerDive("medium_term"));

        Button btnYear = new Button("Último Ano");
        btnYear.getStyleClass().add("action-button");
        btnYear.setOnAction(e -> triggerDive("long_term"));

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);

        HBox btnBox = new HBox(10, btnMonth, btnSixMonths, btnYear, progressBar);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        cardsContainer = new VBox(8);
        cardsContainer.setPadding(new Insets(5));

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(header, btnBox, scrollPane);

        triggerDive("short_term");
    }

    public void triggerDive(String timeRange) {
        int taskId = currentTaskId.incrementAndGet();
        progressBar.setVisible(true);
        cardsContainer.getChildren().clear();

        Label loadingLbl = new Label("A carregar teus artistas mais ouvidos...");
        loadingLbl.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 14px;");
        cardsContainer.getChildren().add(loadingLbl);

        executor.submit(() -> {
            List<Artist> artists = spotifyService.getTopArtists(timeRange, 15);
            if (taskId != currentTaskId.get()) return;

            Platform.runLater(() -> {
                cardsContainer.getChildren().clear();
                progressBar.setVisible(false);

                if (artists.isEmpty()) {
                    cardsContainer.getChildren().add(new Label("Nenhum artista retornado pelo Spotify."));
                    return;
                }

                int rank = 1;
                for (Artist artist : artists) {
                    int localCount = libraryService.getArtistAlbumCount(artist.name());
                    cardsContainer.getChildren().add(createArtistRow(rank++, artist, localCount));
                }
            });
        });
    }

    private HBox createArtistRow(int rank, Artist artist, int localCount) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card-frame");

        Label rankLbl = new Label(String.format("#%02d", rank));
        rankLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent; -fx-background-color: derive(-theme-card-bg, 15%); -fx-background-radius: 6px; -fx-padding: 4 8;");

        ImageView avatar = new ImageView();
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        Circle clip = new Circle(25, 25, 25);
        avatar.setClip(clip);

        if (artist.imageUrl() != null && !artist.imageUrl().isBlank()) {
            ImageCache.getInstance().loadImageAsync(artist.imageUrl(), 50, 50, avatar::setImage);
        }

        Label nameLbl = new Label(artist.name());
        nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label countBadge = new Label();
        if (localCount > 0) {
            countBadge.setText(localCount + " Álbum(s) no Disco");
            countBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34d399; -fx-background-radius: 12px; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");
        } else {
            countBadge.setText("0 Álbuns no Disco");
            countBadge.setStyle("-fx-background-color: rgba(148, 163, 184, 0.1); -fx-text-fill: #94a3b8; -fx-background-radius: 12px; -fx-padding: 4 12; -fx-font-size: 11.5px; -fx-border-color: rgba(148, 163, 184, 0.2); -fx-border-radius: 12px;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExplore = new Button("Explorar no Soulseek");
        btnExplore.getStyleClass().add("action-button");
        btnExplore.setOnAction(e -> {
            String cleanArtist = com.soundvault.util.StringNormalizer.cleanArtist(artist.name());
            String query = cleanArtist + " flac";
            AppLogger.log("Deep Dive a descarregar discografia/faixas de: " + query);
            executor.submit(() -> slskdService.searchAndDownload(query, 0));
        });

        row.getChildren().addAll(rankLbl, avatar, nameLbl, countBadge, spacer, btnExplore);
        return row;
    }
}
