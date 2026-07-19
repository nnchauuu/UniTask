import axiosClient from "./axiosClient";

export const getColumns = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/board-columns`);
  return response.data;
};

export const createColumn = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/board-columns`, payload);
  return response.data;
};

export const updateColumn = async (projectId, columnId, payload) => {
  const response = await axiosClient.put(`/projects/${projectId}/board-columns/${columnId}`, payload);
  return response.data;
};

export const reorderColumns = async (projectId, columnIds) => {
  const response = await axiosClient.put(`/projects/${projectId}/board-columns/order`, { columnIds });
  return response.data;
};

export const deleteColumn = async (projectId, columnId, destinationColumnId) => {
  const response = await axiosClient.delete(`/projects/${projectId}/board-columns/${columnId}`, {
    data: { destinationColumnId }
  });
  return response.data;
};

export const moveTask = async (taskId, columnId, position) => {
  const response = await axiosClient.put(`/tasks/${taskId}/board-position`, { columnId, position });
  return response.data;
};
