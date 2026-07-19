function MemberList({ members, myRole, onRoleChange, onRemove }) {
  const canManage = myRole === "OWNER";
  const ownersCount = members.filter((member) => member.role === "OWNER").length;

  return (
    <div className="border rounded p-3">
      <h2 className="h5 fw-bold mb-3">Thành viên</h2>
      <div className="table-responsive">
        <table className="table align-middle mb-0">
          <thead>
            <tr>
              <th>Tên</th>
              <th>Email</th>
              <th>Vai trò</th>
              {canManage && <th className="text-end">Thao tác</th>}
            </tr>
          </thead>
          <tbody>
            {members.map((member) => {
              const isLastOwner = member.role === "OWNER" && ownersCount <= 1;

              return (
                <tr key={member.userId}>
                  <td className="fw-semibold">{member.fullName}</td>
                  <td>{member.email}</td>
                  <td>
                    {canManage ? (
                      <select
                        className="form-select form-select-sm role-select"
                        value={member.role}
                        disabled={isLastOwner}
                        onChange={(event) => onRoleChange(member.userId, event.target.value)}
                      >
                        <option value="OWNER">OWNER</option>
                        <option value="LEADER">LEADER</option>
                        <option value="MEMBER">MEMBER</option>
                      </select>
                    ) : (
                      <span className="badge text-bg-secondary">{member.role}</span>
                    )}
                  </td>
                  {canManage && (
                    <td className="text-end">
                      <button
                        className="btn btn-outline-danger btn-sm"
                        disabled={isLastOwner}
                        onClick={() => onRemove(member)}
                      >
                        Xóa
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default MemberList;
