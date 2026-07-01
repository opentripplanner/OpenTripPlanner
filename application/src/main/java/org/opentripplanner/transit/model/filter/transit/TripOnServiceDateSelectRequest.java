package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a single selection criterion for filtering
 * {@link org.opentripplanner.transit.model.timetable.TripOnServiceDate} objects.
 * Criteria within a single request are combined with AND logic: all specified criteria must
 * match for the request to match. Unset (null) criteria are ignored (match everything).
 */
public class TripOnServiceDateSelectRequest {

  private final FilterValues<FeedScopedId> agencies;
  private final FilterValues<FeedScopedId> routes;
  private final FilterValues<MainAndSubMode> transportModes;
  private final FilterValues<LocalDateRange> serviceDateRanges;

  private TripOnServiceDateSelectRequest(Builder builder) {
    this.agencies = FilterValues.ofNullIsEverything("agencies", builder.agencies);
    this.routes = FilterValues.ofNullIsEverything("routes", builder.routes);
    this.transportModes = FilterValues.ofNullIsEverything("transportModes", builder.transportModes);
    this.serviceDateRanges = FilterValues.ofNullIsEverything(
      "serviceDateRanges",
      builder.serviceDateRanges
    );
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

  public FilterValues<LocalDateRange> serviceDateRanges() {
    return serviceDateRanges;
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
    if (!serviceDateRanges.includeEverything()) {
      builder.addCol("serviceDateRanges", serviceDateRanges.get());
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

    @Nullable
    private List<LocalDateRange> serviceDateRanges;

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

    public Builder withServiceDateRanges(List<LocalDateRange> serviceDateRanges) {
      this.serviceDateRanges = serviceDateRanges;
      return this;
    }

    public TripOnServiceDateSelectRequest build() {
      return new TripOnServiceDateSelectRequest(this);
    }
  }
}
