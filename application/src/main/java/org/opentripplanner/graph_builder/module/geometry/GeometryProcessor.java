package org.opentripplanner.graph_builder.module.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BogusShapeDistanceTraveled;
import org.opentripplanner.graph_builder.issues.BogusShapeGeometry;
import org.opentripplanner.graph_builder.issues.BogusShapeGeometryCaught;
import org.opentripplanner.graph_builder.issues.MissingShapeGeometry;
import org.opentripplanner.graph_builder.issues.ShapeGeometryTooFar;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module creates hop geometries from GTFS shapes.
 *
 * <p>
 * THREAD SAFETY The computation runs in parallel so be careful about thread safety when modifying
 * the logic here.
 */
public class GeometryProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(GeometryProcessor.class);
  private static final GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
  private final OtpTransitServiceBuilder transitService;
  // this is a thread-safe implementation
  private final Map<ShapeSegmentKey, LineString> geometriesByShapeSegmentKey =
    new ConcurrentHashMap<>();
  // this is a thread-safe implementation
  private final Map<FeedScopedId, LineString> geometriesByShapeId = new ConcurrentHashMap<>();
  // this is a thread-safe implementation
  private final Map<FeedScopedId, double[]> distancesByShapeId = new ConcurrentHashMap<>();
  private final double maxStopToShapeSnapDistance;
  private final DataImportIssueStore issueStore;

  public GeometryProcessor(
    // TODO OTP2 - Operate on the builder, not the transit service and move the execution of
    //           - this to where the builder is in context.
    OtpTransitServiceBuilder transitService,
    double maxStopToShapeSnapDistance,
    DataImportIssueStore issueStore
  ) {
    this.transitService = transitService;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance > 0
      ? maxStopToShapeSnapDistance
      : 150;
    this.issueStore = issueStore;
  }

  /**
   * Generate the geometry for the trip. Assumes that there are already vertices in the graph for
   * the stops.
   * <p>
   * THREAD SAFETY The geometries for the trip patterns are computed in parallel. The collections
   * needed for this are concurrent implementations and therefore threadsafe but the issue store,
   * the graph, the OtpTransitService and others are not.
   */
  public List<LineString> createHopGeometries(Trip trip) {
    if (
      trip.getShapeId() == null ||
      trip.getShapeId().getId() == null ||
      trip.getShapeId().getId().isEmpty()
    ) {
      return null;
    }

    return Arrays.asList(
      createGeometry(trip.getShapeId(), transitService.getStopTimesSortedByTrip().get(trip))
    );
  }

  private static boolean equals(LinearLocation startIndex, LinearLocation endIndex) {
    return (
      startIndex.getSegmentIndex() == endIndex.getSegmentIndex() &&
      startIndex.getSegmentFraction() == endIndex.getSegmentFraction() &&
      startIndex.getComponentIndex() == endIndex.getComponentIndex()
    );
  }

  /**
   * Creates a set of geometries for a single trip, considering the GTFS shapes.txt, The geometry is
   * broken down into one geometry per inter-stop segment ("hop"). We also need a shape for the
   * entire trip and tripPattern, but given the complexity of the existing code for generating hop
   * geometries, we will create the full-trip geometry by simply concatenating the hop geometries.
   * <p>
   * This geometry will in fact be used for an entire set of trips in a trip pattern. Technically
   * one of the trips with exactly the same sequence of stops could follow a different route on the
   * streets, but that's very uncommon.
   */
  private LineString[] createGeometry(FeedScopedId shapeId, List<StopTime> stopTimes) {
    if (hasShapeDist(shapeId, stopTimes)) {
      // this trip has shape_dist in stop_times
      LineString[] geometries = getHopGeometriesViaShapeDistTravelled(stopTimes, shapeId);
      if (geometries != null) {
        return geometries;
      }
      // else proceed to method below which uses shape without distance information
    }

    LineString shapeLineString = getLineStringForShapeId(shapeId);
    if (shapeLineString == null) {
      // this trip has a shape_id, but no such shape exists, and no shape_dist in stop_times
      // create straight line segments between stops for each hop
      issueStore.add(new MissingShapeGeometry(stopTimes.get(0).getTrip().getId(), shapeId));
      return createStraightLineHopGeometries(stopTimes);
    }

    List<LinearLocation> locations = getLinearLocations(stopTimes, shapeLineString);
    if (locations == null) {
      // this only happens on shape which have points very far from
      // their stop sequence. So we'll fall back to trivial stop-to-stop
      // linking, even though theoretically we could do better.
      issueStore.add(new ShapeGeometryTooFar(stopTimes.get(0).getTrip().getId(), shapeId));
      return createStraightLineHopGeometries(stopTimes);
    }

    return getGeometriesByShape(stopTimes, shapeId, shapeLineString, locations);
  }

  private boolean hasShapeDist(FeedScopedId shapeId, List<StopTime> stopTimes) {
    StopTime st0 = stopTimes.get(0);
    return st0.isShapeDistTraveledSet() && getDistanceForShapeId(shapeId) != null;
  }

  private LineString[] getGeometriesByShape(
    List<StopTime> stopTimes,
    FeedScopedId shapeId,
    LineString shape,
    List<LinearLocation> locations
  ) {
    LineString[] geoms = new LineString[stopTimes.size() - 1];
    Iterator<LinearLocation> locationIt = locations.iterator();
    LinearLocation endLocation = locationIt.next();
    double distanceSoFar = 0;
    int last = 0;
    for (int i = 0; i < stopTimes.size() - 1; ++i) {
      LinearLocation startLocation = endLocation;
      endLocation = locationIt.next();

      //convert from LinearLocation to distance
      //advance distanceSoFar up to start of segment containing startLocation;
      //it does not matter at all if this is accurate so long as it is consistent
      for (int j = last; j < startLocation.getSegmentIndex(); ++j) {
        Coordinate from = shape.getCoordinateN(j);
        Coordinate to = shape.getCoordinateN(j + 1);
        double xd = from.x - to.x;
        double yd = from.y - to.y;
        distanceSoFar += Math.sqrt(xd * xd + yd * yd);
      }
      last = startLocation.getSegmentIndex();

      double startIndex =
        distanceSoFar + startLocation.getSegmentFraction() * startLocation.getSegmentLength(shape);
      //advance distanceSoFar up to start of segment containing endLocation
      for (int j = last; j < endLocation.getSegmentIndex(); ++j) {
        Coordinate from = shape.getCoordinateN(j);
        Coordinate to = shape.getCoordinateN(j + 1);
        double xd = from.x - to.x;
        double yd = from.y - to.y;
        distanceSoFar += Math.sqrt(xd * xd + yd * yd);
      }
      last = startLocation.getSegmentIndex();
      double endIndex =
        distanceSoFar + endLocation.getSegmentFraction() * endLocation.getSegmentLength(shape);

      ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startIndex, endIndex);
      LineString geometry = geometriesByShapeSegmentKey.get(key);

      if (geometry == null) {
        LocationIndexedLine locationIndexed = new LocationIndexedLine(shape);
        geometry = (LineString) locationIndexed.extractLine(startLocation, endLocation);

        // Pack the resulting line string
        CoordinateSequence sequence = new PackedCoordinateSequence.Double(
          geometry.getCoordinates(),
          2
        );
        geometry = geometryFactory.createLineString(sequence);
      }
      geoms[i] = geometry;
    }
    return geoms;
  }

  private List<LinearLocation> getLinearLocations(List<StopTime> stopTimes, LineString shape) {
    var isFlexTrip = FlexTrip.containsFlexStops(stopTimes);
    // This trip does not have shape_dist in stop_times, but does have an associated shape.
    ArrayList<IndexedLineSegment> segments = new ArrayList<>();
    for (int i = 0; i < shape.getNumPoints() - 1; ++i) {
      segments.add(new IndexedLineSegment(i, shape.getCoordinateN(i), shape.getCoordinateN(i + 1)));
    }
    // Find possible segment matches for each stop.
    List<List<IndexedLineSegment>> possibleSegmentsForStop = new ArrayList<>();
    int minSegmentIndex = 0;
    for (int i = 0; i < stopTimes.size(); ++i) {
      StopLocation stop = stopTimes.get(i).getStop();
      Coordinate coord = stop.getCoordinate().asJtsCoordinate();
      List<IndexedLineSegment> stopSegments = new ArrayList<>();
      double bestDistance = Double.MAX_VALUE;
      IndexedLineSegment bestSegment = null;
      int maxSegmentIndex = -1;
      int index = -1;
      int minSegmentIndexForThisStop = -1;
      for (IndexedLineSegment segment : segments) {
        index++;
        if (segment.index < minSegmentIndex) {
          continue;
        }
        double distance = segment.distance(coord);
        if (distance < maxStopToShapeSnapDistance || isFlexTrip) {
          stopSegments.add(segment);
          maxSegmentIndex = index;
          if (minSegmentIndexForThisStop == -1) minSegmentIndexForThisStop = index;
        } else if (distance < bestDistance) {
          bestDistance = distance;
          bestSegment = segment;
          if (maxSegmentIndex != -1) {
            maxSegmentIndex = index;
          }
        }
      }
      if (stopSegments.size() == 0 && bestSegment != null) {
        //no segments within 150m
        //fall back to nearest segment
        stopSegments.add(bestSegment);
        minSegmentIndex = bestSegment.index;
      } else {
        minSegmentIndex = minSegmentIndexForThisStop;
      }

      for (int j = i - 1; j >= 0; j--) {
        for (
          Iterator<IndexedLineSegment> it = possibleSegmentsForStop.get(j).iterator();
          it.hasNext();
        ) {
          IndexedLineSegment segment = it.next();
          if (segment.index > maxSegmentIndex) {
            it.remove();
          }
        }
      }
      possibleSegmentsForStop.add(stopSegments);
    }

    return getStopLocations(possibleSegmentsForStop, stopTimes);
  }

  private LineString[] createStraightLineHopGeometries(List<StopTime> stopTimes) {
    LineString[] geoms = new LineString[stopTimes.size() - 1];
    StopTime st0;
    for (int i = 0; i < stopTimes.size() - 1; ++i) {
      st0 = stopTimes.get(i);
      StopTime st1 = stopTimes.get(i + 1);
      LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
      geoms[i] = geometry;
    }
    return geoms;
  }

  private LineString[] getHopGeometriesViaShapeDistTravelled(
    List<StopTime> stopTimes,
    FeedScopedId shapeId
  ) {
    LineString[] geoms = new LineString[stopTimes.size() - 1];
    StopTime st0;
    for (int i = 0; i < stopTimes.size() - 1; ++i) {
      st0 = stopTimes.get(i);
      StopTime st1 = stopTimes.get(i + 1);
      geoms[i] = getHopGeometryViaShapeDistTraveled(shapeId, st0, st1);
      if (geoms[i] == null) {
        return null;
      }
    }
    return geoms;
  }

  /**
   * Find a consistent, increasing list of LinearLocations along a shape for a set of stops. Handles
   * loops routes.
   */
  private List<LinearLocation> getStopLocations(
    List<List<IndexedLineSegment>> possibleSegmentsForStop,
    List<StopTime> stopTimes
  ) {
    var prevSegmentIndex = Integer.MIN_VALUE;
    List<LinearLocation> locations = new ArrayList<>(stopTimes.size());
    for (
      var stopPositionInPattern = 0;
      stopPositionInPattern < stopTimes.size();
      ++stopPositionInPattern
    ) {
      StopTime st = stopTimes.get(stopPositionInPattern);
      StopLocation stop = st.getStop();
      Coordinate stopCoord = stop.getCoordinate().asJtsCoordinate();

      // Arrange segments into list of continuous segments
      // we assume that the first time a shape passes through within 150 m of the stop, it will match
      // the stop. Therefore, we choose the best segment within the first list of continuous
      // segments, rather than trying from the best segment globally.
      // This is to avoid exponential complexity for routes with multiple double-backs with multiple
      // stops within the double-backs.
      //
      // An exception is that, if the discontinuity appears before the minimum possible segment
      // for the next stop, the discontinuity is joined together. This is avoid a simple edge case
      // of a bus first passing within 150 m of a stop, exit the 150 m radius to a turning circle,
      // then call at the stop at the opposite side of the road.
      List<List<IndexedLineSegment>> continuousSegments = new LinkedList<>();
      for (IndexedLineSegment segment : possibleSegmentsForStop.get(stopPositionInPattern)) {
        //can't go backwards along line
        if (segment.index >= prevSegmentIndex) {
          boolean shouldStartNewSegment;
          if (continuousSegments.isEmpty()) {
            shouldStartNewSegment = true;
          } else if (stopPositionInPattern + 1 == stopTimes.size()) {
            shouldStartNewSegment = false;
          } else {
            var lastSegment = continuousSegments.getLast().getLast();
            var segmentsForNextStop = possibleSegmentsForStop.get(stopPositionInPattern + 1);
            shouldStartNewSegment = segmentsForNextStop
              .stream()
              .anyMatch(item -> item.index > lastSegment.index && item.index < segment.index);
          }
          if (shouldStartNewSegment) {
            // start a new continuous segment
            continuousSegments.add(new LinkedList<>());
          }
          continuousSegments.getLast().add(segment);
        }
      }
      // choose the best match from the first list
      if (continuousSegments.isEmpty()) {
        return null;
      }
      List<IndexedLineSegment> firstContinuousSegments = continuousSegments.getFirst();
      var bestMatch = firstContinuousSegments.getFirst();
      for (var segment : firstContinuousSegments) {
        if (segment.distance(stopCoord) < bestMatch.distance(stopCoord)) {
          bestMatch = segment;
        }
      }
      // we found one!
      LinearLocation location = new LinearLocation(
        0,
        bestMatch.index,
        bestMatch.fraction(stopCoord)
      );
      locations.add(location);
      prevSegmentIndex = bestMatch.index;
    }
    return locations;
  }

  private LineString getHopGeometryViaShapeDistTraveled(
    FeedScopedId shapeId,
    StopTime st0,
    StopTime st1
  ) {
    double startDistance = st0.getShapeDistTraveled();
    double endDistance = st1.getShapeDistTraveled();

    ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
    LineString geometry = geometriesByShapeSegmentKey.get(key);
    if (geometry != null) {
      return geometry;
    }

    double[] distances = getDistanceForShapeId(shapeId);

    if (distances == null) {
      issueStore.add(new BogusShapeGeometry(shapeId));
      return null;
    } else {
      LinearLocation startIndex = getSegmentFraction(distances, startDistance);
      LinearLocation endIndex = getSegmentFraction(distances, endDistance);

      if (equals(startIndex, endIndex)) {
        //bogus shape_dist_traveled
        issueStore.add(new BogusShapeDistanceTraveled(st1));
        // return null to indicate failure. Another approach which does not need shape_dist_traveled will be used.
        return null;
      }
      LineString line = getLineStringForShapeId(shapeId);
      LocationIndexedLine lol = new LocationIndexedLine(line);

      geometry = getSegmentGeometry(
        shapeId,
        lol,
        startIndex,
        endIndex,
        startDistance,
        endDistance,
        st0,
        st1
      );

      return geometry;
    }
  }

  /** create a 2-point linestring (a straight line segment) between the two stops */
  private LineString createSimpleGeometry(StopLocation s0, StopLocation s1) {
    Coordinate[] coordinates = new Coordinate[] {
      s0.getCoordinate().asJtsCoordinate(),
      s1.getCoordinate().asJtsCoordinate(),
    };
    CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);

    return geometryFactory.createLineString(sequence);
  }

  private boolean isValid(Geometry geometry, StopLocation s0, StopLocation s1) {
    Coordinate[] coordinates = geometry.getCoordinates();
    if (coordinates.length < 2) {
      return false;
    }
    if (geometry.getLength() == 0) {
      return false;
    }
    for (Coordinate coordinate : coordinates) {
      if (Double.isNaN(coordinate.x) || Double.isNaN(coordinate.y)) {
        return false;
      }
    }
    Coordinate geometryStartCoord = coordinates[0];
    Coordinate geometryEndCoord = coordinates[coordinates.length - 1];

    Coordinate startCoord = s0.getCoordinate().asJtsCoordinate();
    Coordinate endCoord = s1.getCoordinate().asJtsCoordinate();
    if (
      SphericalDistanceLibrary.fastDistance(startCoord, geometryStartCoord) >
      maxStopToShapeSnapDistance
    ) {
      return false;
    } else if (
      SphericalDistanceLibrary.fastDistance(endCoord, geometryEndCoord) > maxStopToShapeSnapDistance
    ) {
      return false;
    }
    return true;
  }

  private LineString getSegmentGeometry(
    FeedScopedId shapeId,
    LocationIndexedLine locationIndexedLine,
    LinearLocation startIndex,
    LinearLocation endIndex,
    double startDistance,
    double endDistance,
    StopTime st0,
    StopTime st1
  ) {
    ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);

    LineString geometry = geometriesByShapeSegmentKey.get(key);
    if (geometry == null) {
      geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex);

      // Pack the resulting line string
      CoordinateSequence sequence = new PackedCoordinateSequence.Double(
        geometry.getCoordinates(),
        2
      );
      geometry = geometryFactory.createLineString(sequence);

      if (!isValid(geometry, st0.getStop(), st1.getStop())) {
        issueStore.add(new BogusShapeGeometryCaught(shapeId, st0, st1));
        return null;
      }
      geometriesByShapeSegmentKey.put(key, geometry);
    }

    return geometry;
  }

  /**
   * If a shape appears in more than one feed, the shape points will be loaded several times, and
   * there will be duplicates in the DAO. Filter out duplicates and repeated coordinates because 1)
   * they are unnecessary, and 2) they define 0-length line segments which cause JTS location
   * indexed line to return a segment location of NaN, which we do not want.
   */
  private List<ShapePoint> getUniqueShapePointsForShapeId(FeedScopedId shapeId) {
    List<ShapePoint> points = new ArrayList<>(transitService.getShapePoints().get(shapeId));
    Collections.sort(points);
    ArrayList<ShapePoint> filtered = new ArrayList<>(points.size());
    ShapePoint last = null;
    for (ShapePoint sp : points) {
      if (last == null || last.getSequence() != sp.getSequence()) {
        if (last != null && last.getLat() == sp.getLat() && last.getLon() == sp.getLon()) {
          LOG.trace("pair of identical shape points (skipping): {} {}", last, sp);
        } else {
          filtered.add(sp);
        }
      }
      last = sp;
    }
    if (filtered.size() != points.size()) {
      filtered.trimToSize();
      return filtered;
    } else {
      return points;
    }
  }

  private LineString getLineStringForShapeId(FeedScopedId shapeId) {
    LineString geometry = geometriesByShapeId.get(shapeId);

    if (geometry != null) {
      return geometry;
    }

    List<ShapePoint> points = getUniqueShapePointsForShapeId(shapeId);
    if (points.size() < 2) {
      return null;
    }
    Coordinate[] coordinates = new Coordinate[points.size()];
    double[] distances = new double[points.size()];

    boolean hasAllDistances = true;

    int i = 0;
    for (ShapePoint point : points) {
      coordinates[i] = new Coordinate(point.getLon(), point.getLat());
      distances[i] = point.getDistTraveled();
      if (!point.isDistTraveledSet()) hasAllDistances = false;
      i++;
    }

    CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);
    geometry = geometryFactory.createLineString(sequence);
    geometriesByShapeId.put(shapeId, geometry);

    // If we don't have distances here, we can't calculate them ourselves because we can't
    // assume the units will match
    if (hasAllDistances) {
      distancesByShapeId.put(shapeId, distances);
    }

    return geometry;
  }

  private double[] getDistanceForShapeId(FeedScopedId shapeId) {
    getLineStringForShapeId(shapeId);
    return distancesByShapeId.get(shapeId);
  }

  private LinearLocation getSegmentFraction(double[] distances, double distance) {
    int index = Arrays.binarySearch(distances, distance);
    if (index < 0) {
      index = -(index + 1);
    }
    if (index == 0) {
      return new LinearLocation(0, 0.0);
    }
    if (index == distances.length) {
      return new LinearLocation(distances.length, 0.0);
    }

    double prevDistance = distances[index - 1];
    if (prevDistance == distances[index]) {
      return new LinearLocation(index - 1, 1.0);
    }
    double indexPart = (distance - distances[index - 1]) / (distances[index] - prevDistance);
    return new LinearLocation(index - 1, indexPart);
  }
}
