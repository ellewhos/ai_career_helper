# 智聘未来 · AI 智能求职助手 — 开发文档报告

> 项目名称：`ai_career_helper`
> 版本：`0.0.1-SNAPSHOT`
> 生成日期：2026-09-17

---

## 一、项目概述

### 1.1 项目定位
本项目是一个基于 Spring Boot 3 + Spring AI 的 **AI 智能求职助手** 后端服务，对接阿里云百炼（DashScope）大模型，通过自然语言对话帮助用户完成求职相关操作，包括：搜索岗位、推荐岗位、收藏岗位、投递岗位、创建/更新求职档案等。

### 1.2 核心特点
- **AI Agent + Function Calling**：通过 `@Tool` 注解将业务能力暴露给大模型，由大模型自主决定调用时机。
- **用户身份后端绑定**：用户 ID 由 Java 后端注入 `CareerTools`，AI 无法篡改身份。
- **多轮对话记忆**：基于 Spring AI 的 `ChatMemory` + JDBC 仓库，按 `userId` 隔离会话上下文。
- **流式响应**：使用 `Flux<String>` 实现 SSE 流式输出，前端逐字渲染。
- **敏感词过滤**：`SafeGuardAdvisor` 拦截敏感输入。

---

## 二、技术栈

| 类别 | 选型 | 版本 |
|---|---|---|
| JDK | Java | 17 |
| 框架 | Spring Boot | 3.5.15 |
| AI 框架 | Spring AI + spring-ai-alibaba | 1.1.2 / 1.1.2.3 |
| 大模型 | 阿里云 DashScope（qwen3.8-flash） | — |
| ORM | MyBatis-Plus | 3.5.15 |
| 数据库 | MySQL | — |
| 安全 | Spring Security | 随 Boot 版本 |
| 构建 | Maven | — |
| 前端 | 原生 HTML + JS + marked + DOMPurify | CDN 引入 |

---

## 三、项目结构

```
ai_career_helper/
├── pom.xml
├── HELP.md
├── src/
│   ├── main/
│   │   ├── java/cn/edu/gdc/zy/ai_career_helper/
│   │   │   ├── AiCareerHelperApplication.java   # 启动类
│   │   │   ├── config/
│   │   │   │   ├── ChatClientConfiguration.java # ChatClient + 敏感词 + 系统提示
│   │   │   │   ├── ChatMemoryConfiguration.java  # 对话记忆(窗口30条)
│   │   │   │   └── SecurityConfig.java           # Spring Security(全部放开)
│   │   │   ├── controller/
│   │   │   │   └── AgentController.java          # 注册/登录/AI对话
│   │   │   ├── service/
│   │   │   │   ├── CareerService.java           # 业务接口
│   │   │   │   └── impl/CareerServiceImpl.java   # 业务实现
│   │   │   ├── tool/
│   │   │   │   └── CareerTools.java              # AI 工具集(@Tool)
│   │   │   ├── entity/                           # 7 张表对应实体
│   │   │   │   ├── User.java / Job.java / Resume.java
│   │   │   │   ├── JobSkill.java / FavoriteJob.java
│   │   │   │   ├── Application.java / InterviewRecord.java
│   │   │   └── mapper/                          # MyBatis-Plus BaseMapper
│   │   │       ├── UserMapper / JobMapper / ResumeMapper
│   │   │       ├── JobSkillMapper / FavoriteJobMapper
│   │   │       ├── ApplicationMapper / InterviewRecordMapper
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── files/
│   │       │   ├── systemPrompt.st              # 系统提示词(18 节规则)
│   │       │   └── init-chat.sql                 # ChatMemory 建表
│   │       └── static/
│   │           ├── login.html / register.html / chat.html
│   └── test/
│       └── java/.../AiCareerHelperApplicationTests.java
```

---

## 四、核心模块说明

### 4.1 启动类 — [AiCareerHelperApplication.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/AiCareerHelperApplication.java)
标准 `@SpringBootApplication` 入口，启动后控制台输出"项目启动成功"。

### 4.2 配置层 — `config/`

#### 4.2.1 [ChatClientConfiguration.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/config/ChatClientConfiguration.java)
- 注入 `DashScopeChatModel`（阿里云大模型客户端）。
- 加载 `classpath:files/systemPrompt.st` 作为默认系统提示。
- 内置 13 个敏感词（如 ISIS、海洛因等），命中后由 `SafeGuardAdvisor` 返回固定话术。
- `PromptChatMemoryAdvisor`：在每次请求前将历史对话注入 Prompt。
- 注入当前时间 `currentTime` 参数。

