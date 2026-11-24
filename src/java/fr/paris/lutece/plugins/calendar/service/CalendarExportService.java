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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.CalendarFilter;
import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exports of the calendars that are not iCalendar: the JSON file of a calendar and the RSS 2.0 feed of the events.
 * Both are limited to the calendars the visitor may see.
 */
@ApplicationScoped
public class CalendarExportService
{
    /** Request parameter: the format of the export of a calendar ( ical or json ) */
    public static final String PARAMETER_FORMAT = "format";
    /** Format of the export: JSON */
    public static final String FORMAT_JSON = "json";

    private static final String PROPERTY_DAYS_BEFORE = "calendar.ical.feed.daysBefore";
    private static final String PROPERTY_DAYS_AFTER = "calendar.ical.feed.daysAfter";
    private static final String PROPERTY_RSS_MAX_ITEMS = "calendar.rss.maxItems";
    private static final String PROPERTY_RSS_TITLE = "calendar.rss.channel.title";
    private static final String PROPERTY_RSS_DESCRIPTION = "calendar.rss.channel.description";
    private static final int DEFAULT_DAYS_BEFORE = 90;
    private static final int DEFAULT_DAYS_AFTER = 365;
    private static final int DEFAULT_RSS_MAX_ITEMS = 50;
    private static final String TEMPLATE_RSS = "skin/plugins/calendar/rss/calendar_rss.html";
    private static final String CONTENT_TYPE_RSS = "application/rss+xml";
    private static final String PATTERN_RFC_822 = "EEE, dd MMM yyyy HH:mm:ss Z";
    private static final String URL_SHOW_EVENT = "jsp/site/Portal.jsp?page=calendar&action=show_result&event_id=%d&agenda=%d";
    private static final String MARK_CHANNEL_TITLE = "channel_title";
    private static final String MARK_CHANNEL_DESCRIPTION = "channel_description";
    private static final String MARK_CHANNEL_LINK = "channel_link";
    private static final String MARK_LANGUAGE = "language";
    private static final String MARK_ITEMS = "items";

    @Inject
    private CalendarService _calendarService;

    @Inject
    private CalendarEventFeedService _feedService;

    @Inject
    private CalendarIcalFeedService _icalFeedService;

