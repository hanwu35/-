# 阶段1：Gradle构建环境（编译SpringBoot项目）
FROM gradle:8.0-jdk17 AS builder
# 设定工作目录
WORKDIR /app
# 复制Gradle配置文件（优先复制，利用Docker缓存）
COPY build.gradle settings.gradle ./
# 复制项目源码
COPY src ./src
# 编译打包（生成可执行Jar包，--no-daemon关闭守护进程，加快构建）
RUN gradle clean bootJar --no-daemon

# 阶段2：运行环境（Java+Python）
FROM openjdk:17-jdk-slim
# 安装Python3和pip（满足项目Python功能依赖）
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*
# 设定工作目录
WORKDIR /app
# 从构建阶段复制打包好的Jar包
COPY --from=builder /app/build/libs/*.jar app.jar
# 复制Python脚本（根据你的项目结构，调整Python文件存放路径，此处假设Python脚本在src/main/python下）
COPY src/main/python ./python
# 复制Python依赖文件
COPY requirements.txt ./
# 安装Python依赖
RUN pip3 install --no-cache-dir -r requirements.txt
# 复制静态资源和模板（确保前后端一体项目的前端资源可访问，兜底配置）
COPY src/main/resources/static ./static
COPY src/main/resources/templates ./templates
# 暴露SpringBoot默认端口（可根据你的application.properties配置修改，默认8080）
EXPOSE 8080
# 启动命令（指定Jar包运行，允许外部访问MySQL容器）
ENTRYPOINT ["java", "-jar", "app.jar"]