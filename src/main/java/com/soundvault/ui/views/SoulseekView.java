package com.soundvault.ui.views;

import com.soundvault.config.AppConfig;
import com.soundvault.model.DownloadTransfer;
import com.soundvault.model.SearchHistoryItem;
import com.soundvault.service.SlskdService;
import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoulseekView extends VBox {

    private final SlskdService slskdService;
    private final VBox listContainer;
    private final ProgressBar progressBar;
    private String currentTab = "transfers";

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SoulseekView(SlskdService slskdService) {
        this.slskdService = slskdService;

        this.setSpacing(15);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Monitor Soulseek (slskd)");
        header.getStyleClass().add("header-label");

        Button btnTransfers = new Button("Transferências");
        btnTransfers.getStyleClass().add("action-button");
        btnTransfers.setOnAction(e -> showTransfers());

        Button btnSearches = new Button("Histórico de Pesquisas");
        btnSearches.getStyleClass().add("action-button");
        btnSearches.setOnAction(e -> showSearches());

        Button btnRefresh = new Button("Atualizar");
        btnRefresh.setStyle("-fx-background-color: #3a3a40; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 14px; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> {
            if ("transfers".equals(currentTab)) showTransfers();
            else showSearches();
        });

        Button btnOpenWeb = new Button("Abrir Web UI (Localhost)");
        btnOpenWeb.setStyle("-fx-background-color: transparent; -fx-border-color: -theme-text-accent; -fx-border-radius: 5px; -fx-text-fill: -theme-text-accent; -fx-padding: 5 10;");
        btnOpenWeb.setOnAction(e -> {
            try {
                String url = AppConfig.getInstance().getSlskdHost() + ":" + AppConfig.getInstance().getSlskdPort();
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception ignored) {}
        });

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(120);
        progressBar.setVisible(false);

        HBox bar = new HBox(10, btnTransfers, btnSearches, btnRefresh, btnOpenWeb, progressBar);
        bar.setAlignment(Pos.CENTER_LEFT);

        listContainer = new VBox(6);
        listContainer.setPadding(new Insets(5));

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        this.getChildren().addAll(header, bar, scroll);

        showTransfers();
    }

    public void showTransfers() {
        this.currentTab = "transfers";
        listContainer.getChildren().clear();
        progressBar.setVisible(true);

        executor.submit(() -> {
            List<DownloadTransfer> transfers = slskdService.getAllTransfers();
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                listContainer.getChildren().clear();

                if (transfers.isEmpty()) {
                    Label lbl = new Label("Nenhuma transferência ativa de momento.");
                    lbl.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 14px;");
                    listContainer.getChildren().add(lbl);
                    return;
                }

                for (DownloadTransfer t : transfers) {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("card-frame");

                    Label userLbl = new Label(t.username() + "  •  " + t.getCleanFileName());
                    userLbl.setStyle("-fx-font-weight: bold;");

                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);

                    String stateColor = switch (t.state()) {
                        case "Completed" -> "#1DB954";
                        case "Queued", "QueuedRemotely" -> "#ffaa00";
                        case "InProgress", "Requested" -> "-theme-text-accent";
                        default -> "#ff5555";
                    };

                    Label stateLbl = new Label(t.state());
                    stateLbl.setStyle("-fx-text-fill: " + stateColor + "; -fx-font-weight: bold;");

                    row.getChildren().addAll(userLbl, sp, stateLbl);
                    listContainer.getChildren().add(row);
                }
            });
        });
    }

    public void showSearches() {
        this.currentTab = "searches";
        listContainer.getChildren().clear();
        progressBar.setVisible(true);

        executor.submit(() -> {
            List<SearchHistoryItem> searches = slskdService.getRecentSearches();
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                listContainer.getChildren().clear();

                if (searches.isEmpty()) {
                    Label lbl = new Label("Nenhuma pesquisa recente encontrada.");
                    lbl.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 14px;");
                    listContainer.getChildren().add(lbl);
                    return;
                }

                for (SearchHistoryItem s : searches) {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("card-frame");

                    Label qLbl = new Label(s.searchText());
                    qLbl.setStyle("-fx-font-weight: bold;");

                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);

                    Label countLbl = new Label("Resultados: " + s.responseCount());
                    countLbl.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%);");

                    String color = "Completed".equalsIgnoreCase(s.state()) ? "#1DB954" : "-theme-text-accent";
                    Label stateLbl = new Label(s.state());
                    stateLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

                    row.getChildren().addAll(qLbl, sp, countLbl, stateLbl);
                    listContainer.getChildren().add(row);
                }
            });
        });
    }
}
