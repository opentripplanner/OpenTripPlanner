package org.opentripplanner.ext.carpickupzone.model;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public class CarPickupZoneLeg extends StreetLeg {

  private final CarPickupZone carPickupZone;

  public CarPickupZoneLeg(StreetLeg streetLeg, CarPickupZone carPickupZone) {
    super(streetLeg.copyOf());
    this.carPickupZone = carPickupZone;
  }

  public CarPickupZone carPickupZone() {
    return carPickupZone;
  }

  public TransitMode mode() {
    return carPickupZone.route().getMode();
  }

  @Override
  public Agency agency() {
    return carPickupZone.route().getAgency();
  }

  @Override
  public Route route() {
    return carPickupZone.route();
  }

  @Override
  @Nullable
  public BookingInfo pickupBookingInfo() {
    return carPickupZone.pickupBookingInfo();
  }

  @Override
  @Nullable
  public BookingInfo dropOffBookingInfo() {
    return carPickupZone.dropOffBookingInfo();
  }

  @Override
  public Leg withEmissionPerPerson(Emission emissionPerPerson) {
    return new CarPickupZoneLeg(
      (StreetLeg) super.withEmissionPerPerson(emissionPerPerson),
      carPickupZone
    );
  }

  @Override
  public StreetLeg withAccessibilityScore(float accessibilityScore) {
    return new CarPickupZoneLeg(
      (StreetLeg) super.withAccessibilityScore(accessibilityScore),
      carPickupZone
    );
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return new CarPickupZoneLeg((StreetLeg) super.withTimeShift(duration), carPickupZone);
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return other instanceof CarPickupZoneLeg o && mode().equals(o.mode());
  }
}
