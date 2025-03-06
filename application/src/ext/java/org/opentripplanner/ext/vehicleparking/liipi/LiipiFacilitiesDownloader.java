package org.opentripplanner.ext.vehicleparking.liipi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiipiFacilitiesDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(LiipiFacilitiesDownloader.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final String jsonParsePath;
  private final BiFunction<
    JsonNode,
    Map<FeedScopedId, VehicleParkingGroup>,
    VehicleParking
  > facilitiesParser;
  private final String url;
  private final OtpHttpClient otpHttpClient;

  public LiipiFacilitiesDownloader(
    String url,
    String jsonParsePath,
    BiFunction<JsonNode, Map<FeedScopedId, VehicleParkingGroup>, VehicleParking> facilitiesParser,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    this.url = url;
    this.jsonParsePath = jsonParsePath;
    this.facilitiesParser = facilitiesParser;
    this.otpHttpClient = otpHttpClientFactory.create(LOG);
  }

  public List<VehicleParking> downloadFacilities(
    Map<FeedScopedId, VehicleParkingGroup> hubForPark
  ) {
    if (url == null) {
      LOG.warn("Cannot download updates, because url is null!");
      return null;
    }

    try {
      return otpHttpClient.getAndMap(URI.create(url), Map.of(), is -> {
        try {
          return parseJSON(is, hubForPark);
        } catch (IllegalArgumentException e) {
          LOG.warn("Error parsing facilities from {}", url, e);
        } catch (JsonProcessingException e) {
          LOG.warn("Error parsing facilities from {} (bad JSON of some sort)", url, e);
        } catch (IOException e) {
          LOG.warn("Error reading facilities from {}", url, e);
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

  private List<VehicleParking> parseJSON(
    InputStream dataStream,
    Map<FeedScopedId, VehicleParkingGroup> hubForPark
  ) throws IllegalArgumentException, IOException {
    ArrayList<VehicleParking> out = new ArrayList<>();

    String facilitiesString = convertStreamToString(dataStream);

    JsonNode rootNode = mapper.readTree(facilitiesString);

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
      VehicleParking parsedElement = facilitiesParser.apply(node, hubForPark);
      if (parsedElement != null) {
        out.add(parsedElement);
      }
    }
    return out;
  }
}
