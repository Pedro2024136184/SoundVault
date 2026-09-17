package com.soundvault;

import com.soundvault.service.ThemeService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeServiceTest {

    @Test
    void testAllThemesHaveDistinctFontsAndColors() {
        List<ThemeService.ThemeProperties> themes = ThemeService.ThemeProperties.getAllThemes();
        assertEquals(25, themes.size(), "Devem existir exatamente 25 temas estéticos.");

        for (ThemeService.ThemeProperties t : themes) {
            assertNotNull(t.name(), "O nome do tema não pode ser nulo");
            assertNotNull(t.fontFamily(), "O fontFamily não pode ser nulo: " + t.name());
            assertFalse(t.fontFamily().isBlank(), "O fontFamily não pode ser vazio: " + t.name());
            assertNotNull(t.bgDark(), "O bgDark não pode ser nulo: " + t.name());
            assertNotNull(t.textAccent(), "O textAccent não pode ser nulo: " + t.name());
        }
    }

    @Test
    void testThemeGeneratesDynamicCssWithCorrectFont() throws Exception {
        ThemeService themeService = new ThemeService();
        ThemeService.ThemeProperties editorial = ThemeService.ThemeProperties.getThemeByName("Editorial");
        themeService.setTheme(editorial);

        Path cssPath = Paths.get("cache", "current_theme.css");
        assertTrue(Files.exists(cssPath), "Ficheiro current_theme.css deve existir");

        String css = Files.readString(cssPath);
        assertTrue(css.contains("Georgia"), "CSS deve conter a fonte Georgia para o tema Editorial");
        assertTrue(css.contains(".header-label"), "CSS deve conter regra para .header-label");
        assertTrue(css.contains("-fx-font-family: 'Georgia'"), "CSS deve aplicar font-family aos elementos");

        // Testar troca de tema para Retro-futuristic (Consolas monospace)
        ThemeService.ThemeProperties retro = ThemeService.ThemeProperties.getThemeByName("Retro-futuristic");
        themeService.setTheme(retro);

        String cssRetro = Files.readString(cssPath);
        assertTrue(cssRetro.contains("Consolas"), "CSS deve conter a fonte Consolas para o tema Retro-futuristic");
        assertFalse(cssRetro.contains("Georgia"), "CSS não deve mais conter a fonte do tema anterior");
    }
}
