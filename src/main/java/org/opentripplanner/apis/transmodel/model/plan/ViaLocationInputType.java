package org.opentripplanner.apis.transmodel.model.plan;

import static graphql.Directives.OneOfDirective;

import graphql.Scalars;
import graphql.language.StringValue;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import java.time.Duration;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;

public class ViaLocationInputType {

  /* type constants */

  private static final String INPUT_VIA_LOCATION = "PlanViaLocationInput";
  private static final String INPUT_VISIT_VIA_LOCATION = "PlanVisitViaLocationInput";
  private static final String INPUT_PASS_THROUGH_VIA_LOCATION = "PlanPassThroughViaLocationInput";

  private static final String DOC_VISIT_VIA_LOCATION =
    """
    A visit-via-location is a physical visit to one of the stops or coordinates listed. An
    on-board visit does not count, the traveler must alight or board at the given stop for
    it to to be accepted. To visit a coordinate, the traveler must walk(bike or drive) to
    the closest point in the street network from a stop and back to another stop to join
    the transit network.
    
    NOTE! Coordinates are NOT supported jet.
    """;
  private static final String DOC_PASS_THROUGH_VIA_LOCATION =
    """
    One of the listed stop locations must be visited on-board a transit vehicle or the journey must
    alight or board at the location.
    """;
  private static final String DOC_VIA_LOCATION =
    """
    A via-location is used to specifying a location as an intermediate place the router must
    route through. The via-location must be either a pass-through-location or a
    visit-via-location. An on-board "visit" is only allowed for pass-through-via-locations, while
    the visit-via-location can visit a stop-location or a coordinate and specify a
    minimum-wait-time.
    """;

  /* field  constants */

  public static final String FIELD_LABEL = "label";
  public static final String FIELD_MINIMUM_WAIT_TIME = "minimumWaitTime";
  public static final String FIELD_STOP_LOCATION_IDS = "stopLocationIds";

  // TODO : Add coordinates
  //private static final String FIELD_COORDINATES = "coordinates";
  public static final String FIELD_VISIT = "visit";
  public static final String FIELD_PASS_THROUGH = "passThrough";

  private static final String DOC_LABEL =
    "The label/name of the location. This is pass-through " +
    "information and is not used in routing.";
  private static final String DOC_MINIMUM_WAIT_TIME =
    """
    The minimum wait time is used to force the trip to stay the given duration at the
    via-location before the trip is continued.
    """;
  private static final String DOC_STOP_LOCATION_IDS =
    """
    A list of stop locations. A stop location can be a quay, a stop place, a multimodal
    stop place or a group of stop places. It is enough to visit ONE of the locations
    listed.
    """;

  static final GraphQLInputObjectType VISIT_VIA_LOCATION_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name(INPUT_VISIT_VIA_LOCATION)
    .description(DOC_VISIT_VIA_LOCATION)
    .field(b -> b.name(FIELD_LABEL).description(DOC_LABEL).type(Scalars.GraphQLString))
    .field(b ->
      b
        .name(FIELD_MINIMUM_WAIT_TIME)
        .description(DOC_MINIMUM_WAIT_TIME)
        .type(TransmodelScalars.DURATION_SCALAR)
        .defaultValueLiteral(StringValue.of(Duration.ZERO.toString()))
    )
    .field(b ->
      b
        .name(FIELD_STOP_LOCATION_IDS)
        .description(DOC_STOP_LOCATION_IDS)
        .type(GraphQLList.list(Scalars.GraphQLString))
    )
    /*
      TODO: Add support for coordinates
       */
    .build();

  static final GraphQLInputObjectType PASS_THROUGH_VIA_LOCATION_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name(INPUT_PASS_THROUGH_VIA_LOCATION)
    .description(DOC_PASS_THROUGH_VIA_LOCATION)
    .field(b -> b.name(FIELD_LABEL).description(DOC_LABEL).type(Scalars.GraphQLString))
    .field(b ->
      b
        .name(FIELD_STOP_LOCATION_IDS)
        .description(DOC_STOP_LOCATION_IDS)
        .type(GraphQLList.list(Scalars.GraphQLString))
    )
    .build();

  public static final GraphQLInputObjectType VIA_LOCATION_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name(INPUT_VIA_LOCATION)
    .description(DOC_VIA_LOCATION)
    .withDirective(OneOfDirective)
    .field(b -> b.name(FIELD_VISIT).type(VISIT_VIA_LOCATION_INPUT))
    .field(b -> b.name(FIELD_PASS_THROUGH).type(PASS_THROUGH_VIA_LOCATION_INPUT))
    .build();
}
