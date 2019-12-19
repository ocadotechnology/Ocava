package com.ocadotechnology.time;

public class ScalableOffsetUtcTimeProvider extends OffsetUtcTimeProvider {

    private final double delta;

    public ScalableOffsetUtcTimeProvider(double simulationStartTime, double delta) {
        super(simulationStartTime / delta);

        this.delta = delta;
    }

    @Override
    public double getTime() {
        return super.getTime() * delta;
    }
}
