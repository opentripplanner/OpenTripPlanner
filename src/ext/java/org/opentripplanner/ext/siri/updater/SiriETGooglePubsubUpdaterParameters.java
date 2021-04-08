package org.opentripplanner.ext.siri.updater;

public class SiriETGooglePubsubUpdaterParameters {
  private final String configRef;
  private final String feedId;
  private final String type;
  private final String projectName;
  private final String topicName;
  private final String dataInitializationUrl;
  private final int reconnectPeriodSec;
  private final boolean purgeExpiredData;

  public SiriETGooglePubsubUpdaterParameters(
      String configRef,
      String feedId,
      String type,
      String projectName,
      String topicName,
      String dataInitializationUrl,
      int reconnectPeriodSec,
      boolean purgeExpiredData
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.type = type;
    this.projectName = projectName;
    this.topicName = topicName;
    this.dataInitializationUrl = dataInitializationUrl;
    this.reconnectPeriodSec = reconnectPeriodSec;
    this.purgeExpiredData = purgeExpiredData;
  }

  String getConfigRef() { return configRef; }
  String getFeedId() { return feedId; }
  String getType() { return this.type; }
  String getProjectName() { return this.projectName; }
  String getTopicName() { return this.topicName; }
  String getDataInitializationUrl() { return this.dataInitializationUrl; }
  boolean purgeExpiredData() { return this.purgeExpiredData; }
  int getReconnectPeriodSec() { return this.reconnectPeriodSec; }
}
