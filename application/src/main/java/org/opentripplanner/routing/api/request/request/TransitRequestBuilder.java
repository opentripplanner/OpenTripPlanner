package org.opentripplanner.routing.api.request.request;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.api.request.DebugRaptorBuilder;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitRequestBuilder {

  private List<TransitFilter> filters;
  private List<FeedScopedId> unpreferredAgencies;
  private List<FeedScopedId> unpreferredRoutes;
  private List<FeedScopedId> bannedTrips;
  private List<TransitGroupSelect> priorityGroupsByAgency;
  private List<TransitGroupSelect> priorityGroupsGlobal;
  private DebugRaptor raptorDebugging;
  private TransitRequest original;

  public TransitRequestBuilder(TransitRequest original) {
    this.original = original;
    this.filters = null;
    this.unpreferredAgencies = null;
    this.unpreferredRoutes = null;
    this.bannedTrips = null;
    this.priorityGroupsByAgency = null;
    this.priorityGroupsGlobal = null;
    this.raptorDebugging = null;
  }

  public TransitRequestBuilder setFilters(List<TransitFilter> filters) {
    this.filters = filters;
    return this;
  }

  public TransitRequestBuilder withFilter(Consumer<TransitFilterRequest.Builder> body) {
    var builder = TransitFilterRequest.of();
    body.accept(builder);
    return setFilters(List.of(builder.build()));
  }

  /**
   * Disables the transit search for this request, for example when you only want bike routes.
   */
  public TransitRequestBuilder disable() {
    return setFilters(List.of(ExcludeAllTransitFilter.of()));
  }

  public TransitRequestBuilder withUnpreferredAgencies(List<FeedScopedId> unpreferredAgencies) {
    this.unpreferredAgencies = unpreferredAgencies;
    return this;
  }

  public TransitRequestBuilder withUnpreferredRoutes(List<FeedScopedId> unpreferredRoutes) {
    this.unpreferredRoutes = unpreferredRoutes;
    return this;
  }

  public TransitRequestBuilder withBannedTrips(List<FeedScopedId> bannedTrips) {
    this.bannedTrips = bannedTrips;
    return this;
  }

  public TransitRequestBuilder addPriorityGroupsGlobal(List<TransitGroupSelect> value) {
    this.priorityGroupsGlobal = value;
    return this;
  }

  public TransitRequestBuilder withPriorityGroupsByAgency(List<TransitGroupSelect> value) {
    this.priorityGroupsByAgency = value;
    return this;
  }

  public TransitRequestBuilder withRaptorDebugging(Consumer<DebugRaptorBuilder> body) {
    this.raptorDebugging = DebugRaptor.of().accept(body).build();
    return this;
  }

  public TransitRequestBuilder apply(Consumer<TransitRequestBuilder> body) {
    body.accept(this);
    return this;
  }

  public TransitRequest build() {
    var newValue = new TransitRequest(
      ifNotNull(filters, original.filters()),
      ifNotNull(unpreferredAgencies, original.unpreferredAgencies()),
      ifNotNull(unpreferredRoutes, original.unpreferredRoutes()),
      ifNotNull(bannedTrips, original.bannedTrips()),
      ifNotNull(priorityGroupsByAgency, original.priorityGroupsByAgency()),
      ifNotNull(priorityGroupsGlobal, original.priorityGroupsGlobal()),
      raptorDebugging == null ? original.raptorDebugging() : raptorDebugging
    );
    return original.equals(newValue) ? original : newValue;
  }

  private static <T> List<T> ifNotNull(List<T> newValue, List<T> original) {
    return newValue == null ? original : List.copyOf(newValue);
  }
}
