package com.library.service;

import com.library.config.AppConfig;
import com.library.model.LibraryInfo;
import com.library.shared.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LibraryInfoService {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryInfoService.class);
    private static LibraryInfoService instance;

    private static final String KEY_LIB_NAME    = "library.name";
    private static final String KEY_INSTITUTION = "library.institution";
    private static final String KEY_EMAIL       = "library.email";
    private static final String KEY_PHONE       = "library.phone";
    private static final String KEY_ADDRESS     = "library.address";
    private static final String KEY_WEBSITE     = "library.website";
    private static final String KEY_CURRENCY    = "library.currency";
    private static final String KEY_TAX_RATE    = "library.taxRate";
    private static final String KEY_PDF_PAPER   = "pdf.paperSize";
    private static final String KEY_PDF_LANG    = "pdf.language";

    private final List<Consumer<LibraryInfo>> changeListeners = new ArrayList<>();

    public static synchronized LibraryInfoService getInstance() {
        if (instance == null) {
            instance = new LibraryInfoService();
        }
        return instance;
    }

    public void addChangeListener(Consumer<LibraryInfo> listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Consumer<LibraryInfo> listener) {
        changeListeners.remove(listener);
    }

    public LibraryInfo getLibraryInfo() {
        LibraryInfo info = new LibraryInfo();
        String name = getSetting(KEY_LIB_NAME, AppConfig.getInstance().getLibraryName());
        String inst = getSetting(KEY_INSTITUTION, "University of Engineering & Technology Peshawar");
        String email = getSetting(KEY_EMAIL, AppConfig.getInstance().get(AppConfig.KEY_LIBRARY_EMAIL));
        String phone = getSetting(KEY_PHONE, AppConfig.getInstance().get(AppConfig.KEY_LIBRARY_PHONE));
        String addr = getSetting(KEY_ADDRESS, AppConfig.getInstance().get(AppConfig.KEY_LIBRARY_ADDRESS));
        String web = getSetting(KEY_WEBSITE, "https://uetpeshawar.edu.pk");

        info.setLibraryName(name != null && !name.isBlank() ? name : "Central Library");
        info.setInstitutionName(inst != null ? inst : "");
        info.setEmail(email != null ? email : "");
        info.setContactNumber(phone != null ? phone : "");
        info.setAddress(addr != null ? addr : "");
        info.setWebsite(web != null ? web : "");
        return info;
    }

    public boolean saveLibraryInfo(LibraryInfo info) {
        if (info == null) return false;
        boolean ok = true;
        ok &= saveSetting(KEY_LIB_NAME, info.getLibraryName());
        ok &= saveSetting(KEY_INSTITUTION, info.getInstitutionName());
        ok &= saveSetting(KEY_EMAIL, info.getEmail());
        ok &= saveSetting(KEY_PHONE, info.getContactNumber());
        ok &= saveSetting(KEY_ADDRESS, info.getAddress());
        ok &= saveSetting(KEY_WEBSITE, info.getWebsite());

        // Sync with AppConfig properties file as well
        AppConfig config = AppConfig.getInstance();
        config.set(AppConfig.KEY_LIBRARY_NAME, info.getLibraryName());
        config.set(AppConfig.KEY_LIBRARY_EMAIL, info.getEmail());
        config.set(AppConfig.KEY_LIBRARY_PHONE, info.getContactNumber());
        config.set(AppConfig.KEY_LIBRARY_ADDRESS, info.getAddress());
        config.save();

        // Notify listeners
        for (Consumer<LibraryInfo> listener : changeListeners) {
            try { listener.accept(info); } catch (Exception ignored) {}
        }
        return ok;
    }

    public String getCurrency() {
        return getSetting(KEY_CURRENCY, AppConfig.getInstance().getCurrency());
    }

    public boolean saveCurrency(String currency) {
        if (currency == null || currency.isBlank()) currency = "PKR";
        AppConfig.getInstance().set(AppConfig.KEY_CURRENCY, currency);
        AppConfig.getInstance().save();
        return saveSetting(KEY_CURRENCY, currency);
    }

    public double getTaxRate() {
        String val = getSetting(KEY_TAX_RATE, "0.00");
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    public boolean saveTaxRate(double taxRate) {
        return saveSetting(KEY_TAX_RATE, String.format("%.2f", taxRate));
    }

    public String getPdfPaperSize() {
        return getSetting(KEY_PDF_PAPER, "A4 — 210 × 297 mm");
    }

    public boolean savePdfPaperSize(String paperSize) {
        return saveSetting(KEY_PDF_PAPER, paperSize);
    }

    public String getPdfLanguage() {
        return getSetting(KEY_PDF_LANG, "English");
    }

    public boolean savePdfLanguage(String language) {
        return saveSetting(KEY_PDF_LANG, language);
    }

    public String getSetting(String key, String def) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("value");
                    return val != null ? val : def;
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to getSetting {}: {}", key, e.getMessage());
        }
        return def;
    }

    public boolean saveSetting(String key, String value) {
        String sql = "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value != null ? value : "");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to saveSetting {} = {}: {}", key, value, e.getMessage());
            return false;
        }
    }
}
