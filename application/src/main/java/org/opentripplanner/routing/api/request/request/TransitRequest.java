package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a transit request with various configuration options such as
 * banned trips, preferred/unpreferred agencies, and routes, as well as
 * filters and debugging capabilities. This class is designed to allow for
 * customization of transit-related searches.
 */
public class TransitRequest implements Serializable {

  public static final TransitRequest DEFAULT = new TransitRequest(
    List.of(AllowAllTransitFilter.of()),
    List.of(),
    List.of(),
    List.of(),
    List.of(),
    List.of(),
    DebugRaptor.defaltValue()
  );

  private final List<TransitFilter> filters;

  private final List<FeedScopedId> unpreferredAgencies;

  private final List<FeedScopedId> unpreferredRoutes;
  private final List<FeedScopedId> bannedTrips;
  private final List<TransitGroupSelect> priorityGroupsByAgency;
  private final List<TransitGroupSelect> priorityGroupsGlobal;
  private final DebugRaptor raptorDebugging;

  TransitRequest(
    List<TransitFilter> filters,
    List<FeedScopedId> unpreferredAgencies,
    List<FeedScopedId> unpreferredRoutes,
    List<FeedScopedId> bannedTrips,
    List<TransitGroupSelect> priorityGroupsByAgency,
    List<TransitGroupSelect> priorityGroupsGlobal,
    DebugRaptor raptorDebugging
  ) {
    this.filters = filters;
    this.unpreferredAgencies = unpreferredAgencies;
    this.unpreferredRoutes = unpreferredRoutes;
    this.bannedTrips = bannedTrips;
    this.priorityGroupsByAgency = priorityGroupsByAgency;
    this.priorityGroupsGlobal = priorityGroupsGlobal;
    this.raptorDebugging = raptorDebugging;
  }

  public static TransitRequestBuilder of() {
    return DEFAULT.copyOf();
  }

  public TransitRequestBuilder copyOf() {
    return new TransitRequestBuilder(this);
  }

  @Deprecated
  public List<FeedScopedId> bannedTrips() {
    return bannedTrips;
  }

  public List<TransitFilter> filters() {
    return filters;
  }

  /**
   * A unique group-id is assigned to all patterns grouped by matching select and agency.
   * In other words, two patterns matching the same select and with the same agency-id
   * will get the same group-id.
   * <p>
   * Note! Entities that are not matched are put in the BASE-GROUP with id 0.
   */
  public List<TransitGroupSelect> priorityGroupsByAgency() {
    return priorityGroupsByAgency;
  }

  /**
   * A unique group-id is assigned all patterns grouped by matching selects.
   * <p>
   * Note! Entities that are not matched are put in the BASE-GROUP with id 0.
   */
  public List<TransitGroupSelect> priorityGroupsGlobal() {
    return priorityGroupsGlobal;
  }

  public List<FeedScopedId> unpreferredAgencies() {
    return unpreferredAgencies;
  }

  /**
   * Set of unpreferred routes for given user and configuration.
   */
  public List<FeedScopedId> unpreferredRoutes() {
    return unpreferredRoutes;
  }

  public DebugRaptor raptorDebugging() {
    return raptorDebugging;
  }

  /**
   * Returns whether it is requested to run the transit search for this request.
   */
  public boolean enabled() {
    return filters.stream().noneMatch(ExcludeAllTransitFilter.class::isInstance);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransitRequest that = (TransitRequest) o;
    return (
      Objects.equals(filters, that.filters) &&
      Objects.equals(unpreferredAgencies, that.unpreferredAgencies) &&
      Objects.equals(unpreferredRoutes, that.unpreferredRoutes) &&
      Objects.equals(bannedTrips, that.bannedTrips) &&
      Objects.equals(priorityGroupsByAgency, that.priorityGroupsByAgency) &&
      Objects.equals(priorityGroupsGlobal, that.priorityGroupsGlobal) &&
      Objects.equals(raptorDebugging, that.raptorDebugging)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      filters,
      unpreferredAgencies,
      unpreferredRoutes,
      bannedTrips,
      priorityGroupsByAgency,
      priorityGroupsGlobal,
      raptorDebugging
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.ofEmbeddedType()
      .addCol("filters", filters, DEFAULT.filters)
      .addCol("unpreferredAgencies", unpreferredAgencies)
      .addCol("unpreferredRoutes", unpreferredRoutes)
      .addCol("bannedTrips", bannedTrips)
      .addCol("priorityGroupsByAgency", priorityGroupsByAgency)
      .addCol("priorityGroupsGlobal", priorityGroupsGlobal)
      .addObj("raptorDebugging", raptorDebugging, DEFAULT.raptorDebugging)
      .toString();
  }
}
