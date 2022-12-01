package org.opentripplanner.ext.vehicleparking.hslpark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingGroup;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HslFacilitiesDownloader {

  private static final Logger log = LoggerFactory.getLogger(HslFacilitiesDownloader.class);
  private final String jsonParsePath;
  private final BiFunction<JsonNode, Map<FeedScopedId, VehicleParkingGroup>, VehicleParking> facilitiesParser;
  private String url;

  private static final ObjectMapper mapper = new ObjectMapper();

  public HslFacilitiesDownloader(
    String url,
    String jsonParsePath,
    BiFunction<JsonNode, Map<FeedScopedId, VehicleParkingGroup>, VehicleParking> facilitiesParser
  ) {
    this.url = url;
    this.jsonParsePath = jsonParsePath;
    this.facilitiesParser = facilitiesParser;
  }

  public List<VehicleParking> downloadFacilities(
    Map<FeedScopedId, VehicleParkingGroup> hubForPark
  ) {
    if (url == null) {
      log.warn("Cannot download updates, because url is null!");
      return null;
    }

    try (InputStream data = HttpUtils.openInputStream(url, null)) {
      if (data == null) {
        log.warn("Failed to get data from url {}", url);
        return null;
      }
      return parseJSON(data, hubForPark);
    } catch (IllegalArgumentException e) {
      log.warn("Error parsing facilities from {}", url, e);
    } catch (JsonProcessingException e) {
      log.warn("Error parsing facilities from {} (bad JSON of some sort)", url, e);
    } catch (IOException e) {
      log.warn("Error reading facilities from {}", url, e);
    }
    return null;
  }

  private static String convertStreamToString(java.io.InputStream is) {
    try (java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A")) {
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  private List<VehicleParking> parseJSON(
    InputStream dataStream,
    Map<FeedScopedId, VehicleParkingGroup> hubForPark
  ) throws IllegalArgumentException, IOException {
    ArrayList<VehicleParking> out = new ArrayList<>();

    String facilitiesString = convertStreamToString(dataStream);

    JsonNode rootNode = mapper.readTree(facilitiesString);

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
      VehicleParking parsedElement = facilitiesParser.apply(node, hubForPark);
      if (parsedElement != null) {
        out.add(parsedElement);
      }
    }
    return out;
  }
}
