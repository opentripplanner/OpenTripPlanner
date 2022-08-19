package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.Locale;
import org.opentripplanner.ext.transmodelapi.TransmodelGraphQLUtils;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.scalars.GeoJSONCoordinatesScalar;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;

public class PlanPlaceType {

  public static GraphQLObjectType create(
    GraphQLOutputType bikeRentalStationType,
    GraphQLOutputType rentalVehicleType,
    GraphQLOutputType quayType
  ) {
    return GraphQLObjectType
      .newObject()
      .name("Place")
      .description(
        "Common super class for all places (stop places, quays, car parks, bike parks and bike rental stations )"
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .description(
            "For transit quays, the name of the quay. For points of interest, the name of the POI."
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            Locale locale = TransmodelGraphQLUtils.getLocale(environment);
            return ((Place) environment.getSource()).name.toString(locale);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("vertexType")
          .description(
            "Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit quay) Mostly used for better localization of bike sharing and P+R station names"
          )
          .type(EnumTypes.VERTEX_TYPE)
          .dataFetcher(environment -> ((Place) environment.getSource()).vertexType)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("latitude")
          .description("The latitude of the place.")
          .type(new GraphQLNonNull(Scalars.GraphQLFloat))
          .dataFetcher(environment -> ((Place) environment.getSource()).coordinate.latitude())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("longitude")
          .description("The longitude of the place.")
          .type(new GraphQLNonNull(Scalars.GraphQLFloat))
          .dataFetcher(environment -> ((Place) environment.getSource()).coordinate.longitude())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quay")
          .description("The quay related to the place.")
          .type(quayType)
          .dataFetcher(environment ->
            ((Place) environment.getSource()).stop instanceof RegularStop
              ? ((RegularStop) ((Place) environment.getSource()).stop)
              : null
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("flexibleArea")
          .description("The flexible area related to the place.")
          .type(GeoJSONCoordinatesScalar.getGraphQGeoJSONCoordinatesScalar())
          .dataFetcher(environment ->
            ((Place) environment.getSource()).stop instanceof AreaStop
              ? ((AreaStop) ((Place) environment.getSource()).stop).getGeometry().getCoordinates()
              : null
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikeRentalStation")
          .type(bikeRentalStationType)
          .description("The bike rental station related to the place")
          .dataFetcher(environment ->
            ((Place) environment.getSource()).vertexType.equals(VertexType.VEHICLERENTAL) &&
              ((Place) environment.getSource()).vehicleRentalPlace instanceof VehicleRentalStation
              ? ((Place) environment.getSource()).vehicleRentalPlace
              : null
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("rentalVehicle")
          .type(rentalVehicleType)
          .description("The rental vehicle related to the place")
          .dataFetcher(environment ->
            ((Place) environment.getSource()).vertexType.equals(VertexType.VEHICLERENTAL) &&
              ((Place) environment.getSource()).vehicleRentalPlace instanceof VehicleRentalVehicle
              ? ((Place) environment.getSource()).vehicleRentalPlace
              : null
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("bikePark")
      //                        .type(bikeParkType)
      //                        .description("The bike parking related to the place")
      //                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKEPARK) ?
      //                                index.graph.getService(VehicleRentalStationService.class)
      //                                        .getBikeParks()
      //                                        .stream()
      //                                        .filter(bikePark -> bikePark.id.equals(((Place) environment.getSource()).bikeParkId))
      //                                        .findFirst()
      //                                        .orElse(null)
      //                                : null)
      //                        .build())
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("carPark")
      //                        .type(carParkType)
      //                        .description("The car parking related to the place")
      //                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.PARKANDRIDE) ?
      //                                index.graph.getService(CarParkService.class)
      //                                        .getCarParks()
      //                                        .stream()
      //                                        .filter(carPark -> carPark.id.equals(((Place) environment.getSource()).carParkId))
      //                                        .findFirst()
      //                                        .orElse(null)
      //                                : null)
      //                        .build())
      .build();
  }
}
