/*
 * Copyright © 2017-2025 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.physics;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.google.common.math.DoubleMath;

/**
 * Helpful class for visualising traversal. Plots acceleration, speed and displacement
 */
public final class TraversalPlotter {
    private TraversalPlotter() {
    }

    /**
     * Build a JFreeChart with three stacked subplots for the provided traversals.
     */
    public static JFreeChart createChart(
            List<Traversal> traversals,
            List<String> labels,
            VehicleMotionProperties props,
            double dtSeconds
    ) {
        if (traversals == null || traversals.isEmpty()) {
            throw new IllegalArgumentException("No traversals to plot");
        }
        if (dtSeconds <= 0) {
            throw new IllegalArgumentException("dtSeconds must be > 0");
        }

        final int n = traversals.size();
        List<String> seriesLabels = (labels == null || labels.size() != n)
                ? autoLabels(n)
                : labels;

        NumberAxis timeAxis = new NumberAxis("Time [s]");
        timeAxis.setAutoRangeIncludesZero(true);

        XYPlot accelPlot = new XYPlot();
        XYPlot speedPlot = new XYPlot();
        XYPlot distPlot = new XYPlot();

        accelPlot.setRangeAxis(new NumberAxis("Acceleration [m/s²]"));
        speedPlot.setRangeAxis(new NumberAxis("Speed [m/s]"));
        distPlot.setRangeAxis(new NumberAxis("Distance [m]"));

        // Palette for up to 6 traversals (extend if needed)
        java.awt.Paint[] palette = new java.awt.Paint[]{
                new java.awt.Color(0x1f77b4), // blue
                new java.awt.Color(0xff7f0e), // orange
                new java.awt.Color(0x2ca02c), // green
                new java.awt.Color(0xd62728), // red
                new java.awt.Color(0x9467bd), // purple
                new java.awt.Color(0x8c564b)  // brown
        };

        for (int i = 0; i < n; i++) {
            Traversal tr = traversals.get(i);
            String label = seriesLabels.get(i);

            XYSeriesCollection accelDS = new XYSeriesCollection(seriesFor(tr, label, dtSeconds, Metric.ACCEL));
            XYSeriesCollection speedDS = new XYSeriesCollection(seriesFor(tr, label, dtSeconds, Metric.SPEED));
            XYSeriesCollection distDS = new XYSeriesCollection(seriesFor(tr, label, dtSeconds, Metric.DIST));

            int accelIdx = accelPlot.getDatasetCount();
            int speedIdx = speedPlot.getDatasetCount();
            int distIdx = distPlot.getDatasetCount();

            float width = 2.2f;

            float dash = (i == 0 ? 0f : (i == 1 ? 6f : 3f));
            java.awt.BasicStroke stroke = (dash > 0f)
                    ? new java.awt.BasicStroke(width, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND, 10f, new float[]{dash, 6f}, 0f)
                    : new java.awt.BasicStroke(width, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND);

            org.jfree.chart.renderer.xy.XYLineAndShapeRenderer rA =
                    new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
            rA.setSeriesPaint(0, palette[i % palette.length]);
            rA.setSeriesStroke(0, stroke);
            accelPlot.setRenderer(accelIdx, rA);

            org.jfree.chart.renderer.xy.XYLineAndShapeRenderer rV =
                    new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
            rV.setSeriesPaint(0, palette[i % palette.length]);
            rV.setSeriesStroke(0, stroke);
            speedPlot.setRenderer(speedIdx, rV);

            org.jfree.chart.renderer.xy.XYLineAndShapeRenderer rS =
                    new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
            rS.setSeriesPaint(0, palette[i % palette.length]);
            rS.setSeriesStroke(0, stroke);
            distPlot.setRenderer(distIdx, rS);

            accelPlot.setDataset(accelIdx, accelDS);
            speedPlot.setDataset(speedIdx, speedDS);
            distPlot.setDataset(distIdx, distDS);

            XYLineAndShapeRenderer ra = (XYLineAndShapeRenderer) accelPlot.getRenderer();
            XYLineAndShapeRenderer rv = (XYLineAndShapeRenderer) speedPlot.getRenderer();
            XYLineAndShapeRenderer rs = (XYLineAndShapeRenderer) distPlot.getRenderer();
        }

        addAccelMarkers(accelPlot, props);
        addSpeedMarkers(speedPlot, props);

        CombinedDomainXYPlot combined = new CombinedDomainXYPlot(timeAxis);
        combined.add(accelPlot, 1);
        combined.add(speedPlot, 1);
        combined.add(distPlot, 1);
        combined.setGap(6.0);

        JFreeChart chart = new JFreeChart(
                "Traversal Visualisation",
                new Font("SansSerif", Font.BOLD, 16),
                combined,
                true // legend
        );
        chart.setBackgroundPaint(Color.WHITE);
        combined.setInsets(new RectangleInsets(6, 6, 6, 6));
        return chart;
    }

