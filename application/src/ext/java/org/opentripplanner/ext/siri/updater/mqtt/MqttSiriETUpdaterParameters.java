package org.opentripplanner.ext.siri.updater.mqtt;

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
    int numberOfPrimingWorkers
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
}
