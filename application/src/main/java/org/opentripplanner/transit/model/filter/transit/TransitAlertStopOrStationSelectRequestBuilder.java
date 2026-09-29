package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Builder for {@link TransitAlertStopOrStationSelectRequest}.
 */
public class TransitAlertStopOrStationSelectRequestBuilder {

  @Nullable
  private List<FeedScopedId> ids;

  @Nullable
  private List<TransitMode> modes;

  private boolean includeParentStationAlerts = false;
  private boolean includeChildStopAlerts = false;

  public TransitAlertStopOrStationSelectRequestBuilder withIds(@Nullable List<FeedScopedId> ids) {
    this.ids = ids;
    return this;
  }

  public TransitAlertStopOrStationSelectRequestBuilder withModes(
    @Nullable List<TransitMode> modes
  ) {
    this.modes = modes;
    return this;
  }

  public TransitAlertStopOrStationSelectRequestBuilder withIncludeParentStationAlerts(
    boolean includeParentStationAlerts
  ) {
    this.includeParentStationAlerts = includeParentStationAlerts;
    return this;
  }

  public TransitAlertStopOrStationSelectRequestBuilder withIncludeChildStopAlerts(
    boolean includeChildStopAlerts
  ) {
    this.includeChildStopAlerts = includeChildStopAlerts;
    return this;
  }

  public TransitAlertStopOrStationSelectRequest build() {
    return new TransitAlertStopOrStationSelectRequest(this);
  }

  /* package-private accessors for TransitAlertStopOrStationSelectRequest */
  @Nullable
  List<FeedScopedId> ids() {
    return ids;
  }

  @Nullable
  List<TransitMode> modes() {
    return modes;
  }

  boolean includeParentStationAlerts() {
    return includeParentStationAlerts;
  }

  boolean includeChildStopAlerts() {
    return includeChildStopAlerts;
  }
}