    public static void saveTraversal(Traversal traversal, VehicleMotionProperties props, String name) throws IOException {
        JFreeChart chart = TraversalPlotter.createChart(List.of(traversal), List.of(""), props, traversal.getTotalDuration() / 1000);
        TraversalPlotter.savePng(chart, 2000, 1500, new File("output/" + name + ".png"));
    }

    public static void savePng(
            JFreeChart chart,
            int width,
            int height,
            File out
    ) throws IOException {
        ChartUtils.saveChartAsPNG(out, chart, width, height);
    }

    public static void visualise(JFreeChart chart) {
        ChartFrame frame = new ChartFrame("", chart);
        frame.setSize(2000, 1500);
        frame.setVisible(true);
    }

    private enum Metric {ACCEL, SPEED, DIST}

    private static XYSeries seriesFor(Traversal tr, String label, double dt, Metric m) {
        XYSeries s = new XYSeries(label);
        double T = tr.getTotalDuration();
        if (DoubleMath.fuzzyEquals(T, 0.0, 1e-12)) {
            // single point if zero duration
            double a = tr.getAccelerationAtTime(0.0);
            double v = tr.getSpeedAtTime(0.0);
            double x = tr.getDistanceAtTime(0.0);
            s.add(0.0, valueOfMetric(m, a, v, x));
            return s;
        }
        int n = Math.max(1, (int) Math.ceil(T / dt));
        double t = 0.0;
        for (int i = 0; i <= n; i++, t += dt) {
            double ti = Math.min(t, T);
            double a = tr.getAccelerationAtTime(ti);
            double v = tr.getSpeedAtTime(ti);
            double x = tr.getDistanceAtTime(ti);
            s.add(ti, valueOfMetric(m, a, v, x));
        }
        return s;
    }

    private static double valueOfMetric(Metric m, double a, double v, double x) {
        return switch (m) {
            case ACCEL -> a;
            case SPEED -> v;
            case DIST -> x;
        };
    }

    private static void addAccelMarkers(XYPlot plot, VehicleMotionProperties p) {
        ValueMarker aMax = new ValueMarker(p.acceleration, new Color(0x555555), dashed());
        aMax.setLabel("a_max");
        aMax.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        aMax.setLabelAnchor(RectangleAnchor.TOP_LEFT);
        aMax.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
        plot.addRangeMarker(aMax);

        ValueMarker aMin = new ValueMarker(p.deceleration, new Color(0x555555), dashed());
        aMin.setLabel("a_min");
        aMin.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        aMin.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
        aMin.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        plot.addRangeMarker(aMin);
    }

    private static void addSpeedMarkers(XYPlot plot, VehicleMotionProperties p) {
        ValueMarker vMax = new ValueMarker(p.maxSpeed, new Color(0x555555), dashed());
        vMax.setLabel("v_max");
        vMax.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        vMax.setLabelAnchor(RectangleAnchor.TOP_LEFT);
        vMax.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
        plot.addRangeMarker(vMax);
    }

    private static BasicStroke dashed() {
        return new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[]{6f, 6f}, 0f);
    }

    private static List<String> autoLabels(int n) {
        List<String> labs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            labs.add("Traversal " + (i + 1));
        }
        return labs;
    }

    public static void main(String[] args) {
        VehicleMotionProperties props = new VehicleMotionProperties(
                0.004,
                2E-6,
                1E-9,
                0
        );
        double distance = 12.176;

        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(
                distance,
                0.002,
                1.6589958605140875E-6,
                props
        );

        Traversal t2 = ConstantAccelerationTraversalCalculator.INSTANCE.create(
                distance,
                0.002,
                1.6589958605140875E-6,
                props);
        JFreeChart chart = TraversalPlotter.createChart(List.of(traversal, t2), List.of("jerk", "acc"), props, 0.01);
        visualise(chart);
    }
}
