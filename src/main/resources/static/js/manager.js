let currentUser = null;
let stationId = null;   // 当前操作的站点（管理员可切换）
let equipmentTypes = [];
let riders = [];

function qs() { return stationId ? `?stationId=${stationId}` : ''; }
function closeModal(id) { document.getElementById(id).classList.remove('show'); }

function switchTab(name) {
  document.querySelectorAll('.tabs button').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab-pane').forEach(p => p.style.display = 'none');
  document.getElementById('tab-' + name).style.display = '';
  loadTab(name);
}

function loadTab(name) {
  const loaders = {
    overview: loadOverview, stock: loadStockTab, issues: loadIssues,
    replacements: loadReplacements, accidents: loadAccidents, claims: loadClaims,
    reissues: loadReissues, assessments: loadAssessments, trainings: loadTrainings,
    analytics: loadAnalytics, policies: loadPolicies,
  };
  if (loaders[name]) loaders[name]();
}

async function changeStation() {
  stationId = Number(document.getElementById('stationSelect').value);
  await loadRiders();
  const active = document.querySelector('.tabs button.active').dataset.tab;
  loadTab(active);
}

// ---------- 总览 ----------
async function loadOverview() {
  const d = await api('/api/analytics/dashboard' + qs());
  const stats = [
    ['骑手人数', d.riderCount], ['在用装备', d.equipmentInUse],
    ['已超期装备', d.equipmentExpired, true], ['待处理更换', d.pendingReplacements, d.pendingReplacements > 0],
    ['待核查事故', d.pendingAccidents, d.pendingAccidents > 0], ['本月事故', d.accidentsThisMonth],
    ['进行中理赔', d.openClaims],
  ];
  document.getElementById('statGrid').innerHTML = stats.map(([label, num, warn]) =>
    `<div class="stat"><div class="num ${warn ? 'warn' : ''}">${num}</div><div class="label">${label}</div></div>`).join('');
  const alerts = await api('/api/analytics/alerts' + qs());
  document.getElementById('alertList').innerHTML = alerts.length
    ? alerts.map(a => `<div class="alert-item ${a.level}">
        <div class="t">【${a.level === 'HIGH' ? '高' : '中'}】${esc(a.title)}</div>
        <div>${esc(a.detail)}</div><div class="s">💡 ${esc(a.suggestion)}</div>
      </div>`).join('')
    : '<p style="color:#1a9e54;font-size:13px">✓ 当前无风险告警</p>';
}

// ---------- 库存与发放 ----------
async function loadStockTab() {
  await loadRiders();
  const stock = await api('/api/manager/stock' + qs());
  document.getElementById('stockTable').innerHTML = `<table><thead><tr>
    <th>装备</th><th>尺码</th><th>库存数量</th><th>操作</th></tr></thead>
    <tbody>${stock.map(s => `<tr>
      <td>${esc(s.typeName)}</td><td>${esc(s.size)}</td>
      <td><b style="color:${s.quantity <= 3 ? '#d4380d' : '#0d3b66'}">${s.quantity}</b></td>
      <td>
        <button class="btn small ghost" onclick="adjustStock(${s.id}, 1)">+1</button>
        <button class="btn small ghost" onclick="adjustStock(${s.id}, 10)">+10</button>
        <button class="btn small ghost" onclick="adjustStock(${s.id}, -1)">-1</button>
      </td></tr>`).join('')}</tbody></table>`;
}

async function adjustStock(id, delta) {
  const msg = document.getElementById('msg');
  try {
    await api(`/api/manager/stock/${id}/adjust`, { method: 'POST', body: { delta } });
    showMsg(msg, '库存已更新', true);
    loadStockTab();
  } catch (err) { showMsg(msg, err.message, false); }
}

