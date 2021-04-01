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

import java.util.List;

public class VehicleInfo {

    public enum StopStatus { INCOMING_AT, STOPPED_AT, IN_TRANSIT_TO };
    public enum OccupancyStatus {EMPTY, MANY_SEATS_AVAILABLE, FEW_SEATS_AVAILABLE,
        STANDING_ROOM_ONLY, CRUSHED_STANDING_ROOM_ONLY, FULL, NOT_ACCEPTING_PASSENGERS, NO_DATA_AVAILABLE};

    private String vehicleId;

    private String vehicleLabel;

    private Double lat;

    private Double lon;

    private String stop;

    private Integer currentStopSequence;

    private StopStatus currentStopStatus;

    private Double bearing;

    private OccupancyStatus occupancyStatus;

    private List<CarriageInfo> carriages;

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public void setVehicleLabel(String vehicleLabel) {
        this.vehicleLabel = vehicleLabel;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public Integer getCurrentStopSequence() {
        return currentStopSequence;
    }

    public void setCurrentStopSequence(Integer currentStopSequence) {
        this.currentStopSequence = currentStopSequence;
    }

    public StopStatus getCurrentStopStatus() {
        return currentStopStatus;
    }

    public void setCurrentStopStatus(StopStatus currentStopStatus) {
        this.currentStopStatus = currentStopStatus;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public OccupancyStatus getOccupancyStatus() { return occupancyStatus; }

    public void setOccupancyStatus(OccupancyStatus occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public List<CarriageInfo> getCarriages() {
        return carriages;
    }

    public void setCarriages(List<CarriageInfo> carriages) {
        this.carriages = carriages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VehicleInfo that = (VehicleInfo) o;

        if (vehicleId != null ? !vehicleId.equals(that.vehicleId) : that.vehicleId != null) return false;
        if (vehicleLabel != null ? !vehicleLabel.equals(that.vehicleLabel) : that.vehicleLabel != null) return false;
        if (lat != null ? !lat.equals(that.lat) : that.lat != null) return false;
        if (lon != null ? !lon.equals(that.lon) : that.lon != null) return false;
        if (stop != null ? !stop.equals(that.stop) : that.stop != null) return false;
        if (currentStopSequence != null ? !currentStopSequence.equals(that.currentStopSequence) : that.currentStopSequence != null)
            return false;
        if (currentStopStatus != that.currentStopStatus) return false;
        if (occupancyStatus != null ? !occupancyStatus.equals(that.occupancyStatus) : that.occupancyStatus != null) return false;
        return bearing != null ? bearing.equals(that.bearing) : that.bearing == null;
    }

    @Override
    public int hashCode() {
        int result = vehicleId != null ? vehicleId.hashCode() : 0;
        result = 31 * result + (vehicleLabel != null ? vehicleLabel.hashCode() : 0);
        result = 31 * result + (lat != null ? lat.hashCode() : 0);
        result = 31 * result + (lon != null ? lon.hashCode() : 0);
        result = 31 * result + (stop != null ? stop.hashCode() : 0);
        result = 31 * result + (currentStopSequence != null ? currentStopSequence.hashCode() : 0);
        result = 31 * result + (currentStopStatus != null ? currentStopStatus.hashCode() : 0);
        result = 31 * result + (bearing != null ? bearing.hashCode() : 0);
        result = 31 * result + (occupancyStatus != null ? occupancyStatus.hashCode() : 0);
        return result;
    }
}
