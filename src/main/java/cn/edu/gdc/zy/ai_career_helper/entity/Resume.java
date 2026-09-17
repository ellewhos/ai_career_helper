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
@TableName("resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String skill;
    private LocalDateTime createdAt;
}