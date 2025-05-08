package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class TransitRequest implements Cloneable, Serializable {

  public static final TransitRequest DEFAULT = new TransitRequest(
    List.of(AllowAllTransitFilter.of()),
    List.of(),
    List.of(),
    List.of(),
    List.of(),
    new ArrayList<>(),
    new ArrayList<>(),
    new ArrayList<>(),
    new DebugRaptor()
  );

  private List<TransitFilter> filters;

  @Deprecated
  private List<FeedScopedId> preferredAgencies;

  private List<FeedScopedId> unpreferredAgencies;

  /**
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  private List<FeedScopedId> preferredRoutes;

  private List<FeedScopedId> unpreferredRoutes;
  private List<FeedScopedId> bannedTrips;
  private List<TransitGroupSelect> priorityGroupsByAgency;
  private List<TransitGroupSelect> priorityGroupsGlobal;
  private DebugRaptor raptorDebugging;

  public TransitRequest() {
    this.filters = List.of(AllowAllTransitFilter.of());
    this.preferredAgencies = List.of();
    this.unpreferredAgencies = List.of();
    this.preferredRoutes = List.of();
    this.unpreferredRoutes = List.of();
    this.bannedTrips = new ArrayList<>();
    this.priorityGroupsByAgency = new ArrayList<>();
    this.priorityGroupsGlobal = new ArrayList<>();
    this.raptorDebugging = new DebugRaptor();
  }

  public TransitRequest(
    List<TransitFilter> filters,
    List<FeedScopedId> preferredAgencies,
    List<FeedScopedId> unpreferredAgencies,
    List<FeedScopedId> preferredRoutes,
    List<FeedScopedId> unpreferredRoutes,
    List<FeedScopedId> bannedTrips,
    List<TransitGroupSelect> priorityGroupsByAgency,
    List<TransitGroupSelect> priorityGroupsGlobal,
    DebugRaptor raptorDebugging
  ) {
    this.filters = filters;
    this.preferredAgencies = preferredAgencies;
    this.unpreferredAgencies = unpreferredAgencies;
    this.preferredRoutes = preferredRoutes;
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
  public void setBannedTrips(List<FeedScopedId> bannedTrips) {
    this.bannedTrips = bannedTrips;
  }

  public List<FeedScopedId> bannedTrips() {
    return bannedTrips;
  }

  public List<TransitFilter> filters() {
    return filters;
  }

  @Deprecated
  public void setFilters(List<TransitFilter> filters) {
    this.filters = filters;
  }

  /**
   * A unique group-id is assigned all patterns grouped by matching select and agency.
   * In other words, two patterns matching the same select and with the same agency-id
   * will get the same group-id.
   * <p>
   * Note! Entities that are not matched are put in the BASE-GROUP with id 0.
   */
  public List<TransitGroupSelect> priorityGroupsByAgency() {
    return priorityGroupsByAgency;
  }

  /**
   * All patterns matching the same select will be assigned the same group-id.
   */
  @Deprecated
  public void addPriorityGroupsByAgency(Collection<TransitGroupSelect> priorityGroupsByAgency) {
    this.priorityGroupsByAgency.addAll(priorityGroupsByAgency);
  }

  /**
   * A unique group-id is assigned all patterns grouped by matching selects.
   * <p>
   * Note! Entities that are not matched are put in the BASE-GROUP with id 0.
   */
  public List<TransitGroupSelect> priorityGroupsGlobal() {
    return priorityGroupsGlobal;
  }

  @Deprecated
  public void addPriorityGroupsGlobal(Collection<TransitGroupSelect> priorityGroupsGlobal) {
    this.priorityGroupsGlobal.addAll(priorityGroupsGlobal);
  }

  @Deprecated
  public void setPreferredAgencies(List<FeedScopedId> preferredAgencies) {
    this.preferredAgencies = preferredAgencies;
  }

  /**
   * List of preferred agencies by user.
   */
  @Deprecated
  public List<FeedScopedId> preferredAgencies() {
    return preferredAgencies;
  }

  @Deprecated
  public void setUnpreferredAgencies(List<FeedScopedId> unpreferredAgencies) {
    this.unpreferredAgencies = unpreferredAgencies;
  }

  public List<FeedScopedId> unpreferredAgencies() {
    return unpreferredAgencies;
  }

  @Deprecated
  public void setPreferredRoutes(List<FeedScopedId> preferredRoutes) {
    this.preferredRoutes = preferredRoutes;
  }

  /**
   * Set of preferred routes by user and configuration.
   */
  @Deprecated
  public List<FeedScopedId> preferredRoutes() {
    return preferredRoutes;
  }

  @Deprecated
  public void setUnpreferredRoutes(List<FeedScopedId> unpreferredRoutes) {
    this.unpreferredRoutes = unpreferredRoutes;
  }

  /**
   * Set of unpreferred routes for given user and configuration.
   */
  public List<FeedScopedId> unpreferredRoutes() {
    return unpreferredRoutes;
  }

  @Deprecated
  public void setRaptorDebugging(DebugRaptor raptorDebugging) {
    this.raptorDebugging = raptorDebugging;
  }

  public DebugRaptor raptorDebugging() {
    return raptorDebugging;
  }

  public TransitRequest clone() {
    try {
      var clone = (TransitRequest) super.clone();

      clone.preferredAgencies = List.copyOf(this.preferredAgencies);
      clone.unpreferredAgencies = List.copyOf(this.unpreferredAgencies);
      clone.preferredRoutes = List.copyOf(this.preferredRoutes);
      clone.unpreferredRoutes = List.copyOf(this.unpreferredRoutes);
      clone.raptorDebugging = new DebugRaptor(this.raptorDebugging);
      clone.priorityGroupsByAgency = new ArrayList<>(this.priorityGroupsByAgency);
      clone.priorityGroupsGlobal = new ArrayList<>(this.priorityGroupsGlobal);

      // filters are immutable
      clone.setFilters(this.filters);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns whether it is requested to run the transit search for this request.
   */
  public boolean enabled() {
    return filters.stream().noneMatch(ExcludeAllTransitFilter.class::isInstance);
  }

  /**
   * Disables the transit search for this request, for example when you only want bike routes.
   */
  public void disable() {
    this.filters = List.of(ExcludeAllTransitFilter.of());
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
      Objects.equals(preferredAgencies, that.preferredAgencies) &&
      Objects.equals(unpreferredAgencies, that.unpreferredAgencies) &&
      Objects.equals(preferredRoutes, that.preferredRoutes) &&
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
      preferredAgencies,
      unpreferredAgencies,
      preferredRoutes,
      unpreferredRoutes,
      bannedTrips,
      priorityGroupsByAgency,
      priorityGroupsGlobal,
      raptorDebugging
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransitRequest.class)
      .addCol("filters", filters, DEFAULT.filters)
      .addCol("preferredAgencies", preferredAgencies)
      .addCol("unpreferredAgencies", unpreferredAgencies)
      .addCol("preferredRoutes", preferredRoutes)
      .addCol("unpreferredRoutes", unpreferredRoutes)
      .addCol("bannedTrips", bannedTrips)
      .addCol("priorityGroupsByAgency", priorityGroupsByAgency)
      .addCol("priorityGroupsGlobal", priorityGroupsGlobal)
      .addObj("raptorDebugging", raptorDebugging, DEFAULT.raptorDebugging)
      .toString();
  }
}
