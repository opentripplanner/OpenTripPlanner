package org.opentripplanner.ext.edgenaming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A namer that assigns names of nearby streets to sidewalks if they meet certain
 * geometric similarity criteria.
 * <p>
 * The algorithm works as follows:
 *  - for each sidewalk we look up (named) street edges nearby
 *  - group those edges into groups where each edge has the same name
 *  - draw a flat-capped buffer around the sidewalk, like this: https://tinyurl.com/4fpe882h
 *  - check how much of a named edge group is inside the buffer
 *  - remove those groups which are below MIN_PERCENT_IN_BUFFER
 *  - take the group that has the highest percentage (as a proportion of the sidewalk length) inside
 *    the buffer and apply its name to the sidewalk.
 * <p>
 * This works very well for OSM data where the sidewalk runs a parallel to the street and at each
 * intersection the sidewalk is also split. It doesn't work well for sidewalks that go around
 * the corner, like https://www.openstreetmap.org/way/1059101564. These cases are, however, detected
 * by the above algorithm and the sidewalk name remains the same.
 */
class SidewalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(SidewalkNamer.class);
  private static final double MIN_PERCENT_IN_BUFFER = .85;
  private static final int BUFFER_METERS = 25;

  private final BufferedEdgeProcessor processor;
  private StreetEdgeIndex streetIndex = new StreetEdgeIndex();
  private Collection<EdgeOnLevel> unnamedSidewalks = new ArrayList<>();

  public SidewalkNamer() {
    processor = new BufferedEdgeProcessor(BUFFER_METERS, "sidewalks", LOG, this::assignNameToEdge);
  }

  @Override
  public I18NString name(OsmEntity entity) {
    return entity.getAssumedName();
  }

  @Override
  public void recordEdges(OsmWay way, StreetEdgePair pair, OsmDatabase osmdb) {
    Set<OsmLevel> levelSet = osmdb.getLevelSetForEntity(way);
    // This way is a sidewalk and hasn't been named yet (and is not explicitly unnamed)
    if (
      way instanceof OsmWay osmWay &&
      way.isSidewalk() &&
      way.hasNoName() &&
      !way.isExplicitlyUnnamed()
    ) {
      pair
        .asIterable()
        .forEach(edge -> unnamedSidewalks.add(new EdgeOnLevel(osmWay, edge, levelSet)));
    }
    // The way is _not_ a sidewalk and does have a name
    else if (way.isNamed() && !way.isLink()) {
      streetIndex.add(way, pair, levelSet);
    }
  }

  @Override
  public void finalizeNames() {
    processor.applyNames(unnamedSidewalks);

    // Set the indices to null so they can be garbage-collected
    streetIndex = null;
    unnamedSidewalks = null;
  }

  /**
   * The actual logic for naming individual sidewalk edges.
   */
  public boolean assignNameToEdge(EdgeOnLevel sidewalkOnLevel, Geometry buffer) {
    var sidewalk = sidewalkOnLevel.edge();
    var sidewalkLength = SphericalDistanceLibrary.length(sidewalk.getGeometry());

    var candidates = streetIndex.query(buffer);

    AtomicBoolean result = new AtomicBoolean(false);

    groupEdgesByName(candidates)
      // Make sure we only compare sidewalks and streets that are on the same level
      .filter(g -> g.levels.equals(sidewalkOnLevel.levels()))
      .map(g -> computePercentInsideBuffer(g, buffer, sidewalkLength))
      // Remove those groups where less than a certain percentage is inside the buffer around
      // the sidewalk. This is a safety mechanism for sidewalks that snake around the corner,
      // like https://www.openstreetmap.org/way/1059101564.
      .filter(group -> group.percentInBuffer > MIN_PERCENT_IN_BUFFER)
      .max(Comparator.comparingDouble(NamedEdgeGroup::percentInBuffer))
      .ifPresent(group -> {
        result.set(true);
        sidewalk.setName(Objects.requireNonNull(group.name));
      });

    return result.get();
  }

  /**
   * Compute the length of the group that is inside the buffer and return it as a percentage
   * of the length of the sidewalk.
   */
  private static NamedEdgeGroup computePercentInsideBuffer(
    CandidateGroup g,
    Geometry buffer,
    double sidewalkLength
  ) {
    var lengthInsideBuffer = g.intersectionLength(buffer);
    double percentInBuffer = lengthInsideBuffer / sidewalkLength;
    return new NamedEdgeGroup(percentInBuffer, g.name);
  }

  /**
   * If a single street is split into several edges, each individual part of the street would potentially
   * have a low similarity with the (longer) sidewalk. For that reason we combine them into a group
   * and have a better basis for comparison.
   */
  private static Stream<CandidateGroup> groupEdgesByName(List<EdgeOnLevel> candidates) {
    return candidates
      .stream()
      .collect(Collectors.groupingBy(e -> e.edge().getName()))
      .entrySet()
      .stream()
      .map(entry -> {
        var levels = entry
          .getValue()
          .stream()
          .flatMap(e -> e.levels().stream())
          .collect(Collectors.toSet());
        return new CandidateGroup(
          entry.getKey(),
          entry.getValue().stream().map(EdgeOnLevel::edge).toList(),
          levels
        );
      });
  }

  private record NamedEdgeGroup(double percentInBuffer, I18NString name) {
    NamedEdgeGroup {
      Objects.requireNonNull(name);
    }
  }

  /**
   * A group of edges that are near a sidewalk that have the same name. These groups are used
   * to figure out if the name of the group can be applied to a nearby sidewalk.
   */
  private record CandidateGroup(I18NString name, List<StreetEdge> edges, Set<OsmLevel> levels) {
    /**
     * How much of this group intersects with the given geometry, in meters.
     */
    double intersectionLength(Geometry polygon) {
      return edges
        .stream()
        .mapToDouble(edge -> {
          var intersection = polygon.intersection(edge.getGeometry());
          return length(intersection);
        })
        .sum();
    }

    private double length(Geometry intersection) {
      return switch (intersection) {
        case LineString ls -> SphericalDistanceLibrary.length(ls);
        case MultiLineString mls -> GeometryUtils.getLineStrings(mls)
          .stream()
          .mapToDouble(this::intersectionLength)
          .sum();
        case Point ignored -> 0;
        case Geometry g -> throw new IllegalStateException(
          "Didn't expect geometry %s".formatted(g.getClass())
        );
      };
    }
  }
}
