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
package org.opentripplanner.index.model;

import org.opentripplanner.routing.trippattern.FrequencyEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Service parameters of a frequency-based trip */
public class FrequencyDetail {

    private int startTime;

    private int endTime;

    private int headway;

    private boolean exactTimes;

    private String startTimeFmt;

    private String endTimeFmt;

    public FrequencyDetail(FrequencyEntry entry) {
        this.startTime = entry.startTime;
        this.endTime = entry.endTime;
        this.headway = entry.headway;
        this.exactTimes = entry.exactTimes;
        this.startTimeFmt = formatTime(startTime);
        this.endTimeFmt = formatTime(endTime);
    }

    /**
     * Time at which the first vehicle departs from the first stop of the trip with the specified
     * frequency, specified in seconds after midnight.
     */
    public int getStartTime() {
        return startTime;
    }

    /**
     * Time at which service changes to a different frequency (or ceases) at the first stop in the
     * trip, specified in seconds after midnight.
     */
    public int getEndTime() {
        return endTime;
    }

    /**
     * The time between departures from the same stop (headway) for this trip type, during the time
     * interval specified by start_time and end_time, specified in seconds.
     */
    public int getHeadway() {
        return headway;
    }

    /**
     * True if frequency-based trips should be exactly scheduled based on the specified headway
     * information. If service is not exactly scheduled ("load-and-go"), this value is false.
     */
    public boolean isExactTimes() {
        return exactTimes;
    }

    /**
     * Start time as a formatted string, e.g. "09:00am"
     */
    public String getStartTimeFmt() {
        return startTimeFmt;
    }

    /**
     * End time as a formatted string, e.g. "05:00pm"
     */
    public String getEndTimeFmt() {
        return endTimeFmt;
    }

    private String formatTime(int sec) {
        LocalTime time = LocalTime.ofSecondOfDay((long) sec);
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time);
    }
}
