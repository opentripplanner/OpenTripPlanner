package org.opentripplanner.routing.api.request;

public class RequestModesBuilder {

  private StreetMode accessMode;
  private StreetMode egressMode;
  private StreetMode directMode;
  private StreetMode transferMode;

  RequestModesBuilder(RequestModes origin) {
    this.accessMode = origin.accessMode;
    this.egressMode = origin.egressMode;
    this.directMode = origin.directMode;
    this.transferMode = origin.transferMode;
  }

  public StreetMode accessMode() {
    return accessMode;
  }

  public RequestModesBuilder withAccessMode(StreetMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public StreetMode egressMode() {
    return egressMode;
  }

  public RequestModesBuilder withEgressMode(StreetMode egressMode) {
    this.egressMode = egressMode;
    return this;
  }

  public StreetMode directMode() {
    return directMode;
  }

  public RequestModesBuilder withDirectMode(StreetMode directMode) {
    this.directMode = directMode;
    return this;
  }

  public StreetMode transferMode() {
    return transferMode;
  }

  public RequestModesBuilder withTransferMode(StreetMode transferMode) {
    this.transferMode = transferMode;
    return this;
  }

  public RequestModesBuilder withAllStreetModes(StreetMode streetMode) {
    return withAccessMode(streetMode)
      .withEgressMode(streetMode)
      .withDirectMode(streetMode)
      .withTransferMode(streetMode);
  }

  public RequestModes build() {
    return new RequestModes(this);
  }
}
