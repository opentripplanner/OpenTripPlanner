package org.opentripplanner.ext.vectortiles.layers.stations;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitStationPropertyMapper extends PropertyMapper<Station> {

  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
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
    try {
      var childStops = station.getChildStops();
      return List.of(
        kv("gtfsId", station.getId()),
        kv("code", station.getCode()),
        new KeyValue("name", i18NStringMapper.mapNonnullToApi(station.getName())),
        new KeyValue(
          "type",
          childStops
            .stream()
            .flatMap(stop -> transitService.findPatterns(stop).stream())
            .map(tripPattern -> tripPattern.getMode().name())
            .distinct()
            .collect(Collectors.joining(","))
        ),
        new KeyValue(
          "stops",
          OBJECT_MAPPER.writeValueAsString(
            childStops.stream().map(StopLocation::getId).map(FeedScopedId::toString).toList()
          )
        ),
        new KeyValue(
          "routes",
          OBJECT_MAPPER.writeValueAsString(
            childStops
              .stream()
              .flatMap(stop -> transitService.findRoutes(stop).stream())
              .distinct()
              .map(route -> {
                var obj = OBJECT_MAPPER.createObjectNode();
                obj.put("mode", route.getMode().name());
                obj.put("gtfsType", route.getGtfsType());
                if (route.getShortName() != null) {
                  obj.put("shortName", route.getShortName());
                }
                return obj;
              })
              .toList()
          )
        )
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
