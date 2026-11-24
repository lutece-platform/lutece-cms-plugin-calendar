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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.calendar.business.button.CalendarButtonConfig;

/**
 * The links of the "add to calendar" button and the cleaning of its options
 */
public class CalendarButtonServiceTest
{
    /**
     * Unknown options and values are replaced by the defaults, the order of the options is the reference one
     */
    @Test
    public void testSanitize( )
    {
        CalendarButtonConfig config = new CalendarButtonConfig( );
        config.setOptions( new ArrayList<>( List.of( "ical", "unknown", "google" ) ) );
        config.setColor( "fuchsia" );
        config.setDisplayMode( "carousel" );
        CalendarButtonService.sanitize( config );

        assertEquals( List.of( "google", "ical" ), config.getOptions( ) );
        assertEquals( new CalendarButtonConfig( ).getColor( ), config.getColor( ) );
        assertEquals( CalendarButtonConfig.DISPLAY_DROPDOWN, config.getDisplayMode( ) );
    }

    /**
     * Each option gives its link: Google and Outlook with the title encoded, the iCal options the file of the occurrence
     */
    @Test
    public void testBuildLinks( )
    {
        CalendarButtonConfig config = new CalendarButtonConfig( );
        config.setOptions( new ArrayList<>( List.of( "google", "outlookcom", "ical" ) ) );

        Map<String, String> links = CalendarButtonService.buildLinks( config, "Fête de quartier", LocalDateTime.of( 2026, 11, 12, 9, 30 ),
                LocalDateTime.of( 2026, 11, 12, 11, 0 ), "Place de l'Hôtel de Ville", "Ouvert à tous", "https://example.org/event.ics" );

        assertEquals( List.of( "google", "outlookcom", "ical" ), new ArrayList<>( links.keySet( ) ) );
        assertTrue( links.get( "google" ).startsWith( "https://calendar.google.com/calendar/render?action=TEMPLATE&text=F%C3%AAte%20de%20quartier&dates=" ) );
        assertTrue( links.get( "google" ).contains( "&location=Place%20de%20l%27H%C3%B4tel%20de%20Ville" ) );
        assertTrue( links.get( "outlookcom" ).contains( "&subject=F%C3%AAte%20de%20quartier&startdt=" ) );
        assertEquals( "https://example.org/event.ics", links.get( "ical" ) );
    }
}
