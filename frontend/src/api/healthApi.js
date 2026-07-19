import axiosClient from "./axiosClient";

export const getHealth = async () => {
  const response = await axiosClient.get("/health");
  return response.data;
};
