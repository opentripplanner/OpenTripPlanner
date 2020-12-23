package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.scalars.GeoJSONCoordinatesScalar;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.VertexType;

public class PlanPlaceType {

  public static GraphQLObjectType create(
      GraphQLOutputType bikeRentalStationType,
      GraphQLOutputType quayType
  ) {
    return GraphQLObjectType
        .newObject()
        .name("Place")
        .description(
            "Common super class for all places (stop places, quays, car parks, bike parks and bike rental stations )")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("name")
            .description(
                "For transit quays, the name of the quay. For points of interest, the name of the POI.")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((Place) environment.getSource()).name)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("vertexType")
            .description(
                "Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit quay) Mostly used for better localization of bike sharing and P+R station names")
            .type(EnumTypes.VERTEX_TYPE)
            .dataFetcher(environment -> ((Place) environment.getSource()).vertexType)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("latitude")
            .description("The latitude of the place.")
            .type(new GraphQLNonNull(Scalars.GraphQLFloat))
            .dataFetcher(environment -> ((Place) environment.getSource()).coordinate.latitude())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("longitude")
            .description("The longitude of the place.")
            .type(new GraphQLNonNull(Scalars.GraphQLFloat))
            .dataFetcher(environment -> ((Place) environment.getSource()).coordinate.longitude())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("quay")
            .description("The quay related to the place.")
            .type(quayType)
            .dataFetcher(environment -> ((Place) environment.getSource()).stopId != null
                ? GqlUtil.getRoutingService(environment)
                .getStopForId(((Place) environment.getSource()).stopId) : null)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("flexibleArea")
            .description("The flexible area related to the place.")
            .type(GeoJSONCoordinatesScalar.getGraphQGeoJSONCoordinatesScalar())
            .dataFetcher(environment -> ((Place) environment.getSource()).stopId != null
                ? GqlUtil.getRoutingService(environment)
                .getLocationById(((Place) environment.getSource()).stopId)
                .getGeometry().getCoordinates()
                : null)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bikeRentalStation")
            .type(bikeRentalStationType)
            .description("The bike rental station related to the place")
            .dataFetcher(environment -> {
              return ((Place) environment.getSource()).vertexType.equals(VertexType.BIKESHARE)
                  ? GqlUtil
                  .getRoutingService(environment)
                  .getBikerentalStationService()
                  .getBikeRentalStations()
                  .stream()
                  .filter(bikeRentalStation -> bikeRentalStation.id.equals(((Place) environment.getSource()).bikeShareId))
                  .findFirst()
                  .orElse(null)
                  : null;
            })
            .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("bikePark")
        //                        .type(bikeParkType)
        //                        .description("The bike parking related to the place")
        //                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKEPARK) ?
        //                                index.graph.getService(BikeRentalStationService.class)
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
