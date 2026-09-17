package com.soundvault.ui.views;

import com.soundvault.model.AudioAnalysis;
import com.soundvault.model.Track;
import com.soundvault.service.SpotifyService;
import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysesView extends VBox {

    private final SpotifyService spotifyService;
    private final BarChart<String, Number> barChart;
    private final XYChart.Series<String, Number> series;
    private final ProgressBar progressBar;
    private final Button btnLoad;
    private final VBox metricsBox;
    private final FlowPane genresPane;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AnalysesView(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;

        this.setSpacing(16);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("Perfil Psicoacústico & Mood");
        header.getStyleClass().add("header-label");

        Label subtitle = new Label("Análise das propriedades psicoacústicas e tendências sonoras da tua biblioteca e hábitos de escuta.");
        subtitle.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 13px;");

        btnLoad = new Button("Atualizar Análise Sonora");
        btnLoad.getStyleClass().add("action-button");
        btnLoad.setOnAction(e -> loadAudioFeatures());

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);

        HBox topBar = new HBox(12, btnLoad, progressBar);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Gráfico JavaFX nativo com eixos limpos e sem colisões de texto
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Dimensão Sonora");
        xAxis.setTickLabelFill(javafx.scene.paint.Color.web("#e0e0e0"));
        xAxis.setTickLabelGap(10);
        xAxis.setTickLabelRotation(0);
        xAxis.setCategories(javafx.collections.FXCollections.observableArrayList(
                "Energia", "Positividade", "Dança", "Acústica", "Instrumental"
        ));

        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Nível (%)");
        yAxis.setTickLabelFill(javafx.scene.paint.Color.web("#e0e0e0"));

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Impressão Psicoacústica Global");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setCategoryGap(35);
        barChart.setBarGap(10);
        barChart.setMinHeight(280);
        barChart.setPrefHeight(320);

        series = new XYChart.Series<>();
        barChart.getData().add(series);

        // Painel de métricas detalhadas
        metricsBox = new VBox(10);
        metricsBox.setPadding(new Insets(15));
        metricsBox.getStyleClass().add("card-frame");

        Label metricsTitle = new Label("MÉTRICAS PSICOACÚSTICAS DETALHADAS");
        metricsTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: derive(-theme-text-primary, -40%); -fx-letter-spacing: 1px;");
        metricsBox.getChildren().add(metricsTitle);

        // Painel de géneros sonoros identificados
        VBox genresCard = new VBox(10);
        genresCard.setPadding(new Insets(15));
        genresCard.getStyleClass().add("card-frame");

        Label genresTitle = new Label("GÉNEROS MUSICAIS PREDOMINANTES");
        genresTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: derive(-theme-text-primary, -40%); -fx-letter-spacing: 1px;");

        genresPane = new FlowPane();
        genresPane.setHgap(8);
        genresPane.setVgap(8);
        genresCard.getChildren().addAll(genresTitle, genresPane);

        HBox detailsRow = new HBox(16, metricsBox, genresCard);
        HBox.setHgrow(metricsBox, Priority.ALWAYS);
        HBox.setHgrow(genresCard, Priority.ALWAYS);

        VBox contentContainer = new VBox(16, barChart, detailsRow);
        ScrollPane scrollPane = new ScrollPane(contentContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(header, subtitle, topBar, scrollPane);

        // Carrega automaticamente em background
        loadAudioFeatures();
    }

    private void loadAudioFeatures() {
        btnLoad.setDisable(true);
        progressBar.setVisible(true);

        executor.submit(() -> {
            AppLogger.log("A extrair amostra de faixas e géneros para perfil sonoro...");
            List<Track> sample = new ArrayList<>();
            spotifyService.loadLikedSongsProgressively(batch -> {
                if (sample.size() < 50) {
                    sample.addAll(batch);
                }
            });

            List<String> ids = sample.stream().map(Track::id).filter(id -> id != null && !id.isBlank()).toList();
            AudioAnalysis analysis = spotifyService.getAverageAudioFeatures(ids);
            List<String> topGenres = spotifyService.getUserTopGenres();

            Platform.runLater(() -> {
                // Atualiza o gráfico de barras diretamente nos slots corretos
                series.getData().setAll(
                        new XYChart.Data<>("Energia", (int) Math.round(analysis.energy() * 100)),
                        new XYChart.Data<>("Positividade", (int) Math.round(analysis.valence() * 100)),
                        new XYChart.Data<>("Dança", (int) Math.round(analysis.danceability() * 100)),
                        new XYChart.Data<>("Acústica", (int) Math.round(analysis.acousticness() * 100)),
                        new XYChart.Data<>("Instrumental", (int) Math.round(analysis.instrumentalness() * 100))
                );

                // Atualiza as métricas detalhadas com barras de progresso
                metricsBox.getChildren().retainAll(metricsBox.getChildren().get(0));
                addMetricRow(metricsBox, "Energia & Intensidade", analysis.energy());
                addMetricRow(metricsBox, "Positividade & Luminosidade (Valence)", analysis.valence());
                addMetricRow(metricsBox, "Dançabilidade & Ritmo", analysis.danceability());
                addMetricRow(metricsBox, "Acústica & Timbre Natural", analysis.acousticness());
                addMetricRow(metricsBox, "Instrumentalidade & Textura Sonora", analysis.instrumentalness());

                // Atualiza as badges de géneros
                genresPane.getChildren().clear();
                if (topGenres.isEmpty()) {
                    Label noGen = new Label("Nenhum género detectado.");
                    noGen.setStyle("-fx-text-fill: derive(-theme-text-primary, -30%); -fx-font-size: 12px;");
                    genresPane.getChildren().add(noGen);
                } else {
                    for (String g : topGenres) {
                        Label tag = new Label(g);
                        tag.setStyle("-fx-background-color: derive(-theme-card-bg, 18%); -fx-text-fill: -theme-text-accent; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-padding: 5 12; -fx-background-radius: 12px; -fx-border-color: derive(-theme-text-accent, -50%); -fx-border-radius: 12px;");
                        genresPane.getChildren().add(tag);
                    }
                }

                btnLoad.setDisable(false);
                progressBar.setVisible(false);
                AppLogger.log("Perfil psicoacústico atualizado com sucesso.");
            });
        });
    }

    private void addMetricRow(VBox container, String title, double value) {
        int percent = (int) Math.round(value * 100);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.setPrefWidth(240);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: derive(-theme-text-primary, -15%); -fx-font-weight: 600;");

        ProgressBar pb = new ProgressBar(value);
        pb.setPrefWidth(180);
        pb.setStyle("-fx-accent: -theme-text-accent;");
        HBox.setHgrow(pb, Priority.ALWAYS);

        Label valLbl = new Label(percent + "%");
        valLbl.setPrefWidth(45);
        valLbl.setAlignment(Pos.CENTER_RIGHT);
        valLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        row.getChildren().addAll(label, pb, valLbl);
        container.getChildren().add(row);
    }
}
