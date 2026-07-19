import axiosClient from "./axiosClient";

export const getWorkspaces = async () => {
  const response = await axiosClient.get("/workspaces");
  return response.data;
};

export const createWorkspace = async (payload) => {
  const response = await axiosClient.post("/workspaces", payload);
  return response.data;
};

export const getWorkspaceDetail = async (workspaceId) => {
  const response = await axiosClient.get(`/workspaces/${workspaceId}`);
  return response.data;
};

export const updateWorkspace = async (workspaceId, payload) => {
  const response = await axiosClient.put(`/workspaces/${workspaceId}`, payload);
  return response.data;
};

export const deleteWorkspace = async (workspaceId) => {
  const response = await axiosClient.delete(`/workspaces/${workspaceId}`);
  return response.data;
};

export const addWorkspaceMember = async (workspaceId, payload) => {
  const response = await axiosClient.post(`/workspaces/${workspaceId}/members`, payload);
  return response.data;
};

export const removeWorkspaceMember = async (workspaceId, userId) => {
  const response = await axiosClient.delete(`/workspaces/${workspaceId}/members/${userId}`);
  return response.data;
};

export const updateWorkspaceMemberRole = async (workspaceId, userId, payload) => {
  const response = await axiosClient.put(`/workspaces/${workspaceId}/members/${userId}/role`, payload);
  return response.data;
};
