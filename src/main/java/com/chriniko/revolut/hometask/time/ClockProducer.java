package com.chriniko.revolut.hometask.time;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.time.Clock;

@ApplicationScoped
public class ClockProducer {

    @Produces
    @UtcZone
    @ApplicationScoped
    public Clock clock() {
        return Clock.systemUTC();
    }

}
