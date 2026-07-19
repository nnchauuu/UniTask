export function acceptRealtimeEvent(currentIds, eventId, limit = 200) {
  if (!eventId || currentIds.has(eventId)) return { accepted: false, ids: currentIds };
  const ids = currentIds.size >= limit ? new Set() : new Set(currentIds);
  ids.add(eventId);
  return { accepted: true, ids };
}

export function normalizeTasksById(tasks) {
  return [...new Map(tasks.map((task) => [task.id, task])).values()];
}
