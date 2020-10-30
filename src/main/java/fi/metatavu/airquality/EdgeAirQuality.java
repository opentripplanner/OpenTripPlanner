package fi.metatavu.airquality;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class for air quality edge updater
 */
public class EdgeAirQuality {

  private final Map<Integer, float[]> pollutantValues;

  /**
   * Constructor
   */
  public EdgeAirQuality() {
    pollutantValues = new HashMap<>();
  }

  /**
   * Adds a pollutant value for given time
   *
   * @param time time
   * @param pollutantValue pollutantValue
   */
  public void addPollutantValueSample(int time, float pollutantValue) {
    float[] existing = pollutantValues.get(time);
    if (existing == null) {
      pollutantValues.put(time, new float[] { pollutantValue });
    } else {
      float[] updated = new float[existing.length + 1];
      System.arraycopy(existing, 0, updated, 0, existing.length);
      updated[existing.length] = pollutantValue;
      pollutantValues.put(time, updated);
    }
  }

  /**
   * Returns pollutant value average for given time
   *
   * @param time time
   * @return pollutant value
   */
  public float getPollutantValue(int time) {
    return getAverage(getPollutantValuesInTime(time));
  }

  /**
   * Returns pollutant value averages given times
   *
   * @param times times
   * @return pollutantValues
   */
  public float[] getPollutantValues(int times) {
    float[] result = new float[times];

    for (int time = 0; time < times; time++) {
      result[time] = getPollutantValue(time);
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
   * @return array of pollutant value
   */
  private float[] getPollutantValuesInTime(int time) {
    return pollutantValues.get(time);
  }

}
