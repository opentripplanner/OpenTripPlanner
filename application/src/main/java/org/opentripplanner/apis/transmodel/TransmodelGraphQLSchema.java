package org.opentripplanner.apis.transmodel;

import graphql.schema.GraphQLSchema;
import javax.annotation.Nullable;

/**
 * Wraps the Transmodel {@link GraphQLSchema} so it can be exposed as its own HK2 {@code @Context}-
 * injectable binding. A bare {@code GraphQLSchema} is ambiguous for that purpose since the GTFS
 * API binds one too, and JAX-RS {@code @Context} resolves by type only, not by Dagger qualifier.
 */
public record TransmodelGraphQLSchema(@Nullable GraphQLSchema schema) {}
