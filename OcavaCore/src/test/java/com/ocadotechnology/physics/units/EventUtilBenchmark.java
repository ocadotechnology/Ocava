/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
package com.ocadotechnology.physics.units;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.ocadotechnology.event.EventUtil;

@State(Scope.Benchmark)
public class EventUtilBenchmark {

    @State(Scope.Thread)
    public static class MyState {
        public double theDouble = 1.5d;
        public long theLong = 1L;
    }

    @Benchmark
    public void branchEventTimeDoubleToString(MyState state, Blackhole blackhole) {
        blackhole.consume(EventUtil.eventTimeToString(state.theDouble));
    }

    @Benchmark
    public void branchEventTimeLongToString(MyState state, Blackhole blackhole) {
        blackhole.consume(EventUtil.eventTimeToString(state.theLong));
    }
}
