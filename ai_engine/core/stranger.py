"""
陌生人检测模块
功能：人脸检测、人脸识别、陌生人判断、人脸注册
"""

import cv2
import numpy as np
import json
import os
from datetime import datetime


class StrangerDetector:
    def __init__(self, face_detector_path, db_file, threshold=0.85):
        self.threshold = threshold
        self.db_file = db_file

        # 加载人脸检测模型
        self.face_detector = None
        if os.path.exists(face_detector_path):
            self.face_detector = cv2.FaceDetectorYN.create(face_detector_path, "", (320, 320))
            print("✅ 人脸检测器加载成功")
        else:
            print(f"❌ 人脸检测器加载失败: {face_detector_path}")

        # 加载人脸数据库
        self.face_db = self._load_db()
        print(f"✅ 已加载 {len(self.face_db)} 个注册人脸")

    def _load_db(self):
        if os.path.exists(self.db_file):
            with open(self.db_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}

    def _save_db(self):
        with open(self.db_file, 'w', encoding='utf-8') as f:
            json.dump(self.face_db, f, ensure_ascii=False, indent=2)

    def _get_face_hash(self, face_img):
        """提取人脸特征（感知哈希）"""
        if face_img is None or face_img.size == 0:
            return None

        try:
            h, w = face_img.shape[:2]
            if h < 30 or w < 30:
                return None

            # 缩放
            face_resized = cv2.resize(face_img, (64, 64))

            # 转灰度
            if len(face_resized.shape) == 3:
                gray = cv2.cvtColor(face_resized, cv2.COLOR_BGR2GRAY)
            else:
                gray = face_resized

            # 计算哈希
            small = cv2.resize(gray, (8, 8))
            avg = np.mean(small)
            hash_bits = (small > avg).flatten()
            return ''.join(['1' if bit else '0' for bit in hash_bits])
        except:
            return None

    def _hash_similarity(self, h1, h2):
        """计算两个哈希的相似度（0-1）"""
        if not h1 or not h2:
            return 0
        same = sum(1 for i in range(len(h1)) if h1[i] == h2[i])
        return same / len(h1)

    def _detect_face_boxes(self, img):
        """检测图片中所有人脸框"""
        if self.face_detector is None:
            return []

        h, w = img.shape[:2]
        self.face_detector.setInputSize((w, h))
        faces = self.face_detector.detect(img)

        face_boxes = []
        if faces is not None and len(faces) > 1 and faces[1] is not None:
            for face in faces[1]:
                x1, y1 = int(face[0]), int(face[1])
                wf, hf = int(face[2]), int(face[3])
                if wf > 30 and hf > 30:
                    x2 = min(x1 + wf, w)
                    y2 = min(y1 + hf, h)
                    x1, y1 = max(0, x1), max(0, y1)
                    if x2 > x1 and y2 > y1:
                        face_boxes.append([x1, y1, x2, y2])

        return face_boxes

    def _recognize_single_face(self, face_img):
        """识别单张人脸，返回 (是否已知, 姓名, 相似度)"""
        if len(self.face_db) == 0:
            return False, None, 0

        feat = self._get_face_hash(face_img)
        if feat is None:
            return False, None, 0

        best_name = None
        best_score = 0

        for pid, data in self.face_db.items():
            stored = data.get("feature")
            if stored:
                sim = self._hash_similarity(feat, stored)
                if sim > best_score:
                    best_score = sim
                    best_name = data.get("name")

        if best_score >= self.threshold:
            return True, best_name, best_score
        return False, None, best_score

    def detect(self, img):
        """
        检测图片中的陌生人
        返回: (has_stranger, stranger_count, stranger_details)
        """
        if len(self.face_db) == 0:
            # 没有注册任何人，所有人都是陌生人
            face_boxes = self._detect_face_boxes(img)
            return True, len(face_boxes), []

        face_boxes = self._detect_face_boxes(img)
        strangers = []

        for (x1, y1, x2, y2) in face_boxes:
            face_img = img[y1:y2, x1:x2]
            is_known, name, score = self._recognize_single_face(face_img)

            if not is_known:
                strangers.append({
                    "box": [x1, y1, x2, y2],
                    "score": round(score, 3)
                })

        return len(strangers) > 0, len(strangers), strangers

    def register_face(self, img, name):
        """注册人脸"""
        if self.face_detector is None:
            return False, "人脸检测器未加载"

        # 检测人脸
        h, w = img.shape[:2]
        self.face_detector.setInputSize((w, h))
        faces = self.face_detector.detect(img)

        if faces is None or len(faces) < 2 or faces[1] is None:
            return False, "未检测到人脸，请使用清晰正面照片"

        # 取最大的人脸
        best_face = None
        max_size = 0
        for face in faces[1]:
            x1, y1, wf, hf = int(face[0]), int(face[1]), int(face[2]), int(face[3])
            if wf * hf > max_size:
                max_size = wf * hf
                best_face = face

        if best_face is None:
            return False, "未检测到有效人脸"

        x1, y1, wf, hf = int(best_face[0]), int(best_face[1]), int(best_face[2]), int(best_face[3])

        if wf < 50 or hf < 50:
            return False, "人脸太小，请使用更清晰的照片"

        # 裁剪人脸
        x1, y1 = max(0, x1), max(0, y1)
        x2, y2 = min(w, x1 + wf), min(h, y1 + hf)
        face_img = img[y1:y2, x1:x2]

        # 提取特征
        feat = self._get_face_hash(face_img)
        if feat is None:
            return False, "提取人脸特征失败"

        # 检查是否重名
        for pid, data in self.face_db.items():
            if data.get("name") == name:
                return False, f"姓名 {name} 已存在"

        # 生成新 ID
        existing_ids = [int(k[1:]) for k in self.face_db.keys() if k.startswith('p')]
        next_id = max(existing_ids) + 1 if existing_ids else 1
        face_id = f"p{next_id}"

        # 保存
        self.face_db[face_id] = {
            "name": name,
            "feature": feat,
            "time": datetime.now().isoformat()
        }
        self._save_db()

        return True, f"注册成功: {name}"

    def list_faces(self):
        """获取所有人脸列表"""
        return [{"id": k, "name": v["name"]} for k, v in self.face_db.items()]

    def delete_face(self, face_id):
        """删除人脸"""
        if face_id in self.face_db:
            del self.face_db[face_id]
            self._save_db()
            return True
        return False

    def get_face_count(self):
        return len(self.face_db)
