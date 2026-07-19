import axiosClient from "./axiosClient";

export const getProjectContributions = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/contributions`);
  return response.data;
};
