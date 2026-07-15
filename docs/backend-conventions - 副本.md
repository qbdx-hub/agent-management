# agent_management 后端开发规范（对齐 community 命名习惯）

本规范以参考项目 **community（包名 `com.southwind`）** 的命名分层与写法为基准。agent_management 的 Java 根包为 **`com.agentmanagement`**（即把 community 的 `southwind` 整体替换为 `agentmanagement`），其余分层目录、类名后缀、注解用法、DTO 拆分、统一返回结构全部沿用 community 的习惯。之前曾误用 `untitled2`（`dao` / `daoImpl` / `config`）作为基准，现已废弃，一切以本文档为准。

## 0. 技术基线

| 项 | 取值 |
|---|---|
| Spring Boot | 2.7.18 |
| JDK | 1.8（pom 用 `maven-compiler-plugin` 显式锁定，覆盖 settings.xml 中的 jdk-17 profile） |
| MyBatis-Plus | 3.5.3.1 |
| MySQL | 8.0.33（驱动 `com.mysql:mysql-connector-j`） |
| 连接池 | Druid 1.2.20 |
| 缓存 | Redis（spring-boot-starter-data-redis） |
| 安全 | Spring Security 5.7 + JWT（jjwt 0.11.5，三件套同版本） |
| 工具 | Lombok 1.18.30、Hutool 5.8.25、fastjson |
| 根包 | `com.agentmanagement` |
| 启动类 | `AgentManagementApplication` |
| API 前缀 | `/api/v1`（通过 `server.servlet.context-path: /api/v1` 统一加前缀） |
| 请求头 | `Authorization: Bearer <token>`、`X-Workspace-Id` |
| 统一响应 | `{ code, message, data }`，**code=0 成功** |
| 分页响应 | `{ list, total, page, pageSize }` |
| RBAC 角色 | ADMIN / MANAGER / DEVELOPER / VIEWER |

> 路由风格说明：community 的 controller 实际是「半 RESTful」（用对了 HTTP 动词，但 path 仍是 `/list /add /edit /del`）。本规范要求 **agent_management 一律走标准 RESTful**（`GET /agents`、`POST /agents`、`PUT /agents/{id}`、`DELETE /agents/{id}`），以对齐前端 `frontend/src/api/*.ts` 的契约。controller 模板按 RESTful 写，命名分层仍照 community。

---

## 1. 目录结构

以下目录树严格对齐 community 的分层，根包 `com.agentmanagement` 与 `com.southwind` 一一对应：

```
src/main/java/com/agentmanagement/
├── AgentManagementApplication.java        # 启动类（根包下）
├── configuration/                         # 配置类（叫 configuration，不是 config）
│   ├── PageConfiguration.java             # 分页（对齐 community 类名；MP 3.5.x 用 MybatisPlusInterceptor）
│   ├── WebMvcConfiguration.java           # 静态资源/上传路径映射
│   ├── ApiConfiguration.java              # 第三方 API 参数绑定（按需）
│   └── MyMetaObjectHandler.java           # 自动填充 createdAt/updatedAt
├── security/                              # 安全相关：JwtUtils / SecurityConfig / JwtAuthenticationFilter
├── entity/                                # 实体类（对应数据库表，单数大驼峰）
├── mapper/                                # Mapper 接口（继承 BaseMapper）
├── service/                               # Service 接口（继承 IService）
│   └── impl/                              # Service 实现类（继承 ServiceImpl）
├── controller/                            # Controller（@RestController + RESTful）
├── form/                                  # 请求 DTO（XxxForm / XxxQueryForm / XxxSaveForm / LoginForm）
├── vo/                                    # 响应 VO（XxxVO / PageVO）
├── common/                                # Result<T>、PageResult<T>、常量、枚举、错误码
├── annotation/                            # 自定义注解（@Log，按需）
├── aspect/                                # AOP 切面（LogAspect，按需）
└── util/                                  # 其他纯工具类
src/main/resources/
├── application.yml
└── mapper/                                # Mapper XML（如有自定义多表查询）
```

**与 untitled2 的对比（务必避免混淆）：**

- community 用 `service` + `service.impl`（接口在 `service/`、实现在 `service/impl/`），agent_management **同样走 service/serviceImpl**。untitled2 用的是 `dao` / `daoImpl`，本项目**不采用**。
- community 配置类放在 `configuration` 包，类名 `XxxConfiguration`；untitled2 用 `config` 包 + `XxxConfig`，本项目**不采用**。
- 统一响应：agent_management 放在 `common` 包（`com.agentmanagement.common.Result`），不放 `util`，便于与 `PageResult`、错误码常量集中管理。

---

## 2. 各层命名规范

### 2.1 启动类

