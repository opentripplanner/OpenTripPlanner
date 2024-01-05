package org.opentripplanner.ext.vectortiles.layers.stations;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitStationPropertyMapper extends PropertyMapper<Station> {

  private final TransitService transitService;
  private final I18NStringMapper i18NStringMapper;

  private DigitransitStationPropertyMapper(TransitService transitService, Locale locale) {
    this.transitService = transitService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  public static DigitransitStationPropertyMapper create(
    TransitService transitService,
    Locale locale
  ) {
    return new DigitransitStationPropertyMapper(transitService, locale);
  }

  @Override
  public Collection<KeyValue> map(Station station) {
    var childStops = station.getChildStops();

    return List.of(
      new KeyValue("gtfsId", station.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(station.getName())),
      new KeyValue(
        "type",
        childStops
          .stream()
          .flatMap(stop -> transitService.getPatternsForStop(stop).stream())
          .map(tripPattern -> tripPattern.getMode().name())
          .distinct()
          .collect(Collectors.joining(","))
      ),
      new KeyValue(
        "stops",
        JSONArray.toJSONString(
          childStops.stream().map(StopLocation::getId).map(FeedScopedId::toString).toList()
        )
      ),
      new KeyValue(
        "routes",
        JSONArray.toJSONString(
          childStops
            .stream()
            .flatMap(stop -> transitService.getRoutesForStop(stop).stream())
            .distinct()
            .map(route ->
              route.getShortName() == null
                ? Map.of("mode", route.getMode().name())
                : Map.of("mode", route.getMode().name(), "shortName", route.getShortName())
            )
            .collect(Collectors.toList())
        )
      )
    );
  }
}
