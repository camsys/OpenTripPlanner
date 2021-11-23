package org.opentripplanner.ext.flex.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

import java.util.Locale;

public class FlexDeviationEdge extends TemporaryFreeEdge {

	private static final long serialVersionUID = 2302097796401633525L;

	public FlexDeviationEdge(TemporaryVertex v1, Vertex v2) {
		super(v1, v2);
	}

	public FlexDeviationEdge(Vertex v1, TemporaryVertex v2) {
		super(v1, v2);
	}

	@Override
	public State traverse(State s0) {
		StateEditor editor = s0.edit(this);
	    editor.setBackMode(TraverseMode.BUS);
	    
	    editor.incrementWeight(60 * 5);
	    editor.incrementTimeInSeconds(60 * 5);
	    
	    editor.resetEnteredNoThroughTrafficArea();
	    
	    return editor.makeState();
	}
	
	@Override
	public String getName() {
		return null;
	}
	
	@Override
	public String getName(Locale locale) {
		return null;
	}
}
