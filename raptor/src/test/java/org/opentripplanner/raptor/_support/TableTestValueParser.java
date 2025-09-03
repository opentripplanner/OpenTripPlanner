package org.opentripplanner.raptor._support;

import java.util.Map;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.utils.time.TimeUtils;


/**
 * This is used by the {@link io.github.nchaugen.tabletest.junit.TableTest} to parse
 * string and convert them to frequently used types and values.
 * <p/>
 * The following mappings exist:
 * <ul>
 *   <li>
 *     Constant integers like {@code UNDEFINED}, {@code NOT_FOUND} and {@code NOT_SET} is mapped
 *     to an int value.
 *   </li>
 *   <li>
 *     Time format {@code hh:mm:ss} is mapped to seconds-after-midnight.
 *   </li>
 * </ul>
 *
 */

@SuppressWarnings("unused")
public class TableTestValueParser {
    public static final int UNDEFINED = -987_987_987;

    private static final Map<String, Integer> INT_CONSTANTS = Map.ofEntries(
      Map.entry("UNDEFINED", UNDEFINED),
      Map.entry("NOT_FOUND", RaptorConstants.NOT_FOUND),
      Map.entry("NOT_SET", RaptorConstants.NOT_SET),
      Map.entry("TIME_NOT_SET", RaptorConstants.TIME_NOT_SET),
      Map.entry("TIME_UNREACHED_FORWARD", RaptorConstants.TIME_UNREACHED_FORWARD),
      Map.entry("TIME_UNREACHED_REVERSE", RaptorConstants.TIME_UNREACHED_REVERSE)
  );

  public static Integer parseInteger(String value) {
    if (INT_CONSTANTS.containsKey(value)) {
      return INT_CONSTANTS.get(value);
    }
    if (value.contains(":")) {
      return TimeUtils.time(value);
    }
    return Integer.parseInt(value);
  }
}
