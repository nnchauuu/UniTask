export function validateWeeklyPlanForm(form) {
  const errors = {};
  if (!form.name?.trim()) errors.name = "Tên kế hoạch không được để trống";
  if (!form.startDate) errors.startDate = "Hãy chọn ngày bắt đầu";
  if (!form.endDate) errors.endDate = "Hãy chọn ngày kết thúc";
  if (form.startDate && form.endDate && form.endDate <= form.startDate) errors.endDate = "Ngày kết thúc phải sau ngày bắt đầu";
  if (!Number(form.capacity) || Number(form.capacity) <= 0) errors.capacity = "Sức chứa phải lớn hơn 0";
  return errors;
}

export function filterPlanningTasks(tasks, filters, now = new Date()) {
  return tasks.filter((task) => {
    const text = `${task.title} CV-${task.id}`.toLowerCase();
    const due = task.dueDate ? new Date(`${task.dueDate}T23:59:59`) : null;
    return (!filters.search || text.includes(filters.search.toLowerCase()))
      && (filters.assignee === "ALL" || String(task.assignedTo?.id) === filters.assignee)
      && (filters.category === "ALL" || task.workCategoryName === filters.category)
      && (filters.priority === "ALL" || task.priority === filters.priority)
      && (filters.deadline === "ALL" || (filters.deadline === "HAS" ? due : !due))
      && (!filters.unassigned || !task.assignedTo)
      && (!filters.overdue || (due && due < now));
  });
}

export function requiresOverloadConfirmation(plan) {
  return Boolean(plan?.overloaded || plan?.memberWorkloads?.some((item) => item.overloaded));
}

export function formatCapacitySummary(plan, unitLabel = "giờ") {
  if (!Number(plan?.capacity)) return "Chưa ước tính";
  return `${Number(plan.allocatedEffort || 0)}/${Number(plan.capacity)} ${unitLabel}`;
}

export function filterPlanHistory(plans, filters) {
  return plans.filter((plan) => {
    const rate = plan.totalTasks ? plan.completedTasks / plan.totalTasks * 100 : 0;
    const startsBeforeRangeEnds = !filters.dateTo || !plan.startDate || plan.startDate <= filters.dateTo;
    const endsAfterRangeStarts = !filters.dateFrom || !plan.endDate || plan.endDate >= filters.dateFrom;
    return (!filters.search || plan.name.toLowerCase().includes(filters.search.toLowerCase()))
      && (filters.status === "ALL" || plan.status === filters.status)
      && (filters.completion === "ALL"
        || (filters.completion === "FULL" ? rate === 100
          : filters.completion === "HIGH" ? rate >= 75
            : filters.completion === "MEDIUM" ? rate >= 50 && rate < 75
              : rate < 50))
      && startsBeforeRangeEnds && endsAfterRangeStarts;
  });
}

export function paginatePlanningTasks(tasks, page, pageSize) {
  const totalPages = Math.max(1, Math.ceil(tasks.length / pageSize));
  const safePage = Math.min(Math.max(1, page), totalPages);
  return { totalPages, page: safePage, items: tasks.slice((safePage - 1) * pageSize, safePage * pageSize) };
}

export function countPlanningFilters(filters) {
  return [filters.assignee !== "ALL", filters.category !== "ALL", filters.priority !== "ALL", filters.deadline !== "ALL", filters.unassigned, filters.overdue].filter(Boolean).length;
}
