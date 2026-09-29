# 更新日志 Changelog

本项目的所有重要变更都会记录在此文件中。
格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)（分类沿用其标准章节 Added / Changed / Fixed / Removed），版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [1.1.5] - 2026-09-29

### 新增 Added
- **HUD**：新增「Hide visuals」开关（默认开启），开启后 HUD 模块列表不再显示 `visuals` 分类模块，功能本身不受影响。
- **ClickGUI 搜索增强**：
  - 按类名索引的集中式搜索别名表（模块 `matchesSearch` 匹配显示名或任一别名）。
  - 支持显示名**首字母缩写**前缀匹配（如 `Kill Aura`→`ka`、`ChestESP`→`ce`）。
- **Auto Clicker V4**：
  - 接上原生的 ghost click 输入（Windows `SendInput`，JNA 经由游戏自带 launchwrapper 提供），非 Windows / JNA 不可用时自动降级为反射模拟。
  - 新增周期点击调度：预生成约 1 秒周期内的点击序列并锁定平均 CPS，节奏更稳。
  - 新增「Cycle scheduling」开关（默认开启），关闭后回退为逐次独立随机延迟。

### 变更 Changed

#### 性能
- **渲染通用**：`RenderUtils.drawBoundingBox` 由 6 次独立 GL 批处理合并为单次，ESP / Box 类渲染更省绘制指令。
- **实体系**：
  - `MobESP` / `HitBox`：渲染帧全列表 `loadedEntityList` 扫描改为按距离的区块级 AABB 查询，实体多时明显提速。
  - `DamageTags`：健康快照全量重建由每帧改为每秒约 2 次（过期清理仍逐帧保留，判定不受影响）。
  - `Radar`：玩家表三角函数与 AntiBot 判定改为 tick 内预计算缓存，渲染帧只画点。
- **HUD**：`HudRenderer` 模块列表每帧排序与 `getInfo()` 反复调用改为缓存条目（宽度只算一次、每帧约两次 `getInfo`），背景色分配提到循环外。
- **ClickGUI 搜索**：`getDisplayModules()` 在搜索词/分类不变时返回缓存列表，不再每帧重建 Stream 与 List。
- **Auto Clicker V4**：
  - 每帧 `new Context()` 改为复用单例字段；每次点击 `new RandomizationStrategy(...)` 改为按档位缓存 3 个固定实例。
  - 全局 `getInfo()` 缓存机制：文本只在相关设置变化时重算，消掉每帧 `String.format` 分配。

#### 代码风格与可读性
- **重构 `RenderUtils` 手写 GL**：
  - `glColor(int)` 拆分为具名 ARGB 通道；`drawRoundedGradientOutlinedRectangle` / `drawRoundedGradientRect` 抽成可读的圆角辅助函数，魔数改具名 GL 常量。
  - `drawPolygon` 紧凑参数与内部变量改为具名。
- **全库 GL 魔数 → 具名常量**：`GL_BLEND / GL_TEXTURE_2D / GL_DEPTH_TEST / GL_LINE_SMOOTH / GL_SMOOTH / GL_FLAT / GL_SRC_ALPHA / GL_ONE_MINUS_SRC_ALPHA` 等（覆盖 `RenderUtils`、`ScriptDefaults`、`BedESP`、`BlockOverlay`、`Nametags`、`TNTTimer`、`HitBox`、`MurderMystery`、`SimpleFontRenderer`、`MixinRenderEntityItem` 等）。
- **紧凑/单字母方法改名**：
  - `RotationUtils`：`i`→`getYawTo`、`angle`→`getYawTowards`、`deltaAngle`→`getRelativeYaw`、`getRotations(BlockPos, n, n2)`→`(blockPos, yaw, pitch)`。
  - `Utils`：`ae(float,float,float)`→`getMovementYawRadians(yaw, forward, strafe)`、`n()`→`getPlayerMovementYaw()`。
  - `MurderMystery.drawBox(n, n4..n7)`→`(color, x, y, z, distance)`。
  - `MixinRenderManager` mixin 参数→`(entity, partialTicks, includeStatic)`。
- **单字母方法语义澄清 + 重命名**：`MouseHelper.f()/i()` 处理鼠标**左/右键 CPS**，已改为 `getLeftCps()` / `getRightCps()`；ClickGui 组件内的 `ButtonComponent.i(x,y)`、`SliderComponent.u()/i()` 实为**坐标命中检测（与鼠标左右键无关）**，已改为 `isHovered(x,y)` / `isInLeftHalf(x,y)` / `isInRightHalf(x,y)`。
- **Auto Clicker V4 · 结构去重**：删除 `StarAutoClicker` 内部与同包独立类重复的 `enum State`（含从未被使用的 `AIMING` / `PAUSED`）与 `static class Context`，统一使用 `State.java`（`IDLE` / `BURST`）与 `Context.java`，并清理因此失效的 `import net.minecraft.util.BlockPos;`。

#### 元数据
- 作者标识统一为 `vliantwo`（`mcmod.info` / `LICENSE` / `README` / 脚本文档 URL）。
- 版本号由 `1.1.4` 提升至 `1.1.5`（`gradle.properties`）。

### 修复 Fixed
- **Auto Clicker V4 · `Context` 跨帧数据残留**：上一项「每帧 `new Context()` 改为复用单例字段」引入的副作用——未命中实体 / 方块时 `target`、`breakPos` 会保留上一帧的旧值。现为 `Context` 补 `reset()`（清空全部字段）并在 `collect()` 开头调用，保证每帧都是干净快照。

### 移除 Removed
- **派生鸣谢段落**：移除 README.md / LICENSE 末尾的派生鸣谢段落（「StarShack specific credits」）；GPLv3 正文与许可条款未作改动。
- **第三方出处注释**：实现思路类注释（如 Rounded 圆角着色器、各客户端比对注释）已精简；`src/main/java/starshack/module/impl/combat/autoclicker/CycleClickScheduler.java:7` 原引用的第三方实现思路注释亦已移除。至此代码层（所有 `.java`）已无第三方客户端品牌引用残留。
