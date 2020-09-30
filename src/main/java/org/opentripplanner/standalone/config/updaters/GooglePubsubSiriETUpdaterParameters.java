package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriEstimatedTimetableGooglePubsubUpdater;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.updaters.sources.UpdaterSourceParameters;

public class GooglePubsubSiriETUpdaterParameters extends UpdaterSourceParameters implements SiriEstimatedTimetableGooglePubsubUpdater.Parameters {

  private final String feedId;
  private final boolean purgeExpiredData;
  private final String dataInitializationUrl;
  private final int reconnectPeriodSec;
  private final String type;
  private final String projectName;
  private final String topicName;

  public GooglePubsubSiriETUpdaterParameters(NodeAdapter c) {
    super(c);
    feedId = c.asText("feedId", null);
    purgeExpiredData = c.asBoolean("purgeExpiredData", false);
    dataInitializationUrl = c.asText("dataInitializationUrl", null);
    reconnectPeriodSec = c.asInt("reconnectPeriodSec", 30);

    type = c.asText("type");
    projectName = c.asText("projectName");
    topicName = c.asText("topicName");
  }

  @Override
  public String getFeedId() {
    return feedId;
  }

  @Override
  public String getDataInitializationUrl() {
    return dataInitializationUrl;
  }

  @Override
  public boolean purgeExpiredData() {
    return purgeExpiredData;
  }


  @Override
  public int getReconnectPeriodSec() {
    return reconnectPeriodSec;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getProjectName() {
    return projectName;
  }

  @Override
  public String getTopicName() {
    return topicName;
  }
}
