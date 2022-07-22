package org.opentripplanner.util.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * @author mabu
 */
public enum ResourceBundleSingleton {
  INSTANCE;

  static final ResourceBundle.Control noFallbackControl = Control.getNoFallbackControl(
    Control.FORMAT_PROPERTIES
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
      ResourceBundle resourceBundle = null;
      if (
        key.equals("corner") ||
        key.equals("unnamedStreet") ||
        key.equals("origin") ||
        key.equals("destination") ||
        key.equals("partOf")
      ) {
        resourceBundle = ResourceBundle.getBundle("internals", locale, noFallbackControl);
      } else {
        resourceBundle = ResourceBundle.getBundle("WayProperties", locale, noFallbackControl);
      }
      //LOG.debug(String.format("Localized '%s' using '%s'", key, retval));
      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      //LOG.debug("Missing translation for key: " + key);
      return key;
    }
  }
}
