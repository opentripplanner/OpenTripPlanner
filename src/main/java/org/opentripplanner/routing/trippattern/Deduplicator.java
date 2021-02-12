package org.opentripplanner.routing.trippattern;

import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Does the same thing as String.intern, but for several different types.
 * Java's String.intern uses perm gen space and is broken anyway.
 */
public class Deduplicator implements Serializable {
    private static final long serialVersionUID = 20140524L;

    private final Map<BitSet, BitSet> canonicalBitSets = Maps.newHashMap();
    private final Map<IntArray, IntArray> canonicalIntArrays = Maps.newHashMap();
    private final Map<String, String> canonicalStrings = Maps.newHashMap();
    private final Map<StringArray, StringArray> canonicalStringArrays = Maps.newHashMap();

    // Other none basic types
    private final Map<Class<?>, Map<?, ?>> canonicalObjects = new HashMap<>();
    private final Map<Class<?>, Map<List<?>, List<?>>> canonicalLists = new HashMap<>();

    /** Free up any memory used by the deduplicator. */
    public void reset() {
        canonicalBitSets.clear();
        canonicalIntArrays.clear();
        canonicalStrings.clear();
        canonicalStringArrays.clear();
        canonicalObjects.clear();
        canonicalLists.clear();
    }

    @Nullable
    public BitSet deduplicateBitSet(BitSet original) {
        if (original == null) { return null; }
        BitSet canonical = canonicalBitSets.get(original);
        if (canonical == null) {
            canonical = original;
            canonicalBitSets.put(canonical, canonical);
        }
        return canonical;
    }

    /** Used to deduplicate time and stop sequence arrays. The same times may occur in many trips. */
    @Nullable
    public int[] deduplicateIntArray(int[] original) {
        if (original == null) { return null; }
        IntArray intArray = new IntArray(original);
        IntArray canonical = canonicalIntArrays.get(intArray);
        if (canonical == null) {
            canonical = intArray;
            canonicalIntArrays.put(canonical, canonical);
        }
        return canonical.array;
    }

    @Nullable
    public String deduplicateString(String original) {
        if (original == null) { return null; }
        String canonical = canonicalStrings.putIfAbsent(original, original);
        return canonical == null ? original : canonical;
    }

    @Nullable
    public String[] deduplicateStringArray(String[] original) {
        if (original == null) { return null; }
        StringArray canonical = canonicalStringArrays.get(new StringArray(original, false));
        if (canonical == null) {
            canonical = new StringArray(original, true);
            canonicalStringArrays.put(canonical, canonical);
        }
        return canonical.array;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T deduplicateObject(Class<T> cl, T original) {
        if (original == null) { return null; }
        Map<T, T> objects = (Map<T, T>) canonicalObjects.computeIfAbsent(cl, c -> new HashMap<T, T>());
        T canonical = objects.putIfAbsent(original, original);
        return canonical == null ? original : canonical;
    }

    @Nullable
    public <T> List<T> deduplicateImmutableList(Class<T> clazz, List<T> original) {
        if (original == null) { return null; }

        Map<List<?>, List<?>> canonicalLists = this.canonicalLists.computeIfAbsent(
            clazz, key -> new HashMap<>()
        );

        @SuppressWarnings("unchecked")
        List<T> canonical = (List<T>) canonicalLists.get(original);
        if(canonical == null) {
            // The list may contain nulls, hence the use of the old unmodifiable wrapper
            //noinspection FuseStreamOperations
            canonical = Collections.unmodifiableList(
                original
                    .stream()
                    .map(it -> deduplicateObject(clazz, it))
                    .collect(Collectors.toList())
            );
            canonicalLists.put(canonical, canonical);
        }
        return canonical;
    }


    /* private classes */

    /** A wrapper for a primitive int array. This is insane but necessary in Java. */
    private static class IntArray implements Serializable {
        private static final long serialVersionUID = 20140524L;
        final int[] array;

        IntArray(int[] array) {
            this.array = array;
        }

        @Override
        public boolean equals (Object other) {
            if (!(other instanceof IntArray)) { return false; }

            IntArray that = (IntArray) other;
            return Arrays.equals(array, that.array);
        }

        @Override
        public int hashCode() { return Arrays.hashCode(array); }
    }

    /** A wrapper for a String array. Optionally, the individual Strings may be deduplicated too. */
    private class StringArray implements Serializable {
        private static final long serialVersionUID = 20140524L;
        final String[] array;

        StringArray(String[] array, boolean deduplicateStrings) {
            if (deduplicateStrings) {
                this.array = new String[array.length];
                for (int i = 0; i < array.length; i++) {
                    this.array[i] = deduplicateString(array[i]);
                }
            } else {
                this.array = array;
            }
        }
        @Override
        public boolean equals (Object other) {
            if(!(other instanceof StringArray)) {
                return false;
            }
            StringArray that = (StringArray) other;
            return Arrays.equals(array, that.array);
        }
        @Override
        public int hashCode() { return Arrays.hashCode(array); }
    }
}
