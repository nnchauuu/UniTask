import axiosClient from "./axiosClient";

export const getTemplates = async () => {
  const response = await axiosClient.get("/evaluation-templates");
  return response;
};

export const getProjectEvaluationConfig = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/evaluation/config`);
  return response;
};

export const applyTemplate = async (projectId, templateId) => {
  const response = await axiosClient.post(`/projects/${projectId}/evaluation/config/apply-template`, { templateId });
  return response;
};

export const updateCriteria = async (projectId, criteria) => {
  const response = await axiosClient.put(`/projects/${projectId}/evaluation/config/criteria`, criteria);
  return response;
};

export const validateCriteria = async (projectId, criteria) => {
  const response = await axiosClient.post(`/projects/${projectId}/evaluation/config/criteria/validate`, criteria);
  return response;
};

export const restoreCriteria = async (projectId) => {
  const response = await axiosClient.post(`/projects/${projectId}/evaluation/config/restore`);
  return response;
};

export const saveConfigAsTemplate = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/evaluation/config/save-template`, payload);
  return response;
};

export const createCycle = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/evaluation/cycles`, payload);
  return response;
};

export const getProjectCycles = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/evaluation/cycles`);
  return response;
};

export const getCycleResults = async (cycleId) => {
  const response = await axiosClient.get(`/evaluation/cycles/${cycleId}/results`);
  return response;
};

export const calculateCycle = async (cycleId) => {
  const response = await axiosClient.post(`/evaluation/cycles/${cycleId}/calculate`);
  return response;
};

export const finalizeCycle = async (cycleId) => {
  const response = await axiosClient.post(`/evaluation/cycles/${cycleId}/finalize`);
  return response;
};
