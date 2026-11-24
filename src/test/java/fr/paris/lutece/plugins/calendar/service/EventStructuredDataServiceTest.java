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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.business.details.EventDetails;

/**
 * The schema.org description of an event
 */
public class EventStructuredDataServiceTest
{
    /**
     * The statuses of the plugin give the schema.org ones
     */
    @Test
    public void testEventStatus( )
    {
        assertEquals( "EventCancelled", EventStructuredDataService.getEventStatus( "cancelled" ) );
        assertEquals( "EventPostponed", EventStructuredDataService.getEventStatus( "postponed" ) );
        assertEquals( "EventMovedOnline", EventStructuredDataService.getEventStatus( "movedOnline" ) );
        assertEquals( "EventScheduled", EventStructuredDataService.getEventStatus( "confirmed" ) );
        assertEquals( "EventScheduled", EventStructuredDataService.getEventStatus( null ) );
    }

    /**
     * A free hybrid event gives its place, its online location, a free offer and its organizer; a closing script tag
     * in a value is escaped
     */
    @Test
    public void testJsonLd( )
    {
        SimpleEvent event = new SimpleEvent( );
        event.setTitle( "Atelier </script> cuisine" );
        event.setDate( Date.from( LocalDate.of( 2026, 12, 10 ).atStartOfDay( ZoneId.systemDefault( ) ).toInstant( ) ) );
        event.setDateTimeStart( "18:30" );
        event.setDateTimeEnd( "20:00" );
        event.setLocation( "Mairie du 4e" );
        event.setLocationAddress( "2 place Baudoyer" );
        event.setLocationZip( "75004" );
        event.setLocationTown( "Paris" );
        event.setStatus( "confirmed" );

        EventDetails details = new EventDetails( );
        details.setPriceType( EventDetails.PRICE_FREE );
        details.setAttendanceMode( EventDetails.ATTENDANCE_MIXED );
        details.setOnlineUrl( "https://example.org/live" );
        details.setOrganizerName( "Ville de Paris" );
        details.setLatitude( 48.856 );
        details.setLongitude( 2.354 );

        String strJson = new EventStructuredDataService( ).buildJsonLd( event, details, "https://example.org/event", null );

        assertFalse( strJson.contains( "</script>" ) );
        assertTrue( strJson.contains( "\"@type\":\"Event\"" ) );
        assertTrue( strJson.contains( "\"startDate\":\"2026-12-10T18:30" ) );
        assertTrue( strJson.contains( "\"eventStatus\":\"https://schema.org/EventScheduled\"" ) );
        assertTrue( strJson.contains( "\"eventAttendanceMode\":\"https://schema.org/MixedEventAttendanceMode\"" ) );
        assertTrue( strJson.contains( "\"@type\":\"VirtualLocation\"" ) );
        assertTrue( strJson.contains( "\"postalCode\":\"75004\"" ) );
        assertTrue( strJson.contains( "\"price\":\"0\"" ) );
        assertTrue( strJson.contains( "\"name\":\"Ville de Paris\"" ) );
    }
}
