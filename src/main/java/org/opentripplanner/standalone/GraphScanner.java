package org.opentripplanner.standalone;

import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.standalone.server.RouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simplified support for graph reloading via replacing the Router.
 *
 * Implementation details:  Uses graph file lastModified as the test.
 */
public class GraphScanner {

    private static final int AUTOSCAN_PERIOD_SEC = 10;
    private static final Logger LOG = LoggerFactory.getLogger(GraphScanner.class);
    private ScheduledExecutorService scanExecutor;
    private RouterService routerService;
    private DataSource inputGraph;
    private OTPAppConstruction app;
    private long lastGraphAge;

    public GraphScanner(OTPAppConstruction app, RouterService routerService, DataSource inputGraph) {
        this.app = app;
        this.routerService = routerService;
        this.inputGraph = inputGraph;
        lastGraphAge = inputGraph.lastModified();
    }

    /**
     * check for a new graph.
     */
    public void scan() {
        if (graphChanged()) {
            long start = System.currentTimeMillis();
            LOG.info("loading new graph with file date " + new Date(inputGraph.lastModified()));
            SerializedGraphObject obj = SerializedGraphObject.load(inputGraph);
            Graph graph = obj.graph;
            app.config().updateConfigFromSerializedGraph(obj.buildConfig, obj.routerConfig);
            graph.index();
            app.setOtpConfigVersionsOnServerInfo();
            Router newRouter = new Router(graph, app.config().routerConfig());
            newRouter.startup();
            app.reloadConfig();
            Router oldRouter = routerService.getRouter();
            routerService.setRouter(newRouter);
            if (oldRouter != null) {
                oldRouter.reset();
            }
            oldRouter = null;
            System.gc(); // give a hint to JVM to clean up now
            LOG.info("new graph load complete in " + (System.currentTimeMillis()-start) + "ms");
        } else {
            LOG.debug("graph stable");
        }
    }

    private boolean graphChanged() {
        boolean hasChanged = lastGraphAge != inputGraph.lastModified();
        if (hasChanged) lastGraphAge = inputGraph.lastModified();
        return hasChanged;
    }


    public void start() {
        scanExecutor = Executors.newSingleThreadScheduledExecutor();
        scanExecutor.scheduleWithFixedDelay(new WorkerThread(this),
                AUTOSCAN_PERIOD_SEC, AUTOSCAN_PERIOD_SEC, TimeUnit.SECONDS);
    }

    private static class WorkerThread implements Runnable {

        private GraphScanner scanner;
        public WorkerThread(GraphScanner scanner) {
            this.scanner = scanner;
        }
        @Override
        public void run() {
            try {
                scanner.scan();
            } catch (Throwable t) {
                LOG.error("GraphScanner failed: ", t, t);
            }
        }
    }
}
