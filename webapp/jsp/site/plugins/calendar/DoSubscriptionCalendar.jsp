<%@ page errorPage="../../ErrorPagePortal.jsp" %>

${ pageContext.response.sendRedirect( calendarSiteJspBean.doSubscription( pageContext.request ) ) }
