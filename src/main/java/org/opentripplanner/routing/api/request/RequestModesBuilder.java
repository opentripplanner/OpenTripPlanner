package org.opentripplanner.routing.api.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.transit.model.network.MainAndSubMode;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

public class RequestModesBuilder {

  private StreetMode accessMode;
  private StreetMode egressMode;
  private StreetMode directMode;
  private StreetMode transferMode;
  private List<MainAndSubMode> transitModes;

  RequestModesBuilder(RequestModes origin) {
    this.accessMode = origin.accessMode;
    this.egressMode = origin.egressMode;
    this.directMode = origin.directMode;
    this.transferMode = origin.transferMode;
    this.transitModes = new ArrayList<>(origin.transitModes);
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

  public List<MainAndSubMode> transitModes() {
    return transitModes;
  }

  public RequestModesBuilder withTransitMode(TransitMode mainMode) {
    this.transitModes.add(new MainAndSubMode(mainMode, null));
    return this;
  }

  public RequestModesBuilder withTransitMode(TransitMode mainMode, String subMode) {
    this.transitModes.add(new MainAndSubMode(mainMode, SubMode.of(subMode)));
    return this;
  }

  public RequestModesBuilder clearTransitMode() {
    this.transitModes.clear();
    return this;
  }

  public RequestModesBuilder withTransitModes(Collection<MainAndSubMode> transitModes) {
    this.transitModes = new ArrayList<>(transitModes);
    return this;
  }

  public RequestModes build() {
    return new RequestModes(this);
  }
}
