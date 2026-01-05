// ==================== 页面初始化 ====================
document.addEventListener('DOMContentLoaded', function() {
    console.log('医生页面加载完成');

    // 检查登录状态
    const savedUser = localStorage.getItem('aiHealthUser');
    if (!savedUser) {
        window.location.href = '/login';
        return;
    }

    try {
        currentUser = JSON.parse(savedUser);

        // 更新医生信息
        const doctorNameElement = document.getElementById('doctorName');
        const doctorDeptElement = document.getElementById('doctorDept');
        const todayElement = document.getElementById('todayDate');

        if (doctorNameElement) {
            doctorNameElement.textContent = currentUser.greeting || currentUser.real_name;
        }
        if (doctorDeptElement) {
            doctorDeptElement.textContent = currentUser.dept_name || '全科医生';
        }

        // 设置今天日期
        const today = new Date();
        if (todayElement) {
            todayElement.textContent = today.toLocaleDateString('zh-CN', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                weekday: 'long'
            });
        }

        // 更新统计数据
        updateDoctorStats();

        // 初始化报告查看功能
        initReportViewer();

    } catch (e) {
        console.error('解析医生数据失败:', e);
        window.location.href = '/login';
    }
});

// ==================== 医生端功能 ====================
function updateDoctorStats() {
    const patientCount = document.getElementById('patientCount');
    const pendingReports = document.getElementById('pendingReports');
    const messages = document.getElementById('messages');

    const today = new Date().toISOString().split('T')[0];
    fetch(`/api/doctors/daily-patients?date=${today}`, { credentials: 'include' })
        .then(r => r.json())
        .then(list => {
            if (patientCount) patientCount.textContent = Array.isArray(list) ? list.length : 0;
            if (pendingReports) {
                const pending = Array.isArray(list) ? list.filter(p => p.status === '待处理').length : 0;
                pendingReports.textContent = pending;
            }
            if (messages) messages.textContent = Math.floor(Math.random() * 3) + 1;
        })
        .catch(() => {
            if (patientCount) patientCount.textContent = 0;
            if (pendingReports) pendingReports.textContent = 0;
        });
}

function refreshPatientList() {
    const today = new Date().toISOString().split('T')[0];
    fetch(`/api/doctors/daily-patients?date=${today}`, { credentials: 'include' })
        .then(r => r.json())
        .then(list => {
            renderPatientList(list);
            showMessage('患者列表已刷新', 'success');
        })
        .catch(() => showMessage('刷新失败', 'error'));
}

// ==================== 报告查看功能 ====================

let currentPatientUserId = null;
let currentAIAnalysis = null;

function renderPatientList(patients) {
    const table = document.querySelector('.patient-table');
    if (!table) return;
    const headerHtml = `
        <div class="table-header">
          <div>患者姓名</div>
          <div>检查项目</div>
          <div>状态</div>
          <div>操作</div>
        </div>`;
    const rows = (patients || []).map(p => {
        const initials = (p.name || '').charAt(0) || '患';
        const tests = (p.tests || []).join(' + ') || '—';
        return `
        <div class="table-row">
          <div class="patient-info">
            <div class="patient-avatar">${initials}</div>
            <div>
              <strong>${p.name}</strong>
              <small>${p.gender} · ${p.age}岁</small>
            </div>
          </div>
          <div>${tests}</div>
          <div>${getStatusBadge(p.status)}</div>
          <div>
            ${renderActionButton(p)}
          </div>
        </div>`;
    }).join('');
    table.innerHTML = headerHtml + rows;
}

function getStatusBadge(status) {
    switch (status) {
        case '待检查':
            return '<span class="status-badge pending" style="background-color: #6c757d; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">待检查</span>';
        case '待提交报告':
            return '<span class="status-badge warning" style="background-color: #ffc107; color: black; padding: 2px 8px; border-radius: 12px; font-size: 12px;">待提交报告</span>';
        case '待处理':
            return '<span class="status-badge info" style="background-color: #0d6efd; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">待处理</span>';
        case '处理完成':
            return '<span class="status-badge completed" style="background-color: #28a745; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">处理完成</span>';
        default:
            return '<span class="status-badge pending" style="background-color: #6c757d; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">—</span>';
    }
}

