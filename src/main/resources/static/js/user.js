// 用户端特定变量
let currentPage = 1;
let currentSort = 'rating';
let currentDept = 'all';
const doctorsPerPage = 6;

// API配置
const API_BASE = '/api';
const DOCTORS_API = `${API_BASE}/doctors`;
const DEPARTMENTS_API = `${API_BASE}/departments`;
const REPORTS_API = `${API_BASE}/reports/me`;
let aggregatedReports = null;

document.addEventListener('DOMContentLoaded', function() {
    console.log('用户页面加载完成');

    const savedUser = localStorage.getItem('aiHealthUser');
    if (!savedUser) {
        window.location.href = '/login';
        return;
    }

    try {
        currentUser = JSON.parse(savedUser);

        const displayName = document.getElementById('displayUserName');
        const currentUserSpan = document.getElementById('currentUser');

        if (displayName && currentUser) {
            displayName.textContent = currentUser.greeting || currentUser.real_name;
        }
        if (currentUserSpan && currentUser) {
            currentUserSpan.textContent = currentUser.greeting || currentUser.real_name;
        }

        initAppointmentSection();
        initAnalysisTabs();
        initScrollAnimations();
        initAssignedDoctorInfo(); // Initialize assigned doctor info
        initCallNotice();
        initExamChecklist();

    } catch (e) {
        console.error('解析用户数据失败:', e);
        window.location.href = '/login';
    }
});

function initAssignedDoctorInfo() {
    const infoDiv = document.getElementById('assignedDoctorInfo');
    const textSpan = document.getElementById('assignedDoctorText');
    
    if (!infoDiv || !textSpan) return;

    fetch('/api/appointments/last-successful')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                textSpan.textContent = `您的体检数据将同步发送给：${data.deptName} - ${data.doctorName} 医生`;
                infoDiv.style.display = 'inline-block';
            } else {
                // 如果没有预约记录，可以显示默认提示或者不显示
                if (data.message === '无预约记录') {
                     textSpan.textContent = '您尚未预约医生，数据将仅由AI进行分析';
                     infoDiv.style.display = 'inline-block';
                     // 样式可以稍微改一下，比如黄色警告
                     infoDiv.style.backgroundColor = '#fff7ed';
                     infoDiv.style.color = '#9a3412';
                     infoDiv.style.borderLeftColor = '#f97316';
                }
            }
        })
        .catch(err => {
            console.error('获取负责医生失败:', err);
        });
}

function initCallNotice() {
    const banner = document.getElementById('callNoticeBanner') || (function() {
        const b = document.createElement('div');
        b.id = 'callNoticeBanner';
        b.style.display = 'none';
        b.style.position = 'fixed';
        b.style.top = '0';
        b.style.left = '0';
        b.style.right = '0';
        b.style.zIndex = '1000';
        b.style.padding = '12px 16px';
        b.style.background = '#fff7ed';
        b.style.color = '#9a3412';
        b.style.borderBottom = '1px solid #f97316';
        b.style.boxShadow = '0 2px 8px rgba(0,0,0,0.06)';
        b.style.textAlign = 'center';
        document.body.prepend(b);
        return b;
    })();
    function applyBodyOffset(show) {
        const offset = show ? '52px' : '0px';
        document.body.style.paddingTop = offset;
    }
    function refreshCallNotice() {
        fetch('/api/doctors/call/me', { credentials: 'include' })
            .then(r => r.json())
            .then(data => {
                if (data.success && data.active && data.message) {
                    banner.textContent = data.message;
                    banner.style.display = 'block';
                    applyBodyOffset(true);
                } else {
                    banner.style.display = 'none';
                    applyBodyOffset(false);
                }
            })
            .catch(() => {});
    }
    refreshCallNotice();
    window.__callNoticeTimer && clearInterval(window.__callNoticeTimer);
    window.__callNoticeTimer = setInterval(refreshCallNotice, 10000);
}

function initExamChecklist() {
    const sec = document.getElementById('examChecklistSection') || (function() {
        const s = document.createElement('div');
        s.id = 'examChecklistSection';
        s.style.margin = '20px 0';
        s.innerHTML = `
          <h3 style="margin-bottom:8px">我的检查清单</h3>
          <div id="examChecklistContainer" style="border:1px solid #e2e8f0;border-radius:8px;padding:12px;background:#fff"></div>
        `;
        const anchor = document.getElementById('uploadArea') || document.body.firstChild;
        anchor.parentNode.insertBefore(s, anchor);
        return s;
    })();
    loadChecklistForMe();
    window.__checklistTimer && clearInterval(window.__checklistTimer);
    window.__checklistTimer = setInterval(loadChecklistForMe, 10000);
}