- **命名规则**：项目名去下划线转 PascalCase + `Application` → 固定 `AgentManagementApplication`（不要写成 `Application` 或别的）。
- **所属包**：`com.agentmanagement`（根包，与 `mapper` / `service` 等同级，保证 `@SpringBootApplication` 默认扫描整个根包）。
- **注解**：`@SpringBootApplication` + `@MapperScan("com.agentmanagement.mapper")`。

community 真实示例：

```java
package com.southwind;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.southwind.mapper")
public class CommunityDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityDemoApplication.class, args);
    }
}
```

agent_management 可复制模板：

```java
package com.agentmanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.agentmanagement.mapper")
public class AgentManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentManagementApplication.class, args);
    }
}
```

要点：`@MapperScan` 必须显式指到 `com.agentmanagement.mapper`，否则 MyBatis-Plus 不会注入 Mapper；`main` 用 `SpringApplication.run(当前类.class, args)`。

### 2.2 entity

- **命名规则**：与表名对应的单数大驼峰，如 `Agent.java` / `User.java` / `Tool.java`。
- **所属包**：`com.agentmanagement.entity`。
- **类级注解**：`@Data` + `@EqualsAndHashCode(callSuper = false)`，`implements Serializable`，声明 `serialVersionUID`。
- **主键**：`@TableId(value = "蛇形列名", type = IdType.AUTO)`，类型 `Integer`/`Long`。

community 真实示例：

```java
package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    private String username;
    private String password;
    /** 状态  0正常   1禁用 */
    private Integer status;
}
```

agent_management 可复制模板（以 `Agent` 为例）：

```java
package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 坐席
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("agent")
public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "agent_id", type = IdType.AUTO)
    private Long agentId;

    private Long workspaceId;

    private String agentName;

    private String extNumber;

    /** 状态  0空闲   1忙碌   2离线 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

要点：
- 时间字段统一用 `createdAt` / `updatedAt`（驼峰，对应库 `created_at`/`updated_at`），配合 `MyMetaObjectHandler` 自动填充；如不启用自动填充，则在 service 内手动 `setCreatedAt(LocalDateTime.now())`。
- community 原文不加 `@TableName`（依赖 MP 驼峰转下划线），agent_management 建议显式 `@TableName` 以避免表名复数歧义。
- 普通字段不加 `@TableField`（靠驼峰自动映射，如 `agentName → agent_name`）。

### 2.3 mapper

- **命名规则**：实体名 + `Mapper`，如 `AgentMapper`。
- **所属包**：`com.agentmanagement.mapper`。
- 接口 `extends BaseMapper<实体>`，**不加** `@Mapper` / `@Repository`（靠启动类 `@MapperScan` 统一扫描）。

community 真实示例：

```java
package com.southwind.mapper;

import com.southwind.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface UserMapper extends BaseMapper<User> {
}
```

agent_management 可复制模板：

```java
package com.agentmanagement.mapper;

import com.agentmanagement.entity.Agent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AgentMapper extends BaseMapper<Agent> {
}
```

单表 CRUD 直接继承 `BaseMapper`（`selectById`/`insert`/`updateById`/`deleteById`/`selectPage`）。需要自定义多表查询时，再声明方法并配 XML（放 `resources/mapper/AgentMapper.xml`）或注解。

### 2.4 service 接口

- **命名规则**：实体名 + `Service`，如 `AgentService`。
- **所属包**：`com.agentmanagement.service`（**不是** `dao`）。
- 接口 `extends IService<实体>`，声明业务方法。

community 真实示例：

```java
package com.southwind.service;

import com.southwind.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.southwind.form.UserForm;
import com.southwind.vo.PageVO;

public interface UserService extends IService<User> {
    PageVO userList(UserForm userForm);
}
```

agent_management 可复制模板：

```java
package com.agentmanagement.service;

import com.agentmanagement.entity.Agent;
import com.baomidou.mybatisplus.extension.service.IService;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.vo.AgentVO;

