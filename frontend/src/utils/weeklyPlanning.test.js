import assert from "node:assert/strict";
import test from "node:test";
import { countPlanningFilters, filterPlanHistory, filterPlanningTasks, formatCapacitySummary, paginatePlanningTasks, requiresOverloadConfirmation, validateWeeklyPlanForm } from "./weeklyPlanning.js";

test("validates required fields, date range and capacity", () => {
  const errors = validateWeeklyPlanForm({ name: " ", startDate: "2026-07-15", endDate: "2026-07-15", capacity: 0 });
  assert.deepEqual(Object.keys(errors).sort(), ["capacity", "endDate", "name"]);
  assert.deepEqual(validateWeeklyPlanForm({ name: "Tuần 1", startDate: "2026-07-15", endDate: "2026-07-21", capacity: 40 }), {});
});

test("filters backlog by search, assignee, priority and overdue state", () => {
  const tasks = [
    { id: 1, title: "Thiết kế", priority: "HIGH", assignedTo: { id: 9 }, workCategoryName: "UI", dueDate: "2026-07-10" },
    { id: 2, title: "API login", priority: "MEDIUM", assignedTo: null, workCategoryName: "Backend", dueDate: null }
  ];
  const base = { search: "cv-1", assignee: "9", category: "ALL", priority: "HIGH", deadline: "ALL", unassigned: false, overdue: true };
  assert.deepEqual(filterPlanningTasks(tasks, base, new Date("2026-07-15T00:00:00")).map((task) => task.id), [1]);
  assert.deepEqual(filterPlanningTasks(tasks, { ...base, search: "", assignee: "ALL", priority: "ALL", overdue: false, unassigned: true }).map((task) => task.id), [2]);
});

test("requires confirmation for plan or member overload", () => {
  assert.equal(requiresOverloadConfirmation({ overloaded: false, memberWorkloads: [{ overloaded: true }] }), true);
  assert.equal(requiresOverloadConfirmation({ overloaded: false, memberWorkloads: [] }), false);
});

test("does not render a misleading zero capacity", () => {
  assert.equal(formatCapacitySummary({ capacity: 0, allocatedEffort: 0 }), "Chưa ước tính");
  assert.equal(formatCapacitySummary({ capacity: 40, allocatedEffort: 12 }, "giờ"), "12/40 giờ");
});

test("filters plan history by name, status and completion rate", () => {
  const plans = [
    { name: "Tuần 1", status: "COMPLETED", totalTasks: 4, completedTasks: 4, startDate: "2026-07-01", endDate: "2026-07-07" },
    { name: "Tuần 2", status: "CANCELLED", totalTasks: 4, completedTasks: 2, startDate: "2026-07-08", endDate: "2026-07-14" }
  ];
  assert.deepEqual(filterPlanHistory(plans, { search: "tuần", status: "COMPLETED", completion: "FULL" }).map((plan) => plan.name), ["Tuần 1"]);
  assert.deepEqual(filterPlanHistory(plans, { search: "2", status: "ALL", completion: "MEDIUM" }).map((plan) => plan.name), ["Tuần 2"]);
  assert.deepEqual(filterPlanHistory(plans, { search: "", status: "ALL", completion: "ALL", dateFrom: "2026-07-10", dateTo: "2026-07-20" }).map((plan) => plan.name), ["Tuần 2"]);
});

test("paginates backlog and counts active filters", () => {
  const result = paginatePlanningTasks([1, 2, 3, 4, 5, 6], 2, 5);
  assert.deepEqual(result, { totalPages: 2, page: 2, items: [6] });
  assert.equal(countPlanningFilters({ assignee: "9", category: "ALL", priority: "HIGH", deadline: "NONE", unassigned: false, overdue: true }), 4);
});
