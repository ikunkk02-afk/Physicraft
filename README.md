# Physicraft

NeoForge 1.21.1 模组：用**物理转换器**把实心方块变成刚体。没被转换的原版世界保持原样。

Physicraft is a NeoForge 1.21.1 mod. The **Physics Converter** turns one solid block into a rigid body. The rest of the vanilla world stays static.

物理引擎是内嵌的 [Libbulletjme](https://github.com/stephengold/Libbulletjme) 23.2.0（Bullet，JNI）。不依赖 Rayon、DynamX、Velthoric 或其他物理模组。

The physics engine is embedded [Libbulletjme](https://github.com/stephengold/Libbulletjme) 23.2.0 (Bullet via JNI). It does not depend on Rayon, DynamX, Velthoric, or any other physics mod.

## 环境 / Requirements

- JDK 21（模板使用的版本；Gradle 会通过 toolchain 查找）
- 构建和启动客户端需要能访问 Maven Central 以及 NeoForge / Minecraft 的依赖源

## 构建 / Build

```bash
./gradlew build
```

产物在 `build/libs/physicraft-0.1.0.jar`。这个 jar 会打进 Libbulletjme 的 Java 类，以及 Windows / Linux / macOS（x64 与 ARM64）的 `SpRelease` 原生库。

`./gradlew test` 会加载本机原生库，做一次下落、刮风和撞击冲量的无界面检查。`build` 已包含测试。

## 运行客户端 / runClient

```bash
./gradlew runClient
```

第一次会下载游戏资源，时间比较长。进入世界后：

1. 创造模式打开 **Physicraft** 创造标签，或工具标签，拿出**物理转换器**。生存模式可以用：

   ```
   /give @s physicraft:physics_converter
   ```

2. 对方块**右键**（泥土、石头、木板这类完整实心方块）。方块会从世界里消失，并变成一个会掉落的刚体。含方块实体的方块（箱子等）、没有完整碰撞箱的方块、基岩都不会被转换。
3. 再对飞行中的物理方块**右键**，如果它所在的那一格能被替换，就会还原成原来的方块。
4. 默认全局风沿 **+X（东）** 推，加速度 6 方块/秒²。方块下落时会向东偏，落地后也可能被风慢慢推着滑。
5. 把两个物理方块叠出落差再让上面的砸下去。冲量超过阈值时，较软（或同样硬度里更快）的那一个会碎掉，并按原版战利品掉落。泥土从大约 8 格高砸到另一块泥土上，应该能打碎。

## 配置 / Config

游戏目录里的 `config/physicraft-common.toml`，也可以在模组列表里打开配置界面。

| 键 | 默认 | 作用 |
| --- | --- | --- |
| `gravity` | 20 | 向下加速度，方块/秒²。原版掉落大约是 16 |
| `windX` / `windY` / `windZ` | 6 / 0 / 0 | 全局风加速度。全设为 0 就没有风 |
| `friction` / `terrainFriction` | 0.55 / 0.9 | Bullet 会把两个摩擦值相乘 |
| `breakImpulse` | 4 | 破坏基础冲量。实际阈值 = `breakImpulse * (0.35 + 硬度)` |
| `minImpactSpeed` | 3.5 | 相对速度低于这个值（方块/秒）不会因为挤在一起而碎 |
| `breakDropsItem` | true | 撞击破坏时是否掉落 |
| `terrainRadius` | 3 | 每个刚体周围生成地形碰撞盒的半径 |
| `maxBodies` | 256 | 单个维度的刚体上限 |

改完配置后下一 tick 就会生效，不必重启。已经存在的方块也会在下一 tick 套上新的摩擦、阻尼、重力和风。

## 这个 MVP 做了什么

- 服务器模拟，客户端只渲染同步过来的位置和四元数。单人游戏里模拟跑在集成服务器上。
- 刚体有重力、和附近地形以及其他刚体的碰撞、摩擦、阻尼，以及全局风。
- 地形碰撞是刚体周围实心方块的**单位立方体**近似。楼梯、栅栏只要有碰撞箱，也会被当成整格方块。
- 硬碰硬只发生在两个物理方块之间。撞到普通地形不会把方块撞碎。
- 新生刚体有 5 tick 的撞击保护，避免刚转换时和邻居的接触被当成撞击。

## 还没做 / Not in this MVP

- 玩家和生物不会挡住物理方块，也不会站在上面。
- 没有浮力、软体、载具、传送枪。
- 风目前只有配置里的全局向量。`WindField.addSampler` 留作以后加风力方块。
- 原生库是单精度 `Sp`。离世界原点很远时，位置会开始发抖。
- 已打包的平台：Linux x64、Linux ARM64、Windows x64、Windows ARM64、macOS x64、macOS ARM64。其他平台会在日志里报加载失败，转换器不会拆掉方块。
- Linux 上 `libbulletjme.so` 还需要系统里的 `libstdc++.so.6` 和 `libgcc_s.so.1`（一般的桌面发行版都有）。

## 许可 / License

模组源码是 MIT（见 `LICENSE`）。Libbulletjme / Bullet 的许可证在 `THIRD_PARTY_NOTICES.md`。NeoForge MDK 模板见 `TEMPLATE_LICENSE.txt`。
