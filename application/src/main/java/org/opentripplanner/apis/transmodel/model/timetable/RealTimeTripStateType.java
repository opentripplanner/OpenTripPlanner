package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

/**
 * GraphQL type for the structured real-time state of a journey, exposed on the
 * {@code DatedServiceJourney} type. Each field corresponds to one boolean flag on
 * {@link TransmodelRealTimeTripStateModel}.
 */
public class RealTimeTripStateType {

  private static final String NAME = "RealTimeJourneyState";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("The real-time state of a service journey.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("extraJourney")
          .description(
            "Has this journey been added via real-time updates? (extra journey not in the planned data)"
          )
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).extraJourney())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("cancellation")
          .description("Has this journey been cancelled?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).cancellation())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timesModified")
          .description(
            "Have any quay's arrival or departure times been modified from the scheduled times?"
          )
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).timesModified())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("journeyPatternModified")
          .description(
            "Has the quay sequence changed from the planned journey pattern? True if quays were added, removed, reordered, or reassigned to a different quay's location. False if the journey was cancelled"
          )
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env ->
            ((TransmodelRealTimeTripStateModel) env.getSource()).journeyPatternModified()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("updated")
          .description("Have there been any real-time updates on this journey?")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> ((TransmodelRealTimeTripStateModel) env.getSource()).updated())
          .build()
      )
      .build();
  }
}
