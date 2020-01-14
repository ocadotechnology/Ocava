package com.ocadotechnology.trafficlightsimulation.steps;

import java.util.Map;

import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlightsimulation.util.PropertiesUtil;

public class ConfigGivenSteps {
    private StepManager stepManager;
    private Map<String, String> propertiesMap;

    public ConfigGivenSteps(StepManager stepManager, Map<String, String> configMap) {
        this.stepManager = stepManager;
        this.propertiesMap = configMap;
    }

    public void set(Enum<?> key, String value) {
        set(PropertiesUtil.getPropertyName(key), value);
    }

    public void set(Enum<?> key, Object value) {
        set(key, value.toString());
    }

    private void set(String key, String value) {
        stepManager.addExecuteStep(() -> propertiesMap.put(key, value));
    }
}
