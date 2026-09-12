const Labels = {
  weather: { SUNNY: '晴天', RAINY: '雨天', SNOWY: '雪天', WINDY: '大风', FOGGY: '雾天', EXTREME_HEAT: '高温' },
  accidentType: { TRAFFIC_ACCIDENT: '交通事故', FALL: '摔倒', EQUIPMENT_DAMAGE: '装备损坏', FOOD_CONTAMINATION: '餐品污染' },
  accidentStatus: { PENDING: '待核查', UNDER_REVIEW: '核查中', APPROVED: '核查通过', REJECTED: '已驳回', CLOSED: '已结案' },
  replacementStatus: { PENDING: '待处理', APPROVED_FREE: '免费更换', APPROVED_DEPOSIT: '扣押金更换', REPAIR: '建议维修', REJECTED: '已驳回' },
  issueStatus: { IN_USE: '使用中', RETURNED: '已归还', REPLACED: '已更换', SCRAPPED: '已报废' },
  claimStatus: { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已批准', PAID: '已赔付', REJECTED: '已拒赔' },
  reissueStatus: { PENDING: '待补发', ISSUED: '已补发', CANCELLED: '已取消' },
  policyType: { PROCUREMENT: '采购规则', TRAINING: '培训规则', REVIEW_RULE: '审核规则' },
  trainingCategory: { SAFETY: '安全培训', EQUIPMENT_USE: '装备使用', FOOD_SAFETY: '食品安全' },
};

const StatusColor = {
  PENDING: 'orange', UNDER_REVIEW: 'blue', APPROVED: 'green', REJECTED: 'red', CLOSED: 'gray',
  APPROVED_FREE: 'green', APPROVED_DEPOSIT: 'orange', REPAIR: 'blue',
  IN_USE: 'green', RETURNED: 'gray', REPLACED: 'blue', SCRAPPED: 'gray',
  DRAFT: 'orange', SUBMITTED: 'blue', PAID: 'green', ISSUED: 'green', CANCELLED: 'gray',
};

function L(map, key) { return (Labels[map] && Labels[map][key]) || key || '-'; }
function badge(map, key) { return `<span class="badge ${StatusColor[key] || 'gray'}">${L(map, key)}</span>`; }
function fmtDT(s) { return s ? String(s).replace('T', ' ').slice(0, 16) : '-'; }
function fmtD(s) { return s ? String(s).slice(0, 10) : '-'; }
function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function boolText(b) { return b === true ? '是' : b === false ? '否' : '-'; }

async function api(path, opts = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    method: opts.method || 'GET',
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  if (res.status === 401) { location.href = '/login.html'; throw new Error('未登录'); }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || ('请求失败 ' + res.status));
  return data;
}

function showMsg(el, text, ok) {
  el.className = 'msg ' + (ok ? 'ok' : 'err');
  el.textContent = text;
  setTimeout(() => { el.className = 'msg'; }, 4000);
}

async function logout() {
  await api('/api/auth/logout', { method: 'POST' }).catch(() => {});
  location.href = '/login.html';
}

function barChart(rows, nameKey, valKey) {
  if (!rows.length) return '<p style="color:#999;font-size:13px">暂无数据</p>';
  const max = Math.max(...rows.map(r => r[valKey]), 1);
  return rows.map(r => `
    <div class="bar-row">
      <div class="name">${esc(r[nameKey])}</div>
      <div class="bar" style="width:${Math.round(r[valKey] / max * 100)}%"></div>
      <div class="val">${r[valKey]}</div>
    </div>`).join('');
}
