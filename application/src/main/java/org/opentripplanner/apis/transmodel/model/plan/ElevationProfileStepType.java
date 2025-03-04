package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.plan.ElevationProfile;

public class ElevationProfileStepType {

  private static final String NAME = "ElevationProfileStep";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  static String makeDescription(String name) {
    return """
    The %s's elevation profile. All elevation values, including the first one, are in meters
    above sea level. The elevation is negative for places below sea level. The profile
    includes both the start and end coordinate.
    """.formatted(name).stripIndent();
  }

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("Individual step of an elevation profile.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("distance")
          .description("The horizontal distance from the start of the step, in meters.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> step(env).x())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("elevation")
          .description(
            """
            The elevation at this distance, in meters above sea level. It is negative if the
            location is below sea level.
            """.stripIndent()
          )
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> step(env).y())
          .build()
      )
      .build();
  }

  private static ElevationProfile.Step step(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  protected static List<ElevationProfile.Step> mapElevationProfile(ElevationProfile profile) {
    return Objects.requireNonNullElse(profile, ElevationProfile.empty()).stepsWithoutUnknowns();
  }
}
