package org.opentripplanner.transit.api.request;

public class FindStopLocationsRequestBuilder {

  private String name;

  protected FindStopLocationsRequestBuilder() {}

  public FindStopLocationsRequestBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public FindStopLocationsRequest build() {
    return new FindStopLocationsRequest(name);
  }
}
