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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.CalendarSubscriber;
import fr.paris.lutece.plugins.calendar.business.CalendarSubscriberHome;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.business.reminder.ReminderSentHome;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.mail.MailService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Sends to the subscribers of a calendar a reminder of each occurrence that starts within the next hours
 * (calendar.reminder.hoursBefore). Each reminder is claimed in the log calendar_reminder_sent before it is sent, so
 * that a subscriber receives it once, even when the daemon runs on several servers.
 */
@ApplicationScoped
public class CalendarReminderService
{
    private static final String PROPERTY_HOURS_BEFORE = "calendar.reminder.hoursBefore";
    private static final String PROPERTY_LOG_DAYS = "calendar.reminder.logDays";
    private static final String PROPERTY_BASE_URL = "lutece.base.url";
    private static final String PROPERTY_PROD_URL = "lutece.prod.url";
    private static final String PROPERTY_SITE_URL = "calendar.reminder.siteUrl";
    private static final String PROPERTY_SENDER_NAME = "calendar.sender.name";
    private static final String PROPERTY_SENDER_EMAIL = "calendar.sender.email";
    private static final String PROPERTY_SUBJECT = "calendar.reminder.email.subject";
    private static final int DEFAULT_HOURS_BEFORE = 24;
    private static final int DEFAULT_LOG_DAYS = 60;
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String TEMPLATE_REMINDER = "skin/plugins/calendar/reminder_email.html";
    private static final String MARK_OCCURRENCE = "occurrence";
    private static final String MARK_CALENDAR_NAME = "calendar_name";
    private static final String MARK_DATE = "occurrence_date";
    private static final String MARK_TIME_START = "time_start";
    private static final String MARK_TIME_END = "time_end";
    private static final String MARK_LOCATION = "location";
    private static final String MARK_EVENT_URL = "event_url";
    private static final String MARK_UNSUBSCRIBE_URL = "unsubscribe_url";
    private static final String JSP_PORTAL = "jsp/site/Portal.jsp";

    @Inject
    private CalendarService _calendarService;

    /**
     * Returns the absolute address of the site the links of a reminder point to. The daemon has no request to read it from:
     * it is the property calendar.reminder.siteUrl, else lutece.prod.url, else lutece.base.url
     *
     * @return the address, ending with a slash, or null when none of these properties holds an absolute http or https address
     */
    static String getReminderSiteUrl( )
    {
        for ( String strProperty : List.of( PROPERTY_SITE_URL, PROPERTY_PROD_URL, PROPERTY_BASE_URL ) )
        {
            String strUrl = StringUtils.trimToEmpty( AppPropertiesService.getProperty( strProperty, StringUtils.EMPTY ) );

            if ( strUrl.matches( "(?i)^https?://\\S+$" ) )
            {
                return strUrl.endsWith( "/" ) ? strUrl : ( strUrl + "/" );
            }
        }

        return null;
    }

    /**
     * Sends the reminders of the occurrences starting after a time and before the end of the reminder delay
     *
     * @param now
     *            the time of the run
     * @return the number of reminders sent
     */
    public int sendReminders( LocalDateTime now )
    {
        if ( getReminderSiteUrl( ) == null )
        {
            AppLogService.error( "Calendar reminders not sent: set calendar.reminder.siteUrl (or lutece.prod.url) to the absolute address of the site, the links of a reminder must work in a mail client" );

            return 0;
        }

        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        LocalDateTime limit = now.plusHours( AppPropertiesService.getPropertyInt( PROPERTY_HOURS_BEFORE, DEFAULT_HOURS_BEFORE ) );
        int nSent = 0;

        List<OccurrenceEvent> listOccurrences = EventProposals.publicOnly( CalendarHome.findOccurrencesByDateRange( CalendarHome.findCalendarIds( plugin ),
                now.toLocalDate( ), limit.toLocalDate( ).plusDays( 1 ), List.of( ), plugin ) );

        for ( OccurrenceEvent occurrence : listOccurrences )
        {
            LocalDateTime start = CalendarEventFeedService.getStart( occurrence );

            if ( start.isAfter( now ) && !start.isAfter( limit ) && !STATUS_CANCELLED.equalsIgnoreCase( StringUtils.trim( occurrence.getStatus( ) ) ) )
            {
                nSent += sendOccurrenceReminders( occurrence, plugin );
            }
        }

        ReminderSentHome.removeOlderThan( Timestamp.valueOf( now.minusDays( AppPropertiesService.getPropertyInt( PROPERTY_LOG_DAYS, DEFAULT_LOG_DAYS ) ) ),
                plugin );

        return nSent;
    }

