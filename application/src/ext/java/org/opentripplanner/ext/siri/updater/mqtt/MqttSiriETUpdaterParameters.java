package org.opentripplanner.ext.siri.updater.mqtt;

import java.time.Duration;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public class MqttSiriETUpdaterParameters implements UrlUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String host;
  private final int port;
  private final String user;
  private final String password;
  private final String topic;
  private final int qos;
  private final boolean fuzzyTripMatching;
  private final int numberOfPrimingWorkers;
  private final Duration maxPrimingIdleTime;

  public MqttSiriETUpdaterParameters(
    String configRef,
    String feedId,
    String host,
    int port,
    String user,
    String password,
    String topic,
    int qos,
    boolean fuzzyTripMatching,
    int numberOfPrimingWorkers,
    Duration maxPrimingIdleTime
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.host = host;
    this.port = port;
    this.user = user;
    this.password = password;
    this.topic = topic;
    this.qos = qos;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.numberOfPrimingWorkers = numberOfPrimingWorkers;
    this.maxPrimingIdleTime = maxPrimingIdleTime;
  }

  @Override
  public String url() {
    return host + ":" + port;
  }

  @Override
  public String configRef() {
    return configRef;
  }

  @Override
  public String feedId() {
    return feedId;
  }

  public String topic() {
    return topic;
  }

  public int qos() {
    return qos;
  }

  public boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  public String user() {
    return user;
  }

  public String password() {
    return password;
  }

  public int port() {
    return port;
  }

  public String host() {
    return host;
  }

  public int numberOfPrimingWorkers() {
    return numberOfPrimingWorkers;
  }

  public Duration maxPrimingIdleTime() {
    return maxPrimingIdleTime;
  }
}
