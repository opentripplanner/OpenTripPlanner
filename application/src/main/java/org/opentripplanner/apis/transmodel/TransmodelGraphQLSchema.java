package org.opentripplanner.apis.transmodel;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.opentripplanner.apis.transmodel.mapping.SeverityMapper.getTransmodelSeverity;
import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDToDomain;
import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomain;
import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.FILTER_PLACE_TYPE_ENUM;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.MULTI_MODAL_MODE;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.apis.transmodel.model.scalars.DateTimeScalarFactory.createMillisecondsSinceEpochAsDateTimeStringScalar;
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
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.SchemaTransformer;
import java.time.ZoneId;
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
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.apis.support.graphql.injectdoc.ApiDocumentationProfile;
import org.opentripplanner.apis.support.graphql.injectdoc.CustomDocumentation;
import org.opentripplanner.apis.support.graphql.injectdoc.InjectCustomDocumentation;
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
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
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
import org.opentripplanner.apis.transmodel.model.plan.TripPatternTimePenaltyType;
import org.opentripplanner.apis.transmodel.model.plan.TripPatternType;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.apis.transmodel.model.plan.TripType;
import org.opentripplanner.apis.transmodel.model.plan.legacyvia.ViaLocationInputType;
import org.opentripplanner.apis.transmodel.model.plan.legacyvia.ViaSegmentInputType;
import org.opentripplanner.apis.transmodel.model.plan.legacyvia.ViaTripQuery;
import org.opentripplanner.apis.transmodel.model.plan.legacyvia.ViaTripType;
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
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.FindRegularStopsByBoundingBoxRequest;
import org.opentripplanner.transit.api.request.FindRoutesRequest;
import org.opentripplanner.transit.api.request.FindStopLocationsRequest;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
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

  private final TransitTuningParameters transitTuningParameters;

  private final ZoneId timeZoneId;

  private final Relay relay = new Relay();

  private TransmodelGraphQLSchema(
    RouteRequest defaultRequest,
    ZoneId timeZoneId,
    TransitTuningParameters transitTuningParameters
  ) {
    this.timeZoneId = timeZoneId;
    this.routing = new DefaultRouteRequestType(defaultRequest);
    this.transitTuningParameters = transitTuningParameters;
  }

  public static GraphQLSchema create(
    RouteRequest defaultRequest,
    ZoneId timeZoneId,
    ApiDocumentationProfile docProfile,
    TransitTuningParameters transitTuning
  ) {
    var schema = new TransmodelGraphQLSchema(defaultRequest, timeZoneId, transitTuning).create();
    schema = decorateSchemaWithCustomDocumentation(schema, docProfile);
    return schema;
  }

  @SuppressWarnings("unchecked")
  private GraphQLSchema create() {
    // Framework
    GraphQLScalarType dateTimeScalar = createMillisecondsSinceEpochAsDateTimeStringScalar(
      timeZoneId
    );
    GraphQLOutputType multilingualStringType = MultilingualStringType.create();
    GraphQLObjectType validityPeriodType = ValidityPeriodType.create(dateTimeScalar);
    GraphQLObjectType infoLinkType = InfoLinkType.create();
    GraphQLOutputType bookingArrangementType = BookingArrangementType.create();
    GraphQLOutputType systemNoticeType = SystemNoticeType.create();
    GraphQLOutputType linkGeometryType = PointsOnLinkType.create();
    GraphQLOutputType serverInfoType = ServerInfoType.create();
    GraphQLOutputType authorityType = AuthorityType.create(
      LineType.REF,
      PtSituationElementType.REF
    );
    GraphQLOutputType operatorType = OperatorType.create(LineType.REF, ServiceJourneyType.REF);
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
      dateTimeScalar
    );
    GraphQLOutputType quayType = QuayType.create(
      placeInterface,
      stopPlaceType,
      LineType.REF,
      JourneyPatternType.REF,
      EstimatedCallType.REF,
      PtSituationElementType.REF,
      tariffZoneType,
      dateTimeScalar
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
      DatedServiceJourneyType.REF
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
      dateTimeScalar,
      relay
    );
    GraphQLOutputType journeyPatternType = JourneyPatternType.create(
      linkGeometryType,
      noticeType,
      quayType,
      lineType,
      ServiceJourneyType.REF,
      stopToStopGeometryType,
      ptSituationElementType
    );
    GraphQLOutputType estimatedCallType = EstimatedCallType.create(
      bookingArrangementType,
      noticeType,
      quayType,
      destinationDisplayType,
      ptSituationElementType,
      ServiceJourneyType.REF,
      DatedServiceJourneyType.REF,
      dateTimeScalar
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
      TimetabledPassingTimeType.REF
    );

    GraphQLOutputType datedServiceJourneyType = DatedServiceJourneyType.create(
      serviceJourneyType,
      journeyPatternType,
      estimatedCallType,
      quayType
    );

    GraphQLOutputType timetabledPassingTime = TimetabledPassingTimeType.create(
      bookingArrangementType,
      noticeType,
      quayType,
      destinationDisplayType,
      serviceJourneyType
    );

    GraphQLObjectType tripPatternTimePenaltyType = TripPatternTimePenaltyType.create();
    GraphQLObjectType tripMetadataType = TripMetadataType.create(dateTimeScalar);
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
      dateTimeScalar
    );
    GraphQLObjectType tripPatternType = TripPatternType.create(
      systemNoticeType,
      legType,
      tripPatternTimePenaltyType,
      dateTimeScalar
    );
    GraphQLObjectType routingErrorType = RoutingErrorType.create();

    GraphQLOutputType tripType = TripType.create(
      placeType,
      tripPatternType,
      tripMetadataType,
      routingErrorType,
      dateTimeScalar
    );

    GraphQLInputObjectType durationPerStreetModeInput = StreetModeDurationInputType.create();
    GraphQLInputObjectType penaltyForStreetMode = PenaltyForStreetModeType.create();

    GraphQLFieldDefinition tripQuery = TripQuery.create(
      routing,
      transitTuningParameters,
      tripType,
      durationPerStreetModeInput,
      penaltyForStreetMode,
      dateTimeScalar
    );

    GraphQLOutputType viaTripType = ViaTripType.create(tripPatternType, routingErrorType);
    GraphQLInputObjectType viaLocationInputType = ViaLocationInputType.create();
    GraphQLInputObjectType viaSegmentInputType = ViaSegmentInputType.create();

    GraphQLFieldDefinition viaTripQuery = ViaTripQuery.create(
      routing,
      viaTripType,
      viaLocationInputType,
      viaSegmentInputType,
      dateTimeScalar
    );

    GraphQLInputObjectType inputPlaceIds = GraphQLInputObjectType.newInputObject()
      .name("InputPlaceIds")
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("quays")
          .description("Quays to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("lines")
          .description("Lines to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("bikeRentalStations")
          .description("Bike rentals to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("bikeParks")
          .description("Bike parks to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("carParks")
          .description("Car parks to include by id.")
          .type(new GraphQLList(Scalars.GraphQLString))
          .build()
      )
      .build();

    GraphQLObjectType queryType = GraphQLObjectType.newObject()
      .name("QueryType")
      .field(tripQuery)
      .field(viaTripQuery)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlace")
          .description("Get a single stopPlace based on its id)")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(stopPlaceType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(env ->
            StopPlaceType.fetchStopPlaceById(mapIDToDomain(env.getArgument("id")), env)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlaces")
          .description("Get stopPlaces by ids. The ids argument must be set to a non-null value.")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
          .argument(
            GraphQLArgument.newArgument()
              .name("ids")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(env -> {
            if (env.getArgument("ids") == null) {
              throw new IllegalArgumentException("ids argument must be set to a non-null value.");
            }
            var ids = mapIDsToDomainNullSafe(env.getArgument("ids"));
            return ids
              .stream()
              .map(id -> StopPlaceType.fetchStopPlaceById(id, env))
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlacesByBbox")
          .description("Get all stop places within the specified bounding box")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
          .argument(
            GraphQLArgument.newArgument()
              .name("minimumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("minimumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument().name("authority").type(Scalars.GraphQLString).build()
          )
          .argument(
            GraphQLArgument.newArgument()
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
            GraphQLArgument.newArgument()
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .description("Get a single quay based on its id)")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(quayType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).getStopLocation(
              mapIDToDomain(environment.getArgument("id"))
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quays")
          .description("Get all quays")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(quayType)))
          .argument(
            GraphQLArgument.newArgument()
              .name("ids")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(GraphQLArgument.newArgument().name("name").type(Scalars.GraphQLString).build())
          .dataFetcher(environment -> {
            if (environment.containsArgument("ids")) {
              var ids = mapIDsToDomainNullSafe(environment.getArgument("ids"));

              if (environment.getArgument("name") != null) {
                throw new IllegalArgumentException("Unable to combine other filters with ids");
              }

              TransitService transitService = GqlUtil.getTransitService(environment);
              return ids.stream().map(transitService::getStopLocation).toList();
            }

            FindStopLocationsRequest request = FindStopLocationsRequest.of()
              .withName(environment.getArgument("name"))
              .build();

            return GqlUtil.getTransitService(environment).findStopLocations(request);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quaysByBbox")
          .description("Get all quays within the specified bounding box")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(quayType)))
          .argument(
            GraphQLArgument.newArgument()
              .name("minimumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("minimumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumLatitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumLongitude")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("authority")
              .deprecate(
                "This is the Transmodel namespace or the GTFS feedID - avoid using this. Request a new field if necessary."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filterByInUse")
              .description("If true only quays with at least one visiting line are included.")
              .type(Scalars.GraphQLBoolean)
              .defaultValueProgrammatic(Boolean.FALSE)
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

            var authority = environment.<String>getArgument("authority");
            var filterInUse = environment.<Boolean>getArgument("filterByInUse");

            FindRegularStopsByBoundingBoxRequest findRegularStopsByBoundingBoxRequest =
              FindRegularStopsByBoundingBoxRequest.of(envelope)
                .withFeedId(authority)
                .filterByInUse(filterInUse)
                .build();

            return GqlUtil.getTransitService(environment).findRegularStopsByBoundingBox(
              findRegularStopsByBoundingBoxRequest
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quaysByRadius")
          .description(
            "Get all quays within the specified walking radius from a location. There are no maximum " +
            "limits for the input parameters, but the query will timeout and return if the parameters " +
            "are too high."
          )
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(
            relay.connectionType(
              "quayAtDistance",
              relay.edgeType("quayAtDistance", quayAtDistance, null, new ArrayList<>()),
              new ArrayList<>()
            )
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("latitude")
              .description("Latitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("longitude")
              .description("Longitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
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
              stops = GqlUtil.getGraphFinder(environment)
                .findClosestStops(
                  new Coordinate(
                    environment.getArgument("longitude"),
                    environment.getArgument("latitude")
                  ),
                  environment.getArgument("radius")
                )
                .stream()
                .filter(
                  stopAtDistance ->
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("nearest")
          .description(
            "Get all places (quays, stop places, car parks etc. with coordinates) within the specified radius from a location. The returned type has two fields place and distance. The search is done by walking so the distance is according to the network of walkables."
          )
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(
            relay.connectionType(
              "placeAtDistance",
              relay.edgeType("placeAtDistance", placeAtDistanceType, null, new ArrayList<>()),
              new ArrayList<>()
            )
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("latitude")
              .description("Latitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("longitude")
              .description("Longitude of the location")
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumDistance")
              .description(
                "Maximum distance (in meters) to search for from the specified location. Default is 2000m."
              )
              .defaultValueProgrammatic(2000)
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumResults")
              .description(
                "Maximum number of results. Search is stopped when this limit is reached. Default is 20."
              )
              .defaultValue(20)
              .type(Scalars.GraphQLInt)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filterByPlaceTypes")
              .description("Only include places of given types if set. Default accepts all types")
              .defaultValue(Arrays.asList(TransmodelPlaceType.values()))
              .type(new GraphQLList(FILTER_PLACE_TYPE_ENUM))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filterByModes")
              .description(
                "Only include places that include this mode. Only checked for places with mode i.e. quays, departures."
              )
              .type(new GraphQLList(TRANSPORT_MODE))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filterByInUse")
              .description(
                "Only affects queries for quays and stop places. If true only quays and stop places with at least one visiting line are included."
              )
              .type(Scalars.GraphQLBoolean)
              .defaultValue(Boolean.FALSE)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filterByIds")
              .description("Only include places that match one of the given ids.")
              .type(inputPlaceIds)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
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
            List<String> filterByNetwork = null;
            @SuppressWarnings("rawtypes")
            Map filterByIds = environment.getArgument("filterByIds");
            if (filterByIds != null) {
              filterByStops = toIdList(((List<String>) filterByIds.get("quays")));
              filterByRoutes = toIdList(((List<String>) filterByIds.get("lines")));
              filterByBikeRentalStations = filterByIds.get("bikeRentalStations") != null
                ? (List<String>) filterByIds.get("bikeRentalStations")
                : List.of();
              filterByBikeParks = filterByIds.get("bikeParks") != null
                ? (List<String>) filterByIds.get("bikeParks")
                : List.of();
              filterByCarParks = filterByIds.get("carParks") != null
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
            places = GqlUtil.getGraphFinder(environment).findClosestPlaces(
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
              filterByNetwork,
              GqlUtil.getTransitService(environment)
            );

            if (TRUE.equals(environment.getArgument("filterByInUse"))) {
              places = places
                .stream()
                .filter(placeAtDistance -> {
                  if (placeAtDistance.place() instanceof StopLocation stop) {
                    return !GqlUtil.getTransitService(environment)
                      .findPatterns(stop, true)
                      .isEmpty();
                  } else {
                    return true;
                  }
                })
                .toList();
            }

            places = PlaceAtDistanceType.convertQuaysToStopPlaces(
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("authority")
          .description("Get an authority by ID")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(authorityType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            return GqlUtil.getTransitService(environment).getAgency(
              TransitIdMapper.mapIDToDomain(environment.getArgument("id"))
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("authorities")
          .description("Get all authorities")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(authorityType)))
          .dataFetcher(environment -> {
            return new ArrayList<>(GqlUtil.getTransitService(environment).listAgencies());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operator")
          .description("Get a operator by ID")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(operatorType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).getOperator(
              TransitIdMapper.mapIDToDomain(environment.getArgument("id"))
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operators")
          .description("Get all operators")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(operatorType)))
          .dataFetcher(environment -> {
            return new ArrayList<>(GqlUtil.getTransitService(environment).listOperators());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("line")
          .description("Get a single line based on its id")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(lineType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLID))
              .build()
          )
          .dataFetcher(environment -> {
            final String id = environment.getArgument("id");
            if (id.isBlank()) {
              return null;
            }
            return GqlUtil.getTransitService(environment).getRoute(
              TransitIdMapper.mapIDToDomain(id)
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("lines")
          .description("Get all _lines_")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(lineType)))
          .argument(
            GraphQLArgument.newArgument()
              .name("ids")
              .description(
                "Set of ids of _lines_ to fetch. If this is set, no other filters can be set."
              )
              .type(new GraphQLList(Scalars.GraphQLID))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("name")
              .description(
                "Prefix of the _name_ of the _line_ to fetch. This filter is case insensitive."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("publicCode")
              .description("_Public code_ of the _line_ to fetch.")
              .type(Scalars.GraphQLString)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("publicCodes")
              .description("Set of _public codes_ to fetch _lines_ for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("transportModes")
              .description("Set of _transport modes_ to fetch _lines_ for.")
              .type(new GraphQLList(TRANSPORT_MODE))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("authorities")
              .description("Set of ids of _authorities_ to fetch _lines_ for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("flexibleOnly")
              .description(
                "Filter by _lines_ containing flexible / on demand _service journey_ only."
              )
              .type(Scalars.GraphQLBoolean)
              .defaultValueProgrammatic(false)
              .build()
          )
          .dataFetcher(environment -> {
            if (environment.containsArgument("ids")) {
              var ids = mapIDsToDomainNullSafe(environment.getArgument("ids"));

              // flexibleLines gets special treatment because it has a default value.
              if (
                Stream.of(
                  "name",
                  "publicCode",
                  "publicCodes",
                  "transportModes",
                  "authorities"
                ).anyMatch(environment::containsArgument) ||
                Boolean.TRUE.equals(environment.getArgument("flexibleOnly"))
              ) {
                throw new IllegalArgumentException("Unable to combine other filters with ids");
              }

              return GqlUtil.getTransitService(environment).getRoutes(ids);
            }

            var name = environment.<String>getArgument("name");
            var publicCode = environment.<String>getArgument("publicCode");
            var publicCodes = FilterValues.ofEmptyIsEverything(
              "publicCodes",
              environment.<List<String>>getArgument("publicCodes")
            );
            var transportModes = FilterValues.ofEmptyIsEverything(
              "transportModes",
              environment.<List<TransitMode>>getArgument("transportModes")
            );
            var authorities = FilterValues.ofEmptyIsEverything(
              "authorities",
              environment.<List<String>>getArgument("authorities")
            );
            boolean flexibleOnly = Boolean.TRUE.equals(environment.getArgument("flexibleOnly"));

            FindRoutesRequest findRoutesRequest = FindRoutesRequest.of()
              .withLongName(name)
              .withShortName(publicCode)
              .withShortNames(publicCodes)
              .withTransitModes(transportModes)
              .withAgencies(authorities)
              .withFlexibleOnly(flexibleOnly)
              .build();

            return GqlUtil.getTransitService(environment).findRoutes(findRoutesRequest);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("groupOfLines")
          .description("Get a single group of lines based on its id")
          .type(groupOfLinesType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).getGroupOfRoutes(
              TransitIdMapper.mapIDToDomain(environment.getArgument("id"))
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("groupsOfLines")
          .description("Get all groups of lines")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(groupOfLinesType))))
          .dataFetcher(environment -> GqlUtil.getTransitService(environment).listGroupsOfRoutes())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .description("Get a single service journey based on its id")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(serviceJourneyType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            return GqlUtil.getTransitService(environment).getTrip(
              TransitIdMapper.mapIDToDomain(environment.getArgument("id"))
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourneys")
          .description("Get all _service journeys_")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
          .argument(
            GraphQLArgument.newArgument()
              .name("lines")
              .description("Set of ids of _lines_ to fetch _service journeys_ for.")
              .type(new GraphQLList(Scalars.GraphQLID))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("privateCodes")
              .description("Set of ids of _private codes_ to fetch _service journeys_ for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("activeDates")
              .description("Set of _operating days_ to fetch _service journeys_ for.")
              .type(new GraphQLList(TransmodelScalars.DATE_SCALAR))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("authorities")
              .description("Set of ids of _authorities_ to fetch _service journeys_ for.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            var tripRequest = TripRequest.of()
              .withIncludeAgencies(mapIDsToDomain(environment.getArgument("authorities")))
              .withIncludeRoutes(mapIDsToDomain(environment.getArgument("lines")))
              .withIncludeNetexInternalPlanningCodes(environment.getArgument("privateCodes"))
              .withIncludeServiceDates(environment.getArgument("activeDates"))
              .build();

            return GqlUtil.getTransitService(environment).getTrips(tripRequest);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalStations")
          .description("Get all bike rental stations")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .argument(
            GraphQLArgument.newArgument()
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalStation")
          .description("Get all bike rental stations")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(bikeRentalStationType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            return GqlUtil.getVehicleRentalService(environment)
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalStationsByBbox")
          .description("Get all bike rental stations within the specified bounding box.")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
          .argument(
            GraphQLArgument.newArgument().name("minimumLatitude").type(Scalars.GraphQLFloat).build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("minimumLongitude")
              .type(Scalars.GraphQLFloat)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument().name("maximumLatitude").type(Scalars.GraphQLFloat).build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("maximumLongitude")
              .type(Scalars.GraphQLFloat)
              .build()
          )
          .dataFetcher(environment ->
            GqlUtil.getVehicleRentalService(environment).getVehicleRentalStationForEnvelope(
              environment.getArgument("minimumLongitude"),
              environment.getArgument("minimumLatitude"),
              environment.getArgument("maximumLongitude"),
              environment.getArgument("maximumLatitude")
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikePark")
          .description("Get a single bike park based on its id")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(bikeParkType)
          .argument(
            GraphQLArgument.newArgument()
              .name("id")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            var bikeParkId = mapIDToDomain(environment.getArgument("id"));
            return GqlUtil.getVehicleParkingService(environment)
              .listBikeParks()
              .stream()
              .filter(bikePark -> bikePark.getId().equals(bikeParkId))
              .findFirst()
              .orElse(null);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeParks")
          .description("Get all bike parks")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(bikeParkType)))
          .dataFetcher(environment ->
            GqlUtil.getVehicleParkingService(environment)
              .listBikeParks()
              .stream()
              .collect(Collectors.toCollection(ArrayList::new))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("routingParameters")
          .description("Get default routing parameters.")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(this.routing.graphQLType)
          .dataFetcher(environment -> routing.request)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .description("Get all active situations.")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .argument(
            GraphQLArgument.newArgument()
              .name("authorities")
              .description("Filter by reporting authorities.")
              .deprecate(
                "Use codespaces instead. This only uses the codespace of the given authority."
              )
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("codespaces")
              .description("Filter by reporting source.")
              .type(new GraphQLList(Scalars.GraphQLString))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("severities")
              .description("Filter by severity.")
              .type(new GraphQLList(EnumTypes.SEVERITY))
              .build()
          )
          .dataFetcher(environment -> {
            Collection<TransitAlert> alerts = GqlUtil.getTransitService(environment)
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
              alerts = alerts
                .stream()
                .filter(alert -> codespaces.contains(alert.siriCodespace()))
                .collect(Collectors.toSet());
            }

            if (environment.getArgument("severities") instanceof List) {
              List<String> severities = environment.getArgument("severities");
              alerts = alerts
                .stream()
                .filter(alert -> severities.contains(getTransmodelSeverity(alert.severity())))
                .collect(Collectors.toSet());
            }
            return alerts;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situation")
          .description("Get a single situation based on its situationNumber")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(ptSituationElementType)
          .argument(
            GraphQLArgument.newArgument()
              .name("situationNumber")
              .type(new GraphQLNonNull(Scalars.GraphQLString))
              .build()
          )
          .dataFetcher(environment -> {
            String situationNumber = environment.getArgument("situationNumber");
            if (situationNumber.isBlank()) {
              return null;
            }
            return GqlUtil.getTransitService(environment)
              .getTransitAlertService()
              .getAlertById(mapIDToDomain(situationNumber));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("leg")
          .description("Refetch a single transit leg based on its id")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(LegType.REF)
          .argument(
            GraphQLArgument.newArgument()
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serverInfo")
          .description(
            "Get OTP deployment information. This is only useful for developers of OTP itself not regular API users."
          )
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(serverInfoType))
          .dataFetcher(e -> projectInfo())
          .build()
      )
      .field(DatedServiceJourneyQuery.createGetById(datedServiceJourneyType))
      .field(DatedServiceJourneyQuery.createQuery(datedServiceJourneyType))
      .build();

    var schema = GraphQLSchema.newSchema()
      .query(queryType)
      .additionalType(placeInterface)
      .additionalType(timetabledPassingTime)
      .additionalType(Relay.pageInfoType)
      .additionalDirective(TransmodelDirectives.TIMING_DATA)
      .build();

    return schema;
  }

  private static GraphQLSchema decorateSchemaWithCustomDocumentation(
    GraphQLSchema schema,
    ApiDocumentationProfile docProfile
  ) {
    var customDocumentation = CustomDocumentation.of(docProfile);
    if (customDocumentation.isEmpty()) {
      return schema;
    }
    var visitor = new InjectCustomDocumentation(customDocumentation);
    return SchemaTransformer.transformSchema(schema, visitor);
  }

  private List<FeedScopedId> toIdList(@Nullable List<String> ids) {
    if (ids == null) {
      return Collections.emptyList();
    }
    return ids.stream().map(TransitIdMapper::mapIDToDomain).collect(Collectors.toList());
  }
}
