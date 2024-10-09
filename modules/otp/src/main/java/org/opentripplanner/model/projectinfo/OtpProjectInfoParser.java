package org.opentripplanner.model.projectinfo;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        new GraphFileHeader(get(props, "otp.serialization.version.id")),
        new VersionControlInfo(
          get(props, "git.commit.id"),
          get(props, "git.branch"),
          get(props, "git.commit.time"),
          get(props, "git.build.time"),
          getBool(props, "git.dirty")
        )
      );
      LOG.debug("Parsed Maven artifact version: {}", version);
      return version;
    } catch (Exception e) {
      LOG.error("Error reading version from properties file: {}", e.getMessage());
      return new OtpProjectInfo();
    }
  }

  private static String get(Properties props, String key) {
    return normalize(props.getProperty(key));
  }

  private static boolean getBool(Properties props, String key) {
    return "true".equalsIgnoreCase(props.getProperty(key, "true"));
  }

  private static String normalize(String text) {
    if (text == null || text.isBlank()) {
      return OtpProjectInfo.UNKNOWN;
    }
    if (text.startsWith("${") && text.endsWith("}")) {
      return OtpProjectInfo.UNKNOWN;
    }
    return text;
  }
}
