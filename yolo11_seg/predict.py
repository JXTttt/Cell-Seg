import sys
import json
import os
import cv2
import numpy as np
from ultralytics import YOLO


def main():
    # 用于清理的临时文件路径变量
    temp_processed_path = None

    try:
        # ==================== 1. 接收参数 ====================
        # 参数顺序: 脚本路径 [1]图片路径 [2]旋转角度 [3]翻转标识
        if len(sys.argv) < 2:
            print(json.dumps({"error": "No image path provided"}))
            return

        image_path = sys.argv[1]  # 原图绝对路径

        # 接收可选参数 (做越界检查，没有传则默认为 0)
        rotate_angle = int(sys.argv[2]) if len(sys.argv) > 2 else 0
        flip_h = int(sys.argv[3]) if len(sys.argv) > 3 else 0

        # ==================== 2. 加载模型 ====================
        # 请务必确认此路径是你电脑上 best.pt 的真实路径
        model_path = r'E:\code\python\yolo11_seg\runs\train\11v2.0\weights\best.pt'

        if not os.path.exists(model_path):
            print(json.dumps({"error": f"Model not found at {model_path}"}))
            return

        model = YOLO(model_path)

        # ==================== 3. 图像预处理 (增广) ====================
        # 使用 OpenCV 读取原图
        img = cv2.imread(image_path)
        if img is None:
            print(json.dumps({"error": f"Failed to read image: {image_path}"}))
            return

        processed = False  # 标记是否进行了处理

        # A. 处理旋转 (OpenCV 的 rotate 函数处理 90度 倍数)
        # 注意：前端传来的 rotate_angle 应该是 90, 180, 270 (或 0)
        if rotate_angle == 90:
            img = cv2.rotate(img, cv2.ROTATE_90_CLOCKWISE)
            processed = True
        elif rotate_angle == 180:
            img = cv2.rotate(img, cv2.ROTATE_180)
            processed = True
        elif rotate_angle == 270:
            img = cv2.rotate(img, cv2.ROTATE_90_COUNTERCLOCKWISE)
            processed = True

        # B. 处理水平翻转 (镜像)
        if flip_h == 1:  # Java传过来 true 转成了 "1"
            img = cv2.flip(img, 1)  # 1 表示水平翻转
            processed = True

        # C. 确定预测源
        # 如果进行了处理，保存一个临时图片用于预测，否则直接用原图
        predict_source = image_path
        if processed:
            # 生成临时文件名: xxx_processed.jpg
            temp_processed_path = image_path.replace(".jpg", "_processed.jpg").replace(".png", "_processed.png")
            cv2.imwrite(temp_processed_path, img)
            predict_source = temp_processed_path

        # ==================== 4. 模型推理 ====================
        # 注意：这里使用的是 predict_source (可能是原图，也可能是旋转后的图)
        # conf=0.25 是置信度阈值，可根据需要调整
        results = model.predict(source=predict_source, save=False, conf=0.25, verbose=False)
        result = results[0]

        # ==================== 5. 生成结果图 ====================
        # plot() 返回带有边框和掩码的 BGR 图像数组
        res_img_array = result.plot()

        # 保存路径：在原图路径基础上加 _result
        # 即使旋转了，结果图也应该基于那个临时文件名保存，或者覆盖原逻辑
        # 这里逻辑是：最终结果图文件名 = 原文件名 + "_result.jpg"
        save_path = image_path.replace(".jpg", "_result.jpg").replace(".png", "_result.png")

        cv2.imwrite(save_path, res_img_array)

        # ==================== 6. 提取数据 (Summary & Details) ====================
        summary = {}
        details = []
        names = result.names

        if result.boxes:
            for i, box in enumerate(result.boxes):
                # --- A. 提取基础信息 ---
                cls_id = int(box.cls[0])
                class_name = names[cls_id]
                conf = float(box.conf[0])

                # 统计总数
                summary[class_name] = summary.get(class_name, 0) + 1

                # --- B. 提取坐标 (XYWH) ---
                x, y, w, h = box.xywh[0].tolist()

                # --- C. 提取分割掩码 (可选) ---
                mask_points_str = ""
                if result.masks is not None and len(result.masks.xy) > i:
                    points = result.masks.xy[i]
                    # 将坐标点转为字符串 "x1,y1,x2,y2..."
                    mask_points_str = ",".join([f"{p[0]:.1f},{p[1]:.1f}" for p in points])

                # --- D. 组装单个对象 ---
                detail_item = {
                    "class_name": class_name,
                    "confidence": f"{conf:.4f}",
                    "box_x": int(x),
                    "box_y": int(y),
                    "box_w": int(w),
                    "box_h": int(h),
                    "mask_points": mask_points_str
                }
                details.append(detail_item)

        # ==================== 7. 构造并打印 JSON ====================
        final_output = {
            "code": 200,
            "msg": "success",
            "data": {
                "summary": summary,
                "result_image_path": save_path,
                "details": details
            }
        }
        print(json.dumps(final_output))

    except Exception as e:
        # 捕获所有异常并以 JSON 格式打印
        print(json.dumps({"code": 500, "msg": str(e)}))

    finally:
        # ==================== 8. 清理临时文件 ====================
        # 如果生成了中间过程的旋转图，识别完后删掉，保持文件夹整洁
        if temp_processed_path and os.path.exists(temp_processed_path):
            try:
                os.remove(temp_processed_path)
            except:
                pass  # 删不掉也不报错


if __name__ == "__main__":
    main()