function loadChecklistForMe() {
    fetch('/api/doctors/checklist/me', { credentials: 'include' })
        .then(r => r.json())
        .then(data => {
            const container = document.getElementById('examChecklistContainer');
            const uploadArea = document.getElementById('uploadArea');
            const fileInput = document.getElementById('excelFileInput');
            if (!container) return;

            // 辅助函数：更新上传区域状态
            const updateUploadStatus = (done) => {
                if (!uploadArea) return;
                let msgDiv = document.getElementById('checklistIncompleteMsg');
                if (!done) {
                    if (!msgDiv) {
                        msgDiv = document.createElement('div');
                        msgDiv.id = 'checklistIncompleteMsg';
                        msgDiv.style.color = '#ef4444';
                        msgDiv.style.fontWeight = 'bold';
                        msgDiv.style.textAlign = 'center';
                        msgDiv.style.marginTop = '10px';
                        msgDiv.innerText = '未完成全部检查内容';
                        uploadArea.appendChild(msgDiv);
                    }
                    uploadArea.style.pointerEvents = 'none';
                    uploadArea.style.opacity = '0.6';
                    if (fileInput) fileInput.disabled = true;
                } else {
                    if (msgDiv) msgDiv.remove();
                    uploadArea.style.pointerEvents = 'auto';
                    uploadArea.style.opacity = '1';
                    if (fileInput) fileInput.disabled = false;
                }
            };

            if (!data.success) {
                container.innerHTML = '<div style="color:#b91c1c">未登录，请先登录</div>';
                return;
            }
            if (!data.hasChecklist) {
                container.innerHTML = '<div style="color:#64748b">等待医生制定</div>';
                updateUploadStatus(false);
                return;
            }
            const items = data.items || [];
            container.innerHTML = items.map((it, idx) => `
              <label style="display:flex;gap:8px;align-items:center;margin:6px 0">
                <input type="checkbox" data-idx="${idx}" ${it.completed ? 'checked' : ''}/>
                <span>${it.name}</span>
                <small style="color:#64748b">· ${it.location}</small>
              </label>
            `).join('');
            
            const allDone = items.length > 0 && items.every(i => i.completed);
            
            // 检查是否有免完成权限
            const allowBypass = data.allowBypass === true;
            
            // 如果全部完成或者允许跳过，则启用上传
            updateUploadStatus(allDone || allowBypass);

            container.querySelectorAll('input[type="checkbox"]').forEach(cb => {
                cb.addEventListener('change', () => {
                    const updated = items.map((it, i) => ({
                        name: it.name,
                        location: it.location,
                        completed: container.querySelector(`input[data-idx="${i}"]`).checked
                    }));
                    fetch('/api/doctors/checklist/progress', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                        body: JSON.stringify({ items: updated })
                    }).then(r => r.json()).then(d => {
                        const done = updated.length > 0 && updated.every(i => i.completed);
                        updateUploadStatus(done || allowBypass);
                        showMessage(done ? '已完成所有检查，可进行指标分析' : '已更新检查进度', 'success');
                    });
                });
            });
        });
}

async function initAppointmentSection() {
    console.log('初始化新的预约挂号功能');

    try {
        await initDepartmentFilter();
        await filterAndSortDoctors();
        bindFilterEvents();
        bindSortEvents();
        bindPaginationEvents();
        console.log('预约功能初始化完成');
    } catch (error) {
        console.error('初始化预约功能失败:', error);
        showMessage('加载数据失败，请刷新页面重试', 'error');
    }
}

async function initDepartmentFilter() {
    const deptDropdown = document.getElementById('deptDropdown');
    if (!deptDropdown) {
        console.error('找不到科室下拉菜单');
        return;
    }

    const deptContent = deptDropdown.querySelector('.dropdown-content');
    if (!deptContent) {
        console.error('找不到科室下拉菜单内容区域');
        return;
    }

    try {
        // 从后端获取科室数据
        const response = await fetch(DEPARTMENTS_API);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const departments = await response.json();
        console.log('获取到科室数据:', departments);

        deptContent.innerHTML = '';

        // 添加"全部科室"选项
        const allItem = document.createElement('div');
        allItem.className = 'dropdown-item selected';
        allItem.dataset.deptCode = 'all';
        allItem.innerHTML = `
            <span>全部科室</span>
        `;
        allItem.addEventListener('click', () => selectDepartment('all', '全部科室'));
        deptContent.appendChild(allItem);

        // 添加科室选项
        departments.forEach(dept => {
            const item = document.createElement('div');
            item.className = 'dropdown-item';
            item.dataset.deptCode = dept.deptCode;
            item.innerHTML = `
                <span>${dept.deptName}</span>
                <span class="dept-type">${dept.deptType || '通用'}</span>
            `;
            item.addEventListener('click', () => selectDepartment(dept.deptCode, dept.deptName));
            deptContent.appendChild(item);
        });

    } catch (error) {
        console.error('获取科室数据失败:', error);
        showMessage('加载科室数据失败', 'error');
    }
}

function selectDepartment(deptCode, deptName) {
    console.log('选择科室:', deptCode, deptName);

    currentDept = deptCode;
    currentPage = 1; // 重置到第一页

    // 更新选中状态
    document.querySelectorAll('.dropdown-item').forEach(item => {
        item.classList.remove('selected');
    });

    const selectedItem = document.querySelector(`.dropdown-item[data-dept-code="${deptCode}"]`);
    if (selectedItem) {
        selectedItem.classList.add('selected');
    }

    // 更新按钮显示
    const selectedDeptSpan = document.getElementById('selectedDept');
    if (selectedDeptSpan) {
        selectedDeptSpan.textContent = deptName;
    }

    // 隐藏下拉菜单
    const dropdown = document.getElementById('deptDropdown');
    if (dropdown) {
        dropdown.classList.remove('show');
    }

    // 重新加载医生列表
    filterAndSortDoctors();
}

function bindFilterEvents() {
    const deptFilterBtn = document.getElementById('deptFilterBtn');
    const deptDropdown = document.getElementById('deptDropdown');

    if (deptFilterBtn && deptDropdown) {
        deptFilterBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            deptDropdown.classList.toggle('show');
        });

        document.addEventListener('click', (e) => {
            if (deptDropdown && !deptDropdown.contains(e.target) &&
                deptFilterBtn && !deptFilterBtn.contains(e.target)) {
                deptDropdown.classList.remove('show');
            }
        });
    }
}

