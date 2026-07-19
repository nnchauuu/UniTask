package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__add_weekly_planning extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "tasks") || !tableExists(connection, "projects")) return;

        if (!tableExists(connection, "weekly_plans")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE weekly_plans (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            project_id BIGINT NOT NULL,
                            name VARCHAR(150) NOT NULL,
                            goal VARCHAR(1000) NULL,
                            start_date DATE NOT NULL,
                            end_date DATE NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            active_project_key BIGINT NULL,
                            created_by_id BIGINT NOT NULL,
                            started_at DATETIME(6) NULL,
                            completed_at DATETIME(6) NULL,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,
                            version BIGINT NOT NULL DEFAULT 0,
                            PRIMARY KEY (id),
                            UNIQUE INDEX uk_weekly_plan_active_project (active_project_key),
                            INDEX idx_weekly_plan_project_status (project_id, status),
                            INDEX idx_weekly_plan_created_by (created_by_id),
                            CONSTRAINT fk_weekly_plan_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                            CONSTRAINT fk_weekly_plan_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE RESTRICT
                        )
                        """);
            }
        }

        addColumn(connection, "tasks", "planning_state", "VARCHAR(20) NULL");
        addColumn(connection, "tasks", "weekly_plan_id", "BIGINT NULL");
        addColumn(connection, "tasks", "backlog_position", "BIGINT NULL");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO weekly_plans (
                        project_id, name, goal, start_date, end_date, status, active_project_key,
                        created_by_id, started_at, created_at, updated_at, version
                    )
                    SELECT p.id, 'Kế hoạch hiện tại', 'Kế hoạch được tạo tự động để giữ nguyên công việc hiện có',
                           CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 'ACTIVE', p.id,
                           p.created_by_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 0
                    FROM projects p
                    WHERE EXISTS (SELECT 1 FROM tasks t WHERE t.project_id = p.id)
                      AND NOT EXISTS (SELECT 1 FROM weekly_plans wp WHERE wp.project_id = p.id)
                    """);
            statement.executeUpdate("""
                    UPDATE tasks t
                    JOIN weekly_plans wp ON wp.project_id = t.project_id AND wp.status = 'ACTIVE'
                    SET t.planning_state = 'ACTIVE', t.weekly_plan_id = wp.id
                    WHERE t.planning_state IS NULL
                    """);
            statement.executeUpdate("UPDATE tasks SET planning_state = 'ACTIVE' WHERE planning_state IS NULL");
            statement.execute("ALTER TABLE tasks MODIFY planning_state VARCHAR(20) NOT NULL");
        }

        createIndex(connection, "tasks", "idx_tasks_planning", false, "project_id, planning_state, backlog_position");
        createIndex(connection, "tasks", "idx_tasks_weekly_plan", false, "weekly_plan_id");
        addForeignKey(connection, "tasks", "fk_tasks_weekly_plan", "weekly_plan_id", "weekly_plans", "id", "SET NULL");
    }

    private void addColumn(Connection connection, String table, String column, String definition) throws Exception {
        if (!columnExists(connection, table, column)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    private void createIndex(Connection connection, String table, String name, boolean unique, String columns) throws Exception {
        if (!indexExists(connection, table, name)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + name + " ON " + table + " (" + columns + ")");
            }
        }
    }

    private void addForeignKey(Connection connection, String table, String name, String column,
            String targetTable, String targetColumn, String onDelete) throws Exception {
        if (!constraintExists(connection, table, name)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " FOREIGN KEY (" + column
                        + ") REFERENCES " + targetTable + "(" + targetColumn + ") ON DELETE " + onDelete);
            }
        }
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        try (ResultSet result = connection.getMetaData().getTables(connection.getCatalog(), null, table, new String[] { "TABLE" })) {
            return result.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
        }
    }

    private boolean indexExists(Connection connection, String table, String index) throws Exception {
        try (ResultSet result = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (result.next()) if (index.equalsIgnoreCase(result.getString("INDEX_NAME"))) return true;
            return false;
        }
    }

    private boolean constraintExists(Connection connection, String table, String constraint) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = '" + table + "' AND CONSTRAINT_NAME = '" + constraint + "'")) {
            result.next();
            return result.getInt(1) > 0;
        }
    }
}
