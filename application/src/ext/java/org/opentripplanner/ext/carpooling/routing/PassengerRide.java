package org.opentripplanner.ext.carpooling.routing;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.carpooling.street.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A ride a passenger could take with a carpool, between two vertices the driver can stop at.
 *
 * @param pickup      where the driver picks the passenger up, with the passenger's walk to it
 * @param dropoff     where the driver drops the passenger off, with the passenger's walk from it
 * @param transitStop the transit stop an access ride ends at or an egress ride starts at;
 *                    {@code null} for a direct ride
 */
public record PassengerRide(
  SnapResult pickup,
  SnapResult dropoff,
  @Nullable StopLocation transitStop
) {}
