export const normalizeTaskLabels = (value) => String(value || "")
  .split(",")
  .map((label) => label.trim())
  .filter(Boolean)
  .join(",");

const nullableNumber = (value) => {
  if (value === "" || value == null) return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

export const buildTaskPayload = (form, taskTypes = [], fixedStatus) => {
  const fallbackCategory = taskTypes.find((category) => category.active !== false);
  const selectedCategoryId = form.workCategoryId || fallbackCategory?.id || fallbackCategory?.value;
  const selectedCategory = taskTypes.find(
    (item) => String(item.id ?? item.value) === String(selectedCategoryId ?? "")
  );
  const estimatedEffort = nullableNumber(form.estimatedEffort);

  return {
    title: form.title,
    description: form.description,
    assignedToUserId: nullableNumber(form.assignedToUserId),
    boardColumnId: nullableNumber(form.boardColumnId),
    parentTaskId: nullableNumber(form.parentTaskId),
    status: fixedStatus || form.status,
    priority: form.priority,
    type: selectedCategory?.label || form.type || "Thiết kế",
    workCategoryId: nullableNumber(selectedCategoryId),
    reviewRequired: Boolean(form.reviewRequired),
    startDate: form.startDate || null,
    dueDate: form.dueDate || null,
    estimatedEffort: estimatedEffort ?? 0,
    actualEffort: nullableNumber(form.actualEffort),
    labels: normalizeTaskLabels(form.labels),
    version: form.version ?? null
  };
};
