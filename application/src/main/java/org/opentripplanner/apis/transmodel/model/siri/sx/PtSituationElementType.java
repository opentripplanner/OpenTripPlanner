package org.opentripplanner.apis.transmodel.model.siri.sx;

import static java.util.Collections.emptyList;
import static org.opentripplanner.apis.transmodel.mapping.SeverityMapper.getTransmodelSeverity;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.stop.MonoOrMultiModalStation;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.service.TransitService;

public class PtSituationElementType {

  private static final String NAME = "PtSituationElement";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
    GraphQLOutputType authorityType,
    GraphQLOutputType quayType,
    GraphQLOutputType stopPlaceType,
    GraphQLOutputType lineType,
    GraphQLOutputType serviceJourneyType,
    GraphQLOutputType multilingualStringType,
    GraphQLObjectType validityPeriodType,
    GraphQLObjectType infoLinkType,
    GraphQLOutputType affectsType,
    GraphQLScalarType dateTimeScalar,
    Relay relay
  ) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("Simple public transport situation element")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment ->
            relay.toGlobalId(NAME, ((TransitAlert) environment.getSource()).getId().getId())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("authority")
          .type(authorityType)
          .description("Get affected authority for this situation element")
          .deprecate("Use affects instead")
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).getAgency(
              ((TransitAlert) environment.getSource()).entities()
                .stream()
                .filter(EntitySelector.Agency.class::isInstance)
                .map(EntitySelector.Agency.class::cast)
                .findAny()
                .map(EntitySelector.Agency::agencyId)
                .orElse(null)
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("lines")
          .type(new GraphQLNonNull(new GraphQLList(lineType)))
          .deprecate("Use affects instead")
          .dataFetcher(environment -> {
            TransitService transitService = GqlUtil.getTransitService(environment);
            return ((TransitAlert) environment.getSource()).entities()
              .stream()
              .filter(EntitySelector.Route.class::isInstance)
              .map(EntitySelector.Route.class::cast)
              .map(EntitySelector.Route::routeId)
              .map(transitService::getRoute)
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourneys")
          .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
          .deprecate("Use affects instead")
          .dataFetcher(environment -> {
            TransitService transitService = GqlUtil.getTransitService(environment);
            return ((TransitAlert) environment.getSource()).entities()
              .stream()
              .filter(EntitySelector.Trip.class::isInstance)
              .map(EntitySelector.Trip.class::cast)
              .map(EntitySelector.Trip::tripId)
              .map(transitService::getTrip)
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quays")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
          .deprecate("Use affects instead")
          .dataFetcher(environment -> {
            TransitService transitService = GqlUtil.getTransitService(environment);
            return ((TransitAlert) environment.getSource()).entities()
              .stream()
              .filter(EntitySelector.Stop.class::isInstance)
              .map(EntitySelector.Stop.class::cast)
              .map(EntitySelector.Stop::stopId)
              .map(transitService::getRegularStop)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlaces")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopPlaceType))))
          .deprecate("Use affects instead")
          .dataFetcher(environment -> {
            TransitService transitService = GqlUtil.getTransitService(environment);
            return ((TransitAlert) environment.getSource()).entities()
              .stream()
              .filter(EntitySelector.Stop.class::isInstance)
              .map(EntitySelector.Stop.class::cast)
              .map(EntitySelector.Stop::stopId)
              .map(transitService::getStation)
              .filter(Objects::nonNull)
              .map(station ->
                new MonoOrMultiModalStation(station, transitService.findMultiModalStation(station))
              )
              .toList();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("affects")
          .description("Get all affected entities for the situation")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(affectsType))))
          .dataFetcher(environment -> ((TransitAlert) environment.getSource()).entities())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("summary")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
          .description("Summary of situation in all different translations available")
          .dataFetcher(environment ->
            environment
              .<TransitAlert>getSource()
              .headerText()
              .map(headerText -> {
                if (headerText instanceof TranslatedString translatedString) {
                  return translatedString.getTranslations();
                } else {
                  return List.of(new AbstractMap.SimpleEntry<>(null, headerText.toString()));
                }
              })
              .orElse(emptyList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
          .description("Description of situation in all different translations available")
          .dataFetcher(environment ->
            environment
              .<TransitAlert>getSource()
              .descriptionText()
              .map(descriptionText -> {
                if (descriptionText instanceof TranslatedString translatedString) {
                  return translatedString.getTranslations();
                } else {
                  return List.of(new AbstractMap.SimpleEntry<>(null, descriptionText.toString()));
                }
              })
              .orElse(emptyList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("advice")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
          .description("Advice of situation in all different translations available")
          .dataFetcher(environment -> {
            I18NString adviceText = environment.<TransitAlert>getSource().adviceText();
            if (adviceText instanceof TranslatedString translatedString) {
              return translatedString.getTranslations();
            } else if (adviceText != null) {
              return List.of(new AbstractMap.SimpleEntry<>(null, adviceText.toString()));
            } else {
              return emptyList();
            }
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("infoLinks")
          .type(new GraphQLList(new GraphQLNonNull(infoLinkType)))
          .description("Optional links to more information.")
          .dataFetcher(environment -> {
            List<AlertUrl> siriUrls = environment.<TransitAlert>getSource().siriUrls();
            if (!siriUrls.isEmpty()) {
              return siriUrls;
            }
            return emptyList();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("validityPeriod")
          .type(validityPeriodType)
          .description("Period this situation is in effect")
          .dataFetcher(environment -> {
            TransitAlert alert = environment.getSource();
            Long startTime = alert.getEffectiveStartDate() != null
              ? alert.getEffectiveStartDate().toEpochMilli()
              : null;
            Long endTime = alert.getEffectiveEndDate() != null
              ? alert.getEffectiveEndDate().toEpochMilli()
              : null;
            return new ValidityPeriod(startTime, endTime);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("reportType")
          .type(EnumTypes.REPORT_TYPE)
          .description("ReportType of this situation")
          .dataFetcher(environment -> ((TransitAlert) environment.getSource()).type())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situationNumber")
          .type(Scalars.GraphQLString)
          .description("Operator's internal id for this situation")
          .dataFetcher(environment -> ((TransitAlert) environment.getSource()).getId().getId())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("severity")
          .type(EnumTypes.SEVERITY)
          .description("Severity of this situation ")
          .dataFetcher(environment ->
            getTransmodelSeverity(((TransitAlert) environment.getSource()).severity())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("priority")
          .type(Scalars.GraphQLInt)
          .description("Priority of this situation ")
          .dataFetcher(environment -> ((TransitAlert) environment.getSource()).priority())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("creationTime")
          .type(dateTimeScalar)
          .description("Timestamp for when the situation was created.")
          .dataFetcher(environment -> {
            final ZonedDateTime creationTime = environment.<TransitAlert>getSource().creationTime();
            return creationTime == null ? null : creationTime.toInstant().toEpochMilli();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("versionedAtTime")
          .type(dateTimeScalar)
          .description("Timestamp when the situation element was updated.")
          .dataFetcher(environment -> {
            final ZonedDateTime updatedTime = environment.<TransitAlert>getSource().updatedTime();
            return updatedTime == null ? null : updatedTime.toInstant().toEpochMilli();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("version")
          .type(Scalars.GraphQLInt)
          .description("Operator's version number for the situation element.")
          .dataFetcher(environment -> environment.<TransitAlert>getSource().version())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("participant")
          .type(Scalars.GraphQLString)
          .description("Codespace of the data source.")
          .dataFetcher(environment -> environment.<TransitAlert>getSource().siriCodespace())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("reportAuthority")
          .type(authorityType)
          .description(
            "Authority that reported this situation. Always returns the first agency in the codespace"
          )
          .deprecate("Not yet officially supported. May be removed or renamed.")
          .dataFetcher(environment -> {
            TransitAlert alert = environment.getSource();
            String feedId = alert.getId().getFeedId();
            String codespace = alert.siriCodespace();
            if (codespace == null) {
              return null;
            }
            return GqlUtil.getTransitService(environment)
              .listAgencies()
              .stream()
              .filter(agency -> agency.getId().getFeedId().equals(feedId))
              .filter(agency -> agency.getId().getId().startsWith(codespace))
              .findFirst()
              .orElse(null);
          })
          .build()
      )
      .build();
  }
}
