import {
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  CircleUserRound,
  CornerDownRight,
  Flag,
  GitBranch,
  ListChecks,
  MessageSquare,
  MoreHorizontal,
  Paperclip,
  Palette,
  Plus,
  Search,
  SlidersHorizontal,
  Tag,
  Trash2,
  X
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import * as boardApi from "../api/boardApi";
import * as taskApi from "../api/taskApi";

const defaultColumns = [
  { key: "TODO", label: "Cần làm", color: "#3b82f6", limit: "", collapsed: false, system: true, statusGroup: "TODO", defaultForGroup: true },
  { key: "IN_PROGRESS", label: "Đang thực hiện", color: "#7c3aed", limit: 5, collapsed: false, system: true, statusGroup: "IN_PROGRESS", defaultForGroup: true },
  { key: "REVIEW", label: "Chờ duyệt", color: "#a855f7", limit: "", collapsed: false, system: true, statusGroup: "IN_REVIEW", defaultForGroup: true },
  { key: "DONE", label: "Hoàn thành", color: "#19a66a", limit: "", collapsed: false, system: true, statusGroup: "DONE", defaultForGroup: true }
];

const backendStatuses = new Set(defaultColumns.map((column) => column.key));
const statusByColumnGroup = { TODO: "TODO", IN_PROGRESS: "IN_PROGRESS", IN_REVIEW: "REVIEW", DONE: "DONE" };
const taskStatusForColumn = (column) => statusByColumnGroup[column.statusGroup]
  || (backendStatuses.has(column.key) ? column.key : "TODO");

const priorityLabels = {
  LOW: "Thấp",
  MEDIUM: "Trung bình",
  HIGH: "Ưu tiên",
  URGENT: "Khẩn cấp"
};

const priorityFilterLabels = {
  ALL: "Ưu tiên",
  URGENT: "Khẩn cấp",
  HIGH: "Cao",
  MEDIUM: "Trung bình",
  LOW: "Thấp"
};

const taskTypeLabels = {
  DESIGN: "Thiết kế",
  CONTENT: "Nội dung",
  RESEARCH: "Nghiên cứu"
};

const typeLabels = { ALL: "Loại công việc" };
const statusGroupLabels = { TODO: "Chưa làm", IN_PROGRESS: "Đang làm", IN_REVIEW: "Chờ duyệt", DONE: "Hoàn thành" };

const deadlineLabels = {
  ALL: "Deadline",
  TODAY: "Hôm nay",
  WEEK: "Tuần này",
  UPCOMING: "Sắp đến hạn",
  OVERDUE: "Quá hạn",
  NONE: "Không có deadline"
};

const hexToRgb = (hex) => {
  const normalized = hex?.replace("#", "");
  if (!normalized || normalized.length !== 6) return { r: 59, g: 130, b: 246 };
  return {
    r: parseInt(normalized.slice(0, 2), 16),
    g: parseInt(normalized.slice(2, 4), 16),
    b: parseInt(normalized.slice(4, 6), 16)
  };
};

const tint = (hex, alpha) => {
  const { r, g, b } = hexToRgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};

const getTaskType = (task) => String(task.workCategoryId || task.type || task.taskType || task.category || "DESIGN");

const orderColumnTasksByHierarchy = (tasks = []) => {
  const ids = new Set(tasks.map((task) => String(task.id)));
  const childrenByParent = new Map();
  const childIds = new Set();

  tasks.forEach((task) => {
    const parentId = task.parentTaskId ? String(task.parentTaskId) : "";
    if (!parentId || !ids.has(parentId)) return;
    const children = childrenByParent.get(parentId) || [];
    children.push(task);
    childrenByParent.set(parentId, children);
    childIds.add(String(task.id));
  });

  const ordered = [];
  const seen = new Set();
  const appendFamily = (task) => {
    const id = String(task.id);
    if (seen.has(id)) return;
    seen.add(id);
    ordered.push(task);
    (childrenByParent.get(id) || []).forEach(appendFamily);
  };
  tasks.filter((task) => !childIds.has(String(task.id))).forEach(appendFamily);
  tasks.forEach(appendFamily);
  return ordered;
};

const taskBelongsToColumn = (task, column) => task.boardColumnId
  ? String(task.boardColumnId) === String(column.id)
  : task.status === column.key;

const formatDate = (value) => {
  if (!value) return "";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(new Date(`${value}T00:00:00`));
};

const formatCardDate = (value) => value
  ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`))
  : "";

const isToday = (value) => value && new Date(`${value}T00:00:00`).toDateString() === new Date().toDateString();
const isOverdue = (value) => value && new Date(`${value}T23:59:59`).getTime() < Date.now();

const isThisWeek = (value) => {
  if (!value) return false;
  const target = new Date(`${value}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const end = new Date(today);
  end.setDate(today.getDate() + 7);
  return target >= today && target <= end;
};

