package com.soundvault.ui.views;

import com.soundvault.service.SlskdService;
import com.soundvault.service.SlskdService.RelicCandidate;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.soundvault.service.WishlistService;

public class RelicExplorerView extends VBox {

    private final SlskdService slskdService;
    private final WishlistService wishlistService;
    private final TextField artistField;
    private final ComboBox<String> categoryCombo;
    private final Button btnSearch;
    private final ProgressBar progressBar;
    private final Label resultsSummary;
    private final VBox resultsContainer;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public RelicExplorerView(SlskdService slskdService) {
        this(slskdService, null);
    }

    public RelicExplorerView(SlskdService slskdService, WishlistService wishlistService) {
        this.slskdService = slskdService;
        this.wishlistService = wishlistService;

        this.setSpacing(16);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Relic Explorer");
        header.getStyleClass().add("header-label");

        Label desc = new Label("Explorador de raridades musicais no Soulseek: vinis, demos, gravações raras, bootlegs e edições japonesas.");
        desc.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 13px;");

        // Barra de Pesquisa e Filtros
        artistField = new TextField();
        artistField.setPromptText("Nome do Artista ou Banda (ex: Pink Floyd, Deftones, Aphex Twin)...");
        artistField.setPrefWidth(360);
        artistField.setOnAction(e -> triggerSearch());

        categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(
                "Todas as Raridades",
                "Vinil (Vinyl Rip)",
                "Demo / Tape",
                "Unreleased / Raros",
                "Bootleg / Live",
                "Edição Japonesa"
        );
        categoryCombo.setValue("Todas as Raridades");

        btnSearch = new Button("Procurar Raridades");
        btnSearch.getStyleClass().add("action-button");
        btnSearch.setOnAction(e -> triggerSearch());

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(120);
        progressBar.setVisible(false);

        HBox searchRow = new HBox(12, artistField, categoryCombo, btnSearch, progressBar);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        resultsSummary = new Label("Introduz o nome de um artista para pesquisar raridades disponíveis na rede.");
        resultsSummary.setStyle("-fx-text-fill: derive(-theme-text-primary, -35%); -fx-font-size: 12.5px; -fx-font-style: italic;");

        resultsContainer = new VBox(10);
        resultsContainer.setPadding(new Insets(5));

        ScrollPane scroll = new ScrollPane(resultsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        this.getChildren().addAll(header, desc, searchRow, resultsSummary, scroll);
    }

    private void triggerSearch() {
        String artist = artistField.getText().trim();
        if (artist.isBlank()) return;

        btnSearch.setDisable(true);
        progressBar.setVisible(true);
        resultsContainer.getChildren().clear();
        resultsSummary.setText("A sondar ficheiros e pares na rede Soulseek para '" + artist + "'...");

        String selectedCat = categoryCombo.getValue();
        List<String> querySuffixes = new ArrayList<>();
        if ("Todas as Raridades".equals(selectedCat)) {
            querySuffixes.add("vinyl");
            querySuffixes.add("demo");
            querySuffixes.add("bootleg");
            querySuffixes.add("japanese edition");
        } else if ("Vinil (Vinyl Rip)".equals(selectedCat)) {
            querySuffixes.add("vinyl");
        } else if ("Demo / Tape".equals(selectedCat)) {
            querySuffixes.add("demo");
        } else if ("Unreleased / Raros".equals(selectedCat)) {
            querySuffixes.add("unreleased");
        } else if ("Bootleg / Live".equals(selectedCat)) {
            querySuffixes.add("bootleg");
        } else if ("Edição Japonesa".equals(selectedCat)) {
            querySuffixes.add("japanese edition");
        }

        executor.submit(() -> {
            String cleanArt = StringNormalizer.cleanArtist(artist);
            List<RelicCandidate> allFound = new ArrayList<>();

            for (String suffix : querySuffixes) {
                String query = cleanArt + " " + suffix + " flac";
                AppLogger.log("Relic Explorer a pesquisar: " + query);
                List<RelicCandidate> candidates = slskdService.searchRelics(query, 25, suffix.toUpperCase());
                allFound.addAll(candidates);
            }

            Platform.runLater(() -> {
                btnSearch.setDisable(false);
                progressBar.setVisible(false);

                if (allFound.isEmpty()) {
                    resultsSummary.setText("Nenhuma raridade encontrada para '" + artist + "'. Tenta outro termo ou pesquisa sem filtro específico.");
                    return;
                }

                resultsSummary.setText(allFound.size() + " raridades encontradas para '" + artist + "'. Escolhe quais queres descarregar:");

                for (RelicCandidate r : allFound) {
                    resultsContainer.getChildren().add(createRelicRow(r));
                }
            });
        });
    }

    private HBox createRelicRow(RelicCandidate relic) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card-frame");

        // Badge da Categoria
        Label catBadge = new Label(relic.tagType());
        catBadge.setStyle("-fx-background-color: derive(-theme-btn-primary, -20%); -fx-text-fill: -theme-text-accent; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 4px;");

        // Info da Relíquia
        VBox infoBox = new VBox(4);
        Label titleLbl = new Label(relic.title());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #ffffff;");

        Label pathLbl = new Label(relic.folderPath());
        pathLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: derive(-theme-text-primary, -40%);");

        Label statsLbl = new Label(relic.flacFiles().size() + " faixas FLAC  •  " + relic.getFormattedSize() + "  •  Peer: " + relic.username() + " (" + relic.getFormattedSpeed() + ")");
        statsLbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: derive(-theme-text-primary, -20%);");

        infoBox.getChildren().addAll(titleLbl, pathLbl, statsLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Estado do slot
        Label slotLbl = new Label(relic.hasFreeSlots() ? "SLOT LIVRE" : "FILA");
        String slotColor = relic.hasFreeSlots() ? "#10b981" : "#f59e0b";
        slotLbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: " + slotColor + "; -fx-background-color: derive(-theme-card-bg, 15%); -fx-padding: 4 8; -fx-background-radius: 6px;");

        // Botão Wishlist
        Button btnWish = new Button("+ Wishlist");
        String currentArtist = artistField.getText().trim();
        String candidateTitle = relic.title();
        boolean inWish = wishlistService != null && wishlistService.isInWishlist(currentArtist, candidateTitle);

        if (inWish) {
            btnWish.setText("★ Na Wishlist");
            btnWish.setStyle("-fx-background-color: -theme-btn-primary; -fx-text-fill: #ffffff; -fx-background-radius: 20px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
        } else {
            btnWish.setStyle("-fx-background-color: transparent; -fx-border-color: -theme-btn-primary; -fx-border-width: 1px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-text-fill: -theme-btn-primary; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
        }

        btnWish.setOnAction(e -> {
            if (wishlistService == null) return;
            String art = artistField.getText().trim();
            if (art.isBlank()) art = "Vários Artistas";
            boolean added = wishlistService.toggleWishlist(art, relic.title());
            if (added) {
                btnWish.setText("★ Na Wishlist");
                btnWish.setStyle("-fx-background-color: -theme-btn-primary; -fx-text-fill: #ffffff; -fx-background-radius: 20px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
                com.soundvault.service.NotificationService.getInstance().notifyWishlistAdded(art + " - " + relic.title());
            } else {
                btnWish.setText("+ Wishlist");
                btnWish.setStyle("-fx-background-color: transparent; -fx-border-color: -theme-btn-primary; -fx-border-width: 1px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-text-fill: -theme-btn-primary; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
            }
        });

        // Botão Descarregar
        Button btnDl = new Button("Descarregar");
        btnDl.getStyleClass().add("action-button");
        btnDl.setOnAction(e -> {
            btnDl.setDisable(true);
            btnDl.setText("A submeter...");

            executor.submit(() -> {
                boolean ok = slskdService.downloadPeerFiles(relic.username(), relic.flacFiles());
                Platform.runLater(() -> {
                    if (ok) {
                        btnDl.setText("Transferência Iniciada");
                        btnDl.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 20px;");
                    } else {
                        btnDl.setDisable(false);
                        btnDl.setText("Falha ao Enviar");
                        btnDl.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 20px;");
                    }
                });
            });
        });

        row.getChildren().addAll(catBadge, infoBox, spacer, slotLbl, btnWish, btnDl);
        return row;
    }
}
