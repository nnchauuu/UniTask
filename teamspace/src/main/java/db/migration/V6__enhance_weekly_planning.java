package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__enhance_weekly_planning extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "weekly_plans") || !tableExists(connection, "tasks")) return;

        addColumn(connection, "weekly_plans", "description", "VARCHAR(2000) NULL");
        addColumn(connection, "weekly_plans", "capacity", "DECIMAL(10,2) NULL");
        addColumn(connection, "weekly_plans", "estimate_unit", "VARCHAR(20) NOT NULL DEFAULT 'HOURS'");
        addColumn(connection, "weekly_plans", "started_by_id", "BIGINT NULL");
        addColumn(connection, "weekly_plans", "completed_by_id", "BIGINT NULL");
        addColumn(connection, "weekly_plans", "cancelled_by_id", "BIGINT NULL");
        addColumn(connection, "weekly_plans", "cancelled_at", "DATETIME(6) NULL");
        addColumn(connection, "tasks", "estimated_effort", "DECIMAL(10,2) NOT NULL DEFAULT 0");
        addColumn(connection, "tasks", "actual_effort", "DECIMAL(10,2) NULL");

        addForeignKey(connection, "weekly_plans", "fk_weekly_plan_started_by", "started_by_id", "users", "id");
        addForeignKey(connection, "weekly_plans", "fk_weekly_plan_completed_by", "completed_by_id", "users", "id");
        addForeignKey(connection, "weekly_plans", "fk_weekly_plan_cancelled_by", "cancelled_by_id", "users", "id");

        if (!tableExists(connection, "weekly_plan_task_snapshots")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE weekly_plan_task_snapshots (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        weekly_plan_id BIGINT NOT NULL,
                        task_id BIGINT NOT NULL,
                        task_code VARCHAR(40) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        assignee_id BIGINT NULL,
                        assignee_name VARCHAR(150) NULL,
                        priority VARCHAR(30) NOT NULL,
                        estimated_effort DECIMAL(10,2) NOT NULL DEFAULT 0,
                        actual_effort DECIMAL(10,2) NULL,
                        completed BOOLEAN NOT NULL,
                        sort_order BIGINT NOT NULL,
                        captured_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE INDEX uk_plan_snapshot_task (weekly_plan_id, task_id),
                        INDEX idx_plan_snapshot_assignee (weekly_plan_id, assignee_id),
                        CONSTRAINT fk_plan_snapshot_plan FOREIGN KEY (weekly_plan_id) REFERENCES weekly_plans(id) ON DELETE CASCADE
                    )
                    """);
            }
        }
        addColumn(connection, "weekly_plan_task_snapshots", "actual_effort", "DECIMAL(10,2) NULL");
    }

    private void addColumn(Connection c, String table, String column, String definition) throws Exception {
        if (!columnExists(c, table, column)) try (Statement s = c.createStatement()) {
            s.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void addForeignKey(Connection c, String table, String name, String column, String target, String targetColumn) throws Exception {
        if (!constraintExists(c, table, name)) try (Statement s = c.createStatement()) {
            s.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " FOREIGN KEY (" + column + ") REFERENCES " + target + "(" + targetColumn + ") ON DELETE SET NULL");
        }
    }

    private boolean tableExists(Connection c, String table) throws Exception {
        try (ResultSet r = c.getMetaData().getTables(c.getCatalog(), null, table, new String[]{"TABLE"})) { return r.next(); }
    }

    private boolean columnExists(Connection c, String table, String column) throws Exception {
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) { return r.next(); }
    }

    private boolean constraintExists(Connection c, String table, String name) throws Exception {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='" + table + "' AND CONSTRAINT_NAME='" + name + "'")) {
            r.next(); return r.getInt(1) > 0;
        }
    }
}
