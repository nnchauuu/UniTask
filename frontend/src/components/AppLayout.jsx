import {
  BriefcaseBusiness,
  CheckSquare2,
  Clock3,
  Home,
  Inbox,
  Star
} from "lucide-react";
import { NavLink, useLocation } from "react-router-dom";
import AppNavbar from "./AppNavbar";

const navGroups = [
  {
    items: [
      { to: "/dashboard", label: "Trang chủ", icon: Home },
      { to: "/notifications", label: "Thông báo", icon: Inbox },
      { to: "/my-tasks", label: "Công việc của tôi", icon: CheckSquare2 }
    ]
  },
  {
    items: [
      { to: "/workspaces", label: "Không gian làm việc", icon: BriefcaseBusiness }
    ]
  },
  {
    title: "Truy cập nhanh",
    items: [
      { label: "Gần đây", icon: Clock3, disabled: true },
      { label: "Đã đánh dấu", icon: Star, disabled: true }
    ]
  }
];

function AppLayout({ children }) {
  const location = useLocation();
  const isProjectDetail = /^\/projects\/[^/]+$/.test(location.pathname);

  if (isProjectDetail) {
    return <div className="workspace-focus-root">{children}</div>;
  }

  return (
    <div className="app-layout">
      <aside className="app-sidebar">
        <NavLink className="sidebar-brand" to="/dashboard">
          <span className="sidebar-brand-mark" aria-hidden="true">U</span>
          <span>UniTask</span>
        </NavLink>
        <nav className="sidebar-nav" aria-label="Điều hướng ứng dụng">
          {navGroups.map((group, groupIndex) => (
            <section className="sidebar-nav-group" key={group.title || `main-${groupIndex}`}>
              {group.title && <p className="sidebar-group-title">{group.title}</p>}
              {group.items.map((item) => {
                const Icon = item.icon;

                if (item.disabled) {
                  return (
                    <span className="sidebar-link sidebar-link-disabled" key={item.label} aria-disabled="true">
                      <Icon className="sidebar-icon" size={19} strokeWidth={1.8} />
                      <span>{item.label}</span>
                    </span>
                  );
                }

                return (
                  <NavLink className="sidebar-link" key={item.to} to={item.to}>
                    <Icon className="sidebar-icon" size={19} strokeWidth={1.8} />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </section>
          ))}
        </nav>
      </aside>
      <div className="app-content">
        <AppNavbar />
        <div className="app-main">{children}</div>
      </div>
    </div>
  );
}

export default AppLayout;
