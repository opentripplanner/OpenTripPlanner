package org.opentripplanner.street.linking;

import java.util.Objects;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * A linking candidate: a street edge paired with its squared equirectangular distance to the
 * vertex being linked, expressed in degrees latitude. Equality is based on the edge only, so a
 * {@code Set} of candidates holds each edge once whatever its recorded distance.
 */
final class CandidateEdge {

  private final StreetEdge edge;
  private final double squaredDistanceDegreesLat;

  CandidateEdge(StreetEdge edge, double squaredDistanceDegreesLat) {
    this.edge = edge;
    this.squaredDistanceDegreesLat = squaredDistanceDegreesLat;
  }

  StreetEdge edge() {
    return edge;
  }

  double squaredDistanceDegreesLat() {
    return squaredDistanceDegreesLat;
  }

  @Override
  public int hashCode() {
    return Objects.hash(edge);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CandidateEdge that = (CandidateEdge) o;
    return Objects.equals(edge, that.edge);
  }
}
