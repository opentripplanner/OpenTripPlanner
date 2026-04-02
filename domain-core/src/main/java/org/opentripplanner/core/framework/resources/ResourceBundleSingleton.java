package org.opentripplanner.core.framework.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Set;

/**
 * @author mabu
 */
public enum ResourceBundleSingleton {
  INSTANCE;

  static final ResourceBundle.Control NO_FALLBACK_CONTROL = Control.getNoFallbackControl(
    Control.FORMAT_PROPERTIES
  );
  private static final Set<String> INTERNAL_KEYS = ResourceBundle.getBundle("internals").keySet();

  //in singleton because resurce bundles are cached based on calling class
  //http://java2go.blogspot.com/2010/03/dont-be-smart-never-implement-resource.html
  public String localize(String key, Locale locale) {
    if (key == null) {
      return null;
    }
    if (locale == null) {
      locale = Locale.ROOT;
    }
    try {
      ResourceBundle resourceBundle;
      if (INTERNAL_KEYS.contains(key)) {
        resourceBundle = ResourceBundle.getBundle("internals", locale, NO_FALLBACK_CONTROL);
      } else {
        resourceBundle = ResourceBundle.getBundle("WayProperties", locale, NO_FALLBACK_CONTROL);
      }
      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }
}