async function loadRiders() {
  riders = await api('/api/manager/riders' + qs());
  const opts = riders.map(r => `<option value="${r.id}">${esc(r.name)}（${esc(r.username)}）</option>`).join('');
  for (const id of ['iRider', 'asRider', 'tRider']) {
    const el = document.getElementById(id);
    if (el) el.innerHTML = opts;
  }
}

function fillTypeSizes() {
  const sel = document.getElementById('iType');
  const t = equipmentTypes.find(x => x.id === Number(sel.value));
  document.getElementById('iSize').innerHTML = t ? t.sizes.map(s => `<option>${esc(s)}</option>`).join('') : '';
}

// ---------- 领用记录 ----------
async function loadIssues() {
  const expiredOnly = document.getElementById('expiredOnly').checked;
  const list = await api('/api/manager/issues' + qs() + (expiredOnly ? '&expiredOnly=true' : ''));
  document.getElementById('issueTable').innerHTML = list.length ? `<table><thead><tr>
    <th>骑手</th><th>装备</th><th>尺码</th><th>发放时间</th><th>押金</th><th>应更换日期</th><th>状态</th><th>备注</th>
    </tr></thead><tbody>${list.map(i => `<tr>
      <td>${esc(i.riderName)}</td><td>${esc(i.typeName)}</td><td>${esc(i.size)}</td>
      <td>${fmtDT(i.issuedAt)}</td><td>${i.depositPaid}</td>
      <td>${fmtD(i.expectedReplaceAt)} ${i.expired ? '<span class="badge red">已超期</span>' : ''}</td>
      <td>${badge('issueStatus', i.status)}</td><td>${esc(i.notes || '-')}</td>
    </tr>`).join('')}</tbody></table>` : '<p style="color:#999">暂无记录</p>';
}

// ---------- 更换审核 ----------
async function loadReplacements() {
  const list = await api('/api/manager/replacements' + qs());
  const el = document.getElementById('replacementList');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无更换申请</p>'; return; }
  el.innerHTML = list.map(r => `
    <div style="border-bottom:1px solid #f0f0f0;padding:12px 0">
      <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px">
        <div><b>${esc(r.riderName)}</b> 申请更换 <b>${esc(r.typeName)}</b>（${esc(r.size)}）
          · 发放于 ${fmtDT(r.issuedAt)} · 应更换 ${fmtD(r.expectedReplaceAt)} · 天气 ${L('weather', r.weather)}</div>
        <div>${badge('replacementStatus', r.status)}</div>
      </div>
      <div style="font-size:13px;color:#666;margin-top:4px">原因：${esc(r.reason)}${r.wearPhotos ? ` · 历史照片链接：${esc(r.wearPhotos)}` : ''}</div>
      ${photoThumbs(r.photos)}
      ${r.evaluation ? `<div class="eval">系统评估：${esc(r.evaluation)}</div>` : ''}
      ${r.status === 'PENDING' ? `
      <div style="margin-top:8px;display:flex;gap:8px;flex-wrap:wrap;align-items:center">
        <button class="btn small" onclick="processReplacement(${r.id},'APPROVE_FREE')">免费更换</button>
        <button class="btn small ghost" onclick="processReplacement(${r.id},'APPROVE_DEPOSIT',${r.depositDeducted || 0})">扣押金更换（建议扣 ${r.depositDeducted || 0} 元）</button>
        <button class="btn small ghost" onclick="processReplacement(${r.id},'REPAIR')">建议维修</button>
        <button class="btn small danger" onclick="processReplacement(${r.id},'REJECT')">驳回</button>
      </div>` : `<div style="font-size:12px;color:#888;margin-top:4px">处理人：${esc(r.processedBy || '-')} ${fmtDT(r.processedAt)} ${r.processNote ? '· ' + esc(r.processNote) : ''}</div>`}
    </div>`).join('');
}

