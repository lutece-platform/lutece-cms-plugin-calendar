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
package fr.paris.lutece.plugins.calendar.service;

import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.service.search.CalendarSearchService;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.url.UrlItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;


/**
 * The month shown by the calendar portlets: its days, the days having events and the period shortcuts
 */
public final class PortletMonthCalendar
{
    private static final String [ ] DAY_LABEL_KEYS = { Constants.PROPERTY_SHORTLABEL_MONDAY, Constants.PROPERTY_SHORTLABEL_TUESDAY,
        Constants.PROPERTY_SHORTLABEL_WEDNESDAY, Constants.PROPERTY_SHORTLABEL_THURSDAY, Constants.PROPERTY_SHORTLABEL_FRIDAY,
        Constants.PROPERTY_SHORTLABEL_SATURDAY, Constants.PROPERTY_SHORTLABEL_SUNDAY, };

    private String _strMonthLabel;
    private String _strMonthUrl;
    private final List<String> _listDayLabels = new ArrayList<>( );
    private final List<List<Day>> _listWeeks = new ArrayList<>( );
    private final List<Shortcut> _listShortcuts = new ArrayList<>( );

    /**
     * Private constructor: built by {@link #build( Locale, Calendar, HttpServletRequest )}
     */
    private PortletMonthCalendar( )
    {
    }

    /**
     * Builds the month of a date
     * @param locale the locale
     * @param cal a date of the month to show
     * @param request the request, or null
     * @return the month
     */
    public static PortletMonthCalendar build( Locale locale, Calendar cal, HttpServletRequest request )
    {
        PortletMonthCalendar month = new PortletMonthCalendar( );
        Calendar calendar = new GregorianCalendar( );

        //Set the calendar in the beginning of the month
        calendar.set( cal.get( Calendar.YEAR ), cal.get( Calendar.MONTH ), 1 );

        int nDayOfWeek = calendar.get( Calendar.DAY_OF_WEEK );

        //If day of week is sunday: nDayOfWeek = 8
        if ( nDayOfWeek == 1 )
        {
            nDayOfWeek = 8;
        }

        Calendar calendarEnd = new GregorianCalendar( );
        calendarEnd.set( cal.get( Calendar.YEAR ), cal.get( Calendar.MONTH ), calendar.getMaximum( Calendar.DAY_OF_MONTH ) );

        String strBaseUrl = AppPathService.getPortalUrl( );
        UrlItem urlMonth = new UrlItem( strBaseUrl );
        urlMonth.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        urlMonth.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_DO_SEARCH );
        urlMonth.addParameter( Constants.PARAMETER_DATE_START, DateUtil.getDateString( calendar.getTime( ), locale ) );
        urlMonth.addParameter( Constants.PARAMETER_DATE_END, DateUtil.getDateString( calendarEnd.getTime( ), locale ) );
        urlMonth.addParameter( Constants.PARAMETER_PERIOD, Constants.PROPERTY_PERIOD_RANGE );
        month._strMonthUrl = urlMonth.getUrl( );
        month._strMonthLabel = Utils.getMonthLabel( Utils.getDate( calendar ), locale );

        month.addShortcuts( locale );

        for ( String strKey : DAY_LABEL_KEYS )
        {
            month._listDayLabels.add( I18nService.getLocalizedString( strKey, locale ) );
        }

        String strPageId = ( request != null ) ? request.getParameter( Constants.PARAMETER_PAGE_ID ) : StringUtils.EMPTY;
        String [ ] arrayCalendarIds = Utils.getCalendarIds( request );
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        CalendarSearchService searchService = CDI.current( ).select( CalendarSearchService.class ).get( );

        boolean bDone = false;
        boolean bStarted = false;

        while ( !bDone )
        {
            List<Day> listWeek = new ArrayList<>( );

            for ( int i = 0; i < 7; i++ )
            {
                if ( ( ( ( i + 2 ) != nDayOfWeek ) && !bStarted ) || bDone )
                {
                    listWeek.add( new Day( 0, getDayClass( calendar ), null ) );

                    continue;
                }

                bStarted = true;

                int nDay = calendar.get( Calendar.DAY_OF_MONTH );
                Date date = Utils.getDate( Utils.getDate( calendar ) );
                List<Event> listEvent = searchService.getSearchResults( arrayCalendarIds, null, StringUtils.EMPTY, date, date, plugin );

                if ( !listEvent.isEmpty( ) )
                {
                    UrlItem urlDay = new UrlItem( strBaseUrl );

                    if ( StringUtils.isNotBlank( strPageId ) && StringUtils.isNumeric( strPageId ) )
                    {
                        urlDay.addParameter( Constants.PARAMETER_PAGE_ID, strPageId );
                    }

                    urlDay.addParameter( Constants.PARAMETER_DATE, DateUtil.getDateString( calendar.getTime( ), locale ) );
                    listWeek.add( new Day( nDay, Constants.STYLE_CLASS_SMALLMONTH_DAY + Constants.STYLE_CLASS_SUFFIX_EVENT, urlDay.getUrl( ) ) );
                }
                else
                {
                    listWeek.add( new Day( nDay, getDayClass( calendar ), null ) );
                }

                calendar.roll( Calendar.DAY_OF_MONTH, true );

                if ( calendar.get( Calendar.DAY_OF_MONTH ) < nDay )
                {
                    bDone = true;
                }
            }

            month._listWeeks.add( listWeek );
        }

