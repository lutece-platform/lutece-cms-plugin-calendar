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

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.SimpleEvent;

/**
 * The filters and the sort of the event list of a calendar in the back office. It works on a copy of the list: the list of
 * the events of a calendar is cached and shared.
 */
public final class EventListFilter
{
    /** All the events, whatever their dates */
    public static final String PERIOD_ALL = "";
    /** The events that are not over */
    public static final String PERIOD_UPCOMING = "upcoming";
    /** The events that are over */
    public static final String PERIOD_PAST = "past";
    /** First day first */
    public static final String SORT_DATE_ASC = "date_asc";
    /** Last day first */
    public static final String SORT_DATE_DESC = "date_desc";
    /** Title from A to Z */
    public static final String SORT_TITLE_ASC = "title_asc";
    /** Title from Z to A */
    public static final String SORT_TITLE_DESC = "title_desc";
    /** Last created first */
    public static final String SORT_CREATION_DESC = "creation_desc";
    /** Most occurrences first */
    public static final String SORT_OCCURRENCE_DESC = "occurrence_desc";
    /** The sorts offered, in the order of the list */
    public static final List<String> SORTS = List.of( SORT_DATE_ASC, SORT_DATE_DESC, SORT_TITLE_ASC, SORT_TITLE_DESC, SORT_CREATION_DESC,
            SORT_OCCURRENCE_DESC );
    /** The periods offered, in the order of the list */
    public static final List<String> PERIODS = List.of( PERIOD_ALL, PERIOD_UPCOMING, PERIOD_PAST );
    private static final String LEGACY_ATTRIBUTE_TITLE = "title";
    private static final String LEGACY_ATTRIBUTE_OCCURRENCE = "occurrence";
    private static final String LEGACY_ATTRIBUTE_CREATION = "dateCreation";

    /**
     * Private constructor
     */
    private EventListFilter( )
    {
    }

    /**
     * Filters and sorts events
     * @param listEvents the events of the calendar, left unchanged
     * @param strKeyword a text the title or the location must contain, accents and case ignored, or blank
     * @param strPeriod one of the periods PERIOD_*
     * @param setEventIds the identifiers of the events kept, or null to keep them all
     * @param strSort one of the sorts SORT_*
     * @param today the current day at midnight
     * @param locale the locale of the alphabetical sort
     * @return a new list of the kept events, sorted
     */
    public static List<SimpleEvent> apply( List<SimpleEvent> listEvents, String strKeyword, String strPeriod, Set<Integer> setEventIds,
            String strSort, Date today, Locale locale )
    {
        String strSearched = normalize( strKeyword );
        Predicate<SimpleEvent> filter = event -> ( setEventIds == null ) || setEventIds.contains( event.getId( ) );
        filter = filter.and( event -> strSearched.isEmpty( ) || normalize( event.getTitle( ) ).contains( strSearched )
                || normalize( event.getLocation( ) ).contains( strSearched ) );
        filter = filter.and( event -> isInPeriod( event, strPeriod, today ) );

        return listEvents.stream( ).filter( filter ).sorted( getComparator( strSort, locale ) ).toList( );
    }

    /**
     * Returns the sort of the list: the one asked, or the sort of the former sort links (sorted_attribute_name and
     * asc_sort), or the first day first
     * @param strSort the sort asked
     * @param strLegacyAttribute the attribute of a former sort link, or null
     * @param strLegacyAsc the order of a former sort link, or null
     * @return one of the sorts SORT_*
     */
    public static String getSort( String strSort, String strLegacyAttribute, String strLegacyAsc )
    {
        if ( ( strSort != null ) && SORTS.contains( strSort ) )
        {
            return strSort;
        }

        boolean bAsc = !Boolean.FALSE.toString( ).equals( strLegacyAsc );

        if ( LEGACY_ATTRIBUTE_TITLE.equals( strLegacyAttribute ) )
        {
            return bAsc ? SORT_TITLE_ASC : SORT_TITLE_DESC;
        }

        if ( LEGACY_ATTRIBUTE_OCCURRENCE.equals( strLegacyAttribute ) )
        {
            return SORT_OCCURRENCE_DESC;
        }

        if ( LEGACY_ATTRIBUTE_CREATION.equals( strLegacyAttribute ) )
        {
            return SORT_CREATION_DESC;
        }

        return bAsc ? SORT_DATE_ASC : SORT_DATE_DESC;
    }

    /**
     * Returns the period of the list
     * @param strPeriod the period asked
     * @return one of the periods PERIOD_*, all the events when the period is unknown
     */
    public static String getPeriod( String strPeriod )
    {
        return ( ( strPeriod != null ) && PERIODS.contains( strPeriod ) ) ? strPeriod : PERIOD_ALL;
    }

    /**
     * Tells whether an event is in a period
     * @param event the event
     * @param strPeriod one of the periods PERIOD_*
     * @param today the current day at midnight
     * @return true when the event is in the period
     */
    private static boolean isInPeriod( SimpleEvent event, String strPeriod, Date today )
    {
        Date dateEnd = ( event.getDateEnd( ) != null ) ? event.getDateEnd( ) : event.getDate( );

        if ( PERIOD_UPCOMING.equals( strPeriod ) )
        {
            return ( dateEnd != null ) && !dateEnd.before( today );
        }

        if ( PERIOD_PAST.equals( strPeriod ) )
        {
            return ( dateEnd != null ) && dateEnd.before( today );
        }

        return true;
    }

    /**
     * Returns the comparator of a sort
     * @param strSort one of the sorts SORT_*
     * @param locale the locale of the alphabetical sort
     * @return the comparator, the events of the same rank sorted by their first day
     */
    private static Comparator<SimpleEvent> getComparator( String strSort, Locale locale )
    {
        Collator collator = Collator.getInstance( locale );
        collator.setStrength( Collator.PRIMARY );
        Comparator<SimpleEvent> byDate = Comparator.comparing( SimpleEvent::getDate, Comparator.nullsLast( Comparator.naturalOrder( ) ) );
        Comparator<SimpleEvent> byTitle = Comparator.comparing( event -> StringUtils.defaultString( event.getTitle( ) ), collator );

        return switch( StringUtils.defaultString( strSort ) )
        {
            case SORT_DATE_DESC -> byDate.reversed( );
            case SORT_TITLE_ASC -> byTitle.thenComparing( byDate );
            case SORT_TITLE_DESC -> byTitle.reversed( ).thenComparing( byDate );
            case SORT_CREATION_DESC -> Comparator
                    .comparing( SimpleEvent::getDateCreation, Comparator.nullsLast( Comparator.<Date> naturalOrder( ).reversed( ) ) )
                    .thenComparing( byDate );
            case SORT_OCCURRENCE_DESC -> Comparator.comparingInt( SimpleEvent::getOccurrence ).reversed( ).thenComparing( byDate );
            default -> byDate;
        };
    }

    /**
     * Normalizes a text for the search: no accent, lower case, trimmed
     * @param strText the text
     * @return the normalized text, empty when the text is null
     */
    private static String normalize( String strText )
    {
        return StringUtils.stripAccents( StringUtils.defaultString( strText ) ).toLowerCase( Locale.ROOT ).trim( );
    }
}
