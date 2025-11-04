package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultTransitDataProviderFilterBuilder {

  private boolean requireBikesAllowed = false;

  private boolean requireCarsAllowed = false;

  private boolean requireWheelchairAccessibleTrips = false;

  private boolean requireWheelchairAccessibleStops = false;

  private boolean includePlannedCancellations = false;

  private boolean includeRealtimeCancellations = false;

  @Nullable
  private List<TransitFilter> filters = null;

  private Collection<FeedScopedId> bannedTrips = List.of();

  public static DefaultTransitDataProviderFilterBuilder ofRequest(RouteRequest request) {
    var wheelchairEnabled = request.journey().wheelchair();
    var wheelchair = request.preferences().wheelchair();

    var builder = new DefaultTransitDataProviderFilterBuilder()
      .withRequireBikesAllowed(request.journey().transfer().mode() == StreetMode.BIKE)
      .withRequireCarsAllowed(request.journey().transfer().mode() == StreetMode.CAR)
      .withRequireWheelchairAccessibleTrips(
        wheelchairEnabled && wheelchair.trip().onlyConsiderAccessible()
      )
      .withRequireWheelchairAccessibleStops(
        wheelchairEnabled && wheelchair.stop().onlyConsiderAccessible()
      )
      .withIncludePlannedCancellations(
        request.preferences().transit().includePlannedCancellations()
      )
      .withIncludeRealtimeCancellations(
        request.preferences().transit().includeRealtimeCancellations()
      )
      .withBannedTrips(request.journey().transit().bannedTrips())
      .withFilters(request.journey().transit().filters());
    return builder;
  }

  public DefaultTransitDataProviderFilterBuilder withRequireBikesAllowed(
    boolean requireBikesAllowed
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withRequireCarsAllowed(
    boolean requireCarsAllowed
  ) {
    this.requireCarsAllowed = requireCarsAllowed;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withRequireWheelchairAccessibleTrips(
    boolean requireWheelchairAccessibleTrips
  ) {
    this.requireWheelchairAccessibleTrips = requireWheelchairAccessibleTrips;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withRequireWheelchairAccessibleStops(
    boolean requireWheelchairAccessibleStops
  ) {
    this.requireWheelchairAccessibleStops = requireWheelchairAccessibleStops;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withIncludePlannedCancellations(
    boolean includePlannedCancellations
  ) {
    this.includePlannedCancellations = includePlannedCancellations;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withIncludeRealtimeCancellations(
    boolean includeRealtimeCancellations
  ) {
    this.includeRealtimeCancellations = includeRealtimeCancellations;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withBannedTrips(
    Collection<FeedScopedId> bannedTrips
  ) {
    this.bannedTrips = bannedTrips;
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder withFilters(List<TransitFilter> filters) {
    this.filters = new ArrayList<>(filters);
    return this;
  }

  public DefaultTransitDataProviderFilterBuilder addFilter(TransitFilter filter) {
    if (this.filters == null) {
      this.filters = new ArrayList<>();
    }
    this.filters.add(filter);
    return this;
  }

  public boolean requireBikesAllowed() {
    return requireBikesAllowed;
  }

  public boolean requireCarsAllowed() {
    return requireCarsAllowed;
  }

  public boolean requireWheelchairAccessibleTrips() {
    return requireWheelchairAccessibleTrips;
  }

  public boolean requireWheelchairAccessibleStops() {
    return requireWheelchairAccessibleStops;
  }

  public boolean includePlannedCancellations() {
    return includePlannedCancellations;
  }

  public boolean includeRealtimeCancellations() {
    return includeRealtimeCancellations;
  }

  public Collection<FeedScopedId> bannedTrips() {
    return bannedTrips;
  }

  public List<TransitFilter> filters() {
    if (filters == null) {
      return List.of(AllowAllTransitFilter.of());
    }
    return filters;
  }

  public DefaultTransitDataProviderFilter build() {
    return new DefaultTransitDataProviderFilter(this);
  }
}
