package io.featureflow.client.model;

import com.google.gson.JsonPrimitive;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Date handling for the before/after operators.
 *
 * The shared SDK contract (testbed CONTRACT.md, "Operators") states that a date-only value such
 * as 2026-07-03 denotes UTC midnight - 2026-07-03T00:00:00Z - on every SDK and every host.
 * These tests deliberately run under a non-UTC default timezone so that any regression back to
 * local-midnight parsing fails here rather than in a customer's fleet.
 */
public class OperatorDateTest {

    private static final DateTimeZone SYDNEY = DateTimeZone.forID("Australia/Sydney");

    private DateTimeZone originalDefault;

    @BeforeEach
    public void setNonUtcDefaultZone() {
        originalDefault = DateTimeZone.getDefault();
        DateTimeZone.setDefault(SYDNEY);
    }

    @AfterEach
    public void restoreDefaultZone() {
        DateTimeZone.setDefault(originalDefault);
    }

    @Test
    public void dateOnlyResolvesToUtcMidnight() {
        DateTime parsed = Operator.getDateTime(new JsonPrimitive("2026-07-03"));
        assertNotNull(parsed);
        assertEquals(new DateTime("2026-07-03T00:00:00Z").getMillis(), parsed.getMillis(),
                "a date-only value must denote UTC midnight, not local midnight");
    }

    @Test
    public void dateOnlyOnEitherSideOfAfterIsUtcMidnight() {
        // 2026-07-03T00:00:01Z is one second after UTC midnight on the 3rd.
        assertTrue(Operator.after.evaluate(new JsonPrimitive("2026-07-03T00:00:01Z"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        // 2026-07-02T23:59:59Z is one second before it.
        assertFalse(Operator.after.evaluate(new JsonPrimitive("2026-07-02T23:59:59Z"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
    }

    @Test
    public void dateOnlyAsContextValueIsUtcMidnight() {
        // Under Australia/Sydney (UTC+10) local midnight on the 3rd is 2026-07-02T14:00:00Z, so a
        // local reading would place the context value before this target and flip both assertions.
        assertTrue(Operator.after.evaluate(new JsonPrimitive("2026-07-03"),
                Arrays.asList(new JsonPrimitive("2026-07-02T23:59:59Z"))));
        assertFalse(Operator.before.evaluate(new JsonPrimitive("2026-07-03"),
                Arrays.asList(new JsonPrimitive("2026-07-02T23:59:59Z"))));
    }

    @Test
    public void dateOnlyBeforeComparesAtUtcMidnight() {
        assertTrue(Operator.before.evaluate(new JsonPrimitive("2026-07-02T23:59:59Z"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertFalse(Operator.before.evaluate(new JsonPrimitive("2026-07-03T00:00:01Z"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
    }

    @Test
    public void bothSidesDateOnlyCompareAtUtcMidnight() {
        assertTrue(Operator.before.evaluate(new JsonPrimitive("2026-07-02"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertTrue(Operator.after.evaluate(new JsonPrimitive("2026-07-04"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertFalse(Operator.before.evaluate(new JsonPrimitive("2026-07-03"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertFalse(Operator.after.evaluate(new JsonPrimitive("2026-07-03"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
    }

    @Test
    public void explicitZuluTimestampIsNotShifted() {
        DateTime parsed = Operator.getDateTime(new JsonPrimitive("2026-07-03T10:00:00Z"));
        assertNotNull(parsed);
        assertEquals(1783072800000L, parsed.getMillis());
    }

    @Test
    public void explicitNumericOffsetIsPreserved() {
        // 2026-07-03T10:00:00+04:00 is 2026-07-03T06:00:00Z - it already names its instant and
        // must not be re-anchored to UTC.
        DateTime parsed = Operator.getDateTime(new JsonPrimitive("2026-07-03T10:00:00+04:00"));
        assertNotNull(parsed);
        assertEquals(new DateTime("2026-07-03T06:00:00Z").getMillis(), parsed.getMillis());

        DateTime negative = Operator.getDateTime(new JsonPrimitive("2026-07-03T10:00:00-05:00"));
        assertNotNull(negative);
        assertEquals(new DateTime("2026-07-03T15:00:00Z").getMillis(), negative.getMillis());
    }

    @Test
    public void offsetAndZuluTimestampsForTheSameInstantAreEqual() {
        assertFalse(Operator.after.evaluate(new JsonPrimitive("2026-07-03T10:00:00+04:00"),
                Arrays.asList(new JsonPrimitive("2026-07-03T06:00:00Z"))));
        assertFalse(Operator.before.evaluate(new JsonPrimitive("2026-07-03T10:00:00+04:00"),
                Arrays.asList(new JsonPrimitive("2026-07-03T06:00:00Z"))));
    }

    @Test
    public void fractionalSecondsAreSupported() {
        DateTime parsed = Operator.getDateTime(new JsonPrimitive("2026-07-03T10:00:00.250Z"));
        assertNotNull(parsed);
        assertEquals(new DateTime("2026-07-03T10:00:00Z").getMillis() + 250, parsed.getMillis());
    }

    @Test
    public void timestampWithoutZoneIsReadAsUtc() {
        // No offset and no Z: the value does not name an instant, so the contract's UTC default
        // applies here too, keeping a fleet consistent.
        DateTime parsed = Operator.getDateTime(new JsonPrimitive("2026-07-03T10:00:00"));
        assertNotNull(parsed);
        assertEquals(new DateTime("2026-07-03T10:00:00Z").getMillis(), parsed.getMillis());
    }

    @Test
    public void epochMillisAreStillSupported() {
        DateTime parsed = Operator.getDateTime(new JsonPrimitive(1783072800000L));
        assertNotNull(parsed);
        assertEquals(1783072800000L, parsed.getMillis());
    }

    @Test
    public void unparseableValueReturnsNoMatchRatherThanThrowing() {
        assertNull(Operator.getDateTime(new JsonPrimitive("not-a-date")));
        assertFalse(Operator.after.evaluate(new JsonPrimitive("not-a-date"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertFalse(Operator.before.evaluate(new JsonPrimitive("not-a-date"),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertFalse(Operator.after.evaluate(new JsonPrimitive("2026-07-03"),
                Arrays.asList(new JsonPrimitive("not-a-date"))));
        assertFalse(Operator.before.evaluate(new JsonPrimitive("2026-07-03"),
                Arrays.asList(new JsonPrimitive("not-a-date"))));
        assertFalse(Operator.after.evaluate(new JsonPrimitive(""),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
        assertFalse(Operator.after.evaluate(new JsonPrimitive(true),
                Arrays.asList(new JsonPrimitive("2026-07-03"))));
    }
}
