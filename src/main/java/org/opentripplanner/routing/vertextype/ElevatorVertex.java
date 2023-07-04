package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marker class for elevator vertices.
 */
public abstract class ElevatorVertex extends StreetVertex {

    // assumes MTA format 120S-S2P_EL145_S-IN (STOP ID-TYPE OF PATHWAY_ELEVATOR ID_PLATFORM DIRECTION-TRAVERSE_DIRECTION
    private static final Pattern ELEVATOR_ID_IN_PATHWAY = Pattern.compile(".*_(EL[A-Z0-9]+)_.*");
    private String elevatorId = null;

    public ElevatorVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, new NonLocalizedString(name));
        if (label != null) {
            Matcher m = ELEVATOR_ID_IN_PATHWAY.matcher(label);
            if (m.matches()) elevatorId = m.group(1);
        }
    }

    /**
     * Provide the elevator id present at this vertex
     *
     * @return The id of the elevator present at this vertex
     */

    public String getElevatorId() {
        return elevatorId;
    }
}
