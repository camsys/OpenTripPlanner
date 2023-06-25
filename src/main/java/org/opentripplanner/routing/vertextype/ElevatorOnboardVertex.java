package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevatorOnboardVertex extends ElevatorVertex {

    private static final long serialVersionUID = 20120209L;

    public ElevatorOnboardVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, name);
    }
}
