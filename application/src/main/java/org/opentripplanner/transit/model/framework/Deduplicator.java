package org.opentripplanner.transit.model.framework;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Does the same thing as String.intern, but for several different types. Java's String.intern uses
 * perm gen space and is broken anyway.
 */
public class Deduplicator implements DeduplicatorService, Serializable {

  private static final String ZERO_COUNT = sizeAndCount(0, 0);

  private final Map<BitSet, BitSet> canonicalBitSets = new HashMap<>();
  private final Map<IntArray, IntArray> canonicalIntArrays = new HashMap<>();
  private final Map<String, String> canonicalStrings = new HashMap<>();
  private final Map<StringArray, StringArray> canonicalStringArrays = new HashMap<>();
  private final Map<String2DArray, String2DArray> canonicalString2DArrays = new HashMap<>();
  private final Map<Class<?>, Map<?, ?>> canonicalObjects = new HashMap<>();
  private final Map<Class<?>, Map<?, ?>> canonicalObjArrays = new HashMap<>();
  private final Map<Class<?>, Map<List<?>, List<?>>> canonicalLists = new HashMap<>();

  private final Map<String, Integer> effectCounter = new HashMap<>();

  @Inject
  public Deduplicator() {}

  /** Free up any memory used by the deduplicator. */
  public void reset() {
    canonicalBitSets.clear();
    canonicalIntArrays.clear();
    canonicalStrings.clear();
    canonicalStringArrays.clear();
    canonicalString2DArrays.clear();
    canonicalObjects.clear();
    canonicalObjArrays.clear();
    canonicalLists.clear();
  }

  @Override
  @Nullable
  public BitSet deduplicateBitSet(BitSet original) {
    if (original == null) {
      return null;
    }
    BitSet canonical = canonicalBitSets.get(original);
    if (canonical == null) {
      canonical = original;
      canonicalBitSets.put(canonical, canonical);
    }
    incrementEffectCounter(BitSet.class);
    return canonical;
  }

  @Override
  @Nullable
  public int[] deduplicateIntArray(int[] original) {
    if (original == null) {
      return null;
    }
    IntArray intArray = new IntArray(original);
    IntArray canonical = canonicalIntArrays.get(intArray);
    if (canonical == null) {
      canonical = intArray;
      canonicalIntArrays.put(canonical, canonical);
    }
    incrementEffectCounter(IntArray.class);
    return canonical.array;
  }

  @Override
  @Nullable
  public String deduplicateString(String original) {
    if (original == null) {
      return null;
    }
    String canonical = canonicalStrings.putIfAbsent(original, original);
    incrementEffectCounter(String.class);
    return canonical == null ? original : canonical;
  }

  @Override
  @Nullable
  public String[] deduplicateStringArray(String[] original) {
    if (original == null) {
      return null;
    }
    StringArray canonical = canonicalStringArrays.get(new StringArray(original));
    if (canonical == null) {
      canonical = StringArray.deepDeduplicateOf(original, this);
      canonicalStringArrays.put(canonical, canonical);
    }
    incrementEffectCounter(StringArray.class);
    return canonical.array;
  }

  @Override
  @Nullable
  public String[][] deduplicateString2DArray(String[][] original) {
    if (original == null) {
      return null;
    }
    String2DArray canonical = canonicalString2DArrays.get(new String2DArray(original));
    if (canonical == null) {
      canonical = String2DArray.deepDeduplicateOf(original, this);
      canonicalString2DArrays.put(canonical, canonical);
    }
    incrementEffectCounter(String2DArray.class);
    return canonical.array;
  }

  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T deduplicateObject(Class<T> cl, T original) {
    if (String.class == cl) {
      throw new IllegalArgumentException("Use #deduplicateString() instead.");
    }
    if (original == null) {
      return null;
    }
    Map<T, T> objects = (Map<T, T>) canonicalObjects.computeIfAbsent(cl, c -> new HashMap<T, T>());
    T canonical = objects.putIfAbsent(original, original);
    incrementEffectCounter(objCounterName(cl));
    return canonical == null ? original : canonical;
  }

  @Override
  @Nullable
  public <T> T[] deduplicateObjectArray(Class<T> type, T[] original) {
    if (original == null) {
      return null;
    }
    Map<ObjArray<T>, ObjArray<T>> map;
    if (canonicalObjArrays.containsKey(type)) {
      //noinspection unchecked
      map = (Map<ObjArray<T>, ObjArray<T>>) canonicalObjArrays.get(type);
    } else {
      map = new HashMap<>();
      canonicalObjArrays.put(type, map);
    }
    ObjArray<T> canonical = map.get(new ObjArray<>(original));

    if (canonical == null) {
      canonical = ObjArray.deepDeduplicateOf(type, original, this);
      map.put(canonical, canonical);
    }
    incrementEffectCounter(arrayCounterName(type));
    return canonical.array();
  }

  @Override
  @Nullable
  public <T> List<T> deduplicateImmutableList(Class<T> clazz, List<T> original) {
    if (original == null) {
      return null;
    }

    Map<List<?>, List<?>> canonicalLists =
      this.canonicalLists.computeIfAbsent(clazz, key -> new HashMap<>());

    @SuppressWarnings("unchecked")
    List<T> canonical = (List<T>) canonicalLists.get(original);
    if (canonical == null) {
      // The list may contain nulls, hence the use of the old unmodifiable wrapper
      boolean containsNull = original.stream().anyMatch(Objects::isNull);
      Stream<T> stream = original.stream().map(it -> deduplicateObject(clazz, it));
      // The list may contain nulls, hence the use of the old unmodifiable wrapper
      //noinspection SimplifyStreamApiCallChains
      canonical = containsNull
        ? Collections.unmodifiableList(stream.collect(Collectors.toList()))
        : stream.collect(Collectors.toUnmodifiableList());
      canonicalLists.put(canonical, canonical);
    }

    incrementEffectCounter(listCounterName(clazz));
    return canonical;
  }

