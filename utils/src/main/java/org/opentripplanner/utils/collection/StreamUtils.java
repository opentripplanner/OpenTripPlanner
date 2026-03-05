package org.opentripplanner.utils.collection;

import java.util.Collection;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class StreamUtils {

  /**
   * Returns a stream from the given collection, or an empty stream if the collection is null.
   */
  public static <T> Stream<T> ofNullableCollection(@Nullable Collection<T> value) {
    if (value == null) {
      return Stream.empty();
    } else {
      return value.stream();
    }
  }
}
