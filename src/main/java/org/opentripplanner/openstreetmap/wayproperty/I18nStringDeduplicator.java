package org.opentripplanner.openstreetmap.wayproperty;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * De-duplicates instances of {@link I18NString}. This is useful for lowering the memory consumption
 * for simple names like "sidewalk" or "service road" that appear several thousand or even million
 * times in a typical graph.
 */
class I18nStringDeduplicator {

  private final Map<I18NString, I18NString> creativeNameCache = new HashMap<>();

  /**
   * If an equal instance of this string exists, use it in order to reduce memory consumption.
   */
  @Nullable
  public I18NString deduplicate(@Nullable I18NString name) {
    if (name == null) {
      return null;
    }
    if (creativeNameCache.containsKey(name)) {
      return creativeNameCache.get(name);
    }
    creativeNameCache.put(name, name);
    return name;
  }
}
