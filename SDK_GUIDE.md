# Maingraph For MC (MGMC) 接入指南

本文档介绍第三方模组如何接入 MGMC 框架，包括注册自定义节点、事件节点以及使用最新的上下文机制。

---

## 1. 基础节点接入

基础节点包括 **动作节点 (Action)** 和 **数据节点 (Data)**等

### 注册普通节点
使用 `NodeHelper` 可以在 `RegisterMGMCNodesEvent` 事件中快速注册节点。

```java
@SubscribeEvent
public static void onRegister(RegisterMGMCNodesEvent event) {
    NodeHelper.setup("my_node", "node.modid.my_node.name")
        .category("node_category.mgmc.action")
        .color(NodeThemes.COLOR_NODE_ACTION)
        .execIn()
        .input("val", "node.port.val", PortType.FLOAT, 0xFFFFFFFF)
        .execOut()
        .registerExec((node, ctx) -> {
            // 执行逻辑
            NodeLogicRegistry.triggerExec(node, "exec", ctx);
        });
}
```

---

## 2. 事件节点接入

事件节点用于将 Minecraft 的 `Event` 转化为蓝图的起始触发点。

### 方式 A：标准事件接入
如果你注册的是常见的 Minecraft 事件（如 `BlockEvent`, `PlayerEvent` 等），可以直接使用旧版接口。MGMC 内置了默认的上下文提供者（`DefaultProvider`）来识别这些事件。

```java
NodeHelper.setup("on_break", "node.name")
    .registerEvent(BlockEvent.BreakEvent.class, 
        (event, builder) -> { /* 填充数据到 builder */ },
        event -> "routing_id",
        valueHandler
    );
```

### 方式 B：自定义/第三方事件接入
如果你需要接入 MGMC 不认识的第三方模组事件（例如机械动力的特殊事件），**必须**提供一个 `EventContextProvider`。否则，MGMC 将无法从该事件中提取 `Level` 和 `Player`，导致蓝图无法触发。

#### 步骤 1: 注册全局上下文提供者
在模组初始化阶段注册一次即可：
```java
ContextProviders.register(MyCustomEvent.class, new EventContextProvider<MyCustomEvent>() {
    @Override
    public Level getLevel(MyCustomEvent event) {
        return event.getLevel(); // 告诉 MGMC 如何拿 Level
    }

    @Override
    public Player getPlayer(MyCustomEvent event) {
        return event.getActor(); // 可选：告诉 MGMC 如何拿 Player
    }
});
```

#### 步骤 2: 注册节点
使用带 Provider 的重载方法：
```java
NodeHelper.setup("on_custom_event", "node.name")
    .registerEvent(MyCustomEvent.class, 
        ContextProviders.getProvider(MyCustomEvent.class), // 显式传入 Provider
        populator, 
        extractor, 
        valueHandler
    );
```
