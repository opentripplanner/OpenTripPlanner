package org.opentripplanner.analyst.qbroker;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * Represents a dormant connection waiting for
 */
public class LongPollConnection {

    Task affinity;
    Request request;
    Response response;

    public LongPollConnection(Request request, Response response) {
        this.request = request;
        this.response = response;
    }


}
