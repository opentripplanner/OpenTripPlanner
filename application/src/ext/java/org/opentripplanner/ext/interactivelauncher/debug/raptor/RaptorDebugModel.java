package org.opentripplanner.ext.interactivelauncher.debug.raptor;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator;
import org.opentripplanner.routing.api.request.DebugEventType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.utils.lang.StringUtils;

public class RaptorDebugModel implements LauncherRequestDecorator {

  private final Set<DebugEventType> eventTypes = EnumSet.noneOf(DebugEventType.class);
  private String stops = null;
  private String path = null;
  private Runnable saveCallback;

  public RaptorDebugModel() {}

  public void init(Runnable saveCallback) {
    this.saveCallback = saveCallback;
  }

  /** Used by JSON serialization */
  public Set<DebugEventType> getEventTypes() {
    return Collections.unmodifiableSet(eventTypes);
  }

  /** Used by JSON serialization */
  public void setEventTypes(Collection<DebugEventType> eventTypes) {
    this.eventTypes.clear();
    this.eventTypes.addAll(eventTypes);
  }

  public void enableEventTypes(DebugEventType eventType, boolean enable) {
    if (enable) {
      if (!isEventTypeSet(eventType)) {
        this.eventTypes.add(eventType);
        save();
      }
    } else {
      if (isEventTypeSet(eventType)) {
        this.eventTypes.remove(eventType);
        save();
      }
    }
  }

  @Nullable
  public String getStops() {
    return stops;
  }

  public void setStops(@Nullable String stops) {
    stops = StringUtils.hasValue(stops) ? stops : null;
    if (!Objects.equals(this.stops, stops)) {
      this.stops = stops;
      save();
    }
  }

  @Nullable
  public String getPath() {
    return path;
  }

  public void setPath(@Nullable String path) {
    path = StringUtils.hasValue(path) ? path : null;
    if (!Objects.equals(this.path, path)) {
      this.path = path;
      save();
    }
  }

  public boolean isEventTypeSet(DebugEventType eventType) {
    return eventTypes.contains(eventType);
  }

  @Override
  public RouteRequest intercept(RouteRequest defaultRequest) {
    return defaultRequest
      .copyOf()
      .withJourney(jb ->
        jb.withTransit(tb ->
          tb.withRaptorDebugging(rd -> rd.withEventTypes(eventTypes).withStops(stops).withPath(path)
          )
        )
      )
      .buildDefault();
  }

  private void save() {
    if (saveCallback != null) {
      saveCallback.run();
    }
  }
}
