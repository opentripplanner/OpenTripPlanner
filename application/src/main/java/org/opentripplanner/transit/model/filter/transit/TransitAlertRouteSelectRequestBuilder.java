package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Builder for {@link TransitAlertRouteSelectRequest}.
 */
public class TransitAlertRouteSelectRequestBuilder {

  @Nullable
  private List<FeedScopedId> ids;

  @Nullable
  private List<TransitMode> modes;

  public TransitAlertRouteSelectRequestBuilder withIds(@Nullable List<FeedScopedId> ids) {
    this.ids = ids;
    return this;
  }

  public TransitAlertRouteSelectRequestBuilder withModes(@Nullable List<TransitMode> modes) {
    this.modes = modes;
    return this;
  }

  public TransitAlertRouteSelectRequest build() {
    return new TransitAlertRouteSelectRequest(this);
  }

  @Nullable
  List<FeedScopedId> ids() {
    return ids;
  }

  @Nullable
  List<TransitMode> modes() {
    return modes;
  }
}
