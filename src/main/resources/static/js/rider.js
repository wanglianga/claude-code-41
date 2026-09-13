let currentUser = null;
let equipmentTypes = [];
let accidentPhotoIds = [];
let replacePhotoIds = [];

function closeModal(id) { document.getElementById(id).classList.remove('show'); }

function switchTab(name) {
  document.querySelectorAll('.tabs button').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab-pane').forEach(p => p.style.display = 'none');
  document.getElementById('tab-' + name).style.display = '';
  loadTab(name);
}

function loadTab(name) {
  if (name === 'equipment') loadEquipment();
  else if (name === 'replacements') loadReplacements();
  else if (name === 'accidents') loadAccidents();
  else if (name === 'assessment') loadAssessment();
  else if (name === 'training') loadTrainings();
  else if (name === 'reminders') loadReminders();
}

/** 雨天接单提示横幅：未领取雨季雨衣 / 雨衣失效 / 有未读提醒时展示 */
async function loadRainReadiness() {
  const banner = document.getElementById('rainBanner');
  try {
    const r = await api('/api/rider/rain-readiness');
    const warn = !r.hasValidRaincoat || r.pendingPickup || r.unreadReminders > 0;
    if (!warn) { banner.style.display = 'none'; return; }
    banner.style.display = '';
    banner.innerHTML = `
      <div class="alert-item ${r.pendingPickup || !r.hasValidRaincoat ? 'HIGH' : 'MEDIUM'}">
        <div class="t">${esc(r.orderPrompt)}</div>
        ${r.weatherAlert ? `<div>🌧️ 天气预警：${esc(r.weatherAlert)}</div>` : ''}
        ${r.unreadReminders > 0 ? `<div class="s">您有 ${r.unreadReminders} 条未读安全提醒，请查看「安全提醒」页</div>` : ''}
      </div>`;
  } catch (e) { banner.style.display = 'none'; }
}

async function loadReminders() {
  const list = await api('/api/rider/reminders');
  const el = document.getElementById('reminderList');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无安全提醒</p>'; return; }
  el.innerHTML = list.map(r => `
    <div class="alert-item ${r.read ? 'MEDIUM' : 'HIGH'}" style="${r.read ? 'opacity:.65' : ''}">
      <div class="t">${r.read ? '' : '🔴 '}[${L('reminderType', r.type)}] ${esc(r.title)}
        <span style="float:right;font-weight:normal;font-size:12px">${fmtDT(r.createdAt)}</span></div>
      <div>${esc(r.content)}</div>
      ${r.read ? '' : `<div class="s"><button class="btn small" onclick="markReminderRead(${r.id})">知道了，标记已读</button></div>`}
    </div>`).join('');
}

async function markReminderRead(id) {
  await api('/api/rider/reminders/' + id + '/read', { method: 'POST' });
  loadReminders();
  loadRainReadiness();
}

async function loadEquipment() {
  const list = await api('/api/rider/equipment');
  const el = document.getElementById('equipmentTable');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无领用记录</p>'; return; }
  el.innerHTML = `<table><thead><tr>
    <th>装备</th><th>尺码</th><th>发放时间</th><th>押金(元)</th><th>应更换日期</th><th>状态</th><th>操作</th>
  </tr></thead><tbody>${list.map(i => `<tr>
    <td>${esc(i.typeName)}</td><td>${esc(i.size)}</td><td>${fmtDT(i.issuedAt)}</td>
    <td>${i.depositPaid}</td>
    <td>${fmtD(i.expectedReplaceAt)} ${i.expired ? '<span class="badge red">已超期</span>' : ''}</td>
    <td>${badge('issueStatus', i.status)}</td>
    <td>${i.status === 'IN_USE' ? `<button class="btn small" onclick="openReplace(${i.id},'${esc(i.typeName)}','${esc(i.size)}')">申请更换</button>` : ''}</td>
  </tr>`).join('')}</tbody></table>`;
}

function openReplace(issueId, typeName, size) {
  document.getElementById('rIssueId').value = issueId;
  document.getElementById('replaceInfo').textContent = `装备：${typeName}（${size}）`;
  replacePhotoIds = [];
  document.getElementById('rPhotos').value = '';
  document.getElementById('rPhotoPreview').innerHTML = '';
  document.getElementById('replaceModal').classList.add('show');
}

async function loadReplacements() {
  const list = await api('/api/rider/replacements');
  const el = document.getElementById('replacementTable');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无更换申请</p>'; return; }
  el.innerHTML = list.map(r => `
    <div style="border-bottom:1px solid #f0f0f0;padding:10px 0">
      <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px">
        <div><b>${esc(r.typeName)}</b>（${esc(r.size)}） · 申请于 ${fmtDT(r.createdAt)} · 天气 ${L('weather', r.weather)}</div>
        <div>${badge('replacementStatus', r.status)}</div>
      </div>
      <div style="font-size:13px;color:#666;margin-top:4px">原因：${esc(r.reason)}</div>
      ${photoThumbs(r.photos)}
      ${r.evaluation ? `<div class="eval">系统评估：${esc(r.evaluation)}</div>` : ''}
      ${r.depositDeducted > 0 ? `<div style="font-size:12px;color:#d4380d;margin-top:4px">押金扣减：${r.depositDeducted} 元</div>` : ''}
      ${r.processNote ? `<div style="font-size:12px;color:#888;margin-top:4px">处理备注：${esc(r.processNote)}（${esc(r.processedBy || '')} ${fmtDT(r.processedAt)}）</div>` : ''}
    </div>`).join('');
}

