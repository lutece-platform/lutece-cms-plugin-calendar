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

import fr.paris.lutece.plugins.calendar.service.EventProposals;
import fr.paris.lutece.plugins.calendar.business.Agenda;
import fr.paris.lutece.plugins.calendar.business.CalendarSubscriber;
import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.business.MultiAgenda;
import fr.paris.lutece.plugins.calendar.service.EventDisplayData;
import fr.paris.lutece.plugins.calendar.service.MapPreview;
import fr.paris.lutece.plugins.calendar.service.EventStructuredDataService;
import fr.paris.lutece.plugins.calendar.business.details.EventDetailsHome;
import fr.paris.lutece.plugins.calendar.business.details.EventDetails;
import fr.paris.lutece.plugins.calendar.service.CalendarIcalFeedService;
import fr.paris.lutece.plugins.calendar.service.CalendarEventFeedService;
import fr.paris.lutece.plugins.calendar.service.CalendarButtonService;
import fr.paris.lutece.plugins.calendar.business.button.CalendarButtonHome;
import fr.paris.lutece.plugins.calendar.business.button.CalendarButtonConfig;
import fr.paris.lutece.plugins.calendar.business.category.CategoryHome;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.business.category.Category;
import fr.paris.lutece.plugins.calendar.service.AgendaResource;
import fr.paris.lutece.plugins.calendar.service.AgendaSubscriberService;
import fr.paris.lutece.plugins.calendar.service.CalendarLibraryService;
import fr.paris.lutece.plugins.calendar.service.CalendarService;
import fr.paris.lutece.plugins.calendar.service.CategoryService;
import fr.paris.lutece.plugins.calendar.service.EventImageResourceService;
import fr.paris.lutece.plugins.calendar.service.EventListService;
import fr.paris.lutece.plugins.calendar.service.Utils;
import fr.paris.lutece.plugins.calendar.service.search.CalendarSearchService;
import fr.paris.lutece.portal.service.captcha.ICaptchaService;
import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.mail.MailService;
import fr.paris.lutece.portal.service.message.SiteMessage;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.message.SiteMessageService;
import fr.paris.lutece.portal.service.page.PageNotFoundException;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.SecurityTokenService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.template.HtmlMarkup;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.util.BeanUtils;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.portal.web.xpages.XPageApplication;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.html.HtmlTemplate;
import fr.paris.lutece.util.html.Paginator;
import fr.paris.lutece.util.string.StringUtil;
import fr.paris.lutece.util.url.UrlItem;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;


/**
 * This class is the main class of the XPage application of the plugin calendar. It is request scoped: it holds no state
 * between requests (the page of the search results comes from the request), so no session restores it with its
 * injected services missing.
 * 
 */
@RequestScoped
@Named( "calendar.xpage.calendar" )
public class CalendarApp implements XPageApplication
{
    public static final String PROPERTY_INVALID_MAIL_TITLE_MESSAGE = "calendar.siteMessage.invalid_mail.title";
    public static final String PROPERTY_INVALID_MAIL_ERROR_MESSAGE = "calendar.siteMessage.invalid_mail.message";

    /** serial id */
    private static final String MESSAGE_INVALID_TOKEN = "calendar.message.invalidToken";
    private static final long serialVersionUID = 3326962245456630378L;

    //Templates
    private static final String TEMPLATE_CALENDAR = "skin/plugins/calendar/calendar.html";
    private static final String MARK_CALENDAR_VIEW = "calendar_view";
    private static final String MARK_LOCALE = "locale";
    private static final String MARK_CATEGORY_LIST = "category_list";
    private static final String MARK_ICAL_LIST = "ical_list";
    private static final String MARK_EXPORT_LIST = "export_list";
    private static final String MARK_CALENDAR_BUTTON = "calendarButton";
    private static final String MARK_CALENDAR_LINKS = "calendarLinks";
    private static final String MARK_CALENDAR_EVENT = "calendarEvent";
    private static final String MARK_EVENT_DETAILS = "event_details";
    private static final String MARK_EVENT_STATUS = "event_status";
    private static final String MARK_EVENT_JSON_LD = "event_json_ld";
    private static final String MARK_EVENT_HAS_IMAGE = "event_has_image";
    private static final String MARK_SEARCH_RESULTS = "search_results";
    private static final String MARK_SEARCH_FILTERED = "search_filtered";
    private static final String MARK_TAG = "search_tag";
    private static final String MARK_SUBSCRIPTION_AGENDA = "subscription_agenda";
    private static final String MARK_URL_WITHOUT_TAG = "url_without_tag";
    private static final String PARAMETER_TAG = "tag";
    private static final String TAG_PREFIX = "#";
    private static final String KEY_IMAGE_URL = "imageUrl";
    private static final String KEY_HAS_IMAGE = "hasImage";
    private static final String KEY_TITLE = "title";
    private static final String MARK_PAGE_AGENDA = "page_agenda";
    private static final String MARK_PAGE_AGENDA_IDS = "page_agenda_ids";
    private static final String MARK_UPCOMING_EVENTS = "upcoming_events";
    private static final String MARK_MONTH_SEARCH_URL = "month_search_url";
    private static final String MESSAGE_PROPOSAL_NOT_ALLOWED = "calendar.event_proposal.notAllowed";
    private static final String MARK_MANAGED_CALENDAR_IDS = "managed_calendar_ids";
    private static final String MARK_PROPOSAL_CALENDAR_IDS = "proposal_calendar_ids";
    private static final String MESSAGE_PROPOSAL_SENT = "calendar.event_proposal.sent";
    private static final String URL_CALENDAR_OF = "jsp/site/Portal.jsp?page=calendar&agenda=%d";
    private static final String MESSAGE_INVALID_LINK = "calendar.create_event_front.invalidLink";
    private static final int LINK_MAX_LENGTH = 130;
    private static final java.util.regex.Pattern PATTERN_LINK = java.util.regex.Pattern.compile( "^https?://\\S+$", java.util.regex.Pattern.CASE_INSENSITIVE );
    private static final int UPCOMING_EVENTS_COUNT = 3;
    private static final int UPCOMING_EVENTS_DAYS = 90;
    private static final String MARK_EVENT_CATEGORIES = "event_categories";
    private static final String MARK_EVENT_NEXT_OCCURRENCES = "event_next_occurrences";
    private static final String MARK_EVENT_OCCURRENCES_COUNT = "event_occurrences_count";
    private static final String MARK_EVENT_MAP_URL = "event_map_url";
    private static final String MARK_EVENT_MAP_TILES = "event_map_tiles";
    private static final String URL_EVENT_OSM = "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f";
    private static final int MAX_NEXT_OCCURRENCES = 8;
    private static final String URL_SHOW_EVENT = "jsp/site/Portal.jsp?page=calendar&action=show_result&event_id=%d&agenda=%d";
    private static final String URL_ICAL_OCCURRENCE = "jsp/site/plugins/calendar/CalendarIcal.jsp?occurrence_id=";
    private static final String URL_EXPORT = "jsp/site/plugins/calendar/GetCalendarFile.jsp?calendar_id=";
    private static final String URL_RSS = "jsp/site/plugins/calendar/GetCalendarFile.jsp?action=rss&calendar_id=";
    private static final String URL_ICAL = "jsp/site/plugins/calendar/CalendarIcal.jsp?calendar_id=";
    private static final java.util.Set<String> VIEW_KEYS = java.util.Set.of( "day", "week", "month", "list" );
    private static final String TEMPLATE_CALENDAR_LEGEND = "skin/plugins/calendar/calendar_legend.html";
    private static final String TEMPLATE_CALENDAR_MANAGE_EVENTS = "skin/plugins/calendar/calendar_manage_events.html";
    private static final String TEMPLATE_CREATE_EVENT_FRONT = "skin/plugins/calendar/create_event_front.html";
    private static final String TEMPLATE_MODIFY_EVENT_FRONT = "skin/plugins/calendar/modify_event_front.html";
    private static final String TEMPLATE_SEND_NOTIFICATION_MAIL = "skin/plugins/calendar/notification_email.html";
    private static final String TEMPLATE_DO_SEARCH_EVENTS = "skin/plugins/calendar/search/dosearch_event_results.html";
    private static final String TEMPLATE_SHOW_RESULT = "skin/plugins/calendar/search/show_result.html";
    private static final String TEMPLATE_SEARCH_EVENTS = "skin/plugins/calendar/search/search_events.html";
    private static final String TEMPLATE_SUBSCRIPTION_FORM = "skin/plugins/calendar/subscription_form.html";
    private static final String TEMPLATE_EMAIL_FRIEND = "skin/plugins/calendar/email_friend.html";
    private static final String TEMPLATE_DOWNLOAD_CALENDAR = "skin/plugins/calendar/download_calendar.html";

    //Properties
    private static final String PROPERTY_PLUGIN_NAME = "calendar";

    //private static final String PROPERTY_FEATURE_URL = "?page=calendar&action=do_search";
    private static final String PROPERTY_UTIL_DOCUMENT_CLASS = "fr.paris.lutece.plugins.calendar.modules.document.web.DocumentCalendarUtils";
    private static final String PROPERTY_EMAIL_FRIEND_OBJECT = "calendar.friend.email.object";

    //Front Messages
    private static final String PROPERTY_CONFIRM_REMOVE_TITLE_MESSAGE = "calendar.siteMessage.confirmRemove.title";
    private static final String PROPERTY_CONFIRM_REMOVE_ALERT_MESSAGE = "calendar.siteMessage.confirmRemove.alertMessage";
    private static final String PROPERTY_INVALID_DATE_TITLE_MESSAGE = "calendar.siteMessage.invalidDate.title";
    private static final String PROPERTY_INVALID_DATE_MESSAGE = "calendar.siteMessage.invalidDate.alertMessage";
    private static final String PROPERTY_INVALID_TIME_TITLE_MESSAGE = "calendar.siteMessage.invalidTime.title";
    private static final String PROPERTY_INVALID_TIME_MESSAGE = "calendar.siteMessage.invalidTime.alertMessage";
    private static final String PROPERTY_CAPTCHA_INVALID_MESSAGE = "calendar.siteMessage.invalid_captcha.message";
    private static final String PROPERTY_CAPTCHA_INVALID_TITLE_MESSAGE = "calendar.siteMessage.invalid_captcha.title";
    private static final String JSP_PAGE_PORTAL = "jsp/site/Portal.jsp";

    //JSP
    private static final String URL_JSP_RETURN_SEND_FRIEND_MAIL = "../../Portal.jsp?page=calendar&action=";

    //Mark
    private static final String MARK_BASE_URL = "base_url";
    private static final String MARK_SENDER_MESSAGE = "sender_message";

    //Session
    private int _nDefaultItemsPerPage = AppPropertiesService.getPropertyInt( Constants.PROPERTY_EVENTS_PER_PAGE, 10 );
    private String _strCurrentPageIndex;
    private int _nItemsPerPage;
    @Inject
    private EventStructuredDataService _structuredDataService;

    @Inject
    @Named( BeanUtils.BEAN_CAPTCHA_SERVICE )
    private Instance<ICaptchaService> _captchaService;
    @Inject
    private AgendaSubscriberService _agendaSubscriberService;
    @Inject
    private CategoryService _categoryService;
    @Inject
    private EventListService _eventListService;
    @Inject
    private CalendarService _calendarService;
    @Inject
    private CalendarSearchService _calendarSearchService;
    @Inject
    private SecurityTokenService _securityTokenService;