function bindSortEvents() {
    const sortBtn = document.querySelector('.sort-btn');
    const sortDropdown = document.querySelector('.sort-dropdown');

    if (sortBtn && sortDropdown) {
        sortBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            sortDropdown.classList.toggle('show');
        });

        document.addEventListener('click', (e) => {
            if (sortDropdown && !sortDropdown.contains(e.target) &&
                sortBtn && !sortBtn.contains(e.target)) {
                sortDropdown.classList.remove('show');
            }
        });

        const sortOptions = document.querySelectorAll('.sort-option');
        sortOptions.forEach(option => {
            option.addEventListener('click', function(e) {
                e.stopPropagation();
                const sortBy = this.getAttribute('data-sort');
                selectSort(sortBy);
            });
        });
    }
}

function selectSort(sortBy) {
    console.log('选择排序:', sortBy);
    currentSort = sortBy;
    currentPage = 1; // 重置到第一页

    const sortBtn = document.querySelector('.sort-btn');
    if (sortBtn) {
        const sortText = getSortText(sortBy);
        const currentIcon = sortBtn.querySelector('i.fa-star') ? '<i class="fas fa-star"></i> ' : '';
        sortBtn.innerHTML = `
            ${currentIcon}${sortText}
            <i class="fas fa-chevron-down"></i>
        `;
    }

    const sortDropdown = document.querySelector('.sort-dropdown');
    if (sortDropdown) {
        sortDropdown.classList.remove('show');
    }

    // 重新加载医生列表
    filterAndSortDoctors();
}

function getSortText(sortBy) {
    switch(sortBy) {
        case 'rating': return '综合排序';
        case 'appointments': return '预约量最多';
        case 'fee': return '费用最低';
        case 'name': return '医生姓名';
        default: return '综合排序';
    }
}

let doctorSearchTerm = '';

async function filterAndSortDoctors() {
    try {
        // 构建查询参数
        const params = new URLSearchParams({
            page: currentPage,
            pageSize: doctorsPerPage
        });

        if (currentDept !== 'all') {
            params.append('deptCode', currentDept);
        }

        if (currentSort && currentSort !== 'rating') {
            params.append('sortBy', currentSort);
        }
        
        if (doctorSearchTerm && doctorSearchTerm.trim().length > 0) {
            params.append('doctorName', doctorSearchTerm.trim());
        }

        const url = `${DOCTORS_API}?${params.toString()}`;
        console.log('请求医生数据:', url);

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('获取到医生数据:', result);

        renderDoctorList(result.data);
        updatePagination(result);

    } catch (error) {
        console.error('获取医生数据失败:', error);
        showMessage('加载医生数据失败', 'error');
        renderEmptyDoctorList();
    }
}

function renderDoctorList(doctors) {
    const doctorList = document.getElementById('doctorList');
    if (!doctorList) {
        console.error('找不到医生列表容器');
        return;
    }

    doctorList.innerHTML = '';

    if (!doctors || doctors.length === 0) {
        doctorList.innerHTML = `
            <div class="no-doctors" style="text-align: center; padding: 60px 20px; color: #64748b; grid-column: 1 / -1;">
                <i class="fas fa-user-md" style="font-size: 48px; color: #cbd5e1; margin-bottom: 20px;"></i>
                <h3 style="color: #475569; margin-bottom: 10px;">暂无医生信息</h3>
                <p>该科室暂无可预约的医生</p>
            </div>
        `;
        return;
    }

    doctors.forEach(doctor => {
        const doctorCard = document.createElement('div');
        doctorCard.className = 'doctor-card' + (doctor.hasScheduleToday === false ? ' no-schedule' : '');
        doctorCard.innerHTML = `
            <div class="doctor-avatar-section">
                <div class="doctor-avatar">
                    <i class="fas fa-user-md"></i>
                </div>
                <div class="doctor-basic-info">
                    <div class="doctor-name">${doctor.name}</div>
                    <div class="doctor-title">
                        <span class="title-text">${doctor.title}</span>
                        <span class="dept-text">${doctor.deptName || '全科'}</span>
                    </div>
                </div>
            </div>
            
            <div class="doctor-simple-stats">
                <div class="stat-item">
                    <i class="fas fa-calendar-check"></i>
                    <div class="stat-label">挂号量</div>
                    <div class="stat-value">${doctor.appointments || 0}</div>
                </div>
                <div class="stat-item">
                    <i class="fas fa-file-medical"></i>
                    <div class="stat-label">费用</div>
                    <div class="stat-value">￥${doctor.normalFee || 0}</div>
                </div>
            </div>
            
            <div class="doctor-tags">
                <span class="tag insurance-tag">医保报销</span>
                <span class="tag online-tag">在线咨询</span>
                <span class="tag time-tag">${doctor.hasScheduleToday === false ? '未排班' : '可预约'}</span>
            </div>
            
            <div class="doctor-action-section">
                <button class="appointment-btn" ${doctor.hasScheduleToday === false ? 'disabled' : ''} onclick="makeAppointment('${doctor.doctorCode}')">
                    <i class="fas fa-calendar-alt"></i>
                    ${doctor.hasScheduleToday === false ? '暂不可约' : '立即预约'}
                </button>
                <button class="consult-btn">
                    <i class="fas fa-comment-medical"></i>
                    在线咨询
                </button>
            </div>
        `;

        doctorList.appendChild(doctorCard);
    });
}

function renderEmptyDoctorList() {
    const doctorList = document.getElementById('doctorList');
    if (doctorList) {
        doctorList.innerHTML = `
            <div class="no-doctors" style="text-align: center; padding: 60px 20px; color: #64748b; grid-column: 1 / -1;">
                <i class="fas fa-exclamation-triangle" style="font-size: 48px; color: #f59e0b; margin-bottom: 20px;"></i>
                <h3 style="color: #475569; margin-bottom: 10px;">加载失败</h3>
                <p>无法获取医生数据，请稍后重试</p>
            </div>
        `;
    }
}

