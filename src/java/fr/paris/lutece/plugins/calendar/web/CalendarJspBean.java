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
package fr.paris.lutece.plugins.calendar.web;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.CalendarSubscriber;
import fr.paris.lutece.plugins.calendar.business.CalendarSubscriberHome;
import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.business.category.Category;
import fr.paris.lutece.plugins.calendar.business.category.CategoryHome;
import fr.paris.lutece.plugins.calendar.business.parameter.CalendarParameterHome;
import fr.paris.lutece.plugins.calendar.service.AgendaResource;
import fr.paris.lutece.plugins.calendar.service.AgendaSubscriberService;
import fr.paris.lutece.plugins.calendar.service.CalendarLibraryService;
import fr.paris.lutece.plugins.calendar.service.CalendarResourceIdService;
import fr.paris.lutece.plugins.calendar.service.CalendarService;
import fr.paris.lutece.plugins.calendar.service.CategoryService;
import fr.paris.lutece.plugins.calendar.service.EventImageResourceService;
import fr.paris.lutece.plugins.calendar.business.button.CalendarButtonConfig;
import fr.paris.lutece.plugins.calendar.business.button.CalendarButtonHome;
import fr.paris.lutece.plugins.calendar.service.CalendarButtonService;
import fr.paris.lutece.plugins.calendar.service.CalendarDemoService;
import fr.paris.lutece.plugins.calendar.service.EventListFilter;
import fr.paris.lutece.plugins.calendar.service.EventProposals;
import fr.paris.lutece.plugins.calendar.service.CalendarEventFeedService;
import fr.paris.lutece.plugins.calendar.service.EventDetailsService;
import fr.paris.lutece.plugins.calendar.business.details.EventDetails;
import fr.paris.lutece.plugins.calendar.business.details.EventDetailsHome;
import fr.paris.lutece.plugins.calendar.service.EventListService;
import fr.paris.lutece.plugins.calendar.service.Utils;
import fr.paris.lutece.plugins.calendar.service.search.CalendarIndexer;
import fr.paris.lutece.plugins.calendar.utils.CalendarIndexerUtils;
import fr.paris.lutece.portal.business.indexeraction.IndexerAction;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.business.role.RoleHome;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.image.ImageResource;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.search.IndexationService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.ResponseBody;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.portal.web.constants.Parameters;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.portal.web.util.LocalizedPaginator;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.html.Paginator;
import fr.paris.lutece.util.sort.AttributeComparator;
import fr.paris.lutece.util.string.StringUtil;
import fr.paris.lutece.util.url.UrlItem;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;


/**
 * This class provides the user interface to manage calendars from the dataBase
 * features ( manage, create, modify, remove)
 */
@SessionScoped
@Named
@Controller( controllerJsp = CalendarJspBean.CONTROLLER_JSP, controllerPath = CalendarJspBean.CONTROLLER_PATH, right = CalendarJspBean.RIGHT_MANAGE_CALENDAR, securityTokenEnabled = true )
public class CalendarJspBean extends MVCAdminJspBean
{
    ////////////////////////////////////////////////////////////////////////////
    // Constants
    // Prefix of the properties related to this checkbox
    public static final String PROPERTY_TIME_INTERVAL_LIST = "calendar.interval.time";
    public static final String PROPERTY_TOP_EVENT_LIST = "calendar.topevent.values";
    public static final String PROPERTY_EVENT_STATUS_LIST = "calendar.event.status.list";
    public static final String PROPERTY_EVENT_STATUS_VALUES = "calendar.event.status.values";
    public static final String PROPERTY_TOP_EVENT_DEFAULT = "calendar.topevent.default";
    public static final String PROPERTY_EMAIL_NOTIFY = "calendar.email.notify";
    public static final String PROPERTY_TAGS_REGEXP = "[0-9\\p{L}\\p{M}\\s\\-\\_]*";

    // Right
    public static final String RIGHT_MANAGE_CALENDAR = "CALENDAR_MANAGEMENT";

    // Controller
    public static final String CONTROLLER_JSP = "ManageCalendars.jsp";
    public static final String CONTROLLER_PATH = "jsp/admin/plugins/calendar/";

    // Views
    public static final String VIEW_MANAGE_CALENDARS = "manageCalendars";
    public static final String VIEW_MANAGE_ADVANCED_PARAMETERS = "manageAdvancedParameters";
    public static final String VIEW_MANAGE_DEMO = "manageDemo";
    public static final String VIEW_CONFIRM_REMOVE_DEMO = "confirmRemoveDemo";
    public static final String VIEW_CREATE_CALENDAR = "createCalendar";
    public static final String VIEW_MODIFY_CALENDAR = "modifyCalendar";
    public static final String VIEW_CONFIRM_REMOVE_CALENDAR = "confirmRemoveCalendar";
    public static final String VIEW_CREATE_EVENT = "createEvent";
    public static final String VIEW_EVENT_LIST = "eventList";
    public static final String VIEW_OCCURRENCE_LIST = "occurrenceList";
    public static final String VIEW_MODIFY_EVENT = "modifyEvent";
    public static final String VIEW_MODIFY_OCCURRENCE = "modifyOccurrence";
    public static final String VIEW_CONFIRM_REMOVE_EVENT = "confirmRemoveEvent";
    public static final String VIEW_CONFIRM_REMOVE_OCCURRENCE = "confirmRemoveOccurrence";
    public static final String VIEW_MODIFY_OCCURRENCE_LIST = "modifyOccurrenceList";
    public static final String VIEW_CONFIRM_MODIFY_OCCURRENCE_STATUS = "confirmModifyOccurrenceStatus";
    public static final String VIEW_MANAGE_SUBSCRIBERS = "manageSubscribers";
    public static final String VIEW_CONFIRM_REMOVE_SUBSCRIBER = "confirmRemoveSubscriber";
    public static final String VIEW_INSERT_SERVICE = "insertService";
    public static final String VIEW_INSERT_LINK_SERVICE = "insertLinkService";
    public static final String VIEW_EVENTS_FEED = "eventsFeed";
    public static final String VIEW_CALENDAR_VIEW = "calendarView";
    public static final String VIEW_MODIFY_CALENDAR_BUTTON = "modifyCalendarButton";

    // Actions
    public static final String ACTION_MODIFY_CALENDAR_PARAMETER_VALUES = "modifyCalendarParameterValues";
    public static final String ACTION_INSTALL_DEMO = "installDemo";
    public static final String ACTION_REMOVE_DEMO = "removeDemo";
    public static final String ACTION_CREATE_CALENDAR = "createCalendar";
    public static final String ACTION_MODIFY_CALENDAR = "modifyCalendar";
    public static final String ACTION_REMOVE_CALENDAR = "removeCalendar";
    public static final String ACTION_CREATE_EVENT = "createEvent";
    public static final String ACTION_MODIFY_EVENT = "modifyEvent";
    public static final String ACTION_MODIFY_OCCURRENCE = "modifyOccurrence";
    public static final String ACTION_MODIFY_OCCURRENCE_PERIODICITY = "modifyOccurrencePeriodicity";
    public static final String ACTION_REMOVE_EVENT = "removeEvent";
    public static final String ACTION_REMOVE_OCCURRENCE = "removeOccurrence";
    public static final String ACTION_MODIFY_OCCURRENCE_STATUS = "modifyOccurrenceStatus";
    public static final String ACTION_REMOVE_SUBSCRIBER = "removeSubscriber";
    public static final String ACTION_MOVE_OCCURRENCE = "moveOccurrence";

    // Session attribute
    public static final String ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_CONTENT_FILE = "lutece_document_calendar_content_file";
    public static final String ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_MIME_TYPE_FILE = "lutece_document_calendar_mime_type_file";

    /**
     * Generated serial version UID
     */
    private static final long serialVersionUID = 7168759278389663673L;

    private static final String PROPERTY_LIMIT_MAX_SUSCRIBER = "calendar.limit.max";
    private static final String PROPERTY_LIMIT_MIN_SUSCRIBER = "calendar.limit.min";

    //Templates
    private static final String TEMPLATE_MANAGE_CALENDARS = "admin/plugins/calendar/manage_calendars.html";
    private static final String TEMPLATE_MANAGE_ADVANCED_PARAMETERS = "admin/plugins/calendar/manage_advanced_parameters.html";
    private static final String TEMPLATE_MANAGE_DEMO = "admin/plugins/calendar/manage_demo.html";
    private static final String TEMPLATE_CREATE_CALENDAR = "admin/plugins/calendar/create_calendar.html";
    private static final String TEMPLATE_MODIFY_CALENDAR = "admin/plugins/calendar/modify_calendar.html";
    private static final String TEMPLATE_CREATE_EVENT = "admin/plugins/calendar/create_event.html";
    private static final String TEMPLATE_MODIFY_EVENT = "admin/plugins/calendar/modify_event.html";
    private static final String TEMPLATE_MODIFY_OCCURRENCE = "admin/plugins/calendar/modify_occurrence.html";
    private static final String TEMPLATE_MANAGE_SUBSCRIBERS = "admin/plugins/calendar/manage_subscribers.html";
    private static final String TEMPLATE_EVENT_LIST = "admin/plugins/calendar/event_list.html";
    private static final String TEMPLATE_OCCURRENCE_LIST = "admin/plugins/calendar/occurrence_list.html";
    private static final String MARK_INTERVAL_LIST = "interval_list";
    private static final String MARK_NUMBER_DAYS = "number_days";
    private static final String MARK_INTERVAL_TIME_SPAN = "time_span";
    private static final String MARK_TOP_EVENT_LIST = "top_event_list";
    private static final String MARK_TOP_EVENT_DEFAULT = "top_event_default";
    private static final String MARK_EVENT_STATUS_LIST = "event_status_list";
    private static final String MARK_DEFAULT_STATUS = "default_status";
    private static final String MARK_EMAIL_NOTIFY = "notify";
    private static final String MARK_INSERT_SERVICE_PAGE = "insert_service_page";
    private static final String MARK_INSERT_SERVICE_LINK_PAGE = "insert_service_link_page";
    private static final String MARK_WEBAPP_URL = "webapp_url";

    // Jsp Definition
    private static final String JSP_GET_DOCUMENT_INSERT_SERVICE = "modules/document/SelectDocument.jsp";
    private static final String JSP_GET_DOCUMENT_INSERT_LINK_SERVICE = "jsp/admin/insert/GetAvailableInsertServices.jsp";

    // Message keys
    private static final String MESSAGE_ACCESS_DENIED = "Access denied";
    private static final String MESSAGE_CONFIRM_REMOVE_CALENDAR = "calendar.message.confirmRemoveCalendar";
    private static final String MESSAGE_CONFIRM_REMOVE_DEMO = "calendar.manage_demo.confirmRemove";
    private static final String MESSAGE_DEMO_INSTALLED = "calendar.manage_demo.installed";
    private static final String MESSAGE_DEMO_REMOVED = "calendar.manage_demo.removed";
    private static final String MARK_DEMO_INSTALLED = "demo_installed";
    private static final String MESSAGE_INVALID_LIBRARY = "calendar.advanced_parameters.library.invalid";
    private static final String PARAMETER_FROM_TECHNICAL_MENU = "from_technical_menu";
    private static final String ANCHOR_CALENDAR_ADMIN_DASHBOARD = "calendarAdminDashboard";
    private static final String PARAMETER_FILTER_KEYWORD = "filter_keyword";
    private static final String PARAMETER_SEARCH_NAME = "search_name";
    private static final String PARAMETER_FRONT_PROPOSALS = "calendar_front_proposals";
    private static final String MARK_PROPOSAL_COUNTS = "proposal_counts";
    private static final String PARAMETER_DEFAULT_VIEW = "calendar_default_view";
    private static final String MARK_DEFAULT_VIEW_LIST = "default_view_list";
    private static final String PREFIX_DEFAULT_VIEW_LABEL = "calendar.create_calendar.defaultView.";
    private static final String DEFAULT_VIEW_AUTO = "auto";
    private static final List<String> DEFAULT_VIEWS = List.of( "", "month", "week", "day", "list" );
    private static final String MARK_SEARCH_NAME = "search_name";
    private static final String PARAMETER_FILTER_PERIOD = "filter_period";
    private static final String PARAMETER_FILTER_CATEGORY = "filter_category";
    private static final String PARAMETER_FILTER_SORT = "filter_sort";
    private static final String MARK_FILTER_KEYWORD = "filter_keyword";
    private static final String MARK_FILTER_PERIOD = "filter_period";
    private static final String MARK_FILTER_CATEGORY = "filter_category";
    private static final String MARK_FILTER_SORT = "filter_sort";
    private static final String MARK_FILTER_ACTIVE = "filter_active";
    private static final String MARK_FILTER_PERIOD_LIST = "filter_period_list";
    private static final String MARK_FILTER_SORT_LIST = "filter_sort_list";
    private static final String MARK_FILTER_CATEGORY_LIST = "filter_category_list";
    private static final String MARK_EVENTS_TOTAL = "events_total";
    private static final String PREFIX_FILTER_PERIOD_LABEL = "calendar.event_list.period.";
    private static final String PREFIX_FILTER_SORT_LABEL = "calendar.event_list.sort.";
    private static final String KEY_FILTER_ALL = "all";
    private static final String KEY_FILTER_ALL_CATEGORIES = "calendar.event_list.filter.allCategories";
    private static final String MARK_DEMO_PAGE_ID = "demo_page_id";
    private static final String MESSAGE_CONFIRM_REMOVE_EVENT = "calendar.message.confirmRemoveEvent";
    private static final String MESSAGE_CONFIRM_REMOVE_OCCURRENCE = "calendar.message.confirmRemoveOccurrence";
    private static final String MESSAGE_CONFIRM_REMOVE_ALL_OCCURRENCE = "calendar.message.confirmRemoveAllOccurrence";
    private static final String PREFIX_DOT_COLOR_LABEL = "calendar.dot.color.";
    private static final String DEFAULT_DOT_COLORS = "blue,green,orange,purple,red";
    private static final String MESSAGE_INVALID_OCCURRENCE_NUMBER = "calendar.message.invalidOccurrenceNumber";
    private static final String MESSAGE_INVALID_TAG = "calendar.message.invalidTagsInput";
    private static final String MESSAGE_CONFIRM_REMOVE_SUBSCRIBER = "calendar.message.confirmRemoveSubscriber";
    private static final String MESSAGE_PLUGIN_DOCUMENT_UNINSTALLED = "calendar.message.plugin-document.uninstalled";
    private static final String MESSAGE_CONFIRM_MODIFY_OCCURRENCES_STATUS = "calendar.message.confirmModifyOccurrenceStatus";
    private static final String MESSAGE_INVALID_NUMBER_FORMAT = "calendar.message.invalidNumBerFormat";
    private static final String MESSAGE_ELEMENT_NOT_FOUND = "calendar.message.elementNotFound";
    private static final String MESSAGE_INVALID_TOKEN = "calendar.message.invalidToken";
    private static final String MESSAGE_CALENDAR_ACCESS_DENIED = "calendar.message.calendarAccessDenied";
    private static final String URL_MODIFY_OCCURRENCE = "jsp/admin/plugins/calendar/ManageCalendars.jsp?view=modifyOccurrence&calendar_id=%d&event_id=%d&occurrence_id=%d";
    private static final String PARAMETER_SCOPE = "scope";
    private static final String TEMPLATE_CALENDAR_VIEW = "admin/plugins/calendar/calendar_view.html";
    private static final String PROPERTY_PAGE_TITLE_CALENDAR_VIEW = "calendar.view.pageTitle";
    private static final String MARK_MOVE_TOKEN = "move_token";
    private static final String MARK_SELECTED_CALENDAR_ID = "selected_calendar_id";
    private static final String MARK_EVENT_DETAILS = "event_details";
    private static final String MARK_REGISTRATION_FORMS = "registration_forms";
    private static final String MARK_CALENDAR = "calendar";
    private static final String MARK_CALENDAR_BUTTON = "calendarButton";
    private static final String MARK_CALENDAR_BUTTON_CHOICES = "calendarButtonChoices";
    private static final String PREFIX_CALENDAR_BUTTON = "calendar_button_";
    private static final String MARK_CALENDAR_ITEMS = "calendar_items";
    private static final String MARK_SUBSCRIBER_COUNTS = "subscriber_counts";
    private static final String MARK_EVENT_COUNTS = "event_counts";
    private static final String MARK_BUTTON_ENABLED = "button_enabled";
    private static final String MARK_CATEGORY_ITEMS = "category_items";
    private static final String MESSAGE_ALL_CATEGORIES = "calendar.view.allCategories";
    private static final String MARK_DEFAULT_EVENT_DATE = "default_event_date";
    private static final String MARK_DEFAULT_TIME_START = "default_time_start";
    private static final String MARK_DEFAULT_TIME_END = "default_time_end";
    private static final Pattern PATTERN_TIME = Pattern.compile( "([01]\\d|2[0-3]):[0-5]\\d" );
    private static final String PARAMETER_ALL_DAY = "all_day";
    private static final String SCOPE_SERIES = "series";

