package org.opentripplanner.index.graphql;

import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

public class GraphQLRequestContext {
  private final Router router;
  private final GraphIndex index;

  private boolean signMode = false;
  
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

  public void setSignMode(boolean m) {
	  this.signMode = m;
  }

  public boolean getSignMode() {
	  return this.signMode;
  }
  
}
