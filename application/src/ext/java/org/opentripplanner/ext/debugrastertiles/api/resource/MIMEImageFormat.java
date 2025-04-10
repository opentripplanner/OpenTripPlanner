package org.opentripplanner.ext.debugrastertiles.api.resource;

import jakarta.ws.rs.BadRequestException;
import java.util.Arrays;
import java.util.Collection;

class MIMEImageFormat {

  public static final Collection<String> acceptedTypes = Arrays.asList(
    "png",
    "gif",
    "jpeg",
    "geotiff"
  );

  public final String type;

  public MIMEImageFormat(String s) {
    String[] parts = s.split("/");
    if (parts.length == 2 && parts[0].equals("image")) {
      if (acceptedTypes.contains(parts[1])) {
        type = parts[1];
      } else {
        throw new BadRequestException("unsupported image format: " + parts[1]);
      }
    } else {
      throw new BadRequestException("malformed image format mime type: " + s);
    }
  }

  public String toString() {
    return "image/" + type;
  }
}
