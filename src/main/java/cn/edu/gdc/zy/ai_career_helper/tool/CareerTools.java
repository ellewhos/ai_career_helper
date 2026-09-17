package cn.edu.gdc.zy.ai_career_helper.tool;

import cn.edu.gdc.zy.ai_career_helper.service.CareerService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

public class CareerTools {

    private final CareerService careerService;

    private final Long userId;

    public CareerTools(CareerService careerService, Long userId) {
        this.careerService = careerService;
        this.userId = userId;
    }

    @Tool(description = "根据职位名称或公司名称搜索岗位。")
    public List<Map<String, Object>> queryJobs(String keyword) {
        return careerService.queryJobs(keyword);
    }

    @Tool(description = "收藏指定岗位。只需要提供岗位ID，不需要提供用户ID。")
    public String favoriteJob(Long jobId) {
        return careerService.favoriteJob(userId, jobId);
    }

    @Tool(description = "投递指定岗位。只需要提供岗位ID，不需要提供用户ID。")
    public String applyJob(Long jobId) {
        return careerService.applyJob(userId, jobId);
    }

    @Tool(description = "根据当前登录用户的求职画像和技能推荐匹配岗位。不需要提供用户ID。")
    public List<Map<String, Object>> aiRecommend() {
        return careerService.aiRecommend(userId);
    }

    @Tool(description = "创建或修改当前登录用户的求职档案。需要提供姓名和技能，不需要提供用户ID。")
    public String createResume(String name, String skills) {
        return careerService.createResume(userId, name, skills);
    }
}