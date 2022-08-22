package org.opentripplanner.routing.api.request.refactor.request;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public class JourneyRequest {

  // TODO: 2022-08-22 maybe move it to transit so that it lies together with main modes?
  // TODO: 2022-08-18 should it be here?
  /**
   * The set of TraverseModes allowed when doing creating sub requests and doing street routing. //
   * TODO OTP2 Street routing requests should eventually be split into its own request class.
   */
  private TraverseModeSet streetSubRequestModes = new TraverseModeSet(TraverseMode.WALK);
  private TransitRequest transit = new TransitRequest();
  private StreetRequest access = new StreetRequest();
  private StreetRequest egress = new StreetRequest();
  private StreetRequest transfer = new StreetRequest();
  private StreetRequest direct = new StreetRequest();

  public TransitRequest transit() {
    return transit;
  }

  public StreetRequest access() {
    return access;
  }

  public StreetRequest egress() {
    return egress;
  }

  public StreetRequest transfer() {
    return transfer;
  }

  public StreetRequest direct() {
    return direct;
  }

  public void setStreetSubRequestModes(TraverseModeSet streetSubRequestModes) {
    this.streetSubRequestModes = streetSubRequestModes;
  }

  public TraverseModeSet streetSubRequestModes() {
    return streetSubRequestModes;
  }
}
