package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInputObjectType;
import org.apache.commons.collections.CollectionUtils;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.RoutingService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.opentripplanner.ext.transmodelapi.support.GqlUtil.newIdListInputField;

public class JourneyWhiteListed {
    public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
        .name("InputWhiteListed")
        .description(
            "Filter trips by only allowing lines involving certain "
                + "elements. If both lines and authorities are specified, only one must be valid "
                + "for each line to be used. If a line is both banned and whitelisted, it will "
                + "be counted as banned."
        )
        .field(newIdListInputField("lines","Set of ids for lines that should be used"))
        .field(newIdListInputField("authorities", "Set of ids for authorities that should be used"))
        .build();

    public final Set<FeedScopedId> authorityIds;
    public final Set<FeedScopedId> lineIds;

    public JourneyWhiteListed(DataFetchingEnvironment environment) {
        Map<String, List<String>> whiteList = environment.getArgument("whiteListed");
        if (whiteList == null) {
            this.authorityIds = Set.of();
            this.lineIds = Set.of();
        }
        else {
            this.authorityIds = Set.copyOf(TransitIdMapper.mapIDsToDomain(whiteList.get("authorities")));
            this.lineIds = Set.copyOf(TransitIdMapper.mapIDsToDomain(whiteList.get("lines")));
        }
    }


    public static Stream<TripTimeShort> whiteListAuthoritiesAndOrLines(
        Stream<TripTimeShort> stream,
        Collection<FeedScopedId> authorityIds,
        Collection<FeedScopedId> lineIds
    ) {
        if (CollectionUtils.isEmpty(authorityIds) && CollectionUtils.isEmpty(lineIds)) {
            return stream;
        }
        return stream.filter(it -> isTripTimeShortAcceptable(
            it,
            authorityIds,
            lineIds
        ));
    }

    private static boolean isTripTimeShortAcceptable(
        TripTimeShort tts,
        Collection<FeedScopedId> authorityIds,
        Collection<FeedScopedId> lineIds
    ) {
        Trip trip = tts.getTrip();

        if (trip == null || trip.getRoute() == null) {
            return true;
        }

        Route route = trip.getRoute();
        boolean okForAuthority = authorityIds.contains(route.getAgency().getId());
        boolean okForLine = lineIds.contains(route.getId());

        return okForAuthority || okForLine;
    }
}
