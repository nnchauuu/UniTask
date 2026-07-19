import { useEffect, useMemo, useRef, useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Copy,
  Download,
  Eye,
  File,
  FileImage,
  FileSpreadsheet,
  FileText,
  Folder,
  FolderInput,
  Grid2X2,
  History,
  List,
  MessageSquare,
  MoreHorizontal,
  RotateCcw,
  Search,
  Star,
  Trash2,
  Upload,
  X
} from "lucide-react";
import * as fileApi from "../api/fileApi";

const STORAGE_LIMIT = 1024 * 1024 * 1024;

const formatFileSize = (size = 0) => {
  const units = ["B", "KB", "MB", "GB"];
  let value = Number(size) || 0;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
};

const formatDate = (value) => {
  if (!value) return "Vừa xong";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  }).format(new Date(value));
};

const getExtension = (name = "") => name.split(".").pop()?.toLowerCase() || "";

const getFileKind = (name = "") => {
  const extension = getExtension(name);
  if (["png", "jpg", "jpeg", "gif", "webp", "svg"].includes(extension)) return "image";
  if (extension === "pdf") return "pdf";
  if (["xls", "xlsx", "csv"].includes(extension)) return "sheet";
  if (["doc", "docx", "txt", "md"].includes(extension)) return "document";
  return "other";
};

const fileIcon = (name, size = 18) => {
  const kind = getFileKind(name);
  if (kind === "image") return <FileImage size={size} />;
  if (kind === "sheet") return <FileSpreadsheet size={size} />;
  if (kind === "pdf" || kind === "document") return <FileText size={size} />;
  return <File size={size} />;
};

const getFileSource = (file) => {
  if (file.task?.title || file.taskTitle) return { label: file.task?.title || file.taskTitle, type: "task" };
  if (file.meeting?.title || file.meetingTitle) return { label: file.meeting?.title || file.meetingTitle, type: "meeting" };
  return { label: "Dự án", type: "project" };
};

const defaultMeta = { folders: [], files: {}, comments: {} };

