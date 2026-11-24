-- liquibase formatted sql
-- changeset calendar:upgrade_db_core_calendar-3.0.1-3.0.2.sql
-- preconditions onFail:MARK_RAN onError:WARN
--
-- Update table core_admin_right
--
UPDATE core_admin_right SET admin_url = 'jsp/admin/plugins/calendar/ManageCalendars.jsp' WHERE id_right = 'CALENDAR_MANAGEMENT';

-- changeset calendar:upgrade_db_core_calendar-3.0.1-3.0.2-rev2.sql
-- preconditions onFail:MARK_RAN onError:WARN
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
