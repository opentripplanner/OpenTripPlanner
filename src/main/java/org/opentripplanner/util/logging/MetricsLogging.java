package org.opentripplanner.util.logging;

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
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;

/**
 * This class is responsible for wiring up various metrics to micrometer, which we use for
 * performance logging, through the Actuator API.
 */
public class MetricsLogging {

    public MetricsLogging(OTPServer otpServer) {
        new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
        new FileDescriptorMetrics().bindTo(Metrics.globalRegistry);
        new JvmCompilationMetrics().bindTo(Metrics.globalRegistry);
        new JvmGcMetrics().bindTo(Metrics.globalRegistry);
        new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
        new JvmInfoMetrics().bindTo(Metrics.globalRegistry);
        new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
        new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
        new LogbackMetrics().bindTo(Metrics.globalRegistry);
        new ProcessorMetrics().bindTo(Metrics.globalRegistry);
        new UptimeMetrics().bindTo(Metrics.globalRegistry);

        Router router = otpServer.getRouter();
        Graph graph = router.graph;

        if(graph.getTransitLayer() != null) {
            new GuavaCacheMetrics(
                    graph.getTransitLayer().getTransferCache().getTransferCache(),
                    "raptorTransfersCache",
                    List.of(Tag.of("cache", "raptorTransfers"))
            ).bindTo(Metrics.globalRegistry);
        }
        new ExecutorServiceMetrics(
                ForkJoinPool.commonPool(),
                "commonPool",
                List.of(Tag.of("pool", "commonPool"))
        ).bindTo(Metrics.globalRegistry);

        if (graph.updaterManager != null) {
            new ExecutorServiceMetrics(
                    graph.updaterManager.getUpdaterPool(),
                    "graphUpdaters",
                    List.of(Tag.of("pool", "graphUpdaters"))
            ).bindTo(Metrics.globalRegistry);

            new ExecutorServiceMetrics(
                    graph.updaterManager.getScheduler(),
                    "graphUpdateScheduler",
                    List.of(Tag.of("pool", "graphUpdateScheduler"))
            ).bindTo(Metrics.globalRegistry);
        }

        if (router.raptorConfig.isMultiThreaded()) {
            new ExecutorServiceMetrics(
                    router.raptorConfig.threadPool(),
                    "raptorHeuristics",
                    List.of(Tag.of("pool", "raptorHeuristics"))
            ).bindTo(Metrics.globalRegistry);
        }
    }
}
