package org.opentripplanner.updater.spi;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.opentripplanner.framework.io.JsonDataListDownloader;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericJsonDataSource<T> implements DataSource<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GenericJsonDataSource.class);
  private final JsonDataListDownloader<T> jsonDataListDownloader;
  private final String url;
  protected List<T> updates = List.of();

  protected GenericJsonDataSource(String url, String jsonParsePath, HttpHeaders headers) {
    this(url, jsonParsePath, headers, new OtpHttpClientFactory().create(LOG));
  }

  protected GenericJsonDataSource(
    String url,
    String jsonParsePath,
    HttpHeaders headers,
    OtpHttpClient otpHttpClient
  ) {
    this.url = url;
    jsonDataListDownloader = new JsonDataListDownloader<>(
      url,
      jsonParsePath,
      this::parseElement,
      headers.asMap(),
      otpHttpClient
    );
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
