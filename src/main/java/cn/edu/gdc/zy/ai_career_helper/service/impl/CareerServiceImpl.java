package cn.edu.gdc.zy.ai_career_helper.service.impl;

import cn.edu.gdc.zy.ai_career_helper.entity.*;
import cn.edu.gdc.zy.ai_career_helper.mapper.*;
import cn.edu.gdc.zy.ai_career_helper.service.CareerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ai.tool.annotation.Tool;
import java.util.*;

@Service
public class CareerServiceImpl extends ServiceImpl<UserMapper, User> implements CareerService {

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private JobSkillMapper jobSkillMapper;

    @Autowired
    private FavoriteJobMapper favoriteJobMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String register(String username, String password) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (baseMapper.selectOne(wrapper) != null) {
            return "用户名已存在";
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        baseMapper.insert(user);
        return "注册成功";
    }

    @Override
    public String login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = baseMapper.selectOne(wrapper);
        if (user == null || !encoder.matches(password, user.getPassword())) {
            return "用户名或密码错误";
        }
        return "登录成功，用户ID：" + user.getId();
    }

    @Override
    public String createResume(Long userId, String name, String skills) {
        QueryWrapper<Resume> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        Resume exist = resumeMapper.selectOne(wrapper);
        if (exist != null) {
            exist.setName(name);
            exist.setSkill(skills);
            resumeMapper.updateById(exist);
            return "档案更新成功";
        }
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setName(name);
        resume.setSkill(skills);
        resumeMapper.insert(resume);
        return "档案创建成功";
    }

    @Override
    public List<Map<String, Object>> queryJobs(String keyword) {
        QueryWrapper<Job> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            // 防 SQL 注入：使用占位符拼接
            String safeKeyword = keyword.replace("'", "''");
            wrapper.like("title", safeKeyword).or().like("company", safeKeyword);
        }
        List<Job> jobs = jobMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Job job : jobs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", job.getId());
            map.put("title", job.getTitle());
            map.put("company", job.getCompany());
            map.put("salary", job.getSalary());
            // 查询岗位技能
            QueryWrapper<JobSkill> skillWrapper = new QueryWrapper<>();
            skillWrapper.eq("job_id", job.getId());
            List<JobSkill> skills = jobSkillMapper.selectList(skillWrapper);
            List<String> skillList = new ArrayList<>();
            for (JobSkill s : skills) {
                skillList.add(s.getSkill());
            }
            map.put("skills", skillList);
            result.add(map);
        }
        return result;
    }

    @Override
    public String favoriteJob(Long userId, Long jobId) {

        // 1. 查询是否已经收藏
        QueryWrapper<FavoriteJob> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("job_id", jobId);

        FavoriteJob exist = favoriteJobMapper.selectOne(wrapper);

        if (exist != null) {
            return "已收藏过该岗位";
        }

        // 2. 没收藏过，执行收藏
        FavoriteJob favorite = new FavoriteJob();
        favorite.setUserId(userId);
        favorite.setJobId(jobId);

        try {
            favoriteJobMapper.insert(favorite);
            return "收藏成功";
        } catch (Exception e) {
            // 数据库唯一索引也可以防止并发重复收藏
            return "已收藏过该岗位";
        }
    }

    @Override
    public String applyJob(Long userId, Long jobId) {
        QueryWrapper<Application> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("job_id", jobId);
        if (applicationMapper.selectOne(wrapper) != null) {
            return "已投递过该岗位";
        }
        Application app = new Application();
        app.setUserId(userId);
        app.setJobId(jobId);
        app.setStatus(1);
        applicationMapper.insert(app);
        return "投递成功";
    }

    @Override
    public List<Map<String, Object>> aiRecommend(Long userId) {
        // 1. 获取用户技能
        QueryWrapper<Resume> resumeWrapper = new QueryWrapper<>();
        resumeWrapper.eq("user_id", userId);
        Resume resume = resumeMapper.selectOne(resumeWrapper);
        if (resume == null || resume.getSkill() == null) {
            return new ArrayList<>();
        }
        String[] userSkills = resume.getSkill().split(",");

        // 2. 遍历所有岗位，计算技能匹配度
        List<Job> allJobs = jobMapper.selectList(null);
        List<Map<String, Object>> recommendList = new ArrayList<>();

        for (Job job : allJobs) {
            QueryWrapper<JobSkill> skillWrapper = new QueryWrapper<>();
            skillWrapper.eq("job_id", job.getId());
            List<JobSkill> jobSkills = jobSkillMapper.selectList(skillWrapper);

            int matchCount = 0;
            for (JobSkill js : jobSkills) {
                for (String us : userSkills) {
                    if (js.getSkill().equalsIgnoreCase(us.trim())) {
                        matchCount++;
                        break;
                    }
                }
            }

            if (matchCount > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", job.getId());
                map.put("title", job.getTitle());
                map.put("company", job.getCompany());
                map.put("salary", job.getSalary());
                map.put("matchCount", matchCount);
                recommendList.add(map);
            }
        }

        // 3. 按匹配度降序排序
        recommendList.sort((a, b) -> (int) b.get("matchCount") - (int) a.get("matchCount"));
        return recommendList;
    }
}