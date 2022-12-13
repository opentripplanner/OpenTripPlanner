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
import org.opentripplanner.openstreetmap.OSMOpeningHoursParser;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParkingProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ParkingProcessor.class);
  private static final String VEHICLE_PARKING_OSM_FEED_ID = "OSM";
  private final Graph graph;
  private final DataImportIssueStore issueStore;
  private final OSMOpeningHoursParser osmOpeningHoursParser;
  private final BiFunction<OSMNode, OSMWithTags, IntersectionVertex> getVertexForOsmNode;

  public ParkingProcessor(
    Graph graph,
    DataImportIssueStore issueStore,
    BiFunction<OSMNode, OSMWithTags, IntersectionVertex> getVertexForOsmNode
  ) {
    this.graph = graph;
    this.issueStore = issueStore;
    this.getVertexForOsmNode = getVertexForOsmNode;
    this.osmOpeningHoursParser =
      new OSMOpeningHoursParser(graph.getOpeningHoursCalendarService(), issueStore);
  }

  public List<VehicleParking> buildParkAndRideNodes(
    Collection<OSMNode> nodes,
    boolean isCarParkAndRide
  ) {
    LOG.info("Processing {} P+R nodes.", isCarParkAndRide ? "car" : "bike");
    int n = 0;

    List<VehicleParking> vehicleParkingToAdd = new ArrayList<>();

    for (OSMNode node : nodes) {
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

      VehicleParkingEntranceVertex parkVertex = new VehicleParkingEntranceVertex(
        graph,
        vehicleParking.getEntrances().get(0)
      );
      new VehicleParkingEdge(parkVertex);
    }

    LOG.info("Created {} {} P+R nodes.", n, isCarParkAndRide ? "car" : "bike");
    return vehicleParkingToAdd;
  }

  public Collection<VehicleParking> buildBikeParkAndRideAreas(List<AreaGroup> areaGroups) {
    return buildParkAndRideAreasForGroups(areaGroups, false);
  }

  public Collection<VehicleParking> buildParkAndRideAreas(List<AreaGroup> areaGroups) {
    return buildParkAndRideAreasForGroups(areaGroups, true);
  }

  private List<VehicleParking> buildParkAndRideAreasForGroups(
    List<AreaGroup> areaGroups,
    boolean isCarParkAndRide
  ) {
    List<VehicleParking> vehicleParkingToAdd = new ArrayList<>();
    for (AreaGroup group : areaGroups) {
      var vehicleParking = buildParkAndRideAreasForGroup(group, isCarParkAndRide);
      if (vehicleParking != null) {
        vehicleParkingToAdd.add(vehicleParking);
      }
    }
    return vehicleParkingToAdd;
  }

  private OHCalendar parseOpeningHours(OSMWithTags entity) {
    final var openingHoursTag = entity.getTag("opening_hours");
    if (openingHoursTag != null) {
      final ZoneId zoneId = entity.getOsmProvider().getZoneId();
      final var id = entity.getId();
      final var link = entity.getOpenStreetMapLink();
      try {
        return osmOpeningHoursParser.parseOpeningHours(
          openingHoursTag,
          String.valueOf(id),
          link,
          zoneId
        );
      } catch (OpeningHoursParseException e) {
        issueStore.add(
          "OSMOpeningHoursUnparsed",
          "OSM object with id '%s' (%s) has an invalid opening_hours value, it will always be open",
          id,
          link
        );
      }
    }
    return null;
  }

  private List<VertexAndName> processVehicleParkingArea(Area area, Envelope envelope) {
    return area.outermostRings
      .stream()
      .flatMap(ring -> processVehicleParkingArea(ring, area.parent, envelope).stream())
      .toList();
  }

  private List<VertexAndName> processVehicleParkingArea(
    Ring ring,
    OSMWithTags entity,
    Envelope envelope
  ) {
    List<VertexAndName> accessVertices = new ArrayList<>();
    for (OSMNode node : ring.nodes) {
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

  private VehicleParking buildParkAndRideAreasForGroup(AreaGroup group, boolean isCarParkAndRide) {
    Envelope envelope = new Envelope();
    Set<VertexAndName> accessVertices = new HashSet<>();

    OSMWithTags entity = null;

    // Process all nodes from outer rings
    // These are IntersectionVertices not OsmVertices because there can be both OsmVertices and TransitStopStreetVertices.
    for (Area area : group.areas) {
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
        return null;
      }
    } else {
      if (!walkAccessibleOut || !walkAccessibleIn) {
        // This will prevent the P+R to be useful.
        issueStore.add(new ParkAndRideUnlinked(creativeName.toString(), entity));
        return null;
      }
    }

    List<VehicleParking.VehicleParkingEntranceCreator> entrances = createParkingEntrancesFromAccessVertices(
      accessVertices,
      creativeName,
      entity
    );

    var vehicleParking = createVehicleParkingObjectFromOsmEntity(
      isCarParkAndRide,
      envelope.centre(),
      entity,
      creativeName,
      entrances
    );

    VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

    return vehicleParking;
  }

  private VehicleParking createVehicleParkingObjectFromOsmEntity(
    boolean isCarParkAndRide,
    Coordinate coordinate,
    OSMWithTags entity,
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
      vehicleParkingSpaces =
        VehicleParkingSpaces
          .builder()
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

    return VehicleParking
      .builder()
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

  private I18NString nameParkAndRideEntity(OSMWithTags osmWithTags) {
    // If there is an explicit name user that. The explicit name is used so that tag-based
    // translations are used, which are not handled by "CreativeNamer"s.
    I18NString creativeName = osmWithTags.getAssumedName();
    if (creativeName == null) {
      // ... otherwise resort to "CreativeNamer"s
      creativeName =
        osmWithTags.getOsmProvider().getWayPropertySet().getCreativeNameForWay(osmWithTags);
    }
    if (creativeName == null) {
      creativeName =
        new NonLocalizedString(
          "Park & Ride (%s/%d)".formatted(
              osmWithTags.getClass().getSimpleName(),
              osmWithTags.getId()
            )
        );
    }
    return creativeName;
  }

  private OptionalInt parseCapacity(OSMWithTags element) {
    return parseCapacity(element, "capacity");
  }

  private OptionalInt parseCapacity(OSMWithTags element, String capacityTag) {
    return element.getTagAsInt(
      capacityTag,
      v -> issueStore.add(new InvalidVehicleParkingCapacity(element.getId(), v))
    );
  }

  private List<VehicleParking.VehicleParkingEntranceCreator> createParkingEntrancesFromAccessVertices(
    Set<VertexAndName> accessVertices,
    I18NString vehicleParkingName,
    OSMWithTags entity
  ) {
    List<VehicleParking.VehicleParkingEntranceCreator> entrances = new ArrayList<>();
    var sortedAccessVertices = accessVertices
      .stream()
      .sorted(Comparator.comparing(vn -> vn.vertex().getLabel()))
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
