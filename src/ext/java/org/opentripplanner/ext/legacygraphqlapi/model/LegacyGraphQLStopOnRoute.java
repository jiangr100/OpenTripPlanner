package org.opentripplanner.ext.legacygraphqlapi.model;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;

/**
 * Class that contains a {@link Stop} on a {@link Route}.
 */
public class LegacyGraphQLStopOnRoute {

    /**
     * Stop that should be on the route but technically it's possible that it isn't or that it's
     * null.
     */
    private final Stop stop;

    /**
     * Route that should contain the stop but technically it's possible that the stop isn't on the
     * route and it's also possible that the route is null.
     */
    private final Route route;

    public LegacyGraphQLStopOnRoute(Stop stop, Route route) {
        this.stop = stop;
        this.route = route;
    }

    public Stop getStop() {
        return stop;
    }

    public Route getRoute() {
        return route;
    }
}
