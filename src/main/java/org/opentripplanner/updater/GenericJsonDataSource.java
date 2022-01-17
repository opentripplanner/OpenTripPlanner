package org.opentripplanner.updater;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.opentripplanner.util.xml.JsonDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericJsonDataSource<T> implements DataSource<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GenericJsonDataSource.class);

  private String url;
  private final JsonDataListDownloader<T> jsonDataListDownloader;

  protected List<T> updates = List.of();

  public GenericJsonDataSource(String url, String jsonParsePath, Map<String, String> headers) {
    this.url = url;
    jsonDataListDownloader = new JsonDataListDownloader<>(url, jsonParsePath, this::parseElement, headers);
  }

  public GenericJsonDataSource(String url, String jsonParsePath) {
    this(url, jsonParsePath, null);
  }

  protected abstract T parseElement(JsonNode jsonNode);

  @Override
  public boolean update() {
    List<T> updates = jsonDataListDownloader.download();
    if (updates != null) {
      synchronized(this) {
        this.updates = updates;
      }
      return true;
    }
    LOG.info("Can't update entities from: " + url + ", keeping current list.");
    return false;
  }

  @Override
  public List<T> getUpdates() {
    return updates;
  }

  public void setUrl(String url) {
    this.url = url;
    this.jsonDataListDownloader.setUrl(url);
  }
}
