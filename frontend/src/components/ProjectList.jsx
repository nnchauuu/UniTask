import { Link } from "react-router-dom";

const statusLabels = {
  PLANNING: "Lên kế hoạch",
  IN_PROGRESS: "Đang thực hiện",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy"
};

const statusClasses = {
  PLANNING: "text-bg-secondary",
  IN_PROGRESS: "text-bg-primary",
  COMPLETED: "text-bg-success",
  CANCELLED: "text-bg-danger"
};

function ProjectList({ projects }) {
  if (projects.length === 0) {
    return (
      <div className="border rounded p-4 text-center text-secondary">
        Chưa có dự án nào.
      </div>
    );
  }

  return (
    <div className="row g-3">
      {projects.map((project) => (
        <div className="col-md-6" key={project.id}>
          <div className="project-card bg-white border rounded p-3 h-100 shadow-sm">
            <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
              <h3 className="h6 fw-bold mb-0">{project.name}</h3>
              <span className={`badge ${statusClasses[project.status] || "text-bg-secondary"}`}>
                {statusLabels[project.status] || project.status}
              </span>
            </div>
            <p className="text-secondary small mb-3">
              {project.description || "Chưa có mô tả."}
            </p>
            <div className="d-flex justify-content-between align-items-center gap-2">
              <span className="small text-secondary">
                {project.startDate || "Chưa có ngày bắt đầu"} - {project.endDate || "Chưa có ngày kết thúc"}
              </span>
              <Link className="btn btn-outline-primary btn-sm" to={`/projects/${project.id}`}>
                Xem
              </Link>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default ProjectList;
