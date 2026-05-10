package com.library.dashboard;

import javafx.scene.chart.*;

import java.util.Map;

/**
 * DASHBOARD BRANCH — chart factory.
 * Builds JavaFX chart instances from raw data maps.
 */
public class ChartFactory {

    /** PieChart: slice per category name → count. */
    public static PieChart buildPieChart(Map<String, Integer> data, String title) {
        PieChart chart = new PieChart();
        chart.setTitle(title);
        data.forEach((label, value) ->
                chart.getData().add(new PieChart.Data(label + " (" + value + ")", value)));
        chart.setLegendVisible(true);
        return chart;
    }

    /** BarChart: X = month label, Y = count. */
    public static BarChart<String, Number> buildBarChart(
            Map<String, Integer> data, String title, String xLabel, String yLabel) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel(xLabel);
        yAxis.setLabel(yLabel);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
        chart.getData().add(series);
        return chart;
    }

    /** LineChart: X = date label, Y = count. Supports two series (issued / returned). */
    public static LineChart<String, Number> buildLineChart(
            Map<String, int[]> data, String title) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);

        XYChart.Series<String, Number> issued   = new XYChart.Series<>();
        XYChart.Series<String, Number> returned = new XYChart.Series<>();
        issued.setName("Issued");
        returned.setName("Returned");

        data.forEach((date, counts) -> {
            issued.getData().add(new XYChart.Data<>(date, counts[0]));
            returned.getData().add(new XYChart.Data<>(date, counts[1]));
        });

        chart.getData().add(issued);
        chart.getData().add(returned);
        return chart;
    }
}