    /**
     * Returns the content of the page Contact. It is composed by a form which
     * to capture the data to send a message to
     * a contact of the portal.
     * 
     * @return the Content of the page Contact
     * @param request The http request
     * @param nMode The current mode
     * @param plugin The plugin object
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException A
     *             message exception treated on front
     * @throws UserNotSignedException user not signed
     */
    public XPage getPage( HttpServletRequest request, int nMode, Plugin plugin ) throws SiteMessageException,
            UserNotSignedException
    {
        XPage page = null;

        String strAction = request.getParameter( Constants.PARAMETER_ACTION );
        String strPluginName = request.getParameter( Constants.PARAMETER_PAGE );
        if ( StringUtils.isBlank( strPluginName ) )
        {
            strPluginName = Constants.PLUGIN_NAME;
        }

        if ( StringUtils.isNotBlank( strAction ) )
        {
            if ( Constants.ACTION_MANAGE_EVENTS.equals( strAction ) )
            {
                page = getManageEventsPage( request, plugin );
            }
            else if ( Constants.ACTION_ADD_EVENT.equals( strAction ) )
            {
                page = getAddEventPage( request, plugin );
            }
            else if ( Constants.ACTION_DO_CREATE_EVENT.equals( strAction ) )
            {
                checkSecurityToken( request, Constants.ACTION_DO_CREATE_EVENT );
                doCreateEvent( request, plugin );
                page = getManageEventsPage( request, plugin );
            }
            else if ( Constants.ACTION_MODIFY_EVENT.equals( strAction ) )
            {
                page = getModifyEventPage( request, plugin );
            }
            else if ( Constants.ACTION_DO_MODIFY_EVENT.equals( strAction ) )
            {
                checkSecurityToken( request, Constants.ACTION_DO_MODIFY_EVENT );
                doModifyEvent( request, plugin );
                page = getManageEventsPage( request, plugin );
            }
            else if ( Constants.ACTION_REMOVE_EVENT.equals( strAction ) )
            {
                getRemoveEvent( request, plugin );
            }
            else if ( Constants.ACTION_DO_REMOVE_EVENT.equals( strAction ) )
            {
                checkSecurityToken( request, Constants.ACTION_DO_REMOVE_EVENT );
                doRemoveEvent( request, plugin );
                page = getManageEventsPage( request, plugin );
            }
            else if ( Constants.ACTION_GET_SUBSCRIPTION_PAGE.equals( strAction ) )
            {
                page = getSubscriptionPage( request, plugin );
            }
            else if ( Constants.ACTION_GET_FRIEND_EMAIL_PAGE.equals( strAction ) )
            {
                page = getGetFriendEmailPage( request, plugin );
            }
            else if ( Constants.ACTION_GET_DOWNLOAD_PAGE.equals( strAction ) )
            {
                page = getDownloadPage( request, plugin );
            }
            else if ( Constants.ACTION_SEND_FRIEND_EMAIL.equals( strAction ) )
            {
                _agendaSubscriberService.sendFriendMail( request );
            }
            else if ( Constants.ACTION_VERIFY_SUBSCRIBE.equals( strAction ) )
            {
                doVerifySubscription( request, plugin );
            }
            else if ( Constants.ACTION_CONFIRM_UNSUBSCRIBE.equals( strAction ) )
            {
                getConfirmUnSubscribe( request );
            }
            else if ( Constants.ACTION_UNSUBSCRIBE.equals( strAction ) )
            {
                _agendaSubscriberService.doUnSubscribe( request, plugin );
            }
            else if ( Constants.ACTION_SEARCH.equals( strAction ) )
            {
                page = getSearchPage( request, plugin );
            }
            else if ( Constants.ACTION_DO_SEARCH.equals( strAction ) )
            {
                page = getSearchResultPage( request, plugin );
            }
            else if ( Constants.ACTION_SHOW_RESULT.equals( strAction ) )
            {
                page = getShowResultPage( request, plugin );
            }
            else if ( Constants.ACTION_RSS.equals( strAction ) )
            {
                page = getRssPage( request, plugin );
            }
        }

        if ( page == null )
        {
            page = getDefaultPage( request, plugin );
        }

        return page;
    }

