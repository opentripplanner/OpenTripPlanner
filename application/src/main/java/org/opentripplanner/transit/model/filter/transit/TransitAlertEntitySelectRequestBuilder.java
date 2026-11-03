package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Builder for {@link TransitAlertEntitySelectRequest}.
 */
public class TransitAlertEntitySelectRequestBuilder {

  @Nullable
  private List<AlertEntityType> entityTypes;

  @Nullable
  private List<TransitAlertRouteSelectRequest> routes;

  @Nullable
  private List<TransitAlertStopOrStationSelectRequest> stopsOrStations;

  public TransitAlertEntitySelectRequestBuilder withEntityTypes(
    @Nullable List<AlertEntityType> entityTypes
  ) {
    this.entityTypes = entityTypes;
    return this;
  }

  public TransitAlertEntitySelectRequestBuilder withRoutes(
    @Nullable List<TransitAlertRouteSelectRequest> routes
  ) {
    this.routes = routes;
    return this;
  }

  public TransitAlertEntitySelectRequestBuilder withStopsOrStations(
    @Nullable List<TransitAlertStopOrStationSelectRequest> stopsOrStations
  ) {
    this.stopsOrStations = stopsOrStations;
    return this;
  }

  public TransitAlertEntitySelectRequest build() {
    return new TransitAlertEntitySelectRequest(this);
  }

  @Nullable
  List<AlertEntityType> entityTypes() {
    return entityTypes;
  }

  @Nullable
  List<TransitAlertRouteSelectRequest> routes() {
    return routes;
  }

  @Nullable
  List<TransitAlertStopOrStationSelectRequest> stopsOrStations() {
    return stopsOrStations;
  }
}
