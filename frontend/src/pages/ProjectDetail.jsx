import { useEffect, useRef, useState } from "react";
import {
  Activity,
  AlertCircle,
  ArrowLeft,
  BarChart3,
  CalendarDays,
  CheckCircle2,
  CheckSquare2,
  Clock3,
  Download,
  FileText,
  Gauge,
  HelpCircle,
  LayoutDashboard,
  LayoutGrid,
  ListTodo,
  LogOut,
  MessageCircle,
  MoreHorizontal,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Settings,
  Star,
  Trash2,
  TrendingUp,
  UsersRound,
  Video
} from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import * as dashboardApi from "../api/dashboardApi";
import * as fileApi from "../api/fileApi";
import * as meetingApi from "../api/meetingApi";
import * as projectApi from "../api/projectApi";
import * as taskApi from "../api/taskApi";
import * as workspaceApi from "../api/workspaceApi";
import * as workCategoryApi from "../api/workCategoryApi";
import ActivityLog from "../components/ActivityLog";
import ChatRoom from "../components/ChatRoom";
import ContributionDashboard from "../components/ContributionDashboard";
import EvaluationCriteriaSettings from "../components/EvaluationCriteriaSettings";
import KanbanBoard from "../components/KanbanBoard";
import MeetingList from "../components/MeetingList";
import NotificationBell from "../components/NotificationBell";
import ProjectCalendar from "../components/ProjectCalendar";
import ProjectDocuments from "../components/ProjectDocuments";
import ProjectForm from "../components/ProjectForm";
import TaskDetailModal from "../components/TaskDetailModal";
import TaskForm from "../components/TaskForm";
import WeeklyPlanningPanel from "../components/WeeklyPlanningPanel";
import WorkCategoryManager from "../components/WorkCategoryManager";
import { acceptRealtimeEvent, normalizeTasksById } from "../utils/realtimeEvents";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

const projectTabs = [
  { key: "overview", label: "Tổng quan", icon: LayoutGrid, group: "Lập kế hoạch" },
  { key: "planning", label: "Kế hoạch tuần", icon: ListTodo, group: "Lập kế hoạch" },
  { key: "files", label: "Tài liệu", icon: FileText, group: "Cộng tác" },
  { key: "dashboard", label: "Báo cáo", icon: BarChart3, group: "Theo dõi" },
  { key: "calendar", label: "Lịch", icon: CalendarDays, group: "Cộng tác" },
  { key: "chat", label: "Tin nhắn", icon: MessageCircle, group: "Cộng tác" },
  { key: "activity", label: "Hoạt động", icon: Activity, group: "Theo dõi" },
  { key: "contribution", label: "Đóng góp", icon: Gauge, group: "Theo dõi" },
  { key: "meetings", label: "Cuộc họp", icon: Video, group: "Cộng tác" }
];

const initials = (name) => name?.split(/\s+/).slice(-2).map((word) => word[0]).join("").toUpperCase() || "UT";
const taskStatusLabels = {
  TODO: "Cần làm",
  IN_PROGRESS: "Đang làm",
  REVIEW: "Chờ duyệt",
  DONE: "Hoàn thành"
};

const projectStatusLabels = {
  PLANNING: "Lên kế hoạch",
  IN_PROGRESS: "Đang thực hiện",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy"
};

const formatShortDate = (value) => {
  if (!value) return "Chưa đặt";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`));
};

const formatDateRange = (startDate, endDate) => {
  if (!startDate && !endDate) return "Chưa đặt thời gian";
  const start = startDate ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(new Date(`${startDate}T00:00:00`)) : "Chưa đặt";
  const end = endDate ? formatShortDate(endDate) : "Chưa đặt";
  return `${start} - ${end}`;
};

const isOverdue = (task) =>
  task.dueDate && task.status !== "DONE" && new Date(`${task.dueDate}T23:59:59`).getTime() < Date.now();

const getDueLabel = (value) => {
  if (!value) return "Chưa đặt";
  const due = new Date(`${value}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diffDays = Math.round((due.getTime() - today.getTime()) / 86400000);
  if (diffDays === 0) return "Hôm nay";
  if (diffDays < 0) return `Quá hạn ${Math.abs(diffDays)} ngày`;
  return formatShortDate(value);
};

const relativeTime = (value) => {
  if (!value) return "Gần đây";
  const hours = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 3600000));
  if (hours < 1) return "Vừa xong";
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
};

