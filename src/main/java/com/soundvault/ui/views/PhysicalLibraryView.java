package com.soundvault.ui.views;

import com.soundvault.model.Album;
import com.soundvault.model.PhysicalAlbum;
import com.soundvault.service.LibraryService;
import com.soundvault.service.NotificationService;
import com.soundvault.service.PhysicalLibraryService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.SpotifyService;
import com.soundvault.ui.components.ArtworkModal;
import com.soundvault.ui.components.ImageCache;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhysicalLibraryView extends VBox {

    private final PhysicalLibraryService physicalService;
    private final SpotifyService spotifyService;
    private final LibraryService libraryService;
    private final SlskdService slskdService;
    private final com.soundvault.service.WishlistService wishlistService;

    private final FlowPane gridContainer;
    private final TextField searchField;
    private final ComboBox<String> filterCombo;
    private final Label lblTotalCount;
    private final Label lblRipCount;
    private final Label lblCoverCount;
    private final ProgressBar syncProgressBar;
    private final Button btnSyncCovers;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PhysicalLibraryView(PhysicalLibraryService physicalService,
                               SpotifyService spotifyService,
                               LibraryService libraryService,
                               SlskdService slskdService) {
        this(physicalService, spotifyService, libraryService, slskdService, null);
    }

    public PhysicalLibraryView(PhysicalLibraryService physicalService,
                               SpotifyService spotifyService,
                               LibraryService libraryService,
                               SlskdService slskdService,
                               com.soundvault.service.WishlistService wishlistService) {
        this.physicalService = physicalService;
        this.spotifyService = spotifyService;
        this.libraryService = libraryService;
        this.slskdService = slskdService;
        this.wishlistService = wishlistService;

        this.setSpacing(16);
        this.setPadding(new Insets(24));
        this.getStyleClass().add("main-background");

        // --- 1. CABEÇALHO & BADGES ---
        Label headerTitle = new Label("Biblioteca Física & CDs");
        headerTitle.getStyleClass().add("header-label");

        lblTotalCount = new Label("0 CDs");
        lblTotalCount.getStyleClass().add("badge-pill");

        lblRipCount = new Label("0 com Rip FLAC");
        lblRipCount.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");

        lblCoverCount = new Label("0 com Capa");
        lblCoverCount.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 12px;");

        HBox titleRow = new HBox(12, headerTitle, lblTotalCount, lblRipCount, lblCoverCount);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // --- 2. BARRA DE FERRAMENTAS & AÇÕES ---
        searchField = new TextField();
        searchField.setPromptText("Pesquisar por artista ou álbum em CD...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderCards());

        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("Todos os Álbuns", "Com Rip FLAC", "Sem Rip FLAC", "Com Capa", "Sem Capa");
        filterCombo.setValue("Todos os Álbuns");
        filterCombo.setOnAction(e -> renderCards());

        Button btnAddCd = new Button("+ Adicionar CD");
        btnAddCd.setStyle("-fx-background-color: -theme-btn-selected; -fx-text-fill: -theme-text-selected; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnAddCd.setOnAction(e -> showAddCdDialog());

        btnSyncCovers = new Button("Obter Capas Spotify");
        btnSyncCovers.getStyleClass().add("action-button");
        btnSyncCovers.setOnAction(e -> triggerCoverSync());

        Button btnCheckRips = new Button("Verificar Rips FLAC");
        btnCheckRips.setStyle("-fx-background-color: transparent; -fx-border-color: -theme-btn-hover; -fx-border-radius: 20px; -fx-text-fill: -theme-text-primary; -fx-font-size: 11.5px; -fx-padding: 6 12; -fx-cursor: hand;");
        btnCheckRips.setOnAction(e -> {
            libraryService.updateCache(false);
            renderCards();
        });

        syncProgressBar = new ProgressBar();
        syncProgressBar.setPrefWidth(120);
        syncProgressBar.setVisible(false);

        HBox toolbar = new HBox(10, searchField, filterCombo, btnAddCd, btnSyncCovers, btnCheckRips, syncProgressBar);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // --- 3. CONTAINER DE CARTÕES ---
        gridContainer = new FlowPane(16, 16);
        gridContainer.setAlignment(Pos.TOP_LEFT);
        gridContainer.setPadding(new Insets(6, 0, 14, 0));

        ScrollPane scrollPane = new ScrollPane(gridContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(titleRow, toolbar, scrollPane);

        renderCards();

        // Se houver CDs sem capa, sugere sincronização automática inicial
        long withoutCovers = physicalService.getAlbums().stream().filter(a -> a.imageUrl() == null || a.imageUrl().isBlank()).count();
        if (withoutCovers > 0) {
            triggerCoverSync();
        }
    }

    private void renderCards() {
        gridContainer.getChildren().clear();

        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String filter = filterCombo.getValue();

        List<PhysicalAlbum> all = physicalService.getAlbums();
        int total = all.size();
        int withRip = 0;
        int withCover = 0;

        for (PhysicalAlbum album : all) {
            boolean hasRip = physicalService.hasDigitalRip(album, libraryService);
            boolean hasCover = album.imageUrl() != null && !album.imageUrl().isBlank();

            if (hasRip) withRip++;
            if (hasCover) withCover++;

            // Filtro de texto
            if (!query.isEmpty()) {
                String fullText = (album.artist() + " " + album.title() + " " + album.notes()).toLowerCase();
                if (!fullText.contains(query)) {
                    continue;
                }
            }

            // Filtro de seleção
            if ("Com Rip FLAC".equals(filter) && !hasRip) continue;
            if ("Sem Rip FLAC".equals(filter) && hasRip) continue;
            if ("Com Capa".equals(filter) && !hasCover) continue;
            if ("Sem Capa".equals(filter) && hasCover) continue;

            gridContainer.getChildren().add(createAlbumCard(album, hasRip));
        }

        lblTotalCount.setText(total + " CDs");
        lblRipCount.setText(withRip + " com Rip FLAC");
        lblCoverCount.setText(withCover + " com Capa");
    }

    private VBox createAlbumCard(PhysicalAlbum album, boolean hasRip) {
        VBox card = new VBox(6);
        card.setPrefWidth(170);
        card.setMaxWidth(170);
        card.getStyleClass().add("card-frame");
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.TOP_CENTER);

        // --- CAPA OU PLACEHOLDER DE CD ---
        StackPane coverStack = new StackPane();
        coverStack.setPrefSize(150, 150);
        coverStack.setMaxSize(150, 150);

        ImageView coverImg = new ImageView();
        coverImg.setFitWidth(150);
        coverImg.setFitHeight(150);
        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        coverImg.setClip(clip);

        if (album.imageUrl() != null && !album.imageUrl().isBlank()) {
            ImageCache.getInstance().loadImageAsync(album.imageUrl(), 150, 150, img -> {
                if (img != null) coverImg.setImage(img);
            });
            coverStack.getChildren().add(coverImg);
        } else {
            // Placeholder estilizado de disco de vinil / CD acústico
            StackPane discPlaceholder = createCdPlaceholder(album.artist());
            coverStack.getChildren().add(discPlaceholder);
        }

        // Clique na capa abre o visualizador de arte em alta resolução
        coverStack.setStyle("-fx-cursor: hand;");
        coverStack.setOnMouseClicked(e -> {
            if (this.getScene() != null) {
                Pane rootPane = (Pane) this.getScene().getRoot();
                ArtworkModal.show(rootPane, album.title(), album.artist(), album.imageUrl(), album.releaseYear(), album.format(), wishlistService, slskdService);
            }
        });

        // Tag do Formato no canto superior direito (ex: "CD")
        Label formatTag = new Label(album.format());
        formatTag.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-text-fill: -theme-text-accent; -fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-border-color: derive(-theme-text-accent, -50%); -fx-border-radius: 4px;");
        StackPane.setAlignment(formatTag, Pos.TOP_RIGHT);
        StackPane.setMargin(formatTag, new Insets(6));
        coverStack.getChildren().add(formatTag);

        // --- TÍTULO E ARTISTA ---
        Label titleLbl = new Label(album.title());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: #ffffff;");
        titleLbl.setMaxWidth(150);
        titleLbl.setTooltip(new Tooltip(album.title()));

        Label artistLbl = new Label(album.artist());
        artistLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -theme-text-accent;");
        artistLbl.setMaxWidth(150);
        artistLbl.setTooltip(new Tooltip(album.artist()));

        // --- LINHA DE STATUS & ANO ---
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label ripBadge = new Label(hasRip ? "FLAC LOCAL" : "FÍSICO");
        if (hasRip) {
            ripBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4px;");
        } else {
            ripBadge.setStyle("-fx-background-color: derive(-theme-card-bg, 15%); -fx-text-fill: derive(-theme-text-primary, -35%); -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4px;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label yearLbl = new Label(album.releaseYear() != null && !album.releaseYear().isBlank() ? album.releaseYear() : "");
        yearLbl.setStyle("-fx-font-size: 9.5px; -fx-text-fill: derive(-theme-text-primary, -40%);");

        statusRow.getChildren().addAll(ripBadge, spacer, yearLbl);

        // --- AÇÕES DO CARTÃO ---
        HBox actionsRow = new HBox(5);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.setPadding(new Insets(4, 0, 0, 0));

        Button btnCover = new Button("Buscar Capa");
        btnCover.setStyle("-fx-background-color: transparent; -fx-border-color: derive(-theme-card-bg, 30%); -fx-border-radius: 4px; -fx-text-fill: -theme-text-primary; -fx-font-size: 10px; -fx-padding: 2 5; -fx-cursor: hand;");
        btnCover.setOnAction(e -> searchSingleCover(album));

        Button btnSlsk = new Button("Baixar FLAC");
        btnSlsk.setStyle("-fx-background-color: transparent; -fx-border-color: #f97316; -fx-border-radius: 4px; -fx-text-fill: #f97316; -fx-font-size: 10px; -fx-padding: 2 5; -fx-cursor: hand;");
        btnSlsk.setTooltip(new Tooltip("Descarregar álbum em FLAC pelo Soulseek com verificação"));
        btnSlsk.setOnAction(e -> {
            btnSlsk.setDisable(true);
            btnSlsk.setText("A procurar...");
            executor.submit(() -> {
                boolean ok = slskdService.searchAndDownloadAlbumWithFallback(
                        album.artist(),
                        album.title(),
                        java.util.Collections.emptyList(),
                        spotifyService,
                        album.totalTracks()
                );
                Platform.runLater(() -> {
                    btnSlsk.setDisable(false);
                    if (ok) {
                        btnSlsk.setText("Iniciado");
                        btnSlsk.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 2 5; -fx-background-radius: 4px;");
                    } else {
                        btnSlsk.setText("Não achou");
                        btnSlsk.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 2 5; -fx-background-radius: 4px;");
                    }
                });
            });
        });

        // Botão Wishlist ao lado de Download
        boolean inWishlist = wishlistService != null && wishlistService.isInWishlist(album.artist(), album.title());
        Button btnWishlist = new Button(inWishlist ? "★ Wish" : "+ Wish");
        btnWishlist.setStyle(inWishlist
                ? "-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 5; -fx-background-radius: 4px; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-border-color: #f59e0b; -fx-border-radius: 4px; -fx-text-fill: #f59e0b; -fx-font-size: 10px; -fx-padding: 2 5; -fx-cursor: hand;");
        btnWishlist.setOnAction(e -> {
            if (wishlistService != null) {
                boolean added = wishlistService.toggleWishlist(album.artist(), album.title(), album.imageUrl(), album.spotifyId(), album.releaseYear(), album.notes());
                if (added) {
                    btnWishlist.setText("★ Wish");
                    btnWishlist.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 5; -fx-background-radius: 4px; -fx-cursor: hand;");
                    NotificationService.getInstance().notifyWishlistAdded(album.artist() + " - " + album.title());
                } else {
                    btnWishlist.setText("+ Wish");
                    btnWishlist.setStyle("-fx-background-color: transparent; -fx-border-color: #f59e0b; -fx-border-radius: 4px; -fx-text-fill: #f59e0b; -fx-font-size: 10px; -fx-padding: 2 5; -fx-cursor: hand;");
                }
            }
        });

        Button btnDelete = new Button("X");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 4;");
        btnDelete.setTooltip(new Tooltip("Remover da coleção"));
        btnDelete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remover '" + album.title() + "' da biblioteca física?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    physicalService.removeAlbum(album.id());
                    renderCards();
                }
            });
        });

        actionsRow.getChildren().addAll(btnCover, btnSlsk, btnWishlist, btnDelete);

        card.getChildren().addAll(coverStack, titleLbl, artistLbl, statusRow, actionsRow);
        return card;
    }

    private StackPane createCdPlaceholder(String artistName) {
        StackPane pane = new StackPane();
        pane.setPrefSize(150, 150);
        pane.setStyle("-fx-background-color: #14171f; -fx-background-radius: 10px; -fx-border-color: #232936; -fx-border-radius: 10px;");

        // Círculos concêntricos de CD
        Circle outer = new Circle(55);
        outer.setStyle("-fx-fill: transparent; -fx-stroke: #2a3142; -fx-stroke-width: 1.5px; -fx-stroke-dash-array: 6 3;");

        Circle middle = new Circle(32);
        middle.setStyle("-fx-fill: #0c0e14; -fx-stroke: #3b4252; -fx-stroke-width: 1px;");

        Circle inner = new Circle(12);
        inner.setStyle("-fx-fill: #14171f; -fx-stroke: -theme-text-accent; -fx-stroke-width: 1.5px;");

        Label label = new Label(artistName != null && !artistName.isBlank() ? artistName.substring(0, Math.min(12, artistName.length())) : "CD");
        label.setStyle("-fx-font-size: 8.5px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent;");
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
        StackPane.setMargin(label, new Insets(0, 0, 10, 0));

        pane.getChildren().addAll(outer, middle, inner, label);
        return pane;
    }

    private void searchSingleCover(PhysicalAlbum album) {
        executor.submit(() -> {
            Optional<Album> found = spotifyService.searchAlbum(album.artist(), album.title());
            Platform.runLater(() -> {
                if (found.isPresent()) {
                    Album sp = found.get();
                    String year = (sp.releaseDate() != null && sp.releaseDate().length() >= 4) ? sp.releaseDate().substring(0, 4) : "";
                    PhysicalAlbum enriched = album.withSpotifyDetails(sp.imageUrl(), sp.id(), year, sp.totalTracks());
                    physicalService.updateAlbum(enriched);
                    renderCards();
                } else {
                    Alert notFound = new Alert(Alert.AlertType.INFORMATION, "Capa não encontrada no Spotify para: " + album.artist() + " - " + album.title());
                    notFound.setHeaderText(null);
                    notFound.show();
                }
            });
        });
    }

    private void triggerCoverSync() {
        btnSyncCovers.setDisable(true);
        syncProgressBar.setVisible(true);

        physicalService.syncMissingCovers(
                spotifyService,
                updated -> Platform.runLater(this::renderCards),
                () -> Platform.runLater(() -> {
                    btnSyncCovers.setDisable(false);
                    syncProgressBar.setVisible(false);
                    renderCards();
                })
        );
    }

    private void showAddCdDialog() {
        Dialog<PhysicalAlbum> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Álbum em CD");
        dialog.setHeaderText("Registar novo exemplar na biblioteca física");

        ButtonType btnAddType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAddType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField tfArtist = new TextField();
        tfArtist.setPromptText("ex: Pink Floyd");

        TextField tfTitle = new TextField();
        tfTitle.setPromptText("ex: Animals");

        ComboBox<String> cbFormat = new ComboBox<>();
        cbFormat.getItems().addAll("CD", "Vinil", "Cassete", "Box Set");
        cbFormat.setValue("CD");

        TextField tfNotes = new TextField();
        tfNotes.setPromptText("ex: Digipak, Edição Remasterizada");

        CheckBox chkFetchSpotify = new CheckBox("Procurar capa no Spotify de imediato");
        chkFetchSpotify.setSelected(true);

        grid.add(new Label("Artista:"), 0, 0);
        grid.add(tfArtist, 1, 0);
        grid.add(new Label("Álbum:"), 0, 1);
        grid.add(tfTitle, 1, 1);
        grid.add(new Label("Formato:"), 0, 2);
        grid.add(cbFormat, 1, 2);
        grid.add(new Label("Notas:"), 0, 3);
        grid.add(tfNotes, 1, 3);
        grid.add(chkFetchSpotify, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAddType) {
                String artist = tfArtist.getText().trim();
                String title = tfTitle.getText().trim();
                if (!artist.isEmpty() && !title.isEmpty()) {
                    PhysicalAlbum added = physicalService.addAlbum(artist, title, cbFormat.getValue(), tfNotes.getText().trim());
                    if (chkFetchSpotify.isSelected()) {
                        searchSingleCover(added);
                    }
                    return added;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> renderCards());
    }
}
