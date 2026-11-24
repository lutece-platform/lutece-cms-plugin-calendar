/*
 * Copyright (c) 2002-2014, Mairie de Paris
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
package fr.paris.lutece.plugins.calendar.business.portlet;

import fr.paris.lutece.plugins.calendar.service.EventProposals;
import jakarta.enterprise.inject.spi.CDI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.business.CalendarHome;




import fr.paris.lutece.plugins.calendar.service.AgendaResource;
import fr.paris.lutece.plugins.calendar.service.EventDisplayData;
import fr.paris.lutece.plugins.calendar.service.EventImageResourceService;
import fr.paris.lutece.plugins.calendar.service.Utils;
import fr.paris.lutece.plugins.calendar.service.PortletMonthCalendar;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.business.portlet.PortletHtmlContent;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.date.DateUtil;


/**
 * This class represents the business object CalendarPortlet.
 */
public class CalendarPortlet extends PortletHtmlContent
{
    private static final String TEMPLATE_PORTLET_DEFAULT = "skin/plugins/calendar/portlet/calendar_portlet.html";

    // Markers
    private static final String MARK_EVENTS = "events";
    private static final String MARK_MONTH = "month";
    private static final String MARK_SITE_PATH = "site_path";
    private static final String MARK_WIDGET_MONTH = "widget_month";

    // Event keys
    private static final String KEY_AGENDA_ID = "agendaId";
    private static final String KEY_AGENDA_NAME = "agendaName";
    private static final String KEY_EVENT_ID = "eventId";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TOP_EVENT = "topEvent";
    private static final String KEY_DATE = "date";
    private static final String KEY_DATE_LOCAL = "dateLocal";
    private static final String KEY_MONTH = "month";
    private static final String KEY_IMAGE_URL = "imageUrl";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_START = "start";
    private static final long DEFAULT_RANGE_DAYS = 366;
    private static final String PROPERTY_MAX_EVENTS = "calendar.portlet.maxEvents";
    private static final int DEFAULT_MAX_EVENTS = 50;

