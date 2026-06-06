#!/bin/bash

echo "=========================================="
echo "RSS 阅读器项目 - 前端启动脚本"
echo "=========================================="
echo ""

# 检查 node 是否安装
echo "[1/4] 检查 Node.js 环境..."
sleep 1
if ! command -v node &> /dev/null; then
    echo "错误: 未找到 Node.js。请先安装 Node.js。"
    exit 1
fi
echo "Node.js 已安装: $(node --version)"
echo ""

echo "[2/4] 进入前端目录..."
sleep 1
cd frontend
echo "目录: $(pwd)"
echo ""

# 检查依赖是否安装
echo "[3/4] 检查依赖..."
sleep 1
if [ ! -d "node_modules" ]; then
    echo "正在安装依赖（这可能需要几分钟）..."
    npm install
    echo "依赖安装完成"
else
    echo "依赖已安装"
fi
echo ""

echo "[4/4] 启动 Vue 前端服务..."
echo ""
echo "=========================================="
echo "前端服务启动中..."
echo "地址: http://localhost:5173"
echo "按 Ctrl+C 停止服务"
echo "=========================================="
echo ""
sleep 2

npm run dev
