package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper.getBaseKeyValues;
import static org.opentripplanner.inspector.vector.KeyValue.kColl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.core.model.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.ListUtils;

public class DigitransitRealtimeStopPropertyMapper extends PropertyMapper<RegularStop> {

  private final TransitService transitService;
  private final TransitAlertService transitAlertService;
  private final I18NStringMapper i18NStringMapper;

  public DigitransitRealtimeStopPropertyMapper(
    TransitService transitService,
    TransitAlertService transitAlertService,
    Locale locale
  ) {
    this.transitService = transitService;
    this.transitAlertService = transitAlertService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(RegularStop stop) {
    Instant currentTime = Instant.now();
    var stopAlerts = transitAlertService.getStopLocationsAlerts(stop.getIdAndParentStationId());
    boolean noServiceAlert = stopAlerts
      .stream()
      .anyMatch(alert -> alert.effect() == AlertEffect.NO_SERVICE && alert.isActiveAt(currentTime));

    var validAlerts = stopAlerts
      .stream()
      .filter(alert -> alert.isActiveAt(currentTime))
      .toList();

    var mostSevereAlert = validAlerts
      .stream()
      .filter(alert -> alert.severity() != null)
      .max(Comparator.comparingInt(alert -> alert.severity().sortingIndex()));
    String mostSevereAlertSeverityLevel = mostSevereAlert
      .map(alert -> AlertSeverityToStringMapper.map(alert.severity()))
      .orElse(null);

    List<String> mostSevereAlertEffects = mostSevereAlert
      .map(TransitAlert::severity)
      .map(severity ->
        validAlerts
          .stream()
          .filter(alert -> severity.equals(alert.severity()))
          .filter(alert -> alert.effect() != null)
          .map(alert -> AlertEffectToStringMapper.map(alert.effect()))
          .distinct()
          .sorted()
          .toList()
      )
      .orElse(List.of());

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
        new KeyValue("mostSevereAlertSeverityLevel", mostSevereAlertSeverityLevel),
        kColl("mostSevereAlertsEffects", mostSevereAlertEffects),
        new KeyValue("servicesRunningOnServiceDate", stopTimesExist),
        new KeyValue("servicesRunningInFuture", inService)
      )
    );
  }
}