// 绑定医生搜索框
document.addEventListener('DOMContentLoaded', function() {
    const doctorSearchInput = document.getElementById('doctorSearch');
    if (doctorSearchInput) {
        doctorSearchInput.addEventListener('input', function() {
            doctorSearchTerm = doctorSearchInput.value || '';
            currentPage = 1;
            filterAndSortDoctors();
        });
    }
});

function bindPaginationEvents() {
    const prevBtn = document.querySelector('.prev-btn');
    const nextBtn = document.querySelector('.next-btn');

    if (prevBtn) {
        const newPrevBtn = prevBtn.cloneNode(true);
        prevBtn.parentNode.replaceChild(newPrevBtn, prevBtn);

        document.querySelector('.prev-btn').addEventListener('click', function(e) {
            e.preventDefault();
            goToPrevPage();
        });
    }

    if (nextBtn) {
        const newNextBtn = nextBtn.cloneNode(true);
        nextBtn.parentNode.replaceChild(newNextBtn, nextBtn);

        document.querySelector('.next-btn').addEventListener('click', function(e) {
            e.preventDefault();
            goToNextPage();
        });
    }
}

function goToPrevPage() {
    if (currentPage > 1) {
        currentPage--;
        console.log('DEBUG: 前往上一页，新页码:', currentPage);
        filterAndSortDoctors();
    }
}

function goToNextPage() {
    // 只在当前页有6个医生时才允许翻到下一页
    const doctorList = document.getElementById('doctorList');
    if (doctorList) {
        const currentDoctors = doctorList.querySelectorAll('.doctor-card');
        if (currentDoctors.length === doctorsPerPage) {
            currentPage++;
            console.log('DEBUG: 前往下一页，新页码:', currentPage);
            filterAndSortDoctors();
        } else {
            console.log('DEBUG: 当前页不足6个医生，不允许翻页');
        }
    }
}

function updatePagination(result) {
    const prevBtn = document.querySelector('.prev-btn');
    const nextBtn = document.querySelector('.next-btn');
    const pageNumbers = document.querySelector('.page-numbers');

    // 更新上一页/下一页按钮状态
    if (prevBtn) {
        prevBtn.disabled = currentPage === 1;
    }

    if (nextBtn) {
        // 如果当前页数等于总页数，禁用下一页按钮
        const totalPages = result.totalPages || 1;
        nextBtn.disabled = currentPage >= totalPages;
    }

    // 更新页码显示
    if (pageNumbers && result.totalPages) {
        pageNumbers.innerHTML = '';

        const totalPages = result.totalPages;
        if (totalPages <= 1) {
            return;
        }

        const maxVisiblePages = 5;
        let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

        if (endPage - startPage + 1 < maxVisiblePages) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            const pageNumber = document.createElement('div');
            pageNumber.className = `page-number ${i === currentPage ? 'active' : ''}`;
            pageNumber.textContent = i;
            pageNumber.addEventListener('click', () => {
                currentPage = i;
                filterAndSortDoctors();
            });
            pageNumbers.appendChild(pageNumber);
        }
    }
}

async function makeAppointment(doctorCode) {
    try {
        // 发送预约请求
        const response = await fetch('/api/appointments/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ doctorCode: doctorCode })
        });

        const result = await response.json();

        if (response.ok && result.success) {
             // 重新获取医生信息以显示详情（可选，或者直接用返回的 doctorName）
             // 为了保持一致性，我们还是获取一下详情用于显示
             const docRes = await fetch(`${DOCTORS_API}/${doctorCode}`);
             const doctor = await docRes.json();
             
             showMessage(`✅ 预约成功！医生：${doctor.name}，科室：${doctor.deptName}，费用：${doctor.normalFee}元`, 'success');
        } else {
             throw new Error(result.message || '预约失败');
        }

    } catch (error) {
        console.error('预约失败:', error);
        showMessage(error.message || '预约失败，请稍后重试', 'error');
    }
}

function initScrollAnimations() {
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate');
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -100px 0px'
    });

    document.querySelectorAll('.reveal, .package-card, .metric-card').forEach(el => {
        observer.observe(el);
    });
}

function initAnalysisTabs() {
    const tabs = document.querySelectorAll('.analysis-tab');
    if (tabs.length === 0) return;

    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const tabId = this.dataset.tab;

            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('active');
            });

            const targetTab = document.getElementById(`${tabId}Tab`);
            if (targetTab) targetTab.classList.add('active');
        });
    });
}

async function handleFileSelect(input) {
    const file = input.files[0];
    if (!file) return;

    // 显示文件信息
    const uploadArea = document.getElementById('uploadArea');
    const existingContent = uploadArea.getAttribute('data-original-content') || uploadArea.innerHTML;
    if (!uploadArea.getAttribute('data-original-content')) {
        uploadArea.setAttribute('data-original-content', existingContent);
    }
    
    uploadArea.innerHTML = `
        <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;padding:20px;">
            <i class="fas fa-file-excel" style="font-size: 48px; color: #10b981;"></i>
            <div style="font-weight:bold;color:#334155;">${file.name}</div>
            <div style="color:#64748b;font-size:0.9em;">${(file.size / 1024).toFixed(1)} KB</div>
            <div style="margin-top:10px;color:#3b82f6;">正在准备上传...</div>
        </div>
    `;

    // 显示上传状态区域
    const statusDiv = document.getElementById('uploadStatus');
    const progressDiv = statusDiv.querySelector('.upload-progress');
    const resultDiv = document.getElementById('uploadResult');

    statusDiv.style.display = 'block';
    progressDiv.style.display = 'block';
    resultDiv.style.display = 'none';

    // 获取当前用户ID（优先本地存储，其次会话接口）
    const userId = await resolveUserId();

    // 上传文件
    uploadExcelFile(file, userId);
}

