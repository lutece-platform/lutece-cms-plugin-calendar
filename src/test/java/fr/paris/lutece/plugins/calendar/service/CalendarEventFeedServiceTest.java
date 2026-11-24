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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/**
 * The reading of the parameters of the EventCalendar feed and of the calendar colours
 */
public class CalendarEventFeedServiceTest
{
    /**
     * A date or a date-time gives its day, anything else nothing
     */
    @Test
    public void testParseDate( )
    {
        assertEquals( LocalDate.of( 2026, 10, 26 ), CalendarEventFeedService.parseDate( "2026-10-26T00:00:00" ) );
        assertEquals( LocalDate.of( 2026, 10, 26 ), CalendarEventFeedService.parseDate( "2026-10-26" ) );
        assertNull( CalendarEventFeedService.parseDate( "26/10/2026" ) );
        assertNull( CalendarEventFeedService.parseDate( "2026-13-40" ) );
        assertNull( CalendarEventFeedService.parseDate( null ) );
    }

    /**
     * A time of the plugin ( HH:mm ) is read, an invalid one is ignored
     */
    @Test
    public void testParseTime( )
    {
        assertEquals( LocalTime.of( 9, 5 ), CalendarEventFeedService.parseTime( "9:05" ) );
        assertEquals( LocalTime.of( 23, 59 ), CalendarEventFeedService.parseTime( " 23:59 " ) );
        assertNull( CalendarEventFeedService.parseTime( "24:00" ) );
        assertNull( CalendarEventFeedService.parseTime( "" ) );
        assertNull( CalendarEventFeedService.parseTime( null ) );
    }

    /**
     * The colour of a calendar is its stored name, or the file name of a legacy dot image
     */
    @Test
    public void testEventColor( )
    {
        AgendaResource agenda = new AgendaResource( );
        agenda.setEventImage( "blue" );
        assertEquals( "blue", agenda.getEventColor( ) );
        agenda.setEventImage( "images/local/skin/plugins/calendar/dots/Orange.png" );
        assertEquals( "orange", agenda.getEventColor( ) );
        agenda.setEventImage( "red\" onmouseover=\"x" );
        assertEquals( "redonmouseoverx", agenda.getEventColor( ) );
        agenda.setEventImage( null );
        assertEquals( "", agenda.getEventColor( ) );
    }

    /**
     * The hexadecimal colour of a calendar: a colour from the picker, the colour a former dot name stood for, or the default grey
     */
    @Test
    public void testEventColorCode( )
    {
        AgendaResource agenda = new AgendaResource( );
        agenda.setEventImage( "#1D4ED8" );
        assertEquals( "#1d4ed8", agenda.getEventColorCode( ) );
        agenda.setEventImage( "green" );
        assertEquals( "#2fb344", agenda.getEventColorCode( ) );
        agenda.setEventImage( "images/local/skin/plugins/calendar/dots/Orange.png" );
        assertEquals( "#f76707", agenda.getEventColorCode( ) );
        agenda.setEventImage( "#12345\" onmouseover=\"x" );
        assertEquals( AgendaResource.DEFAULT_COLOR_CODE, agenda.getEventColorCode( ) );
        agenda.setEventImage( null );
        assertEquals( AgendaResource.DEFAULT_COLOR_CODE, agenda.getEventColorCode( ) );
        assertNull( AgendaResource.getValidColorCode( "red" ) );
        assertEquals( "#d63939", AgendaResource.getColorCode( "red" ) );
    }
}
