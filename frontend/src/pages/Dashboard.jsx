import {
  Activity,
  ArrowRight,
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  CircleAlert,
  ClipboardCheck,
  Clock3,
  ListTodo,
  MessageCircle,
  Plus,
  RefreshCw,
  UsersRound
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import * as notificationApi from "../api/notificationApi";
import * as taskApi from "../api/taskApi";
import * as workspaceApi from "../api/workspaceApi";
import { useAuth } from "../context/AuthContext";

const statusLabels = {
  TODO: "Cần làm",
  IN_PROGRESS: "Đang thực hiện",
  REVIEW: "Chờ duyệt",
  DONE: "Hoàn thành"
};

const priorityLabels = {
  LOW: "Ưu tiên thấp",
  MEDIUM: "Ưu tiên vừa",
  HIGH: "Ưu tiên cao",
  URGENT: "Khẩn cấp"
};

const formatDate = (value) => {
  if (!value) return "Chưa đặt";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" })
    .format(new Date(`${value}T00:00:00`));
};

const formatRelativeTime = (value) => {
  if (!value) return "Gần đây";
  const diffMinutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
  if (diffMinutes < 1) return "Vừa xong";
  if (diffMinutes < 60) return `${diffMinutes} phút trước`;
  if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)} giờ trước`;
  return `${Math.floor(diffMinutes / 1440)} ngày trước`;
};

function Dashboard() {
  const { user, refreshUser } = useAuth();
  const [workspaces, setWorkspaces] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = async () => {
    setLoading(true);
    setError("");

    try {
      await refreshUser();
      const [workspaceResponse, taskResponse, notificationResponse] = await Promise.all([
        workspaceApi.getWorkspaces(),
        taskApi.getMyTasks(),
        notificationApi.getNotifications()
      ]);
      setWorkspaces(workspaceResponse.data || []);
      setTasks(taskResponse.data || []);
      setNotifications(notificationResponse.data || []);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải dữ liệu tổng quan");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  const today = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  }, []);

  const upcomingTasks = useMemo(() => [...tasks]
    .filter((task) => task.status !== "DONE")
    .sort((a, b) => (a.dueDate || "9999-12-31").localeCompare(b.dueDate || "9999-12-31"))
    .slice(0, 5), [tasks]);

  const todayTasks = tasks.filter((task) => task.status !== "DONE" && task.dueDate === today).slice(0, 4);
  const pendingNotifications = notifications.filter((item) => !item.isRead).slice(0, 3);
  const recentNotifications = notifications.slice(0, 4);
  const displayName = user?.fullName?.split(" ").slice(-1)[0] || "bạn";

  const deadlineLabel = (dueDate) => {
    if (!dueDate) return "Chưa đặt";
    if (dueDate < today) return "Đã quá hạn";
    if (dueDate === today) return "Hôm nay";
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowValue = tomorrow.toISOString().slice(0, 10);
    return dueDate === tomorrowValue ? "Ngày mai" : formatDate(dueDate);
  };

  return (
    <main className="dashboard-page">


      {error && <div className="alert alert-danger">{error}</div>}
      {loading ? (
        <div className="dashboard-loading"><RefreshCw className="dashboard-spin" size={22} /> Đang tải tổng quan...</div>
      ) : (
        <div className="dashboard-grid">
          <section className="dashboard-panel dashboard-upcoming">
            <div className="dashboard-panel-header">
              <h2><ListTodo size={19} /> Công việc sắp tới</h2>
              <span>{upcomingTasks.length} công việc</span>
            </div>
            {upcomingTasks.length === 0 ? (
              <div className="dashboard-empty"><CheckCircle2 size={25} /> Không có công việc đang mở.</div>
            ) : (
              <div className="dashboard-task-table">
                <div className="dashboard-task-head">
                  <span>Công việc</span><span>Dự án</span><span>Trạng thái</span><span>Deadline</span>
                </div>
                {upcomingTasks.map((task) => (
                  <Link className="dashboard-task-row" key={task.id} to={`/projects/${task.projectId}`}>
                    <span className="dashboard-task-title"><i />{task.title}</span>
                    <span>{task.projectName || "Dự án"}</span>
                    <span><b className={`task-status task-status-${task.status?.toLowerCase()}`}>{statusLabels[task.status] || task.status}</b></span>
                    <span className={task.dueDate && task.dueDate <= today ? "task-deadline-hot" : ""}>{deadlineLabel(task.dueDate)}</span>
                  </Link>
                ))}
              </div>
            )}
            <Link className="dashboard-panel-link" to="/my-tasks">Xem tất cả công việc <ArrowRight size={15} /></Link>
          </section>

          <section className="dashboard-panel dashboard-today">
            <div className="dashboard-panel-header">
              <h2><CalendarDays size={19} /> Lịch hôm nay</h2>
              <span>{new Intl.DateTimeFormat("vi-VN", { weekday: "long", day: "numeric", month: "long" }).format(new Date())}</span>
            </div>
            <div className="dashboard-schedule">
              {todayTasks.length === 0 ? (
                <div className="dashboard-empty"><CalendarDays size={25} /> Hôm nay chưa có deadline.</div>
              ) : todayTasks.map((task, index) => (
                <Link className="dashboard-schedule-item" key={task.id} to={`/projects/${task.projectId}`}>
                  <time>{String(9 + index * 2).padStart(2, "0")}:00</time>
                  <span><strong>{task.title}</strong><small>{task.projectName || "Dự án"}</small></span>
                </Link>
              ))}
            </div>
            <Link className="dashboard-panel-link" to="/my-tasks">Xem lịch công việc <ArrowRight size={15} /></Link>
          </section>

          <section className="dashboard-panel dashboard-workspaces">
            <div className="dashboard-panel-header">
              <h2><UsersRound size={19} /> Không gian làm việc gần đây</h2>
            </div>
            <div className="dashboard-workspace-list">
              {workspaces.length === 0 ? (
                <div className="dashboard-empty"><BriefcaseBusiness size={25} /> Bạn chưa có workspace.</div>
              ) : workspaces.slice(0, 3).map((workspace, index) => (
                <Link className="dashboard-workspace-item" key={workspace.id} to={`/workspaces?workspace=${workspace.id}`}>
                  <span className={`workspace-initial workspace-initial-${index % 3}`}>{workspace.name?.slice(0, 2).toUpperCase()}</span>
                  <span><strong>{workspace.name}</strong><small>{workspace.description || "Không gian cộng tác của nhóm"}</small></span>
                  <ArrowRight size={16} />
                </Link>
              ))}
            </div>
            <Link className="dashboard-panel-link" to="/workspaces">Xem tất cả không gian <ArrowRight size={15} /></Link>
          </section>

          <section className="dashboard-panel dashboard-actions">
            <div className="dashboard-panel-header">
              <h2><Bell size={19} /> Cần bạn xử lý</h2>
              {pendingNotifications.length > 0 && <b>{pendingNotifications.length}</b>}
            </div>
            <div className="dashboard-action-list">
              {pendingNotifications.length === 0 ? (
                <div className="dashboard-empty"><ClipboardCheck size={25} /> Không có yêu cầu mới.</div>
              ) : pendingNotifications.map((item, index) => (
                <Link className="dashboard-action-item" key={item.id} to="/notifications">
                  <span>{index % 2 ? <MessageCircle size={16} /> : <CircleAlert size={16} />}</span>
                  <strong>{item.title}</strong>
                  <em>Mở</em>
                </Link>
              ))}
            </div>
            <Link className="dashboard-panel-link" to="/notifications">Xem tất cả <ArrowRight size={15} /></Link>
          </section>

          <section className="dashboard-panel dashboard-activity">
            <div className="dashboard-panel-header">
              <h2><Activity size={19} /> Hoạt động gần đây</h2>
            </div>
            <div className="dashboard-activity-list">
              {recentNotifications.length === 0 ? (
                <div className="dashboard-empty"><Clock3 size={25} /> Chưa có hoạt động gần đây.</div>
              ) : recentNotifications.map((item, index) => (
                <Link className="dashboard-activity-item" key={item.id} to="/notifications">
                  <span className={`activity-kind activity-kind-${index % 4}`}><CheckCircle2 size={15} /></span>
                  <strong>{item.title}</strong>
                  <time>{formatRelativeTime(item.createdAt)}</time>
                </Link>
              ))}
            </div>
            <Link className="dashboard-panel-link" to="/notifications">Xem tất cả hoạt động <ArrowRight size={15} /></Link>
          </section>
        </div>
      )}
    </main>
  );
}

export default Dashboard;
