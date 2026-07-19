import test from "node:test";
import assert from "node:assert/strict";

import { acceptRealtimeEvent, normalizeTasksById } from "./realtimeEvents.js";

test("two clients process an event once and keep one task per id", () => {
  let firstClient = new Set();
  let secondClient = new Set();

  const first = acceptRealtimeEvent(firstClient, "event-1");
  firstClient = first.ids;
  const second = acceptRealtimeEvent(secondClient, "event-1");
  secondClient = second.ids;

  assert.equal(first.accepted, true);
  assert.equal(second.accepted, true);
  assert.equal(acceptRealtimeEvent(firstClient, "event-1").accepted, false);
  assert.equal(acceptRealtimeEvent(secondClient, "event-1").accepted, false);
  assert.deepEqual(normalizeTasksById([{ id: 7, title: "Old" }, { id: 7, title: "New" }]),
    [{ id: 7, title: "New" }]);
});
