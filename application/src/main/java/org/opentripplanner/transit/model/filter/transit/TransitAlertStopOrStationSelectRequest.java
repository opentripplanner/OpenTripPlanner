package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Selects stops and stations when filtering alerts by the entities they affect. The id and mode
 * criteria are combined with AND logic, unset (null) criteria match every stop and station.
 * <p>
 * The selection can be expanded with the parent stations of the selected stops and with the child
 * stops of the selected stations. The flags are ignored when they don't apply to the type of the
 * selected location.
 */
public class TransitAlertStopOrStationSelectRequest {

  private final FilterValues<FeedScopedId> ids;
  private final FilterValues<TransitMode> modes;
  private final boolean includeParentStationAlerts;
  private final boolean includeChildStopAlerts;

  TransitAlertStopOrStationSelectRequest(TransitAlertStopOrStationSelectRequestBuilder builder) {
    this.ids = FilterValues.ofNullIsEverything("ids", toSet(builder.ids()));
    this.modes = FilterValues.ofNullIsEverything("modes", toSet(builder.modes()));
    this.includeParentStationAlerts = builder.includeParentStationAlerts();
    this.includeChildStopAlerts = builder.includeChildStopAlerts();
  }

  @Nullable
  private static <T> Set<T> toSet(@Nullable List<T> values) {
    return values == null ? null : Set.copyOf(values);
  }

  public static TransitAlertStopOrStationSelectRequestBuilder of() {
    return new TransitAlertStopOrStationSelectRequestBuilder();
  }

  public FilterValues<FeedScopedId> ids() {
    return ids;
  }

  public FilterValues<TransitMode> modes() {
    return modes;
  }

  /**
   * If true, the parent stations of the selected stops are also selected.
   */
  public boolean includeParentStationAlerts() {
    return includeParentStationAlerts;
  }

  /**
   * If true, the child stops of the selected stations are also selected.
   */
  public boolean includeChildStopAlerts() {
    return includeChildStopAlerts;
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.ofEmbeddedType();
    if (!ids.includeEverything()) {
      builder.addCol("ids", ids.get());
    }
    if (!modes.includeEverything()) {
      builder.addCol("modes", modes.get());
    }
    builder.addBoolIfTrue("includeParentStationAlerts", includeParentStationAlerts);
    builder.addBoolIfTrue("includeChildStopAlerts", includeChildStopAlerts);
    return builder.toString();
  }
}
