package org.opentripplanner.ext.vectortiles.layers.areastops;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.I18NStringMapper;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;

public class DebugClientAreaStopPropertyMapper extends PropertyMapper<AreaStop> {

  private final I18NStringMapper i18NStringMapper;

  public DebugClientAreaStopPropertyMapper(TransitService transitService, Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  public static PropertyMapper<AreaStop> create(TransitService transitService, Locale locale) {
    return new DebugClientAreaStopPropertyMapper(transitService, locale);
  }

  @Override
  protected Collection<T2<String, Object>> map(AreaStop input) {
    return List.of(
      new T2<>("id", input.getId().toString()),
      new T2<>("name", i18NStringMapper.mapNonnullToApi(input.getName()))
    );
  }
}
