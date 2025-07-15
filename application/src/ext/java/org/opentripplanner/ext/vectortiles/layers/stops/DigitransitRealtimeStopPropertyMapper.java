package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper.getBaseKeyValues;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.ListUtils;

public class DigitransitRealtimeStopPropertyMapper extends PropertyMapper<RegularStop> {

  private final TransitService transitService;
  private final I18NStringMapper i18NStringMapper;

  public DigitransitRealtimeStopPropertyMapper(TransitService transitService, Locale locale) {
    this.transitService = transitService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(RegularStop stop) {
    Instant currentTime = Instant.now();
    boolean noServiceAlert = transitService
      .getTransitAlertService()
      .getStopAlerts(stop.getId())
      .stream()
      .anyMatch(alert -> alert.noServiceAt(currentTime));

    var serviceDate = LocalDate.now(transitService.getTimeZone());
    boolean stopTimesExist = transitService
      .findStopTimesInPattern(stop, serviceDate, ArrivalDeparture.BOTH, true)
      .stream()
      .anyMatch(stopTime -> stopTime.times.size() > 0);
    var inService = transitService.hasScheduledServicesAfter(LocalDate.now(), stop);

    Collection<KeyValue> sharedKeyValues = getBaseKeyValues(stop, i18NStringMapper, transitService);
    return ListUtils.combine(
      sharedKeyValues,
      List.of(
        new KeyValue("closedByServiceAlert", noServiceAlert),
        new KeyValue("servicesRunningOnServiceDate", stopTimesExist),
        new KeyValue("servicesRunningInFuture", inService)
      )
    );
  }
}
