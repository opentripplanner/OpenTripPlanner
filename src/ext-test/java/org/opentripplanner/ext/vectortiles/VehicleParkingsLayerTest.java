package org.opentripplanner.ext.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.layers.vehicleparkings.DigitransitVehicleParkingPropertyMapper;
import org.opentripplanner.ext.vectortiles.layers.vehicleparkings.VehicleParkingsLayerBuilder;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;

public class VehicleParkingsLayerTest {

    private static class VehicleParkingsLayerBuilderWithPublicGeometry extends VehicleParkingsLayerBuilder {
        public VehicleParkingsLayerBuilderWithPublicGeometry(Graph graph, VectorTilesResource.LayerParameters layerParameters) {
            super(graph, layerParameters);
        }

        @Override
        public List<Geometry> getGeometries(Envelope query) {
            return super.getGeometries(query);
        }
    }

    private VehicleParking vehicleParking;

    @Before
    public void setUp() {
        vehicleParking = VehicleParking.builder()
                .id(new FeedScopedId("id", "id"))
                .name(TranslatedString.getI18NString(Map.of("", "name", "de", "DE")))
                .x(1)
                .y(2)
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
                .availability(VehicleParkingSpaces.builder().wheelchairAccessibleCarSpaces(1).bicycleSpaces(1).build())
                .build();
    }

    @Test
    public void vehicleParkingGeometryTest() {
        VehicleParkingService service = mock(VehicleParkingService.class);
        when(service.getVehicleParkings()).thenReturn(List.of(vehicleParking).stream());

        Graph graph = mock(Graph.class);
        when(graph.getVehicleParkingService()).thenReturn(service);

        VehicleParkingsLayerBuilderWithPublicGeometry builder = new VehicleParkingsLayerBuilderWithPublicGeometry(graph,
                new VectorTilesResource.LayerParameters() {
                    @Override public String name() { return "vehicleparkings"; }
                    @Override public String type() { return "VehicleParking"; }
                    @Override public String mapper() { return "Digitransit"; }
                    @Override public int maxZoom() { return 20; }
                    @Override public int minZoom() { return 14; }
                    @Override public int cacheMaxSeconds() { return 60; }
                    @Override public double expansionFactor() { return 0; }
                }
        );

        List<Geometry> geometries = builder.getGeometries(new Envelope(0.99, 1.01, 1.99, 2.01));

        assertEquals("[POINT (1 2)]", geometries.toString());
        assertEquals("VehicleParking(name at 2.000000, 1.000000)", geometries.get(0).getUserData().toString());
    }

    private static class VehicleParkingPropertyMapperWithPublicMap extends DigitransitVehicleParkingPropertyMapper {
        public VehicleParkingPropertyMapperWithPublicMap() { super(); }

        @Override
        public Collection<T2<String, Object>> map(VehicleParking vehicleParking) {
            return super.map(vehicleParking);
        }
    }

    @Test
    public void digitransitVehicleParkingPropertyMapperTest() {
        VehicleParkingPropertyMapperWithPublicMap mapper = new VehicleParkingPropertyMapperWithPublicMap();
        Map<String, Object> map = new HashMap<>();
        mapper.map(vehicleParking).forEach(o -> map.put(o.first, o.second));

        assertEquals("id:id", map.get("id").toString());
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

        assertEquals("{\"bicyclePlaces\":5,\"carPlaces\":6,\"wheelchairAccessibleCarPlaces\":null}",
                map.get("capacity").toString());
        assertEquals("{\"bicyclePlaces\":1,\"carPlaces\":null,\"wheelchairAccessibleCarPlaces\":1}",
                map.get("availability").toString());
    }
}
