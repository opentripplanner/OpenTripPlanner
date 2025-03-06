package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleParkingsLayerTest {

  private static final FeedScopedId ID = TimetableRepositoryForTest.id("id");

  private VehicleParking vehicleParking;

  @BeforeEach
  public void setUp() {
    var service = new OpeningHoursCalendarService(
      new Deduplicator(),
      LocalDate.of(2022, Month.JANUARY, 1),
      LocalDate.of(2024, Month.DECEMBER, 31)
    );

    // Create a OHCalendarBuilder for each entity with opening hours
    var calBuilder = service.newBuilder(ZoneId.of("Europe/Berlin"));

    // Simple case 08:00- 16:30  April 1st to April 3rd
    calBuilder
      .openingHours("Mo-Fr", LocalTime.of(8, 0), LocalTime.of(16, 30))
      .on(LocalDate.of(2022, Month.APRIL, 1))
      .add();

    vehicleParking = VehicleParking.builder()
      .id(ID)
      .name(
        TranslatedString.getI18NString(
          Map.of("", "default name", "de", "deutscher Name"),
          false,
          false
        )
      )
      .coordinate(new WgsCoordinate(2, 1))
      .bicyclePlaces(true)
      .carPlaces(true)
      .wheelchairAccessibleCarPlaces(false)
      .imageUrl("image")
      .detailsUrl("details")
      .note(TranslatedString.getI18NString("default note", "DE", "deutsche Notiz"))
      .tags(List.of("tag1", "tag2"))
      .openingHoursCalendar(calBuilder.build())
      .state(VehicleParkingState.OPERATIONAL)
      .capacity(VehicleParkingSpaces.builder().bicycleSpaces(5).carSpaces(6).build())
      .availability(
        VehicleParkingSpaces.builder().wheelchairAccessibleCarSpaces(1).bicycleSpaces(1).build()
      )
      .build();
  }

  @Test
  public void vehicleParkingGeometryTest() {
    var repo = new DefaultVehicleParkingRepository();
    repo.updateVehicleParking(List.of(vehicleParking), List.of());

    var config =
      """
      {
        "vectorTiles": {
          "layers" : [
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
      }
      """;
    var nodeAdapter = newNodeAdapterForTest(config);
    var tiles = VectorTileConfig.mapVectorTilesParameters(nodeAdapter, "vectorTiles");
    assertEquals(1, tiles.layers().size());
    var builder = new VehicleParkingsLayerBuilder(
      new DefaultVehicleParkingService(repo),
      tiles.layers().getFirst(),
      Locale.US
    );

    List<Geometry> geometries = builder.getGeometries(new Envelope(0.99, 1.01, 1.99, 2.01));

    assertEquals("[POINT (1 2)]", geometries.toString());
    assertEquals(
      "VehicleParking{id: 'F:id', name: 'default name', coordinate: (2.0, 1.0)}",
      geometries.getFirst().getUserData().toString()
    );
  }

  @Test
  public void stadtnaviVehicleParkingPropertyMapperTest() {
    StadtnaviVehicleParkingPropertyMapper mapper = new StadtnaviVehicleParkingPropertyMapper(
      Locale.GERMANY
    );
    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicleParking).forEach(o -> map.put(o.key(), o.value()));

    assertEquals(ID.toString(), map.get("id").toString());
    assertEquals("deutscher Name", map.get("name").toString());
    assertEquals("details", map.get("detailsUrl").toString());
    assertEquals("image", map.get("imageUrl").toString());
    assertEquals("deutsche Notiz", map.get("note").toString());
    assertEquals("OPERATIONAL", map.get("state").toString());

    assertEquals("Mo-Fr 8:00-16:30", map.get("openingHours"));

    assertTrue((Boolean) map.get("bicyclePlaces"));
    assertTrue((Boolean) map.get("anyCarPlaces"));
    assertTrue((Boolean) map.get("carPlaces"));
    assertFalse((Boolean) map.get("wheelchairAccessibleCarPlaces"));
    assertTrue((Boolean) map.get("realTimeData"));

    assertEquals(
      Set.of("tag1", "tag2"),
      Set.copyOf(Arrays.asList(map.get("tags").toString().split(",")))
    );

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
    mapper.map(vehicleParking).forEach(o -> map.put(o.key(), o.value()));

    assertEquals(ID.toString(), map.get("id").toString());
    assertEquals("default name", map.get("name").toString());

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
    mapper.map(vehicleParking).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("deutscher Name", map.get("name").toString());
  }
}
