package org.opentripplanner.ext.ojp.mapping;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.ONWARD_CALLS;
import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.PREVIOUS_CALLS;
import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.REALTIME_DATA;

import de.vdv.ojp20.ModeFilterStructure;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.PersonalModesEnumeration;
import de.vdv.ojp20.StopEventParamStructure;
import de.vdv.ojp20.StopEventTypeEnumeration;
import de.vdv.ojp20.UseRealtimeDataEnumeration;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.ojp.service.CallAtStopService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class StopEventParamsMapper {

  private static final Duration DEFAULT_TIME_WINDOW = Duration.ofHours(2);
  public static final int DEFAULT_RADIUS_METERS = 1000;
  public static final int DEFAULT_NUM_DEPARTURES = 1;
  private final ZoneId zoneId;
  private final FilterMapper filterMapper;

  public StopEventParamsMapper(ZoneId zoneId, FeedScopedIdMapper idMapper) {
    this.zoneId = zoneId;
    this.filterMapper = new FilterMapper(idMapper);
  }

  public CallAtStopService.StopEventRequestParams extractStopEventParams(
    OJPStopEventRequestStructure ser
  ) {
    var time = Optional.ofNullable(ser.getLocation().getDepArrTime())
      .map(t -> t.atZone(zoneId).toInstant())
      .orElse(Instant.now());
    int numResults = params(ser)
      .map(s -> s.getNumberOfResults())
      .map(i -> i.intValue())
      .orElse(DEFAULT_NUM_DEPARTURES);

    var arrivalDeparture = arrivalDeparture(ser);
    var timeWindow = timeWindow(ser);
    Set<FeedScopedId> includedAgencies = filterMapper.includedAgencies(ser);
    Set<FeedScopedId> includedRoutes = filterMapper.includedRoutes(ser);
    Set<FeedScopedId> excludedAgencies = filterMapper.excludedAgencies(ser);
    Set<FeedScopedId> excludedRoutes = filterMapper.excludedRoutes(ser);
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
      .orElse(DEFAULT_RADIUS_METERS);

    return new CallAtStopService.StopEventRequestParams(
      time,
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

  public static Set<StopEventResponseMapper.OptionalFeature> mapOptionalFeatures(
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

  private static ArrivalDeparture arrivalDeparture(OJPStopEventRequestStructure ser) {
    return params(ser)
      .map(StopEventParamStructure::getStopEventType)
      .map(StopEventParamsMapper::mapType)
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

  private static Optional<StopEventParamStructure> params(OJPStopEventRequestStructure ser) {
    return Optional.ofNullable(ser.getParams());
  }
}
