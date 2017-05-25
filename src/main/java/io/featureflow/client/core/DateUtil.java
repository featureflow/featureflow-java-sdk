package io.featureflow.client.core;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by oliver on 25/5/17.
 */
public class DateUtil {
    protected static String toIso(DateTime date) {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String str = fmt.print(date);
        return str;
    }
}