    /**
     * Writes the JSON file of the calendar given by the parameter calendar_id: the calendar and its occurrences over the
     * period of the iCalendar feed, in the format of the EventCalendar events, or answers 404
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @throws IOException
     *             if the response cannot be written
     */
    public void writeJson( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        AgendaResource agenda = _icalFeedService.getVisibleAgenda( request, request.getParameter( CalendarIcalFeedService.PARAMETER_CALENDAR_ID ) );

        if ( agenda == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        String strBaseUrl = AppPathService.getBaseUrl( request );
        LocalDate today = LocalDate.now( );
        Map<String, Object> calendar = new LinkedHashMap<>( );
        calendar.put( "id", Integer.parseInt( agenda.getId( ) ) );
        calendar.put( "name", StringUtils.defaultString( agenda.getName( ) ) );
        calendar.put( "color", agenda.getEventColorCode( ) );

        Map<String, Object> export = new LinkedHashMap<>( );
        export.put( "calendar", calendar );
        export.put( "events", _feedService.getItems( List.of( agenda ),
                today.minusDays( AppPropertiesService.getPropertyInt( PROPERTY_DAYS_BEFORE, DEFAULT_DAYS_BEFORE ) ),
                today.plusDays( AppPropertiesService.getPropertyInt( PROPERTY_DAYS_AFTER, DEFAULT_DAYS_AFTER ) + 1L ), List.of( ),
                occurrence -> strBaseUrl + String.format( URL_SHOW_EVENT, occurrence.getEventId( ), occurrence.getIdCalendar( ) ), false ) );

        response.setHeader( "Content-Disposition", "attachment; filename=\"calendar-" + agenda.getId( ) + ".json\"" );
        _feedService.writeJson( response, export );
    }

    /**
     * Writes the RSS 2.0 feed of the events of the calendars given by the parameters calendar_id (all the visible ones
     * when absent) and category, sorted by the parameter sort_events
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @throws IOException
     *             if the response cannot be written
     */
    public void writeRss( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        Locale locale = request.getLocale( );
        Set<String> setVisible = _calendarService.getAgendaResources( request ).stream( ).map( AgendaResource::getId ).collect( Collectors.toSet( ) );
        String [ ] arrayRequested = request.getParameterValues( Constants.PARAMETER_CALENDAR_ID );
        List<String> listKept = ( arrayRequested == null ) ? new ArrayList<>( setVisible )
                : Arrays.stream( arrayRequested ).filter( setVisible::contains ).toList( );
        List<Map<String, Object>> listItems = new ArrayList<>( );

        if ( !listKept.isEmpty( ) )
        {
            CalendarFilter filter = new CalendarFilter( );
            filter.setCalendarIds( listKept.stream( ).filter( id -> id.matches( "\\d{1,9}" ) ).mapToInt( Integer::parseInt ).toArray( ) );
            filter.setCategoriesId( toIds( request.getParameterValues( Constants.PARAMETER_CATEGORY ) ) );
            filter.setSortEvents( StringUtils.isNumeric( request.getParameter( Constants.PARAMETER_SORT_EVENTS ) )
                    ? Integer.parseInt( request.getParameter( Constants.PARAMETER_SORT_EVENTS ) )
                    : -1 );

            String strBaseUrl = AppPathService.getBaseUrl( request );
            int nMax = AppPropertiesService.getPropertyInt( PROPERTY_RSS_MAX_ITEMS, DEFAULT_RSS_MAX_ITEMS );

            Map<Integer, java.util.Set<Integer>> mapProposals = new HashMap<>( );

            for ( Event event : CalendarHome.findEventsByFilter( filter, plugin ) )
            {
                if ( listItems.size( ) >= nMax )
                {
                    break;
                }

                if ( mapProposals.computeIfAbsent( event.getIdCalendar( ), nId -> EventProposals.getProposedEventIds( nId, plugin ) ).contains( event.getId( ) ) )
                {
                    continue;
                }

                listItems.add( toRssItem( event, strBaseUrl ) );
            }
        }

        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_CHANNEL_TITLE, I18nService.getLocalizedString( PROPERTY_RSS_TITLE, locale ) );
        model.put( MARK_CHANNEL_DESCRIPTION, I18nService.getLocalizedString( PROPERTY_RSS_DESCRIPTION, locale ) );
        model.put( MARK_CHANNEL_LINK, AppPathService.getBaseUrl( request ) + "jsp/site/Portal.jsp?page=calendar" );
        model.put( MARK_LANGUAGE, locale.getLanguage( ) );
        model.put( MARK_ITEMS, listItems );

        response.setContentType( CONTENT_TYPE_RSS );
        response.setCharacterEncoding( StandardCharsets.UTF_8.name( ) );
        response.getWriter( ).write( AppTemplateService.getTemplate( TEMPLATE_RSS, locale, model ).getHtml( ) );
        response.getWriter( ).flush( );
    }

    /**
     * Converts an event into an RSS item
     *
     * @param event
     *            the event
     * @param strBaseUrl
     *            the base URL of the site
     * @return the item: title, link, description, location, date of the event and publication date
     */
    private static Map<String, Object> toRssItem( Event event, String strBaseUrl )
    {
        Map<String, Object> item = new HashMap<>( );
        String strLink = strBaseUrl + String.format( URL_SHOW_EVENT, event.getId( ), event.getIdCalendar( ) );
        String strLocation = Arrays.asList( event.getLocation( ), event.getLocationAddress( ), event.getLocationZip( ), event.getLocationTown( ) ).stream( )
                .filter( StringUtils::isNotBlank ).collect( Collectors.joining( " " ) );

        item.put( "title", StringUtils.defaultString( event.getTitle( ) ) );
        item.put( "link", strLink );
        item.put( "guid", strLink );
        item.put( "description", CalendarIcalFeedService.toPlainText( event.getDescription( ) ) );
        item.put( "location", strLocation );
        item.put( "date", event.getDate( ) );
        item.put( "pubDate", formatRfc822( ( event.getDateCreation( ) != null ) ? event.getDateCreation( ) : event.getDate( ) ) );

        return item;
    }

    /**
     * Formats a date as RSS 2.0 expects (RFC 822, in English)
     *
     * @param date
     *            the date, or null
     * @return the formatted date, empty when there is none
     */
    private static String formatRfc822( java.util.Date date )
    {
        return ( date == null ) ? StringUtils.EMPTY : new SimpleDateFormat( PATTERN_RFC_822, Locale.ENGLISH ).format( date );
    }

    /**
     * Reads the numeric identifiers of a repeatable parameter
     *
     * @param values
     *            the values, or null
     * @return the identifiers, or null when there is none
     */
    private static int [ ] toIds( String [ ] values )
    {
        if ( values == null )
        {
            return null;
        }

        int [ ] ids = Arrays.stream( values ).filter( v -> v != null && v.matches( "\\d{1,9}" ) ).mapToInt( Integer::parseInt ).toArray( );

        return ( ids.length == 0 ) ? null : ids;
    }
}
