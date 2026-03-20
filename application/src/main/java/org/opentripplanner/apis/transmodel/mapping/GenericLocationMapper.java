package org.opentripplanner.apis.transmodel.mapping;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.framework.DatedServiceJourneyReferenceInputType;
import org.opentripplanner.apis.transmodel.model.framework.PointInJourneyPatternReferenceInputType;
import org.opentripplanner.apis.transmodel.support.OneOfInputValidator;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.routing.api.request.TripOnDateReference;

class GenericLocationMapper {

  private final FeedScopedIdMapper idMapper;

  GenericLocationMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  /**
   * Maps a GraphQL Location input type to a GenericLocation
   */
  GenericLocation toGenericLocation(Map<String, Object> m) {
    Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
    Double lat = null;
    Double lon = null;
    if (coordinates != null) {
      lat = (Double) coordinates.get("latitude");
      lon = (Double) coordinates.get("longitude");
    }

    String placeRef = (String) m.get("place");
    FeedScopedId stopId = idMapper.parseNullSafe(placeRef).orElse(null);
    String name = (String) m.get("name");
    name = name == null ? "" : name;

    TripLocation tripLocation = mapTripLocation((Map<String, Object>) m.get("onBoardLocation"));

    return new GenericLocation(name, stopId, lat, lon, tripLocation);
  }

  @Nullable
  private TripLocation mapTripLocation(@Nullable Map<String, Object> m) {
    if (m == null) {
      return null;
    }

    var tripOnDateReference = mapTripOnDateReference(
      (Map<String, Object>) m.get("datedServiceJourneyReference")
    );

    return mapPointInJourneyPatternReference(
      (Map<String, Object>) m.get("pointInJourneyPatternReference"),
      tripOnDateReference
    );
  }

  private TripLocation mapPointInJourneyPatternReference(
    Map<String, Object> m,
    TripOnDateReference tripOnDateReference
  ) {
    FeedScopedId stopLocationId = idMapper.parseStrict(
      (String) m.get(PointInJourneyPatternReferenceInputType.FIELD_STOP_LOCATION_ID)
    );
    Long aimedDepartureTimeMillis = (Long) m.get(
      PointInJourneyPatternReferenceInputType.FIELD_AIMED_DEPARTURE_TIME
    );

    if (aimedDepartureTimeMillis != null) {
      return TripLocation.of(
        tripOnDateReference,
        stopLocationId,
        Instant.ofEpochMilli(aimedDepartureTimeMillis)
      );
    }
    return TripLocation.of(tripOnDateReference, stopLocationId);
  }

  private TripOnDateReference mapTripOnDateReference(Map<String, Object> m) {
    var fieldName = OneOfInputValidator.validateOneOf(
      m,
      "DatedServiceJourneyReference",
      DatedServiceJourneyReferenceInputType.FIELD_SERVICE_JOURNEY_ON_SERVICE_DATE,
      DatedServiceJourneyReferenceInputType.FIELD_DATED_SERVICE_JOURNEY_ID
    );

    return switch (fieldName) {
      case DatedServiceJourneyReferenceInputType.FIELD_SERVICE_JOURNEY_ON_SERVICE_DATE -> {
        var tripIdOnDate = (Map<String, Object>) m.get(fieldName);
        FeedScopedId tripId = idMapper.parseStrict((String) tripIdOnDate.get("serviceJourneyId"));
        LocalDate serviceDate = (LocalDate) tripIdOnDate.get("serviceDate");
        yield TripOnDateReference.ofTripIdAndServiceDate(tripId, serviceDate);
      }
      case DatedServiceJourneyReferenceInputType.FIELD_DATED_SERVICE_JOURNEY_ID -> {
        FeedScopedId tripOnDateId = idMapper.parseStrict((String) m.get(fieldName));
        yield TripOnDateReference.ofTripOnServiceDateId(tripOnDateId);
      }
      default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
    };
  }
}
