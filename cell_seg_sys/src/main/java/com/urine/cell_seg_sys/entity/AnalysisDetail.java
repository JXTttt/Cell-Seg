package com.urine.cell_seg_sys.entity;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 细胞检测详情实体类
 * 对应表: analysis_detail
 */
@Data
public class AnalysisDetail {
    private Long detailId;      // 主键
    private Long recordId;      // 关联的记录ID
    private String className;   // 类别 (rbc, wbc...)
    private BigDecimal confidence; // 置信度 (用BigDecimal保证精度)
    private Integer boxX;       // 坐标X
    private Integer boxY;       // 坐标Y
    private Integer boxW;       // 宽
    private Integer boxH;       // 高
    private String maskPoints;  // 分割掩码点集 (JSON字符串)
}