public interface AgentService extends IService<Agent> {
    PageResult<AgentVO> pageAgents(AgentQueryForm form);
}
```

### 2.5 service.impl（Service 实现类）

- **命名规则**：实体名 + `ServiceImpl`，如 `AgentServiceImpl`。
- **所属包**：`com.agentmanagement.service.impl`（**不是** `dao.impl`）。
- 类签名模板：`class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService`。
- 类级注解：`@Service`；依赖注入用字段注入 + `@Autowired`。

community 真实示例（分页查询范式）：

```java
package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.User;
import com.southwind.form.UserForm;
import com.southwind.mapper.UserMapper;
import com.southwind.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.vo.PageVO;
import com.southwind.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageVO userList(UserForm userForm) {
        Page<User> page = new Page<>(userForm.getPage(), userForm.getLimit());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(userForm.getUsername()), "username", userForm.getUsername())
                .like(StringUtils.isNotBlank(userForm.getRealName()), "real_name", userForm.getRealName());
        Page<User> resultPage = this.userMapper.selectPage(page, queryWrapper);
        PageVO pageVO = new PageVO();
        pageVO.setTotalCount(resultPage.getTotal());
        pageVO.setPageSize(resultPage.getSize());
        pageVO.setCurrPage(resultPage.getCurrent());
        pageVO.setTotalPage(resultPage.getPages());
        List<UserVO> list = new ArrayList<>();
        for (User record : resultPage.getRecords()) {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(record, vo);
            list.add(vo);
        }
        pageVO.setList(list);
        return pageVO;
    }
}
```

agent_management 可复制模板（分页字段名对齐前端 `page`/`pageSize`/`total`/`list`）：

```java
package com.agentmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.service.AgentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.vo.AgentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    @Autowired
    private AgentMapper agentMapper;

    @Override
    public PageResult<AgentVO> pageAgents(AgentQueryForm form) {
        Page<Agent> page = new Page<>(form.getPage(), form.getPageSize());
        QueryWrapper<Agent> qw = new QueryWrapper<>();
        qw.eq(form.getWorkspaceId() != null, "workspace_id", form.getWorkspaceId())
          .like(StringUtils.isNotBlank(form.getAgentName()), "agent_name", form.getAgentName())
          .eq(form.getStatus() != null, "status", form.getStatus())
          .orderByDesc("created_at");
        Page<Agent> result = this.agentMapper.selectPage(page, qw);

        List<AgentVO> list = new ArrayList<>();
        for (Agent record : result.getRecords()) {
            AgentVO vo = new AgentVO();
            BeanUtils.copyProperties(record, vo);
            list.add(vo);
        }
        return PageResult.of(list, result.getTotal(), form.getPage(), form.getPageSize());
    }
}
```

要点：`QueryWrapper` 列名写**数据库蛇形列名**（`real_name`），不是实体驼峰属性；条件式 `like/eq` 用 `StringUtils.isNotBlank(...)` / `!= null` 做开关；实体转 VO 用 `org.springframework.beans.BeanUtils.copyProperties(record, vo)`。

### 2.6 controller

- **命名规则**：业务实体名 + `Controller`，如 `AgentController`、`AuthController`、`ToolController`。
- **所属包**：`com.agentmanagement.controller`。
- 类级注解 `@RestController` + `@RequestMapping("/agents")`（前缀 `/api/v1` 由 context-path 统一加，不要在类上重复写）。
- 方法级用 RESTful 动词：`@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/`@PatchMapping`。
- 写操作参数加 `@RequestBody` + `@Valid`；路径变量 `@PathVariable("id")`；查询 list 接口用对象绑定（POJO 无注解）。

community 真实示例（半 RESTful 风格，仅作分层参考）：

```java
package com.southwind.controller;

import com.southwind.annotation.LogAnnotation;
import com.southwind.entity.User;
import com.southwind.form.UserAddOrUpdateForm;
import com.southwind.form.UserForm;
import com.southwind.service.UserService;
import com.southwind.util.Result;
import com.southwind.vo.PageVO;
import com.southwind.vo.UserEditVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/sys/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result list(UserForm userForm){
        PageVO pageVO = this.userService.userList(userForm);
        return Result.ok().put("data", pageVO);
    }

    @LogAnnotation("添加用户")
    @PostMapping("/add")
    public Result add(@RequestBody UserAddOrUpdateForm form){
        User user = new User();
        BeanUtils.copyProperties(form, user);
        boolean save = this.userService.save(user);
        if(save) return Result.ok();
        return Result.error("添加用户失败");
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        User user = this.userService.getById(id);
        UserEditVO vo = new UserEditVO();
        BeanUtils.copyProperties(user, vo);
        return Result.ok().put("data", vo);
    }

    @LogAnnotation("编辑用户")
    @PutMapping("/edit")
    public Result edit(@RequestBody UserEditVO vo){
        User user = new User();
        BeanUtils.copyProperties(vo, user);
        boolean ok = this.userService.updateById(user);
        if(ok) return Result.ok();
        return Result.error("编辑用户失败");
    }

    @LogAnnotation("删除用户")
    @DeleteMapping("/del")
    public Result del(@RequestBody Integer[] ids){
        boolean ok = this.userService.removeByIds(Arrays.asList(ids));
        if(ok) return Result.ok();
        return Result.error("删除用户失败");
    }
}
```

agent_management 可复制模板（**强制标准 RESTful，对齐前端**）：

