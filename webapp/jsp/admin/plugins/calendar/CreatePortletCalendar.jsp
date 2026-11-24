<%@ page errorPage="../../ErrorPage.jsp" %>
<jsp:include page="../../PortletAdminHeader.jsp" />

<%@page import="fr.paris.lutece.plugins.calendar.web.portlet.CalendarPortletJspBean"%>

${ calendarPortletJspBean.init( pageContext.request, CalendarPortletJspBean.RIGHT_MANAGE_ADMIN_SITE ) }
${ calendarPortletJspBean.getCreate( pageContext.request ) }

<%@ include file="../../AdminFooter.jsp" %>
