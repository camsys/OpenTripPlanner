/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.pattern_graph.model;
import org.opentripplanner.pattern_graph.model.StopAttribute;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.profile.StopCluster;

import java.util.HashSet;
import java.util.Set;

public class StopNode {

    private StopShort oldAttributes;

    private StopAttribute attribute;

    private Set<String> oldSuccessors = new HashSet<>();

    private Set<SuccessorAttribute> successors = new HashSet<>();

    private String id;

    public StopNode(String id) {
        this.id = id;
    }

    public StopShort getOldAttributes() {
        return oldAttributes;
    }

    public void setOldAttributes(StopShort oldAttributes) {
        this.oldAttributes = oldAttributes;
    }

    public Set<String> getOldSuccessors() {
        return oldSuccessors;
    }

    public Set<SuccessorAttribute> getSuccessors() {
        return successors;
    }

    public String getId() {
        return this.id;
    }

    public void addOldSuccessor(StopNode node) {
        oldSuccessors.add(node.getId());
    }

    public void addSuccessor(SuccessorAttribute sA) { successors.add(sA); }

    public StopAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(StopAttribute attribute) {
        this.attribute = attribute;
    }
}
