package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper.getRoutes;
import static org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper.getType;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitRealtimeStopPropertyMapper extends PropertyMapper<RegularStop> {

  private final TransitService transitService;
  private final I18NStringMapper i18NStringMapper;

  public DigitransitRealtimeStopPropertyMapper(TransitService transitService, Locale locale) {
    this.transitService = transitService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(RegularStop stop) {
    Instant currentTime = ZonedDateTime.now(transitService.getTimeZone()).toInstant();
    boolean noServiceAlert = transitService
      .getTransitAlertService()
      .getStopAlerts(stop.getId())
      .stream()
      .anyMatch(alert ->
        alert.effect().equals(AlertEffect.NO_SERVICE) &&
        (
          alert.getEffectiveStartDate() != null &&
          alert.getEffectiveStartDate().isBefore(currentTime)
        ) &&
        (alert.getEffectiveEndDate() != null && alert.getEffectiveEndDate().isAfter(currentTime))
      );

    return List.of(
      new KeyValue("gtfsId", stop.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(stop.getName())),
      new KeyValue("code", stop.getCode()),
      new KeyValue("platform", stop.getPlatformCode()),
      new KeyValue("desc", i18NStringMapper.mapToApi(stop.getDescription())),
      new KeyValue(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : "null"
      ),
      new KeyValue("type", getType(transitService, stop)),
      new KeyValue("routes", getRoutes(transitService, stop)),
      new KeyValue("noServiceAlert", noServiceAlert)
    );
  }
}