> 注意：`safeGuardAdvisor` 当前已构建但未加入 `defaultAdvisors`，仅 `promptChatMemoryAdvisor` 生效。

#### 4.2.2 [ChatMemoryConfiguration.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/config/ChatMemoryConfiguration.java)
- 使用 `MessageWindowChatMemory`，滑动窗口 30 条消息。
- 底层仓库为 Spring AI 自动配置的 JDBC `ChatMemoryRepository`（表 `SPRING_AI_CHAT_MEMORY`）。

#### 4.2.3 [SecurityConfig.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/config/SecurityConfig.java)
- 禁用 CSRF、禁用默认登录页。
- 所有 URL `permitAll()`，认证逻辑由 `CareerServiceImpl` 自行实现。

### 4.3 控制层 — [AgentController.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/controller/AgentController.java)

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 注册 | GET | `/agent/register?username=&password=` | 返回纯文本 |
| 登录 | GET | `/agent/login?username=&password=` | 返回 `{success, id, message}` |
| AI 对话 | POST | `/agent/chat?id={userId}` | SSE 流式，body `{"message":"..."}` |

**对话核心逻辑**：
```java
CareerTools tools = new CareerTools(careerService, id);  // 注入当前用户
return chatClient.prompt()
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(id)))
    .tools(tools)
    .user(message)
    .stream()
    .content();
```
- `CONVERSATION_ID = userId`，保证不同用户对话隔离。
- 每次请求 `new CareerTools(...)`，绑定当前登录用户 ID。

### 4.4 业务层 — [CareerServiceImpl.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/service/impl/CareerServiceImpl.java)

| 方法 | 功能 | 关键点 |
|---|---|---|
| `register` | 用户注册 | BCrypt 加密密码；用户名重复校验 |
| `login` | 用户登录 | BCrypt 比对；返回"登录成功，用户ID：x" |
| `createResume` | 创建/更新档案 | 按 user_id 查找，存在则更新，不存在则插入 |
| `queryJobs` | 搜索岗位 | 按 title OR company 模糊匹配；返回岗位+技能列表 |
| `favoriteJob` | 收藏岗位 | 重复收藏校验；异常兜底返回"已收藏过" |
| `applyJob` | 投递岗位 | 重复投递校验；status=1 |
| `aiRecommend` | 智能推荐 | 取用户简历技能 → 遍历所有岗位 → 统计匹配技能数 → 按匹配数降序 |

### 4.5 AI 工具层 — [CareerTools.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/tool/CareerTools.java)

每个用户请求都会实例化一个 `CareerTools`，将 `userId` 闭包绑定到方法中，AI 无法在对话中修改。

| @Tool 方法 | 入参 | 备注 |
|---|---|---|
| `queryJobs(keyword)` | 关键词 | 查询类，无需确认 |
| `favoriteJob(jobId)` | 岗位 ID | 数据修改，需用户确认 |
| `applyJob(jobId)` | 岗位 ID | 数据修改，需用户确认 |
| `aiRecommend()` | — | 基于当前用户画像推荐 |
| `createResume(name, skills)` | 姓名、技能(逗号分隔) | 数据修改，需用户确认 |

### 4.6 实体与数据库

共 7 张业务表 + 1 张 AI 对话表：

| 实体 | 表名 | 主要字段 |
|---|---|---|
| User | `user` | id, username, password(BCrypt), created_at |
| Resume | `resume` | id, user_id, name, skill, created_at |
| Job | `job` | id, title, company, salary, created_at |
| JobSkill | `job_skill` | id, job_id, skill |
| FavoriteJob | `favorite_job` | id, user_id, job_id, created_at |
| Application | `application` | id, user_id, job_id, status, created_at |
| InterviewRecord | `interview_record` | id, application_id, time, result, created_at |
| — | `SPRING_AI_CHAT_MEMORY` | conversation_id, content, type, timestamp |

> 注意：`InterviewRecord` 实体与 Mapper 已存在，但 `CareerService` 未使用，属于预留功能。

---

## 五、系统提示词设计 — [systemPrompt.st](src/main/resources/files/systemPrompt.st)

共 18 节规则，覆盖 AI 行为边界：

