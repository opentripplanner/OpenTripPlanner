package org.opentripplanner.framework.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author mabu
 */
public enum ResourceBundleSingleton {
  INSTANCE;

  //in singleton because resource bundles are cached based on calling class
  //http://java2go.blogspot.com/2010/03/dont-be-smart-never-implement-resource.html
  public String localize(String key, Locale locale) {
    if (key == null) {
      return null;
    }
    if (locale == null) {
      locale = Locale.ROOT;
    }
    try {
      ResourceBundle resourceBundle = ResourceBundle.getBundle(
        "translations",
        locale,
        new XmlResourceBundleControl()
      );
      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }
}
