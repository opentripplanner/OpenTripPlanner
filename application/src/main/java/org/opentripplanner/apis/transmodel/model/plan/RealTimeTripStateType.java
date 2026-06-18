package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

/**
 * GraphQL type for the structured real-time state of a trip, exposed on the {@code Leg} type.
 * Each field corresponds to one boolean flag on {@link TransmodelRealTimeTripStateModel}.
 */
public class RealTimeTripStateType {

  private static final String NAME = "RealTimeTripState";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("The real-time state of a trip on a leg.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("added")
          .description("Has the trip been added via real-time updates? (extra trip)")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).added())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("canceled")
          .description("Has the trip been canceled?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).canceled())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("deleted")
          .description("Has the trip been deleted?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).deleted())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timesModified")
          .description("Have the departure/arrival times been modified?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).timesModified())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tripPatternModified")
          .description("Has the trip pattern been modified?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env ->
            ((TransmodelRealTimeTripStateModel) env.getSource()).tripPatternModified()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("updated")
          .description("Have there been any real-time updates on this trip?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).updated())
          .build()
      )
      .build();
  }
}