const isUpcoming = (value) => {
  if (!value) return false;
  const target = new Date(`${value}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return target >= today;
};

const isTomorrow = (value) => {
  if (!value) return false;
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return new Date(`${value}T00:00:00`).toDateString() === tomorrow.toDateString();
};

const initials = (name) => name?.split(/\s+/).slice(-2).map((word) => word[0]).join("").toUpperCase() || "?";
const columnColorPreferenceKey = (projectId, userId) => `unitask:kanban-column-colors:${userId ?? "anonymous"}:${projectId}`;

function KanbanBoard({ projectId, tasks = [], members = [], taskTypes = [], currentUserId, canManageTasks, viewMode = "board", sprintMode = false, weeklyPlanningMode = false, externalFilter = "ALL", onExternalFilterClear, onOpenTask, onCreateTask, onBoardChanged }) {
  const taskList = Array.isArray(tasks) ? tasks : [];
  const memberList = Array.isArray(members) ? members : [];
  const taskById = useMemo(() => new Map(taskList.map((task) => [String(task.id), task])), [taskList]);
  const [searchTerm, setSearchTerm] = useState("");
  const [assigneeFilter, setAssigneeFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [deadlineFilter, setDeadlineFilter] = useState("ALL");
  const [showDone, setShowDone] = useState(true);
  const [assigneeMenuOpen, setAssigneeMenuOpen] = useState(false);
  const [priorityMenuOpen, setPriorityMenuOpen] = useState(false);
  const [typeMenuOpen, setTypeMenuOpen] = useState(false);
  const [secondaryFiltersOpen, setSecondaryFiltersOpen] = useState(false);
  const [columns, setColumns] = useState(defaultColumns);
  const [personalColumnColors, setPersonalColumnColors] = useState({});
  const [openColumnMenu, setOpenColumnMenu] = useState("");
  const [draggingTaskId, setDraggingTaskId] = useState(null);
  const [dragOverColumn, setDragOverColumn] = useState("");
  const [draggingColumnKey, setDraggingColumnKey] = useState("");
  const [dragOverColumnKey, setDragOverColumnKey] = useState("");
  const [showAddColumn, setShowAddColumn] = useState(false);
  const [newColumnName, setNewColumnName] = useState("");
  const canConfigureBoard = canManageTasks && !sprintMode;

  const updatePersonalColumnColor = (columnId, color) => {
    if (!/^#[0-9a-fA-F]{6}$/.test(color)) return;
    setPersonalColumnColors((current) => {
      const next = { ...current, [String(columnId)]: color };
      try {
        localStorage.setItem(columnColorPreferenceKey(projectId, currentUserId), JSON.stringify(next));
      } catch {
        // Keep the current-session preference when browser storage is unavailable.
      }
      return next;
    });
  };

  const loadBoardColumns = async () => {
    try {
      const response = await boardApi.getColumns(projectId);
      setColumns(response.data || defaultColumns);
    } catch (error) {
      window.alert(error.response?.data?.message || "Không thể tải trạng thái của bảng");
    }
  };

  useEffect(() => {
    loadBoardColumns();
  }, [projectId]);

  useEffect(() => {
    try {
      const savedColors = JSON.parse(localStorage.getItem(columnColorPreferenceKey(projectId, currentUserId)) || "{}");
      const validColors = savedColors && typeof savedColors === "object" && !Array.isArray(savedColors)
        ? Object.fromEntries(Object.entries(savedColors).filter(([, color]) => /^#[0-9a-fA-F]{6}$/.test(color)))
        : {};
      setPersonalColumnColors(validColors);
    } catch {
      setPersonalColumnColors({});
    }
  }, [currentUserId, projectId]);

  useEffect(() => {
    if (!openColumnMenu) return undefined;

    const closeMenuOnOutsideClick = (event) => {
      if (!event.target.closest(".kanban-column-menu-wrap")) {
        setOpenColumnMenu("");
      }
    };

    document.addEventListener("pointerdown", closeMenuOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeMenuOnOutsideClick);
  }, [openColumnMenu]);

  useEffect(() => {
    if (!assigneeMenuOpen) return undefined;

    const closeAssigneeMenuOnOutsideClick = (event) => {
      if (!event.target.closest(".task-assignee-filter")) {
        setAssigneeMenuOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeAssigneeMenuOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeAssigneeMenuOnOutsideClick);
  }, [assigneeMenuOpen]);

  useEffect(() => {
    if (!priorityMenuOpen) return undefined;

    const closePriorityMenuOnOutsideClick = (event) => {
      if (!event.target.closest(".task-priority-filter")) {
        setPriorityMenuOpen(false);
      }
    };

    document.addEventListener("pointerdown", closePriorityMenuOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closePriorityMenuOnOutsideClick);
  }, [priorityMenuOpen]);

  useEffect(() => {
    if (!typeMenuOpen) return undefined;

    const closeTypeMenuOnOutsideClick = (event) => {
      if (!event.target.closest(".task-type-filter")) {
        setTypeMenuOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeTypeMenuOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeTypeMenuOnOutsideClick);
  }, [typeMenuOpen]);

  useEffect(() => {
    if (!secondaryFiltersOpen) return undefined;

    const closeSecondaryFiltersOnOutsideClick = (event) => {
      if (!event.target.closest(".task-board-filter-popover")) {
        setSecondaryFiltersOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeSecondaryFiltersOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeSecondaryFiltersOnOutsideClick);
  }, [secondaryFiltersOpen]);

  const statusLabels = useMemo(
    () => Object.fromEntries(columns.map((column) => [column.key, column.label])),
    [columns]
  );

  const canUpdateTask = (task) =>
    canManageTasks || (task.assignedTo && task.assignedTo.id === currentUserId);

  const updateColumn = async (key, patch) => {
    if (!canManageTasks) return;
    const currentColumn = columns.find((column) => column.key === key);
    if (!currentColumn) return;
    const nextColumn = { ...currentColumn, ...patch };
    setColumns((current) => current.map((column) => column.key === key ? nextColumn : column));
    try {
      const response = await boardApi.updateColumn(projectId, currentColumn.id, {
        label: nextColumn.label,
        color: nextColumn.color,
        limit: nextColumn.limit === "" ? null : nextColumn.limit,
        collapsed: Boolean(nextColumn.collapsed),
        statusGroup: nextColumn.statusGroup || "IN_PROGRESS",
        defaultForGroup: Boolean(nextColumn.defaultForGroup),
        version: nextColumn.version
      });
      setColumns((current) => current.map((column) => column.id === currentColumn.id ? response.data : column));
    } catch (error) {
      setColumns((current) => current.map((column) => column.key === key ? currentColumn : column));
      window.alert(error.response?.data?.message || "Không thể cập nhật cột");
    }
  };

  const moveColumnTo = async (fromKey, toKey) => {
    if (!canManageTasks || fromKey === toKey) return;
    const previous = columns;
    const fromIndex = columns.findIndex((column) => column.key === fromKey);
    const toIndex = columns.findIndex((column) => column.key === toKey);
    if (fromIndex < 0 || toIndex < 0) return;
    const next = [...columns];
    const [movedColumn] = next.splice(fromIndex, 1);
    next.splice(toIndex, 0, movedColumn);
    setColumns(next);
    try {
      const response = await boardApi.reorderColumns(projectId, next.map((column) => column.id));
      setColumns(response.data);
    } catch (error) {
      setColumns(previous);
      window.alert(error.response?.data?.message || "Không thể sắp xếp cột");
    }
  };

  const addColumn = async () => {
    if (!canManageTasks) return;
    const label = newColumnName.trim();
    if (!label) return;
    try {
      const response = await boardApi.createColumn(projectId, { label, color: "#0f766e", statusGroup: "IN_PROGRESS", defaultForGroup: false });
      setColumns((current) => [...current, response.data]);
      setNewColumnName("");
      setShowAddColumn(false);
    } catch (error) {
      window.alert(error.response?.data?.message || "Không thể thêm cột");
    }
  };

  const cancelAddColumn = () => {
    setNewColumnName("");
    setShowAddColumn(false);
  };

  const renameColumn = (column) => {
    const label = window.prompt("Tên trạng thái", column.label)?.trim();
    if (label) {
      updateColumn(column.key, { label });
      setOpenColumnMenu("");
    }
  };

  const deleteColumn = async (column) => {
    if (!canManageTasks || columns.length <= 1) return;
    const destinations = columns.filter((item) => item.id !== column.id);
    const destinationName = window.prompt(
      `Chuyển công việc sang cột nào trước khi xóa?\n${destinations.map((item) => item.label).join(", ")}`,
      destinations[0]?.label || ""
    )?.trim();
    if (!destinationName) return;
    const destination = destinations.find((item) => item.label.toLocaleLowerCase("vi") === destinationName.toLocaleLowerCase("vi"));
    if (!destination) {
      window.alert("Không tìm thấy cột đích");
      return;
    }
    if (!window.confirm(`Xóa cột "${column.label}" và chuyển công việc sang "${destination.label}"?`)) return;
    try {
      await boardApi.deleteColumn(projectId, column.id, destination.id);
      await loadBoardColumns();
      await onBoardChanged?.();
      setOpenColumnMenu("");
    } catch (error) {
      window.alert(error.response?.data?.message || "Không thể xóa cột");
    }
  };

  const setColumnLimit = (column) => {
    const value = window.prompt("Giới hạn số công việc trong cột. Bỏ trống để không giới hạn.", column.limit || "");
    if (value === null) return;
    const limit = value.trim() === "" ? "" : Math.max(1, Number(value));
    if (limit === "" || Number.isFinite(limit)) updateColumn(column.key, { limit });
  };

  const assigneeOptions = useMemo(() => {
    const map = new Map();
    memberList.forEach((member) => {
      const id = member.userId || member.id;
      if (id) {
        map.set(String(id), {
          id,
          fullName: member.fullName,
          email: member.email
        });
      }
    });
    taskList.forEach((task) => {
      if (task.assignedTo?.id) {
        map.set(String(task.assignedTo.id), task.assignedTo);
      }
    });
    return Array.from(map.values());
  }, [memberList, taskList]);

  const typeOptions = useMemo(() => {
    const values = new Map(taskTypes.map((item) => [item.value, item.label]));
    taskList.forEach((task) => {
      const value = getTaskType(task);
      if (value) values.set(value, taskTypeLabels[value] || typeLabels[value] || value);
    });
    return Array.from(values, ([value, label]) => ({ value, label }));
  }, [taskList, taskTypes]);

  const assigneeLabel = useMemo(() => {
    if (assigneeFilter === "ALL") return "Người thực hiện";
    if (assigneeFilter === "ME") return "Của tôi";
    if (assigneeFilter === "UNASSIGNED") return "Chưa được giao";
    return assigneeOptions.find((member) => String(member.id) === assigneeFilter)?.fullName || "Thành viên";
  }, [assigneeFilter, assigneeOptions]);

  const selectAssigneeFilter = (value) => {
    setAssigneeFilter(value);
    setAssigneeMenuOpen(false);
  };

  const selectPriorityFilter = (value) => {
    setPriorityFilter(value);
    setPriorityMenuOpen(false);
  };

  const selectTypeFilter = (value) => {
    setTypeFilter(value);
    setTypeMenuOpen(false);
  };

  const visibleTasks = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();
    const filteredTasks = taskList
      .filter((task) => externalFilter === "ALL"
        || (externalFilter === "UNASSIGNED" && !task.assignedTo)
        || (externalFilter === "NO_DEADLINE" && !task.dueDate)
        || (externalFilter === "UNESTIMATED" && !Number(task.estimatedEffort)))
      .filter((task) => showDone || task.status !== "DONE")
      .filter((task) => {
        if (!normalizedSearch) return true;
        return `${task.title || ""} ${task.description || ""} ${task.assignedTo?.fullName || ""} UNI-${task.id} CV-${task.id}`.toLowerCase().includes(normalizedSearch);
      })
      .filter((task) => {
        if (assigneeFilter === "ALL") return true;
        if (assigneeFilter === "ME") return task.assignedTo?.id === currentUserId;
        if (assigneeFilter === "UNASSIGNED") return !task.assignedTo;
        return String(task.assignedTo?.id || "") === assigneeFilter;
      })
      .filter((task) => priorityFilter === "ALL" || task.priority === priorityFilter)
      .filter((task) => typeFilter === "ALL" || getTaskType(task) === typeFilter)
      .filter((task) => {
        if (deadlineFilter === "ALL") return true;
        if (deadlineFilter === "TODAY") return isToday(task.dueDate);
        if (deadlineFilter === "WEEK") return isThisWeek(task.dueDate);
        if (deadlineFilter === "UPCOMING") return isUpcoming(task.dueDate);
        if (deadlineFilter === "OVERDUE") return isOverdue(task.dueDate) && task.status !== "DONE";
        if (deadlineFilter === "NONE") return !task.dueDate;
        return true;
      });

    if (!weeklyPlanningMode) return filteredTasks;

    const allTasksById = new Map(taskList.map((task) => [String(task.id), task]));
    const includedTaskIds = new Set(filteredTasks.map((task) => String(task.id)));

    filteredTasks.forEach((task) => {
      let parentId = task.parentTaskId ? String(task.parentTaskId) : "";
      const visited = new Set();
      while (parentId && !visited.has(parentId)) {
        visited.add(parentId);
        const parent = allTasksById.get(parentId);
        if (!parent) break;
        includedTaskIds.add(parentId);
        parentId = parent.parentTaskId ? String(parent.parentTaskId) : "";
      }
    });

    return taskList.filter((task) => includedTaskIds.has(String(task.id)));
  }, [assigneeFilter, currentUserId, deadlineFilter, externalFilter, priorityFilter, searchTerm, showDone, taskList, typeFilter, weeklyPlanningMode]);

  const activeFilters = [
    assigneeFilter !== "ALL" && { key: "assignee", label: assigneeFilter === "ME" ? "Người thực hiện: Của tôi" : assigneeFilter === "UNASSIGNED" ? "Chưa được giao" : `Người thực hiện: ${assigneeOptions.find((member) => String(member.id) === assigneeFilter)?.fullName || "Thành viên"}`, clear: () => setAssigneeFilter("ALL") },
    priorityFilter !== "ALL" && { key: "priority", label: `Ưu tiên: ${priorityLabels[priorityFilter] || priorityFilter}`, clear: () => setPriorityFilter("ALL") },
    typeFilter !== "ALL" && { key: "type", label: `Loại: ${typeOptions.find((option) => option.value === typeFilter)?.label || typeFilter}`, clear: () => setTypeFilter("ALL") },
    deadlineFilter !== "ALL" && { key: "deadline", label: deadlineLabels[deadlineFilter], clear: () => setDeadlineFilter("ALL") },
    externalFilter !== "ALL" && { key: "external", label: { UNASSIGNED: "Task chưa giao", NO_DEADLINE: "Task chưa có deadline", UNESTIMATED: "Task chưa ước tính" }[externalFilter] || "Bộ lọc kế hoạch", clear: () => onExternalFilterClear?.() },
    !showDone && { key: "done", label: "Ẩn task đã hoàn thành", clear: () => setShowDone(true) }
  ].filter(Boolean);

  const handleDrop = async (event, column, targetPosition) => {
    event.preventDefault();
    event.stopPropagation();
    const taskId = Number(event.dataTransfer.getData("text/plain"));
    const task = taskList.find((item) => item.id === taskId);
    const columnTasks = visibleTasks.filter((item) => taskBelongsToColumn(item, column));
    const limit = Number(column.limit);
    setDraggingTaskId(null);
    setDragOverColumn("");
    if (!task || !canUpdateTask(task)) return;
    if (task.boardColumnId !== column.id && Number.isFinite(limit) && limit > 0 && columnTasks.length >= limit) {
      window.alert("Cột này đã đạt giới hạn số công việc.");
      return;
    }
    try {
      if (task.reviewRequired && column.statusGroup === "IN_REVIEW" && ["NONE", "CHANGES_REQUESTED"].includes(task.reviewStatus)) {
        await taskApi.submitTaskReview(task.id);
      } else {
        await boardApi.moveTask(task.id, column.id, targetPosition ?? columnTasks.length);
      }
      await onBoardChanged?.();
    } catch (error) {
      window.alert(error.response?.data?.message || "Không thể di chuyển công việc");
    }
  };

  return (
    <section className={`task-board-shell ${sprintMode ? "task-board-shell-sprint" : ""} ${weeklyPlanningMode ? "task-board-shell-weekly" : ""}`}>
      <div className="task-board-toolbar task-board-toolbar-modern">
        <label className="task-board-search"><Search size={17} /><input value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Tìm công việc..." /></label>
        <div className={`task-board-filter-popover ${secondaryFiltersOpen ? "is-open" : ""}`}>
          {weeklyPlanningMode && <button className="task-mobile-filter-toggle" type="button" aria-controls={`kanban-secondary-filters-${projectId}`} aria-expanded={secondaryFiltersOpen} onClick={() => { setSecondaryFiltersOpen((current) => !current); setAssigneeMenuOpen(false); setPriorityMenuOpen(false); setTypeMenuOpen(false); }}><SlidersHorizontal size={16} /> Bộ lọc{activeFilters.length > 0 && <b>{activeFilters.length}</b>}</button>}
          <div className="task-board-secondary-filters" id={`kanban-secondary-filters-${projectId}`}>
            <div className="task-assignee-filter">
              <button type="button" onClick={() => { setAssigneeMenuOpen((current) => !current); setPriorityMenuOpen(false); setTypeMenuOpen(false); }}>
                <span className="task-filter-avatar">{assigneeFilter === "UNASSIGNED" ? "?" : initials(assigneeLabel)}</span>
                {assigneeLabel}
                <ChevronDown className="task-filter-chevron" size={16} />
              </button>
              {assigneeMenuOpen && (
                <div className="task-assignee-menu">
                  <button type="button" onClick={() => selectAssigneeFilter("ALL")}><span><CircleUserRound size={15} /></span><b>Người thực hiện</b></button>
                  <button type="button" onClick={() => selectAssigneeFilter("ME")}><span>{initials(assigneeOptions.find((member) => String(member.id) === String(currentUserId))?.fullName || "Tôi")}</span><b>Của tôi</b></button>
                  <button type="button" onClick={() => selectAssigneeFilter("UNASSIGNED")}><span>?</span><b>Chưa được giao</b></button>
                  {assigneeOptions.map((member) => (
                    <button key={member.id} type="button" onClick={() => selectAssigneeFilter(String(member.id))}>
                      <span>{initials(member.fullName)}</span>
                      <b>{member.fullName}</b>
                      {member.email && <small>{member.email}</small>}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="task-assignee-filter task-priority-filter">
              <button type="button" onClick={() => { setPriorityMenuOpen((current) => !current); setAssigneeMenuOpen(false); setTypeMenuOpen(false); }}>
                <span className="task-filter-avatar"><Flag size={15} /></span>
                {priorityFilterLabels[priorityFilter] || priorityFilter}
                <ChevronDown className="task-filter-chevron" size={16} />
              </button>
              {priorityMenuOpen && (
                <div className="task-assignee-menu task-priority-menu">
                  {Object.entries(priorityFilterLabels).map(([value, label]) => (
                    <button className={priorityFilter === value ? "is-selected" : ""} key={value} type="button" onClick={() => selectPriorityFilter(value)}>
                      <span><Flag size={14} /></span>
                      <b>{label}</b>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="task-assignee-filter task-type-filter">
              <button type="button" onClick={() => { setTypeMenuOpen((current) => !current); setAssigneeMenuOpen(false); setPriorityMenuOpen(false); }}>
                <span className="task-filter-avatar"><Tag size={15} /></span>
                {typeFilter === "ALL" ? "Loại công việc" : typeOptions.find((option) => option.value === typeFilter)?.label || typeFilter}
                <ChevronDown className="task-filter-chevron" size={16} />
              </button>
              {typeMenuOpen && (
                <div className="task-assignee-menu task-type-menu">
                  <button className={typeFilter === "ALL" ? "is-selected" : ""} type="button" onClick={() => selectTypeFilter("ALL")}><span><Tag size={14} /></span><b>Loại công việc</b></button>
                  {typeOptions.map((option) => (
                    <button className={typeFilter === option.value ? "is-selected" : ""} key={option.value} type="button" onClick={() => selectTypeFilter(option.value)}><span><Tag size={14} /></span><b>{option.label}</b></button>
                  ))}
                </div>
              )}
            </div>
            <label className="task-filter-control"><CalendarDays size={17} /><select value={deadlineFilter} onChange={(event) => setDeadlineFilter(event.target.value)}>{Object.entries(deadlineLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <label className="task-filter-toggle"><input type="checkbox" checked={showDone} onChange={(event) => setShowDone(event.target.checked)} /> Hoàn thành</label>
          </div>
        </div>
        {canConfigureBoard && <button className="task-add-column-button" type="button" onClick={() => setShowAddColumn(true)}><Plus size={17} /> Thêm cột</button>}
      </div>
      {activeFilters.length > 0 && (
        <div className="task-active-filters">
          {activeFilters.map((filter) => (
            <button key={filter.key} type="button" onClick={filter.clear}>{filter.label}<X size={14} /></button>
          ))}
        </div>
      )}

      {viewMode === "list" && (
        <div className="task-list-view">
          <div className="task-list-head"><span>Mã</span><span>Công việc</span><span>Người thực hiện</span><span>Ưu tiên</span><span>Deadline</span><span>Trạng thái</span></div>
          {visibleTasks.map((task) => (
            <button className="task-list-row" key={task.id} type="button" onClick={() => onOpenTask(task)}>
              <span>UNI-{task.id}</span>
              <strong>{task.title}</strong>
              <span>{task.assignedTo?.fullName || "Chưa được giao"}</span>
              <em className={`task-type task-type-${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority] || task.priority}</em>
              <span className={isOverdue(task.dueDate) && task.status !== "DONE" ? "task-date-hot" : ""}>{task.dueDate ? formatDate(task.dueDate) : "Không có"}</span>
              <span>{statusLabels[task.boardColumnKey || task.status] || task.boardColumnKey || task.status}</span>
            </button>
          ))}
          {visibleTasks.length === 0 && <div className="empty-column">Không có công việc phù hợp</div>}
        </div>
      )}

      <div className={`kanban-board ${sprintMode ? "kanban-board-sprint" : ""} ${weeklyPlanningMode ? "kanban-board-weekly" : ""} ${viewMode === "list" ? "kanban-board-hidden" : ""}`} style={{ gridTemplateColumns: [...columns.map((column) => weeklyPlanningMode ? (column.collapsed ? "64px" : "288px") : !sprintMode && column.collapsed ? "74px" : "minmax(250px, 1fr)"), ...(showAddColumn ? [weeklyPlanningMode ? "288px" : "minmax(250px, 1fr)"] : [])].join(" ") }}>
        {columns.map((column, columnIndex) => {
          const positionedTasks = visibleTasks
            .filter((task) => taskBelongsToColumn(task, column))
            .sort((first, second) => (first.boardPosition ?? Number.MAX_SAFE_INTEGER) - (second.boardPosition ?? Number.MAX_SAFE_INTEGER));
          const columnTasks = orderColumnTasksByHierarchy(positionedTasks);
          const columnTaskIds = new Set(columnTasks.map((task) => String(task.id)));
          const parentTaskIdsInColumn = new Set(columnTasks.map((task) => task.parentTaskId ? String(task.parentTaskId) : "").filter(Boolean));
          const limit = Number(column.limit);
          const hasLimit = Number.isFinite(limit) && limit > 0;
          const isOverLimit = hasLimit && columnTasks.length > limit;
          const columnCollapsed = !sprintMode && Boolean(column.collapsed);
          const cardDensityClass = weeklyPlanningMode
            ? columnTasks.length >= 5 ? "is-card-density-dense" : columnTasks.length >= 3 ? "is-card-density-compact" : ""
            : columnTasks.length >= 6 ? "is-card-density-dense" : columnTasks.length >= 3 ? "is-card-density-compact" : "";
          const columnColorPreferenceId = String(column.id ?? column.key);
          const configuredColumnColor = personalColumnColors[columnColorPreferenceId] || column.color || defaultColumns[columnIndex % defaultColumns.length].color;
          const displayColumnColor = configuredColumnColor;

          return (
            <section
              className={`kanban-column ${cardDensityClass} ${columnCollapsed ? "is-collapsed" : ""} ${isOverLimit ? "is-over-limit" : ""} ${dragOverColumn === column.key ? "is-drag-over" : ""}`}
              draggable={canConfigureBoard && openColumnMenu !== column.key}
              key={column.key}
              style={{
                "--column-color": displayColumnColor,
                "--column-soft": tint(displayColumnColor, 0.1),
                "--column-softer": tint(displayColumnColor, 0.045),
                "--column-body-bg": tint(displayColumnColor, 0.065),
                "--column-header-bg": tint(displayColumnColor, 0.24),
                "--column-border": tint(displayColumnColor, 0.28)
              }}
              onDragStart={(event) => {
                if (!canConfigureBoard) return;
                setDraggingColumnKey(column.key);
                event.dataTransfer.setData("application/x-kanban-column", column.key);
                event.dataTransfer.effectAllowed = "move";
              }}
              onDragOver={(event) => {
                event.preventDefault();
                const movingColumnKey = Array.from(event.dataTransfer.types).includes("application/x-kanban-column")
                  ? event.dataTransfer.getData("application/x-kanban-column") || draggingColumnKey
                  : "";
                if (movingColumnKey) {
                  setDragOverColumnKey(column.key);
                } else {
                  setDragOverColumn(column.key);
                }
              }}
              onDragLeave={() => {
                setDragOverColumn("");
                setDragOverColumnKey("");
              }}
              onDrop={(event) => {
                const movingColumnKey = event.dataTransfer.getData("application/x-kanban-column");
                if (movingColumnKey) {
                  event.preventDefault();
                  moveColumnTo(movingColumnKey, column.key);
                  setDraggingColumnKey("");
                  setDragOverColumnKey("");
                  return;
                }
                handleDrop(event, column, columnTasks.length);
              }}
              onDragEnd={() => {
                setDraggingColumnKey("");
                setDragOverColumnKey("");
              }}
            >
              <header className={`kanban-column-header kanban-column-header-modern ${draggingColumnKey === column.key ? "is-column-dragging" : ""} ${dragOverColumnKey === column.key ? "is-column-drop-target" : ""}`}>
                <div>
                  <i className="kanban-column-color" />
                  <h3>{columnCollapsed ? columnIndex + 1 : column.label}</h3>
                  {!columnCollapsed && <span className={isOverLimit ? "is-hot" : ""} title={hasLimit ? `${columnTasks.length} công việc · Giới hạn ${limit}` : `${columnTasks.length} công việc`}>{columnTasks.length}</span>}
                </div>
                {weeklyPlanningMode && canManageTasks && onCreateTask && !columnCollapsed && <button className="kanban-column-quick-add" type="button" title={`Tạo công việc trong ${column.label}`} aria-label={`Tạo công việc trong ${column.label}`} onClick={() => onCreateTask(taskStatusForColumn(column), column.id)}><Plus size={20} /></button>}
                {!sprintMode && <div className="kanban-column-menu-wrap">
                  <button type="button" title="Quản lý cột" onClick={() => setOpenColumnMenu((current) => current === column.key ? "" : column.key)}><MoreHorizontal size={18} /></button>
                  {openColumnMenu === column.key && (
                    <div className="kanban-column-menu">
                      <button type="button" onClick={() => onCreateTask?.(taskStatusForColumn(column), column.id)}>Thêm task</button>
                      {canManageTasks && <button type="button" onClick={() => renameColumn(column)}>Đổi tên cột</button>}
                      {canManageTasks && <button type="button" onClick={() => setColumnLimit(column)}>Đặt giới hạn công việc</button>}
                      {canManageTasks && (
                        <label className="kanban-color-picker">
                          <Palette size={15} /> Màu cột
                          <input type="color" value={configuredColumnColor} onPointerDown={(event) => event.stopPropagation()} onDragStart={(event) => event.stopPropagation()} onChange={(event) => updatePersonalColumnColor(columnColorPreferenceId, event.target.value)} />
                        </label>
                      )}
                      {canManageTasks && (
                        <label className="kanban-menu-select">
                          Nhóm trạng thái
                          <select value={column.statusGroup || "IN_PROGRESS"} onChange={(event) => updateColumn(column.key, { statusGroup: event.target.value, defaultForGroup: false })}>
                            {Object.entries(statusGroupLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                          </select>
                        </label>
                      )}
                      {canManageTasks && <label className="kanban-menu-check"><input type="checkbox" checked={Boolean(column.defaultForGroup)} onChange={(event) => updateColumn(column.key, { defaultForGroup: event.target.checked })} /> Cột mặc định của nhóm</label>}
                      {canManageTasks && <button type="button" onClick={() => updateColumn(column.key, { collapsed: !column.collapsed })}>{column.collapsed ? "Mở rộng cột" : "Thu gọn cột"}</button>}
                      {canManageTasks && columns.length > 1 && <button className="kanban-delete-column" type="button" onClick={() => deleteColumn(column)}><Trash2 size={15} /> Xóa cột</button>}
                    </div>
                  )}
                </div>}
              </header>

              {columnCollapsed ? (
                <button className="kanban-collapsed-body" type="button" onClick={() => updateColumn(column.key, { collapsed: false })}>
                  <span>{column.label}</span>
                  <b>{columnTasks.length}</b>
                </button>
              ) : (
                <>
                  <div className="kanban-task-list">
                    {columnTasks.map((task, index) => {
                      const draggable = canUpdateTask(task);
                      const isSubtask = Boolean(task.parentTaskId);
                      const hasSubtasks = Number(task.subtaskCount) > 0 || parentTaskIdsInColumn.has(String(task.id));
                      const isLinkedSubtask = isSubtask && columnTaskIds.has(String(task.parentTaskId));
                      const parentTask = isSubtask ? taskById.get(String(task.parentTaskId)) : null;
                      const parentTitle = task.parentTaskTitle || parentTask?.title || "Task";
                      const commentCount = Number(task.commentCount) || 0;
                      const attachmentCount = Number(task.attachmentCount ?? task.files?.length) || 0;
                      const assigneeCandidates = (Array.isArray(task.assignees) && task.assignees.length ? task.assignees : task.assignedTo ? [task.assignedTo] : []).filter(Boolean);
                      const assignees = Array.from(new Map(assigneeCandidates.map((person, personIndex) => [String(person.id || person.email || `${person.fullName}-${personIndex}`), person])).values());
                      const taskKindLabel = isSubtask ? "Subtask" : "Task";
                      return (
                        <article
                          className={`task-card ${isSubtask ? "is-subtask-card" : ""} ${isLinkedSubtask ? "is-linked-subtask" : ""} ${hasSubtasks ? "is-parent-task-card" : ""} ${draggable ? "" : "is-locked"} ${draggingTaskId === task.id ? "is-dragging" : ""}`}
                          draggable={draggable}
                          key={task.id}
                          onDragStart={(event) => {
                            event.stopPropagation();
                            event.dataTransfer.setData("text/plain", String(task.id));
                            event.dataTransfer.effectAllowed = "move";
                            setDraggingTaskId(task.id);
                          }}
                          onDragEnd={(event) => {
                            event.stopPropagation();
                            setDraggingTaskId(null);
                            setDragOverColumn("");
                          }}
                          onDragOver={(event) => {
                            if (!draggingTaskId || draggingTaskId === task.id) return;
                            event.preventDefault();
                            event.stopPropagation();
                            setDragOverColumn(column.key);
                          }}
                          onDrop={(event) => handleDrop(event, column, index)}
                        >
                          <span className="task-card-number">{index + 1}</span>
                          <button className="task-card-main" type="button" onClick={() => onOpenTask(task)}>
                            {weeklyPlanningMode ? <>
                              <span className={`task-kind-chip ${isSubtask ? "is-child" : "is-parent"}`} title={isSubtask ? `Subtask của CV-${task.parentTaskId} · ${parentTitle}` : "Task"}>{taskKindLabel}</span>
                              <div className="task-card-topline">
                                <em className={`task-priority task-priority-${task.priority?.toLowerCase() || "medium"}`}>{priorityLabels[task.priority] || "Trung bình"}</em>
                                <time className={(isToday(task.dueDate) || isOverdue(task.dueDate)) && task.status !== "DONE" ? "task-date-hot" : ""} dateTime={task.dueDate || undefined} title={task.dueDate ? `${isToday(task.dueDate) && task.status !== "DONE" ? "Hết hạn hôm nay" : isOverdue(task.dueDate) && task.status !== "DONE" ? "Đã quá hạn" : "Deadline"}: ${formatCardDate(task.dueDate)}` : "Chưa đặt thời hạn"}>{task.dueDate ? formatCardDate(task.dueDate) : "Chưa đặt"}</time>
                              </div>
                              {isSubtask && (
                                <div className="task-parent-reference task-parent-reference-weekly" title={`Thuộc CV-${task.parentTaskId} · ${parentTitle}`}>
                                  <CornerDownRight size={12} />
                                  <span>Thuộc <b>CV-{task.parentTaskId}</b> · {parentTitle}</span>
                                </div>
                              )}
                              <h4>{task.title}</h4>
                              <div className="task-card-detail-row">
                                {task.description ? <p className="task-card-description" title={task.description}>{task.description}</p> : <span />}
                                <span className="task-card-code-badge" title={`Mã công việc CV-${task.id}`}>CV-{task.id}</span>
                              </div>
                            </> : <>
                              <div className="task-card-code"><span><Tag size={15} /></span> CV-{task.id}{isSubtask && <em className="task-hierarchy-chip is-child"><CornerDownRight size={11} /> Subtask</em>}{hasSubtasks && <em className="task-hierarchy-chip is-parent"><GitBranch size={11} /> Task</em>}</div>
                              {isSubtask && <div className="task-parent-reference" title={`Thuộc CV-${task.parentTaskId} · ${parentTitle}`}><CornerDownRight size={12} /><span>Thuộc <b>CV-{task.parentTaskId}</b> · {parentTitle}</span></div>}
                              <h4>{task.title}</h4>
                              <div className="task-card-tags">
                                <em className="task-type" style={{ color: task.workCategoryColor || undefined, borderColor: task.workCategoryColor || undefined }}><span className="task-category-swatch" style={{ background: task.workCategoryColor || "#64748B" }} />{task.workCategoryName || typeOptions.find((option) => option.value === getTaskType(task))?.label || taskTypeLabels[getTaskType(task)] || "Công việc"}</em>
                                <em className={`task-priority task-priority-${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority] || task.priority}</em>
                              </div>
                            </>}
                          </button>

                          <footer className="task-card-footer">
                            {weeklyPlanningMode ? <>
                              <div className="task-card-assignees" title={assignees.map((assignee) => assignee.fullName).join(", ") || "Chưa giao"}>
                                {assignees.slice(0, 3).map((assignee, assigneeIndex) => {
                                  const avatarUrl = assignee.avatarUrl || assignee.profileImageUrl || assignee.imageUrl;
                                  return <span className={`task-assignee ${avatarUrl ? "has-image" : ""}`} style={avatarUrl ? { backgroundImage: `url(${avatarUrl})` } : undefined} key={assignee.id || `${assignee.fullName}-${assigneeIndex}`}>{avatarUrl ? "" : initials(assignee.fullName)}</span>;
                                })}
                                {!assignees.length && <span className="task-assignee is-unassigned">?</span>}
                                {assignees.length > 3 && <span className="task-assignee task-assignee-more">+{assignees.length - 3}</span>}
                              </div>
                              <div className="task-card-metrics">
                                <span title={`${commentCount} bình luận`}><MessageSquare size={18} /> {commentCount}</span>
                                <span title={`${attachmentCount} tệp đính kèm`}><Paperclip size={18} /> {attachmentCount}</span>
                                {hasSubtasks && (
                                  <span className="task-subtask-progress" title={`Tiến độ Subtask: ${Number(task.completedSubtaskCount) || 0}/${Number(task.subtaskCount) || 0} hoàn thành`}>
                                    <CheckCircle2 size={18} /> {Number(task.completedSubtaskCount) || 0}/{Number(task.subtaskCount) || 0}
                                  </span>
                                )}
                              </div>
                            </> : <>
                              <div>
                                {task.priority === "URGENT" || task.priority === "HIGH" ? <Flag size={15} className="task-hot" /> : <span className="task-dash" />}
                                <span><MessageSquare size={14} /> {task.commentCount ?? (task.description ? Math.min(3, Math.max(1, Math.ceil(task.description.length / 80))) : 0)}</span>
                                <span><Tag size={14} /> {task.attachmentCount ?? task.files?.length ?? 0}</span>
                                {task.subtaskCount > 0 && <span title="Subtask"><CheckCircle2 size={14} /> {task.completedSubtaskCount}/{task.subtaskCount}</span>}
                                {task.checklistCount > 0 && <span title="Checklist"><ListChecks size={14} /> {task.completedChecklistCount}/{task.checklistCount}</span>}
                                {task.reviewStatus === "PENDING" && <span className="task-review-badge is-pending">Chờ duyệt</span>}
                                {task.reviewStatus === "CHANGES_REQUESTED" && <span className="task-review-badge is-changes">Cần chỉnh sửa</span>}
                                {task.dueDate && <span className={isToday(task.dueDate) ? "task-date-hot" : ""}><CalendarDays size={14} /> {isToday(task.dueDate) ? "Hôm nay" : isTomorrow(task.dueDate) ? "Ngày mai" : formatDate(task.dueDate)}</span>}
                              </div>
                              <span className="task-assignee" title={task.assignedTo?.fullName || "Chưa giao"}>{initials(task.assignedTo?.fullName)}</span>
                              {column.key === "DONE" && <CheckCircle2 className="task-done-check" size={20} />}
                            </>}
                          </footer>
                        </article>
                      );
                    })}

                    {columnTasks.length === 0 && (
                      <div className="empty-column">Kéo công việc vào đây</div>
                    )}
                  </div>

                  {canManageTasks && onCreateTask && <button className="kanban-add-task" type="button" onClick={() => onCreateTask(taskStatusForColumn(column), column.id)}>+ Tạo công việc</button>}
                </>
              )}
            </section>
          );
        })}
        {canConfigureBoard && showAddColumn && (
          <section className="kanban-column kanban-new-column">
            <label htmlFor="kanban-new-column-name">Tên cột mới</label>
            <input
              id="kanban-new-column-name"
              autoFocus
              value={newColumnName}
              onChange={(event) => setNewColumnName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") addColumn();
                if (event.key === "Escape") cancelAddColumn();
              }}
              placeholder="Nhập tên trạng thái"
            />
            <div>
              <button className="kanban-confirm-column" type="button" onClick={addColumn} disabled={!newColumnName.trim()} title="Thêm cột"><Plus size={18} /></button>
              <button className="kanban-cancel-column" type="button" onClick={cancelAddColumn} title="Hủy"><X size={18} /></button>
            </div>
          </section>
        )}
      </div>
    </section>
  );
}

export default KanbanBoard;
