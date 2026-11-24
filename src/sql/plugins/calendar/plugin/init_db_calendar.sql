-- liquibase formatted sql
-- changeset calendar:init_db_calendar.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM calendar_parameter WHERE parameter_key = 'dashboard_n_next_days'
--
-- Dumping data for table calendar_parameter
--
INSERT INTO calendar_parameter (parameter_key, parameter_value) VALUES ('dashboard_n_next_days', '3');
INSERT INTO calendar_parameter (parameter_key, parameter_value) VALUES ('dashboard_nb_events', '3');

-- changeset calendar:init_db_calendar.sql-rev1
-- preconditions onFail:MARK_RAN onError:WARN
-- comment The JavaScript library drawing the calendar views: event-calendar (EventCalendar) or agendo (Agendo)
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM calendar_parameter WHERE parameter_key = 'calendar_library'
INSERT INTO calendar_parameter (parameter_key, parameter_value) VALUES ('calendar_library', 'event-calendar');
