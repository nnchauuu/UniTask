import { useState } from "react";

const statusOptions = ["PLANNING", "IN_PROGRESS", "COMPLETED", "CANCELLED"];
const statusLabels = {
  PLANNING: "Lên kế hoạch",
  IN_PROGRESS: "Đang thực hiện",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy"
};

function ProjectForm({ initialValues, submitLabel, loadingLabel, onSubmit }) {
  const [form, setForm] = useState(
    initialValues || {
      name: "",
      description: "",
      status: "PLANNING",
      startDate: "",
      endDate: ""
      ,allowCustomReviewers: false
    }
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value
    }));
  };

  const buildPayload = () => ({
    name: form.name,
    description: form.description,
    status: form.status,
    startDate: form.startDate || null,
    endDate: form.endDate || null
    ,allowCustomReviewers: Boolean(form.allowCustomReviewers)
  });

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      await onSubmit(buildPayload());
      if (!initialValues) {
        setForm({
          name: "",
          description: "",
          status: "PLANNING",
          startDate: "",
          endDate: ""
          ,allowCustomReviewers: false
        });
      }
    } catch (err) {
      const validation = err.response?.data?.data;
      const firstValidationError = validation && Object.values(validation)[0];
      setError(firstValidationError || err.response?.data?.message || "Không thể lưu dự án");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="d-grid gap-3" onSubmit={handleSubmit}>
      {error && <div className="alert alert-danger">{error}</div>}

      <div>
        <label className="form-label" htmlFor={initialValues ? "editProjectName" : "projectName"}>
          Tên
        </label>
        <input
          className="form-control"
          id={initialValues ? "editProjectName" : "projectName"}
          name="name"
          maxLength={150}
          value={form.name}
          onChange={handleChange}
          required
        />
      </div>

      <div>
        <label className="form-label" htmlFor={initialValues ? "editProjectDescription" : "projectDescription"}>
          Mô tả
        </label>
        <textarea
          className="form-control"
          id={initialValues ? "editProjectDescription" : "projectDescription"}
          name="description"
          maxLength={1000}
          rows={4}
          value={form.description}
          onChange={handleChange}
        />
      </div>

      <div className="row g-3">
        <div className="col-md-4">
          <label className="form-label" htmlFor={initialValues ? "editProjectStatus" : "projectStatus"}>
            Trạng thái
          </label>
          <select
            className="form-select"
            id={initialValues ? "editProjectStatus" : "projectStatus"}
            name="status"
            value={form.status}
            onChange={handleChange}
          >
            {statusOptions.map((status) => (
              <option key={status} value={status}>
                {statusLabels[status]}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor={initialValues ? "editProjectStartDate" : "projectStartDate"}>
            Ngày bắt đầu
          </label>
          <input
            className="form-control"
            id={initialValues ? "editProjectStartDate" : "projectStartDate"}
            name="startDate"
            type="date"
            value={form.startDate}
            onChange={handleChange}
          />
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor={initialValues ? "editProjectEndDate" : "projectEndDate"}>
            Ngày kết thúc
          </label>
          <input
            className="form-control"
            id={initialValues ? "editProjectEndDate" : "projectEndDate"}
            name="endDate"
            type="date"
            value={form.endDate}
            onChange={handleChange}
          />
        </div>
      </div>

      <label className="form-check d-flex align-items-center gap-2">
        <input className="form-check-input" type="checkbox" checked={Boolean(form.allowCustomReviewers)} onChange={(event) => setForm((current) => ({ ...current, allowCustomReviewers: event.target.checked }))} />
        <span>Cho phép chỉ định thành viên khác làm người duyệt</span>
      </label>

      <button className="btn btn-primary" type="submit" disabled={loading}>
        {loading ? loadingLabel : submitLabel}
      </button>
    </form>
  );
}

export default ProjectForm;