```java
package com.agentmanagement.controller;

import com.agentmanagement.annotation.Log;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.form.AgentSaveForm;
import com.agentmanagement.service.AgentService;
import com.agentmanagement.vo.AgentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;

@RestController
@RequestMapping("/agents")
public class AgentController {

    @Autowired
    private AgentService agentService;

    /** GET /api/v1/agents —— 分页列表 */
    @GetMapping
    public Result<PageResult<AgentVO>> list(AgentQueryForm form) {
        return Result.success(agentService.pageAgents(form));
    }

    /** GET /api/v1/agents/{id} —— 详情 */
    @GetMapping("/{id}")
    public Result<AgentVO> get(@PathVariable("id") Long id) {
        Agent agent = agentService.getById(id);
        AgentVO vo = new AgentVO();
        BeanUtils.copyProperties(agent, vo);
        return Result.success(vo);
    }

    /** POST /api/v1/agents —— 新增 */
    @Log("新增坐席")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AgentSaveForm form) {
        Agent agent = new Agent();
        BeanUtils.copyProperties(form, agent);
        agentService.save(agent);
        return Result.success();
    }

    /** PUT /api/v1/agents/{id} —— 全量更新 */
    @Log("编辑坐席")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody AgentSaveForm form) {
        Agent agent = new Agent();
        BeanUtils.copyProperties(form, agent);
        agent.setAgentId(id);
        agentService.updateById(agent);
        return Result.success();
    }

    /** PATCH /api/v1/agents/{id}/status —— 修改状态 */
    @Log("修改坐席状态")
    @PatchMapping("/{id}/status")
    public Result<Void> patchStatus(@PathVariable("id") Long id,
                                    @RequestParam Integer status) {
        Agent agent = new Agent();
        agent.setAgentId(id);
        agent.setStatus(status);
        agentService.updateById(agent);
        return Result.success();
    }

    /** DELETE /api/v1/agents/{id} —— 删除（支持批量传 id 数组走 body） */
    @Log("删除坐席")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentService.removeByIds(Arrays.asList(id));
        return Result.success();
    }
}
```

要点：
- controller 只注入 `Service`，**不直接注入 Mapper**。
- 返回统一 `Result<T>`（详见第 4 节），不要返回裸 entity、不要返回 `ResponseEntity`。
- 子资源路由示例：`GET /agents/{id}/config/model`、`POST /agents/{id}/config/model`、`POST /auth/login`、`POST /auth/logout`、`GET /sessions/{id}/messages/stream`（SSE）。

### 2.7 form（请求 DTO）

- **命名规则**：`XxxForm`，按用途细分：`XxxQueryForm`（查询/分页）、`XxxSaveForm`（新增/编辑）、`LoginForm`、`UpdatePasswordForm`。
- **所属包**：`com.agentmanagement.form`。
- 仅用 `@Data`；查询表单与保存表单**分两个类**，不要合并。
- 分页字段统一 `page` + `pageSize`（不是 community 的 `page`/`limit`，也不是 `pageNum`/`pageSize`，**以前端契约为准**）。

community 真实示例：

```java
// 查询/分页
@Data
public class UserForm {
    private Integer page;
    private Integer limit;
    private String username;
    private String realName;
}

// 新增/编辑
@Data
public class UserAddOrUpdateForm {
    private Integer userId;
    private String username;
    private String password;
    private String realName;
    private String mobile;
    private Integer status;
    private Integer roleId;
}

// 登录
@Data
public class LoginForm {
    private String captcha;
    private String password;
    private String username;
    private String uuid;
}
```

agent_management 可复制模板：

```java
package com.agentmanagement.form;

import lombok.Data;

/** 查询/分页（controller 参数无 @RequestBody，对象绑定 query 参数） */
@Data
public class AgentQueryForm {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Long workspaceId;
    private String agentName;   // 模糊
    private Integer status;
}

/** 新增/编辑（@RequestBody 接收 JSON） */
@Data
public class AgentSaveForm {
    private Long agentId;
    private Long workspaceId;
    private String agentName;
    private String extNumber;
    private String phone;
    private Integer status;
    private String remark;
}

/** 登录 */
@Data
public class LoginForm {
    private String username;
    private String password;
    private String captcha;
    private String uuid;
}
```

要点：community 完全不用 `@NotBlank`/`@NotNull` 等 validation 注解。agent_management 允许在 Form 上加 `javax.validation` 注解（如 `@NotBlank`），controller 参数前补 `@Valid` 做增强。

### 2.8 vo（响应视图对象）

- **命名规则**：`XxxVO`，按场景细分：`XxxVO`（列表/详情）、`XxxEditVO`（编辑回显）、`PageVO`（通用分页）。
- **所属包**：`com.agentmanagement.vo`。
- controller **永远不直接返回 entity**，必须 copy 到 VO。

community 真实示例：

```java
@Data
public class UserVO {
    private Integer userId;
    private String username;
    private String roleName;   // 关联表带出
    private String realName;
    private String contact;
    private String mobile;
    private Integer status;
}

@Data
public class PageVO {
    private Long totalCount;
    private Long pageSize;
    private Long totalPage;
    private Long currPage;
    private List list;         // community 用裸 List
}
```

