-- liquibase formatted sql
-- changeset calendar:create_db_calendar.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM calendar_agendas WHERE 1 = 0
--
-- Table structure for table calendar_agendas
--
DROP TABLE IF EXISTS calendar_agendas;
CREATE TABLE calendar_agendas
(
	id_agenda int default 0 NOT NULL,
	agenda_name varchar(130) default NULL,
	agenda_image varchar(130) default NULL,
	agenda_prefix varchar(130) default NULL,
	role varchar(130) NOT NULL,
	workgroup_key varchar(50) default NULL,
	role_manage varchar(130) default NULL,
	is_notify smallint DEFAULT 0 NOT NULL,
	period_validity int DEFAULT -1,
	front_proposals smallint DEFAULT 0 NOT NULL,
	default_view varchar(10) default NULL,
	PRIMARY KEY (id_agenda)
);

--
-- Table structure for table calendar_events
--
DROP TABLE IF EXISTS calendar_events;
CREATE TABLE calendar_events
(
	id_event int NOT NULL,
	id_agenda int default 0 NOT NULL,
	event_date date NOT NULL,
	event_date_end date default NULL,
	event_time_start varchar(5) default NULL,
	event_time_end varchar(5) default NULL,
	event_title varchar(60) NOT NULL,
	event_date_occurence int default NULL,
	event_date_periodicity int default NULL,
	event_date_creation timestamp default CURRENT_TIMESTAMP NOT NULL,
	event_excluded_day varchar(13) default NULL,
	PRIMARY KEY (id_event)
);

--
-- Table structure for table calendar_events_occurrences
--
DROP TABLE IF EXISTS calendar_events_occurrences;
CREATE TABLE calendar_events_occurrences
(
	id_occurrence int NOT NULL,
	id_event int NOT NULL,
	id_agenda int NOT NULL,
	occurrence_date date NOT NULL,
	occurrence_time_start varchar(5) default NULL,
	occurrence_time_end varchar(5) default NULL,
	occurrence_title varchar(60) NOT NULL,
	occurrence_status varchar(20) default NULL,
	PRIMARY KEY (id_occurrence)
);

--
-- Table structure for table calendar_events_features
--
DROP TABLE IF EXISTS calendar_events_features;
CREATE TABLE calendar_events_features
(
	id_feature int NOT NULL,
	id_event int NOT NULL,
	feature_description long varchar NOT NULL,
	feature_tags varchar(130) default NULL,
	feature_location varchar(60) default NULL,
	feature_location_town varchar(60) default NULL,
	feature_location_zip varchar(20) default NULL,
	feature_location_address varchar(130) default NULL,
	feature_map_url varchar(130) default NULL,
	feature_link_url varchar(130) default NULL,
	document_id int default NULL,
	feature_page_url varchar(300) default NULL,
	feature_top_event int default 0,
	feature_image long varbinary default NULL,
	image_mime_type varchar(100) default NULL,
	PRIMARY KEY (id_feature)
);

--
-- Table structure for table calendar_subscriber_details
--
DROP TABLE IF EXISTS calendar_subscriber_details;
CREATE TABLE calendar_subscriber_details
(
	id_subscriber int default 0 NOT NULL,
	email varchar(100) default NULL,
	PRIMARY KEY (id_subscriber)
);

--
-- Table structure for table calendar_subscriber
--
DROP TABLE IF EXISTS calendar_subscriber;
CREATE TABLE calendar_subscriber
(
	id_subscriber int default 0 NOT NULL,
	id_agenda int default 0 NOT NULL,
	date_subscription timestamp default CURRENT_TIMESTAMP NOT NULL,
	PRIMARY KEY (id_subscriber, id_agenda)
);

--
-- Table structure for table calendar_category
--
DROP TABLE IF EXISTS calendar_category;
CREATE TABLE calendar_category
(
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
CREATE TABLE calendar_category_link
(
	id_event int NOT NULL,
	id_category int NOT NULL,
	PRIMARY KEY (id_event, id_category)
);

--
-- Table structure for table calendar_events_users
--
DROP TABLE IF EXISTS calendar_events_users;
CREATE TABLE calendar_events_users
(
	id_event int NOT NULL,
	user_login VARCHAR(100) NOT NULL,
	PRIMARY KEY (id_event, user_login)
);

--
-- Table structure for calendar_notify_key
--
DROP TABLE IF EXISTS calendar_notify_key;
CREATE TABLE calendar_notify_key
(
	key_email varchar(255) NOT NULL,
	email varchar(100) default NULL,
	id_agenda int DEFAULT NULL,
	date_expiry timestamp DEFAULT NULL NULL,
	PRIMARY KEY  (key_email)
);

--
-- Table structure for table calendar_parameter
--
DROP TABLE IF EXISTS calendar_parameter;
CREATE TABLE calendar_parameter
(
	parameter_key varchar(100) NOT NULL,
	parameter_value varchar(100) NOT NULL,
	PRIMARY KEY (parameter_key)
);

-- changeset calendar:create_db_calendar.sql-rev1
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The reminders sent to the subscribers, one row per occurrence and subscriber
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_reminder_sent'
CREATE TABLE calendar_reminder_sent (
	id_occurrence int NOT NULL,
	id_subscriber int NOT NULL,
	date_sent timestamp default CURRENT_TIMESTAMP NOT NULL,
	PRIMARY KEY (id_occurrence, id_subscriber)
);

-- changeset calendar:create_db_calendar.sql-rev2
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The options of the "add to calendar" button of each calendar, as JSON
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = 'calendar_button'
CREATE TABLE calendar_button (
	id_agenda int NOT NULL,
	config long varchar,
	PRIMARY KEY (id_agenda)
);

-- changeset calendar:create_db_calendar.sql-rev3
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

-- changeset calendar:create_db_calendar.sql-rev4
-- preconditions onFail:MARK_RAN onError:WARN
-- comment Each calendar may accept events proposed from the site by logged-in users
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = 'calendar_agendas' AND column_name = 'front_proposals'
ALTER TABLE calendar_agendas ADD COLUMN front_proposals smallint DEFAULT 0 NOT NULL;

-- changeset calendar:create_db_calendar.sql-rev5
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The view the calendar page opens on for each calendar: month, week, day or list (empty: month on a large screen, list on a small one)
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = 'calendar_agendas' AND column_name = 'default_view'
ALTER TABLE calendar_agendas ADD COLUMN default_view varchar(10) default NULL;
