package ai.sovereignrag.accounting;/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2024 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.jdom2.Element;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Assorted helper methods
 *
 * @author <a href="mailto:apr@jpos.org">Alejandro Revilla</a>
 */
public class Util {
    private static final DateTimeFormatter df_yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter df_yyyyMMddhhmmss = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    /**
     * @param s date string in YYYYMMdd format
     * @return an Instant object
     */
    public static Instant parseDate(String s) {
        if (s == null)
            return null;
        LocalDate date = LocalDate.parse(s, df_yyyyMMdd);
        return date.atStartOfDay(SYSTEM_ZONE).toInstant();
    }

    /**
     * @param s date string in YYYYMMddhhmmss format
     * @return an Instant object
     */
    public static Instant parseDateTime(String s) {
        if (s == null)
            return null;
        LocalDateTime dateTime = LocalDateTime.parse(s, df_yyyyMMddhhmmss);
        return dateTime.atZone(SYSTEM_ZONE).toInstant();
    }

    /**
     * @param instant an Instant object
     * @return date string in YYYYMMDD format
     */
    public static String dateToString(Instant instant) {
        if (instant == null)
            return null;
        return df_yyyyMMdd.format(instant.atZone(SYSTEM_ZONE));
    }

    /**
     * @param instant an Instant object
     * @return date string in YYYYMMDDHHMMSS format
     */
    public static String dateTimeToString(Instant instant) {
        if (instant == null)
            return null;
        return df_yyyyMMddhhmmss.format(instant.atZone(SYSTEM_ZONE));
    }

    /**
     * Sets date="YYYYMMDD" attribute to an XML Element
     * @param elem JDOM Element
     * @param attributeName attribute name
     * @param instant Instant object
     */
    public static void setDateAttribute(Element elem, String attributeName, Instant instant) {
        if (instant != null) {
            elem.setAttribute(attributeName, dateToString(instant));
        }
    }

    /**
     * Sets date="YYYYMMDDHHMMSS" attribute to an XML Element
     * @param elem JDOM Element
     * @param attributeName attribute name
     * @param instant Instant object
     */
    public static void setDateTimeAttribute(Element elem, String attributeName, Instant instant) {
        if (instant != null) {
            elem.setAttribute(attributeName, dateTimeToString(instant));
        }
    }

    /**
     * Force the 'time' portion of a date up to 23:59:59.999
     * @param instant Instant
     * @return converted Instant
     */
    public static Instant ceil(Instant instant) {
        if (instant == null) {
            // Default to far future date
            return LocalDate.of(2099, 12, 31)
                .atTime(23, 59, 59, 999_000_000)
                .atZone(SYSTEM_ZONE)
                .toInstant();
        }
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        return zdt.toLocalDate()
            .atTime(23, 59, 59, 999_000_000)
            .atZone(SYSTEM_ZONE)
            .toInstant();
    }

    public static Instant ceil(Instant instant, int precision) {
        if (instant == null) {
            // Default to far future date
            return LocalDate.of(2099, 12, 31)
                .atTime(23, 59, 59, 999_000_000)
                .atZone(SYSTEM_ZONE)
                .toInstant();
        }
        
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        
        switch (precision) {
            case java.util.Calendar.DATE:
                return zdt.toLocalDate()
                    .atTime(23, 59, 59, 999_000_000)
                    .atZone(SYSTEM_ZONE)
                    .toInstant();
            case java.util.Calendar.HOUR:
            case java.util.Calendar.HOUR_OF_DAY:
                return zdt.withMinute(59)
                    .withSecond(59)
                    .withNano(999_000_000)
                    .toInstant();
            case java.util.Calendar.MINUTE:
                return zdt.withSecond(59)
                    .withNano(999_000_000)
                    .toInstant();
            case java.util.Calendar.SECOND:
            case java.util.Calendar.MILLISECOND:
                return zdt.withNano(999_000_000)
                    .toInstant();
            default:
                return instant;
        }
    }

    /**
     * Force the 'time' portion of a date down to 00:00:00.000
     * @param instant Instant (if null, we default to 01/01/1970)
     * @return converted Instant
     */
    public static Instant floor(Instant instant) {
        if (instant == null) {
            // Default to epoch
            return LocalDate.of(1970, 1, 1)
                .atStartOfDay(SYSTEM_ZONE)
                .toInstant();
        }
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        return zdt.toLocalDate()
            .atStartOfDay(SYSTEM_ZONE)
            .toInstant();
    }

    public static Instant floor(Instant instant, int precision) {
        if (instant == null) {
            // Default to epoch
            return LocalDate.of(1970, 1, 1)
                .atStartOfDay(SYSTEM_ZONE)
                .toInstant();
        }
        
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        
        switch (precision) {
            case java.util.Calendar.DATE:
                return zdt.toLocalDate()
                    .atStartOfDay(SYSTEM_ZONE)
                    .toInstant();
            case java.util.Calendar.HOUR:
            case java.util.Calendar.HOUR_OF_DAY:
                return zdt.withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant();
            case java.util.Calendar.MINUTE:
                return zdt.withSecond(0)
                    .withNano(0)
                    .toInstant();
            case java.util.Calendar.SECOND:
                return zdt.withNano(0)
                    .toInstant();
            default:
                return instant;
        }
    }

    /**
     * Increment the given precision unit, and set the next lower unit to ZERO.
     *
     * For example, if the Instant represents "Mon Jan 21 20:34:46 UYT 2019",
     * then, Util.nextFloor(instant, Calendar.HOUR_OF_DAY) will advance to the beginning
     * of next hour, i.e. "Mon Jan 21 21:00:00 UYT 2019".
     *
     * @param instant Instant
     * @param precision is one of the Calendar constants: DATE, HOUR, HOUR_OF_DAY, MINUTE, SECOND
     * @return converted Instant
     * @throws NullPointerException if instant is null
     */
    public static Instant nextFloor(Instant instant, int precision) {
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        
        switch (precision) {
            case java.util.Calendar.DATE:
                return zdt.plusDays(1)
                    .toLocalDate()
                    .atStartOfDay(SYSTEM_ZONE)
                    .toInstant();
            case java.util.Calendar.HOUR:
            case java.util.Calendar.HOUR_OF_DAY:
                return zdt.plusHours(1)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant();
            case java.util.Calendar.MINUTE:
                return zdt.plusMinutes(1)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant();
            case java.util.Calendar.SECOND:
                return zdt.plusSeconds(1)
                    .withNano(0)
                    .toInstant();
            default:
                return instant;
        }
    }

    /**
     * Force date to tomorrow at 00:00:00.000
     * @param instant Instant
     * @return converted Instant
     */
    public static Instant tomorrow(Instant instant) {
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        return zdt.plusDays(1)
            .toLocalDate()
            .atStartOfDay(SYSTEM_ZONE)
            .toInstant();
    }
}