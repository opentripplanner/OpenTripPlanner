package org.opentripplanner.transit.api.request;

import java.util.List;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;

/**
 * A request for {@link TransitAlert}s.
 * <p>
 * The request holds a list of filters which are combined with OR semantics: an alert matches if it
 * matches at least one of the filters. An empty list of filters matches all alerts.
 */
public class TransitAlertRequest {

  private final List<FilterRequest<TransitAlertSelectRequest>> filters;

  TransitAlertRequest(List<FilterRequest<TransitAlertSelectRequest>> filters) {
    this.filters = filters;
  }

  public static TransitAlertRequestBuilder of() {
    return new TransitAlertRequestBuilder();
  }

  public List<FilterRequest<TransitAlertSelectRequest>> filters() {
    return filters;
  }
}
