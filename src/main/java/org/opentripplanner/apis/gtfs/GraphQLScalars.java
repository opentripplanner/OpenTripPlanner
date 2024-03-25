package org.opentripplanner.apis.gtfs;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.graphql.scalar.DurationScalarFactory;
import org.opentripplanner.framework.model.Grams;
import org.opentripplanner.framework.time.OffsetDateTimeParser;

public class GraphQLScalars {

  private static final ObjectMapper geoJsonMapper = new ObjectMapper()
    .registerModule(new JtsModule(GeometryUtils.getGeometryFactory()));
  public static GraphQLScalarType DURATION_SCALAR = DurationScalarFactory.createDurationScalar();

  public static final GraphQLScalarType POLYLINE_SCALAR = GraphQLScalarType
    .newScalar()
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

  public static final GraphQLScalarType OFFSET_DATETIME_SCALAR = GraphQLScalarType
    .newScalar()
    .name("OffsetDateTime")
    .coercing(
      new Coercing<OffsetDateTime, String>() {
        @Override
        public String serialize(@Nonnull Object dataFetcherResult)
          throws CoercingSerializeException {
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

  public static final GraphQLScalarType GEOJSON_SCALAR = GraphQLScalarType
    .newScalar()
    .name("GeoJson")
    .description("Geographic data structures in JSON format. See: https://geojson.org/")
    .coercing(
      new Coercing<Geometry, JsonNode>() {
        @Override
        public JsonNode serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Geometry) {
            var geom = (Geometry) dataFetcherResult;
            return geoJsonMapper.valueToTree(geom);
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

  public static final GraphQLScalarType GRAPHQL_ID_SCALAR = GraphQLScalarType
    .newScalar()
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

  public static final GraphQLScalarType GRAMS_SCALAR = GraphQLScalarType
    .newScalar()
    .name("Grams")
    .coercing(
      new Coercing<Grams, Double>() {
        @Override
        public Double serialize(Object dataFetcherResult) throws CoercingSerializeException {
          if (dataFetcherResult instanceof Grams) {
            var grams = (Grams) dataFetcherResult;
            return Double.valueOf(grams.asDouble());
          }
          return null;
        }

        @Override
        public Grams parseValue(Object input) throws CoercingParseValueException {
          if (input instanceof Double) {
            var grams = (Double) input;
            return new Grams(grams);
          }
          return null;
        }

        @Override
        public Grams parseLiteral(Object input) throws CoercingParseLiteralException {
          if (input instanceof Double) {
            var grams = (Double) input;
            return new Grams(grams);
          }
          return null;
        }
      }
    )
    .build();
}
