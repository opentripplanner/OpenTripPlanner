package org.opentripplanner.street.model.edge;

import java.util.List;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

/**
 * This represents the connection between a boarding location and a transit vertex where going from the
 * street to the vehicle is immediate and you don't want to display a polyline to the user.
 */
public class BoardingLocationToStopLink extends StreetTransitEntityLink<TransitStopVertex> {

  private BoardingLocationToStopLink(StreetVertex fromv, TransitStopVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
  }

  private BoardingLocationToStopLink(TransitStopVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
  }

  public static BoardingLocationToStopLink createBoardingLocationToStopLink(
    StreetVertex fromv,
    TransitStopVertex tov
  ) {
    return connectToGraph(new BoardingLocationToStopLink(fromv, tov));
  }

  public static BoardingLocationToStopLink createBoardingLocationToStopLink(
    TransitStopVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new BoardingLocationToStopLink(fromv, tov));
  }

  protected int getStreetToStopTime() {
    return 0;
  }

  @Override
  public LineString getGeometry() {
    return GeometryUtils.makeLineString(List.of(fromv.getCoordinate(), tov.getCoordinate()));
  }
}
