package org.opentripplanner.gtfs.mapping;

import static org.onebusaway.gtfs.serialization.mappings.StopTimeFieldMappingFactory.getStringAsSeconds;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.csv_entities.schema.EntitySchema;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

/**
 * Responsible for mapping GTFS StopTime into the OTP Transit model.
 */
class StopTimeMapper {

  public static final EntitySchema SCHEMA = new DefaultEntitySchemaFactory()
    .getSchema(org.onebusaway.gtfs.model.StopTime.class);
  private final IdFactory idFactory;

  private final LocationMapper locationMapper;

  private final LocationGroupMapper locationGroupMapper;

  private final TripMapper tripMapper;
  private final BookingRuleMapper bookingRuleMapper;

  private final TranslationHelper translationHelper;
  private final SiteRepositoryBuilder siteRepositoryBuilder;

  StopTimeMapper(
    IdFactory idFactory,
    StopMapper stopMapper,
    LocationMapper locationMapper,
    LocationGroupMapper locationGroupMapper,
    TripMapper tripMapper,
    BookingRuleMapper bookingRuleMapper,
    TranslationHelper translationHelper
  ) {
    this.idFactory = idFactory;
    this.siteRepositoryBuilder = stopMapper.siteRepositoryBuilder();
    this.locationMapper = locationMapper;
    this.locationGroupMapper = locationGroupMapper;
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
      .map(st -> this.doMap(st, trips));
  }

  private StopTime doMap(Map<String, String> row, Map<FeedScopedId, Trip> trips) {
    StopTime lhs = new StopTime();

    var tripId = idFactory.createId(row.get("trip_id"), "stop time's trip");
    var trip = Objects.requireNonNull(
      trips.get(tripId),
      "Stop time refers to non-existent trip with id %s".formatted(tripId)
    );
    lhs.setTrip(trip);

    var stopId = row.get("stop_id");
    var stopLocationId = row.get("stop_location_id");
    var locationGroupId = row.get("stop_group_id");

    if (stopId != null) {
      var id = idFactory.createId(stopId, "stop id");
      lhs.setStop(Objects.requireNonNull(siteRepositoryBuilder.regularStopsById().get(id)));
    }

    lhs.setArrivalTime(getStringAsSeconds(row.get("arrival_time")));
    lhs.setDepartureTime(getStringAsSeconds(row.get("departure_time")));
    //lhs.setTimepoint(row.getTimepoint());
    lhs.setStopSequence(Integer.parseInt(row.get("stop_sequence")));
    //lhs.setStopHeadsign(stopHeadsign);
    lhs.setPickupType(PickDropMapper.map(row.get("pickup_type")));
    lhs.setDropOffType(PickDropMapper.map(row.get("drop_off_type")));

    /**
    lhs.setShapeDistTraveled( row.get("shape_dist_traveled"));
    lhs.setFlexWindowStart(row.getStartPickupDropOffWindow());
    lhs.setFlexWindowEnd(row.getEndPickupDropOffWindow());

    lhs.setFlexContinuousPickup(
      PickDropMapper.mapFlexContinuousPickDrop(row.getContinuousPickup())
    );
    lhs.setFlexContinuousDropOff(
      PickDropMapper.mapFlexContinuousPickDrop(row.getContinuousDropOff())
    );
    lhs.setPickupBookingInfo(bookingRuleMapper.map(row.getPickupBookingRule()));
    lhs.setDropOffBookingInfo(bookingRuleMapper.map(row.getDropOffBookingRule()));
       */

    return lhs;
  }
}