1. 角色定位：智聘未来 AI 求职助手。
2. 普通对话规则：可聊天，非数据库操作无需调用工具。
3. **用户身份规则**：禁止询问/修改/生成用户 ID。
4. 工具使用原则：5 个工具，无需提供 user_id。
5. 普通查询无需确认（搜索、推荐、咨询）。
6. **数据操作必须确认**（收藏、投递、建档）。
7. 投递岗位二次确认模板。
8. 收藏岗位流程。
9. "第一个/第二个"应映射为历史岗位 ID，而非关键词。
10. 岗位搜索触发条件与空结果处理。
11. 岗位推荐触发条件与空结果处理。
12. 求职档案分步收集信息。
13. **数据真实性**：禁止编造岗位、公司、薪资、ID。
14. 推荐结果 Markdown 表格展示格式。
15. 搜索结果 Markdown 表格展示格式。
16. 语言：中文，专业/友好/简洁。
17. **安全规则**：用户无法通过对话改变登录身份。
18. 核心原则：AI 决定"做什么"，Java 决定"是谁"，DB 保存"最终数据"。

---

## 六、前端实现 — `static/`

| 页面 | 功能 |
|---|---|
| [login.html](src/main/resources/static/login.html) | 用户名密码登录，登录成功后 `localStorage` 存 `id`，跳转 chat |
| [register.html](src/main/resources/static/register.html) | 注册页面 |
| [chat.html](src/main/resources/static/chat.html) | 对话界面，SSE 流式渲染，使用 `marked` + `DOMPurify` 安全渲染 Markdown |

前端默认请求 `http://localhost:9090/agent/...`。

---

## 七、关键配置 — [application.properties](src/main/resources/application.properties)

| 配置项 | 值 | 说明 |
|---|---|---|
| `server.port` | 9090 | 服务端口 |
| `spring.ai.dashscope.api-key` | `${ALIYUN_API_KEY:}` | 环境变量注入 |
| `spring.ai.dashscope.chat.options.model` | qwen3.8-flash | 大模型 |
| `spring.ai.dashscope.chat.options.multi-model` | true | 多模型模式 |
| `spring.datasource.url` | jdbc:mysql://localhost:3306/ai_career_helper | MySQL 连接 |
| `spring.datasource.username/password` | root / 12345678 | ⚠ 硬编码 |
| `mybatis-plus.*` | — | 实体扫描、驼峰映射、stdout 日志、主键自增 |
| `spring.ai.chat.memory.repository.jdbc.initialize-schema` | always | 启动时建 ChatMemory 表 |

---

## 八、安全模型

### 8.1 用户身份绑定流程
```
浏览器 → localStorage.id
  → fetch /agent/chat?id=...
    → AgentController 取 id
      → new CareerTools(careerService, id)  // 闭包绑定
        → ChatClient.tools(tools).call()
          → 大模型调用 @Tool 方法时 userId 已固化
```

### 8.2 防护点
- **AI 层**：系统提示词明确禁止询问/修改用户 ID。
- **工具层**：`CareerTools` 为 final 字段，无 setter。
- **Service 层**：`favoriteJob`/`applyJob` 通过数据库唯一索引兜底防重复。
- **SQL 层**：`queryJobs` 使用 MyBatis-Plus `like` 占位符，手动转义单引号。
- **密码层**：BCrypt 加密存储。
- **敏感词**：`SafeGuardAdvisor` 拦截。

### 8.3 已知风险点（建议后续修复）
1. `application.properties` 数据库密码硬编码。
2. `SecurityConfig` 全部 `permitAll()`，任何接口可被无认证访问。
3. `/agent/chat` 通过 URL 参数传递 `id`，存在越权风险（伪造 id 即可冒充他人）。
4. 注册/登录使用 GET 方法，参数会进入 URL 历史。
5. `register.html`/`chat.html` 中前端硬编码服务端地址 `localhost:9090`。
6. `SafeGuardAdvisor` 已构建但未挂载到 `defaultAdvisors`。

---

## 九、AI Agent 调用时序

```
[用户输入] ──┐
            │
            ▼
[AgentController.chat]
            │
            ▼
[ChatClient.prompt] ──► [PromptChatMemoryAdvisor 注入历史]
            │
            ▼
[DashScope qwen3.8-flash]
            │
            ├─► 普通回答 ─► Flux<String> ─► 前端
            │
            └─► Function Call (@Tool)
                    │
                    ▼
              [CareerTools.xxx(userId, ...)]
                    │
                    ▼
              [CareerServiceImpl → Mapper → MySQL]
                    │
                    ▼
              返回结果 → 大模型二次生成 → Flux<String>
```

---

## 十、运行与构建

