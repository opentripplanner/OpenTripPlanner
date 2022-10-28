package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingGroup;
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

public class VehicleParkingGroupsLayerTest {

  private static final FeedScopedId ID = TransitModelForTest.id("id");

  private VehicleParkingGroup vehicleParkingGroup;
  private VehicleParking vehicleParking;

  @BeforeEach
  public void setUp() {
    vehicleParkingGroup =
      VehicleParkingGroup
        .of(ID)
        .withName(
          TranslatedString.getI18NString(
            new HashMap<>() {
              {
                put(null, "groupName");
                put("de", "groupDE");
              }
            },
            false,
            false
          )
        )
        .withCoordinate(new WgsCoordinate(1.9, 1.1))
        .build();
    vehicleParking =
      VehicleParking
        .builder()
        .id(ID)
        .name(
          TranslatedString.getI18NString(
            new HashMap<>() {
              {
                put(null, "name");
                put("de", "DE");
              }
            },
            false,
            false
          )
        )
        .coordinate(new WgsCoordinate(2, 1))
        .bicyclePlaces(false)
        .carPlaces(true)
        .wheelchairAccessibleCarPlaces(false)
        .imageUrl("image")
        .detailsUrl("details")
        .note(new NonLocalizedString("note"))
        .tags(List.of("tag1", "tag2"))
        .state(VehicleParkingState.OPERATIONAL)
        .capacity(VehicleParkingSpaces.builder().bicycleSpaces(5).carSpaces(6).build())
        .availability(
          VehicleParkingSpaces.builder().wheelchairAccessibleCarSpaces(1).bicycleSpaces(1).build()
        )
        .vehicleParkingGroup(vehicleParkingGroup)
        .build();
  }

  @Test
  public void vehicleParkingGroupGeometryTest() {
    Graph graph = new Graph();
    VehicleParkingService service = graph.getVehicleParkingService();
    service.updateVehicleParking(List.of(vehicleParking), List.of());

    var config =
      """
      {
        "vectorTileLayers": [
          {
            "name": "vehicleParkingGroups",
            "type": "VehicleParkingGroup",
            "mapper": "Digitransit",
            "maxZoom": 20,
            "minZoom": 14,
            "cacheMaxSeconds": 600,
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
      VehicleParkingGroupsLayerBuilderWithPublicGeometry builder = new VehicleParkingGroupsLayerBuilderWithPublicGeometry(
        graph,
        tiles.layers().get(0),
        Locale.US
      );

      List<Geometry> geometries = builder.getGeometries(new Envelope(0.99, 1.01, 1.99, 2.01));

      assertEquals("[POINT (1.1 1.9)]", geometries.toString());
      assertEquals(
        "VehicleParkingAndGroup[vehicleParkingGroup=VehicleParkingGroup{name: 'groupName', coordinate: (1.9, 1.1)}, vehicleParking=[VehicleParking{name: 'name', coordinate: (2.0, 1.0)}]]",
        geometries.get(0).getUserData().toString()
      );
    } catch (JacksonException exception) {
      fail(exception.toString());
    }
  }

  @Test
  public void digitransitVehicleParkingGroupPropertyMapperTest() {
    VehicleParkingGroupPropertyMapperWithPublicMap mapper = new VehicleParkingGroupPropertyMapperWithPublicMap(
      Locale.US
    );
    Map<String, Object> map = new HashMap<>();
    mapper
      .map(new VehicleParkingAndGroup(vehicleParkingGroup, Set.of(vehicleParking)))
      .forEach(o -> map.put(o.first, o.second));

    assertEquals(ID.toString(), map.get("id").toString());
    assertEquals("groupName", map.get("name").toString());

    assertEquals(
      "[{\"bicyclePlaces\":false,\"carPlaces\":true,\"name\":\"name\",\"id\":\"F:id\"}]",
      map.get("vehicleParking")
    );
  }

  @Test
  public void digitransitVehicleParkingGroupPropertyMapperTranslationTest() {
    VehicleParkingGroupPropertyMapperWithPublicMap mapper = new VehicleParkingGroupPropertyMapperWithPublicMap(
      new Locale("de")
    );
    Map<String, Object> map = new HashMap<>();
    mapper
      .map(new VehicleParkingAndGroup(vehicleParkingGroup, Set.of(vehicleParking)))
      .forEach(o -> map.put(o.first, o.second));

    assertEquals("groupDE", map.get("name").toString());

    assertEquals(
      "[{\"bicyclePlaces\":false,\"carPlaces\":true,\"name\":\"DE\",\"id\":\"F:id\"}]",
      map.get("vehicleParking")
    );
  }

  private static class VehicleParkingGroupsLayerBuilderWithPublicGeometry
    extends VehicleParkingGroupsLayerBuilder {

    public VehicleParkingGroupsLayerBuilderWithPublicGeometry(
      Graph graph,
      VectorTilesResource.LayerParameters layerParameters,
      Locale locale
    ) {
      super(graph, layerParameters, locale);
    }

    @Override
    public List<Geometry> getGeometries(Envelope query) {
      return super.getGeometries(query);
    }
  }

  private static class VehicleParkingGroupPropertyMapperWithPublicMap
    extends DigitransitVehicleParkingGroupPropertyMapper {

    public VehicleParkingGroupPropertyMapperWithPublicMap(Locale locale) {
      super(locale);
    }

    @Override
    public Collection<T2<String, Object>> map(VehicleParkingAndGroup vehicleParkingAndGroup) {
      return super.map(vehicleParkingAndGroup);
    }
  }
}
