package org.opentripplanner.street.model.path;

import java.util.List;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.state.State;

/// This class represents a path within the street network
public class StreetPath {

  private final List<StreetPathSegment> segments;

  public StreetPath(State endState) {
    this(new StreetPathSegment(new GraphPath<>(endState)));
  }

  public StreetPath(StreetPathSegment segment) {
    this(List.of(segment));
  }

  public StreetPath(List<StreetPathSegment> segments) {
    this.segments = segments;
  }

  public List<StreetPathSegment> segments() {
    return segments;
  }

  public double weight() {
    return segments.stream().mapToDouble(StreetPathSegment::finalWeight).sum();
  }

  public boolean arrivedToDestinationOnRentedVehicle() {
    return segments.getLast().lastState().isRentingVehicleFromStation();
  }

  /// Calculate the elevationGained and elevationLost
  public ElevationChange calculateElevations() {
    double elevationGained_m = 0.0;
    double elevationLost_m = 0.0;
    var edges = segments
      .stream()
      .flatMap(s -> s.edges().stream())
      .toList();
    for (Edge edge : edges) {
      if (!(edge instanceof StreetEdge edgeWithElevation)) {
        continue;
      }
      PackedCoordinateSequence coordinates = edgeWithElevation.getElevationProfile();

      if (coordinates == null) {
        continue;
      }
      // TODO Check the test below, AFAIU current elevation profile has 3 dimensions.
      if (coordinates.getDimension() != 2) {
        continue;
      }

      for (int i = 0; i < coordinates.size() - 1; i++) {
        double change_m = coordinates.getOrdinate(i + 1, 1) - coordinates.getOrdinate(i, 1);
        if (change_m > 0.0) {
          elevationGained_m += change_m;
        } else {
          elevationLost_m -= change_m;
        }
      }
    }
    return new ElevationChange(elevationGained_m, elevationLost_m);
  }
}
