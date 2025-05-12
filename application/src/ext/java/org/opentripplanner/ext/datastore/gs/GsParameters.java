package org.opentripplanner.ext.datastore.gs;

public interface GsParameters {
  String host();

  String credentialFile();

  static GsParameters defaultValues() {
    return new GsParameters() {
      @Override
      public String host() {
        return null;
      }

      @Override
      public String credentialFile() {
        return null;
      }
    };
  }
}
