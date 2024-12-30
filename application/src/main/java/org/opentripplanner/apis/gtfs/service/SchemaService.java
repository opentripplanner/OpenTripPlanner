package org.opentripplanner.apis.gtfs.service;

import graphql.schema.GraphQLSchema;
import jakarta.inject.Inject;

/**
 * Service for fetching the {@link GraphQLSchema}. The purpose of this class is to avoid
 * reconstructing the schema on each request.
 */
public class SchemaService {

  private final GraphQLSchema schema;

  @Inject
  public SchemaService(GraphQLSchema schema) {
    this.schema = schema;
  }

  public GraphQLSchema schema() {
    return schema;
  }
}
