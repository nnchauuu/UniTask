import { useRef, useState } from "react";
import { buildTaskPayload } from "../utils/taskForm";

const statusOptions = ["TODO", "IN_PROGRESS", "REVIEW", "DONE"];
const priorityOptions = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const statusLabels = {
  TODO: "Cần làm",
  IN_PROGRESS: "Đang làm",
  REVIEW: "Đang duyệt",
  DONE: "Hoàn thành"
};
const priorityLabels = {
  LOW: "Thấp",
  MEDIUM: "Trung bình",
  HIGH: "Cao",
  URGENT: "Khẩn cấp"
};

const defaultTaskForm = {
  title: "",
  description: "",
  assignedToUserId: "",
  boardColumnId: null,
  parentTaskId: null,
  status: "TODO",
  priority: "MEDIUM",
  type: "DESIGN",
  workCategoryId: "",
  reviewRequired: true,
  startDate: "",
  dueDate: "",
  estimatedEffort: 0,
  actualEffort: "",
  labels: "",
  attachmentName: ""
};

function TaskForm({
  initialValues,
  members,
  parentTasks = [],
  taskTypes = [],
  onAddTaskType,
  canAssign,
  fixedStatus,
  variant = "default",
  submitLabel,
  loadingLabel,
  onSubmit
}) {
  const isApplePlanning = variant === "apple-planning";
  const [form, setForm] = useState(
    { ...defaultTaskForm, ...(initialValues || {}), ...(fixedStatus ? { status: fixedStatus } : {}) }
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [attachmentFile, setAttachmentFile] = useState(null);
  const attachmentInputRef = useRef(null);

  const handleChange = (event) => {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value
    }));
  };

  const handleFileChange = (event) => {
    const file = event.target.files?.[0];
    setAttachmentFile(file || null);
    setForm((current) => ({
      ...current,
      attachmentName: file?.name || ""
    }));
  };

  const handleAddTaskType = async () => {
    const label = window.prompt("Tên loại công việc mới")?.trim();
    if (!label) return;
    const value = await onAddTaskType?.(label);
    if (value) setForm((current) => ({ ...current, workCategoryId: value }));
  };

  const handleTypeChange = (event) => {
    if (event.target.value === "__ADD_TASK_TYPE__") {
      handleAddTaskType();
      return;
    }
    handleChange(event);
  };

  const handleCheckboxChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.checked }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      await onSubmit(buildTaskPayload(form, taskTypes, fixedStatus), attachmentFile);
      if (!initialValues?.id) {
        setForm({ ...defaultTaskForm });
        setAttachmentFile(null);
        if (attachmentInputRef.current) attachmentInputRef.current.value = "";
      }
    } catch (err) {
      const validation = err.response?.data?.data;
      const firstValidationError = validation && Object.values(validation)[0];
      setError(firstValidationError || err.response?.data?.message || err.message || "Không thể lưu công việc");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className={`d-grid gap-3 task-form ${isApplePlanning ? "task-form-apple" : ""}`} onSubmit={handleSubmit}>
      {error && <div className="alert alert-danger">{error}</div>}

      <div>
        <label className="form-label" htmlFor={initialValues ? "editTaskTitle" : "taskTitle"}>
          Tiêu đề
        </label>
        <input
          className="form-control"
          id={initialValues ? "editTaskTitle" : "taskTitle"}
          name="title"
          maxLength={200}
          value={form.title}
          onChange={handleChange}
          required
        />
      </div>

      <div>
        <label className="form-label" htmlFor={initialValues ? "editTaskDescription" : "taskDescription"}>
          Mô tả
        </label>
        <textarea
          className="form-control"
          id={initialValues ? "editTaskDescription" : "taskDescription"}
          name="description"
          maxLength={1500}
          rows={3}
          value={form.description}
          onChange={handleChange}
        />
      </div>

      <div className="row g-3">
        <div className="col-md-6">
          <label className="form-label" htmlFor={initialValues ? "editTaskAssignee" : "taskAssignee"}>
            Người được giao
          </label>
          <select
            className="form-select"
            id={initialValues ? "editTaskAssignee" : "taskAssignee"}
            name="assignedToUserId"
            value={form.assignedToUserId}
            onChange={handleChange}
            disabled={!canAssign}
          >
            <option value="">Chưa giao</option>
            {members.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.fullName} ({member.role})
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor={initialValues ? "editTaskDueDate" : "taskDueDate"}>
            Hạn hoàn thành
          </label>
          <input
            className="form-control"
            id={initialValues ? "editTaskDueDate" : "taskDueDate"}
            name="dueDate"
            type="date"
            value={form.dueDate}
            onChange={handleChange}
          />
        </div>
      </div>

      {!isApplePlanning && <>
        <div className="mb-3">
          <label className="form-label" htmlFor={initialValues ? "editTaskEstimate" : "taskEstimate"}>Khối lượng ước tính</label>
          <input className="form-control" id={initialValues ? "editTaskEstimate" : "taskEstimate"} name="estimatedEffort"
            type="number" min="0" step="0.25" value={form.estimatedEffort ?? 0} onChange={handleChange} />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor={initialValues ? "editTaskActual" : "taskActual"}>Khối lượng thực tế</label>
          <input className="form-control" id={initialValues ? "editTaskActual" : "taskActual"} name="actualEffort"
            type="number" min="0" step="0.25" value={form.actualEffort ?? ""} onChange={handleChange} placeholder="Nhập khi hoàn thành" />
        </div>
      </>}

      <label className="form-check d-flex align-items-center gap-2">
        <input className="form-check-input" type="checkbox" name="reviewRequired" checked={Boolean(form.reviewRequired)} onChange={handleCheckboxChange} />
        <span>Công việc cần gửi duyệt trước khi hoàn thành</span>
      </label>
      {form.reviewRequired && (
        <div className="form-text">
          Dùng nút “Gửi duyệt” trong chi tiết task hoặc kéo thẻ sang cột Chờ duyệt; task sẽ được khóa trạng thái cho tới khi người duyệt xử lý.
        </div>
      )}

      <div className="row g-3">
        {!isApplePlanning && <div className="col-md-6">
          <label className="form-label" htmlFor={initialValues ? "editTaskStatus" : "taskStatus"}>
            Trạng thái
          </label>
          <select
            className="form-select"
            id={initialValues ? "editTaskStatus" : "taskStatus"}
            name="status"
            value={fixedStatus || form.status}
            onChange={handleChange}
            disabled={Boolean(fixedStatus)}
          >
            {statusOptions.map((status) => (
              <option
                key={status}
                value={status}
                disabled={Boolean(form.reviewRequired)
                  && ["REVIEW", "DONE"].includes(status)
                  && status !== (fixedStatus || form.status)}
              >
                {statusLabels[status]}
              </option>
            ))}
          </select>
        </div>}
        <div className={isApplePlanning ? "col-12" : "col-md-6"}>
          <label className="form-label" htmlFor={initialValues ? "editTaskPriority" : "taskPriority"}>
            Mức ưu tiên
          </label>
          <select
            className="form-select"
            id={initialValues ? "editTaskPriority" : "taskPriority"}
            name="priority"
            value={form.priority}
            onChange={handleChange}
          >
            {priorityOptions.map((priority) => (
              <option key={priority} value={priority}>
                {priorityLabels[priority]}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-md-6">
          <label className="form-label" htmlFor={initialValues ? "editTaskParent" : "taskParent"}>Task cha</label>
          <select
            className="form-select"
            id={initialValues ? "editTaskParent" : "taskParent"}
            name="parentTaskId"
            value={form.parentTaskId || ""}
            onChange={handleChange}
          >
            <option value="">Task độc lập</option>
            {parentTasks.filter((item) => !item.parentTaskId && item.id !== initialValues?.id).map((item) => (
              <option key={item.id} value={item.id}>{item.title}</option>
            ))}
          </select>
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor={initialValues ? "editTaskType" : "taskType"}>
            Loại công việc
          </label>
          <select
            className="form-select"
            id={initialValues ? "editTaskType" : "taskType"}
            name="workCategoryId"
            value={form.workCategoryId || taskTypes.find((item) => item.active !== false)?.id || ""}
            onChange={handleTypeChange}
          >
            {taskTypes.length === 0 && <option value="">Thiết kế (mặc định)</option>}
            {taskTypes.map((taskType) => (
              <option key={taskType.id || taskType.value} value={taskType.id || taskType.value} disabled={taskType.active === false}>{taskType.label}{taskType.active === false ? " (Ngừng sử dụng)" : ""}</option>
            ))}
            {onAddTaskType && <option value="__ADD_TASK_TYPE__">+ Thêm loại công việc</option>}
          </select>
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor={initialValues ? "editTaskStartDate" : "taskStartDate"}>
            Ngày bắt đầu
          </label>
          <input
            className="form-control"
            id={initialValues ? "editTaskStartDate" : "taskStartDate"}
            name="startDate"
            type="date"
            value={form.startDate || ""}
            onChange={handleChange}
          />
        </div>
      </div>

      {!isApplePlanning && <div>
        <label className="form-label" htmlFor={initialValues ? "editTaskLabels" : "taskLabels"}>
          Nhãn
        </label>
        <input
          className="form-control"
          id={initialValues ? "editTaskLabels" : "taskLabels"}
          name="labels"
          maxLength={500}
          placeholder="UI, Backend, Bug..."
          value={form.labels || ""}
          onChange={handleChange}
        />
      </div>}

      <div>
        <label className="form-label" htmlFor={initialValues ? "editTaskAttachment" : "taskAttachment"}>
          File đính kèm
        </label>
        <input
          className="form-control"
          id={initialValues ? "editTaskAttachment" : "taskAttachment"}
          ref={attachmentInputRef}
          type="file"
          onChange={handleFileChange}
        />
        {form.attachmentName && <div className="form-text">{form.attachmentName}</div>}
      </div>

      <button className="btn btn-primary" type="submit" disabled={loading}>
        {loading ? loadingLabel : submitLabel}
      </button>
    </form>
  );
}

export default TaskForm;
