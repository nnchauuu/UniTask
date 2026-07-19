import { useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  ChevronDown,
  Eye,
  GripVertical,
  Plus,
  RefreshCw,
  RotateCcw,
  Save,
  ShieldCheck,
  Trash2
} from "lucide-react";
import * as evaluationApi from "../api/evaluationApi";

const evaluationTypes = ["AUTO", "MANUAL", "HYBRID"];
const metricKeys = ["MEETING_ATTENDANCE", "ON_TIME_SUBMISSION", "TASK_COMPLETION", "PROGRESS_UPDATE", "DOCUMENT_CONTRIBUTION"];
const evaluatorTypes = ["LEADER", "PEER", "LEADER_AND_PEER"];

const evaluationTypeLabels = {
  AUTO: "Hệ thống tự tính",
  MANUAL: "Chấm thủ công",
  HYBRID: "Kết hợp"
};

const metricLabels = {
  MEETING_ATTENDANCE: "Mức độ tham gia cuộc họp",
  ON_TIME_SUBMISSION: "Tiến độ hoàn thành đúng hạn",
  TASK_COMPLETION: "Mức độ hoàn thành công việc",
  PROGRESS_UPDATE: "Tần suất cập nhật tiến độ",
  DOCUMENT_CONTRIBUTION: "Đóng góp tài liệu"
};

const evaluatorLabels = {
  LEADER: "Leader",
  PEER: "Đồng đội",
  LEADER_AND_PEER: "Leader và đồng đội"
};

const emptyCriterion = (sortOrder) => ({
  name: "Tiêu chí mới",
  description: "",
  weight: 10,
  evaluationType: "MANUAL",
  metricKey: "",
  scaleMax: 10,
  manualEvaluator: "LEADER",
  requiresEvidence: false,
  requiresComment: true,
  sortOrder,
  active: true
});

const criterionFromTemplate = (criterion) => ({
  ...criterion,
  name: criterion.name || "",
  description: criterion.description || "",
  weight: criterion.weight || 0,
  evaluationType: criterion.evaluationType || "MANUAL",
  metricKey: criterion.metricKey || "",
  scaleMax: criterion.scaleMax || 10,
  manualEvaluator: criterion.manualEvaluator || (criterion.evaluationType === "AUTO" ? "" : "LEADER"),
  requiresEvidence: Boolean(criterion.requiresEvidence),
  requiresComment: Boolean(criterion.requiresComment),
  sortOrder: criterion.sortOrder || 1,
  active: criterion.active !== false
});

