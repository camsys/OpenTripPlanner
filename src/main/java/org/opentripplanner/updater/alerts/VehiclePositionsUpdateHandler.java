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

package org.opentripplanner.updater.alerts;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtimeMNR;
import com.google.transit.realtime.GtfsRealtimeMNR.MnrVehiclePosition;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.model.VehicleInfo;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.TimePeriod;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;


/**
 * This updater updates AlertPatches via VehiclePosition messages
 */
public class VehiclePositionsUpdateHandler extends AbstractUpdateHandler {

    public void update(FeedMessage message) {
        alertPatchService.expire(patchIds);
        patchIds.clear();

        for (FeedEntity entity : message.getEntityList()) {
            if (entity.hasVehicle()) {
                handleVehiclePosition(entity.getVehicle());
            }
        }
    }

    private void handleVehiclePosition(VehiclePosition vehiclePosition) {
        if (vehiclePosition.hasTrip()) {
            String tripId = vehiclePosition.getTrip().getTripId();
            AlertPatch patch = new AlertPatch();
            patch.setTrip(new AgencyAndId(feedId, tripId));
            ServiceDate sd;
            // time period should be the service day for which the trip is active
            if (vehiclePosition.getTrip().hasStartDate()) {
                try {
                    sd = ServiceDate.parseString(vehiclePosition.getTrip().getStartDate());
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    return;
                }
            } else {
                sd = new ServiceDate(new Date(vehiclePosition.getTimestamp() * 1000));
            }
            ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
            periods.add(new TimePeriod(sd.getAsDate().getTime() / 1000, sd.next().getAsDate().getTime() / 1000));
            patch.setTimePeriods(periods);

            VehicleInfo vehicleInfo = new VehicleInfo();
            if (vehiclePosition.getVehicle().hasId()) {
                vehicleInfo.setVehicleId(vehiclePosition.getVehicle().getId());
            }
            if (vehiclePosition.getVehicle().hasLabel()) {
                vehicleInfo.setVehicleLabel(vehiclePosition.getVehicle().getLabel());
            }
            if (vehiclePosition.hasPosition()) {
                if (vehiclePosition.getPosition().hasLatitude()) {
                    vehicleInfo.setLat((double) vehiclePosition.getPosition().getLatitude());
                }
                if (vehiclePosition.getPosition().hasLongitude()) {
                    vehicleInfo.setLon((double) vehiclePosition.getPosition().getLongitude());
                }
            }
            if (vehiclePosition.hasStopId()) {
                vehicleInfo.setStop(vehiclePosition.getStopId());
            }
            if (vehiclePosition.hasCurrentStopSequence()) {
                vehicleInfo.setCurrentStopSequence(vehiclePosition.getCurrentStopSequence());
            }
            if (vehiclePosition.hasExtension(GtfsRealtimeMNR.mnrVehiclePosition)) {
                MnrVehiclePosition ext = vehiclePosition.getExtension(GtfsRealtimeMNR.mnrVehiclePosition);
                if (ext.hasCurrentStatus()) {
                    vehicleInfo.setCurrentStopStatus(convertVehicleStatus(ext.getCurrentStatus()));
                }
            }
            if (vehiclePosition.hasCurrentStatus() && vehicleInfo.getCurrentStopStatus() == null) {
                vehicleInfo.setCurrentStopStatus(convertVehicleStatus(vehiclePosition.getCurrentStatus()));
            }
            if (vehiclePosition.getPosition().hasBearing()) {
                vehicleInfo.setBearing((double) vehiclePosition.getPosition().getBearing());
            }
            patch.setVehicleInfo(vehicleInfo);
            String patchId = tripId + " " + vehiclePosition.getVehicle().getId();
            patch.setId(patchId);
            patchIds.add(patchId);
            alertPatchService.apply(patch);
        }
    }

    private static VehicleInfo.StopStatus convertVehicleStatus(VehiclePosition.VehicleStopStatus status) {
        switch(status) {
            case INCOMING_AT:
                return VehicleInfo.StopStatus.INCOMING_AT;
            case STOPPED_AT:
                return VehicleInfo.StopStatus.STOPPED_AT;
            case IN_TRANSIT_TO:
                return VehicleInfo.StopStatus.IN_TRANSIT_TO;
        }
        return null;
    }

    private static VehicleInfo.StopStatus convertVehicleStatus(MnrVehiclePosition.MnrVehicleStopStatus status) {
        if (MnrVehiclePosition.MnrVehicleStopStatus.DELAYED.equals(status)) {
            return VehicleInfo.StopStatus.DELAYED;
        }
        return null;
    }

}
