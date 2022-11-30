package org.opentripplanner.framework.resources;

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

  static final ResourceBundle.Control noFallbackControl = Control.getNoFallbackControl(
    Control.FORMAT_PROPERTIES
  );
  private final Set<String> internalKeys = Set.of(
    "corner",
    "unnamedStreet",
    "origin",
    "destination",
    "partOf",
    "price.free",
    "price.startMain"
  );

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
      if (internalKeys.contains(key)) {
        resourceBundle = ResourceBundle.getBundle("internals", locale, noFallbackControl);
      } else {
        resourceBundle = ResourceBundle.getBundle("WayProperties", locale, noFallbackControl);
      }
      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }
}
