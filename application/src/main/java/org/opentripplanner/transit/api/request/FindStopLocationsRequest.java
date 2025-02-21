package org.opentripplanner.transit.api.request;

import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A request for finding {@link StopLocation}.
 * </p>
 * This request is used to retrieve StopLocations that match the provided filter values.
 */
public class FindStopLocationsRequest {

  private String name;

  protected FindStopLocationsRequest(String name) {
    this.name = name;
  }

  public static FindStopLocationsRequestBuilder of() {
    return new FindStopLocationsRequestBuilder();
  }

  public String name() {
    return name;
  }
}
