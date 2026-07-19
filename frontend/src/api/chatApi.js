import { Client } from "@stomp/stompjs";
import axiosClient from "./axiosClient";

export const getProjectMessages = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/messages`);
  return response.data;
};

export const createChatClient = ({ token, projectId, onMessage, onError }) => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  const wsBaseUrl = apiBaseUrl.replace(/^http/, "ws").replace(/\/api\/?$/, "");

  const client = new Client({
    brokerURL: `${wsBaseUrl}/ws`,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/projects/${projectId}/chat`, (message) => {
        onMessage(JSON.parse(message.body));
      });
    },
    onStompError: (frame) => {
      onError?.(frame.headers?.message || "Lỗi kết nối trò chuyện");
    },
    onWebSocketError: () => {
      onError?.("Không thể kết nối tới máy chủ trò chuyện");
    }
  });

  return client;
};
