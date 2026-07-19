package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__add_task_collaboration extends BaseJavaMigration {
    private static final String[][] DEFAULT_CATEGORIES = {
            {"Nghiên cứu", "nghiên cứu", "#2563EB", "Search"},
            {"Viết nội dung", "viết nội dung", "#16A34A", "FileText"},
            {"Thiết kế", "thiết kế", "#7C3AED", "Palette"},
            {"Lập trình", "lập trình", "#0891B2", "Code2"},
            {"Thuyết trình", "thuyết trình", "#EA580C", "Presentation"},
            {"Kiểm tra/chỉnh sửa", "kiểm tra/chỉnh sửa", "#DC2626", "CheckCircle2"}
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "tasks") || !tableExists(connection, "projects")) return;

        createWorkCategories(connection);
        addColumn(connection, "tasks", "work_category_id", "BIGINT NULL");
        migrateTaskCategories(connection);
        createCollaborationTables(connection);
        migrateInitialWatchers(connection);
        extendNotifications(connection);

        createIndex(connection, "tasks", "idx_tasks_work_category", false, "work_category_id");
        createIndex(connection, "notifications", "uk_notification_dedup", true, "dedup_key");
        createIndex(connection, "notifications", "idx_notification_project", false, "project_id");
        createIndex(connection, "notifications", "idx_notification_task", false, "task_id");
        addForeignKey(connection, "tasks", "fk_tasks_work_category", "work_category_id",
                "work_categories", "id", "RESTRICT");
        addForeignKey(connection, "notifications", "fk_notification_project", "project_id",
                "projects", "id", "SET NULL");
        addForeignKey(connection, "notifications", "fk_notification_task", "task_id",
                "tasks", "id", "SET NULL");
    }

    private void createWorkCategories(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS work_categories (
                      id BIGINT NOT NULL AUTO_INCREMENT, project_id BIGINT NOT NULL, name VARCHAR(100) NOT NULL,
                      normalized_name VARCHAR(100) NOT NULL, color VARCHAR(20) NOT NULL, icon VARCHAR(50) NOT NULL,
                      position INT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, created_by_id BIGINT NOT NULL,
                      created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
                      PRIMARY KEY(id), UNIQUE INDEX uk_work_category_name(project_id, normalized_name),
                      INDEX idx_work_category_project_active(project_id, active, position),
                      CONSTRAINT fk_work_category_project FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE,
                      CONSTRAINT fk_work_category_creator FOREIGN KEY(created_by_id) REFERENCES users(id) ON DELETE RESTRICT
                    )
                    """);
            for (int position = 0; position < DEFAULT_CATEGORIES.length; position++) {
                String[] category = DEFAULT_CATEGORIES[position];
                statement.executeUpdate("INSERT IGNORE INTO work_categories(project_id,name,normalized_name,color,icon,position,active,created_by_id,created_at,updated_at,version) "
                        + "SELECT id,'" + category[0] + "','" + category[1] + "','" + category[2] + "','" + category[3] + "',"
                        + position + ",TRUE,created_by_id,NOW(6),NOW(6),0 FROM projects");
            }
        }
    }

    private void migrateTaskCategories(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT IGNORE INTO work_categories(project_id,name,normalized_name,color,icon,position,active,created_by_id,created_at,updated_at,version)
                    SELECT DISTINCT t.project_id, TRIM(t.type), LOWER(TRIM(t.type)), '#64748B', 'Tag', 100, TRUE,
                           p.created_by_id, NOW(6), NOW(6), 0
                    FROM tasks t JOIN projects p ON p.id=t.project_id
                    WHERE t.type IS NOT NULL AND TRIM(t.type)<>''
                      AND UPPER(TRIM(t.type)) NOT IN ('DESIGN','CONTENT','RESEARCH','PROGRAMMING','PRESENTATION','TESTING')
                    """);
            statement.executeUpdate("""
                    UPDATE tasks t
                    JOIN work_categories wc ON wc.project_id=t.project_id
                      AND wc.normalized_name=LOWER(TRIM(CASE UPPER(TRIM(t.type))
                        WHEN 'DESIGN' THEN 'thiết kế'
                        WHEN 'CONTENT' THEN 'viết nội dung'
                        WHEN 'RESEARCH' THEN 'nghiên cứu'
                        WHEN 'PROGRAMMING' THEN 'lập trình'
                        WHEN 'PRESENTATION' THEN 'thuyết trình'
                        WHEN 'TESTING' THEN 'kiểm tra/chỉnh sửa'
                        ELSE t.type END))
                    SET t.work_category_id=wc.id WHERE t.work_category_id IS NULL
                    """);
            statement.executeUpdate("UPDATE tasks t JOIN work_categories wc ON wc.project_id=t.project_id "
                    + "AND wc.normalized_name='thiết kế' SET t.work_category_id=wc.id WHERE t.work_category_id IS NULL");
            statement.execute("ALTER TABLE tasks MODIFY work_category_id BIGINT NOT NULL");
        }
    }

    private void createCollaborationTables(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS task_activities (
                      id BIGINT NOT NULL AUTO_INCREMENT, task_id BIGINT NOT NULL, actor_id BIGINT NULL,
                      action_type VARCHAR(60) NOT NULL, field_name VARCHAR(80), old_value VARCHAR(1000),
                      new_value VARCHAR(1000), metadata VARCHAR(2000), created_at DATETIME(6) NOT NULL,
                      PRIMARY KEY(id), INDEX idx_task_activity_task_created(task_id,created_at),
                      INDEX idx_task_activity_actor(actor_id),
                      CONSTRAINT fk_task_activity_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                      CONSTRAINT fk_task_activity_actor FOREIGN KEY(actor_id) REFERENCES users(id) ON DELETE SET NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS task_watchers (
                      id BIGINT NOT NULL AUTO_INCREMENT, task_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
                      created_at DATETIME(6) NOT NULL, PRIMARY KEY(id),
                      UNIQUE INDEX uk_task_watcher(task_id,user_id), INDEX idx_task_watcher_user(user_id),
                      CONSTRAINT fk_task_watcher_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                      CONSTRAINT fk_task_watcher_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS task_comment_mentions (
                      comment_id BIGINT NOT NULL, user_id BIGINT NOT NULL, PRIMARY KEY(comment_id,user_id),
                      INDEX idx_comment_mention_user(user_id),
                      CONSTRAINT fk_comment_mention_comment FOREIGN KEY(comment_id) REFERENCES task_comments(id) ON DELETE CASCADE,
                      CONSTRAINT fk_comment_mention_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private void migrateInitialWatchers(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT IGNORE INTO task_watchers(task_id,user_id,created_at) "
                    + "SELECT id,created_by_id,NOW(6) FROM tasks WHERE created_by_id IS NOT NULL");
            statement.executeUpdate("INSERT IGNORE INTO task_watchers(task_id,user_id,created_at) "
                    + "SELECT id,assigned_to_id,NOW(6) FROM tasks WHERE assigned_to_id IS NOT NULL");
        }
    }

    private void extendNotifications(Connection connection) throws Exception {
        addColumn(connection, "notifications", "project_id", "BIGINT NULL");
        addColumn(connection, "notifications", "task_id", "BIGINT NULL");
        addColumn(connection, "notifications", "dedup_key", "VARCHAR(180) NULL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE notifications MODIFY type ENUM('TASK_ASSIGNED','TASK_COMMENT','FILE_UPLOADED',"
                    + "'MEETING_CREATED','TASK_REVIEW_SUBMITTED','TASK_REVIEW_APPROVED','TASK_REVIEW_CHANGES_REQUESTED',"
                    + "'TASK_UNASSIGNED','TASK_MENTION','TASK_WATCHED_COMMENT','TASK_DUE_SOON','TASK_OVERDUE',"
                    + "'WEEKLY_PLAN_STARTED','WEEKLY_PLAN_ENDING') NOT NULL");
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

    private void addForeignKey(Connection connection, String table, String name, String column,
            String targetTable, String targetColumn, String deleteRule) throws Exception {
        if (!constraintExists(connection, table, name)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " FOREIGN KEY(" + column
                        + ") REFERENCES " + targetTable + "(" + targetColumn + ") ON DELETE " + deleteRule);
            }
        }
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        try (ResultSet result = connection.getMetaData().getTables(connection.getCatalog(), null, table, new String[] {"TABLE"})) {
            return result.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
        }
    }

    private boolean indexExists(Connection connection, String table, String name) throws Exception {
        try (ResultSet result = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (result.next()) if (name.equalsIgnoreCase(result.getString("INDEX_NAME"))) return true;
            return false;
        }
    }

    private boolean constraintExists(Connection connection, String table, String name) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                        + "WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "' AND CONSTRAINT_NAME='" + name + "'")) {
            result.next();
            return result.getInt(1) > 0;
        }
    }
}
