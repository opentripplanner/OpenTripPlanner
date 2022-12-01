package org.opentripplanner.ext.vehicleparking.hslpark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingGroup;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HslHubsDownloader {

  private static final Logger log = LoggerFactory.getLogger(HslHubsDownloader.class);
  private final String jsonParsePath;
  private final Function<JsonNode, Map<FeedScopedId, VehicleParkingGroup>> hubsParser;
  private String url;
  private static final ObjectMapper mapper = new ObjectMapper();

  public HslHubsDownloader(
    String url,
    String jsonParsePath,
    Function<JsonNode, Map<FeedScopedId, VehicleParkingGroup>> hubsParser
  ) {
    this.url = url;
    this.jsonParsePath = jsonParsePath;
    this.hubsParser = hubsParser;
  }

  public Map<FeedScopedId, VehicleParkingGroup> downloadHubs() {
    if (url == null) {
      log.warn("Cannot download updates, because url is null!");
      return null;
    }

    try (InputStream data = HttpUtils.openInputStream(url, null)) {
      if (data == null) {
        log.warn("Failed to get data from url {}", url);
        return null;
      }
      return parseJSON(data);
    } catch (IllegalArgumentException e) {
      log.warn("Error parsing hubs from {}", url, e);
    } catch (JsonProcessingException e) {
      log.warn("Error parsing hubs from {} (bad JSON of some sort)", url, e);
    } catch (IOException e) {
      log.warn("Error reading hubs from {}", url, e);
    }
    return null;
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

    if (!jsonParsePath.equals("")) {
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
