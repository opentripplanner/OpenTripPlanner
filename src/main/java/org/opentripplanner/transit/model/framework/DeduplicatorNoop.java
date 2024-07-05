package org.opentripplanner.transit.model.framework;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;

class DeduplicatorNoop implements DeduplicatorService {

  @Nullable
  @Override
  public BitSet deduplicateBitSet(BitSet original) {
    return original;
  }

  @Nullable
  @Override
  public int[] deduplicateIntArray(int[] original) {
    return original;
  }

  @Nullable
  @Override
  public String deduplicateString(String original) {
    return original;
  }

  @Nullable
  @Override
  public String[] deduplicateStringArray(String[] original) {
    return original;
  }

  @Nullable
  @Override
  public String[][] deduplicateString2DArray(String[][] original) {
    return original;
  }

  @Nullable
  @Override
  public <T> T deduplicateObject(Class<T> cl, T original) {
    return original;
  }

  @Nullable
  @Override
  public <T> T[] deduplicateObjectArray(Class<T> type, T[] original) {
    return original;
  }

  @Nullable
  @Override
  public <T> List<T> deduplicateImmutableList(Class<T> clazz, List<T> original) {
    return original;
  }
}
