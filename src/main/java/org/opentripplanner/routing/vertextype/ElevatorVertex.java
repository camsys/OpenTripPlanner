package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marker class for elevator vertices.
 */
public abstract class ElevatorVertex extends StreetVertex {

    private static final Pattern ELEVATOR_ID_IN_PATHWAY = Pattern.compile(".*_(EL[A-Z0-9]+)_.*");

    public ElevatorVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, new NonLocalizedString(name));
    }

    /**
     * Provide the elevator id present at this vertex
     *
     * @return The id of the elevator present at this vertex
     */

    public String getElevatorId() {
        Matcher m = ELEVATOR_ID_IN_PATHWAY.matcher(getLabel());
        if (m.matches()) return m.group(1);
        return null;//no match
    }
}
