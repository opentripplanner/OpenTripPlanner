package org.opentripplanner.framework.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Custom control for reading XML resource bundles. It's pretty strange that this doesn't already
 * exist.
 */
class XMLResourceBundleControl extends ResourceBundle.Control {

  private static final List<String> XML = List.of("xml");

  @Override
  public ResourceBundle newBundle(
    String baseName,
    Locale locale,
    String format,
    ClassLoader loader,
    boolean reload
  ) throws java.io.IOException {
    String resourceName = toResourceName(toBundleName(baseName, locale), "xml");
    InputStream stream = loader.getResourceAsStream(resourceName);
    if (stream == null) {
      return null;
    }

    return new XMLResourceBundle(stream);
  }

  @Override
  public List<String> getFormats(String baseName) {
    return XML;
  }

  /**
   * Custom implementation of a resource bundle that reads the standard Java XML properties format.
   * It's pretty strange that this doesn't exist already.
   */
  static class XMLResourceBundle extends ResourceBundle {

    private final Properties props;

    public XMLResourceBundle(InputStream stream) throws IOException {
      props = new Properties();
      props.loadFromXML(stream);
    }

    @Override
    protected Object handleGetObject(String key) {
      return props.getProperty(key);
    }

    @Override
    public Enumeration<String> getKeys() {
      Set<String> handleKeys = props.stringPropertyNames();
      return Collections.enumeration(handleKeys);
    }
  }
}
