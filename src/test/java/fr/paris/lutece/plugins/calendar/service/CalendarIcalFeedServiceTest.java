/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.calendar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;

/**
 * The iCalendar text written by {@link CalendarIcalFeedService}
 */
public class CalendarIcalFeedServiceTest
{
    /**
     * Backslash, semicolon, comma and line ends are escaped
     */
    @Test
    public void testEscapeText( )
    {
        assertEquals( "a\\\\b\\;c\\,d\\ne", CalendarIcalFeedService.escapeText( "a\\b;c,d\ne" ) );
        assertEquals( "", CalendarIcalFeedService.escapeText( null ) );
    }

    /**
     * A long line is folded every 75 octets, without cutting an accented character
     */
    @Test
    public void testFolding( )
    {
        StringBuilder sb = new StringBuilder( );
        CalendarIcalFeedService.appendLine( sb, "SUMMARY:" + "é".repeat( 60 ) );

        String [ ] lines = sb.toString( ).split( "\r\n" );
        assertTrue( lines.length > 1 );

        for ( String strLine : lines )
        {
            assertTrue( strLine.getBytes( StandardCharsets.UTF_8 ).length <= 75 );
        }

        assertTrue( lines [1].startsWith( " " ) );
        assertEquals( "SUMMARY:" + "é".repeat( 60 ), sb.toString( ).replace( "\r\n ", "" ).trim( ) );
    }

    /**
     * An occurrence with times gives UTC times, one without gives a whole day
     */
    @Test
    public void testBuildCalendar( )
    {
        AgendaResource agenda = new AgendaResource( );
        agenda.setName( "Agenda, test" );

        OccurrenceEvent timed = occurrence( 1, LocalDate.of( 2026, 11, 12 ), "09:30", "11:00", "Réunion; publique" );
        OccurrenceEvent allDay = occurrence( 2, LocalDate.of( 2026, 11, 13 ), null, null, "Fête" );

        String strIcs = new CalendarIcalFeedService( ).buildCalendar( agenda, List.of( timed, allDay ), "https://example.org/site/", "example.org",
                LocalDateTime.of( 2026, 11, 1, 8, 0 ) );

        assertTrue( strIcs.startsWith( "BEGIN:VCALENDAR\r\n" ) );
        assertTrue( strIcs.endsWith( "END:VCALENDAR\r\n" ) );
        assertTrue( strIcs.contains( "X-WR-CALNAME:Agenda\\, test\r\n" ) );
        assertTrue( strIcs.contains( "UID:calendar-occurrence-1@example.org\r\n" ) );
        assertTrue( strIcs.contains( "SUMMARY:Réunion\\; publique\r\n" ) );
        assertTrue( strIcs.contains( "DTSTART;VALUE=DATE:20261113\r\nDTEND;VALUE=DATE:20261114\r\n" ) );
        assertTrue( strIcs.contains( "DTSTART:" + utc( LocalDateTime.of( 2026, 11, 12, 9, 30 ) ) ) );
        assertTrue( strIcs.contains( "DTEND:" + utc( LocalDateTime.of( 2026, 11, 12, 11, 0 ) ) ) );
        assertTrue( strIcs.contains( "STATUS:CONFIRMED\r\n" ) );
        assertFalse( strIcs.contains( "\n\n" ) );
    }

    /**
     * Builds an occurrence
     *
     * @param nId
     *            its id
     * @param day
     *            its day
     * @param strStart
     *            its start time
     * @param strEnd
     *            its end time
     * @param strTitle
     *            its title
     * @return the occurrence
     */
    private static OccurrenceEvent occurrence( int nId, LocalDate day, String strStart, String strEnd, String strTitle )
    {
        OccurrenceEvent occurrence = new OccurrenceEvent( );
        occurrence.setId( nId );
        occurrence.setEventId( 10 );
        occurrence.setIdCalendar( 3 );
        occurrence.setDate( Date.from( day.atStartOfDay( ZoneId.systemDefault( ) ).toInstant( ) ) );
        occurrence.setDateTimeStart( strStart );
        occurrence.setDateTimeEnd( strEnd );
        occurrence.setTitle( strTitle );
        occurrence.setStatus( "confirmed" );

        return occurrence;
    }

    /**
     * Gives the iCalendar UTC form of a local time of the server
     *
     * @param dateTime
     *            the local time
     * @return the UTC form
     */
    private static String utc( LocalDateTime dateTime )
    {
        return java.time.format.DateTimeFormatter.ofPattern( "yyyyMMdd'T'HHmmss'Z'" )
                .format( dateTime.atZone( ZoneId.systemDefault( ) ).withZoneSameInstant( java.time.ZoneOffset.UTC ) );
    }
}
