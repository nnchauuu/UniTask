import { ChevronDown, LogOut, Menu, Search } from "lucide-react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import NotificationBell from "./NotificationBell";

function AppNavbar() {
  const { token, user, logout } = useAuth();
  const navigate = useNavigate();
  const userLabel = user?.fullName || user?.email || "user";
  const userInitial = userLabel.trim().charAt(0).toLocaleUpperCase("vi") || "U";

  const handleSearch = (event) => {
    event.preventDefault();
    navigate("/my-tasks");
  };

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="app-topbar">
      <Link className="topbar-mobile-brand" to="/dashboard">
        <span>U</span> UniTask
      </Link>

      <form className="topbar-search" onSubmit={handleSearch} role="search">
        <Search size={18} aria-hidden="true" />
        <input type="search" placeholder="Tìm kiếm công việc, dự án..." aria-label="Tìm kiếm" />
      </form>

      <button
        className="topbar-menu-button"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#mainNavbar"
        aria-label="Mở menu"
      >
        <Menu size={22} />
      </button>

      <div className="collapse navbar-collapse topbar-account" id="mainNavbar">
        {token && (
          <>
            <div className="topbar-mobile-links">
              <NavLink to="/dashboard">Trang chủ</NavLink>
              <NavLink to="/notifications">Hộp thư</NavLink>
              <NavLink to="/my-tasks">Công việc của tôi</NavLink>
              <NavLink to="/workspaces">Không gian làm việc</NavLink>
            </div>

            <NotificationBell />
            <div className="dropdown">
              <button className="topbar-user" data-bs-toggle="dropdown" type="button">
                <span className="topbar-avatar" aria-hidden="true">{userInitial}</span>
                <span className="topbar-user-name">{userLabel}</span>
                <ChevronDown size={15} />
              </button>
              <ul className="dropdown-menu dropdown-menu-end">
                <li><Link className="dropdown-item" to="/profile">Hồ sơ cá nhân</Link></li>
                <li><hr className="dropdown-divider" /></li>
                <li>
                  <button className="dropdown-item text-danger d-flex gap-2 align-items-center" onClick={handleLogout}>
                    <LogOut size={16} /> Đăng xuất
                  </button>
                </li>
              </ul>
            </div>
          </>
        )}
      </div>
    </nav>
  );
}

export default AppNavbar;
