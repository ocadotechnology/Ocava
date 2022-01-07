/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public enum TraversalGraphPlotter {
    INSTANCE;

    public void plot(Traversal traversal) {
        XYSeries speedAtTime = new XYSeries("Speed-time");
        XYSeries distanceAtTime = new XYSeries("Distance-time");

        double traversalDuration = traversal.getTotalDuration();
        for (double t = 0; t < traversalDuration; t += 0.001) {
            speedAtTime.add(t, traversal.getSpeedAtTime(t));
            distanceAtTime.add(t, traversal.getDistanceAtTime(t));
        }

        XYSeries speedAtDistance = new XYSeries("Speed-distance");

        double totalTraversalDistance = traversal.getTotalDistance();
        for (double d = 0; d < totalTraversalDistance; d += 0.001) {
            speedAtDistance.add(d, traversal.getSpeedAtDistance(d));
        }

        XYSeriesCollection xyDataset = new XYSeriesCollection();
        xyDataset.addSeries(speedAtTime);

        JFreeChart chart = ChartFactory.createXYLineChart("Traversal", "Time",
                "Speed", xyDataset, PlotOrientation.VERTICAL, true, false,
                false);
        chart.setBackgroundPaint(Color.yellow);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.GREEN);
        plot.setRangeGridlinePaint(Color.orange);
        plot.setAxisOffset(new RectangleInsets(50, 0, 20, 5));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                .getRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);

        ChartFrame frame = new ChartFrame("ChartFrame", chart);
        frame.setSize(450, 250);
        frame.setVisible(true);
    }

    public void plotWithClearances(Traversal fullTraversal, TraversalCalculator traversalCalculator, VehicleMotionProperties vehicleProperties, double gridPositionSizeInDirectionOfTravel) {
        XYSeries timeAtDistanceInCells = new XYSeries("Distance-time");

        double fullTraversalDuration = fullTraversal.getTotalDuration();
        for (double t = 0; t < fullTraversalDuration; t += 0.001) {
            timeAtDistanceInCells.add(t, fullTraversal.getDistanceAtTime(t) / gridPositionSizeInDirectionOfTravel);
        }

        XYSeriesCollection xyDataset = new XYSeriesCollection();
        xyDataset.addSeries(timeAtDistanceInCells);

        double fullTraversalDistance = fullTraversal.getTotalDistance();
        for (int i = 1; gridPositionSizeInDirectionOfTravel * i < fullTraversalDistance; i++) {
            double timeUntilBrake = fullTraversal.getTimeAtDistance(gridPositionSizeInDirectionOfTravel * i) + 0.525; //TODO: Get numbers from somewhere sane
            double distanceUntilBrake = fullTraversal.getDistanceAtTime(timeUntilBrake);
            Traversal brakingTraversal = traversalCalculator.getBrakingTraversal(fullTraversal, distanceUntilBrake, vehicleProperties);

            double brakingTraversalDuration = brakingTraversal.getTotalDuration();
            XYSeries series = new XYSeries("P-"+i);
            for (double t = 0; t < brakingTraversalDuration; t += 0.001) {
                series.add(t+timeUntilBrake, (distanceUntilBrake + brakingTraversal.getDistanceAtTime(t)) / gridPositionSizeInDirectionOfTravel);
            }
            xyDataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart("Traversal", "Time", "Grid Positions", xyDataset, PlotOrientation.VERTICAL, true, false, false);
        chart.setBackgroundPaint(Color.yellow);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.GREEN);
        plot.setRangeGridlinePaint(Color.orange);
        plot.setAxisOffset(new RectangleInsets(50, 0, 20, 5));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);

        ChartFrame frame = new ChartFrame("ChartFrame", chart);
        frame.setSize(450, 250);
        frame.setVisible(true);
    }
}
