#!/bin/bash
set -e

echo "🚀 开始设置 AI 健康检测系统开发环境..."

# 1. 更新系统包
sudo apt-get update

# 2. 安装 MySQL（用于本地测试）
echo "安装 MySQL 客户端和服务端..."
sudo apt-get install -y mysql-server mysql-client
sudo service mysql start

# 3. 创建数据库（匹配你的配置）
echo "设置 MySQL 数据库..."
sudo mysql -e "
CREATE DATABASE IF NOT EXISTS ai_health_check
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'hanwu'@'localhost' IDENTIFIED BY 'WDz18708835370';
GRANT ALL PRIVILEGES ON ai_health_check.* TO 'hanwu'@'localhost';
FLUSH PRIVILEGES;

-- 创建测试表（如果需要）
USE ai_health_check;
CREATE TABLE IF NOT EXISTS dev_setup_check (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO dev_setup_check (message) VALUES ('开发环境初始化完成');
"

# 4. 安装 Python 依赖（使用清华镜像加速）
echo "安装 Python 依赖..."
cd /workspace

# 检查 requirements.txt 是否存在
if [ -f "requirements.txt" ]; then
    echo "使用清华镜像安装 Python 包..."
    pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
    pip install --upgrade pip

    # 分批安装，避免内存溢出
    pip install numpy pandas scipy  # 基础科学计算
    pip install scikit-learn matplotlib seaborn  # 机器学习
    pip install tensorflow keras torch  # 深度学习框架
    pip install flask flask-login flask-sqlalchemy  # Web 框架

    # 安装剩余的包
    pip install -r requirements.txt

    echo "✅ Python 依赖安装完成"
else
    echo "⚠️  未找到 requirements.txt，跳过 Python 依赖安装"
fi

# 5. 检查并构建 Gradle 项目
echo "检查 Gradle 项目..."
if [ -f "gradlew" ]; then
    echo "发现 Gradle 项目，进行初始化构建..."
    chmod +x gradlew

    # 下载 Gradle 包装器需要的文件
    ./gradlew wrapper --gradle-version=8.5

    # 清理并编译（跳过测试加速）
    ./gradlew clean compileJava --no-daemon

    echo "✅ Gradle 项目初始化完成"
else
    echo "⚠️  未找到 Gradle 包装器，请确保 gradlew 文件存在"
fi

# 6. 创建本地配置文件（如果需要）
if [ ! -f "src/main/resources/application-dev.properties" ]; then
    echo "创建开发环境配置文件..."
    cat > src/main/resources/application-dev.properties << EOF
# 开发环境配置
spring.datasource.url=\${MYSQL_URL}
spring.datasource.username=\${MYSQL_USERNAME}
spring.datasource.password=\${MYSQL_PASSWORD}

# 开发时使用 create-drop，方便测试
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# 开发时启用热部署
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true

# 更详细的日志
logging.level.com.example.aihealthcheck=DEBUG
EOF
    echo "✅ 开发配置文件创建完成"
fi

# 7. 设置 Python 服务目录
echo "设置 Python 服务..."
PYTHON_SRC_DIR="src/main/python"
if [ ! -d "$PYTHON_SRC_DIR" ]; then
    mkdir -p "$PYTHON_SRC_DIR"
    echo "创建了 Python 源码目录: $PYTHON_SRC_DIR"
fi

# 8. 创建启动脚本
echo "创建便捷启动脚本..."
cat > start-dev.sh << 'EOF'
#!/bin/bash
echo "启动 AI 健康检测系统..."

# 启动 MySQL（如果未运行）
sudo service mysql start

# 检查数据库连接
echo "检查数据库连接..."
mysql -u hanwu -pWDz18708835370 -e "USE ai_health_check; SELECT '数据库连接正常' as status;" 2>/dev/null || {
    echo "❌ 数据库连接失败，尝试重新创建用户..."
    sudo mysql -e "CREATE USER IF NOT EXISTS 'hanwu'@'localhost' IDENTIFIED BY 'WDz18708835370'; GRANT ALL ON ai_health_check.* TO 'hanwu'@'localhost';"
}

# 启动 Spring Boot 应用
echo "启动 Spring Boot 应用..."
./gradlew bootRun --args='--spring.profiles.active=dev'

# 如果上面失败，尝试直接运行
if [ $? -ne 0 ]; then
    echo "尝试直接运行..."
    java -jar build/libs/*.jar --spring.profiles.active=dev
fi
EOF

chmod +x start-dev.sh

echo "========================================"
echo "🎉 开发环境设置完成！"
echo ""
echo "下一步操作："
echo "1. 运行项目: ./start-dev.sh"
echo "2. 或使用: ./gradlew bootRun"
echo "3. 访问: http://localhost:8080"
echo ""
echo "已安装的工具："
echo "✓ Java $(java -version 2>&1 | head -1)"
echo "✓ Python $(python3 --version)"
echo "✓ MySQL $(mysql --version)"
echo "✓ TensorFlow $(python3 -c "import tensorflow as tf; print(f'TensorFlow {tf.__version__}')" 2>/dev/null || echo "需要手动导入")"
echo "========================================"