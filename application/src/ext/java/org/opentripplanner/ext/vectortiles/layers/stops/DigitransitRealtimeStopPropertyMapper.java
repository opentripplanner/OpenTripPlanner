package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper.getBaseKeyValues;
import static org.opentripplanner.inspector.vector.KeyValue.kColl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.gtfs.mapping.AlertEffectMapper;
import org.opentripplanner.apis.gtfs.mapping.SeverityMapper;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.core.model.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.alertpatch.TransitAlert;
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
    long currentTimeSeconds = currentTime.getEpochSecond();
    var alertService = transitService.getTransitAlertService();

    Collection<TransitAlert> stopAlerts = new ArrayList<>(alertService.getStopAlerts(stop.getId()));
    for (var route : transitService.findRoutes(stop)) {
      stopAlerts.addAll(alertService.getStopAndRouteAlerts(stop.getId(), route.getId()));
      stopAlerts.addAll(alertService.getRouteAlerts(route.getId()));
    }

    boolean noServiceAlert = stopAlerts.stream().anyMatch(alert -> alert.noServiceAt(currentTime));

    var validAlerts = stopAlerts
      .stream()
      .filter(alert -> alert.displayDuring(currentTimeSeconds, currentTimeSeconds))
      .toList();

    var mostSevereAlert = validAlerts
      .stream()
      .filter(alert -> alert.severity() != null)
      .max(Comparator.comparing(TransitAlert::severity));
    String alertSeverityLevel = mostSevereAlert
      .map(alert -> SeverityMapper.getGraphQLSeverity(alert.severity()).name())
      .orElse(null);

    List<String> alertEffects = mostSevereAlert
      .map(TransitAlert::severity)
      .map(severity ->
        validAlerts
          .stream()
          .filter(alert -> severity.equals(alert.severity()))
          .filter(alert -> alert.effect() != null)
          .map(alert -> AlertEffectMapper.getGraphQLEffect(alert.effect()).name())
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
        new KeyValue("alertSeverityLevel", alertSeverityLevel),
        kColl("alertEffects", alertEffects),
        new KeyValue("servicesRunningOnServiceDate", stopTimesExist),
        new KeyValue("servicesRunningInFuture", inService)
      )
    );
  }
}
