package org.opentripplanner.inspector.vector.stop;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A {@link PropertyMapper} for the {@link StopLocationPropertyMapper} for the OTP debug client.
 */
public class StopLocationPropertyMapper extends PropertyMapper<StopLocation> {

  private final I18NStringMapper i18NStringMapper;

  public StopLocationPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(StopLocation stop) {
    return List.of(
      kv("name", i18NStringMapper.mapToApi(stop.getName())),
      kv("id", stop.getId().toString()),
      kv("parentId", stop.isPartOfStation() ? stop.getParentStation().getId().toString() : null)
    );
  }
}
