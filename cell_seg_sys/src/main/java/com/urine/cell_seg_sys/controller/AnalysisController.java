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
@CrossOrigin // 允许跨域 (Vue 和 SpringBoot 端口不一样，必须加这个)
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private com.urine.cell_seg_sys.mapper.AnalysisDetailMapper detailMapper; // 注入详情Mapper

    /**
     * 上传接口
     * POST http://localhost:8080/api/analysis/upload
     */
    @PostMapping("/upload")
    public Result<AnalysisRecord> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("userId") Long userId) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 1. 保存文件并创建初始记录 (状态: 0)
        AnalysisRecord record = analysisService.uploadAndCreateRecord(file, userId);

        // 2. 同步调用 Python 进行分析 (这里为了简单直接同步调用，如果图片大可以改成异步)
        // 注意：这里会阻塞几秒钟，直到 Python 跑完
        analysisService.runPythonAnalysis(record.getRecordId(), record.getImageUrl());

        // 3. 重新查询最新的记录 (为了把 Python 跑出来的结果返回给前端)
        AnalysisRecord finalRecord = analysisService.getById(record.getRecordId());

        List<AnalysisDetail> details = detailMapper.selectByRecordId(finalRecord.getRecordId());
        finalRecord.setDetails(details);

        return Result.success(finalRecord);
    }


}