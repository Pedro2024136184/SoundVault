package com.soundvault.ui.views;

import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class LogsView extends VBox {

    private final TextArea logArea;
    private final CheckBox autoScrollCheck;
    private final Consumer<String> logListener;

    public LogsView() {
        this.setSpacing(15);
        this.setPadding(new Insets(25));
        this.getStyleClass().add("main-background");

        Label header = new Label("System Logs");
        header.getStyleClass().add("header-label");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-control-inner-background: #000000; -fx-text-fill: -theme-text-accent;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Button btnClear = new Button("Limpar Logs");
        btnClear.setStyle("-fx-background-color: #3a3a40; -fx-text-fill: #ffffff; -fx-padding: 5 12; -fx-background-radius: 12px; -fx-cursor: hand;");
        btnClear.setOnAction(e -> logArea.clear());

        autoScrollCheck = new CheckBox("Rolar automaticamente");
        autoScrollCheck.setSelected(true);
        autoScrollCheck.setStyle("-fx-text-fill: derive(-theme-text-primary, -20%);");

        HBox bottomBar = new HBox(12, btnClear, autoScrollCheck);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(header, logArea, bottomBar);

        // Preenche histórico inicial
        List<String> history = AppLogger.getHistory();
        StringBuilder sb = new StringBuilder();
        for (String line : history) {
            sb.append(line).append("\n");
        }
        logArea.setText(sb.toString());
        if (autoScrollCheck.isSelected()) {
            logArea.positionCaret(logArea.getText().length());
        }

        // Listener de novas linhas
        this.logListener = line -> Platform.runLater(() -> {
            logArea.appendText(line + "\n");
            if (autoScrollCheck.isSelected()) {
                logArea.positionCaret(logArea.getText().length());
            }
        });

        AppLogger.addListener(logListener);
    }
}
