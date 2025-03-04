package org.opentripplanner.graph_builder.module.osm;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiFunction;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedStringFormat;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.InvalidVehicleParkingCapacity;
import org.opentripplanner.graph_builder.issues.ParkAndRideUnlinked;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.osm.OsmOpeningHoursParser;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingHelper;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParkingProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ParkingProcessor.class);
  private static final String VEHICLE_PARKING_OSM_FEED_ID = "OSM";
  private final DataImportIssueStore issueStore;
  private final OsmOpeningHoursParser osmOpeningHoursParser;
  private final BiFunction<OsmNode, OsmEntity, IntersectionVertex> getVertexForOsmNode;
  private final VertexFactory vertexFactory;
  private final VehicleParkingHelper vehicleParkingHelper;

  public ParkingProcessor(
    Graph graph,
    DataImportIssueStore issueStore,
    BiFunction<OsmNode, OsmEntity, IntersectionVertex> getVertexForOsmNode
  ) {
    this.issueStore = issueStore;
    this.getVertexForOsmNode = getVertexForOsmNode;
    this.osmOpeningHoursParser = new OsmOpeningHoursParser(
      graph.getOpeningHoursCalendarService(),
      issueStore
    );
    this.vertexFactory = new VertexFactory(graph);
    this.vehicleParkingHelper = new VehicleParkingHelper(graph);
  }

  public List<VehicleParking> buildParkAndRideNodes(
    Collection<OsmNode> nodes,
    boolean isCarParkAndRide
  ) {
    LOG.info("Processing {} P+R nodes.", isCarParkAndRide ? "car" : "bike");
    int n = 0;

    List<VehicleParking> vehicleParkingToAdd = new ArrayList<>();

    for (OsmNode node : nodes) {
      n++;

      I18NString creativeName = nameParkAndRideEntity(node);

      VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
        builder
          .entranceId(
            new FeedScopedId(
              VEHICLE_PARKING_OSM_FEED_ID,
              String.format("%s/%s/entrance", node.getClass().getSimpleName(), node.getId())
            )
          )
          .name(creativeName)
          .coordinate(new WgsCoordinate(node.getCoordinate()))
          .walkAccessible(true)
          .carAccessible(isCarParkAndRide);

      var vehicleParking = createVehicleParkingObjectFromOsmEntity(
        isCarParkAndRide,
        node.getCoordinate(),
        node,
        creativeName,
        List.of(entrance)
      );

      vehicleParkingToAdd.add(vehicleParking);

      VehicleParkingEntranceVertex parkVertex = vertexFactory.vehicleParkingEntrance(
        vehicleParking
      );
      VehicleParkingEdge.createVehicleParkingEdge(parkVertex);
    }

    LOG.info("Created {} {} P+R nodes.", n, isCarParkAndRide ? "car" : "bike");
    return vehicleParkingToAdd;
  }

  public Collection<VehicleParking> buildBikeParkAndRideAreas(List<OsmAreaGroup> areaGroups) {
    return buildParkAndRideAreasForGroups(areaGroups, false);
  }

  public Collection<VehicleParking> buildParkAndRideAreas(List<OsmAreaGroup> areaGroups) {
    return buildParkAndRideAreasForGroups(areaGroups, true);
  }

  private List<VehicleParking> buildParkAndRideAreasForGroups(
    List<OsmAreaGroup> areaGroups,
    boolean isCarParkAndRide
  ) {
    List<VehicleParking> vehicleParkingToAdd = new ArrayList<>();
    for (OsmAreaGroup group : areaGroups) {
      var vehicleParking = buildParkAndRideAreasForGroup(group, isCarParkAndRide);
      if (vehicleParking != null) {
        vehicleParkingToAdd.add(vehicleParking);
      }
    }
    return vehicleParkingToAdd;
  }

  private OHCalendar parseOpeningHours(OsmEntity entity) {
    final var openingHoursTag = entity.getTag("opening_hours");
    if (openingHoursTag != null) {
      final ZoneId zoneId = entity.getOsmProvider().getZoneId();
      final var id = entity.getId();
      final var link = entity.url();
      try {
        return osmOpeningHoursParser.parseOpeningHours(
          openingHoursTag,
          String.valueOf(id),
          link,
          zoneId
        );
      } catch (OpeningHoursParseException e) {
        issueStore.add(
          "OsmOpeningHoursUnparsed",
          "OSM object with id '%s' (%s) has an invalid opening_hours value, it will always be open",
          id,
          link
        );
      }
    }
    return null;
  }

  private List<VertexAndName> processVehicleParkingArea(OsmArea area, Envelope envelope) {
    return area.outermostRings
      .stream()
      .flatMap(ring -> processVehicleParkingArea(ring, area.parent, envelope).stream())
      .toList();
  }

  private List<VertexAndName> processVehicleParkingArea(
    Ring ring,
    OsmEntity entity,
    Envelope envelope
  ) {
    List<VertexAndName> accessVertices = new ArrayList<>();
    for (OsmNode node : ring.nodes) {
      envelope.expandToInclude(new Coordinate(node.lon, node.lat));
      var accessVertex = getVertexForOsmNode.apply(node, entity);
      if (accessVertex.getIncoming().isEmpty() || accessVertex.getOutgoing().isEmpty()) {
        continue;
      }
      accessVertices.add(new VertexAndName(node.getAssumedName(), accessVertex));
    }

    accessVertices.addAll(
      ring
        .getHoles()
        .stream()
        .flatMap(innerRing -> processVehicleParkingArea(innerRing, entity, envelope).stream())
        .toList()
    );

    return accessVertices;
  }

  private VehicleParking buildParkAndRideAreasForGroup(
    OsmAreaGroup group,
    boolean isCarParkAndRide
  ) {
    Envelope envelope = new Envelope();
    Set<VertexAndName> accessVertices = new HashSet<>();

    OsmEntity entity = null;

    // Process all nodes from outer rings
    // These are IntersectionVertices not OsmVertices because there can be both OsmVertices and TransitStopStreetVertices.
    for (OsmArea area : group.areas) {
      entity = area.parent;

      var areaAccessVertices = processVehicleParkingArea(area, envelope);
      accessVertices.addAll(areaAccessVertices);
    }

    if (entity == null) {
      return null;
    }

    var creativeName = nameParkAndRideEntity(entity);

    // Check P+R accessibility by walking and driving.
    boolean walkAccessibleIn = false;
    boolean carAccessibleIn = false;
    boolean walkAccessibleOut = false;
    boolean carAccessibleOut = false;
    for (VertexAndName access : accessVertices) {
      var accessVertex = access.vertex();
      for (Edge incoming : accessVertex.getIncoming()) {
        if (incoming instanceof StreetEdge streetEdge) {
          if (streetEdge.canTraverse(TraverseMode.WALK)) {
            walkAccessibleIn = true;
          }
          if (streetEdge.canTraverse(TraverseMode.CAR)) {
            carAccessibleIn = true;
          }
        }
      }
      for (Edge outgoing : accessVertex.getOutgoing()) {
        if (outgoing instanceof StreetEdge streetEdge) {
          if (streetEdge.canTraverse(TraverseMode.WALK)) {
            walkAccessibleOut = true;
          }
          if (streetEdge.canTraverse(TraverseMode.CAR)) {
            carAccessibleOut = true;
          }
        }
      }
    }

    if (walkAccessibleIn != walkAccessibleOut) {
      LOG.error(
        "P+R walk IN/OUT accessibility mismatch! Please have a look as this should not happen."
      );
    }

    if (isCarParkAndRide) {
      if (!walkAccessibleOut || !carAccessibleIn || !walkAccessibleIn || !carAccessibleOut) {
        // This will prevent the P+R to be useful.
        issueStore.add(new ParkAndRideUnlinked(creativeName.toString(), entity));
      }
    } else {
      if (!walkAccessibleOut || !walkAccessibleIn) {
        // This will prevent the P+R to be useful.
        issueStore.add(new ParkAndRideUnlinked(creativeName.toString(), entity));
      }
    }

    List<VehicleParking.VehicleParkingEntranceCreator> entrances =
      createParkingEntrancesFromAccessVertices(accessVertices, creativeName, entity);

    if (entrances.isEmpty()) {
      entrances = createArtificialEntrances(group, creativeName, entity, isCarParkAndRide);
    }

    var vehicleParking = createVehicleParkingObjectFromOsmEntity(
      isCarParkAndRide,
      envelope.centre(),
      entity,
      creativeName,
      entrances
    );

    vehicleParkingHelper.linkVehicleParkingToGraph(vehicleParking);

    return vehicleParking;
  }

  /**
   * Creates an artificial entrance to a parking facility's centroid.
   * <p>
   * This is useful if the facility is not linked to the street network in OSM. Without this method
   * it would not be usable by the routing algorithm as it's unreachable.
   */
  private List<VehicleParking.VehicleParkingEntranceCreator> createArtificialEntrances(
    OsmAreaGroup group,
    I18NString vehicleParkingName,
    OsmEntity entity,
    boolean isCarPark
  ) {
    LOG.debug(
      "Creating an artificial entrance for {} as it's not linked to the street network",
      entity.url()
    );
    return List.of(builder ->
      builder
        .entranceId(
          new FeedScopedId(
            VEHICLE_PARKING_OSM_FEED_ID,
            String.format("%s/%d/centroid", entity.getClass().getSimpleName(), entity.getId())
          )
        )
        .name(vehicleParkingName)
        .coordinate(new WgsCoordinate(group.union.getInteriorPoint()))
        // setting the vertex to null signals the rest of the build process that this needs to be linked to the street network
        .vertex(null)
        .walkAccessible(true)
        .carAccessible(isCarPark)
    );
  }

  VehicleParking createVehicleParkingObjectFromOsmEntity(
    boolean isCarParkAndRide,
    Coordinate coordinate,
    OsmEntity entity,
    I18NString creativeName,
    List<VehicleParking.VehicleParkingEntranceCreator> entrances
  ) {
    OptionalInt bicycleCapacity, carCapacity, wheelchairAccessibleCarCapacity;
    if (isCarParkAndRide) {
      carCapacity = parseCapacity(entity);
      bicycleCapacity = parseCapacity(entity, "capacity:bike");
      wheelchairAccessibleCarCapacity = parseCapacity(entity, "capacity:disabled");
    } else {
      bicycleCapacity = parseCapacity(entity);
      carCapacity = OptionalInt.empty();
      wheelchairAccessibleCarCapacity = OptionalInt.empty();
    }

    VehicleParkingSpaces vehicleParkingSpaces = null;
    if (
      bicycleCapacity.isPresent() ||
      carCapacity.isPresent() ||
      wheelchairAccessibleCarCapacity.isPresent()
    ) {
      vehicleParkingSpaces = VehicleParkingSpaces.builder()
        .bicycleSpaces(bicycleCapacity.isPresent() ? bicycleCapacity.getAsInt() : null)
        .carSpaces(carCapacity.isPresent() ? carCapacity.getAsInt() : null)
        .wheelchairAccessibleCarSpaces(
          wheelchairAccessibleCarCapacity.isPresent()
            ? wheelchairAccessibleCarCapacity.getAsInt()
            : null
        )
        .build();
    }

    var bicyclePlaces = !isCarParkAndRide || bicycleCapacity.orElse(0) > 0;
    var carPlaces =
      (isCarParkAndRide && wheelchairAccessibleCarCapacity.isEmpty() && carCapacity.isEmpty()) ||
      carCapacity.orElse(0) > 0;
    var wheelchairAccessibleCarPlaces = wheelchairAccessibleCarCapacity.orElse(0) > 0;

    var openingHours = parseOpeningHours(entity);

    var id = new FeedScopedId(
      VEHICLE_PARKING_OSM_FEED_ID,
      String.format("%s/%d", entity.getClass().getSimpleName(), entity.getId())
    );

    var tags = new ArrayList<String>();

    tags.add(isCarParkAndRide ? "osm:amenity=parking" : "osm:amenity=bicycle_parking");

    if (entity.isTagTrue("fee")) {
      tags.add("osm:fee");
    }
    if (entity.hasTag("supervised") && !entity.isTagTrue("supervised")) {
      tags.add("osm:supervised");
    }
    if (entity.hasTag("covered") && !entity.isTagFalse("covered")) {
      tags.add("osm:covered");
    }
    if (entity.hasTag("surveillance") && !entity.isTagFalse("surveillance")) {
      tags.add("osm:surveillance");
    }

    return VehicleParking.builder()
      .id(id)
      .name(creativeName)
      .coordinate(new WgsCoordinate(coordinate))
      .tags(tags)
      .detailsUrl(entity.getTag("website"))
      .openingHoursCalendar(openingHours)
      .bicyclePlaces(bicyclePlaces)
      .carPlaces(carPlaces)
      .wheelchairAccessibleCarPlaces(wheelchairAccessibleCarPlaces)
      .capacity(vehicleParkingSpaces)
      .entrances(entrances)
      .build();
  }

  private I18NString nameParkAndRideEntity(OsmEntity osmEntity) {
    // If there is an explicit name use that. The explicit name is used so that tag-based
    // translations are used, which are not handled by "CreativeNamer"s.
    I18NString creativeName = osmEntity.getAssumedName();
    if (creativeName == null) {
      // ... otherwise resort to "CreativeNamer"s
      creativeName = osmEntity
        .getOsmProvider()
        .getWayPropertySet()
        .getCreativeNameForWay(osmEntity);
    }
    if (creativeName == null) {
      creativeName = new NonLocalizedString(
        "Park & Ride (%s/%d)".formatted(osmEntity.getClass().getSimpleName(), osmEntity.getId())
      );
    }
    return creativeName;
  }

  private OptionalInt parseCapacity(OsmEntity element) {
    return parseCapacity(element, "capacity");
  }

  private OptionalInt parseCapacity(OsmEntity element, String capacityTag) {
    return element.parseIntOrBoolean(capacityTag, v ->
      issueStore.add(new InvalidVehicleParkingCapacity(element, v))
    );
  }

  private List<
    VehicleParking.VehicleParkingEntranceCreator
  > createParkingEntrancesFromAccessVertices(
    Set<VertexAndName> accessVertices,
    I18NString vehicleParkingName,
    OsmEntity entity
  ) {
    List<VehicleParking.VehicleParkingEntranceCreator> entrances = new ArrayList<>();
    var sortedAccessVertices = accessVertices
      .stream()
      .sorted(Comparator.comparing(vn -> vn.vertex().getLabelString()))
      .toList();

    for (var access : sortedAccessVertices) {
      I18NString suffix = null;
      if (access.name() != null) {
        suffix = access.name();
      }

      if (suffix == null) {
        suffix = new NonLocalizedString(String.format("#%d", entrances.size() + 1));
      }

      var entranceName = new LocalizedStringFormat("%s (%s)", vehicleParkingName, suffix);

      entrances.add(builder ->
        builder
          .entranceId(
            new FeedScopedId(
              VEHICLE_PARKING_OSM_FEED_ID,
              String.format(
                "%s/%d/%s",
                entity.getClass().getSimpleName(),
                entity.getId(),
                access.vertex().getLabel()
              )
            )
          )
          .name(entranceName)
          .coordinate(new WgsCoordinate(access.vertex().getCoordinate()))
          .vertex(access.vertex())
          .walkAccessible(access.vertex().isConnectedToWalkingEdge())
          .carAccessible(access.vertex().isConnectedToDriveableEdge())
      );
    }

    return entrances;
  }
}

record VertexAndName(I18NString name, IntersectionVertex vertex) {}
