package com.urine.cell_seg_sys.mapper;

import com.urine.cell_seg_sys.entity.AnalysisRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AnalysisRecordMapper {

    /**
     * 插入一条新记录（刚上传图片时调用）
     */
    @Insert("INSERT INTO analysis_record(user_id, sample_name, image_url, status, detect_time) " +
            "VALUES(#{userId}, #{sampleName}, #{imageUrl}, #{status}, #{detectTime})")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(AnalysisRecord record);

    /**
     * 更新检测结果（AI 跑完后调用）
     */
    @Update("UPDATE analysis_record SET status = #{status}, summary_json = #{summaryJson}, result_image_url = #{resultImageUrl} " +
            "WHERE record_id = #{recordId}")
    int updateResult(AnalysisRecord record);

    /**
     * 查询某用户的历史记录（按时间倒序）
     */
    @Select("SELECT * FROM analysis_record WHERE user_id = #{userId} ORDER BY detect_time DESC")
    List<AnalysisRecord> selectByUserId(Long userId);

    /**
     * 根据ID查单条详情
     */
    @Select("SELECT * FROM analysis_record WHERE record_id = #{recordId}")
    AnalysisRecord selectById(Long recordId);
}