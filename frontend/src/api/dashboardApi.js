import axiosClient from "./axiosClient";

export const getProjectDashboard = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/dashboard`);
  return response.data;
};
