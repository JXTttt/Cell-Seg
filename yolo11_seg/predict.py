import sys
import json
import os
import cv2  # 需要 pip install opencv-python
from ultralytics import YOLO


def main():
    try:
        # 1. 接收参数
        if len(sys.argv) < 2:
            print(json.dumps({"error": "No image path provided"}))
            return

        image_path = sys.argv[1]  # 原图路径

        # 2. 加载模型 (请确认路径正确)
        model_path = r'E:\code\python\yolo11_seg\runs\train\11v2.0\weights\best.pt'
        if not os.path.exists(model_path):
            print(json.dumps({"error": f"Model not found at {model_path}"}))
            return

        model = YOLO(model_path)

        # 3. 推理 (verbose=False 静默模式)
        results = model.predict(source=image_path, save=False, conf=0.25, verbose=False)
        result = results[0]

        # result.names[0] = 'rbc'  # 将 ID 0 强行命名为 红细胞
        # result.names[1] = 'wbc'  # 将 ID 1 强行命名为 白细胞

        # ==================== 👇 新增核心代码 👇 ====================
        # 4. 生成结果图 (包含分割掩码、边框、标签)
        # plot() 方法会返回一个 numpy 数组格式的图片
        res_img_array = result.plot()

        # 5. 保存结果图
        # 逻辑：如果原图是 test.jpg，结果图存为 test_result.jpg
        save_path = image_path.replace(".jpg", "_result.jpg").replace(".png", "_result.png")
        cv2.imwrite(save_path, res_img_array)
        # ==================== 👆 新增结束 👆 ====================

        # 6. 统计数据 (summary)
        summary = {}
        names = result.names
        for box in result.boxes:
            cls_id = int(box.cls[0])
            class_name = names[cls_id]
            summary[class_name] = summary.get(class_name, 0) + 1

        # 7. 构造返回 JSON
        final_output = {
            "code": 200,
            "msg": "success",
            "data": {
                "summary": summary,
                "result_image_path": save_path,  # 👈 把结果图的绝对路径传回给 Java
                # details 暂时不需要了，因为我们直接看图，但为了兼容性可以留着空数组
                "details": []
            }
        }
        print(json.dumps(final_output))

    except Exception as e:
        print(json.dumps({"code": 500, "msg": str(e)}))


if __name__ == "__main__":
    main()