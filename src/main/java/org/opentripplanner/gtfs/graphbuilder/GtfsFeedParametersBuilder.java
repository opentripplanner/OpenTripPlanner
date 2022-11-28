package org.opentripplanner.gtfs.graphbuilder;

import java.net.URI;
import org.opentripplanner.transit.model.site.StopTransferPriority;

/**
 * Configure a GTFS feed.
 */
public class GtfsFeedParametersBuilder {

  private URI source;
  private String feedId;
  private boolean removeRepeatedStops = true;
  private StopTransferPriority stationTransferPreference;

  public GtfsFeedParametersBuilder withFeedId(String feedId) {
    this.feedId = feedId;
    return this;
  }

  String feedId() {
    return feedId;
  }

  public GtfsFeedParametersBuilder withStationTransferPreference(
    StopTransferPriority stationTransferPreference
  ) {
    this.stationTransferPreference = stationTransferPreference;
    return this;
  }

  StopTransferPriority stationTransferPreference() {
    return stationTransferPreference;
  }

  public GtfsFeedParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  URI source() {
    return source;
  }

  public GtfsFeedParametersBuilder withRemoveRepeatedStops(boolean value) {
    this.removeRepeatedStops = value;
    return this;
  }

  boolean removeRepeatedStops() {
    return removeRepeatedStops;
  }

  public GtfsFeedParameters build() {
    return new GtfsFeedParameters(this);
  }
}
