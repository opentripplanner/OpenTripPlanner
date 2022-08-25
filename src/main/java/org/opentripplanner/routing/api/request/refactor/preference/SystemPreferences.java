package org.opentripplanner.routing.api.request.refactor.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RoutingTag;

public class SystemPreferences implements Cloneable, Serializable {

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
  private boolean geoidElevation = false;

  /** Option to disable the default filtering of GTFS-RT alerts by time. */
  @Deprecated
  private boolean disableAlertFiltering = false;

  // TODO: 2022-08-23 refactor
  // The REST API should hold onto this, and pass it to the mapper, no need to include it in the
  // request.
  /** Whether the planner should return intermediate stops lists for transit legs. */
  private boolean showIntermediateStops = false;

  // TODO: 2022-08-18 Should it be here?
  /**
   * The expected maximum time a journey can last across all possible journeys for the current
   * deployment. Normally you would just do an estimate and add enough slack, so you are sure that
   * there is no journeys that falls outside this window. The parameter is used find all possible
   * dates for the journey and then search only the services which run on those dates. The duration
   * must include access, egress, wait-time and transit time for the whole journey. It should also
   * take low frequency days/periods like holidays into account. In other words, pick the two points
   * within your area that has the worst connection and then try to travel on the worst possible
   * day, and find the maximum journey duration. Using a value that is too high has the effect of
   * including more patterns in the search, hence, making it a bit slower. Recommended values would
   * be from 12 hours(small town/city), 1 day (region) to 2 days (country like Norway).
   */
  private Duration maxJourneyDuration = Duration.ofHours(24);

  public void setItineraryFilters(@Nonnull ItineraryFilterParameters itineraryFilters) {
    this.itineraryFilters = itineraryFilters;
  }

  @Nonnull
  public ItineraryFilterParameters itineraryFilters() {
    return itineraryFilters;
  }

  public void setTags(Set<RoutingTag> tags) {
    this.tags = tags;
  }

  public Set<RoutingTag> tags() {
    return tags;
  }

  public void setDataOverlay(DataOverlayParameters dataOverlay) {
    this.dataOverlay = dataOverlay;
  }

  public DataOverlayParameters dataOverlay() {
    return dataOverlay;
  }

  public void setGeoidElevation(boolean geoidElevation) {
    this.geoidElevation = geoidElevation;
  }

  public boolean geoidElevation() {
    return geoidElevation;
  }

  public void setDisableAlertFiltering(boolean disableAlertFiltering) {
    this.disableAlertFiltering = disableAlertFiltering;
  }

  public boolean disableAlertFiltering() {
    return disableAlertFiltering;
  }

  public void setShowIntermediateStops(boolean showIntermediateStops) {
    this.showIntermediateStops = showIntermediateStops;
  }

  public boolean showIntermediateStops() {
    return showIntermediateStops;
  }

  public void setMaxJourneyDuration(Duration maxJourneyDuration) {
    this.maxJourneyDuration = maxJourneyDuration;
  }

  public Duration maxJourneyDuration() {
    return maxJourneyDuration;
  }
}
