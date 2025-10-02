package org.opentripplanner.graph_builder.module.osm.naming;

import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
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
 * A namer that assigns names to crosswalks using the name or type of the crossed street.
 * <p>
 * The algorithm works as follows:
 *  - For each crosswalk, we find the intersecting street edge that shares a node.
 *  - Apply a name depending on the type of street:
 *      * For named streets, name the crossing so it reads "crossing over 10th Street".
 *      * For service roads (e.g. car access to commercial complexes, such as
 *        <a href="https://www.openstreetmap.org/way/1024601318">...</a>),
 *        use "crossing over service road".
 *      * For turn lanes or slip lanes at intersections (shortcuts from a street to another,
 *        to bypass traffic signals, prevalent in North America,
 *        e.g. <a href="https://www.openstreetmap.org/way/1139062913">...</a>),
 *        use "crossing over turn lane".
 */
public class CrosswalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(CrosswalkNamer.class);
  private static final int BUFFER_METERS = 25;

  private HashGridSpatialIndex<EdgeOnLevel> streetEdges = new HashGridSpatialIndex<>();
  private HashGridSpatialIndex<EdgeOnLevel> sidewalkEdges = new HashGridSpatialIndex<>();
  private Collection<EdgeOnLevel> unnamedCrosswalks = new ArrayList<>();
  private PreciseBuffer preciseBuffer;

  @Override
  public I18NString name(OsmEntity way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdges(OsmEntity way, StreetEdgePair pair) {
    if (way instanceof OsmWay osmWay) {
      // Record unnamed crossings to a list.
      if (osmWay.isCrossing() && way.hasNoName() && !way.isExplicitlyUnnamed()) {
        pair
          .asIterable()
          .forEach(edge -> unnamedCrosswalks.add(new EdgeOnLevel(osmWay, edge, way.getLevels())));
      }
      // Record (short) sidewalks to a geometric index
      else if (way.isSidewalk()) {
        // We generate two edges for each osm way: one there and one back. This spatial index only
        // needs to contain one item for each road segment with a unique geometry and name, so we
        // add only one of the two edges.
        var edge = pair.pickAny();
        if (edge.getDistanceMeters() <= BUFFER_METERS) {
          sidewalkEdges.insert(
            edge.getGeometry().getEnvelopeInternal(),
            new EdgeOnLevel(osmWay, edge, way.getLevels())
          );
        }
      }
      // Record named streets, service roads, and slip/turn lanes to a geometric index.
      else if (
        !osmWay.isFootway() && (way.isNamed() || osmWay.isServiceRoad() || isTurnLane(osmWay))
      ) {
        // We generate two edges for each osm way: one there and one back. This spatial index only
        // needs to contain one item for each road segment with a unique geometry and name, so we
        // add only one of the two edges.
        var edge = pair.pickAny();
        streetEdges.insert(
          edge.getGeometry().getEnvelopeInternal(),
          new EdgeOnLevel(osmWay, edge, way.getLevels())
        );
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

    this.preciseBuffer = new PreciseBuffer(computeEnvelopeCenter(), BUFFER_METERS);

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
    streetEdges = null;
    sidewalkEdges = null;
    unnamedCrosswalks = null;
  }

  /**
   * The actual worker method that runs the business logic on an individual sidewalk edge.
   * This will also name adjacent sidewalks on each end if they are the only adjacent sidewalks.
   */
  private void assignNameToCrosswalk(EdgeOnLevel crosswalkOnLevel, AtomicInteger namesApplied) {
    var crosswalk = crosswalkOnLevel.edge;
    var buffer = preciseBuffer.preciseBuffer(crosswalk.getGeometry());
    OsmWay way = crosswalkOnLevel.way;

    var streetCandidates = streetEdges
      .query(buffer.getEnvelopeInternal())
      .stream()
      .map(e -> e.way)
      .toList();

    var crossStreetOpt = getIntersectingStreet(way, streetCandidates);
    if (crossStreetOpt.isPresent()) {
      OsmWay crossStreet = crossStreetOpt.get();
      // TODO: i18n
      if (crossStreet.isNamed()) {
        crosswalk.setName(
          I18NString.of(String.format("crossing over %s", crossStreet.getAssumedName()))
        );
      } else if (crossStreet.isServiceRoad()) {
        crosswalk.setName(I18NString.of("crossing over service road"));
      } else if (isTurnLane(crossStreet)) {
        crosswalk.setName(I18NString.of("crossing over turn lane"));
      } else {
        // Default on using the OSM way ID, which should not happen.
        crosswalk.setName(I18NString.of(String.format("crossing %s", way.getId())));
      }
      namesApplied.incrementAndGet();

      var adjacentSidewalks = sidewalkEdges
        .query(buffer.getEnvelopeInternal())
        .stream()
        .filter(e -> e.way().isAdjacentTo(way))
        .filter(e -> e.edge.nameIsDerived())
        .toList();

      // Group sidewalks at each end of the crosswalk.
      TLongList nodes = way.getNodeRefs();
      renameAdjacentSidewalk(adjacentSidewalks, crosswalk.getName(), nodes.get(0));
      renameAdjacentSidewalk(adjacentSidewalks, crosswalk.getName(), nodes.get(nodes.size() - 1));
    }
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
      .filter(e -> e.way.getNodeRefs().contains(nodeId))
      .toList();
    if (sidewalks.size() == 1) {
      sidewalks.getFirst().edge.setName(crosswalkName);
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

  private static boolean isTurnLane(OsmEntity way) {
    Optional<TraverseDirection> oneWayCar = way.isOneWay("motorcar");
    return oneWayCar.isPresent() && oneWayCar.get() == FORWARD;
  }

  public Collection<EdgeOnLevel> getUnnamedCrosswalks() {
    return unnamedCrosswalks;
  }

  /**
   * Compute the centroid of all sidewalk edges.
   */
  private Coordinate computeEnvelopeCenter() {
    var envelope = new Envelope();
    unnamedCrosswalks.forEach(e -> {
      envelope.expandToInclude(e.edge.getFromVertex().getCoordinate());
      envelope.expandToInclude(e.edge.getToVertex().getCoordinate());
    });
    return envelope.centre();
  }

  public record EdgeOnLevel(OsmWay way, StreetEdge edge, Set<String> levels) {}

  /**
   * A class to cache the expensive construction of a Universal Traverse Mercator coordinate
   * reference system.
   * Re-using the same CRS for all edges might introduce tiny imprecisions for OTPs use cases
   * but speeds up the processing enormously and is a price well worth paying.
   */
  private static final class PreciseBuffer {

    private final double distanceInMeters;
    private final MathTransform toTransform;
    private final MathTransform fromTransform;

    private PreciseBuffer(Coordinate coordinate, double distanceInMeters) {
      this.distanceInMeters = distanceInMeters;
      String code = "AUTO:42001,%s,%s".formatted(coordinate.x, coordinate.y);
      try {
        CoordinateReferenceSystem auto = CRS.decode(code);
        this.toTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
        this.fromTransform = CRS.findMathTransform(auto, DefaultGeographicCRS.WGS84);
      } catch (FactoryException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Add a buffer around a geometry that makes sure that the buffer is the same distance (in
     * meters) anywhere on earth.
     * <p>
     * Background: If you call the regular buffer() method on a JTS geometry that uses WGS84 as the
     * coordinate reference system, the buffer will be accurate at the equator but will become more
     * and more elongated the farther north/south you go.
     * <p>
     * Taken from https://stackoverflow.com/questions/36455020
     */
    private Geometry preciseBuffer(Geometry geometry) {
      try {
        Geometry pGeom = JTS.transform(geometry, toTransform);
        Geometry pBufferedGeom = pGeom.buffer(distanceInMeters, 4, BufferParameters.CAP_FLAT);
        return JTS.transform(pBufferedGeom, fromTransform);
      } catch (TransformException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
