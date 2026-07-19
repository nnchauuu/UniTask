import { useEffect, useState } from "react";
import * as activityLogApi from "../api/activityLogApi";

const actionLabels = {
  PROJECT_CREATED: "created project",
  PROJECT_UPDATED: "updated project",
  TASK_CREATED: "created task",
  TASK_UPDATED: "updated task",
  TASK_STATUS_CHANGED: "changed task status",
  TASK_COMMENTED: "commented on task",
  FILE_UPLOADED: "uploaded file",
  FILE_DELETED: "deleted file",
  MEETING_CREATED: "created meeting"
};

function ActivityLog({ projectId, refreshKey = 0 }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadLogs = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await activityLogApi.getProjectActivityLogs(projectId);
      setLogs(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Cannot load activity logs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [projectId, refreshKey]);

  return (
    <section className="bg-white border rounded p-4 shadow-sm mb-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <p className="text-uppercase text-primary fw-semibold small mb-1">Activity</p>
          <h2 className="h5 fw-bold mb-0">Project timeline</h2>
        </div>
        <button className="btn btn-outline-secondary btn-sm" onClick={loadLogs}>
          Refresh
        </button>
      </div>

      {loading && <div className="alert alert-info">Loading activity logs...</div>}
      {error && <div className="alert alert-danger">{error}</div>}
      {!loading && !error && logs.length === 0 && (
        <div className="text-secondary small border rounded p-4 text-center">No activity yet.</div>
      )}

      {!loading && !error && logs.length > 0 && (
        <div className="activity-timeline">
          {logs.map((log) => (
            <div className="activity-item" key={log.id}>
              <div className="activity-dot" />
              <div className="activity-content border rounded p-3">
                <div className="d-flex justify-content-between align-items-start gap-3">
                  <div>
                    <div className="fw-semibold">
                      {log.actor?.fullName || "Unknown"} {actionLabels[log.action] || log.action}
                    </div>
                    <div className="text-secondary small">{log.description}</div>
                    <div className="small mt-1">
                      <span className="badge text-bg-light border me-2">{log.targetType}</span>
                      <span className="text-secondary">#{log.targetId}</span>
                    </div>
                  </div>
                  <div className="small text-secondary text-end">
                    {new Date(log.createdAt).toLocaleString()}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default ActivityLog;
