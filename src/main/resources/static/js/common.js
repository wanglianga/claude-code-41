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
  rainPlanStatus: { DRAFT: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消' },
  rainPlanItemStatus: { PENDING: '待领取', ISSUED: '已领取', SKIPPED: '无需更换' },
  reminderType: { RAINCOAT_PICKUP: '雨衣领取提醒', RAINY_ORDER_WARNING: '雨天接单提醒', GENERAL: '通用提醒' },
  protectionStatus: { VALID: '有效', EXPIRED: '已过期', NONE: '未配备' },
};

const StatusColor = {
  PENDING: 'orange', UNDER_REVIEW: 'blue', APPROVED: 'green', REJECTED: 'red', CLOSED: 'gray',
  APPROVED_FREE: 'green', APPROVED_DEPOSIT: 'orange', REPAIR: 'blue',
  IN_USE: 'green', RETURNED: 'gray', REPLACED: 'blue', SCRAPPED: 'gray',
  DRAFT: 'orange', SUBMITTED: 'blue', PAID: 'green', ISSUED: 'green', CANCELLED: 'gray',
  CONFIRMED: 'blue', COMPLETED: 'green', SKIPPED: 'gray',
  VALID: 'green', EXPIRED: 'red', NONE: 'gray',
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

/** 渲染附件照片缩略图（点击新窗口查看原图） */
function photoThumbs(photos) {
  if (!photos || !photos.length) return '';
  return '<div class="photo-preview">' + photos.map(p =>
    `<a href="${p.url}" target="_blank" title="${esc(p.name)}"><img class="photo-thumb" src="${p.url}" alt="${esc(p.name)}"></a>`
  ).join('') + '</div>';
}

/**
 * 上传文件选择框中的全部图片，成功后把缩略图回显到 previewEl。
 * 返回附件 ID 数组；任一文件被校验拒绝时抛错（调用方负责提示）。
 */
async function uploadPhotos(inputEl, previewEl) {
  const ids = [];
  if (previewEl) previewEl.innerHTML = '';
  for (const f of inputEl.files) {
    const fd = new FormData();
    fd.append('file', f);
    const res = await fetch('/api/attachments', { method: 'POST', body: fd, credentials: 'same-origin' });
    const data = await res.json().catch(() => ({}));
    if (res.status === 401) { location.href = '/login.html'; throw new Error('未登录'); }
    if (!res.ok) throw new Error(`${f.name}: ${data.error || ('上传失败 ' + res.status)}`);
    ids.push(data.id);
    if (previewEl) {
      const a = document.createElement('a');
      a.href = data.url; a.target = '_blank';
      const img = document.createElement('img');
      img.src = data.url; img.className = 'photo-thumb'; img.title = f.name;
      a.appendChild(img);
      previewEl.appendChild(a);
    }
  }
  return ids;
}
