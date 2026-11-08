package org.opentripplanner.core.model.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author mabu
 */
public enum ResourceBundleSingleton {
  INSTANCE;

  private static final ResourceBundle.Control CONTROL = ResourceBundle.Control.getControl(
    ResourceBundle.Control.FORMAT_PROPERTIES
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
        resourceBundle = getBundleWithoutFallback("internals", locale);
      } else {
        resourceBundle = getBundleWithoutFallback("WayProperties", locale);
      }
      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /**
   * Load a bundle for the given locale without falling back to the JVM default locale. If no
   * bundle matches the requested language, the root bundle is returned.
   * <p>
   * The {@link ResourceBundle.Control} based {@code getBundle} overloads throw
   * {@link UnsupportedOperationException} when called from a named module, so the no-fallback
   * behavior is done here instead.
   */
  private static ResourceBundle getBundleWithoutFallback(String baseName, Locale locale) {
    var bundle = ResourceBundle.getBundle(baseName, locale);
    // The JDK silently picks the default locale's bundle if the requested one does not exist.
    // Accept the bundle only if it is in the JDK's own candidate list for the requested locale
    // (e.g. nb -> [nb, no, ROOT]).
    if (CONTROL.getCandidateLocales(baseName, locale).contains(bundle.getLocale())) {
      return bundle;
    }
    return ResourceBundle.getBundle(baseName, Locale.ROOT);
  }
}
