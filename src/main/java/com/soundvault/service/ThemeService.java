package com.soundvault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundvault.util.AppLogger;
import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ThemeService {

    private static final Path THEME_JSON = Paths.get("cache", "theme.json");
    private static final Path THEME_CSS = Paths.get("cache", "current_theme.css");
    private final ObjectMapper mapper = new ObjectMapper();

    public record ThemeProperties(
            String name,
            String bgDark,
            String sidebarDark,
            String buttonPrimary,
            String buttonHover,
            String buttonSelected,
            String textPrimary,
            String textAccent,
            String textSelected,
            String cardBg,
            int fontSize,
            String fontFamily
    ) {
        public ThemeProperties {
            if (fontFamily == null || fontFamily.isBlank()) {
                fontFamily = "'Segoe UI', 'Inter', sans-serif";
            }
        }

        public ThemeProperties(
                String name, String bgDark, String sidebarDark, String buttonPrimary,
                String buttonHover, String buttonSelected, String textPrimary,
                String textAccent, String textSelected, String cardBg, int fontSize
        ) {
            this(name, bgDark, sidebarDark, buttonPrimary, buttonHover, buttonSelected,
                 textPrimary, textAccent, textSelected, cardBg, fontSize, "'Segoe UI', 'Inter', sans-serif");
        }

        public static List<ThemeProperties> getAllThemes() {
            return List.of(
                    // 1. Neobrutalist
                    new ThemeProperties("Neobrutalist", "#0a0a0a", "#141414", "#ffe600", "#fff04d", "#ffe600", "#ffffff", "#ffe600", "#000000", "#181818", 14, "'Arial Black', 'Trebuchet MS', sans-serif"),
                    // 2. Swiss / International
                    new ThemeProperties("Swiss/International", "#0e0e10", "#16161a", "#e11d48", "#f43f5e", "#e11d48", "#ffffff", "#e11d48", "#ffffff", "#1c1c22", 14, "'Arial', 'Helvetica', 'Franklin Gothic Medium', sans-serif"),
                    // 3. Editorial
                    new ThemeProperties("Editorial", "#171412", "#201c19", "#d97706", "#b45309", "#d97706", "#f5f5f4", "#f59e0b", "#000000", "#25201b", 14, "'Georgia', 'Palatino Linotype', 'Times New Roman', serif"),
                    // 4. Glassmorphism
                    new ThemeProperties("Glassmorphism", "#0b0f19", "#111827", "#38bdf8", "#0ea5e9", "#38bdf8", "#f8fafc", "#38bdf8", "#0b0f19", "#151e32", 14, "'Segoe UI', 'Inter', 'Calibri', sans-serif"),
                    // 5. Retro-futuristic
                    new ThemeProperties("Retro-futuristic", "#12072b", "#1b0b3e", "#ff2a85", "#ff529f", "#ff2a85", "#fdf4ff", "#00f0ff", "#ffffff", "#240e54", 14, "'Consolas', 'Lucida Console', 'Courier New', monospace"),
                    // 6. Bauhaus
                    new ThemeProperties("Bauhaus", "#12151a", "#1a1f28", "#e63946", "#ff4d5a", "#e63946", "#f1faee", "#ffbe0b", "#ffffff", "#222936", 14, "'Century Gothic', 'Trebuchet MS', 'Futura', sans-serif"),
                    // 7. Art Deco
                    new ThemeProperties("Art Deco", "#0a0f0d", "#101815", "#d4af37", "#e6c35c", "#d4af37", "#f5f5f0", "#2ec4b6", "#0a0f0d", "#15221e", 14, "'Copperplate Gothic Bold', 'Century Gothic', serif"),
                    // 8. Minimal
                    new ThemeProperties("Minimal", "#121212", "#181818", "#2b2b2b", "#383838", "#ffffff", "#f5f5f5", "#e5e5e5", "#000000", "#1c1c1c", 14, "'Segoe UI Light', 'Calibri Light', 'Arial', sans-serif"),
                    // 9. Flat
                    new ThemeProperties("Flat", "#1a1c23", "#21242d", "#06b6d4", "#0891b2", "#06b6d4", "#f8fafc", "#22d3ee", "#0f172a", "#282c37", 14, "'Trebuchet MS', 'Verdana', sans-serif"),
                    // 10. Material
                    new ThemeProperties("Material", "#131316", "#1c1b1f", "#6750a4", "#7f67be", "#d0bcff", "#e6e1e5", "#d0bcff", "#381e72", "#2b2930", 14, "'Roboto', 'Segoe UI', 'Noto Sans', sans-serif"),
                    // 11. Neumorphic
                    new ThemeProperties("Neumorphic", "#1e2126", "#23272e", "#2c313a", "#353b46", "#4fa6e8", "#e0e6ed", "#61afef", "#ffffff", "#272c34", 14, "'Segoe UI Semibold', 'Calibri', sans-serif"),
                    // 12. Monochromatic
                    new ThemeProperties("Monochromatic", "#0f172a", "#1e293b", "#334155", "#475569", "#94a3b8", "#f8fafc", "#cbd5e1", "#0f172a", "#1e293b", 14, "'Lucida Console', 'Consolas', monospace"),
                    // 13. Scandinavian
                    new ThemeProperties("Scandinavian", "#1f2022", "#27292c", "#52796f", "#6b9080", "#84a98c", "#f4f4f2", "#a4c3b2", "#1b2623", "#2e3135", 14, "'Corbel', 'Verdana', sans-serif"),
                    // 14. Japandi
                    new ThemeProperties("Japandi", "#1c1b18", "#24231f", "#c49b66", "#d4ac7d", "#c49b66", "#f7f5f0", "#c49b66", "#1c1b18", "#2d2b26", 14, "'Palatino Linotype', 'Georgia', serif"),
                    // 15. Dark Mode First
                    new ThemeProperties("Dark Mode First", "#000000", "#080808", "#10b981", "#059669", "#10b981", "#ffffff", "#10b981", "#000000", "#111111", 14, "'Segoe UI', 'Arial', sans-serif"),
                    // 16. Modernist
                    new ThemeProperties("Modernist", "#111215", "#181a1f", "#2563eb", "#1d4ed8", "#3b82f6", "#f3f4f6", "#60a5fa", "#ffffff", "#20232a", 14, "'Franklin Gothic Medium', 'Arial', sans-serif"),
                    // 17. Organic/Fluid
                    new ThemeProperties("Organic/Fluid", "#151a17", "#1c231f", "#2d6a4f", "#40916c", "#52b788", "#edf5f0", "#74c69d", "#081c15", "#232c27", 14, "'Candara', 'Calibri', sans-serif"),
                    // 18. Corporate Professional
                    new ThemeProperties("Corporate Professional", "#0a1128", "#101c3d", "#1c3d72", "#254e8e", "#48cae4", "#f0f4f8", "#00b4d8", "#03071e", "#15254f", 14, "'Calibri', 'Segoe UI', 'Tahoma', sans-serif"),
                    // 19. Tech Forward
                    new ThemeProperties("Tech Forward", "#080c14", "#0f1726", "#0ea5e9", "#0284c7", "#0ea5e9", "#f1f5f9", "#38bdf8", "#080c14", "#172236", 14, "'Bahnschrift', 'Segoe UI Semibold', sans-serif"),
                    // 20. Luxury Minimal
                    new ThemeProperties("Luxury Minimal", "#0a0a0b", "#121215", "#c5a059", "#d8b56f", "#c5a059", "#fafafa", "#dfc082", "#0a0a0b", "#1a1a1f", 14, "'Garamond', 'Palatino Linotype', 'Georgia', serif"),
                    // 21. Neo-Geo
                    new ThemeProperties("Neo-Geo", "#130e26", "#1c1538", "#7c3aed", "#6d28d9", "#facc15", "#f5f3ff", "#facc15", "#130e26", "#251c4a", 14, "'Century Gothic', 'Trebuchet MS', sans-serif"),
                    // 22. Kinetic
                    new ThemeProperties("Kinetic", "#170e0a", "#22150f", "#ea580c", "#f97316", "#ea580c", "#fff7ed", "#fb923c", "#170e0a", "#2d1d16", 14, "'Arial Black', 'Impact', sans-serif"),
                    // 23. Gradient Modern
                    new ThemeProperties("Gradient Modern", "#0f0a1c", "#18102b", "#c026d3", "#d946ef", "#c026d3", "#faf5ff", "#e879f9", "#ffffff", "#22173d", 14, "'Segoe UI Variable Display', 'Segoe UI', sans-serif"),
                    // 24. Typography First
                    new ThemeProperties("Typography First", "#111111", "#191919", "#dc2626", "#ef4444", "#dc2626", "#ffffff", "#ef4444", "#ffffff", "#222222", 14, "'Rockwell', 'Georgia', 'Arial Black', serif"),
                    // 25. Metropolitan
                    new ThemeProperties("Metropolitan", "#141619", "#1c2024", "#eab308", "#facc15", "#eab308", "#f8fafc", "#facc15", "#0f172a", "#242a30", 14, "'Segoe UI Variable Text', 'Segoe UI', sans-serif")
            );
        }

        public static ThemeProperties getRandomTheme() {
            List<ThemeProperties> list = getAllThemes();
            return list.get(new java.util.Random().nextInt(list.size()));
        }

        public static ThemeProperties getThemeByName(String name) {
            for (ThemeProperties t : getAllThemes()) {
                if (t.name().equalsIgnoreCase(name)) {
                    return t;
                }
            }
            return getAllThemes().get(0);
        }

        public static ThemeProperties defaultCyberpunk() {
            return getThemeByName("Tech Forward");
        }

        public static ThemeProperties spotifyGreen() {
            return getThemeByName("Dark Mode First");
        }

        public static ThemeProperties draculaPurple() {
            return getThemeByName("Neo-Geo");
        }
    }

    private ThemeProperties currentTheme;

    public ThemeService() {
        // Inicializa com um tema estético escolhido aleatoriamente dentre os 25
        this.currentTheme = ThemeProperties.getRandomTheme();
        AppLogger.log("Tema estético inicial selecionado aleatoriamente: " + currentTheme.name());
        generateCssFile();
    }

    public ThemeProperties getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(ThemeProperties newTheme) {
        this.currentTheme = newTheme;
        saveTheme();
        generateCssFile();
    }

    private Path activeCssFile = null;

    public void applyTheme(Scene scene) {
        if (scene == null) return;
        try {
            Files.createDirectories(Paths.get("cache"));
            String css = buildCssContent(currentTheme);
            
            // Grava cópia padrão para persistência
            Files.writeString(THEME_CSS, css);

            // Cria ficheiro com timestamp para forçar o JavaFX a recarregar completamente os estilos e fontes
            Path newCssFile = Paths.get("cache", "theme_active_" + System.currentTimeMillis() + ".css");
            Files.writeString(newCssFile, css);

            String cssUrl = newCssFile.toUri().toURL().toExternalForm();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl);

            // Limpa ficheiro CSS anterior
            if (activeCssFile != null && !activeCssFile.equals(newCssFile)) {
                try {
                    Files.deleteIfExists(activeCssFile);
                } catch (Exception ignored) {}
            }
            activeCssFile = newCssFile;

            AppLogger.log("Tema aplicado: " + currentTheme.name() + " (Fonte: " + currentTheme.fontFamily() + ")");
        } catch (Exception e) {
            AppLogger.log("Erro ao aplicar folha de estilos CSS: " + e.getMessage());
        }
    }

    private void generateCssFile() {
        try {
            Files.createDirectories(THEME_CSS.getParent());
            String css = buildCssContent(currentTheme);
            Files.writeString(THEME_CSS, css);
        } catch (IOException e) {
            AppLogger.log("Erro ao gerar CSS do tema: " + e.getMessage());
        }
    }

    private String buildCssContent(ThemeProperties t) {
        return """
            .root {
                -fx-font-family: %1$s;
                -fx-font-size: %2$dpx;
                -fx-base: %3$s;
                -fx-background: %3$s;
                -fx-focus-color: transparent;
                -fx-faint-focus-color: transparent;
                -fx-font-smoothing-type: lcd;
                
                -theme-bg: %3$s;
                -theme-sidebar: %4$s;
                -theme-btn-primary: %5$s;
                -theme-btn-hover: %6$s;
                -theme-btn-selected: %7$s;
                -theme-text-primary: %8$s;
                -theme-text-accent: %9$s;
                -theme-card-bg: %10$s;
            }

            /* Cascata universal de tipografia para todos os nós de texto */
            .label, .button, .text, .header-label, .section-label, .card-frame, .combo-box {
                -fx-font-family: %1$s;
            }

            /* --- Background Principal & Sidebar --- */
            .main-background {
                -fx-background-color: -theme-bg;
            }

            .sidebar {
                -fx-background-color: -theme-sidebar;
                -fx-border-color: transparent derive(-theme-sidebar, 15%%) transparent transparent;
                -fx-border-width: 0 1 0 0;
            }

            /* --- Botões de Navegação da Sidebar --- */
            .sidebar-button {
                -fx-font-family: %1$s;
                -fx-background-color: transparent;
                -fx-text-fill: derive(-theme-text-primary, -25%%);
                -fx-font-size: 13px;
                -fx-font-weight: normal;
                -fx-background-radius: 8px;
                -fx-padding: 9px 14px;
                -fx-cursor: hand;
                -fx-alignment: BASELINE_LEFT;
                -fx-border-color: transparent;
                -fx-border-width: 0 0 0 3.5px;
            }

            .sidebar-button:hover {
                -fx-background-color: derive(-theme-sidebar, 12%%);
                -fx-text-fill: -theme-text-primary;
            }

            .sidebar-button-active {
                -fx-font-family: %1$s;
                -fx-background-color: linear-gradient(to right, derive(-theme-btn-selected, -60%%), transparent);
                -fx-text-fill: -theme-text-accent;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-background-radius: 8px;
                -fx-padding: 9px 14px;
                -fx-cursor: hand;
                -fx-alignment: BASELINE_LEFT;
                -fx-border-color: transparent transparent transparent -theme-text-accent;
                -fx-border-width: 0 0 0 3.5px;
            }

            /* --- Botões de Ação Gerais --- */
            .action-button {
                -fx-font-family: %1$s;
                -fx-background-color: -theme-btn-primary;
                -fx-text-fill: -theme-text-primary;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-background-radius: 20px;
                -fx-padding: 6px 14px;
                -fx-cursor: hand;
                -fx-border-color: derive(-theme-btn-primary, 25%%);
                -fx-border-radius: 20px;
                -fx-border-width: 1px;
            }

            .action-button:hover {
                -fx-background-color: -theme-btn-hover;
                -fx-border-color: -theme-text-accent;
            }

            /* --- Cartões e Linhas de Músicas --- */
            .card-frame {
                -fx-font-family: %1$s;
                -fx-background-color: -theme-card-bg;
                -fx-background-radius: 10px;
                -fx-border-color: derive(-theme-card-bg, 18%%);
                -fx-border-radius: 10px;
                -fx-border-width: 1px;
                -fx-padding: 10px 16px;
            }

            .card-frame:hover {
                -fx-background-color: derive(-theme-card-bg, 8%%);
                -fx-border-color: derive(-theme-text-accent, -30%%);
            }

            /* --- List Views & List Cells --- */
            .list-view {
                -fx-background-color: transparent;
                -fx-background-insets: 0;
                -fx-padding: 0;
            }

            .list-cell {
                -fx-font-family: %1$s;
                -fx-background-color: transparent;
                -fx-text-fill: -theme-text-primary;
                -fx-padding: 4px 0;
            }

            .list-cell:empty {
                -fx-background-color: transparent;
            }

            /* --- Inputs e Campos de Texto Modernos --- */
            .text-field {
                -fx-font-family: %1$s;
                -fx-background-color: derive(-theme-card-bg, 6%%);
                -fx-text-fill: #ffffff;
                -fx-prompt-text-fill: #64748b;
                -fx-background-radius: 20px;
                -fx-border-color: derive(-theme-card-bg, 22%%);
                -fx-border-radius: 20px;
                -fx-border-width: 1px;
                -fx-padding: 8px 16px;
                -fx-font-size: 13px;
            }

            .text-field:focused {
                -fx-border-color: -theme-text-accent;
                -fx-background-color: derive(-theme-card-bg, 12%%);
            }

            .combo-box {
                -fx-font-family: %1$s;
                -fx-background-color: derive(-theme-card-bg, 6%%);
                -fx-background-radius: 8px;
                -fx-border-color: derive(-theme-card-bg, 22%%);
                -fx-border-radius: 8px;
                -fx-cursor: hand;
                -fx-padding: 4px 10px;
            }

            .combo-box .list-cell {
                -fx-font-family: %1$s;
                -fx-text-fill: #ffffff;
            }

            .combo-box-popup .list-view {
                -fx-background-color: %4$s;
                -fx-border-color: derive(%4$s, 25%%);
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
            }

            .combo-box-popup .list-cell {
                -fx-font-family: %1$s;
                -fx-background-color: transparent;
                -fx-text-fill: #ffffff;
                -fx-padding: 6px 12px;
            }

            .combo-box-popup .list-cell:hover {
                -fx-background-color: %6$s;
                -fx-text-fill: #ffffff;
            }

            .combo-box-popup .list-cell:filled:selected {
                -fx-background-color: %7$s;
                -fx-text-fill: #ffffff;
            }

            /* --- Tipografia & Títulos --- */
            .header-label {
                -fx-font-family: %1$s;
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: #ffffff;
            }

            .section-label {
                -fx-font-family: %1$s;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-text-fill: -theme-text-accent;
            }

            .badge-pill {
                -fx-font-family: %1$s;
                -fx-background-color: linear-gradient(to right, derive(-theme-btn-selected, -60%%), derive(-theme-btn-selected, -75%%));
                -fx-text-fill: -theme-text-accent;
                -fx-background-radius: 14px;
                -fx-padding: 4px 12px;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
                -fx-border-color: derive(-theme-text-accent, -50%%);
                -fx-border-radius: 14px;
                -fx-border-width: 1px;
            }

            /* --- ScrollBars Ultra Finas e Discretas --- */
            .scroll-pane {
                -fx-background-color: transparent;
                -fx-background-insets: 0;
                -fx-padding: 0;
            }

            .scroll-pane > .viewport {
                -fx-background-color: transparent;
            }

            .scroll-bar:vertical {
                -fx-background-color: transparent;
                -fx-pref-width: 8px;
            }

            .scroll-bar:horizontal {
                -fx-background-color: transparent;
                -fx-pref-height: 8px;
            }

            .scroll-bar .track {
                -fx-background-color: transparent;
            }

            .scroll-bar .thumb {
                -fx-background-color: derive(-theme-sidebar, 35%%);
                -fx-background-radius: 6px;
            }

            .scroll-bar .thumb:hover {
                -fx-background-color: -theme-text-accent;
            }

            .scroll-bar .increment-button, .scroll-bar .decrement-button {
                -fx-pref-height: 0;
                -fx-pref-width: 0;
                -fx-opacity: 0;
            }

            /* --- Progress Bar --- */
            .progress-bar {
                -fx-pref-height: 5px;
            }

            .progress-bar .track {
                -fx-background-color: derive(-theme-card-bg, 15%%);
                -fx-background-radius: 4px;
            }

            .progress-bar .bar {
                -fx-background-color: linear-gradient(to right, -theme-btn-selected, -theme-text-accent);
                -fx-background-radius: 4px;
            }

            /* --- Gráfico BarChart --- */
            .chart {
                -fx-padding: 10px;
            }

            .chart-plot-background {
                -fx-background-color: transparent;
            }

            .chart-vertical-grid-lines, .chart-horizontal-grid-lines {
                -fx-stroke: derive(-theme-card-bg, 15%%);
            }

            .chart-bar {
                -fx-bar-fill: -theme-text-accent;
                -fx-background-radius: 6px 6px 0 0;
            }

            .chart-title, .axis-label, .tick-mark, .axis-tick-mark {
                -fx-font-family: %1$s;
            }

            /* --- TextArea Logs Terminal --- */
            .text-area {
                -fx-font-family: 'Consolas', 'Courier New', monospace;
                -fx-background-color: #000000;
                -fx-border-color: derive(-theme-card-bg, 25%%);
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
            }

            .text-area .content {
                -fx-background-color: #050608;
                -fx-background-radius: 8px;
            }
            """.formatted(
                t.fontFamily(),
                t.fontSize(),
                t.bgDark(),
                t.sidebarDark(),
                t.buttonPrimary(),
                t.buttonHover(),
                t.buttonSelected(),
                t.textPrimary(),
                t.textAccent(),
                t.cardBg()
        );
    }

    private void saveTheme() {
        try {
            Files.createDirectories(THEME_JSON.getParent());
            mapper.writeValue(THEME_JSON.toFile(), currentTheme);
        } catch (IOException ignored) {}
    }

    private ThemeProperties loadTheme() {
        if (!Files.exists(THEME_JSON)) {
            return ThemeProperties.defaultCyberpunk();
        }
        try {
            return mapper.readValue(THEME_JSON.toFile(), ThemeProperties.class);
        } catch (Exception e) {
            return ThemeProperties.defaultCyberpunk();
        }
    }
}
