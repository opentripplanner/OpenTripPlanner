package org.opentripplanner.framework.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of Properties is to easily read a ResourceBundle (set of localized .properties
 * files), and get the named contents. Goes really well with an enumerated type (@see
 * org.opentripplanner.api.ws.Message)
 */
public class ResourceBundleAdaptor {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceBundleAdaptor.class);

  private final String bundle;

  public ResourceBundleAdaptor(Class<?> c) {
    bundle = c.getSimpleName();
  }

  public synchronized String get(String name, Locale l) throws Exception {
    ResourceBundle rb = getBundle(bundle, l);
    return rb.getString(name);
  }

  /**
   * static .properties resource loader will first look for a resource
   * org.opentripplaner.blah.blah.blah.ClassName.properties. if that doesn't work, it searches for
   * ClassName.properties.
   */
  private static ResourceBundle getBundle(String name, Locale l) {
    try {
      return ResourceBundle.getBundle(name, l);
    } catch (MissingResourceException e) {
      LOG.error(
        "Uh oh...no .properties file could be found, so things are most definitely not going to turn out well!!!",
        e
      );
      throw e;
    }
  }
}