/**
 * 上传Excel文件到服务器
 */
function uploadExcelFile(file, userId) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', userId);

    const progressBar = document.getElementById('uploadProgressBar');
    const percentText = document.getElementById('uploadPercent');
    const resultDiv = document.getElementById('uploadResult');

    // 模拟上传进度（实际项目中可能需要XMLHttpRequest来获取真实进度）
    let progress = 0;
    const progressInterval = setInterval(() => {
        progress += 10;
        if (progress > 90) {
            clearInterval(progressInterval);
            return;
        }
        progressBar.style.width = progress + '%';
        percentText.textContent = progress + '%';
    }, 200);

    // 发送请求到后端
    fetch('/api/excel/upload', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            clearInterval(progressInterval);
            progressBar.style.width = '100%';
            percentText.textContent = '100%';

            // 显示结果
            setTimeout(() => {
                document.querySelector('.upload-progress').style.display = 'none';

                if (data.success) {
                    resultDiv.innerHTML = `
                    <div style="background-color: #d1fae5; padding: 1rem; border-radius: 8px; border-left: 4px solid #10b981;">
                        <div style="display: flex; align-items: center; gap: 0.75rem; color: #065f46;">
                            <i class="fas fa-check-circle" style="font-size: 1.25rem;"></i>
                            <h4 style="margin: 0;">上传成功！</h4>
                        </div>
                        <p style="margin: 0.75rem 0 0 0; color: #047857;">
                            <strong>${data.fileName}</strong> 已成功处理<br>
                            共解析了 <strong>${data.recordsProcessed}</strong> 条健康指标记录
                        </p>
                        <button onclick="refreshAnalysis()" style="margin-top: 1rem; background-color: #10b981; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer;">
                            <i class="fas fa-sync-alt"></i> 刷新指标分析
                        </button>
                    </div>
                `;
                } else {
                    resultDiv.innerHTML = `
                    <div style="background-color: #fee2e2; padding: 1rem; border-radius: 8px; border-left: 4px solid #ef4444;">
                        <div style="display: flex; align-items: center; gap: 0.75rem; color: #991b1b;">
                            <i class="fas fa-exclamation-circle" style="font-size: 1.25rem;"></i>
                            <h4 style="margin: 0;">上传失败</h4>
                        </div>
                        <p style="margin: 0.75rem 0 0 0; color: #b91c1c;">
                            ${data.message}
                        </p>
                    </div>
                `;
                }

                resultDiv.style.display = 'block';

                // 清空文件输入
                document.getElementById('excelFileInput').value = '';

            }, 500);
        })
        .catch(error => {
            console.error('上传错误:', error);
            resultDiv.innerHTML = `
            <div style="background-color: #fee2e2; padding: 1rem; border-radius: 8px; border-left: 4px solid #ef4444;">
                <p style="margin: 0; color: #b91c1c;">
                    网络错误：${error.message}
                </p>
            </div>
        `;
            resultDiv.style.display = 'block';
            document.querySelector('.upload-progress').style.display = 'none';
        });
}

/**
 * 下载Excel模板
 */
function downloadTemplate() {
    // 这里可以链接到实际的模板文件
    alert('模板下载功能待实现。请创建一个包含指定列的Excel文件。');

    // 实际实现示例：
    // window.location.href = '/api/excel/download-template';
}

/**
 * 刷新指标分析（重新加载数据）
 */
