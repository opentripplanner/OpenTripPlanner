package org.opentripplanner.openstreetmap.wayproperty;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

class I18nStringDeduplicator {

  private final Map<I18NString, I18NString> creativeNameCache = new HashMap<>();

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
