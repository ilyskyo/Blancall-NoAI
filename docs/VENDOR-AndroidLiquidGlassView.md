# 收录（vendored）AndroidLiquidGlassView 的记录

- 上游仓库：https://github.com/QmDeve/AndroidLiquidGlassView（公开）
- 收录版本：**tag `v1.0.5`**，commit `28ab7121f4f03fc78ba05a914fa3a6d59eb2c80a`（2026-05-31）
- 许可证：MIT（`LICENSE` 全文见仓库根 `THIRD-PARTY-LICENSES` 第 1 条；每个源码文件顶部也带上游 MIT 头）
- 收录原因：冷启动一次性 NPE，根因在上游源码里，而库的发布节奏不由我们控制。
  拿进来自己维护 = 能当场修、能在设备证伪、上游更新时手动 diff 即可（只 8 个文件）。

## 为什么不用 Maven 坐标而要搬源码

崩溃栈（2026-10-07 11:47:18，冷启动后首次拉起）：

```
java.lang.NullPointerException: Attempt to invoke virtual method
  'android.content.res.Resources android.view.View.getResources()' on a null object reference
    at com.qmdeve.liquidglass.impl.LiquidGlassimpl.<init>(LiquidGlassimpl.java:79)
    at com.qmdeve.liquidglass.LiquidGlass.init(LiquidGlass.java:105)
    at com.qmdeve.liquidglass.widget.LiquidGlassView.ensureGlass(LiquidGlassView.java:409)
    at ... LiquidGlassView.lambda$rebuild$... / android.os.Handler.dispatchMessage
```

`ensureGlass()` 是 `post()` 出去的延迟任务。`BottomNavBar` 里两个 `LiquidGlassView`
一个是玻璃条、一个是滑块，都在创建后才 `bind(source)`；如果这一帧之间 View 被
detach（冷启动首帧的组合顺序正好会造成），`customSource` 与 `getParent()` 同时为 null，
上游那句 `if (source == null && getParent() instanceof ViewGroup) return;` 不成立，
于是带着 null 采样源去 `new LiquidGlassimpl(this, null, config)`，
构造函数第 79 行 `target.getResources()` 直接空指针。

## 收录了哪些文件

`app/src/main/java/` 下 7 个 Java + `app/src/main/res/raw/` 下 1 个 AGSL，
包名沿用上游（`com.qmdeve.liquidglass.*`），这样以后对上游做 diff 是逐字对齐的：

| 文件 | 上游字节数 | 本项目改动 |
| --- | --- | --- |
| `com/qmdeve/liquidglass/Config.java` | 5169 | 无 |
| `com/qmdeve/liquidglass/LiquidGlass.java` | 5536 | 改动 2 |
| `com/qmdeve/liquidglass/impl/Impl.java` | 1635 | 无 |
| `com/qmdeve/liquidglass/impl/LiquidGlassimpl.java` | 10580 | 改动 3、4 |
| `com/qmdeve/liquidglass/util/LiquidTracker.java` | 6296 | 改动 5 |
| `com/qmdeve/liquidglass/util/Utils.java` | 2649 | 无 |
| `com/qmdeve/liquidglass/widget/LiquidGlassView.java` | 15208 | 改动 1 |
| `res/raw/liquidglass_effect.agsl` | 6471 | 无 |

上游的 `core/src/main/AndroidManifest.xml` 只声明库自己的 namespace，收录进 app 模块后
不需要它，故未搬。`demo/` 目录也不搬。

## 逐条改动（都以 `// Blancall：` 注释就地标注）

1. **`LiquidGlassView.ensureGlass()`：`if (source == null && getParent() instanceof ViewGroup)`
   → `if (source == null)`**。上游原条件只挡「有父 ViewGroup」这一种待绑源情形；
   已 detach（父为 null）时反而放行 null。改后两种「还没源」的情况都不放行 —— 语义是收紧，
   不是放宽：真正拿到源靠随后的 `bind(source)`。
2. **`LiquidGlass.init()`：构造 impl 的条件补 `&& target != null`**。同一处崩溃的第二道闸，
   保证无论从哪条路径进来都不可能拿 null 源建 impl（`record()` / `applyRenderEffect()`
   也全都直接解引用 `target`）。
3. **`LiquidGlassimpl`：`import com.qmdeve.liquidglass.R` → `import com.ilyskyo.blancall.R`**。
   AAR 时代 `R` 是库自己的；源码并入 app 模块后资源由 app 生成 `R`，不改包名就找不到符号。
   两版 `namespace` 都是 `com.ilyskyo.blancall`（`applicationId` 才区分 `.noai`），
   所以这一行在 Pro 与 NoAI 里完全一致。
4. **`LiquidGlassimpl.loadAgsl()`：删掉 `@Language("AGSL")` 及其 import**。
   那只是 IntelliJ 的字符串注入提示，来自 `org.jetbrains:annotations`，
   没有它功能不减，也就不必为一句提示新增一个依赖。
5. **`LiquidTracker`：`Math.clamp(...)` → 文件内自带的 `clamp(float, float, float)`**。
   `java.lang.Math.clamp` 是 Java 21 才有的，Android 静态库（本机 android-36.1 stubs 已核）不含，
   我们 `sourceCompatibility 17 / minSdk 26` 编不过。语义等价：`max(min, min(max, v))`。

依赖侧：`app/build.gradle.kts` 去掉 `com.qmdeve.liquidglass:core:1.0.5`，补上
`androidx.dynamicanimation:dynamicanimation:1.1.0`（`LiquidTracker` 的弹簧动画用）。
上游另一个依赖 `androidx.annotation` 本项目本来就通过 core-ktx 等间接拿到，
按仓库既有做法不重复声明。

## 以后怎么跟上游同步

```bash
B=https://raw.githubusercontent.com/QmDeve/AndroidLiquidGlassView/<新tag>/core/src/main
for f in Config.java LiquidGlass.java impl/Impl.java impl/LiquidGlassimpl.java \
         util/LiquidTracker.java util/Utils.java widget/LiquidGlassView.java; do
  curl -sSL --ssl-no-revoke -o "drop/$f" "$B/java/com/qmdeve/liquidglass/$f"
done
curl -sSL --ssl-no-revoke -o drop/liquidglass_effect.agsl "$B/res/raw/liquidglass_effect.agsl"
diff -ru drop app/src/main/java/com/qmdeve/liquidglass   # 期望只看到上面 5 条改动
```

拿到 diff 后逐条把 `// Blancall：` 的改动重新贴回去，别整目录覆盖 ——
那会把修复静默吃掉，下次冷启动照崩。
