package org.opentripplanner.graph_builder.module.osm.naming;

import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A namer that assigns names to crosswalks using the name of the crossed street, if available.
 * <p>
 * The algorithm works as follows:
 *  - For each crosswalk, we find the intersecting street edge that shares a node.
 *  - (To be completed)
 * <p>
 * In North America, it is common for a street to have turn lanes (slip lanes)
 * that are their own OSM ways because they are separate from the main streets they join.
 * The resulting crosswalk name is "Crossing over turn lane" or equivalent in other locales.
 */
public class CrosswalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(CrosswalkNamer.class);

  private Collection<OsmWay> streets = new ArrayList<>();
  private Collection<EdgeOnLevel> unnamedCrosswalks = new ArrayList<>();

  @Override
  public I18NString name(OsmEntity way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdges(OsmEntity way, StreetEdgePair pair) {
    if (way instanceof OsmWay osmWay) {
      // Record unnamed crossings to a list.
      if (
        osmWay.isFootway() &&
        osmWay.isMarkedCrossing() &&
        way.hasNoName() &&
        !way.isExplicitlyUnnamed()
      ) {
        pair
          .asIterable()
          .forEach(edge -> unnamedCrosswalks.add(new EdgeOnLevel(osmWay, edge, way.getLevels())));
      }
      // Record named streets, service roads, and slip/turn lanes to a list.
      else if (
        !osmWay.isFootway() &&
        (way.isNamed() || osmWay.isServiceRoad() || isTurnLane(osmWay))
      ) {
        streets.add(osmWay);
      }
    }
  }

  @Override
  public void postprocess() {
    ProgressTracker progress = ProgressTracker.track(
      "Assigning names to crosswalks",
      500,
      unnamedCrosswalks.size()
    );

    final AtomicInteger namesApplied = new AtomicInteger(0);
    unnamedCrosswalks
      .parallelStream()
      .forEach(crosswalkOnLevel -> {
        assignNameToCrosswalk(crosswalkOnLevel, namesApplied);

        // Keep lambda! A method-ref would cause incorrect class and line number to be logged
        // noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });

    LOG.info(
      "Assigned names to {} of {} of crosswalks ({}%)",
      namesApplied.get(),
      unnamedCrosswalks.size(),
      DoubleUtils.roundTo2Decimals(((double) namesApplied.get() / unnamedCrosswalks.size()) * 100)
    );

    LOG.info(progress.completeMessage());

    // Set the indices to null so they can be garbage-collected
    streets = null;
    unnamedCrosswalks = null;
  }

  /**
   * The actual worker method that runs the business logic on an individual sidewalk edge.
   */
  private void assignNameToCrosswalk(EdgeOnLevel crosswalkOnLevel, AtomicInteger namesApplied) {
    var crosswalk = crosswalkOnLevel.edge;
    var crossStreetOpt = getIntersectingStreet(crosswalkOnLevel.way, streets);

    if (crossStreetOpt.isPresent()) {
      OsmWay crossStreet = crossStreetOpt.get();
      // TODO: i18n
      if (crossStreet.isNamed()) {
        crosswalk.setName(I18NString.of(String.format("crossing over %s", crossStreet.getAssumedName())));
      } else if (crossStreet.isServiceRoad()) {
        crosswalk.setName(I18NString.of("crossing over service road"));
      } else if (isTurnLane(crossStreet)) {
        crosswalk.setName(I18NString.of("crossing over turn lane"));
      } else {
        // Default on using the OSM way ID, which should not happen.
        crosswalk.setName(I18NString.of(String.format("crossing %s", crosswalkOnLevel.way.getId())));
      }
      namesApplied.incrementAndGet();
    }
  }

  /** Gets the intersecting street, if any, for the given way and candidate streets. */
  public static Optional<OsmWay> getIntersectingStreet(OsmWay way, Collection<OsmWay> streets) {
    TLongList nodeRefs = way.getNodeRefs();
    if (nodeRefs.size() >= 3) {
      // There needs to be at least three nodes: 2 extremities that are on the sidewalk,
      // and one somewhere in the middle that joins the crossing with the street.
      // We exclude the first and last node which are on the sidewalk.
      long[] nodeRefsArray = nodeRefs.toArray(1, nodeRefs.size() - 2);
      return streets
        .stream()
        .filter(w -> Arrays.stream(nodeRefsArray).anyMatch(nid -> w.getNodeRefs().contains(nid)))
        .findFirst();
    }
    return Optional.empty();
  }

  private static boolean isTurnLane(OsmEntity way) {
    Optional<TraverseDirection> oneWayCar = way.isOneWay("motorcar");
    return oneWayCar.isPresent() && oneWayCar.get() == FORWARD;
  }

  public Collection<EdgeOnLevel> getUnnamedCrosswalks() {
    return unnamedCrosswalks;
  }

  public Collection<OsmWay> getStreets() {
    return streets;
  }

  public record EdgeOnLevel(OsmWay way, StreetEdge edge, Set<String> levels) {}
}
