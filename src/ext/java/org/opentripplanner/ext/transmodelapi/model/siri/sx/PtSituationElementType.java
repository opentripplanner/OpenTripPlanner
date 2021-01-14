package org.opentripplanner.ext.transmodelapi.model.siri.sx;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.util.TranslatedString;

import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class PtSituationElementType {
  private static final String NAME = "PtSituationElement";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLOutputType authorityType,
      GraphQLOutputType quayType,
      GraphQLOutputType lineType,
      GraphQLOutputType serviceJourneyType,
      GraphQLOutputType multilingualStringType,
      GraphQLObjectType validityPeriodType,
      GraphQLObjectType infoLinkType,
      Relay relay
  ) {
    return GraphQLObjectType.newObject()
            .name(NAME)
            .description("Simple public transport situation element")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                    .dataFetcher(environment -> relay.toGlobalId(NAME, ((TransitAlert) environment.getSource()).getId()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("authority")
                    .type(authorityType)
                    .description("Get affected authority for this situation element")
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment)
                          .getAgencyForId(((TransitAlert) environment.getSource()).getEntities()
                              .stream()
                              .filter(EntitySelector.Agency.class::isInstance)
                              .map(EntitySelector.Agency.class::cast)
                              .findAny()
                              .map(entitySelector -> entitySelector.agencyId)
                              .orElse(null));
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("lines")
                    .type(new GraphQLNonNull(new GraphQLList(lineType)))
                    .dataFetcher(environment -> {
                      RoutingService routingService = GqlUtil.getRoutingService(environment);
                      return ((TransitAlert) environment.getSource()).getEntities()
                        .stream()
                        .filter(EntitySelector.Route.class::isInstance)
                        .map(EntitySelector.Route.class::cast)
                        .map(entitySelector -> entitySelector.routeId)
                        .map(routeId -> routingService.getRouteForId(routeId))
                        .collect(Collectors.toList());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("serviceJourneys")
                    .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                    .dataFetcher(environment -> {
                      RoutingService routingService = GqlUtil.getRoutingService(environment);
                      return ((TransitAlert) environment.getSource()).getEntities()
                          .stream()
                          .filter(EntitySelector.Trip.class::isInstance)
                          .map(EntitySelector.Trip.class::cast)
                          .map(entitySelector -> entitySelector.tripId)
                          .map(tripId -> routingService.getTripForId().get(tripId))
                          .collect(Collectors.toList());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("quays")
                    .type(new GraphQLNonNull(new GraphQLList(quayType)))
                    .dataFetcher(environment -> {
                          RoutingService routingService = GqlUtil.getRoutingService(environment);
                          return ((TransitAlert) environment.getSource()).getEntities()
                              .stream()
                              .filter(EntitySelector.Stop.class::isInstance)
                              .map(EntitySelector.Stop.class::cast)
                              .map(entitySelector -> entitySelector.stopId)
                              .map(stopId -> routingService.getStopForId(stopId))
                              .collect(Collectors.toList());
                        }
                    )
                    .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("stopPlaces")
//                        .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
//                        .dataFetcher(environment ->
//                                wrapInListUnlessNull(index.stationForId.get(((AlertPatch) environment.getSource()).getStop()))
//                        )
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("journeyPatterns")
//                        .description("Get all journey patterns for this situation element")
//                        .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
//                        .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getTripPatterns())
//                        .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("summary")
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(
                        multilingualStringType))))
                    .description("Summary of situation in all different translations available")
                    .dataFetcher(environment -> {
                        TransitAlert alert = environment.getSource();
                        if (alert.alertHeaderText instanceof TranslatedString) {
                            return ((TranslatedString) alert.alertHeaderText).getTranslations();
                        } else if (alert.alertHeaderText != null) {
                            return List.of(new AbstractMap.SimpleEntry<>(null, alert.alertHeaderText.toString()));
                        } else {
                            return emptyList();
                        }
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("description")
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(
                        multilingualStringType))))
                    .description("Description of situation in all different translations available")
                    .dataFetcher(environment -> {
                        TransitAlert alert = environment.getSource();
                        if (alert.alertDescriptionText instanceof TranslatedString) {
                            return ((TranslatedString) alert.alertDescriptionText).getTranslations();
                        } else if (alert.alertDescriptionText != null) {
                            return List.of(new AbstractMap.SimpleEntry<>(null, alert.alertDescriptionText.toString()));
                        } else {
                            return emptyList();
                        }
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("advice")
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(
                        multilingualStringType))))
                    .description("Advice of situation in all different translations available")
                    .dataFetcher(environment -> {
                        TransitAlert alert = environment.getSource();
                        if (alert.alertAdviceText instanceof TranslatedString) {
                            return ((TranslatedString) alert.alertAdviceText).getTranslations();
                        } else if (alert.alertAdviceText != null) {
                            return List.of(new AbstractMap.SimpleEntry<>(null, alert.alertAdviceText.toString()));
                        } else {
                            return emptyList();
                        }
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("infoLinks")
                    .type(new GraphQLList(infoLinkType))
                    .description("Optional links to more information.")
                    .dataFetcher(environment -> null)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("validityPeriod")
                    .type(validityPeriodType)
                    .description("Period this situation is in effect")
                    .dataFetcher(environment -> {
                      TransitAlert alert = environment.getSource();
                        Long startTime = alert.getEffectiveStartDate() != null ? alert.getEffectiveStartDate().getTime() : null;
                        Long endTime = alert.getEffectiveEndDate() != null ? alert.getEffectiveEndDate().getTime() : null;
                        return Pair.of(startTime, endTime);
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("reportType")
                .type(EnumTypes.REPORT_TYPE)
                .description("ReportType of this situation")
                .dataFetcher(environment -> ((TransitAlert) environment.getSource()).alertType)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("situationNumber")
                    .type(Scalars.GraphQLString)
                    .description("Operator's internal id for this situation")
                    .dataFetcher(environment -> ((TransitAlert) environment.getSource()).getId())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("severity")
                    .type(EnumTypes.SEVERITY)
                    .description("Severity of this situation ")
                    .dataFetcher(environment -> ((TransitAlert) environment.getSource()).severity)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("reportAuthority")
                    .type(authorityType)
                    .description("Authority that reported this situation")
                    .deprecate("Not yet officially supported. May be removed or renamed.")
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment)
                          .getAgencyForId(((TransitAlert) environment.getSource()).getEntities()
                              .stream()
                              .filter(EntitySelector.Agency.class::isInstance)
                              .map(EntitySelector.Agency.class::cast)
                              .findAny()
                              .map(entitySelector -> entitySelector.agencyId)
                              .orElse(null)
                          );
                    })
                    .build())
            .build();
  }
}
