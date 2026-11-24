<%@ page errorPage="../../ErrorPage.jsp" %>

<%@page import="fr.paris.lutece.plugins.calendar.web.portlet.MiniCalendarPortletJspBean"%>

${ miniCalendarPortletJspBean.init( pageContext.request, MiniCalendarPortletJspBean.RIGHT_MANAGE_ADMIN_SITE ) }
${ pageContext.response.sendRedirect( miniCalendarPortletJspBean.doModify( pageContext.request ) ) }