    /**
     * Sends the reminder of an occurrence to the subscribers of its calendar who have not received it
     *
     * @param occurrence
     *            the occurrence
     * @param plugin
     *            the plugin
     * @return the number of reminders sent
     */
    private int sendOccurrenceReminders( OccurrenceEvent occurrence, Plugin plugin )
    {
        AgendaResource agenda = _calendarService.getAgendaResource( occurrence.getIdCalendar( ) );
        Locale locale = I18nService.getDefaultLocale( );
        int nSent = 0;

        for ( CalendarSubscriber subscriber : CalendarSubscriberHome.findSubscribers( occurrence.getIdCalendar( ), plugin ) )
        {
            if ( StringUtils.isBlank( subscriber.getEmail( ) ) || !ReminderSentHome.claim( occurrence.getId( ), subscriber.getId( ), plugin ) )
            {
                continue;
            }

            try
            {
                MailService.sendMailHtml( subscriber.getEmail( ), AppPropertiesService.getProperty( PROPERTY_SENDER_NAME ),
                        AppPropertiesService.getProperty( PROPERTY_SENDER_EMAIL ), I18nService.getLocalizedString( PROPERTY_SUBJECT,
                                new Object [ ] { StringUtils.defaultString( occurrence.getTitle( ) ) }, locale ),
                        buildMessage( occurrence, agenda, subscriber, locale ) );
                nSent++;
            }
            catch( RuntimeException e )
            {
                ReminderSentHome.release( occurrence.getId( ), subscriber.getId( ), plugin );
                AppLogService.error( "Calendar: the reminder of the occurrence {} could not be sent to the subscriber {}", occurrence.getId( ),
                        subscriber.getId( ), e );
            }
        }

        return nSent;
    }

    /**
     * Builds the HTML message of a reminder
     *
     * @param occurrence
     *            the occurrence
     * @param agenda
     *            its calendar, or null
     * @param subscriber
     *            the subscriber
     * @param locale
     *            the language of the message
     * @return the message
     */
    private String buildMessage( OccurrenceEvent occurrence, AgendaResource agenda, CalendarSubscriber subscriber, Locale locale )
    {
        String strBaseUrl = getReminderSiteUrl( );
        LocalDate day = occurrence.getDate( ).toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( );
        LocalTime timeStart = CalendarEventFeedService.parseTime( occurrence.getDateTimeStart( ) );
        LocalTime timeEnd = CalendarEventFeedService.parseTime( occurrence.getDateTimeEnd( ) );
        DateTimeFormatter formatTime = DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ).withLocale( locale );

        UrlItem urlEvent = new UrlItem( strBaseUrl + JSP_PORTAL );
        urlEvent.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        urlEvent.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_SHOW_RESULT );
        urlEvent.addParameter( Constants.PARAMETER_EVENT_ID, occurrence.getEventId( ) );
        urlEvent.addParameter( Constants.PARAM_AGENDA, occurrence.getIdCalendar( ) );

        UrlItem urlUnsubscribe = new UrlItem( strBaseUrl + JSP_PORTAL );
        urlUnsubscribe.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        urlUnsubscribe.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_CONFIRM_UNSUBSCRIBE );
        urlUnsubscribe.addParameter( Constants.PARAMETER_EMAIL, subscriber.getEmail( ) );
        urlUnsubscribe.addParameter( Constants.PARAM_AGENDA, occurrence.getIdCalendar( ) );

        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_OCCURRENCE, occurrence );
        model.put( MARK_CALENDAR_NAME, ( agenda != null ) ? StringUtils.defaultString( agenda.getName( ) ) : StringUtils.EMPTY );
        model.put( MARK_DATE, DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ).format( day ) );
        model.put( MARK_TIME_START, ( timeStart != null ) ? formatTime.format( timeStart ) : StringUtils.EMPTY );
        model.put( MARK_TIME_END, ( timeEnd != null ) ? formatTime.format( timeEnd ) : StringUtils.EMPTY );
        model.put( MARK_LOCATION, String.join( ", ", java.util.stream.Stream.of( occurrence.getLocation( ), occurrence.getLocationTown( ) )
                .filter( StringUtils::isNotBlank ).toList( ) ) );
        model.put( MARK_EVENT_URL, urlEvent.getUrl( ) );
        model.put( MARK_UNSUBSCRIBE_URL, urlUnsubscribe.getUrl( ) );

        return AppTemplateService.getTemplate( TEMPLATE_REMINDER, locale, model ).getHtml( );
    }
}
