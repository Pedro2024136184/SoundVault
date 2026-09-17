package com.soundvault.ui.views;

import com.soundvault.model.Album;
import com.soundvault.model.WishlistItem;
import com.soundvault.service.NotificationService;
import com.soundvault.service.PhysicalLibraryService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.SpotifyService;
import com.soundvault.service.WishlistService;
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

public class WishlistView extends VBox {

    private final WishlistService wishlistService;
    private final PhysicalLibraryService physicalService;
    private final SpotifyService spotifyService;
    private final SlskdService slskdService;

    private final FlowPane gridContainer;
    private final TextField searchField;
    private final ComboBox<String> filterCombo;
    private final Label lblTotal;
    private final Label lblPending;
    private final Label lblAcquired;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WishlistView(WishlistService wishlistService,
                        PhysicalLibraryService physicalService,
                        SpotifyService spotifyService,
                        SlskdService slskdService) {
        this.wishlistService = wishlistService;
        this.physicalService = physicalService;
        this.spotifyService = spotifyService;
        this.slskdService = slskdService;

        this.setSpacing(16);
        this.setPadding(new Insets(24));
        this.getStyleClass().add("main-background");

        // --- 1. CABEÇALHO ---
        Label title = new Label("Lista de Desejos (Wishlist)");
        title.getStyleClass().add("header-label");

        lblTotal = new Label("0 Itens");
        lblTotal.getStyleClass().add("badge-pill");

        lblPending = new Label("0 Pendentes");
        lblPending.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 12px;");

        lblAcquired = new Label("0 Adquiridos");
        lblAcquired.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");

        HBox titleRow = new HBox(12, title, lblTotal, lblPending, lblAcquired);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // --- 2. BARRA DE FERRAMENTAS ---
        searchField = new TextField();
        searchField.setPromptText("Pesquisar na Wishlist...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> renderCards());

        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("Todos", "Pendentes", "Adquiridos", "CDs", "Vinil");
        filterCombo.setValue("Todos");
        filterCombo.setOnAction(e -> renderCards());

        Button btnAddManual = new Button("+ Adicionar Álbum");
        btnAddManual.setStyle("-fx-background-color: -theme-btn-selected; -fx-text-fill: -theme-text-selected; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnAddManual.setOnAction(e -> showAddDialog());

        HBox toolbar = new HBox(10, searchField, filterCombo, btnAddManual);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // --- 3. GRID DE CARTÕES ---
        gridContainer = new FlowPane(16, 16);
        gridContainer.setAlignment(Pos.TOP_LEFT);
        gridContainer.setPadding(new Insets(6, 0, 14, 0));

        ScrollPane scrollPane = new ScrollPane(gridContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(titleRow, toolbar, scrollPane);

        wishlistService.addOnItemAddedListener(item -> Platform.runLater(this::renderCards));

        renderCards();
    }

    public void renderCards() {
        gridContainer.getChildren().clear();

        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String filter = filterCombo.getValue();

        List<WishlistItem> all = wishlistService.getItems();
        int total = all.size();
        int pending = 0;
        int acquired = 0;

        for (WishlistItem item : all) {
            if (item.acquired()) acquired++;
            else pending++;

            if (!query.isEmpty()) {
                String match = (item.artist() + " " + item.title() + " " + item.notes()).toLowerCase();
                if (!match.contains(query)) continue;
            }

            if ("Pendentes".equals(filter) && item.acquired()) continue;
            if ("Adquiridos".equals(filter) && !item.acquired()) continue;
            if ("CDs".equals(filter) && !"CD".equalsIgnoreCase(item.format())) continue;
            if ("Vinil".equals(filter) && !"Vinil".equalsIgnoreCase(item.format())) continue;

            gridContainer.getChildren().add(createWishlistCard(item));
        }

        lblTotal.setText(total + " Itens");
        lblPending.setText(pending + " Pendentes");
        lblAcquired.setText(acquired + " Adquiridos");
    }

    private VBox createWishlistCard(WishlistItem item) {
        VBox card = new VBox(6);
        card.setPrefWidth(170);
        card.setMaxWidth(170);
        card.getStyleClass().add("card-frame");
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.TOP_CENTER);

        // Capa com clique para abrir o Visualizador em Alta Resolução
        StackPane coverStack = new StackPane();
        coverStack.setPrefSize(150, 150);
        coverStack.setMaxSize(150, 150);
        coverStack.setStyle("-fx-cursor: hand;");

        ImageView coverImg = new ImageView();
        coverImg.setFitWidth(150);
        coverImg.setFitHeight(150);
        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        coverImg.setClip(clip);

        if (item.imageUrl() != null && !item.imageUrl().isBlank()) {
            ImageCache.getInstance().loadImageAsync(item.imageUrl(), 150, 150, img -> {
                if (img != null) coverImg.setImage(img);
            });
            coverStack.getChildren().add(coverImg);
        } else {
            coverStack.getChildren().add(createDiscPlaceholder(item.artist()));
        }

        // Clique para abrir ArtworkModal
        coverStack.setOnMouseClicked(e -> {
            Pane rootPane = (Pane) this.getScene().getRoot();
            ArtworkModal.show(rootPane, item.title(), item.artist(), item.imageUrl(), item.releaseYear(), item.format(), wishlistService, slskdService);
        });

        // Tag de formato no canto
        Label formatTag = new Label(item.format());
        formatTag.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-text-fill: -theme-text-accent; -fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4px;");
        StackPane.setAlignment(formatTag, Pos.TOP_RIGHT);
        StackPane.setMargin(formatTag, new Insets(6));
        coverStack.getChildren().add(formatTag);

        Label titleLbl = new Label(item.title());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: #ffffff;");
        titleLbl.setMaxWidth(150);
        titleLbl.setTooltip(new Tooltip(item.title()));

        Label artistLbl = new Label(item.artist());
        artistLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -theme-text-accent;");
        artistLbl.setMaxWidth(150);
        artistLbl.setTooltip(new Tooltip(item.artist()));

        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(item.acquired() ? "ADQUIRIDO" : "PENDENTE");
        if (item.acquired()) {
            badge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4px;");
        } else {
            badge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.2); -fx-text-fill: #f59e0b; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4px;");
        }

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label yearLbl = new Label(item.releaseYear());
        yearLbl.setStyle("-fx-font-size: 9.5px; -fx-text-fill: derive(-theme-text-primary, -40%);");

        statusRow.getChildren().addAll(badge, sp, yearLbl);

        // Ações do Cartão
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button btnAcquire = new Button(item.acquired() ? "Desmarcar" : "Comprado!");
        btnAcquire.setStyle("-fx-background-color: transparent; -fx-border-color: derive(-theme-card-bg, 35%); -fx-border-radius: 4px; -fx-text-fill: " + (item.acquired() ? "#94a3b8" : "#34d399") + "; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-cursor: hand;");
        btnAcquire.setOnAction(e -> {
            boolean next = !item.acquired();
            wishlistService.markAsAcquired(item.id(), next, physicalService);
            if (next) {
                NotificationService.getInstance().showNotification("Álbum Adquirido", item.artist() + " - " + item.title() + " adicionado à tua Biblioteca Física!", java.awt.TrayIcon.MessageType.INFO);
            }
            renderCards();
        });

        Button btnSlsk = new Button("Baixar FLAC");
        btnSlsk.setStyle("-fx-background-color: transparent; -fx-border-color: #f97316; -fx-border-radius: 4px; -fx-text-fill: #f97316; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-cursor: hand;");
        btnSlsk.setTooltip(new Tooltip("Descarregar álbum em FLAC pelo Soulseek com verificação"));
        btnSlsk.setOnAction(e -> {
            btnSlsk.setDisable(true);
            btnSlsk.setText("...");
            executor.submit(() -> {
                boolean ok = slskdService.searchAndDownloadAlbumWithFallback(
                        item.artist(),
                        item.title(),
                        java.util.Collections.emptyList(),
                        spotifyService,
                        0
                );
                Platform.runLater(() -> {
                    btnSlsk.setDisable(false);
                    btnSlsk.setText(ok ? "Iniciado" : "Falha");
                    if (ok) {
                        btnSlsk.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px;");
                    }
                });
            });
        });

        Button btnDel = new Button("X");
        btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 5;");
        btnDel.setOnAction(e -> {
            wishlistService.removeItem(item.id());
            renderCards();
        });

        actions.getChildren().addAll(btnAcquire, btnSlsk, btnDel);

        card.getChildren().addAll(coverStack, titleLbl, artistLbl, statusRow, actions);
        return card;
    }

    private StackPane createDiscPlaceholder(String artistName) {
        StackPane pane = new StackPane();
        pane.setPrefSize(150, 150);
        pane.setStyle("-fx-background-color: #11141c; -fx-background-radius: 10px; -fx-border-color: #212635; -fx-border-radius: 10px;");

        Circle outer = new Circle(52);
        outer.setStyle("-fx-fill: transparent; -fx-stroke: #252c3d; -fx-stroke-width: 1.5px; -fx-stroke-dash-array: 6 3;");

        Circle middle = new Circle(28);
        middle.setStyle("-fx-fill: #0c0e14; -fx-stroke: #333d54; -fx-stroke-width: 1px;");

        Circle inner = new Circle(10);
        inner.setStyle("-fx-fill: #11141c; -fx-stroke: -theme-text-accent; -fx-stroke-width: 1.5px;");

        Label label = new Label(artistName != null && !artistName.isBlank() ? artistName.substring(0, Math.min(10, artistName.length())) : "WISH");
        label.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent;");
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
        StackPane.setMargin(label, new Insets(0, 0, 10, 0));

        pane.getChildren().addAll(outer, middle, inner, label);
        return pane;
    }

    private void showAddDialog() {
        Dialog<WishlistItem> dialog = new Dialog<>();
        dialog.setTitle("Adicionar à Wishlist");
        dialog.setHeaderText("Registar álbum na Lista de Desejos");

        ButtonType btnAdd = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAdd, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField tfArtist = new TextField();
        tfArtist.setPromptText("ex: Pink Floyd");

        TextField tfTitle = new TextField();
        tfTitle.setPromptText("ex: Wish You Were Here");

        ComboBox<String> cbFormat = new ComboBox<>();
        cbFormat.getItems().addAll("CD", "Vinil", "Cassete", "Box Set");
        cbFormat.setValue("CD");

        TextField tfNotes = new TextField();
        tfNotes.setPromptText("ex: Edição 1975, Digipak");

        grid.add(new Label("Artista:"), 0, 0);
        grid.add(tfArtist, 1, 0);
        grid.add(new Label("Álbum:"), 0, 1);
        grid.add(tfTitle, 1, 1);
        grid.add(new Label("Formato:"), 0, 2);
        grid.add(cbFormat, 1, 2);
        grid.add(new Label("Notas:"), 0, 3);
        grid.add(tfNotes, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogBtn -> {
            if (dialogBtn == btnAdd) {
                String artist = tfArtist.getText().trim();
                String title = tfTitle.getText().trim();
                if (!artist.isEmpty() && !title.isEmpty()) {
                    WishlistItem item = wishlistService.addItem(artist, title, cbFormat.getValue(), tfNotes.getText().trim(), "", "", "");
                    NotificationService.getInstance().notifyWishlistAdded(artist + " - " + title);

                    // Procura capa no Spotify em segundo plano
                    executor.submit(() -> {
                        Optional<Album> sp = spotifyService.searchAlbum(artist, title);
                        if (sp.isPresent()) {
                            Album alb = sp.get();
                            String year = (alb.releaseDate() != null && alb.releaseDate().length() >= 4) ? alb.releaseDate().substring(0, 4) : "";
                            WishlistItem updated = item.withSpotifyDetails(alb.imageUrl(), alb.id(), year);
                            wishlistService.removeItem(item.id());
                            wishlistService.addItem(updated.artist(), updated.title(), updated.format(), updated.notes(), updated.imageUrl(), updated.spotifyId(), updated.releaseYear());
                            Platform.runLater(this::renderCards);
                        }
                    });
                    return item;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> renderCards());
    }
}
