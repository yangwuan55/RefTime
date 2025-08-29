# 执行计划：添加JVM demo并用Compose统一所有demo

## 需求分析

### 当前项目结构
- **:app** - Android示例应用，使用Compose UI
- **:desktop-demo** - 桌面JVM demo，使用Compose Desktop
- **:library** - 核心时间同步库，包含共享的Compose UI组件

### 现有demo分析
1. **Android demo** (`:app`): 完整的Android应用，使用Jetpack Compose
2. **Desktop demo** (`:desktop-demo`): 简单的桌面应用，使用Compose Desktop
3. **共享UI组件**: `SharedDemo.kt` 包含可复用的Compose组件

### 问题识别
- 桌面demo功能相对简单
- 缺乏统一的demo管理界面
- 各demo之间没有统一的用户体验

## 方案设计

### 目标
1. 增强JVM demo功能，使其与Android demo功能对等
2. 创建统一的demo选择界面
3. 使用Compose实现跨平台一致的UI体验

### 技术方案
1. **扩展桌面demo**: 使用现有的共享UI组件增强功能
2. **创建统一启动器**: 开发一个demo选择器界面
3. **模块化架构**: 保持各demo独立但共享核心UI组件

## 详细执行步骤

### 步骤1: 增强桌面demo功能
- **文件**: `desktop-demo/src/main/kotlin/com/milo/desktop/Main.kt`
- **操作**: 扩展桌面demo，使用完整的`RefTimeApp`组件
- **预期结果**: 桌面demo具有与Android demo相同的功能

### 步骤2: 创建统一demo启动器
- **文件**: 新建 `jvm-demo/src/main/kotlin/com/milo/demo/App.kt`
- **操作**: 开发一个Compose界面，提供demo选择功能
- **预期结果**: 用户可以选择运行Android demo或Desktop demo

### 步骤3: 配置构建系统
- **文件**: `settings.gradle.kts` 和新的构建配置文件
- **操作**: 添加新的JVM demo模块配置
- **预期结果**: 项目支持新的demo模块构建

### 步骤4: 测试验证
- **操作**: 运行各demo，确保功能正常
- **预期结果**: 所有demo都能正常运行，UI一致

## 文件结构变更

```
truetime-android/
├── app/                    # Android demo (保持不变)
├── desktop-demo/           # 增强的桌面demo
├── jvm-demo/              # 新的统一JVM demo启动器
│   └── src/main/kotlin/com/milo/demo/
│       ├── App.kt         # 统一demo选择器
│       └── DemoLauncher.kt # demo启动逻辑
└── library/               # 核心库 (保持不变)
```

## 技术约束
- 使用现有Compose组件，避免重复开发
- 保持跨平台兼容性
- 遵循项目现有的代码规范

## 成功标准
- ✅ JVM demo功能与Android demo对等
- ✅ 统一的demo选择界面
- ✅ 所有demo使用相同的Compose UI组件
- ✅ 构建系统配置正确
- ✅ 功能测试通过

## 风险评估
- 低风险：基于现有成熟组件开发
- 中风险：跨平台兼容性需要测试验证