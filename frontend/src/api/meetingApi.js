import axiosClient from "./axiosClient";

export const createMeeting = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/meetings`, payload);
  return response.data;
};

export const getProjectMeetings = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/meetings`);
  return response.data;
};

export const getMeetingDetail = async (meetingId) => {
  const response = await axiosClient.get(`/meetings/${meetingId}`);
  return response.data;
};

export const updateMeetingNotes = async (meetingId, payload) => {
  const response = await axiosClient.put(`/meetings/${meetingId}/notes`, payload);
  return response.data;
};

export const addMeetingParticipant = async (meetingId, userId) => {
  const response = await axiosClient.post(`/meetings/${meetingId}/participants/${userId}`);
  return response.data;
};

export const createTaskFromMeeting = async (meetingId, payload) => {
  const response = await axiosClient.post(`/meetings/${meetingId}/tasks`, payload);
  return response.data;
};
