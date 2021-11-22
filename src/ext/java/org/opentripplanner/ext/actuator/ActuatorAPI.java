package org.opentripplanner.ext.actuator;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/actuators")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class ActuatorAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ActuatorAPI.class);

    private static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT
    );

    static {
        Metrics.globalRegistry.add(prometheusRegistry);
    }

    private final Router router;

    public ActuatorAPI(@Context OTPServer otpServer) {
        this.router = otpServer.getRouter();
    }

    /**
     * List the actuator endpoints available
     */
    @GET
    public Response actuator(@Context UriInfo uriInfo) {
        return Response.status(Response.Status.OK).entity(String.format(
            "{\n"
            + "  \"_links\" : {\n"
            + "    \"self\" : {\n"
            + "      \"href\" : \"%1$s\",\n"
            + "      \"templated\" : false\n"
            + "    },\n"
            + "    \"health\" : {\n"
            + "      \"href\" : \"%1$s/health\",\n"
            + "      \"templated\" : false\n"
            + "    },\n"
            + "    \"prometheus\" : {\n"
            + "      \"href\" : \"%1$s/prometheus\",\n"
            + "      \"templated\" : false\n"
            + "    }\n"
            + "  }\n"
            + "}", uriInfo.getRequestUri().toString()))
            .type("application/json").build();
    }

    /**
     * Return 200 when the instance is ready to use
     */
    @GET
    @Path("/health")
    public Response health() {
        if (router.graph.updaterManager != null) {
            Collection<String> waitingUpdaters = router.graph.updaterManager.waitingUpdaters();

            if (!waitingUpdaters.isEmpty()) {
                LOG.info("Graph ready, waiting for updaters: {}", waitingUpdaters);
                throw new WebApplicationException(Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("Graph ready, waiting for updaters: " + waitingUpdaters + "\n")
                    .type("text/plain")
                    .build());
            }
        }

        return Response.status(Response.Status.OK).entity(
            "{\n"
            + "  \"status\" : \"UP\""
            + "\n}")
            .type("application/json").build();
    }

    /**
     * Returns micrometer metrics in a prometheus structured format.
     */
    @GET
    @Path("/prometheus")
    public Response prometheus() {
        return Response.status(Response.Status.OK)
            .entity(prometheusRegistry.scrape())
            .type("text/plain")
            .build();
    }
}
