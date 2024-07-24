package org.opentripplanner.apis.gtfs.model;

import java.time.LocalDate;
import javax.annotation.Nullable;

/**
 * See the API documentation for a discussion of {@code startInclusive} and {@code endExclusive}.
 */
public record LocalDateRange(@Nullable LocalDate startInclusive, @Nullable LocalDate endExclusive) {
  /**
   * Does it actually define a limit or is the range unlimited?
   */
  public boolean unlimited() {
    return startInclusive == null && endExclusive == null;
  }

  /**
   * Is the start date before the end?
   */
  public boolean startBeforeEnd() {
    return startInclusive != null && endExclusive != null && startInclusive.isAfter(endExclusive);
  }

  /**
   * Is the given LocalDate instance inside of this date range?
   */
  public boolean contains(LocalDate date) {
    return (
      (startInclusive == null || date.isEqual(startInclusive) || date.isAfter(startInclusive)) &&
      (endExclusive == null || date.isBefore(endExclusive))
    );
  }
}
