package org.opentripplanner.transit.raptor.api.response;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Collection;

public class RaptorResponse<T extends RaptorTripSchedule> {
    private final Collection<Path<T>> paths;
    private final RaptorRequest<T> requestOriginal;
    private final RaptorRequest<T> requestUsed;

    public RaptorResponse(
            Collection<Path<T>> paths,
            RaptorRequest<T> requestOriginal,
            RaptorRequest<T> requestUsed
    ) {
        this.paths = paths;
        this.requestOriginal = requestOriginal;
        this.requestUsed = requestUsed;
    }

    public Collection<Path<T>> paths() {
        return paths;
    }

    public RaptorRequest<T> requestOriginal() {
        return requestOriginal;
    }

    public RaptorRequest<T> requestUsed() {
        return requestUsed;
    }
}
