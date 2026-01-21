package com.urine.cell_seg_sys.service;

import com.urine.cell_seg_sys.entity.AnalysisRecord;
import com.urine.cell_seg_sys.mapper.AnalysisRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urine.cell_seg_sys.entity.AnalysisDetail;
import com.urine.cell_seg_sys.mapper.AnalysisDetailMapper; // 记得注入这个
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AnalysisService {

    // 1. 定义图片上传到电脑的哪个文件夹 (请确保 E盘 有这个 uploads 文件夹，或者程序会自动创建)
    private static final String UPLOAD_DIR = "E:/code/upload_files/";

    @Autowired
    private AnalysisRecordMapper recordMapper;

    /**
     * 处理上传逻辑
     * @param file 前端传来的文件
     * @param userId 哪个用户传的
     */
    public AnalysisRecord uploadAndCreateRecord(MultipartFile file, Long userId) {
        // A. 准备文件夹
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // 如果文件夹不存在，就创建
        }

        // B. 生成唯一文件名 (防止文件名冲突)
        // 例如: uuid-image.jpg
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 完整路径
        File saveFile = new File(dir, newFileName);

        try {
            // C. 核心动作：把文件写入硬盘
            file.transferTo(saveFile);

            // D. 写入数据库
            AnalysisRecord record = new AnalysisRecord();
            record.setUserId(userId);
            record.setSampleName("样本-" + System.currentTimeMillis()); // 暂时自动生成个名字
            record.setImageUrl(saveFile.getAbsolutePath()); // 存文件的绝对路径
            record.setStatus(0); // 0 代表 "识别中"
            record.setDetectTime(LocalDateTime.now());

            // 保存到 MySQL
            recordMapper.insert(record);

            return record; // 把生成的数据返回回去

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件保存失败");
        }
    }

    @Autowired
    private AnalysisDetailMapper detailMapper; // 注入详情 Mapper

    // 定义 Python解释器的路径 (建议用绝对路径，例如 Anaconda 的 python.exe)
    // 如果你已经在环境变量里配好了，直接写 "python" 也可以
    private static final String PYTHON_EXE = "C:\\Users\\JXTttt\\miniconda3\\envs\\YOLO11\\python.exe";

    // 定义我们刚才写的脚本路径
    private static final String SCRIPT_PATH = "E:\\Github\\Cell-Seg\\yolo11_seg\\predict.py";

    /**
     * 核心功能：调用 Python 脚本进行分析
     */
    public void runPythonAnalysis(Long recordId, String imagePath) {
        try {
            // 1. 构建命令: python predict.py "E:/images/test.jpg"
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXE, SCRIPT_PATH, imagePath);
            pb.redirectErrorStream(true); // 把错误输出也合并到标准输出，防止缓冲区堵塞

            // 2. 启动进程
            Process process = pb.start();

            // 3. 读取 Python 的输出 (那行 JSON)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK")); // 或者是 "UTF-8"
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python 脚本异常退出，代码: " + exitCode);
            }

            // 4. 解析 JSON 结果
            String rawOutput = output.toString();
            System.out.println("Python原始输出: " + rawOutput); // 打印出来方便调试

            // 关键逻辑：只提取 JSON 部分
            // 寻找第一个左大括号 '{'
            int firstBrace = rawOutput.indexOf("{");
            // 寻找最后一个右大括号 '}'
            int lastBrace = rawOutput.lastIndexOf("}");

            if (firstBrace == -1 || lastBrace == -1) {
                // 如果找不到大括号，说明 Python 根本没返回 JSON，可能报错了但没捕获到
                throw new RuntimeException("Python 没有返回有效的 JSON 数据。原始内容: " + rawOutput);
            }

            // 截取纯净的 JSON 字符串
            String jsonStr = rawOutput.substring(firstBrace, lastBrace + 1);
            System.out.println("清洗后的 JSON: " + jsonStr);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonStr); // 解析清洗后的字符串

            if (rootNode.get("code").asInt() == 200) {
                JsonNode data = rootNode.get("data");

                // 1. 获取 Summary
                String summaryStr = data.get("summary").toString();

                // 2. 获取 Python 生成的结果图路径
                String resultImgPath = data.get("result_image_path").asText();

                AnalysisRecord updateRecord = new AnalysisRecord();
                updateRecord.setRecordId(recordId);
                updateRecord.setSummaryJson(summaryStr);
                updateRecord.setResultImageUrl(resultImgPath);
                updateRecord.setStatus(1);

                // 更新主记录
                recordMapper.updateResult(updateRecord);

                // =========== 👇 核心修改：取消注释并保存详情 👇 ===========
                // 3. 插入详情 (AnalysisDetail)
                JsonNode details = data.get("details");
                if (details.isArray()) {
                    for (JsonNode item : details) {
                        AnalysisDetail detail = new AnalysisDetail();
                        detail.setRecordId(recordId);

                        // 从 JSON 读取字段
                        detail.setClassName(item.get("class_name").asText());
                        detail.setConfidence(new BigDecimal(item.get("confidence").asText()));
                        detail.setBoxX(item.get("box_x").asInt());
                        detail.setBoxY(item.get("box_y").asInt());
                        detail.setBoxW(item.get("box_w").asInt());
                        detail.setBoxH(item.get("box_h").asInt());

                        // 防止 mask_points 为空时报错 (虽然 Python 处理了空串，加个判断更稳妥)
                        if (item.has("mask_points")) {
                            detail.setMaskPoints(item.get("mask_points").asText());
                        }

                        // 插入数据库
                        detailMapper.insert(detail);
                    }
                }
                // =========== 👆 修改结束 👆 ===========

            } else {
                throw new RuntimeException("Python 识别失败: " + rootNode.get("msg").asText());
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 如果出错，把数据库状态改成 "失败"
            AnalysisRecord failRecord = new AnalysisRecord();
            failRecord.setRecordId(recordId);
            failRecord.setStatus(2); // 2 = 失败
            recordMapper.updateResult(failRecord);
        }
    }

    public AnalysisRecord getById(Long id) {
        return recordMapper.selectById(id);
    }
}