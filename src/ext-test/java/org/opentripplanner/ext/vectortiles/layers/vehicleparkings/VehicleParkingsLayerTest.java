package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TranslatedString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleParkingsLayerTest {

  private static final FeedScopedId ID = TransitModelForTest.id("id");

  private VehicleParking vehicleParking;

  @BeforeEach
  public void setUp() {
    vehicleParking =
      VehicleParking
        .builder()
        .id(ID)
        .name(TranslatedString.getI18NString(Map.of("", "name", "de", "DE"), false, false))
        .coordinate(new WgsCoordinate(2, 1))
        .bicyclePlaces(true)
        .carPlaces(true)
        .wheelchairAccessibleCarPlaces(false)
        .imageUrl("image")
        .detailsUrl("details")
        .note(new NonLocalizedString("note"))
        .tags(List.of("tag1", "tag2"))
        // TODO add when openingHours are implemented
        // .openingHours(
        //         RepeatingTimePeriod.parseFromOsmTurnRestriction("Monday", "Friday", "07:30", "09:30"))
        // .feeHours(null)
        .state(VehicleParkingState.OPERATIONAL)
        .capacity(VehicleParkingSpaces.builder().bicycleSpaces(5).carSpaces(6).build())
        .availability(
          VehicleParkingSpaces.builder().wheelchairAccessibleCarSpaces(1).bicycleSpaces(1).build()
        )
        .build();
  }

  @Test
  public void vehicleParkingGeometryTest() {
    Graph graph = new Graph();
    VehicleParkingService service = graph.getVehicleParkingService();
    service.updateVehicleParking(List.of(vehicleParking), List.of());

    var config =
      """
      {
        "vectorTileLayers": [
          {
            "name": "vehicleParking",
            "type": "VehicleParking",
            "mapper": "Stadtnavi",
            "maxZoom": 20,
            "minZoom": 14,
            "cacheMaxSeconds": 60,
            "expansionFactor": 0
          }
        ]
      }
      """;
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.readTree(config);
      var tiles = VectorTileConfig.mapVectorTilesParameters(
        new NodeAdapter(mapper.readTree(config), "vectorTiles"),
        "vectorTileLayers"
      );
      assertEquals(1, tiles.layers().size());
      VehicleParkingsLayerBuilder builder = new VehicleParkingsLayerBuilder(
        graph,
        tiles.layers().get(0),
        Locale.US
      );

      List<Geometry> geometries = builder.getGeometries(new Envelope(0.99, 1.01, 1.99, 2.01));

      assertEquals("[POINT (1 2)]", geometries.toString());
      assertEquals(
        "VehicleParking{name: 'name', coordinate: (2.0, 1.0)}",
        geometries.get(0).getUserData().toString()
      );
    } catch (JacksonException exception) {
      fail(exception.toString());
    }
  }

  @Test
  public void stadtnaviVehicleParkingPropertyMapperTest() {
    StadtnaviVehicleParkingPropertyMapper mapper = new StadtnaviVehicleParkingPropertyMapper();
    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicleParking).forEach(o -> map.put(o.first, o.second));

    assertEquals(ID.toString(), map.get("id").toString());
    assertEquals("name", map.get("name").toString());
    assertEquals("DE", map.get("name.de").toString());
    assertEquals("details", map.get("detailsUrl").toString());
    assertEquals("image", map.get("imageUrl").toString());
    assertEquals("note", map.get("note").toString());
    assertEquals("OPERATIONAL", map.get("state").toString());

    // openingHours, feeHours

    assertTrue((Boolean) map.get("bicyclePlaces"));
    assertTrue((Boolean) map.get("anyCarPlaces"));
    assertTrue((Boolean) map.get("carPlaces"));
    assertFalse((Boolean) map.get("wheelchairAccessibleCarPlaces"));
    assertTrue((Boolean) map.get("realTimeData"));

    assertEquals("tag1,tag2", map.get("tags").toString());

    assertEquals(5, map.get("capacity.bicyclePlaces"));
    assertEquals(6, map.get("capacity.carPlaces"));
    assertNull(map.get("capacity.wheelchairAccessibleCarPlaces"));
    assertEquals(1, map.get("availability.bicyclePlaces"));
    assertNull(map.get("availability.carPlaces"));
    assertEquals(1, map.get("availability.wheelchairAccessibleCarPlaces"));

    assertEquals(
      "{\"bicyclePlaces\":5,\"carPlaces\":6,\"wheelchairAccessibleCarPlaces\":null}",
      map.get("capacity").toString()
    );
    assertEquals(
      "{\"bicyclePlaces\":1,\"carPlaces\":null,\"wheelchairAccessibleCarPlaces\":1}",
      map.get("availability").toString()
    );
  }

  @Test
  public void digitransitVehicleParkingPropertyMapperTest() {
    DigitransitVehicleParkingPropertyMapper mapper = DigitransitVehicleParkingPropertyMapper.create(
      Locale.US
    );
    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicleParking).forEach(o -> map.put(o.first, o.second));

    assertEquals(ID.toString(), map.get("id").toString());
    assertEquals("name", map.get("name").toString());

    assertTrue((Boolean) map.get("bicyclePlaces"));
    assertTrue((Boolean) map.get("anyCarPlaces"));
    assertTrue((Boolean) map.get("carPlaces"));
    assertFalse((Boolean) map.get("wheelchairAccessibleCarPlaces"));
  }

  @Test
  public void digitransitVehicleParkingPropertyMapperTranslationTest() {
    DigitransitVehicleParkingPropertyMapper mapper = DigitransitVehicleParkingPropertyMapper.create(
      new Locale("de")
    );
    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicleParking).forEach(o -> map.put(o.first, o.second));

    assertEquals("DE", map.get("name").toString());
  }
}
