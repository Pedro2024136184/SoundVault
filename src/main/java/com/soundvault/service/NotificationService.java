package com.soundvault.service;

import com.soundvault.util.AppLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private TrayIcon trayIcon = null;
    private boolean isSupported = false;

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    public NotificationService() {
        initTray();
    }

    private void initTray() {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            AppLogger.log("SystemTray não disponível neste ambiente.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image iconImage = null;

            try (InputStream is = getClass().getResourceAsStream("/icons/icon.png")) {
                if (is != null) {
                    iconImage = ImageIO.read(is);
                }
            } catch (Exception ignored) {}

            if (iconImage == null) {
                iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }

            trayIcon = new TrayIcon(iconImage, "SoundVault");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            isSupported = true;
            AppLogger.log("Serviço de notificações nativas do Windows inicializado.");
        } catch (Exception e) {
            AppLogger.log("Aviso ao inicializar SystemTray: " + e.getMessage());
            isSupported = false;
        }
    }

    public void showNotification(String title, String message, TrayIcon.MessageType type) {
        AppLogger.log("[Notificação Windows] " + title + ": " + message);
        if (isSupported && trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, type);
            } catch (Exception e) {
                AppLogger.log("Erro ao emitir notificação nativa: " + e.getMessage());
            }
        }
    }

    public void notifyDownloadCompleted(String title) {
        showNotification("Download Concluído", title + " descarregado com sucesso.", TrayIcon.MessageType.INFO);
    }

    public void notifyWishlistAdded(String albumTitle) {
        showNotification("Lista de Desejos", "Adicionado à Wishlist: " + albumTitle, TrayIcon.MessageType.INFO);
    }

    public void notifyCoversSynced(int count) {
        showNotification("Sincronização de Capas", count + " capas atualizadas com sucesso.", TrayIcon.MessageType.INFO);
    }
}
