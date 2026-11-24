<%@ page errorPage="../../ErrorPage.jsp" %>

<%@page import="fr.paris.lutece.plugins.calendar.web.portlet.CalendarPortletJspBean"%>

${ calendarPortletJspBean.init( pageContext.request, CalendarPortletJspBean.RIGHT_MANAGE_ADMIN_SITE ) }
${ pageContext.response.sendRedirect( calendarPortletJspBean.doCreate( pageContext.request ) ) }