    /**
     * Get the default XPage
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     */
    private XPage getDefaultPage( HttpServletRequest request, Plugin plugin )
    {
        XPage page = new XPage( );
        // Gets calendar infos from the request parameters and session
        CalendarView view = getView( request );

        MultiAgenda agendaWithOccurences = keepRequestedAgenda( _calendarService.getMultiAgenda( request ), request.getParameter( Constants.PARAM_AGENDA ) );
        String strDate = getDate( request );
        CalendarUserOptions options = getUserOptions( request );
        options.setShowSearchEngine( Boolean.TRUE );

        // Load and fill the page template
        Map<String, Object> model = new HashMap<String, Object>( );
        model.put( Constants.MARK_PREVIOUS, view.getPrevious( strDate ) );
        model.put( Constants.MARK_NEXT, view.getNext( strDate ) );
        model.put( Constants.MARK_TITLE, view.getTitle( strDate, options ) );
        model.put( Constants.MARK_DATE, strDate );
        model.put( Constants.MARK_LEGEND, HtmlMarkup.of( getLegend( request, agendaWithOccurences, options ) ) );
        model.put( MARK_CALENDAR_VIEW, getViewKey( request, agendaWithOccurences ) );
        model.put( MARK_LOCALE, options.getLocale( ).getLanguage( ) );
        model.put( CalendarLibraryService.MARK_CALENDAR_LIBRARY, CalendarLibraryService.getLibrary( plugin ) );
        putPageHeader( model, agendaWithOccurences, request, plugin );

        model.put( MARK_MONTH_SEARCH_URL, getMonthSearchUrl( strDate, agendaWithOccurences ) );
        putAdvancedSearch( model, agendaWithOccurences, request, plugin );

        String strRunAppJspUrl = AppPropertiesService.getProperty( Constants.PROPERTY_RUNAPP_JSP_URL );
        model.put( Constants.MARK_JSP_URL, strRunAppJspUrl );

        // Set XPage data
        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_CALENDAR, options.getLocale( ), model );
        page.setContent( template.getHtml( ) );
        page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_TITLE, options.getLocale( ) )
                + view.getTitle( strDate, options ) );
        page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PATH, options.getLocale( ) )
                + view.getPath( strDate, options ) );

        return page;
    }

    /**
     * Reads the optional link of an event added from the site: an http or https address of at most 130 characters
     * @param request the request, with the parameter event_link_url
     * @return the link, or an empty string when none is given
     * @throws SiteMessageException when the link is not an http or https address or is too long
     */
    private static String readEventLink( HttpServletRequest request ) throws SiteMessageException
    {
        String strLink = org.apache.commons.text.StringEscapeUtils.unescapeHtml4( StringUtils.trimToEmpty( request.getParameter( Constants.PARAMETER_EVENT_LINK_URL ) ) );

        if ( !strLink.isEmpty( ) && ( ( strLink.length( ) > LINK_MAX_LENGTH ) || !PATTERN_LINK.matcher( strLink ).matches( ) ) )
        {
            SiteMessageService.setMessage( request, MESSAGE_INVALID_LINK, SiteMessage.TYPE_STOP );
        }

        return strLink;
    }

    /**
     * Reads the optional category of an event added from the site
     * @param request the request, with the parameter category
     * @param plugin the plugin
     * @return the category chosen, or an empty list when none is chosen or the category does not exist
     */
    private static List<Category> readEventCategory( HttpServletRequest request, Plugin plugin )
    {
        String strCategoryId = request.getParameter( Constants.PARAMETER_CATEGORY );
        Category category = StringUtils.isNumeric( strCategoryId ) && ( strCategoryId.length( ) < 10 ) ? CategoryHome.find( Integer.parseInt( strCategoryId ), plugin )
                : null;

        return ( category != null ) ? new ArrayList<>( List.of( category ) ) : new ArrayList<>( );
    }

    /**
     * Tells whether a visitor may see a calendar: a calendar without role, or one whose role or manager role the visitor has
     * @param agenda the calendar
     * @param request the request
     * @return true when the visitor may see the calendar
     */
    private boolean isVisibleTo( AgendaResource agenda, HttpServletRequest request )
    {
        String strRole = agenda.getRole( );

        return StringUtils.isBlank( strRole ) || Constants.PROPERTY_ROLE_NONE.equals( strRole ) || hasRole( strRole, request )
                || hasManagerRole( agenda, request );
    }

    /**
     * Checks that the user may add an event to a calendar from the site: a manager of the calendar (its manager role)
     * publishes directly; when the calendar accepts proposals, a logged-in user proposes an event that waits for the
     * confirmation of an administrator; anybody else is refused
     * @param agenda the calendar
     * @param request the request
     * @return true when the event is published directly, false when it is a proposal
     * @throws SiteMessageException when the user may not add an event to the calendar
     * @throws UserNotSignedException when the calendar accepts proposals and the user is not logged in
     */
    private boolean checkCanAddEvent( AgendaResource agenda, HttpServletRequest request ) throws SiteMessageException, UserNotSignedException
    {
        if ( ( agenda != null ) && hasManagerRole( agenda, request ) )
        {
            return true;
        }

        if ( ( agenda == null ) || !agenda.isFrontProposals( ) || !SecurityService.isAuthenticationEnable( ) )
        {
            SiteMessageService.setMessage( request, MESSAGE_PROPOSAL_NOT_ALLOWED, SiteMessage.TYPE_STOP );
        }

        if ( SecurityService.getInstance( ).getRemoteUser( request ) == null )
        {
            throw new UserNotSignedException( );
        }

        return false;
    }

    /**
     * Keeps only the calendar asked by the parameter agenda of the calendar page, when the user may see it: the page, its
     * legend, its quick search and its feed then show that calendar alone
     * @param multiAgenda the calendars the user may see
     * @param strAgendaId the identifier of the calendar asked, or null
     * @return the calendar asked alone, or all the calendars when none is asked or the one asked is not among them
     */
    private static MultiAgenda keepRequestedAgenda( MultiAgenda multiAgenda, String strAgendaId )
    {
        if ( ( multiAgenda == null ) || !StringUtils.isNumeric( strAgendaId ) )
        {
            return multiAgenda;
        }

        return multiAgenda.getAgendas( ).stream( ).filter( agenda -> ( agenda != null ) && strAgendaId.equals( agenda.getKeyName( ) ) ).findFirst( )
                .map( agenda -> {
                    MultiAgenda single = new MultiAgenda( );
                    single.setName( multiAgenda.getName( ) );
                    single.setKeyName( multiAgenda.getKeyName( ) );
                    single.addAgenda( agenda );
                    return single;
                } ).orElse( multiAgenda );
    }

    /**
     * Returns the URL of the search results of the month of a day, for the calendars shown: the list of the events offered
     * when the EventCalendar view cannot run (no JavaScript). It replaces the server-side month list, which ran one search
     * per day of the month grid on every display of the page
     * @param strDate the day shown, yyyyMMdd
     * @param multiAgenda the calendars shown
     * @return the URL of the search
     */
    private static String getMonthSearchUrl( String strDate, MultiAgenda multiAgenda )
    {
        java.time.LocalDate day = java.time.LocalDate.parse( strDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE );
        UrlItem url = new UrlItem( JSP_PAGE_PORTAL );
        url.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        url.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_DO_SEARCH );
        url.addParameter( Constants.PARAMETER_PERIOD, Constants.PROPERTY_PERIOD_RANGE );
        url.addParameter( Constants.PARAMETER_DATE_START, day.withDayOfMonth( 1 ).toString( ) );
        url.addParameter( Constants.PARAMETER_DATE_END, day.withDayOfMonth( day.lengthOfMonth( ) ).toString( ) );

        if ( multiAgenda != null )
        {
            multiAgenda.getAgendas( ).stream( ).filter( java.util.Objects::nonNull )
                    .forEach( agenda -> url.addParameter( Constants.PARAMETER_CALENDAR_ID, agenda.getKeyName( ) ) );
        }

        return url.getUrl( );
    }

    /**
     * Puts in the model of the calendar page the lists of the advanced search: the calendars, the ones shown being checked,
     * and the categories
     * @param model the model of the page
     * @param multiAgenda the calendars shown
     * @param request the request
     * @param plugin the plugin
     */
    private void putAdvancedSearch( Map<String, Object> model, MultiAgenda multiAgenda, HttpServletRequest request, Plugin plugin )
    {
        ReferenceList listAgendas = getListAgenda( request, plugin );

        if ( multiAgenda != null )
        {
            listAgendas.checkItems( multiAgenda.getAgendas( ).stream( ).filter( java.util.Objects::nonNull ).map( Agenda::getKeyName ).toArray( String [ ]::new ) );
        }

        model.put( Constants.MARK_CALENDARS_LIST, listAgendas );
        model.put( Constants.MARK_CATEGORY_LIST, getReferenceListCategory( _categoryService.getCategories( plugin ) ) );
    }

    /**
     * Puts in the model of the calendar page its header: the calendar shown when there is only one, the identifiers of
     * the calendars shown (for the quick search) and the next events of those calendars, read from their occurrences in one
     * query (the search index would load every hit)
     * @param model the model of the page
     * @param multiAgenda the calendars shown
     * @param request the request
     * @param plugin the plugin
     */
    private void putPageHeader( Map<String, Object> model, MultiAgenda multiAgenda, HttpServletRequest request, Plugin plugin )
    {
        List<String> listAgendaIds = ( multiAgenda == null ) ? List.of( )
                : multiAgenda.getAgendas( ).stream( ).filter( java.util.Objects::nonNull ).map( Agenda::getKeyName ).toList( );

        if ( listAgendaIds.size( ) == 1 )
        {
            model.put( MARK_PAGE_AGENDA, _calendarService.getAgendaResource( listAgendaIds.get( 0 ) ) );
        }

        model.put( MARK_PAGE_AGENDA_IDS, listAgendaIds );

        List<Integer> listIds = listAgendaIds.stream( ).filter( StringUtils::isNumeric ).map( Integer::valueOf ).toList( );

        if ( !listIds.isEmpty( ) )
        {
            java.time.LocalDate today = java.time.LocalDate.now( );
            Map<Integer, OccurrenceEvent> mapNext = new java.util.LinkedHashMap<>( );

            for ( OccurrenceEvent occurrence : EventProposals.publicOnly(
                    CalendarHome.findOccurrencesByDateRange( listIds, today, today.plusDays( UPCOMING_EVENTS_DAYS ), List.of( ), plugin ) ) )
            {
                mapNext.putIfAbsent( occurrence.getEventId( ), occurrence );

                if ( mapNext.size( ) == UPCOMING_EVENTS_COUNT )
                {
                    break;
                }
            }

            List<Map<String, Object>> listUpcoming = new ArrayList<>( );

            for ( OccurrenceEvent occurrence : mapNext.values( ) )
            {
                SimpleEvent event = _eventListService.getEvent( occurrence.getEventId( ), plugin );
                AgendaResource agenda = _calendarService.getAgendaResource( occurrence.getIdCalendar( ) );

                if ( ( event != null ) && ( agenda != null ) )
                {
                    event.setStatus( occurrence.getStatus( ) );
                    event.setDateTimeStart( occurrence.getDateTimeStart( ) );
                    event.setDateTimeEnd( occurrence.getDateTimeEnd( ) );
                    Map<String, Object> mapEvent = EventDisplayData.build( event, agenda, occurrence.getDate( ), request.getLocale( ), plugin );
                    mapEvent.put( KEY_TITLE, StringUtils.defaultString( event.getTitle( ) ) );
                    listUpcoming.add( mapEvent );
                }
            }

            model.put( MARK_UPCOMING_EVENTS, listUpcoming );
        }
    }

    /**
     * Get the XPage for managing event
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     * @throws SiteMessageException message if error
     */
    private XPage getManageEventsPage( HttpServletRequest request, Plugin plugin ) throws SiteMessageException
    {
        XPage page = null;

        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            page = new XPage( );
            int nCalendarId = Integer.parseInt( strCalendarId );

            // The sort function is not used yet
            /*
             * String strSortEvents = request.getParameter(
             * Constants.PARAMETER_SORT_EVENTS );
             * int nSortEvent = Constants.SORT_ASC;
             * if ( StringUtils.isNotBlank( strSortEvents ) &&
             * StringUtils.isNumeric( strSortEvents ) )
             * {
             * nSortEvent = Integer.parseInt( strSortEvents );
             * }
             */

            AgendaResource agenda = _calendarService.getAgendaResource( nCalendarId );

            List<SimpleEvent> listEvents = null;
            // Check security access
            if ( hasManagerRole( agenda, request ) )
            {
                listEvents = _eventListService.getSimpleEvents( nCalendarId, Constants.SORT_ASC );
            }
            else
            {
                LuteceUser user;
                try
                {
                    user = getUser( request );
                    if ( user != null )
                    {
                        listEvents = _eventListService.getSimpleEventsByUserLogin( nCalendarId, user );
                    }
                }
                catch ( UserNotSignedException ue )
                {
                    listEvents = new ArrayList<SimpleEvent>( );
                }
                catch ( PageNotFoundException pe )
                {
                    listEvents = new ArrayList<SimpleEvent>( );
                }
            }

            Map<String, Object> model = new HashMap<String, Object>( );

            //Fetch the name of the calendar to be modified
            model.put( Constants.MARK_CALENDAR, agenda );
            model.put( Constants.MARK_EVENT_LIST, listEvents );

            HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_CALENDAR_MANAGE_EVENTS,
                    request.getLocale( ), model );

            page.setContent( template.getHtml( ) );
            page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_MANAGE_EVENTS,
                    request.getLocale( ) ) );
            page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_MANAGE_EVENTS,
                    request.getLocale( ) ) );
        }
        return page;
    }

    /**
     * Return unregistered form if unregistered user wants to acces application
     * management form
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     * @throws SiteMessageException message if error, or when the user may not add an event to the calendar
     * @throws UserNotSignedException when the calendar accepts proposals and the user is not logged in
     */
    private XPage getAddEventPage( HttpServletRequest request, Plugin plugin ) throws SiteMessageException, UserNotSignedException
    {
        XPage page = null;
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            checkCanAddEvent( _calendarService.getAgendaResource( Integer.parseInt( strCalendarId ) ), request );
            page = new XPage( );

            Map<String, Object> model = new HashMap<String, Object>( );

            model.put( Constants.MARK_CALENDAR_ID, strCalendarId );
            model.put( Constants.MARK_LOCALE, request.getLocale( ).getLanguage( ) );

            model.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, Constants.ACTION_DO_CREATE_EVENT ) );
            model.put( MARK_CATEGORY_LIST, CategoryHome.findAll( plugin ) );
            HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_CREATE_EVENT_FRONT, request.getLocale( ),
                    model );

            page.setContent( template.getHtml( ) );
            page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_CREATE_EVENT,
                    request.getLocale( ) ) );
            page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_CREATE_EVENT,
                    request.getLocale( ) ) );
        }

        return page;

    }

    /**
     * Get the XPage for modifying an event
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the hmlt form
     * @throws SiteMessageException message if error
     * @throws UserNotSignedException exception if user is not connected
     */
    private XPage getModifyEventPage( HttpServletRequest request, Plugin plugin ) throws SiteMessageException,
            UserNotSignedException
    {
        XPage page = null;
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            int nEventId = Integer.parseInt( strEventId );
            if ( verifiyUserAccess( request, nCalendarId, nEventId, plugin ) )
            {
                HashMap<String, Object> model = new HashMap<String, Object>( );
                model.put( Constants.MARK_EVENT, _eventListService.getEvent( nEventId, plugin ) );
                model.put( Constants.MARK_CALENDAR_ID, nCalendarId );
                model.put( Constants.MARK_DEFAULT_SORT_EVENT, request.getParameter( Constants.PARAMETER_SORT_EVENTS ) );
                model.put( Constants.MARK_LOCALE, request.getLocale( ).getLanguage( ) );

            model.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, Constants.ACTION_DO_MODIFY_EVENT ) );
                HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_MODIFY_EVENT_FRONT,
                        request.getLocale( ), model );

                page = new XPage( );
                page.setContent( template.getHtml( ) );
                page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_MODIFY_EVENT,
                        request.getLocale( ) ) );
                page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_MODIFY_EVENT,
                        request.getLocale( ) ) );
            }
            else
            {
                SiteMessageService.setMessage( request, Messages.USER_ACCESS_DENIED, SiteMessage.TYPE_STOP );
            }
        }

        return page;
    }

    /**
     * The method calling the remove action
     * 
     * @param request The HttpRequest
     * @param plugin The plugin
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException
     *             Exception used by the front Message mechanism
     * @throws UserNotSignedException user not signed
     */
    private void getRemoveEvent( HttpServletRequest request, Plugin plugin ) throws SiteMessageException,
            UserNotSignedException
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            int nEventId = Integer.parseInt( strEventId );
            if ( verifiyUserAccess( request, nCalendarId, nEventId, plugin ) )
            {
                UrlItem url = new UrlItem( JSP_PAGE_PORTAL );
                url.addParameter( Constants.PARAMETER_PAGE, PROPERTY_PLUGIN_NAME );
                url.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_DO_REMOVE_EVENT );
                url.addParameter( Constants.PARAMETER_CALENDAR_ID,
                        request.getParameter( Constants.PARAMETER_CALENDAR_ID ) );
                url.addParameter( Constants.PARAMETER_EVENT_ID, request.getParameter( Constants.PARAMETER_EVENT_ID ) );
                url.addParameter( SecurityTokenService.PARAMETER_TOKEN, _securityTokenService.getToken( request, Constants.ACTION_DO_REMOVE_EVENT ) );
                SiteMessageService.setMessage( request, PROPERTY_CONFIRM_REMOVE_ALERT_MESSAGE, null,
                        PROPERTY_CONFIRM_REMOVE_TITLE_MESSAGE, url.getUrl( ), null, SiteMessage.TYPE_CONFIRMATION );
            }
            else
            {
                SiteMessageService.setMessage( request, Messages.USER_ACCESS_DENIED, SiteMessage.TYPE_STOP );
            }
        }
    }

    /**
     * Get the XPage for subscribing
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     */
    private XPage getSubscriptionPage( HttpServletRequest request, Plugin plugin )
    {
        XPage page = new XPage( );
        boolean bIsCaptchaEnabled = _captchaService.isResolvable( );

        String strBaseUrl = AppPathService.getBaseUrl( request );
        UrlItem url = new UrlItem( strBaseUrl + JSP_PAGE_PORTAL );
        url.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        url.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_VERIFY_SUBSCRIBE );

        Map<String, Object> model = new HashMap<String, Object>( );
        model.put( Constants.MARK_JSP_URL, url.getUrl( ) );
        model.put( Constants.MARK_LOCALE, request.getLocale( ) );
        ReferenceList listAgendas = getListAgenda( request, plugin );
        String strAgendaId = request.getParameter( Constants.PARAM_AGENDA );
        model.put( Constants.MARK_CALENDARS_LIST, listAgendas );
        model.put( MARK_SUBSCRIPTION_AGENDA, listAgendas.stream( ).filter( item -> item.getCode( ).equals( strAgendaId ) ).findFirst( ).orElse( null ) );
        model.put( Constants.MARK_IS_ACTIVE_CAPTCHA, bIsCaptchaEnabled );

        if ( bIsCaptchaEnabled )
        {
            model.put( Constants.MARK_CAPTCHA, HtmlMarkup.of( _captchaService.get( ).getHtmlCode( ) ) );
        }

        HtmlTemplate template = AppTemplateService
                .getTemplate( TEMPLATE_SUBSCRIPTION_FORM, request.getLocale( ), model );

        page.setContent( template.getHtml( ) );
        page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_SUBSCRIPTION_TITLE, request.getLocale( ) ) );
        page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_SUBSCRIPTION_TITLE,
                request.getLocale( ) ) );

        return page;
    }

    /**
     * Get the XPage for sending a email to a friend
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     */
    private XPage getGetFriendEmailPage( HttpServletRequest request, Plugin plugin )
    {
        XPage page = null;
        String strCalendarId = request.getParameter( Constants.PARAM_AGENDA );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            page = new XPage( );
            Map<String, Object> model = new HashMap<String, Object>( );
            model.put( Constants.MARK_LOCALE, request.getLocale( ) );
            model.put( Constants.MARK_EVENT_ID, Integer.parseInt( strEventId ) );
            model.put( Constants.MARK_CALENDAR_ID, Integer.parseInt( strCalendarId ) );

            HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_EMAIL_FRIEND, request.getLocale( ), model );

            page.setContent( template.getHtml( ) );
            page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_EMAIL_FRIEND_TITLE,
                    request.getLocale( ) ) );
            page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_EMAIL_FRIEND_TITLE,
                    request.getLocale( ) ) );
        }

        return page;
    }

    /**
     * Get the XPage for downloading an event
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     */
    private XPage getDownloadPage( HttpServletRequest request, Plugin plugin )
    {
        XPage page = new XPage( );
        Map<String, Object> model = new HashMap<String, Object>( );
        model.put( Constants.MARK_LOCALE, request.getLocale( ) );
        model.put( Constants.MARK_CALENDARS_LIST, getListAgenda( request, plugin ) );
        model.put( MARK_EXPORT_LIST, getExportLinks( request ) );

        HtmlTemplate template = AppTemplateService
                .getTemplate( TEMPLATE_DOWNLOAD_CALENDAR, request.getLocale( ), model );

        page.setContent( template.getHtml( ) );
        page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_DOWNLOAND_TITLE, request.getLocale( ) ) );
        page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_DOWNLOAND_TITLE, request.getLocale( ) ) );

        return page;
    }

    /**
     * Get the XPage for searching an event
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html form
     */
    private XPage getSearchPage( HttpServletRequest request, Plugin plugin )
    {
        XPage page = new XPage( );
        Map<String, Object> model = new HashMap<String, Object>( );

        model.put( Constants.MARK_NB_ITEMS_PER_PAGE, Integer.toString( _nDefaultItemsPerPage ) );
        model.put( Constants.MARK_CALENDARS_LIST, getListAgenda( request, plugin ) );
        model.put( Constants.MARK_AGENDA, Constants.SPACE );
        model.put( Constants.MARK_LOCALE, request.getLocale( ) );

        Collection<Category> categoryList = _categoryService.getCategories( plugin );
        model.put( Constants.MARK_CATEGORY_LIST, getReferenceListCategory( categoryList ) );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_SEARCH_EVENTS, request.getLocale( ), model );

        page.setContent( template.getHtml( ) );
        page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_SEARCH, request.getLocale( ) ) );
        page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_SEARCH, request.getLocale( ) ) );

        return page;
    }

    /**
     * Builds the display data of the search results of the current page. The date badge of a result shows its first date
     * from the beginning of the searched period, or from today when no period is searched
     * @param listEvents the events of the current page
     * @param dateBegin the beginning of the searched period, or null
     * @param locale the locale
     * @param plugin the plugin
     * @return the display data of the results (see EventDisplayData), with title, imageUrl and hasImage
     */
    private List<Map<String, Object>> getSearchResults( List<Event> listEvents, Date dateBegin, java.util.Locale locale, Plugin plugin )
    {
        String strReference = Utils.getDate( ( dateBegin != null ) ? dateBegin : new Date( ) );
        List<Map<String, Object>> listResults = new ArrayList<>( );

        for ( Event event : listEvents )
        {
            AgendaResource agenda = _calendarService.getAgendaResource( event.getIdCalendar( ) );

            if ( agenda != null )
            {
                Date dateShown = _eventListService.getOccurrenceEvents( event.getIdCalendar( ), event.getId( ), Constants.SORT_ASC, plugin ).stream( )
                        .map( OccurrenceEvent::getDate ).filter( date -> Utils.getDate( date ).compareTo( strReference ) >= 0 ).findFirst( )
                        .orElse( event.getDate( ) );
                Map<String, Object> result = EventDisplayData.build( event, agenda, dateShown, locale, plugin );
                boolean bHasImage = EventDisplayData.hasImage( event.getId( ), plugin );
                result.put( KEY_TITLE, StringUtils.defaultString( event.getTitle( ) ) );
                result.put( KEY_HAS_IMAGE, bHasImage );
                result.put( KEY_IMAGE_URL, bHasImage ? EventImageResourceService.getResourceImageEvent( event.getId( ) ) : StringUtils.EMPTY );
                listResults.add( result );
            }
        }

        return listResults;
    }

    /**
     * Gives the tag searched, without its leading # and its surrounding spaces
     * @param request The Http request, with the parameter tag
     * @return the tag, or an empty string when no tag is searched
     */
    private static String getTagParameter( HttpServletRequest request )
    {
        return Strings.CS.removeStart( StringUtils.trimToEmpty( request.getParameter( PARAMETER_TAG ) ), TAG_PREFIX ).trim( );
    }

    /**
     * Keeps the events that carry a tag, ignoring the case
     * @param listEvents the events found by the search, may be null
     * @param strTag the tag, an empty string to keep every event
     * @return the events that carry the tag, or the events given when no tag is searched
     */
    private static List<Event> filterByTag( List<Event> listEvents, String strTag )
    {
        if ( listEvents == null || StringUtils.isEmpty( strTag ) )
        {
            return listEvents;
        }

        return listEvents.stream( ).filter( event -> hasTag( event, strTag ) ).collect( Collectors.toCollection( ArrayList::new ) );
    }

    /**
     * Tells whether an event carries a tag, ignoring the case
     * @param event the event
     * @param strTag the tag
     * @return true if one of the tags of the event is the tag
     */
    private static boolean hasTag( Event event, String strTag )
    {
        String [ ] arrayTags = ( event instanceof SimpleEvent ) ? ( (SimpleEvent) event ).getTags( ) : null;

        return arrayTags != null && Arrays.stream( arrayTags ).anyMatch( tag -> strTag.equalsIgnoreCase( Strings.CS.removeStart( StringUtils.trimToEmpty( tag ), TAG_PREFIX ) ) );
    }

    /**
     * Get the XPage for getting the search result
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     * @throws SiteMessageException message if error
     */
    private XPage getSearchResultPage( HttpServletRequest request, Plugin plugin ) throws SiteMessageException
    {
        String strQuery = request.getParameter( Constants.PARAMETER_QUERY );

        String[] arrayCategory = request.getParameterValues( Constants.PARAMETER_CATEGORY );
        String[] arrayCalendar = request.getParameterValues( Constants.PARAMETER_CALENDAR_ID );

        String strDateBegin = request.getParameter( Constants.PARAMETER_DATE_START );
        String strDateEnd = request.getParameter( Constants.PARAMETER_DATE_END );
        String strPeriod = request.getParameter( Constants.PARAMETER_PERIOD );

        if ( StringUtils.isBlank( strPeriod ) || !StringUtils.isNumeric( strPeriod ) )
        {
            strPeriod = Integer.toString( Constants.PROPERTY_PERIOD_NONE );
        }

        String strAgenda = null;

        String strBaseUrl = AppPathService.getBaseUrl( request );
        UrlItem url = new UrlItem( JSP_PAGE_PORTAL );
        url.addParameter( Constants.PARAMETER_QUERY, strQuery == null ? Constants.EMPTY_STRING : strQuery );
        url.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        url.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_DO_SEARCH );

        if ( arrayCalendar != null )
        {
            for ( String strAgendaId : arrayCalendar )
            {
                url.addParameter( Constants.PARAMETER_CALENDAR_ID, strAgendaId );
            }
        }
        else
        {
            arrayCalendar = Utils.getCalendarIds( request );
        }

        url.addParameter( Constants.PARAMETER_DATE_START, strDateBegin );
        url.addParameter( Constants.PARAMETER_DATE_END, strDateEnd );
        url.addParameter( Constants.PARAMETER_PERIOD, strPeriod );

        String strTag = getTagParameter( request );
        String strUrlWithoutTag = url.getUrl( );

        if ( StringUtils.isNotEmpty( strTag ) )
        {
            url.addParameter( PARAMETER_TAG, URLEncoder.encode( strTag, StandardCharsets.UTF_8 ) );
        }

        List<Event> listEvent = null;

        Date dateBegin = null;
        Date dateEnd = null;

        switch ( Integer.parseInt( strPeriod ) )
        {
        case Constants.PROPERTY_PERIOD_NONE:
            break;

        case Constants.PROPERTY_PERIOD_TODAY:
            dateBegin = new Date( );
            dateEnd = new Date( );
            strDateBegin = DateUtil.getDateString( new Date( ), request.getLocale( ) );
            strDateEnd = DateUtil.getDateString( new Date( ), request.getLocale( ) );
            break;

        case Constants.PROPERTY_PERIOD_WEEK:

            Calendar calendar = new GregorianCalendar( );
            Calendar calendarFirstDay = new GregorianCalendar( );
            Calendar calendarLastDay = new GregorianCalendar( );

            int nDayOfWeek = calendar.get( Calendar.DAY_OF_WEEK );

            if ( nDayOfWeek == 1 )
            {
                nDayOfWeek = 8;
            }

            calendarFirstDay = calendar;
            calendarFirstDay.add( Calendar.DATE, Calendar.MONDAY - nDayOfWeek );
            calendarLastDay = (GregorianCalendar) calendarFirstDay.clone( );
            calendarLastDay.add( Calendar.DATE, 6 );
            dateBegin = calendarFirstDay.getTime( );
            dateEnd = calendarLastDay.getTime( );
            strDateBegin = DateUtil.getDateString( dateBegin, request.getLocale( ) );
            strDateEnd = DateUtil.getDateString( dateEnd, request.getLocale( ) );

            break;

        case Constants.PROPERTY_PERIOD_RANGE:
            if ( StringUtils.isNotBlank( strDateBegin ) && StringUtils.isNotBlank( strDateEnd ) )
            {
                dateBegin = Utils.parseFormDate( strDateBegin, request.getLocale( ) );
                dateEnd = Utils.parseFormDate( strDateEnd, request.getLocale( ) );

                if ( dateBegin == null || !Utils.isValidDate( dateBegin ) || dateEnd == null
                        || !Utils.isValidDate( dateEnd ) )
                {
                    errorDateFormat( request );
                }
            }
            else
            {
                errorDateFormat( request );
            }

            break;
        default:
            break;
        }

        listEvent = filterByTag( _calendarSearchService.getSearchResults( arrayCalendar, arrayCategory, strQuery,
                dateBegin, dateEnd, plugin ), strTag );

        _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );
        _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage,
                _nDefaultItemsPerPage );

        if ( listEvent == null )
        {
            listEvent = new ArrayList<Event>( );
        }

        Map<String, Object> model = new HashMap<String, Object>( );

        Paginator<Event> paginator = new Paginator<Event>( listEvent, _nItemsPerPage, url.getUrl( ),
                Constants.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );

        //if one calendar is selected            
        if ( ( arrayCalendar != null ) && ( arrayCalendar.length == 1 ) )
        {
            strAgenda = arrayCalendar[0];
        }

        UrlItem urlSubscription = new UrlItem( strBaseUrl + JSP_PAGE_PORTAL );
        urlSubscription.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        urlSubscription.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_GET_SUBSCRIPTION_PAGE );
        urlSubscription.addParameter( Constants.PARAM_AGENDA, strAgenda );

        ReferenceList listAgendas = getListAgenda( request, plugin );

        if ( arrayCalendar != null )
        {
            listAgendas.checkItems( arrayCalendar );
        }

        Collection<Category> categoryList = _categoryService.getCategories( plugin );
        ReferenceList listCategorys = getReferenceListCategory( categoryList );

        if ( arrayCategory != null )
        {
            listCategorys.checkItems( arrayCategory );
        }

        // Evol List occurrences
        List<List<OccurrenceEvent>> listOccurrences = new ArrayList<List<OccurrenceEvent>>( );

        for ( Event event : listEvent )
        {
            List<OccurrenceEvent> listOccurrence = _eventListService.getOccurrenceEvents( event.getIdCalendar( ),
                    event.getId( ), Constants.SORT_ASC, plugin );
            listOccurrences.add( listOccurrence );
        }

        CalendarUserOptions options = getUserOptions( request );
        options.setShowSearchEngine( Boolean.TRUE );

        boolean bIsSelectedDay = false;
        String strDate;
        if ( StringUtils.isNotBlank( strDateBegin ) && !Constants.NULL.equals( strDateBegin ) )
        {
            strDate = Utils.getDate( Utils.parseFormDate( strDateBegin, request.getLocale( ) ) );
            if ( strDateBegin.equals( strDateEnd ) )
            {
                bIsSelectedDay = true;
            }
        }
        else
        {
            strDate = Utils.getDate( new Date( ) );
        }

        MultiAgenda agendaWithOccurences = _calendarService.getMultiAgenda( request );

        model.put( Constants.MARK_QUERY, ( StringUtils.isNotBlank( strQuery ) ) ? strQuery : StringUtils.EMPTY );
        model.put( Constants.MARK_SUBSCRIPTION_PAGE, urlSubscription.getUrl( ) );
        model.put( Constants.MARK_DATE_START, Utils.getIsoFormDate( strDateBegin, request.getLocale( ) ) );
        model.put( Constants.MARK_DATE_END, Utils.getIsoFormDate( strDateEnd, request.getLocale( ) ) );
        model.put( Constants.MARK_PERIOD, ( StringUtils.isNotBlank( strPeriod ) ) ? strPeriod : StringUtils.EMPTY );
        model.put( Constants.MARK_EVENTS_LIST, paginator.getPageItems( ) );
        model.put( MARK_SEARCH_RESULTS, getSearchResults( paginator.getPageItems( ), dateBegin, request.getLocale( ), plugin ) );
        model.put( MARK_TAG, strTag );
        model.put( MARK_URL_WITHOUT_TAG, strUrlWithoutTag );
        model.put( MARK_SEARCH_FILTERED, ( arrayCategory != null ) || ( request.getParameterValues( Constants.PARAMETER_CALENDAR_ID ) != null )
                || ( Integer.parseInt( strPeriod ) != Constants.PROPERTY_PERIOD_NONE ) );
        model.put( Constants.MARK_PAGINATOR, paginator );
        model.put( Constants.MARK_NB_ITEMS_PER_PAGE, Integer.toString( _nItemsPerPage ) );
        model.put( Constants.MARK_CALENDARS_LIST, listAgendas );
        model.put( Constants.MARK_AGENDA, ( StringUtils.isNotBlank( strAgenda ) ) ? strAgenda : StringUtils.EMPTY );
        model.put( Constants.MARK_LOCALE, request.getLocale( ) );
        model.put( Constants.MARK_CATEGORY_LIST, listCategorys );
        model.put( Constants.MARK_OCCURRENCES_LIST, listOccurrences );
        model.put( Constants.MARK_SMALL_MONTH_CALENDAR,
                HtmlMarkup.of( SmallMonthCalendar.getSmallMonthCalendar( strDate, agendaWithOccurences, options, bIsSelectedDay ) ) );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_DO_SEARCH_EVENTS, request.getLocale( ), model );

        XPage page = new XPage( );
        page.setContent( template.getHtml( ) );
        page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_SEARCH_RESULT, request.getLocale( ) ) );
        page.setPathLabel( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_SEARCH, request.getLocale( ) ) );

        return page;
    }

    /**
     * Get the XPage for getting the recording of an event
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     */
    private XPage getShowResultPage( HttpServletRequest request, Plugin plugin )
    {
        XPage page = null;
        String strDocumentId = request.getParameter( Constants.PARAMETER_DOCUMENT_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        String strPathResult = StringUtils.EMPTY;

        if ( StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            int nEventId = Integer.parseInt( strEventId );

            SimpleEvent event = _eventListService.getEvent( nEventId, plugin );

            HtmlTemplate template = new HtmlTemplate( );

            if ( event != null )
            {
                int nDocumentId = -1;

                if ( StringUtils.isNotBlank( strDocumentId ) && StringUtils.isNumeric( strDocumentId ) )
                {
                    nDocumentId = Integer.parseInt( strDocumentId );
                }
                else if ( event.getDocumentId( ) > -1 )
                {
                    nDocumentId = event.getDocumentId( );
                }

                String strTemplateDocument = StringUtils.EMPTY;
                Plugin pluginModule = PluginService.getPlugin( Constants.PROPERTY_MODULE_CALENDAR );

                if ( ( pluginModule != null ) && pluginModule.isInstalled( ) && ( nDocumentId > 0 ) )
                {
                    try
                    {
                        IAppUtils documentUtil = (IAppUtils) Class.forName( PROPERTY_UTIL_DOCUMENT_CLASS )
                                .getDeclaredConstructor( ).newInstance( );
                        strTemplateDocument = documentUtil.getTemplateDocument( nDocumentId, request );
                    }
                    catch ( Exception e )
                    {
                        AppLogService.error( e );
                    }
                }

                CalendarUserOptions options = getUserOptions( request );
                options.setShowSearchEngine( Boolean.TRUE );

                AgendaResource agenda = _calendarService.getAgendaResource( event.getIdCalendar( ) );

                MultiAgenda agendaWithOccurences = _calendarService.getMultiAgenda( request );

                String strRole = agenda.getRole( );

                boolean bIsUserAuthorized = false;

                if ( hasManagerRole( agenda, request ) || hasRole( strRole, request )
                        || Constants.PROPERTY_ROLE_NONE.equals( strRole ) )
                {
                    bIsUserAuthorized = true;
                }

                if ( bIsUserAuthorized && !hasManagerRole( agenda, request )
                        && EventProposals.getProposedEventIds( event.getIdCalendar( ), plugin ).contains( event.getId( ) ) )
                {
                    bIsUserAuthorized = false;
                }

                Map<String, Object> model = new HashMap<String, Object>( );

                if ( bIsUserAuthorized )
                {
                    String strBaseUrl = AppPathService.getBaseUrl( request );
                    UrlItem urlEmailFriend = new UrlItem( strBaseUrl + JSP_PAGE_PORTAL );
                    urlEmailFriend.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
                    urlEmailFriend.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_GET_FRIEND_EMAIL_PAGE );

                    UrlItem urlSubscription = new UrlItem( strBaseUrl + JSP_PAGE_PORTAL );
                    urlSubscription.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
                    urlSubscription.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_GET_SUBSCRIPTION_PAGE );
                    urlSubscription.addParameter( Constants.PARAM_AGENDA, agenda.getId( ) );

                    event.setImageUrl( EventImageResourceService.getResourceImageEvent( event.getId( ) ) );
                    model.put( Constants.MARK_EVENT, event );
                    model.put( Constants.MARK_DOCUMENT, HtmlMarkup.of( strTemplateDocument ) );
                    model.put( Constants.MARK_AGENDA, agenda );
                    model.put( Constants.MARK_EMAIL_FRIEND_PAGE, urlEmailFriend.getUrl( ) );
                    model.put( Constants.MARK_SUBSCRIPTION_PAGE, urlSubscription.getUrl( ) );
                    putCalendarButton( model, event, agenda, request, plugin );
                    putEventDetails( model, event, request, plugin );
                }

                model.put( Constants.MARK_IS_AUTHORIZED, bIsUserAuthorized );
                model.put( Constants.MARK_SMALL_MONTH_CALENDAR, HtmlMarkup.of( SmallMonthCalendar.getSmallMonthCalendar(
                        Utils.getDate( event.getDate( ) ), agendaWithOccurences, options, true ) ) );
                template = AppTemplateService.getTemplate( TEMPLATE_SHOW_RESULT, request.getLocale( ), model );
                strPathResult = event.getTitle( );
            }

            page = new XPage( );
            page.setContent( template.getHtml( ) );
            page.setTitle( I18nService.getLocalizedString( Constants.PROPERTY_PAGE_TITLE_SEARCH_RESULT,
                    request.getLocale( ) ) );
            page.setPathLabel( strPathResult );
        }
        return page;
    }

    /**
     * Get the XPage for getting the rss
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @return the html
     */
    private XPage getRssPage( HttpServletRequest request, Plugin plugin )
    {
        return getDownloadPage( request, plugin );
    }

    /**
     * Performs confirm unsubscription process
     * @param request The http request
     * @throws SiteMessageException The error message handled by the front
     *             office
     */
    public void getConfirmUnSubscribe( HttpServletRequest request ) throws SiteMessageException
    {
        UrlItem urlItem = new UrlItem( request.getRequestURI( ) );
        urlItem.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_UNSUBSCRIBE );
        urlItem.addParameter( Constants.PARAMETER_EMAIL, request.getParameter( Constants.PARAMETER_EMAIL ) );
        urlItem.addParameter( Constants.PARAM_AGENDA, request.getParameter( Constants.PARAM_AGENDA ) );
        SiteMessageService.setMessage( request, Constants.PROPERTY_CONFIRM_UNSUBSCRIPTION_ALERT_MESSAGE, null,
                Constants.PROPERTY_CONFIRM_UNSUBSCRIPTION_TITLE_MESSAGE, urlItem.getUrl( ), null,
                SiteMessage.TYPE_CONFIRMATION );
    }

    /**
     * Get the date from the request parameter
     * @param request The HTTP request
     * @return The string date code
     */
    private String getDate( HttpServletRequest request )
    {
        String strDate = request.getParameter( Constants.PARAM_DATE );

        if ( !Utils.isValid( strDate ) )
        {
            strDate = Utils.getDateToday( );
        }

        return strDate;
    }

    /**
     * Get the calendar view from the request parameter or stored in the session
     * @param request The HTTP request
     * @return A Calendar View object
     */
    private CalendarView getView( HttpServletRequest request )
    {
        CalendarView view = null;
        String strView = request.getParameter( Constants.PARAM_VIEW );

        if ( strView != null )
        {
            if ( strView.equals( Constants.VIEW_DAY ) )
            {
                view = new DayCalendarView( );
            }
            else if ( strView.equals( Constants.VIEW_WEEK ) )
            {
                view = new WeekCalendarView( );
            }
            else if ( strView.equals( Constants.VIEW_MONTH ) )
            {
                view = new MonthCalendarView( );
            }
            else
            {
                // Default view
                view = new MonthCalendarView( );
            }

            HttpSession session = request.getSession( true );
            session.setAttribute( Constants.ATTRIBUTE_CALENDAR_VIEW, view );
        }
        else
        {
            HttpSession session = request.getSession( );
            CalendarView viewCurrentSession = (CalendarView) session.getAttribute( Constants.ATTRIBUTE_CALENDAR_VIEW );

            if ( viewCurrentSession != null )
            {
                view = viewCurrentSession;
            }
            else
            {
                // Default view
                view = new MonthCalendarView( );
            }
        }

        return view;
    }

    /**
     * Get user options from the request parameter or stored in the session or
     * in cookies
     * @param request The HTTP request
     * @return A CalendarUserOptions
     */
    private CalendarUserOptions getUserOptions( HttpServletRequest request )
    {
        CalendarUserOptions options = new CalendarUserOptions( );
        options.setLocale( request.getLocale( ) );
        options.setDayOffDisplayed( true );

        return options;
    }

    /**
     * Build the legend of all agenda selected
     * @param multiAgenda A multi agenda
     * @param options Options storing display settings
     * @param request The http request
     * @return The HTML code of the Legend
     */
    private String getLegend( HttpServletRequest request, MultiAgenda multiAgenda, CalendarUserOptions options )
    {
        String strLegend = StringUtils.EMPTY;

        if ( multiAgenda != null && !multiAgenda.getAgendas( ).isEmpty( ) )
        {
            Map<String, Object> model = new HashMap<String, Object>( );

            List<Agenda> listAgendas = multiAgenda.getAgendas( );
            List<AgendaResource> listAgendaResource = new ArrayList<AgendaResource>( );

            for ( Agenda agenda : listAgendas )
            {
                if ( agenda != null )
                {
                    AgendaResource agendaResource = _calendarService.getAgendaResource( agenda.getKeyName( ) );
                    if ( agendaResource != null )
                    {
                        listAgendaResource.add( agendaResource );
                    }
                }
            }

            model.put( Constants.MARK_AGENDA_RESOURCE_LIST, listAgendaResource );
            model.put( MARK_CATEGORY_LIST, CategoryHome.findAll( PluginService.getPlugin( Constants.PLUGIN_NAME ) ) );
            model.put( MARK_ICAL_LIST, getIcalLinks( request, listAgendaResource ) );
            model.put( MARK_MANAGED_CALENDAR_IDS, listAgendaResource.stream( ).filter( agenda -> hasManagerRole( agenda, request ) ).map( AgendaResource::getId ).toList( ) );
            model.put( MARK_PROPOSAL_CALENDAR_IDS, listAgendaResource.stream( ).filter( agenda -> agenda.isFrontProposals( ) && SecurityService.isAuthenticationEnable( ) )
                    .map( AgendaResource::getId ).toList( ) );

            HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_CALENDAR_LEGEND, options.getLocale( ),
                    model );
            strLegend = template.getHtml( );
        }

        return strLegend;
    }

    /**
     * The Creation action of an event
     * @param request The HttpRequest
     * @param plugin The Plugin
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException
     *             Exception used by the front Message mechanism
     * @throws UserNotSignedException
     */
    private void doCreateEvent( HttpServletRequest request, Plugin plugin ) throws SiteMessageException, UserNotSignedException
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId ) )
        {
            boolean bPublishedDirectly = checkCanAddEvent( _calendarService.getAgendaResource( Integer.parseInt( strCalendarId ) ), request );
            //Creation of the event
            String strDate = request.getParameter( Constants.PARAMETER_EVENT_DATE );
            String strEventTitle = request.getParameter( Constants.PARAMETER_EVENT_TITLE );

            int nCalendarId = Integer.parseInt( strCalendarId );
            //Mandatory fields
            verifyFieldFilled( request, strDate );
            verifyFieldFilled( request, strEventTitle );

            //Convert the date in form to a java.util.Date object
            Date dateEvent = Utils.parseFormDate( strDate, request.getLocale( ) );

            if ( dateEvent == null )
            {
                errorDateFormat( request );
            }

            SimpleEvent event = new SimpleEvent( );
            event.setIdCalendar( nCalendarId );
            event.setDate( dateEvent );
            event.setDateEnd( dateEvent );

            String strTimeStart = request.getParameter( Constants.PARAMETER_EVENT_TIME_START );
            String strTimeEnd = request.getParameter( Constants.PARAMETER_EVENT_TIME_END );

            if ( !Utils.checkTime( strTimeStart ) || !Utils.checkTime( strTimeEnd ) )
            {
                errorTimeFormat( request );
            }

            event.setDateTimeStart( strTimeStart );
            event.setDateTimeEnd( strTimeEnd );
            event.setTitle( request.getParameter( Constants.PARAMETER_EVENT_TITLE ) );
            event.setDescription( Constants.EMPTY_STRING );
            event.setOccurrence( 1 );
            event.setLinkUrl( readEventLink( request ) );
            event.setListCategories( readEventCategory( request, plugin ) );

            event.setStatus( bPublishedDirectly ? AppPropertiesService.getProperty( Constants.PROPERTY_EVENT_STATUS_DEFAULT )
                    : EventProposals.getProposedStatus( ) );

            LuteceUser user;
            try
            {
                user = getUser( request );
                _eventListService.doAddEvent( event, user, plugin );
            }
            catch ( PageNotFoundException e )
            {
                _eventListService.doAddEvent( event, null, plugin );
            }
            catch ( UserNotSignedException e )
            {
                _eventListService.doAddEvent( event, null, plugin );
            }

            // Send email to notify an event has been created if the event has the status 'confirmed'
            int nSubscriber = _agendaSubscriberService.getSubscriberNumber( nCalendarId, plugin );
            Collection<CalendarSubscriber> listSubscribers = new ArrayList<CalendarSubscriber>( );
            if ( nSubscriber > 0
                    && AppPropertiesService.getProperty( Constants.PROPERTY_EVENT_STATUS_CONFIRMED ).equals(
                            event.getStatus( ) ) )
            {
                listSubscribers = _agendaSubscriberService.getSubscribers( nCalendarId, plugin );
            }
            // Notify the webmaster an event has been created
            String strWebmasterEmail = DatastoreService.getDataValue( Constants.DATASTORE_KEY_WEBMASTER_EMAIL,
                    AppPropertiesService.getProperty( Constants.PROPERTY_WEBMASTER_EMAIL ) );
            if ( StringUtils.isNotBlank( strWebmasterEmail ) && StringUtil.checkEmail( strWebmasterEmail ) )
            {
                CalendarSubscriber webmaster = new CalendarSubscriber( );
                webmaster.setEmail( strWebmasterEmail );
                listSubscribers.add( webmaster );
            }
            _agendaSubscriberService.sendSubscriberMail( request, listSubscribers, event, nCalendarId );

            if ( !bPublishedDirectly )
            {
                SiteMessageService.setMessage( request, MESSAGE_PROPOSAL_SENT, SiteMessage.TYPE_INFO, String.format( URL_CALENDAR_OF, nCalendarId ) );
            }
        }
    }

    /**
     * Method modifying the event
     * 
     * @param request The request
     * @param plugin The plugin
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException
     *             Exception used by the front Message mechanism
     * @throws UserNotSignedException user not signed
     */
    private void doModifyEvent( HttpServletRequest request, Plugin plugin ) throws SiteMessageException,
            UserNotSignedException
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            int nEventId = Integer.parseInt( strEventId );
            if ( verifiyUserAccess( request, nCalendarId, nEventId, plugin ) )
            {
                String strEventDate = request.getParameter( Constants.PARAMETER_EVENT_DATE );
                verifyFieldFilled( request, strEventDate );

                //Convert the date in form to a java.util.Date object
                Date dateEvent = Utils.parseFormDate( strEventDate, request.getLocale( ) );

                if ( dateEvent == null )
                {
                    errorDateFormat( request );
                }

                SimpleEvent event = _eventListService.getEvent( nEventId, plugin );
                AgendaResource agenda = _calendarService.getAgendaResource( nCalendarId );

                List<OccurrenceEvent> listOccurrenceEvents = _eventListService.getOccurrenceEvents( nCalendarId,
                        nEventId, Constants.SORT_ASC, plugin );

                // Check security access
                if ( hasManagerRole( agenda, request ) )
                {
                    for ( OccurrenceEvent occurrenceEvent : listOccurrenceEvents )
                    {
                        if ( event.getDate( ).equals( occurrenceEvent.getDate( ) ) )
                        {
                            event.setStatus( occurrenceEvent.getStatus( ) );
                            break;
                        }
                    }
                }
                else
                {
                    if ( ( agenda == null ) || !agenda.isFrontProposals( ) )
                    {
                        SiteMessageService.setMessage( request, MESSAGE_PROPOSAL_NOT_ALLOWED, SiteMessage.TYPE_STOP );
                    }

                    event.setStatus( EventProposals.getProposedStatus( ) );
                    for ( OccurrenceEvent occurrenceEvent : listOccurrenceEvents )
                    {
                        occurrenceEvent.setStatus( event.getStatus( ) );
                        _eventListService.doModifyOccurrenceEvent( occurrenceEvent, plugin );
                    }
                }

                event.setDate( dateEvent );

                String strTimeStart = request.getParameter( Constants.PARAMETER_EVENT_TIME_START );
                String strTimeEnd = request.getParameter( Constants.PARAMETER_EVENT_TIME_END );

                if ( !Utils.checkTime( strTimeStart ) || !Utils.checkTime( strTimeEnd ) )
                {
                    errorTimeFormat( request );
                }

                event.setDateTimeStart( strTimeStart );
                event.setDateTimeEnd( strTimeEnd );
                event.setTitle( request.getParameter( Constants.PARAMETER_EVENT_TITLE ) );

                _eventListService.doModifySimpleEvent( event, true, getUser( request ), plugin );
            }
            else
            {
                SiteMessageService.setMessage( request, Messages.USER_ACCESS_DENIED, SiteMessage.TYPE_STOP );
            }
        }
    }

    /**
     * Remove an event
     * @param request {@link HttpServletRequest}
     * @param plugin {@link Plugin}
     * @throws UserNotSignedException exception if user is not connected
     * @throws SiteMessageException message if error
     */
    private void doRemoveEvent( HttpServletRequest request, Plugin plugin ) throws UserNotSignedException,
            SiteMessageException
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        if ( StringUtils.isNotBlank( strCalendarId ) && StringUtils.isNumeric( strCalendarId )
                && StringUtils.isNotBlank( strEventId ) && StringUtils.isNumeric( strEventId ) )
        {
            int nCalendarId = Integer.parseInt( strCalendarId );
            int nEventId = Integer.parseInt( strEventId );
            if ( verifiyUserAccess( request, nCalendarId, nEventId, plugin ) )
            {
                _eventListService.doRemoveEvent( nCalendarId, nEventId, getUser( request ), plugin );
            }
            else
            {
                SiteMessageService.setMessage( request, Messages.USER_ACCESS_DENIED, SiteMessage.TYPE_STOP );
            }
        }
    }

    /**
     * Verify subscription
     * @param request the request
     * @param plugin the plugin
     * @throws SiteMessageException site exception
     */
    private void doVerifySubscription( HttpServletRequest request, Plugin plugin ) throws SiteMessageException
    {
        String strEmail = request.getParameter( Constants.PARAMETER_EMAIL );
        if ( StringUtils.isNotBlank( strEmail ) && StringUtil.checkEmail( strEmail ) )
        {
            if ( _captchaService.isResolvable( ) && !_captchaService.get( ).validate( request ) )
            {
                //invalid captcha
                SiteMessageService.setMessage( request, PROPERTY_CAPTCHA_INVALID_MESSAGE,
                        PROPERTY_CAPTCHA_INVALID_TITLE_MESSAGE, SiteMessage.TYPE_INFO );
            }
            else
            {
                String strAgenda = request.getParameter( Constants.PARAM_AGENDA );
                if ( StringUtils.isNotBlank( strAgenda ) && StringUtils.isNumeric( strAgenda ) )
                {
                    int nAgendaId = Integer.parseInt( strAgenda );
                    AgendaResource agenda = _calendarService.getAgendaResource( nAgendaId );
                    _agendaSubscriberService.doNotificationSubscription( agenda, request, plugin );
                }
            }
        }
        else
        {
            SiteMessageService.setMessage( request, PROPERTY_INVALID_MAIL_ERROR_MESSAGE, new String[] { strEmail },
                    PROPERTY_INVALID_MAIL_TITLE_MESSAGE, SiteMessage.TYPE_STOP );
        }
    }

    /**
     * Method verifies whether a mandatory field is filled
     * @param request The HttpRequest
     * @param strField The field to be checked
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException
     *             Exception used by the front Message mechanism
     */
    private void verifyFieldFilled( HttpServletRequest request, String strField ) throws SiteMessageException
    {
        if ( StringUtils.isBlank( strField ) )
        {
            SiteMessageService.setMessage( request, Messages.MANDATORY_FIELDS, SiteMessage.TYPE_STOP );
        }
    }

    /**
     * Verifiy if the current user has the right to manage the event
     * @param request HttpServletRequest
     * @param nCalendarId The id of the calendar
     * @param nEventId The id of the event
     * @param plugin Plugin
     * @return True if the user has the right, false otherwise
     * @throws UserNotSignedException user not signed
     */
    private boolean verifiyUserAccess( HttpServletRequest request, int nCalendarId, int nEventId, Plugin plugin )
            throws UserNotSignedException
    {
        boolean bIsVerified = false;
        AgendaResource agenda = _calendarService.getAgendaResource( nCalendarId );
        SimpleEvent eventChecked = _eventListService.getEvent( nEventId, plugin );

        if ( ( eventChecked == null ) || ( eventChecked.getIdCalendar( ) != nCalendarId ) )
        {
            return false;
        }

        if ( hasManagerRole( agenda, request ) )
        {
            bIsVerified = true;
        }
        else
        {
            LuteceUser user = getUser( request );
            if ( user != null )
            {
                List<SimpleEvent> listEvents = _eventListService.getSimpleEventsByUserLogin( nCalendarId, user );
                for ( SimpleEvent event : listEvents )
                {
                    if ( event.getId( ) == nEventId )
                    {
                        bIsVerified = true;
                        break;
                    }
                }
            }
        }
        return bIsVerified;
    }

    /**
     * Refuses a change of the events whose form token is missing or spent
     * @param request the request
     * @param strAction the action the token was given for
     * @throws SiteMessageException the message refusing the change
     */
    private void checkSecurityToken( HttpServletRequest request, String strAction ) throws SiteMessageException
    {
        if ( !_securityTokenService.validate( request, strAction ) )
        {
            SiteMessageService.setMessage( request, MESSAGE_INVALID_TOKEN, SiteMessage.TYPE_STOP );
        }
    }

    /**
     * Verifies the date format
     * @param request The HttpRequest
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException
     *             Exception used by the front Message mechanism
     */
    private void errorDateFormat( HttpServletRequest request ) throws SiteMessageException
    {
        SiteMessageService.setMessage( request, PROPERTY_INVALID_DATE_MESSAGE, null,
                PROPERTY_INVALID_DATE_TITLE_MESSAGE, null, null, SiteMessage.TYPE_STOP );
    }

    /**
     * Verifies the time format
     * @param request The HttpRequest
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException
     *             Exception used by the front Message mechanism
     */
    private void errorTimeFormat( HttpServletRequest request ) throws SiteMessageException
    {
        SiteMessageService.setMessage( request, PROPERTY_INVALID_TIME_MESSAGE, null,
                PROPERTY_INVALID_TIME_TITLE_MESSAGE, null, null, SiteMessage.TYPE_STOP );
    }

    /**
     * Performs the subscription process
     * @param request The Http request
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException The
     *             error message thrown to the user
     */
    public void doSubscription( HttpServletRequest request ) throws SiteMessageException
    {
        _agendaSubscriberService.doValidationSubscription( request );
    }

    /**
     * Send a mail to a friend
     * @param request The request
     * @return The next URL to redirect to
     * @throws fr.paris.lutece.portal.service.message.SiteMessageException The
     *             error message thrown to the user
     */
    public String doSendToFriend( HttpServletRequest request ) throws SiteMessageException
    {
        //Form parameters
        String strIdEvent = request.getParameter( Constants.PARAMETER_EVENT_ID );
        String strIdCalendar = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strFriendEmail = request.getParameter( Constants.PARAMETER_SENDER_FRIEND_EMAIL );
        String strSenderFirstName = request.getParameter( Constants.PARAMETER_SENDER_FIRST_NAME );
        String strSenderLastName = request.getParameter( Constants.PARAMETER_SENDER_LAST_NAME );
        String strSenderEmail = request.getParameter( Constants.PARAMETER_SENDER_EMAIL );
        String strSenderMessage = request.getParameter( Constants.PARAMETER_SENDER_MESSAGE );

        if ( !StringUtils.isNumeric( strIdCalendar ) || ( strIdCalendar.length( ) > 9 ) || !StringUtils.isNumeric( strIdEvent )
                || ( strIdEvent.length( ) > 9 ) )
        {
            SiteMessageService.setMessage( request, Messages.USER_ACCESS_DENIED, SiteMessage.TYPE_STOP );
        }

        int nIdCalendar = Integer.parseInt( strIdCalendar );
        int nIdEvent = Integer.parseInt( strIdEvent );
        SimpleEvent eventSent = _eventListService.getEvent( nIdEvent, PluginService.getPlugin( Constants.PLUGIN_NAME ) );
        AgendaResource agendaSent = _calendarService.getAgendaResource( nIdCalendar );

        if ( ( eventSent == null ) || ( agendaSent == null ) || ( eventSent.getIdCalendar( ) != nIdCalendar ) || !isVisibleTo( agendaSent, request )
                || EventProposals.getProposedEventIds( nIdCalendar, PluginService.getPlugin( Constants.PLUGIN_NAME ) ).contains( nIdEvent ) )
        {
            SiteMessageService.setMessage( request, Messages.USER_ACCESS_DENIED, SiteMessage.TYPE_STOP );
        }

        // Mandatory field
        if ( StringUtils.isEmpty( strFriendEmail ) || StringUtils.isEmpty( strSenderFirstName )
                || StringUtils.isEmpty( strSenderLastName ) || StringUtils.isEmpty( strSenderEmail )
                || StringUtils.isEmpty( strSenderMessage ) )
        {
            SiteMessageService.setMessage( request, Messages.MANDATORY_FIELDS, Messages.MANDATORY_FIELDS,
                    SiteMessage.TYPE_STOP );
        }

        if ( ( strFriendEmail == null ) || !StringUtil.checkEmail( strFriendEmail ) )
        {
            Object[] args = { strFriendEmail };
            SiteMessageService.setMessage( request, PROPERTY_INVALID_MAIL_ERROR_MESSAGE, args,
                    PROPERTY_INVALID_MAIL_TITLE_MESSAGE, SiteMessage.TYPE_STOP );
        }

        if ( ( strSenderEmail == null ) || !StringUtil.checkEmail( strSenderEmail ) )
        {
            Object[] args = { strSenderEmail };
            SiteMessageService.setMessage( request, PROPERTY_INVALID_MAIL_ERROR_MESSAGE, args,
                    PROPERTY_INVALID_MAIL_TITLE_MESSAGE, SiteMessage.TYPE_STOP );
        }

        String strSenderName = strSenderFirstName + Constants.SPACE + strSenderLastName;

        //Properties
        String strObject = I18nService.getLocalizedString( PROPERTY_EMAIL_FRIEND_OBJECT, request.getLocale( ) );
        String strBaseUrl = AppPathService.getBaseUrl( request );

        Map<String, Object> emailModel = new HashMap<String, Object>( );
        emailModel.put( MARK_SENDER_MESSAGE, strSenderMessage );
        emailModel.put( MARK_BASE_URL, strBaseUrl );
        emailModel.put( Constants.PARAMETER_EVENT_ID, nIdEvent );
        emailModel.put( Constants.PARAMETER_CALENDAR_ID, nIdCalendar );
        emailModel.put( Constants.MARK_ACTION, Constants.ACTION_SHOW_RESULT );

        HtmlTemplate templateAgenda = AppTemplateService.getTemplate( TEMPLATE_SEND_NOTIFICATION_MAIL,
                request.getLocale( ), emailModel );

        String strNewsLetterCode = templateAgenda.getHtml( );

        MailService.sendMailHtml( strFriendEmail, strSenderName, strSenderEmail, strObject, strNewsLetterCode );

        return URL_JSP_RETURN_SEND_FRIEND_MAIL + Constants.ACTION_SHOW_RESULT + "&" + Constants.PARAMETER_EVENT_ID
                + "=" + nIdEvent + "&" + Constants.PARAM_AGENDA + "=" + nIdCalendar;
    }

    /**
     * Return the list of agenda for the template html search
     * @param request The request
     * @param plugin The plugin
     * @return The reference list of agendas
     */
    public ReferenceList getListAgenda( HttpServletRequest request, Plugin plugin )
    {
        ReferenceList listAgendas = new ReferenceList( );

        for ( AgendaResource a : _calendarService.getAgendaResources( request ) )
        {
            if ( a != null )
            {
                listAgendas.addItem( a.getId( ), a.getName( ) );
            }
        }

        return listAgendas;
    }

    /**
     * Return the list
     * @param collection The collection of categories
     * @return a refenceList
     */
    private ReferenceList getReferenceListCategory( Collection<Category> collection )
    {
        ReferenceList list = new ReferenceList( );

        if ( collection != null )
        {
            Iterator<Category> i = collection.iterator( );

            while ( i.hasNext( ) )
            {
                Category category = i.next( );
                list.addItem( category.getId( ), category.getName( ) );
            }
        }

        return list;
    }

    /**
     * Check whether the current user has the manager role or not
     * @param agenda the {@link AgendaResource}
     * @param request {@link HttpServletRequest}
     * @return true if the user has the manager role, false otherwise
     */
    private boolean hasManagerRole( AgendaResource agenda, HttpServletRequest request )
    {
        boolean bHasManagerRole = false;
        if ( agenda != null )
        {
            String strManagerRole = agenda.getRoleManager( );

            bHasManagerRole = hasRole( strManagerRole, request );
        }
        return bHasManagerRole;
    }

    /**
     * Check whether the current user has the given role or not
     * @param strRole the role
     * @param request {@link HttpServletRequest}
     * @return true if the user has the role, false otherwise
     */
    private boolean hasRole( String strRole, HttpServletRequest request )
    {
        boolean bHasRole = false;

        if ( StringUtils.isNotBlank( strRole ) && !Constants.PROPERTY_ROLE_NONE.equals( strRole )
                && SecurityService.isAuthenticationEnable( )
                && SecurityService.getInstance( ).isUserInRole( request, strRole ) )
        {
            bHasRole = true;
        }

        return bHasRole;
    }

    /**
     * Gets the user from the request
     * @param request The HTTP user
     * @return The Lutece User
     * @throws UserNotSignedException exception if user not connected
     * @throws PageNotFoundException If the authentication is not enabled
     */
    private LuteceUser getUser( HttpServletRequest request ) throws UserNotSignedException, PageNotFoundException
    {
        if ( SecurityService.isAuthenticationEnable( ) )
        {
            LuteceUser user = SecurityService.getInstance( ).getRemoteUser( request );

            if ( user == null )
            {
                throw new UserNotSignedException( );
            }

            return user;
        }
        throw new PageNotFoundException( );
    }
    /**
     * Gives the view the calendar page opens on, read by the EventCalendar view: the one asked in the address of the page,
     * else the default view of the calendar when the page shows a single calendar
     * @param request The HTTP request
     * @param multiAgenda the calendars shown
     * @return day, week, month or list, or an empty string to let the view choose by the size of the screen
     */
    private String getViewKey( HttpServletRequest request, MultiAgenda multiAgenda )
    {
        String strView = request.getParameter( Constants.PARAM_VIEW );

        if ( strView != null && VIEW_KEYS.contains( strView ) )
        {
            return strView;
        }

        if ( ( multiAgenda != null ) && ( multiAgenda.getAgendas( ).size( ) == 1 ) && ( multiAgenda.getAgendas( ).get( 0 ) != null ) )
        {
            AgendaResource agenda = _calendarService.getAgendaResource( multiAgenda.getAgendas( ).get( 0 ).getKeyName( ) );
            String strDefaultView = ( agenda != null ) ? agenda.getDefaultView( ) : StringUtils.EMPTY;

            return VIEW_KEYS.contains( strDefaultView ) ? strDefaultView : StringUtils.EMPTY;
        }

        return StringUtils.EMPTY;
    }

    /**
     * Gives the subscription and export links of the calendars open to everybody: the iCalendar feed, the JSON file and
     * the RSS feed. A calendar application or a feed reader cannot log in, so the calendars restricted to a role have none
     * @param request The HTTP request
     * @param listAgendas the calendars shown
     * @return the links, each one a map with the name and the colour of the calendar and the absolute URLs url (iCalendar),
     *         json and rss
     */
    private static List<Map<String, String>> getIcalLinks( HttpServletRequest request, List<AgendaResource> listAgendas )
    {
        List<Map<String, String>> listLinks = new ArrayList<>( );
        String strBaseUrl = AppPathService.getBaseUrl( request );

        for ( AgendaResource agenda : listAgendas )
        {
            String strRole = agenda.getRole( );

            if ( agenda.getId( ) != null && agenda.getId( ).matches( "\\d{1,9}" )
                    && ( StringUtils.isBlank( strRole ) || Constants.PROPERTY_ROLE_NONE.equals( strRole ) ) )
            {
                listLinks.add( Map.of( "name", StringUtils.defaultString( agenda.getName( ) ), "color", agenda.getEventColorCode( ),
                        "url", strBaseUrl + URL_ICAL + agenda.getId( ), "json", strBaseUrl + URL_EXPORT + agenda.getId( ) + "&format=json",
                        "rss", strBaseUrl + URL_RSS + agenda.getId( ) ) );
            }
        }

        return listLinks;
    }
    /**
     * Gives the exports of the calendars the visitor may see: iCalendar, JSON and RSS
     * @param request The HTTP request
     * @return the exports, each one a map with the name of the calendar and the absolute URLs of its files
     */
    private List<Map<String, String>> getExportLinks( HttpServletRequest request )
    {
        List<Map<String, String>> listLinks = new ArrayList<>( );
        String strBaseUrl = AppPathService.getBaseUrl( request );

        for ( AgendaResource agenda : _calendarService.getAgendaResources( request ) )
        {
            if ( agenda.getId( ) != null && agenda.getId( ).matches( "\\d{1,9}" ) )
            {
                listLinks.add( Map.of( "name", StringUtils.defaultString( agenda.getName( ) ), "color", agenda.getEventColorCode( ),
                        "ical", strBaseUrl + URL_EXPORT + agenda.getId( ),
                        "json", strBaseUrl + URL_EXPORT + agenda.getId( ) + "&format=json",
                        "rss", strBaseUrl + URL_RSS + agenda.getId( ) ) );
            }
        }

        return listLinks;
    }
    /**
     * Puts in the model of the page of an event its "add to calendar" button, when the calendar shows one: the options,
     * the links to each calendar application for the next occurrence of the event (its last one when all are past) and
     * the data of the event card
     * @param model the model of the page
     * @param event the event
     * @param agenda its calendar
     * @param request The HTTP request
     * @param plugin The plugin
     */
    private void putCalendarButton( Map<String, Object> model, SimpleEvent event, AgendaResource agenda, HttpServletRequest request, Plugin plugin )
    {
        CalendarButtonConfig config = CalendarButtonHome.findByCalendar( Integer.parseInt( agenda.getId( ) ), plugin );
        List<OccurrenceEvent> listOccurrences = CalendarHome.findOccurrencesList( event.getIdCalendar( ), event.getId( ), 1, plugin );

        if ( !config.isEnabled( ) || config.getOptions( ).isEmpty( ) || listOccurrences.isEmpty( ) )
        {
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now( );
        OccurrenceEvent occurrence = listOccurrences.stream( ).filter( o -> !CalendarEventFeedService.getStart( o ).isBefore( now.toLocalDate( ).atStartOfDay( ) ) )
                .findFirst( ).orElse( listOccurrences.get( listOccurrences.size( ) - 1 ) );
        java.time.LocalDateTime start = CalendarEventFeedService.getStart( occurrence );
        java.time.LocalTime timeStart = CalendarEventFeedService.parseTime( occurrence.getDateTimeStart( ) );
        java.time.LocalTime timeEnd = CalendarEventFeedService.parseTime( occurrence.getDateTimeEnd( ) );
        java.time.LocalDateTime end;

        if ( timeStart == null )
        {
            end = start.plusDays( 1 );
        }
        else
        {
            end = ( timeEnd != null && timeEnd.isAfter( timeStart ) ) ? start.toLocalDate( ).atTime( timeEnd ) : start.plusHours( 1 );
        }

        String strEventLocation = java.util.stream.Stream
                .of( event.getLocation( ), event.getLocationAddress( ), event.getLocationZip( ), event.getLocationTown( ) )
                .filter( StringUtils::isNotBlank ).collect( java.util.stream.Collectors.joining( " " ) );
        String strLocation = CalendarButtonService.getLocation( config, strEventLocation );
        String strIcsUrl = AppPathService.getBaseUrl( request ) + URL_ICAL_OCCURRENCE + occurrence.getId( );
        Locale locale = request.getLocale( );

        Map<String, String> mapCard = new HashMap<>( );
        mapCard.put( "day", String.valueOf( start.getDayOfMonth( ) ) );
        mapCard.put( "month", start.getMonth( ).getDisplayName( java.time.format.TextStyle.SHORT, locale ) );
        mapCard.put( "title", StringUtils.defaultString( occurrence.getTitle( ) ) );
        mapCard.put( "location", strLocation );
        mapCard.put( "date", java.time.format.DateTimeFormatter.ofLocalizedDate( java.time.format.FormatStyle.FULL ).withLocale( locale ).format( start ) );
        mapCard.put( "time", ( timeStart == null ) ? StringUtils.EMPTY
                : occurrence.getDateTimeStart( ) + ( ( timeEnd != null ) ? " - " + occurrence.getDateTimeEnd( ) : StringUtils.EMPTY ) );

        model.put( MARK_CALENDAR_BUTTON, config );
        model.put( MARK_CALENDAR_LINKS, CalendarButtonService.buildLinks( config, occurrence.getTitle( ), start, end, strLocation,
                CalendarIcalFeedService.toPlainText( event.getDescription( ) ), strIcsUrl ) );
        model.put( MARK_CALENDAR_EVENT, mapCard );
    }
    /**
     * Puts in the model of the page of an event its details (price, attendance, organizer, registration, accessibility),
     * the status of its next occurrence, whether it has a picture, its categories, its next dates, its map link and its
     * schema.org description (JSON-LD)
     * @param model the model of the page
     * @param event the event
     * @param request The HTTP request
     * @param plugin The plugin
     */
    private void putEventDetails( Map<String, Object> model, SimpleEvent event, HttpServletRequest request, Plugin plugin )
    {
        EventDetails details = EventDetailsHome.findByEvent( event.getId( ), plugin );
        List<OccurrenceEvent> listOccurrences = CalendarHome.findOccurrencesList( event.getIdCalendar( ), event.getId( ), 1, plugin );
        java.time.LocalDateTime today = java.time.LocalDate.now( ).atStartOfDay( );
        String strStatus = listOccurrences.stream( ).filter( o -> !CalendarEventFeedService.getStart( o ).isBefore( today ) ).findFirst( )
                .or( ( ) -> listOccurrences.isEmpty( ) ? java.util.Optional.empty( ) : java.util.Optional.of( listOccurrences.get( listOccurrences.size( ) - 1 ) ) )
                .map( OccurrenceEvent::getStatus ).map( String::trim ).orElse( StringUtils.EMPTY );
        String strBaseUrl = AppPathService.getBaseUrl( request );
        String strImageUrl = StringUtils.isNotBlank( event.getImageUrl( ) ) ? strBaseUrl + event.getImageUrl( ) : null;

        List<OccurrenceEvent> listNext = listOccurrences.stream( ).filter( o -> !CalendarEventFeedService.getStart( o ).toLocalDate( ).isBefore( today.toLocalDate( ) ) )
                .limit( MAX_NEXT_OCCURRENCES ).toList( );
        String strMapUrl = ( details != null ) && details.isLocated( )
                ? String.format( java.util.Locale.ROOT, URL_EVENT_OSM, details.getLatitude( ), details.getLongitude( ), details.getLatitude( ), details.getLongitude( ) )
                : StringUtils.defaultString( event.getMapUrl( ) );

        event.setStatus( strStatus );
        model.put( MARK_EVENT_HAS_IMAGE, CalendarHome.hasImage( event.getId( ), plugin ) );
        model.put( MARK_EVENT_CATEGORIES, CategoryHome.findByEvent( event.getId( ), plugin ) );
        model.put( MARK_EVENT_NEXT_OCCURRENCES, ( listOccurrences.size( ) > 1 ) ? listNext : List.of( ) );
        model.put( MARK_EVENT_OCCURRENCES_COUNT, listOccurrences.size( ) );
        model.put( MARK_EVENT_MAP_URL, strMapUrl );
        model.put( MARK_EVENT_MAP_TILES, ( details != null ) && details.isLocated( ) ? MapPreview.getTiles( details.getLatitude( ), details.getLongitude( ) )
                : List.of( ) );
        model.put( MARK_EVENT_DETAILS, details );
        model.put( MARK_EVENT_STATUS, strStatus );
        model.put( MARK_EVENT_JSON_LD, HtmlMarkup.of( _structuredDataService.buildJsonLd( event, details,
                strBaseUrl + String.format( URL_SHOW_EVENT, event.getId( ), event.getIdCalendar( ) ), strImageUrl ) ) );
    }
}
