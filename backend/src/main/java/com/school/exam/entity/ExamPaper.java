package com.school.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_paper")
public class ExamPaper {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Integer type;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Integer duration;
    private Integer status;
    private LocalDateTime createTime;
    private Integer delFlag;
}
