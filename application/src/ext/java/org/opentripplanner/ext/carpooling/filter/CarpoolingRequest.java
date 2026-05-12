package org.opentripplanner.ext.carpooling.filter;

import java.time.Instant;
import java.util.Objects;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Encapsulates the passenger-facing parameters of a carpooling routing request: pickup and dropoff
 * coordinates, requested time, whether it is an arrive-by or depart-after search, and — for
 * transit-combined searches — whether the carpool leg is access or egress.
 * <p>
 * Instances are constructed from a {@link org.opentripplanner.routing.api.request.RouteRequest}
 * via the {@link #of} factory methods.
 */
public class CarpoolingRequest {

  private final AccessEgressType accessOrEgress;
  private final boolean isArriveByRequest;
  private final WgsCoordinate passengerPickup;
  private final WgsCoordinate passengerDropoff;
  private final Instant requestedDateTime;

  CarpoolingRequest(
    AccessEgressType accessOrEgress,
    boolean isArriveByRequest,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Instant requestedDateTime
  ) {
    this.accessOrEgress = accessOrEgress;
    this.isArriveByRequest = isArriveByRequest;
    this.passengerPickup = passengerPickup;
    this.passengerDropoff = passengerDropoff;
    this.requestedDateTime = requestedDateTime;
  }

  public static CarpoolingRequest of(RouteRequest request) {
    return new CarpoolingRequestBuilder(request).build();
  }

  public static CarpoolingRequest of(RouteRequest request, AccessEgressType accessOrEgress) {
    return new CarpoolingRequestBuilder(request).withAccessOrEgress(accessOrEgress).build();
  }

  public AccessEgressType getAccessOrEgress() {
    return accessOrEgress;
  }

  /**
   * Returns {@code true} if this is an access-egress request (the carpool leg connects to a
   * transit stop), determined by the presence of {@code accessOrEgress}.
   * Returns {@code false} for direct routing (origin to destination without transit).
   */
  public boolean isAccessEgressRequest() {
    return accessOrEgress != null;
  }

  /**
   * Returns {@code true} if this is an access leg (passenger origin to transit stop).
   * Returns {@code false} for egress legs and for direct routing (where {@code accessOrEgress} is
   * {@code null}).
   */
  public boolean isAccessRequest() {
    return accessOrEgress != null && accessOrEgress.isAccess();
  }

  /**
   * Returns {@code true} if this is an egress leg (transit stop to passenger destination).
   * Returns {@code false} for access legs and for direct routing (where {@code accessOrEgress} is
   * {@code null}).
   */
  public boolean isEgressRequest() {
    return accessOrEgress != null && accessOrEgress.isEgress();
  }

  public boolean isArriveByRequest() {
    return isArriveByRequest;
  }

  public WgsCoordinate getPassengerPickup() {
    return passengerPickup;
  }

  public WgsCoordinate getPassengerDropoff() {
    return passengerDropoff;
  }

  public Instant getRequestedDateTime() {
    return requestedDateTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      accessOrEgress,
      isArriveByRequest,
      passengerPickup,
      passengerDropoff,
      requestedDateTime
    );
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CarpoolingRequest that = (CarpoolingRequest) o;
    return (
      isArriveByRequest == that.isArriveByRequest &&
      accessOrEgress == that.accessOrEgress &&
      Objects.equals(passengerPickup, that.passengerPickup) &&
      Objects.equals(passengerDropoff, that.passengerDropoff) &&
      Objects.equals(requestedDateTime, that.requestedDateTime)
    );
  }

  @Override
  public String toString() {
    return (
      "CarpoolingRequest{" +
      "accessOrEgress=" +
      accessOrEgress +
      ", isArriveByRequest=" +
      isArriveByRequest +
      ", passengerPickup=" +
      passengerPickup +
      ", passengerDropoff=" +
      passengerDropoff +
      ", requestedDateTime=" +
      requestedDateTime +
      '}'
    );
  }
}
