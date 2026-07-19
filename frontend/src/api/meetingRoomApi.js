import { Client } from "@stomp/stompjs";
import axiosClient from "./axiosClient";

export const createMeetingRoom = async (projectId, payload) => {
  const response = await axiosClient.post(`/projects/${projectId}/meeting-rooms`, payload);
  return response.data;
};

export const getProjectMeetingRooms = async (projectId) => {
  const response = await axiosClient.get(`/projects/${projectId}/meeting-rooms`);
  return response.data;
};

export const getMeetingRoom = async (roomId) => {
  const response = await axiosClient.get(`/meeting-rooms/${roomId}`);
  return response.data;
};

export const createMeetingRoomClient = ({ token, roomId, onSignal, onError }) => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  const wsBaseUrl = apiBaseUrl.replace(/^http/, "ws").replace(/\/api\/?$/, "");

  const client = new Client({
    brokerURL: `${wsBaseUrl}/ws`,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/meeting-rooms/${roomId}/signal`, (message) => {
        onSignal(JSON.parse(message.body));
      });
    },
    onStompError: (frame) => {
      onError?.(frame.headers?.message || "Loi signaling phong hop");
    },
    onWebSocketError: () => {
      onError?.("Khong the ket noi signaling server");
    }
  });

  return client;
};
