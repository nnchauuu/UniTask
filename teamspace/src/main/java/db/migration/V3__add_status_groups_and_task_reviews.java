package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__add_status_groups_and_task_reviews extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "tasks") || !tableExists(connection, "board_columns")) return;

        addColumn(connection, "board_columns", "status_group", "VARCHAR(30) NULL");
        addColumn(connection, "board_columns", "is_default_for_group", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumn(connection, "board_columns", "default_group_key", "VARCHAR(30) NULL");
        addColumn(connection, "board_columns", "version", "BIGINT NOT NULL DEFAULT 0");
        addColumn(connection, "tasks", "review_required", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumn(connection, "tasks", "review_status", "VARCHAR(30) NOT NULL DEFAULT 'NONE'");
        addColumn(connection, "tasks", "reviewer_id", "BIGINT NULL");
        addColumn(connection, "tasks", "submitted_by_id", "BIGINT NULL");
        addColumn(connection, "tasks", "submitted_at", "DATETIME(6) NULL");
        addColumn(connection, "tasks", "reviewed_by_id", "BIGINT NULL");
        addColumn(connection, "tasks", "reviewed_at", "DATETIME(6) NULL");
        addColumn(connection, "tasks", "version", "BIGINT NOT NULL DEFAULT 0");
        if (tableExists(connection, "projects")) {
            addColumn(connection, "projects", "allow_custom_reviewers", "BOOLEAN NOT NULL DEFAULT FALSE");
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE board_columns SET status_group = CASE "
                    + "WHEN column_key = 'TODO' THEN 'TODO' "
                    + "WHEN column_key = 'IN_PROGRESS' THEN 'IN_PROGRESS' "
                    + "WHEN column_key = 'REVIEW' THEN 'IN_REVIEW' "
                    + "WHEN column_key = 'DONE' THEN 'DONE' "
                    + "ELSE 'IN_PROGRESS' END WHERE status_group IS NULL");
            statement.execute("ALTER TABLE board_columns MODIFY status_group VARCHAR(30) NOT NULL");
            statement.executeUpdate("UPDATE board_columns SET is_default_for_group = FALSE, default_group_key = NULL");
            statement.executeUpdate("UPDATE board_columns SET is_default_for_group = TRUE, default_group_key = status_group "
                    + "WHERE column_key IN ('TODO','IN_PROGRESS','REVIEW','DONE')");
            statement.executeUpdate("UPDATE board_columns bc JOIN ("
                    + "SELECT project_id, status_group, MIN(position) min_position FROM board_columns "
                    + "WHERE status_group NOT IN (SELECT status_group FROM board_columns defaults WHERE defaults.project_id = board_columns.project_id AND defaults.is_default_for_group = TRUE) "
                    + "GROUP BY project_id, status_group) missing "
                    + "ON bc.project_id = missing.project_id AND bc.status_group = missing.status_group AND bc.position = missing.min_position "
                    + "SET bc.is_default_for_group = TRUE, bc.default_group_key = bc.status_group");
        }

        createIndex(connection, "board_columns", "uk_board_default_group", true, "project_id, default_group_key");
        createIndex(connection, "tasks", "idx_tasks_reviewer", false, "reviewer_id");
        createIndex(connection, "tasks", "idx_tasks_submitted_by", false, "submitted_by_id");
        createIndex(connection, "tasks", "idx_tasks_reviewed_by", false, "reviewed_by_id");
        addForeignKey(connection, "tasks", "fk_tasks_reviewer", "reviewer_id", "users", "id", "SET NULL");
        addForeignKey(connection, "tasks", "fk_tasks_submitted_by", "submitted_by_id", "users", "id", "SET NULL");
        addForeignKey(connection, "tasks", "fk_tasks_reviewed_by", "reviewed_by_id", "users", "id", "SET NULL");

        if (!tableExists(connection, "task_review_history")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE task_review_history (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            task_id BIGINT NOT NULL,
                            action VARCHAR(30) NOT NULL,
                            actor_id BIGINT NOT NULL,
                            reviewer_id BIGINT NULL,
                            comment VARCHAR(1000) NULL,
                            created_at DATETIME(6) NOT NULL,
                            PRIMARY KEY (id),
                            INDEX idx_review_history_task_created (task_id, created_at),
                            INDEX idx_review_history_reviewer (reviewer_id),
                            CONSTRAINT fk_review_history_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                            CONSTRAINT fk_review_history_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT,
                            CONSTRAINT fk_review_history_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL
                        )
                        """);
            }
        }
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

    private void addForeignKey(Connection connection, String table, String name, String column, String targetTable, String targetColumn, String onDelete) throws Exception {
        if (!constraintExists(connection, table, name)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " FOREIGN KEY (" + column + ") REFERENCES "
                        + targetTable + "(" + targetColumn + ") ON DELETE " + onDelete);
            }
        }
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        try (ResultSet result = connection.getMetaData().getTables(connection.getCatalog(), null, table, new String[] { "TABLE" })) { return result.next(); }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) { return result.next(); }
    }

    private boolean indexExists(Connection connection, String table, String index) throws Exception {
        try (ResultSet result = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (result.next()) if (index.equalsIgnoreCase(result.getString("INDEX_NAME"))) return true;
            return false;
        }
    }

    private boolean constraintExists(Connection connection, String table, String constraint) throws Exception {
        try (var statement = connection.prepareStatement("SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?")) {
            statement.setString(1, table);
            statement.setString(2, constraint);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }
}
