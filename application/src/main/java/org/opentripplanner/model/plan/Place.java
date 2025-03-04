package org.opentripplanner.model.plan;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A Place is where a journey starts or ends, or a transit stop along the way.
 */
public class Place {

  /**
   * For transit stops, the name of the stop.  For points of interest, the name of the POI.
   */
  public final I18NString name;

  /**
   * The coordinate of the place.
   */
  public final WgsCoordinate coordinate;

  /**
   * Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit stop) Mostly used for better
   * localization of bike sharing and P+R station names
   */
  public final VertexType vertexType;

  /**
   * Reference to the stop if the type is {@link VertexType#TRANSIT}.
   */
  public final StopLocation stop;

  /**
   * The vehicle rental place if the type is {@link VertexType#VEHICLERENTAL}.
   */
  public final VehicleRentalPlace vehicleRentalPlace;

  /**
   * The vehicle parking entrance if the type is {@link VertexType#VEHICLEPARKING}.
   */
  public final VehicleParkingWithEntrance vehicleParkingWithEntrance;

  private Place(
    I18NString name,
    WgsCoordinate coordinate,
    VertexType vertexType,
    StopLocation stop,
    VehicleRentalPlace vehicleRentalPlace,
    VehicleParkingWithEntrance vehicleParkingWithEntrance
  ) {
    this.name = name;
    this.coordinate = coordinate;
    this.vertexType = vertexType;
    this.stop = stop;
    this.vehicleRentalPlace = vehicleRentalPlace;
    this.vehicleParkingWithEntrance = vehicleParkingWithEntrance;
  }

  public static Place normal(Double lat, Double lon, I18NString name) {
    return new Place(
      name,
      WgsCoordinate.creatOptionalCoordinate(lat, lon),
      VertexType.NORMAL,
      null,
      null,
      null
    );
  }

  public static Place normal(Vertex vertex, I18NString name) {
    return new Place(
      name,
      WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
      VertexType.NORMAL,
      null,
      null,
      null
    );
  }

  public static Place forStop(StopLocation stop) {
    return new Place(stop.getName(), stop.getCoordinate(), VertexType.TRANSIT, stop, null, null);
  }

  public static Place forFlexStop(StopLocation stop, Vertex vertex) {
    var name = stop.getName();

    if (stop instanceof AreaStop flexArea && vertex instanceof StreetVertex s) {
      if (flexArea.hasFallbackName()) {
        name = s.getIntersectionName();
      } else {
        name = new LocalizedString("partOf", s.getIntersectionName(), flexArea.getName());
      }
    }
    // The actual vertex is used because the StopLocation coordinates may not be equal to the vertex's
    // coordinates.
    return new Place(
      name,
      WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
      VertexType.TRANSIT,
      stop,
      null,
      null
    );
  }

  public static Place forVehicleRentalPlace(VehicleRentalPlaceVertex vertex) {
    return new Place(
      vertex.getName(),
      WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
      VertexType.VEHICLERENTAL,
      null,
      vertex.getStation(),
      null
    );
  }

  public static Place forVehicleParkingEntrance(VehicleParkingEntranceVertex vertex, State state) {
    TraverseMode traverseMode = null;
    final StreetSearchRequest request = state.getRequest();
    if (request.mode().includesDriving()) {
      traverseMode = TraverseMode.CAR;
    } else if (request.mode().includesBiking()) {
      traverseMode = TraverseMode.BICYCLE;
    }

    boolean realTime = vertex
      .getVehicleParking()
      .hasRealTimeDataForMode(traverseMode, request.wheelchair());
    return new Place(
      vertex.getName(),
      WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
      VertexType.VEHICLEPARKING,
      null,
      null,
      VehicleParkingWithEntrance.builder()
        .vehicleParking(vertex.getVehicleParking())
        .entrance(vertex.getParkingEntrance())
        .realtime(realTime)
        .build()
    );
  }

  /**
   * Test if the place is likely to be at the same location. First check the coordinates then check
   * the stopId [if it exist].
   */
  public boolean sameLocation(Place other) {
    if (this == other) {
      return true;
    }
    if (coordinate != null) {
      return coordinate.sameLocation(other.coordinate);
    }
    return stop != null && stop.equals(other.stop);
  }

  /**
   * Return a short version to be used in other classes toStringMethods. Should return just the
   * necessary information for a human to identify the place in a given the context.
   */
  public String toStringShort() {
    StringBuilder buf = new StringBuilder(name.toString());
    if (stop != null) {
      buf.append(" (").append(stop.getId()).append(")");
    } else {
      buf.append(" ").append(coordinate.toString());
    }

    return buf.toString();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(Place.class)
      .addObj("name", name)
      .addObj("stop", stop)
      .addObj("coordinate", coordinate)
      .addEnum("vertexType", vertexType)
      .addObj("vehicleRentalPlace", vehicleRentalPlace)
      .addObj("vehicleParkingEntrance", vehicleParkingWithEntrance)
      .toString();
  }
}
