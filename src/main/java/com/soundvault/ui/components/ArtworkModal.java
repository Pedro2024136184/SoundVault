package com.soundvault.ui.components;

import com.soundvault.service.NotificationService;
import com.soundvault.service.SlskdService;
import com.soundvault.service.WishlistService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class ArtworkModal {

    public static void show(Pane rootPane,
                            String title,
                            String artist,
                            String imageUrl,
                            String releaseYear,
                            String format,
                            WishlistService wishlistService,
                            SlskdService slskdService) {
        if (rootPane == null) return;

        // Fundo escurecido semi-transparente
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(5, 7, 12, 0.88);");
        overlay.setPrefSize(rootPane.getWidth(), rootPane.getHeight());

        // Caixa de Conteúdo Centralizada
        VBox dialogBox = new VBox(14);
        dialogBox.setMaxSize(480, 620);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setStyle("-fx-background-color: -theme-card-bg; -fx-background-radius: 16px; -fx-border-color: derive(-theme-card-bg, 30%); -fx-border-radius: 16px; -fx-border-width: 1.5px; -fx-padding: 20 24; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 35, 0, 0, 10);");

        // Top Bar com Tag e Botão Fechar
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label tag = new Label("HI-RES ALBUM ARTWORK");
        tag.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent; -fx-background-color: derive(-theme-card-bg, 18%); -fx-padding: 3 8; -fx-background-radius: 4px; -fx-border-color: derive(-theme-text-accent, -50%); -fx-border-radius: 4px;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 6;");
        btnClose.setOnAction(e -> rootPane.getChildren().remove(overlay));

        topBar.getChildren().addAll(tag, topSpacer, btnClose);

        // Capa Grande em Alta Resolução (380x380)
        StackPane coverContainer = new StackPane();
        coverContainer.setPrefSize(380, 380);
        coverContainer.setMaxSize(380, 380);

        ImageView bigCover = new ImageView();
        bigCover.setFitWidth(380);
        bigCover.setFitHeight(380);
        Rectangle clip = new Rectangle(380, 380);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        bigCover.setClip(clip);

        if (imageUrl != null && !imageUrl.isBlank()) {
            ImageCache.getInstance().loadImageAsync(imageUrl, 640, 640, img -> {
                if (img != null) bigCover.setImage(img);
            });
            coverContainer.getChildren().add(bigCover);
        } else {
            // Arte alternativa para disco físico sem imagem de capa
            StackPane disc = createLargeDisc(artist, title);
            coverContainer.getChildren().add(disc);
        }

        // Títulos e Metadados
        VBox metaBox = new VBox(4);
        metaBox.setAlignment(Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        titleLbl.setMaxWidth(420);
        titleLbl.setAlignment(Pos.CENTER);

        Label artistLbl = new Label(artist);
        artistLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent;");
        artistLbl.setMaxWidth(420);
        artistLbl.setAlignment(Pos.CENTER);

        String metaText = (format != null && !format.isBlank() ? format : "Álbum");
        if (releaseYear != null && !releaseYear.isBlank()) {
            metaText += "  •  Ano " + releaseYear;
        }
        Label subMeta = new Label(metaText);
        subMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: derive(-theme-text-primary, -35%);");

        metaBox.getChildren().addAll(titleLbl, artistLbl, subMeta);

        // Barra de Ações Rápidas
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8, 0, 0, 0));

        boolean inWishlist = wishlistService != null && wishlistService.isInWishlist(artist, title);
        Button btnWishlist = new Button(inWishlist ? "★ Na Wishlist" : "+ Wishlist");
        btnWishlist.setStyle(inWishlist
                ? "-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 20px; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-border-color: #f59e0b; -fx-border-radius: 20px; -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-cursor: hand;");

        btnWishlist.setOnAction(e -> {
            if (wishlistService != null) {
                boolean added = wishlistService.toggleWishlist(artist, title, imageUrl, "", releaseYear, "");
                if (added) {
                    btnWishlist.setText("★ Na Wishlist");
                    btnWishlist.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 20px; -fx-cursor: hand;");
                    NotificationService.getInstance().notifyWishlistAdded(artist + " - " + title);
                } else {
                    btnWishlist.setText("+ Wishlist");
                    btnWishlist.setStyle("-fx-background-color: transparent; -fx-border-color: #f59e0b; -fx-border-radius: 20px; -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-cursor: hand;");
                }
            }
        });

        Button btnSlsk = new Button("Baixar FLAC (Soulseek)");
        btnSlsk.setStyle("-fx-background-color: linear-gradient(to right, #ea580c, #f97316); -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnSlsk.setOnAction(e -> {
            if (slskdService != null) {
                btnSlsk.setDisable(true);
                btnSlsk.setText("A procurar...");
                Thread.ofVirtual().start(() -> {
                    boolean ok = slskdService.searchAndDownloadAlbumWithFallback(artist, title, java.util.Collections.emptyList(), null, 0);
                    javafx.application.Platform.runLater(() -> {
                        btnSlsk.setDisable(false);
                        if (ok) {
                            btnSlsk.setText("Download Iniciado");
                            btnSlsk.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18; -fx-background-radius: 20px;");
                        } else {
                            btnSlsk.setText("Falha na Pesquisa");
                            btnSlsk.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18; -fx-background-radius: 20px;");
                        }
                    });
                });
            }
        });

        actions.getChildren().addAll(btnWishlist, btnSlsk);

        dialogBox.getChildren().addAll(topBar, coverContainer, metaBox, actions);
        overlay.getChildren().add(dialogBox);

        // Fechar ao clicar fora da caixa
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                rootPane.getChildren().remove(overlay);
            }
        });

        // Fechar ao pressionar ESC
        overlay.setFocusTraversable(true);
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                rootPane.getChildren().remove(overlay);
            }
        });

        rootPane.getChildren().add(overlay);
        overlay.requestFocus();
    }

    private static StackPane createLargeDisc(String artist, String title) {
        StackPane pane = new StackPane();
        pane.setPrefSize(380, 380);
        pane.setStyle("-fx-background-color: #0c0e14; -fx-background-radius: 12px; -fx-border-color: #1e2433; -fx-border-radius: 12px;");

        Circle c1 = new Circle(140);
        c1.setStyle("-fx-fill: transparent; -fx-stroke: #1f2636; -fx-stroke-width: 2px; -fx-stroke-dash-array: 10 5;");

        Circle c2 = new Circle(95);
        c2.setStyle("-fx-fill: #11151f; -fx-stroke: #2b354a; -fx-stroke-width: 1.5px;");

        Circle c3 = new Circle(40);
        c3.setStyle("-fx-fill: -theme-card-bg; -fx-stroke: -theme-text-accent; -fx-stroke-width: 2px;");

        Circle center = new Circle(14);
        center.setStyle("-fx-fill: #050608;");

        VBox text = new VBox(2);
        text.setAlignment(Pos.CENTER);
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label lArtist = new Label(artist);
        lArtist.setStyle("-fx-font-size: 11px; -fx-text-fill: -theme-text-accent;");
        text.getChildren().addAll(lTitle, lArtist);
        StackPane.setAlignment(text, Pos.BOTTOM_CENTER);
        StackPane.setMargin(text, new Insets(0, 0, 25, 0));

        pane.getChildren().addAll(c1, c2, c3, center, text);
        return pane;
    }
}
