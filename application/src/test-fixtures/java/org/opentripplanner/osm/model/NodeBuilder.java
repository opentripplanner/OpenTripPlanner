package org.opentripplanner.osm.model;

import org.opentripplanner.street.geometry.WgsCoordinate;

public class NodeBuilder {

  public static OsmNodeBuilder of() {
    return OsmNode.of();
  }

  public static OsmNodeBuilder of(long id, WgsCoordinate wgsCoordinate) {
    return OsmNode.of().withId(id).withLatLon(wgsCoordinate.latitude(), wgsCoordinate.longitude());
  }

  public static OsmNode node(long id, WgsCoordinate wgsCoordinate) {
    return of(id, wgsCoordinate).build();
  }
}