agent_management 可复制模板（列表 VO + 详情 VO 分离，分页字段名对齐前端）：

```java
package com.agentmanagement.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** 列表/详情 VO（脱敏 + 关联字段） */
@Data
public class AgentVO {
    private Long agentId;
    private Long workspaceId;
    private String agentName;
    private String extNumber;
    private String groupName;     // 关联 group 表带出
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
```

> 分页结构 agent_management 不复用 community 的 `PageVO`，而是用 `common.PageResult<T>`（见第 4 节），字段为 `{ list, total, page, pageSize }`，与前端 `frontend/src/api/*.ts` 严格对齐。

### 2.9 configuration（配置类）

- **包名**：`configuration`（**不是** `config`）；类名 `XxxConfiguration`（**不是** `XxxConfig`）。
- 固定三类：`PageConfiguration`（分页，对齐 community 类名，但内容用 MP 3.5.x 的 `MybatisPlusInterceptor`）、`WebMvcConfiguration`（静态资源映射/跨域）、`ApiConfiguration`（第三方 API 参数绑定，按需）。
- 自动填充处理器本项目命名 `MyMetaObjectHandler`，放 `configuration` 包。

#### PageConfiguration（community 原文，MP 3.4.x 老插件）

```java
package com.southwind.configuration;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PageConfiguration {
    @Bean
    public PaginationInterceptor paginationInterceptor(){
        return new PaginationInterceptor();
    }
}
```

#### agent_management 版（MP 3.5.x 新插件，必须用这个）

```java
package com.agentmanagement.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PageConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor page = new PaginationInnerInterceptor(DbType.MYSQL);
        page.setMaxLimit(500L);          // 单页上限，防恶意大页
        interceptor.addInnerInterceptor(page);
        return interceptor;
    }
}
```

> MP 3.5.3.1 中老 `PaginationInterceptor` 已移除，必须用 `MybatisPlusInterceptor + PaginationInnerInterceptor`。

#### WebMvcConfiguration

community 原文（注册上传文件磁盘路径映射）：

```java
package com.southwind.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Value("${upload.face}")
    String face;
    @Value("${upload.excel}")
    String excel;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/community/upload/face/**").addResourceLocations("file:" + face);
        registry.addResourceHandler("/community/upload/excel/**").addResourceLocations("file:" + excel);
    }
}
```

agent_management 版（若需跨域/拦截器，全部归到此类，不再拆 `WebConfig`）：

```java
package com.agentmanagement.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

#### ApiConfiguration（第三方 API 参数绑定，按需）

community 原文（人脸识别参数）：

```java
package com.southwind.configuration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(value = "plateocr")
@Component
@Data
@ApiModel(value = "ApiConfiguration", description = "人脸识别参数描述")
public class ApiConfiguration {
    @ApiModelProperty("secretId")
    private String secretId;
    @ApiModelProperty("secretKey")
    private String secretKey;
    @ApiModelProperty("是否启用")
    private boolean used = false;
}
```

agent_management 版（前缀与 yml 顶层段名一致；暂无第三方 API 可省略）：

```java
package com.agentmanagement.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(value = "api")
@Component
@Data
public class ApiConfiguration {
    private String secretId;
    private String secretKey;
    private boolean used = false;
}
```

#### MyMetaObjectHandler（自动填充时间）

community 风格字段是 `createTime`/`updateTime`；agent_management 统一为 `createdAt`/`updatedAt`。

```java
package com.agentmanagement.configuration;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

> 第二参数必须是 `"createdAt"`/`"updatedAt"`（与 entity 字段名一致），否则静默失败。`@Component` 必须加，否则不生效且无报错。配合 entity 字段上的 `@TableField(fill = FieldFill.INSERT)` / `FieldFill.INSERT_UPDATE`。

### 2.10 util / common（统一响应 Result）

community 风格的 `Result` 是 `HashMap` 子类、链式 `.put()`、成功码非 0。agent_management **必须改成强类型 `Result<T>`、字段 `{code,message,data}`、`code=0` 成功**，以严格对齐前端 `frontend/src/api/index.ts` 第 28 行 `if (data.code !== 0)` 的判定。

agent_management 可复制模板（放 `common` 包，不放 `util`）：

