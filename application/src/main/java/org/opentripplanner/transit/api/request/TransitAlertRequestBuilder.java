package org.opentripplanner.transit.api.request;

import java.util.List;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;

public class TransitAlertRequestBuilder {

  private List<FilterRequest<TransitAlertSelectRequest>> filters = List.of();

  public TransitAlertRequestBuilder withFilters(
    List<FilterRequest<TransitAlertSelectRequest>> filters
  ) {
    this.filters = List.copyOf(filters);
    return this;
  }

  public TransitAlertRequest build() {
    return new TransitAlertRequest(filters);
  }
}
