import { useEffect, useMemo, useRef, useState } from "react";
import { Bell } from "lucide-react";
import * as notificationApi from "../api/notificationApi";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState("");
  const clientRef = useRef(null);
  const { token, user } = useAuth();
  const navigate = useNavigate();

  const unreadCount = useMemo(
    () => notifications.filter((notification) => !notification.isRead).length,
    [notifications]
  );

  const loadNotifications = async () => {
    if (!token) {
      return;
    }

    try {
      const response = await notificationApi.getNotifications();
      setNotifications(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải thông báo");
    }
  };

  useEffect(() => {
    loadNotifications();
  }, [token]);

  useEffect(() => {
    if (!token || !user?.id) {
      return undefined;
    }

    const client = notificationApi.createNotificationClient({
      token,
      userId: user.id,
      onNotification: (notification) => {
        setNotifications((current) => {
          if (current.some((item) => item.id === notification.id)) {
            return current;
          }

          return [notification, ...current];
        });
      },
      onError: setError
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [token, user?.id]);

  const handleMarkAsRead = async (notification) => {
    if (notification.isRead) {
      return;
    }

    try {
      const response = await notificationApi.markNotificationAsRead(notification.id);
      setNotifications((current) =>
        current.map((item) => (item.id === notification.id ? response.data : item))
      );
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật thông báo");
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllNotificationsAsRead();
      setNotifications((current) => current.map((item) => ({ ...item, isRead: true })));
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật thông báo");
    }
  };

  return (
    <div className="notification-bell">
      <button
        className="topbar-notification-button"
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-label="Thông báo"
      >
        <Bell size={19} />
        {unreadCount > 0 && (
          <span className="topbar-notification-count">
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="notification-menu bg-white border rounded shadow-sm">
          <div className="d-flex justify-content-between align-items-center gap-2 p-3 border-bottom">
            <h2 className="h6 fw-bold mb-0">Thông báo</h2>
            <button className="btn btn-outline-secondary btn-sm" onClick={handleMarkAllAsRead}>
              Đọc tất cả
            </button>
          </div>
          {error && <div className="alert alert-danger m-3 mb-0">{error}</div>}
          {notifications.length === 0 && (
            <div className="text-secondary small p-3">Chưa có thông báo.</div>
          )}
          <div className="notification-list">
            {notifications.map((notification) => (
              <button
                className={`notification-item text-start w-100 border-0 border-bottom p-3 ${
                  notification.isRead ? "bg-white" : "notification-unread"
                }`}
                key={notification.id}
                onClick={async () => { await handleMarkAsRead(notification); setOpen(false); if (notification.projectId) navigate(`/projects/${notification.projectId}?tab=planning${notification.taskId ? `&taskId=${notification.taskId}` : ""}`); }}
              >
                <div className="d-flex justify-content-between align-items-start gap-2">
                  <span className="fw-semibold">{notification.title}</span>
                  {!notification.isRead && <span className="badge text-bg-primary">Mới</span>}
                </div>
                <div className="small text-secondary">{notification.content}</div>
                <div className="small text-secondary mt-1">
                  {new Date(notification.createdAt).toLocaleString()}
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default NotificationBell;
