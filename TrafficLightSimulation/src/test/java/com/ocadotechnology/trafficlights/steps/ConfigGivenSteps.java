/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
package com.ocadotechnology.trafficlights.steps;

import java.util.Map;

import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlights.util.PropertiesUtil;

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
