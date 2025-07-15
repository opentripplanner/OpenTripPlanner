package org.opentripplanner.ext.trias.service;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.trias.mapping.StopEventResponseMapper.OptionalFeature.ONWARD_CALLS;
import static org.opentripplanner.ext.trias.mapping.StopEventResponseMapper.OptionalFeature.PREVIOUS_CALLS;
import static org.opentripplanner.ext.trias.mapping.StopEventResponseMapper.OptionalFeature.REALTIME_DATA;

import de.vdv.ojp20.LineDirectionFilterStructure;
import de.vdv.ojp20.ModeFilterStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.OperatorFilterStructure;
import de.vdv.ojp20.PersonalModesEnumeration;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.PlaceRefStructure;
import de.vdv.ojp20.StopEventParamStructure;
import de.vdv.ojp20.StopEventTypeEnumeration;
import de.vdv.ojp20.UseRealtimeDataEnumeration;
import de.vdv.ojp20.siri.StopPointRefStructure;
import java.math.BigInteger;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.ext.trias.id.IdResolver;
import org.opentripplanner.ext.trias.mapping.PtModeMapper;
import org.opentripplanner.ext.trias.mapping.StopEventResponseMapper;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.ArrivalDeparture;

/**
 * Takes raw OJP types, extracts information and forwards it to the underlying services.
 */
public class OjpServiceMapper {

  public static final int DEFAULT_RADIUS_METERS = 1000;
  public static final int DEFAULT_NUM_DEPARTURES = 1;
  private static final Duration DEFAULT_TIME_WINDOW = Duration.ofHours(2);
  private final OjpService vdvService;
  private final IdResolver idResolver;
  private final ZoneId zoneId;

  public OjpServiceMapper(OjpService vdvService, IdResolver idResolver, ZoneId zoneId) {
    this.vdvService = vdvService;
    this.idResolver = idResolver;
    this.zoneId = zoneId;
  }

  public OJP handleStopEventRequest(OJPStopEventRequestStructure ser) {
    var stopId = stopPointRef(ser);
    var coordinate = coordinate(ser);

    final var params = extractStopEventParams(ser);

    List<CallAtStop> callsAtStop = List.of();
    if (stopId.isPresent()) {
      callsAtStop = vdvService.findCallsAtStop(stopId.get(), params);
    } else if (coordinate.isPresent()) {
      callsAtStop = vdvService.findCallsAtStop(coordinate.get(), params);
    }
    var optional = mapOptionalFeatures(ser.getParams());
    var mapper = new StopEventResponseMapper(
      optional,
      zoneId,
      idResolver,
      vdvService::resolveLanguage
    );
    return mapper.mapCalls(callsAtStop, ZonedDateTime.now());
  }

  protected OjpService.StopEventRequestParams extractStopEventParams(
    OJPStopEventRequestStructure ser
  ) {
    var time = Optional.ofNullable(ser.getLocation().getDepArrTime().atZone(zoneId)).orElse(
      ZonedDateTime.now(zoneId)
    );
    int numResults = params(ser)
      .map(s -> s.getNumberOfResults())
      .map(i -> i.intValue())
      .orElse(DEFAULT_NUM_DEPARTURES);

    var arrivalDeparture = arrivalDeparture(ser);
    var timeWindow = timeWindow(ser);
    Set<FeedScopedId> includedAgencies = agencyFilter(ser, o -> !isExclude(o.isExclude()));
    Set<FeedScopedId> includedRoutes = lineFilter(ser, o -> !isExclude(o.isExclude()));
    Set<FeedScopedId> excludedAgencies = agencyFilter(ser, f -> isExclude(f.isExclude()));
    Set<FeedScopedId> excludedRoutes = lineFilter(ser, f -> isExclude(f.isExclude()));
    Set<TransitMode> includedModes = modeFilter(ser, m -> !isExclude(m.isExclude()));
    Set<TransitMode> excludedModes = modeFilter(ser, m -> isExclude(m.isExclude()));
    int maxWalkDistance = Optional.ofNullable(ser.getLocation())
      .flatMap(l ->
        l
          .getIndividualTransportOption()
          .stream()
          .filter(
            o -> o.getItModeAndModeOfOperation().getPersonalMode() == PersonalModesEnumeration.FOOT
          )
          .findFirst()
          .flatMap(o -> Optional.ofNullable(o.getMaxDistance()))
      )
      .map(BigInteger::intValue)
      .orElse(DEFAULT_RADIUS_METERS);

    return new OjpService.StopEventRequestParams(
      time.toInstant(),
      arrivalDeparture,
      timeWindow,
      maxWalkDistance,
      numResults,
      includedAgencies,
      includedRoutes,
      excludedAgencies,
      excludedRoutes,
      includedModes,
      excludedModes
    );
  }

  private static boolean isExclude(Boolean b) {
    return b == null || TRUE.equals(b);
  }

  private Set<TransitMode> modeFilter(
    OJPStopEventRequestStructure ser,
    Predicate<ModeFilterStructure> predicate
  ) {
    return params(ser)
      .map(StopEventParamStructure::getModeFilter)
      .filter(predicate)
      .map(ModeFilterStructure::getPtMode)
      .stream()
      .flatMap(m -> m.stream().map(PtModeMapper::map))
      .collect(Collectors.toSet());
  }

  private Set<FeedScopedId> agencyFilter(
    OJPStopEventRequestStructure ser,
    Predicate<OperatorFilterStructure> predicate
  ) {
    return params(ser)
      .map(p -> p.getOperatorFilter())
      .filter(predicate)
      .map(o -> o.getOperatorRef())
      .stream()
      .flatMap(r -> r.stream().map(ref -> ref.getValue()))
      .map(idResolver::parse)
      .collect(Collectors.toSet());
  }

  private Set<FeedScopedId> lineFilter(
    OJPStopEventRequestStructure ser,
    Predicate<LineDirectionFilterStructure> predicate
  ) {
    return params(ser)
      .map(p -> p.getLineFilter())
      .filter(predicate)
      .map(o -> o.getLine())
      .stream()
      .flatMap(r -> r.stream().map(l -> l.getLineRef().getValue()))
      .map(idResolver::parse)
      .collect(Collectors.toSet());
  }

  private static Optional<StopEventParamStructure> params(OJPStopEventRequestStructure ser) {
    return Optional.ofNullable(ser.getParams());
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
    if (UseRealtimeDataEnumeration.NONE != params.getUseRealtimeData()) {
      res.add(REALTIME_DATA);
    }
    return res;
  }

  private static Optional<PlaceRefStructure> placeRefStructure(OJPStopEventRequestStructure ser) {
    return Optional.ofNullable(ser.getLocation()).map(PlaceContextStructure::getPlaceRef);
  }

  private static ArrivalDeparture arrivalDeparture(OJPStopEventRequestStructure ser) {
    return params(ser)
      .map(StopEventParamStructure::getStopEventType)
      .map(OjpServiceMapper::mapType)
      .orElse(ArrivalDeparture.BOTH);
  }

  private static ArrivalDeparture mapType(StopEventTypeEnumeration t) {
    return switch (t) {
      case DEPARTURE -> ArrivalDeparture.DEPARTURES;
      case ARRIVAL -> ArrivalDeparture.ARRIVALS;
      case BOTH -> ArrivalDeparture.BOTH;
    };
  }

  private static Duration timeWindow(OJPStopEventRequestStructure ser) {
    return params(ser).map(StopEventParamStructure::getTimeWindow).orElse(DEFAULT_TIME_WINDOW);
  }
}
