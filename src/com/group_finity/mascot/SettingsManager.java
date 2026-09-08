package com.group_finity.mascot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;

/** 桌宠 UI 设置。设置保存在 conf/settings.properties，并在运行时立即生效。 */
public final class SettingsManager {
    private static final Logger log = Logger.getLogger(SettingsManager.class.getName());
    private static final SettingsManager INSTANCE = new SettingsManager();
    private File settingsFile;
    private int dialogWidth = 400;
    private int dialogHeight = 280;
    private double characterScale = 0.70;
    private float characterOpacity = 1.0f;
    private String fontFamily = "Microsoft YaHei";
    private int fontSize = 16;

    private SettingsManager() {}
    public static SettingsManager getInstance() { return INSTANCE; }

    public synchronized void init(File baseDir) {
        settingsFile = new File(baseDir, "conf/settings.properties");
        Properties properties = new Properties();
        if (settingsFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(settingsFile), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (Exception e) { log.warning("读取设置失败，使用默认值: " + e.getMessage()); }
        }
        dialogWidth = clamp(parseInt(properties, "dialog.width", dialogWidth), 280, 1000);
        dialogHeight = clamp(parseInt(properties, "dialog.height", dialogHeight), 180, 800);
        characterScale = clamp(parseDouble(properties, "character.scale", characterScale), 0.40, 1.50);
        characterOpacity = (float)clamp(parseDouble(properties, "character.opacity", characterOpacity), 0.20, 1.0);
        String configuredFont = properties.getProperty("font.family", fontFamily).trim();
        if (!configuredFont.isEmpty()) fontFamily = configuredFont;
        fontSize = clamp(parseInt(properties, "font.size", fontSize), 12, 32);
        if (!settingsFile.exists()) save();
    }

    public synchronized void update(int width, int height, double scale, float opacity, String family, int size) {
        dialogWidth = clamp(width, 280, 1000);
        dialogHeight = clamp(height, 180, 800);
        characterScale = clamp(scale, 0.40, 1.50);
        characterOpacity = (float)clamp(opacity, 0.20, 1.0);
        if (family != null && !family.trim().isEmpty()) fontFamily = family.trim();
        fontSize = clamp(size, 12, 32);
        save();
    }

    public synchronized void updateDialogSize(int width, int height) {
        dialogWidth = clamp(width, 280, 1000);
        dialogHeight = clamp(height, 180, 800);
        save();
    }

    public synchronized void save() {
        if (settingsFile == null) return;
        File parent = settingsFile.getParentFile();
        if (parent != null) parent.mkdirs();
        Properties p = new Properties();
        p.setProperty("dialog.width", Integer.toString(dialogWidth));
        p.setProperty("dialog.height", Integer.toString(dialogHeight));
        p.setProperty("character.scale", String.format(java.util.Locale.ROOT, "%.2f", characterScale));
        p.setProperty("character.opacity", String.format(java.util.Locale.ROOT, "%.2f", characterOpacity));
        p.setProperty("font.family", fontFamily);
        p.setProperty("font.size", Integer.toString(fontSize));
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8)) {
            p.store(writer, "Dottore desktop pet UI settings");
        } catch (Exception e) { log.warning("保存设置失败: " + e.getMessage()); }
    }

    public synchronized int getDialogWidth() { return dialogWidth; }
    public synchronized int getDialogHeight() { return dialogHeight; }
    public synchronized double getCharacterScale() { return characterScale; }
    public synchronized float getCharacterOpacity() { return characterOpacity; }
    public synchronized String getFontFamily() { return fontFamily; }
    public synchronized int getFontSize() { return fontSize; }

    private int parseInt(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
    private double parseDouble(Properties p, String key, double fallback) {
        try { return Double.parseDouble(p.getProperty(key, Double.toString(fallback)).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}