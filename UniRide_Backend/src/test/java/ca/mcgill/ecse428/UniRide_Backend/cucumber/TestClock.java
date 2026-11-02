package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Clock that tests can pin to a given date and time ("the current date is ...").
 * Uses the real time until {@link #setTo} is called, and again after {@link #reset}.
 */
public class TestClock extends Clock {

    private final ZoneId zone;
    private volatile Instant fixedInstant;

    public TestClock(ZoneId zone) {
        this.zone = zone;
    }

    /** Makes "now" this local date and time (in the clock's zone, Montréal by default). */
    public void setTo(LocalDateTime localNow) {
        this.fixedInstant = localNow.atZone(zone).toInstant();
    }

    /** Goes back to the real time. */
    public void reset() {
        this.fixedInstant = null;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        Instant fixed = fixedInstant;
        return fixed == null ? Clock.system(newZone) : Clock.fixed(fixed, newZone);
    }

    @Override
    public Instant instant() {
        Instant fixed = fixedInstant;
        return fixed == null ? Instant.now() : fixed;
    }
}
