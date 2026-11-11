package org.opentripplanner.street.model.path;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.model.edge.ExternalLegEdge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.model.path.step.StreetStep;
import org.opentripplanner.street.search.TraverseMode;

/**
 * A leg consisting of a single {@link ExternalLegEdge}. The street module does not know what the
 * edge represents, so it is up to the caller to turn it into something meaningful.
 * <p>
 * The values are taken from the states before and after traversing the edge. An external edge
 * has no elevation data and no turn-by-turn directions.
 *
 * @deprecated This is a temporary stopgap so that flex trips, which are modelled as edges in the
 * street graph, can be returned as a leg of a {@link StreetPath}. It will be refactored away once
 * flex no longer depends on internal street model classes.
 */
@Deprecated
public record ExternalEdgeLeg(
  ExternalLegEdge edge,
  TraverseMode mode,
  Instant startTime,
  Instant endTime,
  StreetLegPlace from,
  StreetLegPlace to,
  double distanceMeters,
  int generalizedCost,
  LineString geometry,
  boolean rentedVehicle,
  @Nullable String vehicleRentalNetwork
) implements StreetLeg {
  @Nullable
  @Override
  public ElevationProfile elevationProfile() {
    return null;
  }

  @Override
  public List<StreetStep> steps() {
    return List.of();
  }
}
