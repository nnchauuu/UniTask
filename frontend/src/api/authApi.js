import axiosClient from "./axiosClient";

export const register = async (payload) => {
  const response = await axiosClient.post("/auth/register", payload);
  return response.data;
};

export const login = async (payload) => {
  const response = await axiosClient.post("/auth/login", payload);
  return response.data;
};

export const googleLogin = async (payload) => {
  const response = await axiosClient.post("/auth/google", payload, {
    headers: { "X-Requested-With": "XMLHttpRequest" }
  });
  return response.data;
};

export const getMe = async () => {
  const response = await axiosClient.get("/auth/me");
  return response.data;
};
