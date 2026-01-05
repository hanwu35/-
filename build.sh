#!/bin/bash

# 设置镜像名称和标签
IMAGE_NAME="ai-health-check"
VERSION="1.0.0"

echo "开始构建 Docker 镜像..."

# 使用国内镜像源构建
docker build \
    --build-arg http_proxy=http://mirrors.aliyun.com \
    --build-arg https_proxy=http://mirrors.aliyun.com \
    -t ${IMAGE_NAME}:${VERSION} \
    -t ${IMAGE_NAME}:latest \
    .

if [ $? -eq 0 ]; then
    echo "✅ 镜像构建成功: ${IMAGE_NAME}:${VERSION}"
    echo "运行以下命令启动容器:"
    echo "  docker-compose up -d"
    echo "或:"
    echo "  docker run -d --name ai-health-check -p 8080:8080 --env-file .env ${IMAGE_NAME}:latest"
else
    echo "❌ 镜像构建失败"
    exit 1
fi