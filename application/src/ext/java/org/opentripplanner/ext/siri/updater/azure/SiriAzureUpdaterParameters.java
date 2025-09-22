package org.opentripplanner.ext.siri.updater.azure;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;

public abstract class SiriAzureUpdaterParameters {

  private String configRef;
  private final String type;
  private AuthenticationType authenticationType;
  private String fullyQualifiedNamespace;
  private String serviceBusUrl;
  private String topicName;
  private String dataInitializationUrl;
  private String feedId;
  private int timeout;
  /**
   * Maximum time to wait for real-time services during startup.
   * If exceeded, OTP starts without real-time data for graceful degradation.
   */
  private Duration startupTimeout = Duration.ofMinutes(5); // 5 minutes default

  private boolean fuzzyTripMatching;
  private Duration autoDeleteOnIdle;
  private int prefetchCount;

  public SiriAzureUpdaterParameters(String type) {
    this.type = type;
  }

  public String configRef() {
    return configRef;
  }

  public void setConfigRef(String configRef) {
    this.configRef = configRef;
  }

  public String getType() {
    return type;
  }

  public AuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  public void setAuthenticationType(AuthenticationType authenticationType) {
    this.authenticationType = authenticationType;
  }

  public String getFullyQualifiedNamespace() {
    return fullyQualifiedNamespace;
  }

  public void setFullyQualifiedNamespace(String fullyQualifiedNamespace) {
    this.fullyQualifiedNamespace = fullyQualifiedNamespace;
  }

  public String getServiceBusUrl() {
    return serviceBusUrl;
  }

  public void setServiceBusUrl(String serviceBusUrl) {
    this.serviceBusUrl = serviceBusUrl;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public String getDataInitializationUrl() {
    return dataInitializationUrl;
  }

  public void setDataInitializationUrl(String dataInitializationUrl) {
    this.dataInitializationUrl = dataInitializationUrl;
  }

  public String feedId() {
    return feedId;
  }

  public void setFeedId(String feedId) {
    this.feedId = feedId;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * Gets the startup timeout for real-time service initialization.
   *
   * @return timeout in milliseconds
   */
  public Duration getStartupTimeout() {
    return startupTimeout;
  }

  /**
   * Sets the startup timeout for real-time service initialization.
   *
   * @param startupTimeout timeout in milliseconds, should be positive
   */
  public void setStartupTimeout(Duration startupTimeout) {
    this.startupTimeout = startupTimeout;
  }

  public boolean isFuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  public void setFuzzyTripMatching(boolean fuzzyTripMatching) {
    this.fuzzyTripMatching = fuzzyTripMatching;
  }

  public Duration getAutoDeleteOnIdle() {
    return autoDeleteOnIdle;
  }

  public void setAutoDeleteOnIdle(Duration autoDeleteOnIdle) {
    this.autoDeleteOnIdle = autoDeleteOnIdle;
  }

  public int getPrefetchCount() {
    return prefetchCount;
  }

  public void setPrefetchCount(int prefetchCount) {
    this.prefetchCount = prefetchCount;
  }

  /**
   * Create the url used for fetching initial data. Returns empty if there is no initial data url
   * configured.
   */
  public abstract Optional<URI> buildDataInitializationUrl() throws URISyntaxException;
}
