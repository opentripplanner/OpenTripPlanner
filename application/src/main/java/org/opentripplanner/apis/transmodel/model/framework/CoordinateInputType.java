package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.framework.geometry.WgsCoordinate;

@SuppressWarnings("unchecked")
public class CoordinateInputType {

  /* Constants are package local to be used in unit-tests */
  static final String LATITUDE = "latitude";
  static final String LONGITUDE = "longitude";

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("InputCoordinates")
    .description("Input type for coordinates in the WGS84 system")
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name(LATITUDE)
        .description("The latitude of the place.")
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name(LONGITUDE)
        .description("The longitude of the place.")
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .build();

  public static Optional<WgsCoordinate> mapToWgsCoordinate(
    String fieldName,
    Map<String, Object> input
  ) {
    Map<String, Object> coordinate = (Map<String, Object>) input.get(fieldName);

    if (coordinate == null) {
      return Optional.empty();
    }

    return Optional.of(
      new WgsCoordinate((Double) coordinate.get(LATITUDE), (Double) coordinate.get(LONGITUDE))
    );
  }

  public static Map<String, Object> mapForTest(WgsCoordinate coordinate) {
    return Map.ofEntries(
      Map.entry(LATITUDE, coordinate.latitude()),
      Map.entry(LONGITUDE, coordinate.longitude())
    );
  }
}
