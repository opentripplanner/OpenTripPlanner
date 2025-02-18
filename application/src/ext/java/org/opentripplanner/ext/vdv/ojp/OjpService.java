package org.opentripplanner.ext.vdv.ojp;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper.OptionalFeature.ONWARD_CALLS;
import static org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper.OptionalFeature.PREVIOUS_CALLS;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.PlaceRefStructure;
import de.vdv.ojp20.StopEventParamStructure;
import de.vdv.ojp20.siri.CoordinatesStructure;
import de.vdv.ojp20.siri.StopPointRefStructure;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.ext.vdv.id.IdResolver;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class OjpService {

  private final VdvService vdvService;
  private final IdResolver idResolver;
  private final StopEventResponseMapper mapper;
  private final ZoneId zoneId;

  public OjpService(
    VdvService vdvService,
    IdResolver idResolver,
    StopEventResponseMapper mapper,
    ZoneId zoneId
  ) {
    this.vdvService = vdvService;
    this.idResolver = idResolver;
    this.mapper = mapper;
    this.zoneId = zoneId;
  }

  public OJP handleStopEvenRequest(OJPStopEventRequestStructure ser) {
    var stopId = stopPointRef(ser);
    var coordinate = coordinate(ser);

    var time = Optional
      .ofNullable(ser.getLocation().getDepArrTime().atZone(zoneId))
      .orElse(ZonedDateTime.now(zoneId));
    int numResults = Optional
      .ofNullable(ser.getParams())
      .map(s -> s.getNumberOfResults())
      .map(i -> i.intValue())
      .orElse(1);

    List<TripTimeOnDate> tripTimesOnDate = List.of();

    if (stopId.isPresent()) {
      tripTimesOnDate = vdvService.findTripTimesOnDate(stopId.get(), time.toInstant(), numResults);
    } else if (coordinate.isPresent()) {
      tripTimesOnDate =
        vdvService.findTripTimesOnDate(coordinate.get(), time.toInstant(), numResults);
    }
    return mapper.mapStopTimesInPattern(
      tripTimesOnDate,
      ZonedDateTime.now(),
      mapOptionalFeatures(ser.getParams())
    );
  }

  private Optional<FeedScopedId> stopPointRef(OJPStopEventRequestStructure ser) {
    return placeRefStructure(ser)
      .map(PlaceRefStructure::getStopPointRef)
      .map(StopPointRefStructure::getValue)
      .map(idResolver::parse);
  }

  private Optional<WgsCoordinate> coordinate(OJPStopEventRequestStructure ser) {
    return placeRefStructure(ser)
      .map(PlaceRefStructure::getGeoPosition)
      .map(c -> new WgsCoordinate(c.getLatitude().doubleValue(), c.getLongitude().doubleValue()));
  }

  private static Set<StopEventResponseMapper.OptionalFeature> mapOptionalFeatures(
    StopEventParamStructure params
  ) {
    var res = new HashSet<StopEventResponseMapper.OptionalFeature>();

    if (TRUE.equals(params.isIncludePreviousCalls())) {
      res.add(PREVIOUS_CALLS);
    }
    if (TRUE.equals(params.isIncludeOnwardCalls())) {
      res.add(ONWARD_CALLS);
    }
    return res;
  }

  private static Optional<PlaceRefStructure> placeRefStructure(OJPStopEventRequestStructure ser) {
    return Optional.ofNullable(ser.getLocation()).map(PlaceContextStructure::getPlaceRef);
  }
}
