package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.routing.linking.VertexLinker.getNoThruModes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

public class BarrierEdgeBuilder {

  private final EdgeNamer edgeNamer;

  public BarrierEdgeBuilder(EdgeNamer edgeNamer) {
    this.edgeNamer = edgeNamer;
  }

  /**
   * Link the vertices provided which share the barriers given
   */
  public void build(OsmNode node, Collection<OsmVertex> vertices, Collection<OsmWay> barriers) {
    var permission = StreetTraversalPermission.ALL;
    var wheelchairAccessible = true;
    I18NString name = null;

    // I consider a node with barrier=* on a linear barrier a hole in that linear barrier.
    // For example, a node marked with barrier=bollard on a barrier=wall is a hole in the wall,
    // which allows pedestrians and bikes passing through.
    if (!node.hasTag("barrier")) {
      for (var barrier : barriers) {
        permission = permission.intersection(
          barrier.overridePermissions(StreetTraversalPermission.ALL)
        );
        wheelchairAccessible = wheelchairAccessible && barrier.isWheelchairAccessible();
        if (!barrier.hasNoName()) {
          name = edgeNamer.getNameForWay(barrier, ("barrier " + barrier.getId()).intern());
        }
      }
    }

    permission = node.overridePermissions(permission);
    if (permission.allowsNothing()) {
      return;
    }

    var vs = vertices.toArray(new OsmVertex[0]);
    var inDegrees = Arrays.stream(vs).map(Vertex::getDegreeIn).toArray(Integer[]::new);
    var outDegrees = Arrays.stream(vs).map(Vertex::getDegreeOut).toArray(Integer[]::new);
    for (var i = 0; i < vs.length; ++i) {
      for (var j = 0; j < vs.length; ++j) {
        if (i != j) {
          var from = vs[i];
          var to = vs[j];

          if (inDegrees[i] > 0 && outDegrees[j] > 0) {
            StreetEdgeBuilder<?> seb = new StreetEdgeBuilder<>()
              .withFromVertex(from)
              .withToVertex(to)
              .withGeometry(
                GeometryUtils.makeLineString(List.of(from.getCoordinate(), to.getCoordinate()))
              )
              .withName(
                name == null ? I18NString.of("barrier crossing at node " + vs[i].nodeId) : name
              )
              .withPermission(permission)
              .withWheelchairAccessible(wheelchairAccessible)
              .withBogusName(name == null);

            // If the nodes being connected has no through traffic mode set,
            // the edge crossing the barrier also needs to have no through traffic mode set as well.
            // This is to ensure that local traffic can get through the barrier between two
            // no through traffic areas.
            for (var mode : getNoThruModes(from.getIncoming())) {
              seb.withNoThruTrafficTraverseMode(mode);
            }
            for (var mode : getNoThruModes(to.getOutgoing())) {
              seb.withNoThruTrafficTraverseMode(mode);
            }

            seb.buildAndConnect();
          }
        }
      }
    }
  }
}
