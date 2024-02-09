package org.opentripplanner.apis.transmodel;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.opentripplanner.apis.transmodel.mapping.SeverityMapper.getTransmodelSeverity;
import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.FILTER_PLACE_TYPE_ENUM;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.MULTI_MODAL_MODE;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import graphql.Scalars;
import graphql.relay.DefaultConnection;
import graphql.relay.DefaultPageInfo;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.apis.transmodel.mapping.PlaceMapper;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.apis.transmodel.model.DefaultRouteRequestType;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransmodelPlaceType;
import org.opentripplanner.apis.transmodel.model.framework.AuthorityType;
import org.opentripplanner.apis.transmodel.model.framework.BrandingType;
import org.opentripplanner.apis.transmodel.model.framework.InfoLinkType;
import org.opentripplanner.apis.transmodel.model.framework.MultilingualStringType;
import org.opentripplanner.apis.transmodel.model.framework.NoticeType;
import org.opentripplanner.apis.transmodel.model.framework.OperatorType;
import org.opentripplanner.apis.transmodel.model.framework.PenaltyForStreetModeType;
import org.opentripplanner.apis.transmodel.model.framework.PointsOnLinkType;
import org.opentripplanner.apis.transmodel.model.framework.RentalVehicleTypeType;
import org.opentripplanner.apis.transmodel.model.framework.ServerInfoType;
import org.opentripplanner.apis.transmodel.model.framework.StreetModeDurationInputType;
import org.opentripplanner.apis.transmodel.model.framework.SystemNoticeType;
import org.opentripplanner.apis.transmodel.model.framework.ValidityPeriodType;
import org.opentripplanner.apis.transmodel.model.network.DestinationDisplayType;
import org.opentripplanner.apis.transmodel.model.network.GroupOfLinesType;
import org.opentripplanner.apis.transmodel.model.network.JourneyPatternType;
import org.opentripplanner.apis.transmodel.model.network.LineType;
import org.opentripplanner.apis.transmodel.model.network.PresentationType;
import org.opentripplanner.apis.transmodel.model.network.StopToStopGeometryType;
import org.opentripplanner.apis.transmodel.model.plan.ElevationProfileStepType;
import org.opentripplanner.apis.transmodel.model.plan.LegType;
import org.opentripplanner.apis.transmodel.model.plan.PathGuidanceType;
import org.opentripplanner.apis.transmodel.model.plan.PlanPlaceType;
import org.opentripplanner.apis.transmodel.model.plan.RoutingErrorType;
import org.opentripplanner.apis.transmodel.model.plan.TripPatternType;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.apis.transmodel.model.plan.TripType;
import org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType;
import org.opentripplanner.apis.transmodel.model.plan.ViaSegmentInputType;
import org.opentripplanner.apis.transmodel.model.plan.ViaTripQuery;
import org.opentripplanner.apis.transmodel.model.plan.ViaTripType;
import org.opentripplanner.apis.transmodel.model.siri.et.EstimatedCallType;
import org.opentripplanner.apis.transmodel.model.siri.sx.AffectsType;
import org.opentripplanner.apis.transmodel.model.siri.sx.PtSituationElementType;
import org.opentripplanner.apis.transmodel.model.stop.BikeParkType;
import org.opentripplanner.apis.transmodel.model.stop.BikeRentalStationType;
import org.opentripplanner.apis.transmodel.model.stop.MonoOrMultiModalStation;
import org.opentripplanner.apis.transmodel.model.stop.PlaceAtDistanceType;
import org.opentripplanner.apis.transmodel.model.stop.PlaceInterfaceType;
import org.opentripplanner.apis.transmodel.model.stop.QuayAtDistanceType;
import org.opentripplanner.apis.transmodel.model.stop.QuayType;
import org.opentripplanner.apis.transmodel.model.stop.RentalVehicleType;
import org.opentripplanner.apis.transmodel.model.stop.StopPlaceType;
import org.opentripplanner.apis.transmodel.model.stop.TariffZoneType;
import org.opentripplanner.apis.transmodel.model.timetable.BookingArrangementType;
import org.opentripplanner.apis.transmodel.model.timetable.DatedServiceJourneyQuery;
import org.opentripplanner.apis.transmodel.model.timetable.DatedServiceJourneyType;
import org.opentripplanner.apis.transmodel.model.timetable.InterchangeType;
import org.opentripplanner.apis.transmodel.model.timetable.ServiceJourneyType;
import org.opentripplanner.apis.transmodel.model.timetable.TimetabledPassingTimeType;
import org.opentripplanner.apis.transmodel.model.timetable.TripMetadataType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schema definition for the Transmodel GraphQL API.
 * <p>
 * Currently a simplified version of the IndexGraphQLSchema, with gtfs terminology replaced with
 * corresponding terms from Transmodel.
 */
