#!/bin/bash
echo "血常规预测系统 - 依赖安装"
echo "================================"
echo ""

# 设置清华大学镜像源
echo "使用清华大学镜像源安装..."
python3 -m pip install --upgrade pip -i https://pypi.tuna.tsinghua.edu.cn/simple --trusted-host pypi.tuna.tsinghua.edu.cn

# 安装依赖
python3 -m pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple --trusted-host pypi.tuna.tsinghua.edu.cn

echo ""
echo "安装完成！"
