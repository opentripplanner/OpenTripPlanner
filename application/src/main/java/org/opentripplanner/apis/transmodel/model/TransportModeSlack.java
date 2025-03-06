package org.opentripplanner.apis.transmodel.model;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLNonNull.nonNull;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.TRANSPORT_MODE;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.transit.model.basic.TransitMode;

public class TransportModeSlack {

  private static final String BOARD_SLACK_DESCRIPTION;
  private static final String ALIGHT_SLACK_DESCRIPTION;
  private static final GraphQLNonNull INT_TYPE;
  private static final GraphQLNonNull MODE_LIST_TYPE;
  private static final GraphQLInputObjectType SLACK_INPUT_TYPE;
  private static final GraphQLOutputType SLACK_OUTPUT_TYPE;
  public static final GraphQLList SLACK_LIST_INPUT_TYPE;
  public static final GraphQLList SLACK_LIST_OUTPUT_TYPE;
  public final int slack;
  public final List<TransitMode> modes;

  static {
    BOARD_SLACK_DESCRIPTION =
      "The boardSlack is the minimum extra time to board a public transport vehicle. This is " +
      "the same as the 'minimumTransferTime', except that this also applies to to the first " +
      "transit leg in the trip.";

    ALIGHT_SLACK_DESCRIPTION =
      "The alightSlack is the minimum extra time after exiting a public transport vehicle.";

    INT_TYPE = nonNull(Scalars.GraphQLInt);
    MODE_LIST_TYPE = nonNull(GraphQLList.list(nonNull(TRANSPORT_MODE)));
    SLACK_INPUT_TYPE = GraphQLInputObjectType.newInputObject()
      .name("TransportModeSlack")
      .description("Used to specify board and alight slack for a given modes.")
      .field(
        newInputObjectField()
          .name("slack")
          .description("The slack used for all given modes.")
          .type(INT_TYPE)
          .build()
      )
      .field(
        newInputObjectField()
          .name("modes")
          .description("List of modes for which the given slack apply.")
          .type(MODE_LIST_TYPE)
          .build()
      )
      .build();
    SLACK_OUTPUT_TYPE = GraphQLObjectType.newObject()
      .name("TransportModeSlackType")
      .description("Used to specify board and alight slack for a given modes.")
      .field(GraphQLFieldDefinition.newFieldDefinition().name("slack").type(INT_TYPE).build())
      .field(GraphQLFieldDefinition.newFieldDefinition().name("modes").type(MODE_LIST_TYPE).build())
      .build();
    SLACK_LIST_INPUT_TYPE = GraphQLList.list(SLACK_INPUT_TYPE);
    SLACK_LIST_OUTPUT_TYPE = GraphQLList.list(SLACK_OUTPUT_TYPE);
  }

  private TransportModeSlack(int slack, List<TransitMode> modes) {
    this.slack = slack;
    this.modes = modes;
  }

  public static String boardSlackDescription(String byGroupName) {
    return BOARD_SLACK_DESCRIPTION + " " + defaultDescription(byGroupName);
  }

  public static String alightSlackDescription(String byGroupName) {
    return ALIGHT_SLACK_DESCRIPTION + " " + defaultDescription(byGroupName);
  }

  public static String slackByGroupDescription(String name) {
    return String.format("List of %s for a given set of modes.", name);
  }

  public static String slackByGroupDescription(
    String name,
    DurationForEnum<TransitMode> defaultValues
  ) {
    return slackByGroupDescription(name) + " " + defaultsToString(defaultValues);
  }

  public static List<TransportModeSlack> mapToApiList(DurationForEnum<TransitMode> domain) {
    // Group modes by slack value
    Multimap<Integer, TransitMode> modesBySlack = ArrayListMultimap.create();
    Arrays.stream(TransitMode.values())
      .filter(domain::isSet)
      .forEach(m -> modesBySlack.put((int) domain.valueOf(m).toSeconds(), m));

    // Create a new entry for each group of modes
    List<TransportModeSlack> result = new ArrayList<>();
    modesBySlack.asMap().forEach((k, v) -> result.add(new TransportModeSlack(k, List.copyOf(v))));

    // Sort on slack value - just to make a nice presentation
    result.sort(Comparator.comparingInt(a -> a.slack));
    return result;
  }

  @SuppressWarnings("unchecked")
  public static void mapIntoDomain(DurationForEnum.Builder<TransitMode> builder, Object value) {
    if (value instanceof List) {
      List<Map<String, Object>> list = (List<Map<String, Object>>) value;
      for (Map<String, Object> map : list) {
        int slack = (Integer) map.get("slack");
        List<TransitMode> modes = (List<TransitMode>) map.get("modes");
        modes.forEach(m -> builder.with(m, Duration.ofSeconds(slack)));
      }
    } else {
      throw new IllegalArgumentException(
        "Expected List, but got: " + value + " (" + value.getClass() + ")"
      );
    }
  }

  @Override
  public String toString() {
    if (modes == null) {
      return "{slack: " + slack + "}";
    }
    return (
      "{" +
      modes.stream().map(TransportModeSlack::serializeTransportMode).collect(Collectors.toList()) +
      " : " +
      slack +
      "}"
    );
  }

  private static String defaultDescription(String groupName) {
    return String.format(
      "This is the default value used, if not overridden by the '%s'.",
      groupName
    );
  }

  private static String defaultsToString(DurationForEnum<TransitMode> byMode) {
    List<String> groups = new ArrayList<>();
    Arrays.stream(TransitMode.values())
      .filter(byMode::isSet)
      .forEach(m -> groups.add(serializeTransportMode(m) + " : " + byMode.valueOf(m)));
    Collections.sort(groups);
    return "Defaults: " + groups;
  }

  private static Object serializeTransportMode(TransitMode m) {
    return TRANSPORT_MODE.serialize(m);
  }
}
