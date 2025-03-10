package org.opentripplanner.framework.resources;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class XMllet {

  public static void main(String[] args) throws IOException {

    for (String lang : Set.of("en", "fr", "de", "hu", "nl")) {
      var loc = new Locale(lang);
      Properties properties = new Properties();



      var bundle = ResourceBundle.getBundle("Message", loc);

      bundle.keySet().stream().forEach(key -> properties.setProperty(key.toString(), bundle.getString(key)));
      // userCreated.properties is created at the mentioned path
      FileOutputStream fileOutputStream = new FileOutputStream("application/src/main/resources/Messages_%s.xml".formatted(lang));

      // storeToXML() method is used to write the properties into properties xml file
      properties.storeToXML(fileOutputStream, "COmment");
      fileOutputStream.close();
    }
  }
}
