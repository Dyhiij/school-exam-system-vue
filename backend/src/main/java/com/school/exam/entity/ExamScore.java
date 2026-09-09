package com.school.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_score")
public class ExamScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long userId;
    private BigDecimal totalScore;
    private Integer status;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
    private Integer delFlag;
}
