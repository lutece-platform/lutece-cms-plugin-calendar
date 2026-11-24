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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.calendar.business.SimpleEvent;

/**
 * The filters and the sort of the event list of a calendar
 */
public class EventListFilterTest
{
    private static final Date TODAY = day( 2026, 11, 13 );

    /**
     * Builds a day at midnight
     * @param nYear the year
     * @param nMonth the month
     * @param nDay the day
     * @return the day
     */
    private static Date day( int nYear, int nMonth, int nDay )
    {
        return Date.from( LocalDate.of( nYear, nMonth, nDay ).atStartOfDay( ZoneId.systemDefault( ) ).toInstant( ) );
    }

    /**
     * Builds an event
     * @param nId its identifier
     * @param strTitle its title
     * @param dateStart its first day
     * @param dateEnd its last day
     * @return the event
     */
    private static SimpleEvent event( int nId, String strTitle, Date dateStart, Date dateEnd )
    {
        SimpleEvent event = new SimpleEvent( );
        event.setId( nId );
        event.setTitle( strTitle );
        event.setDate( dateStart );
        event.setDateEnd( dateEnd );
        return event;
    }

    /**
     * The events of the tests: one past, one in progress, one to come
     * @return the events
     */
    private static List<SimpleEvent> events( )
    {
        return List.of( event( 1, "Zumba au parc", day( 2026, 12, 1 ), day( 2026, 12, 1 ) ), event( 2, "Exposition", day( 2026, 11, 1 ), day( 2026, 11, 30 ) ),
                event( 3, "Écriture", day( 2026, 10, 1 ), day( 2026, 10, 1 ) ) );
    }

    /**
     * Returns the identifiers of events
     * @param listEvents the events
     * @return their identifiers, in order
     */
    private static List<Integer> ids( List<SimpleEvent> listEvents )
    {
        return listEvents.stream( ).map( SimpleEvent::getId ).toList( );
    }

    /**
     * The sorts by date and by title, accents ignored
     */
    @Test
    public void testSort( )
    {
        assertEquals( List.of( 3, 2, 1 ), ids( EventListFilter.apply( events( ), "", "", null, EventListFilter.SORT_DATE_ASC, TODAY, Locale.FRENCH ) ) );
        assertEquals( List.of( 1, 2, 3 ), ids( EventListFilter.apply( events( ), "", "", null, EventListFilter.SORT_DATE_DESC, TODAY, Locale.FRENCH ) ) );
        assertEquals( List.of( 3, 2, 1 ), ids( EventListFilter.apply( events( ), "", "", null, EventListFilter.SORT_TITLE_ASC, TODAY, Locale.FRENCH ) ) );
        assertEquals( List.of( 1, 2, 3 ), ids( EventListFilter.apply( events( ), "", "", null, EventListFilter.SORT_TITLE_DESC, TODAY, Locale.FRENCH ) ) );
    }

    /**
     * The filters by keyword, period and category
     */
    @Test
    public void testFilters( )
    {
        assertEquals( List.of( 3 ), ids( EventListFilter.apply( events( ), "ecri", "", null, EventListFilter.SORT_DATE_ASC, TODAY, Locale.FRENCH ) ) );
        assertEquals( List.of( 2, 1 ), ids( EventListFilter.apply( events( ), "", EventListFilter.PERIOD_UPCOMING, null, EventListFilter.SORT_DATE_ASC, TODAY, Locale.FRENCH ) ) );
        assertEquals( List.of( 3 ), ids( EventListFilter.apply( events( ), "", EventListFilter.PERIOD_PAST, null, EventListFilter.SORT_DATE_ASC, TODAY, Locale.FRENCH ) ) );
        assertEquals( List.of( 1 ), ids( EventListFilter.apply( events( ), "", "", Set.of( 1 ), EventListFilter.SORT_DATE_ASC, TODAY, Locale.FRENCH ) ) );
    }

    /**
     * The former sort links are still understood
     */
    @Test
    public void testLegacySort( )
    {
        assertEquals( EventListFilter.SORT_DATE_DESC, EventListFilter.getSort( null, "date", "false" ) );
        assertEquals( EventListFilter.SORT_TITLE_ASC, EventListFilter.getSort( null, "title", "true" ) );
        assertEquals( EventListFilter.SORT_DATE_ASC, EventListFilter.getSort( "bogus", null, null ) );
        assertEquals( EventListFilter.PERIOD_ALL, EventListFilter.getPeriod( "bogus" ) );
        assertEquals( EventListFilter.PERIOD_ALL, EventListFilter.getPeriod( null ) );
    }
}
