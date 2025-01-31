package org.opentripplanner.apis.transmodel.model.plan;

import static graphql.Directives.OneOfDirective;
import static graphql.Scalars.GraphQLString;

import graphql.language.StringValue;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import java.time.Duration;
import org.opentripplanner.apis.transmodel.model.framework.CoordinateInputType;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;

public class ViaLocationInputType {

  /* type constants */

  private static final String INPUT_VIA_LOCATION = "TripViaLocationInput";
  private static final String INPUT_VISIT_VIA_LOCATION = "TripVisitViaLocationInput";
  private static final String INPUT_PASS_THROUGH_VIA_LOCATION = "TripPassThroughViaLocationInput";

  private static final String DOC_VISIT_VIA_LOCATION =
    """
    A visit-via-location is a physical visit to one of the stop locations or coordinates listed. An
    on-board visit does not count, the traveler must alight or board at the given stop for it to to
    be accepted. To visit a coordinate, the traveler must walk(bike or drive) to the closest point
    in the street network from a stop and back to another stop to join the transit network.
    
    NOTE! Coordinates are NOT supported yet.
    """;
  private static final String DOC_PASS_THROUGH_VIA_LOCATION =
    """
    One of the listed stop locations must be visited on-board a transit vehicle or the journey must
    alight or board at the location.
    """;
  private static final String DOC_VIA_LOCATION =
    """
    A via-location is used to specifying a location as an intermediate place the router must
    route through. The via-location is either a pass-through-location or a visit-via-location.
    """;

  /* field  constants */

  public static final String FIELD_LABEL = "label";
  public static final String FIELD_MINIMUM_WAIT_TIME = "minimumWaitTime";
  public static final String FIELD_STOP_LOCATION_IDS = "stopLocationIds";
  public static final String FIELD_COORDINATE = "coordinate";

  public static final String FIELD_VISIT = "visit";
  public static final String DOC_FIELD_VISIT =
    "Board or alight at a stop location or visit a coordinate.";
  public static final String FIELD_PASS_THROUGH = "passThrough";
  public static final String DOC_FIELD_PASS_THROUGH =
    "Board, alight or pass-through(on-board) at the stop location.";

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
  private static final String DOC_COORDINATE = "A coordinate to route through.";

  static final GraphQLInputObjectType VISIT_VIA_LOCATION_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name(INPUT_VISIT_VIA_LOCATION)
    .description(DOC_VISIT_VIA_LOCATION)
    .field(b -> b.name(FIELD_LABEL).description(DOC_LABEL).type(GraphQLString))
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
        .type(optionalListOfNonNullStrings())
    )
    .field(b ->
      b.name(FIELD_COORDINATE).description(DOC_COORDINATE).type(CoordinateInputType.INPUT_TYPE)
    )
    .build();

  static final GraphQLInputObjectType PASS_THROUGH_VIA_LOCATION_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name(INPUT_PASS_THROUGH_VIA_LOCATION)
    .description(DOC_PASS_THROUGH_VIA_LOCATION)
    .field(b -> b.name(FIELD_LABEL).description(DOC_LABEL).type(GraphQLString))
    .field(b ->
      // This is NOT nonNull, because we might add other parameters later, like 'list of line-ids'
      b
        .name(FIELD_STOP_LOCATION_IDS)
        .description(DOC_STOP_LOCATION_IDS)
        .type(requiredListOfNonNullStrings())
    )
    .build();

  public static final GraphQLInputObjectType VIA_LOCATION_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name(INPUT_VIA_LOCATION)
    .description(DOC_VIA_LOCATION)
    .withDirective(OneOfDirective)
    .field(b -> b.name(FIELD_VISIT).description(DOC_FIELD_VISIT).type(VISIT_VIA_LOCATION_INPUT))
    .field(b ->
      b
        .name(FIELD_PASS_THROUGH)
        .description(DOC_FIELD_PASS_THROUGH)
        .type(PASS_THROUGH_VIA_LOCATION_INPUT)
    )
    .build();

  private static GraphQLInputType requiredListOfNonNullStrings() {
    return new GraphQLNonNull(optionalListOfNonNullStrings());
  }

  private static GraphQLInputType optionalListOfNonNullStrings() {
    return new GraphQLList(new GraphQLNonNull(GraphQLString));
  }
}
