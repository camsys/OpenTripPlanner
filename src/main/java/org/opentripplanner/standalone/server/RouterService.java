package org.opentripplanner.standalone.server;

/**
 * Simple wrapper around Router allowing it to be swapped out dynamically.
 */
public class RouterService {
    private Router router;
    public void setRouter(Router router) {
        this.router = router;
    }
    public Router getRouter() {
        return router;
    }
}
