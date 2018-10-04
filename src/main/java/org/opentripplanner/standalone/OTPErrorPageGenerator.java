package org.opentripplanner.standalone;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;

public class OTPErrorPageGenerator implements ErrorPageGenerator {
    @Override
    public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception) {
        return "<!DOCTYPE html><html><head><meta name=\"robots\" content=\"noindex, nofollow\"><title>OpenTripPlanner error</title></head><body>Internal server error</body></html>";
    }
}
