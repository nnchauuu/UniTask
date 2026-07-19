import axiosClient from "./axiosClient";

export const getProjectCalendarEvents = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/calendar-events`);
  return response.data;
};

export const getWorkspaceCalendarEvents = async (workspaceId) => {
  const response = await axiosClient.get(`/workspaces/${workspaceId}/calendar-events`);
  return response.data;
};

export const createCalendarEvent = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/calendar-events`, payload);
  return response.data;
};

export const updateCalendarEvent = async (eventId, payload) => {
  const response = await axiosClient.put(`/calendar-events/${eventId}`, payload);
  return response.data;
};

export const deleteCalendarEvent = async (eventId) => {
  const response = await axiosClient.delete(`/calendar-events/${eventId}`);
  return response.data;
};
