import { useEffect, useRef, useState } from "react";
import {
  CalendarDays,
  Check,
  CheckSquare2,
  Download,
  Edit3,
  FileText,
  Paperclip,
  Send,
  Trash2,
  Undo2,
  X
} from "lucide-react";
import * as fileApi from "../api/fileApi";
import * as taskApi from "../api/taskApi";
import TaskForm from "./TaskForm";
import { useToast } from "../context/ToastContext";
import { formatFileSize, parseTaskLabels } from "../utils/taskDetails";
import { buildTaskPayload } from "../utils/taskForm";

const statusLabels = {
  TODO: "Cần làm",
  IN_PROGRESS: "Đang thực hiện",
  REVIEW: "Chờ duyệt",
  DONE: "Hoàn thành"
};

const priorityLabels = {
  LOW: "Thấp",
  MEDIUM: "Trung bình",
  HIGH: "Cao",
  URGENT: "Khẩn cấp"
};

const typeLabels = {
  FRONTEND: "Frontend",
  BACKEND: "Backend",
  UI_UX: "UI/UX",
  TESTING: "Kiểm thử",
  CONTENT: "Nội dung"
};
const reviewStatusLabels = { NONE: "Chưa gửi", PENDING: "Chờ duyệt", APPROVED: "Đã phê duyệt", CHANGES_REQUESTED: "Cần chỉnh sửa" };
const reviewActionLabels = { SUBMITTED: "Đã gửi duyệt", APPROVED: "Đã phê duyệt", CHANGES_REQUESTED: "Yêu cầu chỉnh sửa" };

const initials = (name) => name?.split(/\s+/).slice(-2).map((word) => word[0]).join("").toUpperCase() || "?";

