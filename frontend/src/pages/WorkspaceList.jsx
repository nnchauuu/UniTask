import { CalendarDays, ChevronRight, FolderKanban, Grid3X3, List, PanelRightClose, Plus, RefreshCw, Search, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import * as projectApi from "../api/projectApi";
import * as workspaceApi from "../api/workspaceApi";
import CreateWorkspaceForm from "../components/CreateWorkspaceForm";
import ProjectForm from "../components/ProjectForm";
import WorkspaceCard from "../components/WorkspaceCard";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

const PAGE_SIZE = 6;
const projectStatusLabels = {
  PLANNING: "Lên kế hoạch",
  IN_PROGRESS: "Đang thực hiện",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy"
};

const formatDate = (value) => {
  if (!value) return "Chưa có hạn";
  const normalizedValue = /^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value}T00:00:00` : value;
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" })
    .format(new Date(normalizedValue));
};

function WorkspaceList() {
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [search, setSearch] = useState("");
  const [activeTab, setActiveTab] = useState("all");
  const [role, setRole] = useState("all");
  const [sort, setSort] = useState("recent");
  const [view, setView] = useState("grid");
  const [page, setPage] = useState(1);
  const [projects, setProjects] = useState([]);
  const [projectsLoading, setProjectsLoading] = useState(false);
  const [projectsError, setProjectsError] = useState("");
  const [showCreateProject, setShowCreateProject] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState(() => searchParams.get("workspace") || "");
  const [pinnedIds, setPinnedIds] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("pinnedWorkspaces") || "[]");
    } catch {
      return [];
    }
  });
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

  const loadWorkspaces = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await workspaceApi.getWorkspaces();
      setWorkspaces(response.data || []);
    } catch (err) {
      handleApiError(err, "Không thể tải không gian làm việc");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadWorkspaces(); }, []);
  useEffect(() => { setPage(1); }, [search, activeTab, role, sort]);
  useEffect(() => {
    const requestedWorkspace = searchParams.get("workspace") || "";
    setSelectedWorkspaceId((current) => current === requestedWorkspace ? current : requestedWorkspace);
  }, [searchParams]);

  const handleCreateWorkspace = async (payload) => {
    const response = await workspaceApi.createWorkspace(payload);
    setWorkspaces((current) => [response.data, ...current]);
    showToast("Tạo không gian làm việc thành công");
  };

  const togglePin = (workspaceId) => {
    setPinnedIds((current) => {
      const next = current.includes(workspaceId)
        ? current.filter((id) => id !== workspaceId)
        : [...current, workspaceId];
      localStorage.setItem("pinnedWorkspaces", JSON.stringify(next));
      return next;
    });
  };

  const filteredWorkspaces = useMemo(() => {
    const query = search.trim().toLowerCase();
    return workspaces
      .filter((workspace) => !query
        || workspace.name?.toLowerCase().includes(query)
        || workspace.description?.toLowerCase().includes(query))
      .filter((workspace) => activeTab === "all"
        || (activeTab === "owned" && workspace.myRole === "OWNER")
        || (activeTab === "joined" && workspace.myRole !== "OWNER"))
      .filter((workspace) => role === "all" || workspace.myRole === role)
      .sort((a, b) => sort === "name"
        ? (a.name || "").localeCompare(b.name || "", "vi")
        : new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
  }, [workspaces, search, activeTab, role, sort]);

  const selectedWorkspace = useMemo(
    () => workspaces.find((workspace) => String(workspace.id) === String(selectedWorkspaceId)),
    [workspaces, selectedWorkspaceId]
  );
  const selectedWorkspaceKey = selectedWorkspace?.id;
  const canManageProjects = selectedWorkspace?.myRole === "OWNER" || selectedWorkspace?.myRole === "LEADER";

  useEffect(() => {
    if (!selectedWorkspaceKey) {
      setProjects([]);
      setProjectsError("");
      return undefined;
    }

    let active = true;
    const loadProjects = async () => {
      setProjects([]);
      setProjectsLoading(true);
      setProjectsError("");
      try {
        const response = await projectApi.getWorkspaceProjects(selectedWorkspaceKey);
        if (active) setProjects(response.data || []);
      } catch (err) {
        if (!active) return;
        if (err.response?.status === 401) {
          logout();
          navigate("/login");
          return;
        }
        setProjectsError(err.response?.data?.message || "Không thể tải dự án trong không gian này");
      } finally {
        if (active) setProjectsLoading(false);
      }
    };

    loadProjects();
    return () => { active = false; };
  }, [selectedWorkspaceKey, logout, navigate]);

  const handleCollapseWorkspace = () => {
    setSelectedWorkspaceId("");
    setProjects([]);
    setProjectsError("");
    setShowCreateProject(false);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete("workspace");
    setSearchParams(nextParams, { replace: true });
  };

  const handleSelectWorkspace = (workspace) => {
    const workspaceId = String(workspace.id);
    if (workspaceId === String(selectedWorkspaceId)) {
      handleCollapseWorkspace();
      return;
    }
    setSelectedWorkspaceId(workspaceId);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set("workspace", workspaceId);
    setSearchParams(nextParams, { replace: true });
  };

  const handleCreateProject = async (payload) => {
    const response = await projectApi.createProject(selectedWorkspace.id, payload);
    setProjects((current) => [response.data, ...current]);
    setShowCreateProject(false);
  };

  const pinnedWorkspaces = filteredWorkspaces.filter((workspace) => pinnedIds.includes(workspace.id));
  const regularWorkspaces = filteredWorkspaces.filter((workspace) => !pinnedIds.includes(workspace.id));
  const pageCount = Math.max(1, Math.ceil(regularWorkspaces.length / PAGE_SIZE));
  const visibleWorkspaces = regularWorkspaces.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  return (
    <main className="workspace-library-page">


      <nav className="workspace-library-tabs" aria-label="Lọc không gian làm việc">
        <button className={activeTab === "all" ? "active" : ""} onClick={() => setActiveTab("all")}>Tất cả <span>{workspaces.length}</span></button>
        <button className={activeTab === "owned" ? "active" : ""} onClick={() => setActiveTab("owned")}>Tôi sở hữu</button>
        <button className={activeTab === "joined" ? "active" : ""} onClick={() => setActiveTab("joined")}>Đang tham gia</button>
        <button className="workspace-library-create" type="button" onClick={() => setShowCreate(true)}><Plus size={18} /> Tạo không gian</button>

      </nav>

      <div className="workspace-library-toolbar">
        <label className="workspace-library-search">
          <Search size={17} />
          <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tìm kiếm không gian làm việc..." />
        </label>
        <div className="workspace-library-filters">
          <select value={role} onChange={(event) => setRole(event.target.value)} aria-label="Lọc theo vai trò">
            <option value="all">Tất cả vai trò</option>
            <option value="OWNER">Chủ sở hữu</option>
            <option value="LEADER">Trưởng nhóm</option>
            <option value="MEMBER">Thành viên</option>
          </select>
          <select value={sort} onChange={(event) => setSort(event.target.value)} aria-label="Sắp xếp">
            <option value="recent">Cập nhật gần nhất</option>
            <option value="name">Tên A-Z</option>
          </select>
          <div className="workspace-view-toggle">
            <button className={view === "grid" ? "active" : ""} onClick={() => setView("grid")} title="Dạng lưới"><Grid3X3 size={17} /></button>
            <button className={view === "list" ? "active" : ""} onClick={() => setView("list")} title="Dạng danh sách"><List size={18} /></button>
          </div>
          <button className="workspace-reload" onClick={loadWorkspaces} title="Làm mới"><RefreshCw size={17} /></button>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {loading ? (
        <section className="workspace-library-loading" aria-live="polite" aria-busy="true">
          <span className="workspace-library-loading-label">Đang tải không gian làm việc...</span>
          <div className="workspace-library-loading-toolbar"><i /><i /><i /></div>
          <div className="workspace-library-loading-grid">
            {Array.from({ length: 6 }, (_, index) => (
              <div key={index}><i /><span /><span /><small /></div>
            ))}
          </div>
        </section>
      ) : (
        <div className={`workspace-browser${selectedWorkspace ? " has-selection" : ""}`}>
          <aside className="workspace-browser-list" aria-label="Danh sách không gian làm việc">
          {pinnedWorkspaces.length > 0 && (
            <section className="workspace-library-section">
              <h2>Đã ghim</h2>
              <div className={`workspace-library-grid workspace-library-grid-${view} workspace-library-grid-pinned`}>
                {pinnedWorkspaces.map((workspace, index) => (
                  <WorkspaceCard key={workspace.id} workspace={workspace} view={view} pinned selected={String(workspace.id) === String(selectedWorkspaceId)} onSelect={handleSelectWorkspace} onTogglePin={togglePin} colorIndex={index} />
                ))}
              </div>
            </section>
          )}

          <section className="workspace-library-section">
            <h2>{pinnedWorkspaces.length ? "Tất cả không gian làm việc" : "Không gian làm việc của bạn"}</h2>
            {visibleWorkspaces.length === 0 ? (
              <div className="workspace-library-empty">
                <Grid3X3 size={30} />
                <strong>Không tìm thấy không gian làm việc</strong>
                <span>Thử thay đổi bộ lọc hoặc tạo không gian mới.</span>
              </div>
            ) : (
              <div className={`workspace-library-grid workspace-library-grid-${view}`}>
                {visibleWorkspaces.map((workspace, index) => (
                  <WorkspaceCard key={workspace.id} workspace={workspace} view={view} pinned={false} selected={String(workspace.id) === String(selectedWorkspaceId)} onSelect={handleSelectWorkspace} onTogglePin={togglePin} colorIndex={index + pinnedWorkspaces.length} />
                ))}
              </div>
            )}
          </section>

          {pageCount > 1 && (
            <nav className="workspace-pagination" aria-label="Phân trang">
              {Array.from({ length: pageCount }, (_, index) => index + 1).map((number) => (
                <button className={page === number ? "active" : ""} key={number} onClick={() => setPage(number)}>{number}</button>
              ))}
            </nav>
          )}
          </aside>

          {selectedWorkspace && (
            <section className="workspace-inline-projects" aria-labelledby="workspace-projects-title" aria-busy={projectsLoading}>
              <header className="workspace-inline-projects-header">
                <div className="workspace-inline-projects-heading">
                  <span className="workspace-inline-projects-icon" aria-hidden="true"><FolderKanban size={20} /></span>
                  <div>
                    <p>Dự án trong không gian</p>
                    <h2 id="workspace-projects-title">{selectedWorkspace.name}</h2>
                    <span>{projectsLoading ? "Đang tải dự án" : `${projects.length} dự án`} · {selectedWorkspace.membersCount || 0} thành viên</span>
                  </div>
                </div>
                <div className="workspace-inline-project-actions">
                  {canManageProjects && (
                    <button className="workspace-inline-create" type="button" onClick={() => setShowCreateProject(true)}>
                      <Plus size={17} /> Tạo dự án
                    </button>
                  )}
                  <button className="workspace-inline-collapse" type="button" onClick={handleCollapseWorkspace} title="Thu gọn" aria-label="Thu gọn danh sách dự án">
                    <PanelRightClose size={18} />
                  </button>
                </div>
              </header>

              {projectsError && <div className="alert alert-danger">{projectsError}</div>}
              {projectsLoading ? (
                <div className="workspace-inline-project-skeleton" aria-live="polite">
                  <span className="visually-hidden">Đang tải dự án...</span>
                  {Array.from({ length: 4 }, (_, index) => (
                    <div key={index}><i /><span /><span /><small /><small /></div>
                  ))}
                </div>
              ) : projects.length === 0 && !projectsError ? (
                <div className="workspace-inline-empty">
                  <span><FolderKanban size={24} /></span>
                  <strong>Chưa có dự án trong không gian này</strong>
                  <p>{canManageProjects ? "Tạo dự án đầu tiên để nhóm bắt đầu làm việc." : "Chủ sở hữu hoặc trưởng nhóm có thể tạo dự án mới."}</p>
                  {canManageProjects && <button type="button" onClick={() => setShowCreateProject(true)}><Plus size={16} /> Tạo dự án đầu tiên</button>}
                </div>
              ) : (
                <div className="workspace-inline-project-grid">
                  {projects.map((project) => (
                    <Link className="workspace-inline-project-card" key={project.id} to={`/projects/${project.id}`}>
                      <span className="workspace-inline-project-card-icon" aria-hidden="true"><FolderKanban size={18} /></span>
                      <span className="workspace-inline-project-card-copy">
                        <strong>{project.name}</strong>
                        <small>{project.description || "Dự án cộng tác của nhóm"}</small>
                      </span>
                      <ChevronRight className="workspace-inline-project-card-arrow" size={18} aria-hidden="true" />
                      <span className={`workspace-inline-project-status status-${project.status?.toLowerCase()}`}>
                        <i aria-hidden="true" /> {projectStatusLabels[project.status] || project.status}
                      </span>
                      <span className="workspace-inline-project-date"><CalendarDays size={14} /> {formatDate(project.endDate)}</span>
                    </Link>
                  ))}
                </div>
              )}
            </section>
          )}
        </div>
      )}

      {showCreate && <CreateWorkspaceForm onCreate={handleCreateWorkspace} onClose={() => setShowCreate(false)} />}
      {showCreateProject && selectedWorkspace && (
        <div className="workspace-modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setShowCreateProject(false)}>
          <section className="workspace-detail-modal" role="dialog" aria-modal="true" aria-labelledby="create-project-title">
            <header>
              <h2 id="create-project-title">Tạo dự án trong {selectedWorkspace.name}</h2>
              <button type="button" onClick={() => setShowCreateProject(false)} title="Đóng"><X size={20} /></button>
            </header>
            <div className="workspace-detail-modal-body">
              <ProjectForm submitLabel="Tạo dự án" loadingLabel="Đang tạo..." onSubmit={handleCreateProject} />
            </div>
          </section>
        </div>
      )}
    </main>
  );
}

export default WorkspaceList;
