package com.library.shared;

import javafx.scene.chart.Chart;
import javafx.scene.chart.XYChart;

/**
 * SHARED BRANCH — chart styling utilities.
 * Use ChartFactory (dashboard branch) to build charts;
 * use this class to style/configure them after creation.
 */
public class ChartUtil {

    /** Apply consistent styling to any chart. */
    public static void applyStyle(Chart chart) {
        chart.setAnimated(false);
        chart.setStyle("-fx-font-size: 11px;");
    }

    /** Remove symbols from an XYChart series (cleaner line charts). */
    public static <X, Y> void removeSymbols(XYChart.Series<X, Y> series) {
        series.getData().forEach(d -> {
            if (d.getNode() != null) d.getNode().setVisible(false);
        });
    }
}