const formatDate = (value) => value
  ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`))
  : "Chưa đặt";

function TaskDetailModal({
  task: initialTask,
  members,
  taskTypes = [],
  projectTasks = [],
  allowCustomReviewers = false,
  onAddTaskType,
  currentUserId,
  canManageTasks,
  onClose,
  onOpenTask,
  onTaskUpdated,
  onTasksChanged,
  onTaskDeleted,
  onActivityChanged,
  onContributionChanged
}) {
  const [task, setTask] = useState(initialTask);
  const [comments, setComments] = useState([]);
  const [files, setFiles] = useState([]);
  const [checklist, setChecklist] = useState([]);
  const [subtasks, setSubtasks] = useState([]);
  const [newChecklistItem, setNewChecklistItem] = useState("");
  const [showSubtaskForm, setShowSubtaskForm] = useState(false);
  const [reviewHistory, setReviewHistory] = useState([]);
  const [taskActivities, setTaskActivities] = useState([]);
  const [watchers, setWatchers] = useState([]);
  const [mentionedUserIds, setMentionedUserIds] = useState([]);
  const [reviewerId, setReviewerId] = useState(task.reviewer?.id || "");
  const [showChangesDialog, setShowChangesDialog] = useState(false);
  const [changesReason, setChangesReason] = useState("");
  const [draggingChecklistId, setDraggingChecklistId] = useState(null);
  const [comment, setComment] = useState("");
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [savingEdit, setSavingEdit] = useState(false);
  const [activityTab, setActivityTab] = useState("comments");
  const [loadingComments, setLoadingComments] = useState(true);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const headerFileInputRef = useRef(null);
  const commentFileInputRef = useRef(null);
  const busyActionRef = useRef("");
  const activeTaskIdRef = useRef(initialTask.id);
  const { showToast } = useToast();

  useEffect(() => {
    const root = document.documentElement;
    const body = document.body;
    root.classList.add("task-detail-scroll-locked");
    body.classList.add("task-detail-scroll-locked");

    return () => {
      root.classList.remove("task-detail-scroll-locked");
      body.classList.remove("task-detail-scroll-locked");
    };
  }, []);

  const canEdit = canManageTasks || task.assignedTo?.id === currentUserId;
  const canDelete = canManageTasks || task.createdBy?.id === currentUserId;
  const canMoveToUnplanned = task.planningState === "ACTIVE" && (canManageTasks || task.assignedTo?.id === currentUserId);
  const canMoveToBoard = task.planningState === "UNPLANNED" && (canManageTasks || task.assignedTo?.id === currentUserId);
  const canRemoveFromPlan = task.planningState === "PLANNED" && Boolean(task.weeklyPlanId) && canManageTasks;
  const taskType = String(task.workCategoryId || task.type || "");
  const isFollowing = watchers.some((watcher) => watcher.user?.id === currentUserId);
  const completedChecklist = checklist.filter((item) => item.completed).length;
  const completedSubtasks = subtasks.filter((item) => item.statusGroup === "DONE").length;
  const labels = parseTaskLabels(task.labels);
  const parentTask = task.parentTaskId
    ? projectTasks.find((item) => String(item.id) === String(task.parentTaskId)) || { id: task.parentTaskId, title: task.parentTaskTitle }
    : null;
  const taskTypeLabel = task.workCategoryName
    || taskTypes.find((item) => String(item.value ?? item.id) === taskType)?.label
    || typeLabels[taskType]
    || task.type
    || "Chưa phân loại";

  const canDeleteTaskFile = (file) => canManageTasks
    || String(file.uploadedBy?.id ?? file.uploadedByUserId ?? "") === String(currentUserId ?? "");

  const runBusy = async (action, callback) => {
    if (busyActionRef.current) return undefined;
    busyActionRef.current = action;
    setBusyAction(action);
    try {
      return await callback();
    } finally {
      busyActionRef.current = "";
      setBusyAction("");
    }
  };

  const loadComments = async (taskId = task.id) => {
    setLoadingComments(true);
    try {
      const response = await taskApi.getTaskComments(taskId);
      if (String(activeTaskIdRef.current) === String(taskId)) setComments(response.data || []);
    } catch (err) {
      if (String(activeTaskIdRef.current) === String(taskId)) setError(err.response?.data?.message || "Không thể tải bình luận");
    } finally {
      if (String(activeTaskIdRef.current) === String(taskId)) setLoadingComments(false);
    }
  };

  const loadFiles = async (taskId = task.id) => {
    setLoadingFiles(true);
    try {
      const response = await fileApi.getTaskFiles(taskId);
      if (String(activeTaskIdRef.current) === String(taskId)) setFiles(response.data || []);
    } catch (err) {
      if (String(activeTaskIdRef.current) === String(taskId)) setError(err.response?.data?.message || "Không thể tải tệp");
    } finally {
      if (String(activeTaskIdRef.current) === String(taskId)) setLoadingFiles(false);
    }
  };

  const loadTaskStructure = async (taskId = task.id) => {
    const requests = [
      { request: taskApi.getChecklist(taskId), apply: (response) => setChecklist(response.data || []) },
      { request: taskApi.getSubtasks(taskId), apply: (response) => setSubtasks(response.data || []) },
      { request: taskApi.getTaskActivities(taskId, 0, 100), apply: (response) => setTaskActivities(response.data?.content || []) },
      { request: taskApi.getTaskWatchers(taskId), apply: (response) => setWatchers(response.data || []) },
      { request: taskApi.getTaskReviewHistory(taskId), apply: (response) => setReviewHistory(response.data || []) }
    ];
    const results = await Promise.allSettled(requests.map(({ request }) => request));
    if (String(activeTaskIdRef.current) !== String(taskId)) return;
    results.forEach((result, index) => {
      if (result.status === "fulfilled") requests[index].apply(result.value);
    });
    const failure = results.find((result) => result.status === "rejected");
    if (failure) setError(failure.reason?.response?.data?.message || "Một phần dữ liệu chi tiết chưa thể tải");
  };

  const refreshTaskActivities = async () => {
    const taskId = task.id;
    try {
      const response = await taskApi.getTaskActivities(taskId, 0, 100);
      if (String(activeTaskIdRef.current) === String(taskId)) setTaskActivities(response.data?.content || []);
    } catch {
      // The main action has already succeeded; keep the current timeline if refreshing it fails.
    }
  };

  useEffect(() => {
    const taskId = initialTask.id;
    let active = true;
    activeTaskIdRef.current = taskId;
    setTask(initialTask);
    setComments([]);
    setFiles([]);
    setChecklist([]);
    setSubtasks([]);
    setTaskActivities([]);
    setWatchers([]);
    setReviewHistory([]);
    setNewChecklistItem("");
    setMentionedUserIds([]);
    setComment("");
    setChangesReason("");
    setShowChangesDialog(false);
    setDraggingChecklistId(null);
    setActivityTab("comments");
    busyActionRef.current = "";
    setBusyAction("");
    setLoadingComments(true);
    setLoadingFiles(true);
    setReviewerId(initialTask.reviewer?.id || "");
    setEditing(false);
    setEditForm(null);
    setSavingEdit(false);
    setShowSubtaskForm(false);
    setError("");
    loadComments(taskId);
    loadFiles(taskId);
    loadTaskStructure(taskId);
    taskApi.getTaskDetail(taskId)
      .then((response) => {
        if (!active || String(activeTaskIdRef.current) !== String(taskId) || !response.data) return;
        setTask(response.data);
        setReviewerId(response.data.reviewer?.id || "");
      })
      .catch((err) => {
        if (active && String(activeTaskIdRef.current) === String(taskId)) setError(err.response?.data?.message || "Không thể tải đầy đủ chi tiết công việc");
      });
    return () => { active = false; };
  }, [initialTask.id]);

  useEffect(() => {
    if (initialTask.id !== task.id || initialTask.version == null || initialTask.version === task.version) return;
    setTask(initialTask);
  }, [initialTask, task.id, task.version]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key !== "Escape") return;
      if (showChangesDialog) {
        setShowChangesDialog(false);
      } else if (editing) {
        setEditing(false);
        setEditForm(null);
        setError("");
      } else {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [editing, onClose, showChangesDialog]);

  const initialValues = {
    id: task.id,
    title: task.title,
    description: task.description || "",
    assignedToUserId: task.assignedTo?.id || "",
    boardColumnId: task.boardColumnId || null,
    status: task.status,
    priority: task.priority,
    type: taskType,
    workCategoryId: task.workCategoryId || "",
    reviewRequired: task.reviewRequired,
    parentTaskId: task.parentTaskId || null,
    startDate: task.startDate || "",
    dueDate: task.dueDate || "",
    estimatedEffort: task.estimatedEffort ?? 0,
    actualEffort: task.actualEffort ?? "",
    labels: task.labels || "",
    version: task.version ?? null
  };

  const handleUpdateTask = async (payload, attachment) => {
    const response = await taskApi.updateTask(task.id, payload);
    setTask(response.data);
    onTaskUpdated?.(response.data);
    if (attachment) await handleUploadTaskFile(attachment);
    setEditing(false);
    setEditForm(null);
    showToast("Cập nhật task thành công");
    await refreshTaskActivities();
    onActivityChanged?.();
    onContributionChanged?.();
  };

  const beginInlineEdit = () => {
    setEditForm({ ...initialValues });
    setEditing(true);
    setError("");
  };

  const cancelInlineEdit = () => {
    setEditing(false);
    setEditForm(null);
    setError("");
  };

  const updateInlineField = (event) => {
    const { checked, name, type, value } = event.target;
    setEditForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  };

  const handleInlineTaskTypeChange = async (event) => {
    if (event.target.value !== "__ADD_TASK_TYPE__") {
      updateInlineField(event);
      return;
    }
    const label = window.prompt("Tên loại công việc mới")?.trim();
    if (!label) return;
    const value = await onAddTaskType?.(label);
    if (value) setEditForm((current) => ({ ...current, workCategoryId: value }));
  };

  const handleSaveInlineEdit = async () => {
    if (!editForm?.title?.trim()) {
      setError("Tiêu đề công việc không được để trống");
      return;
    }
    setSavingEdit(true);
    setError("");
    try {
      await handleUpdateTask(buildTaskPayload(editForm, taskTypes));
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật công việc");
    } finally {
      setSavingEdit(false);
    }
  };

  const handleAddComment = async (event) => {
    event.preventDefault();
    if (!comment.trim()) return;
    await runBusy("comment", async () => {
      setError("");
      try {
        const response = await taskApi.createTaskComment(task.id, { content: comment, mentionedUserIds });
        setComments((current) => [...current, response.data]);
        setComment("");
        setMentionedUserIds([]);
        showToast("Đã thêm bình luận");
        await refreshTaskActivities();
        onActivityChanged?.();
        onContributionChanged?.();
      } catch (err) {
        setError(err.response?.data?.message || "Không thể thêm bình luận");
      }
    });
  };

  const toggleFollowing = async () => {
    await runBusy("watch", async () => {
      try {
        if (isFollowing) await taskApi.unfollowTask(task.id); else await taskApi.followTask(task.id);
        const response = await taskApi.getTaskWatchers(task.id);
        setWatchers(response.data || []);
        await refreshTaskActivities();
      } catch (err) { setError(err.response?.data?.message || "Không thể cập nhật theo dõi"); }
    });
  };

  const handleUploadTaskFile = async (file) => {
    if (!file) return;
    await runBusy("upload", async () => {
      setError("");
      try {
        const response = await fileApi.uploadTaskFile(task.id, file);
        setFiles((current) => [response.data, ...current]);
        showToast("Tải tệp lên thành công");
        await refreshTaskActivities();
        onActivityChanged?.();
        onContributionChanged?.();
      } catch (err) {
        setError(err.response?.data?.message || "Không thể tải tệp lên");
      }
    });
  };

  const handleDownloadFile = async (file) => {
    await runBusy(`download-${file.id}`, async () => {
      try {
        const blob = await fileApi.downloadFile(file.id);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = file.originalName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      } catch (err) {
        setError(err.response?.data?.message || "Không thể tải tệp xuống");
      }
    });
  };

  const handleDeleteTaskFile = async (file) => {
    if (!window.confirm(`Xóa ${file.originalName}?`)) return;
    await runBusy(`delete-file-${file.id}`, async () => {
      try {
        await fileApi.deleteFile(file.id);
        setFiles((current) => current.filter((item) => item.id !== file.id));
        showToast("Xóa tệp thành công");
        await refreshTaskActivities();
        onActivityChanged?.();
        onContributionChanged?.();
      } catch (err) {
        setError(err.response?.data?.message || "Không thể xóa tệp");
      }
    });
  };

  const handleCreateChecklistItem = async (event) => {
    event.preventDefault();
    const content = newChecklistItem.trim();
    if (!content) return;
    try {
      const response = await taskApi.createChecklistItem(task.id, { content });
      setChecklist((current) => [...current, response.data]);
      setNewChecklistItem("");
      await refreshTaskActivities();
      onTasksChanged?.();
    } catch (err) {
      setError(err.response?.data?.message || "Không thể thêm checklist");
    }
  };

  const handleToggleChecklistItem = async (item) => {
    try {
      const response = await taskApi.updateChecklistItem(task.id, item.id, { content: item.content, completed: !item.completed });
      setChecklist((current) => current.map((currentItem) => currentItem.id === item.id ? response.data : currentItem));
      await refreshTaskActivities();
      onTasksChanged?.();
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật checklist");
    }
  };

  const handleRenameChecklistItem = async (item) => {
    const content = window.prompt("Nội dung checklist", item.content)?.trim();
    if (!content || content === item.content) return;
    try {
      const response = await taskApi.updateChecklistItem(task.id, item.id, { content, completed: item.completed });
      setChecklist((current) => current.map((currentItem) => currentItem.id === item.id ? response.data : currentItem));
      await refreshTaskActivities();
    } catch (err) {
      setError(err.response?.data?.message || "Không thể sửa checklist");
    }
  };

  const handleDeleteChecklistItem = async (item) => {
    if (!window.confirm(`Xóa mục "${item.content}"?`)) return;
    try {
      await taskApi.deleteChecklistItem(task.id, item.id);
      setChecklist((current) => current.filter((currentItem) => currentItem.id !== item.id));
      await refreshTaskActivities();
      onTasksChanged?.();
    } catch (err) {
      setError(err.response?.data?.message || "Không thể xóa checklist");
    }
  };

  const handleCreateSubtask = async (payload, attachment) => {
    const response = await taskApi.createTask(task.projectId, { ...payload, parentTaskId: task.id });
    setSubtasks((current) => [...current, response.data]);
    if (attachment) {
      try {
        await fileApi.uploadTaskFile(response.data.id, attachment);
      } catch (err) {
        setError(err.response?.data?.message || "Task con đã được tạo nhưng chưa thể tải tệp đính kèm");
      }
    }
    setShowSubtaskForm(false);
    await refreshTaskActivities();
    await onTasksChanged?.();
  };

  const handleDeleteTask = async () => {
    let subtaskAction;
    if (subtasks.length > 0) {
      subtaskAction = window.confirm("Nhấn OK để xóa cả task con. Nhấn Hủy để chuyển task con thành task độc lập.") ? "DELETE" : "DETACH";
    }
    if (!window.confirm(`Xóa công việc "${task.title}"?`)) return;
    await runBusy("delete-task", async () => {
      try {
        await taskApi.deleteTask(task.id, subtaskAction);
        onTaskDeleted?.(task.id);
      } catch (err) {
        setError(err.response?.data?.message || "Không thể xóa công việc");
      }
    });
  };

  const refreshReviewHistory = async () => {
    const taskId = task.id;
    try {
      const response = await taskApi.getTaskReviewHistory(taskId);
      if (String(activeTaskIdRef.current) === String(taskId)) setReviewHistory(response.data || []);
    } catch {
      // Keep the existing review timeline when its refresh fails after a successful action.
    }
  };

  const applyReviewResult = async (updatedTask, message) => {
    setTask(updatedTask);
    onTaskUpdated?.(updatedTask);
    await refreshReviewHistory();
    await refreshTaskActivities();
    await onTasksChanged?.();
    showToast(message);
  };

  const handleSubmitReview = async () => {
    await runBusy("submit-review", async () => {
      try {
        const response = await taskApi.submitTaskReview(task.id, reviewerId || null);
        await applyReviewResult(response.data, "Đã gửi công việc để duyệt");
      } catch (err) {
        setError(err.response?.data?.message || "Không thể gửi duyệt");
      }
    });
  };

  const handleApproveReview = async () => {
    await runBusy("approve-review", async () => {
      try {
        const response = await taskApi.approveTaskReview(task.id);
        await applyReviewResult(response.data, "Đã phê duyệt công việc");
      } catch (err) {
        setError(err.response?.data?.message || "Không thể phê duyệt");
      }
    });
  };

  const handleRequestChanges = async (event) => {
    event.preventDefault();
    if (!changesReason.trim()) return;
    await runBusy("request-changes", async () => {
      try {
        const response = await taskApi.requestTaskChanges(task.id, changesReason.trim());
        setShowChangesDialog(false);
        setChangesReason("");
        await applyReviewResult(response.data, "Đã yêu cầu chỉnh sửa");
      } catch (err) {
        setError(err.response?.data?.message || "Không thể yêu cầu chỉnh sửa");
      }
    });
  };

  const handleChecklistDrop = async (targetId) => {
    if (!draggingChecklistId || draggingChecklistId === targetId) return;
    const next = [...checklist];
    const fromIndex = next.findIndex((item) => item.id === draggingChecklistId);
    const toIndex = next.findIndex((item) => item.id === targetId);
    const [moved] = next.splice(fromIndex, 1);
    next.splice(toIndex, 0, moved);
    setChecklist(next);
    setDraggingChecklistId(null);
    try {
      const response = await taskApi.reorderChecklist(task.id, next.map((item) => item.id));
      setChecklist(response.data || next);
      await refreshTaskActivities();
    } catch (err) {
      await loadTaskStructure();
      setError(err.response?.data?.message || "Không thể sắp xếp checklist");
    }
  };

  const handleMoveToUnplanned = async () => {
    const includeSubtasks = subtasks.length > 0
      ? window.confirm("Task này có task con. Đưa toàn bộ task con về danh sách chưa lên kế hoạch?")
      : false;
    if (subtasks.length > 0 && !includeSubtasks) return;
    await runBusy("move-to-unplanned", async () => {
      try {
        await taskApi.moveTaskToUnplanned(task.id, includeSubtasks);
        await onTasksChanged?.();
        showToast("Đã đưa công việc về danh sách chờ");
        onClose();
      } catch (err) {
        setError(err.response?.data?.message || "Không thể đưa task về danh sách chưa lên kế hoạch");
      }
    });
  };

  const handleMoveToBoard = async () => {
    const includeSubtasks = subtasks.length > 0
      ? window.confirm("Task này có task con. Đưa toàn bộ task con lên Board?")
      : false;
    if (subtasks.length > 0 && !includeSubtasks) return;
    await runBusy("move-to-board", async () => {
      try {
        await taskApi.moveTaskToBoard(task.id, includeSubtasks);
        await onTasksChanged?.();
        showToast("Đã đưa công việc lên Board");
        onClose();
      } catch (err) {
        setError(err.response?.data?.message || "Không thể đưa task lên Board");
      }
    });
  };

  const handleRemoveFromWeeklyPlan = async () => {
    const includeSubtasks = subtasks.length > 0
      ? window.confirm("Task này có task con. Đưa toàn bộ task con ra khỏi kế hoạch tuần?")
      : true;
    if (subtasks.length > 0 && !includeSubtasks) return;
    await runBusy("remove-from-plan", async () => {
      try {
        await taskApi.removeTaskFromWeeklyPlan(task.weeklyPlanId, task.id, includeSubtasks);
        await onTasksChanged?.();
        showToast("Đã đưa công việc ra khỏi kế hoạch tuần");
        onClose();
      } catch (err) {
        setError(err.response?.data?.message || "Không thể đưa task ra khỏi kế hoạch tuần");
      }
    });
  };

  const handleOpenRelatedTask = async (relatedTask) => {
    if (!relatedTask?.id || !onOpenTask) return;
    await runBusy(`open-task-${relatedTask.id}`, async () => {
      try {
        const response = await taskApi.getTaskDetail(relatedTask.id);
        onOpenTask(response.data || relatedTask);
      } catch {
        onOpenTask(relatedTask);
      }
    });
  };

  const reviewerOptions = members.filter((member) => allowCustomReviewers || ["OWNER", "LEADER"].includes(member.role));
  const canSubmitReview = task.planningState === "ACTIVE" && task.reviewRequired && task.assignedTo?.id === currentUserId && ["NONE", "CHANGES_REQUESTED"].includes(task.reviewStatus || "NONE");
  const canProcessReview = task.reviewStatus === "PENDING" && (canManageTasks || task.reviewer?.id === currentUserId);

  return (
    <div className="task-modal-backdrop task-detail-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <aside className="task-detail-drawer" role="dialog" aria-modal="true">
        <header className="task-detail-header">
          <div>
            <span>CV-{task.id}</span>
            <h2>{editing ? <input className="task-inline-title" name="title" maxLength={200} value={editForm?.title || ""} onChange={updateInlineField} autoFocus /> : task.title}</h2>
            <div className="task-detail-badges">
              <em className={`task-detail-status status-${task.status?.toLowerCase()}`}>{statusLabels[task.status] || task.status}</em>
              <em className={`task-detail-priority priority-${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority] || task.priority}</em>
              {task.projectName && <em>{task.projectName}</em>}
            </div>
          </div>
          <div>
            {canEdit && !editing && <button type="button" onClick={beginInlineEdit}><Edit3 size={16} /> Chỉnh sửa</button>}
            {canEdit && editing && <><button className="task-inline-save" type="button" disabled={savingEdit} onClick={handleSaveInlineEdit}><Check size={16} /> {savingEdit ? "Đang lưu..." : "Lưu"}</button><button type="button" disabled={savingEdit} onClick={cancelInlineEdit}><Undo2 size={16} /> Hủy</button></>}
            {canDelete && <button type="button" disabled={Boolean(busyAction)} onClick={handleDeleteTask} title="Xóa công việc"><Trash2 size={18} /></button>}
            <button type="button" onClick={onClose} title="Đóng"><X size={20} /></button>
          </div>
        </header>

        {error && <div className="alert alert-danger mx-4">{error}</div>}

        <div className={`task-detail-body ${editing ? "is-inline-editing" : ""}`}>
            <main className="task-detail-main">
              <section className="task-detail-section">
                <header className="task-description-header">
                  <h3>Mô tả</h3>
                  {canEdit && !editing && <button type="button" onClick={beginInlineEdit}><Edit3 size={14} /> Chỉnh sửa</button>}
                </header>
                {editing ? <textarea className="task-inline-description" name="description" maxLength={1500} rows={4} value={editForm?.description || ""} onChange={updateInlineField} placeholder="Thêm mô tả công việc" /> : task.description ? <p>{task.description}</p> : <p className="task-detail-muted">Chưa có mô tả.</p>}
              </section>

              <section className="task-detail-section">
                <header>
                  <h3>Task con <span>{completedSubtasks}/{subtasks.length}</span></h3>
                  {!task.parentTaskId && <button type="button" onClick={() => setShowSubtaskForm((current) => !current)}>+ Thêm task con</button>}
                </header>
                {showSubtaskForm && (
                  <div className="task-subtask-form">
                    <TaskForm
                      initialValues={{ parentTaskId: task.id, status: "TODO", workCategoryId: task.workCategoryId || "", type: task.workCategoryName || task.type }}
                      members={members}
                      parentTasks={[]}
                      taskTypes={taskTypes}
                      onAddTaskType={onAddTaskType}
                      canAssign={canManageTasks}
                      submitLabel="Tạo task con"
                      loadingLabel="Đang tạo..."
                      onSubmit={handleCreateSubtask}
                    />
                  </div>
                )}
                <div className="task-subtask-list">
                  {subtasks.length === 0 && <div className="task-detail-empty">Chưa có task con.</div>}
                  {subtasks.map((subtask) => (
                    <button type="button" key={subtask.id} className="task-subtask-link" disabled={!onOpenTask} onClick={() => handleOpenRelatedTask(subtask)}>
                      <CheckSquare2 size={16} />
                      <div><strong>{subtask.title}</strong><small>{subtask.assignedTo?.fullName || "Chưa giao"}</small></div>
                      <em className={`task-detail-status status-${subtask.status?.toLowerCase()}`}>{statusLabels[subtask.status] || subtask.status}</em>
                    </button>
                  ))}
                </div>
              </section>

              <section className="task-detail-section">
                <h3>Checklist <span>{completedChecklist}/{checklist.length}</span></h3>
                <div className="task-check-progress"><i style={{ width: `${checklist.length ? Math.round((completedChecklist / checklist.length) * 100) : 0}%` }} /></div>
                <div className="task-checklist">
                  {checklist.length === 0 && <div className="task-detail-empty">Chưa có mục checklist.</div>}
                  {checklist.map((item) => (
                    <div
                      className={item.completed ? "is-completed" : ""}
                      draggable
                      key={item.id}
                      onDragStart={() => setDraggingChecklistId(item.id)}
                      onDragOver={(event) => event.preventDefault()}
                      onDrop={() => handleChecklistDrop(item.id)}
                      onDragEnd={() => setDraggingChecklistId(null)}
                    >
                      <button type="button" onClick={() => handleToggleChecklistItem(item)}><span className={item.completed ? "checked" : ""}>{item.completed && <Check size={14} />}</span></button>
                      <button type="button" onClick={() => handleRenameChecklistItem(item)}>{item.content}</button>
                      <button type="button" onClick={() => handleDeleteChecklistItem(item)} title="Xóa"><Trash2 size={14} /></button>
                    </div>
                  ))}
                </div>
                <form className="task-checklist-add" onSubmit={handleCreateChecklistItem}>
                  <input value={newChecklistItem} onChange={(event) => setNewChecklistItem(event.target.value)} maxLength={300} placeholder="Thêm mục checklist" />
                  <button type="submit">Thêm</button>
                </form>
              </section>

              <section className="task-detail-section">
                <header>
                  <h3>Tệp đính kèm <span>{files.length}</span></h3>
                  <button type="button" disabled={busyAction === "upload"} onClick={() => headerFileInputRef.current?.click()}>
                    {busyAction === "upload" ? "Đang tải..." : "+ Thêm tệp"}
                  </button>
                </header>
                <input
                  ref={headerFileInputRef}
                  className="task-hidden-file-input"
                  type="file"
                  onChange={(event) => {
                    const selectedFile = event.target.files?.[0];
                    event.target.value = "";
                    if (selectedFile) handleUploadTaskFile(selectedFile);
                  }}
                />
                {loadingFiles ? <div className="task-detail-empty">Đang tải tệp...</div> : (
                  <div className="task-file-list">
                    {files.length === 0 && <div className="task-detail-empty">Chưa có tệp đính kèm.</div>}
                    {files.map((file) => (
                      <article key={file.id}>
                        <FileText size={18} />
                        <button className="task-file-name" type="button" disabled={Boolean(busyAction)} onClick={() => handleDownloadFile(file)}>
                          <strong>{file.originalName}</strong>
                          <small>{formatFileSize(file.fileSize)}</small>
                        </button>
                        <div className="task-file-actions">
                          <button type="button" disabled={Boolean(busyAction)} onClick={() => handleDownloadFile(file)} title={`Tải xuống ${file.originalName}`}><Download size={15} /></button>
                          {canDeleteTaskFile(file) && <button className="task-file-delete" type="button" disabled={Boolean(busyAction)} onClick={() => handleDeleteTaskFile(file)} title={`Xóa ${file.originalName}`}><Trash2 size={15} /></button>}
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </section>

              <section className="task-detail-section">
                <div className="task-activity-tabs">
                  <button className={activityTab === "comments" ? "active" : ""} onClick={() => setActivityTab("comments")} type="button">Bình luận</button>
                  <button className={activityTab === "history" ? "active" : ""} onClick={() => setActivityTab("history")} type="button">Lịch sử</button>
                </div>
                {activityTab === "comments" ? (
                  <>
                    <div className="task-comment-list">
                      {loadingComments && <div className="task-detail-empty">Đang tải bình luận...</div>}
                      {!loadingComments && comments.length === 0 && <div className="task-detail-empty">Chưa có bình luận.</div>}
                      {comments.map((item) => (
                        <article key={item.id}>
                          <span>{initials(item.author?.fullName)}</span>
                          <div><strong>{item.author?.fullName}</strong><small>{new Date(item.createdAt).toLocaleString("vi-VN")}</small><p>{item.content}</p></div>
                        </article>
                      ))}
                    </div>
                    <div className="task-mention-picker">
                      <span>Nhắc thành viên:</span>
                      {members.filter((member) => (member.userId || member.id) !== currentUserId).map((member) => {
                        const memberId = member.userId || member.id;
                        return <button type="button" className={mentionedUserIds.includes(memberId) ? "active" : ""} key={memberId} onClick={() => setMentionedUserIds((ids) => ids.includes(memberId) ? ids.filter((id) => id !== memberId) : [...ids, memberId])}>@{member.fullName}</button>;
                      })}
                    </div>
                    <form className="task-comment-form" onSubmit={handleAddComment}>
                      <input value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Viết bình luận..." maxLength={2000} />
                      <button type="button" disabled={Boolean(busyAction)} onClick={() => commentFileInputRef.current?.click()} title="Đính kèm tệp"><Paperclip size={17} /></button>
                      <button type="submit" disabled={Boolean(busyAction) || !comment.trim()}><Send size={16} /> {busyAction === "comment" ? "Đang gửi..." : "Gửi"}</button>
                    </form>
                    <input
                      ref={commentFileInputRef}
                      className="task-hidden-file-input"
                      type="file"
                      onChange={(event) => {
                        const selectedFile = event.target.files?.[0];
                        event.target.value = "";
                        if (selectedFile) handleUploadTaskFile(selectedFile);
                      }}
                    />
                  </>
                ) : (
                  <div className="task-history-list">
                    {taskActivities.length === 0 && <div className="task-detail-empty">Chưa có lịch sử thay đổi.</div>}
                    {taskActivities.map((item) => (
                      <article className="task-review-history-item" key={`${item.source}-${item.id}`}>
                        <span>{initials(item.actor?.fullName)}</span>
                        <div>
                          <strong>{item.description || `${item.actor?.fullName || "Thành viên đã rời dự án"} · ${reviewActionLabels[item.actionType] || item.actionType}`}</strong>
                          <small>{new Date(item.createdAt).toLocaleString("vi-VN")}</small>
                          {item.oldValue !== null && item.newValue !== null && <p>{item.oldValue} → {item.newValue}</p>}
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </main>

            <aside className="task-detail-side">
              <section>
                <h3>Chi tiết</h3>
                <dl>
                  <dt>Người thực hiện</dt><dd>{editing ? <select className="task-inline-control" name="assignedToUserId" value={editForm?.assignedToUserId || ""} onChange={updateInlineField} disabled={!canManageTasks}><option value="">Chưa giao</option>{members.map((member) => <option key={member.userId || member.id} value={member.userId || member.id}>{member.fullName} ({member.role})</option>)}</select> : <><span className="task-person-avatar">{initials(task.assignedTo?.fullName)}</span>{task.assignedTo?.fullName || "Chưa giao"}</>}</dd>
                  <dt>Người tạo</dt><dd><span className="task-person-avatar">{initials(task.createdBy?.fullName)}</span>{task.createdBy?.fullName || "Không rõ"}</dd>
                  <dt>Task cha</dt><dd>{editing && !task.parentTaskId ? <select className="task-inline-control" name="parentTaskId" value={editForm?.parentTaskId || ""} onChange={updateInlineField} disabled={subtasks.length > 0}><option value="">Task độc lập</option>{projectTasks.filter((item) => !item.parentTaskId && item.id !== task.id).map((item) => <option key={item.id} value={item.id}>CV-{item.id} · {item.title}</option>)}</select> : parentTask ? <button className="task-parent-link" type="button" disabled={!onOpenTask || Boolean(busyAction)} onClick={() => handleOpenRelatedTask(parentTask)}>CV-{parentTask.id} · {parentTask.title || "Task cha"}</button> : "Không có"}</dd>
                  <dt>Trạng thái</dt><dd>{editing ? <select className="task-inline-control" name="status" value={editForm?.status || "TODO"} onChange={updateInlineField}>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value} disabled={Boolean(editForm?.reviewRequired) && ["REVIEW", "DONE"].includes(value) && value !== editForm?.status}>{label}</option>)}</select> : <em className={`task-detail-status status-${task.status?.toLowerCase()}`}>{statusLabels[task.status] || task.status}</em>}</dd>
                  <dt>Mức ưu tiên</dt><dd>{editing ? <select className="task-inline-control" name="priority" value={editForm?.priority || "MEDIUM"} onChange={updateInlineField}>{Object.entries(priorityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select> : <em className={`task-detail-priority priority-${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority] || task.priority}</em>}</dd>
                  <dt>Lĩnh vực</dt><dd>{editing ? <select className="task-inline-control" name="workCategoryId" value={editForm?.workCategoryId || taskTypes.find((item) => item.active !== false)?.id || ""} onChange={handleInlineTaskTypeChange}>{taskTypes.length === 0 && <option value="">Chưa phân loại</option>}{taskTypes.map((item) => <option key={item.id || item.value} value={item.id || item.value} disabled={item.active === false}>{item.label}</option>)}{onAddTaskType && <option value="__ADD_TASK_TYPE__">+ Thêm loại công việc</option>}</select> : <em className="task-detail-type" style={{ color: task.workCategoryColor }}>{taskTypeLabel}</em>}</dd>
                  <dt>Ngày bắt đầu</dt><dd>{editing ? <input className="task-inline-control" name="startDate" type="date" value={editForm?.startDate || ""} onChange={updateInlineField} /> : <><CalendarDays size={15} /> {formatDate(task.startDate)}</>}</dd>
                  <dt>Deadline</dt><dd className={!editing ? "task-detail-deadline" : ""}>{editing ? <input className="task-inline-control" name="dueDate" type="date" value={editForm?.dueDate || ""} onChange={updateInlineField} /> : <><CalendarDays size={15} /> {formatDate(task.dueDate)}</>}</dd>
                  <dt>Ước tính</dt><dd>{editing ? <input className="task-inline-control" name="estimatedEffort" type="number" min="0" step="0.25" value={editForm?.estimatedEffort ?? 0} onChange={updateInlineField} /> : task.estimatedEffort != null ? task.estimatedEffort : "Chưa ước tính"}</dd>
                  <dt>Thực tế</dt><dd>{editing ? <input className="task-inline-control" name="actualEffort" type="number" min="0" step="0.25" value={editForm?.actualEffort ?? ""} onChange={updateInlineField} placeholder="Chưa ghi nhận" /> : task.actualEffort != null ? task.actualEffort : "Chưa ghi nhận"}</dd>
                  <dt>Nhãn</dt><dd>{editing ? <input className="task-inline-control" name="labels" maxLength={500} value={editForm?.labels || ""} onChange={updateInlineField} placeholder="UI, Backend, Bug..." /> : labels.length > 0 ? labels.map((label) => <span className="task-label" key={label}>{label}</span>) : <span className="task-detail-muted">Chưa có nhãn</span>}</dd>
                  {editing && <><dt>Cần gửi duyệt</dt><dd><label className="task-inline-checkbox"><input name="reviewRequired" type="checkbox" checked={Boolean(editForm?.reviewRequired)} onChange={updateInlineField} /><span>Bật quy trình duyệt</span></label></dd></>}
                </dl>
              </section>
              <section className="task-review-panel">
                <h3>Gửi duyệt</h3>
                <dl>
                  <dt>Trạng thái</dt><dd><em className={`task-review-state review-${(task.reviewStatus || "NONE").toLowerCase()}`}>{reviewStatusLabels[task.reviewStatus || "NONE"]}</em></dd>
                  <dt>Người duyệt</dt><dd>{task.reviewer?.fullName || "Tự động chọn"}</dd>
                </dl>
                {canSubmitReview && (
                  <div className="task-review-submit">
                    <select value={reviewerId} onChange={(event) => setReviewerId(event.target.value)}>
                      <option value="">Tự động chọn người duyệt</option>
                      {reviewerOptions.map((member) => <option key={member.userId || member.id} value={member.userId || member.id}>{member.fullName} ({member.role})</option>)}
                    </select>
                    <button type="button" disabled={Boolean(busyAction)} onClick={handleSubmitReview}>{busyAction === "submit-review" ? "Đang gửi..." : "Gửi duyệt"}</button>
                  </div>
                )}
                {canProcessReview && (
                  <div className="task-review-actions">
                    <button type="button" disabled={Boolean(busyAction)} onClick={handleApproveReview}>{busyAction === "approve-review" ? "Đang duyệt..." : "Phê duyệt"}</button>
                    <button type="button" disabled={Boolean(busyAction)} onClick={() => setShowChangesDialog(true)}>Yêu cầu chỉnh sửa</button>
                  </div>
                )}
                <div className="task-review-timeline">
                  <h4>Lịch sử duyệt</h4>
                  {reviewHistory.length === 0 && <p>Chưa có lượt gửi duyệt.</p>}
                  {[...reviewHistory].reverse().slice(0, 5).map((item) => (
                    <article key={item.id}>
                      <span>{initials(item.actor?.fullName)}</span>
                      <div>
                        <strong>{reviewActionLabels[item.action] || item.action}</strong>
                        <small>{item.actor?.fullName || "Thành viên đã rời dự án"} · {new Date(item.createdAt).toLocaleString("vi-VN")}</small>
                        {item.reviewer?.fullName && <small>Người duyệt: {item.reviewer.fullName}</small>}
                        {item.comment && <p>{item.comment}</p>}
                      </div>
                    </article>
                  ))}
                </div>
              </section>
              <section>
                <h3>Theo dõi</h3>
                <div className="task-followers">
                  {watchers.slice(0, 4).map((watcher, index) => <span key={watcher.id} className={`avatar-color-${index % 4}`}>{initials(watcher.user?.fullName)}</span>)}
                  <small>{watchers.length} người đang theo dõi</small>
                </div>
                <button type="button" disabled={Boolean(busyAction)} className="task-watch-button" onClick={toggleFollowing}>{busyAction === "watch" ? "Đang cập nhật..." : isFollowing ? "Bỏ theo dõi" : "Theo dõi"}</button>
              </section>
              {(canMoveToUnplanned || canMoveToBoard || canRemoveFromPlan) && (
                <section className="task-planning-action">
                  {canMoveToUnplanned && <button type="button" disabled={Boolean(busyAction)} onClick={handleMoveToUnplanned}><Undo2 size={16} /> {busyAction === "move-to-unplanned" ? "Đang chuyển..." : "Đưa về danh sách chờ"}</button>}
                  {canMoveToBoard && <button type="button" disabled={Boolean(busyAction)} onClick={handleMoveToBoard}><CheckSquare2 size={16} /> {busyAction === "move-to-board" ? "Đang chuyển..." : "Đưa công việc lên Board"}</button>}
                  {canRemoveFromPlan && <button type="button" disabled={Boolean(busyAction)} onClick={handleRemoveFromWeeklyPlan}><Undo2 size={16} /> {busyAction === "remove-from-plan" ? "Đang chuyển..." : "Loại khỏi kế hoạch tuần"}</button>}
                </section>
              )}
            </aside>
        </div>
      </aside>
      {showChangesDialog && (
        <div className="task-review-dialog-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setShowChangesDialog(false)}>
          <form className="task-review-dialog" onSubmit={handleRequestChanges}>
            <header><h3>Yêu cầu chỉnh sửa</h3><button type="button" onClick={() => setShowChangesDialog(false)}><X size={18} /></button></header>
            <label>Lý do<textarea autoFocus required maxLength={1000} rows={5} value={changesReason} onChange={(event) => setChangesReason(event.target.value)} /></label>
            <footer><button type="button" disabled={Boolean(busyAction)} onClick={() => setShowChangesDialog(false)}>Hủy</button><button type="submit" disabled={Boolean(busyAction) || !changesReason.trim()}>{busyAction === "request-changes" ? "Đang gửi..." : "Gửi yêu cầu"}</button></footer>
          </form>
        </div>
      )}
    </div>
  );
}

export default TaskDetailModal;
