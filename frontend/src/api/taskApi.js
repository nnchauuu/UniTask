import axiosClient from "./axiosClient";
import { Client } from "@stomp/stompjs";

export const createTask = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/tasks`, payload);
  return response.data;
};

export const getProjectTasks = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/tasks`);
  return response.data;
};

export const getTaskDetail = async (taskId) => {
  const response = await axiosClient.get(`/tasks/${taskId}`);
  return response.data;
};

export const updateTask = async (taskId, payload) => {
  const response = await axiosClient.put(`/tasks/${taskId}`, payload);
  return response.data;
};

export const updateTaskStatus = async (taskId, payload) => {
  const response = await axiosClient.patch(`/tasks/${taskId}/status`, payload);
  return response.data;
};

export const getMyTasks = async () => {
  const response = await axiosClient.get("/tasks/my");
  return response.data;
};

export const createTaskComment = async (taskId, payload) => {
  const response = await axiosClient.post(`/tasks/${taskId}/comments`, payload);
  return response.data;
};

export const getTaskComments = async (taskId) => {
  const response = await axiosClient.get(`/tasks/${taskId}/comments`);
  return response.data;
};

export const getSubtasks = async (taskId) => {
  const response = await axiosClient.get(`/tasks/${taskId}/subtasks`);
  return response.data;
};

export const deleteTask = async (taskId, subtaskAction) => {
  const response = await axiosClient.delete(`/tasks/${taskId}`, {
    params: subtaskAction ? { subtaskAction } : undefined
  });
  return response.data;
};

export const getChecklist = async (taskId) => {
  const response = await axiosClient.get(`/tasks/${taskId}/checklist`);
  return response.data;
};

export const createChecklistItem = async (taskId, payload) => {
  const response = await axiosClient.post(`/tasks/${taskId}/checklist`, payload);
  return response.data;
};

export const updateChecklistItem = async (taskId, itemId, payload) => {
  const response = await axiosClient.put(`/tasks/${taskId}/checklist/${itemId}`, payload);
  return response.data;
};

export const deleteChecklistItem = async (taskId, itemId) => {
  const response = await axiosClient.delete(`/tasks/${taskId}/checklist/${itemId}`);
  return response.data;
};

export const reorderChecklist = async (taskId, itemIds) => {
  const response = await axiosClient.put(`/tasks/${taskId}/checklist/order`, { itemIds });
  return response.data;
};

export const submitTaskReview = async (taskId, reviewerId) => {
  const response = await axiosClient.post(`/tasks/${taskId}/reviews/submit`, reviewerId ? { reviewerId } : {});
  return response.data;
};

export const approveTaskReview = async (taskId) => {
  const response = await axiosClient.post(`/tasks/${taskId}/reviews/approve`);
  return response.data;
};

export const requestTaskChanges = async (taskId, reason) => {
  const response = await axiosClient.post(`/tasks/${taskId}/reviews/request-changes`, { reason });
  return response.data;
};

export const getTaskReviewHistory = async (taskId) => {
  const response = await axiosClient.get(`/tasks/${taskId}/reviews`);
  return response.data;
};

export const getUnplannedTasks = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/unplanned-tasks`);
  return response.data;
};

export const createUnplannedTask = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/unplanned-tasks`, payload);
  return response.data;
};

export const reorderUnplannedTasks = async (projectId, taskIds) => {
  const response = await axiosClient.put(`/projects/${projectId}/unplanned-tasks/order`, { taskIds });
  return response.data;
};

export const moveTaskToUnplanned = async (taskId, includeSubtasks = false) => {
  const response = await axiosClient.post(`/tasks/${taskId}/move-to-unplanned`, { includeSubtasks });
  return response.data;
};

export const moveTaskToBoard = async (taskId, includeSubtasks = false) => {
  const response = await axiosClient.post(`/tasks/${taskId}/move-to-board`, { includeSubtasks });
  return response.data;
};

export const getWeeklyPlans = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/weekly-plans`);
  return response.data;
};

export const createWeeklyPlan = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/weekly-plans`, payload);
  return response.data;
};

export const updateWeeklyPlan = async (planId, payload) => {
  const response = await axiosClient.put(`/weekly-plans/${planId}`, payload);
  return response.data;
};

export const deleteWeeklyPlan = async (planId) => {
  const response = await axiosClient.delete(`/weekly-plans/${planId}`);
  return response.data;
};

export const addTasksToWeeklyPlan = async (planId, taskIds, includeSubtasks = false) => {
  const response = await axiosClient.post(`/weekly-plans/${planId}/tasks`, { taskIds, includeSubtasks });
  return response.data;
};

export const removeTaskFromWeeklyPlan = async (planId, taskId, includeSubtasks = true) => {
  const response = await axiosClient.delete(`/weekly-plans/${planId}/tasks/${taskId}`, { params: { includeSubtasks } });
  return response.data;
};

export const reorderWeeklyPlanTasks = async (planId, taskIds) => {
  const response = await axiosClient.put(`/weekly-plans/${planId}/tasks/order`, { taskIds });
  return response.data;
};

export const startWeeklyPlan = async (planId, payload) => {
  const response = await axiosClient.post(`/weekly-plans/${planId}/start`, payload);
  return response.data;
};

export const cloneWeeklyPlan = async (planId) => (await axiosClient.post(`/weekly-plans/${planId}/clone`)).data;

export const cancelWeeklyPlan = async (planId) => (await axiosClient.post(`/weekly-plans/${planId}/cancel`)).data;

export const getWeeklyPlanCompletion = async (planId) => {
  const response = await axiosClient.get(`/weekly-plans/${planId}/completion-preview`);
  return response.data;
};

export const completeWeeklyPlan = async (planId, payload) => {
  const response = await axiosClient.post(`/weekly-plans/${planId}/complete`, payload);
  return response.data;
};

export const getTaskActivities = async (taskId, page = 0, size = 20) => (await axiosClient.get(`/tasks/${taskId}/activities`, { params: { page, size } })).data;
export const getTaskWatchers = async (taskId) => (await axiosClient.get(`/tasks/${taskId}/watchers`)).data;
export const followTask = async (taskId) => (await axiosClient.post(`/tasks/${taskId}/watchers/me`)).data;
export const unfollowTask = async (taskId) => (await axiosClient.delete(`/tasks/${taskId}/watchers/me`)).data;

export const createBoardRealtimeClient = ({ token, projectId, onEvent, onReconnect, onError }) => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  const wsBaseUrl = apiBaseUrl.replace(/^http/, "ws").replace(/\/api\/?$/, "");
  let connectedOnce = false;
  return new Client({
    brokerURL: `${wsBaseUrl}/ws`, connectHeaders: { Authorization: `Bearer ${token}` }, reconnectDelay: 4000,
    onConnect() { if (connectedOnce) onReconnect?.(); connectedOnce = true; this.subscribe(`/topic/projects/${projectId}/board`, (message) => onEvent?.(JSON.parse(message.body))); },
    onStompError: (frame) => onError?.(frame.headers?.message || "Lỗi đồng bộ Board"),
    onWebSocketError: () => onError?.("Mất kết nối realtime, hệ thống sẽ tự kết nối lại")
  });
};
