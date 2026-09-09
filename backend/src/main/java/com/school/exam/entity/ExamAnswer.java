package com.school.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long userId;
    private Long questionId;
    private Integer questionType;
    private String userAnswer;
    /** 客观题: 1对 0错; 主观题: 0未阅 1已阅 */
    private Integer correctFlag;
    private BigDecimal gotScore;
    private LocalDateTime createTime;
}
