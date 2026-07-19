import axiosClient from "./axiosClient";

export const getWorkspaceProjects = async (workspaceId) => {
  const response = await axiosClient.get(`/workspaces/${workspaceId}/projects`);
  return response.data;
};

export const createProject = async (workspaceId, payload) => {
  const response = await axiosClient.post(`/workspaces/${workspaceId}/projects`, payload);
  return response.data;
};

export const getProjectDetail = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}`);
  return response.data;
};

export const updateProject = async (projectId, payload) => {
  const response = await axiosClient.put(`/projects/${projectId}`, payload);
  return response.data;
};

export const deleteProject = async (projectId) => {
  const response = await axiosClient.delete(`/projects/${projectId}`);
  return response.data;
};
