package org.opentripplanner.ext.vdv.ojp;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
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
    return mapper.mapStopTimesInPattern(tripTimesOnDate, ZonedDateTime.now());
  }
}
