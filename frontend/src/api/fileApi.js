import axiosClient from "./axiosClient";

const buildFormData = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return formData;
};

export const uploadProjectFile = async (projectId, file) => {
  const response = await axiosClient.post(`/projects/${projectId}/files`, buildFormData(file), {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
  return response.data;
};

export const uploadTaskFile = async (taskId, file) => {
  const response = await axiosClient.post(`/tasks/${taskId}/files`, buildFormData(file), {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
  return response.data;
};

export const getProjectFiles = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/files`);
  return response.data;
};

export const getTaskFiles = async (taskId) => {
  const response = await axiosClient.get(`/tasks/${taskId}/files`);
  return response.data;
};

export const downloadFile = async (fileId) => {
  const response = await axiosClient.get(`/files/${fileId}/download`, {
    responseType: "blob"
  });
  return response.data;
};

export const deleteFile = async (fileId) => {
  const response = await axiosClient.delete(`/files/${fileId}`);
  return response.data;
};