async function loadAccidents() {
  const list = await api('/api/rider/accidents');
  const el = document.getElementById('accidentTable');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无事故记录</p>'; return; }
  el.innerHTML = `<table><thead><tr>
    <th>时间</th><th>类型</th><th>地点</th><th>线路</th><th>天气</th><th>照片</th><th>状态</th><th>操作</th>
  </tr></thead><tbody>${list.map(a => `<tr>
    <td>${fmtDT(a.occurredAt)}</td><td>${L('accidentType', a.type)}</td>
    <td>${esc(a.location)}</td><td>${esc(a.routeArea || '-')}</td>
    <td>${L('weather', a.weather)}</td>
    <td>${a.photos && a.photos.length ? photoThumbs(a.photos) : '<span style="color:#bbb">无</span>'}</td>
    <td>${badge('accidentStatus', a.status)}</td>
    <td><button class="btn small ghost" onclick="showAccident(${a.id})">详情</button></td>
  </tr>`).join('')}</tbody></table>`;
}

async function showAccident(id) {
  const a = await api('/api/rider/accidents/' + id);
  const el = document.getElementById('accidentDetail');
  let html = `<table>
    <tr><th style="width:110px">类型</th><td>${L('accidentType', a.type)}</td></tr>
    <tr><th>时间</th><td>${fmtDT(a.occurredAt)}</td></tr>
    <tr><th>地点</th><td>${esc(a.location)}（${esc(a.routeArea || '-')}）</td></tr>
    <tr><th>订单号</th><td>${esc(a.orderNo || '-')}</td></tr>
    <tr><th>装备状态</th><td>${esc(a.equipmentStatusDesc || '-')}</td></tr>
    <tr><th>损坏装备</th><td>${esc(a.damagedEquipmentTypeName || '无')}</td></tr>
    <tr><th>伤情</th><td>${esc(a.injuryDesc || '-')}</td></tr>
    <tr><th>交警记录</th><td>${esc(a.policeRecordNo || '-')}</td></tr>
    <tr><th>状态</th><td>${badge('accidentStatus', a.status)}</td></tr>
  </table>`;
  if (a.photos && a.photos.length) {
    html += `<div class="section-title">现场照片（${a.photos.length} 张）</div>` + photoThumbs(a.photos);
  }
  if (a.review) {
    html += `<div class="section-title">站长核查结论</div><table>
      <tr><th style="width:110px">配送中</th><td>${boolText(a.review.wasDelivering)}</td></tr>
      <tr><th>佩戴装备</th><td>${boolText(a.review.wearingEquipment)}</td></tr>
      <tr><th>违规骑行</th><td>${boolText(a.review.hasViolation)} ${esc(a.review.violationDesc || '')}</td></tr>
      <tr><th>需要保险</th><td>${boolText(a.review.insuranceNeeded)}</td></tr>
      <tr><th>材料齐全</th><td>${boolText(a.review.materialsComplete)}</td></tr>
      <tr><th>备注</th><td>${esc(a.review.reviewNotes || '-')}</td></tr>
    </table>`;
  }
  if (a.claims && a.claims.length) {
    html += `<div class="section-title">保险理赔</div><table><thead><tr><th>理赔单号</th><th>金额</th><th>状态</th><th>材料说明</th></tr></thead>
      <tbody>${a.claims.map(c => `<tr><td>${esc(c.claimNo)}</td><td>${c.amount}</td><td>${badge('claimStatus', c.status)}</td><td>${esc(c.materialsNotes || '-')}</td></tr>`).join('')}</tbody></table>`;
  }
  if (a.reissues && a.reissues.length) {
    html += `<div class="section-title">装备补发</div><table><thead><tr><th>装备</th><th>尺码</th><th>状态</th></tr></thead>
      <tbody>${a.reissues.map(r => `<tr><td>${esc(r.typeName)}</td><td>${esc(r.size)}</td><td>${badge('reissueStatus', r.status)}</td></tr>`).join('')}</tbody></table>`;
  }
  if (a.assessments && a.assessments.length) {
    html += `<div class="section-title">关联考核</div><table><thead><tr><th>加减分</th><th>原因</th></tr></thead>
      <tbody>${a.assessments.map(x => `<tr><td>${x.pointsChange}</td><td>${esc(x.reason)}</td></tr>`).join('')}</tbody></table>`;
  }
  el.innerHTML = html;
  document.getElementById('accidentModal').classList.add('show');
}

