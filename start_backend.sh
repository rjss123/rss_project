#!/bin/bash

echo "=========================================="
echo "RSS 阅读器项目 - 后端启动脚本"
echo "=========================================="
echo ""

# 检查 conda 是否安装
echo "[1/4] 检查 Conda 环境..."
sleep 1
if ! command -v conda &> /dev/null; then
    echo "错误: 未找到 conda。请先安装 Anaconda 或 Miniconda。"
    exit 1
fi
echo " Conda 已安装"
echo ""

# 检查环境是否存在
echo "[2/4] 检查虚拟环境..."
sleep 1
if conda env list | grep -q "rss_project"; then
    echo "Conda 环境已存在"
else
    echo "→ 创建 Conda 环境（这可能需要几分钟）..."
    sleep 2
    conda env create -f environment.yml
    echo "环境创建完成"
fi
echo ""

echo "[3/4] 进入后端目录..."
sleep 1
cd backend
echo " 目录: $(pwd)"
echo ""

echo "[4/4] 启动 FastAPI 后端服务..."
echo ""
echo "=========================================="
echo "后端服务启动中..."
echo "地址: http://localhost:8000"
echo "API 文档: http://localhost:8000/docs"
echo "按 Ctrl+C 停止服务"
echo "=========================================="
echo ""
sleep 2

# 激活环境并启动（使用 conda run）
conda run -n rss_project python main.py
