package org.opentripplanner.ext.reportapi.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.opentripplanner.ext.reportapi.model.TransfersReport;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.server.OTPServer;

@Path("/report")
@Produces(MediaType.TEXT_PLAIN)
public class ReportResource {

    private final TransferService transferService;
    private final GraphIndex index;

    @SuppressWarnings("unused")
    public ReportResource(@Context OTPServer server) {
        this.transferService = server.getRouter().graph.getTransferService();
        this.index = server.getRouter().graph.index;
    }

    @GET
    @Path("/transfers.csv")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getTransfersAsCsv() {
        return TransfersReport.export(transferService.listAll(), index);
    }
}