function refreshAnalysis() {
    document.querySelectorAll('.report-table tbody').forEach(tbody => {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-row">
                    <i class="fas fa-spinner fa-spin"></i> 正在刷新报告数据...
                </td>
            </tr>
        `;
    });
    loadUserReports();
}

/**
 * 获取当前用户ID（需要您根据实际系统实现）
 */
async function resolveUserId() {
    try {
        const saved = localStorage.getItem('aiHealthUser');
        if (saved) {
            const u = JSON.parse(saved);
            if (u) {
                if (u.user_id) return u.user_id;
                if (u.userId) return u.userId;
            }
        }
    } catch (e) {
        console.warn('解析本地用户信息失败', e);
    }
    try {
        const r = await fetch('/api/auth/me', { credentials: 'include' });
        const data = await r.json();
        if (data && data.success && data.user && (data.user.user_id || data.user.userId)) {
            return data.user.user_id || data.user.userId;
        }
    } catch (e) {
        console.warn('获取会话用户信息失败', e);
    }
    return null;
}

/**
 * 添加拖拽上传支持
 */
document.addEventListener('DOMContentLoaded', function() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('excelFileInput');

    // 阻止默认拖拽行为
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    // 添加拖拽高亮效果
    ['dragenter', 'dragover'].forEach(eventName => {
        uploadArea.addEventListener(eventName, highlight, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, unhighlight, false);
    });

    function highlight() {
        uploadArea.style.borderColor = '#3b82f6';
        uploadArea.style.backgroundColor = '#f0f7ff';
    }

    function unhighlight() {
        uploadArea.style.borderColor = '#93c5fd';
        uploadArea.style.backgroundColor = 'white';
    }

    // 处理文件放置
    uploadArea.addEventListener('drop', function(e) {
        const dt = e.dataTransfer;
        const files = dt.files;

        if (files.length > 0) {
            fileInput.files = files;
            handleFileSelect(fileInput);
        }
    }, false);
});

window.makeAppointment = makeAppointment;
// ==================== 用户端报告查询功能 ====================

// 模拟用户报告数据
const mockUserReports = {
    blood: [
        { cardNo: 'B001', itemName: '白细胞计数(WBC)', result: '6.2×10³/L', trend: '', reference: '3.5-9.5×10³/L', status: 'normal' },
        { cardNo: 'B002', itemName: '血红蛋白(HGB)', result: '135g/L', trend: '', reference: '120-160g/L', status: 'normal' },
        { cardNo: 'B003', itemName: '血小板计数(PLT)', result: '85×10³/L', trend: '↓', reference: '100-300×10³/L', status: 'alert' },
        { cardNo: 'B004', itemName: '空腹血糖(GLU)', result: '6.8mmol/L', trend: '↑', reference: '3.9-6.1mmol/L', status: 'warning' },
        { cardNo: 'B005', itemName: '总胆固醇(TC)', result: '5.2mmol/L', trend: '', reference: '2.8-5.7mmol/L', status: 'normal' },
        { cardNo: 'B006', itemName: '甘油三酯(TG)', result: '1.8mmol/L', trend: '↑', reference: '0.56-1.7mmol/L', status: 'warning' }
    ],
    urine: [
        { cardNo: 'U001', itemName: '尿蛋白(PRO)', result: '阴性', trend: '', reference: '阴性', status: 'normal' },
        { cardNo: 'U002', itemName: '尿糖(GLU)', result: '弱阳性', trend: '↑', reference: '阴性', status: 'warning' },
        { cardNo: 'U003', itemName: '尿潜血(BLD)', result: '1+', trend: '↑', reference: '阴性', status: 'warning' },
        { cardNo: 'U004', itemName: '尿比重(SG)', result: '1.020', trend: '', reference: '1.003-1.030', status: 'normal' },
        { cardNo: 'U005', itemName: '尿白细胞(LEU)', result: '阴性', trend: '', reference: '阴性', status: 'normal' }
    ],
    kidney: [
        { cardNo: 'K001', itemName: '肌酐(CREA)', result: '85μmol/L', trend: '', reference: '59-104μmol/L', status: 'normal' },
        { cardNo: 'K002', itemName: '尿素氮(BUN)', result: '5.2mmol/L', trend: '', reference: '2.9-8.2mmol/L', status: 'normal' },
        { cardNo: 'K003', itemName: '尿酸(UA)', result: '480μmol/L', trend: '↑', reference: '208-428μmol/L', status: 'warning' },
        { cardNo: 'K004', itemName: 'β2-微球蛋白', result: '2.1mg/L', trend: '', reference: '0.8-2.4mg/L', status: 'normal' }
    ],
    liver: [
        { cardNo: 'L001', itemName: '谷丙转氨酶(ALT)', result: '28U/L', trend: '', reference: '0-40U/L', status: 'normal' },
        { cardNo: 'L002', itemName: '谷草转氨酶(AST)', result: '25U/L', trend: '', reference: '0-40U/L', status: 'normal' },
        { cardNo: 'L003', itemName: '总胆红素(TBIL)', result: '15μmol/L', trend: '', reference: '5-21μmol/L', status: 'normal' },
        { cardNo: 'L004', itemName: '直接胆红素(DBIL)', result: '7.3μmol/L', trend: '', reference: '0-6.8μmol/L', status: 'warning' },
        { cardNo: 'L005', itemName: '间接胆红素(IBIL)', result: '7.7μmol/L', trend: '', reference: '3.4-12μmol/L', status: 'normal' },
        { cardNo: 'L006', itemName: '碱性磷酸酶(ALP)', result: '51U/L', trend: '', reference: '45-125U/L', status: 'normal' },
        { cardNo: 'L007', itemName: 'γ-谷氨酰转肽酶(GGT)', result: '6U/L', trend: '↓', reference: '8-61U/L', status: 'warning' },
        { cardNo: 'L008', itemName: '白蛋白/球蛋白(A/G)', result: '1.54', trend: '↓', reference: '1.5-2.5', status: 'warning' }
    ]
};

// 用户个人信息
const userPersonalInfo = {
    name: '张先生',
    gender: '男',
    age: 35,
    userId: 'USER123456',
    medicalCardNumber: 'MC20250012345',
    lastExamDate: '2025-12-15'
};

// 初始化报告查询功能
function initReportsSection() {
    console.log('初始化报告查询功能');

    // 绑定报告类型切换事件
    initReportTypeTabs();

    loadUserReports();
}

// 绑定报告类型切换事件
function initReportTypeTabs() {
    const tabs = document.querySelectorAll('.report-type-tab');

    tabs.forEach(tab => {
        tab.addEventListener('click', function(e) {
            e.preventDefault();
            const type = this.getAttribute('onclick').match(/'(\w+)'/)[1];
            showReportType(type);
        });
    });
}

// 显示报告查询页面（通过锚点跳转）
// 注意：这里我们不需要隐藏其他section，因为使用锚点导航

// 加载用户报告数据
function loadUserReports() {
    console.log('加载用户报告数据');
    fetch(REPORTS_API, { method: 'GET', credentials: 'include' })
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                showMessage(data.message || '未登录，请先登录', 'error');
                return;
            }
            aggregatedReports = data;
            updateUserInfo();
            loadBloodReport();
            loadUrineReport();
            loadKidneyReport();
            loadLiverReport();
            loadDoctorSuggestion();
            loadHealthPrediction();
        })
        .catch(err => {
            console.error('加载报告失败', err);
            showMessage('加载报告失败', 'error');
        });
}

// 加载医生建议
function loadDoctorSuggestion() {
    const suggestionElement = document.getElementById('doctorSuggestionText');
    if (!suggestionElement) return;

    if (aggregatedReports && aggregatedReports.doctorSuggestion) {
        const formattedSuggestion = formatSuggestionText(aggregatedReports.doctorSuggestion);
        suggestionElement.innerHTML = formattedSuggestion;
    } else {
        suggestionElement.innerHTML = '<div class="no-suggestion">暂无医生建议，请耐心等待医生审核您的报告。</div>';
    }
}

// 格式化医生建议文本
function formatSuggestionText(text) {
    // 将换行符转换为HTML换行，加粗关键词
    let formatted = text
        .replace(/\n\n/g, '<br><br>')
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/•/g, '•')
        .replace(/- /g, '• ');

    // 添加段落样式
    formatted = formatted.split('<br><br>').map(paragraph => {
        if (paragraph.includes('**重点关注：**') || paragraph.includes('**具体建议：**') || paragraph.includes('**复查安排：**')) {
            return `<div class="suggestion-section"><strong>${paragraph}</strong></div>`;
        }
        return `<div class="suggestion-paragraph">${paragraph}</div>`;
    }).join('<br>');

    return formatted;
}

// 更新用户个人信息
function updateUserInfo() {
    if (!aggregatedReports) return;
    const b = aggregatedReports.basicInfo || {};
    document.getElementById('reportUserName').textContent = b.name || '--';
    document.getElementById('reportUserGender').textContent = b.gender || '--';
    document.getElementById('reportUserAge').textContent = b.age != null ? `${b.age}岁` : '--';
    document.getElementById('reportUserId').textContent = b.userId || '--';
    document.getElementById('medicalCardNumber').textContent = b.medicalCardNumber || '--';
    document.getElementById('lastExamDate').textContent = aggregatedReports.lastExamDate || '--';
}

// 加载血常规报告
function loadBloodReport() {
    const container = document.getElementById('bloodReport');
    renderAnalysisSummary('blood', container);
    const tbody = document.getElementById('bloodReportBody');
    if (!tbody) return;

    tbody.innerHTML = '';
    const items = aggregatedReports && aggregatedReports.reports && aggregatedReports.reports.blood ? aggregatedReports.reports.blood : [];
    if (!items || items.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-row">目前没有记录哦</td>
            </tr>
        `;
        return;
    }
    items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.cardNo || ''}</td>
            <td>${item.itemName || ''}</td>
            <td><div class="result-value">${item.result || ''}</div></td>
            <td>${item.reference || ''}</td>
            <td><span class="status-badge ${item.status || 'normal'}">${getStatusText(item.status || 'normal')}</span></td>
        `;
        tbody.appendChild(row);
    });
}

// 加载尿常规报告
function loadUrineReport() {
    const container = document.getElementById('urineReport');
    renderAnalysisSummary('urine', container);
    const tbody = document.getElementById('urineReportBody');
    if (!tbody) return;

    tbody.innerHTML = '';
    const items = aggregatedReports && aggregatedReports.reports && aggregatedReports.reports.urine ? aggregatedReports.reports.urine : [];
    if (!items || items.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-row">目前没有记录哦</td>
            </tr>
        `;
        return;
    }
    items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.cardNo || ''}</td>
            <td>${item.itemName || ''}</td>
            <td><div class="result-value">${item.result || ''}</div></td>
            <td>${item.reference || ''}</td>
            <td><span class="status-badge ${item.status || 'normal'}">${getStatusText(item.status || 'normal')}</span></td>
        `;
        tbody.appendChild(row);
    });
}

// 加载肾功能报告
function loadKidneyReport() {
    const container = document.getElementById('kidneyReport');
    renderAnalysisSummary('kidney', container);
    const tbody = document.getElementById('kidneyReportBody');
    if (!tbody) return;

    tbody.innerHTML = '';
    const items = aggregatedReports && aggregatedReports.reports && aggregatedReports.reports.kidney ? aggregatedReports.reports.kidney : [];
    if (!items || items.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-row">目前没有记录哦</td>
            </tr>
        `;
        return;
    }
    items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.cardNo || ''}</td>
            <td>${item.itemName || ''}</td>
            <td><div class="result-value">${item.result || ''}</div></td>
            <td>${item.reference || ''}</td>
            <td><span class="status-badge ${item.status || 'normal'}">${getStatusText(item.status || 'normal')}</span></td>
        `;
        tbody.appendChild(row);
    });
}

// 加载肝功能报告
function loadLiverReport() {
    const container = document.getElementById('liverReport');
    renderAnalysisSummary('liver', container);
    const tbody = document.getElementById('liverReportBody');
    if (!tbody) return;

    tbody.innerHTML = '';
    const items = aggregatedReports && aggregatedReports.reports && aggregatedReports.reports.liver ? aggregatedReports.reports.liver : [];
    if (!items || items.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-row">目前没有记录哦</td>
            </tr>
        `;
        return;
    }
    items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.cardNo || ''}</td>
            <td>${item.itemName || ''}</td>
            <td><div class="result-value">${item.result || ''}</div></td>
            <td>${item.reference || ''}</td>
            <td><span class="status-badge ${item.status || 'normal'}">${getStatusText(item.status || 'normal')}</span></td>
        `;
        tbody.appendChild(row);
    });
}

// 显示特定类型的报告
function showReportType(type) {
    console.log('显示报告类型:', type);

    // 更新标签页状态
    document.querySelectorAll('.report-type-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    document.querySelectorAll('.report-type-content').forEach(content => {
        content.classList.remove('active');
    });

    const activeTab = document.querySelector(`.report-type-tab[onclick*="${type}"]`);
    const activeContent = document.getElementById(`${type}Report`);

    if (activeTab) activeTab.classList.add('active');
    if (activeContent) activeContent.classList.add('active');
}

// 刷新报告数据
function refreshReports() {
    console.log('刷新报告数据');
    document.querySelectorAll('.report-table tbody').forEach(tbody => {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-row">
                    <i class="fas fa-spinner fa-spin"></i> 正在刷新报告数据...
                </td>
            </tr>
        `;
    });
    loadUserReports();
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

