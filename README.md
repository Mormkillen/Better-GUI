# Better GUI

Turns the player inventory and every container GUI in Minecraft into a translucent acrylic (frosted glass) style: the panel has a real blur behind it, and the color, opacity and corner radius are all customizable.

- Game version: **Minecraft 1.20.1**
- Loader: **Fabric Loader 0.14.21+**
- Side: **Client only** (nothing to install on the server)
- Optional dependency: **Mod Menu** (only needed for the in-game config screen; without it you can edit the config file by hand)
- Author: mormkillen

## Features

- **Translucent acrylic panel** — real blur behind the panel (it reuses vanilla 1.20.1's post-processing blur program); the panel itself is a translucent base with rounded corners, a light border and a soft shadow
- **Adjustable blur strength** — slider from 0 to 100%, where 0 turns blur off and keeps only the translucency
- **Translucency master switch** — turn it off and the whole GUI becomes a solid panel in the color you picked
- **Custom GUI color** — a HEX field (ARGB) plus a swatch button that opens a full color picker (SV square, hue strip, H/S/V/R/G/B/A sliders, checkerboard preview, HEX input)
- **Flowing rainbow** — the panel becomes a scrolling rainbow that flows from the bottom-left corner toward the top-right corner
- **Gradient** — give each of the four corners its own color and the panel interpolates bilinearly between them
- **Translucent slots** — slots keep only their border so the background shows through; turn it off to get vanilla opaque slots back
- **Label recolor** — the dark gray GUI titles are drawn in a light color so they stay readable on dark glass
- **Localization** — UI text follows the game language; Simplified Chinese, English, Russian and French are included
- **Recipe book panel** — the panel behind the recipe book gets the same acrylic treatment, positioned from vanilla's own internal fields so it never drifts out of place

## Where it applies

| Screen | Handled |
| --- | --- |
| Player inventory (2x2 crafting, armor, offhand) | ✅ |
| Chests / double chests / barrels / ender chests | ✅ |
| Shulker boxes | ✅ |
| Hoppers | ✅ |
| Dispensers / droppers | ✅ |
| Panel behind an open recipe book | ✅ |
| Furnaces, crafting tables, anvils, enchanting tables and other containers | ✅ (`applyToAllContainers`, on by default) |
| Creative inventory | ❌ kept vanilla |

## Installation

1. Install Fabric Loader 0.14.21 or newer for Minecraft 1.20.1
2. Drop `bettergui-1.0.0.jar` into `.minecraft/mods`
3. Launch the game

## In-game configuration

With Mod Menu installed: **Mods → Better GUI → Configure**. The screen is split in two, with the settings on the left and a live preview on the right:

```
Translucency: on/off        enables the translucency and the blur; when off the panel is solid
Blur: XX%                   0% turns blur off
Translucent slots: on/off   slots keep only their border, so the background shows through
———————— Color ————————
GUI color   [HEX] [swatch] [reset]      click the swatch for the full color picker
Flowing rainbow: on/off     the color scrolls across the panel (mutually exclusive with Gradient)
Gradient: on/off            the panel color is built from the four corner colors
Top left / Top right / Bottom left / Bottom right   [HEX] [swatch]
Done                        saves and goes back
```

Colors use the **ARGB** format (`#AARRGGBB`); `#3AFFFFFF`, for example, is white at about 23% opacity. Every change shows up in the preview immediately, and the config file is written when the screen closes.

## Config file

Location: `.minecraft/config/better-gui.json` (created on first launch). Besides the options exposed in the UI, these can be edited by hand:

| Option | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Master switch |
| `applyToAllContainers` | `true` | Whether furnaces, crafting tables and other containers are handled too |
| `translucent` | `true` | Translucency master switch |
| `blurRadius` | `0.0` | Blur strength, 0 = off; values around 6-24 work well |
| `blurFlipVertical` | `true` | Vertical orientation of the blurred texture; normally left alone |
| `cornerRadius` | `6` | Panel corner radius in pixels |
| `rainbow` / `rainbowSpeed` | `false` / `1.0` | Flowing rainbow switch and speed (2.0 is twice as fast) |
| `gradient` | `false` | Gradient mode switch |
| `cornerTopLeft` / `cornerTopRight` | `0x3AFFFFFF` | Gradient colors: top left / top right |
| `cornerBottomLeft` / `cornerBottomRight` | `0x1CFFFFFF` | Gradient colors: bottom left / bottom right |
| `panelTopColor` / `panelBottomColor` | `0x3AFFFFFF` / `0x1CFFFFFF` | GUI color in normal mode (the two can differ to form a gradient) |
| `borderColor` | `0x33FFFFFF` | Panel border color |
| `shadow` / `shadowColor` | `true` / `0x40000000` | Shadow around the panel |
| `dimTopColor` / `dimBottomColor` | `0x28000000` / `0x40000000` | How much the world is dimmed while a screen is open |
| `panelAlpha` | `0` | Opacity of vanilla's panel base (#C6C6C6); 0 leaves the whole panel to this mod |
| `slotAlpha` / `slotBorderAlpha` / `highlightAlpha` | `0` / `110` / `90` | Opacity of the slot fill, dark edge and highlight |
| `darkAlpha` | `0` | Opacity of vanilla's dark frame; 0 removes the frame |
| `frameThickness` | `4` | Width in pixels of the texture frame that gets removed |
| `recolorLabels` / `labelColor` | `true` / `0xFFE8E8E8` | GUI title recolor |

## Performance

- **With no container screen open: zero cost** (a single texture check that costs almost nothing)
- **With a container screen open: roughly 0.5-1 ms per frame**
  - Panel drawing: about 56 quads (one body, a few for the rounded corners, about 30 for the border and 12 for the shadow)
  - Blur: two box-blur passes, clipped to the panel rectangle, so the rest of the screen is never touched
  - Processed textures are cached, about 256 KB each; the two blur buffers are each the size of the main framebuffer
- To spend less: set the blur strength to 0 (the blur path is skipped entirely) or set `enabled` to `false`

## Known limitations

- Minecraft 1.20.1 only (1.20.2 and later changed the GUI system enough that they need their own branch)
- The creative inventory keeps its vanilla look
- The description shown in the mod list is English only and does not follow the game language (Fabric's description field does not support language files)
- The panel shadow is built from straight edge strips rather than a rounded shadow, which is a deliberate tradeoff for performance

## Building from source

Requires JDK 17:

```
gradlew.bat build        # Windows
./gradlew build          # macOS / Linux
```

The first build downloads Fabric Loom and the Yarn mappings; the jar ends up in `build/libs/bettergui-1.0.0.jar`.

## Troubleshooting

Check `.minecraft/logs/latest.log`:

- `[BetterGUI] loaded (enabled=..., radius=...)` — loaded normally
- `[BetterGUI] failed to process texture ...` — one texture could not be processed, so the vanilla texture is used instead (everything still works)
- `[BetterGUI] blur post-effect unavailable, disabling blur` — the blur failed to initialize and was disabled; the panel degrades to translucency only

## Implementation overview

| File | Role |
| --- | --- |
| `BetterGuiConfig` | Config read/write (Gson) and the "effective" color and opacity values |
| `BetterGuiRenderer` | Panel drawing: blurred background, gradient fill, rounded corners, border, shadow |
| `BlurEffect` | Runs vanilla's post-processing blur twice into its own render target |
| `GuiTextureProcessor` | Makes vanilla GUI textures translucent at runtime (panel base, slots, frame) and caches the results |
| `BetterGuiTargets` | Decides which screens and which textures are handled |
| `config/BetterGuiConfigScreen` | The Mod Menu config screen |
| `config/ColorPickerScreen` | Color picker (SV square, hue strip, channel sliders, HEX) |
| `compat/BetterGuiModMenu` | Optional Mod Menu entrypoint |
| `mixin/*` | Hooks into vanilla rendering: screen detection, texture swapping, panel drawing, recipe book positioning, title recolor |

## License

MIT — see [LICENSE](LICENSE).
