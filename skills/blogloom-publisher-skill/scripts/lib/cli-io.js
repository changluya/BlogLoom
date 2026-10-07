'use strict';

/** CLI 输出：日志走 stderr，结果 JSON 走 stdout（便于 Agent 解析）。 */

function log(msg) {
  process.stderr.write(`[publisher] ${msg}\n`);
}

function emit(payload) {
  process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
}

module.exports = { log, emit };