// 在DOM加载完成后初始化报告功能
document.addEventListener('DOMContentLoaded', function() {
    // 初始化报告功能
    initReportsSection();
});

// 将函数暴露到全局
window.showReportType = showReportType;
window.refreshReports = refreshReports;

function renderAnalysisSummary(type, container) {
    if (!container || !aggregatedReports) return;
    const analysis = aggregatedReports.analysis && aggregatedReports.analysis[type] ? aggregatedReports.analysis[type] : null;
    const existing = container.querySelector('.analysis-summary');
    if (existing) existing.remove();
    const summary = document.createElement('div');
    summary.className = 'analysis-summary';
    if (!analysis) {
        summary.innerHTML = `
            <div class="summary-box empty">
                <i class="fas fa-info-circle"></i>
                <span>暂无该类别的AI分析结果</span>
            </div>
        `;
        container.insertBefore(summary, container.firstChild);
        return;
    }
    const riskText = analysis.riskLevel || '未知';
    const prob = (analysis.abnormalProbability ?? analysis.riskProbability);
    const probText = prob != null ? `${Number(prob).toFixed(1)}%` : '--';
    const confText = analysis.modelConfidence != null ? `${Number(analysis.modelConfidence).toFixed(1)}%` : '--';
    const recommendation = analysis.recommendation || '—';
    const indicators = (analysis.abnormalIndicators || analysis.keyIndicators || []);
    const indicatorList = Array.isArray(indicators) && indicators.length
        ? indicators.map(i => {
            const name = i.name || i.indicator || '';
            const value = i.value || i.level || '';
            return `<span class="indicator-chip">${name}${value ? `：${value}` : ''}</span>`;
        }).join(' ')
        : '<span class="indicator-chip">无明显异常指标</span>';
    summary.innerHTML = `
        <div class="summary-box">
            <div class="summary-header">
                <span class="summary-title"><i class="fas fa-brain"></i> AI分析结果</span>
                <span class="summary-risk ${String(riskText).includes('高') ? 'risk-high' : String(riskText).includes('中') ? 'risk-medium' : 'risk-low'}">${riskText}</span>
            </div>
            <div class="summary-content">
                <div class="summary-metrics">
                    <div class="metric"><label>风险概率</label><span>${probText}</span></div>
                    <div class="metric"><label>模型置信度</label><span>${confText}</span></div>
                    <div class="metric"><label>预测结论</label><span>${analysis.predictionResult || '—'}</span></div>
                </div>
                <div class="summary-indicators">
                    <label>关键指标</label>
                    <div class="chips">${indicatorList}</div>
                </div>
                <div class="summary-recommendation">
                    <label>建议</label>
                    <div class="text">${recommendation}</div>
                </div>
            </div>
        </div>
    `;
    container.insertBefore(summary, container.firstChild);
}

