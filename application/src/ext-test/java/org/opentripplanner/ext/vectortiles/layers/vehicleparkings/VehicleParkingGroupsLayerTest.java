package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

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
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleParkingGroupsLayerTest {

  private static final FeedScopedId ID = TimetableRepositoryForTest.id("id");

  private VehicleParkingGroup vehicleParkingGroup;
  private VehicleParking vehicleParking;

  @BeforeEach
  public void setUp() {
    vehicleParkingGroup = VehicleParkingGroup.of(ID)
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
    vehicleParking = VehicleParking.builder()
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
    var repository = new DefaultVehicleParkingRepository();
    repository.updateVehicleParking(List.of(vehicleParking), List.of());

    var config =
      """
      {
        "vectorTiles": {
          "layers" :[
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
      }
      """;
    var nodeAdapter = newNodeAdapterForTest(config);
    var tiles = VectorTileConfig.mapVectorTilesParameters(nodeAdapter, "vectorTiles");
    assertEquals(1, tiles.layers().size());
    var builder = new VehicleParkingGroupsLayerBuilderWithPublicGeometry(
      new DefaultVehicleParkingService(repository),
      tiles.layers().get(0),
      Locale.US
    );

    List<Geometry> geometries = builder.getGeometries(new Envelope(0.99, 1.01, 1.99, 2.01));

    assertEquals("[POINT (1.1 1.9)]", geometries.toString());
    assertEquals(
      "VehicleParkingAndGroup[vehicleParkingGroup=VehicleParkingGroup{name: 'groupName', coordinate: (1.9, 1.1)}, vehicleParking=[VehicleParking{id: 'F:id', name: 'name', coordinate: (2.0, 1.0)}]]",
      geometries.getFirst().getUserData().toString()
    );
  }

  @Test
  public void digitransitVehicleParkingGroupPropertyMapperTest() {
    VehicleParkingGroupPropertyMapperWithPublicMap mapper =
      new VehicleParkingGroupPropertyMapperWithPublicMap(Locale.US);
    Map<String, Object> map = new HashMap<>();
    mapper
      .map(new VehicleParkingAndGroup(vehicleParkingGroup, Set.of(vehicleParking)))
      .forEach(o -> map.put(o.key(), o.value()));

    assertEquals(ID.toString(), map.get("id").toString());
    assertEquals("groupName", map.get("name").toString());

    assertEquals(
      "[{\"carPlaces\":true,\"bicyclePlaces\":false,\"id\":\"F:id\",\"name\":\"name\"}]",
      map.get("vehicleParking")
    );
  }

  @Test
  public void digitransitVehicleParkingGroupPropertyMapperTranslationTest() {
    VehicleParkingGroupPropertyMapperWithPublicMap mapper =
      new VehicleParkingGroupPropertyMapperWithPublicMap(new Locale("de"));
    Map<String, Object> map = new HashMap<>();
    mapper
      .map(new VehicleParkingAndGroup(vehicleParkingGroup, Set.of(vehicleParking)))
      .forEach(o -> map.put(o.key(), o.value()));

    assertEquals("groupDE", map.get("name").toString());

    assertEquals(
      "[{\"carPlaces\":true,\"bicyclePlaces\":false,\"id\":\"F:id\",\"name\":\"DE\"}]",
      map.get("vehicleParking")
    );
  }

  private static class VehicleParkingGroupsLayerBuilderWithPublicGeometry
    extends VehicleParkingGroupsLayerBuilder {

    public VehicleParkingGroupsLayerBuilderWithPublicGeometry(
      VehicleParkingService service,
      LayerParameters<VectorTilesResource.LayerType> layerParameters,
      Locale locale
    ) {
      super(service, layerParameters, locale);
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
    public Collection<KeyValue> map(VehicleParkingAndGroup vehicleParkingAndGroup) {
      return super.map(vehicleParkingAndGroup);
    }
  }
}
