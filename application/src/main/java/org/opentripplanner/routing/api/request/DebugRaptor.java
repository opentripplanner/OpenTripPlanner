package org.opentripplanner.routing.api.request;

import static org.opentripplanner.routing.api.request.DebugEventType.DESTINATION_ARRIVALS;
import static org.opentripplanner.routing.api.request.DebugEventType.STOP_ARRIVALS;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.utils.collection.EnumSetUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Use this class to configure Raptor Event Debugging. There are two ways to debug:
 * <ol>
 *     <li>
 *         Add a list of stops, and Raptor will print all events for these stops. This can be
 *         a bit overwhelming so using the path option might be a better option.
 *     </li>
 *     <li>
 *         Add a path (also a list of stops), and Raptor will print all events on the path.
 *         So if you arrive at a stop listed, but have not followed the exact same path of
 *         stops, then the event is NOT listed. Note! there are events for dropping an accepted
 *         path. If an none matching path dominate a matching path, then both paths are logged
 *         as part of the event.
 * <p>
 *         You may also specify the first stop in the path to start logging events for. For example
 *         given the path {@code [1010, 1183, 3211, 492]}, then you may know, that you can get to
 *         stop 2 without any problems. So, to avoid getting spammed by logging events at the first
 *         two stops, you set the stop index 3211 as the first stop to print events for. This is
 *         done by adding an {@code *} to the stop: {@code [1010,1183,3211*,492]}.
 *     </li>
 *     To list stops you need to know the Raptor stop index. Enable log {@code level="debug"} for
 *     the {@code org.opentripplanner.raptor} package to list all paths found by Raptor.
 *     The paths will contain the stop index. For paths not listed you will have to do some
 *     research.
 * </ol>
 */
public class DebugRaptor implements Serializable {

  private static final int FIRST_STOP_INDEX = 0;
  private static final int DEFAULT_DEBUG_PATH_FROM_STOP_INDEX = 0;
  private static final Set<DebugEventType> DEFAULT_EVENT_TYPES = Set.of(
    STOP_ARRIVALS,
    DESTINATION_ARRIVALS
  );
  private static final DebugRaptor DEFAULT = new DebugRaptor();

  private final List<Integer> stops;
  private final List<Integer> path;
  private final int debugPathFromStopIndex;
  private final Set<DebugEventType> eventTypes;

  DebugRaptor(
    List<Integer> stops,
    List<Integer> path,
    int debugPathFromStopIndex,
    Set<DebugEventType> eventTypes
  ) {
    this.stops = List.copyOf(stops);
    this.path = List.copyOf(path);
    this.debugPathFromStopIndex = debugPathFromStopIndex;
    this.eventTypes = EnumSetUtils.unmodifiableEnumSet(eventTypes, DebugEventType.class);
  }

  public DebugRaptor() {
    this(List.of(), List.of(), DEFAULT_DEBUG_PATH_FROM_STOP_INDEX, DEFAULT_EVENT_TYPES);
  }

  public static DebugRaptor defaltValue() {
    return DEFAULT;
  }

  public static DebugRaptorBuilder of() {
    return DEFAULT.copyOf();
  }

  public DebugRaptorBuilder copyOf() {
    return new DebugRaptorBuilder(this);
  }

  public boolean isEnabled() {
    return !stops.isEmpty() || !path.isEmpty();
  }

  public List<Integer> stops() {
    return stops;
  }

  public List<Integer> path() {
    return path;
  }

  public int debugPathFromStopIndex() {
    return debugPathFromStopIndex;
  }

  public Set<DebugEventType> eventTypes() {
    return eventTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DebugRaptor that = (DebugRaptor) o;
    return (
      debugPathFromStopIndex == that.debugPathFromStopIndex &&
      Objects.equals(stops, that.stops) &&
      Objects.equals(path, that.path) &&
      Objects.equals(eventTypes, that.eventTypes)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(stops, path, debugPathFromStopIndex, eventTypes);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DebugRaptor.class)
      .addObj("stops", stopsToString(stops, FIRST_STOP_INDEX))
      .addObj("path", stopsToString(path, debugPathFromStopIndex))
      .addCol("eventType", eventTypes, DEFAULT_EVENT_TYPES)
      .toString();
  }

  private static String stopsToString(List<Integer> stops, int fromStopIndex) {
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
