package org.opentripplanner.transit.model.framework;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The deduplication service is used to reduce memory consumption by returning the
 * same instance if to value-objects are the same. The value-objects must implement
 * hashCode and equals.
 * <p>
 * This is also used with arrays of primitive types. Arrays are mutable, so be sure they are
 * well protected and do not change if you deduplicate them.
 * <p>
 * Note! The deduplicator should ONLY be used with immutable types and well protected
 *       fields - guaranteed not to be changed.
 */
public interface DeduplicatorService {
  DeduplicatorService NOOP = new DeduplicatorNoop();

  @Nullable
  BitSet deduplicateBitSet(BitSet original);

  @Nullable
  int[] deduplicateIntArray(int[] original);

  @Nullable
  String deduplicateString(String original);

  @Nullable
  String[] deduplicateStringArray(String[] original);

  @Nullable
  String[][] deduplicateString2DArray(String[][] original);

  @Nullable
  <T> T deduplicateObject(Class<T> cl, T original);

  @Nullable
  <T> T[] deduplicateObjectArray(Class<T> type, T[] original);

  @Nullable
  <T> List<T> deduplicateImmutableList(Class<T> clazz, List<T> original);
}
