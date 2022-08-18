package org.opentripplanner.routing.api.request.refactor.preference;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RoutingTag;

public class SystemPreferences {

  @Nonnull
  private ItineraryFilterParameters itineraryFilters = ItineraryFilterParameters.createDefault();
  // TODO: 2022-08-17 is this right?
  /**
   * List of OTP request tags, these are used to cross-cutting concerns like logging and micrometer
   * tags. Currently, all tags are added to all the timer instances for this request.
   */
  private Set<RoutingTag> tags = Set.of();
  /**
   * The filled request parameters for penalties and thresholds values
   */
  private DataOverlayParameters dataOverlay;
  /** Whether to apply the ellipsoid→geoid offset to all elevations in the response */
  private boolean geoidElevation=false;

  // The REST API should hold onto this, and pass it to the mapper, no need to include it in the
  // request.
  // boolean showIntermediateStops=false;


  @Nonnull
  public ItineraryFilterParameters itineraryFilters() {
    return itineraryFilters;
  }

  public Set<RoutingTag> tags() {
    return tags;
  }

  public DataOverlayParameters dataOverlay() {
    return dataOverlay;
  }

  public boolean geoidElevation() {
    return geoidElevation;
  }
}
