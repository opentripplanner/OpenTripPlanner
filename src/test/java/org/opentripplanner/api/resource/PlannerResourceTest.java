package org.opentripplanner.api.resource;
import org.junit.Test;
import org.opentripplanner.api.common.ParameterException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import static org.junit.Assert.assertEquals;

public class PlannerResourceTest {
	@Test
    public void testShuttlePenaltyParam() throws IllegalAccessException, ParameterException {
        OTPServer otpServer = new OTPServer(new CommandLineParameters(), new GraphService());
        otpServer.getGraphService().registerGraph("A", new MemoryGraphSource("", new Graph()));
        PlannerResource resource = new PlannerResource();
        resource.routerId = "A";
        FieldUtils.writeField(resource, "otpServer", otpServer, true);
        assertEquals(resource.buildRequest().mbtaShuttlePenalty, 0);

        FieldUtils.writeField(resource, "shuttlePenalty", 299, true);
        assertEquals(resource.buildRequest().mbtaShuttlePenalty, 299);
    }
}
