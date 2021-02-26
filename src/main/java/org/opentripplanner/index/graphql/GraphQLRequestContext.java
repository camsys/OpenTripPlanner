package org.opentripplanner.index.graphql;

import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

public class GraphQLRequestContext {
  private final Router router;
  private final GraphIndex index;

  public GraphQLRequestContext(Router router, GraphIndex index) {
    this.router = router;
    this.index = index;
  }

  public Router getRouter() {
    return router;
  }

  public GraphIndex getIndex() {
	    return index;
  }

}
