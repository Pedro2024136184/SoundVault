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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistsView extends VBox {

    private final SpotifyService spotifyService;
    private final LibraryService libraryService;
    private final SlskdService slskdService;
    private final com.soundvault.service.WishlistService wishlistService;

    private final ComboBox<String> playlistCombo;
    private final List<Map<String, String>> playlistsData = new ArrayList<>();

    private final ObservableList<Track> masterTracks = FXCollections.observableArrayList();
    private final FilteredList<Track> filteredTracks = new FilteredList<>(masterTracks, p -> true);

    private final Label countLabel;
    private final ProgressBar progressBar;
    private final ListView<Track> listView;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PlaylistsView(SpotifyService spotifyService, LibraryService libraryService, SlskdService slskdService) {
        this(spotifyService, libraryService, slskdService, null);
    }

    public PlaylistsView(SpotifyService spotifyService, LibraryService libraryService, SlskdService slskdService, com.soundvault.service.WishlistService wishlistService) {
        this.spotifyService = spotifyService;
        this.libraryService = libraryService;
        this.slskdService = slskdService;
        this.wishlistService = wishlistService;

        this.setSpacing(15);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Minhas Playlists");
        header.getStyleClass().add("header-label");

        playlistCombo = new ComboBox<>();
        playlistCombo.setPromptText("A carregar playlists...");
        playlistCombo.setPrefWidth(320);
        playlistCombo.setOnAction(e -> onPlaylistSelected());

        Button btnRefresh = new Button("Atualizar");
        btnRefresh.getStyleClass().add("action-button");
        btnRefresh.setOnAction(e -> loadPlaylists());

        TextField searchField = new TextField();
        searchField.setPromptText("Filtrar músicas...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.isBlank()) {
                filteredTracks.setPredicate(t -> true);
            } else {
                String norm = StringNormalizer.normalize(newV);
                filteredTracks.setPredicate(t ->
                        StringNormalizer.normalize(t.title()).contains(norm) ||
                        StringNormalizer.normalize(t.artist()).contains(norm) ||
                        StringNormalizer.normalize(t.album()).contains(norm)
                );
            }
        });

        countLabel = new Label("0 faixas");
        countLabel.getStyleClass().add("badge-pill");

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(120);
        progressBar.setVisible(false);

        HBox bar = new HBox(12, playlistCombo, btnRefresh, searchField, countLabel, progressBar);
        bar.setAlignment(Pos.CENTER_LEFT);

        listView = new ListView<>(filteredTracks);
        listView.setCellFactory(lv -> new TrackListCell(libraryService, slskdService, wishlistService));
        VBox.setVgrow(listView, Priority.ALWAYS);

        this.getChildren().addAll(header, bar, listView);

        loadPlaylists();
    }

    public void loadPlaylists() {
        progressBar.setVisible(true);
        executor.submit(() -> {
            List<Map<String, String>> lists = spotifyService.getUserPlaylists();
            Platform.runLater(() -> {
                playlistsData.clear();
                playlistsData.addAll(lists);
                playlistCombo.getItems().clear();
                for (Map<String, String> p : lists) {
                    playlistCombo.getItems().add(p.get("name"));
                }
                progressBar.setVisible(false);
                if (!playlistCombo.getItems().isEmpty()) {
                    playlistCombo.getSelectionModel().selectFirst();
                }
            });
        });
    }

    private void onPlaylistSelected() {
        int index = playlistCombo.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= playlistsData.size()) return;

        String playlistId = playlistsData.get(index).get("id");
        masterTracks.clear();
        progressBar.setVisible(true);

        executor.submit(() -> {
            List<Track> tracks = spotifyService.getPlaylistTracks(playlistId);
            Platform.runLater(() -> {
                masterTracks.addAll(tracks);
                if (tracks.isEmpty()) {
                    countLabel.setText("0 faixas (restrita pelo Spotify)");
                } else {
                    countLabel.setText(tracks.size() + " faixas");
                }
                progressBar.setVisible(false);
            });
        });
    }
}
