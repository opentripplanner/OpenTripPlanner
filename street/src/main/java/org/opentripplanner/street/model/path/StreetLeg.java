package org.opentripplanner.street.model.path;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.model.path.step.StreetStep;
import org.opentripplanner.street.search.TraverseMode;

/**
 * A part of a {@link StreetPath} which is traversed in a single street mode, see
 * {@link StreetPath#computeLegs(double)}. Note, walking a bike does not start a new leg.
 */
public interface StreetLeg {
  Instant startTime();

  Instant endTime();

  /**
   * The mode of the leg. If a vehicle is rented, this is the mode of the rented vehicle.
   */
  TraverseMode mode();

  StreetLegPlace from();

  StreetLegPlace to();

  double distanceMeters();

  int generalizedCost();

  LineString geometry();

  /**
   * The elevation profile of the leg, or null if no elevation data is available.
   */
  @Nullable
  ElevationProfile elevationProfile();

  /**
   * The turn-by-turn directions of this leg.
   */
  List<StreetStep> steps();

  /**
   * Whether the leg is traversed with a rented vehicle.
   */
  boolean rentedVehicle();

  /**
   * The network of the rented vehicle, or null if no vehicle is rented or the network is unknown.
   */
  @Nullable
  String vehicleRentalNetwork();
}
