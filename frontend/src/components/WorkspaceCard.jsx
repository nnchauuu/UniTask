import { Clock3, Pin, UsersRound } from "lucide-react";

const roleLabels = { OWNER: "Chủ sở hữu", LEADER: "Trưởng nhóm", MEMBER: "Thành viên" };

const relativeTime = (value) => {
  if (!value) return "Gần đây";
  const hours = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 3600000));
  if (hours < 1) return "Vừa cập nhật";
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return days === 1 ? "Hôm qua" : `${days} ngày trước`;
};

function WorkspaceCard({ workspace, view = "grid", pinned, selected, onSelect, onTogglePin, colorIndex = 0 }) {
  const initials = workspace.name
    ?.split(/\s+/)
    .slice(0, 2)
    .map((word) => word[0])
    .join("")
    .toUpperCase();

  return (
    <article className={`workspace-library-card workspace-library-card-${view}${selected ? " is-selected" : ""}`}>
      <button
        className="workspace-library-select"
        type="button"
        onClick={() => onSelect(workspace)}
        aria-pressed={selected}
      >
        <span className={`workspace-library-initial workspace-color-${colorIndex % 5}`}>{initials || "UT"}</span>
        <span className="workspace-library-content">
          <span className="workspace-library-name">{workspace.name}</span>
          <span className="workspace-library-description">{workspace.description || "Không gian cộng tác và quản lý công việc nhóm."}</span>
          <span className="workspace-library-meta">
            <span className={`workspace-role workspace-role-${workspace.myRole?.toLowerCase()}`}>
              {roleLabels[workspace.myRole] || workspace.myRole}
            </span>
            <span><UsersRound size={14} /> {workspace.membersCount} thành viên</span>
          </span>
        </span>
        <span className="workspace-library-time"><Clock3 size={14} /> {relativeTime(workspace.updatedAt)}</span>
      </button>
      <div className="workspace-library-actions">
        <button className={pinned ? "is-pinned" : ""} type="button" onClick={() => onTogglePin(workspace.id)} title={pinned ? "Bỏ ghim" : "Ghim"} aria-label={pinned ? `Bỏ ghim ${workspace.name}` : `Ghim ${workspace.name}`}>
          <Pin size={16} fill={pinned ? "currentColor" : "none"} />
        </button>
      </div>
    </article>
  );
}

export default WorkspaceCard;
