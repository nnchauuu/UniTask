import { useEffect, useMemo, useRef, useState } from "react";
import { AlertTriangle, BarChart3, CalendarDays, Check, CheckCircle2, ChevronDown, ChevronLeft, ChevronRight, Copy, Download, FileSpreadsheet, GripVertical, History, ListTodo, MoreHorizontal, Pencil, PlayCircle, Plus, Printer, Search, SlidersHorizontal, Target, Trash2, Undo2, UserRound, X } from "lucide-react";
import * as boardApi from "../api/boardApi";
import * as fileApi from "../api/fileApi";
import * as taskApi from "../api/taskApi";
import { countPlanningFilters, filterPlanHistory, filterPlanningTasks, formatCapacitySummary, paginatePlanningTasks, requiresOverloadConfirmation, validateWeeklyPlanForm } from "../utils/weeklyPlanning";
import KanbanBoard from "./KanbanBoard";
import TaskForm from "./TaskForm";

const isoDate = (offset = 0) => { const date = new Date(); date.setDate(date.getDate() + offset); return date.toISOString().slice(0, 10); };
const formatDate = (value) => value ? new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`)) : "Chưa đặt";
const priorityLabels = { LOW: "Thấp", MEDIUM: "Trung bình", HIGH: "Cao", URGENT: "Khẩn cấp" };
const statusLabels = { DRAFT: "Bản nháp", ACTIVE: "Đang thực hiện", COMPLETED: "Hoàn thành", CANCELLED: "Đã hủy" };
const unitLabels = { HOURS: "giờ", STORY_POINTS: "điểm" };
const taskStatusByColumnGroup = { TODO: "TODO", IN_PROGRESS: "IN_PROGRESS", IN_REVIEW: "REVIEW", DONE: "DONE" };
const planningSections = new Set(["backlog", "current", "history"]);
const emptyPlan = () => ({ name: "", goal: "", description: "", startDate: isoDate(), endDate: isoDate(6), capacity: 40, estimateUnit: "HOURS" });
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" })[character]);
const orderTasksByHierarchy = (tasks = []) => {
  const visibleIds = new Set(tasks.map((task) => task.id));
  const childrenByParent = new Map();
  tasks.forEach((task) => {
    if (task.parentTaskId && visibleIds.has(task.parentTaskId)) {
      const children = childrenByParent.get(task.parentTaskId) || [];
      children.push(task);
      childrenByParent.set(task.parentTaskId, children);
    }
  });
  const ordered = [];
  tasks.filter((task) => !task.parentTaskId || !visibleIds.has(task.parentTaskId)).forEach((task) => {
    ordered.push(task, ...(childrenByParent.get(task.id) || []));
  });
  const orderedIds = new Set(ordered.map((task) => task.id));
  tasks.forEach((task) => { if (!orderedIds.has(task.id)) ordered.push(task); });
  return ordered;
};

export default function WeeklyPlanningPanel({ projectId, members = [], taskTypes = [], refreshKey = 0, currentUserId, canManage, onAddTaskType, onOpenTask, onDataChanged }) {
  const [section, setSection] = useState(() => {
    const saved = typeof window !== "undefined" ? window.sessionStorage.getItem(`weekly-planning-section:${projectId}`) : null;
    return planningSections.has(saved) ? saved : "current";
  });
  const [unplanned, setUnplanned] = useState([]);
  const [plans, setPlans] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [selectedPlanId, setSelectedPlanId] = useState("");
  const [filters, setFilters] = useState({ search: "", assignee: "ALL", category: "ALL", priority: "ALL", deadline: "ALL", unassigned: false, overdue: false });
  const [historyFilters, setHistoryFilters] = useState({ search: "", status: "ALL", completion: "ALL", dateFrom: "", dateTo: "" });
  const [historyPage, setHistoryPage] = useState(1);
  const historyPageSize = 5;
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [collapsedPlanIds, setCollapsedPlanIds] = useState([]);
  const [backlogCollapsed, setBacklogCollapsed] = useState(false);
  const [dragged, setDragged] = useState(null);
  const [dragOverTarget, setDragOverTarget] = useState("");
  const [pointerPreview, setPointerPreview] = useState(null);
  const draggedRef = useRef(null);
  const pointerGestureRef = useRef(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [showTaskCreate, setShowTaskCreate] = useState(false);
  const [taskCreateContext, setTaskCreateContext] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [planTaskFilter, setPlanTaskFilter] = useState("ALL");
  const [planForm, setPlanForm] = useState(emptyPlan());
  const [formErrors, setFormErrors] = useState({});
  const [completion, setCompletion] = useState(null);
  const [completeForm, setCompleteForm] = useState({ action: "MOVE_TO_UNPLANNED", targetPlanId: "", nextPlanName: "", nextPlanGoal: "", nextPlanStartDate: isoDate(7), nextPlanEndDate: isoDate(13) });

  const load = async (preferredPlanId) => {
    setLoading(true); setError("");
    try {
      const [taskResponse, planResponse] = await Promise.all([taskApi.getUnplannedTasks(projectId), taskApi.getWeeklyPlans(projectId)]);
      const nextPlans = planResponse.data || [];
      setUnplanned(taskResponse.data || []); setPlans(nextPlans);
      setSelectedPlanId((current) => String(preferredPlanId || current || nextPlans.find((p) => p.status === "ACTIVE")?.id || nextPlans.find((p) => p.status === "DRAFT")?.id || nextPlans[0]?.id || ""));
    } catch (requestError) { setError(requestError.response?.data?.message || "Không thể tải dữ liệu lập kế hoạch tuần"); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [projectId, refreshKey]);
  useEffect(() => () => document.body.classList.remove("planning-pointer-dragging"), []);
  useEffect(() => { window.sessionStorage.setItem(`weekly-planning-section:${projectId}`, section); }, [projectId, section]);

  const activePlan = plans.find((plan) => plan.status === "ACTIVE");
  const draftPlans = plans.filter((plan) => plan.status === "DRAFT");
  const historyPlans = plans.filter((plan) => ["COMPLETED", "CANCELLED"].includes(plan.status));
  const visibleHistoryPlans = filterPlanHistory(historyPlans, historyFilters);
  const historyTotalPages = Math.max(1, Math.ceil(visibleHistoryPlans.length / historyPageSize));
  const pagedHistoryPlans = visibleHistoryPlans.slice((historyPage - 1) * historyPageSize, historyPage * historyPageSize);
  const selectedPlan = plans.find((plan) => String(plan.id) === String(selectedPlanId));
  const selectedDraftPlan = selectedPlan?.status === "DRAFT" ? selectedPlan : draftPlans[0];
  const planningTargets = [...(activePlan ? [activePlan] : []), ...draftPlans];
  const selectedPlanningTarget = planningTargets.find((plan) => String(plan.id) === String(selectedPlanId)) || planningTargets[0];
  const draggedTask = dragged ? [...unplanned, ...plans.flatMap((plan) => plan.tasks || [])].find((task) => task.id === dragged.id) : null;
  const categories = useMemo(() => [...new Set(unplanned.map((task) => task.workCategoryName).filter(Boolean))], [unplanned]);
  const filtered = useMemo(() => filterPlanningTasks(unplanned, filters), [unplanned, filters]);
  const pagination = paginatePlanningTasks(orderTasksByHierarchy(filtered), page, pageSize);
  const totalPages = pagination.totalPages;
  const visibleTasks = pagination.items;
  const activeFilterCount = countPlanningFilters(filters);

  useEffect(() => { setPage(1); }, [filters, pageSize]);
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [page, totalPages]);
  useEffect(() => { setHistoryPage(1); }, [historyFilters]);
  useEffect(() => { if (historyPage > historyTotalPages) setHistoryPage(historyTotalPages); }, [historyPage, historyTotalPages]);

  const run = async (action, preferredPlanId) => {
    setBusy(true); setError("");
    try { await action(); setSelectedIds([]); await load(preferredPlanId); await onDataChanged?.(); return true; }
    catch (requestError) { setError(requestError.response?.data?.message || "Không thể thực hiện thao tác"); return false; }
    finally { setBusy(false); }
  };

  const validatePlan = () => {
    const errors = validateWeeklyPlanForm(planForm);
    setFormErrors(errors); return Object.keys(errors).length === 0;
  };

  const savePlan = async (event) => {
    event.preventDefault(); if (!validatePlan()) return;
    let newId;
    const payload = { ...planForm, capacity: Number(planForm.capacity), version: editingPlan?.version };
    const ok = await run(async () => {
      const response = editingPlan ? await taskApi.updateWeeklyPlan(editingPlan.id, payload) : await taskApi.createWeeklyPlan(projectId, payload);
      newId = response.data?.id || editingPlan?.id;
    }, newId);
    if (ok) { setShowCreate(false); setEditingPlan(null); setPlanForm(emptyPlan()); if (!editingPlan) setSection("backlog"); if (newId) setSelectedPlanId(String(newId)); }
  };

  const openPlanEditor = (plan) => {
    setEditingPlan(plan);
    setPlanForm({ name: plan.name, goal: plan.goal || "", description: plan.description || "", startDate: plan.startDate, endDate: plan.endDate, capacity: plan.capacity || 40, estimateUnit: plan.estimateUnit || "HOURS" });
    setFormErrors({}); setShowCreate(true);
  };

  const openTaskCreator = (context = null) => {
    setTaskCreateContext(context);
    setShowTaskCreate(true);
  };

  const openTaskCreatorInPlan = async (plan) => {
    setError("");
    try {
      const response = await boardApi.getColumns(projectId);
      const columns = response.data || [];
      const targetColumn = columns[0];
      if (!targetColumn) throw new Error("Dự án chưa có cột công việc");
      const status = taskStatusByColumnGroup[targetColumn.statusGroup]
        || (["TODO", "IN_PROGRESS", "REVIEW", "DONE"].includes(targetColumn.key) ? targetColumn.key : "TODO");
      openTaskCreator({ planId: plan.id, status, boardColumnId: targetColumn.id, boardColumns: columns });
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Không thể xác định cột để tạo công việc");
    }
  };

  const closeTaskCreator = () => {
    setShowTaskCreate(false);
    setTaskCreateContext(null);
  };

  const createPlanningTask = async (payload, attachmentFile) => {
    const destinationPlanId = taskCreateContext?.planId;
    let createPayload = payload;
    if (destinationPlanId) {
      let columns = taskCreateContext.boardColumns || [];
      let targetColumn = columns.find((column) => String(column.id) === String(taskCreateContext.boardColumnId)) || columns[0];
      if (!targetColumn) {
        const columnResponse = await boardApi.getColumns(projectId);
        columns = columnResponse.data || [];
        targetColumn = columns[0];
      }
      if (!targetColumn) throw new Error("Dự án chưa có cột công việc");
      const status = taskStatusByColumnGroup[targetColumn.statusGroup]
        || (["TODO", "IN_PROGRESS", "REVIEW", "DONE"].includes(targetColumn.key) ? targetColumn.key : "TODO");
      createPayload = { ...payload, status, boardColumnId: targetColumn.id };
    }
    const response = await taskApi.createUnplannedTask(projectId, createPayload);
    const createdTaskId = response.data?.id;
    if (destinationPlanId && createdTaskId) {
      await taskApi.addTasksToWeeklyPlan(destinationPlanId, [createdTaskId], true);
    }
    let attachmentError = "";
    if (attachmentFile && createdTaskId) {
      try {
        await fileApi.uploadTaskFile(createdTaskId, attachmentFile);
      } catch (err) {
        attachmentError = err.response?.data?.message || "Không thể tải tệp đính kèm";
      }
    }
    const preferredPlanId = destinationPlanId || selectedPlanId;
    closeTaskCreator();
    await load(preferredPlanId);
    if (destinationPlanId) setSection("current");
    await onDataChanged?.();
    if (attachmentError) setError(`Công việc đã được tạo nhưng ${attachmentError.toLowerCase()}`);
  };

  const replacePlan = (updatedPlan) => setPlans((current) => current.map((plan) => plan.id === updatedPlan.id ? updatedPlan : plan));

  const endDrag = () => { draggedRef.current = null; setDragged(null); setDragOverTarget(""); };

  const planningDropAt = (clientX, clientY) => document.elementFromPoint(clientX, clientY)?.closest?.("[data-planning-drop]") || null;

  const pointerTargetKey = (target) => {
    if (!target) return "";
    if (target.dataset.planningDrop === "plan") return `plan-${target.dataset.planId}`;
    if (target.dataset.planningDrop === "plan-task") return `plan-task-${target.dataset.taskId}`;
    if (target.dataset.planningDrop === "backlog-task") return `backlog-${target.dataset.taskId}`;
    if (target.dataset.planningDrop === "root") return `root-${target.dataset.planId || target.dataset.location || "task"}`;
    return target.dataset.planningDrop === "backlog" ? "backlog" : "";
  };

  const activatePointerGesture = (gesture, x, y) => {
    if (!gesture || gesture.dragging || pointerGestureRef.current !== gesture) return;
    gesture.dragging = true;
    draggedRef.current = gesture.value;
    setDragged(gesture.value);
    setPointerPreview({ label: gesture.label, x, y });
    document.body.classList.add("planning-pointer-dragging");
  };

  const beginPointerDrag = (event, value, label, onClick) => {
    if (event.button !== 0) return;
    if (event.target.closest?.("[data-no-drag]")) return;
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.setPointerCapture?.(event.pointerId);
    const gesture = { value, label, onClick, startX: event.clientX, startY: event.clientY, dragging: false, timer: null };
    gesture.timer = window.setTimeout(() => activatePointerGesture(gesture, gesture.startX, gesture.startY), 180);
    pointerGestureRef.current = gesture;
  };

  const movePointerDrag = (event) => {
    const gesture = pointerGestureRef.current;
    if (!gesture) return;
    if (!gesture.dragging) {
      const distance = Math.hypot(event.clientX - gesture.startX, event.clientY - gesture.startY);
      if (distance < 5) return;
      window.clearTimeout(gesture.timer);
      activatePointerGesture(gesture, event.clientX, event.clientY);
    }
    event.preventDefault();
    const target = planningDropAt(event.clientX, event.clientY);
    setDragOverTarget(pointerTargetKey(target));
    const dropType = target?.dataset.planningDrop;
    const sameListTask = (gesture.value.source === "backlog" && dropType === "backlog-task")
      || (gesture.value.source === "plan" && dropType === "plan-task" && String(gesture.value.planId) === String(target?.dataset.planId));
    const hint = dropType === "root" ? "Thả để đưa thành task cha"
      : sameListTask && String(gesture.value.id) !== String(target?.dataset.taskId)
      ? "Thả để tạo task con"
      : dropType === "plan" && gesture.value.source === "backlog" ? "Thả vào kế hoạch" : "Đang di chuyển";
    setPointerPreview((current) => current ? { ...current, x: event.clientX, y: event.clientY, hint } : current);
    if (event.clientY < 90) window.scrollBy({ top: -12, behavior: "auto" });
    else if (event.clientY > window.innerHeight - 70) window.scrollBy({ top: 12, behavior: "auto" });
  };

  const clearPointerDrag = (event) => {
    if (event?.currentTarget?.hasPointerCapture?.(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    window.clearTimeout(pointerGestureRef.current?.timer);
    pointerGestureRef.current = null;
    draggedRef.current = null;
    setDragged(null);
    setDragOverTarget("");
    setPointerPreview(null);
    document.body.classList.remove("planning-pointer-dragging");
  };

  const addTasks = async (ids = selectedIds, plan = selectedPlanningTarget) => {
    if (!plan || !["DRAFT", "ACTIVE"].includes(plan.status)) { setError("Hãy chọn một kế hoạch có thể lập lịch"); return; }
    const previousUnplanned = unplanned;
    const previousPlans = plans;
    const movingIds = new Set(ids);
    const movingTasks = unplanned.filter((task) => movingIds.has(task.id));
    setError("");
    setSelectedIds([]);
    setUnplanned((current) => current.filter((task) => !movingIds.has(task.id)));
    setPlans((current) => current.map((item) => item.id === plan.id ? { ...item, tasks: [...(item.tasks || []), ...movingTasks], totalTasks: Number(item.totalTasks || 0) + movingTasks.length } : item));
    try {
      const response = await taskApi.addTasksToWeeklyPlan(plan.id, ids, true);
      const updatedPlan = response.data;
      replacePlan(updatedPlan);
      const plannedIds = new Set((updatedPlan.tasks || []).map((task) => task.id));
      setUnplanned((current) => current.filter((task) => !plannedIds.has(task.id)));
      onDataChanged?.();
      return true;
    } catch (requestError) {
      setUnplanned(previousUnplanned);
      setPlans(previousPlans);
      setError(requestError.response?.data?.message || "Không thể thêm công việc vào kế hoạch");
      return false;
    }
  };

  const reorderPlanTask = async (targetId, plan, source = dragged) => {
    if (source?.source !== "plan" || String(source.planId) !== String(plan?.id) || source.id === targetId) return;
    const next = [...plan.tasks]; const from = next.findIndex((task) => task.id === source.id); const to = next.findIndex((task) => task.id === targetId);
    if (from < 0 || to < 0) return;
    const [moved] = next.splice(from, 1); next.splice(to, 0, moved); setDragged(null);
    setDragOverTarget("");
    const previousPlans = plans;
    setPlans((current) => current.map((item) => item.id === plan.id ? { ...item, tasks: next } : item));
    try {
      const response = await taskApi.reorderWeeklyPlanTasks(plan.id, next.map((task) => task.id));
      replacePlan(response.data);
    } catch (requestError) {
      setPlans(previousPlans);
      setError(requestError.response?.data?.message || "Không thể sắp xếp công việc trong kế hoạch");
    }
  };

  const reorderBacklogTask = async (targetId, source = dragged) => {
    if (source?.source !== "backlog" || source.id === targetId) return;
    const next = [...unplanned];
    const from = next.findIndex((task) => task.id === source.id);
    const to = next.findIndex((task) => task.id === targetId);
    if (from < 0 || to < 0) return;
    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);
    endDrag();
    setUnplanned(next);
    try {
      const response = await taskApi.reorderUnplannedTasks(projectId, next.map((task) => task.id));
      setUnplanned(response.data || next);
    } catch (requestError) {
      setUnplanned(unplanned);
      setError(requestError.response?.data?.message || "Không thể thay đổi thứ tự ưu tiên");
    }
  };

  const taskUpdatePayload = (task, parentTaskId) => ({
    title: task.title,
    description: task.description || "",
    assignedToUserId: task.assignedTo?.id || null,
    boardColumnId: task.boardColumnId || null,
    parentTaskId,
    status: task.status || "TODO",
    priority: task.priority || "MEDIUM",
    type: task.type || task.workCategoryName || "TASK",
    workCategoryId: task.workCategoryId || null,
    dueDate: task.dueDate || null,
    estimatedEffort: Number(task.estimatedEffort || 0),
    actualEffort: task.actualEffort == null ? null : Number(task.actualEffort),
    reviewRequired: Boolean(task.reviewRequired),
    version: task.version
  });

  const makeTaskChild = async (source, parentId) => {
    if (!source || source.id === parentId) return;
    const allTasks = [...unplanned, ...plans.flatMap((plan) => plan.tasks || [])];
    const sourceTask = allTasks.find((task) => task.id === source.id);
    const parentTask = allTasks.find((task) => task.id === parentId);
    if (!sourceTask || !parentTask) return;
    if (sourceTask.parentTaskId === parentId) return;
    if (parentTask.parentTaskId) { setError("Task con không thể chứa thêm task con"); return; }
    if (sourceTask.subtaskCount > 0 || allTasks.some((task) => task.parentTaskId === sourceTask.id)) {
      setError("Task đang có task con không thể chuyển thành task con"); return;
    }

    const previousUnplanned = unplanned;
    const previousPlans = plans;
    const optimistic = { ...sourceTask, parentTaskId: parentId, parentTaskTitle: parentTask.title };
    const updateList = (tasks = []) => orderTasksByHierarchy(tasks.map((task) => task.id === source.id ? optimistic : task));
    setUnplanned((current) => updateList(current));
    setPlans((current) => current.map((plan) => ({ ...plan, tasks: updateList(plan.tasks) })));
    setError("");
    try {
      const response = await taskApi.updateTask(source.id, taskUpdatePayload(sourceTask, parentId));
      const updated = response.data;
      const applyUpdated = (tasks = []) => orderTasksByHierarchy(tasks.map((task) => task.id === source.id ? updated : task));
      setUnplanned((current) => applyUpdated(current));
      setPlans((current) => current.map((plan) => ({ ...plan, tasks: applyUpdated(plan.tasks) })));
      onDataChanged?.();
    } catch (requestError) {
      setUnplanned(previousUnplanned);
      setPlans(previousPlans);
      setError(requestError.response?.data?.message || "Không thể chuyển công việc thành task con");
    }
  };

  const makeTaskRoot = async (source) => {
    if (!source) return;
    const allTasks = [...unplanned, ...plans.flatMap((plan) => plan.tasks || [])];
    const sourceTask = allTasks.find((task) => task.id === source.id);
    if (!sourceTask?.parentTaskId) return;
    const previousUnplanned = unplanned;
    const previousPlans = plans;
    const optimistic = { ...sourceTask, parentTaskId: null, parentTaskTitle: null };
    const updateList = (tasks = []) => orderTasksByHierarchy(tasks.map((task) => task.id === source.id ? optimistic : task));
    setUnplanned((current) => updateList(current));
    setPlans((current) => current.map((plan) => ({ ...plan, tasks: updateList(plan.tasks) })));
    setError("");
    try {
      const response = await taskApi.updateTask(source.id, taskUpdatePayload(sourceTask, null));
      const updated = response.data;
      const applyUpdated = (tasks = []) => orderTasksByHierarchy(tasks.map((task) => task.id === source.id ? updated : task));
      setUnplanned((current) => applyUpdated(current));
      setPlans((current) => current.map((plan) => ({ ...plan, tasks: applyUpdated(plan.tasks) })));
      onDataChanged?.();
    } catch (requestError) {
      setUnplanned(previousUnplanned);
      setPlans(previousPlans);
      setError(requestError.response?.data?.message || "Không thể đưa task con thành task cha");
    }
  };

  const finishPointerDrag = (event) => {
    const gesture = pointerGestureRef.current;
    if (!gesture) return;
    if (!gesture.dragging) {
      const onClick = gesture.onClick;
      clearPointerDrag(event);
      onClick?.();
      return;
    }
    const source = gesture.value;
    event.preventDefault();
    event.stopPropagation();
    const target = planningDropAt(event.clientX, event.clientY);
    const dropType = target?.dataset.planningDrop;
    const targetPlanId = target?.dataset.planId;
    const targetTaskId = target?.dataset.taskId;

    if (dropType === "root") {
      makeTaskRoot(source);
    } else if (source.source === "backlog" && dropType === "plan") {
      const targetPlan = plans.find((plan) => String(plan.id) === String(targetPlanId));
      if (targetPlan) addTasks([source.id], targetPlan);
    } else if (source.source === "backlog" && dropType === "backlog-task") {
      makeTaskChild(source, Number(targetTaskId));
    } else if (source.source === "backlog" && dropType === "plan-task") {
      setError("Hãy đưa task vào kế hoạch trước, sau đó kéo task lên task cha trong cùng kế hoạch");
    } else if (source.source === "plan" && dropType === "plan-task" && String(source.planId) === String(targetPlanId)) {
      makeTaskChild(source, Number(targetTaskId));
    } else if (source.source === "plan" && ["backlog", "backlog-task"].includes(dropType)) {
      run(() => taskApi.removeTaskFromWeeklyPlan(source.planId, source.id, true), source.planId);
    }
    clearPointerDrag(event);
  };

  const cancelPointerDrag = (event) => clearPointerDrag(event);

  const startPlan = async (plan) => {
    if (!plan.totalTasks) { setError("Kế hoạch phải có ít nhất một công việc"); return; }
    const overloaded = requiresOverloadConfirmation(plan);
    const unassigned = plan.tasks?.filter((task) => !task.assignedTo).length || 0;
    const noDeadline = plan.tasks?.filter((task) => !task.dueDate).length || 0;
    const unestimated = plan.tasks?.filter((task) => !Number(task.estimatedEffort)).length || 0;
    const warnings = [unassigned && `${unassigned} task chưa giao người`, noDeadline && `${noDeadline} task chưa có deadline`, unestimated && `${unestimated} task chưa ước tính`, overloaded && "thành viên hoặc kế hoạch đang quá tải"].filter(Boolean);
    const message = warnings.length ? `Kế hoạch còn các vấn đề:\n- ${warnings.join("\n- ")}\n\nBạn xác nhận vẫn bắt đầu?` : "Bắt đầu kế hoạch này? Sau khi bắt đầu không thể sửa danh sách task.";
    if (!window.confirm(message)) return;
    await run(() => taskApi.startWeeklyPlan(plan.id, { confirmOverload: overloaded, version: plan.version }), plan.id);
  };

  const openCompletion = async (plan) => {
    setBusy(true);
    try { const response = await taskApi.getWeeklyPlanCompletion(plan.id); setCompletion({ ...response.data, sourcePlan: plan }); }
    catch (requestError) { setError(requestError.response?.data?.message || "Không thể tổng hợp kế hoạch"); }
    finally { setBusy(false); }
  };

  const completePlan = async () => {
    if (!window.confirm("Xác nhận kết thúc và lưu kết quả kế hoạch?")) return;
    const ok = await run(() => taskApi.completeWeeklyPlan(completion.sourcePlan.id, { ...completeForm, targetPlanId: completeForm.targetPlanId ? Number(completeForm.targetPlanId) : null, version: completion.sourcePlan.version }));
    if (ok) { setCompletion(null); setSection("history"); }
  };

  const PlanCard = ({ plan, detailMode = false }) => {
    if (!plan) return <div className="planning-empty"><CalendarDays size={32} /><p>Chưa có kế hoạch phù hợp.</p></div>;
    const unit = unitLabels[plan.estimateUnit] || "giờ";
    const planTasks = orderTasksByHierarchy(plan.tasks || []).filter((task) => planTaskFilter === "ALL"
      || (planTaskFilter === "UNESTIMATED" && !Number(task.estimatedEffort))
      || (planTaskFilter === "UNASSIGNED" && !task.assignedTo)
      || (planTaskFilter === "NO_DEADLINE" && !task.dueDate));
    const canStart = plan.totalTasks > 0;
    return <div data-planning-drop="plan" data-plan-id={plan.id} className={`weekly-plan-detail${detailMode ? " weekly-plan-detail-page" : ""}`}>
      <div className="plan-card-heading"><div><div className="weekly-plan-status"><span className={plan.status.toLowerCase()}>{statusLabels[plan.status]}</span><small>{formatDate(plan.startDate)} – {formatDate(plan.endDate)}</small></div><h3>{plan.name}</h3>{detailMode && <p>{plan.description || "Chưa có mô tả kế hoạch."}</p>}</div>{detailMode && canManage && plan.status === "DRAFT" && <button className="plan-edit-button" title="Chỉnh sửa thông tin kế hoạch" onClick={() => openPlanEditor(plan)}><Pencil size={15} /></button>}</div>
      {!detailMode && <p>{plan.goal || "Chưa đặt mục tiêu tuần"}</p>}
      <div className="plan-summary-metrics"><section className="planning-metric"><header><span>Tiến độ công việc</span><b>{plan.completedTasks}/{plan.totalTasks} task</b></header><i><span className="progress" style={{ width: `${plan.totalTasks ? plan.completedTasks / plan.totalTasks * 100 : 0}%` }} /></i><small>{plan.totalTasks ? `${Math.round(plan.completedTasks / plan.totalTasks * 100)}% hoàn thành` : "Chưa có công việc"}</small></section>
      <section className="planning-metric planning-capacity"><header><span>Sức chứa kế hoạch</span><b>{formatCapacitySummary(plan, unit)}</b></header>{plan.capacity ? <><i><span className={plan.overloaded ? "overloaded" : ""} style={{ width: `${Math.min(100, plan.utilizationPercent || 0)}%` }} /></i><small>{Math.round(plan.utilizationPercent || 0)}% sức chứa đã sử dụng</small></> : <div className="capacity-empty"><strong>Chưa thiết lập</strong>{canManage && plan.status === "DRAFT" && <button onClick={() => openPlanEditor(plan)}>Thiết lập sức chứa</button>}</div>}</section>
      {detailMode && <section className="planning-metric plan-member-summary"><header><span>Tải thành viên</span><b>{plan.memberWorkloads?.length || 0} người</b></header><div>{plan.memberWorkloads?.slice(0, 4).map((item) => <span key={item.member?.id || item.label}><b className="member-mini-avatar">{item.label?.[0] || "?"}</b>{item.label}<small>{item.allocatedEffort}/{item.capacityShare || 0} {unit}</small></span>)}</div></section>}</div>
      {plan.overloaded && <div className="planning-warning"><AlertTriangle size={15} /> Khối lượng vượt sức chứa dự kiến</div>}
      {!detailMode && <div className="member-workloads">{plan.memberWorkloads?.map((item) => <div key={item.member?.id || item.label}><span><UserRound size={14} />{item.label}</span><b className={item.overloaded ? "overloaded" : ""}>{item.allocatedEffort} {unit}{item.overloaded ? " · Quá tải" : ""}</b></div>)}</div>}
      {detailMode && <div className="plan-weekly-goal"><Target size={17} /><div><strong>Mục tiêu tuần</strong><span>{plan.goal || "Chưa đặt mục tiêu tuần"}</span></div></div>}
      {planTaskFilter !== "ALL" && <div className="plan-task-filter"><span>Đang lọc công việc cần xử lý</span><button onClick={() => setPlanTaskFilter("ALL")}>Hiện tất cả</button></div>}
      {detailMode && <div className="plan-task-table-head"><span>Công việc</span><span>Người thực hiện</span><span>Deadline</span><span>Ưu tiên</span><span>Ước tính</span></div>}
      <div className="weekly-plan-tasks">{planTasks.map((task) => { const movable = canManage && ["DRAFT", "ACTIVE"].includes(plan.status); return <div data-planning-drop="plan-task" data-plan-id={plan.id} data-task-id={task.id} key={task.id} className={`${detailMode ? "detail-task-row " : ""}${task.parentTaskId ? "is-subtask " : ""}${movable ? "jira-draggable-row " : ""}${dragged?.source === "plan" && dragged.id === task.id ? "is-dragging" : ""}`} onPointerDown={(event) => movable && beginPointerDrag(event, { id: task.id, source: "plan", planId: plan.id }, task.title, () => onOpenTask?.(task))} onPointerMove={movePointerDrag} onPointerUp={finishPointerDrag} onPointerCancel={cancelPointerDrag}>
        <span className="jira-drag-handle" title="Giữ và kéo để sắp xếp"><GripVertical size={15} /></span>{detailMode ? <><div className="detail-task-name"><button className="plan-task-title" onClick={(event) => { if (movable) event.preventDefault(); else onOpenTask?.(task); }}>{task.title}</button><small>CV-{task.id} · <span>{task.workCategoryName || task.type}</span></small></div><div className="detail-task-assignee"><span className="plan-task-avatar">{task.assignedTo?.fullName?.trim()?.[0]?.toUpperCase() || "?"}</span>{task.assignedTo?.fullName || "Chưa giao"}</div><span>{formatDate(task.dueDate)}</span><em className={`planning-priority ${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority]}</em><b>{Number(task.estimatedEffort) ? `${task.estimatedEffort} ${unit}` : "Chưa ước tính"}</b></> : <><span className="plan-task-avatar" title={task.assignedTo?.fullName || "Chưa giao"}>{task.assignedTo?.fullName?.trim()?.[0]?.toUpperCase() || "?"}</span><div><button className="plan-task-title" onClick={(event) => { if (movable) event.preventDefault(); else onOpenTask?.(task); }}>{task.title}</button><small>{task.assignedTo?.fullName || "Chưa giao"} · {formatDate(task.dueDate)} · <em className={`planning-priority ${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority]}</em> · {Number(task.estimatedEffort) ? `${task.estimatedEffort} ${unit}` : "Chưa ước tính"}</small></div></>}
        {movable && <button data-no-drag className="return-to-backlog" onPointerDown={(event) => event.stopPropagation()} title="Loại khỏi kế hoạch và đưa task về Danh sách chờ" aria-label={`Đưa ${task.title} về danh sách chờ`} onClick={() => run(() => taskApi.removeTaskFromWeeklyPlan(plan.id, task.id, true), plan.id)}><Undo2 size={14} /><span>Đưa về danh sách chờ</span></button>}
      </div>; })}{!planTasks.length && <div className="planning-drop-empty">{plan.tasks?.length ? "Không có công việc phù hợp bộ lọc." : "Kéo công việc vào đây hoặc chọn nhiều công việc bên trái."}</div>}</div>
      {canManage && <div className="weekly-plan-actions">
        {plan.status === "DRAFT" && <button className="planning-secondary" onClick={() => { setSelectedPlanId(String(plan.id)); setSection("backlog"); }}>Tiếp tục lập kế hoạch</button>}
        {plan.status === "DRAFT" && <button className="planning-primary" disabled={busy || !canStart} title={!canStart ? "Cần ít nhất một công việc" : "Bắt đầu kế hoạch"} onClick={() => startPlan(plan)}>Bắt đầu kế hoạch <ChevronRight size={16} /></button>}
        {plan.status === "ACTIVE" && <button className="planning-primary" disabled={busy} onClick={() => openCompletion(plan)}>Kết thúc kế hoạch</button>}
        <details className="planning-more"><summary><MoreHorizontal size={18} /></summary><div>
          {plan.status === "DRAFT" && <button onClick={() => openPlanEditor(plan)}><Pencil size={14} /> Chỉnh sửa</button>}
          <button onClick={() => run(() => taskApi.cloneWeeklyPlan(plan.id))}><Copy size={14} /> Nhân bản</button>
          {["DRAFT", "ACTIVE"].includes(plan.status) && <button onClick={() => window.confirm("Hủy kế hoạch và đưa các task về danh sách chờ?") && run(() => taskApi.cancelWeeklyPlan(plan.id))}><X size={14} /> Hủy kế hoạch</button>}
          {plan.status === "DRAFT" && <button className="danger" onClick={() => window.confirm("Xóa bản nháp này?") && run(() => taskApi.deleteWeeklyPlan(plan.id))}><Trash2 size={14} /> Xóa</button>}
        </div></details>
      </div>}
    </div>;
  };

  const PlanReadiness = ({ plan }) => {
    if (!plan) return null;
    const tasks = orderTasksByHierarchy(plan.tasks || []);
    const checks = [
      { ok: tasks.length > 0, text: tasks.length > 0 ? `Đã có ${tasks.length} công việc` : "Kế hoạch chưa có công việc" },
      { ok: Number(plan.capacity) > 0, text: Number(plan.capacity) > 0 ? "Đã thiết lập sức chứa" : "Chưa thiết lập sức chứa" },
      { ok: !tasks.some((task) => !task.assignedTo), text: tasks.some((task) => !task.assignedTo) ? `${tasks.filter((task) => !task.assignedTo).length} công việc chưa có người thực hiện` : "Tất cả công việc đã được giao" },
      { ok: !tasks.some((task) => !task.dueDate), text: tasks.some((task) => !task.dueDate) ? `${tasks.filter((task) => !task.dueDate).length} công việc chưa có deadline` : "Tất cả công việc đã có deadline" },
      { ok: !tasks.some((task) => !Number(task.estimatedEffort)), text: tasks.some((task) => !Number(task.estimatedEffort)) ? `${tasks.filter((task) => !Number(task.estimatedEffort)).length} công việc chưa ước tính` : "Tất cả công việc đã được ước tính" }
    ];
    const unit = unitLabels[plan.estimateUnit] || "giờ";
    return <aside className="plan-readiness-side"><section><h3>Mức độ sẵn sàng</h3><div className="readiness-checks">{checks.map((item) => <div className={item.ok ? "ready" : "attention"} key={item.text}>{item.ok ? <CheckCircle2 size={16} /> : <AlertTriangle size={16} />}<span>{item.text}</span></div>)}</div></section><section><h3>Thành viên</h3><div className="readiness-members">{plan.memberWorkloads?.map((item) => <div key={item.member?.id || item.label}><header><span className="plan-task-avatar">{item.label?.[0] || "?"}</span><strong>{item.label}</strong><small>{item.allocatedEffort}/{item.capacityShare || 0} {unit}</small></header><i><span className={item.overloaded ? "overloaded" : ""} style={{ width: `${Math.min(100, item.utilizationPercent || 0)}%` }} /></i><small>{Math.round(item.utilizationPercent || 0)}%{item.overloaded ? " · Quá tải" : ""}</small></div>)}{!plan.memberWorkloads?.length && <p>Chưa có dữ liệu phân bổ thành viên.</p>}</div></section><div className="readiness-note">Bạn vẫn có thể bắt đầu nhưng cần xác nhận các cảnh báo.</div></aside>;
  };

  const CurrentSprintView = () => {
    const plan = activePlan;
    if (!plan) {
      const draft = selectedDraftPlan || draftPlans[0];
      return <div className="jira-sprint-view jira-sprint-empty-view">
        <header className="jira-sprint-heading"><div><h2>Kế hoạch tuần</h2></div></header>
        <section className="jira-no-active-sprint">
          <span className="jira-empty-sprint-icon"><PlayCircle size={30} /></span>
          <h3>Chưa có kế hoạch tuần đang chạy</h3>
          <p>Chuẩn bị công việc trong Danh sách chờ, sau đó bắt đầu kế hoạch để làm việc trên board giống Active Sprint của Jira.</p>
          {draft && <article className="jira-ready-draft"><div><span className="jira-sprint-status draft">Bản nháp</span><strong>{draft.name}</strong><small>{formatDate(draft.startDate)} – {formatDate(draft.endDate)} · {draft.totalTasks || draft.tasks?.length || 0} công việc</small></div><div><button className="planning-secondary" type="button" onClick={() => { setSelectedPlanId(String(draft.id)); setSection("backlog"); }}>Mở danh sách chờ</button>{canManage && <button className="planning-primary" type="button" disabled={busy || !draft.tasks?.length} onClick={() => startPlan(draft)}>Bắt đầu kế hoạch</button>}</div></article>}
          {!draft && canManage && <button className="planning-primary" type="button" onClick={() => { setEditingPlan(null); setPlanForm(emptyPlan()); setShowCreate(true); }}>Tạo kế hoạch tuần</button>}
          {!draft && !canManage && <button className="planning-secondary" type="button" onClick={() => setSection("backlog")}>Mở danh sách chờ</button>}
        </section>
      </div>;
    }

    const tasks = plan.tasks || [];
    const taskGroup = (task) => task.statusGroup || ({ TODO: "TODO", IN_PROGRESS: "IN_PROGRESS", REVIEW: "IN_REVIEW", DONE: "DONE" }[task.status]) || "TODO";
    const counts = tasks.reduce((result, task) => ({ ...result, [taskGroup(task)]: (result[taskGroup(task)] || 0) + 1 }), { TODO: 0, IN_PROGRESS: 0, IN_REVIEW: 0, DONE: 0 });
    const completedTasks = counts.DONE || 0;
    const remainingTasks = Math.max(tasks.length - completedTasks, 0);
    const completionRate = tasks.length ? Math.round(completedTasks / tasks.length * 100) : 0;
    const participants = Array.from(new Map(tasks.filter((task) => task.assignedTo?.id).map((task) => [task.assignedTo.id, task.assignedTo])).values());

    return <div className="jira-sprint-view jira-sprint-board-page">
      <section className={`jira-sprint-summary ${canManage ? "has-actions" : "no-actions"}`}>
        <div className="jira-sprint-goal"><Target size={17} /><div><div className="jira-sprint-title"><h2>{plan.name}</h2><span className="jira-sprint-status active">Đang thực hiện</span></div><p className="jira-sprint-period">{formatDate(plan.startDate)} – {formatDate(plan.endDate)} · {tasks.length} công việc</p><span className="jira-sprint-goal-text">{plan.goal || "Chưa đặt mục tiêu cho kế hoạch này"}</span></div></div>
        <div className={`jira-sprint-progress${tasks.length > 0 && completionRate === 100 ? " is-complete" : ""}`}>
          <header><span>Tiến độ hoàn thành</span><strong>{completionRate}%</strong></header>
          <div
            aria-label={`Đã hoàn thành ${completedTasks} trên ${tasks.length} công việc`}
            aria-valuemax="100"
            aria-valuemin="0"
            aria-valuenow={completionRate}
            className="jira-sprint-progress-track"
            role="progressbar"
          >
            <span style={{ width: `${completionRate}%` }} />
          </div>
          <footer>
            <small><b>{completedTasks}</b>/{tasks.length} đã hoàn thành</small>
            <small>{tasks.length ? `${remainingTasks} còn lại` : "Chưa có công việc"}</small>
          </footer>
        </div>
        <div className="jira-sprint-people"><span>Thành viên</span><div>{participants.slice(0, 5).map((member) => <b key={member.id} title={member.fullName}>{member.fullName?.trim()?.[0]?.toUpperCase() || "?"}</b>)}{participants.length > 5 && <b>+{participants.length - 5}</b>}{!participants.length && <small>Chưa giao</small>}</div></div>
        {canManage && <div className="jira-sprint-summary-actions"><div className="jira-sprint-actions"><details className="planning-more"><summary><MoreHorizontal size={18} /></summary><div><button type="button" disabled={busy} onClick={() => openCompletion(plan)}><Check size={14} /> Hoàn thành kế hoạch</button><button type="button" onClick={() => run(() => taskApi.cloneWeeklyPlan(plan.id))}><Copy size={14} /> Nhân bản</button><button className="danger" type="button" onClick={() => window.confirm("Hủy kế hoạch và đưa các task về danh sách chờ?") && run(() => taskApi.cancelWeeklyPlan(plan.id))}><X size={14} /> Hủy kế hoạch</button></div></details></div></div>}
      </section>

      {plan.description && <details className="jira-sprint-description"><summary>Thông tin kế hoạch</summary><p>{plan.description}</p></details>}
      <KanbanBoard
        projectId={projectId}
        tasks={tasks}
        members={members}
        taskTypes={taskTypes}
        currentUserId={currentUserId}
        canManageTasks={canManage}
        weeklyPlanningMode
        externalFilter={planTaskFilter}
        onExternalFilterClear={() => setPlanTaskFilter("ALL")}
        onOpenTask={onOpenTask}
        onCreateTask={(status, boardColumnId) => openTaskCreator({ planId: plan.id, status, boardColumnId })}
        onBoardChanged={async () => { await load(plan.id); await onDataChanged?.(); }}
      />
    </div>;
  };

  const BacklogPlanSection = ({ plan }) => {
    const collapsed = collapsedPlanIds.includes(plan.id);
    const editable = canManage && ["DRAFT", "ACTIVE"].includes(plan.status);
    const unit = unitLabels[plan.estimateUnit] || "giờ";
    const tasks = plan.tasks || [];
    const allocated = plan.allocatedEffort || tasks.reduce((total, task) => total + Number(task.estimatedEffort || 0), 0);
    const toggleCollapsed = () => setCollapsedPlanIds((ids) => ids.includes(plan.id) ? ids.filter((id) => id !== plan.id) : [...ids, plan.id]);
    return <section key={plan.id} data-planning-drop="plan" data-plan-id={plan.id} className={`jira-plan-section ${plan.status === "ACTIVE" ? "is-active" : ""} ${dragOverTarget === `plan-${plan.id}` ? "is-drop-target" : ""}`}>
      <header className="jira-plan-header">
        <button className="jira-collapse" type="button" onClick={toggleCollapsed} aria-label={collapsed ? "Mở kế hoạch" : "Thu gọn kế hoạch"}>{collapsed ? <ChevronRight size={18} /> : <ChevronDown size={18} />}</button>
        <button className="jira-plan-title" type="button" onClick={() => { setSelectedPlanId(String(plan.id)); setSection("current"); }}><strong>{plan.name}</strong><span className={plan.status.toLowerCase()}>{statusLabels[plan.status]}</span></button>
        <span className="jira-plan-dates">{formatDate(plan.startDate)} – {formatDate(plan.endDate)}</span>
        <span className={`jira-plan-capacity ${plan.overloaded ? "overloaded" : ""}`}>{tasks.length} công việc · {allocated}/{plan.capacity || 0} {unit}</span>
        <div className="jira-plan-actions">
          {plan.status === "DRAFT" && editable && <button className="planning-primary" type="button" disabled={busy || !tasks.length} onClick={() => startPlan(plan)}>Bắt đầu</button>}
          {plan.status === "ACTIVE" && <button className="planning-secondary" type="button" onClick={() => { setSelectedPlanId(String(plan.id)); setSection("current"); }}>Xem kế hoạch</button>}
          {plan.status === "DRAFT" && editable && <details className="planning-more"><summary><MoreHorizontal size={18} /></summary><div><button type="button" onClick={() => openPlanEditor(plan)}><Pencil size={14} /> Chỉnh sửa</button><button className="danger" type="button" onClick={() => window.confirm("Xóa bản nháp này?") && run(() => taskApi.deleteWeeklyPlan(plan.id))}><Trash2 size={14} /> Xóa</button></div></details>}
        </div>
      </header>
      {dragged?.source === "plan" && String(dragged.planId) === String(plan.id) && draggedTask?.parentTaskId && <div
        className={`jira-root-drop-zone ${dragOverTarget === `root-${plan.id}` ? "is-active" : ""}`}
        data-planning-drop="root"
        data-plan-id={plan.id}
      ><Undo2 size={16} /><span>Thả vào đây để đưa thành task cha</span></div>}
      {!collapsed && <>
        {plan.goal && <div className="jira-plan-goal"><Target size={14} /><span>{plan.goal}</span></div>}
        <div className="jira-plan-list">
          {tasks.map((task) => <article data-planning-drop="plan-task" data-plan-id={plan.id} data-task-id={task.id} className={`${task.parentTaskId ? "is-subtask " : ""}${editable ? "jira-draggable-row " : ""}${dragged?.source === "plan" && dragged.id === task.id ? "is-dragging " : ""}${dragOverTarget === `plan-task-${task.id}` ? "is-drop-target" : ""}`} key={task.id} onPointerDown={(event) => editable && beginPointerDrag(event, { id: task.id, source: "plan", planId: plan.id }, task.title, () => onOpenTask?.(task))} onPointerMove={movePointerDrag} onPointerUp={finishPointerDrag} onPointerCancel={cancelPointerDrag}>
            <span className="jira-drag-handle" title="Giữ và kéo để sắp xếp"><GripVertical size={15} /></span>
            <span className="jira-task-type">{task.workCategoryName?.slice(0, 1)?.toUpperCase() || "T"}</span>
            <button className="jira-task-name" type="button" onClick={(event) => { if (editable) event.preventDefault(); else onOpenTask?.(task); }}><strong>{task.title}</strong><small>{task.parentTaskId ? `Task con · CV-${task.id}` : `CV-${task.id}${task.subtaskCount ? ` · ${task.subtaskCount} task con` : ""}`}</small></button>
            <em className={`planning-priority ${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority]}</em>
            <span className="jira-task-assignee" title={task.assignedTo?.fullName || "Chưa giao"}>{task.assignedTo?.fullName?.trim()?.[0]?.toUpperCase() || "?"}</span>
            <span className="jira-task-date">{formatDate(task.dueDate)}</span>
            <b className="jira-task-effort">{Number(task.estimatedEffort) ? `${task.estimatedEffort} ${unit}` : "—"}</b>
            {editable && <button data-no-drag className="jira-row-action" type="button" onPointerDown={(event) => event.stopPropagation()} title="Đưa về danh sách chờ" onClick={() => run(() => taskApi.removeTaskFromWeeklyPlan(plan.id, task.id, true), plan.id)}><Undo2 size={15} /></button>}
          </article>)}
          {!tasks.length && <div className="jira-plan-empty"><ListTodo size={18} /><span>Kéo công việc từ danh sách chờ vào đây</span></div>}
        </div>
      </>}
    </section>;
  };

  const Backlog = () => <>
    <header className="weekly-planning-header"><div><span>Lập kế hoạch tuần / Danh sách chờ</span><h2>Danh sách chờ</h2></div><div className="weekly-planning-header-actions"><button className="planning-secondary" onClick={() => openTaskCreator()}><Plus size={17} /> Tạo công việc</button>{canManage && <button className="planning-primary" onClick={() => { setEditingPlan(null); setPlanForm(emptyPlan()); setShowCreate(true); }}><Plus size={17} /> Tạo kế hoạch tuần</button>}</div></header>
    <div className="jira-backlog-page">
      <div className="jira-backlog-toolbar"><label><Search size={16} /><input value={filters.search} onChange={(e) => setFilters({ ...filters, search: e.target.value })} placeholder="Tìm kiếm công việc" /></label><select value={filters.assignee} onChange={(e) => setFilters({ ...filters, assignee: e.target.value })}><option value="ALL">Tất cả người thực hiện</option>{members.map((m) => <option key={m.userId || m.id} value={m.userId || m.id}>{m.fullName}</option>)}</select><button className={showAdvancedFilters ? "active" : ""} type="button" onClick={() => setShowAdvancedFilters((current) => !current)}><SlidersHorizontal size={15} /> Bộ lọc {activeFilterCount > 0 && <b>{activeFilterCount}</b>}</button></div>
      {showAdvancedFilters && <div className="planning-advanced-filters jira-advanced-filters"><select value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value })}><option value="ALL">Mọi lĩnh vực</option>{categories.map((item) => <option key={item}>{item}</option>)}</select><select value={filters.priority} onChange={(e) => setFilters({ ...filters, priority: e.target.value })}><option value="ALL">Mọi ưu tiên</option>{Object.entries(priorityLabels).map(([key, value]) => <option key={key} value={key}>{value}</option>)}</select><select value={filters.deadline} onChange={(e) => setFilters({ ...filters, deadline: e.target.value })}><option value="ALL">Mọi deadline</option><option value="HAS">Có deadline</option><option value="NONE">Chưa có deadline</option></select><label><input type="checkbox" checked={filters.unassigned} onChange={(e) => setFilters({ ...filters, unassigned: e.target.checked })} /> Chưa giao người</label><label><input type="checkbox" checked={filters.overdue} onChange={(e) => setFilters({ ...filters, overdue: e.target.checked })} /> Đã quá hạn</label>{activeFilterCount > 0 && <button onClick={() => setFilters({ search: filters.search, assignee: "ALL", category: "ALL", priority: "ALL", deadline: "ALL", unassigned: false, overdue: false })}>Xóa bộ lọc</button>}</div>}
      <div className="jira-plan-stack">{activePlan && BacklogPlanSection({ plan: activePlan })}{draftPlans.map((plan) => BacklogPlanSection({ plan }))}{!activePlan && !draftPlans.length && <div className="jira-no-plan"><CalendarDays size={22} /><span>Chưa có kế hoạch tuần. Tạo một kế hoạch rồi kéo công việc vào.</span>{canManage && <button type="button" onClick={() => { setEditingPlan(null); setPlanForm(emptyPlan()); setShowCreate(true); }}>Tạo kế hoạch</button>}</div>}</div>
      <section data-planning-drop="backlog" className={`jira-backlog-section ${backlogCollapsed ? "is-collapsed" : ""}`}>
        <header className="jira-collapsible-header" onClick={() => setBacklogCollapsed((current) => !current)}><button className="jira-collapse" type="button" onClick={(event) => { event.stopPropagation(); setBacklogCollapsed((current) => !current); }} aria-expanded={!backlogCollapsed} aria-label={backlogCollapsed ? "Mở danh sách chờ" : "Thu gọn danh sách chờ"}>{backlogCollapsed ? <ChevronRight size={18} /> : <ChevronDown size={18} />}</button><div><strong>Danh sách chờ</strong><span>{filtered.length} công việc</span></div><small>Kéo để ưu tiên hoặc đưa vào kế hoạch phía trên</small></header>
        {dragged?.source === "backlog" && draggedTask?.parentTaskId && <div
          className={`jira-root-drop-zone ${dragOverTarget === "root-backlog" ? "is-active" : ""}`}
          data-planning-drop="root"
          data-location="backlog"
        ><Undo2 size={16} /><span>Thả vào đây để đưa thành task cha</span></div>}
        {!backlogCollapsed && <>{canManage && selectedIds.length > 0 && <div className="unplanned-selection jira-selection"><strong>Đã chọn {selectedIds.length} công việc</strong><select value={selectedPlanningTarget?.id || ""} onChange={(event) => setSelectedPlanId(event.target.value)}><option value="">Chọn kế hoạch</option>{planningTargets.map((plan) => <option key={plan.id} value={plan.id}>{plan.name} · {statusLabels[plan.status]}</option>)}</select><button type="button" disabled={!selectedPlanningTarget} onClick={() => addTasks()}>Thêm vào kế hoạch</button><button type="button" onClick={() => setSelectedIds([])}>Bỏ chọn</button></div>}
        <div className="jira-backlog-list">{loading && [...Array(5)].map((_, i) => <div className="planning-skeleton" key={i} />)}{!loading && !visibleTasks.length && <div className="planning-empty"><ListTodo size={34} /><p>Không có công việc phù hợp trong danh sách chờ.</p></div>}{visibleTasks.map((task) => <article data-planning-drop="backlog-task" data-task-id={task.id} className={`jira-draggable-row ${task.parentTaskId ? "is-subtask " : ""}${dragged?.source === "backlog" && dragged.id === task.id ? "is-dragging " : ""}${dragOverTarget === `backlog-${task.id}` ? "is-drop-target" : ""}`} key={task.id} onPointerDown={(event) => canManage && beginPointerDrag(event, { id: task.id, source: "backlog" }, task.title, () => onOpenTask?.(task))} onPointerMove={movePointerDrag} onPointerUp={finishPointerDrag} onPointerCancel={cancelPointerDrag}>
          {canManage ? <button data-no-drag className="planning-check" type="button" onPointerDown={(event) => event.stopPropagation()} onClick={() => setSelectedIds((ids) => ids.includes(task.id) ? ids.filter((id) => id !== task.id) : [...ids, task.id])}><span className={selectedIds.includes(task.id) ? "checked" : ""}>{selectedIds.includes(task.id) && <Check size={13} />}</span></button> : <span />}
          <span className="jira-drag-handle" title="Giữ và kéo công việc"><GripVertical size={15} /></span><span className="jira-task-type">{task.workCategoryName?.slice(0, 1)?.toUpperCase() || "T"}</span><button className="jira-task-name" type="button" onClick={(event) => { if (canManage) event.preventDefault(); else onOpenTask?.(task); }}><strong>{task.title}</strong><small>{task.parentTaskId ? `↳ Task con · CV-${task.id}` : `CV-${task.id} · ${task.workCategoryName || task.type}${task.subtaskCount ? ` · ${task.subtaskCount} task con` : ""}`}</small></button><em className={`planning-priority ${task.priority?.toLowerCase()}`}>{priorityLabels[task.priority]}</em><span className="jira-task-assignee" title={task.assignedTo?.fullName || "Chưa giao"}>{task.assignedTo?.fullName?.trim()?.[0]?.toUpperCase() || "?"}</span><span className="jira-task-date">{formatDate(task.dueDate)}</span><b className="jira-task-effort">{Number(task.estimatedEffort) ? `${task.estimatedEffort} giờ` : "—"}</b><button data-no-drag className="jira-row-action" type="button" onPointerDown={(event) => event.stopPropagation()} onClick={() => onOpenTask?.(task)}><MoreHorizontal size={16} /></button>
        </article>)}</div>
        <button className="jira-create-inline" type="button" onClick={() => openTaskCreator()}><Plus size={16} /> Tạo công việc</button>
        <footer className="planning-pagination"><span>Hiển thị {filtered.length ? (page - 1) * pageSize + 1 : 0}–{Math.min(page * pageSize, filtered.length)} trong {filtered.length} công việc</span><div><button disabled={page <= 1} onClick={() => setPage((current) => current - 1)}><ChevronLeft size={15} /></button><b>{page}/{totalPages}</b><button disabled={page >= totalPages} onClick={() => setPage((current) => current + 1)}><ChevronRight size={15} /></button></div><label><select value={pageSize} onChange={(e) => setPageSize(Number(e.target.value))}><option value={5}>5 / trang</option><option value={10}>10 / trang</option><option value={20}>20 / trang</option></select></label></footer></>}
      </section>
    </div>
  </>;

  const historyRows = (sourcePlans) => sourcePlans.flatMap((plan) => {
    const snapshots = plan.taskSnapshots?.length ? plan.taskSnapshots : [null];
    const rate = plan.totalTasks ? Math.round(plan.completedTasks / plan.totalTasks * 100) : 0;
    return snapshots.map((task) => ({
      plan: plan.name, status: statusLabels[plan.status], period: `${formatDate(plan.startDate)} - ${formatDate(plan.endDate)}`,
      owner: plan.startedBy?.fullName || plan.createdBy?.fullName || "—", progress: `${plan.completedTasks}/${plan.totalTasks}`,
      capacity: `${plan.allocatedEffort || 0}/${plan.capacity || 0} ${unitLabels[plan.estimateUnit] || "giờ"}`, rate: `${rate}%`,
      task: task ? `${task.taskCode} - ${task.title}` : "—", assignee: task?.assigneeName || "—",
      effort: task ? `${task.actualEffort ?? 0}/${task.estimatedEffort ?? 0}` : "—", result: task ? (task.completed ? "Hoàn thành" : "Chưa hoàn thành") : "—"
    }));
  });

  const exportHistory = (format, sourcePlans = visibleHistoryPlans) => {
    if (!sourcePlans.length) { setError("Không có dữ liệu lịch sử phù hợp để xuất"); return; }
    const rows = historyRows(sourcePlans);
    const headers = ["Kế hoạch", "Trạng thái", "Thời gian", "Người phụ trách", "Tiến độ", "Khối lượng", "Tỷ lệ", "Công việc", "Thành viên", "Thực tế/Dự kiến", "Kết quả"];
    const keys = ["plan", "status", "period", "owner", "progress", "capacity", "rate", "task", "assignee", "effort", "result"];
    const filename = `lich-su-ke-hoach-${isoDate()}`;
    const download = (content, type, extension) => {
      const url = URL.createObjectURL(new Blob([content], { type }));
      const anchor = document.createElement("a"); anchor.href = url; anchor.download = `${filename}.${extension}`; anchor.click(); URL.revokeObjectURL(url);
    };
    if (format === "CSV") {
      const csv = [headers, ...rows.map((row) => keys.map((key) => row[key]))].map((line) => line.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n");
      download(`\ufeff${csv}`, "text/csv;charset=utf-8", "csv"); return;
    }
    const table = `<table><thead><tr>${headers.map((item) => `<th>${escapeHtml(item)}</th>`).join("")}</tr></thead><tbody>${rows.map((row) => `<tr>${keys.map((key) => `<td>${escapeHtml(row[key])}</td>`).join("")}</tr>`).join("")}</tbody></table>`;
    if (format === "EXCEL") { download(`\ufeff<html><meta charset="utf-8"><body>${table}</body></html>`, "application/vnd.ms-excel", "xls"); return; }
    const report = window.open("", "_blank");
    if (!report) { setError("Trình duyệt đang chặn cửa sổ báo cáo. Hãy cho phép cửa sổ bật lên để xuất PDF."); return; }
    report.document.write(`<html><head><title>${escapeHtml(filename)}</title><style>body{font:13px Arial;padding:24px;color:#26334d}h1{font-size:22px}p{color:#66758c}table{width:100%;border-collapse:collapse;margin-top:20px}th,td{border:1px solid #dce3ee;padding:8px;text-align:left}th{background:#f3f5fa}@media print{body{padding:0}}</style></head><body><h1>Báo cáo lịch sử kế hoạch</h1><p>${escapeHtml(historyFilters.dateFrom ? formatDate(historyFilters.dateFrom) : "Tất cả thời gian")} – ${escapeHtml(historyFilters.dateTo ? formatDate(historyFilters.dateTo) : "Hiện tại")}</p>${table}<script>window.onload=()=>window.print()<\/script></body></html>`);
    report.document.close();
  };

  const incompleteResolution = (sourcePlan, task) => {
    const targetPlan = plans.find((plan) => plan.id !== sourcePlan.id && plan.tasks?.some((item) => item.id === task.taskId));
    if (targetPlan) return `Chuyển sang ${targetPlan.name}`;
    if (unplanned.some((item) => item.id === task.taskId)) return "Đưa về danh sách chờ";
    return "Giữ lại để ghi nhận";
  };

  const HistoryView = () => {
    return <div className="jira-history-page"><header className="weekly-planning-header history-page-heading"><div><span>Lập kế hoạch tuần / Lịch sử</span><h2>Kế hoạch đã kết thúc</h2><p>Xem lại kết quả và công việc của các kế hoạch trước đây.</p></div><details className="history-export"><summary><Download size={16} /> Xuất dữ liệu <ChevronDown size={14} /></summary><div><button onClick={() => exportHistory("CSV")}><FileSpreadsheet size={15} /> CSV</button><button onClick={() => exportHistory("EXCEL")}><FileSpreadsheet size={15} /> Excel</button><button onClick={() => exportHistory("PDF")}><Printer size={15} /> PDF</button></div></details></header>
      <div className="jira-history-toolbar"><label className="history-search"><Search size={16} /><input value={historyFilters.search} onChange={(e) => setHistoryFilters({ ...historyFilters, search: e.target.value })} placeholder="Tìm kế hoạch" /></label><select aria-label="Lọc trạng thái" value={historyFilters.status} onChange={(e) => setHistoryFilters({ ...historyFilters, status: e.target.value })}><option value="ALL">Tất cả trạng thái</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Đã hủy</option></select><select aria-label="Lọc tỷ lệ hoàn thành" value={historyFilters.completion} onChange={(e) => setHistoryFilters({ ...historyFilters, completion: e.target.value })}><option value="ALL">Mọi tiến độ</option><option value="FULL">Hoàn thành 100%</option><option value="HIGH">Từ 75% trở lên</option><option value="MEDIUM">Từ 50% đến 74%</option><option value="LOW">Dưới 50%</option></select><div className="history-date-range"><CalendarDays size={15} /><input aria-label="Từ ngày" type="date" value={historyFilters.dateFrom} max={historyFilters.dateTo || undefined} onChange={(e) => setHistoryFilters({ ...historyFilters, dateFrom: e.target.value })} /><span>–</span><input aria-label="Đến ngày" type="date" value={historyFilters.dateTo} min={historyFilters.dateFrom || undefined} onChange={(e) => setHistoryFilters({ ...historyFilters, dateTo: e.target.value })} /></div><span className="jira-history-count">{visibleHistoryPlans.length} kế hoạch</span></div>
      <div className="planning-history jira-closed-sprints">{pagedHistoryPlans.map((plan) => {
        const rate = plan.totalTasks ? Math.round(plan.completedTasks / plan.totalTasks * 100) : 0;
        const unit = unitLabels[plan.estimateUnit] || "giờ";
        const snapshots = plan.taskSnapshots || [];
        const completed = snapshots.filter((task) => task.completed);
        const incomplete = snapshots.filter((task) => !task.completed);
        const contributions = Object.values(snapshots.reduce((result, task) => { const key = task.assigneeId || "unassigned"; result[key] ||= { id: task.assigneeId, name: task.assigneeName || "Chưa giao", assigned: 0, completed: 0 }; result[key].assigned += 1; if (task.completed) result[key].completed += 1; return result; }, {}));
        const IssueGroup = ({ title, items, done }) => <section className={`jira-issue-group ${done ? "done" : "open"}`}><header><span>{title}</span><b>{items.length} công việc</b></header>{items.map((task) => <div className="jira-history-issue" key={task.taskId}><span className="jira-issue-type"><Check size={12} /></span><button type="button" onClick={() => task.taskId && onOpenTask?.({ id: task.taskId, title: task.title, status: done ? "DONE" : "TODO", estimatedEffort: task.estimatedEffort, assignedTo: task.assigneeId ? { id: task.assigneeId, fullName: task.assigneeName } : null })}>{task.taskCode || `CV-${task.taskId}`}</button><strong>{task.title}</strong><span className="jira-issue-assignee"><b className="plan-task-avatar">{(task.assigneeName || "?")[0]}</b>{task.assigneeName || "Chưa giao"}</span><span className={`jira-issue-status ${done ? "done" : "open"}`}>{done ? "HOÀN THÀNH" : "CHƯA XONG"}</span><span className="jira-issue-effort">{done ? `${task.actualEffort ?? 0}/${task.estimatedEffort ?? 0} ${unit}` : incompleteResolution(plan, task)}</span></div>)}{!items.length && <p>Không có công việc.</p>}</section>;
        return <details className="jira-sprint-card" key={plan.id}><summary><ChevronRight className="jira-sprint-chevron" size={18} /><div className="jira-sprint-summary"><div><strong>{plan.name}</strong><span className={`history-status ${plan.status.toLowerCase()}`}>{statusLabels[plan.status]}</span></div><small>{formatDate(plan.startDate)} – {formatDate(plan.endDate)} · {plan.startedBy?.fullName || plan.createdBy?.fullName || "Chưa có người phụ trách"}</small></div><div className="jira-sprint-results"><span className="done"><b>{completed.length}</b> hoàn thành</span><span className="open"><b>{incomplete.length}</b> chưa xong</span></div><div className="jira-sprint-progress"><div><i style={{ width: `${rate}%` }} /><span>{rate}%</span></div><small>{plan.allocatedEffort || 0}/{plan.capacity || 0} {unit}</small></div></summary>
          <div className="history-detail jira-sprint-detail"><div className="jira-sprint-goal"><span>MỤC TIÊU KẾ HOẠCH</span><p>{plan.goal || plan.description || "Kế hoạch này chưa có mục tiêu."}</p></div><div className="jira-sprint-issues"><IssueGroup title="Công việc đã hoàn thành" items={completed} done /><IssueGroup title="Công việc chưa hoàn thành" items={incomplete} /></div>
            <footer className="jira-sprint-footer"><div><span>Thành viên</span><div>{contributions.slice(0, 5).map((item) => <b className="plan-task-avatar" title={`${item.name}: ${item.completed}/${item.assigned} công việc`} key={item.id || item.name}>{item.name[0]}</b>)}{!contributions.length && <small>Chưa có dữ liệu</small>}</div></div><button className="history-report-button" onClick={() => exportHistory("PDF", [plan])}><BarChart3 size={15} /> Xem báo cáo</button></footer>
          </div></details>;
      })}{!visibleHistoryPlans.length && <div className="planning-empty"><History size={36} /><p>Không có kế hoạch phù hợp bộ lọc.</p></div>}</div>
      <footer className="history-pagination"><span>Hiển thị {visibleHistoryPlans.length ? (historyPage - 1) * historyPageSize + 1 : 0}–{Math.min(historyPage * historyPageSize, visibleHistoryPlans.length)} trong {visibleHistoryPlans.length} kế hoạch</span><div><button disabled={historyPage <= 1} onClick={() => setHistoryPage((current) => current - 1)}><ChevronLeft size={15} /></button><b>{historyPage}/{historyTotalPages}</b><button disabled={historyPage >= historyTotalPages} onClick={() => setHistoryPage((current) => current + 1)}><ChevronRight size={15} /></button></div></footer>
    </div>;
  };

  return <section className="weekly-planning">
    <nav className="planning-subnav">{[["backlog", "Danh sách chờ"], ["current", "Kế hoạch tuần"], ["history", "Lịch sử"]].map(([key, label]) => <button key={key} className={section === key ? "active" : ""} onClick={() => { setSection(key); if (key === "current" && activePlan) setSelectedPlanId(String(activePlan.id)); }}>{label}</button>)}</nav>
    {error && <div className="planning-error">{error}<button onClick={() => setError("")}><X size={15} /></button></div>}
    {section === "backlog" && Backlog()}{section === "current" && CurrentSprintView()}{section === "history" && HistoryView()}
    {pointerPreview && <div className="planning-drag-preview" style={{ transform: `translate3d(${pointerPreview.x + 14}px, ${pointerPreview.y + 14}px, 0)` }}><GripVertical size={15} /><span><b>{pointerPreview.label}</b><small>{pointerPreview.hint || "Đang di chuyển"}</small></span></div>}

    {showCreate && <div className="planning-modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) { setShowCreate(false); setEditingPlan(null); } }}><form className="planning-modal planning-create-modal" onSubmit={savePlan}><header><h3>{editingPlan ? "Chỉnh sửa kế hoạch tuần" : "Tạo kế hoạch tuần"}</h3><button type="button" onClick={() => { setShowCreate(false); setEditingPlan(null); }}><X /></button></header>
      <label>Tên kế hoạch<input value={planForm.name} onChange={(e) => setPlanForm({ ...planForm, name: e.target.value })} />{formErrors.name && <small>{formErrors.name}</small>}</label>
      <label>Mục tiêu tuần<textarea value={planForm.goal} onChange={(e) => setPlanForm({ ...planForm, goal: e.target.value })} /></label><label>Mô tả<textarea value={planForm.description} onChange={(e) => setPlanForm({ ...planForm, description: e.target.value })} /></label>
      <div><label>Ngày bắt đầu<input type="date" value={planForm.startDate} onChange={(e) => setPlanForm({ ...planForm, startDate: e.target.value })} />{formErrors.startDate && <small>{formErrors.startDate}</small>}</label><label>Ngày kết thúc<input type="date" value={planForm.endDate} onChange={(e) => setPlanForm({ ...planForm, endDate: e.target.value })} />{formErrors.endDate && <small>{formErrors.endDate}</small>}</label></div>
      <div><label>Tổng sức chứa<input type="number" min="0.25" step="0.25" value={planForm.capacity} onChange={(e) => setPlanForm({ ...planForm, capacity: e.target.value })} />{formErrors.capacity && <small>{formErrors.capacity}</small>}</label><label>Đơn vị<select value={planForm.estimateUnit} onChange={(e) => setPlanForm({ ...planForm, estimateUnit: e.target.value })}><option value="HOURS">Số giờ</option><option value="STORY_POINTS">Điểm công việc</option></select></label></div>
      <button className="planning-primary" disabled={busy}>{editingPlan ? "Lưu thay đổi" : "Tạo kế hoạch"}</button></form></div>}

    {showTaskCreate && <div className={`task-create-backdrop ${taskCreateContext?.planId ? "is-apple-planning" : ""}`} onMouseDown={(event) => event.target === event.currentTarget && closeTaskCreator()}><section className={`task-create-modal ${taskCreateContext?.planId ? "task-create-modal-apple" : ""}`}><header><div><h2>{taskCreateContext?.planId ? "Tạo công việc trong kế hoạch" : "Tạo công việc chưa lên kế hoạch"}</h2><p>{taskCreateContext?.planId ? "Công việc sẽ được thêm vào cột đã chọn của kế hoạch đang chạy." : "Công việc sẽ được thêm trực tiếp vào Danh sách chờ."}</p></div><button type="button" onClick={closeTaskCreator} aria-label="Đóng"><X size={20} /></button></header><TaskForm
      initialValues={taskCreateContext ? { status: taskCreateContext.status || "TODO", boardColumnId: taskCreateContext.boardColumnId || null } : undefined}
      members={members}
      parentTasks={taskCreateContext?.planId ? (plans.find((plan) => plan.id === taskCreateContext.planId)?.tasks || []) : unplanned}
      taskTypes={taskTypes}
      onAddTaskType={onAddTaskType}
      canAssign={canManage}
      variant={taskCreateContext?.planId ? "apple-planning" : "default"}
      submitLabel={taskCreateContext?.planId ? "Tạo trong kế hoạch" : "Tạo vào danh sách chờ"}
      loadingLabel="Đang tạo..."
      onSubmit={createPlanningTask}
    /></section></div>}

    {completion && <div className="planning-modal-backdrop"><div className="planning-modal completion-modal"><header><h3>Kết thúc kế hoạch</h3><button onClick={() => setCompletion(null)}><X /></button></header><div className="completion-summary"><div><strong>{completion.totalTasks}</strong><span>Tổng task</span></div><div><strong>{completion.completedTasks}</strong><span>Đã xong</span></div><div><strong>{completion.incompleteTasks}</strong><span>Chưa xong</span></div></div><p>Tiến độ: <b>{Math.round(completion.completionRate || 0)}%</b> · Khối lượng hoàn thành: <b>{completion.completedEffort || 0}/{completion.plannedEffort || 0}</b></p>{completion.incompleteTasks > 0 && <><label>Xử lý task chưa hoàn thành<select value={completeForm.action} onChange={(e) => setCompleteForm({ ...completeForm, action: e.target.value })}><option value="MOVE_TO_UNPLANNED">Đưa về danh sách chờ</option><option value="MOVE_TO_PLAN">Chuyển sang bản nháp khác</option><option value="CREATE_NEXT_PLAN">Tạo kế hoạch tiếp theo</option><option value="KEEP_IN_PLAN">Giữ lại để ghi nhận</option></select></label>{completeForm.action === "MOVE_TO_PLAN" && <label>Kế hoạch đích<select value={completeForm.targetPlanId} onChange={(e) => setCompleteForm({ ...completeForm, targetPlanId: e.target.value })}><option value="">Chọn kế hoạch</option>{draftPlans.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label>}{completeForm.action === "CREATE_NEXT_PLAN" && <><label>Tên kế hoạch tiếp theo<input value={completeForm.nextPlanName} onChange={(e) => setCompleteForm({ ...completeForm, nextPlanName: e.target.value })} /></label><div><label>Bắt đầu<input type="date" value={completeForm.nextPlanStartDate} onChange={(e) => setCompleteForm({ ...completeForm, nextPlanStartDate: e.target.value })} /></label><label>Kết thúc<input type="date" value={completeForm.nextPlanEndDate} onChange={(e) => setCompleteForm({ ...completeForm, nextPlanEndDate: e.target.value })} /></label></div></>}</>}<button className="planning-primary" disabled={busy} onClick={completePlan}>Xác nhận kết thúc</button></div></div>}
  </section>;
}
