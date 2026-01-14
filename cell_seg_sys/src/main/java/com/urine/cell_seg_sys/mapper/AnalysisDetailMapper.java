package com.urine.cell_seg_sys.mapper;

import com.urine.cell_seg_sys.entity.AnalysisDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnalysisDetailMapper {

    /**
     * 插入单个细胞详情
     */
    @Insert("INSERT INTO analysis_detail(record_id, class_name, confidence, box_x, box_y, box_w, box_h, mask_points) " +
            "VALUES(#{recordId}, #{className}, #{confidence}, #{boxX}, #{boxY}, #{boxW}, #{boxH}, #{maskPoints})")
    int insert(AnalysisDetail detail);

    /**
     * 根据记录ID查询所有的细胞（用于前端大图画框）
     */
    @Select("SELECT * FROM analysis_detail WHERE record_id = #{recordId}")
    List<AnalysisDetail> selectByRecordId(Long recordId);
}