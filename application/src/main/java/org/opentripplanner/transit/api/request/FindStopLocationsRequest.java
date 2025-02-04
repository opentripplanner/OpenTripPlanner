package org.opentripplanner.transit.api.request;

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
