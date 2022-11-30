package org.opentripplanner.api.model;

import java.util.Date;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.ModeMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

public class ApiRouterInfo {

  private final ApiWorldEnvelope envelope;
  public final boolean hasBikePark;
  public final boolean hasCarPark;
  public final boolean hasVehicleParking;
  public String routerId;
  public Geometry polygon;
  public Date buildTime;
  public long transitServiceStarts;
  public long transitServiceEnds;
  public List<String> transitModes;
  public double centerLatitude;
  public double centerLongitude;
  public boolean hasParkRide;
  public boolean hasBikeSharing;
  public List<ApiTravelOption> travelOptions;

  /** TODO: Do not pass in the graph here, do this in a mapper instead. */
  public ApiRouterInfo(String routerId, Graph graph, TransitService transitService) {
    VehicleRentalService vehicleRentalService = graph.getVehicleRentalService();
    VehicleParkingService vehicleParkingService = graph.getVehicleParkingService();

    this.routerId = routerId;
    this.polygon = graph.getConvexHull();
    this.buildTime = Date.from(graph.buildTime);
    this.transitServiceStarts = transitService.getTransitServiceStarts().toEpochSecond();
    this.transitServiceEnds = transitService.getTransitServiceEnds().toEpochSecond();
    this.transitModes = ModeMapper.mapToApi(transitService.getTransitModes());
    var ev = graph.getEnvelope();
    this.envelope =
      new ApiWorldEnvelope(
        ev.getLowerLeftLatitude(),
        ev.getLowerLeftLongitude(),
        ev.getUpperRightLatitude(),
        ev.getUpperRightLongitude()
      );
    this.hasBikeSharing = mapHasBikeSharing(vehicleRentalService);
    this.hasBikePark = mapHasBikePark(vehicleParkingService);
    this.hasCarPark = mapHasCarPark(vehicleParkingService);
    this.hasParkRide = this.hasCarPark;
    this.hasVehicleParking = mapHasVehicleParking(vehicleParkingService);
    this.travelOptions = ApiTravelOptionsMaker.makeOptions(graph, transitService);
    transitService.getCenter().ifPresentOrElse(this::setCenter, this::calculateCenter);
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
    return service.getBikeParks().findAny().isPresent();
  }

  public boolean mapHasCarPark(VehicleParkingService service) {
    if (service == null) {
      return false;
    }
    return service.getCarParks().findAny().isPresent();
  }

  public boolean mapHasVehicleParking(VehicleParkingService service) {
    if (service == null) {
      return false;
    }
    return service.getVehicleParkings().findAny().isPresent();
  }

  /**
   * Set center coordinate from transit center in {@link TransitModel#calculateTransitCenter()} if transit
   * is used.
   * <p>
   * It is first called when OSM is loaded. Then after transit data is loaded. So that center is set
   * in all combinations of street and transit loading.
   */
  public void setCenter(Coordinate center) {
    //Transit data was loaded and center was calculated with calculateTransitCenter
    centerLongitude = center.x;
    centerLatitude = center.y;
  }

  /**
   * Set center coordinate from mean coordinates of bounding box.
   *
   * @see #setCenter(Coordinate)
   */
  public void calculateCenter() {
    // Does not work around 180th parallel.
    centerLatitude = (getUpperRightLatitude() + getLowerLeftLatitude()) / 2;
    centerLongitude = (getUpperRightLongitude() + getLowerLeftLongitude()) / 2;
  }

  public double getLowerLeftLatitude() {
    return envelope.getLowerLeftLatitude();
  }

  public double getLowerLeftLongitude() {
    return envelope.getLowerLeftLongitude();
  }

  public double getUpperRightLatitude() {
    return envelope.getUpperRightLatitude();
  }

  public double getUpperRightLongitude() {
    return envelope.getUpperRightLongitude();
  }
}
