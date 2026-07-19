import assert from "node:assert/strict";
import test from "node:test";
import { formatFileSize, parseTaskLabels } from "./taskDetails.js";

test("formats backend fileSize values", () => {
  assert.equal(formatFileSize(900), "900 B");
  assert.equal(formatFileSize(2048), "2 KB");
  assert.equal(formatFileSize(1.5 * 1024 ** 2), "1.5 MB");
  assert.equal(formatFileSize(undefined), "Không rõ dung lượng");
});

test("parses CSV and array task labels without empty values", () => {
  assert.deepEqual(parseTaskLabels("Frontend, Responsive, ,API"), ["Frontend", "Responsive", "API"]);
  assert.deepEqual(parseTaskLabels([" UI ", "", "Mobile"]), ["UI", "Mobile"]);
  assert.deepEqual(parseTaskLabels(null), []);
});
