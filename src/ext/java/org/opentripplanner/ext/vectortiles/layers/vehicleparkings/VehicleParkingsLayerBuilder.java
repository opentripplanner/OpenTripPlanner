package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VehicleParkingsLayerBuilder extends LayerBuilder<VehicleParking> {
    enum MapperType { Digitransit }

    static Map<VehicleParkingsLayerBuilder.MapperType, Function<Graph, PropertyMapper<VehicleParking>>> mappers = Map.of(
            VehicleParkingsLayerBuilder.MapperType.Digitransit, DigitransitVehicleParkingPropertyMapper::create
    );

    private final Graph graph;

    public VehicleParkingsLayerBuilder(Graph graph, VectorTilesResource.LayerParameters layerParameters) {
        super(
                layerParameters.name(),
                mappers.get(VehicleParkingsLayerBuilder.MapperType.valueOf(layerParameters.mapper())).apply(graph)
        );

        this.graph = graph;
    }

    @Override
    protected List<Geometry> getGeometries(Envelope query) {
        VehicleParkingService service = graph.getVehicleParkingService();
        if (service == null) {return List.of();}
        return service.getVehicleParkings()
                .map(vehicleParking -> {
                    Coordinate coordinate = new Coordinate(vehicleParking.getX(), vehicleParking.getY());
                    Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
                    point.setUserData(vehicleParking);
                    return point;
                })
                .collect(Collectors.toList());
    }
}
