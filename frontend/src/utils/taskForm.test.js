import assert from "node:assert/strict";
import test from "node:test";
import { buildTaskPayload, normalizeTaskLabels } from "./taskForm.js";

test("task edit payload preserves detail fields and optimistic version", () => {
  const payload = buildTaskPayload({
    title: "Đổi tiêu đề",
    description: "Mô tả",
    assignedToUserId: "7",
    boardColumnId: "12",
    parentTaskId: "3",
    status: "IN_PROGRESS",
    priority: "HIGH",
    type: "BACKEND",
    workCategoryId: "9",
    reviewRequired: true,
    startDate: "2026-07-15",
    dueDate: "2026-07-20",
    estimatedEffort: "5.5",
    actualEffort: "2.25",
    labels: " API, Backend, , Khẩn cấp ",
    version: 8
  }, [{ id: 9, label: "Backend", active: true }]);

  assert.deepEqual(payload, {
    title: "Đổi tiêu đề",
    description: "Mô tả",
    assignedToUserId: 7,
    boardColumnId: 12,
    parentTaskId: 3,
    status: "IN_PROGRESS",
    priority: "HIGH",
    type: "Backend",
    workCategoryId: 9,
    reviewRequired: true,
    startDate: "2026-07-15",
    dueDate: "2026-07-20",
    estimatedEffort: 5.5,
    actualEffort: 2.25,
    labels: "API,Backend,Khẩn cấp",
    version: 8
  });
});

test("task payload normalizes optional values without inventing data", () => {
  const payload = buildTaskPayload({
    title: "Task",
    description: "",
    assignedToUserId: "",
    boardColumnId: null,
    parentTaskId: "",
    status: "TODO",
    priority: "MEDIUM",
    type: "DESIGN",
    workCategoryId: "",
    reviewRequired: false,
    startDate: "",
    dueDate: "",
    estimatedEffort: "",
    actualEffort: "",
    labels: "",
    version: null
  });

  assert.equal(payload.assignedToUserId, null);
  assert.equal(payload.boardColumnId, null);
  assert.equal(payload.parentTaskId, null);
  assert.equal(payload.startDate, null);
  assert.equal(payload.dueDate, null);
  assert.equal(payload.estimatedEffort, 0);
  assert.equal(payload.actualEffort, null);
  assert.equal(payload.labels, "");
  assert.equal(payload.version, null);
});

test("normalizes labels as a compact CSV value", () => {
  assert.equal(normalizeTaskLabels(" UI, Backend, ,Bug "), "UI,Backend,Bug");
});
