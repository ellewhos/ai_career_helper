package cn.edu.gdc.zy.ai_career_helper.service;

import cn.edu.gdc.zy.ai_career_helper.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface CareerService extends IService<User> {

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @return 注册结果
     */
    public String register(String username, String password);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    public String login(String username, String password);

    /**
     * 创建/更新求职档案
     * @param userId 用户ID
     * @param name 姓名
     * @param skills 技能（逗号分隔）
     * @return 操作结果
     */
    public String createResume(Long userId, String name, String skills);

    /**
     * 查询岗位列表
     * @param keyword 搜索关键词
     * @return 岗位列表
     */
    public List<Map<String, Object>> queryJobs(String keyword);

    /**
     * 收藏岗位
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 操作结果
     */
    public String favoriteJob(Long userId, Long jobId);

    /**
     * 投递简历
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 投递结果
     */
    public String applyJob(Long userId, Long jobId);

    /**
     * AI智能推荐岗位
     * @param userId 用户ID
     * @return 推荐岗位列表
     */
    public List<Map<String, Object>> aiRecommend(Long userId);
}