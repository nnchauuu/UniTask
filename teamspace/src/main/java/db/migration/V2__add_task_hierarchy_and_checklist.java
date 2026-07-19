package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__add_task_hierarchy_and_checklist extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "tasks")) return;

        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "tasks", "parent_task_id")) {
                statement.execute("ALTER TABLE tasks ADD COLUMN parent_task_id BIGINT NULL");
            }
            if (!tableExists(connection, "task_checklist_items")) {
                statement.execute("""
                        CREATE TABLE task_checklist_items (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            task_id BIGINT NOT NULL,
                            content VARCHAR(300) NOT NULL,
                            completed BOOLEAN NOT NULL DEFAULT FALSE,
                            position INT NOT NULL DEFAULT 0,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,
                            PRIMARY KEY (id),
                            INDEX idx_checklist_task (task_id),
                            CONSTRAINT fk_checklist_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                        )
                        """);
            }
        }
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getTables(connection.getCatalog(), null, table, new String[] { "TABLE" })) {
            return result.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
        }
    }
}
