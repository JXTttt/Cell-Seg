from ultralytics import YOLO

# Load a model
if __name__ == '__main__':
    model = YOLO('E:/code/python/yolo11_seg/ultralytics/cfg/models/11/yolo11s-seg.yaml')# build a new model from scratch
    model.info()
    model.train(data="datasets/cell.yaml",
                epochs=50,
                cache=False,
                batch=11,
                imgsz=640,
                device='0',
                optimizer='SGD',
                project='runs/train',
                name='11n',
               )  # train t