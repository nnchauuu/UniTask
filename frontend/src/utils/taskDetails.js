export const formatFileSize = (value) => {
  const size = Number(value);
  if (!Number.isFinite(size) || size < 0) return "Không rõ dung lượng";
  if (size < 1024) return `${size} B`;
  if (size < 1024 ** 2) return `${Math.round(size / 1024)} KB`;
  return `${(size / (1024 ** 2)).toFixed(size < 10 * 1024 ** 2 ? 1 : 0)} MB`;
};

export const parseTaskLabels = (value) => {
  if (Array.isArray(value)) return value.map((label) => String(label).trim()).filter(Boolean);
  return String(value || "").split(",").map((label) => label.trim()).filter(Boolean);
};
