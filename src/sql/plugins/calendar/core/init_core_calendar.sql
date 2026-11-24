-- liquibase formatted sql
-- changeset calendar:init_core_calendar.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_admin_right WHERE id_right = 'CALENDAR_MANAGEMENT'
--
-- Dumping data for table core_admin_right
--
INSERT INTO core_admin_right(id_right,name,level_right,admin_url,description,is_updatable,plugin_name,id_feature_group,icon_url,documentation_url)
	VALUES('CALENDAR_MANAGEMENT','calendar.adminFeature.calendar_management.name',1,'jsp/admin/plugins/calendar/ManageCalendars.jsp','calendar.adminFeature.calendar_management.description',0,'calendar',NULL,'ti ti-calendar',NULL);

--
-- Dumping data for table core_portlet_type
--
INSERT INTO core_portlet_type(id_portlet_type,name,url_creation,url_update,home_class,plugin_name,url_docreate,create_script,create_specific,create_specific_form,url_domodify,modify_script,modify_specific,modify_specific_form)
	VALUES('CALENDAR_PORTLET','calendar.portlet.name','plugins/calendar/CreatePortletCalendar.jsp','plugins/calendar/ModifyPortletCalendar.jsp','fr.paris.lutece.plugins.calendar.business.portlet.CalendarPortletHome','calendar','plugins/calendar/DoCreatePortletCalendar.jsp','/admin/portlet/script_create_portlet.html','/admin/plugins/calendar/portlet_create_calendar.html','','plugins/calendar/DoModifyPortletCalendar.jsp','/admin/portlet/script_modify_portlet.html','/admin/plugins/calendar/portlet_modify_calendar.html','');
INSERT INTO core_portlet_type(id_portlet_type,name,url_creation,url_update,home_class,plugin_name,url_docreate,create_script,create_specific,create_specific_form,url_domodify,modify_script,modify_specific,modify_specific_form)
	VALUES('MINI_CALENDAR_PORTLET','calendar.portlet.mini_agenda.name','plugins/calendar/CreatePortletMiniCalendar.jsp','plugins/calendar/ModifyPortletMiniCalendar.jsp','fr.paris.lutece.plugins.calendar.business.portlet.MiniCalendarPortletHome','calendar','plugins/calendar/DoCreatePortletMiniCalendar.jsp','/admin/portlet/script_create_portlet.html','/admin/plugins/calendar/portlet_create_mini_calendar.html','','plugins/calendar/DoModifyPortletMiniCalendar.jsp','/admin/portlet/script_modify_portlet.html','/admin/plugins/calendar/portlet_modify_mini_calendar.html','');

--
-- Init  table core_admin_role
--
INSERT INTO core_admin_role (role_key,role_description) VALUES ('calendar_manager', 'Calendar management');

--
-- Init  table core_admin_role_resource
--
INSERT INTO core_admin_role_resource (rbac_id,role_key,resource_type,resource_id,permission) VALUES (250,'calendar_manager','CALENDAR','*','*');

--
-- Init  table core_user_role
--
INSERT INTO core_user_role (role_key,id_user) VALUES ('calendar_manager',1);

--
-- Init  table core_admin_dashboard
--
INSERT INTO core_admin_dashboard(dashboard_name, dashboard_column, dashboard_order) VALUES('calendarAdminDashboardComponent', 1, 1);

--
-- Init  table core_dashboard
--
INSERT INTO core_dashboard(dashboard_name, dashboard_column, dashboard_order) VALUES('CALENDAR', 3, 1);

-- changeset calendar:init_core_calendar.sql-rev1.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The FreeMarker templates of the calendar portlets, the first one of a type is its default
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_portlet_template WHERE id_portlet_type IN ('CALENDAR_PORTLET','MINI_CALENDAR_PORTLET')
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Défaut', 'skin/plugins/calendar/portlet/calendar_portlet.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Organizedevent', 'skin/plugins/calendar/portlet/calendar_portlet_organized_events.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('MINI_CALENDAR_PORTLET', 'MiniAgenda', 'skin/plugins/calendar/portlet/mini_calendar_portlet.html');

-- changeset calendar:init_core_calendar.sql-rev2.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The administrator of a new site is given the calendar management right, as the other plugins give theirs
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_user_right WHERE id_right = 'CALENDAR_MANAGEMENT' AND id_user = 1
INSERT INTO core_user_right (id_right,id_user) VALUES ('CALENDAR_MANAGEMENT',1);

-- changeset calendar:init_core_calendar.sql-rev3
-- preconditions onFail:MARK_RAN onError:WARN
-- comment More templates of the calendar portlet: carousel, slider, timeline, countdown, widget, grid and list
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_portlet_template WHERE template_path = 'skin/plugins/calendar/portlet/calendar_portlet_carousel.html'
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Carousel', 'skin/plugins/calendar/portlet/calendar_portlet_carousel.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Slider', 'skin/plugins/calendar/portlet/calendar_portlet_slider.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Timeline', 'skin/plugins/calendar/portlet/calendar_portlet_timeline.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Countdown', 'skin/plugins/calendar/portlet/calendar_portlet_countdown.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Widget', 'skin/plugins/calendar/portlet/calendar_portlet_widget.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Grid', 'skin/plugins/calendar/portlet/calendar_portlet_grid.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-List', 'skin/plugins/calendar/portlet/calendar_portlet_list.html');

-- changeset calendar:init_core_calendar.sql-rev4
-- preconditions onFail:MARK_RAN onError:WARN
-- comment Compact templates of the calendar portlet, for a narrow column: compact list, bordered compact list and table
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_portlet_template WHERE template_path = 'skin/plugins/calendar/portlet/calendar_portlet_compact.html'
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Compact', 'skin/plugins/calendar/portlet/calendar_portlet_compact.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Compact-bordered', 'skin/plugins/calendar/portlet/calendar_portlet_compact_bordered.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Table', 'skin/plugins/calendar/portlet/calendar_portlet_table.html');
