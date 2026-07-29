package org.opentripplanner.graph_builder.module.osm;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.util.List;
import org.geotools.geometry.jts.JTS;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.geometry.Coordinates;

class PlatformEntranceCandidatesIndexTest {

  @Test
  void query(){
    var v1 = intersectionVertex(0,0);
    var v2 = intersectionVertex(0.00001, 0.00001);
    var v3 = intersectionVertex(0.00002, 0.00002);
    streetEdge(v1, v2, PEDESTRIAN);
    streetEdge(v2, v1, PEDESTRIAN);
    streetEdge(v2, v3, PEDESTRIAN);
    streetEdge(v3, v2, PEDESTRIAN);

    var hamburg = intersectionVertex(2,2);
    var index = PlatformEntranceFinder.of(List.of(v1, v2, v3, hamburg));
    var env = new Envelope();
    env.expandToInclude(Coordinates.BERLIN);
    env.expandBy(0.001);
    var polygon = JTS.toGeometry(env);
    assertThat(index.findCandidates(polygon)).contains(v1);
  }
}