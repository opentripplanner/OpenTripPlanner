package org.opentripplanner.ext.transmodelapi.model;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLNonNull.nonNull;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.model.TransitMode;

public class TransportModeSlack {
    private static final String BOARD_SLACK_DESCRIPTION;
    private static final String ALIGHT_SLACK_DESCRIPTION;
    private static final GraphQLNonNull INT_TYPE;
    private static final GraphQLNonNull MODE_LIST_TYPE;
    private static final GraphQLInputObjectType SLACK_INPUT_TYPE;
    private static final GraphQLOutputType SLACK_OUTPUT_TYPE;
    public static final GraphQLList SLACK_LIST_INPUT_TYPE;
    public static final GraphQLList SLACK_LIST_OUTPUT_TYPE;

    static {
        BOARD_SLACK_DESCRIPTION =
            "The boardSlack is the minimum extra time to board a public transport vehicle. This is "
            + "the same as the 'minimumTransferTime', except that this also apply to to the first "
            + "transit leg in the trip.";

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
                .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                                .name("slack")
                                .type(INT_TYPE)
                                .build()
                )
                .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                                .name("modes")
                                .type(MODE_LIST_TYPE)
                                .build()
                )
                .build();
        SLACK_LIST_INPUT_TYPE = GraphQLList.list(SLACK_INPUT_TYPE);
        SLACK_LIST_OUTPUT_TYPE = GraphQLList.list(SLACK_OUTPUT_TYPE);
    }

    public final int slack;
    public final List<TransitMode> modes;

    private TransportModeSlack(int slack, List<TransitMode> modes) {
        this.slack = slack;
        this.modes = modes;
    }

    private static String defaultDescription( String groupName) {
        return String.format(
                "This is the default value used, if not overridden by the '%s'.", groupName
        );
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
    public static String slackByGroupDescription(String name, Map<TransitMode, Integer> defaultValues) {
        return slackByGroupDescription(name) + " " + defaultsToString(defaultValues);
    }

    @Override
    public String toString() {
        if(modes == null) {
            return "{slack: " + slack + "}";
        }
        return "{"
                + modes.stream()
                        .map(TransportModeSlack::serializeTransportMode)
                        .collect(Collectors.toList())
                + " : " + slack
                + "}";
    }

    public static List<TransportModeSlack> mapToApiList(Map<TransitMode, Integer> domain) {
        // Group modes by slack value
        Multimap<Integer, TransitMode> modesBySlack = ArrayListMultimap.create();
        domain.forEach((k,v) -> modesBySlack.put(v, k));

        // Create a new entry for each group of modes
        List<TransportModeSlack> result = new ArrayList<>();
        modesBySlack.asMap().forEach((k,v) -> result.add(new TransportModeSlack(k, List.copyOf(v))));

        // Sort on slack value - just to make a nice presentation
        result.sort(Comparator.comparingInt(a -> a.slack));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static EnumMap<TransitMode, Integer> mapToDomain(Object value) {
        var result = new EnumMap<TransitMode, Integer>(TransitMode.class);
        if(value instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) value;
            for (Map<String, Object> map : list) {
                int slack = (Integer) map.get("slack");
                ((List<TransitMode>) map.get("modes")).forEach(m -> result.put(m, slack));
            }
        }
        return result;
    }

    private static String defaultsToString(Map<TransitMode, Integer> byMode) {
        List<String> groups = new ArrayList<>();
        byMode.forEach((m,v) -> groups.add(serializeTransportMode(m) + " : " + v));
        Collections.sort(groups);
        return "Defaults: " + groups;
    }

    private static Object serializeTransportMode(TransitMode m) {
        return TRANSPORT_MODE.serialize(m);
    }
}
