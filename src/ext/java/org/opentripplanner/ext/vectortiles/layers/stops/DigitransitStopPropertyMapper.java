package org.opentripplanner.ext.vectortiles.layers.stops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitStopPropertyMapper extends PropertyMapper<RegularStop> {

  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  private final TransitService transitService;
  private final I18NStringMapper i18NStringMapper;

  DigitransitStopPropertyMapper(TransitService transitService, Locale locale) {
    this.transitService = transitService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  protected static DigitransitStopPropertyMapper create(
    TransitService transitService,
    Locale locale
  ) {
    return new DigitransitStopPropertyMapper(transitService, locale);
  }

  @Override
  protected Collection<KeyValue> map(RegularStop stop) {
    return getBaseKeyValues(stop, i18NStringMapper, transitService);
  }

  protected static Collection<KeyValue> getBaseKeyValues(
    RegularStop stop,
    I18NStringMapper i18NStringMapper,
    TransitService transitService
  ) {
    return List.of(
      new KeyValue("gtfsId", stop.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(stop.getName())),
      new KeyValue("code", stop.getCode()),
      new KeyValue("platform", stop.getPlatformCode()),
      new KeyValue("desc", i18NStringMapper.mapToApi(stop.getDescription())),
      new KeyValue("type", getType(transitService, stop)),
      new KeyValue("routes", getRoutes(transitService, stop)),
      new KeyValue(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : null
      )
    );
  }

  protected static String getRoutes(TransitService transitService, RegularStop stop) {
    try {
      var objects = transitService
        .getRoutesForStop(stop)
        .stream()
        .map(route -> {
          var routeObject = OBJECT_MAPPER.createObjectNode();
          routeObject.put("gtfsType", route.getGtfsType());
          return routeObject;
        })
        .toList();
      return OBJECT_MAPPER.writeValueAsString(objects);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  protected static String getType(TransitService transitService, RegularStop stop) {
    Collection<TripPattern> patternsForStop = transitService.getPatternsForStop(stop);

    return patternsForStop
      .stream()
      .map(TripPattern::getMode)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .map(Enum::name)
      .orElse(null);
  }
}
