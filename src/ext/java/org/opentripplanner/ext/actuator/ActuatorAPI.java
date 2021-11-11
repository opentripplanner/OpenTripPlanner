package org.opentripplanner.ext.actuator;

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

    public static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT
    );

    private final Router router;

    public ActuatorAPI(@Context OTPServer otpServer) {
        this.router = otpServer.getRouter();

        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        new FileDescriptorMetrics().bindTo(prometheusRegistry);
        new JvmCompilationMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new JvmHeapPressureMetrics().bindTo(prometheusRegistry);
        new JvmInfoMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);
        new LogbackMetrics().bindTo(prometheusRegistry);
        new ProcessorMetrics().bindTo(prometheusRegistry);
        new UptimeMetrics().bindTo(prometheusRegistry);

        new GuavaCacheMetrics(
                otpServer.getRouter().graph
                        .getTransitLayer().getTransferCache().getTransferCache(),
                "raptorTransfersCache",
                List.of(Tag.of("cache", "raptorTransfers"))
        ).bindTo(prometheusRegistry);

        new ExecutorServiceMetrics(
                ForkJoinPool.commonPool(),
                "commonPool",
                List.of(Tag.of("pool", "commonPool"))
        ).bindTo(prometheusRegistry);

        if (otpServer.getRouter().graph.updaterManager != null) {
            new ExecutorServiceMetrics(
                    otpServer.getRouter().graph.updaterManager.getUpdaterPool(),
                    "graphUpdaters",
                    List.of(Tag.of("pool", "graphUpdaters"))
            ).bindTo(prometheusRegistry);

            new ExecutorServiceMetrics(
                    otpServer.getRouter().graph.updaterManager.getScheduler(),
                    "graphUpdateScheduler",
                    List.of(Tag.of("pool", "graphUpdateScheduler"))
            ).bindTo(prometheusRegistry);
        }

        if (otpServer.getRouter().raptorConfig.isMultiThreaded()) {
            new ExecutorServiceMetrics(
                    otpServer.getRouter().raptorConfig.threadPool(),
                    "raptorHeuristics",
                    List.of(Tag.of("pool", "raptorHeuristics"))
            ).bindTo(prometheusRegistry);
        }
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
            int waitingUpdaters = router.graph.updaterManager.numberOfNonePrimedUpdaters();

            if (waitingUpdaters > 0) {
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
