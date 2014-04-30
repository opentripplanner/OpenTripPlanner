package org.opentripplanner.routing.trippattern;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;

public class Deduplicator {

    // TODO get rid of static so we don't have a big set lying around, and just for good form.
    private static Map<IntArray, IntArray> canonicalIntArrays = Maps.newHashMap();
    private static Map<String, String> canonicalStrings = Maps.newHashMap();

    /** Free up any memory used by the deduplicator. */
    public static void reset() {
        canonicalIntArrays.clear();
        canonicalStrings.clear();
    }

    /** Used to deduplicate time and stop sequence arrays. The same times may occur in many trips. */
    public static int[] deduplicateIntArray(int[] original) {
        IntArray intArray = new IntArray(original);
        IntArray canonical = canonicalIntArrays.get(intArray);
        if (canonical == null) {
            canonical = intArray;
            canonicalIntArrays.put(canonical, canonical);
        }
        return canonical.array;
    }

    public static String deduplicateString(String original) {
        String canonical = canonicalStrings.get(original);
        if (canonical == null) {
            canonical = original;
            canonicalStrings.put(canonical, canonical);
        }
        return canonical;
    }

    /** A wrapper for a primitive int array. This is insane but necessary in Java. */
    private static class IntArray {
        int[] array;
        public IntArray(int[] array) {
            this.array = array;
        }
        @Override
        public boolean equals (Object other) {
            if (other instanceof IntArray) {
                return Arrays.equals(array, ((IntArray) other).array);
            } else return false;
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }
    }

}
