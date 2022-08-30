package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public class JourneyRequest implements Cloneable, Serializable {

  /**
   * The set of TraverseModes allowed when doing creating sub requests and doing street routing. //
   * TODO OTP2 Street routing requests should eventually be split into its own request class.
   */
  private TraverseModeSet streetSubRequestModes = new TraverseModeSet(TraverseMode.WALK);

  // TODO: 2022-08-23 is it right place for it?
  /**
   * Accept only paths that use transit (no street-only paths).
   *
   * @Deprecated TODO OTP2 Regression. Not currently working in OTP2. This is only used in the
   * deprecated Transmodel GraphQL API.
   */
  @Deprecated
  private boolean onlyTransitTrips = false;

  private VehicleRentalRequest rental = new VehicleRentalRequest();
  private VehicleParkingRequest parking = new VehicleParkingRequest();
  private TransitRequest transit = new TransitRequest();
  private StreetRequest access = new StreetRequest();
  private StreetRequest egress = new StreetRequest();
  private StreetRequest transfer = new StreetRequest();
  private StreetRequest direct = new StreetRequest();

  public VehicleRentalRequest rental() {
    return rental;
  }

  public VehicleParkingRequest parking() {
    return parking;
  }

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

  public void setOnlyTransitTrips(boolean onlyTransitTrips) {
    this.onlyTransitTrips = onlyTransitTrips;
  }

  public boolean onlyTransitTrips() {
    return onlyTransitTrips;
  }

  public JourneyRequest clone() {
    try {
      var clone = (JourneyRequest) super.clone();
      clone.rental = this.rental.clone();
      clone.parking = this.parking.clone();
      clone.transit = this.transit.clone();
      clone.access = this.access.clone();
      clone.egress = this.egress.clone();
      clone.transfer = this.transfer.clone();
      clone.direct = this.direct.clone();
      clone.streetSubRequestModes = this.streetSubRequestModes.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