```java
package com.agentmanagement.common;

import lombok.Data;

/**
 * 统一响应包装类。
 * 契约：code=0 成功；非 0 失败。字段名 message（不是 msg）。
 * 前端 frontend/src/api/index.ts 按 data.code !== 0 判失败，务必严格对齐。
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {}

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

> 与 community 的差异（务必在 review 时关注）：community 的 `Result` 是 `HashMap` 子类、**`code=200` 成功**、字段 `msg`、链式 `.put()`；agent_management 改成强类型 `Result<T>`、**`code=0` 成功**、字段 `message`。**绝对不要把 community 的 `code=200`/`msg` 照搬**，否则前端会把成功当失败处理。

---

## 3. application.yml 模板

注意后缀是 `.yml`（不是 `.properties` / `.yaml`），位置 `src/main/resources/application.yml`。`server.servlet.context-path: /api/v1` 统一加 API 前缀，controller 类上的 `@RequestMapping` 不要再重复写 `/api/v1`。

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  application:
    name: agent_management
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/agent_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8&useSSL=false
    username: root
    password: root
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
  redis:
    host: localhost
    port: 6379
    database: 1
    timeout: 5000ms
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB

mybatis-plus:
  mapper-locations:
    - classpath*:/mapper/**/*.xml
  type-aliases-package: com.agentmanagement.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: PLEASE_REPLACE_WITH_A_LONG_RANDOM_SECRET_KEY_AT_LEAST_32_BYTES
  expiration: 86400000          # 24h
  header: Authorization
  prefix: "Bearer "

upload:
  path: D:/agent_management/upload/
  url-prefix: http://localhost:8080/api/v1/upload/

api:
  secret-id: AKIDxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  secret-key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  used: false

logging:
  level:
    root: info
    com.agentmanagement: debug
    com.agentmanagement.mapper: debug
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

要点（避坑）：
- `spring.servlet` 与 `server.servlet` 是两个不同的 key，不要写在同级混淆。`server.servlet.context-path` 控制全局前缀；`spring.servlet.multipart` 控制文件上传。
- yml 顶层段名（`jwt` / `upload` / `api` / `mybatis-plus`）必须与 `@ConfigurationProperties(value="...")` / `@Value("${...}")` 占位符精确对应。
- `jwt.secret` 生产环境务必走环境变量 `${JWT_SECRET}`，不要硬编码进仓库。

---

## 4. 统一响应格式

### 4.1 Result\<T\>

见第 2.10 节模板。固定三字段：

```json
{ "code": 0, "message": "success", "data": {} }
```

- `code = 0` 表示成功；非 0 表示失败。
- 字段名是 `message`（不是 community 的 `msg`）。
- controller 一律用 `Result.success(data)` / `Result.success()` / `Result.error(code, message)`，不要再手写 `Map`。

### 4.2 业务错误码表

| 错误码 | 含义 | 触发场景 |
|---|---|---|
| 0 | 成功 | 正常返回 |
| 1001 | 参数校验失败 | `@Valid` 不通过 |
| 1002 | 资源不存在 | `getById` 返回 null |
| 1003 | 资源已存在 | 唯一约束冲突（如工号重复） |
| 1004 | 未登录 / token 过期 | JWT 解析失败（401） |
| 2001 | 无权限 | RBAC 不足（403） |
| 2002 | 工作空间越权 | 非 workspace 成员操作 |
| 2003 | 状态非法 | 状态机不允许的流转 |
| 3001 | 第三方服务异常 | 模型/OSS/MCP 调用失败 |
| 3002 | 限流 | 触发熔断或频控 |

### 4.3 PageResult\<T \>（分页响应）

分页 `data` 固定字段 `{ list, total, page, pageSize }`，与前端 `frontend/src/api/*.ts` 严格对齐（不复用 community 的 `PageVO` 命名）。

```java
package com.agentmanagement.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private Long total;
    private Long page;
    private Long pageSize;

    public PageResult() {}

    public PageResult(List<T> list, Long total, Long page, Long pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    /** 由 MyBatis-Plus 的 IPage 直接构造（list 已转成 VO） */
    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