async function processReplacement(id, action, suggestedDeduct) {
  const msg = document.getElementById('msg');
  const body = { action };
  if (action === 'APPROVE_DEPOSIT') {
    const v = prompt('请输入押金扣减金额（元）', suggestedDeduct || '');
    if (v === null) return;
    body.depositDeducted = Number(v);
  }
  if (action === 'REJECT' || action === 'REPAIR') {
    body.note = prompt('请填写处理备注（可空）') || '';
  }
  try {
    await api(`/api/manager/replacements/${id}/process`, { method: 'POST', body });
    showMsg(msg, '已处理', true);
    loadReplacements();
  } catch (err) { showMsg(msg, err.message, false); }
}

// ---------- 事故核查 ----------
async function loadAccidents() {
  const list = await api('/api/manager/accidents' + qs());
  const el = document.getElementById('accidentList');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无事故申报</p>'; return; }
  el.innerHTML = `<table><thead><tr>
    <th>时间</th><th>骑手</th><th>类型</th><th>地点</th><th>线路</th><th>天气</th><th>状态</th><th>操作</th>
    </tr></thead><tbody>${list.map(a => `<tr>
      <td>${fmtDT(a.occurredAt)}</td><td>${esc(a.riderName)}</td><td>${L('accidentType', a.type)}</td>
      <td>${esc(a.location)}</td><td>${esc(a.routeArea || '-')}</td><td>${L('weather', a.weather)}</td>
      <td>${badge('accidentStatus', a.status)}</td>
      <td>
        <button class="btn small ghost" onclick="showAccident(${a.id})">详情</button>
        ${(a.status === 'PENDING' || a.status === 'UNDER_REVIEW') ? `<button class="btn small" onclick="openReview(${a.id})">核查</button>` : ''}
      </td></tr>`).join('')}</tbody></table>`;
}

async function openReview(id) {
  const a = await api('/api/manager/accidents/' + id);
  document.getElementById('rvAccidentId').value = id;
  document.getElementById('reviewAccidentInfo').innerHTML = `<table>
    <tr><th style="width:100px">骑手</th><td>${esc(a.riderName)}</td></tr>
    <tr><th>类型</th><td>${L('accidentType', a.type)} · ${fmtDT(a.occurredAt)} · ${L('weather', a.weather)}</td></tr>
    <tr><th>地点/订单</th><td>${esc(a.location)}（${esc(a.routeArea || '-')}）/ ${esc(a.orderNo || '-')}</td></tr>
    <tr><th>装备状态</th><td>${esc(a.equipmentStatusDesc || '-')}${a.damagedEquipmentTypeName ? ' · 损坏：' + esc(a.damagedEquipmentTypeName) : ''}</td></tr>
    <tr><th>伤情</th><td>${esc(a.injuryDesc || '-')}</td></tr>
    <tr><th>交警记录</th><td>${esc(a.policeRecordNo || '无')}</td></tr>
    <tr><th>现场照片</th><td>${a.photos && a.photos.length ? '' : '未提交'}</td></tr>
  </table>${photoThumbs(a.photos)}`;
  document.getElementById('reviewModal').classList.add('show');
}

async function submitReview(result) {
  const msg = document.getElementById('msg');
  const id = document.getElementById('rvAccidentId').value;
  try {
    await api(`/api/manager/accidents/${id}/review`, {
      method: 'POST',
      body: {
        wasDelivering: document.getElementById('rvDelivering').value === 'true',
        wearingEquipment: document.getElementById('rvWearing').value === 'true',
        hasViolation: document.getElementById('rvViolation').value === 'true',
        violationDesc: document.getElementById('rvViolationDesc').value || null,
        insuranceNeeded: document.getElementById('rvInsurance').value === 'true',
        materialsComplete: document.getElementById('rvMaterials').value === 'true',
        reviewNotes: document.getElementById('rvNotes').value || null,
        result,
      },
    });
    closeModal('reviewModal');
    showMsg(msg, result === 'APPROVED' ? '核查通过，已联动生成保险/补发/考核记录' : '已驳回该申报', true);
    loadAccidents();
  } catch (err) { showMsg(msg, err.message, false); }
}

