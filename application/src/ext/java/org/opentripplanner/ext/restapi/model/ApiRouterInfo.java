package org.opentripplanner.ext.restapi.model;

import java.util.Date;
import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.restapi.mapping.ModeMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.transit.service.TransitService;

@SuppressWarnings("unused")
public class ApiRouterInfo {

  /** Keep ref to domain object, but avoid exposing it */
  private final WorldEnvelope envelope;
  public final boolean hasBikePark;
  public final boolean hasCarPark;
  public final boolean hasVehicleParking;
  public String routerId;
  public Geometry polygon;
  public Date buildTime;
  public long transitServiceStarts;
  public long transitServiceEnds;
  public List<String> transitModes;
  public boolean hasParkRide;
  public boolean hasBikeSharing;
  public List<ApiTravelOption> travelOptions;

  /** TODO: Do not pass in the graph here, do this in a mapper instead. */
  public ApiRouterInfo(
    String routerId,
    Graph graph,
    TransitService transitService,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    WorldEnvelope envelope
  ) {
    this.routerId = routerId;
    this.polygon = graph.getConvexHull();
    this.buildTime = Date.from(graph.buildTime);
    this.transitServiceStarts = transitService.getTransitServiceStarts().toEpochSecond();
    this.transitServiceEnds = transitService.getTransitServiceEnds().toEpochSecond();
    this.transitModes = ModeMapper.mapToApi(transitService.listTransitModes());
    this.envelope = envelope;
    this.hasBikeSharing = mapHasBikeSharing(vehicleRentalService);
    this.hasBikePark = mapHasBikePark(vehicleParkingService);
    this.hasCarPark = mapHasCarPark(vehicleParkingService);
    this.hasParkRide = this.hasCarPark;
    this.hasVehicleParking = mapHasVehicleParking(vehicleParkingService);
    this.travelOptions = ApiTravelOptionsMaker.makeOptions(
      graph,
      vehicleRentalService,
      vehicleParkingService,
      transitService
    );
  }

  public boolean mapHasBikeSharing(VehicleRentalService service) {
    if (service == null) {
      return false;
    }

    //at least 2 bike sharing stations are needed for useful bike sharing
    return service.getVehicleRentalPlaces().size() > 1;
  }

  public boolean mapHasBikePark(VehicleParkingService service) {
    if (service == null) {
      return false;
    }
    return service.listBikeParks().stream().findAny().isPresent();
  }

  public boolean mapHasCarPark(VehicleParkingService service) {
    if (service == null) {
      return false;
    }
    return service.listCarParks().stream().findAny().isPresent();
  }

  public boolean mapHasVehicleParking(VehicleParkingService service) {
    if (service == null) {
      return false;
    }
    return service.listVehicleParkings().stream().findAny().isPresent();
  }

  public double getLowerLeftLatitude() {
    return envelope.lowerLeft().latitude();
  }

  public double getLowerLeftLongitude() {
    return envelope.lowerLeft().longitude();
  }

  public double getUpperRightLatitude() {
    return envelope.upperRight().latitude();
  }

  public double getUpperRightLongitude() {
    return envelope.upperRight().longitude();
  }

  public double getCenterLatitude() {
    return envelope.center().latitude();
  }

  public double getCenterLongitude() {
    return envelope.center().longitude();
  }
}