  /**
   * Returns a string with the size of each canonical collection.
   */
  @Override
  public String toString() {
    var builder = ToStringBuilder.of(Deduplicator.class)
      .addObj("BitSet", sizeAndCount(canonicalBitSets.size(), BitSet.class), ZERO_COUNT)
      .addObj("int[]", sizeAndCount(canonicalIntArrays.size(), IntArray.class), ZERO_COUNT)
      .addObj("String", sizeAndCount(canonicalStrings.size(), String.class), ZERO_COUNT)
      .addObj("String[]", sizeAndCount(canonicalStringArrays.size(), StringArray.class), ZERO_COUNT)
      .addObj(
        "String[][]",
        sizeAndCount(canonicalString2DArrays.size(), String2DArray.class),
        ZERO_COUNT
      );
    addToBuilder(builder, canonicalObjects, Deduplicator::objCounterName);
    addToBuilder(builder, canonicalObjArrays, Deduplicator::arrayCounterName);
    addToBuilder(builder, canonicalLists, Deduplicator::listCounterName);

    return builder.toString();
  }

  /* private members */

  private static <T> String objCounterName(Class<T> type) {
    return type.getSimpleName();
  }

  private static <T> String listCounterName(Class<T> type) {
    return "List<" + type.getSimpleName() + ">";
  }

  private static <T> String arrayCounterName(Class<T> type) {
    return type.getSimpleName() + "[]";
  }

  /**
   * Add all entries sorted by the {@code toName} function with count to builder.
   */
  private <K, V extends Map<?, ?>> void addToBuilder(
    ToStringBuilder builder,
    Map<K, V> map,
    Function<K, String> toName
  ) {
    map
      .entrySet()
      .stream()
      .map(e -> new NameSize(toName.apply(e.getKey()), e.getValue().size()))
      .sorted(Comparator.comparing(NameSize::name))
      .forEach(it -> builder.addObj(it.name(), sizeAndCount(it.size(), it.name()), ZERO_COUNT));
  }

  private void incrementEffectCounter(Class<?> clazz) {
    incrementEffectCounter(clazz.getName());
  }

  private void incrementEffectCounter(String key) {
    // Count the first element, start at 1
    effectCounter.compute(key, (k, v) -> v == null ? 1 : ++v);
  }

  private String sizeAndCount(int size, Class<?> clazz) {
    return sizeAndCount(size, clazz.getName());
  }

  private String sizeAndCount(int size, String key) {
    int count = effectCounter.getOrDefault(key, 0);
    return sizeAndCount(size, count);
  }

  private static String sizeAndCount(int size, int count) {
    return size + "(" + count + ")";
  }

  /* private classes */

  /** A wrapper for a primitive int array. This is insane but necessary in Java. */
  private record IntArray(int[] array) implements Serializable {
    @Override
    public int hashCode() {
      return Arrays.hashCode(array);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof IntArray that) {
        return Arrays.equals(array, that.array);
      }
      return false;
    }
  }

  /**
   * A wrapper around an arrays. Use {@code deepDeduplicateOf()} to deduplicate the elements
   * as well.
   */
  private record ObjArray<T>(T[] array) implements Serializable {
    private static <E> ObjArray<E> deepDeduplicateOf(
      Class<E> type,
      E[] array,
      Deduplicator deduplicator
    ) {
      E[] copy = Arrays.copyOf(array, array.length);
      for (int i = 0; i < array.length; i++) {
        copy[i] = deduplicator.deduplicateObject(type, array[i]);
      }
      return new ObjArray<>(copy);
    }

    @Override
    public int hashCode() {
      return Arrays.deepHashCode(array);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof ObjArray that) {
        return Arrays.deepEquals(array, that.array);
      }
      return false;
    }
  }

  /** A wrapper for a String array. Optionally, the individual Strings may be deduplicated too. */
  private record StringArray(String[] array) implements Serializable {
    private static StringArray deepDeduplicateOf(String[] array, Deduplicator deduplicator) {
      String[] copy = new String[array.length];
      for (int i = 0; i < array.length; i++) {
        copy[i] = deduplicator.deduplicateString(array[i]);
      }
      return new StringArray(copy);
    }

    /** Note! Records do a shallow hash, so we need to override */
    @Override
    public int hashCode() {
      return Arrays.hashCode(array);
    }

    /** Note! Records do a shallow compare in equals, so we need to override */
    @Override
    public boolean equals(Object other) {
      if (other instanceof StringArray that) {
        return Arrays.equals(array, that.array);
      }
      return false;
    }
  }

  /** A wrapper for 2D string array */
  private record String2DArray(String[][] array) implements Serializable {
    private static String2DArray deepDeduplicateOf(String[][] array, Deduplicator deduplicator) {
      var copy = new String[array.length][];
      for (int i = 0; i < array.length; i++) {
        copy[i] = deduplicator.deduplicateStringArray(array[i]);
      }
      return new String2DArray(copy);
    }

    @Override
    public int hashCode() {
      return Arrays.deepHashCode(array);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof String2DArray that) {
        return Arrays.deepEquals(array, that.array);
      }
      return false;
    }
  }

  private record NameSize(String name, int size) {}
}
