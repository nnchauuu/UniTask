import {
  ArrowRight,
  CheckCircle2,
  ChevronRight,
  FolderKanban,
  LayoutDashboard,
  ListChecks,
  MessageSquareText,
  PanelsTopLeft,
  UsersRound,
} from "lucide-react";
import { Link } from "react-router-dom";

const hierarchy = [
  {
    icon: UsersRound,
    title: "Workspace",
    description: "Không gian của cả nhóm",
  },
  {
    icon: FolderKanban,
    title: "Project",
    description: "Mục tiêu và thành viên",
  },
  {
    icon: PanelsTopLeft,
    title: "Board",
    description: "Trạng thái công việc",
  },
  {
    icon: CheckCircle2,
    title: "Task",
    description: "Người phụ trách và hạn chót",
  },
];

const capabilities = [
  {
    icon: UsersRound,
    title: "Workspace và project",
    description: "Tổ chức thành viên và mục tiêu trong một cấu trúc chung.",
  },
  {
    icon: ListChecks,
    title: "Task rõ trách nhiệm",
    description: "Giao việc, đặt hạn và xác định người phụ trách.",
  },
  {
    icon: LayoutDashboard,
    title: "Board theo tiến độ",
    description: "Theo dõi việc cần làm, đang làm và đã hoàn thành.",
  },
  {
    icon: MessageSquareText,
    title: "Cộng tác đúng ngữ cảnh",
    description: "Bình luận và cập nhật ngay trong công việc liên quan.",
  },
];

function HomePage() {
  return (
    <div className="home-page">
      <header className="home-header">
        <nav className="home-nav container" aria-label="Điều hướng chính">
          <Link className="home-brand" to="/" aria-label="Trang chủ UniTask">
            <span className="home-brand-mark" aria-hidden="true">U</span>
            <span>UniTask</span>
          </Link>

          <div className="home-nav-links">
            <a href="#gioi-thieu">Trang chủ</a>
            <a href="#tinh-nang">Tính năng</a>
            <a href="#cach-hoat-dong">Cách hoạt động</a>
          </div>

          <Link className="home-login-link" to="/login">Đăng nhập</Link>
        </nav>
      </header>

      <main>
        <section id="gioi-thieu" className="home-hero" aria-labelledby="home-title">
          <div className="home-hero-inner container">
            <div className="home-copy">
              <h1 id="home-title">Dự án nhóm. Tiến độ rõ ràng.</h1>
              <p className="home-description">
                Quản lý workspace, project, board và task tại một nơi.
              </p>
              <div className="home-hero-actions">
                <Link className="home-primary-button" to="/register">
                  Bắt đầu miễn phí
                  <ArrowRight size={18} strokeWidth={1.8} aria-hidden="true" />
                </Link>
              </div>
            </div>

            <figure className="home-visual">
              <img
                src="/images/unitask-home-hero.png"
                alt="Ba sinh viên cùng lập kế hoạch cho một dự án nhóm"
                width="1750"
                height="899"
                fetchPriority="high"
              />
            </figure>
          </div>
        </section>

        <section id="cach-hoat-dong" className="home-hierarchy" aria-labelledby="hierarchy-title">
          <div className="container">
            <div className="home-section-heading">
              <h2 id="hierarchy-title">Từ workspace đến task.</h2>
              <p>Một cấu trúc chung cho mọi thành viên.</p>
            </div>

            <div
              className="home-hierarchy-flow"
              role="group"
              aria-label="Cấu trúc quản lý công việc trên UniTask"
            >
              {hierarchy.map(({ icon: Icon, title, description }, index) => (
                <div className="home-hierarchy-segment" key={title}>
                  <div className="home-hierarchy-item">
                    <Icon size={22} strokeWidth={1.8} aria-hidden="true" />
                    <div>
                      <strong>{title}</strong>
                      <span>{description}</span>
                    </div>
                  </div>
                  {index < hierarchy.length - 1 && (
                    <ChevronRight className="home-hierarchy-arrow" size={18} strokeWidth={1.8} aria-hidden="true" />
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="tinh-nang" className="home-features" aria-labelledby="features-title">
          <div className="home-features-inner container">
            <div className="home-features-intro">
              <h2 id="features-title">Mọi thứ nhóm cần để hoàn thành công việc.</h2>
            </div>

            <div className="home-capability-list">
              {capabilities.map(({ icon: Icon, title, description }) => (
                <article className="home-capability-item" key={title}>
                  <Icon size={22} strokeWidth={1.8} aria-hidden="true" />
                  <div>
                    <h3>{title}</h3>
                    <p>{description}</p>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

      </main>

      <footer className="home-footer">
        <div className="container">
          <Link className="home-brand home-footer-brand" to="/" aria-label="Trang chủ UniTask">
            <span className="home-brand-mark" aria-hidden="true">U</span>
            <span>UniTask</span>
          </Link>
          <p>Học tập có tổ chức. Làm việc nhóm có tiến độ.</p>
        </div>
      </footer>
    </div>
  );
}

export default HomePage;
