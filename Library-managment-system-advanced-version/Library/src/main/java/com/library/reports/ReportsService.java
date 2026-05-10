package com.library.reports;

import com.library.service.ReportService;

import java.io.*;
import java.time.LocalDate;

/**
 * REPORTS BRANCH — service layer.
 */
public class ReportsService {

    private final ReportService delegate = new ReportService();

    public boolean generateOverduePdf(File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            delegate.generateOverdueReport(out);
            return true;
        } catch (Exception e) {
            System.err.println("ReportsService.generateOverduePdf: " + e.getMessage());
            return false;
        }
    }

    public boolean generateCirculationPdf(LocalDate from, LocalDate to, File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            delegate.generateCirculationReport(from, to, out);
            return true;
        } catch (Exception e) {
            System.err.println("ReportsService.generateCirculationPdf: " + e.getMessage());
            return false;
        }
    }

    public boolean generateInventoryPdf(File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            delegate.generateInventoryReport(out);
            return true;
        } catch (Exception e) {
            System.err.println("ReportsService.generateInventoryPdf: " + e.getMessage());
            return false;
        }
    }

    public boolean generateFinePdf(File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            delegate.generateFineReport(out);
            return true;
        } catch (Exception e) {
            System.err.println("ReportsService.generateFinePdf: " + e.getMessage());
            return false;
        }
    }
}