public class TransmodelGraphQLSchema {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLSchema.class);

  private final DefaultRouteRequestType routing;

  private final GqlUtil gqlUtil;

  private final Relay relay = new Relay();

  private TransmodelGraphQLSchema(RouteRequest defaultRequest, GqlUtil gqlUtil) {
    this.gqlUtil = gqlUtil;
    this.routing = new DefaultRouteRequestType(defaultRequest);
  }

  public static GraphQLSchema create(RouteRequest defaultRequest, GqlUtil gqlUtil) {
    return new TransmodelGraphQLSchema(defaultRequest, gqlUtil).create();
  }

  //    private BookingArrangement getBookingArrangementForTripTimeShort(TripTimeShort tripTimeShort) {
  //        Trip trip = index.tripForId.get(tripTimeShort.tripId);
  //        if (trip == null) {
  //            return null;
  //        }
  //        TripPattern tripPattern = index.patternForTrip.get(trip);
  //        if (tripPattern == null || tripPattern.stopPattern == null) {
  //            return null;
  //        }
  //        return tripPattern.stopPattern.bookingArrangements[tripTimeShort.stopIndex];
  //    }

  @SuppressWarnings("unchecked")
  private GraphQLSchema create() {
    /*
    multilingualStringType, validityPeriodType, infoLinkType, bookingArrangementType, systemNoticeType,
    linkGeometryType, serverInfoType, authorityType, operatorType, noticeType



    */

    // Framework
    GraphQLOutputType multilingualStringType = MultilingualStringType.create();
    GraphQLObjectType validityPeriodType = ValidityPeriodType.create(gqlUtil);
    GraphQLObjectType infoLinkType = InfoLinkType.create();
    GraphQLOutputType bookingArrangementType = BookingArrangementType.create(gqlUtil);
    GraphQLOutputType systemNoticeType = SystemNoticeType.create();
    GraphQLOutputType linkGeometryType = PointsOnLinkType.create();
    GraphQLOutputType serverInfoType = ServerInfoType.create();
    GraphQLOutputType authorityType = AuthorityType.create(
      LineType.REF,
      PtSituationElementType.REF,
      gqlUtil
    );
    GraphQLOutputType operatorType = OperatorType.create(
      LineType.REF,
      ServiceJourneyType.REF,
      gqlUtil
    );
    GraphQLOutputType brandingType = BrandingType.create();
    GraphQLOutputType noticeType = NoticeType.create();
    GraphQLOutputType rentalVehicleTypeType = RentalVehicleTypeType.create();

    // Stop
    GraphQLOutputType tariffZoneType = TariffZoneType.createTZ();
    GraphQLInterfaceType placeInterface = PlaceInterfaceType.create();
    GraphQLOutputType bikeRentalStationType = BikeRentalStationType.create(placeInterface);
    GraphQLOutputType rentalVehicleType = RentalVehicleType.create(
      rentalVehicleTypeType,
      placeInterface
    );
    GraphQLOutputType bikeParkType = BikeParkType.createB(placeInterface);
    GraphQLOutputType stopPlaceType = StopPlaceType.create(
      placeInterface,
      QuayType.REF,
      tariffZoneType,
      EstimatedCallType.REF,
      PtSituationElementType.REF,
      gqlUtil
    );
    GraphQLOutputType quayType = QuayType.create(
      placeInterface,
      stopPlaceType,
      LineType.REF,
      JourneyPatternType.REF,
      EstimatedCallType.REF,
      PtSituationElementType.REF,
      tariffZoneType,
      gqlUtil
    );

    GraphQLOutputType stopToStopGeometryType = StopToStopGeometryType.create(
      linkGeometryType,
      quayType
    );

    GraphQLNamedOutputType quayAtDistance = QuayAtDistanceType.createQD(quayType, relay);
    GraphQLNamedOutputType placeAtDistanceType = PlaceAtDistanceType.create(relay, placeInterface);

    // Network
    GraphQLObjectType presentationType = PresentationType.create();
    GraphQLOutputType groupOfLinesType = GroupOfLinesType.create();
    GraphQLOutputType destinationDisplayType = DestinationDisplayType.create();
    GraphQLOutputType lineType = LineType.create(
      bookingArrangementType,
      authorityType,
      operatorType,
      noticeType,
      quayType,
      presentationType,
      JourneyPatternType.REF,
      ServiceJourneyType.REF,
      PtSituationElementType.REF,
      brandingType,
      groupOfLinesType
    );
    GraphQLOutputType interchangeType = InterchangeType.create(lineType, ServiceJourneyType.REF);

    GraphQLOutputType affectsType = AffectsType.create(
      quayType,
      stopPlaceType,
      lineType,
      ServiceJourneyType.REF,
      DatedServiceJourneyType.REF,
      gqlUtil
    );

    // Timetable
    GraphQLNamedOutputType ptSituationElementType = PtSituationElementType.create(
      authorityType,
      quayType,
      stopPlaceType,
      lineType,
      ServiceJourneyType.REF,
      multilingualStringType,
      validityPeriodType,
      infoLinkType,
      affectsType,
      gqlUtil,
      relay
    );
    GraphQLOutputType journeyPatternType = JourneyPatternType.create(
      linkGeometryType,
      noticeType,
      quayType,
      lineType,
      ServiceJourneyType.REF,
      stopToStopGeometryType,
      ptSituationElementType,
      gqlUtil
    );
    GraphQLOutputType estimatedCallType = EstimatedCallType.create(
      bookingArrangementType,
      noticeType,
      quayType,
      destinationDisplayType,
      ptSituationElementType,
      ServiceJourneyType.REF,
      DatedServiceJourneyType.REF,
      gqlUtil
    );

    GraphQLOutputType serviceJourneyType = ServiceJourneyType.create(
      bookingArrangementType,
      linkGeometryType,
      operatorType,
      noticeType,
      quayType,
      lineType,
      ptSituationElementType,
      journeyPatternType,
      estimatedCallType,
      TimetabledPassingTimeType.REF,
      gqlUtil
    );

    GraphQLOutputType datedServiceJourneyType = DatedServiceJourneyType.create(
      serviceJourneyType,
      journeyPatternType,
      estimatedCallType,
      quayType,
      gqlUtil
    );

    GraphQLOutputType timetabledPassingTime = TimetabledPassingTimeType.create(
      bookingArrangementType,
      noticeType,
      quayType,
      destinationDisplayType,
      serviceJourneyType,
      gqlUtil
    );

    GraphQLObjectType tripMetadataType = TripMetadataType.create(gqlUtil);
    GraphQLObjectType placeType = PlanPlaceType.create(
      bikeRentalStationType,
      rentalVehicleType,
      quayType
    );
    GraphQLObjectType elevationStepType = ElevationProfileStepType.create();
    GraphQLObjectType pathGuidanceType = PathGuidanceType.create(elevationStepType);
    GraphQLObjectType legType = LegType.create(
      bookingArrangementType,
      interchangeType,
      linkGeometryType,
      authorityType,
      operatorType,
      quayType,
      estimatedCallType,
      lineType,
      serviceJourneyType,
      datedServiceJourneyType,
      ptSituationElementType,
      placeType,
      pathGuidanceType,
      elevationStepType,
      gqlUtil
    );
    GraphQLObjectType tripPatternType = TripPatternType.create(systemNoticeType, legType, gqlUtil);
    GraphQLObjectType routingErrorType = RoutingErrorType.create();

    GraphQLOutputType tripType = TripType.create(
      placeType,
      tripPatternType,
      tripMetadataType,
      routingErrorType,
      gqlUtil
    );

    GraphQLInputObjectType durationPerStreetModeInput = StreetModeDurationInputType.create(gqlUtil);
    GraphQLInputObjectType penaltyForStreetMode = PenaltyForStreetModeType.create(gqlUtil);

    GraphQLFieldDefinition tripQuery = TripQuery.create(
      routing,
      tripType,
      durationPerStreetModeInput,
      penaltyForStreetMode,
      gqlUtil
    );

    GraphQLOutputType viaTripType = ViaTripType.create(tripPatternType, routingErrorType);
    GraphQLInputObjectType viaLocationInputType = ViaLocationInputType.create(gqlUtil);
    GraphQLInputObjectType viaSegmentInputType = ViaSegmentInputType.create();

    GraphQLFieldDefinition viaTripQuery = ViaTripQuery.create(
      routing,
      viaTripType,
      viaLocationInputType,
      viaSegmentInputType,
      gqlUtil
    );

    GraphQLInputObjectType inputPlaceIds = GraphQLInputObjectType
      .newInputObject()
      .name("InputPlaceIds")
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("quays")
          .description("Quays to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("lines")
          .description("Lines to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("bikeRentalStations")
          .description("Bike rentals to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("bikeParks")
          .description("Bike parks to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("carParks")
          .description("Car parks to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .build();

    GraphQLObjectType queryType = GraphQLObjectType
      .newObject()
      .name("QueryType")
      .field(tripQuery)
      .field(viaTripQuery)
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stopPlace")
          .description("Get a single stopPlace based on its id)")
          .withDirective(gqlUtil.timingData)
          .type(stopPlaceType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(env ->
            StopPlaceType.fetchStopPlaceById(
              TransitIdMapper.mapIDToDomain(env.getArgument("id")),
              env
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stopPlaces")
          .description("Get all stopPlaces")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("ids")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(env -> {
            if ((env.getArgument("ids") instanceof List)) {
              return ((List<String>) env.getArgument("ids")).stream()
                .map(TransitIdMapper::mapIDToDomain)
                .map(id -> StopPlaceType.fetchStopPlaceById(id, env))
                .collect(Collectors.toList());
            }
            TransitService transitService = GqlUtil.getTransitService(env);
            return transitService
              .getStations()
              .stream()
              .map(station ->
                new MonoOrMultiModalStation(
                  station,
                  transitService.getMultiModalStationForStation(station)
                )
              )
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stopPlacesByBbox")
          .description("Get all stop places within the specified bounding box")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("minimumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("minimumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument().name("authority").type(Scalars.GraphQLString).build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("multiModalMode")
              .type(MULTI_MODAL_MODE)
              .description(
                "MultiModalMode for query. To control whether multi modal parent stop places, their mono modal children or both are included in the response." +
                " Does not affect mono modal stop places that do not belong to a multi modal stop place."
              )
              .defaultValue("parent")
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("filterByInUse")
              .description("If true only stop places with at least one visiting line are included.")
              .type(Scalars.GraphQLBoolean)
              .defaultValue(Boolean.FALSE)
              .build()
          )
          .dataFetcher(env -> {
            double minLat = env.getArgument("minimumLatitude");
            double minLon = env.getArgument("minimumLongitude");
            double maxLat = env.getArgument("maximumLatitude");
            double maxLon = env.getArgument("maximumLongitude");
            String authority = env.getArgument("authority");
            boolean filterByInUse = TRUE.equals(env.getArgument("filterByInUse"));
            String multiModalMode = env.getArgument("multiModalMode");

            return StopPlaceType.fetchStopPlaces(
              minLat,
              minLon,
              maxLat,
              maxLon,
              authority,
              filterByInUse,
              multiModalMode,
              env
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quay")
          .description("Get a single quay based on its id)")
          .withDirective(gqlUtil.timingData)
          .type(quayType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getStopLocation(TransitIdMapper.mapIDToDomain(environment.getArgument("id")))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quays")
          .description("Get all quays")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(quayType)))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("ids")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(GraphQLArgument.newArgument().name("name").type(Scalars.GraphQLString).build())
          .dataFetcher(environment -> {
            if ((environment.getArgument("ids") instanceof List)) {
              if (
                environment
                  .getArguments()
                  .entrySet()
                  .stream()
                  .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                  .count() !=
                1
              ) {
                throw new IllegalArgumentException("Unable to combine other filters with ids");
              }
              TransitService transitService = GqlUtil.getTransitService(environment);
              return ((List<String>) environment.getArgument("ids")).stream()
                .map(id -> transitService.getStopLocation(TransitIdMapper.mapIDToDomain(id)))
                .collect(Collectors.toList());
            }
            if (environment.getArgument("name") == null) {
              return GqlUtil.getTransitService(environment).listStopLocations();
            }
            //                            else {
            //                                return index.getLuceneIndex().query(environment.getArgument("name"), true, true, false)
            //                                        .stream()
            //                                        .map(result -> index.stopForId.get(mapper.fromIdString(result.id)));
            //                            }
            return emptyList();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quaysByBbox")
          .description("Get all quays within the specified bounding box")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(quayType)))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("minimumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("minimumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument().name("authority").type(Scalars.GraphQLString).build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("filterByInUse")
              .description("If true only quays with at least one visiting line are included.")
              .type(Scalars.GraphQLBoolean)
              .defaultValue(Boolean.FALSE)
              .build()
          )
          .dataFetcher(environment -> {
            Envelope envelope = new Envelope(
              new Coordinate(
                environment.getArgument("minimumLongitude"),
                environment.getArgument("minimumLatitude")
              ),
              new Coordinate(
                environment.getArgument("maximumLongitude"),
                environment.getArgument("maximumLatitude")
              )
            );
            return GqlUtil
              .getTransitService(environment)
              .findRegularStops(envelope)
              .stream()
              .filter(stop -> envelope.contains(stop.getCoordinate().asJtsCoordinate()))
              .filter(stop ->
                environment.getArgument("authority") == null ||
                stop.getId().getFeedId().equalsIgnoreCase(environment.getArgument("authority"))
              )
              .filter(stop -> {
                boolean filterByInUse = TRUE.equals(environment.getArgument("filterByInUse"));
                boolean inUse = !GqlUtil
                  .getTransitService(environment)
                  .getPatternsForStop(stop, true)
                  .isEmpty();
                return !filterByInUse || inUse;
              })
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quaysByRadius")
          .description(
            "Get all quays within the specified walking radius from a location. There are no maximum " +
            "limits for the input parameters, but the query will timeout and return if the parameters " +
            "are too high."
          )
          .withDirective(gqlUtil.timingData)
          .type(
            relay.connectionType(
              "quayAtDistance",
              relay.edgeType("quayAtDistance", quayAtDistance, null, new ArrayList<>()),
              new ArrayList<>()
            )
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("latitude")
              .description("Latitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("longitude")
              .description("Longitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("radius")
              .description(
                "Radius via streets (in meters) to search for from the specified location"
              )
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument().name("authority").type(Scalars.GraphQLString).build()
          )
          .arguments(relay.getConnectionFieldArguments())
          .dataFetcher(environment -> {
            List<NearbyStop> stops;
            try {
              stops =
                GqlUtil
                  .getGraphFinder(environment)
                  .findClosestStops(
                    new Coordinate(
                      environment.getArgument("longitude"),
                      environment.getArgument("latitude")
                    ),
                    environment.getArgument("radius")
                  )
                  .stream()
                  .filter(stopAtDistance ->
                    environment.getArgument("authority") == null ||
                    stopAtDistance.stop
                      .getId()
                      .getFeedId()
                      .equalsIgnoreCase(environment.getArgument("authority"))
                  )
                  .sorted(Comparator.comparing(s -> s.distance))
                  .collect(Collectors.toList());
            } catch (RoutingValidationException e) {
              LOG.warn(
                "findClosestPlacesByWalking failed with exception, returning empty list of places. ",
                e
              );
              stops = List.of();
            }

            if (stops.isEmpty()) {
              return new DefaultConnection<>(
                emptyList(),
                new DefaultPageInfo(null, null, false, false)
              );
            }
            return new SimpleListConnection<>(stops).get(environment);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("nearest")
          .description(
            "Get all places (quays, stop places, car parks etc. with coordinates) within the specified radius from a location. The returned type has two fields place and distance. The search is done by walking so the distance is according to the network of walkables."
          )
          .withDirective(gqlUtil.timingData)
          .type(
            relay.connectionType(
              "placeAtDistance",
              relay.edgeType("placeAtDistance", placeAtDistanceType, null, new ArrayList<>()),
              new ArrayList<>()
            )
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("latitude")
              .description("Latitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("longitude")
              .description("Longitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumDistance")
              .description(
                "Maximum distance (in meters) to search for from the specified location. Default is 2000m."
              )
              .defaultValueProgrammatic(2000)
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumResults")
              .description(
                "Maximum number of results. Search is stopped when this limit is reached. Default is 20."
              )
              .defaultValue(20)
              .type(Scalars.GraphQLInt)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("filterByPlaceTypes")
              .description("Only include places of given types if set. Default accepts all types")
              .defaultValue(Arrays.asList(TransmodelPlaceType.values()))
              .type(new GraphQLList(FILTER_PLACE_TYPE_ENUM))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("filterByModes")
              .description(
                "Only include places that include this mode. Only checked for places with mode i.e. quays, departures."
              )
              .type(new GraphQLList(TRANSPORT_MODE))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("filterByInUse")
              .description(
                "Only affects queries for quays and stop places. If true only quays and stop places with at least one visiting line are included."
              )
              .type(Scalars.GraphQLBoolean)
              .defaultValue(Boolean.FALSE)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("filterByIds")
              .description("Only include places that match one of the given ids.")
              .type(inputPlaceIds)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("multiModalMode")
              .type(MULTI_MODAL_MODE)
              .description(
                "MultiModalMode for query. To control whether multi modal parent stop places, their mono modal children or both are included in the response." +
                " Does not affect mono modal stop places that do not belong to a multi modal stop place. Only applicable for placeType StopPlace"
              )
              .defaultValue("parent")
              .build()
          )
          .argument(relay.getConnectionFieldArguments())
          .dataFetcher(environment -> {
            List<FeedScopedId> filterByStops = null;
            List<FeedScopedId> filterByStations = null;
            List<FeedScopedId> filterByRoutes = null;
            List<String> filterByBikeRentalStations = null;
            List<String> filterByBikeParks = null;
            List<String> filterByCarParks = null;
            @SuppressWarnings("rawtypes")
            Map filterByIds = environment.getArgument("filterByIds");
            if (filterByIds != null) {
              filterByStops = toIdList(((List<String>) filterByIds.get("quays")));
              filterByRoutes = toIdList(((List<String>) filterByIds.get("lines")));
              filterByBikeRentalStations =
                filterByIds.get("bikeRentalStations") != null
                  ? (List<String>) filterByIds.get("bikeRentalStations")
                  : List.of();
              filterByBikeParks =
                filterByIds.get("bikeParks") != null
                  ? (List<String>) filterByIds.get("bikeParks")
                  : List.of();
              filterByCarParks =
                filterByIds.get("carParks") != null
                  ? (List<String>) filterByIds.get("carParks")
                  : List.of();
            }

            List<TransitMode> filterByTransportModes = environment.getArgument("filterByModes");
            List<TransmodelPlaceType> placeTypes = environment.getArgument("filterByPlaceTypes");
            if (placeTypes == null || placeTypes.isEmpty()) {
              placeTypes = Arrays.asList(TransmodelPlaceType.values());
            }
            List<PlaceType> filterByPlaceTypes = PlaceMapper.mapToDomain(placeTypes);

            // Need to fetch more than requested no of places if stopPlaces are allowed, as this requires fetching potentially multiple quays for the same stop place and mapping them to unique stop places.
            int orgMaxResults = environment.getArgument("maximumResults");
            int maxResults = orgMaxResults;
            if (placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
              maxResults *= 5;
            }

            List<PlaceAtDistance> places;
            places =
              GqlUtil
                .getGraphFinder(environment)
                .findClosestPlaces(
                  environment.getArgument("latitude"),
                  environment.getArgument("longitude"),
                  environment.getArgument("maximumDistance"),
                  maxResults,
                  filterByTransportModes,
                  filterByPlaceTypes,
                  filterByStops,
                  filterByStations,
                  filterByRoutes,
                  filterByBikeRentalStations,
                  GqlUtil.getTransitService(environment)
                );

            if (TRUE.equals(environment.getArgument("filterByInUse"))) {
              places =
                places
                  .stream()
                  .filter(placeAtDistance -> {
                    if (placeAtDistance.place() instanceof StopLocation stop) {
                      return !GqlUtil
                        .getTransitService(environment)
                        .getPatternsForStop(stop, true)
                        .isEmpty();
                    } else {
                      return true;
                    }
                  })
                  .toList();
            }

            places =
              PlaceAtDistanceType
                .convertQuaysToStopPlaces(
                  placeTypes,
                  places,
                  environment.getArgument("multiModalMode"),
                  GqlUtil.getTransitService(environment)
                )
                .stream()
                .limit(orgMaxResults)
                .collect(Collectors.toList());
            if (places.isEmpty()) {
              return new DefaultConnection<>(
                List.of(),
                new DefaultPageInfo(null, null, false, false)
              );
            }
            return new SimpleListConnection(places).get(environment);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("authority")
          .description("Get an authority by ID")
          .withDirective(gqlUtil.timingData)
          .type(authorityType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            return GqlUtil
              .getTransitService(environment)
              .getAgencyForId(TransitIdMapper.mapIDToDomain(environment.getArgument("id")));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("authorities")
          .description("Get all authorities")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(authorityType)))
          .dataFetcher(environment -> {
            return new ArrayList<>(GqlUtil.getTransitService(environment).getAgencies());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("operator")
          .description("Get a operator by ID")
          .withDirective(gqlUtil.timingData)
          .type(operatorType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getOperatorForId(TransitIdMapper.mapIDToDomain(environment.getArgument("id")))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("operators")
          .description("Get all operators")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(operatorType)))
          .dataFetcher(environment -> {
            return new ArrayList<>(GqlUtil.getTransitService(environment).getAllOperators());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("line")
          .description("Get a single line based on its id")
          .withDirective(gqlUtil.timingData)
          .type(lineType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLID))
              .build()
          )
          .dataFetcher(environment -> {
            final String id = environment.getArgument("id");
            if (id.isBlank()) {
              return null;
            }
            return GqlUtil
              .getTransitService(environment)
              .getRouteForId(TransitIdMapper.mapIDToDomain(id));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("lines")
          .description("Get all lines")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(lineType)))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("ids")
              .type(new GraphQLList(Scalars.GraphQLID))
              .build()
          )
          .argument(GraphQLArgument.newArgument().name("name").type(Scalars.GraphQLString).build())
          .argument(
            GraphQLArgument.newArgument().name("publicCode").type(Scalars.GraphQLString).build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("publicCodes")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("transportModes")
              .type(new GraphQLList(TRANSPORT_MODE))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("authorities")
              .description("Set of ids of authorities to fetch lines for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("flexibleOnly")
              .description("Filter by lines containing flexible / on demand serviceJourneys only.")
              .type(Scalars.GraphQLBoolean)
              .defaultValue(false)
              .build()
          )
          .dataFetcher(environment -> {
            if ((environment.getArgument("ids") instanceof List)) {
              if (
                environment
                  .getArguments()
                  .entrySet()
                  .stream()
                  .filter(it ->
                    it.getValue() != null &&
                    !(it.getKey().equals("flexibleOnly") && it.getValue().equals(false))
                  )
                  .count() !=
                1
              ) {
                throw new IllegalArgumentException("Unable to combine other filters with ids");
              }
              return ((List<String>) environment.getArgument("ids")).stream()
                .map(TransitIdMapper::mapIDToDomain)
                .map(id -> {
                  return GqlUtil.getTransitService(environment).getRouteForId(id);
                })
                .collect(Collectors.toList());
            }
            Stream<Route> stream = GqlUtil.getTransitService(environment).getAllRoutes().stream();

            if ((boolean) environment.getArgument("flexibleOnly")) {
              Collection<Route> flexRoutes = GqlUtil
                .getTransitService(environment)
                .getFlexIndex()
                .getAllFlexRoutes();
              stream = stream.filter(flexRoutes::contains);
            }
            if (environment.getArgument("name") != null) {
              stream =
                stream
                  .filter(route -> route.getLongName() != null)
                  .filter(route ->
                    route
                      .getLongName()
                      .toString()
                      .toLowerCase()
                      .startsWith(((String) environment.getArgument("name")).toLowerCase())
                  );
            }
            if (environment.getArgument("publicCode") != null) {
              stream =
                stream
                  .filter(route -> route.getShortName() != null)
                  .filter(route ->
                    route.getShortName().equals(environment.getArgument("publicCode"))
                  );
            }
            if (environment.getArgument("publicCodes") instanceof List) {
              Set<String> publicCodes = Set.copyOf(environment.getArgument("publicCodes"));
              stream =
                stream
                  .filter(route -> route.getShortName() != null)
                  .filter(route -> publicCodes.contains(route.getShortName()));
            }
            if (environment.getArgument("transportModes") != null) {
              Set<TransitMode> modes = Set.copyOf(environment.getArgument("transportModes"));
              stream = stream.filter(route -> modes.contains(route.getMode()));
            }
            if ((environment.getArgument("authorities") instanceof Collection)) {
              Collection<String> authorityIds = environment.getArgument("authorities");
              stream =
                stream.filter(route ->
                  route.getAgency() != null &&
                  authorityIds.contains(route.getAgency().getId().getId())
                );
            }
            return stream.collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("groupOfLines")
          .description("Get a single group of lines based on its id")
          .type(groupOfLinesType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getGroupOfRoutesForId(TransitIdMapper.mapIDToDomain(environment.getArgument("id")))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("groupsOfLines")
          .description("Get all groups of lines")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(groupOfLinesType))))
          .dataFetcher(environment -> GqlUtil.getTransitService(environment).getGroupsOfRoutes())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourney")
          .description("Get a single service journey based on its id")
          .withDirective(gqlUtil.timingData)
          .type(serviceJourneyType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            return GqlUtil
              .getTransitService(environment)
              .getTripForId(TransitIdMapper.mapIDToDomain(environment.getArgument("id")));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourneys")
          .description("Get all service journeys")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("lines")
              .description("Set of ids of lines to fetch serviceJourneys for.")
              .type(new GraphQLList(Scalars.GraphQLID))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("privateCodes")
              .description("Set of ids of private codes to fetch serviceJourneys for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("activeDates")
              .description("Set of ids of active dates to fetch serviceJourneys for.")
              .type(new GraphQLList(gqlUtil.dateScalar))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("authorities")
              .description("Set of ids of authorities to fetch serviceJourneys for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            List<FeedScopedId> lineIds = mapIDsToDomainNullSafe(
              environment.getArgumentOrDefault("lines", List.of())
            );
            List<String> privateCodes = environment.getArgumentOrDefault("privateCodes", List.of());
            List<LocalDate> activeServiceDates = environment.getArgumentOrDefault(
              "activeDates",
              List.of()
            );

            // TODO OTP2 - Use FeedScoped ID
            List<String> authorities = environment.getArgumentOrDefault("authorities", List.of());
            TransitService transitService = GqlUtil.getTransitService(environment);
            return transitService
              .getAllTrips()
              .stream()
              .filter(t -> lineIds.isEmpty() || lineIds.contains(t.getRoute().getId()))
              .filter(t ->
                privateCodes.isEmpty() || privateCodes.contains(t.getNetexInternalPlanningCode())
              )
              .filter(t ->
                authorities.isEmpty() ||
                authorities.contains(t.getRoute().getAgency().getId().getId())
              )
              .filter(t ->
                (
                  activeServiceDates.isEmpty() ||
                  transitService
                    .getCalendarService()
                    .getServiceDatesForServiceId(t.getServiceId())
                    .stream()
                    .anyMatch(activeServiceDates::contains)
                )
              )
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikeRentalStations")
          .description("Get all bike rental stations")
          .withDirective(gqlUtil.timingData)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("ids")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
          .dataFetcher(environment -> {
            Collection<VehicleRentalPlace> all = new ArrayList<>(
              GqlUtil.getVehicleRentalService(environment).getVehicleRentalStations()
            );
            List<String> filterByIds = environment.getArgument("ids");
            if (filterByIds != null && !filterByIds.isEmpty()) {
              return all
                .stream()
                .filter(station -> filterByIds.contains(station.getStationId()))
                .collect(Collectors.toList());
            }
            return all;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikeRentalStation")
          .description("Get all bike rental stations")
          .withDirective(gqlUtil.timingData)
          .type(bikeRentalStationType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            return GqlUtil
              .getVehicleRentalService(environment)
              .getVehicleRentalStations()
              .stream()
              .filter(bikeRentalStation ->
                bikeRentalStation.getStationId().equals(environment.getArgument("id"))
              )
              .findFirst()
              .orElse(null);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikeRentalStationsByBbox")
          .description("Get all bike rental stations within the specified bounding box.")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
          .argument(
            GraphQLArgument.newArgument().name("minimumLatitude").type(Scalars.GraphQLFloat).build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("minimumLongitude")
              .type(Scalars.GraphQLFloat)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument().name("maximumLatitude").type(Scalars.GraphQLFloat).build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("maximumLongitude")
              .type(Scalars.GraphQLFloat)
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil
              .getVehicleRentalService(environment)
              .getVehicleRentalStationForEnvelope(
                environment.getArgument("minimumLongitude"),
                environment.getArgument("minimumLatitude"),
                environment.getArgument("maximumLongitude"),
                environment.getArgument("maximumLatitude")
              )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikePark")
          .description("Get a single bike park based on its id")
          .withDirective(gqlUtil.timingData)
          .type(bikeParkType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            var bikeParkId = TransitIdMapper.mapIDToDomain(environment.getArgument("id"));
            return GqlUtil
              .getVehicleParkingService(environment)
              .getBikeParks()
              .filter(bikePark -> bikePark.getId().equals(bikeParkId))
              .findFirst()
              .orElse(null);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikeParks")
          .description("Get all bike parks")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(bikeParkType)))
          .dataFetcher(environment ->
            GqlUtil
              .getVehicleParkingService(environment)
              .getBikeParks()
              .collect(Collectors.toCollection(ArrayList::new))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("routingParameters")
          .description("Get default routing parameters.")
          .withDirective(gqlUtil.timingData)
          .type(this.routing.graphQLType)
          .dataFetcher(environment -> routing.request)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("situations")
          .description("Get all active situations.")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("authorities")
              .description("Filter by reporting authorities.")
              .deprecate(
                "Use codespaces instead. This only uses the codespace of the given authority."
              )
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("codespaces")
              .description("Filter by reporting source.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("severities")
              .description("Filter by severity.")
              .type(new GraphQLList(EnumTypes.SEVERITY))
              .build()
          )
          .dataFetcher(environment -> {
            Collection<TransitAlert> alerts = GqlUtil
              .getTransitService(environment)
              .getTransitAlertService()
              .getAllAlerts();

            Set<String> codespaces = new HashSet<>();

            if (environment.getArgument("authorities") instanceof List) {
              List<String> authorities = environment.getArgument("authorities");
              authorities
                .stream()
                .map(authority -> authority.split(":")[0])
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isBlank))
                .forEach(codespaces::add);
            }

            if (environment.getArgument("codespaces") instanceof List) {
              codespaces.addAll((environment.getArgument("codespaces")));
            }

            if (!codespaces.isEmpty()) {
              alerts =
                alerts
                  .stream()
                  .filter(alert -> codespaces.contains(alert.siriCodespace()))
                  .collect(Collectors.toSet());
            }

            if (environment.getArgument("severities") instanceof List) {
              List<String> severities = environment.getArgument("severities");
              alerts =
                alerts
                  .stream()
                  .filter(alert -> severities.contains(getTransmodelSeverity(alert.severity())))
                  .collect(Collectors.toSet());
            }
            return alerts;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("situation")
          .description("Get a single situation based on its situationNumber")
          .withDirective(gqlUtil.timingData)
          .type(ptSituationElementType)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("situationNumber")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            String situationNumber = environment.getArgument("situationNumber");
            if (situationNumber.isBlank()) {
              return null;
            }
            return GqlUtil
              .getTransitService(environment)
              .getTransitAlertService()
              .getAlertById(TransitIdMapper.mapIDToDomain(situationNumber));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("leg")
          .description("Refetch a single leg based on its id")
          .withDirective(gqlUtil.timingData)
          .type(LegType.REF)
          .argument(
            GraphQLArgument
              .newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLID))
              .build()
          )
          .dataFetcher(environment -> {
            final String id = environment.getArgument("id");
            if (id.isBlank()) {
              return null;
            }
            LegReference ref = LegReferenceSerializer.decode(id);
            if (ref == null) {
              return null;
            }
            return ref.getLeg(GqlUtil.getTransitService(environment));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serverInfo")
          .description("Get OTP server information")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(serverInfoType))
          .dataFetcher(e -> projectInfo())
          .build()
      )
      .field(DatedServiceJourneyQuery.createGetById(datedServiceJourneyType))
      .field(DatedServiceJourneyQuery.createQuery(datedServiceJourneyType, gqlUtil))
      .build();

    return GraphQLSchema
      .newSchema()
      .query(queryType)
      .additionalType(placeInterface)
      .additionalType(timetabledPassingTime)
      .additionalType(Relay.pageInfoType)
      .additionalDirective(gqlUtil.timingData)
      .build();
  }

  private List<FeedScopedId> toIdList(List<String> ids) {
    if (ids == null) {
      return Collections.emptyList();
    }
    return ids.stream().map(TransitIdMapper::mapIDToDomain).collect(Collectors.toList());
  }
}
