import { Client } from "@stomp/stompjs";
import axiosClient from "./axiosClient";

export const getNotifications = async () => {
  const response = await axiosClient.get("/notifications");
  return response.data;
};

export const markNotificationAsRead = async (id) => {
  const response = await axiosClient.patch(`/notifications/${id}/read`);
  return response.data;
};

export const markAllNotificationsAsRead = async () => {
  const response = await axiosClient.patch("/notifications/read-all");
  return response.data;
};

export const createNotificationClient = ({ token, userId, onNotification, onError }) => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  const wsBaseUrl = apiBaseUrl.replace(/^http/, "ws").replace(/\/api\/?$/, "");

  return new Client({
    brokerURL: `${wsBaseUrl}/ws`,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    onConnect: function onConnect() {
      this.subscribe(`/topic/users/${userId}/notifications`, (message) => {
        onNotification(JSON.parse(message.body));
      });
    },
    onStompError: (frame) => {
      onError?.(frame.headers?.message || "Lỗi kết nối thông báo");
    },
    onWebSocketError: () => {
      onError?.("Không thể kết nối tới máy chủ thông báo");
    }
  });
};