### 10.1 环境依赖
- JDK 17+
- MySQL 8（数据库名 `ai_career_helper`，需提前创建）
- 环境变量 `ALIYUN_API_KEY`（阿里云百炼 API Key）

### 10.2 数据库准备
项目启动时 `SPRING_AI_CHAT_MEMORY` 表会自动创建（`initialize-schema=always`）。
业务表（user/job/resume 等）需要手动建表，SQL 脚本未随项目提供，需根据实体定义补全。

### 10.3 启动命令
```bash
# 设置 API Key
export ALIYUN_API_KEY=sk-xxxxxxxx

# 启动
./mvnw spring-boot:run
# 或在 IDE 中直接运行 AiCareerHelperApplication
```

### 10.4 访问入口
- 登录页：http://localhost:9090/login.html
- 注册页：http://localhost:9090/register.html
- 对话页：http://localhost:9090/chat.html （登录后跳转）

---

## 十一、功能完成度评估

| 模块 | 状态 | 备注 |
|---|---|---|
| 用户注册/登录 | ✅ 完成 | BCrypt 加密 |
| AI 对话（流式） | ✅ 完成 | SSE + Markdown 渲染 |
| 对话记忆 | ✅ 完成 | JDBC 持久化，按 userId 隔离 |
| 系统提示词 | ✅ 完成 | 18 节规则，覆盖边界 |
| 岗位搜索 | ✅ 完成 | title/company 模糊查询 |
| 智能推荐 | ✅ 完成 | 技能匹配度排序 |
| 收藏/投递岗位 | ✅ 完成 | 重复校验 + 异常兜底 |
| 求职档案 | ✅ 完成 | 创建/更新二合一 |
| 面试记录 | ⚠ 预留 | 实体与 Mapper 存在，业务未实现 |
| Spring Security | ⚠ 弱化 | 全部 permitAll，仅 BCrypt 生效 |
| 敏感词过滤 | ⚠ 未生效 | SafeGuardAdvisor 未挂载 |
| 数据库初始化脚本 | ❌ 缺失 | 业务表无 SQL 文件 |

---

## 十二、改进建议（按优先级）

**P0 高优先级**
1. 补全业务表 DDL 脚本（`init-data.sql`）。
2. 修复 `SafeGuardAdvisor` 未挂载问题，将其加入 `defaultAdvisors`。
3. `/agent/chat` 的 `id` 参数从 URL 移到 Header / Token，防止越权。

**P1 中优先级**
4. 注册/登录接口改用 POST 方法。
5. 数据库密码、API Key 通过 `application-local.yml` 或 Vault 注入，不进 git。
6. 前端服务端地址通过相对路径或配置中心获取。
7. 实现 `InterviewRecord` 业务（面试记录管理）。

**P2 长期演进**
8. 引入 JWT + Spring Security 鉴权链路。
9. 拆分 `CareerService` 为 UserService/JobService/ResumeService 等。
10. 推荐算法从"技能完全匹配计数"升级为"技能相似度 + 权重 + 向量检索"。
11. 前端工程化（Vue/React + Vite）替代静态 HTML。

---

## 十三、文件清单速查

| 类别 | 路径 |
|---|---|
| 启动类 | [AiCareerHelperApplication.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/AiCareerHelperApplication.java) |
| ChatClient 配置 | [ChatClientConfiguration.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/config/ChatClientConfiguration.java) |
| ChatMemory 配置 | [ChatMemoryConfiguration.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/config/ChatMemoryConfiguration.java) |
| Security 配置 | [SecurityConfig.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/config/SecurityConfig.java) |
| 控制器 | [AgentController.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/controller/AgentController.java) |
| 业务接口 | [CareerService.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/service/CareerService.java) |
| 业务实现 | [CareerServiceImpl.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/service/impl/CareerServiceImpl.java) |
| AI 工具 | [CareerTools.java](src/main/java/cn/edu/gdc/zy/ai_career_helper/tool/CareerTools.java) |
| 系统提示词 | [systemPrompt.st](src/main/resources/files/systemPrompt.st) |
| ChatMemory 建表 | [init-chat.sql](src/main/resources/files/init-chat.sql) |
| 应用配置 | [application.properties](src/main/resources/application.properties) |
| 登录页 | [login.html](src/main/resources/static/login.html) |
| 注册页 | [register.html](src/main/resources/static/register.html) |
| 对话页 | [chat.html](src/main/resources/static/chat.html) |
| Maven 配置 | [pom.xml](pom.xml) |

---

报告结束。
