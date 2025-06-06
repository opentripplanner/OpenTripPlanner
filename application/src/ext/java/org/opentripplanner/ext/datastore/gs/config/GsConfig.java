package org.opentripplanner.ext.datastore.gs.config;

import org.opentripplanner.datastore.api.GsParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class GsConfig implements GsParameters {

  private final String host;
  private final String credentialFile;

  public GsConfig(String host, String credentialFile) {
    this.host = host;
    this.credentialFile = credentialFile;
  }

  public static GsConfig fromConfig(NodeAdapter root, String parameterName) {
    NodeAdapter gsRoot = root
      .of(parameterName)
      .since(OtpVersion.V2_8)
      .summary("Configuration for Google Cloud Storage")
      .asObject();

    String host = gsRoot
      .of("cloudServiceHost")
      .since(OtpVersion.V2_8)
      .summary("Host of the Google Cloud Storage Server")
      .description(
        """
        Host of the Google Cloud Storage server. In case of a real GCS Bucket this parameter can be
        omitted. When the host differs from the usual GCS host, for example when emulating GCS in a
        docker container for testing purposes, the host has to be specified including the port.
        Eg: http://localhost:4443"""
      )
      .asString(null);

    String credentialFile = gsRoot
      .of("credentialFile")
      .since(OtpVersion.V2_8)
      .summary("Local file system path to Google Cloud Platform service accounts credentials file.")
      .description(
        """
        The credentials are used to access GCS URLs. When using GCS from outside of Google Cloud you
        need to provide a path the the service credentials. Environment variables in the path are
        resolved.

        This is a path to a file on the local file system, not an URI.
        """
      )
      .asString(null);

    return new GsConfig(host, credentialFile);
  }

  @Override
  public String host() {
    return host;
  }

  @Override
  public String credentialFile() {
    return credentialFile;
  }
}
