import { useEffect, useState } from "react";
import * as contributionApi from "../api/contributionApi";

function ContributionDashboard({ projectId, refreshKey = 0 }) {
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadContributions = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await contributionApi.getProjectContributions(projectId);
      setContributions(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Cannot load contributions");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadContributions();
  }, [projectId, refreshKey]);

  return (
    <section className="bg-white border rounded p-4 shadow-sm mb-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <p className="text-uppercase text-primary fw-semibold small mb-1">Contribution</p>
          <h2 className="h5 fw-bold mb-0">Member contribution score</h2>
        </div>
        <button className="btn btn-outline-secondary btn-sm" onClick={loadContributions}>
          Refresh
        </button>
      </div>

      {loading && <div className="alert alert-info">Loading contribution scores...</div>}
      {error && <div className="alert alert-danger">{error}</div>}
      {!loading && !error && contributions.length === 0 && (
        <div className="text-secondary small border rounded p-4 text-center">No members found.</div>
      )}

      {!loading && !error && contributions.length > 0 && (
        <div className="table-responsive">
          <table className="table align-middle mb-0">
            <thead>
              <tr>
                <th>Rank</th>
                <th>Member</th>
                <th>Email</th>
                <th className="text-end">Score</th>
                <th className="text-end">Completed</th>
                <th className="text-end">Late</th>
                <th className="text-end">Overdue</th>
                <th className="text-end">Comments</th>
                <th className="text-end">Files</th>
                <th className="text-end">Meetings</th>
              </tr>
            </thead>
            <tbody>
              {contributions.map((member, index) => (
                <tr key={member.userId}>
                  <td>
                    <span className="badge text-bg-light border">#{index + 1}</span>
                  </td>
                  <td className="fw-semibold">{member.fullName}</td>
                  <td>{member.email}</td>
                  <td className="text-end fw-bold">{member.score}</td>
                  <td className="text-end">{member.completedTasks}</td>
                  <td className="text-end">{member.lateCompletedTasks}</td>
                  <td className="text-end">{member.overdueTasks}</td>
                  <td className="text-end">{member.commentsCount}</td>
                  <td className="text-end">{member.uploadedFilesCount}</td>
                  <td className="text-end">{member.joinedMeetingsCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="small text-secondary mt-3">
        Score: on-time completed task +10, late completed task +5, overdue unfinished task -5,
        comment +2, uploaded file +3, joined meeting +5.
      </div>
    </section>
  );
}

export default ContributionDashboard;
