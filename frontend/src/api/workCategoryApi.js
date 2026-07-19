import axiosClient from "./axiosClient";
export const getWorkCategories=async(projectId)=>(await axiosClient.get(`/projects/${projectId}/work-categories`)).data;
export const createWorkCategory=async(projectId,payload)=>(await axiosClient.post(`/projects/${projectId}/work-categories`,payload)).data;
export const updateWorkCategory=async(id,payload)=>(await axiosClient.put(`/work-categories/${id}`,payload)).data;
export const reorderWorkCategories=async(projectId,categoryIds)=>(await axiosClient.put(`/projects/${projectId}/work-categories/order`,{categoryIds})).data;
export const setWorkCategoryActive=async(id,active)=>(await axiosClient.post(`/work-categories/${id}/${active?"activate":"deactivate"}`)).data;
