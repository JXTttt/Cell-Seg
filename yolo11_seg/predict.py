import sys
import json
import os
import cv2
import numpy as np
from ultralytics import YOLO

def main():
    try:
        # 1. 接收参数
        if len(sys.argv) < 2:
            print(json.dumps({"error": "No image path provided"}))
            return

        image_path = sys.argv[1]

        # 2. 加载模型
        model_path = r'E:\code\python\yolo11_seg\runs\train\11v2.0\weights\best.pt'
        if not os.path.exists(model_path):
            print(json.dumps({"error": f"Model not found at {model_path}"}))
            return

        model = YOLO(model_path)

        # 3. 推理
        results = model.predict(source=image_path, save=False, conf=0.25, verbose=False)
        result = results[0]

        # 4. 生成并保存结果图
        res_img_array = result.plot()
        save_path = image_path.replace(".jpg", "_result.jpg").replace(".png", "_result.png")
        cv2.imwrite(save_path, res_img_array)

        # 5. 统计数据 (summary) 和 收集详情 (details)
        summary = {}
        details = []  # 👈 初始化详情列表
        names = result.names

        # 检查是否有检测结果
        if result.boxes:
            for i, box in enumerate(result.boxes):
                # --- A. 提取基础信息 ---
                cls_id = int(box.cls[0])
                class_name = names[cls_id]
                conf = float(box.conf[0])

                # 更新统计总数
                summary[class_name] = summary.get(class_name, 0) + 1

                # --- B. 提取坐标信息 (XYWH: 中心X, 中心Y, 宽, 高) ---
                # box.xywh 返回的是 tensor，需要转为 list
                x, y, w, h = box.xywh[0].tolist()

                # --- C. 提取分割掩码 (可选) ---
                mask_points_str = ""
                if result.masks is not None and len(result.masks.xy) > i:
                    # result.masks.xy[i] 是一个 numpy 数组，包含该对象的轮廓点坐标
                    points = result.masks.xy[i]
                    # 将坐标点转为字符串 "x1,y1,x2,y2..." 方便存入数据库
                    mask_points_str = ",".join([f"{p[0]:.1f},{p[1]:.1f}" for p in points])

                # --- D. 组装单个对象的数据 ---
                detail_item = {
                    "class_name": class_name,
                    "confidence": f"{conf:.4f}",  # 保留4位小数
                    "box_x": int(x),
                    "box_y": int(y),
                    "box_w": int(w),
                    "box_h": int(h),
                    "mask_points": mask_points_str
                }
                details.append(detail_item)

        # 6. 构造返回 JSON
        final_output = {
            "code": 200,
            "msg": "success",
            "data": {
                "summary": summary,
                "result_image_path": save_path,
                "details": details  # 👈 现在这里有数据了
            }
        }
        print(json.dumps(final_output))

    except Exception as e:
        # 捕获所有异常并以 JSON 格式打印，防止 Java 端解析失败
        print(json.dumps({"code": 500, "msg": str(e)}))

if __name__ == "__main__":
    main()