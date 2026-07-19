import { useEffect, useMemo, useState } from "react";
import { Check, MoreHorizontal } from "lucide-react";
import * as notificationApi from "../api/notificationApi";
import { useToast } from "../context/ToastContext";

const notificationTabs = [
  { key: "all", label: "Tất cả" },
  { key: "unread", label: "Chưa đọc" },
  { key: "mentions", label: "Nhắc đến tôi" },
  { key: "work", label: "Công việc" },
  { key: "system", label: "Hệ thống" }
];

const isMentionNotification = (notification) => notification.type === "TASK_MENTION";

const isWorkNotification = (notification) =>
  notification.type?.startsWith("TASK_") && !isMentionNotification(notification);

const getNotificationGroup = (notification) => {
  if (isMentionNotification(notification)) return "Nhắc đến tôi";
  if (notification.type?.startsWith("TASK_")) return "Công việc";
  if (notification.type === "FILE_UPLOADED") return "Tệp";
  if (notification.type === "MEETING_CREATED") return "Lịch họp";
  if (notification.type?.startsWith("WEEKLY_PLAN_")) return "Kế hoạch tuần";
  return "Hệ thống";
};

const formatNotificationTime = (value) => {
  if (!value) return "";

  const date = new Date(value);
  const elapsed = Date.now() - date.getTime();
  const minutes = Math.max(1, Math.floor(elapsed / 60000));
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);

  if (minutes < 60) return `${minutes} phút trước`;
  if (elapsed < 24 * 60 * 60 * 1000) return `${Math.floor(minutes / 60)} giờ trước`;
  if (date.toDateString() === yesterday.toDateString()) return "Hôm qua";

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  }).format(date);
};

function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [activeTab, setActiveTab] = useState("all");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { showToast } = useToast();

  const loadNotifications = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await notificationApi.getNotifications();
      setNotifications(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải thông báo");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const unreadCount = notifications.filter((notification) => !notification.isRead).length;

  const visibleNotifications = useMemo(() => notifications.filter((notification) => {
    if (activeTab === "unread") return !notification.isRead;
    if (activeTab === "mentions") return isMentionNotification(notification);
    if (activeTab === "work") return isWorkNotification(notification);
    if (activeTab === "system") {
      return !notification.type?.startsWith("TASK_");
    }
    return true;
  }), [activeTab, notifications]);

  const markAsRead = async (notification) => {
    if (notification.isRead) return;

    try {
      const response = await notificationApi.markNotificationAsRead(notification.id);
      setNotifications((current) => current.map((item) => (item.id === notification.id ? response.data : item)));
      showToast("Đã đánh dấu thông báo là đã đọc");
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật thông báo");
    }
  };

  const markAllAsRead = async () => {
    if (unreadCount === 0) return;

    try {
      await notificationApi.markAllNotificationsAsRead();
      setNotifications((current) => current.map((item) => ({ ...item, isRead: true })));
      showToast("Đã đánh dấu tất cả thông báo là đã đọc");
    } catch (err) {
      setError(err.response?.data?.message || "Không thể cập nhật thông báo");
    }
  };

  return (
    <main className="notifications-page">

      <nav className="notifications-tabs" aria-label="Lọc thông báo">
        {notificationTabs.map((tab) => (
          <button
            className={activeTab === tab.key ? "active" : ""}
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            type="button"
          >
            <span>{tab.label}</span>
            {tab.key === "all" && <strong>{notifications.length}</strong>}
            {tab.key === "unread" && <strong>{unreadCount}</strong>}
          </button>

        ))}
        <button className="notifications-mark-all" disabled={unreadCount === 0} onClick={markAllAsRead} type="button">
          <Check aria-hidden="true" size={16} strokeWidth={1.8} />
          <span>Đánh dấu tất cả đã đọc</span>
        </button>
      </nav>

      {error && (
        <div className="notifications-alert" role="alert">
          <span>{error}</span>
          <button onClick={loadNotifications} type="button">Thử lại</button>
        </div>
      )}

      <section className="notifications-list" aria-busy={loading} aria-label="Danh sách thông báo">
        {loading && Array.from({ length: 5 }, (_, index) => (
          <div className="notification-row notification-row-skeleton" key={index} aria-hidden="true">
            <span /><span /><span />
          </div>
        ))}

        {!loading && visibleNotifications.length === 0 && (
          <div className="notifications-empty">
            {notifications.length === 0 ? "Chưa có thông báo." : "Không có thông báo trong mục này."}
          </div>
        )}

        {!loading && visibleNotifications.map((notification) => (
          <button
            className={`notification-row ${notification.isRead ? "is-read" : "is-unread"}`}
            key={notification.id}
            onClick={() => markAsRead(notification)}
            type="button"
          >
            <span className="notification-copy">
              <strong>{notification.title}</strong>
              <span>{notification.content}</span>
            </span>

            <span className="notification-context">
              <span>{getNotificationGroup(notification)}</span>
              <i aria-hidden="true">·</i>
              <time dateTime={notification.createdAt}>{formatNotificationTime(notification.createdAt)}</time>
            </span>

            <span className="notification-more" aria-hidden="true">
              <MoreHorizontal size={18} strokeWidth={2} />
            </span>

            <span className={`notification-read-state ${notification.isRead ? "" : "is-visible"}`} aria-hidden="true" />
          </button>
        ))}
      </section>
    </main>
  );
}

export default Notifications;