    private String _strCurrentPageIndex;
    private int _nItemsPerPage;
    private int _nDefaultItemsPerPage;
    private MultipartItem _EventItem;
    private String[] _EventCategories;
    private String _EventDescription;
    private Map<String, Object> _mapParameters;
    @Inject
    private CategoryService _categoryService;
    @Inject
    private AgendaSubscriberService _agendaSubscriberService;
    @Inject
    private EventListService _eventListService;
    @Inject
    private CalendarService _calendarService;
    @Inject
    private CalendarEventFeedService _feedService;
    @Inject
    private EventDetailsService _eventDetailsService;
    @Inject
    private CalendarDemoService _demoService;
    @Inject
    private Models _models;

    /**
     * This class is used to handle back office management of database
     * calendars.
     */
    public CalendarJspBean( )
    {
        _nDefaultItemsPerPage = AppPropertiesService.getPropertyInt( Constants.PROPERTY_EVENTS_PER_PAGE, 5 );
    }

    /**
     * Initializes the bean for a request, then checks the workgroup of every calendar the request names: its calendar_id,
     * the calendar of its event_id and of its occurrence_id or cbx_occurrence. A calendar outside the workgroups of the user
     * is refused, as is an event or an occurrence that does not belong to the calendar or the event named with it. The
     * list of the calendars only shows the ones of the workgroups of the user; this check refuses the others when their
     * identifier is typed in an address, for every view and action of the bean
     * 
     * @param request
     *            the request
     * @param strRight
     *            the right of the feature
     * @throws AccessDeniedException
     *             if the user may not use the feature or a calendar the request names
     */
    @Override
    public void init( HttpServletRequest request, String strRight ) throws AccessDeniedException
    {
        super.init( request, strRight );
        checkRequestedCalendars( request );
    }

