package cn.edu.gdc.zy.ai_career_helper.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("interview_record")
public class InterviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private LocalDateTime time;
    private Integer result;
    private LocalDateTime createdAt;
}