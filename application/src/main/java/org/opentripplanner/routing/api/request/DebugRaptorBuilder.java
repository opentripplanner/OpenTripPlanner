package org.opentripplanner.routing.api.request;

import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugRaptorBuilder implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(DebugRaptorBuilder.class);

  private static final Pattern FIRST_STOP_PATTERN = Pattern.compile("(\\d+)\\*");
  private static final int FIRST_STOP_INDEX = 0;
  private static final int NOT_SET = -999_999_999;

  private List<Integer> stops;
  private List<Integer> path;
  private int debugPathFromStopIndex;
  private Set<DebugEventType> eventTypes;
  private final DebugRaptor original;

  public DebugRaptorBuilder(DebugRaptor original) {
    this.original = original;

    this.stops = null;
    this.path = null;
    this.debugPathFromStopIndex = NOT_SET;
    this.eventTypes = null;
  }

  /**
   * Add a list of stops to debug as a string. Both Raptor stop indexes and stop ids are acepted.
   * Use a space and/or comma to separate the stops. Example:
   * {@code "12322, 567, 1234"}
   * <p>
   * See {@link DebugRaptor} for more info.
   */
  public DebugRaptorBuilder withStops(String stops) {
    if (stops == null) {
      return this;
    }
    this.stops = split(stops);
    return this;
  }

  /**
   * Add a list of stops that define the path you want to debug. Only paths visiting all stops
   * are debugged. You may add an asterisk {@code '*'} after one of the stops. Stop events for the
   * stops before the tagged stop are ignored. Both Raptor stop indexes and stop ids are accepted.
   * Use a space and/or comma to separate the stops. Example: {@code "12322, 567*, 1234"}
   * <p>
   * See {@link DebugRaptor} for more info.
   */
  public DebugRaptorBuilder withPath(String path) {
    if (path == null) {
      return this;
    }

    this.path = split(path);
    this.debugPathFromStopIndex = firstStopIndexToDebug(this.path, path);
    return this;
  }

  public DebugRaptorBuilder withEventTypes(Collection<DebugEventType> eventTypes) {
    this.eventTypes = Set.copyOf(eventTypes);
    return this;
  }

  public DebugRaptorBuilder accept(Consumer<DebugRaptorBuilder> body) {
    body.accept(this);
    return this;
  }

  public DebugRaptor build() {
    var value = new DebugRaptor(
      ifNotNull(stops, original.stops()),
      ifNotNull(path, original.path()),
      debugPathFromStopIndex == NOT_SET
        ? original.debugPathFromStopIndex()
        : debugPathFromStopIndex,
      ifNotNull(eventTypes, original.eventTypes())
    );
    return original.equals(value) ? original : value;
  }

  private static List<Integer> split(String stops) {
    try {
      if (stops == null) {
        return List.of();
      }

      return Arrays.stream(stops.split("[\\s,;_*]+")).map(Integer::parseInt).toList();
    } catch (NumberFormatException e) {
      LOG.error(e.getMessage(), e);
      // Keep going, we do not want to abort a
      // request because the debug info is wrong.
      return List.of();
    }
  }

  private static int firstStopIndexToDebug(List<Integer> stops, String text) {
    if (text == null) {
      return FIRST_STOP_INDEX;
    }

    var m = FIRST_STOP_PATTERN.matcher(text);
    Integer stop = m.find() ? Integer.parseInt(m.group(1)) : null;
    return stop == null ? FIRST_STOP_INDEX : stops.indexOf(stop);
  }

  private static String toString(List<Integer> stops, int fromStopIndex) {
    if (stops == null || stops.isEmpty()) {
      return null;
    }

    var buf = new StringBuilder();
    for (int i = 0; i < stops.size(); ++i) {
      buf.append(stops.get(i));
      if (i > FIRST_STOP_INDEX && i == fromStopIndex) {
        buf.append("*");
      }
      buf.append(", ");
    }
    return buf.substring(0, buf.length() - 2);
  }
}
