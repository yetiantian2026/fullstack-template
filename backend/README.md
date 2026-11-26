# 后端工程

页面发出请求后，后端负责接收和处理，再把结果返回给前端。后端工程放在这里，固定使用 Spring Boot。本工程已完成初始化并接入数据库，项目流程和 Git 规范见[根 README](../README.md)。项目流程和 Git 规范见[根 README](../README.md)。

刚学 Java 或 SQL 时，可以先保存能独立运行的类、建表和查询练习，写明运行方式。学到框架后再整理到 Spring Boot 工程，不必先完成全部前端或 PR 练习。

## 先弄清要准备哪些工具

JDK 是 Java 开发工具包。后端工程需要兼容的 JDK 和构建工具，不能只凭教程中的版本号安装。先确定要使用的 Spring Boot 版本，再查看该版本的官方要求，亲自准备环境并记录实际版本。

构建工具负责管理依赖，执行编译、测试等操作。可以选择 Maven 或 Gradle，先了解基本用途再决定。项目所需的其他依赖和附加服务，按实际任务选择，不要求把教程提到的工具全部装上。

不清楚某个工具为什么需要，可以请 agent 解释，也可以让它帮你分析安装错误。基础工具安装和系统配置仍由你亲自操作。

## 自己创建工程，再检查是否可用

可以使用 Spring Initializr 生成工程。你需要自己选择选项并执行生成，不要求把生成器提供的初始文件全部手写一遍，也不能让 agent 替你初始化。

把生成的工程放到本目录，保留必要的 `README.md`、`AGENTS.md` 和忽略规则，工程文件直接位于这里。

创建后先确认能够构建，再启动服务。按照实际日志检查启动结果，不把“执行了命令”当作“已经启动”。环境没有准备好时，先处理环境问题，再继续验证。

## 接口要实际调用

接口是后端接受请求、返回结果的入口。服务启动成功后，还需要发出请求，检查状态和返回数据。接入前端时，要确认页面实际请求了这个服务。

已有测试要运行。编译成功、服务启动和接口行为符合预期，分别说明不同结果，不能互相代替。记录验证时写出实际做了什么；没有执行的检查说明原因，计划执行的命令不能算测试结果。

## 哪些文件要提交

提交源代码、构建配置和必要的项目文件。如果工程使用 Maven 或 Gradle 的 Wrapper，把所需脚本和配置一起提交。Wrapper 用于按项目约定获取并运行构建工具，不能只留下一份调用说明。

编译输出、本地缓存和运行日志不提交。真实密码、令牌及私钥留在本地。需要向别人说明配置时，提供占位示例，并解释每项配置的用途。示例里也不能夹带实际凭据。

## 工程建立后补全这份说明

写明实际语言、JDK、Spring Boot 和构建工具版本。提供依赖获取、启动、检查与构建方式，并说明命令在哪个目录执行。

把必要配置及其取得方式写清楚。使用额外服务时，说明启动后端前要怎样准备。随后给出可复现的请求示例，写明预期结果和实际验证情况。

仍未完成的行为单独说明。别人按照文档操作时，应能判断自己遇到的是环境问题、构建失败，还是接口结果不符合预期。

## 官方入口

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/index.html)
- [Spring Initializr 工程生成器](https://start.spring.io/)

## 实际运行说明

### 环境

| 项目 | 版本 |
|---|---|
| JDK | 25（Zulu 25.0.5） |
| Spring Boot | 4.2.0 |
| 构建工具 | Maven Wrapper（`mvnw`，无需单独安装 Maven） |
| 数据库 | MySQL 8.0 |

### 数据库准备

在仓库根目录的 `sql/` 下，按顺序执行：

1. `schema.sql` —— 创建 `campus_erp` 数据库与四张表
2. `seed.sql` —— 插入测试数据

`schema.sql` 开头有 `DROP TABLE IF EXISTS`，重复执行会清空数据。

### 配置

数据库口令通过环境变量注入，不写入仓库：

    $env:DB_PASSWORD = "你的 MySQL 口令"

对应 `src/main/resources/application.properties` 中的
`spring.datasource.password=${DB_PASSWORD}`。

### 启动

在本目录执行：

    .\mvnw.cmd spring-boot:run

启动成功的日志应包含：

    Started CampusErpApplication in x.xxx seconds

服务监听 `http://localhost:8080`。未配置的路由返回 404 页面，属正常现象。

### 测试

    .\mvnw.cmd test

当前测试覆盖：提交申请、批准金额超限、驳回后释放冻结、结算金额校验。

### 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/applications` | 提交申请 |
| GET | `/api/applications?projectId=` | 按项目查询申请列表 |
| GET | `/api/applications/{id}` | 查询单条申请 |
| POST | `/api/applications/{id}/approve` | 批准，可减额 |
| POST | `/api/applications/{id}/settle` | 结算实际花费 |
| POST | `/api/applications/{id}/reject` | 驳回 |
| POST | `/api/applications/{id}/cancel` | 取消 |
| GET | `/api/projects/{id}/summary` | 项目经费摘要 |

失败时统一返回 HTTP 400 与 `{"error": "原因"}`。

### 尚未完成

- 未接入登录，申请人 ID 由请求参数给出，后端未校验"只能操作本人申请"
- 并发申请同一笔预算未加锁处理
