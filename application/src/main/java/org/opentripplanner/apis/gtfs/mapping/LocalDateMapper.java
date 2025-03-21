package org.opentripplanner.apis.gtfs.mapping;

import java.time.LocalDate;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class LocalDateMapper {

  /**
   * Map a LocalDate to the ISO 8601 standard YYYY-MM-DD. If the given input date is {@code null}
   * or <em>unbounded</em> then {@code null} is returned.
   */
  public static String mapToApi(LocalDate date) {
    return (date == null || ServiceDateUtils.isMinMax(date)) ? null : date.toString();
  }
}