    /**
     * Sets the name of the plugin to which the portlet belongs
     * @param strPluginName The plugin name
     */
    @Override
    public void setPluginName( String strPluginName )
    {
        super.setPluginName( strPluginName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHtmlContent( HttpServletRequest request )
    {
        Locale local = ( request != null ) ? request.getLocale( ) : Locale.getDefault( );
        Map<String, Object> model = createPortletModel( );
        List<Map<String, Object>> listEvents = getEvents( request, local );
        model.put( MARK_EVENTS, listEvents );
        model.put( MARK_WIDGET_MONTH, getWidgetMonth( listEvents, local ) );
        model.put( MARK_MONTH, PortletMonthCalendar.build( local, new GregorianCalendar( ), request ) );
        model.put( MARK_SITE_PATH, AppPathService.getPortalUrl( ) );

        return renderTemplate( request, TEMPLATE_PORTLET_DEFAULT, model );
    }

    /**
     * Returns the events of the agendas of the portlet the user may see, within the dates of the portlet or its next
     * number of days (from today and for a year when the portlet has neither), read from their occurrences in one query per
     * calendar; an event appears once, at its first date in the period, and the portlet keeps the first
     * calendar.portlet.maxEvents events (50 by default)
     * @param request the request, or null
     * @param local the locale
     * @return the events
     */
    private List<Map<String, Object>> getEvents( HttpServletRequest request, Locale local )
    {
        List<Map<String, Object>> listEvents = new ArrayList<>( );
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        // fetch all the calendars related to the specified portlet
        List<AgendaResource> listAgendasInPortlet = CalendarPortletHome.findAgendasInPortlet( this.getId( ) );
        List<AgendaResource> listAuthorizedAgenda = new ArrayList<>( );

        // Filter to find whether user is identified and authorized to view the agenda on the front office
        for ( AgendaResource agendaResource : listAgendasInPortlet )
        {
            if ( agendaResource != null )
            {
                // Check security access
                String strRole = agendaResource.getRole( );

                if ( StringUtils.isNotBlank( strRole ) && ( request != null )
                        && ( !Constants.PROPERTY_ROLE_NONE.equals( strRole ) ) )
                {
                    if ( SecurityService.isAuthenticationEnable( ) && SecurityService.getInstance( ).isUserInRole( request, strRole ) )
                    {
                        listAuthorizedAgenda.add( agendaResource );
                    }
                }
                else
                {
                    listAuthorizedAgenda.add( agendaResource );
                }
            }
        }

        Date dateBegin = CalendarPortletHome.getBeginDate( this.getId( ) );
        Date dateEnd = CalendarPortletHome.getEndDate( this.getId( ) );
        int nDays = CalendarPortletHome.getRepetitionDays( this.getId( ) );

        if ( ( dateBegin == null ) && ( nDays > 0 ) )
        {
            dateBegin = Utils.getDate( Utils.getDate( new Date( ) ) );
            dateEnd = Utils.addDays( dateBegin, nDays );
        }
        java.time.LocalDate rangeStart = ( dateBegin != null ) ? toLocalDate( dateBegin ) : java.time.LocalDate.now( );
        java.time.LocalDate rangeEnd = ( dateEnd != null ) ? toLocalDate( dateEnd ).plusDays( 1 ) : rangeStart.plusDays( DEFAULT_RANGE_DAYS );
        int nMaxEvents = AppPropertiesService.getPropertyInt( PROPERTY_MAX_EVENTS, DEFAULT_MAX_EVENTS );

        for ( AgendaResource agendaResource : listAuthorizedAgenda )
        {
            String strAgendaId = agendaResource.getAgenda( ).getKeyName( );
            String strAgendaDesc = agendaResource.getAgenda( ).getName( );

            if ( !StringUtils.isNumeric( strAgendaId ) )
            {
                continue;
            }

            Map<Integer, OccurrenceEvent> mapFirstOccurrences = new java.util.LinkedHashMap<>( );

            for ( OccurrenceEvent occurrence : EventProposals.publicOnly( CalendarHome.findOccurrencesByDateRange( List.of( Integer.valueOf( strAgendaId ) ),
                    rangeStart, rangeEnd, List.of( ), plugin ) ) )
            {
                mapFirstOccurrences.putIfAbsent( occurrence.getEventId( ), occurrence );

                if ( mapFirstOccurrences.size( ) >= nMaxEvents )
                {
                    break;
                }
            }

            for ( OccurrenceEvent occurrence : mapFirstOccurrences.values( ) )
            {
                SimpleEvent event = CalendarHome.findEvent( occurrence.getEventId( ), plugin );

                if ( ( event != null ) && StringUtils.isNotEmpty( event.getTitle( ) ) )
                {
                    String strLocalizedDate = DateUtil.getDateString( event.getDate( ), local );

                    if ( !Utils.getDate( event.getDate( ) ).equals( Utils.getDate( event.getDateEnd( ) ) ) )
                    {
                        strLocalizedDate += ( " - " + DateUtil.getDateString( event.getDateEnd( ), local ) );
                    }

                    Map<String, Object> mapEvent = new HashMap<>( );
                    mapEvent.put( KEY_AGENDA_ID, strAgendaId );
                    mapEvent.put( KEY_AGENDA_NAME, strAgendaDesc );
                    mapEvent.put( KEY_EVENT_ID, event.getId( ) );
                    mapEvent.put( KEY_TITLE, event.getTitle( ) );
                    mapEvent.put( KEY_TOP_EVENT, event.getTopEvent( ) );
                    mapEvent.put( KEY_DATE, DateUtil.getDateString( event.getDate( ), local ) );
                    mapEvent.put( KEY_MONTH, new SimpleDateFormat( "MMMM", local ).format( event.getDate( ) ) );
                    mapEvent.put( KEY_IMAGE_URL, EventDisplayData.hasImage( event.getId( ), plugin ) ? EventImageResourceService.getResourceImageEvent( event.getId( ) ) : StringUtils.EMPTY );
                    mapEvent.put( KEY_DESCRIPTION, StringUtils.defaultString( event.getDescription( ) ) );
                    mapEvent.put( KEY_DATE_LOCAL, strLocalizedDate );
                    event.setStatus( occurrence.getStatus( ) );
                    event.setDateTimeStart( occurrence.getDateTimeStart( ) );
                    event.setDateTimeEnd( occurrence.getDateTimeEnd( ) );
                    putDisplayData( mapEvent, event, agendaResource, occurrence.getDate( ), local, plugin );
                    listEvents.add( mapEvent );
                }
            }
        }

        listEvents.sort( java.util.Comparator.comparing( e -> String.valueOf( e.get( KEY_START ) ) ) );

        return ( listEvents.size( ) > nMaxEvents ) ? new ArrayList<>( listEvents.subList( 0, nMaxEvents ) ) : listEvents;
    }

    /**
     * Updates the current instance of the CalendarPortlet object
     */
    public void update( )
    {
        CDI.current( ).select( CalendarPortletHome.class ).get( ).update( this );
    }

    /**
     * Removes the current instance of the CalendarPortlet object
     */
    public void remove( )
    {
        CDI.current( ).select( CalendarPortletHome.class ).get( ).remove( this );
    }

    @Override
    public boolean canBeCachedForAnonymousUsers( )
    {
        return false;
    }

    @Override
    public boolean canBeCachedForConnectedUsers( )
    {
        return false;
    }
    /**
     * Adds to an event of the portlet the data its templates show: day, weekday, month and year, start (ISO, for the
     * sort and the countdown), times, place, link to its page, colour of its calendar, plain text excerpt, and its
     * details (price, attendance, registration)
     * @param mapEvent the event of the portlet
     * @param event the event
     * @param agenda its calendar
     * @param dateShown the date of the event the templates show: its first occurrence in the period of the portlet
     * @param locale the locale
     * @param plugin the plugin
     */
    private static void putDisplayData( Map<String, Object> mapEvent, Event event, AgendaResource agenda, Date dateShown, Locale locale, Plugin plugin )
    {
        mapEvent.putAll( EventDisplayData.build( event, agenda, dateShown, locale, plugin ) );
    }

    /**
     * Converts a date of the portlet to a local day
     * @param date the date
     * @return the day
     */
    private static java.time.LocalDate toLocalDate( Date date )
    {
        return java.time.LocalDate.parse( Utils.getDate( date ), java.time.format.DateTimeFormatter.BASIC_ISO_DATE );
    }

    /**
     * Builds the month of the widget template: the current month, the place of its first day in the week and the days
     * that have an event of the portlet
     * @param listEvents the events of the portlet
     * @param locale the locale
     * @return the month: label, monthKey (yyyy-MM), offset (0 for a monday), days, today (0 when not in the month), eventDays
     */
    private static Map<String, Object> getWidgetMonth( List<Map<String, Object>> listEvents, Locale locale )
    {
        java.time.LocalDate today = java.time.LocalDate.now( );
        java.time.YearMonth month = java.time.YearMonth.from( today );
        String strMonthKey = month.toString( );
        List<Integer> listEventDays = new ArrayList<>( );

        for ( Map<String, Object> mapEvent : listEvents )
        {
            String strDayKey = String.valueOf( mapEvent.get( "dayKey" ) );

            if ( strDayKey.startsWith( strMonthKey ) )
            {
                int nDay = Integer.parseInt( strDayKey.substring( 8, 10 ) );

                if ( !listEventDays.contains( nDay ) )
                {
                    listEventDays.add( nDay );
                }
            }
        }

        List<String> listDayLabels = new ArrayList<>( );

        for ( java.time.DayOfWeek dayOfWeek : java.time.DayOfWeek.values( ) )
        {
            listDayLabels.add( dayOfWeek.getDisplayName( java.time.format.TextStyle.SHORT, locale ) );
        }

        Map<String, Object> mapMonth = new HashMap<>( );
        mapMonth.put( "label", java.time.format.DateTimeFormatter.ofPattern( "MMMM yyyy", locale ).format( today ) );
        mapMonth.put( "monthKey", strMonthKey );
        mapMonth.put( "offset", month.atDay( 1 ).getDayOfWeek( ).getValue( ) - 1 );
        mapMonth.put( "days", month.lengthOfMonth( ) );
        mapMonth.put( "today", today.getDayOfMonth( ) );
        mapMonth.put( "eventDays", listEventDays );
        mapMonth.put( "dayLabels", listDayLabels );

        return mapMonth;
    }
}
