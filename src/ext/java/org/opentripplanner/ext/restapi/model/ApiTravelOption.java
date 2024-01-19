package org.opentripplanner.ext.restapi.model;

import java.util.HashSet;
import java.util.Objects;

/**
 * This class is used to send to client which Travel Options are possible on this server
 * <p>
 * This options are used in client "Travel by" drop down.
 * <p>
 * Each travel option consist of two variables: - value is a value which is sent to the server if
 * this is chosen ("TRANSIT, WALK", "CAR", etc.) - name is a name with which client can nicely name
 * this option even if specific value changes ("TRANSIT", "PARKRIDE", "TRANSIT_BICYCLE", etc.)
 * <p>
 * Travel options are created from {@link org.opentripplanner.routing.graph.Graph} transitModes
 * variable and based if park &amp; ride, bike &amp; ride, bike sharing is supported. List itself is
 * created in {@link ApiTravelOptionsMaker#makeOptions(HashSet, boolean, boolean, boolean)}
 *
 * @see ApiTravelOptionsMaker#makeOptions(HashSet, boolean, boolean, boolean) * Created by mabu on
 * 28.7.2015.
 */
public class ApiTravelOption {

  public String value;
  public String name;

  public ApiTravelOption(String value, String name) {
    this.value = value;
    this.name = name;
  }

  /**
   * Creates TravelOption where value and name are same
   */
  public ApiTravelOption(String value) {
    this.value = value;
    this.name = value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTravelOption that = (ApiTravelOption) o;
    return Objects.equals(value, that.value) && Objects.equals(name, that.name);
  }

  @Override
  public String toString() {
    return "TravelOption{" + "value='" + value + '\'' + ", name='" + name + '\'' + '}';
  }
}
