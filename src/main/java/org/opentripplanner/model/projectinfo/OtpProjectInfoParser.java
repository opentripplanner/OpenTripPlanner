package org.opentripplanner.model.projectinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

class OtpProjectInfoParser {
  private static final Logger LOG = LoggerFactory.getLogger(OtpProjectInfo.class);
  private static final String FILENAME = "otp-project-info.properties";

  static OtpProjectInfo loadFromProperties() {
    try {
      InputStream in = OtpProjectInfo.class.getClassLoader().getResourceAsStream(FILENAME);
      Properties props = new java.util.Properties();
      props.load(in);
      OtpProjectInfo version = new OtpProjectInfo(
          normalize(props.getProperty("project.version")),
          new VersionControlInfo(
              normalize(props.getProperty("git.commit.id")),
              normalize(props.getProperty("git.branch")),
              normalize(props.getProperty("git.commit.time")),
              normalize(props.getProperty("git.build.time")),
              "true".equalsIgnoreCase(props.getProperty("git.dirty", "true"))
          )
      );
      LOG.debug("Parsed Maven artifact version: {}", version.toString());
      return version;
    } catch (Exception e) {
      LOG.error("Error reading version from properties file: {}", e.getMessage());
      return new OtpProjectInfo();
    }
  }

  private static String normalize(String text) {
    if(text == null || text.isBlank()) { return OtpProjectInfo.UNKNOWN; }
    if(text.startsWith("${") && text.endsWith("}")) { return OtpProjectInfo.UNKNOWN; }
    return text;
  }
}
