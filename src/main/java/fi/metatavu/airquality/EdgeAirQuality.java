package fi.metatavu.airquality;

import java.util.HashMap;
import java.util.Map;
public class EdgeAirQuality {

  private Map<Integer, float[]> airQualities;

  /**
   * Constructor
   */
  public EdgeAirQuality() {
    airQualities = new HashMap<>();
  }

  /**
   * Adds air quality index for given time
   *
   * @param time time
   * @param airQuality air quality index
   */
  public void addAirQualitySample(int time, float airQuality) {
    float[] existing = airQualities.get(time);
    if (existing == null) {
      airQualities.put(time, new float[] { airQuality });
    } else {
      float[] updated = new float[existing.length + 1];
      System.arraycopy(existing, 0, updated, 0, existing.length);
      updated[existing.length] = airQuality;
      airQualities.put(time, updated);
    }
  }

  /**
   * Returns air quality average for given time
   *
   * @param time time
   * @return air quality index
   */
  public float getAirQuality(int time) {
    return getAverage(getAirQualitiesInTime(time));
  }

  /**
   * Returns air quality averages given times
   *
   * @param times times
   * @return air quality indexes
   */
  public float[] getAirQualities(int times) {
    float[] result = new float[times];

    for (int time = 0; time < times; time++) {
      result[time] = getAirQuality(time);
    }

    return result;
  }

  /**
   * Returns average for values
   *
   * @param values values
   * @return average
   */
  private float getAverage(float[] values) {
    if (values == null) {
      return 0;
    }

    float result = 0f;

    for (float value : values) {
      result += value;
    }

    return result / values.length;
  }

  /**
   * Returns array of air quality indices in time
   *
   * @param time time
   * @return array of air quality indices
   */
  private float[] getAirQualitiesInTime(int time) {
    return airQualities.get(time);
  }

}
