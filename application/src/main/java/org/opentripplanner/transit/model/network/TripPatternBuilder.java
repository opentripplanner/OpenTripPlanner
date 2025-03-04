package org.opentripplanner.transit.model.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.CompactLineStringUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.api.SlackProvider;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

@SuppressWarnings("UnusedReturnValue")
public final class TripPatternBuilder
  extends AbstractEntityBuilder<TripPattern, TripPatternBuilder> {

  private Route route;
  private TransitMode mode;
  private SubMode netexSubMode;
  private boolean containsMultipleModes;
  private StopPattern stopPattern;
  private Timetable scheduledTimetable;
  private TimetableBuilder scheduledTimetableBuilder;
  private String name;

  private boolean createdByRealtimeUpdate;

  private TripPattern originalTripPattern;
  private List<LineString> hopGeometries;

  TripPatternBuilder(FeedScopedId id) {
    super(id);
    this.scheduledTimetableBuilder = Timetable.of();
  }

  TripPatternBuilder(TripPattern original) {
    super(original);
    this.name = original.getName();
    this.route = original.getRoute();
    this.mode = original.getMode();
    this.netexSubMode = original.getNetexSubmode();
    this.containsMultipleModes = original.getContainsMultipleModes();
    this.stopPattern = original.getStopPattern();
    this.scheduledTimetable = original.getScheduledTimetable();
    this.createdByRealtimeUpdate = original.isCreatedByRealtimeUpdater();
    this.originalTripPattern = original.getOriginalTripPattern();
    this.hopGeometries = original.getGeometry() == null
      ? null
      : IntStream.range(0, original.numberOfStops() - 1)
        .mapToObj(original::getHopGeometry)
        .toList();
  }

  public TripPatternBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public TripPatternBuilder withRoute(Route route) {
    this.route = route;
    return this;
  }

  public TripPatternBuilder withMode(TransitMode mode) {
    this.mode = mode;
    return this;
  }

  public TripPatternBuilder withNetexSubmode(SubMode netexSubmode) {
    this.netexSubMode = netexSubmode;
    return this;
  }

  public TripPatternBuilder withContainsMultipleModes(boolean containsMultipleModes) {
    this.containsMultipleModes = containsMultipleModes;
    return this;
  }

  public TripPatternBuilder withStopPattern(StopPattern stopPattern) {
    this.stopPattern = stopPattern;
    return this;
  }

  public TripPatternBuilder withScheduledTimeTable(Timetable scheduledTimetable) {
    if (scheduledTimetableBuilder != null) {
      throw new IllegalStateException(
        "Cannot set scheduled Timetable after scheduled Timetable builder is created"
      );
    }
    this.scheduledTimetable = scheduledTimetable;
    return this;
  }

  public TripPatternBuilder withScheduledTimeTableBuilder(
    UnaryOperator<TimetableBuilder> producer
  ) {
    // create a builder for the scheduled timetable only if it needs to be modified.
    // otherwise reuse the existing timetable
    if (scheduledTimetableBuilder == null) {
      scheduledTimetableBuilder = scheduledTimetable.copyOf();
      scheduledTimetable = null;
    }
    producer.apply(scheduledTimetableBuilder);
    return this;
  }

  public TripPatternBuilder withCreatedByRealtimeUpdater(boolean createdByRealtimeUpdate) {
    this.createdByRealtimeUpdate = createdByRealtimeUpdate;
    return this;
  }

  public TripPatternBuilder withOriginalTripPattern(TripPattern originalTripPattern) {
    this.originalTripPattern = originalTripPattern;
    return this;
  }

  public TripPatternBuilder withHopGeometries(List<LineString> hopGeometries) {
    this.hopGeometries = hopGeometries;
    return this;
  }

  // TODO: This uses a static SlackProvider. Change it to be injectable if required
  public int slackIndex() {
    return SlackProvider.slackIndex(route.getMode());
  }

  // TODO: Change the calculation to be injectable if required
  public int transitReluctanceFactorIndex() {
    return route.getMode().ordinal();
  }

  public Direction getDirection() {
    if (scheduledTimetable != null) {
      return scheduledTimetable.getDirection();
    }
    return scheduledTimetableBuilder.getDirection();
  }

  @Override
  protected TripPattern buildFromValues() {
    return new TripPattern(this);
  }

  public Route getRoute() {
    return route;
  }

  public TransitMode getMode() {
    return mode != null ? mode : route.getMode();
  }

  public SubMode getNetexSubmode() {
    return netexSubMode != null ? netexSubMode : route.getNetexSubmode();
  }

  public boolean getContainsMultipleModes() {
    return containsMultipleModes;
  }

  public StopPattern getStopPattern() {
    return stopPattern;
  }

  public Timetable getScheduledTimetable() {
    return scheduledTimetable;
  }

  public TimetableBuilder getScheduledTimetableBuilder() {
    return scheduledTimetableBuilder;
  }

  public String getName() {
    return name;
  }

  public TripPattern getOriginalTripPattern() {
    return originalTripPattern;
  }

  public boolean isCreatedByRealtimeUpdate() {
    return createdByRealtimeUpdate;
  }

  public byte[][] hopGeometries() {
    List<LineString> geometries;
    if (this.hopGeometries != null) {
      geometries = this.hopGeometries;
    } else if (this.originalTripPattern != null) {
      geometries = generateHopGeometriesFromOriginalTripPattern();
    } else {
      return null;
    }

    return geometries
      .stream()
      .map(hopGeometry -> CompactLineStringUtils.compactLineString(hopGeometry, false))
      .toArray(byte[][]::new);
  }

  /**
   * This will copy the geometry from another TripPattern to this one. It checks if each hop is
   * between the same stops before copying that hop geometry. If the stops are different but lie
   * within same station, old geometry will be used with overwrite on first and last point (to match
   * new stop places). Otherwise, it will default to straight lines between hops.
   */
  private List<LineString> generateHopGeometriesFromOriginalTripPattern() {
    // This accounts for the new TripPattern provided by a real-time update and the one that is
    // being replaced having a different number of stops. In that case the geometry will be
    // preserved up until the first mismatching stop, and a straight line will be used for
    // all segments after that.
    List<LineString> hopGeometries = new ArrayList<>();

    for (int i = 0; i < stopPattern.getSize() - 1; i++) {
      LineString hopGeometry = i < originalTripPattern.numberOfStops() - 1
        ? originalTripPattern.getHopGeometry(i)
        : null;

      if (hopGeometry != null && stopPattern.sameStops(originalTripPattern.getStopPattern(), i)) {
        // Copy hop geometry from previous pattern
        hopGeometries.add(originalTripPattern.getHopGeometry(i));
      } else if (
        hopGeometry != null && stopPattern.sameStations(originalTripPattern.getStopPattern(), i)
      ) {
        // Use old geometry but patch first and last point with new stops
        var newStart = stopPattern.getStop(i).getCoordinate().asJtsCoordinate();
        var newEnd = stopPattern.getStop(i + 1).getCoordinate().asJtsCoordinate();

        Coordinate[] coordinates = originalTripPattern.getHopGeometry(i).getCoordinates().clone();
        coordinates[0].setCoordinate(newStart);
        coordinates[coordinates.length - 1].setCoordinate(newEnd);

        hopGeometries.add(GeometryUtils.getGeometryFactory().createLineString(coordinates));
      } else {
        // Create new straight-line geometry for hop
        hopGeometries.add(
          GeometryUtils.getGeometryFactory()
            .createLineString(
              new Coordinate[] {
                stopPattern.getStop(i).getCoordinate().asJtsCoordinate(),
                stopPattern.getStop(i + 1).getCoordinate().asJtsCoordinate(),
              }
            )
        );
      }
    }
    return hopGeometries;
  }
}