```

controller 分页接口示例：

```java
@GetMapping
public Result<PageResult<AgentVO>> list(AgentQueryForm form) {
    return Result.success(agentService.pageAgents(form));
}
```

返回体：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [ { "agentId": 1, "agentName": "...", "status": 0 } ],
    "total": 128,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 5. 各模块文件清单

本项目 10 个业务模块，每个按 community 的 `Controller / Service / ServiceImpl / Entity / Mapper / Form / VO` 分层建文件。**Service 接口在 `service/`、实现在 `service/impl/`，不走 dao/daoImpl。**

### auth（认证）
- `controller/AuthController`
- `service/AuthService` + `service/impl/AuthServiceImpl`
- `form/LoginForm`
- `vo/LoginVO`、`vo/UserInfoVO`
- 复用 entity `User`、`Role`

### workspaces（工作空间）
- `controller/WorkspaceController`、`controller/WorkspaceMemberController`
- `service/WorkspaceService` + `impl/WorkspaceServiceImpl`、`service/WorkspaceMemberService` + `impl/WorkspaceMemberServiceImpl`
- `entity/Workspace`、`entity/WorkspaceMember`
- `mapper/WorkspaceMapper`、`mapper/WorkspaceMemberMapper`
- `form/WorkspaceSaveForm`、`form/WorkspaceMemberForm`、`form/WorkspaceQueryForm`
- `vo/WorkspaceVO`、`vo/WorkspaceMemberVO`

### agents（坐席）
- `controller/AgentController`、`controller/AgentConfigController`
- `service/AgentService` + `impl/AgentServiceImpl`、`service/AgentConfigService` + `impl/AgentConfigServiceImpl`
- `entity/Agent`、`entity/AgentConfig`
- `mapper/AgentMapper`、`mapper/AgentConfigMapper`
- `form/AgentQueryForm`、`form/AgentSaveForm`、`form/AgentConfigForm`
- `vo/AgentVO`、`vo/AgentConfigVO`

### tools（工具）
- `controller/ToolController`、`controller/McpServerController`
- `service/ToolService` + `impl/ToolServiceImpl`、`service/McpServerService` + `impl/McpServerServiceImpl`
- `entity/Tool`、`entity/McpServer`
- `mapper/ToolMapper`、`mapper/McpServerMapper`
- `form/ToolSaveForm`、`form/McpServerSaveForm`
- `vo/ToolVO`、`vo/McpServerVO`

### sessions（会话，含 SSE）
- `controller/SessionController`、`controller/MessageController`
- `service/SessionService` + `impl/SessionServiceImpl`、`service/MessageService` + `impl/MessageServiceImpl`
- `entity/Session`、`entity/Message`、`entity/ExecutionStep`
- `mapper/SessionMapper`、`mapper/MessageMapper`、`mapper/ExecutionStepMapper`
- `form/SessionCreateForm`、`form/MessageSendForm`
- `vo/SessionVO`、`vo/MessageVO`、`vo/ExecutionStepVO`

### workflows（工作流 V2）
- `controller/WorkflowController`
- `service/WorkflowService` + `impl/WorkflowServiceImpl`
- `entity/Workflow`
- `mapper/WorkflowMapper`
- `form/WorkflowSaveForm`、`form/WorkflowQueryForm`
- `vo/WorkflowVO`

### knowledge-bases（知识库 V2）
- `controller/KnowledgeBaseController`、`controller/DocumentController`
- `service/KnowledgeBaseService` + `impl/KnowledgeBaseServiceImpl`、`service/DocumentService` + `impl/DocumentServiceImpl`
- `entity/KnowledgeBase`、`entity/Document`
- `mapper/KnowledgeBaseMapper`、`mapper/DocumentMapper`
- `form/KnowledgeBaseSaveForm`、`form/DocumentUploadForm`
- `vo/KnowledgeBaseVO`、`vo/DocumentVO`

### monitor（监控）
- `controller/MonitorController`
- `service/MonitorService` + `impl/MonitorServiceImpl`
- `entity/ErrorLog`
- `mapper/ErrorLogMapper`
- `form/MonitorQueryForm`
- `vo/MonitorVO`、`vo/ErrorLogVO`

### cost（成本）
- `controller/CostController`、`controller/BudgetController`
- `service/CostService` + `impl/CostServiceImpl`、`service/BudgetService` + `impl/BudgetServiceImpl`
- `entity/CostRecord`、`entity/Budget`、`entity/ModelProvider`、`entity/ApiKey`
- `mapper/CostRecordMapper`、`mapper/BudgetMapper`、`mapper/ModelProviderMapper`、`mapper/ApiKeyMapper`
- `form/CostQueryForm`、`form/BudgetSaveForm`、`form/ModelProviderSaveForm`
- `vo/CostRecordVO`、`vo/BudgetVO`、`vo/ModelProviderVO`

### security（安全审计）
- `controller/SecurityController`
- `service/SecurityService` + `impl/SecurityServiceImpl`
- `entity/AuditLog`、`entity/Role`
- `mapper/AuditLogMapper`、`mapper/RoleMapper`
- `form/AuditLogQueryForm`
- `vo/AuditLogVO`

---

## 6. 常见约定与避坑

### 6.1 community 写法约定（直接沿用）

1. **分层注入**：controller 注入 service，service 注入 mapper；**不要在 controller 里直接 `@Autowired` Mapper**（community 偶有此不规范写法，本项目避免）。
2. **Mapper 扫描**：靠启动类 `@MapperScan("com.agentmanagement.mapper")` 统一扫描，Mapper 接口上**不加** `@Mapper`。
3. **统一返回**：所有 controller 方法返回 `Result<T>`（`code=0` 成功），不要返回裸 `Map`、`ResponseEntity`、entity。
4. **entity ↔ VO/Form 转换**：统一用 `org.springframework.beans.BeanUtils.copyProperties(source, target)`，controller 永不直接返回 entity。
5. **QueryWrapper 列名**：写**数据库蛇形列名**（`agent_name`），不是实体驼峰属性名。
6. **Service 类签名**：`class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService`，类级只加 `@Service`；写操作如需事务再补 `@Transactional(rollbackFor = Exception.class)`。
7. **字段注入**：community 用 `@Autowired` 字段注入，本项目沿用（不强求构造器注入，保持风格一致）。
8. **MetaObjectHandler 自动填充时间**：实现 `insertFill`/`updateFill`，字段名 `createdAt`/`updatedAt`，`@Component` 必须加，配合 entity 的 `@TableField(fill = ...)`。
9. **@Aspect 日志切面**：`@Around("@annotation(log)")` 绑定 `@Log` 注解，环绕采集入参/出参/耗时，`pjp.proceed()` 前后计时，异常 `catch` 后 `re-throw` 不吞；操作人 `userId` 从 `SecurityContextHolder` 取。
10. **包名后缀**：配置包 `configuration`（不是 `config`），配置类 `XxxConfiguration`（不是 `XxxConfig`）；业务层 `service`/`service.impl`（不是 `dao`/`dao.impl`）。

### 6.2 agent_management 特有避坑

1. **JDK 版本**：settings.xml 里有 jdk-17 profile，pom 已用 `maven-compiler-plugin` 显式锁 1.8 覆盖；不要在 IDE 里被 jdk-17 profile 带跑。
2. **Spring Security 默认拦截**：Spring Security 5.7 默认拦截所有请求，必须在 `security/SecurityConfig` 中放行 `/auth/login`、`/auth/captcha`、静态资源、Swagger，其余走 JWT；`JwtAuthenticationFilter` 解析失败统一返回 `Result.error(1004, "未登录/token过期")`（HTTP 401）。
3. **context-path /api/v1**：`server.servlet.context-path: /api/v1` 已统一加前缀，controller 类上 `@RequestMapping("/agents")` 即可，**不要**再写 `/api/v1/agents`，否则变成 `/api/v1/api/v1/agents`。
4. **jjwt 三件套同版本**：`jjwt-api` + `jjwt-impl`(runtime) + `jjwt-jackson`(runtime) 必须都是 0.11.5，否则运行期 `NoSuchMethodError`。0.11.x 用 `Jwts.parserBuilder()`（不是 `parser()`）、`Keys.hmacShaKeyFor()`、`signWith(Key, SignatureAlgorithm)`。
5. **SSE 接口**：`GET /sessions/{id}/messages/stream` 返回 `SseEmitter`，produces `text/event-stream`；注意该方法不经过 `Result` 包装，直接写事件流；需在 SecurityConfig 中单独放行或在 filter 中识别 `Accept: text/event-stream`。
6. **RESTful 路由（强制）**：不论 community 实际是否 RESTful，本项目一律标准 RESTful 对齐前端：`GET /agents`、`POST /agents`、`PUT /agents/{id}`、`DELETE /agents/{id}`、`PATCH /agents/{id}/status`，子资源 `/agents/{id}/config/model`、`/auth/login`。
7. **分页字段名**：用 `page` + `pageSize`（不是 community 的 `page`/`limit`，也不是 `pageNum`），响应 `total`/`list`（不是 community 的 `totalCount`/`currPage`/`totalPage`），以前端契约为准。
8. **code=0 成功**：与 community 的 `code=200` 相反，与前端 `if (data.code !== 0)` 严格对齐；写错会让前端把成功当失败。
9. **字段名 message**：统一 `message`，不是 community 的 `msg`。
10. **时间字段 createdAt/updatedAt**：不是 community 的 `createTime`/`updateTime`，与 `database-schema.sql`、前端字段约定一致。
11. **Result 放 common 包**：不放 `util`；`PageResult`、错误码常量、枚举同包，便于集中维护。
12. **@Valid 校验（增强项）**：community 不用 validation，本项目允许在 Form 上加 `javax.validation` 注解、controller 参数前补 `@Valid`，配合全局异常处理器把 `MethodArgumentNotValidException` 转成 `Result.error(1001, ...)`；但 controller 主体仍用 `Result` 返回，保持风格统一。
13. **@Log 日志注解**：命名 `@Log`（简短），放 `com.agentmanagement.annotation`，`@Retention(RUNTIME)` 必须加；切面 `LogAspect` 放 `com.agentmanagement.aspect`，落库到 `monitor.error_log` 或 `security.audit_log` 由 `module()` 字段决定。
14. **pom 资源打包**：`build/resources` 中同时包含 `src/main/java/**/*.xml`（Mapper XML 若跟 Java 放一起）和 `src/main/resources/**/*`；本项目 XML 默认放 `resources/mapper/`，前者可省略，保留更安全。
15. **全局异常处理（可选增强）**：如加 `@RestControllerAdvice`，放 `configuration` 或独立 `exception` 包，把业务异常统一转 `Result.error(...)`，但 controller 主体仍建议保留 community 的「方法内判断业务结果 → return Result」命令式风格，两层共存。