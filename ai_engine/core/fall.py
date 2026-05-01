"""
摔倒检测模块
功能：人体姿态估计、摔倒判断
"""

from ultralytics import YOLO


class FallDetector:
    def __init__(self, model_path):
        self.model = None
        self.ready = False

        try:
            self.model = YOLO(model_path)
            self.ready = True
            print("✅ 摔倒检测模型加载成功")
        except Exception as e:
            print(f"❌ 摔倒检测模型加载失败: {e}")

    def is_ready(self):
        return self.ready

    def detect(self, img):
        """
        检测图片中是否有人摔倒
        返回: (is_fallen, score)
        """
        if not self.ready:
            return False, 0

        try:
            results = self.model(img)

            if results is None or len(results) == 0:
                return False, 0

            for result in results:
                if result.keypoints is not None:
                    kpts = result.keypoints.xy[0].tolist()
                    if len(kpts) >= 13:
                        left_shoulder = kpts[5]
                        right_shoulder = kpts[6]
                        left_hip = kpts[11]
                        right_hip = kpts[12]

                        # 检查关键点有效性
                        if left_shoulder[0] == 0 and left_shoulder[1] == 0:
                            continue

                        shoulder_y = (left_shoulder[1] + right_shoulder[1]) / 2
                        hip_y = (left_hip[1] + right_hip[1]) / 2

                        # 摔倒判断：躯干接近水平
                        torso_diff = abs(shoulder_y - hip_y)

                        if torso_diff < 50:
                            score = int((50 - torso_diff) / 50 * 100)
                            return True, min(100, max(0, score))

            return False, 0

        except Exception as e:
            print(f"摔倒检测异常: {e}")
            return False, 0
