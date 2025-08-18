package org.opentripplanner.ext.carpooling.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.lang.reflect.Array;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.ext.flex.AreaStopsToVerticesMapper;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Utility class to create realistic carpooling trip data for the Kristiansand area.
 *
 * This creates carpooling trips connecting popular areas around Kristiansand:
 * - Kvadraturen (downtown)
 * - Lund (university area)
 * - Gimle (residential area)
 * - Sørlandsparken (shopping center)
 * - Varoddbrua (bridge area)
 * - Torridal (industrial area)
 */
public class KristiansandCarpoolingData {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final ZoneId KRISTIANSAND_TIMEZONE = ZoneId.of("Europe/Oslo");
  private static final AtomicInteger COUNTER = new AtomicInteger(0);

  /**
   * Populates the repository with realistic Kristiansand carpooling trips
   */
  public static void populateRepository(CarpoolingRepository repository, Graph graph) {
    // Create area stops for popular Kristiansand locations
    AreaStop kvadraturenPickup = createAreaStop("kvadraturen-pickup", 58.1458, 7.9959, 300); // Downtown center
    AreaStop lundDropoff = createAreaStop("lund-dropoff", 58.1665, 8.0041, 400); // University of Agder area

    AreaStop gimlePickup = createAreaStop("gimle-pickup", 58.1547, 7.9899, 250); // Gimle residential
    AreaStop sorlandparkenDropoff = createAreaStop("sorlandparken-dropoff", 58.0969, 7.9786, 500); // Shopping center

    AreaStop varoddbruaPickup = createAreaStop("varoddbrua-pickup", 58.1389, 8.0180, 200); // Bridge area
    AreaStop torridalDropoff = createAreaStop("torridal-dropoff", 58.1244, 8.0500, 350); // Industrial area

    AreaStop lundPickup = createAreaStop("lund-pickup", 58.1665, 8.0041, 400); // University pickup
    AreaStop kvadraturenDropoff = createAreaStop("kvadraturen-dropoff", 58.1458, 7.9959, 300); // Downtown dropoff

    // Create carpooling trips for typical commuting patterns

    // Morning commute: Downtown to University (07:30-08:00)
    repository.addCarpoolTrip(
      createCarpoolTrip(
        "morning-downtown-to-uni",
        kvadraturenPickup,
        lundDropoff,
        LocalTime.of(7, 30),
        LocalTime.of(8, 0),
        "KristiansandRides",
        3
      )
    );

    // Morning commute: Gimle to Sørlandsparken (08:00-08:25)
    repository.addCarpoolTrip(
      createCarpoolTrip(
        "morning-gimle-to-shopping",
        gimlePickup,
        sorlandparkenDropoff,
        LocalTime.of(8, 0),
        LocalTime.of(8, 25),
        "ShareRideKRS",
        2
      )
    );

    // Morning commute: Varoddbrua to Torridal (07:45-08:10)
    repository.addCarpoolTrip(
      createCarpoolTrip(
        "morning-bridge-to-industrial",
        varoddbruaPickup,
        torridalDropoff,
        LocalTime.of(7, 45),
        LocalTime.of(8, 10),
        "CommuteBuddy",
        4
      )
    );

    // Evening commute: University back to Downtown (16:30-17:00)
    repository.addCarpoolTrip(
      createCarpoolTrip(
        "evening-uni-to-downtown",
        lundPickup,
        kvadraturenDropoff,
        LocalTime.of(16, 30),
        LocalTime.of(17, 0),
        "KristiansandRides",
        2
      )
    );

    System.out.println(
      "✅ Populated carpooling repository with " +
      repository.getCarpoolTrips().size() +
      " Kristiansand area trips"
    );

    // Print trip summary for verification
    repository
      .getCarpoolTrips()
      .forEach(trip -> {
        System.out.printf(
          "🚗 %s: %s → %s (%s-%s) [%s seats, %s]%n",
          trip.getId().getId(),
          formatAreaName(trip.getBoardingArea().getId().getId()),
          formatAreaName(trip.getAlightingArea().getId().getId()),
          trip.getStartTime().toLocalTime(),
          trip.getEndTime().toLocalTime(),
          trip.getAvailableSeats(),
          trip.getProvider()
        );
      });
  }

  private static CarpoolTrip createCarpoolTrip(
    String tripId,
    AreaStop boardingArea,
    AreaStop alightingArea,
    LocalTime startTime,
    LocalTime endTime,
    String provider,
    int availableSeats
  ) {
    ZonedDateTime now = ZonedDateTime.now(KRISTIANSAND_TIMEZONE);
    ZonedDateTime startDateTime = now.with(startTime);
    ZonedDateTime endDateTime = now.with(endTime);

    return new CarpoolTripBuilder(new FeedScopedId("CARPOOL", tripId))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(startDateTime)
      .withEndTime(endDateTime)
      .withTrip(null)
      .withProvider(provider)
      .withAvailableSeats(availableSeats)
      .build();
  }

  private static AreaStop createAreaStop(String id, double lat, double lon, double radiusMeters) {
    WgsCoordinate center = new WgsCoordinate(lat, lon);
    Polygon geometry = createCircularPolygon(center, radiusMeters);

    return AreaStop.of(new FeedScopedId("CARPOOL", id), COUNTER::getAndIncrement)
      .withGeometry(geometry)
      .build();
  }

  private static Polygon createCircularPolygon(WgsCoordinate center, double radiusMeters) {
    // Create approximate circle using degree offsets (simplified for Kristiansand latitude)
    double latOffset = radiusMeters / 111000.0; // ~111km per degree latitude
    double lonOffset = radiusMeters / (111000.0 * Math.cos(Math.toRadians(center.latitude()))); // Adjust for latitude

    Coordinate[] coordinates = new Coordinate[13]; // 12 points + closing point
    for (int i = 0; i < 12; i++) {
      double angle = (2 * Math.PI * i) / 12;
      double lat = center.latitude() + (latOffset * Math.cos(angle));
      double lon = center.longitude() + (lonOffset * Math.sin(angle));
      coordinates[i] = new Coordinate(lon, lat);
    }
    coordinates[12] = coordinates[0]; // Close the polygon

    return GEOMETRY_FACTORY.createPolygon(coordinates);
  }

  private static String formatAreaName(String id) {
    return id.replace("-pickup", "").replace("-dropoff", "").replace("-", " ").toUpperCase();
  }
}
