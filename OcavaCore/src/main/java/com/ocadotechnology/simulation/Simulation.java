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
package com.ocadotechnology.simulation;

/**
 * Marker interface for designating an object as the top level container for an Ocava-backed simulation.
 *
 * This is intended to be used with the `SimulationAPI` class in the ScenarioTest framework to allow the steps to gain
 * access to the simulation objects for their logic.
 */
public interface Simulation {
}