function ProjectDocuments({ project, files, currentUserId, canManageFiles, onUpload, onDownload, onDelete }) {
  const inputRef = useRef(null);
  const [activeTab, setActiveTab] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState("all");
  const [uploaderFilter, setUploaderFilter] = useState("all");
  const [sourceFilter, setSourceFilter] = useState("all");
  const [sortOrder, setSortOrder] = useState("recent");
  const [viewMode, setViewMode] = useState("list");
  const [dragging, setDragging] = useState(false);
  const [uploadQueue, setUploadQueue] = useState([]);
  const [openMenuId, setOpenMenuId] = useState(null);
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [detailDialog, setDetailDialog] = useState(null);
  const [commentText, setCommentText] = useState("");
  const [meta, setMeta] = useState(defaultMeta);

  const storageKey = `unitask-project-documents-${project.id}`;

  useEffect(() => {
    try {
      setMeta({ ...defaultMeta, ...JSON.parse(localStorage.getItem(storageKey) || "{}") });
    } catch {
      setMeta(defaultMeta);
    }
  }, [storageKey]);

  const persistMeta = (updater) => {
    setMeta((current) => {
      const next = typeof updater === "function" ? updater(current) : updater;
      localStorage.setItem(storageKey, JSON.stringify(next));
      return next;
    });
  };

  useEffect(() => () => {
    if (preview?.url) window.URL.revokeObjectURL(preview.url);
  }, [preview]);

  const normalizedFiles = useMemo(() => files.map((file) => {
    const local = meta.files[file.id] || {};
    return {
      ...file,
      ...local,
      displayName: local.name || file.originalName || file.fileName || "Tệp không tên"
    };
  }), [files, meta.files]);

  const uploaders = useMemo(() => {
    const values = new Map();
    normalizedFiles.forEach((file) => {
      if (file.uploadedBy?.id) values.set(String(file.uploadedBy.id), file.uploadedBy.fullName || "Thành viên");
    });
    return [...values.entries()];
  }, [normalizedFiles]);

  const visibleFiles = useMemo(() => normalizedFiles
    .filter((file) => {
      const local = meta.files[file.id] || {};
      if (activeTab === "trash") return local.trashed;
      if (local.trashed) return false;
      if (activeTab === "starred" && !local.starred) return false;
      if (activeTab === "mine" && String(file.uploadedBy?.id) !== String(currentUserId)) return false;
      if (activeTab === "recent") {
        const age = Date.now() - new Date(file.createdAt || 0).getTime();
        if (age > 7 * 86400000) return false;
      }
      const source = getFileSource(file);
      const term = searchTerm.trim().toLocaleLowerCase("vi");
      if (term && !`${file.displayName} ${file.uploadedBy?.fullName || ""} ${source.label}`.toLocaleLowerCase("vi").includes(term)) return false;
      if (typeFilter !== "all" && getFileKind(file.displayName) !== typeFilter) return false;
      if (uploaderFilter !== "all" && String(file.uploadedBy?.id) !== uploaderFilter) return false;
      if (sourceFilter !== "all" && source.type !== sourceFilter) return false;
      return true;
    })
    .sort((a, b) => {
      if (sortOrder === "name") return a.displayName.localeCompare(b.displayName, "vi");
      if (sortOrder === "size") return (b.fileSize || 0) - (a.fileSize || 0);
      return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
    }), [normalizedFiles, activeTab, currentUserId, meta.files, searchTerm, typeFilter, uploaderFilter, sourceFilter, sortOrder]);

  const activeFiles = normalizedFiles.filter((file) => !meta.files[file.id]?.trashed);
  const totalSize = activeFiles.reduce((sum, file) => sum + (Number(file.fileSize) || 0), 0);
  const folderCount = meta.folders.length;
  const canEditFile = (file) => canManageFiles || String(file.uploadedBy?.id) === String(currentUserId);

  const updateFileMeta = (fileId, values) => persistMeta((current) => ({
    ...current,
    files: { ...current.files, [fileId]: { ...(current.files[fileId] || {}), ...values } }
  }));

  const handleFiles = async (selectedFiles) => {
    const items = [...selectedFiles];
    if (!items.length) return;
    const queueItems = items.map((file) => ({ id: `${file.name}-${file.lastModified}-${Math.random()}`, file, progress: 4, status: "uploading" }));
    setUploadQueue((current) => [...current, ...queueItems]);

    await Promise.all(queueItems.map(async (item) => {
      const timer = window.setInterval(() => {
        setUploadQueue((current) => current.map((entry) => entry.id === item.id
          ? { ...entry, progress: Math.min(88, entry.progress + Math.ceil(Math.random() * 12)) }
          : entry));
      }, 240);
      try {
        await onUpload(item.file);
        window.clearInterval(timer);
        setUploadQueue((current) => current.map((entry) => entry.id === item.id ? { ...entry, progress: 100, status: "done" } : entry));
        window.setTimeout(() => setUploadQueue((current) => current.filter((entry) => entry.id !== item.id)), 1800);
      } catch (error) {
        window.clearInterval(timer);
        setUploadQueue((current) => current.map((entry) => entry.id === item.id ? { ...entry, status: "error" } : entry));
      }
    }));
    if (inputRef.current) inputRef.current.value = "";
  };

  const createFolder = () => {
    const name = window.prompt("Tên thư mục mới");
    if (!name?.trim()) return;
    persistMeta((current) => ({
      ...current,
      folders: [...current.folders, { id: Date.now(), name: name.trim(), color: ["purple", "blue", "yellow"][current.folders.length % 3] }]
    }));
  };

  const renameFile = (file) => {
    const name = window.prompt("Đổi tên tệp", file.displayName);
    if (name?.trim() && name.trim() !== file.displayName) {
      const versions = meta.files[file.id]?.versions || [];
      updateFileMeta(file.id, {
        name: name.trim(),
        versions: [{ name: file.displayName, date: new Date().toISOString(), author: "Bạn" }, ...versions]
      });
    }
  };

  const moveFile = (file) => {
    if (!meta.folders.length) {
      createFolder();
      return;
    }
    const choices = meta.folders.map((folder, index) => `${index + 1}. ${folder.name}`).join("\n");
    const selected = Number(window.prompt(`Chọn thư mục:\n${choices}`));
    const folder = meta.folders[selected - 1];
    if (folder) updateFileMeta(file.id, { folderId: folder.id });
  };

  const copyShareLink = async (file) => {
    const url = `${window.location.origin}${window.location.pathname}?file=${file.id}`;
    await navigator.clipboard?.writeText(url);
    setOpenMenuId(null);
  };

  const openPreview = async (file) => {
    const kind = getFileKind(file.displayName);
    if (!["image", "pdf"].includes(kind)) return onDownload(file);
    setOpenMenuId(null);
    setPreviewLoading(true);
    setPreview({ file, kind, url: null });
    try {
      const blob = await fileApi.downloadFile(file.id);
      setPreview({ file, kind, url: window.URL.createObjectURL(blob) });
    } finally {
      setPreviewLoading(false);
    }
  };

  const permanentlyDelete = async (file) => {
    await onDelete(file);
    persistMeta((current) => {
      const nextFiles = { ...current.files };
      delete nextFiles[file.id];
      return { ...current, files: nextFiles };
    });
  };

  const addComment = (file) => {
    if (!commentText.trim()) return;
    persistMeta((current) => ({
      ...current,
      comments: {
        ...current.comments,
        [file.id]: [...(current.comments[file.id] || []), { id: Date.now(), text: commentText.trim(), date: new Date().toISOString() }]
      }
    }));
    setCommentText("");
  };

  return (
    <section
      className={`project-documents ${dragging ? "is-dragging" : ""}`}
      onDragEnter={(event) => { event.preventDefault(); setDragging(true); }}
      onDragOver={(event) => event.preventDefault()}
      onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setDragging(false); }}
      onDrop={(event) => { event.preventDefault(); setDragging(false); handleFiles(event.dataTransfer.files); }}
    >
      <header className="documents-heading">
        <div>
          <div className="documents-breadcrumb"><span>{project.workspaceName || "Dự án"}</span><b>/</b>{project.name}</div>
          <h1>Tài liệu</h1>
          <p>Quản lý và chia sẻ tài liệu trong dự án</p>
          <div className="documents-summary">{activeFiles.length} tệp <span>•</span> {folderCount} thư mục <span>•</span> {formatFileSize(totalSize)}</div>
        </div>
        <div className="documents-heading-actions">
          <button type="button" className="documents-secondary-button" onClick={createFolder}><FolderInput size={17} /> Tạo thư mục</button>
          <button type="button" className="documents-upload-button" onClick={() => inputRef.current?.click()}><Upload size={17} /> Tải tệp lên</button>
          <button type="button" className="documents-icon-button" title="Thêm tùy chọn"><MoreHorizontal size={19} /></button>
          <input ref={inputRef} type="file" multiple hidden onChange={(event) => handleFiles(event.target.files)} />
        </div>
      </header>

      <nav className="documents-tabs" aria-label="Bộ sưu tập tài liệu">
        {[
          ["all", "Tất cả"], ["recent", "Gần đây"], ["mine", "Của tôi"], ["starred", "Đã đánh dấu"], ["trash", "Thùng rác"]
        ].map(([key, label]) => <button key={key} className={activeTab === key ? "active" : ""} type="button" onClick={() => setActiveTab(key)}>{label}</button>)}
      </nav>

      <div className="documents-toolbar">
        <label className="documents-search"><Search size={17} /><input value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Tìm tài liệu..." /></label>
        <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} aria-label="Lọc loại tệp">
          <option value="all">Loại tệp</option><option value="pdf">PDF</option><option value="image">Hình ảnh</option><option value="document">Văn bản</option><option value="sheet">Bảng tính</option>
        </select>
        <select value={uploaderFilter} onChange={(event) => setUploaderFilter(event.target.value)} aria-label="Lọc người tải">
          <option value="all">Người tải</option>{uploaders.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
        </select>
        <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)} aria-label="Lọc nguồn">
          <option value="all">Nguồn</option><option value="project">Dự án</option><option value="task">Công việc</option><option value="meeting">Cuộc họp</option>
        </select>
        <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)} aria-label="Sắp xếp">
          <option value="recent">Cập nhật gần đây</option><option value="name">Tên A - Z</option><option value="size">Dung lượng lớn nhất</option>
        </select>
        <div className="documents-view-toggle">
          <button className={viewMode === "list" ? "active" : ""} type="button" onClick={() => setViewMode("list")} title="Danh sách"><List size={17} /></button>
          <button className={viewMode === "grid" ? "active" : ""} type="button" onClick={() => setViewMode("grid")} title="Lưới"><Grid2X2 size={17} /></button>
        </div>
      </div>

      {activeTab !== "trash" && meta.folders.length > 0 && (
        <div className="documents-folders-section">
          <div className="documents-section-title"><h2>Thư mục</h2><button type="button">Xem tất cả</button></div>
          <div className="documents-folder-grid">
            {meta.folders.slice(0, 3).map((folder) => (
              <button key={folder.id} type="button" className="documents-folder-card">
                <span className={`folder-icon ${folder.color}`}><Folder size={24} /></span>
                <span><b>{folder.name}</b><small>{normalizedFiles.filter((file) => meta.files[file.id]?.folderId === folder.id && !meta.files[file.id]?.trashed).length} tệp</small></span>
                <MoreHorizontal size={18} />
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="documents-files-section">
        <div className="documents-section-title"><h2>{activeTab === "trash" ? "Thùng rác" : "Tất cả tài liệu"} <span>{visibleFiles.length}</span></h2></div>
        {visibleFiles.length === 0 ? (
          <div className="documents-empty"><FileText size={34} /><b>Chưa có tài liệu phù hợp</b><span>Kéo thả tệp vào đây hoặc chọn Tải tệp lên.</span></div>
        ) : viewMode === "list" ? (
          <div className="documents-table-wrap">
            <table className="documents-table">
              <thead><tr><th>Tên tệp</th><th>Nguồn</th><th>Người tải</th><th>Cập nhật</th><th>Dung lượng</th><th aria-label="Thao tác" /></tr></thead>
              <tbody>{visibleFiles.map((file) => {
                const source = getFileSource(file);
                const starred = meta.files[file.id]?.starred;
                return (
                  <tr key={file.id}>
                    <td><button type="button" className={`file-type-icon ${getFileKind(file.displayName)}`} onClick={() => openPreview(file)}>{fileIcon(file.displayName)}</button><button type="button" className="file-name-button" onClick={() => openPreview(file)}>{file.displayName}</button></td>
                    <td><span className={`file-source ${source.type}`}>{source.label}</span></td>
                    <td><span className="file-uploader-avatar">{file.uploadedBy?.fullName?.charAt(0) || "U"}</span>{file.uploadedBy?.fullName || "Không rõ"}</td>
                    <td>{formatDate(file.updatedAt || file.createdAt)}</td>
                    <td>{formatFileSize(file.fileSize)}</td>
                    <td className="file-actions-cell">
                      {starred && <Star className="file-starred" size={16} fill="currentColor" />}
                      {activeTab === "trash" ? (
                        <div className="trash-actions"><button type="button" title="Khôi phục" onClick={() => updateFileMeta(file.id, { trashed: false })}><RotateCcw size={16} /></button>{canEditFile(file) && <button type="button" title="Xóa vĩnh viễn" onClick={() => permanentlyDelete(file)}><Trash2 size={16} /></button>}</div>
                      ) : (
                        <div className="document-menu-wrap">
                          <button type="button" className="file-more-button" onClick={() => setOpenMenuId(openMenuId === file.id ? null : file.id)}><MoreHorizontal size={18} /></button>
                          {openMenuId === file.id && <FileMenu file={file} canEdit={canEditFile(file)} starred={starred} onPreview={openPreview} onDownload={onDownload} onStar={() => updateFileMeta(file.id, { starred: !starred })} onShare={copyShareLink} onRename={renameFile} onMove={moveFile} onDetail={setDetailDialog} onTrash={() => updateFileMeta(file.id, { trashed: true })} />}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}</tbody>
            </table>
            <footer className="documents-pagination"><span>Hiển thị 1–{Math.min(visibleFiles.length, 12)} trong {visibleFiles.length} tệp</span><div><button type="button" disabled><ChevronLeft size={15} /></button><button type="button" className="active">1</button><button type="button" disabled><ChevronRight size={15} /></button></div></footer>
          </div>
        ) : (
          <div className="documents-file-grid">{visibleFiles.map((file) => (
            <button key={file.id} type="button" className="documents-file-card" onClick={() => openPreview(file)}>
              <span className={`file-type-icon ${getFileKind(file.displayName)}`}>{fileIcon(file.displayName, 25)}</span><b>{file.displayName}</b><small>{formatFileSize(file.fileSize)} • {formatDate(file.createdAt)}</small>
            </button>
          ))}</div>
        )}
      </div>

      <div className="documents-storage"><div><span>Dung lượng đã sử dụng</span><b>{formatFileSize(totalSize)} / 1 GB</b></div><div className="documents-storage-track"><span style={{ width: `${Math.min(100, (totalSize / STORAGE_LIMIT) * 100)}%` }} /></div></div>

      {dragging && <div className="documents-drop-overlay"><Upload size={36} /><b>Thả tệp để tải lên</b><span>Có thể tải nhiều tệp cùng lúc</span></div>}

      {uploadQueue.length > 0 && <div className="documents-upload-queue">{uploadQueue.map((item) => (
        <div key={item.id} className={`upload-progress-card ${item.status}`}>
          <Upload size={18} /><div><b>{item.status === "error" ? "Tải lên thất bại" : item.status === "done" ? "Đã tải lên" : `Đang tải ${item.file.name}`}</b><div><span style={{ width: `${item.progress}%` }} /></div><small>{item.progress}%</small></div><button type="button" onClick={() => setUploadQueue((current) => current.filter((entry) => entry.id !== item.id))}><X size={17} /></button>
        </div>
      ))}</div>}

      {preview && <div className="document-dialog-backdrop" onMouseDown={() => setPreview(null)}><div className="document-preview-dialog" onMouseDown={(event) => event.stopPropagation()}><header><div>{fileIcon(preview.file.displayName)}<b>{preview.file.displayName}</b></div><button type="button" onClick={() => setPreview(null)}><X size={20} /></button></header><div className="document-preview-body">{previewLoading ? <span>Đang tải bản xem trước...</span> : preview.kind === "image" ? <img src={preview.url} alt={preview.file.displayName} /> : <iframe src={preview.url} title={preview.file.displayName} />}</div></div></div>}

      {detailDialog && <div className="document-dialog-backdrop" onMouseDown={() => setDetailDialog(null)}><div className="document-detail-dialog" onMouseDown={(event) => event.stopPropagation()}><header><div>{detailDialog.mode === "comments" ? <MessageSquare size={20} /> : <History size={20} />}<b>{detailDialog.mode === "comments" ? "Bình luận trên tệp" : "Lịch sử phiên bản"}</b></div><button type="button" onClick={() => setDetailDialog(null)}><X size={20} /></button></header>{detailDialog.mode === "comments" ? <div className="document-comments"><div className="document-comment-list">{(meta.comments[detailDialog.file.id] || []).map((comment) => <div key={comment.id}><span>U</span><p><b>Bạn</b>{comment.text}<small>{formatDate(comment.date)}</small></p></div>)}{!(meta.comments[detailDialog.file.id] || []).length && <p className="document-dialog-empty">Chưa có bình luận nào.</p>}</div><form onSubmit={(event) => { event.preventDefault(); addComment(detailDialog.file); }}><input value={commentText} onChange={(event) => setCommentText(event.target.value)} placeholder="Viết bình luận..." /><button type="submit">Gửi</button></form></div> : <div className="document-version-list"><div><History size={17} /><span><b>Phiên bản hiện tại</b><small>{detailDialog.file.displayName} • {formatDate(detailDialog.file.updatedAt || detailDialog.file.createdAt)}</small></span></div>{(meta.files[detailDialog.file.id]?.versions || []).map((version, index) => <div key={`${version.date}-${index}`}><RotateCcw size={17} /><span><b>{version.name}</b><small>{version.author} • {formatDate(version.date)}</small></span></div>)}</div>}</div></div>}
    </section>
  );
}

function FileMenu({ file, canEdit, starred, onPreview, onDownload, onStar, onShare, onRename, onMove, onDetail, onTrash }) {
  return (
    <div className="document-file-menu">
      <button type="button" onClick={() => onPreview(file)}><Eye size={16} /> Xem trước</button>
      <button type="button" onClick={() => onDownload(file)}><Download size={16} /> Tải xuống</button>
      <button type="button" onClick={onStar}><Star size={16} /> {starred ? "Bỏ đánh dấu" : "Đánh dấu quan trọng"}</button>
      <button type="button" onClick={() => onShare(file)}><Copy size={16} /> Sao chép liên kết</button>
      <button type="button" onClick={() => onDetail({ mode: "comments", file })}><MessageSquare size={16} /> Bình luận</button>
      <button type="button" onClick={() => onDetail({ mode: "versions", file })}><History size={16} /> Lịch sử phiên bản</button>
      {canEdit && <><hr /><button type="button" onClick={() => onRename(file)}><FileText size={16} /> Đổi tên</button><button type="button" onClick={() => onMove(file)}><FolderInput size={16} /> Di chuyển</button><button type="button" className="danger" onClick={onTrash}><Trash2 size={16} /> Chuyển vào thùng rác</button></>}
    </div>
  );
}

export default ProjectDocuments;
