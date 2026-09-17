package com.soundvault.ui.components;

import com.soundvault.model.DownloadTransfer;
import com.soundvault.service.SlskdService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class DownloadManagerBar extends VBox {

    private final SlskdService slskdService;
    private final Runnable onOpenSoulseek;

    private final Label statusIcon;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final ProgressBar progressBar;
    private final Label percentLabel;
    private final Button btnToggleDrawer;
    private final Button btnOpenSoulseek;

    private final VBox drawerContainer;
    private final VBox drawerList;
    private boolean drawerExpanded = false;

    private List<DownloadTransfer> currentTransfers = new ArrayList<>();

    public DownloadManagerBar(SlskdService slskdService, Runnable onOpenSoulseek) {
        this.slskdService = slskdService;
        this.onOpenSoulseek = onOpenSoulseek;

        this.getStyleClass().add("card-frame");
        this.setStyle("-fx-background-color: derive(-theme-card-bg, -10%); -fx-border-color: derive(-theme-card-bg, 25%); -fx-border-width: 1.5px 0 0 0; -fx-padding: 0;");

        // --- 1. GAVETA DE DETALHES (INICIALMENTE OCULTA) ---
        drawerList = new VBox(6);
        drawerList.setPadding(new Insets(10, 16, 10, 16));

        ScrollPane drawerScroll = new ScrollPane(drawerList);
        drawerScroll.setFitToWidth(true);
        drawerScroll.setMaxHeight(160);
        drawerScroll.setPrefHeight(140);
        drawerScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: derive(-theme-card-bg, 15%); -fx-border-width: 0 0 1px 0;");

        drawerContainer = new VBox(drawerScroll);
        drawerContainer.setVisible(false);
        drawerContainer.setManaged(false);

        // --- 2. BARRA PRINCIPAL (COMPACTA) ---
        HBox mainRow = new HBox(14);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(8, 16, 8, 16));

        // Ícone / Indicador de estado
        statusIcon = new Label("●");
        statusIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #10b981;");

        VBox textInfo = new VBox(2);
        textInfo.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label("Soulseek Conectado");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -theme-text-primary;");

        subtitleLabel = new Label("Pronto para transferências");
        subtitleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: derive(-theme-text-primary, -40%);");

        textInfo.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Barra de progresso central
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(160);
        progressBar.setPrefHeight(8);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: -theme-btn-primary;");

        percentLabel = new Label("");
        percentLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent;");
        percentLabel.setVisible(false);

        // Botões de ação à direita
        btnToggleDrawer = new Button("▲ Detalhes");
        btnToggleDrawer.setStyle("-fx-background-color: transparent; -fx-border-color: derive(-theme-card-bg, 35%); -fx-border-radius: 12px; -fx-text-fill: derive(-theme-text-primary, -20%); -fx-font-size: 10.5px; -fx-padding: 4 10; -fx-cursor: hand;");
        btnToggleDrawer.setOnAction(e -> toggleDrawer());

        btnOpenSoulseek = new Button("Monitor");
        btnOpenSoulseek.setStyle("-fx-background-color: derive(-theme-card-bg, 18%); -fx-text-fill: -theme-text-primary; -fx-background-radius: 12px; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
        btnOpenSoulseek.setOnAction(e -> {
            if (onOpenSoulseek != null) onOpenSoulseek.run();
        });

        mainRow.getChildren().addAll(statusIcon, textInfo, spacer, progressBar, percentLabel, btnToggleDrawer, btnOpenSoulseek);

        this.getChildren().addAll(drawerContainer, mainRow);

        // Registar ouvinte no serviço Slskd
        if (slskdService != null) {
            slskdService.addTransferListener(transfers -> Platform.runLater(() -> updateTransfers(transfers)));
        }
    }

    private void toggleDrawer() {
        drawerExpanded = !drawerExpanded;
        drawerContainer.setVisible(drawerExpanded);
        drawerContainer.setManaged(drawerExpanded);
        btnToggleDrawer.setText(drawerExpanded ? "▼ Ocultar" : "▲ Detalhes (" + currentTransfers.size() + ")");
        if (drawerExpanded) {
            refreshDrawerList();
        }
    }

    public void updateTransfers(List<DownloadTransfer> transfers) {
        this.currentTransfers = (transfers != null) ? transfers : new ArrayList<>();

        List<DownloadTransfer> inProgress = currentTransfers.stream().filter(DownloadTransfer::isInProgress).toList();
        List<DownloadTransfer> queued = currentTransfers.stream().filter(DownloadTransfer::isQueued).toList();
        List<DownloadTransfer> completed = currentTransfers.stream().filter(DownloadTransfer::isCompleted).toList();

        if (!inProgress.isEmpty()) {
            DownloadTransfer active = inProgress.get(0);
            statusIcon.setText("⬇");
            statusIcon.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

            String dirName = active.getDirectoryName();
            String title = active.getCleanFileName();
            if (!dirName.isBlank()) {
                title += " (" + dirName + ")";
            }
            titleLabel.setText("A transferir: " + title);

            String speed = active.getFormattedSpeed();
            subtitleLabel.setText("Peer: " + active.username() + " • " + speed + " • " + completed.size() + " concluídas, " + inProgress.size() + " a transferir, " + queued.size() + " em fila");

            progressBar.setVisible(true);
            double progress = active.progressPercent() / 100.0;
            progressBar.setProgress(progress);

            percentLabel.setVisible(true);
            percentLabel.setText(active.getFormattedProgress());

            btnToggleDrawer.setText(drawerExpanded ? "▼ Ocultar" : "▲ Detalhes (" + currentTransfers.size() + ")");

        } else if (!queued.isEmpty()) {
            statusIcon.setText("⏳");
            statusIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #f59e0b;");

            titleLabel.setText("Na fila do Soulseek (" + queued.size() + " ficheiros aguardam slot)");
            subtitleLabel.setText("Próximo: " + queued.get(0).getCleanFileName() + " (Peer: " + queued.get(0).username() + ")");

            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

            percentLabel.setVisible(false);
            btnToggleDrawer.setText(drawerExpanded ? "▼ Ocultar" : "▲ Detalhes (" + currentTransfers.size() + ")");

        } else if (!completed.isEmpty()) {
            statusIcon.setText("✓");
            statusIcon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

            titleLabel.setText("Transferências Concluídas (" + completed.size() + " ficheiros)");
            subtitleLabel.setText("Todos os ficheiros foram descarregados com sucesso.");

            progressBar.setVisible(false);
            percentLabel.setVisible(false);
            btnToggleDrawer.setText(drawerExpanded ? "▼ Ocultar" : "▲ Detalhes (" + currentTransfers.size() + ")");

        } else {
            statusIcon.setText("●");
            statusIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: derive(-theme-text-primary, -50%);");

            titleLabel.setText("Soulseek Conectado");
            subtitleLabel.setText("Nenhum download ativo.");

            progressBar.setVisible(false);
            percentLabel.setVisible(false);
            btnToggleDrawer.setText(drawerExpanded ? "▼ Ocultar" : "▲ Detalhes (0)");
        }

        if (drawerExpanded) {
            refreshDrawerList();
        }
    }

    private void refreshDrawerList() {
        drawerList.getChildren().clear();

        if (currentTransfers.isEmpty()) {
            Label empty = new Label("Nenhuma transferência recente.");
            empty.setStyle("-fx-text-fill: derive(-theme-text-primary, -40%); -fx-font-size: 11px;");
            drawerList.getChildren().add(empty);
            return;
        }

        for (DownloadTransfer t : currentTransfers) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: derive(-theme-card-bg, 10%); -fx-background-radius: 6px; -fx-padding: 4 10;");

            Label nameLbl = new Label(t.getCleanFileName());
            nameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -theme-text-primary;");
            nameLbl.setPrefWidth(220);

            Label userLbl = new Label(t.username());
            userLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: derive(-theme-text-primary, -35%);");
            userLbl.setPrefWidth(90);

            ProgressBar pb = new ProgressBar(t.progressPercent() / 100.0);
            pb.setPrefWidth(90);
            pb.setPrefHeight(6);

            Label pct = new Label(t.getFormattedProgress());
            pct.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -theme-text-accent;");
            pct.setPrefWidth(35);

            String stateColor = t.isCompleted() ? "#10b981" : (t.isInProgress() ? "-theme-text-accent" : (t.isQueued() ? "#f59e0b" : "#ef4444"));
            Label stateBadge = new Label(t.state());
            stateBadge.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: " + stateColor + "; -fx-background-color: derive(-theme-card-bg, 20%); -fx-padding: 2 6; -fx-background-radius: 4px;");

            Label speedLbl = new Label(t.isInProgress() ? t.getFormattedSpeed() : t.getFormattedSize());
            speedLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: derive(-theme-text-primary, -30%);");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            row.getChildren().addAll(nameLbl, userLbl, pb, pct, stateBadge, sp, speedLbl);
            drawerList.getChildren().add(row);
        }
    }
}
