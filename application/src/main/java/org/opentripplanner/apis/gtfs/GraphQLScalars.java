package org.opentripplanner.apis.gtfs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.relay.Relay;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.graphql.scalar.DateScalarFactory;
import org.opentripplanner.framework.graphql.scalar.DurationScalarFactory;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.utils.time.OffsetDateTimeParser;

public class GraphQLScalars {

  private static final ObjectMapper GEOJSON_MAPPER = ObjectMappers.geoJson();
  public static final GraphQLScalarType DURATION_SCALAR =
    DurationScalarFactory.createDurationScalar();

  public static final GraphQLScalarType POLYLINE_SCALAR = GraphQLScalarType.newScalar()
    .name("Polyline")
    .description(
      "List of coordinates in an encoded polyline format (see https://developers.google.com/maps/documentation/utilities/polylinealgorithm). The value appears in JSON as a string."
    )
    .coercing(
      new Coercing<String, String>() {
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
          if (!(input instanceof StringValue)) {
            return null;
          }
          return ((StringValue) input).getValue();
        }
      }
    )
    .build();

  public static final GraphQLScalarType OFFSET_DATETIME_SCALAR = GraphQLScalarType.newScalar()
    .name("OffsetDateTime")
    .coercing(
      new Coercing<OffsetDateTime, String>() {
        @Override
        public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof ZonedDateTime zdt) {
            return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
          } else if (dataFetcherResult instanceof OffsetDateTime odt) {
            return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
          } else {
            throw new CoercingSerializeException(
              "Cannot serialize object of class %s".formatted(
                  dataFetcherResult.getClass().getSimpleName()
                )
            );
          }
        }

        @Override
        public OffsetDateTime parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof CharSequence cs) {
            try {
              return OffsetDateTimeParser.parseLeniently(cs);
            } catch (ParseException e) {
              int errorOffset = e.getErrorOffset();
              throw new CoercingParseValueException(
                "Cannot parse %s into an OffsetDateTime. Error at character index %s".formatted(
                    input,
                    errorOffset
                  )
              );
            }
          }
          throw new CoercingParseValueException(
            "Cannot parse %s into an OffsetDateTime. Must be a string."
          );
        }

        @Override
        public OffsetDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof StringValue sv) {
            try {
              return OffsetDateTimeParser.parseLeniently(sv.getValue());
            } catch (ParseException e) {
              throw new CoercingSerializeException();
            }
          }
          throw new CoercingParseLiteralException();
        }
      }
    )
    .build();

  public static final GraphQLScalarType COORDINATE_VALUE_SCALAR = GraphQLScalarType.newScalar()
    .name("CoordinateValue")
    .coercing(
      new Coercing<Double, Double>() {
        private static final String VALIDATION_ERROR_MESSAGE = "Not a valid WGS84 coordinate value";

        @Override
        public Double serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Double doubleValue) {
            return doubleValue;
          } else if (dataFetcherResult instanceof Float floatValue) {
            return floatValue.doubleValue();
          } else {
            throw new CoercingSerializeException(
              "Cannot serialize object of class %s as a coordinate number".formatted(
                  dataFetcherResult.getClass().getSimpleName()
                )
            );
          }
        }

        @Override
        public Double parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof Double doubleValue) {
            return validateCoordinate(doubleValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          if (input instanceof Integer intValue) {
            return validateCoordinate(intValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseValueException(
            "Expected a number, got %s %s".formatted(input.getClass().getSimpleName(), input)
          );
        }

        @Override
        public Double parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof FloatValue coordinate) {
            return validateCoordinate(coordinate.getValue().doubleValue()).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          if (input instanceof IntValue coordinate) {
            return validateCoordinate(coordinate.getValue().doubleValue()).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseLiteralException(
            "Expected a number, got: " + input.getClass().getSimpleName()
          );
        }

        private static Optional<Double> validateCoordinate(double coordinate) {
          if (coordinate >= -180.001 && coordinate <= 180.001) {
            return Optional.of(coordinate);
          }
          return Optional.empty();
        }
      }
    )
    .build();

  public static final GraphQLScalarType COST_SCALAR = GraphQLScalarType.newScalar()
    .name("Cost")
    .coercing(
      new Coercing<Cost, Integer>() {
        private static final int MAX_COST = 1000000;
        private static final String VALIDATION_ERROR_MESSAGE =
          "Cost cannot be negative or greater than %d".formatted(MAX_COST);

        @Override
        public Integer serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Integer intValue) {
            return intValue;
          } else if (dataFetcherResult instanceof Cost costValue) {
            return costValue.toSeconds();
          } else {
            throw new CoercingSerializeException(
              "Cannot serialize object of class %s as a cost".formatted(
                  dataFetcherResult.getClass().getSimpleName()
                )
            );
          }
        }

        @Override
        public Cost parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof Integer intValue) {
            return validateCost(intValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseValueException(
            "Expected an integer, got %s %s".formatted(input.getClass().getSimpleName(), input)
          );
        }

        @Override
        public Cost parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof IntValue intValue) {
            var value = intValue.getValue().intValue();
            return validateCost(value).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseLiteralException(
            "Expected an integer, got: " + input.getClass().getSimpleName()
          );
        }

        private static Optional<Cost> validateCost(int cost) {
          if (cost >= 0 && cost <= MAX_COST) {
            return Optional.of(Cost.costOfSeconds(cost));
          }
          return Optional.empty();
        }
      }
    )
    .build();

  public static final GraphQLScalarType LOCAL_DATE_SCALAR =
    DateScalarFactory.createGtfsDateScalar();

  public static final GraphQLScalarType GEOJSON_SCALAR = GraphQLScalarType.newScalar()
    .name("GeoJson")
    .description("Geographic data structures in JSON format. See: https://geojson.org/")
    .coercing(
      new Coercing<Geometry, JsonNode>() {
        @Override
        public JsonNode serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Geometry) {
            var geom = (Geometry) dataFetcherResult;
            return GEOJSON_MAPPER.valueToTree(geom);
          }
          return null;
        }

        @Override
        public Geometry parseValue(Object input) throws CoercingParseValueException {
          return null;
        }

        @Override
        public Geometry parseLiteral(Object input) throws CoercingParseLiteralException {
          return null;
        }
      }
    )
    .build();

  public static final GraphQLScalarType GRAPHQL_ID_SCALAR = GraphQLScalarType.newScalar()
    .name("ID")
    .coercing(
      new Coercing<Relay.ResolvedGlobalId, String>() {
        @Override
        public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Relay.ResolvedGlobalId) {
            Relay.ResolvedGlobalId globalId = (Relay.ResolvedGlobalId) dataFetcherResult;
            return new Relay().toGlobalId(globalId.getType(), globalId.getId());
          }
          throw new CoercingSerializeException(
            "Unknown type " + dataFetcherResult.getClass().getSimpleName()
          );
        }

        @Override
        public Relay.ResolvedGlobalId parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof String) {
            return new Relay().fromGlobalId((String) input);
          }
          throw new CoercingParseValueException(
            "Unexpected type " + input.getClass().getSimpleName()
          );
        }

        @Override
        public Relay.ResolvedGlobalId parseLiteral(Object input)
          throws CoercingParseLiteralException {
          if (input instanceof StringValue) {
            return new Relay().fromGlobalId(((StringValue) input).getValue());
          }
          throw new CoercingParseLiteralException(
            "Unexpected type " + input.getClass().getSimpleName()
          );
        }
      }
    )
    .build();

  public static final GraphQLScalarType GRAMS_SCALAR = GraphQLScalarType.newScalar()
    .name("Grams")
    .coercing(
      new Coercing<Gram, Double>() {
        @Override
        public Double serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Gram) {
            var gram = (Gram) dataFetcherResult;
            return Double.valueOf(gram.asDouble());
          }
          return null;
        }

        @Override
        public Gram parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof Double doubleValue) {
            return Gram.of(doubleValue);
          }
          if (input instanceof Integer intValue) {
            return Gram.of(intValue);
          }
          throw new CoercingParseValueException(
            "Expected a number, got %s %s".formatted(input.getClass().getSimpleName(), input)
          );
        }

        @Override
        public Gram parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof FloatValue floatValue) {
            return Gram.of(floatValue.getValue().doubleValue());
          }
          if (input instanceof IntValue intValue) {
            return Gram.of(intValue.getValue().doubleValue());
          }
          throw new CoercingParseLiteralException(
            "Expected a number, got: " + input.getClass().getSimpleName()
          );
        }
      }
    )
    .build();

  public static final GraphQLScalarType RATIO_SCALAR = GraphQLScalarType.newScalar()
    .name("Ratio")
    .coercing(
      new Coercing<Double, Double>() {
        private static final String VALIDATION_ERROR_MESSAGE =
          "Value is under 0 or greater than 1.";

        @Override
        public Double serialize(Object dataFetcherResult) throws CoercingSerializeException {
          var validationException = new CoercingSerializeException(VALIDATION_ERROR_MESSAGE);
          if (dataFetcherResult instanceof Double doubleValue) {
            return validateRatio(doubleValue).orElseThrow(() -> validationException);
          } else if (dataFetcherResult instanceof Float floatValue) {
            return validateRatio(floatValue.doubleValue()).orElseThrow(() -> validationException);
          } else {
            throw new CoercingSerializeException(
              "Cannot serialize object of class %s as a ratio".formatted(
                  dataFetcherResult.getClass().getSimpleName()
                )
            );
          }
        }

        @Override
        public Double parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof Double doubleValue) {
            return validateRatio(doubleValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          if (input instanceof Integer intValue) {
            return validateRatio(intValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseValueException(
            "Expected a number, got %s %s".formatted(input.getClass().getSimpleName(), input)
          );
        }

        @Override
        public Double parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof FloatValue ratio) {
            return validateRatio(ratio.getValue().doubleValue()).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          if (input instanceof IntValue ratio) {
            return validateRatio(ratio.getValue().doubleValue()).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseLiteralException(
            "Expected a number, got: " + input.getClass().getSimpleName()
          );
        }

        private static Optional<Double> validateRatio(double ratio) {
          if (ratio >= -0.001 && ratio <= 1.001) {
            return Optional.of(ratio);
          }
          return Optional.empty();
        }
      }
    )
    .build();

  public static final GraphQLScalarType RELUCTANCE_SCALAR = GraphQLScalarType.newScalar()
    .name("Reluctance")
    .coercing(
      new Coercing<Double, Double>() {
        private static final double MIN_Reluctance = 0.1;
        private static final double MAX_Reluctance = 100000;
        private static final String VALIDATION_ERROR_MESSAGE =
          "Reluctance needs to be between %s and %s".formatted(MIN_Reluctance, MAX_Reluctance);

        @Override
        public Double serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Double doubleValue) {
            return doubleValue;
          } else if (dataFetcherResult instanceof Float floatValue) {
            return floatValue.doubleValue();
          } else {
            throw new CoercingSerializeException(
              "Cannot serialize object of class %s as a reluctance".formatted(
                  dataFetcherResult.getClass().getSimpleName()
                )
            );
          }
        }

        @Override
        public Double parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof Double doubleValue) {
            return validateReluctance(doubleValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          if (input instanceof Integer intValue) {
            return validateReluctance(intValue).orElseThrow(() ->
              new CoercingParseValueException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseValueException(
            "Expected a number, got %s %s".formatted(input.getClass().getSimpleName(), input)
          );
        }

        @Override
        public Double parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof FloatValue reluctance) {
            return validateReluctance(reluctance.getValue().doubleValue()).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          if (input instanceof IntValue reluctance) {
            return validateReluctance(reluctance.getValue().doubleValue()).orElseThrow(() ->
              new CoercingParseLiteralException(VALIDATION_ERROR_MESSAGE)
            );
          }
          throw new CoercingParseLiteralException(
            "Expected a number, got: " + input.getClass().getSimpleName()
          );
        }

        private static Optional<Double> validateReluctance(double reluctance) {
          if (reluctance >= MIN_Reluctance - 0.001 && reluctance <= MAX_Reluctance + 0.001) {
            return Optional.of(reluctance);
          }
          return Optional.empty();
        }
      }
    )
    .build();
}
