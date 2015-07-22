package org.opentripplanner.analyst.core;

import junit.framework.TestCase;
import org.junit.Test;

public class WeightingFunctionTest extends TestCase {
    // two hours of seconds
    public static int[] weights = new int[7200];

    static {
        weights[3601] = 1000;
    }

    @Test
    public static void testSharp () {
        int[] output = new WeightingFunction.SharpCutoff().apply(weights);

        // should be by minute
        assertEquals(120, output.length);

        int cumulative = 0;

        for (int i = 0; i < 60; i++) {
            cumulative += output[i];
            assertEquals(String.format("minute %s should be 0", i), 0, cumulative);
        }

        for (int i = 60; i < 120; i++) {
            cumulative += output[i];
            assertEquals(String.format("minute %s should be 100", i), 1000, cumulative);
        }
    }

    @Test
    public static void testLogistic () {
        double slope = -2.0 / 60;
        int[] output = new WeightingFunction.Logistic(slope).apply(weights);

        int cumulative = 0;

        assertEquals(output.length, 120);

        // the cumulative value at each minute should be the value of the logistic function centered on 3601 seconds
        // (since that is where our impulse is)
        for (int min = 0; min < output.length; min++) {
            int sec = min * 60 - 3601;

            cumulative += output[min];

            // account for int roundoff
            assertTrue(String.format("minute %s", min), Math.abs((int) (1 / (1 + Math.exp(slope * sec)) * 1000) - cumulative) <= 1);
        }

    }
}