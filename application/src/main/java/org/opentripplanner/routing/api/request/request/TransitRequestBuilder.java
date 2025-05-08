package org.opentripplanner.routing.api.request.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitRequestBuilder {

  private List<TransitFilter> filters = null;
  private List<FeedScopedId> preferredAgencies = null;
  private List<FeedScopedId> unpreferredAgencies = null;
  private List<FeedScopedId> preferredRoutes = null;
  private List<FeedScopedId> unpreferredRoutes = null;
  private List<FeedScopedId> bannedTrips = null;
  private List<TransitGroupSelect> priorityGroupsByAgency = null;
  private List<TransitGroupSelect> priorityGroupsGlobal = null;
  private DebugRaptor raptorDebugging = null;
  private TransitRequest original;

  public TransitRequestBuilder(TransitRequest original) {
    this.original = original;
  }

  public TransitRequestBuilder setFilters(List<TransitFilter> filters) {
    this.filters = filters;
    return this;
  }

  /**
   * Disables the transit search for this request, for example when you only want bike routes.
   */
  public TransitRequestBuilder disable() {
    this.filters = List.of(ExcludeAllTransitFilter.of());
    return this;
  }

  @Deprecated
  public TransitRequestBuilder setPreferredAgencies(List<FeedScopedId> preferredAgencies) {
    this.preferredAgencies = preferredAgencies;
    return this;
  }

  @Deprecated
  public TransitRequestBuilder setPreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      preferredAgencies = FeedScopedId.parseList(s);
    }
    return this;
  }

  public TransitRequestBuilder setUnpreferredAgencies(List<FeedScopedId> unpreferredAgencies) {
    this.unpreferredAgencies = unpreferredAgencies;
    return this;
  }

  public TransitRequestBuilder setUnpreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredAgencies = FeedScopedId.parseList(s);
    }
    return this;
  }

  @Deprecated
  public TransitRequestBuilder setPreferredRoutes(List<FeedScopedId> preferredRoutes) {
    this.preferredRoutes = preferredRoutes;
    return this;
  }

  @Deprecated
  public TransitRequestBuilder setPreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      preferredRoutes = List.copyOf(FeedScopedId.parseList(s));
    } else {
      preferredRoutes = List.of();
    }
    return this;
  }

  public TransitRequestBuilder setUnpreferredRoutes(List<FeedScopedId> unpreferredRoutes) {
    this.unpreferredRoutes = unpreferredRoutes;
    return this;
  }

  public TransitRequestBuilder setUnpreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredRoutes = List.copyOf(FeedScopedId.parseList(s));
    } else {
      unpreferredRoutes = List.of();
    }
    return this;
  }

  public TransitRequestBuilder setBannedTrips(List<FeedScopedId> bannedTrips) {
    this.bannedTrips = bannedTrips;
    return this;
  }

  public TransitRequestBuilder setBannedTripsFromString(String ids) {
    if (!ids.isEmpty()) {
      this.bannedTrips = FeedScopedId.parseList(ids);
    }
    return this;
  }

  public TransitRequestBuilder addPriorityGroupsGlobal(Collection<TransitGroupSelect> newValues) {
    this.priorityGroupsGlobal = addToList(this.priorityGroupsGlobal, newValues);
    return this;
  }

  public TransitRequestBuilder addPriorityGroupsByAgency(Collection<TransitGroupSelect> newValues) {
    this.priorityGroupsByAgency = addToList(this.priorityGroupsByAgency, newValues);
    return this;
  }

  public TransitRequestBuilder setRaptorDebugging(DebugRaptor raptorDebugging) {
    this.raptorDebugging = raptorDebugging;
    return this;
  }

  public TransitRequestBuilder apply(Consumer<TransitRequestBuilder> body) {
    body.accept(this);
    return this;
  }

  public TransitRequest build() {
    var newValue = new TransitRequest(
      ifNotNull(filters, original.filters()),
      ifNotNull(preferredAgencies, original.preferredAgencies()),
      ifNotNull(unpreferredAgencies, original.unpreferredAgencies()),
      ifNotNull(preferredRoutes, original.preferredRoutes()),
      ifNotNull(unpreferredRoutes, original.unpreferredRoutes()),
      ifNotNull(bannedTrips, original.bannedTrips()),
      ifNotNull(priorityGroupsByAgency, original.priorityGroupsByAgency()),
      ifNotNull(priorityGroupsGlobal, original.priorityGroupsGlobal()),
      ifNotNull(raptorDebugging, original.raptorDebugging())
    );
    // return original.equals(newValue) ? original : newValue;
    return newValue;
  }

  private static <T> List<T> addToList(List<T> list, Collection<T> newValues) {
    if (list == null) {
      list = new ArrayList<>(newValues);
    } else {
      list.addAll(newValues);
    }
    return list;
  }

  private static <T> T ifNotNull(T newValue, T original) {
    return newValue != null ? newValue : original;
  }
}
