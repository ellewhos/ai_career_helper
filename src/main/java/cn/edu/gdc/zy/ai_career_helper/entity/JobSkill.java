package cn.edu.gdc.zy.ai_career_helper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("job_skill")
public class JobSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private String skill;
}