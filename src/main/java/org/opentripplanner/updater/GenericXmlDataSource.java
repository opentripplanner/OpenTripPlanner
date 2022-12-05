package org.opentripplanner.updater;

import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.io.XmlDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericXmlDataSource<T> implements DataSource<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GenericXmlDataSource.class);

  private String url;

  protected List<T> updates = List.of();

  private final XmlDataListDownloader<T> xmlDownloader;

  /**
   * Initialize GenericXmlDataSource.
   *
   * @param url URL to fetch XML file
   * @param xpath XPath to list elements in XML file
   */
  public GenericXmlDataSource(String url, String xpath) {
    this.url = url;
    this.xmlDownloader = new XmlDataListDownloader<>();
    this.xmlDownloader.setPath(xpath);
    this.xmlDownloader.setDataFactory(attributes -> parseElement(attributes));
  }

  /**
   * Fetch current data about given type and availability from this source.
   *
   * @return true if this operation may have changed something in the list of types.
   */
  @Override
  public boolean update() {
    List<T> updates = xmlDownloader.download(url, false);
    if (updates != null) {
      synchronized (this) {
        this.updates = updates;
      }
      return true;
    }
    LOG.info("Can't update entities from: {}, keeping current list.", url);
    return false;
  }

  /**
   * @return a List of all currently known objects. The updater will use this to update the Graph.
   */
  @Override
  public List<T> getUpdates() {
    return updates;
  }

  protected abstract T parseElement(Map<String, String> attributes);
}
