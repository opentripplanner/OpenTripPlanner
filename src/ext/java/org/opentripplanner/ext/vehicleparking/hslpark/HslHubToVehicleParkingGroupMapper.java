package org.opentripplanner.ext.vehicleparking.hslpark;

import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingGroup;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TranslatedString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.geometry.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a HSL Park hub into a {@link VehicleParkingGroup}.
 */
public class HslHubToVehicleParkingGroupMapper {

  private static final Logger log = LoggerFactory.getLogger(
    HslHubToVehicleParkingGroupMapper.class
  );

  private static final GenericGeometryParser GEOMETRY_PARSER = new GenericGeometryParser(
    GeometryUtils.getGeometryFactory()
  );

  private final String feedId;

  public HslHubToVehicleParkingGroupMapper(String feedId) {
    this.feedId = feedId;
  }

  public Map<VehicleParkingGroup, List<FeedScopedId>> parseHub(JsonNode jsonNode) {
    var hubId = HslParkToVehicleParkingMapper.createIdForNode(jsonNode, "id", feedId);
    try {
      Map<String, String> translations = new HashMap<>();
      JsonNode nameNode = jsonNode.path("name");
      nameNode
        .fieldNames()
        .forEachRemaining(lang -> {
          String name = nameNode.path(lang).asText();
          if (!name.equals("")) {
            translations.put(lang, nameNode.path(lang).asText());
          }
        });
      I18NString name = translations.isEmpty()
        ? new NonLocalizedString(hubId.getId())
        : TranslatedString.getI18NString(translations, true, false);
      Geometry geometry = GEOMETRY_PARSER.geometryFromJson(jsonNode.path("location"));
      double x = geometry.getCentroid().getX();
      double y = geometry.getCentroid().getY();
      var vehicleParkingGroup = VehicleParkingGroup
        .builder()
        .withId(hubId)
        .withName(name)
        .withX(x)
        .withY(y)
        .build();
      var vehicleParkingIds = getVehicleParkingIds((ArrayNode) jsonNode.get("facilityIds"), hubId);
      if (vehicleParkingIds == null) {
        return null;
      }

      return Map.of(vehicleParkingGroup, vehicleParkingIds);
    } catch (Exception e) {
      log.warn("Error parsing hub {}", hubId, e);
      return null;
    }
  }

  public List<FeedScopedId> getVehicleParkingIds(ArrayNode facilityIdsNode, FeedScopedId hubId) {
    if (facilityIdsNode == null || !facilityIdsNode.isArray() || facilityIdsNode.isEmpty()) {
      log.warn("Hub {} contained no facilities", hubId);
      return null;
    }
    var vehicleParkingIds = new ArrayList<FeedScopedId>();
    for (JsonNode jsonNode : facilityIdsNode) {
      vehicleParkingIds.add(new FeedScopedId(feedId, jsonNode.asText()));
    }
    return vehicleParkingIds;
  }
}