function EvaluationCriteriaSettings({ projectId, projectName, canManage }) {
  const [templates, setTemplates] = useState([]);
  const [config, setConfig] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [expandedIndex, setExpandedIndex] = useState(2);
  const [draggedIndex, setDraggedIndex] = useState(null);
  const [showTemplates, setShowTemplates] = useState(false);
  const [cycleName, setCycleName] = useState(`Kỳ đánh giá ${new Date().toLocaleDateString("vi-VN", { month: "2-digit", year: "numeric" })}`);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const templatesResponse = await evaluationApi.getTemplates();
      const availableTemplates = templatesResponse.data || [];
      setTemplates(availableTemplates);
      const basicTemplate = availableTemplates.find((template) => template.level === "BASIC") || availableTemplates[0];
      const configResponse = await evaluationApi.getProjectEvaluationConfig(projectId);
      const projectConfig = configResponse.data;
      const responseCriteria = projectConfig.criteria || [];
      setConfig(projectConfig);
      setCriteria((responseCriteria.length ? responseCriteria : basicTemplate?.criteria || []).map(criterionFromTemplate));
      setSelectedTemplateId(projectConfig.sourceTemplateId || basicTemplate?.id || "");
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải cấu hình tiêu chí đánh giá");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [projectId]);

  const activeCriteria = criteria.filter((item) => item.active);
  const totalWeight = useMemo(() => activeCriteria.reduce((sum, item) => sum + Number(item.weight || 0), 0), [criteria]);
  const canSave = canManage && totalWeight === 100 && activeCriteria.length >= 3 && activeCriteria.length <= 8;
  const selectedTemplate = templates.find((template) => String(template.id) === String(selectedTemplateId));

  const updateCriterion = (index, patch) => {
    setCriteria((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item));
  };

  const buildPayload = () => criteria.map((criterion, index) => ({
    ...criterion,
    weight: Number(criterion.weight),
    scaleMax: Number(criterion.scaleMax),
    sortOrder: index + 1,
    metricKey: criterion.evaluationType === "MANUAL" ? null : criterion.metricKey || null,
    manualEvaluator: criterion.evaluationType === "AUTO" ? null : criterion.manualEvaluator || null
  }));

  const previewTemplate = (templateId) => {
    const template = templates.find((item) => String(item.id) === String(templateId));
    if (!template) return;
    setSelectedTemplateId(templateId);
    setCriteria((template.criteria || []).map(criterionFromTemplate));
    setExpandedIndex(null);
    setMessage(`Đang xem trước mẫu ${template.name}. Chọn Áp dụng để lưu cho dự án.`);
    setError("");
  };

  const applyTemplateById = async (templateId) => {
    if (!templateId) return;
    setSaving(true);
    setError("");
    try {
      const response = await evaluationApi.applyTemplate(projectId, Number(templateId));
      setConfig(response.data);
      setCriteria((response.data.criteria || []).map(criterionFromTemplate));
      setSelectedTemplateId(response.data.sourceTemplateId || templateId);
      setShowTemplates(false);
      setMessage("Đã áp dụng mẫu tiêu chí vào dự án");
    } catch (err) {
      setError(err.response?.data?.message || "Không thể áp dụng mẫu");
    } finally {
      setSaving(false);
    }
  };

  const validateAndSave = async () => {
    const payload = buildPayload();
    const validationResponse = await evaluationApi.validateCriteria(projectId, payload);
    if (!validationResponse.data.valid) {
      throw new Error(validationResponse.data.errors?.join("; ") || "Bộ tiêu chí chưa hợp lệ");
    }
    const response = await evaluationApi.updateCriteria(projectId, payload);
    setConfig(response.data);
    setCriteria((response.data.criteria || []).map(criterionFromTemplate));
    return response.data;
  };

  const saveCriteria = async () => {
    setSaving(true);
    setError("");
    try {
      await validateAndSave();
      setMessage("Đã lưu thay đổi bộ tiêu chí");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Không thể lưu bộ tiêu chí");
    } finally {
      setSaving(false);
    }
  };

  const saveCriterion = async (index) => {
    setSaving(true);
    setError("");
    try {
      await validateAndSave();
      setExpandedIndex(index);
      setMessage(`Đã lưu tiêu chí số ${index + 1}`);
    } catch (err) {
      setExpandedIndex(index);
      setError(err.response?.data?.message || err.message || "Không thể lưu tiêu chí");
    } finally {
      setSaving(false);
    }
  };

  const restoreCriteria = async () => {
    setSaving(true);
    setError("");
    try {
      const response = await evaluationApi.restoreCriteria(projectId);
      setConfig(response.data);
      setCriteria((response.data.criteria || []).map(criterionFromTemplate));
      setSelectedTemplateId(response.data.sourceTemplateId || "");
      setExpandedIndex(null);
      setMessage("Đã khôi phục bộ tiêu chí từ mẫu đang áp dụng");
    } catch (err) {
      setError(err.response?.data?.message || "Không thể khôi phục tiêu chí");
    } finally {
      setSaving(false);
    }
  };

  const saveTemplate = async () => {
    setSaving(true);
    setError("");
    try {
      await validateAndSave();
      await evaluationApi.saveConfigAsTemplate(projectId, {
        name: `${config?.name || "Mẫu đánh giá"} - bản sao`,
        description: "Mẫu riêng được lưu từ cấu hình dự án",
        level: "CUSTOM"
      });
      setMessage("Đã lưu cấu hình thành mẫu riêng");
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Không thể lưu mẫu riêng");
    } finally {
      setSaving(false);
    }
  };

  const createCycle = async () => {
    setSaving(true);
    setError("");
    try {
      await validateAndSave();
      await evaluationApi.createCycle(projectId, { name: cycleName, startImmediately: true });
      setMessage("Đã tạo snapshot và bắt đầu kỳ đánh giá");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Không thể bắt đầu kỳ đánh giá");
    } finally {
      setSaving(false);
    }
  };

  const addCriterion = () => {
    if (criteria.length >= 8) {
      setError("Mỗi bộ chỉ được có tối đa 8 tiêu chí.");
      return;
    }
    setCriteria((current) => [...current, emptyCriterion(current.length + 1)]);
    setExpandedIndex(criteria.length);
  };

  const removeCriterion = (index) => {
    if (activeCriteria.length <= 3) {
      setError("Mỗi bộ phải có ít nhất 3 tiêu chí.");
      return;
    }
    setCriteria((current) => current.filter((_, itemIndex) => itemIndex !== index));
    setExpandedIndex(null);
  };

  const moveCriterion = (fromIndex, toIndex) => {
    if (fromIndex === null || fromIndex === toIndex) return;
    setCriteria((current) => {
      const next = [...current];
      const [moved] = next.splice(fromIndex, 1);
      next.splice(toIndex, 0, moved);
      return next.map((item, index) => ({ ...item, sortOrder: index + 1 }));
    });
    setExpandedIndex(toIndex);
    setDraggedIndex(null);
  };

  if (loading) {
    return <div className="criteria-settings-state"><RefreshCw className="dashboard-spin" size={19} /> Đang tải cấu hình tiêu chí...</div>;
  }

  return (
    <section className="criteria-settings criteria-settings-redesign">
      <header className="criteria-settings-header">
        <div>
          <h2>Thiết lập tiêu chí đánh giá</h2>
          <p>Tùy chỉnh cách đánh giá thành viên trong dự án <b>{projectName}</b></p>
        </div>
        <div className={`criteria-weight ${totalWeight === 100 ? "valid" : "invalid"}`}>
          <span>Tổng trọng số</span><strong>{totalWeight}%</strong>{totalWeight === 100 && <CheckCircle2 size={15} />}
        </div>
      </header>

      {message && <div className="criteria-message success">{message}</div>}
      {error && <div className="criteria-message danger">{error}</div>}
      {!canManage && <div className="criteria-message info">Bạn có quyền xem. Chỉ OWNER hoặc LEADER được thay đổi bộ tiêu chí.</div>}

      <section className="criteria-current-template">
        <span className="criteria-template-icon"><ShieldCheck size={20} /></span>
        <div>
          <small>Bộ tiêu chí đang dùng</small>
          <strong>{selectedTemplate?.name || config?.name || "Cơ bản"}</strong>
          <p>{selectedTemplate?.criteria?.length || criteria.length} tiêu chí · Đã áp dụng vào dự án</p>
        </div>
        <button type="button" onClick={() => previewTemplate(selectedTemplateId)}><Eye size={16} /> Xem trước</button>
        <button type="button" onClick={() => setShowTemplates((current) => !current)}><RefreshCw size={16} /> Đổi mẫu</button>
      </section>

      {showTemplates && (
        <section className="criteria-template-drawer">
          <header><div><h3>Chọn mẫu tiêu chí</h3><p>Áp dụng một mẫu có sẵn rồi điều chỉnh theo dự án.</p></div><button type="button" disabled={!canManage || saving} onClick={saveTemplate}><Save size={15} /> Lưu thành mẫu riêng</button></header>
          <div>
            {templates.map((template) => (
              <article className={String(template.id) === String(selectedTemplateId) ? "active" : ""} key={template.id}>
                <strong>{template.name}</strong><p>{template.description}</p><small>{template.criteria?.length || 0} tiêu chí</small>
                <footer><button type="button" onClick={() => previewTemplate(template.id)}>Xem</button><button type="button" disabled={!canManage || saving} onClick={() => applyTemplateById(template.id)}>Áp dụng</button></footer>
              </article>
            ))}
          </div>
        </section>
      )}

      <section className="criteria-editor">
        <header>
          <div><h3>Danh sách tiêu chí</h3><p>Kéo thả để thay đổi thứ tự</p></div>
          <button type="button" disabled={!canManage || criteria.length >= 8} onClick={addCriterion}><Plus size={16} /> Thêm tiêu chí</button>
        </header>

        <div className="criteria-list">
          {criteria.map((criterion, index) => {
            const expanded = expandedIndex === index;
            return (
              <article
                className={expanded ? "expanded" : ""}
                key={`${criterion.id || "new"}-${index}`}
                draggable={canManage}
                onDragStart={() => setDraggedIndex(index)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => moveCriterion(draggedIndex, index)}
              >
                <button className="criteria-summary" type="button" onClick={() => setExpandedIndex(expanded ? null : index)}>
                  <GripVertical className="criteria-drag" size={16} />
                  <span className="criteria-order">{index + 1}</span>
                  <strong>{criterion.name || "Chưa đặt tên"}</strong>
                  <mark>{criterion.weight}%</mark>
                  <em>{evaluationTypeLabels[criterion.evaluationType]}</em>
                  <ChevronDown size={17} />
                </button>

                {expanded && (
                  <div className="criteria-form">
                    <label className="criteria-name-field">Tên tiêu chí<input value={criterion.name} disabled={!canManage} onChange={(event) => updateCriterion(index, { name: event.target.value })} /></label>
                    <label>Trọng số<select value={criterion.weight} disabled={!canManage} onChange={(event) => updateCriterion(index, { weight: Number(event.target.value) })}>{[5, 10, 15, 20, 25, 30, 35, 40].map((value) => <option key={value} value={value}>{value}%</option>)}</select></label>
                    <label className="criteria-description-field">Mô tả<textarea rows={2} value={criterion.description || ""} disabled={!canManage} onChange={(event) => updateCriterion(index, { description: event.target.value })} /></label>
                    <label>Cách đánh giá<select value={criterion.evaluationType} disabled={!canManage} onChange={(event) => updateCriterion(index, { evaluationType: event.target.value })}>{evaluationTypes.map((type) => <option key={type} value={type}>{evaluationTypeLabels[type]}</option>)}</select></label>
                    <label>Dữ liệu hệ thống<select value={criterion.metricKey || ""} disabled={!canManage || criterion.evaluationType === "MANUAL"} onChange={(event) => updateCriterion(index, { metricKey: event.target.value })}><option value="">Không áp dụng</option>{metricKeys.map((key) => <option key={key} value={key}>{metricLabels[key]}</option>)}</select></label>
                    <label>Người đánh giá<select value={criterion.manualEvaluator || ""} disabled={!canManage || criterion.evaluationType === "AUTO"} onChange={(event) => updateCriterion(index, { manualEvaluator: event.target.value })}><option value="">Không áp dụng</option>{evaluatorTypes.map((type) => <option key={type} value={type}>{evaluatorLabels[type]}</option>)}</select></label>
                    <label>Thang điểm<select value={criterion.scaleMax} disabled={!canManage} onChange={(event) => updateCriterion(index, { scaleMax: Number(event.target.value) })}><option value="5">5 điểm</option><option value="10">10 điểm</option></select></label>
                    <div className="criteria-toggle-row">
                      <label className="criteria-toggle"><input type="checkbox" checked={criterion.requiresComment} disabled={!canManage} onChange={(event) => updateCriterion(index, { requiresComment: event.target.checked })} /><i /> Bắt buộc nhận xét</label>
                      <label className="criteria-toggle"><input type="checkbox" checked={criterion.requiresEvidence} disabled={!canManage} onChange={(event) => updateCriterion(index, { requiresEvidence: event.target.checked })} /><i /> Bắt buộc bằng chứng</label>
                    </div>
                    <div className="criteria-form-actions">
                      <button className="danger" type="button" disabled={!canManage || activeCriteria.length <= 3} onClick={() => removeCriterion(index)}><Trash2 size={14} /> Xóa tiêu chí</button>
                      <button type="button" onClick={() => setExpandedIndex(null)}>Hủy</button>
                      <button className="primary" type="button" disabled={!canManage || saving || !canSave} onClick={() => saveCriterion(index)}>{saving ? "Đang lưu..." : "Lưu tiêu chí"}</button>
                    </div>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      </section>

      <footer className="criteria-actions">
        <div className={totalWeight === 100 ? "valid" : "invalid"}><CheckCircle2 size={16} /> Tổng trọng số: <strong>{totalWeight}%</strong></div>
        <label className="criteria-cycle-name">Tên kỳ đánh giá<input value={cycleName} disabled={!canManage} onChange={(event) => setCycleName(event.target.value)} /></label>
        <button type="button" disabled={!canManage || saving} onClick={restoreCriteria}><RotateCcw size={15} /> Khôi phục mặc định</button>
        <button type="button" disabled={!canSave || saving} onClick={saveCriteria}><Save size={15} /> Lưu thay đổi</button>
        <button className="primary" type="button" disabled={!canSave || saving || !cycleName.trim()} onClick={createCycle}><CheckCircle2 size={15} /> Bắt đầu đánh giá</button>
      </footer>
    </section>
  );
}

export default EvaluationCriteriaSettings;