    /**
     * Checks that the calendars a request names belong to the workgroups of the user, and that its event and occurrences
     * belong to the calendar and the event it names
     * 
     * @param request
     *            the request
     * @throws AccessDeniedException
     *             if a calendar is outside the workgroups of the user, or an event or an occurrence belongs elsewhere
     */
    private void checkRequestedCalendars( HttpServletRequest request ) throws AccessDeniedException
    {
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        int nCalendarId = parseRequestedId( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        int nEventId = parseRequestedId( request.getParameter( Constants.PARAMETER_EVENT_ID ) );
        Set<Integer> setCalendarIds = new HashSet<>( );

        if ( nCalendarId > 0 )
        {
            setCalendarIds.add( nCalendarId );
        }

        if ( nEventId > 0 )
        {
            SimpleEvent event = CalendarHome.findEvent( nEventId, plugin );

            if ( event != null )
            {
                checkSameCalendar( nCalendarId, event.getIdCalendar( ) );
                setCalendarIds.add( event.getIdCalendar( ) );
            }
        }

        List<String> listOccurrenceIds = new ArrayList<>( );
        listOccurrenceIds.add( request.getParameter( Constants.PARAMETER_OCCURRENCE_ID ) );

        if ( request.getParameterValues( Constants.PARAMETER_CBX_OCCURRENCE ) != null )
        {
            listOccurrenceIds.addAll( Arrays.asList( request.getParameterValues( Constants.PARAMETER_CBX_OCCURRENCE ) ) );
        }

        for ( String strOccurrenceId : listOccurrenceIds )
        {
            int nOccurrenceId = parseRequestedId( strOccurrenceId );
            OccurrenceEvent occurrence = ( nOccurrenceId > 0 ) ? CalendarHome.findOccurrence( nOccurrenceId, plugin ) : null;

            if ( occurrence != null )
            {
                checkSameCalendar( nCalendarId, occurrence.getIdCalendar( ) );
                checkSameCalendar( nEventId, occurrence.getEventId( ) );
                setCalendarIds.add( occurrence.getIdCalendar( ) );
            }
        }

        if ( !setCalendarIds.isEmpty( ) )
        {
            Set<String> setAllowed = _calendarService.getAgendaResources( getUser( ), plugin ).stream( ).map( AgendaResource::getId )
                    .collect( java.util.stream.Collectors.toSet( ) );

            for ( int nId : setCalendarIds )
            {
                if ( ( CalendarHome.findAgendaResource( nId, plugin ) != null ) && !setAllowed.contains( String.valueOf( nId ) ) )
                {
                    throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
                }
            }
        }
    }

    /**
     * Refuses an identifier that differs from the one the request names
     * 
     * @param nRequested
     *            the identifier the request names, 0 or less when it names none
     * @param nActual
     *            the identifier of the calendar or the event the object belongs to
     * @throws AccessDeniedException
     *             if the request names an identifier and the object belongs to another one
     */
    private static void checkSameCalendar( int nRequested, int nActual ) throws AccessDeniedException
    {
        if ( ( nRequested > 0 ) && ( nRequested != nActual ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
    }

    /**
     * Parses an identifier of a request
     * 
     * @param strId
     *            the identifier as text
     * @return the identifier, or -1 when it is missing or not a number
     */
    private static int parseRequestedId( String strId )
    {
        return ( StringUtils.isNumeric( strId ) && ( strId.length( ) < 10 ) ) ? Integer.parseInt( strId ) : -1;
    }

    /**
     * Returns calendars management form
     * 
     * @param request The Http request
     * @return Html form
     */
    @View( value = VIEW_MANAGE_CALENDARS, defaultView = true )
    public String getManageCalendars( HttpServletRequest request )
    {
        _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );
        _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage,
                _nDefaultItemsPerPage );

        Models model = _models;
        String strSearchName = StringUtils.trimToEmpty( request.getParameter( PARAMETER_SEARCH_NAME ) );
        String strSearched = normalizeSearch( strSearchName );
        List<AgendaResource> listCalendar = _calendarService.getAgendaResources( getUser( ), getPlugin( ) ).stream( )
                .filter( calendar -> strSearched.isEmpty( ) || normalizeSearch( calendar.getName( ) ).contains( strSearched ) ).toList( );
        UrlItem urlList = new UrlItem( getHomeUrl( request ) );

        if ( !strSearchName.isEmpty( ) )
        {
            urlList.addParameter( PARAMETER_SEARCH_NAME, strSearchName );
        }

        LocalizedPaginator<AgendaResource> paginator = new LocalizedPaginator<AgendaResource>( listCalendar,
                _nItemsPerPage, urlList.getUrl( ), Constants.PARAMETER_PAGE_INDEX, _strCurrentPageIndex,
                getLocale( ) );

        boolean bPermissionAdvancedParameter = RBACService.isAuthorized( CalendarResourceIdService.RESOURCE_TYPE,
                RBAC.WILDCARD_RESOURCES_ID, CalendarResourceIdService.PERMISSION_MANAGE, (User) getUser( ) );

        model.put( Constants.MARK_CALENDARS_LIST, listCalendar );
        model.put( Constants.MARK_PAGINATOR, paginator );
        model.put( Constants.MARK_NB_ITEMS_PER_PAGE, "" + _nItemsPerPage );
        model.put( Constants.MARK_PERMISSION_ADVANCED_PARAMETER, bPermissionAdvancedParameter );
        model.put( MARK_SEARCH_NAME, strSearchName );
        putCalendarCounts( model, paginator.getPageItems( ) );

        return getPage( Constants.PROPERTY_PAGE_TITLE_MANAGE_CALENDARS, TEMPLATE_MANAGE_CALENDARS );
    }

    /**
     * Return Calendar advanced parameters
     * @param request The Http request
     * @return Html form
     * @throws AccessDeniedException If the user is not authorized to access the
     *             feature
     */
    @View( value = VIEW_MANAGE_ADVANCED_PARAMETERS, securityTokenAction = ACTION_MODIFY_CALENDAR_PARAMETER_VALUES )
    public String getManageAdvancedParameters( HttpServletRequest request ) throws AccessDeniedException
    {
        if ( !RBACService.isAuthorized( CalendarResourceIdService.RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                CalendarResourceIdService.PERMISSION_MANAGE, (User) getUser( ) ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
        Models model = _models;
        model.put( Constants.MARK_CALENDAR_PARAMETERS, _calendarService.getCalendarParameters( getPlugin( ) ) );
        model.put( CalendarLibraryService.MARK_CALENDAR_LIBRARY, CalendarLibraryService.getLibrary( getPlugin( ) ) );
        model.put( CalendarLibraryService.MARK_CALENDAR_LIBRARY_LIST, CalendarLibraryService.getLibraryList( getLocale( ) ) );
        return getPage( Constants.PROPERTY_PAGE_TITLE_MANAGE_CALENDARS, TEMPLATE_MANAGE_ADVANCED_PARAMETERS );
    }

    /**
     * Returns the Calendar creation form
     * 
     * @param request The Http request
     * @return Html creation form
     */
    @View( VIEW_CREATE_CALENDAR )
    public String getCreateCalendar( HttpServletRequest request )
    {

        Models model = _models;
        ReferenceList ref = null;

        try
        {
            AdminUser adminUser = getUser( );
            Locale locale = getLocale( );
            ref = AdminWorkgroupService.getUserWorkgroups( adminUser, locale );
        }
        catch ( RuntimeException e )
        {
            throw new AppException( e.getMessage( ), e );
        }

        model.put( Constants.MARK_DOTS_LIST, getDotsList( ) );

        model.put( MARK_DEFAULT_VIEW_LIST, getDefaultViewList( ) );
        model.put( Constants.MARK_WORKGROUPS_LIST, ref );
        model.put( Constants.MARK_ROLES_LIST, RoleHome.getRolesList( ) );
        model.put( MARK_CALENDAR_BUTTON, new CalendarButtonConfig( ) );
        model.put( MARK_CALENDAR_BUTTON_CHOICES, CalendarButtonService.getChoices( ) );

        return getPage( Constants.PROPERTY_PAGE_TITLE_CREATE_CALENDAR, TEMPLATE_CREATE_CALENDAR );
    }

    /**
     * Process Calendar creation
     * 
     * @param request The Http request
     * @return URL
     */
    @Action( ACTION_CREATE_CALENDAR )
    public String doCreateCalendar( HttpServletRequest request )
    {
        String strJspReturn = StringUtils.EMPTY;
        String strName = request.getParameter( Constants.PARAMETER_CALENDAR_NAME );
        String strImage = getValidDotColor( request.getParameter( Constants.PARAMETER_CALENDAR_IMAGE ) );
        String strWorkgroup = request.getParameter( Constants.PARAMETER_WORKGROUP );
        String strIsNotify = request.getParameter( Constants.PARAMETER_CALENDAR_NOTIFICATION );
        boolean bIsNotify = ( strIsNotify != null );
        String strPeriodValidity = request.getParameter( Constants.PARAMETER_CALENDAR_PERIOD );

        // Mandatory field
        if ( StringUtils.isNotBlank( strName ) && StringUtils.isNotBlank( strImage )
                && !( bIsNotify && StringUtils.isBlank( strPeriodValidity ) ) )
        {
            int nPeriodValidity = -1;
            if ( bIsNotify )
            {
                if ( strPeriodValidity.matches( Constants.REG_NUMBER ) )
                {
                    nPeriodValidity = StringUtil.getIntValue( strPeriodValidity, -1 );
                }
                else
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT,
                            AdminMessage.TYPE_STOP ) );
                }
            }

            AgendaResource calendar = new AgendaResource( );
            calendar.setName( strName );
            calendar.setEventImage( strImage );
            calendar.setEventPrefix( request.getParameter( Constants.PARAMETER_CALENDAR_PREFIX ) );
            calendar.setRole( request.getParameter( Constants.PARAMETER_CALENDAR_ROLE ) );
            calendar.setWorkgroup( strWorkgroup );
            calendar.setRoleManager( request.getParameter( Constants.PARAMETER_CALENDAR_ROLE_MANAGER ) );
            calendar.setNotify( bIsNotify );
            calendar.setFrontProposals( request.getParameter( PARAMETER_FRONT_PROPOSALS ) != null );
            calendar.setDefaultView( getValidDefaultView( request.getParameter( PARAMETER_DEFAULT_VIEW ) ) );
            calendar.setPeriodValidity( bIsNotify ? nPeriodValidity : -1 );
            _calendarService.doCreateAgenda( calendar, getPlugin( ) );
            CalendarButtonHome.save( Integer.parseInt( calendar.getId( ) ), readCalendarButton( request ), getPlugin( ) );

            strJspReturn = getViewUrl( VIEW_MANAGE_CALENDARS );
        }
        else
        {
            strJspReturn = AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS,
                    AdminMessage.TYPE_STOP );
        }
        return redirect( request, strJspReturn );
    }

    /**
     * Returns the form for calendar modification
     * 
     * @param request The Http request
     * @return Html form
     * @throws AccessDeniedException if the calendar is not in the workgroups of the user
     */
    @View( VIEW_MODIFY_CALENDAR )
    public String getModifyCalendar( HttpServletRequest request ) throws AccessDeniedException
    {
        if ( StringUtils.isNumeric( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) ) && ( getAuthorizedCalendar( request ) == null ) )
        {
            return redirectNotFound( request );
        }

        String strHtml = StringUtils.EMPTY;

        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            if ( CalendarHome.findAgendaResource( nCalendarId, getPlugin( ) ) == null )
            {
                return redirectNotFound( request );
            }
            String strSortEvents = request.getParameter( Constants.PARAMETER_SORT_EVENTS );
            int nSortEvents = Constants.SORT_ASC;
            if ( StringUtils.isNotBlank( strSortEvents ) && StringUtils.isNumeric( strSortEvents ) )
            {
                nSortEvents = Integer.parseInt( strSortEvents );
            }

            Models model = _models;
            model.put( Constants.MARK_CALENDAR, _calendarService.getAgendaResource( nCalendarId ) );
            model.put( Constants.MARK_WORKGROUPS_LIST,
                    AdminWorkgroupService.getUserWorkgroups( getUser( ), getLocale( ) ) );
            model.put( Constants.MARK_DOTS_LIST, getDotsList( ) );

            model.put( MARK_DEFAULT_VIEW_LIST, getDefaultViewList( ) );
            model.put( MARK_CALENDAR_BUTTON, CalendarButtonHome.findByCalendar( nCalendarId, getPlugin( ) ) );
            model.put( MARK_CALENDAR_BUTTON_CHOICES, CalendarButtonService.getChoices( ) );

            List<SimpleEvent> listEvents = _eventListService.getSimpleEvents( nCalendarId, nSortEvents );

            //paginator parameters
            _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX,
                    _strCurrentPageIndex );
            _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage,
                    _nDefaultItemsPerPage );

            UrlItem url = new UrlItem( getViewFullUrl( VIEW_MODIFY_CALENDAR ) );
            url.addParameter( Constants.PARAMETER_CALENDAR_ID, nCalendarId );
            url.addParameter( Constants.PARAMETER_SORT_EVENTS, nSortEvents );

            LocalizedPaginator<SimpleEvent> paginator = new LocalizedPaginator<SimpleEvent>( listEvents,
                    _nItemsPerPage, url.getUrl( ), Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex, getLocale( ) );

            model.put( Constants.MARK_PAGINATOR, paginator );
            model.put( Constants.MARK_NB_ITEMS_PER_PAGE, Integer.toString( _nItemsPerPage ) );
            model.put( Constants.MARK_EVENTS_LIST, paginator.getPageItems( ) );
            model.put( Constants.MARK_EVENTS_SORT_LIST, getSortEventList( ) );
            model.put( Constants.MARK_DEFAULT_SORT_EVENT, nSortEvents );
            model.put( Constants.MARK_ROLES_LIST, RoleHome.getRolesList( ) );

            strHtml = getPage( Constants.PROPERTY_PAGE_TITLE_MODIFY_CALENDAR, TEMPLATE_MODIFY_CALENDAR );
        }
        else
        {
            strHtml = getManageCalendars( request );
        }
        return strHtml;
    }

    /**
     * Process the Calendar modifications
     * 
     * @param request The Http request
     * @return Html form
     * @throws AccessDeniedException if the calendar is not in the workgroups of the user
     */
    @Action( ACTION_MODIFY_CALENDAR )
    public String doModifyCalendar( HttpServletRequest request ) throws AccessDeniedException
    {
        if ( StringUtils.isNumeric( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) ) && ( getAuthorizedCalendar( request ) == null ) )
        {
            return redirectNotFound( request );
        }

        String strJspReturn = StringUtils.EMPTY;

        String strName = request.getParameter( Constants.PARAMETER_CALENDAR_NAME );
        String strWorkgroup = request.getParameter( Constants.PARAMETER_WORKGROUP );
        String strImage = getValidDotColor( request.getParameter( Constants.PARAMETER_CALENDAR_IMAGE ) );
        String strIsNotify = request.getParameter( Constants.PARAMETER_CALENDAR_NOTIFICATION );
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        boolean bIsNotify = ( strIsNotify != null );
        String strPeriodValidity = request.getParameter( Constants.PARAMETER_CALENDAR_PERIOD );
        int nPeriodValidity = -1;

        // Mandatory field
        if ( StringUtils.isNotBlank( strName ) && StringUtils.isNotBlank( strImage )
                && StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && !( bIsNotify && StringUtils.isBlank( strPeriodValidity ) ) )
        {
            if ( bIsNotify )
            {
                if ( strPeriodValidity.matches( Constants.REG_NUMBER ) )
                {
                    nPeriodValidity = StringUtil.getIntValue( strPeriodValidity, -1 );
                }
                else
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT,
                            AdminMessage.TYPE_STOP ) );
                }
            }

            int nCalendarId = Integer.parseInt( strCalendarId );

            AgendaResource calendar = _calendarService.getAgendaResource( nCalendarId );

            calendar.setName( strName );
            calendar.setEventImage( strImage );
            calendar.setEventPrefix( request.getParameter( Constants.PARAMETER_CALENDAR_PREFIX ) );
            calendar.setRole( request.getParameter( Constants.PARAMETER_CALENDAR_ROLE ) );
            calendar.setRoleManager( request.getParameter( Constants.PARAMETER_CALENDAR_ROLE_MANAGER ) );
            calendar.setWorkgroup( strWorkgroup );
            calendar.setNotify( bIsNotify );
            calendar.setFrontProposals( request.getParameter( PARAMETER_FRONT_PROPOSALS ) != null );
            calendar.setDefaultView( getValidDefaultView( request.getParameter( PARAMETER_DEFAULT_VIEW ) ) );
            calendar.setPeriodValidity( nPeriodValidity );
            _calendarService.doModifyAgenda( calendar, getPlugin( ) );
            CalendarButtonHome.save( nCalendarId, readCalendarButton( request ), getPlugin( ) );

            strJspReturn = getViewUrl( VIEW_MANAGE_CALENDARS );
        }
        else
        {
            strJspReturn = AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS,
                    AdminMessage.TYPE_STOP );
        }

        return redirect( request, strJspReturn );
    }

    /**
     * Returns the confirmation to remove the calendar
     * 
     * @param request The Http request
     * @return the confirmation page
     */
    @View( value = VIEW_CONFIRM_REMOVE_CALENDAR, securityTokenAction = ACTION_REMOVE_CALENDAR )
    public String getConfirmRemoveCalendar( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_CALENDAR ) );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID,
                Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) ) );

        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_CALENDAR, url.getUrl( ),
                AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Remove a calendar
     * 
     * @param request The Http request
     * @return Html form
     */
    @Action( ACTION_REMOVE_CALENDAR )
    public String doRemoveCalendar( HttpServletRequest request )
    {
        String strJspReturn = StringUtils.EMPTY;
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            List<SimpleEvent> listEvents = _eventListService.getSimpleEvents( nCalendarId, Constants.SORT_ASC );

            for ( SimpleEvent event : listEvents )
            {
                List<OccurrenceEvent> listOccurencesEvent = _eventListService.getOccurrenceEvents( nCalendarId,
                        event.getId( ), Constants.SORT_ASC, getPlugin( ) );

                for ( OccurrenceEvent occ : listOccurencesEvent )
                {
                    IndexationService.addIndexerAction( Integer.toString( occ.getId( ) ),
                            AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                            IndexerAction.TASK_DELETE );
                    CalendarIndexerUtils.addIndexerAction( occ.getId( ), IndexerAction.TASK_DELETE );
                }
                _eventListService.doRemoveEvent( nCalendarId, event.getId( ), null, getPlugin( ) );
            }

            _calendarService.doRemoveAgenda( nCalendarId, getPlugin( ) );

            // Go to the parent page
            strJspReturn = getViewUrl( VIEW_MANAGE_CALENDARS );
        }
        else
        {
            strJspReturn = AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT,
                    AdminMessage.TYPE_STOP );
        }

        return redirect( request, strJspReturn );
    }

    /**
     * Returns the Event creation form
     * 
     * @param request The Http request
     * @return Html creation form
     */
    @View( VIEW_CREATE_EVENT )
    public String getCreateEvent( HttpServletRequest request )
    {
        String strHtml = StringUtils.EMPTY;
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );

            //The defaut number of day for the list
            int nDays = 1;

            //Retrieve category list
            Collection<Category> categoryList = _categoryService.getCategories( getPlugin( ) );

            Models model = _models;

            String strBooleanTimeSpan = Constants.TRUE;
            model.put( MARK_INTERVAL_TIME_SPAN, strBooleanTimeSpan );
            model.put( Constants.MARK_CALENDAR_ID, nCalendarId );
            java.time.LocalDate defaultDay = CalendarEventFeedService.parseDate( request.getParameter( Constants.PARAMETER_EVENT_DATE ) );
            model.put( MARK_DEFAULT_EVENT_DATE, ( defaultDay != null ) ? Utils.toDate( defaultDay ) : StringUtils.EMPTY );
            // The hours of a range selected in the week or the day view of the calendar
            model.put( MARK_DEFAULT_TIME_START, readTime( request, Constants.PARAMETER_EVENT_TIME_START ) );
            model.put( MARK_DEFAULT_TIME_END, readTime( request, Constants.PARAMETER_EVENT_TIME_END ) );
            model.put( MARK_EVENT_DETAILS, new EventDetails( ) );
            model.put( MARK_REGISTRATION_FORMS, _eventDetailsService.getRegistrationForms( ) );
            model.put( Constants.MARK_LOCALE, getLocale( ).getLanguage( ) );
            model.put( MARK_INTERVAL_LIST, getIntervalList( request ) );
            model.put( MARK_NUMBER_DAYS, nDays );
            model.put( MARK_INTERVAL_LIST, getIntervalList( request ) );
            model.put( MARK_TOP_EVENT_LIST, getTopEventList( ) );
            model.put( MARK_EMAIL_NOTIFY, AppPropertiesService.getProperty( PROPERTY_EMAIL_NOTIFY ) );
            model.put( MARK_TOP_EVENT_DEFAULT, AppPropertiesService.getProperty( PROPERTY_TOP_EVENT_DEFAULT ) );
            model.put( MARK_TOP_EVENT_DEFAULT, AppPropertiesService.getProperty( PROPERTY_TOP_EVENT_DEFAULT ) );
            model.put( MARK_INSERT_SERVICE_PAGE, getViewUrl( VIEW_INSERT_SERVICE ) );
            model.put( MARK_INSERT_SERVICE_LINK_PAGE, getViewUrl( VIEW_INSERT_LINK_SERVICE ) );
            model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );
            model.put( Constants.MARK_CATEGORY_LIST, new HashMap<String, Object>( ) );
            model.put( Constants.MARK_CATEGORY_DEFAULT_LIST, categoryList );

            strHtml = getPage( Constants.PROPERTY_PAGE_TITLE_CREATE_EVENT, TEMPLATE_CREATE_EVENT );
        }
        else
        {
            strHtml = getManageCalendars( request );
        }
        return strHtml;
    }

    /**
     * Return the list of Event linked with an agenda
     * @param request The request
     * @return The list of Event linked with an agenda
     */
    @View( VIEW_EVENT_LIST )
    public String getEventList( HttpServletRequest request )
    {
        String strHtml = StringUtils.EMPTY;
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            if ( CalendarHome.findAgendaResource( nCalendarId, getPlugin( ) ) == null )
            {
                return redirectNotFound( request );
            }

            Models model = _models;
            model.put( Constants.MARK_CALENDAR_ID, nCalendarId );

            String strKeyword = StringUtils.trimToEmpty( request.getParameter( PARAMETER_FILTER_KEYWORD ) );
            String strPeriod = EventListFilter.getPeriod( request.getParameter( PARAMETER_FILTER_PERIOD ) );
            int nFilterCategoryId = NumberUtils.toInt( request.getParameter( PARAMETER_FILTER_CATEGORY ), -1 );
            String strSort = EventListFilter.getSort( request.getParameter( PARAMETER_FILTER_SORT ),
                    request.getParameter( Parameters.SORTED_ATTRIBUTE_NAME ), request.getParameter( Parameters.SORTED_ASC ) );
            List<SimpleEvent> listAllEvents = _eventListService.getSimpleEvents( nCalendarId, Constants.SORT_ASC );
            List<SimpleEvent> listEvents = EventListFilter.apply( listAllEvents, strKeyword, strPeriod,
                    getEventIdsOfCategory( listAllEvents, nFilterCategoryId ), strSort, Utils.toDate( LocalDate.now( ) ), getLocale( ) );

            //paginator parameters
            _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX,
                    _strCurrentPageIndex );
            _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage,
                    _nDefaultItemsPerPage );

            UrlItem url = new UrlItem( getViewFullUrl( VIEW_EVENT_LIST ) );
            url.addParameter( Constants.PARAMETER_PLUGIN_NAME, Constants.PLUGIN_NAME );
            url.addParameter( Constants.PARAMETER_CALENDAR_ID, nCalendarId );
            url.addParameter( PARAMETER_FILTER_KEYWORD, strKeyword );
            url.addParameter( PARAMETER_FILTER_PERIOD, strPeriod );
            url.addParameter( PARAMETER_FILTER_CATEGORY, nFilterCategoryId );
            url.addParameter( PARAMETER_FILTER_SORT, strSort );

            LocalizedPaginator<SimpleEvent> paginator = new LocalizedPaginator<SimpleEvent>( listEvents,
                    _nItemsPerPage, url.getUrl( ), Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex, getLocale( ) );

            model.put( Constants.MARK_CALENDAR, CalendarHome.findAgendaResource( nCalendarId, getPlugin( ) ) );
            model.put( Constants.MARK_PAGINATOR, paginator );
            model.put( Constants.MARK_NB_ITEMS_PER_PAGE, Integer.toString( _nItemsPerPage ) );
            model.put( Constants.MARK_EVENTS_LIST, paginator.getPageItems( ) );
            model.put( MARK_FILTER_KEYWORD, strKeyword );
            model.put( MARK_FILTER_PERIOD, strPeriod );
            model.put( MARK_FILTER_CATEGORY, nFilterCategoryId );
            model.put( MARK_FILTER_SORT, strSort );
            model.put( MARK_FILTER_ACTIVE, !strKeyword.isEmpty( ) || !strPeriod.isEmpty( ) || nFilterCategoryId > 0 );
            model.put( MARK_FILTER_PERIOD_LIST, getLabelledList( EventListFilter.PERIODS, PREFIX_FILTER_PERIOD_LABEL ) );
            model.put( MARK_FILTER_SORT_LIST, getLabelledList( EventListFilter.SORTS, PREFIX_FILTER_SORT_LABEL ) );
            model.put( MARK_FILTER_CATEGORY_LIST, getFilterCategoryList( ) );
            model.put( MARK_EVENTS_TOTAL, listAllEvents.size( ) );

            strHtml = getPage( Constants.PROPERTY_PAGE_TITLE_MANAGE_EVENTS, TEMPLATE_EVENT_LIST );
        }
        else
        {
            strHtml = getManageCalendars( request );
        }
        return strHtml;
    }

    /**
     * Return the list of occurrence linked with an Event
     * @param request The request
     * @return The list of occurrence linked with an Event
     */
    @View( value = VIEW_OCCURRENCE_LIST, securityTokenAction = ACTION_MODIFY_OCCURRENCE_PERIODICITY )
    public String getOccurrenceList( HttpServletRequest request )
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            int nEventId = Integer.parseInt( strEventId );

            SimpleEvent event = _eventListService.getEvent( nEventId, getPlugin( ) );
            if ( event == null )
            {
                return redirectNotFound( request );
            }
            int nDays = _eventListService.getRepititionDays( nEventId, getPlugin( ) );
            Models model = _models;
            List<OccurrenceEvent> listOccurrences = null;

            // SORT
            String strSortedAttributeName = request.getParameter( Parameters.SORTED_ATTRIBUTE_NAME );
            String strAscSort = request.getParameter( Parameters.SORTED_ASC );
            if ( StringUtils.isNotBlank( strSortedAttributeName ) )
            {
                boolean bIsAscSort = Boolean.parseBoolean( strAscSort );
                int nIsAscSort = bIsAscSort ? Constants.SORT_ASC : Constants.SORT_DESC;
                listOccurrences = _eventListService
                        .getOccurrenceEvents( nCalendarId, nEventId, nIsAscSort, getPlugin( ) );
                if ( !Constants.PARAMETER_DATE.equals( strSortedAttributeName ) )
                {
                    Collections.sort( listOccurrences, new AttributeComparator( strSortedAttributeName, bIsAscSort ) );
                }
            }
            else
            {
                listOccurrences = _eventListService.getOccurrenceEvents( nCalendarId, nEventId, Constants.SORT_ASC,
                        getPlugin( ) );
            }

            //paginator parameters
            _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX,
                    _strCurrentPageIndex );
            _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage,
                    _nDefaultItemsPerPage );

            UrlItem url = new UrlItem( getViewFullUrl( VIEW_OCCURRENCE_LIST ) );
            url.addParameter( Constants.PARAMETER_PLUGIN_NAME, Constants.PLUGIN_NAME );
            url.addParameter( Constants.PARAMETER_CALENDAR_ID, nCalendarId );
            url.addParameter( Constants.PARAMETER_EVENT_ID, event.getId( ) );
            if ( StringUtils.isNotBlank( strSortedAttributeName ) )
            {
                url.addParameter( Parameters.SORTED_ATTRIBUTE_NAME, strSortedAttributeName );
            }

            if ( StringUtils.isNotBlank( strAscSort ) )
            {
                url.addParameter( Parameters.SORTED_ASC, strAscSort );
            }

            LocalizedPaginator<OccurrenceEvent> paginator = new LocalizedPaginator<OccurrenceEvent>( listOccurrences,
                    _nItemsPerPage, url.getUrl( ), Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex, getLocale( ) );

            model.put( Constants.MARK_EVENT, event );
            model.put( Constants.MARK_CALENDAR_ID, nCalendarId );
            model.put( Constants.MARK_PAGINATOR, paginator );
            model.put( Constants.MARK_NB_ITEMS_PER_PAGE, "" + _nItemsPerPage );
            model.put( Constants.MARK_OCCURRENCES_LIST, paginator.getPageItems( ) );
            model.put( Constants.MARK_EVENTS_SORT_LIST, getSortEventList( ) );
            model.put( MARK_INTERVAL_LIST, getIntervalList( request ) );
            model.put( MARK_NUMBER_DAYS, nDays );
            model.put( MARK_EVENT_STATUS_LIST, getStatusList( request ) );
            model.put( MARK_DEFAULT_STATUS, AppPropertiesService.getProperty( Constants.PROPERTY_EVENT_STATUS_DEFAULT ) );

            return getPage( Constants.PROPERTY_PAGE_TITLE_MANAGE_EVENTS, TEMPLATE_OCCURRENCE_LIST );
        }
        return getManageCalendars( request );
    }

    /**
     * Process Event creation
     * 
     * @param request The Http request
     * @return URL
     */
    @Action( ACTION_CREATE_EVENT )
    public String doCreateEvent( HttpServletRequest request )
    {
        String strJspReturn = StringUtils.EMPTY;
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strPeriodicity = request.getParameter( Constants.PARAMETER_PERIODICITY );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strPeriodicity ) && StringUtils.isNumeric( strPeriodicity ) )
        {
            //Retrieving parameters from form
            int nCalendarId = Integer.parseInt( strCalendarId );
            int nPeriodicity = Integer.parseInt( strPeriodicity );
            String strEventDateEnd = request.getParameter( Constants.PARAMETER_EVENT_DATE_END );
            String strTimeStart = request.getParameter( Constants.PARAMETER_EVENT_TIME_START );
            String strTimeEnd = request.getParameter( Constants.PARAMETER_EVENT_TIME_END );
            String strOccurrence = request.getParameter( Constants.PARAMETER_OCCURRENCE );
            String strEventTitle = request.getParameter( Constants.PARAMETER_EVENT_TITLE );
            String strDate = request.getParameter( Constants.PARAMETER_EVENT_DATE );
            String strNotify = request.getParameter( Constants.PARAMETER_NOTIFY );

            //Retrieve the features of an event from form
            String strDescription = request.getParameter( Constants.PARAMETER_DESCRIPTION );
            String strEventTags = request.getParameter( Constants.PARAMETER_EVENT_TAGS ).trim( );
            String strLocationAddress = request.getParameter( Constants.PARAMETER_EVENT_LOCATION_ADDRESS ).trim( );
            String strLocationTown = request.getParameter( Constants.PARAMETER_LOCATION_TOWN ).trim( );
            String strLocation = request.getParameter( Constants.PARAMETER_LOCATION ).trim( );
            String strLocationZip = request.getParameter( Constants.PARAMETER_LOCATION_ZIP ).trim( );
            String strLinkUrl = request.getParameter( Constants.PARAMETER_EVENT_LINK_URL ).trim( );
            String strDocumentId = StringUtils.trimToEmpty( request.getParameter( Constants.PARAMETER_EVENT_DOCUMENT_ID ) );
            String strPageUrl = StringUtils.trimToEmpty( request.getParameter( Constants.PARAMETER_EVENT_PAGE_URL ) );
            String strTopEvent = request.getParameter( Constants.PARAMETER_EVENT_TOP_EVENT ).trim( );
            String strMapUrl = request.getParameter( Constants.PARAMETER_EVENT_MAP_URL ).trim( );

            //Retrieving the excluded days
            String[] arrayExcludedDays = request.getParameterValues( Constants.PARAMETER_EXCLUDED_DAYS );

            MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;
            MultipartItem item = mRequest.getFile( Constants.PARAMETER_EVENT_IMAGE );

            String[] arrayCategory = request.getParameterValues( Constants.PARAMETER_CATEGORY );

            //Categories
            List<Category> listCategories = new ArrayList<Category>( );

            if ( arrayCategory != null )
            {
                for ( String strIdCategory : arrayCategory )
                {
                    Category category = CategoryHome.find( Integer.parseInt( strIdCategory ), getPlugin( ) );

                    if ( category != null )
                    {
                        listCategories.add( category );
                    }
                }
            }

            String[] strTabEventTags = null;

            // Mandatory field
            if ( StringUtils.isBlank( strDate ) || StringUtils.isBlank( strEventTitle )
                    || StringUtils.isBlank( strDescription ) )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
            }

            if ( StringUtils.isNotBlank( strEventTags ) && strEventTags.length( ) > 0 )
            {
                //Test if there aren't special characters in tag list
                boolean isAllowedExp = Pattern.matches( PROPERTY_TAGS_REGEXP, strEventTags );

                if ( !isAllowedExp )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_TAG, AdminMessage.TYPE_STOP ) );
                }

                strTabEventTags = strEventTags.split( "\\s" );
            }

            //Convert the date in form to a java.util.Date object
            Date dateEvent = parseFormDate( strDate );

            if ( ( dateEvent == null ) || !Utils.isValidDate( dateEvent ) )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                        AdminMessage.TYPE_STOP ) );
            }

            Date dateEndEvent = null;

            if ( StringUtils.isNotBlank( strEventDateEnd ) )
            {
                dateEndEvent = parseFormDate( strEventDateEnd );

                if ( ( dateEndEvent == null ) || !Utils.isValidDate( dateEndEvent ) )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                            AdminMessage.TYPE_STOP ) );
                }
            }

            //the number of occurrence is 1 by default
            int nOccurrence = 1;

            if ( ( strOccurrence.length( ) > 0 ) && !strOccurrence.equals( "" ) )
            {
                try
                {
                    nOccurrence = Integer.parseInt( strOccurrence );
                }
                catch ( NumberFormatException e )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_OCCURRENCE_NUMBER,
                            AdminMessage.TYPE_STOP ) );
                }
            }

            int nDocumentId = -1;

            if ( ( strDocumentId.length( ) > 0 ) && !strDocumentId.equals( "" ) )
            {
                try
                {
                    nDocumentId = Integer.parseInt( strDocumentId );
                }
                catch ( NumberFormatException e )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_OCCURRENCE_NUMBER,
                            AdminMessage.TYPE_STOP ) );
                }
            }

            if ( !Utils.checkTime( strTimeStart ) || !Utils.checkTime( strTimeEnd ) )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_TIMEFORMAT,
                        AdminMessage.TYPE_STOP ) );
            }

            //If a date end is chosen
            if ( ( Integer.parseInt( request.getParameter( Constants.PARAMETER_RADIO_PERIODICITY ) ) == 1 )
                    && StringUtils.isNotBlank( strEventDateEnd ) )
            {
                if ( dateEndEvent.before( dateEvent ) )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATE_END_BEFORE,
                            AdminMessage.TYPE_STOP ) );
                }
                nPeriodicity = 0;
                nOccurrence = Utils.getOccurrenceWithinTwoDates( dateEvent, dateEndEvent, arrayExcludedDays );
            }
            else if ( ( Integer.parseInt( request.getParameter( Constants.PARAMETER_RADIO_PERIODICITY ) ) == 1 )
                    && StringUtils.isBlank( strEventDateEnd ) )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                        AdminMessage.TYPE_STOP ) );
            }

            //If a date end is not chosen
            if ( ( Integer.parseInt( request.getParameter( Constants.PARAMETER_RADIO_PERIODICITY ) ) == 0 )
                    && StringUtils.isBlank( strEventDateEnd ) )
            {
                dateEndEvent = Utils.getDateForward( dateEvent, nPeriodicity, nOccurrence, arrayExcludedDays );
                if ( dateEndEvent.before( dateEvent ) )
                {
                    nOccurrence = 0;
                    dateEndEvent = dateEvent;
                }
            }

            SimpleEvent event = new SimpleEvent( );
            event.setDate( dateEvent );
            event.setDateEnd( dateEndEvent );
            event.setDateTimeStart( strTimeStart );
            event.setDateTimeEnd( strTimeEnd );
            event.setTitle( strEventTitle );
            event.setOccurrence( nOccurrence );
            event.setPeriodicity( nPeriodicity );
            event.setDescription( strDescription );

            if ( item.getSize( ) == 0 )
            {
                HttpSession session = request.getSession( true );
                if ( session.getAttribute( ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_CONTENT_FILE ) != null
                        && session.getAttribute( ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_MIME_TYPE_FILE ) != null )
                {
                    ImageResource imageResource = new ImageResource( );
                    imageResource.setImage( (byte[]) session
                            .getAttribute( ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_CONTENT_FILE ) );
                    imageResource.setMimeType( (String) session
                            .getAttribute( ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_MIME_TYPE_FILE ) );
                    event.setImageResource( imageResource );
                    session.removeAttribute( ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_CONTENT_FILE );
                    session.removeAttribute( ATTRIBUTE_MODULE_DOCUMENT_TO_CALENDAR_MIME_TYPE_FILE );
                }
            }

            if ( item.getSize( ) >= 1 )
            {
                ImageResource imageResource = new ImageResource( );
                imageResource.setImage( item.get( ) );
                imageResource.setMimeType( item.getContentType( ) );
                event.setImageResource( imageResource );
            }

            event.setTags( strTabEventTags );
            event.setLocationAddress( strLocationAddress );
            event.setLocation( strLocation );
            event.setLocationTown( strLocationTown );
            event.setLocationZip( strLocationZip );
            event.setLinkUrl( strLinkUrl );
            event.setPageUrl( strPageUrl );
            event.setTopEvent( Integer.parseInt( strTopEvent ) );
            event.setMapUrl( strMapUrl );
            event.setDocumentId( nDocumentId );
            event.setListCategories( listCategories );
            event.setExcludedDays( arrayExcludedDays );
            event.setIdCalendar( nCalendarId );

            _eventListService.doAddEvent( event, null, getPlugin( ) );
            EventDetailsHome.save( _eventDetailsService.readDetails( request, event.getId( ) ), getPlugin( ) );

            List<OccurrenceEvent> listOccurencesEvent = _eventListService.getOccurrenceEvents( nCalendarId,
                    event.getId( ), Constants.SORT_ASC, getPlugin( ) );

            for ( OccurrenceEvent occ : listOccurencesEvent )
            {
                IndexationService.addIndexerAction( Integer.toString( occ.getId( ) ),
                        AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                        IndexerAction.TASK_CREATE );
                CalendarIndexerUtils.addIndexerAction( occ.getId( ), IndexerAction.TASK_CREATE );
            }

            /*
             * IndexationService.addIndexerAction( Constants.EMPTY_STRING +
             * nCalendarId
             * ,AppPropertiesService.getProperty(
             * CalendarIndexer.PROPERTY_INDEXER_NAME ),
             * IndexerAction.TASK_CREATE );
             */
            boolean isNotify = Boolean.parseBoolean( strNotify );

            if ( isNotify )
            {
                int nSubscriber = _agendaSubscriberService.getSubscriberNumber( nCalendarId, getPlugin( ) );

                if ( nSubscriber > 0 )
                {
                    Collection<CalendarSubscriber> calendarSubscribers = _agendaSubscriberService.getSubscribers(
                            nCalendarId, getPlugin( ) );
                    _agendaSubscriberService.sendSubscriberMail( request, calendarSubscribers, event, nCalendarId );
                }
            }

            strJspReturn = getEventListUrl( nCalendarId, null );
        }
        else
        {
            strJspReturn = AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT,
                    AdminMessage.TYPE_STOP );
        }
        return redirect( request, strJspReturn );

    }

    /**
     * Returns the form for event modification
     * 
     * @param request The Http request
     * @return Html form
     */
    @View( VIEW_MODIFY_EVENT )
    public String getModifyEvent( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID, Constants.PARAMETER_EVENT_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }


        //TODO add new parameters

        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        int nIdEvent = Integer.parseInt( strEventId );

        int nDays = CalendarHome.getRepetitionDays( nIdEvent, getPlugin( ) );

        SimpleEvent event = CalendarHome.findEvent( nIdEvent, getPlugin( ) );
        if ( event == null )
        {
            return redirectNotFound( request );
        }
        event.setImageUrl( EventImageResourceService.getResourceImageEvent( event.getId( ) ) );

        Models model = _models;

        String strBooleanTimeSpan = "TRUE";

        if ( nDays <= 0 )
        {
            strBooleanTimeSpan = "FALSE";
        }

        // Generate the list of occurrences
        String strSortEvents = request.getParameter( Constants.PARAMETER_SORT_EVENTS );
        strSortEvents = ( strSortEvents != null ) ? strSortEvents : "1";

        Collection<Category> categoryDefaultList = CategoryHome.findAll( getPlugin( ) );
        Collection<Category> categoryList = event.getListCategories( );
        HashMap<String, Object> mapCategorySelected = new HashMap<String, Object>( );

        for ( Category catSelected : categoryList )
        {
            mapCategorySelected.put( Constants.EMPTY_STRING + catSelected.getId( ), catSelected.getName( ) );
        }

        //Retrieve event category
        model.put( Constants.MARK_CATEGORY_DEFAULT_LIST, categoryDefaultList );
        //Retrieve category list
        model.put( Constants.MARK_CATEGORY_LIST, mapCategorySelected );

        model.put( MARK_INTERVAL_TIME_SPAN, strBooleanTimeSpan );
        // getDateForward() compute the date end depending on the periodicity and the occurrence number
        /*
         * model.put( Constants.MARK_EVENT_DATE_END,
         * Utils.getDateForward( event.getDate( ), event.getPeriodicity( ),
         * event.getOccurrence( ) ) );
         */
        model.put( Constants.MARK_EVENT_DATE_END, event.getDateEnd( ) );
        model.put( Constants.MARK_EVENT, event );
        model.put( MARK_EVENT_DETAILS, EventDetailsHome.findByEvent( event.getId( ), getPlugin( ) ) );
        model.put( MARK_REGISTRATION_FORMS, _eventDetailsService.getRegistrationForms( ) );
        model.put( Constants.MARK_CALENDAR_ID, nCalendarId );
        model.put( Constants.MARK_DEFAULT_SORT_EVENT, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) );
        model.put( Constants.MARK_LOCALE, getLocale( ).getLanguage( ) );
        model.put( MARK_INTERVAL_LIST, getIntervalList( request ) );
        model.put( MARK_NUMBER_DAYS, nDays );
        model.put( Constants.MARK_EVENTS_SORT_LIST, getSortEventList( ) );
        model.put( Constants.MARK_DEFAULT_SORT_EVENT, Integer.parseInt( strSortEvents ) );
        model.put( MARK_TOP_EVENT_LIST, getTopEventList( ) );
        model.put( MARK_TOP_EVENT_DEFAULT, event.getTopEvent( ) );
        model.put( MARK_INSERT_SERVICE_PAGE, getViewUrl( VIEW_INSERT_SERVICE ) );
        model.put( MARK_INSERT_SERVICE_LINK_PAGE, getViewUrl( VIEW_INSERT_LINK_SERVICE ) );
        model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );

        return getPage( Constants.PROPERTY_PAGE_TITLE_MODIFY_EVENT, TEMPLATE_MODIFY_EVENT );
    }

    /**
     * Process the Event modifications
     * 
     * @param request The Http request
     * @return Html form
     */
    @Action( ACTION_MODIFY_EVENT )
    public String doModifyEvent( HttpServletRequest request )
    {
        String strCollectError = collectModifyEventParameters( request );

        if ( strCollectError != null )
        {
            return redirect( request, strCollectError );
        }

        //Retrieving the event object to update
        SimpleEvent event = CalendarHome.findEvent(
                Integer.parseInt( request.getParameter( Constants.PARAMETER_EVENT_ID ) ), getPlugin( ) );

        //Retrieving parameters from form
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );

        String strEventTitle = (String) _mapParameters.get( Constants.PARAMETER_EVENT_TITLE );
        String strTopEvent = (String) _mapParameters.get( Constants.PARAMETER_EVENT_TOP_EVENT );
        String strEventDate = (String) _mapParameters.get( Constants.PARAMETER_EVENT_DATE );

        boolean bPeriociteModify = false;

        // Mandatory field: the date, the title and the description fields
        if ( strEventDate.equals( "" ) || strEventTitle.equals( "" ) || _EventDescription.equals( "" ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        //Convert the date in form to a java.util.Date object
        Date dateEvent = parseFormDate( strEventDate );
        Date dateEventOld = event.getDate( );

        //If event date is different, periodicity is re-initialized
        if ( ( dateEvent != null ) && ( dateEvent.getTime( ) != dateEventOld.getTime( ) ) )
        {
            bPeriociteModify = true;
        }

        if ( ( dateEvent == null ) || !Utils.isValidDate( dateEvent ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                    AdminMessage.TYPE_STOP ) );
        }

        //Fields to Update
        event.setDate( dateEvent );
        event.setTitle( strEventTitle );
        event.setTopEvent( Integer.parseInt( strTopEvent ) );
        event.setDescription( _EventDescription );

        //String strOccurrence = request.getParameter( Constants.PARAMETER_OCCURRENCE );
        String strEventDateEnd = (String) _mapParameters.get( Constants.PARAMETER_EVENT_DATE_END );

        int nPeriodicity = event.getPeriodicity( );

        Date dateEndEvent = null;

        if ( StringUtils.isNotEmpty( strEventDateEnd ) )
        {
            dateEndEvent = parseFormDate( strEventDateEnd );

            if ( ( dateEndEvent == null ) || !Utils.isValidDate( dateEndEvent ) )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                        AdminMessage.TYPE_STOP ) );
            }
        }

        //the number of occurrence is -1 by default
        int nOccurrence = -1;

        String[] arrayExcludedDays = (String[]) _mapParameters.get( Constants.PARAMETER_EXCLUDED_DAYS );
        boolean bExcludedDaysModified = false;
        if ( arrayExcludedDays == null && event.getExcludedDays( ) != null && event.getExcludedDays( ).length != 0 )
        {
            bExcludedDaysModified = true;
        }
        else if ( arrayExcludedDays != null
                && ( event.getExcludedDays( ) == null || ( event.getExcludedDays( ) != null && event.getExcludedDays( ).length == 0 ) ) )
        {
            bExcludedDaysModified = true;
        }
        else if ( arrayExcludedDays != null && event.getExcludedDays( ) != null
                && arrayExcludedDays.length != event.getExcludedDays( ).length )
        {
            bExcludedDaysModified = true;
        }
        else if ( arrayExcludedDays != null && event.getExcludedDays( ) != null && event.getExcludedDays( ).length != 0
                && arrayExcludedDays.length == event.getExcludedDays( ).length )
        {
            for ( int i = 0; i < arrayExcludedDays.length; i++ )
            {
                if ( !arrayExcludedDays[i].equals( event.getExcludedDays( )[i] ) )
                {
                    bExcludedDaysModified = true;
                    break;
                }
            }
        }

        //compute the occurrence number depending on the date end chosen
        //If a date end is chosen
        if ( dateEndEvent != null
                && ( StringUtils.isNotEmpty( strEventDateEnd )
                        && ( event.getDateEnd( ).getTime( ) != dateEndEvent.getTime( ) ) || bExcludedDaysModified ) )
        {
            if ( dateEndEvent.before( dateEvent ) )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATE_END_BEFORE,
                        AdminMessage.TYPE_STOP ) );
            }
            nPeriodicity = 0;
            nOccurrence = Utils.getOccurrenceWithinTwoDates( dateEvent, dateEndEvent, arrayExcludedDays );
            bPeriociteModify = true;
        }
        else if ( StringUtils.isEmpty( strEventDateEnd ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                    AdminMessage.TYPE_STOP ) );
        }

        // Fields to Update
        if ( nOccurrence > 0 )
        {
            event.setOccurrence( nOccurrence );
        }

        event.setDateEnd( dateEndEvent );
        event.setPeriodicity( nPeriodicity );

        String strTimeStart = (String) _mapParameters.get( Constants.PARAMETER_EVENT_TIME_START );
        String strTimeEnd = (String) _mapParameters.get( Constants.PARAMETER_EVENT_TIME_END );
        String strEventTags = ( (String) _mapParameters.get( Constants.PARAMETER_EVENT_TAGS ) ).trim( );
        String strLocationAddress = ( (String) _mapParameters.get( Constants.PARAMETER_EVENT_LOCATION_ADDRESS ) )
                .trim( );
        String strLocation = ( (String) _mapParameters.get( Constants.PARAMETER_LOCATION ) ).trim( );
        String strLocationTown = ( (String) _mapParameters.get( Constants.PARAMETER_LOCATION_TOWN ) ).trim( );
        String strLocationZip = ( (String) _mapParameters.get( Constants.PARAMETER_LOCATION_ZIP ) ).trim( );
        String strLinkUrl = ( (String) _mapParameters.get( Constants.PARAMETER_EVENT_LINK_URL ) ).trim( );
        // Document id and site resource are no longer edited in the form: keep the stored values when absent
        String strDocumentId = (String) _mapParameters.get( Constants.PARAMETER_EVENT_DOCUMENT_ID );
        String strPageUrl = (String) _mapParameters.get( Constants.PARAMETER_EVENT_PAGE_URL );
        String strMapUrl = ( (String) _mapParameters.get( Constants.PARAMETER_EVENT_MAP_URL ) ).trim( );

        //Tags
        String[] strTabEventTags = null;

        if ( ( strEventTags.length( ) > 0 ) && !strEventTags.equals( "" ) )
        {
            //Test if there aren't special characters in strEventTags
            boolean isAllowedExp = Pattern.matches( PROPERTY_TAGS_REGEXP, strEventTags );

            if ( !isAllowedExp )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_TAG, AdminMessage.TYPE_STOP ) );
            }

            strTabEventTags = strEventTags.split( "\\s" );
        }

        //Categories
        List<Category> listCategories = new ArrayList<Category>( );

        if ( _EventCategories != null )
        {
            for ( String strIdCategory : _EventCategories )
            {
                Category category = CategoryHome.find( Integer.parseInt( strIdCategory ), getPlugin( ) );

                if ( category != null )
                {
                    listCategories.add( category );
                }
            }
        }

        // Hours
        if ( !Utils.checkTime( strTimeStart ) || !Utils.checkTime( strTimeEnd ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_TIMEFORMAT,
                    AdminMessage.TYPE_STOP ) );
        }

        // Document
        if ( strDocumentId != null )
        {
            int nDocumentId = -1;

            if ( StringUtils.isNotBlank( strDocumentId ) )
            {
                try
                {
                    nDocumentId = Integer.parseInt( strDocumentId.trim( ) );
                }
                catch ( NumberFormatException e )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_OCCURRENCE_NUMBER,
                            AdminMessage.TYPE_STOP ) );
                }
            }

            event.setDocumentId( nDocumentId );
        }

        event.setTags( strTabEventTags );
        event.setListCategories( listCategories );
        event.setDateTimeStart( strTimeStart );
        event.setDateTimeEnd( strTimeEnd );
        event.setLocation( strLocation );
        event.setLocationAddress( strLocationAddress );
        event.setLocationTown( strLocationTown );
        event.setLocationZip( strLocationZip );
        event.setLinkUrl( strLinkUrl );
        if ( strPageUrl != null )
        {
            event.setPageUrl( strPageUrl.trim( ) );
        }
        event.setMapUrl( strMapUrl );
        event.setExcludedDays( arrayExcludedDays );

        if ( ( _EventItem != null ) && ( _EventItem.getSize( ) >= 1 ) )
        {
            ImageResource imageResource = new ImageResource( );
            imageResource.setImage( _EventItem.get( ) );
            imageResource.setMimeType( _EventItem.getContentType( ) );
            event.setImageResource( imageResource );
        }

        _eventListService.doModifySimpleEvent( event, bPeriociteModify, null, getPlugin( ) );
        EventDetailsHome.save( _eventDetailsService.readDetails( request, event.getId( ) ), getPlugin( ) );

        List<OccurrenceEvent> listOccurencesEvent = _eventListService.getOccurrenceEvents( nCalendarId, event.getId( ),
                Constants.SORT_ASC, getPlugin( ) );

        for ( OccurrenceEvent occ : listOccurencesEvent )
        {
            //Update occurence
            if ( StringUtils.isBlank( occ.getDateTimeStart( ) ) )
            {
                occ.setDateTimeStart( event.getDateTimeStart( ) );
            }

            if ( StringUtils.isBlank( occ.getDateTimeEnd( ) ) )
            {
                occ.setDateTimeEnd( event.getDateTimeEnd( ) );
            }

            // FIXME : Usefull ?
            //CalendarHome.updateOccurrence( occ, nCalendarId, getPlugin(  ) );

            // Index Occurrence
            IndexationService.addIndexerAction( Integer.toString( occ.getId( ) ),
                    AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                    IndexerAction.TASK_MODIFY );
            CalendarIndexerUtils.addIndexerAction( occ.getId( ), IndexerAction.TASK_MODIFY );
        }

        _mapParameters = null;
        _EventItem = null;
        _EventCategories = null;
        _EventDescription = null;

        return redirect( request, getEventListUrl( nCalendarId, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) ) );
    }

    /**
     * Process the occurrence modifications
     * 
     * @param request The Http request
     * @return Html form
     */
    @Action( ACTION_MODIFY_OCCURRENCE )
    public String doModifyOccurrence( HttpServletRequest request )
    {
        //TODO add new parameters
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        String strEventDate = request.getParameter( Constants.PARAMETER_EVENT_DATE );
        String strStatus = request.getParameter( Constants.PARAMETER_EVENT_STATUS ).trim( );
        String strTimeStart = request.getParameter( Constants.PARAMETER_EVENT_TIME_START );
        String strTimeEnd = request.getParameter( Constants.PARAMETER_EVENT_TIME_END );

        // Mandatory field
        if ( strEventDate.equals( "" ) || request.getParameter( Constants.PARAMETER_EVENT_TITLE ).equals( "" ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        //Convert the date in form to a java.util.Date object
        Date dateEvent = parseFormDate( strEventDate );

        if ( ( dateEvent == null ) || !Utils.isValidDate( dateEvent ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                    AdminMessage.TYPE_STOP ) );
        }

        if ( !Utils.checkTime( strTimeStart ) || !Utils.checkTime( strTimeEnd ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_TIMEFORMAT,
                    AdminMessage.TYPE_STOP ) );
        }

        OccurrenceEvent occurrence = CalendarHome.findOccurrence(
                Integer.parseInt( request.getParameter( Constants.PARAMETER_OCCURRENCE_ID ) ), getPlugin( ) );

        occurrence.setDate( dateEvent );
        occurrence.setStatus( strStatus );
        occurrence.setDateTimeStart( strTimeStart );
        occurrence.setDateTimeEnd( strTimeEnd );
        occurrence.setTitle( request.getParameter( Constants.PARAMETER_EVENT_TITLE ) );

        _eventListService.doModifyOccurrenceEvent( occurrence, getPlugin( ) );

        SimpleEvent event = CalendarHome.findEvent( occurrence.getEventId( ), getPlugin( ) );
        List<OccurrenceEvent> listOccurrenceEvent = CalendarHome.findOccurrencesList( event.getIdCalendar( ),
                event.getId( ), 1, getPlugin( ) );

        // Just one occurrence
        if ( listOccurrenceEvent.size( ) == 1 )
        {
            event.setDate( occurrence.getDate( ) );
            event.setDateEnd( occurrence.getDate( ) );
        }

        // First occurrence
        else if ( listOccurrenceEvent.get( 0 ).getId( ) == occurrence.getId( ) )
        {
            event.setDate( occurrence.getDate( ) );
        }

        // Last occurrence
        else if ( listOccurrenceEvent.get( listOccurrenceEvent.size( ) - 1 ).getId( ) == occurrence.getId( ) )
        {
            event.setDateEnd( occurrence.getDate( ) );
        }

        _eventListService.doModifySimpleEvent( event, false, null, getPlugin( ) );

        // Incremental indexation - Modify
        IndexationService.addIndexerAction( Integer.toString( occurrence.getId( ) ),
                AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ), IndexerAction.TASK_MODIFY );

        CalendarIndexerUtils.addIndexerAction( occurrence.getId( ), IndexerAction.TASK_MODIFY );

        return redirect( request, getOccurrenceListUrl( nCalendarId, request.getParameter( Constants.PARAMETER_SORT_EVENTS ),
                String.valueOf( occurrence.getEventId( ) ) ) );
    }

    /**
     * Modify Periodicity of Occurrence
     * @param request The request
     * @return Html form
     */
    @Action( ACTION_MODIFY_OCCURRENCE_PERIODICITY )
    public String doModifyOccurrencePeriodicity( HttpServletRequest request )
    {
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        String strOccurrence = request.getParameter( Constants.PARAMETER_OCCURRENCE );
        int nPeriodicity = Integer.parseInt( request.getParameter( Constants.PARAMETER_PERIODICITY ) );
        String strIdEvent = request.getParameter( Constants.PARAMETER_EVENT_ID );
        int nIdEvent = Integer.parseInt( strIdEvent );
        String strSortEvents = request.getParameter( Constants.PARAMETER_SORT_EVENTS );
        strSortEvents = ( strSortEvents != null ) ? strSortEvents : "1";
        String[] arrayExcludedDays = request.getParameterValues( Constants.PARAMETER_EXCLUDED_DAYS );

        SimpleEvent event = CalendarHome.findEvent( nIdEvent, PluginService.getPlugin( Constants.PLUGIN_NAME ) );

        //the number of occurrence is 1 by default
        int nOccurrence = 1;

        //Retrieving the occurrence from form
        if ( ( strOccurrence.length( ) > 0 ) && !strOccurrence.equals( "" ) )
        {
            try
            {
                nOccurrence = Integer.parseInt( strOccurrence );
            }
            catch ( NumberFormatException e )
            {
                return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_OCCURRENCE_NUMBER,
                        AdminMessage.TYPE_STOP ) );
            }
        }
        event.setExcludedDays( arrayExcludedDays );

        Date dateEndEvent = Utils
                .getDateForward( event.getDate( ), nPeriodicity, nOccurrence, event.getExcludedDays( ) );

        // Fields to Update
        event.setOccurrence( nOccurrence );
        event.setDateEnd( dateEndEvent );
        event.setPeriodicity( nPeriodicity );
        _eventListService.doModifySimpleEvent( event, true, null, getPlugin( ) );

        List<OccurrenceEvent> listOccurencesEvent = CalendarHome.findOccurrencesList( nCalendarId, event.getId( ), 1,
                getPlugin( ) );

        for ( OccurrenceEvent occ : listOccurencesEvent )
        {
            // Index Occurrence - Add
            IndexationService.addIndexerAction( Integer.toString( occ.getId( ) ),
                    AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                    IndexerAction.TASK_CREATE );
            CalendarIndexerUtils.addIndexerAction( occ.getId( ), IndexerAction.TASK_CREATE );
        }

        return redirect( request, getOccurrenceListUrl( nCalendarId, strSortEvents, strIdEvent ) );
    }

    /**
     * Returns the form for occurrence modification
     * 
     * @param request The Http request
     * @return Html form
     */
    @View( VIEW_MODIFY_OCCURRENCE )
    public String getModifyOccurrence( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID, Constants.PARAMETER_OCCURRENCE_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }


        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        String strEventId = request.getParameter( Constants.PARAMETER_OCCURRENCE_ID );
        int nIdOccurrence = Integer.parseInt( strEventId.trim( ) );

        OccurrenceEvent occurrence = CalendarHome.findOccurrence( nIdOccurrence, getPlugin( ) );
        if ( occurrence == null )
        {
            return redirectNotFound( request );
        }

        Models model = _models;
        model.put( Constants.MARK_OCCURRENCE, occurrence );
        model.put( Constants.MARK_CALENDAR_ID, nCalendarId );
        model.put( MARK_EVENT_STATUS_LIST, getStatusList( request ) );
        model.put( MARK_DEFAULT_STATUS, occurrence.getStatus( ) );
        model.put( Constants.MARK_DEFAULT_SORT_EVENT, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) );
        model.put( Constants.MARK_LOCALE, getLocale( ).getLanguage( ) );

        return getPage( Constants.PROPERTY_PAGE_TITLE_MODIFY_OCCURRENCE, TEMPLATE_MODIFY_OCCURRENCE );
    }

    /**
     * Returns the confirmation to remove the event
     * 
     * @param request The Http request
     * @return the confirmation page
     */
    @View( value = VIEW_CONFIRM_REMOVE_EVENT, securityTokenAction = ACTION_REMOVE_EVENT )
    public String getConfirmRemoveEvent( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID, Constants.PARAMETER_EVENT_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_EVENT ) );

        url.addParameter( Constants.PARAMETER_CALENDAR_ID,
                Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) ) );
        url.addParameter( Constants.PARAMETER_EVENT_ID,
                Integer.parseInt( request.getParameter( Constants.PARAMETER_EVENT_ID ) ) );

        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_EVENT, url.getUrl( ),
                AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Remove a event
     * 
     * @param request The Http request
     * @return Html form
     */
    @Action( ACTION_REMOVE_EVENT )
    public String doRemoveEvent( HttpServletRequest request )
    {

        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        Event event = CalendarHome.findEvent( Integer.parseInt( request.getParameter( Constants.PARAMETER_EVENT_ID ) ),
                getPlugin( ) );
        List<OccurrenceEvent> listOccurencesEvent = CalendarHome.findOccurrencesList( nCalendarId, event.getId( ), 1,
                getPlugin( ) );

        for ( OccurrenceEvent occ : listOccurencesEvent )
        {
            IndexationService.addIndexerAction( Integer.toString( occ.getId( ) ),
                    AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                    IndexerAction.TASK_DELETE );
            CalendarIndexerUtils.addIndexerAction( occ.getId( ), IndexerAction.TASK_DELETE );
        }

        _eventListService.doRemoveEvent( nCalendarId, event.getId( ), null, getPlugin( ) );

        //Go to the event list
        return redirect( request, getEventListUrl( nCalendarId, null ) );
    }

    /**
     * Refuses a view whose calendar, event or occurrence does not exist
     * @param request the request
     * @return the url of the message
     */
    private String redirectNotFound( HttpServletRequest request )
    {
        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_ELEMENT_NOT_FOUND, AdminMessage.TYPE_STOP ) );
    }

    /**
     * Tells whether the request carries every parameter given, each holding a number
     * @param request the request
     * @param strParameters the names of the parameters
     * @return true when each parameter is a number
     */
    private static boolean hasNumericParameters( HttpServletRequest request, String... strParameters )
    {
        for ( String strParameter : strParameters )
        {
            String strValue = StringUtils.trim( request.getParameter( strParameter ) );

            if ( StringUtils.isEmpty( strValue ) || !StringUtils.isNumeric( strValue ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Builds the url of the event list of a calendar
     * @param nCalendarId the calendar id
     * @param strSortEvents the sort order, or null
     * @return the url
     */
    private String getEventListUrl( int nCalendarId, String strSortEvents )
    {
        UrlItem url = new UrlItem( getViewUrl( VIEW_EVENT_LIST ) );
        url.addParameter( Constants.PARAMETER_PLUGIN_NAME, Constants.PLUGIN_NAME );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID, nCalendarId );

        if ( strSortEvents != null )
        {
            url.addParameter( Constants.PARAMETER_SORT_EVENTS, strSortEvents );
        }

        return url.getUrl( );
    }

    /**
     * Builds the url of the occurrence list of an event
     * @param nCalendarId the calendar id
     * @param strSortEvents the sort order, or null
     * @param strEventId the event id
     * @return the url
     */
    private String getOccurrenceListUrl( int nCalendarId, String strSortEvents, String strEventId )
    {
        UrlItem url = new UrlItem( getViewUrl( VIEW_OCCURRENCE_LIST ) );
        url.addParameter( Constants.PARAMETER_PLUGIN_NAME, Constants.PLUGIN_NAME );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID, nCalendarId );

        if ( strSortEvents != null )
        {
            url.addParameter( Constants.PARAMETER_SORT_EVENTS, strSortEvents );
        }

        url.addParameter( Constants.PARAMETER_EVENT_ID, strEventId );

        return url.getUrl( );
    }

    /**
     * Return the colours suggested by the colour picker of a calendar (property calendar.dots.colors: colour names or #rrggbb),
     * labelled in the admin language
     * 
     * @return the colours: code = the hexadecimal colour, name = its label
     */
    private ReferenceList getDotsList( )
    {
        ReferenceList listDots = new ReferenceList( );

        for ( String strColor : getDotColors( ) )
        {
            String strCode = AgendaResource.getColorCode( strColor );

            if ( strCode != null )
            {
                String strLabel = I18nService.getLocalizedString( PREFIX_DOT_COLOR_LABEL + strColor, getLocale( ) );
                listDots.addItem( strCode, StringUtils.defaultIfEmpty( strLabel, strCode ) );
            }
        }

        return listDots;
    }

    /**
     * Return the colours offered for the calendar dots
     * 
     * @return the colours of the property calendar.dots.colors
     */
    private static List<String> getDotColors( )
    {
        String strColors = AppPropertiesService.getProperty( Constants.PROPERTY_CALENDAR_DOTS_COLORS, DEFAULT_DOT_COLORS );

        return Arrays.stream( strColors.split( "," ) ).map( String::trim ).filter( StringUtils::isNotEmpty ).toList( );
    }

    /**
     * Keep a posted calendar colour only when it is a hexadecimal colour (#rrggbb) from the colour picker
     * 
     * @param strColor
     *            the posted colour
     * @return the colour in lower case, or null when it is not a hexadecimal colour
     */
    private static String getValidDotColor( String strColor )
    {
        return AgendaResource.getValidColorCode( strColor );
    }

    /**
     * Normalizes a text for the search on the name of the calendars: no accent, lower case, trimmed
     * 
     * @param strText
     *            the text
     * @return the normalized text, empty when the text is null
     */
    private static String normalizeSearch( String strText )
    {
        return StringUtils.stripAccents( StringUtils.defaultString( strText ) ).toLowerCase( Locale.ROOT ).trim( );
    }

    /**
     * Returns the views a calendar may open on, labelled in the admin language: automatic (by the size of the screen),
     * month, week, day or list
     * 
     * @return the list: code = the view, empty for automatic, name = its label
     */
    private ReferenceList getDefaultViewList( )
    {
        ReferenceList list = new ReferenceList( );

        for ( String strView : DEFAULT_VIEWS )
        {
            list.addItem( strView, I18nService.getLocalizedString( PREFIX_DEFAULT_VIEW_LABEL + StringUtils.defaultIfEmpty( strView, DEFAULT_VIEW_AUTO ), getLocale( ) ) );
        }

        return list;
    }

    /**
     * Keeps a posted default view only when it is one of the views offered
     * 
     * @param strView
     *            the posted view
     * @return the view, or an empty string (automatic) when it is not offered
     */
    private static String getValidDefaultView( String strView )
    {
        return ( strView != null ) && DEFAULT_VIEWS.contains( strView ) ? strView : StringUtils.EMPTY;
    }

    /**
     * Returns a list of codes labelled in the admin language
     * 
     * @param listCodes
     *            the codes
     * @param strPrefix
     *            the prefix of the i18n key of a label, followed by the code ("all" for the empty code)
     * @return the list: code = the code, name = its label
     */
    private ReferenceList getLabelledList( List<String> listCodes, String strPrefix )
    {
        ReferenceList list = new ReferenceList( );

        for ( String strCode : listCodes )
        {
            list.addItem( strCode, I18nService.getLocalizedString( strPrefix + StringUtils.defaultIfEmpty( strCode, KEY_FILTER_ALL ), getLocale( ) ) );
        }

        return list;
    }

    /**
     * Returns the categories offered by the filter of the event list, after the choice of all the categories
     * 
     * @return the list: code = the identifier of the category, name = its name
     */
    private ReferenceList getFilterCategoryList( )
    {
        ReferenceList list = new ReferenceList( );
        list.addItem( -1, I18nService.getLocalizedString( KEY_FILTER_ALL_CATEGORIES, getLocale( ) ) );

        for ( Category category : CategoryHome.findAll( getPlugin( ) ) )
        {
            list.addItem( category.getId( ), category.getName( ) );
        }

        return list;
    }

    /**
     * Returns the identifiers of the events of a calendar that belong to a category
     * 
     * @param listEvents
     *            the events of the calendar
     * @param nCategoryId
     *            the identifier of the category, or a negative number for all the categories
     * @return the identifiers, or null when the events are not filtered by category
     */
    private Set<Integer> getEventIdsOfCategory( List<SimpleEvent> listEvents, int nCategoryId )
    {
        if ( nCategoryId <= 0 )
        {
            return null;
        }

        return Arrays.stream( CategoryHome.findEventIds( nCategoryId, getPlugin( ) ) ).boxed( ).collect( java.util.stream.Collectors.toSet( ) );
    }

    /**
     * Return the list [(1, ascendant),(2,descendant)] that is used to sort the
     * events date
     * 
     * @return a refenceList
     */
    private ReferenceList getSortEventList( )
    {
        ReferenceList list = new ReferenceList( );
        list.addItem( 1, I18nService.getLocalizedString( Constants.PROPERTY_SORT_EVENTS + 1, getLocale( ) ) );
        list.addItem( 2, I18nService.getLocalizedString( Constants.PROPERTY_SORT_EVENTS + 2, getLocale( ) ) );

        return list;
    }

    /**
     * Return the list of time intervals declared in properties file
     * @return A ReferenceList of time interval
     * @param request The HttpRequest
     */
    public ReferenceList getIntervalList( HttpServletRequest request )
    {
        StringTokenizer st = new StringTokenizer( AppPropertiesService.getProperty( PROPERTY_TIME_INTERVAL_LIST ), "," );
        ReferenceList timeIntervalList = new ReferenceList( );

        while ( st.hasMoreElements( ) )
        {
            String strIntervalName = st.nextToken( ).trim( );
            String strDescription = I18nService.getLocalizedString( "calendar.interval.periodicity." + strIntervalName
                    + ".description", getLocale( ) );
            int nDays = AppPropertiesService.getPropertyInt( "calendar.interval." + strIntervalName + ".value", 7 );
            timeIntervalList.addItem( nDays, strDescription );
        }

        return timeIntervalList;
    }

    /**
     * Return the list [(0, no),(2,yes)]
     * 
     * @return a refenceList
     */
    private ReferenceList getTopEventList( )
    {
        ReferenceList list = new ReferenceList( );
        StringTokenizer st = new StringTokenizer(
                I18nService.getLocalizedString( PROPERTY_TOP_EVENT_LIST, getLocale( ) ), "," );
        int i = 0;

        while ( st.hasMoreElements( ) )
        {
            list.addItem( i++, st.nextToken( ).trim( ) );
        }

        return list;
    }

    /**
     * Return the list
     * @param request The request
     * @return a refenceList
     */
    private ReferenceList getStatusList( HttpServletRequest request )
    {
        ReferenceList list = new ReferenceList( );
        StringTokenizer stStatus = new StringTokenizer( AppPropertiesService.getProperty( PROPERTY_EVENT_STATUS_LIST ),
                Constants.COMMA );

        while ( stStatus.hasMoreElements( ) )
        {
            String strStatus = stStatus.nextToken( ).trim( );
            String strStatusValue = I18nService.getLocalizedString( "calendar.event.status." + strStatus + ".value",
                    getLocale( ) );

            list.addItem( strStatus, strStatusValue );
        }

        return list;
    }

    /**
     * Return the list
     * 
     * @return a refenceList
     */
    /*
     * private ReferenceList getReferenceListCategory( Collection<Category>
     * collection )
     * {
     * ReferenceList list = new ReferenceList( );
     * 
     * //list.addItem( Constants.PROPERTY_CALENDAR_NONE, " " );
     * if ( collection != null )
     * {
     * Iterator<Category> i = collection.iterator( );
     * 
     * while ( i.hasNext( ) )
     * {
     * Category category = (Category) i.next( );
     * list.addItem( category.getId( ), category.getName( ) );
     * }
     * }
     * 
     * return list;
     * }
     */

    /**
     * Returns the confirmation to modify an event
     * 
     * @param request The Http request
     * @return the confirmation page
     */
    private String collectModifyEventParameters( HttpServletRequest request )
    {
        HashMap<String, Object> mapParameters = new HashMap<String, Object>( );
        Date dateEvent = null;
        Date dateEndEvent = null;

        mapParameters.put( Constants.PARAMETER_EVENT_ID, request.getParameter( Constants.PARAMETER_EVENT_ID ) );
        mapParameters.put( Constants.PARAMETER_CALENDAR_ID, request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        mapParameters.put( Constants.PARAMETER_TYPE_BOX, request.getParameter( Constants.PARAMETER_TYPE_BOX ) );

        if ( request.getParameter( Constants.PARAMETER_EVENT_TITLE ) != null )
        {
            mapParameters
                    .put( Constants.PARAMETER_EVENT_TITLE, request.getParameter( Constants.PARAMETER_EVENT_TITLE ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_DATE ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_DATE, request.getParameter( Constants.PARAMETER_EVENT_DATE ) );
            dateEvent = parseFormDate( request.getParameter( Constants.PARAMETER_EVENT_DATE ) );

            if ( ( dateEvent == null ) || !Utils.isValidDate( dateEvent ) )
            {
                return AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                        AdminMessage.TYPE_STOP );
            }
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_DATE_END ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_DATE_END,
                    request.getParameter( Constants.PARAMETER_EVENT_DATE_END ) );
            dateEndEvent = parseFormDate( request.getParameter( Constants.PARAMETER_EVENT_DATE_END ) );
            if ( ( dateEndEvent == null ) || !Utils.isValidDate( dateEndEvent ) )
            {
                return AdminMessageService.getMessageUrl( request, Constants.PROPERTY_MESSAGE_DATEFORMAT,
                        AdminMessage.TYPE_STOP );
            }
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_TIME_START ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_TIME_START,
                    request.getParameter( Constants.PARAMETER_EVENT_TIME_START ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_TIME_END ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_TIME_END,
                    request.getParameter( Constants.PARAMETER_EVENT_TIME_END ) );
        }

        if ( request.getParameter( Constants.PARAMETER_RADIO_PERIODICITY ) != null )
        {
            mapParameters.put( Constants.PARAMETER_RADIO_PERIODICITY,
                    request.getParameter( Constants.PARAMETER_RADIO_PERIODICITY ) );
        }

        if ( request.getParameter( Constants.PARAMETER_PERIODICITY ) != null )
        {
            mapParameters
                    .put( Constants.PARAMETER_PERIODICITY, request.getParameter( Constants.PARAMETER_PERIODICITY ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_LOCATION_ADDRESS ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_LOCATION_ADDRESS,
                    request.getParameter( Constants.PARAMETER_EVENT_LOCATION_ADDRESS ) );
        }

        if ( request.getParameter( Constants.PARAMETER_LOCATION ) != null )
        {
            mapParameters.put( Constants.PARAMETER_LOCATION, request.getParameter( Constants.PARAMETER_LOCATION ) );
        }

        if ( request.getParameter( Constants.PARAMETER_LOCATION_TOWN ) != null )
        {
            mapParameters.put( Constants.PARAMETER_LOCATION_TOWN,
                    request.getParameter( Constants.PARAMETER_LOCATION_TOWN ) );
        }

        if ( request.getParameter( Constants.PARAMETER_LOCATION_ZIP ) != null )
        {
            mapParameters.put( Constants.PARAMETER_LOCATION_ZIP,
                    request.getParameter( Constants.PARAMETER_LOCATION_ZIP ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_LINK_URL ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_LINK_URL,
                    request.getParameter( Constants.PARAMETER_EVENT_LINK_URL ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_DOCUMENT_ID ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_DOCUMENT_ID,
                    request.getParameter( Constants.PARAMETER_EVENT_DOCUMENT_ID ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_PAGE_URL ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_PAGE_URL,
                    request.getParameter( Constants.PARAMETER_EVENT_PAGE_URL ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_TOP_EVENT ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_TOP_EVENT,
                    request.getParameter( Constants.PARAMETER_EVENT_TOP_EVENT ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_MAP_URL ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_MAP_URL,
                    request.getParameter( Constants.PARAMETER_EVENT_MAP_URL ) );
        }

        if ( request.getParameter( Constants.PARAMETER_OCCURRENCE ) != null )
        {
            mapParameters.put( Constants.PARAMETER_OCCURRENCE, request.getParameter( Constants.PARAMETER_OCCURRENCE ) );
        }

        if ( request.getParameter( Constants.PARAMETER_EVENT_TAGS ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EVENT_TAGS, request.getParameter( Constants.PARAMETER_EVENT_TAGS ) );
        }

        if ( request.getParameter( Constants.PARAMETER_SORT_EVENTS ) != null )
        {
            mapParameters
                    .put( Constants.PARAMETER_SORT_EVENTS, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) );
        }

        if ( request.getParameterValues( Constants.PARAMETER_EXCLUDED_DAYS ) != null )
        {
            mapParameters.put( Constants.PARAMETER_EXCLUDED_DAYS,
                    request.getParameterValues( Constants.PARAMETER_EXCLUDED_DAYS ) );
        }

        MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;

        _EventItem = mRequest.getFile( Constants.PARAMETER_EVENT_IMAGE );
        _EventCategories = request.getParameterValues( Constants.PARAMETER_CATEGORY );
        _EventDescription = StringUtils.defaultString( request.getParameter( Constants.PARAMETER_DESCRIPTION ) );

        _mapParameters = mapParameters;

        return null;
    }

    /**
     * Returns the confirmation to remove occurrence
     * 
     * @param request The Http request
     * @return the confirmation page
     */
    @View( value = VIEW_CONFIRM_REMOVE_OCCURRENCE, securityTokenAction = ACTION_REMOVE_OCCURRENCE )
    public String getConfirmRemoveOccurrence( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_OCCURRENCE ) );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID,
                Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) ) );
        url.addParameter( Constants.PARAMETER_EVENT_ID, request.getParameter( Constants.PARAMETER_EVENT_ID ) );
        url.addParameter( Constants.PARAMETER_SORT_EVENTS, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) );
        url.addParameter( Constants.PARAMETER_OCCURRENCE_ID, request.getParameter( Constants.PARAMETER_OCCURRENCE_ID ) );

        String[] tableCBXOccurrence = request.getParameterValues( Constants.PARAMETER_CBX_OCCURRENCE );
        String strCBXOccurrence = "";

        //TODO In this version of core we cannot use url.addParameter(String strName, String[] strValues)
        //so we put the table parameters values to a string separated with token ";"
        if ( tableCBXOccurrence != null )
        {
            for ( int i = 0; i < tableCBXOccurrence.length; i++ )
            {
                strCBXOccurrence += ( tableCBXOccurrence[i] + Constants.COMMA );
            }

            url.addParameter( Constants.PARAMETER_CBX_OCCURRENCE, strCBXOccurrence );
        }

        if ( tableCBXOccurrence == null )
        {
            tableCBXOccurrence = new String[0];
        }

        return redirect( request, AdminMessageService.getMessageUrl( request,
                ( tableCBXOccurrence.length > 1 ) ? MESSAGE_CONFIRM_REMOVE_ALL_OCCURRENCE
                        : MESSAGE_CONFIRM_REMOVE_OCCURRENCE, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Remove a occurrence
     * 
     * @param request The Http request
     * @return Html form
     */
    @Action( ACTION_REMOVE_OCCURRENCE )
    public String doRemoveOccurrence( HttpServletRequest request )
    {
        int nOccurrenceId;
        String strOccurrenceId = request.getParameter( Constants.PARAMETER_OCCURRENCE_ID );
        String strCBXOccurrence = request.getParameter( Constants.PARAMETER_CBX_OCCURRENCE );
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        int nEventId = Integer.parseInt( request.getParameter( Constants.PARAMETER_EVENT_ID ) );
        SimpleEvent event = CalendarHome.findEvent( nEventId, PluginService.getPlugin( Constants.PLUGIN_NAME ) );
        int nOccurrence = event.getOccurrence( );

        String[] tableCBXOccurrence = new String[0];

        if ( ( strCBXOccurrence != null ) && !strCBXOccurrence.equals( "null" )
                && !strCBXOccurrence.equals( Constants.EMPTY_STRING ) )
        {
            tableCBXOccurrence = strCBXOccurrence.split( Constants.COMMA );
        }

        if ( ( strOccurrenceId != null ) && !strOccurrenceId.equals( "null" )
                && !strOccurrenceId.equals( Constants.EMPTY_STRING ) )
        {
            nOccurrenceId = Integer.parseInt( strOccurrenceId );
            CalendarHome.removeOccurrence( nOccurrenceId, nEventId, nCalendarId, getPlugin( ) );
            nOccurrence--;

            //Incremental indexation - Delete
            IndexationService.addIndexerAction( Integer.toString( nOccurrenceId ),
                    AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                    IndexerAction.TASK_DELETE );
            CalendarIndexerUtils.addIndexerAction( nOccurrenceId, IndexerAction.TASK_DELETE );
        }
        else if ( tableCBXOccurrence.length > 0 )
        {
            for ( int i = 0; tableCBXOccurrence.length > i; i++ )
            {
                int nOccId = Integer.parseInt( tableCBXOccurrence[i] );
                CalendarHome.removeOccurrence( nOccId, nEventId, nCalendarId, getPlugin( ) );
                nOccurrence--;
                //Incremental indexation - Delete
                IndexationService.addIndexerAction( tableCBXOccurrence[i],
                        AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                        IndexerAction.TASK_DELETE );
                CalendarIndexerUtils.addIndexerAction( nOccId, IndexerAction.TASK_DELETE );
            }
        }

        List<OccurrenceEvent> listOccurrence = CalendarHome.findOccurrencesList( nCalendarId, nEventId, 1,
                PluginService.getPlugin( Constants.PLUGIN_NAME ) );

        if ( !listOccurrence.isEmpty( ) )
        {
            OccurrenceEvent firstOccurrenceEvent = listOccurrence.get( 0 );
            OccurrenceEvent lastOccurrenceEvent = listOccurrence.get( listOccurrence.size( ) - 1 );

            event.setDate( firstOccurrenceEvent.getDate( ) );
            event.setDateEnd( lastOccurrenceEvent.getDate( ) );
        }

        event.setOccurrence( nOccurrence );
        _eventListService.doModifySimpleEvent( event, false, null, getPlugin( ) );

        // Go to the parent page          
        return redirect( request, getOccurrenceListUrl( nCalendarId, request.getParameter( Constants.PARAMETER_SORT_EVENTS ),
                String.valueOf( request.getParameter( Constants.PARAMETER_EVENT_ID ) ) ) );
    }

    /**
     * Builds the newsletter's subscribers management page
     * 
     * @param request The HTTP request
     * @return the html code for newsletter's subscribers management page
     */
    @View( VIEW_MANAGE_SUBSCRIBERS )
    public String getManageSubscribers( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }

        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );

        Models model = _models;

        model.put( Constants.MARK_CALENDAR_ID, nCalendarId );

        _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );
        _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage,
                _nDefaultItemsPerPage );

        String strSearchString = request.getParameter( Constants.PARAMETER_SUBSCRIBER_SEARCH );

        if ( strSearchString == null )
        {
            strSearchString = Constants.EMPTY_STRING;
        }

        // get a list of subscribers
        List<CalendarSubscriber> calendarSubscribers = CalendarSubscriberHome.findSubscribers( nCalendarId,
                strSearchString, Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_LIMIT_MIN_SUSCRIBER ) ),
                Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_LIMIT_MAX_SUSCRIBER ) ), getPlugin( ) );

        UrlItem url = new UrlItem( getViewFullUrl( VIEW_MANAGE_SUBSCRIBERS ) );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID, Integer.toString( nCalendarId ) );

        LocalizedPaginator<CalendarSubscriber> paginator = new LocalizedPaginator<CalendarSubscriber>(
                calendarSubscribers, _nItemsPerPage, url.getUrl( ), Paginator.PARAMETER_PAGE_INDEX,
                _strCurrentPageIndex, getLocale( ) );

        model.put( Constants.MARK_NB_ITEMS_PER_PAGE, "" + _nItemsPerPage );
        model.put( Constants.MARK_PAGINATOR, paginator );
        model.put( Constants.MARK_SUBSCRIBERS_LIST, paginator.getPageItems( ) );
        model.put( Constants.MARK_SEARCH_STRING, strSearchString );

        return getPage( Constants.PROPERTY_PAGE_TITLE_MANAGE_SUBSCRIBERS, TEMPLATE_MANAGE_SUBSCRIBERS );
    }

    /**
     * Manages the removal form of a newsletter whose identifier is in the http
     * request
     * 
     * @param request The Http request
     * @return the html code to confirm
     */
    @View( value = VIEW_CONFIRM_REMOVE_SUBSCRIBER, securityTokenAction = ACTION_REMOVE_SUBSCRIBER )
    public String getConfirmRemoveSubscriber( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID, Constants.PARAMETER_SUBSCRIBER_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }

        UrlItem urlItem = new UrlItem( getActionUrl( ACTION_REMOVE_SUBSCRIBER ) );
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        int nSubscriberId = Integer.parseInt( request.getParameter( Constants.PARAMETER_SUBSCRIBER_ID ) );
        urlItem.addParameter( Constants.PARAMETER_CALENDAR_ID, Integer.toString( nCalendarId ) );
        urlItem.addParameter( Constants.PARAMETER_SUBSCRIBER_ID, Integer.toString( nSubscriberId ) );

        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_SUBSCRIBER, urlItem.getUrl( ),
                AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Processes the unregistration of a subscriber for a newsletter
     * 
     * @param request The Http request
     * @return the jsp URL to display the form to manage newsletters
     */
    @Action( ACTION_REMOVE_SUBSCRIBER )
    public String doUnregistration( HttpServletRequest request )
    {
        /* parameters */
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        int nSubscriberId = Integer.parseInt( request.getParameter( Constants.PARAMETER_SUBSCRIBER_ID ) );

        //Checks if a subscriber with the same email address doesn't exist yet
        CalendarSubscriber subscriber = CalendarSubscriberHome.findByPrimaryKey( nSubscriberId, getPlugin( ) );

        if ( subscriber != null )
        {
            CalendarSubscriberHome.removeSubscriber( nCalendarId, nSubscriberId, getPlugin( ) );
            if ( !CalendarSubscriberHome.isUserSubscribed( nSubscriberId, getPlugin( ) ) )
            {
                CalendarSubscriberHome.remove( nSubscriberId, getPlugin( ) );
            }
        }

        return redirect( request, VIEW_MANAGE_SUBSCRIBERS, Constants.PARAMETER_CALENDAR_ID, nCalendarId );
    }

    /**
     * Get the document insert service if the plugin is installed
     * 
     * @param request The Http request
     * @return the jsp URL for displaying the document insert service or an
     *         error message if plugin is uninstalled
     */
    @View( VIEW_INSERT_SERVICE )
    public String getInsertService( HttpServletRequest request )
    {
        Plugin module = PluginService.getPlugin( Constants.PROPERTY_MODULE_CALENDAR );

        if ( ( module != null ) && module.isInstalled( ) )
        {
            UrlItem url = new UrlItem( JSP_GET_DOCUMENT_INSERT_SERVICE );
            url.addParameter( Constants.PARAMETER_INPUT, Constants.PARAMETER_EVENT_DOCUMENT_ID );

            return redirect( request, url.getUrl( ) );
        }
        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_PLUGIN_DOCUMENT_UNINSTALLED, AdminMessage.TYPE_STOP ) );
    }

    /**
     * Get the link insert service
     * 
     * @param request The Http request
     * @return the jsp URL for displaying the link insert service
     */
    @View( VIEW_INSERT_LINK_SERVICE )
    public String getInsertLinkService( HttpServletRequest request )
    {
        return redirect( request, AppPathService.getBaseUrl( request ) + JSP_GET_DOCUMENT_INSERT_LINK_SERVICE + "?"
                + Constants.PARAMETER_INPUT + "=" + Constants.PARAMETER_EVENT_PAGE_URL + "&"
                + Constants.PARAMETER_SELECTED_TEXT + "=" + Constants.EMPTY_STRING );
    }

    /**
     * Sends the bulk action on the checked occurrences, removal or status change, to its confirmation view, which
     * issues the security token of that action
     * @param request HttpServletRequest
     * @return the redirection to the confirmation view
     */
    @View( VIEW_MODIFY_OCCURRENCE_LIST )
    public String getModifyOccurrenceList( HttpServletRequest request )
    {
        String strView = ( request.getParameter( Constants.PARAMETER_REMOVE_OCCURRENCES ) != null ) ? VIEW_CONFIRM_REMOVE_OCCURRENCE
                : VIEW_CONFIRM_MODIFY_OCCURRENCE_STATUS;
        UrlItem url = new UrlItem( getViewUrl( strView ) );

        for ( String strParameter : new String [ ] {
                Constants.PARAMETER_CALENDAR_ID, Constants.PARAMETER_EVENT_ID, Constants.PARAMETER_SORT_EVENTS, Constants.PARAMETER_EVENT_STATUS
        } )
        {
            if ( request.getParameter( strParameter ) != null )
            {
                url.addParameter( strParameter, request.getParameter( strParameter ) );
            }
        }

        String [ ] arrayOccurrences = request.getParameterValues( Constants.PARAMETER_CBX_OCCURRENCE );

        if ( arrayOccurrences != null )
        {
            for ( String strOccurrence : arrayOccurrences )
            {
                url.addParameter( Constants.PARAMETER_CBX_OCCURRENCE, strOccurrence );
            }
        }

        return redirect( request, url.getUrl( ) );
    }

    /**
     * Returns the confirmation to modify occurrences status
     * 
     * @param request The Http request
     * @return the confirmation page
     */
    @View( value = VIEW_CONFIRM_MODIFY_OCCURRENCE_STATUS, securityTokenAction = ACTION_MODIFY_OCCURRENCE_STATUS )
    public String getConfirmModifyOccurrenceStatus( HttpServletRequest request )
    {
        if ( !hasNumericParameters( request, Constants.PARAMETER_CALENDAR_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_MODIFY_OCCURRENCE_STATUS ) );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID,
                Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) ) );
        url.addParameter( Constants.PARAMETER_SORT_EVENTS, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) );
        url.addParameter( Constants.PARAMETER_EVENT_ID, request.getParameter( Constants.PARAMETER_EVENT_ID ) );
        url.addParameter( Constants.PARAMETER_EVENT_STATUS, request.getParameter( Constants.PARAMETER_EVENT_STATUS ) );

        String[] tableCBXOccurrence = request.getParameterValues( Constants.PARAMETER_CBX_OCCURRENCE );
        String strCBXOccurrence = Constants.EMPTY_STRING;

        if ( tableCBXOccurrence != null )
        {
            for ( int i = 0; i < tableCBXOccurrence.length; i++ )
            {
                strCBXOccurrence += ( tableCBXOccurrence[i] + Constants.COMMA );
            }

            url.addParameter( Constants.PARAMETER_CBX_OCCURRENCE, strCBXOccurrence );
        }

        if ( tableCBXOccurrence == null )
        {
            tableCBXOccurrence = new String[0];
        }

        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_MODIFY_OCCURRENCES_STATUS, url.getUrl( ),
                AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Modify occurrencesStatus
     * @param request HttpServletRequest
     * @return the return Jsp
     */
    @Action( ACTION_MODIFY_OCCURRENCE_STATUS )
    public String doModifyOccurrenceStatus( HttpServletRequest request )
    {
        String strCBXOccurrence = request.getParameter( Constants.PARAMETER_CBX_OCCURRENCE );
        int nCalendarId = Integer.parseInt( request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
        String strEventStatus = request.getParameter( Constants.PARAMETER_EVENT_STATUS );

        String[] tableCBXOccurrence = new String[0];

        if ( ( strCBXOccurrence != null ) && !strCBXOccurrence.equals( Constants.NULL )
                && !strCBXOccurrence.equals( Constants.EMPTY_STRING ) )
        {
            tableCBXOccurrence = strCBXOccurrence.split( Constants.COMMA );
        }
        for ( int i = 0; i < tableCBXOccurrence.length; i++ )
        {
            int nIdOccurrenceEvent = Integer.parseInt( tableCBXOccurrence[i] );
            OccurrenceEvent occurrence = CalendarHome.findOccurrence( nIdOccurrenceEvent, getPlugin( ) );
            occurrence.setStatus( strEventStatus );
            _eventListService.doModifyOccurrenceEvent( occurrence, getPlugin( ) );
            //Incremental indexation - Modify
            IndexationService.addIndexerAction( tableCBXOccurrence[i],
                    AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ),
                    IndexerAction.TASK_MODIFY );
            CalendarIndexerUtils.addIndexerAction( nIdOccurrenceEvent, IndexerAction.TASK_MODIFY );
        }

        // Go to the parent page          
        return redirect( request, getOccurrenceListUrl( nCalendarId, request.getParameter( Constants.PARAMETER_SORT_EVENTS ),
                request.getParameter( Constants.PARAMETER_EVENT_ID ) ) );
    }

    /**
     * Modify parameters values
     * @param request HttpServletRequest
     * @return the Jsp return
     * @throws AccessDeniedException access denied
     */
    @Action( ACTION_MODIFY_CALENDAR_PARAMETER_VALUES )
    public String doModifyCalendarParameterValues( HttpServletRequest request ) throws AccessDeniedException
    {
        if ( !RBACService.isAuthorized( CalendarResourceIdService.RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                CalendarResourceIdService.PERMISSION_MANAGE, (User) getUser( ) ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }

        ReferenceList listParams = CalendarParameterHome.findParametersList( getPlugin( ) );
        for ( ReferenceItem param : listParams )
        {
            String strParamValue = request.getParameter( param.getCode( ) );
            if ( CalendarLibraryService.PARAMETER_CALENDAR_LIBRARY.equals( param.getCode( ) ) )
            {
                if ( strParamValue == null )
                {
                    continue;
                }
                if ( !CalendarLibraryService.isLibrary( strParamValue ) )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_LIBRARY, AdminMessage.TYPE_STOP ) );
                }
            }
            else
            {
                if ( strParamValue == null || strParamValue.equals( Constants.EMPTY_STRING ) )
                {
                    strParamValue = Constants.ZERO;
                }
                if ( !strParamValue.matches( Constants.REG_NUMBER ) )
                {
                    return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_NUMBER_FORMAT,
                            AdminMessage.TYPE_STOP ) );
                }
            }
            param.setName( strParamValue );
            CalendarParameterHome.update( param, getPlugin( ) );
        }

        if ( Boolean.parseBoolean( request.getParameter( PARAMETER_FROM_TECHNICAL_MENU ) ) )
        {
            return redirect( request, getAdminDashboardsUrl( request, ANCHOR_CALENDAR_ADMIN_DASHBOARD ) );
        }

        return redirectView( request, VIEW_MANAGE_ADVANCED_PARAMETERS );
    }

    /**
     * Returns the screen of the demonstration data: what it holds, and the buttons to install or remove it
     * @param request The Http request
     * @return the page
     * @throws AccessDeniedException If the user may not manage the calendars
     */
    @View( value = VIEW_MANAGE_DEMO, securityTokenAction = ACTION_INSTALL_DEMO )
    public String getManageDemo( HttpServletRequest request ) throws AccessDeniedException
    {
        checkManagePermission( );
        Models model = _models;
        model.put( MARK_DEMO_INSTALLED, _demoService.isInstalled( ) );
        model.put( MARK_DEMO_PAGE_ID, _demoService.getPageId( ) );
        return getPage( Constants.PROPERTY_PAGE_TITLE_MANAGE_CALENDARS, TEMPLATE_MANAGE_DEMO );
    }

    /**
     * Installs the demonstration data, replacing the previous one
     * @param request The Http request
     * @return the URL of the demonstration screen
     * @throws AccessDeniedException If the user may not manage the calendars
     */
    @Action( ACTION_INSTALL_DEMO )
    public String doInstallDemo( HttpServletRequest request ) throws AccessDeniedException
    {
        checkManagePermission( );
        _demoService.install( getPlugin( ) );
        addInfo( MESSAGE_DEMO_INSTALLED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_DEMO );
    }

    /**
     * Asks for the confirmation of the removal of the demonstration data
     * @param request The Http request
     * @return the URL of the confirmation message
     * @throws AccessDeniedException If the user may not manage the calendars
     */
    @View( value = VIEW_CONFIRM_REMOVE_DEMO, securityTokenAction = ACTION_REMOVE_DEMO )
    public String getConfirmRemoveDemo( HttpServletRequest request ) throws AccessDeniedException
    {
        checkManagePermission( );
        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_DEMO, getActionUrl( ACTION_REMOVE_DEMO ),
                AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Removes the demonstration data
     * @param request The Http request
     * @return the URL of the demonstration screen
     * @throws AccessDeniedException If the user may not manage the calendars
     */
    @Action( ACTION_REMOVE_DEMO )
    public String doRemoveDemo( HttpServletRequest request ) throws AccessDeniedException
    {
        checkManagePermission( );
        _demoService.remove( getPlugin( ) );
        addInfo( MESSAGE_DEMO_REMOVED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_DEMO );
    }

    /**
     * Checks that the user may manage all the calendars
     * @throws AccessDeniedException If the user may not
     */
    private void checkManagePermission( ) throws AccessDeniedException
    {
        if ( !RBACService.isAuthorized( CalendarResourceIdService.RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                CalendarResourceIdService.PERMISSION_MANAGE, (User) getUser( ) ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
    }

    /**
     * Parses a date posted by the date picker of the admin forms: ISO (yyyy-MM-dd HH:mm:ss, the default format of the core date macro),
     * yyyy-MM-dd, or the short date format of the admin locale for a value typed by hand
     *
     * @param strDate
     *            the posted date
     * @return the date, or null when it cannot be parsed
     */
    private Date parseFormDate( String strDate )
    {
        if ( StringUtils.isBlank( strDate ) )
        {
            return null;
        }

        String strValue = strDate.trim( );
        Date date = DateUtil.parseIsoDate( strValue );

        if ( ( date == null ) && strValue.matches( "\\d{4}-\\d{2}-\\d{2}" ) )
        {
            date = DateUtil.parseIsoDate( strValue + " 00:00:00" );
        }

        return ( date != null ) ? date : DateUtil.formatDate( strValue, getLocale( ) );
    }
    /**
     * Gives the JSON feed of the occurrences of the calendars of the workgroups of the user, for the calendar view
     * @param request the request, with the parameters start, end, calendar_id and category_id
     * @return the EventCalendar events, empty when the range is missing or too long
     */
    @View( VIEW_EVENTS_FEED )
    @ResponseBody
    public List<Map<String, Object>> getEventsFeed( HttpServletRequest request )
    {
        return _feedService.getItems( request, _calendarService.getAgendaResources( getUser( ), getPlugin( ) ),
                occurrence -> String.format( URL_MODIFY_OCCURRENCE, occurrence.getIdCalendar( ), occurrence.getEventId( ), occurrence.getId( ) ),
                true );
    }

    /**
     * Moves an occurrence, or its whole series, dropped or resized in the calendar view. The token is the single-use
     * token of this action, sent by the script of the view and renewed in each answer
     * @param request the POST request, with the parameters occurrence_id, start, end, all_day, scope and token
     * @return the outcome ( ok, message ) and the token of the next move
     */
    @Action( value = ACTION_MOVE_OCCURRENCE, securityTokenDisabled = true )
    @ResponseBody
    public Map<String, Object> doMoveOccurrence( HttpServletRequest request )
    {
        String strError = moveOccurrence( request );
        Map<String, Object> result = new java.util.LinkedHashMap<>( );
        result.put( "ok", strError == null );

        if ( strError != null )
        {
            result.put( "message", I18nService.getLocalizedString( strError, getLocale( ) ) );
        }

        result.put( "token", getSecurityTokenService( ).getToken( request, ACTION_MOVE_OCCURRENCE ) );

        return result;
    }

    /**
     * Applies a move of the calendar view
     * @param request the request
     * @return the i18n key of the reason of a refusal, or null when the move is done
     */
    private String moveOccurrence( HttpServletRequest request )
    {
        if ( !getSecurityTokenService( ).validate( request, ACTION_MOVE_OCCURRENCE ) )
        {
            return MESSAGE_INVALID_TOKEN;
        }

        int nOccurrenceId = StringUtil.getIntValue( request.getParameter( Constants.PARAMETER_OCCURRENCE_ID ), -1 );
        OccurrenceEvent occurrence = ( nOccurrenceId > 0 ) ? CalendarHome.findOccurrence( nOccurrenceId, getPlugin( ) ) : null;
        SimpleEvent event = ( occurrence != null ) ? CalendarHome.findEvent( occurrence.getEventId( ), getPlugin( ) ) : null;

        if ( event == null )
        {
            return MESSAGE_ELEMENT_NOT_FOUND;
        }

        if ( _calendarService.getAgendaResources( getUser( ), getPlugin( ) ).stream( )
                .noneMatch( agenda -> String.valueOf( event.getIdCalendar( ) ).equals( agenda.getId( ) ) ) )
        {
            return MESSAGE_CALENDAR_ACCESS_DENIED;
        }

        String strStart = request.getParameter( CalendarEventFeedService.PARAMETER_START );
        String strEnd = request.getParameter( CalendarEventFeedService.PARAMETER_END );
        java.time.LocalDate day = CalendarEventFeedService.parseDate( strStart );

        if ( day == null )
        {
            return Constants.PROPERTY_MESSAGE_DATEFORMAT;
        }

        boolean bAllDay = Boolean.parseBoolean( request.getParameter( PARAMETER_ALL_DAY ) );
        String strTimeStart = bAllDay ? null : getTimeOf( strStart );
        String strTimeEnd = ( bAllDay || !day.equals( CalendarEventFeedService.parseDate( strEnd ) ) ) ? null : getTimeOf( strEnd );

        if ( !bAllDay && strTimeStart == null )
        {
            return Constants.PROPERTY_MESSAGE_TIMEFORMAT;
        }

        if ( SCOPE_SERIES.equals( request.getParameter( PARAMETER_SCOPE ) ) )
        {
            java.time.LocalDate dayBefore = CalendarEventFeedService.getStart( occurrence ).toLocalDate( );
            long lDays = java.time.temporal.ChronoUnit.DAYS.between( dayBefore, day );

            for ( Integer nId : _eventListService.doMoveSeries( event, lDays, strTimeStart, strTimeEnd, getPlugin( ) ) )
            {
                reindexOccurrence( nId );
            }
        }
        else
        {
            _eventListService.doMoveOccurrence( occurrence, event, Utils.toDate( day ), strTimeStart, strTimeEnd, getPlugin( ) );
            reindexOccurrence( occurrence.getId( ) );
        }

        return null;
    }

    /**
     * Reads the time ( HH:mm ) of an ISO date-time sent by the calendar view
     * @param strDateTime the date-time ( 2026-11-12T09:30:00 )
     * @return the time, or null when the value carries no valid time
     */
    private static String getTimeOf( String strDateTime )
    {
        if ( strDateTime == null || strDateTime.length( ) < 16 )
        {
            return null;
        }

        String strTime = strDateTime.substring( 11, 16 );

        return ( CalendarEventFeedService.parseTime( strTime ) != null ) ? strTime : null;
    }

    /**
     * Asks the search indexers to index an occurrence again
     * @param nOccurrenceId the occurrence
     */
    private void reindexOccurrence( int nOccurrenceId )
    {
        IndexationService.addIndexerAction( Integer.toString( nOccurrenceId ),
                AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ), IndexerAction.TASK_MODIFY );
        CalendarIndexerUtils.addIndexerAction( nOccurrenceId, IndexerAction.TASK_MODIFY );
    }
    /**
     * Returns the calendar view of the occurrences: month, week, day and list, with filters, the creation of an
     * event by a click on a day and the move of an occurrence by drag and drop
     * @param request The Http request
     * @return Html page
     */
    @View( VIEW_CALENDAR_VIEW )
    public String getCalendarView( HttpServletRequest request )
    {
        List<AgendaResource> listCalendars = _calendarService.getAgendaResources( getUser( ), getPlugin( ) );
        ReferenceList listCalendarItems = new ReferenceList( );

        for ( AgendaResource calendar : listCalendars )
        {
            listCalendarItems.addItem( calendar.getId( ), calendar.getName( ) );
        }

        ReferenceList listCategoryItems = new ReferenceList( );
        listCategoryItems.addItem( StringUtils.EMPTY, I18nService.getLocalizedString( MESSAGE_ALL_CATEGORIES, getLocale( ) ) );

        for ( Category category : CategoryHome.findAll( getPlugin( ) ) )
        {
            listCategoryItems.addItem( category.getId( ), category.getName( ) );
        }

        Models model = _models;
        model.put( Constants.MARK_CALENDARS_LIST, listCalendars );
        model.put( MARK_CALENDAR_ITEMS, listCalendarItems );
        model.put( MARK_CATEGORY_ITEMS, listCategoryItems );
        model.put( Constants.MARK_LOCALE, getLocale( ).getLanguage( ) );
        model.put( MARK_MOVE_TOKEN, getSecurityTokenService( ).getToken( request, ACTION_MOVE_OCCURRENCE ) );
        model.put( CalendarLibraryService.MARK_CALENDAR_LIBRARY, CalendarLibraryService.getLibrary( getPlugin( ) ) );

        // Opened from one calendar of the list: only that calendar is shown, and the events are created in it
        String strSelectedId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        listCalendars.stream( ).map( AgendaResource::getId ).filter( id -> id.equals( strSelectedId ) ).findFirst( )
                .ifPresent( id -> model.put( MARK_SELECTED_CALENDAR_ID, id ) );

        return getPage( PROPERTY_PAGE_TITLE_CALENDAR_VIEW, TEMPLATE_CALENDAR_VIEW );
    }
    /**
     * The former screen of the "add to calendar" button: its options are now part of the modification of the calendar
     * @param request The Http request, with the parameter calendar_id
     * @return the URL of the modification of the calendar
     * @throws AccessDeniedException if the calendar is not in the workgroups of the user
     */
    @View( VIEW_MODIFY_CALENDAR_BUTTON )
    public String getModifyCalendarButton( HttpServletRequest request ) throws AccessDeniedException
    {
        AgendaResource calendar = getAuthorizedCalendar( request );

        if ( calendar == null )
        {
            return redirectNotFound( request );
        }

        UrlItem url = new UrlItem( getViewUrl( VIEW_MODIFY_CALENDAR ) );
        url.addParameter( Constants.PARAMETER_CALENDAR_ID, calendar.getId( ) );

        return redirect( request, url.getUrl( ) );
    }

    /**
     * Reads the options of the "add to calendar" button posted with the creation or the modification of a calendar
     * @param request The Http request
     * @return the options
     */
    private static CalendarButtonConfig readCalendarButton( HttpServletRequest request )
    {
        CalendarButtonConfig config = new CalendarButtonConfig( );
        String [ ] arrayOptions = request.getParameterValues( PREFIX_CALENDAR_BUTTON + "options" );
        config.setOptions( ( arrayOptions == null ) ? new ArrayList<>( ) : new ArrayList<>( Arrays.asList( arrayOptions ) ) );
        config.setEnabled( request.getParameter( PREFIX_CALENDAR_BUTTON + "enabled" ) != null );
        config.setLabel( request.getParameter( PREFIX_CALENDAR_BUTTON + "label" ) );
        config.setColor( request.getParameter( PREFIX_CALENDAR_BUTTON + "color" ) );
        config.setDisplayMode( request.getParameter( PREFIX_CALENDAR_BUTTON + "displayMode" ) );
        config.setTrigger( request.getParameter( PREFIX_CALENDAR_BUTTON + "trigger" ) );
        config.setShowButtonIcon( request.getParameter( PREFIX_CALENDAR_BUTTON + "showButtonIcon" ) != null );
        config.setShowLinkIcons( request.getParameter( PREFIX_CALENDAR_BUTTON + "showLinkIcons" ) != null );
        config.setButtonsLayout( request.getParameter( PREFIX_CALENDAR_BUTTON + "buttonsLayout" ) );
        config.setButtonStyle( request.getParameter( PREFIX_CALENDAR_BUTTON + "buttonStyle" ) );
        config.setLocation( request.getParameter( PREFIX_CALENDAR_BUTTON + "location" ) );
        config.setAvailability( request.getParameter( PREFIX_CALENDAR_BUTTON + "availability" ) );

        return config;
    }

    /**
     * Reads a time HH:mm of the request
     * @param request The Http request
     * @param strParameter the name of the parameter
     * @return the time, or an empty string when the parameter is missing or is not a time HH:mm
     */
    private static String readTime( HttpServletRequest request, String strParameter )
    {
        String strTime = request.getParameter( strParameter );

        return ( strTime != null && PATTERN_TIME.matcher( strTime ).matches( ) ) ? strTime : StringUtils.EMPTY;
    }

    /**
     * Gives the calendar of the parameter calendar_id when it is in the workgroups of the user
     * @param request The Http request
     * @return the calendar, or null when it does not exist
     * @throws AccessDeniedException if the calendar is not in the workgroups of the user
     */
    private AgendaResource getAuthorizedCalendar( HttpServletRequest request ) throws AccessDeniedException
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        AgendaResource calendar = StringUtils.isNumeric( strCalendarId ) && strCalendarId.length( ) < 10
                ? CalendarHome.findAgendaResource( Integer.parseInt( strCalendarId ), getPlugin( ) )
                : null;

        if ( calendar != null && _calendarService.getAgendaResources( getUser( ), getPlugin( ) ).stream( )
                .noneMatch( agenda -> calendar.getId( ).equals( agenda.getId( ) ) ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }

        return calendar;
    }
    /**
     * Puts in the model, for the calendars of a page of the list, their number of subscribers, of events and of proposals
     * waiting for confirmation, and whether their "add to calendar" button is shown, keyed by the id of the calendar
     * @param model the model
     * @param listCalendars the calendars of the page
     */
    private void putCalendarCounts( Models model, List<AgendaResource> listCalendars )
    {
        Map<String, Integer> mapSubscribers = new HashMap<>( );
        Map<String, Integer> mapEvents = new HashMap<>( );
        Map<String, Boolean> mapButton = new HashMap<>( );
        Map<String, Integer> mapProposals = new HashMap<>( );

        for ( AgendaResource calendar : listCalendars )
        {
            int nCalendarId = StringUtil.getIntValue( calendar.getId( ), -1 );
            mapSubscribers.put( calendar.getId( ), CalendarSubscriberHome.findSubscriberNumber( nCalendarId, getPlugin( ) ) );
            mapEvents.put( calendar.getId( ), CalendarHome.findEventsList( nCalendarId, 1, getPlugin( ) ).size( ) );
            mapButton.put( calendar.getId( ), CalendarButtonHome.findByCalendar( nCalendarId, getPlugin( ) ).isEnabled( ) );
            mapProposals.put( calendar.getId( ), EventProposals.getProposedEventIds( nCalendarId, getPlugin( ) ).size( ) );
        }

        model.put( MARK_SUBSCRIBER_COUNTS, mapSubscribers );
        model.put( MARK_EVENT_COUNTS, mapEvents );
        model.put( MARK_BUTTON_ENABLED, mapButton );
        model.put( MARK_PROPOSAL_COUNTS, mapProposals );
    }
}
