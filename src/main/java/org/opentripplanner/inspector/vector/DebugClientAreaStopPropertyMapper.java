package org.opentripplanner.inspector.vector;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.api.mapping.I18NStringMapper;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;

/**
 * A {@link PropertyMapper} for the {@link AreaStopsLayerBuilder} for the OTP debug client.
 */
public class DebugClientAreaStopPropertyMapper extends PropertyMapper<AreaStop> {

  private final I18NStringMapper i18NStringMapper;

  public DebugClientAreaStopPropertyMapper(TransitService transitService, Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  public static PropertyMapper<AreaStop> create(TransitService transitService, Locale locale) {
    return new DebugClientAreaStopPropertyMapper(transitService, locale);
  }

  @Override
  protected Collection<KeyValue> map(AreaStop input) {
    return List.of(
      new KeyValue("id", input.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(input.getName()))
    );
  }
}
