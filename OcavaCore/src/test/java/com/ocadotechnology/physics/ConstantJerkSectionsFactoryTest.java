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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

class ConstantJerkSectionsFactoryTest {
    private static final double ROUNDING_ERROR_TOLERANCE = 1e-9;
    private static final VehicleMotionProperties propsDefault = new VehicleMotionProperties(
            /*acceleration*/                2.5,
            /*accelerationAbsoluteTolerance*/1e-9,
            /*deceleration*/               -2.0,
            /*decelerationAbsoluteTolerance*/1e-9,
            /*maxSpeed*/                    8.0,
            /*maxSpeedAbsoluteTolerance*/   1e-9,
            /*jerkAccelerationUp*/          3.6,
            /*jerkAccelerationDown*/       -3.4,
            /*jerkDecelerationUp*/         -2.2,
            /*jerkDecelerationDown*/        1.2
    );

    @Test
    void firstHalf_whenAccNegative_thenOneSegmentRelaxToZeroAccel() {
        double a0 = -1.0; // already braking mildly
        double v0 = 2.0;
        // Choose vTarget == v_N (speed after relaxing a→0 only). We can estimate it here,
        // but the test only checks that exactly one segment is returned.
        // To be robust, pick a small delta so the solver lands on the 1-segment branch.
        double vTarget = v0 - (a0 * a0) / (2.0 * propsDefault.jerkDecelerationDown); // v_N

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertEquals(1, secs.size(), "Expected a single jerk segment to neutralize acceleration");
        assertSmoothConnected(secs);
        assertEndsAt(secs, vTarget);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void firstHalf_whenAccNegative_thenThreeSegmentsTriangularPositiveAccel() {
        double a0 = -1.5;
        double v0 = 1.0;

        // Pick vTarget modestly above v_N so we need a triangular +accel bump (no a_max plateau)
        double vN = v0 - (a0 * a0) / (2.0 * propsDefault.jerkDecelerationDown);
        double vTarget = vN + 0.2; // small extra Δv

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertEquals(3, secs.size(), "Expected 3 segments: dec-down → acc-up → acc-down");
        assertSmoothConnected(secs);
        assertEndsAt(secs, vTarget);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void firstHalf_whenAccNegative_thenFourSegmentsTrapezoidWithAccelPlateau() {
        double a0 = -0.8;
        double v0 = 1.5;

        // Push vTarget high enough to require hitting a_max and holding a plateau
        double vN = v0 - (a0 * a0) / (2.0 * propsDefault.jerkDecelerationDown);
        double vTarget = vN + 2.0; // large Δv → plateau likely

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertTrue(
                secs.size() == 4,
                "Expected 4 segments, got: " + secs.size());
        assertSmoothConnected(secs);
        assertEndsAt(secs, vTarget);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void firstHalf_whenNegativeAccelerationAndVelocityReachesZeroBeforeZeroAcc_thenThrows() {
        double a0 = -2.0; // strong braking
        double v0 = 0.5;  // small speed; v(t) can hit 0 before a→0
        double vN = v0 - (a0 * a0) / (2.0 * propsDefault.jerkDecelerationDown);
        double vTarget = Math.max(0.1, vN + 0.05);
        Assertions.assertThrows(Exception.class, () -> ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault));
    }

    // --------------------------------------------------------------------------------------------
    // TESTS — a0 ≥ 0 (already in positive-accel regime)
    // --------------------------------------------------------------------------------------------

    @Test
    void firstHalf_whenAccPositive_thenOneSegmentRampDownToZeroAccel() {
        double a0 = 1.2;
        double v0 = 3.0;

        // v_D after ramping down to a=0 using jAccDown<0
        double vTarget = v0 + a0 * (a0 / -propsDefault.jerkAccelerationDown) + 0.5 * propsDefault.jerkAccelerationDown * Math.pow(a0 / -propsDefault.jerkAccelerationDown, 2);

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertEquals(1, secs.size(), "Expected a single jerk-down segment to a=0");
        assertSmoothConnected(secs);
        assertEndsAt(secs, vTarget);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void firstHalf_whenAccPositive_thenTwoSegmentsTriangularThenDown() {
        double a0 = 0.3;
        double v0 = 2.0;

        // Slightly above v_D so we need an up-then-down triangular bump, but not a_max
        double tDown = a0 / -propsDefault.jerkAccelerationDown;
        double vD = v0 + a0 * tDown + 0.5 * propsDefault.jerkAccelerationDown * tDown * tDown;
        double vTarget = vD + 0.2;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertEquals(2, secs.size(), "Expected 2 segments: acc-up then acc-down");
        assertSmoothConnected(secs);
        assertEndsAt(secs, vTarget);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void firstHalf_whenAccPositive_thenThreeSegmentsTrapezoidWithPlateau() {
        double a0 = 0.1;
        double v0 = 2.0;

        // Push vTarget high to require hitting a_max and holding
        double tDown = a0 / -propsDefault.jerkAccelerationDown;
        double vD = v0 + a0 * tDown + 0.5 * propsDefault.jerkAccelerationDown * tDown * tDown;
        double vTarget = vD + 2.0;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertTrue(
                secs.size() == 3,
                "Expected 3 segments, got: " + secs.size());
        assertSmoothConnected(secs);
        assertEndsAt(secs, vTarget);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    // --------------------------------------------------------------------------------------------
    // TESTS — end-state & impossible requests
    // --------------------------------------------------------------------------------------------

    @Test
    void firstHalf_whenEndStateIsAccZeroAndVTarget() {
        double a0 = -0.6;
        double v0 = 1.8;
        double vN = v0 - (a0 * a0) / (2.0 * propsDefault.jerkDecelerationDown);
        double vTarget = vN + 0.35;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);

        assertEndsAt(secs, vTarget);
        assertSmoothConnected(secs);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void firstHalf_whenTargetSpeedLessThanSpeedReachedOnceZeroAccReached_thenHandles() {
        double a0 = -1.2;
        double v0 = 1.0;

        // Minimal feasible apex at a=0 occurs at vN after relaxing braking.
        double vN = v0 - (a0 * a0) / (2.0 * propsDefault.jerkDecelerationDown);

        double vTarget = vN - 1e-2; // slightly *below* feasible

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(a0, v0, vTarget, propsDefault);
        assertEndsAt(secs, vTarget);
        assertSmoothConnected(secs);
        assertRespectsConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void secondHalf_whenZeroSpeed_thenReturnsEmptyOrZeroDuration() {
        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(0.0, propsDefault);

        Traversal tr = asTraversal(secs);
        assertTrue(
                secs.isEmpty() || DoubleMath.fuzzyEquals(tr.getTotalDuration(), 0.0, ROUNDING_ERROR_TOLERANCE),
                "Zero v0 should produce no movement");
        if (!secs.isEmpty()) {
            assertSmoothConnected(secs);
            assertEndsAt(secs, 0);
            assertRespectsBrakeConstraints(tr, propsDefault);
        }
    }

    @Test
    void secondHalf_whenSmallSpeed_thenTriangularTwoJerkSegments() {
        // Pick a small v0 so we won't need an a_min plateau
        double v0 = 0.6;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(v0, propsDefault);

        // Typical triangular decel: jMoreDecel (<0) then jLessDecel (>0)
        assertTrue(
                secs.size() == 2,
                "Expected 2 jerk segments, got " + secs.size());
        assertSmoothConnected(secs);
        assertEndsAt(secs, 0);
        assertRespectsBrakeConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void secondHalf_whenLargerSpeed_thenTrapezoidWithAccMinPlateau() {
        // Choose a bigger v0 so we likely hit a_min and hold (3 segments)
        double v0 = 3.5;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(v0, propsDefault);

        // Expect: jerk to aMin, constant-accel at aMin, jerk back up to zero accel (3 segments)
        // Allow 2 segments if the plateau collapses (edge of regime).
        assertTrue(
                secs.size() == 3,
                "Expected 3 segments, got " + secs.size());
        assertSmoothConnected(secs);
        assertEndsAt(secs, 0);
        assertRespectsBrakeConstraints(asTraversal(secs), propsDefault);
    }

    @Test
    void secondHalf_whenTotalDeltaVMatchesV0_thenOk() {
        double v0 = 1.75;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(v0, propsDefault);
        Traversal tr = asTraversal(secs);

        // v0 should be shed entirely
        double vEnd = tr.getSpeedAtTime(tr.getTotalDuration());
        assertEquals(0, DoubleMath.fuzzyCompare(vEnd, 0.0, ROUNDING_ERROR_TOLERANCE), "Did not fully shed speed");

        // Sampling check: speed never exceeds initial v0
        int samples = 200;
        for (int i = 0; i <= samples; i++) {
            double t = tr.getTotalDuration() * i / samples;
            double v = tr.getSpeedAtTime(t);
            assertTrue(
                    DoubleMath.fuzzyCompare(v, v0, tol(propsDefault.maxSpeed)) <= 0,
                    "Speed exceeded initial v0 during braking");
        }
    }

    @Test
    void secondHalf_testConstraintsHoldEvenWithHighV0() {
        // v0 below vMax but high enough to exercise bounds
        double v0 = 7.5;

        List<TraversalSection> secs = ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(v0, propsDefault);
        Traversal tr = asTraversal(secs);

        assertSmoothConnected(secs);
        assertEndsAt(secs, 0);
        assertRespectsBrakeConstraints(tr, propsDefault);
    }

    /**
     * Asserts adjacent sections join smoothly in v and a, and all durations positive.
     */
    public static void assertSmoothConnected(List<TraversalSection> secs) {
        assertFalse(secs.isEmpty(), "No sections produced");
        for (TraversalSection s : secs) {
            assertTrue(
                    DoubleMath.fuzzyCompare(s.getDuration(), 0.0, tol(0.0)) > 0,
                    "Non-positive duration in section");
        }
        for (int i = 0; i + 1 < secs.size(); i++) {
            TraversalSection a = secs.get(i);
            TraversalSection b = secs.get(i + 1);
            double aEnd = a.getAccelerationAtTime(a.getDuration());
            double bStart = b.getAccelerationAtTime(0.0);
            double vaEnd = a.getSpeedAtTime(a.getDuration());
            double vbStart = b.getSpeedAtTime(0.0);

            assertEquals(0, DoubleMath.fuzzyCompare(aEnd, bStart, tol(Math.max(Math.abs(aEnd), Math.abs(bStart)))),
                    "Acceleration discontinuity between sections " + i + " and " + (i + 1));
            assertEquals(0, DoubleMath.fuzzyCompare(vaEnd, vbStart, tol(Math.max(Math.abs(vaEnd), Math.abs(vbStart)))),
                    "Speed discontinuity between sections " + i + " and " + (i + 1));
        }
    }

    /**
     * End state must be (v≈vTarget, a≈0).
     */
    private static void assertEndsAt(List<TraversalSection> secs, double vTarget) {
        TraversalSection last = secs.get(secs.size() - 1);
        double aEnd = last.getAccelerationAtTime(last.getDuration());
        double vEnd = last.getSpeedAtTime(last.getDuration());
        assertEquals(0, DoubleMath.fuzzyCompare(aEnd, 0.0, ROUNDING_ERROR_TOLERANCE * propsDefault.acceleration), "Final acceleration not ~0");
        assertEquals(0, DoubleMath.fuzzyCompare(vEnd, vTarget, ROUNDING_ERROR_TOLERANCE * propsDefault.maxSpeed),
                "Final speed not ~vTarget; got " + vEnd + " target " + vTarget);
    }

    private static Traversal asTraversal(List<TraversalSection> sections) {
        return new Traversal(ImmutableList.copyOf(sections));
    }

    /**
     * Asserts constraints v∈[0,vmax], a∈[aMin,aMax] sampled across time.
     */
    private static void assertRespectsConstraints(Traversal tr, VehicleMotionProperties propsDefault) {
        double aMax = propsDefault.acceleration;
        double aMin = propsDefault.deceleration; // negative
        double vMax = propsDefault.maxSpeed;

        int samples = 200;
        double dt = tr.getTotalDuration() / samples;
        double t = 0.0;
        for (int i = 0; i <= samples; i++, t += dt) {
            double v = tr.getSpeedAtTime(Math.min(t, tr.getTotalDuration()));
            double a = tr.getAccelerationAtTime(Math.min(t, tr.getTotalDuration()));
            assertTrue(
                    DoubleMath.fuzzyCompare(v, 0.0, tol(v)) >= 0,
                    "Speed went negative at t=" + t + " v=" + v);
            assertTrue(
                    DoubleMath.fuzzyCompare(v, vMax, tol(vMax)) <= 0,
                    "Speed exceeded max at t=" + t + " v=" + v);
            assertTrue(
                    DoubleMath.fuzzyCompare(a, aMin, tol(Math.abs(aMin))) >= 0,
                    "Acceleration below min at t=" + t + " a=" + a + " < " + aMin);
            assertTrue(
                    DoubleMath.fuzzyCompare(a, aMax, tol(aMax)) <= 0,
                    "Acceleration above max at t=" + t + " a=" + a + " > " + aMax);
        }
    }

    private static void assertRespectsBrakeConstraints(Traversal tr, VehicleMotionProperties propsDefault) {
        double aMax = propsDefault.acceleration;
        double aMin = propsDefault.deceleration; // negative
        double vMax = propsDefault.maxSpeed;

        int samples = 240;
        double dt = tr.getTotalDuration() / samples;
        double prevV = Double.POSITIVE_INFINITY;

        for (int i = 0; i <= samples; i++) {
            double t = Math.min(tr.getTotalDuration(), i * dt);
            double v = tr.getSpeedAtTime(t);
            double a = tr.getAccelerationAtTime(t);

            assertTrue(DoubleMath.fuzzyCompare(v, 0.0, tol(v)) >= 0, "v<0 at t=" + t + " v=" + v);
            assertTrue(DoubleMath.fuzzyCompare(v, vMax, tol(vMax)) <= 0, "v>vMax at t=" + t + " v=" + v);
            assertTrue(DoubleMath.fuzzyCompare(a, aMin, tol(Math.abs(aMin))) >= 0, "a<aMin at t=" + t + " a=" + a);
            assertTrue(DoubleMath.fuzzyCompare(a, aMax, tol(aMax)) <= 0, "a>aMax at t=" + t + " a=" + a);

            assertTrue(
                    DoubleMath.fuzzyCompare(v, prevV, tol(propsDefault.maxSpeed)) <= 0,
                    "Speed increased during braking at t=" + t + " v=" + v + " prev=" + prevV);

            prevV = v;
        }
    }

    private static double tol(double x) {
        return Math.abs(x) * ROUNDING_ERROR_TOLERANCE;
    }
}