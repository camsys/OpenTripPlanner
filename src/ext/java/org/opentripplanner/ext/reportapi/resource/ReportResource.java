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
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.standalone.server.RouterService;

@Path("/report")
@Produces(MediaType.TEXT_PLAIN)
public class ReportResource {

    private final RouterService routerService;

    @SuppressWarnings("unused")
    public ReportResource(@Context OTPServer server) {
        routerService = server.getRouterService();;
    }

    @GET
    @Path("/transfers.csv")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getTransfersAsCsv() {
        Router router = routerService.getRouter();
        TransferService transferService = routerService.getRouter().graph.getTransferService();
        GraphIndex index = router.graph.index;
        return TransfersReport.export(transferService.listAll(), index);
    }
}
