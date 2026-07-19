import { useEffect, useRef, useState } from "react";
import {
  AtSign,
  Bell,
  Hash,
  Image,
  MoreHorizontal,
  Paperclip,
  Plus,
  Search,
  Send,
  Smile,
  UsersRound
} from "lucide-react";
import * as chatApi from "../api/chatApi";
import { useAuth } from "../context/AuthContext";

const initials = (name) => name?.split(/\s+/).slice(-2).map((word) => word[0]).join("").toUpperCase() || "?";

const formatTime = (value) =>
  value ? new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(new Date(value)) : "";

function ChatRoom({ projectId }) {
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const clientRef = useRef(null);
  const bottomRef = useRef(null);
  const { token, user } = useAuth();

  const loadMessages = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await chatApi.getProjectMessages(projectId);
      setMessages(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải tin nhắn");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMessages();
  }, [projectId]);

  useEffect(() => {
    if (!token) {
      return undefined;
    }

    const client = chatApi.createChatClient({
      token,
      projectId,
      onMessage: (message) => {
        setMessages((current) => {
          if (current.some((item) => item.id === message.id)) {
            return current;
          }

          return [...current, message];
        });
      },
      onError: setError
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(`/topic/projects/${projectId}/chat`, (message) => {
        const parsed = JSON.parse(message.body);
        setMessages((current) => {
          if (current.some((item) => item.id === parsed.id)) {
            return current;
          }

          return [...current, parsed];
        });
      });
    };
    client.onDisconnect = () => setConnected(false);
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [token, projectId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!content.trim() || !clientRef.current?.connected) {
      return;
    }

    clientRef.current.publish({
      destination: `/app/projects/${projectId}/chat`,
      body: JSON.stringify({
        content,
        messageType: "TEXT"
      })
    });
    setContent("");
  };

  const directMembers = Array.from(
    new Map(
      messages
        .map((message) => message.sender)
        .filter(Boolean)
        .map((sender) => [sender.id || sender.email || sender.fullName, sender])
    ).values()
  ).slice(0, 5);

  return (
    <section className="project-chat-shell">
      <aside className="project-chat-sidebar">
        <div className="chat-sidebar-title">
          <span>Kênh trò chuyện</span>
          <button type="button" aria-label="Thêm kênh"><Plus size={17} /></button>
        </div>

        <nav className="chat-channel-list" aria-label="Kênh trò chuyện">
          <button className="active" type="button">
            <Hash size={17} />
            <span>Trò chuyện chung</span>
            <b>{messages.length}</b>
          </button>
          <button type="button">
            <Bell size={16} />
            <span>Thông báo</span>
          </button>
        </nav>

        <div className="chat-direct-section">
          <p>Tin nhắn trực tiếp</p>
          <div>
            {(directMembers.length ? directMembers : [user]).filter(Boolean).map((member) => (
              <button key={member.id || member.email || member.fullName} type="button">
                <span>{initials(member.fullName || member.email)}</span>
                <b>{member.fullName || member.email || "Thành viên"}</b>
                <i />
              </button>
            ))}
          </div>
        </div>
      </aside>

      <main className="project-chat-main">
        <header className="project-chat-header">
          <div>
            <h2><Hash size={21} /> Trò chuyện chung</h2>
            <p>Trao đổi chung của dự án · {directMembers.length || 1} thành viên</p>
          </div>
          <div className="project-chat-actions">
            <span className={connected ? "online" : ""}>{connected ? "Đang hoạt động" : "Đang kết nối"}</span>
            <button type="button" aria-label="Tìm kiếm"><Search size={18} /></button>
            <button type="button" aria-label="Thành viên"><UsersRound size={18} /></button>
            <button type="button" aria-label="Tùy chọn"><MoreHorizontal size={18} /></button>
          </div>
        </header>

        {error && <div className="chat-alert chat-alert-danger">{error}</div>}
        {loading && <div className="chat-alert chat-alert-info">Đang tải tin nhắn...</div>}

        <div className="chat-content-frame">
          <div className="chat-day-divider"><span>Hôm nay</span></div>

          <div className="chat-panel">
            {!loading && messages.length === 0 && (
              <div className="chat-empty">Chưa có tin nhắn.</div>
            )}
            <div className="chat-message-list">
              {messages.map((message) => {
                const isMine = message.sender?.id === user?.id;
                const senderName = message.sender?.fullName || "Không rõ";

                return (
                  <article className={`chat-message ${isMine ? "chat-message-own" : ""}`} key={message.id}>
                    <span className="chat-avatar">{initials(senderName)}</span>
                    <div className="chat-message-body">
                      <div className="chat-message-meta">
                        <strong>{senderName}</strong>
                        <span>{formatTime(message.createdAt)}</span>
                      </div>
                      <div className="chat-bubble">{message.content}</div>
                    </div>
                  </article>
                );
              })}
              <div ref={bottomRef} />
            </div>
          </div>

          <form className="chat-composer" onSubmit={handleSubmit}>
            <div className="chat-composer-tools">
              <button type="button" aria-label="Đính kèm"><Paperclip size={18} /></button>
              <button type="button" aria-label="Hình ảnh"><Image size={18} /></button>
              <button type="button" aria-label="Biểu cảm"><Smile size={18} /></button>
              <button type="button" aria-label="Nhắc tên"><AtSign size={18} /></button>
            </div>
            <input
              maxLength={2000}
              placeholder="Nhắn tin tới #trò-chuyện-chung..."
              value={content}
              onChange={(event) => setContent(event.target.value)}
              disabled={!connected}
            />
            <button className="chat-send-button" type="submit" disabled={!connected || !content.trim()} aria-label="Gửi tin nhắn">
              <Send size={19} />
            </button>
          </form>
          <p className="chat-composer-hint">Enter để gửi · Shift + Enter để xuống dòng</p>
        </div>
      </main>
    </section>
  );
}

export default ChatRoom;