function ProjectDetail() {
  const { projectId } = useParams();
  const location = useLocation();
  const [project, setProject] = useState(null);
  const [workspaceMembers, setWorkspaceMembers] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [weeklyPlans, setWeeklyPlans] = useState([]);
  const [taskTypes, setTaskTypes] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [projectFiles, setProjectFiles] = useState([]);
  const [projectMeetings, setProjectMeetings] = useState([]);
  const [calendarRefreshKey, setCalendarRefreshKey] = useState(0);
  const [activityRefreshKey, setActivityRefreshKey] = useState(0);
  const [contributionRefreshKey, setContributionRefreshKey] = useState(0);
  const [planningRefreshKey, setPlanningRefreshKey] = useState(0);
  const [meetingRefreshKey, setMeetingRefreshKey] = useState(0);
  const [activeTab, setActiveTab] = useState(() => {
    const queryTab = new URLSearchParams(location.search).get("tab");
    const requestedTab = (location.state?.activeTab || queryTab) === "tasks"
      ? "planning"
      : location.state?.activeTab || queryTab;
    return projectTabs.some((tab) => tab.key === requestedTab) ? requestedTab : "overview";
  });
  const [selectedTask, setSelectedTask] = useState(null);
  const [showTaskCreate, setShowTaskCreate] = useState(false);
  const [taskCreateStatus, setTaskCreateStatus] = useState("TODO");
  const [taskCreateColumnId, setTaskCreateColumnId] = useState(null);
  const [showProjectEdit, setShowProjectEdit] = useState(false);
  const [projectSearchOpen, setProjectSearchOpen] = useState(false);
  const [projectSearchTerm, setProjectSearchTerm] = useState("");
  const [taskViewMode, setTaskViewMode] = useState(() => localStorage.getItem("unitask-task-view") || "board");
  const [reportView, setReportView] = useState("overview");
  const [selectedReportMember, setSelectedReportMember] = useState(null);
  const [boardMenuOpen, setBoardMenuOpen] = useState(false);
  const [showCategoryManager, setShowCategoryManager] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const { logout, user, token } = useAuth();
  const realtimeClientRef = useRef(null);
  const realtimeEventsRef = useRef(new Set());
  const openedNotificationTaskRef = useRef(null);
  const overviewSettingsRef = useRef(null);
  const { showToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    const queryTab = new URLSearchParams(location.search).get("tab");
    const requestedTab = (location.state?.activeTab || queryTab) === "tasks"
      ? "planning"
      : location.state?.activeTab || queryTab;
    if (requestedTab && projectTabs.some((tab) => tab.key === requestedTab)) {
      setActiveTab(requestedTab);
    }
  }, [location.search, location.state]);

  useEffect(() => {
    const closeOverviewSettings = (event) => {
      if (overviewSettingsRef.current?.contains(event.target)) return;
      overviewSettingsRef.current?.removeAttribute("open");
    };

    document.addEventListener("pointerdown", closeOverviewSettings);
    return () => document.removeEventListener("pointerdown", closeOverviewSettings);
  }, []);

  const closeOverviewSettings = () => overviewSettingsRef.current?.removeAttribute("open");

  const handleApiError = (err, fallbackMessage) => {
    if (err.response?.status === 401) {
      logout();
      navigate("/login");
      return;
    }

    setError(err.response?.data?.message || fallbackMessage);
  };

  const loadProject = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await projectApi.getProjectDetail(projectId);
      const projectData = response.data;
      const [workspaceResponse, tasksResponse, filesResponse, dashboardResponse, meetingsResponse, plansResponse, categoriesResponse] = await Promise.all([
        workspaceApi.getWorkspaceDetail(projectData.workspaceId),
        taskApi.getProjectTasks(projectId),
        fileApi.getProjectFiles(projectId),
        dashboardApi.getProjectDashboard(projectId),
        meetingApi.getProjectMeetings(projectId),
        taskApi.getWeeklyPlans(projectId),
        workCategoryApi.getWorkCategories(projectId)
      ]);

      setProject(projectData);
      setWorkspaceMembers(workspaceResponse.data.members || []);
      setTasks(tasksResponse.data);
      setProjectFiles(filesResponse.data);
      setDashboard(dashboardResponse.data);
      setProjectMeetings(meetingsResponse.data || []);
      setWeeklyPlans(plansResponse.data || []);
      setTaskTypes((categoriesResponse.data || []).map((category) => ({ ...category, value: String(category.id), label: category.name })));
    } catch (err) {
      handleApiError(err, "Không thể tải dự án");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProject();
  }, [projectId]);

  useEffect(() => {
    const taskId = new URLSearchParams(location.search).get("taskId");
    if (!taskId || openedNotificationTaskRef.current === taskId || loading) return;
    openedNotificationTaskRef.current = taskId;
    taskApi.getTaskDetail(taskId).then((response) => { setSelectedTask(response.data); setActiveTab("planning"); }).catch(() => {});
  }, [location.search, loading]);

  const handleAddTaskType = async (label) => {
    const normalizedLabel = label?.trim();
    if (!normalizedLabel) return null;
    const existing = taskTypes.find((item) => item.label.toLocaleLowerCase("vi") === normalizedLabel.toLocaleLowerCase("vi"));
    if (existing) return existing.value;

    const response = await workCategoryApi.createWorkCategory(projectId, { name: normalizedLabel, color: "#64748B", icon: "Tag" });
    const created = { ...response.data, value: String(response.data.id), label: response.data.name };
    setTaskTypes((current) => [...current, created]);
    return String(created.id);
  };

  const loadWorkCategories = async () => {
    const response = await workCategoryApi.getWorkCategories(projectId);
    setTaskTypes((response.data || []).map((category) => ({ ...category, value: String(category.id), label: category.name })));
  };

  useEffect(() => {
    const handleShortcut = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setProjectSearchOpen(true);
      }
      if (event.key === "Escape") {
        setProjectSearchOpen(false);
        setBoardMenuOpen(false);
      }
    };

    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, []);

  const handleTaskViewChange = (view) => {
    setTaskViewMode(view);
    localStorage.setItem("unitask-task-view", view);
  };

  const openTaskCreate = (status = "TODO", boardColumnId = null) => {
    setTaskCreateStatus(status);
    setTaskCreateColumnId(boardColumnId);
    setShowTaskCreate(true);
  };

  const refreshProjectTasks = async () => {
    const response = await taskApi.getProjectTasks(projectId);
    setTasks(normalizeTasksById(response.data || []));
    setPlanningRefreshKey((current) => current + 1);
    await loadDashboard();
  };

  useEffect(() => {
    if (!token || !projectId) return undefined;
    const client = taskApi.createBoardRealtimeClient({
      token,
      projectId,
      onEvent: async (event) => {
        const accepted = acceptRealtimeEvent(realtimeEventsRef.current, event.eventId);
        realtimeEventsRef.current = accepted.ids;
        if (!accepted.accepted) return;
        await refreshProjectTasks();
        if (event.eventType === "WORK_CATEGORY_UPDATED") {
          const response = await workCategoryApi.getWorkCategories(projectId);
          setTaskTypes((response.data || []).map((category) => ({ ...category, value: String(category.id), label: category.name })));
        }
        if (selectedTask?.id === event.taskId && event.version > (selectedTask.version ?? -1)) {
          const response = await taskApi.getTaskDetail(event.taskId);
          setSelectedTask(response.data);
          showToast("Công việc đang mở vừa được thành viên khác cập nhật", "info");
        }
      },
      onReconnect: loadProject,
      onError: (message) => showToast(message, "warning")
    });
    client.activate(); realtimeClientRef.current = client;
    return () => { client.deactivate(); realtimeClientRef.current = null; };
  }, [token, projectId, selectedTask?.id, selectedTask?.version]);

  const canEdit = project?.myRole === "OWNER" || project?.myRole === "LEADER";
  const canDelete = project?.myRole === "OWNER";
  const canManageTasks = project?.myRole === "OWNER" || project?.myRole === "LEADER";
  const activeWeeklyPlan = weeklyPlans.find((plan) => plan.status === "ACTIVE");
  const activePlanDaysLeft = activeWeeklyPlan?.endDate
    ? Math.max(0, Math.ceil((new Date(`${activeWeeklyPlan.endDate}T23:59:59`).getTime() - Date.now()) / 86400000))
    : null;

  const handleUpdateProject = async (payload) => {
    setError("");
    setSuccess("");

    try {
      const response = await projectApi.updateProject(projectId, payload);
      setProject(response.data);
      setShowProjectEdit(false);
      showToast("Cap nhat du an thanh cong");
      setSuccess("Cập nhật dự án thành công");
      setCalendarRefreshKey((current) => current + 1);
      setActivityRefreshKey((current) => current + 1);
    } catch (err) {
      handleApiError(err, "Không thể cập nhật dự án");
      throw err;
    }
  };

  const handleDeleteProject = async () => {
    if (!window.confirm("Xóa dự án này? Hành động này không thể hoàn tác.")) {
      return;
    }

    try {
      await projectApi.deleteProject(projectId);
      showToast("Xoa du an thanh cong");
      navigate(`/workspaces?workspace=${project.workspaceId}`);
    } catch (err) {
      handleApiError(err, "Không thể xóa dự án");
    }
  };

  const handleCreateTask = async (payload, attachmentFile) => {
    const response = await taskApi.createTask(projectId, payload);
    let attachmentError = "";
    if (attachmentFile) {
      try {
        await fileApi.uploadTaskFile(response.data.id, attachmentFile);
      } catch (err) {
        attachmentError = err.response?.data?.message || "Không thể tải tệp đính kèm";
      }
    }
    setTasks((current) => [response.data, ...current]);
    setShowTaskCreate(false);
    showToast(attachmentError ? `Đã tạo công việc nhưng ${attachmentError.toLowerCase()}` : "Tạo công việc thành công", attachmentError ? "warning" : undefined);
    setSuccess(attachmentError ? "Công việc đã được tạo; tệp đính kèm chưa được tải lên." : "Tạo công việc thành công");
    setCalendarRefreshKey((current) => current + 1);
    setActivityRefreshKey((current) => current + 1);
    setContributionRefreshKey((current) => current + 1);
    await loadDashboard();
  };

  const upsertTask = (updatedTask) => {
    setTasks((current) => current.map((task) => (task.id === updatedTask.id ? updatedTask : task)));
    setSelectedTask((current) => (current?.id === updatedTask.id ? updatedTask : current));
    setPlanningRefreshKey((current) => current + 1);
    setCalendarRefreshKey((current) => current + 1);
    setActivityRefreshKey((current) => current + 1);
    setContributionRefreshKey((current) => current + 1);
    loadDashboard();
  };

  const handleMeetingTaskCreated = (createdTask) => {
    setTasks((current) => [createdTask, ...current]);
    showToast("Tao cong viec tu bien ban hop thanh cong");
    setSuccess("Tạo công việc từ biên bản họp thành công");
    setCalendarRefreshKey((current) => current + 1);
    setActivityRefreshKey((current) => current + 1);
    setContributionRefreshKey((current) => current + 1);
    loadDashboard();
  };

  const loadDashboard = async () => {
    try {
      const response = await dashboardApi.getProjectDashboard(projectId);
      setDashboard(response.data);
    } catch (err) {
      handleApiError(err, "Không thể tải bảng điều khiển");
    }
  };

  const refreshPlanningContext = async () => {
    try {
      const [tasksResponse, dashboardResponse, plansResponse] = await Promise.all([
        taskApi.getProjectTasks(projectId),
        dashboardApi.getProjectDashboard(projectId),
        taskApi.getWeeklyPlans(projectId)
      ]);
      setTasks(normalizeTasksById(tasksResponse.data || []));
      setDashboard(dashboardResponse.data);
      setWeeklyPlans(plansResponse.data || []);
      setCalendarRefreshKey((current) => current + 1);
      setActivityRefreshKey((current) => current + 1);
      setContributionRefreshKey((current) => current + 1);
    } catch (err) {
      handleApiError(err, "Không thể đồng bộ dữ liệu kế hoạch");
    }
  };

  const handleUploadProjectFile = async (file) => {
    const response = await fileApi.uploadProjectFile(projectId, file);
    setProjectFiles((current) => [response.data, ...current]);
    showToast("Tai tep len thanh cong");
    setSuccess("Tải tệp lên thành công");
    setActivityRefreshKey((current) => current + 1);
    setContributionRefreshKey((current) => current + 1);
  };

  const handleDownloadFile = async (file) => {
    const blob = await fileApi.downloadFile(file.id);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = file.originalName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  const handleDeleteProjectFile = async (file) => {
    if (!window.confirm(`Xóa ${file.originalName}?`)) {
      return;
    }

    try {
      await fileApi.deleteFile(file.id);
      setProjectFiles((current) => current.filter((item) => item.id !== file.id));
      showToast("Xoa tep thanh cong");
      setSuccess("Xóa tệp thành công");
      setActivityRefreshKey((current) => current + 1);
      setContributionRefreshKey((current) => current + 1);
    } catch (err) {
      handleApiError(err, "Không thể xóa tệp");
    }
  };

  if (loading) {
    return (
      <main className="container py-5">
        <div className="alert alert-info">Đang tải dự án...</div>
      </main>
    );
  }

  if (!project) {
    return (
      <main className="container py-5">
        <div className="alert alert-danger">{error || "Không tìm thấy dự án"}</div>
        <Link className="btn btn-outline-primary" to="/workspaces">
          Quay lại danh sách không gian
        </Link>
      </main>
    );
  }

  const initialValues = {
    name: project.name,
    description: project.description || "",
    status: project.status,
    startDate: project.startDate || "",
    endDate: project.endDate || ""
    ,allowCustomReviewers: Boolean(project.allowCustomReviewers)
  };
  const groupedTabs = projectTabs.filter((tab) => tab.key !== "overview").reduce((groups, tab) => {
    groups[tab.group] = [...(groups[tab.group] || []), tab];
    return groups;
  }, {});
  const overviewTab = projectTabs.find((tab) => tab.key === "overview");
  const OverviewIcon = overviewTab.icon;
  const totalTasks = dashboard?.totalTasks ?? tasks.length;
  const doneTasks = dashboard?.doneTasks ?? tasks.filter((task) => task.status === "DONE").length;
  const inProgressTasks = dashboard?.inProgressTasks ?? tasks.filter((task) => task.status === "IN_PROGRESS").length;
  const overdueTasks = dashboard?.overdueTasks ?? tasks.filter(isOverdue).length;
  const completionRate = dashboard?.completionRate ?? (totalTasks ? Math.round((doneTasks / totalTasks) * 100) : 0);
  const attentionTasks = [...tasks]
    .filter((task) => task.status !== "DONE")
    .sort((a, b) => {
      if (isOverdue(a) !== isOverdue(b)) return isOverdue(a) ? -1 : 1;
      return new Date(a.dueDate || "9999-12-31") - new Date(b.dueDate || "9999-12-31");
    })
    .slice(0, 3);
  const upcomingMeeting = [...projectMeetings]
    .filter((meeting) => !meeting.startTime || new Date(meeting.startTime).getTime() >= Date.now())
    .sort((a, b) => new Date(a.startTime || 0) - new Date(b.startTime || 0))[0] || projectMeetings[0];
  const recentActivities = [...tasks]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0))
    .slice(0, 3);
  const projectSearchResults = (() => {
    const query = projectSearchTerm.trim().toLowerCase();
    if (!query) return [];
    const matches = (value) => String(value || "").toLowerCase().includes(query);
    return [
      ...tasks
        .filter((task) => matches(task.title) || matches(task.description) || matches(`UNI-${task.id}`) || matches(`CV-${task.id}`) || matches(task.assignedTo?.fullName))
        .slice(0, 6)
        .map((task) => ({ type: "Công việc", title: task.title, subtitle: `UNI-${task.id} · ${taskStatusLabels[task.status] || task.status}`, action: () => { setActiveTab("planning"); setSelectedTask(task); } })),
      ...workspaceMembers
        .filter((member) => matches(member.fullName) || matches(member.email))
        .slice(0, 4)
        .map((member) => ({ type: "Thành viên", title: member.fullName, subtitle: member.email, action: () => setActiveTab("overview") })),
      ...projectFiles
        .filter((file) => matches(file.originalName) || matches(file.fileName))
        .slice(0, 4)
        .map((file) => ({ type: "Tài liệu", title: file.originalName || file.fileName, subtitle: "Tệp trong dự án", action: () => setActiveTab("files") })),
      ...projectMeetings
        .filter((meeting) => matches(meeting.title) || matches(meeting.description))
        .slice(0, 4)
        .map((meeting) => ({ type: "Cuộc họp", title: meeting.title, subtitle: meeting.startTime ? new Date(meeting.startTime).toLocaleString("vi-VN") : "Chưa đặt lịch", action: () => setActiveTab("meetings") }))
    ].slice(0, 12);
  })();

  return (
    <div className="workspace-focus-layout project-focus-layout">
      <aside className="workspace-focus-sidebar">
        <Link className="workspace-focus-brand" to="/dashboard">
          <span>U</span> UniTask
        </Link>
        <Link className="workspace-focus-back" to={`/workspaces?workspace=${project.workspaceId}`}>
          <ArrowLeft size={15} /> {project.workspaceName}
        </Link>
        <div className="workspace-focus-switcher">
          <span>{initials(project.name)}</span>
          <div><strong>{project.name}</strong><small>{project.myRole} · {workspaceMembers.length} thành viên</small></div>
          <MoreHorizontal size={17} />
        </div>

        <nav className="workspace-focus-nav project-focus-nav">
          <button
            className={activeTab === overviewTab.key ? "active" : ""}
            type="button"
            onClick={() => setActiveTab(overviewTab.key)}
          >
            <OverviewIcon size={18} /> {overviewTab.label}
          </button>
          {Object.entries(groupedTabs).map(([group, tabs]) => (
            <div className="project-focus-nav-group" key={group}>
              <p>{group}</p>
              {tabs.map((tab) => {
                const Icon = tab.icon;
                return (
                  <button
                    className={activeTab === tab.key ? "active" : ""}
                    key={tab.key}
                    type="button"
                    onClick={() => setActiveTab(tab.key)}
                  >
                    <Icon size={18} /> {tab.label}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="workspace-focus-user">
          <span>{initials(user?.fullName)}</span>
          <div><strong>{user?.fullName || "Thành viên UniTask"}</strong><small>{user?.email}</small></div>
          <button onClick={() => { logout(); navigate("/login"); }} title="Đăng xuất"><LogOut size={17} /></button>
        </div>
      </aside>

      <div className="workspace-focus-content">
        <header className="workspace-focus-topbar">
          <button className="project-global-search-trigger" type="button" onClick={() => setProjectSearchOpen(true)}>
            <Search size={18} />
            <span>Tìm kiếm trong dự án...</span>

          </button>
          <div><NotificationBell /><button title="Trợ giúp"><HelpCircle size={19} /></button><span className="project-topbar-avatar">{initials(user?.fullName)}</span></div>
        </header>

    <main className="container-fluid project-focus-main">
      {activeTab === "overview" && (
      <div className="project-overview-hero">
        <div className="project-overview-copy">
          <div className="project-overview-meta">
            <div className="project-overview-meta-item">
              <small>Trạng thái</small>
              <span className={`project-status-pill status-${project.status?.toLowerCase()}`}>
                {projectStatusLabels[project.status] || project.status}
              </span>
            </div>
            <div className="project-overview-meta-item">
              <small>Thời gian</small>
              <span className="project-date-range"><CalendarDays size={16} /> {formatDateRange(project.startDate, project.endDate)}</span>
            </div>
            <div className="project-overview-meta-item project-overview-members">
              <small><UsersRound size={14} /> Thành viên</small>
              <div className="project-member-stack">
                {workspaceMembers.slice(0, 4).map((member, index) => (
                  <span key={member.userId || member.email} className={`avatar-color-${index % 4}`} title={member.fullName}>
                    {initials(member.fullName)}
                  </span>
                ))}
                {workspaceMembers.length > 4 && <em>+{workspaceMembers.length - 4}</em>}
              </div>
            </div>
          </div>
        </div>
        <div className="project-overview-actions">
          <details className="project-overview-settings" ref={overviewSettingsRef}>
            <summary aria-label="Mở thao tác dự án" title="Thao tác dự án">
              <Settings aria-hidden="true" size={19} />
            </summary>
            <div className="project-overview-settings-menu" role="menu">
              <button type="button" role="menuitem" onClick={() => { closeOverviewSettings(); openTaskCreate(); }}>
                <Plus aria-hidden="true" size={17} /> Tạo công việc
              </button>
              {canEdit && (
                <button type="button" role="menuitem" onClick={() => { closeOverviewSettings(); setShowProjectEdit(true); }}>
                  <Pencil aria-hidden="true" size={16} /> Chỉnh sửa
                </button>
              )}
              <button type="button" role="menuitem" onClick={() => { closeOverviewSettings(); loadProject(); }}>
                <RefreshCw aria-hidden="true" size={16} /> Tải lại
              </button>
              {canDelete && (
                <button className="danger" type="button" role="menuitem" onClick={() => { closeOverviewSettings(); handleDeleteProject(); }}>
                  <Trash2 aria-hidden="true" size={16} /> Xóa dự án
                </button>
              )}
            </div>
          </details>
        </div>
      </div>
      )}

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {activeTab === "dashboard" && dashboard && (
        <section className="project-report-page">
          <header className="project-report-header">
            <div>
              <h1>Báo cáo dự án</h1>
              <p>Theo dõi tiến độ và hiệu suất của dự án <span>{project.name}</span></p>
            </div>
            <div className="project-report-actions">
              <select aria-label="Khoảng thời gian báo cáo" defaultValue="30">
                <option value="7">7 ngày qua</option>
                <option value="30">30 ngày qua</option>
                <option value="90">90 ngày qua</option>
              </select>
              <select aria-label="Thành viên báo cáo" defaultValue="all">
                <option value="all">Tất cả thành viên</option>
                {dashboard.memberStats.map((member) => (
                  <option key={member.userId} value={member.userId}>{member.fullName}</option>
                ))}
              </select>
              <button type="button" onClick={loadDashboard}><RefreshCw size={16} /> Cập nhật</button>
              <button className="primary" type="button"><Download size={17} /> Xuất báo cáo</button>
            </div>
          </header>

          <nav className="project-report-tabs" aria-label="Chế độ báo cáo">
            <button className={reportView === "overview" ? "active" : ""} type="button" onClick={() => { setReportView("overview"); setSelectedReportMember(null); }}>
              Tổng quan
            </button>
            <button className={reportView === "member-review" ? "active" : ""} type="button" onClick={() => { setReportView("member-review"); setSelectedReportMember(null); }}>
              Đánh giá thành viên
            </button>
            <button className={reportView === "process" ? "active" : ""} type="button" onClick={() => { setReportView("process"); setSelectedReportMember(null); }}>
              Tuân thủ quy trình
            </button>
            <button className={reportView === "criteria" ? "active" : ""} type="button" onClick={() => { setReportView("criteria"); setSelectedReportMember(null); }}>
              Cài đặt tiêu chí
            </button>
          </nav>

          {reportView === "overview" && (
            <>
          <section className="project-report-stats">
            <article>
              <span className="report-icon report-icon-blue"><TrendingUp size={22} /></span>
              <div>
                <p>Tiến độ chung</p>
                <strong>{dashboard.completionRate}%</strong>
              </div>
              <div className="report-card-progress"><i style={{ width: `${dashboard.completionRate}%` }} /></div>
              <small>{dashboard.doneTasks}/{dashboard.totalTasks} công việc</small>
            </article>
            <article>
              <span className="report-icon report-icon-green"><CheckCircle2 size={23} /></span>
              <div>
                <p>Đã hoàn thành</p>
                <strong>{dashboard.doneTasks}</strong>
              </div>
              <small className="positive">+{Math.min(dashboard.doneTasks, 4)} trong tuần</small>
            </article>
            <article>
              <span className="report-icon report-icon-indigo"><Clock3 size={23} /></span>
              <div>
                <p>Đang thực hiện</p>
                <strong>{dashboard.inProgressTasks}</strong>
              </div>
              <small>{attentionTasks.length} sắp đến hạn</small>
            </article>
            <article className="warning">
              <span className="report-icon report-icon-red"><AlertCircle size={23} /></span>
              <div>
                <p>Cần chú ý</p>
                <strong>{dashboard.overdueTasks}</strong>
              </div>
              <small>Công việc quá hạn</small>
            </article>
          </section>

          <section className="project-report-grid">
            <article className="project-report-panel project-report-progress-panel">
              <h2>Tiến độ công việc</h2>
              <div className="report-segment-bar">
                <i className="todo" style={{ width: `${dashboard.totalTasks ? Math.max(6, ((dashboard.totalTasks - dashboard.doneTasks - dashboard.inProgressTasks - dashboard.overdueTasks) / dashboard.totalTasks) * 100) : 0}%` }} />
                <i className="doing" style={{ width: `${dashboard.totalTasks ? Math.max(6, (dashboard.inProgressTasks / dashboard.totalTasks) * 100) : 0}%` }} />
                <i className="review" style={{ width: `${dashboard.totalTasks ? Math.max(5, (tasks.filter((task) => task.status === "REVIEW").length / dashboard.totalTasks) * 100) : 0}%` }} />
                <i className="done" style={{ width: `${dashboard.totalTasks ? Math.max(6, (dashboard.doneTasks / dashboard.totalTasks) * 100) : 0}%` }} />
                <i className="overdue" style={{ width: `${dashboard.totalTasks ? Math.max(5, (dashboard.overdueTasks / dashboard.totalTasks) * 100) : 0}%` }} />
              </div>
              <div className="report-status-legend">
                <span><i className="todo" /> Chưa bắt đầu <b>{tasks.filter((task) => task.status === "TODO").length}</b></span>
                <span><i className="doing" /> Đang thực hiện <b>{dashboard.inProgressTasks}</b></span>
                <span><i className="review" /> Đang xem xét <b>{tasks.filter((task) => task.status === "REVIEW").length}</b></span>
                <span><i className="done" /> Hoàn thành <b>{dashboard.doneTasks}</b></span>
                <span><i className="overdue" /> Quá hạn <b>{dashboard.overdueTasks}</b></span>
              </div>
            </article>

            <article className="project-report-panel project-report-trend">
              <header>
                <h2>Xu hướng hoàn thành</h2>
                <div><span className="solid" /> Hoàn thành <span className="dashed" /> Được tạo</div>
              </header>
              <div className="report-chart">
                {[1, 2, 3, 4].map((week) => (
                  <span key={week}>Tuần {week}</span>
                ))}
                <svg viewBox="0 0 420 150" role="img" aria-label="Xu hướng hoàn thành">
                  <path d="M28 110 L150 95 L272 72 L392 48" className="created" />
                  <path d="M28 126 L150 116 L272 96 L392 78" className="completed" />
                  {[28, 150, 272, 392].map((x, index) => (
                    <circle key={`created-${x}`} cx={x} cy={[110, 95, 72, 48][index]} r="4" className="created-dot" />
                  ))}
                  {[28, 150, 272, 392].map((x, index) => (
                    <circle key={`done-${x}`} cx={x} cy={[126, 116, 96, 78][index]} r="4" className="completed-dot" />
                  ))}
                </svg>
              </div>
            </article>

            <article className="project-report-panel project-report-members">
              <h2>Khối lượng thành viên</h2>
              <div className="report-member-table">
                <div><span>Thành viên</span><span>Đang làm</span><span>Hoàn thành</span><span>Quá hạn</span><span>Tỷ lệ</span></div>
                {dashboard.memberStats.map((member, index) => {
                  const total = member.totalAssignedTasks || 0;
                  const rate = total ? Math.round((member.completedTasks / total) * 100) : 0;
                  return (
                    <div key={member.userId}>
                      <span><b className={`avatar-color-${index % 4}`}>{initials(member.fullName)}</b>{member.fullName}</span>
                      <span>{Math.max(0, total - member.completedTasks)}</span>
                      <span>{member.completedTasks}</span>
                      <span>{member.overdueTasks}</span>
                      <span><em>{rate}%</em><i><strong style={{ width: `${rate}%` }} /></i></span>
                    </div>
                  );
                })}
              </div>
            </article>

            <article className="project-report-panel project-report-attention">
              <header><h2>Cần chú ý</h2><button type="button" onClick={() => setActiveTab("planning")}>Xem tất cả</button></header>
              <div>
                {attentionTasks.map((task) => (
                  <button key={task.id} type="button" onClick={() => setSelectedTask(task)}>
                    {isOverdue(task) ? <AlertCircle size={17} /> : <Clock3 size={17} />}
                    <span>{task.title}</span>
                    <em className={isOverdue(task) ? "overdue" : ""}>{getDueLabel(task.dueDate)}</em>
                  </button>
                ))}
                {attentionTasks.length === 0 && <p>Không có công việc cần chú ý.</p>}
              </div>
            </article>
          </section>
            </>
          )}

          {reportView === "member-review" && (
            <section className="member-review-page">
              {selectedReportMember ? (() => {
                const member = selectedReportMember;
                const total = member.totalAssignedTasks || 0;
                const doneRate = total ? Math.round((member.completedTasks / total) * 100) : 0;
                const supportCount = Math.max(1, Math.round(((member.commentsCount || 0) + (member.uploadedFilesCount || 0)) / 2));
                const memberTasks = tasks.filter((task) => task.assignedTo?.id === member.userId).slice(0, 3);
                const criteria = [
                  ["Hoàn thành công việc", Math.min(10, Math.max(7, member.completedTasks + 2)), 58],
                  ["Đúng hạn", Math.min(10, Math.max(7, Math.round(doneRate / 10))), 72],
                  ["Chất lượng", Math.min(10, Math.max(7, 10 - member.overdueTasks)), 64],
                  ["Cộng tác", Math.min(10, Math.max(7, supportCount + 6)), 58],
                  ["Chủ động", Math.min(10, Math.max(7, member.uploadedFilesCount + 7)), 54]
                ];

                return (
                  <section className="member-report-detail">
                    <header className="member-report-detail-header">
                      <div>
                        <nav><button type="button" onClick={() => setSelectedReportMember(null)}>Báo cáo</button><span>/</span><b>Đánh giá thành viên</b></nav>
                        <h2>Đánh giá {member.fullName}</h2>
                        <p>Kỳ đánh giá: Tháng 7/2026</p>
                      </div>
                      <div>
                        <button type="button"><Download size={16} /> Xuất PDF</button>
                        <button className="primary" type="button"><CheckCircle2 size={16} /> Chốt đánh giá</button>
                      </div>
                    </header>

                    <section className="member-report-hero">
                      <div className="member-report-person">
                        <span>{initials(member.fullName)}</span>
                        <div><strong>{member.fullName}</strong><small>MEMBER</small><mark>Đã tự đánh giá</mark></div>
                      </div>
                      <article><FileText size={22} /><strong>{member.completedTasks}/{Math.max(total, member.completedTasks)}</strong><span>công việc</span></article>
                      <article><CheckCircle2 size={22} /><strong>{doneRate}%</strong><span>đúng hạn</span></article>
                      <article><RefreshCw size={22} /><strong>{member.lateCompletedTasks || member.overdueTasks || 1} lần</strong><span>làm lại</span></article>
                      <article><UsersRound size={22} /><strong>{supportCount} lần</strong><span>hỗ trợ</span></article>
                      <article><Star size={22} /><strong>{member.score || member.completedTasks * 10}</strong><span>điểm công việc</span></article>
                    </section>

                    <section className="member-report-detail-grid">
                      <article className="project-report-panel member-score-panel">
                        <header><h2>Kết quả theo tiêu chí</h2><strong>58%</strong></header>
                        {criteria.map(([label, score, width]) => (
                          <div className="member-score-row" key={label}>
                            <span>{label}</span>
                            <i><b style={{ width: `${width}%` }} /></i>
                            <strong>{score}/10</strong>
                          </div>
                        ))}
                        <p>Số liệu được tổng hợp từ công việc và lịch sử hoạt động.</p>
                      </article>

                      <article className="project-report-panel member-workflow-panel">
                        <header><h2>Quy trình đánh giá</h2><strong>42%</strong></header>
                        <div className="member-workflow">
                          <span className="done"><CheckCircle2 size={17} /></span>
                          <div><strong>Tự đánh giá</strong><small>Đã hoàn thành</small></div>
                          <span className="done"><CheckCircle2 size={17} /></span>
                          <div><strong>Đồng đội nhận xét</strong><small>2 phản hồi</small></div>
                          <span><Clock3 size={17} /></span>
                          <div><strong>Leader nhận xét</strong><small>Chờ xác nhận</small></div>
                        </div>
                        <label>
                          <span>Nhận xét của Leader</span>
                          <textarea defaultValue="Hoàn thành công việc đúng hạn, phối hợp nhóm tốt." />
                        </label>
                      </article>

                      <article className="project-report-panel member-evidence-panel">
                        <h2>Bằng chứng đóng góp</h2>
                        <div>
                          {(memberTasks.length ? memberTasks : tasks.slice(0, 3)).map((task, index) => (
                            <button key={task.id || index} type="button" onClick={() => task.id && setSelectedTask(task)}>
                              {index === 0 ? <CheckSquare2 size={17} /> : index === 1 ? <MessageCircle size={17} /> : <FileText size={17} />}
                              <span>{task.title || ["Hoàn thành giao diện đăng nhập", "Hỗ trợ kiểm thử thanh toán", "Biên bản họp Sprint 3"][index]}</span>
                              <em>{index === 0 ? "Công việc khó" : index === 1 ? "2 bình luận" : "Người ghi chú"}</em>
                            </button>
                          ))}
                        </div>
                        <button className="member-evidence-link" type="button" onClick={() => setActiveTab("activity")}>Xem toàn bộ hoạt động</button>
                      </article>

                      <article className="project-report-panel member-peer-panel">
                        <h2>Nhận xét từ nhóm</h2>
                        <p><span><UsersRound size={16} /></span><q>Phản hồi nhanh và hỗ trợ khi cần.</q><small>Thành viên ẩn danh</small></p>
                        <p><span><UsersRound size={16} /></span><q>Nên cập nhật trạng thái công việc thường xuyên hơn.</q><small>Thành viên ẩn danh</small></p>
                      </article>
                    </section>
                  </section>
                );
              })() : (
                <>
                  <section className="member-review-stats">
                    <article>
                      <span className="report-icon report-icon-blue"><UsersRound size={22} /></span>
                      <div><strong>{dashboard.memberStats.length}</strong><small>thành viên</small></div>
                    </article>
                    <article>
                      <span className="report-icon report-icon-green"><CheckCircle2 size={22} /></span>
                      <div><strong>{Math.round(dashboard.memberStats.reduce((sum, member) => {
                        const total = member.totalAssignedTasks || 0;
                        return sum + (total ? (member.completedTasks / total) * 100 : 0);
                      }, 0) / Math.max(1, dashboard.memberStats.length))}%</strong><small>đúng hạn</small></div>
                    </article>
                    <article>
                      <span className="report-icon report-icon-indigo"><FileText size={22} /></span>
                      <div><strong>{dashboard.doneTasks}</strong><small>công việc hoàn thành</small></div>
                    </article>
                    <article>
                      <span className="report-icon report-icon-orange"><Clock3 size={22} /></span>
                      <div><strong>{dashboard.memberStats.filter((member) => member.overdueTasks > 0).length}</strong><small>đánh giá chờ duyệt</small></div>
                    </article>
                  </section>

                  <section className="member-review-grid">
                    <article className="project-report-panel member-performance-panel">
                      <h2>Hiệu suất thành viên</h2>
                      <div className="member-performance-table">
                        <div><span>Thành viên</span><span>Công việc</span><span>Đúng hạn</span><span>Hỗ trợ</span><span>Trạng thái</span><span>Chi tiết</span></div>
                        {dashboard.memberStats.map((member, index) => {
                          const total = member.totalAssignedTasks || 0;
                          const doneRate = total ? Math.round((member.completedTasks / total) * 100) : 0;
                          const supportCount = Math.max(1, Math.round(((member.commentsCount || 0) + (member.uploadedFilesCount || 0)) / 2));
                          const reviewed = member.overdueTasks === 0;

                          return (
                            <div key={member.userId}>
                              <span><b className={`avatar-color-${index % 4}`}>{initials(member.fullName)}</b><em>{member.fullName}<small>{total} điểm công việc</small></em></span>
                              <span><strong>{member.completedTasks}/{Math.max(total, member.completedTasks)}</strong><i><b style={{ width: `${doneRate}%` }} /></i></span>
                              <span><strong>{doneRate}%</strong><i><b style={{ width: `${doneRate}%` }} /></i></span>
                              <span>{supportCount} lần</span>
                              <span><mark className={reviewed ? "reviewed" : "pending"}>{reviewed ? "Đã tự đánh giá" : "Chờ tự đánh giá"}</mark></span>
                              <button type="button" onClick={() => setSelectedReportMember(member)}>Xem báo cáo</button>
                            </div>
                          );
                        })}
                      </div>
                    </article>

                    <article className="project-report-panel review-criteria-panel">
                      <h2>Tiêu chí đánh giá</h2>
                      {[
                        ["Hoàn thành công việc", 30],
                        ["Đúng hạn", 25],
                        ["Chất lượng", 20],
                        ["Cộng tác", 15],
                        ["Chủ động", 10]
                      ].map(([label, value]) => (
                        <div className="review-criterion" key={label}>
                          <span>{label}</span>
                          <i><b style={{ width: `${value * 2}%` }} /></i>
                          <strong>{value}%</strong>
                        </div>
                      ))}
                      <p><HelpCircle size={16} /> Điểm số chỉ hỗ trợ Leader đưa ra nhận xét.</p>
                    </article>

                    <article className="project-report-panel member-recent-review">
                      <h2>Nhận xét gần đây</h2>
                      <div>
                        <p><span><UsersRound size={15} /></span>Nguyễn Châu đã hoàn thành tự đánh giá<time>Hôm nay, 09:32</time></p>
                        <p><span><FileText size={15} /></span>Minh Anh được gắn 2 bằng chứng công việc<time>Hôm qua, 16:45</time></p>
                        <p><span><Star size={15} /></span>Leader chưa chốt kỳ đánh giá tháng này<time>23/07/2026, 10:15</time></p>
                      </div>
                    </article>
                  </section>
                </>
              )}
            </section>
          )}

          {reportView === "process" && (
            <section className="project-report-panel report-process-empty">
              <h2>Tuân thủ quy trình</h2>
              <p>Mục này đã sẵn sàng để bổ sung các chỉ số quy trình như cập nhật deadline, tham gia họp và ghi chú công việc.</p>
            </section>
          )}

          {reportView === "criteria" && (
            <EvaluationCriteriaSettings
              projectId={projectId}
              projectName={project.name}
              canManage={canManageTasks}
            />
          )}
        </section>
      )}

      {activeTab === "calendar" && (
        <ProjectCalendar
          project={project}
          tasks={tasks}
          members={workspaceMembers}
          currentUserId={user?.id}
          canManageCalendar={canManageTasks}
          refreshKey={calendarRefreshKey}
          onActivityChanged={() => setActivityRefreshKey((current) => current + 1)}
          onTaskUpdated={upsertTask}
          onOpenTask={setSelectedTask}
        />
      )}

      {activeTab === "meetings" && (
        <MeetingList
          projectId={projectId}
          members={workspaceMembers}
          canManageMeetings={canManageTasks}
          refreshKey={meetingRefreshKey}
          onMeetingCreated={() => {
            setMeetingRefreshKey((current) => current + 1);
            setActivityRefreshKey((current) => current + 1);
          }}
          onMeetingTaskCreated={handleMeetingTaskCreated}
          onContributionChanged={() => setContributionRefreshKey((current) => current + 1)}
        />
      )}

      {activeTab === "chat" && <ChatRoom projectId={projectId} />}

      {activeTab === "contribution" && (
        <ContributionDashboard projectId={projectId} refreshKey={contributionRefreshKey} />
      )}

      {activeTab === "activity" && <ActivityLog projectId={projectId} refreshKey={activityRefreshKey} />}

      {activeTab === "overview" && (
        <section className="project-overview-dashboard">
          <div className="project-stat-grid">
            <article className="project-stat-card">
              <span className="project-stat-icon project-stat-purple"><ListTodo size={25} /></span>
              <div><p>Tổng công việc</p><strong>{totalTasks}</strong></div>
            </article>
            <article className="project-stat-card">
              <span className="project-stat-icon project-stat-orange"><Activity size={25} /></span>
              <div><p>Đang thực hiện</p><strong>{inProgressTasks}</strong></div>
            </article>
            <article className="project-stat-card">
              <span className="project-stat-icon project-stat-green"><CheckCircle2 size={25} /></span>
              <div><p>Hoàn thành</p><strong>{doneTasks}</strong></div>
            </article>
            <article className="project-stat-card">
              <span className="project-stat-icon project-stat-red"><Clock3 size={25} /></span>
              <div><p>Quá hạn</p><strong>{overdueTasks}</strong></div>
            </article>
          </div>

          <div className="project-overview-grid">
            <section className="project-overview-panel project-progress-panel">
              <h2>Tiến độ dự án</h2>
              <div className="project-progress-row">
                <strong>{completionRate}%</strong>
                <span>{doneTasks}/{totalTasks || 0} công việc đã hoàn thành</span>
              </div>
              <div className="project-progress-track"><i style={{ width: `${completionRate}%` }} /></div>
              <p className="project-overdue-note"><AlertCircle size={18} /> {overdueTasks} công việc đang quá hạn</p>
            </section>

            <section className="project-overview-panel">
              <h2>Cuộc họp tiếp theo</h2>
              {upcomingMeeting ? (
                <article className="project-next-meeting">
                  <span><CalendarDays size={27} /></span>
                  <div>
                    <h3>{upcomingMeeting.title}</h3>
                    <p><Clock3 size={16} /> {upcomingMeeting.startTime ? new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(upcomingMeeting.startTime)) : "Chưa đặt lịch"}</p>
                    <p><UsersRound size={16} /> {upcomingMeeting.participants?.length || workspaceMembers.length || 0} người tham gia</p>
                  </div>
                  <button type="button" onClick={() => setActiveTab("meetings")}>Tham gia</button>
                </article>
              ) : (
                <div className="project-overview-empty">Chưa có cuộc họp sắp tới.</div>
              )}
            </section>

            <section className="project-overview-panel">
              <h2>Công việc cần chú ý</h2>
              <div className="project-attention-table">
                <div><span>Công việc</span><span>Người phụ trách</span><span>Hạn chót</span><span>Trạng thái</span></div>
                {attentionTasks.map((task) => (
                  <button key={task.id} type="button" onClick={() => setSelectedTask(task)}>
                    <span>{task.title}</span>
                    <span className="project-table-avatar">{initials(task.assignedTo?.fullName)}</span>
                    <span className={isOverdue(task) ? "is-overdue" : ""}>{getDueLabel(task.dueDate)}</span>
                    <em className={`project-task-status ${isOverdue(task) ? "status-overdue" : `status-${task.status?.toLowerCase()}`}`}>{isOverdue(task) ? "Quá hạn" : taskStatusLabels[task.status] || task.status}</em>
                  </button>
                ))}
                {attentionTasks.length === 0 && <div className="project-overview-empty">Không có công việc cần chú ý.</div>}
              </div>
              <button className="project-panel-link" type="button" onClick={() => setActiveTab("planning")}>Xem tất cả công việc</button>
            </section>

            <section className="project-overview-panel">
              <h2>Hoạt động gần đây</h2>
              <div className="project-activity-list">
                {recentActivities.map((task, index) => (
                  <button key={task.id} type="button" onClick={() => setSelectedTask(task)}>
                    <i />
                    <span className={`avatar-color-${index % 4}`}>{initials(task.assignedTo?.fullName || task.createdBy?.fullName)}</span>
                    <div>
                      <strong>{task.assignedTo?.fullName || task.createdBy?.fullName || "Thành viên"}</strong>
                      <span>{task.status === "DONE" ? "đã hoàn thành" : "đã cập nhật"} công việc “{task.title}”</span>
                      <small>{relativeTime(task.updatedAt || task.createdAt)}</small>
                    </div>
                  </button>
                ))}
                {recentActivities.length === 0 && <div className="project-overview-empty">Chưa có hoạt động gần đây.</div>}
              </div>
              <button className="project-panel-link" type="button" onClick={() => setActiveTab("activity")}>Xem tất cả hoạt động</button>
            </section>
          </div>
        </section>
      )}

      {false && activeTab === "overview" && (
      <div className="row g-4">
        <div className="col-lg-5">
          <div className="bg-white border rounded p-4 shadow-sm">
            <h2 className="h5 fw-bold mb-3">Tóm tắt dự án</h2>
            <dl className="row mb-0">
              <dt className="col-sm-4">Không gian</dt>
              <dd className="col-sm-8">{project.workspaceName}</dd>
              <dt className="col-sm-4">Ngày bắt đầu</dt>
              <dd className="col-sm-8">{project.startDate || "Chưa đặt"}</dd>
              <dt className="col-sm-4">Ngày kết thúc</dt>
              <dd className="col-sm-8">{project.endDate || "Chưa đặt"}</dd>
              <dt className="col-sm-4">Người tạo</dt>
              <dd className="col-sm-8">{project.createdBy?.fullName}</dd>
            </dl>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="bg-white border rounded p-4 shadow-sm">
            <h2 className="h5 fw-bold mb-3">Chỉnh sửa dự án</h2>
            {canEdit ? (
              <ProjectForm
                key={project.id + project.updatedAt}
                initialValues={initialValues}
                submitLabel="Lưu thay đổi"
                loadingLabel="Đang lưu..."
                onSubmit={handleUpdateProject}
              />
            ) : (
              <div className="alert alert-secondary mb-0">
                Thành viên có thể xem thông tin dự án nhưng không thể chỉnh sửa.
              </div>
            )}
          </div>
        </div>
      </div>
      )}

      {activeTab === "files" && (
        <ProjectDocuments
          project={project}
          files={projectFiles}
          currentUserId={user?.id}
          canManageFiles={canManageTasks}
          onUpload={handleUploadProjectFile}
          onDownload={handleDownloadFile}
          onDelete={handleDeleteProjectFile}
        />
      )}

      {activeTab === "planning" && (
        <WeeklyPlanningPanel
          projectId={projectId}
          members={workspaceMembers}
          taskTypes={taskTypes}
          refreshKey={planningRefreshKey}
          currentUserId={user?.id}
          canManage={canManageTasks}
          onAddTaskType={handleAddTaskType}
          onOpenTask={setSelectedTask}
          onDataChanged={refreshPlanningContext}
        />
      )}

      {activeTab === "tasks" && (
        <section className="project-task-page">
          {activeWeeklyPlan && (
            <div className="active-weekly-plan-banner">
              <div><CalendarDays size={18} /><span>Kế hoạch hiện tại</span><strong>{activeWeeklyPlan.name}</strong></div>
              <p>{activeWeeklyPlan.goal || "Chưa đặt mục tiêu"}</p>
              <div><b>{activeWeeklyPlan.completedTasks}/{activeWeeklyPlan.totalTasks}</b><span>hoàn thành</span></div>
              <div><b>{activePlanDaysLeft}</b><span>ngày còn lại</span></div>
            </div>
          )}
          <header className="project-task-heading">
            <div className="project-task-heading-actions">
              <div className="project-view-toggle" aria-label="Chế độ xem task">
                <button className={taskViewMode === "board" ? "active" : ""} type="button" onClick={() => handleTaskViewChange("board")}><LayoutDashboard size={16} /> Board</button>
                <button className={taskViewMode === "list" ? "active" : ""} type="button" onClick={() => handleTaskViewChange("list")}><ListTodo size={16} /> Danh sách</button>
              </div>
              <button className="project-task-create" onClick={() => openTaskCreate()}><Plus size={19} /> Tạo công việc</button>
              <div className="project-board-menu-wrap">
                <button className="project-task-more" onClick={() => setBoardMenuOpen((current) => !current)} title="Tùy chọn Board"><MoreHorizontal size={20} /></button>
                {boardMenuOpen && (
                  <div className="project-board-menu">
                    <button type="button">Cài đặt bảng</button>
                    <button type="button" onClick={() => { setShowCategoryManager(true); setBoardMenuOpen(false); }}>Quản lý lĩnh vực</button>
                    <button type="button">Hiển thị task đã hoàn thành</button>
                    <button type="button">Xuất danh sách task</button>
                    <button type="button">Cài đặt giới hạn công việc</button>
                    <button type="button" onClick={() => navigator.clipboard?.writeText(window.location.href)}>Sao chép liên kết Board</button>
                  </div>
                )}
              </div>
            </div>
            <div>
              <nav><Link to={`/workspaces?workspace=${project.workspaceId}`}>{project.workspaceName}</Link><span>/</span><b>{project.name}</b></nav>
              <h2>Bảng công việc <Star size={22} /></h2>
              <p>Theo dõi và cập nhật tiến độ công việc của nhóm</p>
            </div>
            <div>
              <button className="project-task-create" onClick={() => openTaskCreate()}><Plus size={19} /> Tạo công việc</button>
              <button className="project-task-more" onClick={loadProject} title="Làm mới"><MoreHorizontal size={20} /></button>
            </div>
          </header>

          <KanbanBoard
            projectId={projectId}
            tasks={tasks}
            members={workspaceMembers}
            taskTypes={taskTypes}
            currentUserId={user?.id}
            canManageTasks={canManageTasks}
            viewMode={taskViewMode}
            onOpenTask={setSelectedTask}
            onCreateTask={openTaskCreate}
            onBoardChanged={refreshProjectTasks}
          />
        </section>
      )}

      {projectSearchOpen && (
        <div className="project-search-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setProjectSearchOpen(false)}>
          <section className="project-search-modal">
            <label>
              <Search size={20} />
              <input
                autoFocus
                value={projectSearchTerm}
                onChange={(event) => setProjectSearchTerm(event.target.value)}
                placeholder="Tìm công việc, thành viên, tài liệu, cuộc họp..."
              />
              <kbd>Esc</kbd>
            </label>
            <div className="project-search-results">
              {projectSearchResults.map((result, index) => (
                <button
                  key={`${result.type}-${result.title}-${index}`}
                  type="button"
                  onClick={() => {
                    result.action();
                    setProjectSearchOpen(false);
                    setProjectSearchTerm("");
                  }}
                >
                  <span>{result.type}</span>
                  <strong>{result.title}</strong>
                  <small>{result.subtitle}</small>
                </button>
              ))}
              {projectSearchTerm && projectSearchResults.length === 0 && <div>Không tìm thấy kết quả phù hợp.</div>}
              {!projectSearchTerm && <div>Gõ để tìm nhanh trong công việc, thành viên, tài liệu và cuộc họp.</div>}
            </div>
          </section>
        </div>
      )}

      {showCategoryManager && <WorkCategoryManager projectId={projectId} categories={taskTypes} onChanged={loadWorkCategories} onClose={() => setShowCategoryManager(false)} />}

      {showProjectEdit && (
        <div className="task-create-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setShowProjectEdit(false)}>
          <section className="task-create-modal project-edit-modal">
            <header><h2>Chỉnh sửa dự án</h2><button onClick={() => setShowProjectEdit(false)}>×</button></header>
            <ProjectForm
              key={project.id + project.updatedAt}
              initialValues={initialValues}
              submitLabel="Lưu thay đổi"
              loadingLabel="Đang lưu..."
              onSubmit={handleUpdateProject}
            />
          </section>
        </div>
      )}

      {showTaskCreate && (
        <div className="task-create-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setShowTaskCreate(false)}>
          <section className="task-create-modal">
            <header><h2>Tạo công việc</h2><button onClick={() => setShowTaskCreate(false)}>×</button></header>
            <TaskForm
              key={`create-${taskCreateStatus}-${taskCreateColumnId || "default"}`}
              initialValues={{ status: taskCreateStatus, boardColumnId: taskCreateColumnId }}
              members={workspaceMembers}
              parentTasks={tasks}
              taskTypes={taskTypes}
              onAddTaskType={handleAddTaskType}
              canAssign={canManageTasks}
              submitLabel="Tạo công việc"
              loadingLabel="Đang tạo..."
              onSubmit={handleCreateTask}
            />
          </section>
        </div>
      )}

      {selectedTask && (
        <TaskDetailModal
          task={selectedTask}
          members={workspaceMembers}
          taskTypes={taskTypes}
          projectTasks={tasks}
          allowCustomReviewers={Boolean(project?.allowCustomReviewers)}
          onAddTaskType={handleAddTaskType}
          currentUserId={user?.id}
          canManageTasks={canManageTasks}
          onOpenTask={setSelectedTask}
          onClose={() => setSelectedTask(null)}
          onTaskUpdated={upsertTask}
          onTasksChanged={refreshProjectTasks}
          onTaskDeleted={() => { setSelectedTask(null); refreshProjectTasks(); }}
          onActivityChanged={() => setActivityRefreshKey((current) => current + 1)}
          onContributionChanged={() => setContributionRefreshKey((current) => current + 1)}
        />
      )}
    </main>
      </div>
    </div>
  );
}

export default ProjectDetail;
