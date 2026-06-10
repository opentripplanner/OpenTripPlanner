package org.opentripplanner.warmup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.Validator;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.gtfs.SchemaFactory;
import org.opentripplanner.apis.support.graphql.injectdoc.ApiDocumentationProfile;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchemaFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParametersTestFactory;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * Validates that the warmup GraphQL queries are syntactically and structurally valid
 * against the actual schemas. This catches field renames, removed arguments, or
 * invalid enum values at build time rather than at runtime.
 */
class WarmupQueryValidationTest {

  private static final GraphQLSchema GTFS_SCHEMA = SchemaFactory.createSchemaWithDefaultInjection(
    RouteRequest.defaultValue()
  );

  private static final GraphQLSchema TRANSMODEL_SCHEMA = new TransmodelGraphQLSchemaFactory(
    RouteRequest.defaultValue(),
    ZoneIds.OSLO,
    TransitTuningParametersTestFactory.forTest(),
    new DefaultFeedIdMapper(),
    ApiDocumentationProfile.DEFAULT
  ).create();

  @Test
  void transmodelQueryIsValid() {
    var errors = new Validator().validateDocument(
      TRANSMODEL_SCHEMA,
      Parser.parse(TransmodelWarmupQueryExecutor.QUERY),
      Locale.ROOT
    );
    assertEquals(0, errors.size(), errors.toString());
  }

  @Test
  void gtfsQueryIsValid() {
    var errors = new Validator().validateDocument(
      GTFS_SCHEMA,
      Parser.parse(GtfsWarmupQueryExecutor.QUERY),
      Locale.ROOT
    );
    assertEquals(0, errors.size(), errors.toString());
  }
}
