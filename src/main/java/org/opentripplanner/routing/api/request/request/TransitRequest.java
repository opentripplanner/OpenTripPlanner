package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.api.request.request.filter.FilterPredicate;
import org.opentripplanner.routing.api.request.request.filter.FilterRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

// TODO VIA: Javadoc
public class TransitRequest implements Cloneable, Serializable {

  private List<FilterPredicate> filters = new ArrayList<>();

  @Deprecated
  private List<FeedScopedId> preferredAgencies = List.of();

  private List<FeedScopedId> unpreferredAgencies = List.of();

  /**
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  private List<FeedScopedId> preferredRoutes = List.of();

  private List<FeedScopedId> unpreferredRoutes = List.of();
  private DebugRaptor raptorDebugging = new DebugRaptor();

  public List<FilterPredicate> filters() {
    return filters;
  }

  public void setFilters(List<FilterPredicate> filters) {
    this.filters = filters;
  }

  @Deprecated
  public void setPreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      preferredAgencies = FeedScopedId.parseListOfIds(s);
    }
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

  public void setUnpreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredAgencies = FeedScopedId.parseListOfIds(s);
    }
  }

  /**
   * List of unpreferred agencies for given user.
   */
  public void setUnpreferredAgencies(List<FeedScopedId> unpreferredAgencies) {
    this.unpreferredAgencies = unpreferredAgencies;
  }

  public List<FeedScopedId> unpreferredAgencies() {
    return unpreferredAgencies;
  }

  @Deprecated
  public void setPreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      preferredRoutes = List.copyOf(FeedScopedId.parseListOfIds(s));
    } else {
      preferredRoutes = List.of();
    }
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

  public void setUnpreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredRoutes = List.copyOf(FeedScopedId.parseListOfIds(s));
    } else {
      unpreferredRoutes = List.of();
    }
  }

  public void setUnpreferredRoutes(List<FeedScopedId> unpreferredRoutes) {
    this.unpreferredRoutes = unpreferredRoutes;
  }

  /**
   * Set of unpreferred routes for given user and configuration.
   */
  public List<FeedScopedId> unpreferredRoutes() {
    return unpreferredRoutes;
  }

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

      var cloneFilters = new ArrayList<FilterPredicate>();
      for (var filterRequest : this.filters) {
        cloneFilters.add(filterRequest.clone());
      }
      clone.setFilters(cloneFilters);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
