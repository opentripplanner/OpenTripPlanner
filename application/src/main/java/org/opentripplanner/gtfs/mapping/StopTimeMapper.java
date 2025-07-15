package org.opentripplanner.gtfs.mapping;

import static org.onebusaway.gtfs.serialization.mappings.StopTimeFieldMappingFactory.getStringAsSeconds;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.csv_entities.schema.EntitySchema;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.utils.lang.StringUtils;

/**
 * Responsible for mapping GTFS StopTime into the OTP Transit model.
 */
class StopTimeMapper {

  public static final EntitySchema SCHEMA = new DefaultEntitySchemaFactory()
    .getSchema(org.onebusaway.gtfs.model.StopTime.class);
  public static final String TIMEPOINT = "timepoint";
  public static final String SHAPE_DIST_TRAVELED = "shape_dist_traveled";
  public static final String STOP_SEQUENCE = "stop_sequence";
  public static final String DEPARTURE_TIME = "departure_time";
  public static final String ARRIVAL_TIME = "arrival_time";
  public static final String PICKUP_TYPE = "pickup_type";
  public static final String DROP_OFF_TYPE = "drop_off_type";
  public static final String PICKUP_BOOKING_RULE_ID = "pickup_booking_rule_id";
  public static final String DROP_OFF_BOOKING_RULE_ID = "drop_off_booking_rule_id";
  public static final String START_PICKUP_DROP_OFF_WINDOW = "start_pickup_drop_off_window";
  public static final String END_PICKUP_DROP_OFF_WINDOW = "end_pickup_drop_off_window";
  public static final String CONTINUOUS_PICKUP = "continuous_pickup";
  public static final String CONTINUOUS_DROP_OFF = "continuous_drop_off";
  private final IdFactory idFactory;

  private final TripMapper tripMapper;

  private final BookingRuleMapper bookingRuleMapper;
  private final TranslationHelper translationHelper;
  private final SiteRepositoryBuilder siteRepositoryBuilder;

  StopTimeMapper(
    IdFactory idFactory,
    StopMapper stopMapper,
    TripMapper tripMapper,
    BookingRuleMapper bookingRuleMapper,
    TranslationHelper translationHelper
  ) {
    this.idFactory = idFactory;
    this.siteRepositoryBuilder = stopMapper.siteRepositoryBuilder();
    this.tripMapper = tripMapper;
    this.bookingRuleMapper = bookingRuleMapper;
    this.translationHelper = translationHelper;
  }

  Stream<StopTime> map(CsvInputSource inputSource) throws IOException {
    var trips = tripMapper
      .getMappedTrips()
      .stream()
      .collect(Collectors.toMap(AbstractTransitEntity::getId, t -> t));
    return new StreamingCsvReader(inputSource)
      .rows(SCHEMA.getFilename())
      .map(st -> this.doMap(new StopTimeRow(st, idFactory), trips));
  }

  private StopTime doMap(StopTimeRow row, Map<FeedScopedId, Trip> trips) {
    StopTime lhs = new StopTime();

    var tripId = idFactory.createId(row.requiredString("trip_id"), "stop time's trip");
    var trip = Objects.requireNonNull(
      trips.get(tripId),
      "Stop time refers to non-existent trip with id %s".formatted(tripId)
    );
    lhs.setTrip(trip);

    var stopId = row.string("stop_id");
    var stopLocationId = row.string("stop_location_id");
    var locationGroupId = row.string("stop_group_id");

    if (stopId != null) {
      var id = idFactory.createId(stopId, "stop_time's stop");
      lhs.setStop(Objects.requireNonNull(siteRepositoryBuilder.regularStopsById().get(id)));
    } else if (stopLocationId != null) {
      var id = idFactory.createId(stopLocationId, "stop time's stop location");
      lhs.setStop(Objects.requireNonNull(siteRepositoryBuilder.areaStopById().get(id)));
    } else if (locationGroupId != null) {
      var id = idFactory.createId(locationGroupId, "stop time's location group");
      lhs.setStop(Objects.requireNonNull(siteRepositoryBuilder.groupStopById().get(id)));
    }

    lhs.setArrivalTime(row.requiredTime(ARRIVAL_TIME));
    lhs.setDepartureTime(row.requiredTime(DEPARTURE_TIME));
    lhs.setStopSequence(row.integer(STOP_SEQUENCE));

    lhs.setTimepoint(row.integer(TIMEPOINT));
    lhs.setShapeDistTraveled(row.getDouble(SHAPE_DIST_TRAVELED));
    lhs.setPickupType(PickDropMapper.map(row.string(PICKUP_TYPE)));
    lhs.setDropOffType(PickDropMapper.map(row.string(DROP_OFF_TYPE)));

    lhs.setFlexWindowStart(row.time(START_PICKUP_DROP_OFF_WINDOW));
    lhs.setFlexWindowEnd(row.time(END_PICKUP_DROP_OFF_WINDOW));

    lhs.setFlexContinuousPickup(
      PickDropMapper.mapFlexContinuousPickDrop(row.integer(CONTINUOUS_PICKUP))
    );
    lhs.setFlexContinuousDropOff(
      PickDropMapper.mapFlexContinuousPickDrop(row.integer(CONTINUOUS_DROP_OFF))
    );

    row
      .id(PICKUP_BOOKING_RULE_ID)
      .ifPresent(id ->
        lhs.setPickupBookingInfo(Objects.requireNonNull(bookingRuleMapper.findBookingRule(id)))
      );
    row
      .id(DROP_OFF_BOOKING_RULE_ID)
      .ifPresent(id ->
        lhs.setPickupBookingInfo(Objects.requireNonNull(bookingRuleMapper.findBookingRule(id)))
      );

    return lhs;
  }

  record StopTimeRow(Map<String, String> row, IdFactory idFactory) {
    public String requiredString(String field) {
      if (row.containsKey(field)) {
        return row.get(field);
      } else {
        throw new IllegalArgumentException(
          "Missing required field '%s' in stop time CSV row".formatted(field)
        );
      }
    }
    @Nullable
    public String string(String field) {
      return row.get(field);
    }
    public int integer(String field) {
      var value = row.get(field);
      if (StringUtils.hasValue(value)) {
        return Integer.parseInt(value);
      } else {
        return StopTime.MISSING_VALUE;
      }
    }
    public double getDouble(String field) {
      if (row.containsKey(field)) {
        return Double.parseDouble(row.get(field));
      } else {
        return StopTime.MISSING_VALUE;
      }
    }

    public int requiredTime(String field) {
      var value = row.get(field);
      if (value != null) {
        return getStringAsSeconds(value);
      } else {
        throw new IllegalArgumentException(
          "Missing required field '%s' in stop_times.txt".formatted(field)
        );
      }
    }
    public int time(String field) {
      var value = row.get(field);
      if (value != null) {
        return getStringAsSeconds(value);
      } else {
        return StopTime.MISSING_VALUE;
      }
    }

    public Optional<FeedScopedId> id(String field) {
      return Optional.ofNullable(row.get(field)).map(s -> idFactory.createId(s, field));
    }
  }
}
