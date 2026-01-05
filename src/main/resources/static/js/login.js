async function handleUserLogin() {
    try {
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        const loginBtn = document.querySelector('.user-btn');

        if (!usernameInput || !passwordInput || !loginBtn) {
            alert('登录表单未正确加载');
            return;
        }

        const username = usernameInput.value.trim();
        const password = passwordInput.value;

        if (!username) {
            alert('请输入用户名/手机号！');
            usernameInput.focus();
            return;
        }

        if (!password) {
            alert('请输入密码！');
            passwordInput.focus();
            return;
        }

        // 显示加载状态
        const originalText = loginBtn.innerHTML;
        loginBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 登录中...';
        loginBtn.disabled = true;

        // 修复：患者登录应该调用 /api/auth/user/login
        console.log('患者登录，调用API:', `${AUTH_API_BASE}/user/login`);
        const response = await fetch(`${AUTH_API_BASE}/user/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                account: username,
                password: password
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP错误: ${response.status}`);
        }

        const data = await response.json();
        console.log('患者登录响应:', data);

        if (data.success) {
            // 登录成功
            currentUser = {
                ...data.user,
                userType: data.userType || 'patient',
                greeting: data.greeting || data.user.real_name
            };

            // 保存登录状态
            localStorage.setItem('aiHealthUser', JSON.stringify(currentUser));

            // 显示成功消息
            showMessage('登录成功！欢迎回来，' + (data.greeting || data.user.real_name), 'success');

            // 重定向到用户页面
            setTimeout(() => {
                window.location.href = '/user';
            }, 1000);
        } else {
            showMessage('登录失败：' + data.message, 'error');
        }

    } catch (error) {
        console.error('患者登录错误:', error);
        showMessage('网络错误：' + error.message, 'error');
    } finally {
        // 恢复按钮状态
        const loginBtn = document.querySelector('.user-btn');
        if (loginBtn) {
            loginBtn.innerHTML = '<i class="fas fa-sign-in-alt"></i> 用户登录';
            loginBtn.disabled = false;
        }
    }
}



async function handleDoctorLogin() {
    try {
        const doctorId = document.getElementById('doctorId').value.trim();
        const password = document.getElementById('doctorPassword').value;
        const loginBtn = document.querySelector('.doctor-btn');

        if (!doctorId || !password) {
            alert('请输入工号和密码！');
            return;
        }

        // 显示加载状态
        const originalText = loginBtn.innerHTML;
        loginBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 登录中...';
        loginBtn.disabled = true;

        // 医生登录：调用 /api/auth/doctor/login
        console.log('医生登录，调用API:', `${AUTH_API_BASE}/doctor/login`);
        const response = await fetch(`${AUTH_API_BASE}/doctor/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                account: doctorId,
                password: password
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP错误: ${response.status}`);
        }

        const data = await response.json();
        console.log('医生登录响应:', data);

        if (data.success) {
            // 登录成功
            currentUser = {
                ...data.user,
                userType: 'doctor',
                greeting: data.greeting || data.user.real_name
            };

            // 保存登录状态
            localStorage.setItem('aiHealthUser', JSON.stringify(currentUser));

            // 显示成功消息
            showMessage('医生登录成功！欢迎回来，' + (data.greeting || data.user.real_name), 'success');

            // 重定向到医生页面
            setTimeout(() => {
                window.location.href = '/doctor';
            }, 1000);
        } else {
            showMessage('登录失败：' + data.message, 'error');
        }

    } catch (error) {
        console.error('医生登录错误:', error);
        showMessage('网络错误：' + error.message, 'error');
    } finally {
        // 恢复按钮状态
        const loginBtn = document.querySelector('.doctor-btn');
        if (loginBtn) {
            loginBtn.innerHTML = '<i class="fas fa-stethoscope"></i> 医生登录';
            loginBtn.disabled = false;
        }
    }
}