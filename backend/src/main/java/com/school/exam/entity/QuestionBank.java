package com.school.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question_bank")
public class QuestionBank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subjectId;
    private Integer type;
    private String content;
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private BigDecimal score;
    private String createBy;
    private LocalDateTime createTime;
    private Integer delFlag;
}
