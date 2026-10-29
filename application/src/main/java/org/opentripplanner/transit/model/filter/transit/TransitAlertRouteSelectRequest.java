package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Selects routes when filtering alerts by the entities they affect. The criteria are combined with
 * AND logic, unset (null) criteria match every route.
 */
public class TransitAlertRouteSelectRequest {

  private final FilterValues<FeedScopedId> ids;
  private final FilterValues<TransitMode> modes;

  TransitAlertRouteSelectRequest(TransitAlertRouteSelectRequestBuilder builder) {
    this.ids = FilterValues.ofNullIsEverything("ids", toSet(builder.ids()));
    this.modes = FilterValues.ofNullIsEverything("modes", toSet(builder.modes()));
  }

  @Nullable
  private static <T> Set<T> toSet(@Nullable List<T> values) {
    return values == null ? null : Set.copyOf(values);
  }

  public static TransitAlertRouteSelectRequestBuilder of() {
    return new TransitAlertRouteSelectRequestBuilder();
  }

  public FilterValues<FeedScopedId> ids() {
    return ids;
  }

  public FilterValues<TransitMode> modes() {
    return modes;
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
    return builder.toString();
  }
}
