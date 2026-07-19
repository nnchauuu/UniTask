function formatFileSize(size) {
  if (!size) {
    return "0 B";
  }

  const units = ["B", "KB", "MB", "GB"];
  let value = size;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  return `${value.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function FileList({ files, currentUserId, canManageFiles, onDownload, onDelete }) {
  const canDelete = (file) => canManageFiles || file.uploadedBy?.id === currentUserId;

  if (files.length === 0) {
    return <div className="text-secondary small">Chưa có tệp nào.</div>;
  }

  return (
    <div className="list-group file-list">
      {files.map((file) => (
        <div className="list-group-item" key={file.id}>
          <div className="d-flex justify-content-between align-items-start gap-3">
            <div>
              <div className="fw-semibold">{file.originalName}</div>
              <div className="small text-secondary">
                {formatFileSize(file.fileSize)} - {file.uploadedBy?.fullName || "Không rõ"} -{" "}
                {new Date(file.createdAt).toLocaleString()}
              </div>
            </div>
            <div className="d-flex gap-2">
              <button className="btn btn-outline-primary btn-sm" onClick={() => onDownload(file)}>
                Tải xuống
              </button>
              {canDelete(file) && (
                <button className="btn btn-outline-danger btn-sm" onClick={() => onDelete(file)}>
                  Xóa
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default FileList;
