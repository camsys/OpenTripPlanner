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
package org.opentripplanner.api.model;

import org.opentripplanner.routing.vertextype.ParkAndRideVertex;

public class ParkAndRideModel {

    private double lat;

    private double lon;

    private String name;

    private String id;

    public ParkAndRideModel(ParkAndRideVertex vertex) {
        this.lat = vertex.getLat();
        this.lon = vertex.getLon();
        this.name = vertex.getName();
        this.id = vertex.getId();
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
