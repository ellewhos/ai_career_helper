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
@TableName("job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String company;
    private String salary;
    private LocalDateTime createdAt;
}