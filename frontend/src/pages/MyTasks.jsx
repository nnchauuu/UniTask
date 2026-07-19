import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  AlertCircle,
  ArrowUpDown,
  CalendarDays,
  Check,
  ChevronDown,
  ListFilter,
  RefreshCw
} from "lucide-react";
import * as taskApi from "../api/taskApi";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

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

const priorityOrder = {
  URGENT: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3
};

const formatDate = (value) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`))
    : "Chưa đặt";

const isToday = (value) => value && new Date(`${value}T00:00:00`).toDateString() === new Date().toDateString();

const isOverdue = (task) =>
  task.dueDate && task.status !== "DONE" && new Date(`${task.dueDate}T23:59:59`).getTime() < Date.now();

const getTaskRoute = (task) => `/projects/${task.projectId}?tab=tasks&taskId=${task.id}`;

function MyTasks() {
  const [tasks, setTasks] = useState([]);
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("DEFAULT");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [updatingTaskIds, setUpdatingTaskIds] = useState(() => new Set());
  const toolsRef = useRef(null);
  const { logout } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const handleApiError = (err, fallbackMessage) => {
    if (err.response?.status === 401) {
      logout();
      navigate("/login");
      return;
    }

    setError(err.response?.data?.message || fallbackMessage);
  };

  const loadTasks = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await taskApi.getMyTasks();
      setTasks(response.data);
    } catch (err) {
      handleApiError(err, "Không thể tải công việc của tôi");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTasks();
  }, []);

  useEffect(() => {
    const closeToolMenus = (event) => {
      if (toolsRef.current?.contains(event.target)) return;

      toolsRef.current?.querySelectorAll("details[open]").forEach((menu) => {
        menu.removeAttribute("open");
      });
    };

    document.addEventListener("pointerdown", closeToolMenus);
    return () => document.removeEventListener("pointerdown", closeToolMenus);
  }, []);

  const handleStatusChange = async (task, status) => {
    setError("");
    setSuccess("");
    setUpdatingTaskIds((current) => {
      const next = new Set(current);
      next.add(task.id);
      return next;
    });

    try {
      const response = await taskApi.updateTaskStatus(task.id, { status });
      setTasks((current) => current.map((item) => (item.id === task.id ? response.data : item)));
      showToast("Cập nhật task thành công");
    } catch (err) {
      handleApiError(err, "Không thể cập nhật trạng thái công việc");
    } finally {
      setUpdatingTaskIds((current) => {
        const next = new Set(current);
        next.delete(task.id);
        return next;
      });
    }
  };

  const doneCount = tasks.filter((task) => task.status === "DONE").length;
  const overdueCount = tasks.filter(isOverdue).length;
  const todayCount = tasks.filter((task) => task.status !== "DONE" && isToday(task.dueDate)).length;
  const filters = [
    { key: "ALL", label: "Tất cả", count: tasks.length },
    { key: "TODAY", label: "Hôm nay", count: todayCount },
    { key: "OVERDUE", label: "Quá hạn", count: overdueCount },
    { key: "DONE", label: "Hoàn thành", count: doneCount }
  ];

  const filteredTasks = tasks
    .filter((task) => {
      if (activeFilter === "TODAY") return task.status !== "DONE" && isToday(task.dueDate);
      if (activeFilter === "OVERDUE") return isOverdue(task);
      if (activeFilter === "DONE") return task.status === "DONE";
      return true;
    })
    .filter((task) => priorityFilter === "ALL" || task.priority === priorityFilter)
    .sort((left, right) => {
      if (sortBy === "PRIORITY") {
        return (priorityOrder[left.priority] ?? 99) - (priorityOrder[right.priority] ?? 99);
      }
      if (sortBy === "DUE_DATE") {
        if (!left.dueDate) return 1;
        if (!right.dueDate) return -1;
        return left.dueDate.localeCompare(right.dueDate);
      }
      if (sortBy === "TITLE") return left.title.localeCompare(right.title, "vi");
      return 0;
    });

  const openTaskProject = (task) => navigate(getTaskRoute(task));

  const handleRowClick = (event, task) => {
    if (event.target.closest("a, button, select, input, textarea, summary")) return;
    openTaskProject(task);
  };

  const handleRowKeyDown = (event, task) => {
    if (event.target !== event.currentTarget || (event.key !== "Enter" && event.key !== " ")) return;
    event.preventDefault();
    openTaskProject(task);
  };

  return (
    <main className="my-tasks-page">


      <div className="my-tasks-navigation">
        <div className="my-tasks-filters" role="tablist" aria-label="Lọc công việc theo thời hạn và trạng thái">
          {filters.map((filter) => (
            <button
              className={activeFilter === filter.key ? "active" : ""}
              type="button"
              role="tab"
              aria-selected={activeFilter === filter.key}
              aria-controls="my-tasks-list"
              key={filter.key}
              onClick={() => setActiveFilter(filter.key)}
            >
              <span>{filter.label}</span>
              <strong>{filter.count}</strong>
            </button>
          ))}
        </div>

        <div className="my-tasks-tools" ref={toolsRef} aria-label="Công cụ danh sách">
          <details className="my-tasks-tool-menu">
            <summary>
              <ListFilter aria-hidden="true" size={17} strokeWidth={1.8} />
              <span>Lọc</span>
            </summary>
            <div className="my-tasks-tool-popover">
              <p>Lọc theo ưu tiên</p>
              {["ALL", "LOW", "MEDIUM", "HIGH", "URGENT"].map((priority) => (
                <button
                  className={priorityFilter === priority ? "active" : ""}
                  key={priority}
                  onClick={() => setPriorityFilter(priority)}
                  type="button"
                >
                  <span>{priority === "ALL" ? "Tất cả mức ưu tiên" : priorityLabels[priority]}</span>
                  {priorityFilter === priority && <Check aria-hidden="true" size={15} />}
                </button>
              ))}
            </div>
          </details>

          <details className="my-tasks-tool-menu">
            <summary>
              <ArrowUpDown aria-hidden="true" size={17} strokeWidth={1.8} />
              <span>Sắp xếp</span>
              <ChevronDown aria-hidden="true" className="my-tasks-tool-chevron" size={13} />
            </summary>
            <div className="my-tasks-tool-popover my-tasks-sort-popover">
              <p>Sắp xếp công việc</p>
              {[
                ["DEFAULT", "Mặc định"],
                ["DUE_DATE", "Thời hạn gần nhất"],
                ["PRIORITY", "Ưu tiên cao nhất"],
                ["TITLE", "Tên công việc"]
              ].map(([value, label]) => (
                <button
                  className={sortBy === value ? "active" : ""}
                  key={value}
                  onClick={() => setSortBy(value)}
                  type="button"
                >
                  <span>{label}</span>
                  {sortBy === value && <Check aria-hidden="true" size={15} />}
                </button>
              ))}
            </div>
          </details>

          <button className="my-tasks-refresh" disabled={loading} onClick={loadTasks} type="button">
            <RefreshCw className={loading ? "is-spinning" : ""} size={17} strokeWidth={1.8} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="my-tasks-alert my-tasks-alert-danger" role="alert">
          <span><AlertCircle aria-hidden="true" size={16} />{error}</span>
          <button disabled={loading} onClick={loadTasks} type="button">Thử lại</button>
        </div>
      )}
      {success && <div className="my-tasks-alert my-tasks-alert-success" role="status">{success}</div>}

      {!loading && !error && tasks.length === 0 && (
        <div className="my-tasks-empty">
          <strong>Chưa có công việc nào</strong>
          <span>Hiện chưa có công việc nào được giao cho bạn.</span>
        </div>
      )}

      {(loading || tasks.length > 0) && (
        <section
          className="my-tasks-list"
          id="my-tasks-list"
          role="tabpanel"
          aria-busy={loading}
          aria-label="Danh sách công việc"
        >
          <div className="my-tasks-list-header" aria-hidden="true">
            <span>Công việc</span>
            <span>Ưu tiên</span>
            <span>Thời hạn</span>
            <span>Trạng thái</span>
          </div>

          {loading && (
            <div className="my-tasks-skeleton" role="status">
              <span className="visually-hidden">Đang tải công việc...</span>
              {Array.from({ length: 6 }, (_, index) => (
                <div className="my-task-skeleton-row" key={index} aria-hidden="true">
                  <i /><i /><i /><i />
                </div>
              ))}
            </div>
          )}

          {!loading && filteredTasks.length === 0 && (
            <div className="my-tasks-filter-empty">Không có công việc trong bộ lọc này.</div>
          )}

          {!loading && filteredTasks.map((task) => {
            const overdue = isOverdue(task);
            const dueToday = isToday(task.dueDate) && task.status !== "DONE";
            const isUpdating = updatingTaskIds.has(task.id);
            const projectRoute = getTaskRoute(task);

            return (
              <article
                className={`my-task-row ${overdue ? "is-overdue" : ""}`}
                key={task.id}
                role="link"
                tabIndex={0}
                aria-label={`Mở công việc ${task.title}`}
                onClick={(event) => handleRowClick(event, task)}
                onKeyDown={(event) => handleRowKeyDown(event, task)}
              >
                <div className="my-task-main">
                  <h2 title={task.title}>
                    <Link className="my-task-title-link" to={projectRoute}>{task.title}</Link>
                  </h2>
                  <div className="my-task-metadata">
                    <span className="my-task-code">UNI-{task.id}</span>
                    {task.workspaceName && <span title={task.workspaceName}>{task.workspaceName}</span>}
                    <span title={task.projectName || "Chưa có dự án"}>{task.projectName || "Chưa có dự án"}</span>
                  </div>
                </div>

                <em className={`my-task-priority priority-${task.priority?.toLowerCase()}`}>
                  {priorityLabels[task.priority] || task.priority || "Chưa đặt"}
                </em>

                <time className={`my-task-deadline ${overdue || dueToday ? "is-hot" : ""}`} dateTime={task.dueDate || undefined}>
                  <CalendarDays aria-hidden="true" size={16} strokeWidth={1.8} />
                  <span>
                    <strong title={overdue ? formatDate(task.dueDate) : undefined}>
                      {overdue ? "Quá hạn" : formatDate(task.dueDate)}
                    </strong>
                    {dueToday && <small>Hôm nay</small>}
                  </span>
                </time>

                <label
                  className={`my-task-status status-${task.status?.toLowerCase()}`}
                  onClick={(event) => event.stopPropagation()}
                >
                  <span className="visually-hidden">Trạng thái của {task.title}</span>
                  <select
                    aria-label={`Trạng thái của ${task.title}`}
                    disabled={isUpdating}
                    value={task.status}
                    onChange={(event) => handleStatusChange(task, event.target.value)}
                  >
                    {Object.entries(statusLabels).map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                </label>

              </article>
            );
          })}
        </section>
      )}
    </main>
  );
}

export default MyTasks;
