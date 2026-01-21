package com.urine.cell_seg_sys.controller;

import com.urine.cell_seg_sys.common.Result;
import com.urine.cell_seg_sys.entity.AnalysisDetail;
import com.urine.cell_seg_sys.entity.AnalysisRecord;
import com.urine.cell_seg_sys.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin // 允许跨域
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private com.urine.cell_seg_sys.mapper.AnalysisDetailMapper detailMapper;

    /**
     * 上传接口
     */
    @PostMapping("/upload")
    public Result<AnalysisRecord> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("userId") Long userId,
                                         // 接收新参数，设定默认值
                                         @RequestParam(value = "rotate", defaultValue = "0") Integer rotate,
                                         @RequestParam(value = "flipH", defaultValue = "false") Boolean flipH) {

        // 1. 先校验文件
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 2. 保存文件并创建初始记录 (只执行一次！)
        AnalysisRecord record = analysisService.uploadAndCreateRecord(file, userId);

        // 3. 调用 Python 进行分析 (传入 rotate 和 flipH 参数)
        // 必须确保 AnalysisService.java 中的 runPythonAnalysis 方法也已经修改为接收4个参数
        analysisService.runPythonAnalysis(record.getRecordId(), record.getImageUrl(), rotate, flipH);

        // 4. 重新查询最新的记录 (为了把 Python 跑出来的结果返回给前端)
        AnalysisRecord finalRecord = analysisService.getById(record.getRecordId());

        // 5. 补充详情列表
        List<AnalysisDetail> details = detailMapper.selectByRecordId(finalRecord.getRecordId());
        finalRecord.setDetails(details);

        return Result.success(finalRecord);
    }
}