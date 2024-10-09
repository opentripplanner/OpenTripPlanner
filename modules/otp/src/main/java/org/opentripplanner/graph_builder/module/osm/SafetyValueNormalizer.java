package org.opentripplanner.graph_builder.module.osm;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.Graphwide;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapper;
import org.opentripplanner.openstreetmap.wayproperty.WayProperties;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeList;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.NamedArea;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Normalizes bike & walk safety values so that the lowest one is no lower than 1.0.
 * <p>
 * Normalization of the safety values is desirable because that means that the heuristic
 * can somewhat accurately predict the remaining cost of the path and won't prune out
 * optimal paths.
 * <p>
 * Further reading: https://github.com/opentripplanner/OpenTripPlanner/issues/4442
 */
class SafetyValueNormalizer {

  private final Graph graph;
  private final DataImportIssueStore issueStore;
  /**
   * The bike safety factor of the safest street
   */
  private float bestBikeSafety = 1.0f;
  /**
   * The walk safety factor of the safest street
   */
  private float bestWalkSafety = 1.0f;

  SafetyValueNormalizer(Graph graph, DataImportIssueStore issueStore) {
    this.graph = graph;
    this.issueStore = issueStore;
  }

  /**
   * The safest bike lane should have a safety weight no lower than the time weight of a flat
   * street. This method divides the safety lengths by the length ratio of the safest street,
   * ensuring this property.
   */
  void applySafetyFactors() {
    issueStore.add(new Graphwide("Multiplying all bike safety values by " + (1 / bestBikeSafety)));

    issueStore.add(new Graphwide("Multiplying all walk safety values by " + (1 / bestWalkSafety)));
    HashSet<Edge> seenEdges = new HashSet<>();
    HashSet<AreaEdgeList> seenAreas = new HashSet<>();
    for (Vertex vertex : graph.getVertices()) {
      for (Edge e : vertex.getOutgoing()) {
        if (e instanceof AreaEdge) {
          AreaEdgeList areaEdgeList = ((AreaEdge) e).getArea();
          if (seenAreas.contains(areaEdgeList)) continue;
          seenAreas.add(areaEdgeList);
          for (NamedArea area : areaEdgeList.getAreas()) {
            area.setBicycleSafetyMultiplier(area.getBicycleSafetyMultiplier() / bestBikeSafety);
            area.setWalkSafetyMultiplier(area.getWalkSafetyMultiplier() / bestWalkSafety);
          }
        }
        applyFactors(seenEdges, e);
      }
      for (Edge e : vertex.getIncoming()) {
        applyFactors(seenEdges, e);
      }
    }
  }

  void applyWayProperties(
    StreetEdge street,
    StreetEdge backStreet,
    WayProperties wayData,
    OSMWithTags way
  ) {
    OsmTagMapper tagMapperForWay = way.getOsmProvider().getOsmTagMapper();

    Set<StreetNoteAndMatcher> notes = way.getOsmProvider().getWayPropertySet().getNoteForWay(way);

    boolean motorVehicleNoThrough = tagMapperForWay.isMotorVehicleThroughTrafficExplicitlyDisallowed(
      way
    );
    boolean bicycleNoThrough = tagMapperForWay.isBicycleNoThroughTrafficExplicitlyDisallowed(way);
    boolean walkNoThrough = tagMapperForWay.isWalkNoThroughTrafficExplicitlyDisallowed(way);

    if (street != null) {
      double bicycleSafety = wayData.bicycleSafety().forward();
      street.setBicycleSafetyFactor((float) bicycleSafety);
      if (bicycleSafety < bestBikeSafety) {
        bestBikeSafety = (float) bicycleSafety;
      }
      double walkSafety = wayData.walkSafety().forward();
      street.setWalkSafetyFactor((float) walkSafety);
      if (walkSafety < bestWalkSafety) {
        bestWalkSafety = (float) walkSafety;
      }
      if (notes != null) {
        for (var it : notes) {
          graph.streetNotesService.addStaticNote(street, it.note(), it.matcher());
        }
      }
      street.setMotorVehicleNoThruTraffic(motorVehicleNoThrough);
      street.setBicycleNoThruTraffic(bicycleNoThrough);
      street.setWalkNoThruTraffic(walkNoThrough);
    }

    if (backStreet != null) {
      double bicycleSafety = wayData.bicycleSafety().back();
      if (bicycleSafety < bestBikeSafety) {
        bestBikeSafety = (float) bicycleSafety;
      }
      backStreet.setBicycleSafetyFactor((float) bicycleSafety);
      double walkSafety = wayData.walkSafety().back();
      if (walkSafety < bestWalkSafety) {
        bestWalkSafety = (float) walkSafety;
      }
      backStreet.setWalkSafetyFactor((float) walkSafety);
      if (notes != null) {
        for (var it : notes) {
          graph.streetNotesService.addStaticNote(backStreet, it.note(), it.matcher());
        }
      }
      backStreet.setMotorVehicleNoThruTraffic(motorVehicleNoThrough);
      backStreet.setBicycleNoThruTraffic(bicycleNoThrough);
      backStreet.setWalkNoThruTraffic(walkNoThrough);
    }
  }

  private void applyFactors(HashSet<Edge> seenEdges, Edge e) {
    if (!(e instanceof StreetEdge pse)) {
      return;
    }

    if (!seenEdges.contains(e)) {
      seenEdges.add(e);
      pse.setBicycleSafetyFactor(pse.getBicycleSafetyFactor() / bestBikeSafety);
      pse.setWalkSafetyFactor(pse.getWalkSafetyFactor() / bestWalkSafety);
    }
  }
}
