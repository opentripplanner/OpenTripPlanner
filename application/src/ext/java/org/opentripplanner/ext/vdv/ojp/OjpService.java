package org.opentripplanner.ext.vdv.ojp;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper.OptionalFeature.ONWARD_CALLS;
import static org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper.OptionalFeature.PREVIOUS_CALLS;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.StopEventParamStructure;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class OjpService {

  private final VdvService vdvService;
  private final StopEventResponseMapper mapper;
  private final ZoneId zoneId;

  public OjpService(VdvService vdvService, StopEventResponseMapper mapper, ZoneId zoneId) {
    this.vdvService = vdvService;
    this.mapper = mapper;
    this.zoneId = zoneId;
  }

  public OJP handleStopEvenRequest(OJPStopEventRequestStructure ser) {
    var stopId = FeedScopedId.parse(ser.getLocation().getPlaceRef().getStopPointRef().getValue());
    var time = Optional
      .ofNullable(ser.getLocation().getDepArrTime().atZone(zoneId))
      .orElse(ZonedDateTime.now(zoneId));
    var numResults = Optional
      .ofNullable(ser.getParams())
      .map(s -> s.getNumberOfResults())
      .map(i -> i.intValue())
      .orElse(1);

    var tripTimesOnDate = vdvService.findStopTimesInPattern(stopId, time.toInstant(), numResults);
    return mapper.mapStopTimesInPattern(
      tripTimesOnDate,
      ZonedDateTime.now(),
      mapOptionalFeatures(ser.getParams())
    );
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
}
