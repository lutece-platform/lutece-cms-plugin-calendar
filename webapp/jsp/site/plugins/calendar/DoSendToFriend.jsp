<%@ page errorPage="../../ErrorPagePortal.jsp" %>

${ pageContext.response.sendRedirect( calendarSiteJspBean.doSendToFriend( pageContext.request ) ) }
