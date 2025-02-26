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

import java.awt.Color;
import java.util.function.ToDoubleFunction;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ConstantJerkTraversalPlotter {
    public static void main(String[] args) {
        double acceleration = 2.5E-6;
        double deceleration = -2E-6;
        double maxSpeed = 8E-3;

        double jerkAccelerationUp = 2E-9;
        double jerkAccelerationDown = -2E-9;
        double jerkDecelerationUp = -2E-9;
        double jerkDecelerationDown = 2E-9;

        double errorFraction = 0.01;

        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(
                maxSpeed,
                acceleration,
                deceleration,
                errorFraction,
                jerkAccelerationUp,
                jerkAccelerationDown,
                jerkDecelerationUp,
                jerkDecelerationDown);
        plotTraversal(ConstantJerkTraversalCalculator.INSTANCE.create(100d, vehicleMotionProperties));

    }

    public static void plotTraversal(Traversal traversal) {
        XYPlot distancePlot = createLinePlotForMetric(traversal, "Distance", traversal::getDistanceAtTime);
        XYPlot speedPlot = createLinePlotForMetric(traversal, "Speed", traversal::getSpeedAtTime);
        XYPlot accPlot = createLinePlotForMetric(traversal, "Acceleration", traversal::getAccelerationAtTime);
        NumberAxis xAxis = new NumberAxis("Time");

        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(xAxis);
        combinedPlot.add(distancePlot, 1);
        combinedPlot.add(speedPlot, 1);
        combinedPlot.add(accPlot, 1);
        combinedPlot.setGap(10);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart("Distance, Speed and Acceleration over time", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);

        ChartFrame frame = new ChartFrame("", chart);
        frame.setSize(1100, 1000);
        frame.setVisible(true);
    }

    private static XYPlot createLinePlotForMetric(Traversal traversal, String metricName, ToDoubleFunction<Double> distanceToMetric) {
        XYSeries series = new XYSeries(metricName);
        double totalTraversalDistance = traversal.getTotalDuration();
        double distanceDelta = 0.01;
        for (double d = 0; d < totalTraversalDistance; d += distanceDelta) {
            series.add(d, distanceToMetric.applyAsDouble(d));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        XYPlot plot = new XYPlot(dataset, null, new NumberAxis(metricName), new XYLineAndShapeRenderer(true, false));
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray);
        plot.setRangeGridlinePaint(Color.gray);
        return plot;
    }
}
