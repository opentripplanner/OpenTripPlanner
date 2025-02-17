package org.opentripplanner.ext.vehicleparking.liipi;

import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a Liipi Park hub into a {@link VehicleParkingGroup}.
 */
public class LiipiHubToVehicleParkingGroupMapper {

  private static final Logger log = LoggerFactory.getLogger(
    LiipiHubToVehicleParkingGroupMapper.class
  );

  private static final GenericGeometryParser GEOMETRY_PARSER = new GenericGeometryParser(
    GeometryUtils.getGeometryFactory()
  );

  private final String feedId;

  public LiipiHubToVehicleParkingGroupMapper(String feedId) {
    this.feedId = feedId;
  }

  public Map<FeedScopedId, VehicleParkingGroup> parseHub(JsonNode jsonNode) {
    var hubId = LiipiParkToVehicleParkingMapper.createIdForNode(jsonNode, "id", feedId);
    try {
      Map<String, String> translations = new HashMap<>();
      JsonNode nameNode = jsonNode.path("name");
      nameNode
        .fieldNames()
        .forEachRemaining(lang -> {
          String name = nameNode.path(lang).asText();
          if (!name.isEmpty()) {
            translations.put(lang, nameNode.path(lang).asText());
          }
        });
      I18NString name = translations.isEmpty()
        ? new NonLocalizedString(hubId.getId())
        : TranslatedString.getI18NString(translations, true, false);
      Geometry geometry = GEOMETRY_PARSER.geometryFromJson(jsonNode.path("location"));
      var vehicleParkingGroup = VehicleParkingGroup
        .of(hubId)
        .withName(name)
        .withCoordinate(new WgsCoordinate(geometry.getCentroid()))
        .build();
      var vehicleParkingIds = getVehicleParkingIds((ArrayNode) jsonNode.get("facilityIds"), hubId);
      if (vehicleParkingIds == null) {
        return null;
      }

      var hubForPark = new HashMap<FeedScopedId, VehicleParkingGroup>();

      vehicleParkingIds.forEach(vehicleParkingId ->
        hubForPark.put(vehicleParkingId, vehicleParkingGroup)
      );

      return hubForPark;
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
