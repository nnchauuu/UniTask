import { useEffect, useMemo, useRef, useState } from "react";
import {
  CalendarDays,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Download,
  ExternalLink,
  Filter,
  List,
  MoreHorizontal,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  Users,
  X
} from "lucide-react";
import * as calendarApi from "../api/calendarApi";
import * as taskApi from "../api/taskApi";
import { useToast } from "../context/ToastContext";

const eventTypeLabels = {
  TASK_DEADLINE: "Công việc",
  MEETING: "Cuộc họp",
  PROJECT_DEADLINE: "Mốc dự án",
  CUSTOM_EVENT: "Sự kiện"
};

const eventTypeOrder = ["TASK_DEADLINE", "MEETING", "PROJECT_DEADLINE", "CUSTOM_EVENT"];
const weekdays = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];

const emptyForm = {
  title: "",
  description: "",
  eventType: "CUSTOM_EVENT",
  startTime: "",
  endTime: "",
  recurrence: "NONE",
  repeatCount: 4
};

const pad = (value) => String(value).padStart(2, "0");
const dateKey = (value) => {
  const date = value instanceof Date ? value : new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const toInputDateTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  return `${dateKey(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const formatTime = (value) => new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(new Date(value));
const formatFullDate = (value) => new Intl.DateTimeFormat("vi-VN", { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
const monthTitle = (date) => `Tháng ${date.getMonth() + 1}, ${date.getFullYear()}`;
const eventKey = (event) => event.virtualId || `${event.eventType}-${event.id}`;
const taskIdFromEvent = (event) => event.eventType === "TASK_DEADLINE" ? Number(event.virtualId?.replace("TASK_DEADLINE-", "")) : null;

const addRecurrence = (value, recurrence, index) => {
  const date = new Date(value);
  if (recurrence === "DAILY") date.setDate(date.getDate() + index);
  if (recurrence === "WEEKLY") date.setDate(date.getDate() + index * 7);
  if (recurrence === "MONTHLY") date.setMonth(date.getMonth() + index);
  return toInputDateTime(date);
};

const escapeIcs = (value = "") => value.replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/,/g, "\\,").replace(/;/g, "\\;");
const toIcsDate = (value) => new Date(value).toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");

function ProjectCalendar({
  project,
  tasks = [],
  members = [],
  currentUserId,
  canManageCalendar,
  refreshKey,
  onActivityChanged,
  onTaskUpdated,
  onOpenTask
}) {
  const [events, setEvents] = useState([]);
  const [currentMonth, setCurrentMonth] = useState(() => new Date());
  const [viewMode, setViewMode] = useState(() => localStorage.getItem("unitask-calendar-view") || "month");
  const [calendarScope, setCalendarScope] = useState("project");
  const [enabledTypes, setEnabledTypes] = useState(() => new Set(eventTypeOrder));
  const [memberFilter, setMemberFilter] = useState("all");
  const [showCreate, setShowCreate] = useState(false);
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [draggingTaskId, setDraggingTaskId] = useState(null);
  const [error, setError] = useState("");
  const remindedEvents = useRef(new Set());
  const { showToast } = useToast();

  const loadEvents = async (quiet = false) => {
    if (!quiet) setLoading(true);
    setError("");
    try {
      const response = calendarScope === "workspace"
        ? await calendarApi.getWorkspaceCalendarEvents(project.workspaceId)
        : await calendarApi.getProjectCalendarEvents(project.id);
      setEvents(response.data || []);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải dữ liệu lịch");
    } finally {
      if (!quiet) setLoading(false);
    }
  };

  useEffect(() => {
    loadEvents();
  }, [project.id, project.workspaceId, calendarScope, refreshKey]);

  useEffect(() => {
    const timer = window.setInterval(() => loadEvents(true), 30000);
    return () => window.clearInterval(timer);
  }, [project.id, project.workspaceId, calendarScope]);

  useEffect(() => {
    const now = Date.now();
    events.forEach((event) => {
      const startsIn = new Date(event.startTime).getTime() - now;
      const key = eventKey(event);
      if (startsIn > 0 && startsIn <= 15 * 60 * 1000 && !remindedEvents.current.has(key)) {
        remindedEvents.current.add(key);
        showToast(`Sắp tới: ${event.title}`);
      }
    });
  }, [events, showToast]);

  const taskMap = useMemo(() => new Map(tasks.map((task) => [Number(task.id), task])), [tasks]);

  const filteredEvents = useMemo(() => events.filter((event) => {
    if (!enabledTypes.has(event.eventType)) return false;
    if (memberFilter === "all") return true;
    const relatedUserId = event.assignedTo?.id || event.createdBy?.id;
    if (memberFilter === "mine") return String(relatedUserId) === String(currentUserId);
    return String(relatedUserId) === memberFilter;
  }), [events, enabledTypes, memberFilter, currentUserId]);

  const groupedEvents = useMemo(() => filteredEvents.reduce((groups, event) => {
    const key = dateKey(event.startTime);
    groups[key] = [...(groups[key] || []), event].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
    return groups;
  }, {}), [filteredEvents]);

  const calendarDays = useMemo(() => {
    const firstDay = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1);
    const mondayOffset = (firstDay.getDay() + 6) % 7;
    const start = new Date(firstDay);
    start.setDate(firstDay.getDate() - mondayOffset);
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      return date;
    });
  }, [currentMonth]);

  const monthEvents = filteredEvents.filter((event) => {
    const date = new Date(event.startTime);
    return date.getMonth() === currentMonth.getMonth() && date.getFullYear() === currentMonth.getFullYear();
  });

  const upcomingEvents = [...filteredEvents]
    .filter((event) => new Date(event.endTime || event.startTime).getTime() >= Date.now())
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
    .slice(0, 5);

  const isOverdue = (event) => {
    if (event.eventType !== "TASK_DEADLINE") return false;
    const task = taskMap.get(taskIdFromEvent(event));
    return new Date(event.startTime).getTime() < Date.now() && task?.status !== "DONE";
  };

  const toggleType = (type) => setEnabledTypes((current) => {
    const next = new Set(current);
    if (next.has(type)) next.delete(type);
    else next.add(type);
    return next;
  });

  const changeView = (view) => {
    setViewMode(view);
    localStorage.setItem("unitask-calendar-view", view);
  };

  const openCreateForDate = (date = new Date()) => {
    const start = new Date(date);
    start.setHours(9, 0, 0, 0);
    const end = new Date(start);
    end.setHours(10, 0, 0, 0);
    setEditingId(null);
    setForm({ ...emptyForm, startTime: toInputDateTime(start), endTime: toInputDateTime(end) });
    setShowCreate(true);
  };

  const startEdit = (event) => {
    setEditingId(event.id);
    setForm({
      ...emptyForm,
      title: event.title,
      description: event.description || "",
      eventType: event.eventType,
      startTime: toInputDateTime(event.startTime),
      endTime: toInputDateTime(event.endTime)
    });
    setSelectedEvent(null);
    setShowCreate(true);
  };

  const handleFormChange = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        eventType: form.eventType,
        startTime: form.startTime,
        endTime: form.endTime
      };
      if (editingId) {
        await calendarApi.updateCalendarEvent(editingId, payload);
        showToast("Đã cập nhật sự kiện");
      } else {
        const count = form.recurrence === "NONE" ? 1 : Math.min(12, Math.max(1, Number(form.repeatCount) || 1));
        await Promise.all(Array.from({ length: count }, (_, index) => calendarApi.createCalendarEvent(project.id, {
          ...payload,
          startTime: addRecurrence(payload.startTime, form.recurrence, index),
          endTime: addRecurrence(payload.endTime, form.recurrence, index)
        })));
        showToast(count > 1 ? `Đã tạo ${count} sự kiện lặp lại` : "Đã tạo sự kiện");
        onActivityChanged?.();
      }
      setShowCreate(false);
      setForm(emptyForm);
      setEditingId(null);
      await loadEvents(true);
    } catch (err) {
      const validation = err.response?.data?.data;
      setError((validation && Object.values(validation)[0]) || err.response?.data?.message || "Không thể lưu sự kiện");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (event) => {
    if (!event.id || !window.confirm(`Xóa ${event.title}?`)) return;
    try {
      await calendarApi.deleteCalendarEvent(event.id);
      setSelectedEvent(null);
      showToast("Đã xóa sự kiện");
      await loadEvents(true);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể xóa sự kiện");
    }
  };

  const moveTaskDeadline = async (taskId, newDate) => {
    if (!taskId || !canManageCalendar) return;
    try {
      const taskResponse = await taskApi.getTaskDetail(taskId);
      const task = taskResponse.data;
      const response = await taskApi.updateTask(taskId, {
        title: task.title,
        description: task.description || "",
        assignedToUserId: task.assignedTo?.id || null,
        status: task.status,
        priority: task.priority,
        dueDate: dateKey(newDate)
      });
      onTaskUpdated?.(response.data);
      showToast(`Đã chuyển deadline sang ${new Intl.DateTimeFormat("vi-VN").format(newDate)}`);
      await loadEvents(true);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật deadline");
    } finally {
      setDraggingTaskId(null);
    }
  };

  const openTaskDetail = async (event) => {
    const taskId = taskIdFromEvent(event);
    if (!taskId) return;
    try {
      const response = await taskApi.getTaskDetail(taskId);
      setSelectedEvent(null);
      onOpenTask?.(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể mở công việc");
    }
  };

  const exportIcs = () => {
    const content = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//UniTask//Project Calendar//VI"];
    filteredEvents.forEach((event) => content.push(
      "BEGIN:VEVENT",
      `UID:${escapeIcs(eventKey(event))}@unitask.local`,
      `DTSTAMP:${toIcsDate(new Date())}`,
      `DTSTART:${toIcsDate(event.startTime)}`,
      `DTEND:${toIcsDate(event.endTime || event.startTime)}`,
      `SUMMARY:${escapeIcs(event.title)}`,
      `DESCRIPTION:${escapeIcs(event.description || eventTypeLabels[event.eventType])}`,
      "END:VEVENT"
    ));
    content.push("END:VCALENDAR");
    const url = URL.createObjectURL(new Blob([content.join("\r\n")], { type: "text/calendar;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `${project.name || "unitask"}-calendar.ics`;
    link.click();
    URL.revokeObjectURL(url);
    setShowMoreMenu(false);
  };

  const openGoogleCalendar = (event) => {
    if (!event) {
      window.open("https://calendar.google.com/calendar/u/0/r", "_blank", "noopener,noreferrer");
      return;
    }
    const query = new URLSearchParams({
      action: "TEMPLATE",
      text: event.title,
      dates: `${toIcsDate(event.startTime)}/${toIcsDate(event.endTime || event.startTime)}`,
      details: event.description || `${eventTypeLabels[event.eventType]} • ${event.projectName || project.name}`
    });
    window.open(`https://calendar.google.com/calendar/render?${query}`, "_blank", "noopener,noreferrer");
  };

  return (
    <section className="project-calendar-page">
      <header className="calendar-page-heading">
        <div>
          <div className="calendar-breadcrumb"><span>{project.workspaceName || "Dự án"}</span><b>/</b>{project.name}</div>
          <h1>Lịch dự án</h1>
          <p>Theo dõi deadline, cuộc họp và các mốc quan trọng</p>
        </div>
        <div className="calendar-heading-actions">
          {canManageCalendar && <button type="button" className="calendar-create-button" onClick={() => openCreateForDate()}><Plus size={18} /> Tạo sự kiện</button>}
          <div className="calendar-more-wrap">
            <button type="button" className="calendar-more-button" onClick={() => setShowMoreMenu((current) => !current)}><MoreHorizontal size={19} /></button>
            {showMoreMenu && <div className="calendar-more-menu"><button type="button" onClick={exportIcs}><Download size={16} /> Xuất lịch .ics</button><button type="button" onClick={() => openGoogleCalendar()}><ExternalLink size={16} /> Mở Google Calendar</button><button type="button" onClick={() => loadEvents()}><RefreshCw size={16} /> Làm mới dữ liệu</button></div>}
          </div>
        </div>
      </header>

      {error && <div className="calendar-error"><span>{error}</span><button type="button" onClick={() => setError("")}><X size={16} /></button></div>}

      <div className="calendar-toolbar">
        <div className="calendar-date-navigation">
          <button type="button" className="calendar-today-button" onClick={() => setCurrentMonth(new Date())}>Hôm nay</button>
          <button type="button" title="Tháng trước" onClick={() => setCurrentMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))}><ChevronLeft size={17} /></button>
          <button type="button" title="Tháng sau" onClick={() => setCurrentMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))}><ChevronRight size={17} /></button>
          <strong>{monthTitle(currentMonth)}</strong>
        </div>
        <div className="calendar-toolbar-filters">
          <div className="calendar-view-switch"><button className={viewMode === "month" ? "active" : ""} type="button" onClick={() => changeView("month")}><CalendarDays size={15} /> Tháng</button><button className={viewMode === "list" ? "active" : ""} type="button" onClick={() => changeView("list")}><List size={15} /> Danh sách</button></div>
          <label><Filter size={15} /><select value={calendarScope} onChange={(event) => setCalendarScope(event.target.value)}><option value="project">Lịch dự án</option><option value="workspace">Toàn workspace</option></select><ChevronDown size={14} /></label>
          <label><Users size={15} /><select value={memberFilter} onChange={(event) => setMemberFilter(event.target.value)}><option value="all">Thành viên</option><option value="mine">Của tôi</option>{members.map((member) => <option key={member.userId} value={member.userId}>{member.fullName}</option>)}</select><ChevronDown size={14} /></label>
        </div>
      </div>

      <div className="calendar-type-filters">
        {eventTypeOrder.map((type) => <label key={type}><input type="checkbox" checked={enabledTypes.has(type)} onChange={() => toggleType(type)} /><span className={`calendar-type-dot ${type.toLowerCase()}`} />{eventTypeLabels[type]}</label>)}
      </div>

      <div className="calendar-layout">
        <div className="calendar-main-panel">
          {loading ? <div className="calendar-loading">Đang tải lịch...</div> : viewMode === "month" ? (
            <div className="calendar-month-grid">
              {weekdays.map((day) => <div className="calendar-weekday" key={day}>{day}</div>)}
              {calendarDays.map((day) => {
                const key = dateKey(day);
                const dayEvents = groupedEvents[key] || [];
                const isToday = key === dateKey(new Date());
                const outside = day.getMonth() !== currentMonth.getMonth();
                return (
                  <div
                    key={key}
                    className={`calendar-date-cell ${outside ? "outside" : ""} ${draggingTaskId ? "drop-ready" : ""}`}
                    onDoubleClick={() => canManageCalendar && openCreateForDate(day)}
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={(event) => { event.preventDefault(); moveTaskDeadline(Number(event.dataTransfer.getData("text/unitask-task")), day); }}
                  >
                    <span className={isToday ? "today" : ""}>{day.getDate()}</span>
                    <div className="calendar-cell-events">
                      {dayEvents.slice(0, 3).map((event) => (
                        <button
                          key={eventKey(event)}
                          type="button"
                          className={`calendar-event-chip ${event.eventType.toLowerCase()} ${isOverdue(event) ? "overdue" : ""}`}
                          draggable={canManageCalendar && event.eventType === "TASK_DEADLINE"}
                          onDragStart={(dragEvent) => { const taskId = taskIdFromEvent(event); dragEvent.dataTransfer.setData("text/unitask-task", String(taskId)); setDraggingTaskId(taskId); }}
                          onDragEnd={() => setDraggingTaskId(null)}
                          onClick={() => setSelectedEvent(event)}
                        >
                          {event.eventType === "MEETING" && <span>{formatTime(event.startTime)}</span>}{event.title}
                        </button>
                      ))}
                      {dayEvents.length > 3 && <button type="button" className="calendar-more-events" onClick={() => changeView("list")}>+{dayEvents.length - 3} sự kiện</button>}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="calendar-list-view">
              {Object.entries(groupedEvents).filter(([key]) => key.startsWith(`${currentMonth.getFullYear()}-${pad(currentMonth.getMonth() + 1)}`)).map(([key, dayEvents]) => (
                <section key={key}><div className="calendar-list-date"><b>{new Date(`${key}T00:00:00`).getDate()}</b><span>{new Intl.DateTimeFormat("vi-VN", { weekday: "short", month: "long" }).format(new Date(`${key}T00:00:00`))}</span></div><div>{dayEvents.map((event) => <button key={eventKey(event)} type="button" className={`calendar-list-event ${event.eventType.toLowerCase()} ${isOverdue(event) ? "overdue" : ""}`} onClick={() => setSelectedEvent(event)}><span className="calendar-event-time">{formatTime(event.startTime)}</span><span className={`calendar-type-dot ${event.eventType.toLowerCase()}`} /><span><b>{event.title}</b><small>{event.projectName || project.name} • {eventTypeLabels[event.eventType]}</small></span></button>)}</div></section>
              ))}
              {!monthEvents.length && <div className="calendar-loading">Không có sự kiện trong tháng này.</div>}
            </div>
          )}
        </div>

        <aside className="calendar-side-panel">
          <section className="calendar-upcoming"><h2>Sắp tới</h2>{upcomingEvents.length ? upcomingEvents.map((event) => <button key={eventKey(event)} type="button" onClick={() => setSelectedEvent(event)}><span className={`calendar-type-dot ${event.eventType.toLowerCase()}`} /><span><small>{dateKey(event.startTime) === dateKey(new Date()) ? "Hôm nay" : new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(new Date(event.startTime))}</small><b>{event.title}</b></span><em>{formatTime(event.startTime)}</em></button>) : <p>Chưa có sự kiện sắp tới.</p>}</section>
          <section className="calendar-month-summary"><h2>Tổng quan tháng</h2>{eventTypeOrder.map((type) => <div key={type}><span className={`calendar-summary-icon ${type.toLowerCase()}`}><CalendarDays size={16} /></span><b>{monthEvents.filter((event) => event.eventType === type).length}</b><span>{eventTypeLabels[type]}</span></div>)}</section>
        </aside>
      </div>

      {showCreate && <div className="calendar-modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setShowCreate(false)}><section className="calendar-event-form-modal"><header><div><h2>{editingId ? "Chỉnh sửa sự kiện" : "Tạo sự kiện"}</h2><p>Thêm lịch riêng hoặc cuộc họp vào dự án</p></div><button type="button" onClick={() => setShowCreate(false)}><X size={20} /></button></header><form onSubmit={handleSubmit}><label>Tiêu đề<input name="title" maxLength={200} value={form.title} onChange={handleFormChange} required /></label><label>Mô tả<textarea name="description" maxLength={1000} rows={3} value={form.description} onChange={handleFormChange} /></label><div className="calendar-form-row"><label>Loại sự kiện<select name="eventType" value={form.eventType} onChange={handleFormChange}><option value="CUSTOM_EVENT">Sự kiện riêng</option><option value="MEETING">Cuộc họp</option></select></label><label>Lặp lại<select name="recurrence" value={form.recurrence} onChange={handleFormChange} disabled={Boolean(editingId)}><option value="NONE">Không lặp</option><option value="DAILY">Hàng ngày</option><option value="WEEKLY">Hàng tuần</option><option value="MONTHLY">Hàng tháng</option></select></label></div><div className="calendar-form-row"><label>Bắt đầu<input name="startTime" type="datetime-local" value={form.startTime} onChange={handleFormChange} required /></label><label>Kết thúc<input name="endTime" type="datetime-local" value={form.endTime} onChange={handleFormChange} required /></label></div>{form.recurrence !== "NONE" && !editingId && <label>Số lần lặp<input name="repeatCount" type="number" min="2" max="12" value={form.repeatCount} onChange={handleFormChange} /></label>}{error && <div className="calendar-form-error">{error}</div>}<footer><button type="button" onClick={() => setShowCreate(false)}>Hủy</button><button type="submit" disabled={saving}>{saving ? "Đang lưu..." : editingId ? "Lưu thay đổi" : "Tạo sự kiện"}</button></footer></form></section></div>}

      {selectedEvent && <div className="calendar-modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setSelectedEvent(null)}><section className="calendar-event-detail-modal"><header><span className={`calendar-detail-icon ${selectedEvent.eventType.toLowerCase()}`}><CalendarDays size={21} /></span><div><small>{eventTypeLabels[selectedEvent.eventType]}</small><h2>{selectedEvent.title}</h2></div><button type="button" onClick={() => setSelectedEvent(null)}><X size={20} /></button></header><div className="calendar-detail-content"><p><Clock3 size={17} /><span><b>{formatFullDate(selectedEvent.startTime)}</b>{formatTime(selectedEvent.startTime)} – {formatTime(selectedEvent.endTime || selectedEvent.startTime)}</span></p><p><CalendarDays size={17} /><span><b>{selectedEvent.projectName || project.name}</b>{calendarScope === "workspace" ? "Thuộc lịch workspace" : "Thuộc dự án hiện tại"}</span></p>{selectedEvent.assignedTo && <p><Users size={17} /><span><b>{selectedEvent.assignedTo.fullName}</b>Người thực hiện</span></p>}{selectedEvent.description && <div className="calendar-detail-description">{selectedEvent.description}</div>}{isOverdue(selectedEvent) && <div className="calendar-overdue-notice">Sự kiện đã quá hạn và công việc chưa hoàn thành.</div>}</div><footer><button type="button" onClick={() => openGoogleCalendar(selectedEvent)}><ExternalLink size={16} /> Google Calendar</button>{selectedEvent.eventType === "TASK_DEADLINE" && <button type="button" onClick={() => openTaskDetail(selectedEvent)}>Xem công việc</button>}{canManageCalendar && selectedEvent.editable && <><button type="button" onClick={() => startEdit(selectedEvent)}><Pencil size={16} /> Sửa</button><button type="button" className="danger" onClick={() => handleDelete(selectedEvent)}><Trash2 size={16} /> Xóa</button></>}</footer></section></div>}
    </section>
  );
}

export default ProjectCalendar;
