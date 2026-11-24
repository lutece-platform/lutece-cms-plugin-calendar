<%@ page errorPage="../../ErrorPage.jsp" %>
<jsp:include page="../../PortletAdminHeader.jsp" />

<%@page import="fr.paris.lutece.plugins.calendar.web.portlet.MiniCalendarPortletJspBean"%>

${ miniCalendarPortletJspBean.init( pageContext.request, MiniCalendarPortletJspBean.RIGHT_MANAGE_ADMIN_SITE ) }
${ miniCalendarPortletJspBean.getModify( pageContext.request ) }

<%@ include file="../../AdminFooter.jsp" %>
