# Feature 规划：任务选择器增加玩家任务状态页签

## 1. 目标

扩展现有管理员坐标记录仪的 `GuiQuestPicker`，提供三个任务视图：

1. **全部任务**
2. **进行中**
3. **已完成**

任务选择后的行为保持不变：返回 `GuiCoordinateRecorder`，继续编辑所选任务的坐标点。

## 2. 已确认需求

- 玩家范围：当前打开 GUI 的操作玩家。
- UI 形式：三个页签。
- 已完成与进行中互斥：如果可重复任务以前完成过、目前又重新接取，只显示在“进行中”。
- 状态筛选后，左侧自动隐藏没有匹配任务的分类。
- 保留现有分类浏览、当前分类内搜索、标题排序、逐条滚动、长标题截断以及任务选择回调。

## 3. 状态定义

### 3.1 全部任务

继续使用现有全局任务目录：

```java
QuestController.Instance.categories
```

保持显示所有分类和分类内全部任务的现有行为。

### 3.2 进行中

读取当前玩家的 CustomNPC+ 任务数据：

```java
PlayerData.get(mc.thePlayer).questData.getActiveQuests()
```

“进行中”包含全部 active 任务，包括：

- 目标尚未完成的任务；
- 目标已完成、但仍等待向 NPC 交付的任务。

v1 不再细分“进行中”和“可交付”。

### 3.3 已完成

读取：

```java
PlayerData.get(mc.thePlayer).questData.getFinishedQuests()
```

并显式排除当前 active 的任务：

```text
finished && !active
```

因此可重复任务再次接取时采用 `active > finished` 的优先级。

## 4. v1 非目标

- 不显示或按完成时间排序；CustomNPC+ 的完成记录混用现实时间毫秒与世界 tick。
- 不增加“可交付”独立页签。
- 不展示重复任务冷却或能否再次接取。
- 不查看其他玩家状态。
- 不专门实现 Party 任务或 Profile cooldown/可重接判定。注意：CustomNPC+ 会把 Profile shared completion 合并进 `finishedQuests`，因此“已完成”页签会遵循该上游实际语义，v1 不尝试区分本地完成与 Profile shared completion。
- 不新增 Container。
- 默认不新增 Quest Pointer 自有任务状态同步包。

## 5. 架构决策

### 5.1 默认复用 CustomNPC+ 客户端玩家缓存

本功能的数据仅用于客户端展示和筛选，不参与任务授权、发奖、任务状态修改或坐标点写入权限判定。CustomNPC+ 已经同步当前玩家的 `activeQuests` 和 `finishedQuests` 到客户端，因此 v1 直接读取当前玩家客户端 `PlayerData`。

建议在 GUI 内维护 ID 快照，而不是长期持有 CustomNPC+ 的可变 map：

```java
private QuestView selectedView = QuestView.ALL;
private Set<Integer> activeQuestIds = new HashSet<Integer>();
private Set<Integer> finishedQuestIds = new HashSet<Integer>();
```

页签枚举可作为 `GuiQuestPicker` 的内部枚举：

```java
private enum QuestView {
    ALL,
    ACTIVE,
    FINISHED
}
```

读取玩家状态只应出现在客户端类中，避免 dedicated server 加载 `Minecraft` 或其他客户端实现类。

### 5.2 网络回退方案

若实机验证发现刚接受或完成任务后，CustomNPC+ 客户端缓存存在不可接受的陈旧状态，再考虑新增：

```text
RequestPlayerQuestStatePacket C2S
SyncPlayerQuestStatePacket S2C
```

包只传 active/finished quest ID，服务端从 `PlayerData.get(player)` 构建快照。该方案是回退，不属于默认 v1 范围。

## 6. 文件级实施步骤

### 6.1 修改任务选择器

文件：

```text
src/main/java/com/github/postyizhan/questpointer/client/gui/GuiQuestPicker.java
```

#### A. 增加页签和任务状态快照

- 增加 `QuestView`。
- 增加当前页签字段。
- 增加 active/finished quest ID 集合。
- 增加不与现有按钮冲突的页签按钮 ID，例如 `1000`、`1001`、`1002`。

#### B. 读取当前玩家任务状态

增加 `refreshPlayerQuestState()`：

1. 清空 active/finished 快照；
2. 安全检查 `mc`、`mc.thePlayer`、`PlayerData.get(mc.thePlayer)` 和 `questData`；
3. 调用 `getActiveQuests()` / `getFinishedQuests()`；
4. 将每个 `IQuest.getId()` 复制进本地 `Set<Integer>`。

刷新时机：

- `initGui()`；
- 切换到“进行中”；
- 切换到“已完成”。

v1 不要求每 tick 刷新。

#### C. 集中定义视图匹配规则

增加 `matchesSelectedView(Quest quest)`：

```text
ALL:      true
ACTIVE:   activeQuestIds.contains(quest.id)
FINISHED: finishedQuestIds.contains(quest.id)
          && !activeQuestIds.contains(quest.id)
```

左侧分类和右侧任务列表必须使用同一个匹配规则。

