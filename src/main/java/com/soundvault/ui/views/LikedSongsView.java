package com.soundvault.ui.views;

import com.soundvault.model.Track;
import com.soundvault.service.LibraryService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.SpotifyService;
import com.soundvault.ui.components.TrackListCell;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LikedSongsView extends VBox {

    private final SpotifyService spotifyService;
    private final LibraryService libraryService;
    private final SlskdService slskdService;
    private final com.soundvault.service.WishlistService wishlistService;

    private final ObservableList<Track> masterData = FXCollections.observableArrayList();
    private final FilteredList<Track> filteredData = new FilteredList<>(masterData, p -> true);

    private final Label countLabel;
    private final ProgressBar progressBar;
    private final Button btnRefresh;
    private final TextField searchField;
    private final ListView<Track> listView;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LikedSongsView(SpotifyService spotifyService, LibraryService libraryService, SlskdService slskdService) {
        this(spotifyService, libraryService, slskdService, null);
    }

    public LikedSongsView(SpotifyService spotifyService, LibraryService libraryService, SlskdService slskdService, com.soundvault.service.WishlistService wishlistService) {
        this.spotifyService = spotifyService;
        this.libraryService = libraryService;
        this.slskdService = slskdService;
        this.wishlistService = wishlistService;

        this.setSpacing(15);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Liked Songs");
        header.getStyleClass().add("header-label");

        countLabel = new Label("0 faixas");
        countLabel.getStyleClass().add("badge-pill");

        HBox titleBox = new HBox(14, header, countLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Barra de Ações (Filtro de busca + Botão Refresh)
        searchField = new TextField();
        searchField.setPromptText("Filtrar por título, artista ou álbum...");
        searchField.setPrefWidth(380);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        btnRefresh = new Button("Atualizar");
        btnRefresh.getStyleClass().add("action-button");
        btnRefresh.setOnAction(e -> loadTracks());

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180);
        progressBar.setVisible(false);

        HBox actionBox = new HBox(12, searchField, btnRefresh, progressBar);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        // ListView Virtualizada
        listView = new ListView<>(filteredData);
        listView.setCellFactory(lv -> new TrackListCell(libraryService, slskdService, wishlistService));
        VBox.setVgrow(listView, Priority.ALWAYS);

        this.getChildren().addAll(titleBox, actionBox, listView);

        // Carregamento inicial em background
        loadTracks();
    }

    public void loadTracks() {
        masterData.clear();
        progressBar.setVisible(true);
        btnRefresh.setDisable(true);
        countLabel.setText("A descarregar do Spotify...");

        executor.submit(() -> {
            spotifyService.loadLikedSongsProgressively(batch -> {
                Platform.runLater(() -> {
                    masterData.addAll(batch);
                    countLabel.setText(masterData.size() + " faixas carregadas");
                });
            });

            Platform.runLater(() -> {
                progressBar.setVisible(false);
                btnRefresh.setDisable(false);
                countLabel.setText(masterData.size() + " faixas");
            });
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(t -> true);
            return;
        }
        String norm = StringNormalizer.normalize(query);
        filteredData.setPredicate(track -> {
            String title = StringNormalizer.normalize(track.title());
            String artist = StringNormalizer.normalize(track.artist());
            String album = StringNormalizer.normalize(track.album());
            return title.contains(norm) || artist.contains(norm) || album.contains(norm);
        });
    }
}
