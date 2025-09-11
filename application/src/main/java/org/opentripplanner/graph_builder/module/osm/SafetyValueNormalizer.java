package org.opentripplanner.graph_builder.module.osm;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects the minimum bike & walk safety values while applying them to the graph.
 * <p>
 * The heuristic function needs to know the minimum values so it never overestimates
 * the remaining cost of the path and won't prune out optimal paths.
 * <p>
 * Further reading: <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/4442">Issue 4442</a>
 * <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/6775">Issue 6775</a>
 */
class SafetyValueNormalizer {

  private static final Logger LOG = LoggerFactory.getLogger(SafetyValueNormalizer.class);

  private final Graph graph;

  /**
   * The bike safety factor of the safest street
   */
  private float bestBikeSafety = 1.0f;
  /**
   * The walk safety factor of the safest street
   */
  private float bestWalkSafety = 1.0f;

  SafetyValueNormalizer(Graph graph) {
    this.graph = graph;
  }

  public float getBestBikeSafety() {
    return bestBikeSafety;
  }

  public float getBestWalkSafety() {
    return bestWalkSafety;
  }

  void applyWayProperties(
    @Nullable StreetEdge street,
    @Nullable StreetEdge backStreet,
    WayProperties forwardWayData,
    WayProperties backwardWayData,
    OsmEntity way
  ) {
    OsmTagMapper tagMapperForWay = way.getOsmProvider().getOsmTagMapper();

    Set<StreetNoteAndMatcher> notes = way.getOsmProvider().getWayPropertySet().getNoteForWay(way);

    boolean motorVehicleNoThrough =
      tagMapperForWay.isMotorVehicleThroughTrafficExplicitlyDisallowed(way);
    boolean bicycleNoThrough = tagMapperForWay.isBicycleThroughTrafficExplicitlyDisallowed(way);
    boolean walkNoThrough = tagMapperForWay.isWalkThroughTrafficExplicitlyDisallowed(way);

    if (street != null) {
      float bicycleSafety = (float) forwardWayData.bicycleSafety();
      street.setBicycleSafetyFactor(bicycleSafety);
      if (bicycleSafety < bestBikeSafety) {
        bestBikeSafety = bicycleSafety;
        LOG.debug(
          "minimum bike safety reduced to {} for street {} forward",
          bestBikeSafety,
          street
        );
      }
      float walkSafety = (float) forwardWayData.walkSafety();
      street.setWalkSafetyFactor(walkSafety);
      if (walkSafety < bestWalkSafety) {
        bestWalkSafety = walkSafety;
        LOG.debug(
          "minimum walk safety reduced to {} for street {} forward",
          bestWalkSafety,
          street
        );
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
      float bicycleSafety = (float) backwardWayData.bicycleSafety();
      if (bicycleSafety < bestBikeSafety) {
        bestBikeSafety = bicycleSafety;
        LOG.debug(
          "minimum bike safety reduced to {} for street {} backward",
          bestBikeSafety,
          backStreet
        );
      }
      backStreet.setBicycleSafetyFactor(bicycleSafety);
      float walkSafety = (float) backwardWayData.walkSafety();
      if (walkSafety < bestWalkSafety) {
        bestWalkSafety = walkSafety;
        LOG.debug(
          "minimum walk safety reduced to {} for street {} backward",
          bestWalkSafety,
          backStreet
        );
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
}
