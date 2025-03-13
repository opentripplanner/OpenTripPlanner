package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLVertexType;
import org.opentripplanner.apis.gtfs.model.StopPosition;
import org.opentripplanner.apis.gtfs.model.StopPosition.PositionAtStop;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.plan.LegCallTime;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

public class PlaceImpl implements GraphQLDataFetchers.GraphQLPlace {

  @Override
  public DataFetcher<LegCallTime> arrival() {
    return environment -> getSource(environment).arrival;
  }

  @Deprecated
  @Override
  public DataFetcher<Long> arrivalTime() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      return stopArrival.arrival.time().toInstant().toEpochMilli();
    };
  }

  @Override
  public DataFetcher<VehicleParking> bikePark() {
    return this::getBikePark;
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      Place place = stopArrival.place;

      if (!place.vertexType.equals(VertexType.VEHICLERENTAL)) {
        return null;
      }

      return place.vehicleRentalPlace;
    };
  }

  @Override
  public DataFetcher<Boolean> canceled() {
    return environment -> getSource(environment).canceled;
  }

  @Override
  public DataFetcher<VehicleParking> carPark() {
    return this::getCarPark;
  }

  @Deprecated
  @Override
  public DataFetcher<LegCallTime> departure() {
    return environment -> getSource(environment).departure;
  }

  @Override
  public DataFetcher<Long> departureTime() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      return stopArrival.departure.time().toInstant().toEpochMilli();
    };
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      return stopArrival.place.coordinate.latitude();
    };
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      return stopArrival.place.coordinate.longitude();
    };
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      return GraphQLUtils.getTranslation(stopArrival.place.name, environment);
    };
  }

  @Override
  public DataFetcher<VehicleRentalVehicle> rentalVehicle() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      Place place = stopArrival.place;

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
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      return stopArrival.place.stop;
    };
  }

  @Override
  public DataFetcher<StopPosition> stopPosition() {
    return environment -> {
      StopArrival stopArrival = getSource(environment);
      var seq = stopArrival.gtfsStopSequence;
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
      StopArrival stopArrival = getSource(environment);
      Place place = stopArrival.place;

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
      StopArrival stopArrival = getSource(environment);
      var place = stopArrival.place;
      return switch (place.vertexType) {
        case NORMAL -> GraphQLVertexType.NORMAL.name();
        case TRANSIT -> GraphQLVertexType.TRANSIT.name();
        case VEHICLERENTAL -> GraphQLVertexType.BIKESHARE.name();
        case VEHICLEPARKING -> GraphQLVertexType.BIKEPARK.name();
      };
    };
  }

  private VehicleParking getBikePark(DataFetchingEnvironment environment) {
    StopArrival stopArrival = getSource(environment);
    var vehicleParkingWithEntrance = stopArrival.place.vehicleParkingWithEntrance;
    if (
      vehicleParkingWithEntrance == null ||
      !vehicleParkingWithEntrance.getVehicleParking().hasBicyclePlaces()
    ) {
      return null;
    }

    return vehicleParkingWithEntrance.getVehicleParking();
  }

  private VehicleParking getCarPark(DataFetchingEnvironment environment) {
    StopArrival stopArrival = getSource(environment);
    var vehicleParkingWithEntrance = stopArrival.place.vehicleParkingWithEntrance;
    if (
      vehicleParkingWithEntrance == null ||
      !vehicleParkingWithEntrance.getVehicleParking().hasAnyCarPlaces()
    ) {
      return null;
    }

    return vehicleParkingWithEntrance.getVehicleParking();
  }

  private VehicleParking getVehicleParking(DataFetchingEnvironment environment) {
    StopArrival stopArrival = getSource(environment);
    var vehicleParkingWithEntrance = stopArrival.place.vehicleParkingWithEntrance;
    if (vehicleParkingWithEntrance == null) {
      return null;
    }

    return vehicleParkingWithEntrance.getVehicleParking();
  }

  private StopArrival getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
