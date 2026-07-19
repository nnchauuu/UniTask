import axiosClient from "./axiosClient";

export const getProjectActivityLogs = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/activity-logs`);
  return response.data;
};