async function loadAssessment() {
  const data = await api('/api/rider/assessments');
  const el = document.getElementById('assessmentView');
  const color = data.score >= 90 ? '#1a9e54' : data.score >= 70 ? '#d9822b' : '#d4380d';
  el.innerHTML = `<div style="font-size:34px;font-weight:700;color:${color};margin-bottom:14px">${data.score} 分</div>` +
    (data.records.length ? `<table><thead><tr><th>时间</th><th>加减分</th><th>原因</th></tr></thead>
      <tbody>${data.records.map(r => `<tr><td>${fmtDT(r.createdAt)}</td>
        <td style="color:${r.pointsChange >= 0 ? '#1a9e54' : '#d4380d'};font-weight:600">${r.pointsChange > 0 ? '+' : ''}${r.pointsChange}</td>
        <td>${esc(r.reason)}</td></tr>`).join('')}</tbody></table>` : '<p style="color:#999">暂无考核记录</p>');
}

async function loadTrainings() {
  const list = await api('/api/rider/trainings');
  const el = document.getElementById('trainingTable');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无培训记录</p>'; return; }
  el.innerHTML = `<table><thead><tr><th>培训主题</th><th>类别</th><th>完成时间</th><th>成绩</th></tr></thead>
    <tbody>${list.map(t => `<tr><td>${esc(t.title)}</td><td>${L('trainingCategory', t.category)}</td>
      <td>${fmtDT(t.completedAt)}</td><td>${t.score ?? '-'}</td></tr>`).join('')}</tbody></table>`;
}

document.addEventListener('DOMContentLoaded', async () => {
  currentUser = await api('/api/auth/me');
  document.getElementById('userInfo').textContent = `${currentUser.name}（${currentUser.stationName || '未分配站点'}）`;
  equipmentTypes = await api('/api/equipment/types');
  document.getElementById('aDamagedType').innerHTML = '<option value="">无</option>' +
    equipmentTypes.map(t => `<option value="${t.id}">${esc(t.name)}</option>`).join('');
  document.querySelectorAll('.tabs button').forEach(b => b.addEventListener('click', () => switchTab(b.dataset.tab)));

  // 事故照片：选择后立即上传并回显
  document.getElementById('aPhotos').addEventListener('change', async (e) => {
    const msg = document.getElementById('msg');
    try {
      accidentPhotoIds = await uploadPhotos(e.target, document.getElementById('aPhotoPreview'));
      if (accidentPhotoIds.length) showMsg(msg, `已上传 ${accidentPhotoIds.length} 张照片`, true);
    } catch (err) {
      accidentPhotoIds = [];
      e.target.value = '';
      document.getElementById('aPhotoPreview').innerHTML = '';
      showMsg(msg, err.message, false);
    }
  });

  // 磨损照片：选择后立即上传并回显
  document.getElementById('rPhotos').addEventListener('change', async (e) => {
    const msg = document.getElementById('msg');
    try {
      replacePhotoIds = await uploadPhotos(e.target, document.getElementById('rPhotoPreview'));
      if (replacePhotoIds.length) showMsg(msg, `已上传 ${replacePhotoIds.length} 张照片`, true);
    } catch (err) {
      replacePhotoIds = [];
      e.target.value = '';
      document.getElementById('rPhotoPreview').innerHTML = '';
      showMsg(msg, err.message, false);
    }
  });

  document.getElementById('replaceForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const msg = document.getElementById('msg');
    try {
      await api('/api/rider/replacements', {
        method: 'POST',
        body: {
          issueId: Number(document.getElementById('rIssueId').value),
          reason: document.getElementById('rReason').value,
          weather: document.getElementById('rWeather').value,
          photoIds: replacePhotoIds,
        },
      });
      closeModal('replaceModal');
      showMsg(msg, '更换申请已提交，系统已完成自动评估，请查看「更换申请」页', true);
      document.getElementById('rReason').value = '';
      replacePhotoIds = [];
      loadReplacements();
    } catch (err) { showMsg(msg, err.message, false); }
  });

  document.getElementById('accidentForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const msg = document.getElementById('msg');
    try {
      await api('/api/rider/accidents', {
        method: 'POST',
        body: {
          type: document.getElementById('aType').value,
          occurredAt: document.getElementById('aOccurredAt').value,
          location: document.getElementById('aLocation').value,
          routeArea: document.getElementById('aRouteArea').value || null,
          orderNo: document.getElementById('aOrderNo').value || null,
          equipmentStatusDesc: document.getElementById('aEquipDesc').value || null,
          damagedEquipmentTypeId: document.getElementById('aDamagedType').value || null,
          injuryDesc: document.getElementById('aInjury').value || null,
          policeRecordNo: document.getElementById('aPoliceNo').value || null,
          photoIds: accidentPhotoIds,
          weather: document.getElementById('aWeather').value,
        },
      });
      showMsg(msg, '事故申报已提交，等待站长核查', true);
      e.target.reset();
      accidentPhotoIds = [];
      document.getElementById('aPhotoPreview').innerHTML = '';
      loadAccidents();
    } catch (err) { showMsg(msg, err.message, false); }
  });

  loadEquipment();
  loadRainReadiness();
});
