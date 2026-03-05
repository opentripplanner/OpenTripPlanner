package org.opentripplanner.street.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

class StreetIndexTest {

  private final FeedScopedId STOP_ID = id("A");
  private final TransitStopVertex stopVertex = TransitStopVertex.of()
    .withId(STOP_ID)
    .withPoint(GeometryUtils.getGeometryFactory().createPoint(new Coordinate(0, 0)))
    .build();
  private final FeedScopedId STATION_ID = id("OMEGA");
  private final StationCentroidVertex centroidVertex = new StationCentroidVertex(
    STATION_ID,
    I18NString.of(STATION_ID.getId()),
    WgsCoordinate.GREENWICH
  );

  @Test
  void stopId() {
    var streetIndex = buildIndex();
    assertThat(streetIndex.findStopVertex(STOP_ID)).hasValue(stopVertex);
  }

  @Test
  void nonExistentId() {
    var streetIndex = buildIndex();
    assertThat(streetIndex.findStopVertex(id("non-existent-stop-id"))).isEmpty();
  }

  @Test
  void stationCentroid() {
    var streetIndex = buildIndex();
    assertThat(streetIndex.findStationCentroidVertex(STATION_ID)).hasValue(centroidVertex);
  }

  private StreetIndex buildIndex() {
    var graph = new Graph();
    graph.addVertex(stopVertex);
    graph.addVertex(centroidVertex);
    return new StreetIndex(graph);
  }
}
