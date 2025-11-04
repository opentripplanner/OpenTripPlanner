package org.opentripplanner.graph_builder.module.osm.naming;

import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A namer that assigns names to crosswalks using the name or type of the crossed street.
 * <p>
 * The algorithm works as follows:
 *  - For each crosswalk, we find the intersecting street edge that shares a node.
 *  - Apply a name depending on the type of street:
 *      * For named streets, name the crossing so it reads "crosswalk over 10th Street".
 *      * For service roads (e.g. car access to commercial complexes, such as
 *        <a href="https://www.openstreetmap.org/way/1024601318">...</a>),
 *        use "crosswalk over service road".
 *      * For turn lanes or slip lanes at intersections (shortcuts from a street to another,
 *        to bypass traffic signals, prevalent in North America,
 *        e.g. <a href="https://www.openstreetmap.org/way/1139062913">...</a>),
 *        use "crosswalk over turn lane".
 */
public class CrosswalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(CrosswalkNamer.class);
  private static final int BUFFER_METERS = 25;
  private final BufferedEdgeProcessor processor; //=

  private StreetEdgeIndex streetIndex = new StreetEdgeIndex();
  private StreetEdgeIndex sidewalkIndex = new StreetEdgeIndex();
  private Collection<EdgeOnLevel> unnamedCrosswalks = new ArrayList<>();

  public CrosswalkNamer() {
    processor = new BufferedEdgeProcessor(BUFFER_METERS, "crosswalks", LOG, this::assignNameToEdge);
  }

  @Override
  public I18NString name(OsmEntity entity) {
    return entity.getAssumedName();
  }

  @Override
  public void recordEdges(OsmWay way, StreetEdgePair pair, OsmDatabase osmdb) {
    // Record unnamed crossings to a list.
    if (way.isCrossing() && way.hasNoName() && !way.isExplicitlyUnnamed()) {
      pair
        .asIterable()
        .forEach(edge -> unnamedCrosswalks.add(new EdgeOnLevel(way, edge, Set.of())));
    }
    // Record (short) sidewalks to a geometric index
    else if (way.isSidewalk()) {
      sidewalkIndex.add(way, pair, Set.of(), BUFFER_METERS);
    }
    // Record named streets, service roads, and slip/turn lanes to a geometric index.
    else if (!way.isFootway() && (way.isNamed() || way.isServiceRoad() || way.isTurnLane())) {
      streetIndex.add(way, pair, Set.of());
    }
  }

  @Override
  public void finalizeNames() {
    processor.applyNames(unnamedCrosswalks);

    // Set the indices to null so they can be garbage-collected
    streetIndex = null;
    sidewalkIndex = null;
    unnamedCrosswalks = null;
  }

  /**
   * The actual logic for naming individual crosswalk edges.
   * This will also name adjacent sidewalks on each end if they are the only adjacent sidewalks to a crosswalk.
   */
  public boolean assignNameToEdge(EdgeOnLevel crosswalkOnLevel, Geometry buffer) {
    var crosswalk = crosswalkOnLevel.edge();
    OsmWay way = crosswalkOnLevel.way();

    var streetCandidates = streetIndex.query(buffer).stream().map(EdgeOnLevel::way).toList();

    var crossStreetOpt = getIntersectingStreet(way, streetCandidates);
    if (crossStreetOpt.isPresent()) {
      OsmWay crossStreet = crossStreetOpt.get();
      if (crossStreet.isNamed()) {
        crosswalk.setName(
          new LocalizedString("name.crosswalk_over_street", crossStreet.getAssumedName())
        );
      } else if (crossStreet.isServiceRoad()) {
        crosswalk.setName(new LocalizedString("name.crosswalk_over_service_road"));
      } else if (crossStreet.isMotorwayRamp()) {
        crosswalk.setName(new LocalizedString("name.crosswalk_over_motorway_ramp"));
      } else if (crossStreet.isTurnLane()) {
        crosswalk.setName(new LocalizedString("name.crosswalk_over_turn_lane"));
      } else {
        // Default on using the OSM way ID, which should not happen.
        crosswalk.setName(I18NString.of(String.format("crosswalk %s", way.getId())));
      }

      var adjacentSidewalks = sidewalkIndex
        .query(buffer)
        .stream()
        .filter(e -> e.way().isAdjacentTo(way))
        .filter(e -> e.edge().nameIsDerived())
        .toList();

      // Group sidewalks at each end of the crosswalk.
      TLongList nodes = way.getNodeRefs();
      renameAdjacentSidewalk(adjacentSidewalks, crosswalk.getName(), nodes.get(0));
      renameAdjacentSidewalk(adjacentSidewalks, crosswalk.getName(), nodes.get(nodes.size() - 1));

      return true;
    }

    return false;
  }

  /**
   * Rename a sidewalk, among candidates, if it is the only adjacent sidewalk to the given crosswalk.
   */
  private void renameAdjacentSidewalk(
    List<EdgeOnLevel> adjacentSidewalks,
    I18NString crosswalkName,
    long nodeId
  ) {
    List<EdgeOnLevel> sidewalks = adjacentSidewalks
      .stream()
      .filter(e -> e.way().getNodeRefs().contains(nodeId))
      .toList();
    if (sidewalks.size() == 1) {
      sidewalks.getFirst().edge().setName(crosswalkName);
    }
  }

  /**
   * Gets the intersecting street, if any, for the given way and candidate streets.
   */
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

  Collection<EdgeOnLevel> getUnnamedCrosswalks() {
    return unnamedCrosswalks;
  }
}
