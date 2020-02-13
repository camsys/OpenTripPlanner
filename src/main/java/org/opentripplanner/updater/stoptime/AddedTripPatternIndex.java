/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.stoptime;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Index added trip patterns for a TimetableSnapshot by trip ID and service date, and by stop.
 * This superceded a Map<TripIdAndServiceDate, TripPattern> that was in TimetableSnapshot
 */
public class AddedTripPatternIndex {

    /**
     * <p>
     * Map containing the last <b>added</b> trip pattern given a trip id (without agency) and a
     * service date as a result of a call to
     * {@link org.opentripplanner.routing.edgetype.TimetableSnapshot#update(String feedId, TripPattern, TripTimes, ServiceDate)}
     * with trip times of a trip that didn't exist yet in the trip pattern.
     * </p>
     * <p>
     * This is a HashMap and not a Map so the clone function is available.
     * </p>
     */
    private HashMap<TripIdAndServiceDate, TripPattern> lastAddedTripPattern = new HashMap<>();

    private Multimap<Stop, TripPattern> patternByStop = ArrayListMultimap.create();

    /**
     * Get the last <b>added</b> trip pattern given a trip id (without agency) and a service date as
     * a result of a call to
     * {@link org.opentripplanner.routing.edgetype.TimetableSnapshot#update(String feedId, TripPattern, TripTimes, ServiceDate)}
     * with trip times of a trip that didn't exist yet in the trip pattern.
     *
     * @param feedId feed id the trip id belongs to
     * @param tripId trip id (without agency)
     * @param serviceDate service date
     * @return last added trip pattern; null if trip never was added to a trip pattern
     */
    public TripPattern getLastAddedTripPattern(String feedId, String tripId, ServiceDate serviceDate) {
        TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(feedId, tripId, serviceDate);
        TripPattern pattern = lastAddedTripPattern.get(tripIdAndServiceDate);
        return pattern;
    }

    public void addTripPattern(String feedId, String tripId, ServiceDate serviceDate, TripPattern pattern) {
        TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(feedId, tripId, serviceDate);
        lastAddedTripPattern.put(tripIdAndServiceDate, pattern);
        for (Stop stop : pattern.getStops()) {
            patternByStop.put(stop, pattern);
        }
    }

    /**
     * Clear all last added trip patterns matching the provided feed id.
     *
     * @param feedId feed id to clear out
     * @return true if the lastAddedTripPattern changed as a result of the call
     */
    public boolean clearTripPattern(String feedId) {
        boolean modified = false;
        Set<TripIdAndServiceDate> oldKeys = new HashSet<>(lastAddedTripPattern.keySet());
        for (TripIdAndServiceDate key : oldKeys) {
            if (key.getFeedId().equals(feedId)) {
                modified = true;
                TripPattern pattern = lastAddedTripPattern.get(key);
                for (Stop stop : pattern.getStops()) {
                    patternByStop.remove(stop, pattern);
                }
                lastAddedTripPattern.remove(key);
            }
        }
        return modified;
    }

    /**
     * Removes all added TripPatterns which are valid for a ServiceDate on-or-before the one supplied.
     */
    public boolean purgeExpiredData(ServiceDate serviceDate) {
        boolean modified = false;
        for (Iterator<Map.Entry<TripIdAndServiceDate, TripPattern>> iterator = lastAddedTripPattern
                .entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<TripIdAndServiceDate, TripPattern> entry = iterator.next();
            TripIdAndServiceDate tripIdAndServiceDate = entry.getKey();
            if (serviceDate.compareTo(tripIdAndServiceDate.getServiceDate()) >= 0) {
                iterator.remove();
                TripPattern pattern = entry.getValue();
                for (Stop stop : pattern.getStops()) {
                    patternByStop.remove(stop, pattern);
                }
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Get all added TripPatterns for stop.
     */
    public Collection<TripPattern> getTripPatternsForStop(Stop stop) {
        return patternByStop.get(stop);
    }

    public AddedTripPatternIndex copy()  {
        AddedTripPatternIndex ret = new AddedTripPatternIndex();
        ret.lastAddedTripPattern = (HashMap<TripIdAndServiceDate, TripPattern>)
                this.lastAddedTripPattern.clone();
        for (TripPattern pattern : lastAddedTripPattern.values()) {
            for (Stop stop : pattern.getStops()) {
                ret.patternByStop.put(stop, pattern);
            }
        }
        return ret;
    }
}

/**
 * Class to use as key in HashMap containing feed id, trip id and service date
 */
class TripIdAndServiceDate {
    private final String feedId;
    private final String tripId;
    private final ServiceDate serviceDate;

    public TripIdAndServiceDate(final String feedId, final String tripId, final ServiceDate serviceDate) {
        this.feedId = feedId;
        this.tripId = tripId;
        this.serviceDate = serviceDate;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getTripId() {
        return tripId;
    }

    public ServiceDate getServiceDate() {
        return serviceDate;
    }


    @Override
    public int hashCode() {
        int result = Objects.hash(tripId, serviceDate, feedId);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TripIdAndServiceDate other = (TripIdAndServiceDate) obj;
        boolean result = Objects.equals(this.tripId, other.tripId) &&
                Objects.equals(this.serviceDate, other.serviceDate) &&
                Objects.equals(this.feedId, other.feedId);
        return result;
    }
}