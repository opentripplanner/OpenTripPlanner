package org.opentripplanner.raptor.util.composite;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class CompositeUtil {

  /**
   * Take a list of children and return a composite instance. {@code null} values are skipped. If
   * the result is empty {@code null} is returned. If just one listener is passed in the listener
   * it-self is returned (without any composite wrapper).
   *
   * @param <T> The base type which the composite inherit from.
   * @param compositeFactory Factory method to create a new composite.
   * @param isComposite used to test if an instance is of a composite type.
   * @param listCompositeChildren is a function used to extract all children out of a composite
   *                              instance.
   * @return {@code null} if the list of children is empty - ignoring {@code null} elements.
   *         Returning THE element if just one element exists. And returning a composite with a
   *         list of children if more than one element exists. The order is kept "as is". Any
   *         composite children flattened, the children are inserted in it place.
   */
  @Nullable
  @SafeVarargs
  public static <T> T of(
    Function<List<T>, T> compositeFactory,
    Predicate<T> isComposite,
    Function<T, Collection<T>> listCompositeChildren,
    T... children
  ) {
    Objects.requireNonNull(children);

    var list = Arrays.stream(children)
      .filter(Objects::nonNull)
      .flatMap(it ->
        isComposite.test(it) ? listCompositeChildren.apply(it).stream() : Stream.of(it)
      )
      .toList();

    if (list.isEmpty()) {
      return null;
    }
    if (list.size() == 1) {
      return list.getFirst();
    }
    return compositeFactory.apply(list);
  }
}
