package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLVertexType;
import org.opentripplanner.apis.gtfs.model.StopPosition;
import org.opentripplanner.apis.gtfs.model.StopPosition.PositionAtStop;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

public class PlaceImpl implements GraphQLDataFetchers.GraphQLPlace {

  @Override
  public DataFetcher<Long> arrivalTime() {
    return environment -> getSource(environment).arrival.toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<VehicleParking> bikePark() {
    return this::getBikePark;
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      Place place = getSource(environment).place;

      if (!place.vertexType.equals(VertexType.VEHICLERENTAL)) {
        return null;
      }

      return place.vehicleRentalPlace;
    };
  }

  @Override
  public DataFetcher<VehicleParking> carPark() {
    return this::getCarPark;
  }

  @Override
  public DataFetcher<Long> departureTime() {
    return environment -> getSource(environment).departure.toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).place.coordinate.latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).place.coordinate.longitude();
  }

  @Override
  public DataFetcher<String> name() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).place.name, environment);
  }

  @Override
  public DataFetcher<VehicleRentalVehicle> rentalVehicle() {
    return environment -> {
      Place place = getSource(environment).place;

      if (
        !place.vertexType.equals(VertexType.VEHICLERENTAL) ||
        !(place.vehicleRentalPlace instanceof VehicleRentalVehicle)
      ) {
        return null;
      }

      return (VehicleRentalVehicle) place.vehicleRentalPlace;
    };
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).place.stop;
  }

  @Override
  public DataFetcher<StopPosition> stopPosition() {
    return environment -> {
      var seq = getSource(environment).gtfsStopSequence;
      if (seq != null) {
        return new PositionAtStop(seq);
      } else {
        return null;
      }
    };
  }

  @Override
  public DataFetcher<VehicleParking> vehicleParking() {
    return this::getVehicleParking;
  }

  @Override
  public DataFetcher<VehicleRentalStation> vehicleRentalStation() {
    return environment -> {
      Place place = getSource(environment).place;

      if (
        !place.vertexType.equals(VertexType.VEHICLERENTAL) ||
        !(place.vehicleRentalPlace instanceof VehicleRentalStation)
      ) {
        return null;
      }

      return (VehicleRentalStation) place.vehicleRentalPlace;
    };
  }

  @Override
  public DataFetcher<String> vertexType() {
    return environment -> {
      var place = getSource(environment).place;
      return switch (place.vertexType) {
        case NORMAL -> GraphQLVertexType.NORMAL.name();
        case TRANSIT -> GraphQLVertexType.TRANSIT.name();
        case VEHICLERENTAL -> GraphQLVertexType.BIKESHARE.name();
        case VEHICLEPARKING -> GraphQLVertexType.BIKEPARK.name();
      };
    };
  }

  private VehicleParking getBikePark(DataFetchingEnvironment environment) {
    var vehicleParkingWithEntrance = getSource(environment).place.vehicleParkingWithEntrance;
    if (
      vehicleParkingWithEntrance == null ||
      !vehicleParkingWithEntrance.getVehicleParking().hasBicyclePlaces()
    ) {
      return null;
    }

    return vehicleParkingWithEntrance.getVehicleParking();
  }

  private VehicleParking getCarPark(DataFetchingEnvironment environment) {
    var vehicleParkingWithEntrance = getSource(environment).place.vehicleParkingWithEntrance;
    if (
      vehicleParkingWithEntrance == null ||
      !vehicleParkingWithEntrance.getVehicleParking().hasAnyCarPlaces()
    ) {
      return null;
    }

    return vehicleParkingWithEntrance.getVehicleParking();
  }

  private VehicleParking getVehicleParking(DataFetchingEnvironment environment) {
    var vehicleParkingWithEntrance = getSource(environment).place.vehicleParkingWithEntrance;
    if (vehicleParkingWithEntrance == null) {
      return null;
    }

    return vehicleParkingWithEntrance.getVehicleParking();
  }

  private StopArrival getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
