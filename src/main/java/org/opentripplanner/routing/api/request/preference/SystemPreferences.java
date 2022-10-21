package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RoutingTag;

// TODO VIA: Javadoc
public class SystemPreferences implements Cloneable, Serializable {

  @Nonnull
  private ItineraryFilterParameters itineraryFilters = ItineraryFilterParameters.createDefault();

  private Set<RoutingTag> tags = Set.of();
  private DataOverlayParameters dataOverlay;
  private boolean geoidElevation = false;

  @Deprecated
  private boolean disableAlertFiltering = false;

  private Duration maxJourneyDuration = Duration.ofHours(24);

  @Nonnull
  public ItineraryFilterParameters itineraryFilters() {
    return itineraryFilters;
  }

  public void setItineraryFilters(@Nonnull ItineraryFilterParameters itineraryFilters) {
    this.itineraryFilters = itineraryFilters;
  }

  /**
   * List of OTP request tags, these are used to cross-cutting concerns like logging and micrometer
   * tags. Currently, all tags are added to all the timer instances for this request.
   */
  public Set<RoutingTag> tags() {
    return tags;
  }

  public void setTags(Set<RoutingTag> tags) {
    this.tags = tags;
  }

  /**
   * The filled request parameters for penalties and thresholds values
   */
  public DataOverlayParameters dataOverlay() {
    return dataOverlay;
  }

  public void setDataOverlay(DataOverlayParameters dataOverlay) {
    this.dataOverlay = dataOverlay;
  }

  /** Whether to apply the ellipsoid→geoid offset to all elevations in the response */
  public boolean geoidElevation() {
    return geoidElevation;
  }

  public void setGeoidElevation(boolean geoidElevation) {
    this.geoidElevation = geoidElevation;
  }

  /** Option to disable the default filtering of GTFS-RT alerts by time. */
  public boolean disableAlertFiltering() {
    return disableAlertFiltering;
  }

  public void setDisableAlertFiltering(boolean disableAlertFiltering) {
    this.disableAlertFiltering = disableAlertFiltering;
  }

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
  public Duration maxJourneyDuration() {
    return maxJourneyDuration;
  }

  public void setMaxJourneyDuration(Duration maxJourneyDuration) {
    this.maxJourneyDuration = maxJourneyDuration;
  }

  public SystemPreferences clone() {
    try {
      // TODO VIA (Thomas): 2022-08-26 leaving out dataOverlay (that's how it was before)

      var clone = (SystemPreferences) super.clone();

      clone.itineraryFilters = new ItineraryFilterParameters(this.itineraryFilters);
      clone.tags = new HashSet<>(this.tags);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
