package org.opentripplanner.gtfs.mapping;

import static org.onebusaway.gtfs.serialization.mappings.StopTimeFieldMappingFactory.getStringAsSeconds;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.onebusaway.csv_entities.CsvInputSource;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.lang.StringUtils;

/**
 * Responsible for mapping GTFS StopTime into the OTP Transit model.
 */
class StopTimeMapper {

  private static final String TIMEPOINT = "timepoint";
  private static final String SHAPE_DIST_TRAVELED = "shape_dist_traveled";
  private static final String STOP_SEQUENCE = "stop_sequence";
  private static final String DEPARTURE_TIME = "departure_time";
  private static final String ARRIVAL_TIME = "arrival_time";
  private static final String PICKUP_TYPE = "pickup_type";
  private static final String DROP_OFF_TYPE = "drop_off_type";
  private static final String PICKUP_BOOKING_RULE_ID = "pickup_booking_rule_id";
  private static final String DROP_OFF_BOOKING_RULE_ID = "drop_off_booking_rule_id";
  private static final String START_PICKUP_DROP_OFF_WINDOW = "start_pickup_drop_off_window";
  private static final String END_PICKUP_DROP_OFF_WINDOW = "end_pickup_drop_off_window";
  private static final String CONTINUOUS_PICKUP = "continuous_pickup";
  private static final String CONTINUOUS_DROP_OFF = "continuous_drop_off";
  private static final String STOP_ID = "stop_id";
  private static final String LOCATION_ID = "location_id";
  private static final String STOP_GROUP_ID = "stop_group_id";
  private static final String STOP_HEADSIGN = "stop_headsign";
  private static final String FILE = "stops_times.txt";
  private final IdFactory idFactory;

  private final BookingRuleMapper bookingRuleMapper;
  private final TranslationHelper translationHelper;
  private final OtpTransitServiceBuilder builder;

  StopTimeMapper(
    IdFactory idFactory,
    OtpTransitServiceBuilder builder,
    BookingRuleMapper bookingRuleMapper,
    TranslationHelper translationHelper
  ) {
    this.idFactory = idFactory;
    this.builder = builder;
    this.bookingRuleMapper = bookingRuleMapper;
    this.translationHelper = translationHelper;
  }

  Stream<StopTime> map(CsvInputSource inputSource) throws IOException {
    return new StreamingCsvReader(inputSource)
      .rows(FILE)
      .map(st -> this.doMap(new StopTimeRow(st, idFactory), builder.getTripsById()));
  }

  private StopTime doMap(StopTimeRow row, EntityById<Trip> trips) {
    StopTime lhs = new StopTime();

    var tripId = idFactory.createId(row.requiredString("trip_id"), "stop time's trip");
    var trip = Objects.requireNonNull(
      trips.get(tripId),
      "Stop time refers to non-existent trip with id %s".formatted(tripId)
    );
    lhs.setTrip(trip);

    var stopId = row.string(STOP_ID);
    var stopLocationId = row.string(LOCATION_ID);
    var locationGroupId = row.string(STOP_GROUP_ID);

    var siteRepositoryBuilder = builder.siteRepository();
    if (stopId != null) {
      var id = idFactory.createId(stopId, "stop_time's stop");
      var stop = Objects.requireNonNull(
        siteRepositoryBuilder.regularStopsById().get(id),
        "Stop '%s' not found".formatted(stopId)
      );
      lhs.setStop(stop);
    } else if (stopLocationId != null) {
      var id = idFactory.createId(stopLocationId, "stop time's location");
      var stop = Objects.requireNonNull(
        siteRepositoryBuilder.areaStopById().get(id),
        "Stop location '%s' not found".formatted(id)
      );
      lhs.setStop(stop);
    } else if (locationGroupId != null) {
      var id = idFactory.createId(locationGroupId, "stop time's location group");
      lhs.setStop(Objects.requireNonNull(siteRepositoryBuilder.groupStopById().get(id)));
    } else {
      throw new IllegalArgumentException(
        "Stop time must have either a %s, %s, or %s".formatted(STOP_ID, LOCATION_ID, STOP_GROUP_ID)
      );
    }

    lhs.setArrivalTime(row.time(ARRIVAL_TIME));
    lhs.setDepartureTime(row.time(DEPARTURE_TIME));
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

    row.optionalString(STOP_HEADSIGN).ifPresent(hs -> lhs.setStopHeadsign(I18NString.of(hs)));
    row
      .id(PICKUP_BOOKING_RULE_ID)
      .ifPresent(id ->
        lhs.setPickupBookingInfo(
          Objects.requireNonNull(
            bookingRuleMapper.findBookingRule(id),
            "Pickup booking rule '%s' not found".formatted(id)
          )
        )
      );
    row
      .id(DROP_OFF_BOOKING_RULE_ID)
      .ifPresent(id ->
        lhs.setPickupBookingInfo(
          Objects.requireNonNull(
            bookingRuleMapper.findBookingRule(id),
            "Drop off booking rule '%s' not found".formatted(id)
          )
        )
      );

    return lhs;
  }

  record StopTimeRow(Map<String, String> row, IdFactory idFactory) {
    public String requiredString(String field) {
      if (row.containsKey(field)) {
        return row.get(field);
      } else {
        throw new IllegalArgumentException(
          "Missing required field '%s' in stop time CSV row: %s".formatted(field, row)
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

    public Optional<String> optionalString(String stopHeadsign) {
      return Optional.ofNullable(row.get(stopHeadsign)).filter(StringUtils::hasValue);
    }
  }
}
