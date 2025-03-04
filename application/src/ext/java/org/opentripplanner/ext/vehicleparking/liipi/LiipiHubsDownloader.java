package org.opentripplanner.ext.vehicleparking.liipi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiipiHubsDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(LiipiHubsDownloader.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final String jsonParsePath;
  private final Function<JsonNode, Map<FeedScopedId, VehicleParkingGroup>> hubsParser;
  private final String url;
  private final OtpHttpClient otpHttpClient;

  public LiipiHubsDownloader(
    String url,
    String jsonParsePath,
    Function<JsonNode, Map<FeedScopedId, VehicleParkingGroup>> hubsParser,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    this.url = url;
    this.jsonParsePath = jsonParsePath;
    this.hubsParser = hubsParser;
    otpHttpClient = otpHttpClientFactory.create(LOG);
  }

  public Map<FeedScopedId, VehicleParkingGroup> downloadHubs() {
    if (url == null) {
      LOG.warn("Cannot download updates, because url is null!");
      return null;
    }
    try {
      return otpHttpClient.getAndMap(URI.create(url), Map.of(), is -> {
        try {
          return parseJSON(is);
        } catch (IllegalArgumentException e) {
          LOG.warn("Error parsing hubs from {}", url, e);
        } catch (JsonProcessingException e) {
          LOG.warn("Error parsing hubs from {} (bad JSON of some sort)", url, e);
        } catch (IOException e) {
          LOG.warn("Error reading hubs from {}", url, e);
        }
        return null;
      });
    } catch (OtpHttpClientException e) {
      LOG.warn("Failed to get data from url {}", url);
      return null;
    }
  }

  private static String convertStreamToString(java.io.InputStream is) {
    try (java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A")) {
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  private Map<FeedScopedId, VehicleParkingGroup> parseJSON(InputStream dataStream)
    throws IllegalArgumentException, IOException {
    Map<FeedScopedId, VehicleParkingGroup> out = new HashMap<>();

    String hubsString = convertStreamToString(dataStream);

    JsonNode rootNode = mapper.readTree(hubsString);

    if (!jsonParsePath.isEmpty()) {
      String delimiter = "/";
      String[] parseElement = jsonParsePath.split(delimiter);
      for (String s : parseElement) {
        rootNode = rootNode.path(s);
      }

      if (rootNode.isMissingNode()) {
        throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
      }
    }

    for (JsonNode node : rootNode) {
      if (node == null) {
        continue;
      }
      Map<FeedScopedId, VehicleParkingGroup> parsedElement = hubsParser.apply(node);
      if (parsedElement != null) {
        out.putAll(parsedElement);
      }
    }
    return out;
  }
}
