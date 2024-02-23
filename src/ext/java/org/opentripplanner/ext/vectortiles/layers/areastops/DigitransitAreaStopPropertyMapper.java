package org.opentripplanner.ext.vectortiles.layers.areastops;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitAreaStopPropertyMapper extends PropertyMapper<AreaStop> {

  private final TransitService transitService;
  private final I18NStringMapper i18NStringMapper;

  private DigitransitAreaStopPropertyMapper(TransitService transitService, Locale locale) {
    this.transitService = transitService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  protected static DigitransitAreaStopPropertyMapper create(
    TransitService transitService,
    Locale locale
  ) {
    return new DigitransitAreaStopPropertyMapper(transitService, locale);
  }

  @Override
  protected Collection<KeyValue> map(AreaStop stop) {
    var routeColors = transitService
      .getRoutesForStop(stop)
      .stream()
      .map(Route::getColor)
      .distinct()
      .toList();
    return List.of(
      new KeyValue("gtfsId", stop.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(stop.getName())),
      new KeyValue("code", stop.getCode()),
      new KeyValue("routeColors", routeColors)
    );
  }
}
