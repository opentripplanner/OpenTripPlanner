package org.opentripplanner.transit.model.basic;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Tupple of main- and sub-mode.
 */
public record MainAndSubMode(TransitMode mainMode, @Nullable SubMode subMode) {
  private static final List<MainAndSubMode> ALL = Stream.of(TransitMode.values())
    .map(MainAndSubMode::new)
    .toList();

  public MainAndSubMode(TransitMode mode) {
    this(mode, null);
  }

  public static List<MainAndSubMode> all() {
    return ALL;
  }

  /**
   * Return the complement of the given list of {@code modes} with {@link #all()} main modes
   * as a starting point. Hence, take all main modes and remove all modes matching one of the
   * given input {@code modes}. If one of the given modes is not a main mode
   * ({@link #isMainModeOnly()}), then it is simply ignored.
   */
  public static List<MainAndSubMode> notMainModes(Collection<MainAndSubMode> modes) {
    return MainAndSubMode.all().stream().filter(Predicate.not(modes::contains)).toList();
  }

  /** Return {@code true} if this only contains a main mode and the sub-mode is {@code null} */
  public boolean isMainModeOnly() {
    return subMode == null;
  }

  @Override
  public String toString() {
    if (subMode == null) {
      return mainMode.name();
    }
    return mainMode.name() + "::" + subMode;
  }

  /**
   * Make sure the String serialization is deterministic by sorting the elements in
   * alphabetic order.
   */
  public static String toString(Collection<MainAndSubMode> modes) {
    return modes != null
      ? modes.stream().map(MainAndSubMode::toString).sorted().toList().toString()
      : null;
  }
}
