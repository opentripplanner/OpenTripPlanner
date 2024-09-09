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
   * Take a list of children and return a composite instance. Input children witch is {@code null}
   * is skipped. If no none {@code null} children are provided, {@code null} is retuned
   * thrown. If just one listener is passed in the listener it-self is returned (without any wrapper).
   *
   * @param <T> The base type which the composite inherit from.
   * @param makeComposite Factory method to create a new composite.
   * @param isComposite see the {@code listChildren} parameter.
   * @param listChildren is a function used together with the {@code isComposite} test to extract
   *                     all children out of a composite. This is used to produce one flat list of
   *                     concrete children, without any composite instances in it. The order is
   *                     kept; the composite children are inserted in the new list of children in
   *                     the same place as the composite instance appeared.
   * @return {@code null} if all children are {@code null}, the child it-self if only one child
   *                      exist, and a new composite instance if more than one child exist.
   */
  @Nullable
  @SafeVarargs
  public static <T> T of(
    Function<List<T>, T> makeComposite,
    Predicate<T> isComposite,
    Function<T, Collection<T>> listChildren,
    T... children
  ) {
    Objects.requireNonNull(children);

    var list = Arrays
      .stream(children)
      .filter(Objects::nonNull)
      .flatMap(it -> isComposite.test(it) ? listChildren.apply(it).stream() : Stream.of(it))
      .toList();

    if (list.isEmpty()) {
      return null;
    }
    if (list.size() == 1) {
      return list.getFirst();
    }
    return makeComposite.apply(list);
  }
}
