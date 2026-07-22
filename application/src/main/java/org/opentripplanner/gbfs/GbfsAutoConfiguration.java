package org.opentripplanner.gbfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GBFS auto-configuration file (gbfs.json) of a GBFS system, together with the url it was
 * fetched from. It declares the GBFS version of the system and lists its feeds.
 */
public class GbfsAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsAutoConfiguration.class);

  // Unknown enum values (e.g. feed names added in newer GBFS versions) must map to null so that
  // the corresponding feeds can be skipped instead of failing the whole file.
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
    true
  );

  private final String url;
  private final JsonNode json;

  private GbfsAutoConfiguration(String url, JsonNode json) {
    this.url = url;
    this.json = json;
  }

  public static GbfsAutoConfiguration fetch(
    String url,
    HttpHeaders httpHeaders,
    OtpHttpClient otpHttpClient
  ) {
    URI uri = toUri(url);
    try {
      return new GbfsAutoConfiguration(
        url,
        otpHttpClient.getAndMapAsJsonNode(uri, httpHeaders, OBJECT_MAPPER)
      );
    } catch (OtpHttpClientException e) {
      LOG.warn("Error fetching GBFS feed from {}. Details: {}.", url, e.getMessage(), e);
      if (!url.endsWith("gbfs.json")) {
        LOG.warn(
          "GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.",
          url
        );
      }
      throw new GbfsConstructionException(
        "Could not fetch the feed auto-configuration file from " + url
      );
    }
  }

  /**
   * The GBFS version declared in the file, or empty if the file does not declare one.
   */
  public Optional<String> version() {
    return JsonUtils.asText(json, "version");
  }

  /**
   * Maps the file onto the model class of the GBFS version it declares.
   */
  public <T> T mapTo(Class<T> clazz) {
    try {
      return OBJECT_MAPPER.treeToValue(json, clazz);
    } catch (JsonProcessingException e) {
      LOG.warn(
        "Error parsing GBFS auto-configuration file from {}. Details: {}.",
        url,
        e.getMessage(),
        e
      );
      throw new GbfsConstructionException(
        "Could not parse the feed auto-configuration file from " + url
      );
    }
  }

  public String url() {
    return url;
  }

  private static URI toUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new GbfsConstructionException("Invalid url " + url);
    }
  }
}
