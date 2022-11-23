package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * This vertex is created from all barrier tags.
 * <p>
 * Currently only barrier=bollard is supported. Node barrier=bollard implies access=no, foot=yes,
 * bicycle=yes
 * <p>
 * On this vertex geometry is split and two new edges are created.
 * <p>
 * If start/end vertex of {@link StreetEdge} is BarrierVertex
 * edge isn't traversable with CAR.
 * <p>
 * Created by mabu on 11.5.2015.
 */
public class BarrierVertex extends OsmVertex {

  //According to OSM default permissions are access=no, foot=yes, bicycle=yes
  public static final StreetTraversalPermission defaultBarrierPermissions =
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
  private StreetTraversalPermission barrierPermissions;

  public BarrierVertex(Graph g, String label, double x, double y, long nodeId) {
    super(g, label, x, y, nodeId);
    barrierPermissions = defaultBarrierPermissions;
  }

  public StreetTraversalPermission getBarrierPermissions() {
    return barrierPermissions;
  }

  public void setBarrierPermissions(StreetTraversalPermission barrierPermissions) {
    this.barrierPermissions = barrierPermissions;
  }
}
