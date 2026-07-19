import {
  Activity,
  ArrowRight,
  BarChart3,
  Bell,
  CalendarDays,
  CheckSquare2,
  Clock3,
  FileText,
  FolderKanban,
  HelpCircle,
  Home,
  LayoutGrid,
  LayoutList,
  LogOut,
  MessageCircle,
  MoreHorizontal,
  Plus,
  RefreshCw,
  Search,
  Settings,
  Trash2,
  UserPlus,
  UsersRound,
  X
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import * as projectApi from "../api/projectApi";
import * as workspaceApi from "../api/workspaceApi";
import AddMemberForm from "../components/AddMemberForm";
import ProjectForm from "../components/ProjectForm";
import { useAuth } from "../context/AuthContext";

const roleLabels = { OWNER: "Chủ sở hữu", LEADER: "Trưởng nhóm", MEMBER: "Thành viên" };
const statusLabels = { PLANNING: "Lên kế hoạch", IN_PROGRESS: "Đang thực hiện", COMPLETED: "Hoàn thành", CANCELLED: "Đã hủy" };
const projectProgress = { PLANNING: 20, IN_PROGRESS: 60, COMPLETED: 100, CANCELLED: 0 };
const projectTabOptions = [
  { key: "ALL", label: "Tất cả" },
  { key: "IN_PROGRESS", label: "Đang thực hiện" },
  { key: "PLANNING", label: "Sắp hoàn thành" },
  { key: "COMPLETED", label: "Hoàn thành" },
  { key: "CANCELLED", label: "Đã lưu trữ" }
];

const formatDate = (value) => value
  ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`))
  : "Chưa đặt";

const relativeTime = (value) => {
  if (!value) return "Gần đây";
  const hours = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 3600000));
  if (hours < 1) return "Vừa xong";
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
};

const initials = (name) => name?.split(/\s+/).slice(-2).map((word) => word[0]).join("").toUpperCase() || "UT";

function WorkspaceDetail() {
  const { workspaceId } = useParams();
  const location = useLocation();
  const [workspace, setWorkspace] = useState(null);
  const [projects, setProjects] = useState([]);
  const [editForm, setEditForm] = useState({ name: "", description: "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [activeTab, setActiveTab] = useState(() => {
    const requestedTab = new URLSearchParams(location.search).get("tab");
    return ["overview", "projects", "members", "settings"].includes(requestedTab) ? requestedTab : "overview";
  });
  const [projectFilter, setProjectFilter] = useState("ALL");
  const [projectSearch, setProjectSearch] = useState("");
  const [projectStatusFilter, setProjectStatusFilter] = useState("ALL");
  const [projectOwnerFilter, setProjectOwnerFilter] = useState("ALL");
  const [projectSort, setProjectSort] = useState("updated");
  const [projectView, setProjectView] = useState("grid");
  const [modal, setModal] = useState(null);
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  const handleApiError = (err, fallbackMessage) => {
    if (err.response?.status === 401) {
      logout();
      navigate("/login");
      return;
    }
    setError(err.response?.data?.message || fallbackMessage);
  };

  const loadWorkspace = async () => {
    setLoading(true);
    setError("");
    try {
      const [workspaceResponse, projectsResponse] = await Promise.all([
        workspaceApi.getWorkspaceDetail(workspaceId),
        projectApi.getWorkspaceProjects(workspaceId)
      ]);
      setWorkspace(workspaceResponse.data);
      setProjects(projectsResponse.data || []);
      setEditForm({ name: workspaceResponse.data.name, description: workspaceResponse.data.description || "" });
    } catch (err) {
      handleApiError(err, "Không thể tải không gian làm việc");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadWorkspace(); }, [workspaceId]);

  useEffect(() => {
    const requestedTab = new URLSearchParams(location.search).get("tab");
    if (["overview", "projects", "members", "settings"].includes(requestedTab)) {
      setActiveTab(requestedTab);
    }
  }, [location.search]);

  const canEdit = workspace?.myRole === "OWNER" || workspace?.myRole === "LEADER";
  const canDelete = workspace?.myRole === "OWNER";
  const canAddMember = workspace?.myRole === "OWNER" || workspace?.myRole === "LEADER";
  const canManageProjects = canAddMember;

  const handleUpdateWorkspace = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await workspaceApi.updateWorkspace(workspaceId, editForm);
      setSuccess("Cập nhật không gian làm việc thành công");
      setModal(null);
      await loadWorkspace();
    } catch (err) {
      handleApiError(err, "Không thể cập nhật không gian làm việc");
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteWorkspace = async () => {
    if (!window.confirm("Xóa không gian làm việc này? Hành động không thể hoàn tác.")) return;
    try {
      await workspaceApi.deleteWorkspace(workspaceId);
      navigate("/workspaces");
    } catch (err) {
      handleApiError(err, "Không thể xóa không gian làm việc");
    }
  };

  const handleAddMember = async (payload) => {
    await workspaceApi.addWorkspaceMember(workspaceId, payload);
    setModal(null);
    setSuccess("Đã thêm thành viên vào workspace");
    await loadWorkspace();
  };

  const handleCreateProject = async (payload) => {
    const response = await projectApi.createProject(workspaceId, payload);
    const createdProject = response.data;
    setProjects((current) => [createdProject, ...current]);
    setSuccess("Tạo dự án thành công");
    setModal(null);

    const requestedProjectTab = new URLSearchParams(location.search).get("projectTab");
    const validProjectTabs = ["tasks", "chat", "calendar", "files", "dashboard", "activity"];
    if (requestedProjectTab && validProjectTabs.includes(requestedProjectTab)) {
      navigate(`/projects/${createdProject.id}?tab=${requestedProjectTab}`);
    }
  };

  const handleRoleChange = async (userId, role) => {
    try {
      await workspaceApi.updateWorkspaceMemberRole(workspaceId, userId, { role });
      setSuccess("Cập nhật vai trò thành công");
      await loadWorkspace();
    } catch (err) {
      handleApiError(err, "Không thể cập nhật vai trò");
    }
  };

  const handleRemoveMember = async (member) => {
    if (!window.confirm(`Xóa ${member.fullName} khỏi không gian làm việc?`)) return;
    try {
      await workspaceApi.removeWorkspaceMember(workspaceId, member.userId);
      setSuccess("Xóa thành viên thành công");
      await loadWorkspace();
    } catch (err) {
      handleApiError(err, "Không thể xóa thành viên");
    }
  };

  const sortedProjects = useMemo(() => [...projects].sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)), [projects]);
  const members = workspace?.members || [];
  const ownerOptions = useMemo(() => {
    const creators = sortedProjects
      .map((project) => project.createdBy)
      .filter(Boolean)
      .reduce((map, creator) => map.set(creator.id || creator.email || creator.fullName, creator), new Map());
    return Array.from(creators.values());
  }, [sortedProjects]);

  const projectCounts = useMemo(() => ({
    ALL: sortedProjects.length,
    IN_PROGRESS: sortedProjects.filter((project) => project.status === "IN_PROGRESS").length,
    PLANNING: sortedProjects.filter((project) => project.status === "PLANNING").length,
    COMPLETED: sortedProjects.filter((project) => project.status === "COMPLETED").length,
    CANCELLED: sortedProjects.filter((project) => project.status === "CANCELLED").length
  }), [sortedProjects]);

  const filteredProjects = useMemo(() => {
    const searchTerm = projectSearch.trim().toLowerCase();
    return sortedProjects
      .filter((project) => projectFilter === "ALL" || project.status === projectFilter)
      .filter((project) => projectStatusFilter === "ALL" || project.status === projectStatusFilter)
      .filter((project) => {
        if (!searchTerm) return true;
        return `${project.name || ""} ${project.description || ""} ${project.createdBy?.fullName || ""}`.toLowerCase().includes(searchTerm);
      })
      .filter((project) => {
        if (projectOwnerFilter === "ALL") return true;
        const creatorKey = String(project.createdBy?.id || project.createdBy?.email || project.createdBy?.fullName || "");
        return creatorKey === projectOwnerFilter;
      })
      .sort((a, b) => {
        if (projectSort === "deadline") return new Date(a.endDate || "9999-12-31") - new Date(b.endDate || "9999-12-31");
        if (projectSort === "name") return (a.name || "").localeCompare(b.name || "", "vi");
        return new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0);
      });
  }, [projectFilter, projectOwnerFilter, projectSearch, projectSort, projectStatusFilter, sortedProjects]);

  if (loading) {
    return <main className="workspace-detail-loading"><RefreshCw className="dashboard-spin" size={22} /> Đang tải không gian làm việc...</main>;
  }

  if (!workspace) {
    return <main className="workspace-detail-loading">{error || "Không tìm thấy không gian làm việc"}</main>;
  }

  const renderProjectCards = (limit, sourceProjects = sortedProjects, view = "grid") => {
    const visible = typeof limit === "number" ? sourceProjects.slice(0, limit) : sourceProjects;
    if (visible.length === 0) return <div className="workspace-detail-empty">Chưa có dự án nào trong workspace.</div>;
    return (
      <div className={`workspace-project-grid workspace-project-grid-${view}`}>
        {visible.map((project, index) => (
          <article className="workspace-project-card" key={project.id}>
            <div className="workspace-project-title">
              <span className={`workspace-project-icon workspace-project-icon-${index % 3}`}><FolderKanban size={20} /></span>
              <div><h3>{project.name}</h3><p>{project.description || "Dự án cộng tác của nhóm"}</p></div>
            </div>
            <span className={`workspace-project-status status-${project.status?.toLowerCase()}`}>{statusLabels[project.status] || project.status}</span>
            <div className="workspace-project-owner">
              <span className="workspace-project-avatar">{initials(project.createdBy?.fullName)}</span>
              <span>{project.createdBy?.fullName || "Thành viên phụ trách"}</span>
            </div>
            <div className="workspace-project-progress-label"><strong>{projectProgress[project.status] ?? 0}%</strong><span>Cập nhật {relativeTime(project.updatedAt)}</span></div>
            <div className="workspace-project-progress"><i style={{ width: `${projectProgress[project.status] ?? 0}%` }} /></div>
            <div className="workspace-project-meta">
              <span><CalendarDays size={14} /> {formatDate(project.endDate)}</span>
              {project.endDate && new Date(`${project.endDate}T23:59:59`).getTime() < Date.now() && !["COMPLETED", "CANCELLED"].includes(project.status) && <b>Quá hạn</b>}
            </div>
            <div className="workspace-project-footer">
              <div className="workspace-mini-avatars">
                {members.slice(0, 4).map((member, memberIndex) => <span key={member.userId} className={`avatar-color-${memberIndex % 4}`}>{initials(member.fullName)}</span>)}
              </div>
              <Link to={`/projects/${project.id}`}>Mở dự án</Link>
            </div>
          </article>
        ))}
      </div>
    );
  };

  const renderMembers = (limit, manageable = false) => {
    const visible = typeof limit === "number" ? members.slice(0, limit) : members;
    return (
      <div className="workspace-member-list">
        {visible.map((member, index) => (
          <div className="workspace-member-row" key={member.userId}>
            <span className={`workspace-member-avatar avatar-color-${index % 4}`}>{initials(member.fullName)}</span>
            <span><strong>{member.fullName}</strong><small>{member.email}</small></span>
            {manageable && workspace.myRole === "OWNER" ? (
              <select value={member.role} onChange={(event) => handleRoleChange(member.userId, event.target.value)}>
                <option value="OWNER">Chủ sở hữu</option><option value="LEADER">Trưởng nhóm</option><option value="MEMBER">Thành viên</option>
              </select>
            ) : <b className={`workspace-role workspace-role-${member.role?.toLowerCase()}`}>{roleLabels[member.role]}</b>}
            <i className="workspace-member-online" />
            {manageable && workspace.myRole === "OWNER" && member.role !== "OWNER" && (
              <button type="button" onClick={() => handleRemoveMember(member)} title="Xóa thành viên"><X size={15} /></button>
            )}
          </div>
        ))}
      </div>
    );
  };

  const firstProject = projects[0] || null;
  const projectNavItems = [
    { key: "tasks", label: "Công việc", icon: CheckSquare2 },
    { key: "chat", label: "Tin nhắn", icon: MessageCircle },
    { key: "calendar", label: "Lịch & cuộc họp", icon: CalendarDays },
    { key: "files", label: "Tài liệu", icon: FileText },
    { key: "dashboard", label: "Báo cáo", icon: BarChart3 },
    { key: "activity", label: "Hoạt động", icon: Activity }
  ];
  const renderProjectNavLink = ({ key, label, icon: Icon }) => {
    if (!firstProject) {
      return (
        <Link
          key={key}
          to={`/workspaces/${workspaceId}?tab=projects&projectTab=${key}`}
          onClick={() => {
            setActiveTab("projects");
            if (canManageProjects) setModal("project");
            else setError("Workspace chưa có dự án để mở mục này.");
          }}
          title="Tạo dự án để mở mục này"
        >
          <Icon size={18} /> {label}
        </Link>
      );
    }

    return (
      <Link key={key} to={`/projects/${firstProject.id}?tab=${key}`}>
        <Icon size={18} /> {label}
      </Link>
    );
  };
  const pageTitles = { overview: "Tổng quan workspace", projects: "Dự án", members: "Thành viên", settings: "Cài đặt workspace" };
  const renderProjectBoard = () => (
    <section className="workspace-project-board">
      <div className="workspace-project-tabs-modern">
        {projectTabOptions.map((tab) => (
          <button key={tab.key} className={projectFilter === tab.key ? "active" : ""} onClick={() => setProjectFilter(tab.key)}>
            {tab.label} <span>{projectCounts[tab.key] || 0}</span>
          </button>
        ))}
      </div>

      <div className="workspace-project-toolbar-modern">
        <label className="workspace-project-search"><Search size={17} /><input value={projectSearch} onChange={(event) => setProjectSearch(event.target.value)} placeholder="Tìm kiếm dự án..." /></label>
        <select value={projectStatusFilter} onChange={(event) => setProjectStatusFilter(event.target.value)} aria-label="Trạng thái">
          <option value="ALL">Trạng thái</option>
          {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
        <select value={projectOwnerFilter} onChange={(event) => setProjectOwnerFilter(event.target.value)} aria-label="Người phụ trách">
          <option value="ALL">Người phụ trách</option>
          {ownerOptions.map((owner) => <option key={owner.id || owner.email || owner.fullName} value={String(owner.id || owner.email || owner.fullName)}>{owner.fullName}</option>)}
        </select>
        <select value={projectSort} onChange={(event) => setProjectSort(event.target.value)} aria-label="Sắp xếp">
          <option value="updated">Cập nhật gần nhất</option>
          <option value="deadline">Deadline gần nhất</option>
          <option value="name">Tên dự án</option>
        </select>
        <div className="workspace-project-view-toggle" aria-label="Kiểu hiển thị">
          <button className={projectView === "grid" ? "active" : ""} onClick={() => setProjectView("grid")} title="Dạng lưới"><LayoutGrid size={18} /></button>
          <button className={projectView === "list" ? "active" : ""} onClick={() => setProjectView("list")} title="Dạng danh sách"><LayoutList size={18} /></button>
        </div>
      </div>

      <div className="workspace-project-scroll">
        {renderProjectCards(undefined, filteredProjects, projectView)}
      </div>
    </section>
  );

  return (
    <div className="workspace-focus-layout">
      <aside className="workspace-focus-sidebar">
        <Link className="workspace-focus-brand" to="/dashboard">
          <span>U</span> UniTask
        </Link>
        <Link className="workspace-focus-back" to="/workspaces"><ArrowRight size={15} /> Tất cả không gian</Link>
        <div className="workspace-focus-switcher">
          <span>{initials(workspace.name)}</span>
          <div><strong>{workspace.name}</strong><small>{roleLabels[workspace.myRole]} · {members.length} thành viên</small></div>
          <MoreHorizontal size={17} />
        </div>

        <nav className="workspace-focus-nav">
          <p>Không gian</p>
          <Link className={activeTab === "overview" ? "active" : ""} to={`/workspaces/${workspaceId}?tab=overview`}><Home size={18} /> Tổng quan</Link>
          <p>Công việc</p>
          <Link className={activeTab === "projects" ? "active" : ""} to={`/workspaces/${workspaceId}?tab=projects`}><FolderKanban size={18} /> Dự án</Link>
          {renderProjectNavLink(projectNavItems[0])}
          <p>Cộng tác</p>
          <Link className={activeTab === "members" ? "active" : ""} to={`/workspaces/${workspaceId}?tab=members`}><UsersRound size={18} /> Thành viên</Link>
          {projectNavItems.slice(1, 4).map(renderProjectNavLink)}
          <p>Quản lý</p>
          {projectNavItems.slice(4).map(renderProjectNavLink)}
          {canEdit && <Link className={activeTab === "settings" ? "active" : ""} to={`/workspaces/${workspaceId}?tab=settings`}><Settings size={18} /> Cài đặt</Link>}
        </nav>

        <div className="workspace-focus-user">
          <span>{initials(user?.fullName)}</span>
          <div><strong>{user?.fullName || "Thành viên UniTask"}</strong><small>{user?.email}</small></div>
          <button onClick={() => { logout(); navigate("/login"); }} title="Đăng xuất"><LogOut size={17} /></button>
        </div>
      </aside>

      <div className="workspace-focus-content">
        <header className="workspace-focus-topbar">
          <label><Search size={18} /><input placeholder={`Tìm trong ${workspace.name}...`} /></label>
          <div><button title="Thông báo"><Bell size={19} /></button><button title="Trợ giúp"><HelpCircle size={19} /></button></div>
        </header>

        <main className="workspace-detail-page workspace-focus-main">
          <header className="workspace-focus-heading">
            <div>
              <h1>{pageTitles[activeTab]}</h1>
              <p>{workspace.name} <span>·</span> {workspace.description || "Không gian cộng tác và quản lý công việc nhóm"}</p>
              <div className="workspace-header-avatars">{members.slice(0, 5).map((member, index) => <span key={member.userId} className={`avatar-color-${index % 4}`} title={member.fullName}>{initials(member.fullName)}</span>)}<button onClick={() => setModal("member")}><Plus size={17} /></button></div>
            </div>
            <div className="workspace-detail-actions">
              {canAddMember && <button className="workspace-outline-action" onClick={() => setModal("member")}><UserPlus size={17} /> Mời thành viên</button>}
              {canManageProjects && <button className="workspace-primary-action" onClick={() => setModal("project")}><Plus size={17} /> Tạo dự án</button>}
              <div className="dropdown">
                <button className="workspace-more-action" data-bs-toggle="dropdown"><MoreHorizontal size={19} /></button>
                <ul className="dropdown-menu dropdown-menu-end">
                  {canEdit && <li><button className="dropdown-item" onClick={() => setModal("settings")}><Settings size={15} /> Chỉnh thông tin</button></li>}
                  {canDelete && <li><button className="dropdown-item text-danger" onClick={handleDeleteWorkspace}><Trash2 size={15} /> Xóa workspace</button></li>}
                </ul>
              </div>
            </div>
          </header>

      {error && <div className="alert alert-danger mt-3">{error}</div>}
      {success && <div className="alert alert-success mt-3">{success}</div>}

      {activeTab === "overview" && (
        <div className="workspace-overview-grid">
          <section className="workspace-detail-panel workspace-projects-panel">
            <header><h2>Dự án của workspace</h2><button onClick={() => setActiveTab("projects")}>Xem tất cả dự án <ArrowRight size={15} /></button></header>
            {renderProjectCards(3)}
          </section>
          <section className="workspace-detail-panel workspace-members-panel">
            <header><h2>Thành viên</h2><button onClick={() => setActiveTab("members")}>Quản lý thành viên</button></header>
            {renderMembers(6)}
          </section>
          <section className="workspace-detail-panel workspace-deadlines-panel">
            <header><h2><CalendarDays size={18} /> Deadline sắp tới</h2></header>
            <div className="workspace-deadline-list">
              {sortedProjects.filter((project) => project.endDate).slice(0, 4).map((project, index) => (
                <Link to={`/projects/${project.id}`} key={project.id}><span className={`deadline-color-${index % 3}`}><Clock3 size={16} /></span><strong>{formatDate(project.endDate)}</strong><em>{project.name}</em></Link>
              ))}
              {!projects.some((project) => project.endDate) && <div className="workspace-detail-empty">Chưa có deadline dự án.</div>}
            </div>
          </section>
          <section className="workspace-detail-panel workspace-activity-panel">
            <header><h2><Activity size={18} /> Cập nhật gần đây</h2></header>
            <div className="workspace-update-list">
              {sortedProjects.slice(0, 4).map((project, index) => (
                <Link to={`/projects/${project.id}`} key={project.id}><span className={`avatar-color-${index % 4}`}>{initials(project.createdBy?.fullName)}</span><strong>{project.name} được cập nhật</strong><time>{relativeTime(project.updatedAt)}</time></Link>
              ))}
              {projects.length === 0 && <div className="workspace-detail-empty">Chưa có cập nhật gần đây.</div>}
            </div>
          </section>
        </div>
      )}

      {activeTab === "projects" && renderProjectBoard()}
      {activeTab === "members" && <section className="workspace-detail-panel workspace-tab-panel"><header><h2>Tất cả thành viên</h2>{canAddMember && <button onClick={() => setModal("member")}><UserPlus size={15} /> Mời thành viên</button>}</header>{renderMembers(undefined, true)}</section>}
      {activeTab === "settings" && canEdit && (
        <section className="workspace-detail-panel workspace-settings-panel"><header><h2>Thông tin workspace</h2></header>
          <form onSubmit={handleUpdateWorkspace}><label>Tên workspace</label><input name="name" value={editForm.name} onChange={(event) => setEditForm((current) => ({ ...current, name: event.target.value }))} required /><label>Mô tả</label><textarea name="description" rows={4} value={editForm.description} onChange={(event) => setEditForm((current) => ({ ...current, description: event.target.value }))} /><button disabled={saving}>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button></form>
        </section>
      )}

      {modal && (
        <div className="workspace-modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setModal(null)}>
          <section className="workspace-detail-modal">
            <header><h2>{modal === "member" ? "Mời thành viên" : modal === "project" ? "Tạo dự án mới" : "Chỉnh thông tin workspace"}</h2><button onClick={() => setModal(null)}><X size={20} /></button></header>
            <div className="workspace-detail-modal-body">
              {modal === "member" && <AddMemberForm myRole={workspace.myRole} onAdd={handleAddMember} />}
              {modal === "project" && <ProjectForm submitLabel="Tạo dự án" loadingLabel="Đang tạo..." onSubmit={handleCreateProject} />}
              {modal === "settings" && <form className="workspace-modal-settings" onSubmit={handleUpdateWorkspace}><label>Tên</label><input name="name" value={editForm.name} onChange={(event) => setEditForm((current) => ({ ...current, name: event.target.value }))} required /><label>Mô tả</label><textarea rows={4} value={editForm.description} onChange={(event) => setEditForm((current) => ({ ...current, description: event.target.value }))} /><button disabled={saving}>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button></form>}
            </div>
          </section>
        </div>
      )}
        </main>
      </div>
    </div>
  );
}

export default WorkspaceDetail;
