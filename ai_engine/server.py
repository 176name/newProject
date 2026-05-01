"""
AI 智能监控服务
功能：陌生人检测 + 摔倒检测
运行：python server.py
"""

from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware  # 添加这行
import cv2
import numpy as np
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core.stranger import StrangerDetector
from core.fall import FallDetector

app = FastAPI(title="AI智能监控", description="陌生人检测 + 摔倒检测")

# ========== 添加 CORS 中间件（解决前端跨域问题）==========
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源，生产环境建议指定具体域名
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有 HTTP 方法
    allow_headers=["*"],  # 允许所有请求头
)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_DIR = os.path.join(BASE_DIR, "models")
DB_FILE = os.path.join(BASE_DIR, "data", "face_db.json")

os.makedirs(os.path.join(BASE_DIR, "data"), exist_ok=True)

print("\n" + "=" * 60)
print("启动 AI 智能监控服务")
print("=" * 60)

# 初始化陌生人检测模块
stranger_detector = StrangerDetector(
    face_detector_path=os.path.join(MODEL_DIR, "face_detection_yunet_2023mar.onnx"),
    db_file=DB_FILE,
    threshold=0.85
)

# 初始化摔倒检测模块
fall_detector = FallDetector(
    model_path=os.path.join(MODEL_DIR, "yolov8n-pose.pt")
)

print("=" * 60)
print(f"已注册人脸: {stranger_detector.get_face_count()}")
print("=" * 60)
print("API 地址: http://localhost:5001")
print("POST /detect      - 检测图片（返回陌生人+摔倒结果）")
print("POST /face/register - 注册人脸")
print("GET  /face/list    - 人脸列表")
print("DELETE /face/{id}  - 删除人脸")
print("=" * 60 + "\n")


@app.get("/health")
def health():
    return {
        "status": "ok",
        "registered_faces": stranger_detector.get_face_count(),
        "fall_detector_ready": fall_detector.is_ready()
    }


@app.post("/detect")
async def detect(file: UploadFile = File(...)):
    """
    检测图片
    返回:
    - has_stranger: 是否有陌生人
    - stranger_count: 陌生人数量
    - fallen: 是否摔倒
    - alert: 告警类型（正常/陌生人闯入/摔倒）
    """
    contents = await file.read()
    img = cv2.imdecode(np.frombuffer(contents, np.uint8), cv2.IMREAD_COLOR)

    if img is None:
        return JSONResponse(status_code=400, content={"code": 400, "msg": "图片解码失败"})

    # 1. 陌生人检测
    has_stranger, stranger_count, strangers = stranger_detector.detect(img)

    # 2. 摔倒检测
    fallen, fall_score = fall_detector.detect(img)

    # 3. 确定告警类型
    alert = "正常"
    if fallen:
        alert = "摔倒"
    elif has_stranger:
        alert = "陌生人闯入"

    return {
        "code": 200,
        "data": {
            "has_stranger": has_stranger,
            "stranger_count": stranger_count,
            "fallen": fallen,
            "fall_score": fall_score,
            "alert": alert
        }
    }


@app.post("/face/register")
async def register(name: str = Form(...), file: UploadFile = File(...)):
    """注册人脸"""
    contents = await file.read()
    img = cv2.imdecode(np.frombuffer(contents, np.uint8), cv2.IMREAD_COLOR)

    if img is None:
        return {"code": 400, "msg": "图片解码失败"}

    success, msg = stranger_detector.register_face(img, name)
    return {"code": 200 if success else 400, "msg": msg}


@app.get("/face/list")
def list_faces():
    """获取所有人脸列表"""
    faces = stranger_detector.list_faces()
    return {"code": 200, "data": faces}


@app.delete("/face/{face_id}")
def delete_face(face_id: str):
    """删除人脸"""
    success = stranger_detector.delete_face(face_id)
    return {"code": 200 if success else 404, "msg": "删除成功" if success else "人脸不存在"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5001)