function renderActionButton(p) {
    if (p.status === '待检查') {
        return `<button class="btn btn-sm btn-secondary" onclick="callForCheck(${p.userId})">呼叫检查</button>`;
    } else if (p.status === '待提交报告') {
        return `<button class="btn btn-sm btn-warning" disabled>待提交</button>`;
    } else {
        return `<button class="btn btn-sm btn-primary view-report-btn" data-patient-id="${p.userId}">查看报告</button>`;
    }
}

function callForCheck(userId) {
    const modal = document.createElement('div');
    modal.id = 'callCheckModal';
    modal.style.position = 'fixed';
    modal.style.inset = '0';
    modal.style.background = 'rgba(0,0,0,0.5)';
    modal.style.display = 'flex';
    modal.style.alignItems = 'center';
    modal.style.justifyContent = 'center';
    modal.innerHTML = `
      <div style="background:#fff;border-radius:12px;max-width:800px;width:90%;padding:20px;box-shadow:0 10px 30px rgba(0,0,0,0.2)">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
          <h3 style="margin:0">呼叫检查与制定清单</h3>
          <button id="closeCallCheck" class="btn btn-sm">关闭</button>
        </div>
        <div style="display:flex;gap:20px">
          <div style="flex:1">
            <h4 style="margin:10px 0">呼叫状态</h4>
            <div style="display:flex;gap:10px">
              <button id="startCallBtn" class="btn btn-primary btn-sm">发送呼叫</button>
              <button id="cancelCallBtn" class="btn btn-outline btn-sm">取消呼叫</button>
            </div>
            <p style="color:#64748b;margin-top:8px">患者页面将显示：请某某某到某科室某医生咨询就诊！</p>
          </div>
          <div style="flex:2">
            <h4 style="margin:10px 0">制定检查清单</h4>
            <div id="checklistEditor" style="display:grid;grid-template-columns:1fr 1fr;gap:8px;border:1px solid #e2e8f0;border-radius:8px;padding:10px">
            </div>
            <div style="display:flex;gap:10px;margin-top:10px">
              <button id="sendChecklistBtn" class="btn btn-success btn-sm">发送清单给患者</button>
              <button id="directSendBtn" class="btn btn-warning btn-sm" title="允许患者未完成清单即可上传">直接发送 (免完成)</button>
            </div>
            <p style="color:#64748b;margin-top:8px">只有发送后患者才能看到清单，否则显示“等待医生制定”。</p>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
    const defaultItems = [
      { name: '血常规', location: '三楼血检科检验室', completed: false },
      { name: '尿常规', location: '三楼检验科203室', completed: false },
      { name: '胸部CT', location: '三楼影像科CT室', completed: false },
      { name: '心电图', location: '二楼心电图室', completed: false },
      { name: '腹部B超', location: '一楼内科B超室', completed: false },
      { name: '腹部B超2', location: '四楼超声科405室', completed: false },
      { name: '视力检查', location: '五楼眼科门诊', completed: false }
    ];
    const editor = modal.querySelector('#checklistEditor');
    editor.innerHTML = defaultItems.map((it, idx) => `
      <label style="display:flex;gap:8px;align-items:center">
        <input type="checkbox" data-idx="${idx}" />
        <span>${it.name}</span>
        <small style="color:#64748b">· ${it.location}</small>
      </label>
    `).join('');
    modal.querySelector('#closeCallCheck').addEventListener('click', () => modal.remove());
    modal.querySelector('#startCallBtn').addEventListener('click', () => {
      fetch('/api/doctors/call/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ patientUserId: userId })
      }).then(r => r.json()).then(data => {
        if (data.success) showMessage('已发送呼叫', 'success'); else showMessage(data.message || '呼叫失败', 'error');
      }).catch(() => showMessage('呼叫失败', 'error'));
    });
    modal.querySelector('#cancelCallBtn').addEventListener('click', () => {
      fetch('/api/doctors/call/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ patientUserId: userId })
      }).then(r => r.json()).then(data => {
        if (data.success) showMessage('已取消呼叫', 'success'); else showMessage(data.message || '取消失败', 'error');
      }).catch(() => showMessage('取消失败', 'error'));
    });
    const handleSendChecklist = (allowBypass) => {
      const chosen = [];
      editor.querySelectorAll('input[type="checkbox"]').forEach((cb, idx) => {
        if (cb.checked) chosen.push(defaultItems[idx]);
      });
      if (chosen.length === 0) { showMessage('请至少勾选一个检查项目', 'error'); return; }
      fetch('/api/doctors/checklist/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ patientUserId: userId, items: chosen, allowBypass: allowBypass })
      }).then(r => r.json()).then(data => {
        if (data.success) { 
            showMessage(allowBypass ? '清单已发送 (允许直接上传)' : '清单已发送', 'success'); 
            modal.remove(); 
        } else showMessage(data.message || '发送失败', 'error');
      }).catch(() => showMessage('发送失败', 'error'));
    };
    modal.querySelector('#sendChecklistBtn').addEventListener('click', () => handleSendChecklist(false));
    modal.querySelector('#directSendBtn').addEventListener('click', () => handleSendChecklist(true));
}

// 报告查看功能初始化
function initReportViewer() {
    console.log('初始化报告查看功能');

    // 绑定查看报告按钮事件（使用事件委托）
    document.addEventListener('click', function(e) {
        if (e.target.closest('.view-report-btn')) {
            const button = e.target.closest('.view-report-btn');
            const patientId = button.dataset.patientId;
            openReportModal(patientId);
        }
    });

    // 绑定关闭按钮事件
    const closeBtn = document.getElementById('reportClose');
    if (closeBtn) {
        closeBtn.addEventListener('click', closeReportModal);
    }

    // 点击模态框背景关闭
    const modal = document.getElementById('reportModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                closeReportModal();
            }
        });
    }

    // 绑定操作按钮事件
    bindReportActions();
}

// 打开报告模态框
function openReportModal(patientId) {
    currentPatientUserId = parseInt(patientId, 10);
    const modal = document.getElementById('reportModal');
    if (!modal) return;
    fetch(`/api/doctors/patient-report/${currentPatientUserId}`, { credentials: 'include' })
        .then(r => r.json())
        .then(data => {
            const basic = data.basicInfo || {};
            updatePatientInfo({
                name: basic.name || '',
                gender: basic.gender || '',
                age: basic.age || '',
                examDate: data.lastExamDate || ''
            });
            const reports = data.reports || {};
            const items = [...(reports.blood || []), ...(reports.urine || []), ...(reports.liver || []), ...(reports.kidney || [])];
            const testResults = items.map(i => ({
                name: i.itemName,
                value: i.result,
                range: i.reference || '',
                status: i.status || 'normal'
            }));
            updateTestResults(testResults);
            currentAIAnalysis = data.analysis || null;
            updateAIReport(currentAIAnalysis);
            updateDoctorAdvice('');
            modal.classList.remove('hidden');
            document.body.style.overflow = 'hidden';
        })
        .catch(() => showMessage('加载报告失败', 'error'));
}

// 关闭报告模态框
function closeReportModal() {
    const modal = document.getElementById('reportModal');
    if (modal) {
        modal.classList.add('hidden');
        document.body.style.overflow = '';
    }
}

// 更新患者信息
function updatePatientInfo(patientData) {
    document.getElementById('reportPatientName').textContent = patientData.name;
    document.getElementById('reportPatientGender').textContent = patientData.gender;
    document.getElementById('reportPatientAge').textContent = `${patientData.age}岁`;
    document.getElementById('reportDate').textContent = patientData.examDate;
}

// 更新检查结果
function updateTestResults(testResults) {
    const resultsGrid = document.getElementById('testResultsGrid');
    if (!resultsGrid) return;

    resultsGrid.innerHTML = '';

    testResults.forEach(result => {
        const resultCard = document.createElement('div');
        resultCard.className = 'result-card';

        const statusClass = getStatusClass(result.status);
        const statusText = getStatusText(result.status);

        resultCard.innerHTML = `
            <div class="result-title">${result.name}</div>
            <div class="result-value">${result.value}</div>
            <div class="result-range">参考值: ${result.range}</div>
            <div class="result-status ${statusClass}">${statusText}</div>
        `;

        resultsGrid.appendChild(resultCard);
    });
}

// 更新AI报告
function updateAIReport(aiReport) {
    const aiContent = document.getElementById('aiReportContent');
    if (!aiContent) return;
    aiContent.innerHTML = '';
    if (!aiReport) return;
    const sections = [
        { key: 'blood', title: '血检AI分析' },
        { key: 'urine', title: '尿检AI分析' },
        { key: 'liver', title: '肝功能AI分析' },
        { key: 'kidney', title: '肾功能AI分析' }
    ];
    sections.forEach(sec => {
        const rec = aiReport[sec.key];
        if (rec && rec.recommendation && rec.recommendation.trim() !== '') {
            const div = document.createElement('div');
            div.className = 'ai-report-section';
            div.innerHTML = `
                <h5>${sec.title}</h5>
                <p><strong>预测结论:</strong> ${rec.predictionResult || ''}</p>
                <p><strong>风险等级:</strong> ${rec.riskLevel || ''}</p>
                <p><strong>建议:</strong> ${rec.recommendation || ''}</p>
            `;
            aiContent.appendChild(div);
        }
    });
}

// 更新医生建议
function updateDoctorAdvice(advice) {
    const adviceTextarea = document.getElementById('doctorAdvice');
    if (adviceTextarea) {
        adviceTextarea.value = advice || '';
    }
}

// 绑定报告操作按钮
function bindReportActions() {
    // 保存草稿按钮
    const saveDraftBtn = document.getElementById('saveDraftBtn');
    if (saveDraftBtn) {
        saveDraftBtn.addEventListener('click', function() {
            const advice = document.getElementById('doctorAdvice').value;

            if (!advice.trim()) {
                showMessage('请填写医生建议', 'error');
                return;
            }

            fetch('/api/doctors/advice/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ patientUserId: currentPatientUserId, suggestion: advice })
            }).then(r => r.json())
             .then(res => {
                 if (res.success) showMessage('医生建议已保存为草稿', 'success');
                 else showMessage(res.message || '保存失败', 'error');
             }).catch(() => showMessage('保存失败', 'error'));
        });
    }

    // 发送报告按钮
    const sendReportBtn = document.getElementById('sendReportBtn');
    if (sendReportBtn) {
        sendReportBtn.addEventListener('click', function() {
            const advice = document.getElementById('doctorAdvice').value;

            if (!advice.trim()) {
                showMessage('请填写医生建议后再发送', 'error');
                return;
            }

            if (confirm('确定要发送报告给患者吗？')) {
                fetch('/api/doctors/advice/send', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ patientUserId: currentPatientUserId, suggestion: advice, ai: currentAIAnalysis })
                }).then(r => r.json())
                 .then(res => {
                     if (res.success) {
                         showMessage('报告已成功发送给患者', 'success');
                         closeReportModal();
                     } else {
                         showMessage(res.message || '发送失败', 'error');
                     }
                 }).catch(() => showMessage('发送失败', 'error'));
            }
        });
    }

    // 打印报告按钮
    const printReportBtn = document.getElementById('printReportBtn');
    if (printReportBtn) {
        printReportBtn.addEventListener('click', function() {
            showMessage('打印功能开发中...', 'info');
            // 这里可以添加打印功能
        });
    }
}

// 辅助函数：获取状态类名
function getStatusClass(status) {
    switch(status) {
        case 'normal': return 'status-normal';
        case 'warning': return 'status-warning';
        case 'alert': return 'status-alert';
        default: return 'status-normal';
    }
}

// 辅助函数：获取状态文本
function getStatusText(status) {
    switch(status) {
        case 'normal': return '正常';
        case 'warning': return '注意';
        case 'alert': return '异常';
        default: return '正常';
    }
}

// 确保函数在全局可用
window.refreshPatientList = refreshPatientList;
window.openReportModal = openReportModal;
window.closeReportModal = closeReportModal;
