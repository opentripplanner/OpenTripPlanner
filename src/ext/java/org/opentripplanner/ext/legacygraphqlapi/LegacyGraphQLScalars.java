package org.opentripplanner.ext.legacygraphqlapi;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

public class LegacyGraphQLScalars {

  public static GraphQLScalarType polylineScalar = GraphQLScalarType
      .newScalar()
      .name("Polyline")
      .description(
          "List of coordinates in an encoded polyline format (see https://developers.google.com/maps/documentation/utilities/polylinealgorithm). The value appears in JSON as a string.")
      .coercing(new Coercing<String, String>() {
        @Override
        public String serialize(Object input) {
          return input == null ? null : input.toString();
        }

        @Override
        public String parseValue(Object input) {
          return serialize(input);
        }

        @Override
        public String parseLiteral(Object input) {
          if (!(input instanceof StringValue)) { return null; }
          return ((StringValue) input).getValue();
        }
      })
      .build();
}