        return month;
    }

    /**
     * Adds the today, tomorrow and week shortcuts
     * @param locale the locale
     */
    private void addShortcuts( Locale locale )
    {
        String strToday = DateUtil.getDateString( new Date( ), locale );
        _listShortcuts.add( new Shortcut( I18nService.getLocalizedString( Constants.PROPERTY_SHORTCUT_TODAY, locale ),
                Constants.PROPERTY_PERIOD_TODAY, strToday, strToday ) );

        Calendar calTomorrow = new GregorianCalendar( );
        calTomorrow.add( Calendar.DATE, 1 );

        String strTomorrow = DateUtil.getDateString( calTomorrow.getTime( ), locale );
        _listShortcuts.add( new Shortcut( I18nService.getLocalizedString( Constants.PROPERTY_SHORTCUT_TOMORROW, locale ),
                Constants.PROPERTY_PERIOD_RANGE, strTomorrow, strTomorrow ) );

        Calendar calendarFirstDay = new GregorianCalendar( );
        calendarFirstDay.set( Calendar.DAY_OF_WEEK, Calendar.MONDAY );

        Calendar calendarLastDay = (Calendar) calendarFirstDay.clone( );
        calendarLastDay.add( Calendar.DATE, 6 );
        _listShortcuts.add( new Shortcut( I18nService.getLocalizedString( Constants.PROPERTY_SHORTCUT_WEEK, locale ),
                Constants.PROPERTY_PERIOD_WEEK, DateUtil.getDateString( calendarFirstDay.getTime( ), locale ),
                DateUtil.getDateString( calendarLastDay.getTime( ), locale ) ) );
    }

    /**
     * Returns the css class of a day
     * @param calendar the day
     * @return the css class
     */
    private static String getDayClass( Calendar calendar )
    {
        StringBuilder sbClass = new StringBuilder( Constants.STYLE_CLASS_SMALLMONTH_DAY );
        String strDate = Utils.getDate( calendar );
        String strToday = Utils.getDateToday( );

        if ( Utils.isDayOff( calendar ) )
        {
            sbClass.append( Constants.STYLE_CLASS_SUFFIX_OFF );
        }
        else if ( strDate.compareTo( strToday ) < 0 )
        {
            sbClass.append( Constants.STYLE_CLASS_SUFFIX_OLD );
        }
        else if ( strDate.equals( strToday ) )
        {
            sbClass.append( Constants.STYLE_CLASS_SUFFIX_TODAY );
        }

        return sbClass.toString( );
    }

    /**
     * @return the label of the month
     */
    public String getMonthLabel( )
    {
        return _strMonthLabel;
    }

    /**
     * @return the url searching the events of the month
     */
    public String getMonthUrl( )
    {
        return _strMonthUrl;
    }

    /**
     * @return the short labels of the days of the week, monday first
     */
    public List<String> getDayLabels( )
    {
        return _listDayLabels;
    }

    /**
     * @return the weeks of the month
     */
    public List<List<Day>> getWeeks( )
    {
        return _listWeeks;
    }

    /**
     * @return the period shortcuts
     */
    public List<Shortcut> getShortcuts( )
    {
        return _listShortcuts;
    }

    /**
     * A day of the month
     */
    public static final class Day
    {
        private final int _nNumber;
        private final String _strCssClass;
        private final String _strUrl;

        /**
         * Constructor
         * @param nNumber the day of the month, 0 for a cell outside the month
         * @param strCssClass the css class
         * @param strUrl the url of the events of the day, null when there is none
         */
        Day( int nNumber, String strCssClass, String strUrl )
        {
            _nNumber = nNumber;
            _strCssClass = strCssClass;
            _strUrl = strUrl;
        }

        /**
         * @return the day of the month, 0 for a cell outside the month
         */
        public int getNumber( )
        {
            return _nNumber;
        }

        /**
         * @return the css class
         */
        public String getCssClass( )
        {
            return _strCssClass;
        }

        /**
         * @return the url of the events of the day, null when there is none
         */
        public String getUrl( )
        {
            return _strUrl;
        }
    }

    /**
     * A period shortcut
     */
    public static final class Shortcut
    {
        private final String _strLabel;
        private final int _nPeriod;
        private final String _strDateStart;
        private final String _strDateEnd;

        /**
         * Constructor
         * @param strLabel the label
         * @param nPeriod the period
         * @param strDateStart the first date
         * @param strDateEnd the last date
         */
        Shortcut( String strLabel, int nPeriod, String strDateStart, String strDateEnd )
        {
            _strLabel = strLabel;
            _nPeriod = nPeriod;
            _strDateStart = strDateStart;
            _strDateEnd = strDateEnd;
        }

        /**
         * @return the label
         */
        public String getLabel( )
        {
            return _strLabel;
        }

        /**
         * @return the period
         */
        public int getPeriod( )
        {
            return _nPeriod;
        }

        /**
         * @return the first date
         */
        public String getDateStart( )
        {
            return _strDateStart;
        }

        /**
         * @return the last date
         */
        public String getDateEnd( )
        {
            return _strDateEnd;
        }
    }
}
