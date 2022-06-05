package org.opentripplanner.transit.model.network;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Tupple of main- and sub-mode.
 */
public record MainAndSubMode(@Nonnull TransitMode mainMode, @Nullable SubMode subMode) {
  private static final List<MainAndSubMode> ALL = Stream
    .of(TransitMode.values())
    .map(MainAndSubMode::new)
    .toList();

  public MainAndSubMode(TransitMode mode) {
    this(mode, null);
  }

  public static List<MainAndSubMode> all() {
    return ALL;
  }
}
