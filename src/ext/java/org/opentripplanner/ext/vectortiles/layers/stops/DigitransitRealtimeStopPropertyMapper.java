package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper.getBaseKeyValues;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
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
    Instant currentTime = Instant.now();
    boolean noServiceAlert = transitService
      .getTransitAlertService()
      .getStopAlerts(stop.getId())
      .stream()
      .anyMatch(alert -> alert.noServiceAt(currentTime));

    Collection<KeyValue> sharedKeyValues = getBaseKeyValues(stop, i18NStringMapper, transitService);
    return ListUtils.combine(
      sharedKeyValues,
      List.of(new KeyValue("closedByServiceAlert", noServiceAlert))
    );
  }
}
