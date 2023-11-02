package org.opentripplanner.routing.fares;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.fares.FareService;

public interface FareServiceFactory {

    FareService makeFareService();

    void processGtfs(OtpTransitService transitService);

    void configure(JsonNode config);
}
