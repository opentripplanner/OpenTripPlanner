package org.opentripplanner.ext.vectortiles.layers.areastops;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class AreaStopPropertyMapper extends PropertyMapper<AreaStop> {

  private final Function<StopLocation, Collection<Route>> getRoutesForStop;
  private final I18NStringMapper i18NStringMapper;

  protected AreaStopPropertyMapper(
    Function<StopLocation, Collection<Route>> getRoutesForStop,
    Locale locale
  ) {
    this.getRoutesForStop = getRoutesForStop;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  protected static AreaStopPropertyMapper create(TransitService transitService, Locale locale) {
    return new AreaStopPropertyMapper(transitService::getRoutesForStop, locale);
  }

  @Override
  protected Collection<KeyValue> map(AreaStop stop) {
    var routeColors = getRoutesForStop
      .apply(stop)
      .stream()
      .map(Route::getColor)
      .filter(Objects::nonNull)
      .distinct()
      // the MVT spec explicitly doesn't cover how to encode arrays
      // https://docs.mapbox.com/data/tilesets/guides/vector-tiles-standards/#what-the-spec-doesnt-cover
      .collect(Collectors.joining(","));
    return List.of(
      new KeyValue("gtfsId", stop.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(stop.getName())),
      new KeyValue("code", stop.getCode()),
      new KeyValue("routeColors", routeColors)
    );
  }
}
