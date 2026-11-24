-- liquibase formatted sql
-- changeset calendar:update_db_calendar-4.9.9-5.0.0.sql
-- preconditions onFail:MARK_RAN onError:WARN
--
-- The admin feature icon is a Tabler class in Lutece 8
--
UPDATE core_admin_right SET icon_url = 'ti ti-calendar' WHERE id_right = 'CALENDAR_MANAGEMENT';
--
-- The creation forms of the calendar portlets carry the security token of their specific template
--
UPDATE core_portlet_type SET create_specific = '/admin/plugins/calendar/portlet_create_calendar.html' WHERE id_portlet_type = 'CALENDAR_PORTLET';
UPDATE core_portlet_type SET create_specific = '/admin/plugins/calendar/portlet_create_mini_calendar.html' WHERE id_portlet_type = 'MINI_CALENDAR_PORTLET';

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev1.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The plugin upgrades run before the core ones: the column of the portlet templates is created as the core creates it when it is still missing
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = 'core_portlet' AND column_name = 'id_template'
ALTER TABLE core_portlet ADD COLUMN id_template int default 0 NOT NULL;

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev2.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The table of the portlet templates, created as the core creates it when it is still missing
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'core_portlet_template'
CREATE TABLE IF NOT EXISTS core_portlet_template (
	id_template int AUTO_INCREMENT NOT NULL,
	id_portlet_type varchar(50) default NULL,
	description varchar(255) default NULL,
	template_path varchar(255) default NULL,
	PRIMARY KEY (id_template)
);

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev3.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The FreeMarker templates replacing the XSL styles of the calendar portlets, the first one of a type is its default
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_portlet_template WHERE id_portlet_type IN ('CALENDAR_PORTLET','MINI_CALENDAR_PORTLET')
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Défaut', 'skin/plugins/calendar/portlet/calendar_portlet.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Organizedevent', 'skin/plugins/calendar/portlet/calendar_portlet_organized_events.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('MINI_CALENDAR_PORTLET', 'MiniAgenda', 'skin/plugins/calendar/portlet/mini_calendar_portlet.html');

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev4.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The calendar portlets rendered by the "organized events" XSL style keep that layout through its template; the others use the default template of their type
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = 'core_portlet' AND column_name = 'id_style'
UPDATE core_portlet SET id_template = ( SELECT MIN(id_template) FROM core_portlet_template WHERE id_portlet_type = 'CALENDAR_PORTLET' AND template_path = 'skin/plugins/calendar/portlet/calendar_portlet_organized_events.html' ) WHERE id_portlet_type = 'CALENDAR_PORTLET' AND id_style IN (1002, 1202);
UPDATE core_portlet SET id_style = 0 WHERE id_portlet_type IN ('CALENDAR_PORTLET', 'MINI_CALENDAR_PORTLET');

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev5.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The XSL styles of the calendar portlets are removed when the legacy style tables still exist
-- precondition-sql-check expectedResult:3 SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=database() AND TABLE_NAME IN ('core_style_mode_stylesheet','core_stylesheet','core_style');
DELETE FROM core_style_mode_stylesheet WHERE id_style IN (1000, 1001, 1002, 1200, 1201, 1202);
DELETE FROM core_stylesheet WHERE id_stylesheet IN (1000, 1001, 1002, 1200, 1201, 1202);
DELETE FROM core_style WHERE id_style IN (1000, 1001, 1002, 1200, 1201, 1202);

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev6.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The calendar dots are drawn in CSS: a calendar keeps the colour of its former dot image (images/.../dots/blue.png becomes blue)
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_agendas'
UPDATE calendar_agendas SET agenda_image = SUBSTRING_INDEX( SUBSTRING_INDEX( agenda_image, '/', -1 ), '.', 1 ) WHERE agenda_image LIKE '%/%' OR agenda_image LIKE '%.%';

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev7.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The reminders sent to the subscribers, one row per occurrence and subscriber
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_reminder_sent'
CREATE TABLE calendar_reminder_sent (
	id_occurrence int NOT NULL,
	id_subscriber int NOT NULL,
	date_sent timestamp default CURRENT_TIMESTAMP NOT NULL,
	PRIMARY KEY (id_occurrence, id_subscriber)
);

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev8.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The XSL export stylesheets are removed: the calendars are exported in iCalendar and JSON, the RSS feed is a FreeMarker template
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_export_stylesheets'
DROP TABLE calendar_export_stylesheets;

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev9.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The options of the "add to calendar" button of each calendar, as JSON
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_button'
CREATE TABLE calendar_button (
	id_agenda int NOT NULL,
	config long varchar,
	PRIMARY KEY (id_agenda)
);

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev10.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The details of the events: price, attendance mode, organizer, registration, accessibility and coordinates of the place
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_event_details'
CREATE TABLE calendar_event_details (
	id_event int NOT NULL,
	price_type varchar(10) default NULL,
	price varchar(100) default NULL,
	attendance_mode varchar(10) default NULL,
	online_url varchar(255) default NULL,
	organizer_name varchar(100) default NULL,
	organizer_email varchar(100) default NULL,
	organizer_phone varchar(30) default NULL,
	organizer_url varchar(255) default NULL,
	registration_url varchar(255) default NULL,
	accessibility varchar(255) default NULL,
	latitude decimal(10,7) default NULL,
	longitude decimal(10,7) default NULL,
	PRIMARY KEY (id_event)
);

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev11.sql
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

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev12.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment Compact templates of the calendar portlet, for a narrow column: compact list, bordered compact list and table
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_portlet_template WHERE template_path = 'skin/plugins/calendar/portlet/calendar_portlet_compact.html'
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Compact', 'skin/plugins/calendar/portlet/calendar_portlet_compact.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Compact-bordered', 'skin/plugins/calendar/portlet/calendar_portlet_compact_bordered.html');
INSERT INTO core_portlet_template (id_portlet_type, description, template_path) VALUES ('CALENDAR_PORTLET', 'Calendar-Table', 'skin/plugins/calendar/portlet/calendar_portlet_table.html');

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev13.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment Each calendar may accept events proposed from the site by logged-in users, published once an administrator confirms them
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = 'calendar_agendas' AND column_name = 'front_proposals'
ALTER TABLE calendar_agendas ADD COLUMN front_proposals smallint DEFAULT 0 NOT NULL;

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev14.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The view the calendar page opens on for each calendar: month, week, day or list (empty: month on a large screen, list on a small one)
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = 'calendar_agendas' AND column_name = 'default_view'
ALTER TABLE calendar_agendas ADD COLUMN default_view varchar(10) default NULL;

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev15.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The JavaScript library drawing the calendar views: event-calendar (EventCalendar) or agendo (Agendo)
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM calendar_parameter WHERE parameter_key = 'calendar_library'
INSERT INTO calendar_parameter (parameter_key, parameter_value) VALUES ('calendar_library', 'event-calendar');

-- changeset calendar:update_db_calendar-4.9.9-5.0.0-rev16.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The panel of the calendar advanced parameters in the technical menu, for the sites that lost or never got its row
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_admin_dashboard WHERE dashboard_name = 'calendarAdminDashboardComponent'
INSERT INTO core_admin_dashboard(dashboard_name, dashboard_column, dashboard_order) VALUES('calendarAdminDashboardComponent', 1, 1);
