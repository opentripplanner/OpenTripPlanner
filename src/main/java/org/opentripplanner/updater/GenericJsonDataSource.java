package org.opentripplanner.updater;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.io.JsonDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericJsonDataSource<T> implements DataSource<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GenericJsonDataSource.class);
  private final JsonDataListDownloader<T> jsonDataListDownloader;
  private final String url;
  protected List<T> updates = List.of();

  public GenericJsonDataSource(String url, String jsonParsePath, Map<String, String> headers) {
    this.url = url;
    jsonDataListDownloader =
      new JsonDataListDownloader<>(url, jsonParsePath, this::parseElement, headers);
  }

  @Override
  public boolean update() {
    List<T> updates = jsonDataListDownloader.download();
    if (updates != null) {
      synchronized (this) {
        this.updates = updates;
      }
      return true;
    }
    LOG.info("Can't update entities from: {}, keeping current list.", url);
    return false;
  }

  @Override
  public List<T> getUpdates() {
    return updates;
  }

  protected abstract T parseElement(JsonNode jsonNode);
}