#### D. 根据状态过滤分类

新增或重构 `refreshCategoryList()`：

- `ALL`：显示所有分类；
- `ACTIVE` / `FINISHED`：仅显示至少含一个匹配任务的分类；
- 分类按标题不区分大小写排序；
- 搜索词不影响左侧分类是否存在，搜索仍只过滤右侧当前分类任务。

#### E. 处理当前分类失效和滚动越界

`QuestCategory` 对象可能在 CustomNPC+ 同步时被整体替换，因此选择状态必须以稳定的 `selectedCategory.id` 判断，而不是对象身份。每次重建分类列表后，应按原分类 ID 在新列表中重新绑定 `selectedCategory`；找不到才清空。

切换视图或刷新状态后的顺序：

1. 保存原 `selectedCategory.id`（若存在）；
2. 重建分类列表；
3. 按 ID 重新绑定 `selectedCategory`，找不到则清空；
4. 重建右侧任务列表；
5. 使用实际可见行数夹紧滚动：`offset = min(max(0, offset), max(0, size - rowsVisible))`；
6. 更新按钮，不得留下旧分类对应的旧任务。

搜索文本变化可以继续将 `questScroll` 重置为 0。v1 不强制自动选中第一个分类，以减少现有交互变化。

#### F. 调整右侧任务过滤

`refreshQuestList()` 改为：

```text
当前分类全部任务
→ 当前页签状态过滤
→ 搜索过滤
→ 标题排序
```

搜索统一使用 `Locale.ROOT` 进行大小写规范化，避免默认系统 Locale 导致异常。

#### G. 页签交互

点击页签时：

1. 忽略对当前页签的重复切换；
2. 更新 `selectedView`；
3. 对 ACTIVE/FINISHED 刷新玩家状态；
4. 重建分类集合；
5. 校验当前分类；
6. 重建任务列表；
7. 修正滚动位置；
8. 更新行按钮和页签视觉状态。

当前页签按钮可设置 `enabled = false` 作为选中状态。

#### H. 布局与空状态

页签占用固定顶部区域；列表和底部控件应按 `height` 自适应计算，不再假设任何窗口高度都能固定显示 8 行。推荐以底部取消按钮和翻页行的非重叠边界反推可用列表高度：

```text
rowsVisible = max(1, min(8, availableListHeight / ROW_HEIGHT))
```

所有行按钮创建、列表绘制、翻页条件和 offset 上界必须使用实际 `rowsVisible`。确保：

- 页签不与分类标题、搜索框重叠；
- 翻页按钮和取消按钮各占独立且不重叠的区域；
- 常见 240 高度与 GUI scale/窗口 resize 后仍可操作；
- `initGui()` 重入时保留 `selectedView`、搜索、分类 ID，并只重建控件和夹紧滚动；只有新建 picker 默认 ALL。

空状态判定优先级：

1. `categories.isEmpty()`：ACTIVE/FINISHED 显示对应状态空文案；ALL 显示通用无任务文案；
2. `selectedCategory == null`：显示“请先选择一个分类”；
3. `quests.isEmpty() && !search.isEmpty()`：显示“没有匹配的任务”；
4. 已选分类、无搜索且任务为空：显示通用无任务文案（正常情况下状态分类过滤会避免此状态，但需安全处理异步目录变化）。

### 6.2 国际化任务选择器

文件：

```text
src/main/resources/assets/questpointer/lang/zh_CN.lang
src/main/resources/assets/questpointer/lang/en_US.lang
```

建议键：

```properties
questpointer.gui.questPicker.title=
questpointer.gui.questPicker.view.all=
questpointer.gui.questPicker.view.active=
questpointer.gui.questPicker.view.finished=
questpointer.gui.questPicker.category=
questpointer.gui.questPicker.search=
questpointer.gui.questPicker.cancel=
questpointer.gui.questPicker.selectCategory=
questpointer.gui.questPicker.empty.active=
questpointer.gui.questPicker.empty.finished=
questpointer.gui.questPicker.empty.search=
questpointer.gui.questPicker.empty.general=
```

中文：

```properties
questpointer.gui.questPicker.title=选择任务
questpointer.gui.questPicker.view.all=全部任务
questpointer.gui.questPicker.view.active=进行中
questpointer.gui.questPicker.view.finished=已完成
questpointer.gui.questPicker.category=分类
questpointer.gui.questPicker.search=任务（可搜索）
questpointer.gui.questPicker.cancel=取消
questpointer.gui.questPicker.selectCategory=请先选择一个分类
questpointer.gui.questPicker.empty.active=暂无进行中的任务
questpointer.gui.questPicker.empty.finished=暂无已完成的任务
questpointer.gui.questPicker.empty.search=没有匹配的任务
questpointer.gui.questPicker.empty.general=暂无任务
```

英文：

