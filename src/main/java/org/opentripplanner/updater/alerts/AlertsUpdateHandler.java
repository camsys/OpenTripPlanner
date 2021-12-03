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

import java.util.*;

import com.google.transit.realtime.GtfsRealtimeOneBusAway;
import com.google.transit.realtime.GtfsRealtimeServiceStatus;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertAlternateStation;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;

/**
 * This updater only includes GTFS-Realtime Service Alert feeds.
 * @author novalis
 *
 */
public class AlertsUpdateHandler extends AbstractUpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(AlertsUpdateHandler.class);

    private static String DEFAULT_MERCURY_LANG = "en";

    @Override
    public void update(FeedMessage message) {
        alertPatchService.expire(patchIds);
        patchIds.clear();

        for (FeedEntity entity : message.getEntityList()) {
            if (!entity.hasAlert()) {
                continue;
            }
            GtfsRealtime.Alert alert = entity.getAlert();
            String id = entity.getId();
            handleAlert(id, alert);
        }
    }

    private void handleAlert(String id, GtfsRealtime.Alert alert) {
        Alert alertText = new Alert();
        alertText.id = id;
        alertText.alertDescriptionText = deBuffer(alert.getDescriptionText());
        alertText.alertHeaderText = deBuffer(alert.getHeaderText());
        alertText.alertUrl = deBuffer(alert.getUrl());


        ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
        if(alert.getActivePeriodCount() > 0) {
            long bestStartTime = Long.MAX_VALUE;
            long lastEndTime = Long.MIN_VALUE;
            for (TimeRange activePeriod : alert.getActivePeriodList()) {
                final long realStart = activePeriod.hasStart() ? activePeriod.getStart() : 0;
                final long start = activePeriod.hasStart() ? realStart - earlyStart : 0;
                if (realStart > 0 && realStart < bestStartTime) {
                    bestStartTime = realStart;
                }
                final long end = activePeriod.hasEnd() ? activePeriod.getEnd() : Long.MAX_VALUE;
                if (end < Long.MAX_VALUE && end > lastEndTime) {
                    lastEndTime = end;
                }
                periods.add(new TimePeriod(start, end));
            }
            if (bestStartTime != Long.MAX_VALUE) {
                alertText.effectiveStartDate = new Date(bestStartTime * 1000);
            }
            if (lastEndTime != Long.MIN_VALUE) {
                alertText.effectiveEndDate = new Date(lastEndTime * 1000);
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }

        // now look for extensions
        if (alert.hasExtension(GtfsRealtimeServiceStatus.mercuryAlert)) {
            GtfsRealtimeServiceStatus.MercuryAlert mercuryAlert =
                    alert.getExtension(GtfsRealtimeServiceStatus.mercuryAlert);
            if (mercuryAlert.hasCreatedAt())
                alertText.createdDate = toDate(mercuryAlert.getCreatedAt());
            if (mercuryAlert.hasAlertType()) {
                alertText.alertType = toTL(mercuryAlert.getAlertType());
            }
            if (mercuryAlert.hasDisplayBeforeActive()) {
                alertText.displayBeforeActive = toDate(mercuryAlert.getDisplayBeforeActive());
            }
            if (mercuryAlert.hasHumanReadableActivePeriod()) {
                alertText.humanReadableActivePeriod = deBuffer(mercuryAlert.getHumanReadableActivePeriod());
            }
            if (mercuryAlert.hasUpdatedAt()) {
                alertText.updatedAt = toDate(mercuryAlert.getUpdatedAt());
            }
            if (mercuryAlert.getGeneralOrderNumberCount() > 0) {
                for (String order : mercuryAlert.getGeneralOrderNumberList()) {
                    if (alertText.generalOrderNumberList == null) {
                        alertText.generalOrderNumberList = new ArrayList<>();
                    }
                    alertText.generalOrderNumberList.add(order);
                }
            }

            if (mercuryAlert.getServicePlanNumberCount() > 0) {
                for (String plan : mercuryAlert.getServicePlanNumberList()) {
                    if (alertText.servicePlanNumberList == null) {
                        alertText.servicePlanNumberList = new ArrayList<>();
                    }
                    alertText.servicePlanNumberList.add(plan);
                }
            }

            if (mercuryAlert.getStationAlternativeCount() > 0) {
                for (GtfsRealtimeServiceStatus.MercuryStationAlternative stationAlternative : mercuryAlert.getStationAlternativeList()) {
                    if (alertText.stationAlternatives == null) {
                        alertText.stationAlternatives = new ArrayList<>();
                    }
                    AlertAlternateStation aas = new AlertAlternateStation();
                    aas.setNotes(deBuffer(stationAlternative.getNotes()));
                    EntitySelector entity = stationAlternative.getAffectedEntity();
                    if (entity.hasStopId()) {
                        aas.setStopId(entity.getStopId());
                    }
                    if (entity.hasRouteId()) {
                        log.error("found unsupported routeId in stationAlternate " + stationAlternative);
                    }
                    if (entity.hasRouteType()) {
                        log.error("found unsupported routeType in stationAlternate " + stationAlternative);
                    }
                    if (entity.hasTrip()) {
                        log.error("found unsupported trip in stationAlternate " + stationAlternative);
                    }
                    if (entity.hasAgencyId()) {
                        aas.setAgencyId(entity.getAgencyId());
                    }
                    alertText.stationAlternatives.add(aas);
                }
            }
        }

        for (EntitySelector informed : alert.getInformedEntityList()) {
            if (fuzzyTripMatcher != null && informed.hasTrip()) {
                TripDescriptor trip = fuzzyTripMatcher.match(feedId, informed.getTrip());
                informed = informed.toBuilder().setTrip(trip).build();
            }
            String patchId = createId(id, informed);

            String routeId = null;
            if (informed.hasRouteId()) {
                routeId = informed.getRouteId();
            } else if (informed.hasTrip() && informed.getTrip().hasRouteId())  {
                routeId = informed.getTrip().getRouteId();
            }

            int direction;
            if (informed.hasTrip() && informed.getTrip().hasDirectionId()) {
                direction = informed.getTrip().getDirectionId();
            } else {
                direction = -1;
            }

            // TODO: The other elements of a TripDescriptor are ignored...
            String tripId = null;
            if (informed.hasTrip() && informed.getTrip().hasTripId()) {
                tripId = informed.getTrip().getTripId();
            }
            String stopId = null;
            if (informed.hasStopId()) {
                stopId = informed.getStopId();
            }

            String agencyId = informed.getAgencyId();
            if (informed.hasAgencyId()) {
                agencyId = informed.getAgencyId().intern();
            }

            String elevatorId = null;
            if (informed.hasExtension(GtfsRealtimeOneBusAway.obaEntitySelector)) {
                GtfsRealtimeOneBusAway.OneBusAwayEntitySelector entitySelector =
                        informed.getExtension(GtfsRealtimeOneBusAway.obaEntitySelector);
                if (entitySelector.hasElevatorId()) {
                    elevatorId = entitySelector.getElevatorId();
                }
            }

            AlertPatch patch = new AlertPatch();
            patch.setFeedId(feedId);
            if (routeId != null) {
                patch.setRoute(new AgencyAndId(feedId, routeId));
                // Makes no sense to set direction if we don't have a route
                if (direction != -1) {
                    patch.setDirectionId(direction);
                }
            }
            if (tripId != null) {
                patch.setTrip(new AgencyAndId(feedId, tripId));
            }
            if (stopId != null) {
                patch.setStop(new AgencyAndId(feedId, stopId));
            }
            if (agencyId != null && routeId == null && tripId == null && stopId == null) {
                patch.setAgencyId(agencyId);
            }
            if (elevatorId != null) {
                patch.setElevatorId(elevatorId);
            }
            patch.setTimePeriods(periods);
            patch.setAlert(alertText);
            patch.setId(patchId);
            patchIds.add(patchId);

            alertPatchService.apply(patch);
        }
    }


    private String createId(String id, EntitySelector informed) {
        return id + " "
            + (informed.hasAgencyId  () ? informed.getAgencyId  () : " null ") + " "
            + (informed.hasRouteId   () ? informed.getRouteId   () : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasRouteId() ?
                informed.getTrip().getRouteId() : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasDirectionId() ?
                informed.getTrip().getDirectionId() : " null ") + " "
            + (informed.hasRouteType () ? informed.getRouteType () : " null ") + " "
            + (informed.hasStopId    () ? informed.getStopId    () : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasTripId() ?
                informed.getTrip().getTripId() : " null ");
    }

    /**
     * convert a protobuf TranslatedString to a OTP TranslatedString
     *
     * @return A TranslatedString containing the same information as the input
     */
    private I18NString deBuffer(GtfsRealtime.TranslatedString input) {
        Map<String, String> translations = new HashMap<>();
        for (GtfsRealtime.TranslatedString.Translation translation : input.getTranslationList()) {
            String language = translation.getLanguage();
            String string = translation.getText();
            translations.put(language, string);
        }
        return translations.isEmpty() ? null : TranslatedString.getI18NString(translations);
    }

    private I18NString toTL(String text) {
        Map<String, String> translations = new HashMap<>();
        translations.put(DEFAULT_MERCURY_LANG, text);
        return TranslatedString.getI18NString(translations);
    }

    private Date toDate(long time) {
        return new Date(time * 1000);
    }

}
