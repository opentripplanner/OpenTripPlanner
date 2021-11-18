package org.opentripplanner.ext.legacygraphqlapi;

import graphql.language.StringValue;
import graphql.relay.Relay;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
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

    public static GraphQLScalarType geoJsonScalar = GraphQLScalarType
            .newScalar()
            .name("GeoJson")
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
                    if (!(input instanceof StringValue)) {return null;}
                    return ((StringValue) input).getValue();
                }
            })
            .build();

    public static GraphQLScalarType graphQLIDScalar = GraphQLScalarType
      .newScalar()
      .name("ID")
      .coercing(new Coercing<Relay.ResolvedGlobalId, String>() {
        @Override
        public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Relay.ResolvedGlobalId) {
            Relay.ResolvedGlobalId globalId = (Relay.ResolvedGlobalId) dataFetcherResult;
            return new Relay().toGlobalId(globalId.getType(), globalId.getId());
          }
          throw new CoercingSerializeException("Unknown type " + dataFetcherResult.getClass().getSimpleName());
        }

        @Override
        public Relay.ResolvedGlobalId parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof String) {
            return new Relay().fromGlobalId((String) input);
          }
          throw new CoercingParseValueException("Unexpected type " + input.getClass().getSimpleName());
        }

        @Override
        public Relay.ResolvedGlobalId parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof StringValue) {
            return new Relay().fromGlobalId(((StringValue) input).getValue());
          }
          throw new CoercingParseLiteralException("Unexpected type " + input.getClass().getSimpleName());
        }
      })
      .build();
}
