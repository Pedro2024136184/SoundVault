package com.soundvault.ui.components;

import com.soundvault.model.Track;
import com.soundvault.service.LibraryService;
import com.soundvault.service.NotificationService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.WishlistService;
import com.soundvault.ui.components.ArtworkModal;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackListCell extends ListCell<Track> {

    private static final ExecutorService WORKER_POOL = Executors.newVirtualThreadPerTaskExecutor();

    private final LibraryService libraryService;
    private final SlskdService slskdService;
    private final WishlistService wishlistService;

    private final HBox root;
    private final ImageView coverView;
    private final Label titleLabel;
    private final Label artistLabel;
    private final Label flacBadge;
    private final Button btnCheck;
    private final Button btnDlSong;
    private final Button btnDlAlbum;
    private final Button btnWishlist;

    private Track currentTrack;

    public TrackListCell(LibraryService libraryService, SlskdService slskdService) {
        this(libraryService, slskdService, null);
    }

    public TrackListCell(LibraryService libraryService, SlskdService slskdService, WishlistService wishlistService) {
        this.libraryService = libraryService;
        this.slskdService = slskdService;
        this.wishlistService = wishlistService;

        this.root = new HBox(14);
        this.root.setAlignment(Pos.CENTER_LEFT);
        this.root.getStyleClass().add("card-frame");

        // Capa com cantos suaves e clique para abrir modal de alta resolução
        this.coverView = new ImageView();
        this.coverView.setFitWidth(48);
        this.coverView.setFitHeight(48);
        this.coverView.setPreserveRatio(true);
        this.coverView.setStyle("-fx-cursor: hand;");
        this.coverView.setOnMouseClicked(e -> {
            if (currentTrack != null && this.getScene() != null) {
                Pane rootPane = (Pane) this.getScene().getRoot();
                ArtworkModal.show(rootPane, currentTrack.album(), currentTrack.artist(), currentTrack.imageUrl(), "", "Álbum", wishlistService, slskdService);
            }
        });
        Rectangle clip = new Rectangle(48, 48);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        this.coverView.setClip(clip);

        // Badge FLAC
        flacBadge = new Label("FLAC");
        flacBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.12); -fx-text-fill: -theme-text-accent; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 1 5;");

        // Título e Artista
        this.titleLabel = new Label();
        this.titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13.5px; -fx-text-fill: #ffffff;");

        this.artistLabel = new Label();
        this.artistLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");

        HBox titleRow = new HBox(8, titleLabel, flacBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox textInfo = new VBox(3, titleRow, artistLabel);
        textInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textInfo, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botão de checagem
        this.btnCheck = new Button("CHECK");
        resetCheckButtonStyle();
        this.btnCheck.setOnAction(e -> handleCheck());

        // Botão DL Faixa
        this.btnDlSong = new Button("Faixa");
        this.btnDlSong.setStyle("-fx-background-color: #1e2536; -fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 12; -fx-border-color: #2e384e; -fx-border-radius: 14px; -fx-cursor: hand;");
        this.btnDlSong.setOnMouseEntered(e -> btnDlSong.setStyle("-fx-background-color: #2b354d; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 12; -fx-border-color: -theme-text-accent; -fx-border-radius: 14px; -fx-cursor: hand;"));
        this.btnDlSong.setOnMouseExited(e -> btnDlSong.setStyle("-fx-background-color: #1e2536; -fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 12; -fx-border-color: #2e384e; -fx-border-radius: 14px; -fx-cursor: hand;"));
        this.btnDlSong.setOnAction(e -> handleDlSong());

        // Botão DL Álbum
        this.btnDlAlbum = new Button("Álbum");
        this.btnDlAlbum.setStyle("-fx-background-color: linear-gradient(to right, -theme-btn-selected, derive(-theme-btn-selected, 25%)); -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 14; -fx-cursor: hand;");
        this.btnDlAlbum.setOnMouseEntered(e -> btnDlAlbum.setStyle("-fx-background-color: linear-gradient(to right, derive(-theme-btn-selected, 15%), derive(-theme-btn-selected, 40%)); -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 14; -fx-cursor: hand;"));
        this.btnDlAlbum.setOnMouseExited(e -> btnDlAlbum.setStyle("-fx-background-color: linear-gradient(to right, -theme-btn-selected, derive(-theme-btn-selected, 25%)); -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 14; -fx-cursor: hand;"));
        this.btnDlAlbum.setOnAction(e -> handleDlAlbum());

        // Botão Wishlist ao lado de Download
        this.btnWishlist = new Button("+ Wish");
        resetWishlistButtonStyle();
        this.btnWishlist.setOnAction(e -> handleWishlist());

        HBox buttonBox = new HBox(8, btnCheck, btnDlSong, btnDlAlbum, btnWishlist);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        this.root.getChildren().addAll(coverView, textInfo, spacer, buttonBox);
    }

    private void resetWishlistButtonStyle() {
        this.btnWishlist.setText("+ Wish");
        this.btnWishlist.setStyle("-fx-background-color: transparent; -fx-border-color: derive(-theme-card-bg, 35%); -fx-border-radius: 14px; -fx-text-fill: -theme-text-primary; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;");
    }

    private void setWishlistActiveStyle() {
        this.btnWishlist.setText("★ Wish");
        this.btnWishlist.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 10; -fx-cursor: hand;");
    }

    private void handleWishlist() {
        if (currentTrack == null || wishlistService == null) return;
        boolean added = wishlistService.toggleWishlist(currentTrack.artist(), currentTrack.album(), currentTrack.imageUrl(), currentTrack.id(), "", "");
        if (added) {
            setWishlistActiveStyle();
            NotificationService.getInstance().notifyWishlistAdded(currentTrack.artist() + " - " + currentTrack.album());
        } else {
            resetWishlistButtonStyle();
        }
    }

    private void resetCheckButtonStyle() {
        this.btnCheck.setText("CHECK");
        this.btnCheck.setStyle("-fx-background-color: #202433; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 12; -fx-border-color: #2f364a; -fx-border-radius: 14px; -fx-cursor: hand;");
    }

    private void setDiscoStyle() {
        this.btnCheck.setText("DISCO");
        this.btnCheck.setStyle("-fx-background-color: rgba(5, 150, 105, 0.2); -fx-text-fill: #34d399; -fx-border-color: rgba(5, 150, 105, 0.4); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12;");
    }

    private void setFaltaStyle() {
        this.btnCheck.setText("FALTA");
        this.btnCheck.setStyle("-fx-background-color: rgba(220, 38, 38, 0.2); -fx-text-fill: #f87171; -fx-border-color: rgba(220, 38, 38, 0.4); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12;");
    }

    @Override
    protected void updateItem(Track track, boolean empty) {
        super.updateItem(track, empty);

        if (empty || track == null) {
            this.currentTrack = null;
            setText(null);
            setGraphic(null);
        } else {
            this.currentTrack = track;
            this.titleLabel.setText(track.title());
            this.artistLabel.setText(track.artist() + "  •  " + track.album());

            if (libraryService.isCached() && libraryService.checkAlbumExists(track.album())) {
                setDiscoStyle();
            } else {
                resetCheckButtonStyle();
            }

            if (wishlistService != null && wishlistService.isInWishlist(track.artist(), track.album())) {
                setWishlistActiveStyle();
            } else {
                resetWishlistButtonStyle();
            }

            this.coverView.setImage(null);
            if (track.imageUrl() != null && !track.imageUrl().isBlank()) {
                ImageCache.getInstance().loadImageAsync(track.imageUrl(), 48, 48, img -> {
                    if (this.currentTrack == track && img != null) {
                        this.coverView.setImage(img);
                    }
                });
            }

            setText(null);
            setGraphic(root);
        }
    }

    private void handleCheck() {
        if (currentTrack == null) return;
        btnCheck.setText("...");
        btnCheck.setDisable(true);

        WORKER_POOL.submit(() -> {
            boolean found = libraryService.checkAlbumExists(currentTrack.album());
            Platform.runLater(() -> {
                btnCheck.setDisable(false);
                if (found) {
                    setDiscoStyle();
                } else {
                    setFaltaStyle();
                }
            });
        });
    }

    private void handleDlSong() {
        if (currentTrack == null) return;
        btnDlSong.setDisable(true);
        btnDlSong.setText("...");
        String query = StringNormalizer.buildSearchQuery(currentTrack.artist(), currentTrack.title(), false, true);
        AppLogger.log("A pesquisar faixa Soulseek: " + query);
        WORKER_POOL.submit(() -> {
            boolean ok = slskdService.searchAndDownload(query, 0);
            Platform.runLater(() -> {
                btnDlSong.setDisable(false);
                btnDlSong.setText(ok ? "Iniciado" : "Faixa");
            });
        });
    }

    private void handleDlAlbum() {
        if (currentTrack == null) return;
        btnDlAlbum.setDisable(true);
        btnDlAlbum.setText("A procurar...");

        WORKER_POOL.submit(() -> {
            boolean ok = slskdService.searchAndDownloadAlbumWithFallback(
                    currentTrack.artist(),
                    currentTrack.album(),
                    List.of(currentTrack.title()),
                    null,
                    0
            );
            Platform.runLater(() -> {
                btnDlAlbum.setDisable(false);
                if (ok) {
                    btnDlAlbum.setText("Iniciado");
                    btnDlAlbum.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 14;");
                } else {
                    btnDlAlbum.setText("Falha");
                    btnDlAlbum.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14px; -fx-padding: 5 14;");
                }
            });
        });
    }
}
