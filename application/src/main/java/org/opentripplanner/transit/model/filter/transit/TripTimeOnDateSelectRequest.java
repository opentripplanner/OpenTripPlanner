package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a single selection criterion for filtering
 * {@link org.opentripplanner.model.TripTimeOnDate} objects.
 * Criteria within a single request are combined with AND logic: all specified criteria must
 * match for the request to match. Unset (null) criteria are ignored (match everything).
 */
public class TripTimeOnDateSelectRequest {

  private final FilterValues<FeedScopedId> agencies;
  private final FilterValues<FeedScopedId> routes;
  private final FilterValues<MainAndSubMode> transportModes;

  private TripTimeOnDateSelectRequest(Builder builder) {
    this.agencies = FilterValues.ofNullIsEverything("agencies", builder.agencies);
    this.routes = FilterValues.ofNullIsEverything("routes", builder.routes);
    this.transportModes = FilterValues.ofNullIsEverything("transportModes", builder.transportModes);
  }

  public static Builder of() {
    return new Builder();
  }

  public FilterValues<FeedScopedId> agencies() {
    return agencies;
  }

  public FilterValues<FeedScopedId> routes() {
    return routes;
  }

  public FilterValues<MainAndSubMode> transportModes() {
    return transportModes;
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.ofEmbeddedType();
    if (!agencies.includeEverything()) {
      builder.addCol("agencies", agencies.get());
    }
    if (!routes.includeEverything()) {
      builder.addCol("routes", routes.get());
    }
    if (!transportModes.includeEverything()) {
      builder.addCol("transportModes", transportModes.get());
    }
    return builder.toString();
  }

  public static class Builder {

    @Nullable
    private List<FeedScopedId> agencies;

    @Nullable
    private List<FeedScopedId> routes;

    @Nullable
    private List<MainAndSubMode> transportModes;

    public Builder withAgencies(List<FeedScopedId> agencies) {
      this.agencies = agencies;
      return this;
    }

    public Builder withRoutes(List<FeedScopedId> routes) {
      this.routes = routes;
      return this;
    }

    public Builder withTransportModes(List<MainAndSubMode> transportModes) {
      this.transportModes = transportModes;
      return this;
    }

    public TripTimeOnDateSelectRequest build() {
      return new TripTimeOnDateSelectRequest(this);
    }
  }
}