// 加载健康预测数据
function loadHealthPrediction() {
    const container = document.getElementById('predictionCardsContainer');
    if (!container) return;

    if (!aggregatedReports || !aggregatedReports.analysis || Object.keys(aggregatedReports.analysis).length === 0) {
        container.innerHTML = `
            <div class="no-prediction" style="text-align: center; width: 100%; padding: 2rem; color: #94a3b8;">
                <i class="fas fa-clipboard-check" style="font-size: 48px; margin-bottom: 1rem; color: #cbd5e1;"></i>
                <p>暂无健康预测数据，请上传体检报告后查看</p>
            </div>
        `;
        return;
    }

    container.innerHTML = '';
    const analysis = aggregatedReports.analysis;
    
    // 定义配置
    const config = {
        blood: { title: '血液健康', icon: 'fa-tint', color: '#ef4444' },
        urine: { title: '尿液分析', icon: 'fa-flask', color: '#eab308' },
        liver: { title: '肝脏功能', icon: 'fa-leaf', color: '#f97316' },
        kidney: { title: '肾脏功能', icon: 'fa-filter', color: '#3b82f6' }
    };

    // 遍历并生成卡片
    Object.keys(config).forEach(key => {
        const item = analysis[key];
        if (!item) return;

        const conf = config[key];
        const riskLevel = item.riskLevel || '未知';
        
        // 确定状态样式
        let statusClass = 'low'; // 默认为绿色/低风险
        if (riskLevel.includes('高') || riskLevel.includes('异常')) {
            statusClass = 'high';
        } else if (riskLevel.includes('中') || riskLevel.includes('注意')) {
            statusClass = 'medium';
        }

        const card = document.createElement('div');
        card.className = 'prediction-card';
        card.innerHTML = `
            <div class="prediction-icon" style="background: ${conf.color};">
              <i class="fas ${conf.icon}"></i>
            </div>
            <h3>${conf.title}</h3>
            <div class="prediction-value ${statusClass}">${riskLevel}</div>
            <p>${item.recommendation || '暂无建议'}</p>
            <div style="margin-top: 10px; font-size: 0.85em; color: #64748b;">
                <i class="fas fa-brain"></i> AI置信度: ${item.modelConfidence ? Number(item.modelConfidence).toFixed(1) + '%' : '--'}
            </div>
        `;
        container.appendChild(card);
    });
    
    // 如果虽然有analysis对象但没有具体内容
    if (container.children.length === 0) {
         container.innerHTML = `
            <div class="no-prediction" style="text-align: center; width: 100%; padding: 2rem; color: #94a3b8;">
                <i class="fas fa-clipboard-check" style="font-size: 48px; margin-bottom: 1rem; color: #cbd5e1;"></i>
                <p>暂无有效的AI预测结果</p>
            </div>
        `;
    }
}
