package com.urine.cell_seg_sys.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 尿液分析记录实体类
 * 对应表: analysis_record
 */
@Data
public class AnalysisRecord {
    private Long recordId;      // 主键
    private Long userId;        // 所属用户ID
    private String sampleName;  // 样本名称/备注
    private String imageUrl;    // 原图路径
    private String resultImageUrl; // 结果图路径
    private String summaryJson; // 统计结果 (JSON字符串)
    private Integer status;     // 状态: 0-识别中, 1-完成, 2-失败
    private Integer isFavorite; // 是否收藏: 0-否, 1-是
    private LocalDateTime detectTime; // 检测时间

    // 👇 新增这个字段，数据库里没有，专门给前端用的
    private List<AnalysisDetail> details;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSampleName() { return sampleName; }
    public void setSampleName(String sampleName) { this.sampleName = sampleName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getResultImageUrl() { return resultImageUrl; }
    public void setResultImageUrl(String resultImageUrl) { this.resultImageUrl = resultImageUrl; }

    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Integer isFavorite) { this.isFavorite = isFavorite; }

    public LocalDateTime getDetectTime() { return detectTime; }
    public void setDetectTime(LocalDateTime detectTime) { this.detectTime = detectTime; }

    public List<AnalysisDetail> getDetails() { return details; }
    public void setDetails(List<AnalysisDetail> details) { this.details = details; }

}