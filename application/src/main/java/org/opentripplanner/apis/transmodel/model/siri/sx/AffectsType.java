package org.opentripplanner.apis.transmodel.model.siri.sx;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLUnionType;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.model.stop.StopPlaceType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;

public class AffectsType {

  private static final String NAME = "Affects";

  public static GraphQLOutputType create(
    GraphQLOutputType quayType,
    GraphQLOutputType stopPlaceType,
    GraphQLOutputType lineType,
    GraphQLOutputType serviceJourneyType,
    GraphQLOutputType datedServiceJourneyType
  ) {
    GraphQLObjectType affectedStopPlace = GraphQLObjectType.newObject()
      .name("AffectedStopPlace")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(quayType)
          .dataFetcher(environment -> {
            FeedScopedId stopId = environment.<EntitySelector.Stop>getSource().stopId();
            return GqlUtil.getTransitService(environment).getRegularStop(stopId);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlace")
          .type(stopPlaceType)
          .dataFetcher(environment -> {
            FeedScopedId stopId = environment.<EntitySelector.Stop>getSource().stopId();
            return StopPlaceType.fetchStopPlaceById(stopId, environment);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopConditions")
          .type(
            new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(EnumTypes.STOP_CONDITION_ENUM)))
          )
          .build()
      )
      .build();

    GraphQLObjectType affectedLine = GraphQLObjectType.newObject()
      .name("AffectedLine")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("line")
          .type(lineType)
          .dataFetcher(environment -> {
            var routeId = environment.<EntitySelector.Route>getSource().routeId();
            return GqlUtil.getTransitService(environment).getRoute(routeId);
          })
          .build()
      )
      .build();

    GraphQLObjectType affectedServiceJourney = GraphQLObjectType.newObject()
      .name("AffectedServiceJourney")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .type(serviceJourneyType)
          .dataFetcher(environment -> {
            var tripId = environment.<EntitySelector.Trip>getSource().tripId();
            return GqlUtil.getTransitService(environment).getTrip(tripId);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operatingDay")
          .type(TransmodelScalars.DATE_SCALAR)
          .dataFetcher(environment -> environment.<EntitySelector.Trip>getSource().serviceDate())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .type(datedServiceJourneyType)
          .dataFetcher(environment -> {
            EntitySelector.Trip entitySelector = environment.getSource();
            return GqlUtil.getTransitService(environment).getTripOnServiceDate(
              new TripIdAndServiceDate(entitySelector.tripId(), entitySelector.serviceDate())
            );
          })
          .build()
      )
      .build();

    GraphQLObjectType affectedStopPlaceOnLine = GraphQLObjectType.newObject()
      .name("AffectedStopPlaceOnLine")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(quayType)
          .dataFetcher(environment -> {
            FeedScopedId stopId = environment.<EntitySelector.StopAndRoute>getSource().stopId();
            return GqlUtil.getTransitService(environment).getRegularStop(stopId);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlace")
          .type(stopPlaceType)
          .dataFetcher(environment -> {
            FeedScopedId stopId = environment.<EntitySelector.StopAndRoute>getSource().stopId();
            return StopPlaceType.fetchStopPlaceById(stopId, environment);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("line")
          .type(lineType)
          .dataFetcher(environment -> {
            var routeId = environment.<EntitySelector.StopAndRoute>getSource().routeId();
            return GqlUtil.getTransitService(environment).getRoute(routeId);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopConditions")
          .type(
            new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(EnumTypes.STOP_CONDITION_ENUM)))
          )
          .build()
      )
      .build();

    GraphQLObjectType affectedStopPlaceOnServiceJourney = GraphQLObjectType.newObject()
      .name("AffectedStopPlaceOnServiceJourney")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(quayType)
          .dataFetcher(environment -> {
            FeedScopedId stopId = environment.<EntitySelector.StopAndTrip>getSource().stopId();
            return GqlUtil.getTransitService(environment).getRegularStop(stopId);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlace")
          .type(stopPlaceType)
          .dataFetcher(environment -> {
            FeedScopedId stopId = environment.<EntitySelector.StopAndTrip>getSource().stopId();
            return StopPlaceType.fetchStopPlaceById(stopId, environment);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .type(serviceJourneyType)
          .dataFetcher(environment -> {
            var tripId = environment.<EntitySelector.StopAndTrip>getSource().tripId();
            return GqlUtil.getTransitService(environment).getTrip(tripId);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operatingDay")
          .type(TransmodelScalars.DATE_SCALAR)
          .dataFetcher(environment ->
            environment.<EntitySelector.StopAndTrip>getSource().serviceDate()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .type(datedServiceJourneyType)
          .dataFetcher(environment -> {
            EntitySelector.StopAndTrip entitySelector = environment.getSource();
            return GqlUtil.getTransitService(environment).getTripOnServiceDate(
              new TripIdAndServiceDate(entitySelector.tripId(), entitySelector.serviceDate())
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopConditions")
          .type(
            new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(EnumTypes.STOP_CONDITION_ENUM)))
          )
          .build()
      )
      .build();

    GraphQLObjectType affectedUnknown = GraphQLObjectType.newObject()
      .name("AffectedUnknown")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            var object = environment.getSource();

            if (object instanceof EntitySelector.Unknown unknownEntitySelector) {
              return unknownEntitySelector.description();
            }

            // Fallback to toString
            return object.toString();
          })
          .build()
      )
      .build();

    return GraphQLUnionType.newUnionType()
      .name(NAME)
      .possibleType(affectedStopPlace)
      .possibleType(affectedLine)
      .possibleType(affectedServiceJourney)
      .possibleType(affectedStopPlaceOnLine)
      .possibleType(affectedStopPlaceOnServiceJourney)
      .possibleType(affectedUnknown)
      .typeResolver(env -> {
        var object = env.getObject();

        if (object instanceof EntitySelector.Stop) {
          return affectedStopPlace;
        } else if (object instanceof EntitySelector.Route) {
          return affectedLine;
        } else if (object instanceof EntitySelector.Trip) {
          return affectedServiceJourney;
        } else if (object instanceof EntitySelector.StopAndRoute) {
          return affectedStopPlaceOnLine;
        } else if (object instanceof EntitySelector.StopAndTrip) {
          return affectedStopPlaceOnServiceJourney;
        }

        return affectedUnknown;
      })
      .build();
  }
}