async function showAccident(id) {
  const a = await api('/api/manager/accidents/' + id);
  let html = `<table>
    <tr><th style="width:110px">骑手</th><td>${esc(a.riderName)}（${esc(a.stationName || '')}）</td></tr>
    <tr><th>类型</th><td>${L('accidentType', a.type)} · ${badge('accidentStatus', a.status)}</td></tr>
    <tr><th>时间/天气</th><td>${fmtDT(a.occurredAt)} · ${L('weather', a.weather)}</td></tr>
    <tr><th>地点/订单</th><td>${esc(a.location)}（${esc(a.routeArea || '-')}）/ ${esc(a.orderNo || '-')}</td></tr>
    <tr><th>装备状态</th><td>${esc(a.equipmentStatusDesc || '-')}</td></tr>
    <tr><th>伤情/交警记录</th><td>${esc(a.injuryDesc || '-')} / ${esc(a.policeRecordNo || '无')}</td></tr>
  </table>`;
  if (a.photos && a.photos.length) {
    html += `<div class="section-title">现场照片（${a.photos.length} 张）</div>` + photoThumbs(a.photos);
  }
  if (a.review) {
    html += `<div class="section-title">核查结论（${esc(a.review.reviewerName)} · ${fmtDT(a.review.reviewedAt)}）</div><table>
      <tr><th style="width:110px">配送中</th><td>${boolText(a.review.wasDelivering)}</td></tr>
      <tr><th>佩戴装备</th><td>${boolText(a.review.wearingEquipment)}</td></tr>
      <tr><th>违规骑行</th><td>${boolText(a.review.hasViolation)} ${esc(a.review.violationDesc || '')}</td></tr>
      <tr><th>需要保险</th><td>${boolText(a.review.insuranceNeeded)} · 材料齐全：${boolText(a.review.materialsComplete)}</td></tr>
      <tr><th>备注</th><td>${esc(a.review.reviewNotes || '-')}</td></tr></table>`;
  }
  if (a.claims && a.claims.length) {
    html += `<div class="section-title">保险理赔</div><table><thead><tr><th>单号</th><th>金额</th><th>状态</th><th>材料</th></tr></thead>
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
  document.getElementById('accidentDetail').innerHTML = html;
  document.getElementById('accidentModal').classList.add('show');
}

// ---------- 保险理赔 ----------
async function loadClaims() {
  const list = await api('/api/manager/claims' + qs());
  const el = document.getElementById('claimTable');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无理赔单</p>'; return; }
  el.innerHTML = `<table><thead><tr>
    <th>单号</th><th>骑手</th><th>金额(元)</th><th>状态</th><th>材料说明</th><th>操作</th>
    </tr></thead><tbody>${list.map(c => `<tr>
      <td>${esc(c.claimNo)}</td><td>${esc(c.riderName)}</td><td>${c.amount}</td>
      <td>${badge('claimStatus', c.status)}</td><td>${esc(c.materialsNotes || '-')}</td>
      <td><button class="btn small ghost" onclick="updateClaim(${c.id},'${c.status}',${c.amount})">更新</button></td>
    </tr>`).join('')}</tbody></table>`;
}

async function updateClaim(id, currentStatus, currentAmount) {
  const msg = document.getElementById('msg');
  const status = prompt('请输入新状态（DRAFT/SUBMITTED/APPROVED/PAID/REJECTED）', currentStatus);
  if (!status) return;
  const amount = prompt('请输入理赔金额（元）', currentAmount);
  if (amount === null) return;
  const notes = prompt('材料说明（可空）') || null;
  try {
    await api('/api/manager/claims/' + id, { method: 'PUT', body: { status, amount: Number(amount), materialsNotes: notes } });
    showMsg(msg, '理赔单已更新', true);
    loadClaims();
  } catch (err) { showMsg(msg, err.message, false); }
}

// ---------- 装备补发 ----------
async function loadReissues() {
  const list = await api('/api/manager/reissues' + qs());
  const el = document.getElementById('reissueTable');
  if (!list.length) { el.innerHTML = '<p style="color:#999">暂无补发单</p>'; return; }
  el.innerHTML = `<table><thead><tr>
    <th>骑手</th><th>装备</th><th>尺码</th><th>原因</th><th>状态</th><th>操作</th>
    </tr></thead><tbody>${list.map(r => `<tr>
      <td>${esc(r.riderName)}</td><td>${esc(r.typeName)}</td><td>${esc(r.size)}</td>
      <td>${esc(r.reason || '-')}</td><td>${badge('reissueStatus', r.status)}</td>
      <td>${r.status === 'PENDING' ? `
        <button class="btn small" onclick="processReissue(${r.id},'ISSUE')">发放</button>
        <button class="btn small danger" onclick="processReissue(${r.id},'CANCEL')">取消</button>` : fmtDT(r.processedAt)}</td>
    </tr>`).join('')}</tbody></table>`;
}

async function processReissue(id, action) {
  const msg = document.getElementById('msg');
  try {
    await api(`/api/manager/reissues/${id}/process`, { method: 'POST', body: { action } });
    showMsg(msg, action === 'ISSUE' ? '已发放（库存已扣减，生成新领用记录）' : '已取消', true);
    loadReissues();
  } catch (err) { showMsg(msg, err.message, false); }
}

// ---------- 考核 ----------
async function loadAssessments() {
  await loadRiders();
  const list = await api('/api/manager/assessments' + qs());
  document.getElementById('assessmentTable').innerHTML = list.length ? `<table><thead><tr>
    <th>时间</th><th>骑手</th><th>加减分</th><th>原因</th>
    </tr></thead><tbody>${list.map(a => `<tr>
      <td>${fmtDT(a.createdAt)}</td><td>${esc(a.riderName)}</td>
      <td style="color:${a.pointsChange >= 0 ? '#1a9e54' : '#d4380d'};font-weight:600">${a.pointsChange > 0 ? '+' : ''}${a.pointsChange}</td>
      <td>${esc(a.reason)}${a.accidentId ? `（事故 #${a.accidentId}）` : ''}</td>
    </tr>`).join('')}</tbody></table>` : '<p style="color:#999">暂无考核记录</p>';
}

// ---------- 培训 ----------
async function loadTrainings() {
  await loadRiders();
  const list = await api('/api/manager/trainings' + qs());
  document.getElementById('trainingTable').innerHTML = list.length ? `<table><thead><tr>
    <th>时间</th><th>骑手</th><th>主题</th><th>类别</th><th>成绩</th>
    </tr></thead><tbody>${list.map(t => `<tr>
      <td>${fmtDT(t.completedAt)}</td><td>${esc(t.riderName)}</td><td>${esc(t.title)}</td>
      <td>${L('trainingCategory', t.category)}</td><td>${t.score ?? '-'}</td>
    </tr>`).join('')}</tbody></table>` : '<p style="color:#999">暂无培训记录</p>';
}

// ---------- 数据分析 ----------
async function loadAnalytics() {
  const [byRider, byRoute, byWeather, consumption, retro] = await Promise.all([
    api('/api/analytics/accidents/by-rider' + qs()),
    api('/api/analytics/accidents/by-route' + qs()),
    api('/api/analytics/accidents/by-weather' + qs()),
    api('/api/analytics/equipment/consumption' + qs()),
    api('/api/analytics/retrospective' + qs()),
  ]);
  document.getElementById('chartRider').innerHTML = byRider.length
    ? barChart(byRider.map(r => ({ name: r.riderName, count: r.accidentCount })), 'name', 'count') +
      `<table style="margin-top:10px"><thead><tr><th>骑手</th><th>站点</th><th>事故数</th><th>考核分</th><th>最近事故</th></tr></thead>
       <tbody>${byRider.map(r => `<tr><td>${esc(r.riderName)}</td><td>${esc(r.stationName || '-')}</td><td>${r.accidentCount}</td>
        <td style="color:${r.assessmentScore >= 90 ? '#1a9e54' : '#d4380d'}">${r.assessmentScore}</td><td>${fmtDT(r.lastAccidentAt)}</td></tr>`).join('')}</tbody></table>`
    : '<p style="color:#999">暂无数据</p>';
  document.getElementById('chartRoute').innerHTML = barChart(byRoute, 'routeArea', 'count');
  document.getElementById('chartWeather').innerHTML = barChart(
    byWeather.map(w => ({ weather: L('weather', w.weather), count: w.count })), 'weather', 'count');
  document.getElementById('consumptionTable').innerHTML = consumption.length ? `<table><thead><tr>
    <th>装备类型</th><th>累计发放</th><th>近90天发放</th><th>当前在用</th><th>已超期在用</th><th>建议</th>
    </tr></thead><tbody>${consumption.map(c => `<tr>
      <td>${esc(c.typeName)}</td><td>${c.totalIssued}</td><td>${c.issuedLast90d}</td><td>${c.inUse}</td>
      <td style="color:${c.expiredInUse > 0 ? '#d4380d' : 'inherit'};font-weight:${c.expiredInUse > 0 ? '600' : 'normal'}">${c.expiredInUse}</td>
      <td>${c.expiredInUse > 0 ? '超期较多，建议升级装备或缩短更换周期' : c.issuedLast90d > 2 ? '消耗较快，关注质量' : '正常'}</td>
    </tr>`).join('')}</tbody></table>` : '<p style="color:#999">暂无数据</p>';

  const s = retro.summary;
  document.getElementById('retrospective').innerHTML = `
    <div class="stat-grid">
      <div class="stat"><div class="num">${s.totalReviewed}</div><div class="label">已核查事故</div></div>
      <div class="stat"><div class="num ${s.equipmentAging ? 'warn' : ''}">${s.equipmentAging}</div><div class="label">装备老化相关</div></div>
      <div class="stat"><div class="num ${s.notWearing ? 'warn' : ''}">${s.notWearing}</div><div class="label">未佩戴装备</div></div>
      <div class="stat"><div class="num ${s.deliveryPressure ? 'warn' : ''}">${s.deliveryPressure}</div><div class="label">配送压力相关</div></div>
      <div class="stat"><div class="num ${s.trainingGap ? 'warn' : ''}">${s.trainingGap}</div><div class="label">培训缺失相关</div></div>
    </div>
    ${(s.suggestions || []).map(x => `<div class="alert-item MEDIUM">💡 ${esc(x)}</div>`).join('')}
    ${retro.items.length ? `<table><thead><tr><th>事故</th><th>骑手</th><th>类型</th><th>时间</th><th>线路</th><th>归因</th></tr></thead>
      <tbody>${retro.items.map(i => `<tr><td>#${i.accidentId}</td><td>${esc(i.riderName)}</td>
        <td>${L('accidentType', i.type)}</td><td>${fmtDT(i.occurredAt)}</td><td>${esc(i.routeArea || '-')}</td>
        <td>${i.factors.length ? i.factors.map(f => `<span class="factor-tag">${esc(f)}</span>`).join('') : '<span style="color:#999">无明显归因</span>'}</td>
      </tr>`).join('')}</tbody></table>` : ''}`;
}

// ---------- 规则调整 ----------
async function loadPolicies() {
  const list = await api('/api/manager/policies' + qs());
  document.getElementById('policyList').innerHTML = list.length ? list.map(p => `
    <div style="border-bottom:1px solid #f0f0f0;padding:10px 0;display:flex;justify-content:space-between;gap:10px">
      <div>
        <div>${badge('policyType', p.type)} <b>${esc(p.title)}</b>
          <span style="font-size:12px;color:#999"> · ${esc(p.createdBy || '')} · ${fmtDT(p.createdAt)}</span></div>
        <div style="font-size:13px;color:#666;margin-top:4px">${esc(p.content)}</div>
      </div>
      <button class="btn small danger" onclick="deletePolicy(${p.id})">删除</button>
    </div>`).join('') : '<p style="color:#999">暂无规则调整</p>';
}

async function deletePolicy(id) {
  if (!confirm('确认删除该规则？')) return;
  await api('/api/manager/policies/' + id, { method: 'DELETE' });
  loadPolicies();
}

// ---------- 初始化 ----------
document.addEventListener('DOMContentLoaded', async () => {
  currentUser = await api('/api/auth/me');
  equipmentTypes = await api('/api/equipment/types');

  if (currentUser.stationId) {
    stationId = currentUser.stationId;
    document.getElementById('userInfo').textContent = `${currentUser.name}（${currentUser.stationName}）`;
  } else {
    // 管理员：站点切换
    const stations = await api('/api/stations');
    document.getElementById('stationPicker').style.display = '';
    document.getElementById('stationSelect').innerHTML =
      stations.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
    stationId = stations.length ? stations[0].id : null;
    document.getElementById('userInfo').textContent = `${currentUser.name}（管理员）`;
  }

  document.getElementById('iType').innerHTML =
    equipmentTypes.map(t => `<option value="${t.id}">${esc(t.name)}（押金 ${t.deposit} 元 / 周期 ${t.replacementCycleMonths} 月）</option>`).join('');
  document.getElementById('iType').addEventListener('change', fillTypeSizes);
  fillTypeSizes();

  document.querySelectorAll('.tabs button').forEach(b => b.addEventListener('click', () => switchTab(b.dataset.tab)));

  const msg = document.getElementById('msg');
  document.getElementById('issueForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('/api/manager/issue' + qs(), {
        method: 'POST',
        body: {
          riderId: Number(document.getElementById('iRider').value),
          equipmentTypeId: Number(document.getElementById('iType').value),
          size: document.getElementById('iSize').value,
          notes: document.getElementById('iNotes').value || null,
        },
      });
      showMsg(msg, '发放成功，库存已扣减', true);
      document.getElementById('iNotes').value = '';
      loadStockTab();
    } catch (err) { showMsg(msg, err.message, false); }
  });

  document.getElementById('assessForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('/api/manager/assessments', {
        method: 'POST',
        body: {
          riderId: Number(document.getElementById('asRider').value),
          pointsChange: Number(document.getElementById('asPoints').value),
          reason: document.getElementById('asReason').value,
        },
      });
      showMsg(msg, '考核已记录', true);
      e.target.reset();
      loadAssessments();
    } catch (err) { showMsg(msg, err.message, false); }
  });

  document.getElementById('trainingForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('/api/manager/trainings', {
        method: 'POST',
        body: {
          riderId: Number(document.getElementById('tRider').value),
          title: document.getElementById('tTitle').value,
          category: document.getElementById('tCategory').value,
          score: document.getElementById('tScore').value ? Number(document.getElementById('tScore').value) : null,
        },
      });
      showMsg(msg, '培训已登记', true);
      e.target.reset();
      loadTrainings();
    } catch (err) { showMsg(msg, err.message, false); }
  });

  document.getElementById('policyForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('/api/manager/policies' + qs(), {
        method: 'POST',
        body: {
          type: document.getElementById('pType').value,
          title: document.getElementById('pTitle').value,
          content: document.getElementById('pContent').value,
        },
      });
      showMsg(msg, '规则已保存', true);
      e.target.reset();
      loadPolicies();
    } catch (err) { showMsg(msg, err.message, false); }
  });

  await loadRiders();
  loadOverview();
});
