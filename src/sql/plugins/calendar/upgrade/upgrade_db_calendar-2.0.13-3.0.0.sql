-- liquibase formatted sql
-- changeset calendar:upgrade_db_calendar-2.0.13-3.0.0.sql
-- preconditions onFail:MARK_RAN onError:WARN

ALTER TABLE calendar_events
  ADD COLUMN event_date_end DATE NOT NULL AFTER event_date,
  ADD COLUMN event_date_occurence int DEFAULT 0 AFTER event_title,
  ADD COLUMN event_date_periodicity int DEFAULT 1 AFTER event_title,
  ADD COLUMN event_date_creation timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE calendar_portlet
	MODIFY COLUMN date_begin DATE DEFAULT NULL,
	MODIFY COLUMN date_end DATE DEFAULT NULL;

ALTER TABLE calendar_events 
	MODIFY COLUMN event_date DATE NOT NULL;

--
-- Table structure for table calendar_events_occurences
--
	DROP TABLE IF EXISTS calendar_events_occurrences;
	CREATE TABLE calendar_events_occurrences (
		id_occurrence int NOT NULL,
		id_event int NOT NULL,
		id_agenda int NOT NULL,  
		occurrence_date date NOT NULL,
		occurrence_time_start varchar(5) DEFAULT NULL,
		occurrence_time_end varchar(5) DEFAULT NULL,
		occurrence_title varchar(60) NOT NULL,
		occurrence_status varchar(20) DEFAULT NULL,
		PRIMARY KEY  (id_occurrence)
);

--
-- Table structure for table calendar_events_features
--
	DROP TABLE IF EXISTS calendar_events_features;
	CREATE TABLE calendar_events_features (
		id_feature int NOT NULL,
		id_event int NOT NULL,
		feature_description TEXT NOT NULL,
		feature_tags varchar(130) DEFAULT NULL,
		feature_location varchar(60) DEFAULT NULL,
		feature_location_town varchar(60) DEFAULT NULL,
		feature_location_zip varchar(20) DEFAULT NULL,
		feature_location_address varchar(130) DEFAULT NULL,
		feature_map_url varchar(130) DEFAULT NULL,
		feature_link_url varchar(130) DEFAULT NULL,
		document_id int DEFAULT NULL,
		feature_page_url varchar(300) DEFAULT NULL,
		feature_top_event int DEFAULT 0,
		feature_image long varbinary default NULL,
		image_mime_type varchar(100) default NULL,
	  	PRIMARY KEY  (id_feature)
);

--
-- Table structure for table calendar_subscriber_details
--
	DROP TABLE IF EXISTS calendar_subscriber_details;
	CREATE TABLE calendar_subscriber_details (
		id_subscriber INT DEFAULT '0' NOT NULL,
		email VARCHAR(100) DEFAULT NULL,
		PRIMARY KEY (id_subscriber)
);

--
-- Table structure for table calendar_subscriber
--
	DROP TABLE IF EXISTS calendar_subscriber;
	CREATE TABLE calendar_subscriber (
		id_subscriber INT DEFAULT '0' NOT NULL,
		id_agenda INT DEFAULT '0' NOT NULL,
		date_subscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
		PRIMARY KEY (id_subscriber, id_agenda)
);

--
-- Table structure for table calendar_category
--
	DROP TABLE IF EXISTS calendar_category;
	CREATE TABLE calendar_category (
		id_category int NOT NULL,
		calendar_category_name varchar(100) NOT NULL,
		description varchar(255) default NULL,
		icon_content long varbinary default NULL,
		icon_mime_type varchar(100) default NULL,
		workgroup_key varchar(50) default NULL,
		PRIMARY KEY (id_category)	
);

--
-- Table structure for table calendar_category_link
--
	DROP TABLE IF EXISTS calendar_category_link;
	CREATE TABLE calendar_category_link (
		id_event int NOT NULL,
		id_category int NOT NULL,
		PRIMARY KEY (id_event,id_category)
);

--
-- Table structure for table calendar_mini_portlet
--
DROP TABLE IF EXISTS calendar_mini_portlet;
CREATE TABLE calendar_mini_portlet
(
	top_event smallint default 0 NOT NULL,
	PRIMARY KEY (top_event)
);

-- changeset calendar:upgrade_db_calendar-2.0.13-3.0.0-rev2.sql
-- preconditions onFail:MARK_RAN onError:WARN
--
-- Dumping data for core_portlet_type
--
INSERT INTO core_portlet_type(id_portlet_type,name,url_creation,url_update,home_class,plugin_name,url_docreate,create_script,create_specific,create_specific_form,url_domodify,modify_script,modify_specific,modify_specific_form)
	VALUES('MINI_CALENDAR_PORTLET','calendar.portlet.mini_agenda.name','plugins/calendar/CreatePortletMiniCalendar.jsp','plugins/calendar/ModifyPortletMiniCalendar.jsp','fr.paris.lutece.plugins.calendar.business.portlet.MiniCalendarPortletHome','calendar','plugins/calendar/DoCreatePortletMiniCalendar.jsp','/admin/portlet/script_create_portlet.html','','','plugins/calendar/DoModifyPortletMiniCalendar.jsp','/admin/portlet/script_modify_portlet.html','/admin/plugins/calendar/portlet_modify_mini_calendar.html','');

--
-- Update data for core_admin_right
--
UPDATE core_admin_right SET admin_url='jsp/admin/plugins/calendar/ManageCalendarsMenu.jsp' WHERE id_right='CALENDAR_MANAGEMENT';

--
-- Update data for calendar_events
--
UPDATE calendar_events SET event_date_end = event_date, event_date = event_date, event_date_creation = CURRENT_DATE;

--
-- Insert data for calendar_events_features
--
SET @position = 0;
INSERT INTO calendar_events_features (id_feature, id_event, feature_description)  SELECT @position := @position + 1,calendar_events.id_event, calendar_events.event_title FROM calendar_events;