```properties
questpointer.gui.questPicker.title=Select Quest
questpointer.gui.questPicker.view.all=All Quests
questpointer.gui.questPicker.view.active=In Progress
questpointer.gui.questPicker.view.finished=Completed
questpointer.gui.questPicker.category=Category
questpointer.gui.questPicker.search=Quests (Searchable)
questpointer.gui.questPicker.cancel=Cancel
questpointer.gui.questPicker.selectCategory=Select a category
questpointer.gui.questPicker.empty.active=No quests in progress
questpointer.gui.questPicker.empty.finished=No completed quests
questpointer.gui.questPicker.empty.search=No matching quests
questpointer.gui.questPicker.empty.general=No quests
```

Java 使用 `StatCollector.translateToLocal(...)`。本次至少迁移 `GuiQuestPicker` 中现有硬编码文案，不强制扩大到整个 `GuiCoordinateRecorder`。

## 7. UI 状态机

```text
打开 GuiQuestPicker
  → 刷新当前玩家 active/finished ID 快照
  → 默认选择 ALL
  → 显示全部分类
```

```text
切换 ACTIVE / FINISHED
  → 刷新玩家状态快照
  → 按当前视图过滤分类
  → 校验 selectedCategory
  → 刷新右侧任务
  → 重置/夹紧滚动位置
  → 更新按钮与空状态
```

```text
选择任务
  → parent.onQuestSelected(quest)
  → 返回 GuiCoordinateRecorder
  → 沿用 RecorderRequestPointsPacket 请求坐标点
```

## 8. 权限和线程边界

- 客户端任务状态只用于展示，不得作为任何服务端权限依据。
- 坐标点读取、增加、删除、重命名继续由现有服务端 packet handler 校验权限。
- 不调用 `startQuest`、`finishQuest`、`stopQuest`、`removeQuest`。
- 不在 common/server 类中引用客户端类。
- 从 CustomNPC+ 可变玩家数据读取后立即复制成 GUI 自己的 ID 快照，避免渲染期间遍历外部 mutable map。

## 9. 验收标准

### 全部任务

- 与改动前行为一致。
- 显示全部分类及其全部任务。
- 搜索、排序、滚动和选择任务正常。

### 进行中

- 只显示当前玩家 active 任务。
- 目标已完成但未交付的 active 任务仍显示。
- 不含匹配任务的分类不显示。
- 完全为空时显示明确空状态。

### 已完成

- 只显示当前玩家 finished 任务。
- 同时 active 的任务被排除。
- 不含匹配任务的分类不显示。
- 完全为空时显示明确空状态。

### 通用交互

- 切换视图后分类和任务滚动位置不越界。
- 原分类失效后不会残留旧任务。
- 搜索仅作用于当前分类右栏，不改变左侧分类集合。
- 长标题仍正确省略。
- 任意页签选择任务后都能正常返回并编辑坐标点。
- 中英文文案正确显示。

## 10. 验证计划

自动检查：

```bash
./gradlew.bat test
./gradlew.bat spotlessCheck
./gradlew.bat checkstyleMain
./gradlew.bat build
```

优先把视图匹配、分类 ID 重绑和 offset clamp 提取为不依赖渲染上下文的 package-private/static 逻辑并补充单元测试；若受旧版 Forge/Gradle 测试环境限制无法合理新增测试，必须在实现报告中明确列为残余风险，并用实机矩阵覆盖这些分支，不能把空跑 `test` 当作充分证据。

实机矩阵：

1. 无进行中、无已完成任务；
2. 只有进行中任务；
3. 只有已完成任务；
4. 两类任务分布在多个分类；
5. 空分类在状态页签中隐藏；
6. 切页签后原分类失效；
7. 搜索框有文本时切换页签；
8. 超过 8 个分类或任务时滚动；
9. 长分类名和长任务名；
10. 完成 active 任务后重开 GUI，任务从进行中移到已完成；
11. 可重复任务完成后重新接取，只出现在进行中；
12. 从三个页签分别选择任务并增删改坐标点；
13. Dedicated Server 无客户端类加载崩溃；
14. 中英文语言切换；
15. 在 ACTIVE/FINISHED 页签改变窗口大小或 GUI Scale，确认页签状态、搜索、分类 ID 保留且控件不重叠。

## 11. 预计改动范围

主要修改：

```text
src/main/java/com/github/postyizhan/questpointer/client/gui/GuiQuestPicker.java
src/main/resources/assets/questpointer/lang/zh_CN.lang
src/main/resources/assets/questpointer/lang/en_US.lang
```

默认不修改：

```text
src/main/java/com/github/postyizhan/questpointer/network/NetworkHandler.java
src/main/java/com/github/postyizhan/questpointer/client/gui/GuiCoordinateRecorder.java
src/main/java/com/github/postyizhan/questpointer/quest/QuestTrackingTicker.java
src/main/java/com/github/postyizhan/questpointer/client/QuestPointerClientData.java
```

## 12. 调查资料

详细代码调查产物：

```text
.pi-subagents/artifacts/outputs/2f956a0b-75ae-4aab-8ce4-a7d5b0bb2e17/planning/gui-current-all-quests.md
.pi-subagents/artifacts/outputs/2f956a0b-75ae-4aab-8ce4-a7d5b0bb2e17/planning/player-quest-state.md
```
