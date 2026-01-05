// ==================== 全局配置 ====================
const USER_API_BASE = 'http://localhost:8080/api/users';    // 患者登录API
const AUTH_API_BASE = 'http://localhost:8080/api/auth';     // 医生登录API

let currentUser = null;

// ==================== 消息提示功能 ====================
function showMessage(message, type = 'info') {
    // 移除现有的消息提示
    const existingMessage = document.querySelector('.message-toast');
    if (existingMessage) {
        existingMessage.remove();
    }

    // 创建新的消息提示
    const toast = document.createElement('div');
    toast.className = `message-toast ${type}`;

    let icon = '';
    switch(type) {
        case 'success':
            icon = 'check-circle';
            break;
        case 'error':
            icon = 'exclamation-circle';
            break;
        default:
            icon = 'info-circle';
    }

    toast.innerHTML = `
        <i class="fas fa-${icon}"></i>
        <span>${message}</span>
    `;

    document.body.appendChild(toast);

    // 显示动画
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // 3秒后消失
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

// ==================== 标签页切换 ====================
function initTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const loginForms = document.querySelectorAll('.login-form');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tab = this.getAttribute('data-tab');

            // 更新按钮状态
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // 显示对应表单
            loginForms.forEach(form => {
                form.classList.remove('active');
                if (form.id === tab + 'Form') {
                    form.classList.add('active');
                }
            });
        });
    });
}

// ==================== 页面切换 ====================
function checkSavedLogin() {
    const savedUser = localStorage.getItem('aiHealthUser');

    if (savedUser) {
        try {
            const user = JSON.parse(savedUser);
            currentUser = user;

            if (user.userType === 'patient') {
                window.location.href = '/user'; // 重定向到用户页面
            } else if (user.userType === 'doctor') {
                window.location.href = '/doctor'; // 重定向到医生页面
            }
        } catch (e) {
            console.error('解析保存的用户数据失败:', e);
            localStorage.removeItem('aiHealthUser');
        }
    }
}

function logout() {
    if (confirm('确定要退出登录吗？')) {
        // 根据当前用户类型调用不同的登出API
        if (currentUser && currentUser.userType === 'doctor') {
            fetch(`${AUTH_API_BASE}/logout`)
                .catch(error => console.error('登出API调用失败:', error));
        } else {
            // 患者可能没有专门的logout接口，可以跳过
        }

        currentUser = null;
        localStorage.removeItem('aiHealthUser');

        // 回到登录页面
        window.location.href = '/login';

        showMessage('已成功登出', 'info');
    }
}

// 确保函数在全局可用
window.logout = logout;

// ==================== 初始化登录页面 ====================
if (document.getElementById('loginModal')) {
    document.addEventListener('DOMContentLoaded', function() {
        console.log('登录页面加载完成');
        initTabs();
        checkSavedLogin();

        // 绑定登录表单事件
        const userForm = document.getElementById('userForm');
        const doctorForm = document.getElementById('doctorForm');

        if (userForm) {
            userForm.addEventListener('submit', function(e) {
                e.preventDefault();
                handleUserLogin();
            });
        }

        if (doctorForm) {
            doctorForm.addEventListener('submit', function(e) {
                e.preventDefault();
                handleDoctorLogin();
            });
        }
    });
}