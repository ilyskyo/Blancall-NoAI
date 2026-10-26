# 底部导航栏液态玻璃参数与实现说明

本文对应 `BottomNavBar.kt` / `NavLiquidPhysics.kt` / `NavLiquidGlass.kt` 与
`AppNavigation.kt` 的 `pageBackdrop` 接线。只服务底部导航栏；阅读模式与弹窗玻璃仍走旧的
`com.qmdeve.liquidglass`，两者互不影响。

## 结构

1. `AppNavigation` 给页面内容加 `Modifier.layerBackdrop(pageBackdrop)`，把整页录进
   Compose `GraphicsLayer`（仅在根 tab 页、玻璃开启、底栏实际可见时启用）。
2. 底栏玻璃 `drawBackdrop(pageBackdrop, ...)` 只重录栏区，折射真实页面。
3. 底栏玻璃通过 `exportedBackdrop = barBackdrop` 输出自己的玻璃层。
4. 指示器 `drawBackdrop(combined(pageBackdrop, barBackdrop), ...)` 折射「页面 + 底栏玻璃」。
5. tab 图标/文字在玻璃之上，永不参与玻璃缩放。

## 物理参数（`NavLiquidPhysics`）

| 量 | 参数 | 说明 |
|---|---|---|
| 位置 | spring(damping=1.0, stiffness=1000, threshold=0.001) | 临界阻尼，只追不弹；手指只改 target |
| 速度 | spring(0.5, 300, 0.005) | 独立欠阻尼弹簧，松手后衰减振荡 |
| 按压进度 | spring(1.0, 1000, 0.001) | 控制 lens / 高光 / 内外阴影 |
| scaleX | spring(0.6, 250, 0.001) | 按压放大 78/56≈1.393 |
| scaleY | spring(0.7, 250, 0.001) | 与 scaleX 用不同阻尼，避免机械同步 |
| 整栏位移 | 4dp × sign(f) × easeOutCubic(|f|) | f = 拖动像素 / 栏宽，夹在 ±1 |
| 松手回正 | spring(1.0, 300, 0.5) | 整栏位移回 0 |
| 拉伸 | s = clamp(v / 10, ±0.2) | scaleX /= 1 - s×0.75；scaleY *= 1 - s×0.25 |
| 落格 | round(target) 后夹在 0..n-1 | 拖动超过半格即切到下一格 |
| 帧积分 | 1/240s 子步，单帧最多 64ms | 高刚度下不炸、帧率无关 |

所有暴露值都是 Compose `State`，UI 只在 `graphicsLayer` / `draw` / `effects` 的 lambda 里读，
因此拖动期间只失效绘制层，不触发重组。

## 渲染参数

| 项 | 值 | 调法 |
|---|---|---|
| 栏体模糊 | 8dp（省电模式 4dp） | 调大更磨砂，但文字对比下降 |
| 栏体 lens | 24dp / 24dp（省电 16dp） | 调大边缘折射更强，过大像果冻 |
| 指示器 lens | 10dp × press / 14dp × press | 只有按住时才出现，色散随 press |
| vibrancy | 饱和 1.18 + 对比 1.06（API 31+） | `NavLiquidGlass.kt` 的 ColorMatrix |
| 栏体表面染色 | 浅色主题 background α0.42 / 深色黑 α0.42 | 深色保持纯黑观感 |
| 指示器表面 | 浅色黑 α0.10 / 深色白 α0.10 | 静止时靠它区分胶囊 |
| 栏体描边 | 浅色黑 α0.08 / 深色白 α0.20 | 纯白页上白描边不可见，故浅色用暗细线 |
| 指示器描边 | 浅色白 α0.80 / 深色白 α0.45 | 高光边 |
| 栏体阴影 | 14dp，ambient 0.20 / spot 0.26 | 悬浮感 |
| 指示器阴影 | 12dp，alpha 0.18 + 0.50×press | 按压时才明显，避免灰雾 |
| 指示器内阴影 | 8dp × press，alpha = press | 按压收边 |
| 手指高光 | 径向白 0.16×press + 整体 0.06×press | `BlendMode.Plus` |

## 降级

- API 33+：vibrancy + blur + lens + 色散 + 高光/阴影全量。
- API 31–32：有 blur，无 lens（RuntimeShader 不可用）；物理完整。
- API 26–30：不做 `drawBackdrop`，回退半透明染色 + 描边 + 阴影 + 高光；物理完整。
- 省电模式：blur 8→4dp、lens 24→16dp；动画参数不变。
- 系统「减少动态效果」：位置直接到位，去掉速度拉伸与超调。

## 交互

- 整条栏的手势层接管点击与水平拖动；垂直方向只取消、不提交切页。
- 点击 tab / 拖动松手都走 `press → 位置滑动 → 松手回弹`。
- 拖动松手后若位置还没到位，保持按压态，等滑到位再缩回（`pendingClickRelease`）。
- 无障碍：每个 tab 仍是 `clickable + Role.Tab + Selected`，名称只来自子树 Text。

## 依赖与许可

`io.github.kyant0:backdrop:1.0.0`（Apache-2.0），许可与来源见根目录
`THIRD-PARTY-LICENSES` 第 2 条。物理参数与分层结构参考其 Catalog 的
`DampedDragAnimation` / `LiquidBottomTabs`，本项目实现为 `NavLiquidPhysics`。

## 验收状态

- [x] 物理层单测：拖动一格、超调 ≤8%、位移 ≤4dp、拉伸有界并衰减、按压回位、减少动态。
- [x] 双版本编译、单元测试、无障碍测试编译。
- [ ] 真机 120Hz 连续拖动 10 秒无掉帧（需设备录屏确认）。
- [ ] 真机前后对比素材（模拟器 SystemUI 不稳定，未能作为有效验收）。
