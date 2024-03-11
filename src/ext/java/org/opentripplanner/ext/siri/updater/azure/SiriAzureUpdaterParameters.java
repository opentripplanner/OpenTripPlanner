package org.opentripplanner.ext.siri.updater.azure;

public abstract class SiriAzureUpdaterParameters {

  private String configRef;
  private String type;
  private AuthenticationType authenticationType;
  private String fullyQualifiedNamespace;
  private String serviceBusUrl;
  private String topicName;
  private String dataInitializationUrl;
  private String feedId;
  private int timeout;

  private boolean fuzzyTripMatching;

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

  public boolean isFuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  public void setFuzzyTripMatching(boolean fuzzyTripMatching) {
    this.fuzzyTripMatching = fuzzyTripMatching;
  }
}
