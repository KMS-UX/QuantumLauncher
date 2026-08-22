# QUARK 3D Avatar — Production Log

Parallel art-asset track, not part of the Tree 1.5 Launcher milestone tree (M0–M7) and not
blocking Checkpoint β. Mirrors `BUILD_LOG.md`'s "resume here" discipline for this track's own
Blender/asset pipeline. Directions on where this eventually plugs into the app are in
`BUILD_LOG.md`'s Phase-1-planning entry (workflow discussion before this log existed).

## Decisions locked (Director, this track)
- **Render path: Hybrid.** Pre-rendered Blender frames (neutral-value, alpha-matted, one per
  posture — NOT pre-tinted per hue) + a real-time AGSL overlay shader in-app for rim glow / edge
  fade / data-particle threads, tinted live from the existing `PhosphorHueRuntime`. Avoids an
  N-postures × 3-hues baked-asset explosion.
- **Scope: new dedicated "QUARK Core App" surface.** Does NOT replace the existing
  Canvas-drawn `QuarkMascot` (`audio/…/QuarkMascot.kt`) used inline in AUDIO etc. — that stays as
  the lightweight small-context glyph.
- **Palette: RESOLVED (Director, 2026-08-20, Phase 4 kickoff session).** QUARK is a guardrail
  exception, not a violation: the four physical materials (Ceramic, Synth-Skin, Graphite, Metal
  Alloy — locked, concept-sheet-accurate hex values) stay their real-world material colors
  permanently, in every hue mode. They were never phosphor colors in either comparison render —
  the `palette_compare` renders only ever swapped the emissive accent (spine conduit, headband),
  not the body — so this wasn't actually the "8-token vs phosphor-collapsed body" choice the
  Phase 3c framing implied; flagged and corrected before locking anything in, per this track's own
  "flag forks, verify don't eyeball" discipline. What's actually decided: the emissive accent is
  the only thing hue-aware, and it's tinted **live at runtime by the Phase 4 AGSL overlay shader**
  from `PhosphorHueRuntime` — consistent with the render-path decision above (pre-rendered frames
  are neutral-value, not pre-tinted per hue). The `sheet_neutral` bake (`#E6F1FF`, the sheet's own
  "Neutral"/rest-state token) is therefore the correct default for the static pre-rendered frames
  — already what `main()` ships — and needs no code change; `phosphor_green` was a preview of what
  the live shader's GREEN-hue tint will look like, not an alternate bake to ship. **The `CLAUDE.md`
  "phosphor only / no off-palette colors" guardrail is satisfied**: the one hue-bearing element
  (the accent) is phosphor-driven and single-sourced from `PhosphorHueRuntime`; the body materials
  are an explicitly Director-approved exception for this one photoreal/holographic surface, not an
  off-palette violation of the rule elsewhere in the app.

## ■ Era 1 (Phases 1–6) — building QUARK as a 3D character. CLOSED.

*Condensed 2026-08-22. ~1,900 lines of build narrative for a path whose artefacts were deleted in
Phases 14–15 and whose renders are gone. What is kept below is everything that still constrains the
code, plus the negative results worth not repeating.*

**What it was.** QUARK modelled from scratch in Blender: a procedural bmesh base mesh and rig,
voxel-remesh retopology to ~21k quads, UV unwrap, a procedural panel/plate material system, baked
base-colour / roughness / normal / AO / emission maps, hand knuckles and separate toe geometry, a
posture library, and a lighting/lens/raytracing pass. Exported to GLB and taken end to end on device
through SceneView.

**Why it lost.** A good stylised character, but a different order of craft from the Director's
reference art, and several sessions could not close that gap. Superseded by the native-art route
(Phase 14); model, textures and renders deleted in Phase 15.

### Findings that outlived it

- **The anthropometric canon was wrong, and flagging beat fixing.** The mesh was built on the classic
  Loomis 7.5-head canon: scaled to 167 cm it put the knee landmark at 33–36 cm where two real
  anthropometric sources put it at 51–61 cm. Raised to the Director rather than silently corrected;
  the Director chose the corrected variant, applied in Phase 4a as a continuous compression
  transform. **This is the origin of this track's standing rule that proportion and look choices go
  to the Director, not into code.**

- **A regression caught by the file's own topology check, not by eye.** The correction produced a
  disconnected mesh fragment. Several wrong theories were chased before dumping the fragment's raw
  vertex coordinates, which pointed at the real cause: `ring()` had no `axis='Y'` mode, so one ring
  varied the wrong pair of axes. **The lesson stuck: dump the data rather than reason about it.**

- **Verification by rendering, not by reading the code.** Repeatedly decisive here — the ear
  audio-module was implemented, rendered, seen to be wrong (an axis-aligned material-region box can
  only make a rectangle, never a small circle) and walked back in the same pass. One texture bake
  silently failed and was caught only because the output PNGs were opened directly.

- **SceneView/Filament, still true today:** a second Filament `Engine` created after a first is
  destroyed **in the same Activity renders NOTHING** — no error, clean logcat. One SceneView, kept
  alive, was the workaround. (Moot since Phase 18 removed SceneView, but it cost a session to find.)

- **The palette guardrail conflict, resolved by the Director (Phase 4 kickoff):** QUARK is an
  exception to "phosphor only" — her art keeps its own colour, and the phosphor treatment is applied
  as a live tint rather than baked. That decision is why `PHOSPHOR TINT` is a control and not a
  constant.

---


## ■ Era 2 (Phases 7–13) — reconstruction → native art. CLOSED.

*Condensed 2026-08-22. The splat and relief paths are both cut (Phases 14 and 18). Kept below: the
two hard SDK limits that closed whole options, and the plate pipeline, which is still live — every
plate the app ships came out of it.*

### The two findings that closed options permanently

- **SceneView 4.22.0 renders Gaussian splats ISOTROPICALLY.** It never packs the per-gaussian
  rotations, so surface detail cannot be sharp on that path *regardless of source quality*. This is
  not a tuning problem and it ended the TripoSplat route outright (Phase 8, after a long hunt that
  assumed a yaw bug).
- **A custom `CameraNode` renders nothing.** SceneView's `DefaultCameraNode` carries framing a bare
  `rememberCameraNode` does not, so tilt parallax was applied by rotating the MODEL instead. At
  ±10° that is visually equivalent (Phase 7).

### The Gatebox presentation (Phase 7) and the plates (Phase 9)

The Director's reference art is dead-on front-facing, and the specified presentation — a projection
housing, front-on, static at rest — never needs a viewing angle the plate does not already contain.
So the plates are shown AS the art, and nothing reconstructs anything. That is the decision the whole
current build rests on.

**Chroma key (`comfy/chroma_key.py`, live):** the art is delivered on flat magenta, and the subject
is entirely blue/cyan, so `R−G` separates them outright — background +235..+238 against subject
−35..−52. Measured residual spill **0.000%**. Despill clamps R to G, which is safe because the
subject never has R > G anywhere. This is exact, not an approximation.

### The state plates (Phase 12) — still the live pipeline

`comfy/flux2_state_plates.py`, Flux 2 Klein 4B fp8, all installed locally, no download.

- **Denoise is the whole ballgame.** ComfyUI's own template starts from an EMPTY latent, i.e. it
  *regenerates* with the source as conditioning only — and the first WARN plate came back with the
  right expression on a subtly different face and hair. Starting from the **encoded source** with
  `SplitSigmasDenoise` on the tail of the schedule makes it a true edit. **0.45** for SCAN and WARN.
- **HAPPY needs 0.30.** At 0.45 the model reads "smile" as a broad open one however the prompt is
  worded; the prompt also had to say what NOT to do. Director's pick from a side-by-side.
- **A shared crop box is mandatory.** Keyed independently each plate gets its own tight bbox and the
  avatar jumps a few pixels on every state change. The bust set is cropped to the union box
  `(121, 40, 900, 996)`; the body set to `(227, 26, 793, 1024)` (Phase 18).

### The housing (Phases 10–13)

The DA3 relief and its tilt parallax lived here and are gone (Phase 18). What survives is the
housing's design language and two rules:

- **WARN drives the HOUSING, not just the plate.** Red inside the art alone measured as merely
  legible; the alert has to be carried by the surface the Operator is already looking at.
- **Animate the PROJECTION, never the character.** The house rule that QUARK is static at rest is
  about *her*, not about the apparatus projecting her — which is what makes the AMBIENT carrier and,
  later, B1's materialise and B3's emitter cadence house-style legal. With AMBIENT off the surface
  is pixel-identical frame to frame; that has been re-verified in every phase since.

Two deliberate house-style departures were opened here and are still live: the **320 ms crossfade**
between states (the style says stepped) and the **AMBIENT loop** (the style says zero idle redraw).
Both were asked for, both are behind controls.

---


## Phase 14 — the native-art route, and everything else removed

**Director's decision: QUARK ships as native art.** The plates are shown as the art they are, with
no reconstruction step anywhere in the pipeline. The four other paths the track evaluated are out.

### Removed from the build

Dead render paths are not left switched off — a mode nobody uses is a thing every future session has
to read past before it can work.

| removed | |
|---|---|
| `assets/models/quark.glb` | 8.53 MB — the Blender/MPFB character model |
| `assets/models/quark_{body,bust,head,holo}.spz` | 12.17 MB — four TripoSplat reconstructions |
| `drawable-nodpi/posture_*.png` | 3.06 MB — the Phase 4b pre-rendered posture frames |
| `drawable-nodpi/quark_plate_bust.png` | 1.77 MB — superseded by `quark_state_idle` |
| `ui/effects/QuarkAvatarShader.kt` | the AGSL green-dominance colour key, dead with the frames it keyed |
| `DemoPosture.kt` | superseded by `QuarkState` |
| `RenderMode.BAKED_2D / MODEL_3D / SPLAT_*` | five modes down to three |

`QuarkSceneView.kt` went from **318 lines to 117** — it had become mostly splat machinery
(percentile fitting, sort-camera counter-rotation, per-gaussian scale trimming) and none of that has
a job any more.

**One measurement I cannot reconcile, stated rather than smoothed over.** The APK went 262.28 → 
261.47 MB, a 0.81 MB drop for ~25.5 MB of removed source assets. What I *can* verify by reading the
APK directly is its current composition: **10.24 MB of QUARK assets total** (relief 3.22, four state
plates 5.11, body plate 1.42, voice 0.50), and none of the deleted files present. The pre-cleanup
figure on disk was probably not a build containing all of them. **No saving is claimed** — the
composition above is the number that was actually measured.

For scale, the APK is dominated by things unrelated to this track: `classes.dex` 28.62 MB,
`classes21.dex` 26.29, `libonnxruntime.so` 24.63, `liblitertlm_jni.so` 24.46 + 20.53, `libmaplibre.so`
10.74 + 10.62.

### Archived, not deleted

`art/quark-avatar/archive/` with a README explaining what each path was and why it lost. The
measurements in this log only mean anything next to the artefacts they were measured on.

One real dependency had to be broken first: `da3_relief_mesh.py` and `flux2_state_plates.py` both
imported their HTTP transport from `triposplat_reference_to_splat.py`, which would have made the
archive load-bearing. Extracted to `comfy/comfy_client.py`.

### Deliberately NOT removed

`blender/` and `reference/` stay. `blender/` is hand-authored source and one of its scripts
(`05_relief_cleanup.py`) is **live** in the relief pipeline; `reference/` is the Director's art and
what every plate is measured against. Removing either is a decision, not a cleanup — flagged rather
than taken.

### What the live route is now

    reference plate (magenta key)
        -> comfy/chroma_key.py                 exact matte, shared crop box
        -> comfy/flux2_state_plates.py         IDLE / SCAN / HAPPY / WARN
        -> res/drawable-nodpi/quark_state_*    PLATE_BUST, crossfaded
        -> comfy/da3_relief_mesh.py            depth as geometry, art as texture
           + blender/scripts/05_relief_cleanup.py
           + comfy/glb_unlit_patch.py
        -> assets/models/quark_relief_bust.glb RELIEF_BUST, tilt parallax

Three render modes, all native art. Verified on the Pixel_10a after the cull: plate renders, states
cycle, housing responds, no crash.

---

## Phase 15 — the archive goes, the laser disc goes, and A1 is armed

Two Director instructions, both narrow, plus the readiness check the workplan asks for.

### The floor pool read as a laser disc, so it is gone

`ui/scene/QuarkHologramOverlay.kt`. The housing drew two ovals on `FLOOR_FRACTION` — a radial
gradient pool and a hard bright core line — whose job (Phase 13) was to make QUARK read as
**projected into** the housing rather than pasted onto the background.

On device it does not do that job. At this framing the pool has no floor to sit on: it reads as a
bright cyan disc hanging in mid-screen, and it is the brightest thing on the surface, so it competes
with QUARK for the eye. Both ovals removed. Nothing else in the housing changed — column, ambient
sweep, scanlines and CRT falloff all stand — and `FLOOR_FRACTION` is still the line the figure is
framed standing on, it simply is not drawn any more.

`:quark-avatar:assembleDebug` green.

**What is now unproven, stated rather than assumed:** Phase 13 claimed the pool was one of three
surfaces WARN turned red (pool, column, CRT glow). It is now two. WARN was measured as
"unmistakable at a glance" *with* the pool; whether it still is, is a device question, not one this
can answer from the source. `QuarkState`'s comment has been corrected to say two rather than three.

**Also unproven:** whether QUARK still reads as projected rather than pasted without the pool. That
was the pool's stated purpose. The column alone may carry it; it may not. Both of these want the
same look on device.

### The archive is deleted, not archived

Phase 14 moved five dead render paths into `archive/` on the reasoning that "the measurements only
mean anything next to the artefacts they were measured on". **The Director has overruled that** —
the route is native art, and the dead paths are not coming back.

| deleted | |
|---|---|
| `archive/` | 84 MB — splats, the Blender GLB, posture frames, baked textures, QA renders |
| `blender/mpfb_test/` | 35 MB — the MPFB character-build experiments and their renders |
| `blender/quark_base.blend` + `.blend1` | 4.4 MB — the Blender character source |
| `hdri/photo_studio_01_2k.hdr` | 6.1 MB — lighting input for the Blender render scripts |
| `blender/scripts/01–04` | the MPFB build, anthro compare, posture library and glTF exporter |

**172 MB → 44 MB on disk.** Verified afterwards: `blender/` now holds exactly one file,
`scripts/05_relief_cleanup.py`, which is live in the relief pipeline; `comfy/`, `reference/`,
`renders/{plate,relief,state}` and `export/{relief,state}` — the whole live native-art route — are
untouched, and `:quark-avatar:assembleDebug` is green after the cull.

**Recoverability, honestly.** Everything above except roughly 26 MB was committed at some point and
restores with `git checkout`. The exception is `archive/export/splat`, `archive/renders/splat` and
`archive/comfy/` — never committed, now unrecoverable. The Director chose that explicitly with the
figure in front of them. What survives is the reasoning, which is in this log.

Two stale pointers to `archive/` were fixed rather than left to rot: the header comment in
`RenderMode.kt` and the provenance note in `comfy/comfy_client.py`.

### A1 readiness — checked, not assumed

The workplan's next item is A1 (relief the other three states). Its prerequisites are all live:

| | |
|---|---|
| ComfyUI on 127.0.0.1:8188 | HTTP 200 |
| DA3 mono large | installed, the model Phase 10 used |
| the four keyed plates | `export/state/{idle,scan,happy,warn}_keyed.png`, all on the shared crop box `(121, 40, 900, 996)` |
| existing relief | `export/relief/quark_relief_bust.glb` — the geometry A1 proposes to reuse |
| `blender/scripts/05_relief_cleanup.py` | survived the cull, still live |

The shared crop box is the thing that makes A1's one-mesh-four-textures plan plausible, and it is
confirmed present on all four. The depth-map diff that would *prove* it has not been run — that is
A1's first step, not a claim to make here.

Nothing committed.

---

## ■ Phases 16–17 — the relief gained expressions and a feathered edge. CLOSED.

*Condensed 2026-08-22. Both phases were relief work and the relief was cut in Phase 18, so the
method is kept and the narrative is not. The FRAMING half of Phase 17 is still live and is below.*

### A1 — one mesh, four textures (dead with the relief)

Proven before building rather than assumed: DA3 depth was run over all four keyed plates and diffed
over the eroded matte intersection — worst single-pixel disagreement **7/255**, mean ≤1.07, against
a relief whose own depth spanned ~29 levels. The expressions move the art, not the surface. And the
plates turned out to BE the relief's texture (same crop, different scale; 6.5/255 inside the matte),
so all four states cost **zero new bytes**. Verified by measuring the relief's state-to-state delta
against the flat plate's as a control: ratio 0.83–1.06 across all six pairs.

### A2 — the silhouette (dead with the relief)

The workplan blamed the hard 0.5 matte cutoff on semi-transparent hair. Measured, the matte was
**99.6% binary** — there were almost no such strands. The real defect was **crenellation**: one
vertex per source texel plus a binary keep/drop quantises the outline into a texel-resolution
staircase. Cutoff → 0.004 plus `alphaMode: BLEND` cut roughness from **17.48 to 6.54 px RMS** and
matched the flat plate's edge softness exactly. A 3-ring `COLOR_0` vertex feather was then built,
measured (roughness 6.54 → 6.40, a 2% gain, while blurring the edge 4× past the reference) and
**removed** — recorded so nobody builds it twice.

*Also from A2: dropping `NORMAL` (the material is unlit, nothing reads them) and tightening Draco
quantisation took the relief 3.38 MB → 1.45 MB. The same lever applies to any future GLB.*

### Framing — STILL LIVE, and the method matters more than the numbers

QUARK is bottom-anchored: her plate runs off the bottom edge so the art's own cut never shows, and
the CRT falloff dissolves the approach to it. Three bugs, each of which presented as "nothing
happened", are worth keeping:

1. **`ContentScale.Fit` centres the bitmap inside whatever box it is given**, so a bottom-anchored
   offset is absorbed by the slack — measured as a 232 px shortfall. Size the layout box with
   `aspectRatio` off the painter's own intrinsic size.
2. **SceneView re-frames the scene**, so `centerOrigin` and `position` were both *exact* no-ops on
   the model. The surface had to be offset instead. (Moot now; SceneView is gone.)
3. **`Modifier.width` is clamped by the parent.** The FRAMING control produced four pixel-identical
   screenshots before this was found. `requiredWidth` ignores the incoming constraint.

**The method lesson, which cost two wrong answers:** the overhang constant was set twice from the
**maximum** of the silhouette's height profile, and was wrong both times. The maximum is a single
narrow notch. Read the distribution — for the body it said 0.08 where the max said 0.230, and for
the bust 0.05 where the max said 0.146 (Phase 19 corrected that one, and found the bust plate has no
cut at all).

The two modes are locked to the same size and position, calibrated by normalised cross-correlation
of their screenshots rather than by eye — which mattered, since eyeballing said `RELIEF_EXTENT`
1.625 and correlation said 1.222.

---


## Phase 18 — the relief is cut, QUARK stands up, and B1 scans her in

Three Director calls in one session: **ditch the DA3 relief** (the slicing), **present the full
figure** rather than a floating bust, and **build B1**.

### The relief, SceneView and 24.98 MB of native libraries are gone

Phase 17 took the relief's silhouette roughness from 17.48 to 6.54 px RMS and could not reach the
flat plate's 3.12 without giving up the shared textures that made it free. The Director's verdict on
the residual is that slicing on a face is not something you ship, and that settles it.

Removed: `QuarkSceneView.kt`, `QuarkReliefTexture.kt`, `QuarkTilt.kt`, `RenderMode.RELIEF_BUST`, the
TILT PARALLAX control, `assets/models/quark_relief_bust.glb`, the whole relief half of the art
pipeline (`relief_pipeline.py`, `da3_relief_mesh.py`, `da3_state_depth_check.py`,
`glb_unlit_patch.py`, `blender/`), and `quark_plate_body.png` — superseded by the new
`quark_body_idle`.

**APK 231.90 → 196.39 MB compressed, a 35.5 MB reclaim.** Larger than the 24.98 MB of
Filament/gltfio `.so` files across four ABIs, because SceneView's own classes came out of the dex
with them. `filament/sceneview` in the APK now measures **0.00 MB**. Tilt parallax goes with it, and
coming back means re-adding the dependency — which the Director accepted with the figure in front
of them.

### Four BODY state plates

The full figure had one pose and no expressions, so A1's four states existed only on the bust.
`flux2_state_plates.py` was run unchanged on `reference/QUARK_HOLOGRAM_FRONT.png` — the prompts
name the face and the accent glow, not the framing, so they transfer — and `body_state_key.py`
keys all four on the shared box `(227, 26, 793, 1024)`.

Result: four plates at **566x998**, visible area 57.9–58.5%, residual spill **0.000%** on every
one, ~1.0 MB each. `renders/state/body_four_states.png`.

Two differences from the bust run, both forced by the art rather than chosen:

* **`drop_base=False`.** `chroma_key.py` crops the bust's projection rings at 0.915 of the height.
  The body plate has no rings — it is cut at mid-thigh over flat magenta — so that crop would
  simply amputate her legs.
* **IDLE is resampled, not regenerated.** The generator runs at ~1 MP so the edits come back
  1024x1024 against a 1254x1254 reference. A shared crop box means nothing across two resolutions.

**A finding about the sampler that cost a sweep to learn.** WARN looked wrong at first pass, so it
was re-run at denoise 0.30 and 0.38 against the standing 0.45. **0.38 and 0.45 came back
byte-identical** — with `STEPS = 4`, `SplitSigmasDenoise` quantises the tail, so there are only
about four distinct denoise levels available on this graph and two of the three tested collapsed
onto the same one. 0.30 holds identity best but loses the red accent almost entirely (mean R-B
−105.2 against IDLE's −100.5, i.e. *bluer* than neutral). 0.45 keeps identity well enough and
puts real red on the ear modules and neck circuitry. Kept at 0.45.
`renders/state/body_warn_denoise_sweep.png`.

*Worth knowing for next time:* the body framing gives the model far less resolution on the face than
the bust did — at 1 MP over a full figure the head is a few hundred pixels — so expression edits
drift more here for the same denoise.

### Framing: the cut is not a line, and treating it as one buried her

`RenderMode.baseOverhang` is now per plate set, because the two cuts are shaped completely
differently and a shared constant is wrong for one of them.

The instructive part is the BODY. Measured off its matte, **88% of the columns run to within 0.05 of
the plate's bottom edge**; only the narrow notches beside and between the legs reach as high as
**0.230**. Sizing the overhang to that maximum — the first attempt, and the same reasoning that
was correct for the bust — hides the entire lower body to bury a few percent of the width. Thighs,
hands, the whole reason for using the full figure, all pushed off screen.

**0.08 is right**, and the notches that remain sit inside the CRT falloff, which dissolves them.
Verified by brightening the base region 4.5x: the figure runs continuously off the bottom edge with
no hard line anywhere.

| | BUST | BODY |
|---|---|---|
| shape of the cut | broad scallop across the chest | 88% flush to the edge, narrow notches |
| overhang | 0.17 | **0.08** |

FRAMING steps were rescaled for the body's 0.567 aspect — 0.9 / 1.0 / 1.25 / 1.5. Measured on
device, her top edge lands **37% / 29% / 14% / 12%** down the frame. **Default 100%**: head about a
quarter down, side margins on both edges, the figure running out of the bottom rather than ending in
it — the closest match to the Director's reference framing.
`renders/plate/device_body_framing_sweep.png`.

BODY is the default presentation now; BUST stays selectable and both sets carry all four states.

### B1 — the materialise

`QuarkMaterialise.kt`. Pure data, no Compose types, so the timing is testable without a device:

    0.00 -- 0.12   the emitter STRIKES at the bottom edge, fast up and slow down
    0.08 -- 0.78   a scan band travels from the bottom edge up past the top of the frame
    0.12 -- 0.82   the figure RESOLVES behind it, revealed bottom-up with a soft edge
    0.10 -- 0.90   the figure fades from nothing to full
    0.72 -- 1.00   the emitter falls back to rest

1400 ms end to end — an entrance, not a wait. The house style reserves PLEASE STANDBY for actual
waiting.

**The emitter is at the bottom edge and off-screen**, per the Director. It is the only placement
that works: the plates have no feet, so nothing can stand on a drawn base, and a drawn base was
already removed once for reading as a disc floating mid-frame. What gets drawn is only the *bloom*
the emitter throws back up into the frame.

**Two mechanics that had to be right:**

* The reveal mask is a full-canvas vertical gradient composited `DstIn`. Full-canvas because a blend
  mode only applies inside the bounds of what is drawn — a partial rect leaves everything above it
  untouched instead of erased.
* `CompositingStrategy.Offscreen` is mandatory. Without its own buffer the blend acts against what is
  already on screen, which here is the housing — so the mask would eat the projection column
  instead of the figure.

**This is reactive, so it is house-style clean** where the AMBIENT loop is a flagged departure. And
it genuinely stops. Eight unit tests cover the timing (monotonic reveal, band leaves, emitter
returns to rest, everything in range), and on device with AMBIENT off:

| | |
|---|---|
| static frames before the replay, 12 fps | mean abs diff **0.000** |
| two screencaps 2 s apart after it settles | mean abs diff **0.0000** |

`renders/state/device_materialise.png`.

### A measured regression from Phase 15, and a partial fix

Phase 15 removed the floor pool and flagged that WARN's "unmistakable at a glance" had been measured
*with* it. Now quantified: driving the housing to `--warn` moved the housing strip's R-B by **1.08
out of 255**. The column alone is drawn at alpha 0.016–0.10, so the colour had nothing bright to
land on. WARN's housing read was, in practice, not working.

B1's emitter bloom is reused as the fix: an alert state now burns the emitter at rest as well as
recolouring the housing, so the red has a bright element to sit on — and it is a projection
artefact at the bottom edge rather than the disc that was removed. Measured over the emitter band:

| | IDLE R−B | WARN R−B | delta |
|---|---|---|---|
| housing strip (colour only) | −7.57 | −7.12 | +0.45 |
| emitter band (colour + burn) | −18.22 | −6.31 | **+11.92** |

**Honest limit:** an 11x stronger signal, and on device it now reads as a warm glow rising from
below where before it read as nothing — but it is still *legible* rather than *loud*. If the
Director wants Phase 13's punch back, the lever is `ALERT_EMITTER` (0.75 today) or giving WARN a
housing element of its own.

Nothing committed.

---

## Phase 19 — the bust stops being cropped, and QUARK finally makes a sound

### FRAMING is two settings now

100% and 125%, defaulting to 100%, on the Director's call. 90% and 150% were only ever there to
bracket the judgement and the judgement has been made.

### The bust overhang was calibrated against a defect that no longer exists

Applying the body's measured approach to the bust turned up a mistake in Phase 17. Re-measured off
the plate itself:

| | |
|---|---|
| columns running flush to the plate's bottom edge | **99%** |
| bottom 15% of the plate that is OPAQUE | **98.8%**, at mean luminance ~190 |
| columns ending more than 0.05 above the edge | **0.5%** |

**There is no cut in the bust plate at all.** It is bright, opaque art right to its bottom edge. The
"hard black scalloped curve across her chest" that `BASE_OVERHANG = 0.17` was set to hide belonged to
the **DA3 relief's culled mesh boundary** — the relief was the mode being looked at when that
number was chosen, and the plate inherited it. With the relief gone (Phase 18) the constant was
cropping roughly **12% of her height** to hide nothing.

All that has to leave the frame is the plate's own straight bottom edge, so **0.05**. Verified on
device: at 100% and 125%, in both BODY and BUST, the bottom row of the frame is lit (mean luminance
~16 against a black ground) — the figure runs out of the frame in every combination and no cut is
visible anywhere. `renders/plate/device_framing_body_and_bust.png`.

*Method note worth carrying:* both times this constant has been wrong, it was wrong because it was
set from the **maximum** of the silhouette's height profile. The maximum is a single narrow notch.
The distribution is the thing to look at — for the body it said 0.08 where the max said 0.230, and
for the bust it says 0.05 where the max said 0.146.

**Flagged, not decided:** with the crop corrected, the BUST at 100% puts her head **49% down** the
frame, leaving the upper half empty (BODY sits at 29%). The bust plate is simply squarer, so the same
width buys less height. It reads better at 125% (36% down), which is one tap away. Whether the BUST
should default differently from the BODY is a look call.

---

### B2 — sound. It was already built, and unreachable

The workplan reads as though the sound needed writing. It did not. `SoundEngine` has existed since
M6 with the full house-style bank — the signature four, the supporting cues, **and QUARK's three
wordless chirps**, synthesised exactly as `design-tokens.md` specifies them:

    CHIRP_SCAN    600 -> 1050 Hz sine, rising interrogative
    CHIRP_HAPPY   880 + 1320 Hz two-note, 1980 Hz sparkle on top
    CHIRP_WARN    300/240 Hz square, 20 Hz tremolo -- the denial language
    BOOT_SWEEP    196 -> 880 Hz power-up sweep, "system alive"

QUARK could not play any of them, and the reason is the same structural one the workplan flags for
state wiring: **`SoundEngine` lived in `com.quantumos.shell.ui`, inside `:app`, and `:app` depends on
every docked module and never the reverse.** Her chirps had been in the build for three milestones
with no path from her own module to reach them.

**Moved to `:app-shell`**, beside the other things every module shares. The class is unchanged apart
from its package; `:app` constructed it in exactly one place (`QuantumRuntime`), so the move is a
one-line import change there. **Every docked module gains sound, not only the avatar** — COMMS,
FILES, OPTICS and the rest can now emit the same bank.

### Verified rather than assumed, twice over

The bank had never been tested; it was only ever known to compile. `synth()` is pure Kotlin — only
`blast()` touches Android — so it is now `internal` and covered by **7 JVM unit tests** in
`:app-shell`: every QUARK cue produces non-silent, non-clipping PCM (peak > 3000, ≤ 32767); the
signature four all exist; the cues are brief (≤ 600 ms, "functional, not cinematic"); the three
chirps are measurably distinct from each other; an unknown token yields **null** rather than a
placeholder beep; and WARN sits in the same weight class as ACCESS DENIED rather than chirping
brightly, which is what "shares the denial language" has to mean.

Then on device, by watching `dumpsys audio` create players:

| | |
|---|---|
| open the avatar | new AudioTrack — **BOOT_SWEEP** |
| STATE → SCAN / HAPPY / WARN | a new AudioTrack **each time** |
| STATE → IDLE | **no** new player |
| 4 s sitting idle | **no** new player |
| state change with STEALTH on | **no** new player — the gate holds |

IDLE is silent by design: it is rest, and the house style is explicit that sound is reactive rather
than ambient. The stealth gate was already in the engine and now has something in this module to
gate.

### Where the cue lives

On `QuarkState`, beside `line` and `housingAccent`, rather than in a `when` at the call site. The
plate, the voice line, the housing colour and the sound are four expressions of one state, and
splitting them across files is how they drift apart. The chirp fires on a state **edge**, not a
level — a remembered previous state — so rotating the device does not sound an alert.

Nothing committed.

---

## Phase 20 — B3: the projector speaks and powers down, and one draw-order bug fixed three things

### Framing is per mode now

`RenderMode.defaultFramingIndex`: **BODY opens at 100%, BUST at 125%** (Director's call — the bust
is squarer, 0.815 against 0.567, so the same width buys it much less height and it sat with its head
49% down the frame). Each mode also *remembers its own* framing choice rather than sharing one index,
so switching between them to compare no longer discards a deliberate change to the other.

### SPEAKING: the apparatus carries it, not a ring over her face

What was there: a stroked Compose circle expanding from the centre of the screen, inherited from the
pre-rendered-frames path. It spoke none of the housing's language — not a projection artefact, no
relationship to the emitter or the column, and the only round thing on the surface. Deleted.

What replaced it: **the emitter burns with her cadence** and the column brightens with it, so the
projector is visibly under load while she talks. Keeps the Phase 13 principle — the apparatus
moves, QUARK does not — and needs no new drawn element, because B1 already made the emitter the
place where the machine is.

The cadence is a **stepped 13-level table, not a sine** (`QuarkSpeaking.kt`). A sine reads as a
smooth ambient throb, which is both the wrong feeling and against the house rule that motion is
"stepped, not interpolated"; a table of levels held in turn reads as a machine keying a signal.
Thirteen because a prime step count against the loop keeps the pattern from settling into a visible
rhythm, and it never rests at zero because a projector carrying a voice is never fully off. Five unit
tests hold that shape — including one that asserts the signal is genuinely stepped by counting value
changes across 2000 samples, and one that asserts the step count is prime.

*(One of those tests failed first time at 13 changes against an expected 12. The test was wrong, not
the table: it sampled the closed interval, and phase 1.0 is the same instant as phase 0.)*

### STEALTH: a power-down, not an opacity multiply

What was there: `stealthDim = 0.35f` on the plate's colour matrix and nothing else. The housing went
on burning at full strength around a dimmed figure, which is the opposite of what going dark looks
like.

Now the **apparatus** powers down, as a 420 ms ramp rather than a switch: the column collapses toward
the emitter, everything the housing draws dims by 80%, the figure settles at 35%, and an ember is
left at the bottom edge so the unit reads as running dark rather than switched off. The house style
says Stealth is "dimmed" — QUARK stays legible, because Stealth is for operating unseen, not for
losing your assistant.

It also plays **STEALTH_DOWN / STEALTH_UP**, which the engine deliberately exempts from its own
stealth gate — those two cues *are* the sound of going dark and coming back, so they have to be
heard as everything else falls silent.

### The bug that was quietly costing three separate things

First measurement of SPEAKING put the housing corners at **1.02x their silent value**. That is
nothing, and the cause was draw ORDER: the emitter's bloom occupies the bottom 22% of the frame and
the CRT falloff paints black over the bottom 26% **after** it. The vignette was erasing the emitter.

The emitter now draws **last**, after the falloff. That is not a workaround, it is the correct
reading of what each element is for: the falloff exists to fade *content* to black at the edges, and
the emitter **is** the edge — it is the apparatus, not something being projected.

| measured in the housing corners / emitter band | before | after |
|---|---|---|
| SPEAKING vs silent | 1.02x | **1.85x**, frame spread 9.93 against 0.27 |
| WARN alert burn (R−B delta) | +11.92 | **+39.87** |
| STEALTH vs silent | — | 54% |

**The WARN row is the one that matters most.** Phases 18 and 19 both closed with the same caveat:
the alert was *legible* rather than *loud*, and Phase 15 had measured the housing colour alone at
1.08/255. It was never a strength problem — the alert burn was being vignetted away by the same
bug. At +39.87 it now reads as unmistakable red from below. **That caveat is closed.**

One retune followed: `STEALTH_EMBER` went 0.10 → **0.035**. Un-vignetted, the old value left Stealth
at 72% of normal brightness, which is brighter than a unit running dark has any business being.

### Still static at rest

`SPEAKING` and `STEALTH` are both reactive — the cadence animation exists only while she is
speaking. Two screencaps 3 s apart with everything off: **mean abs diff 0.0000.**

`renders/state/device_b3_speaking_stealth.png` — normal, speaking at an emitter peak, stealth
power-down, WARN alert burn.

Nothing committed.

---

## Phase 21 — B4: QUARK stops simulating the unit and starts reading it

Also: the speaking cadence slowed **1.4x**, 1300 → 1820 ms, on the Director's call. At the original
rate the emitter read as agitated rather than as a projector carrying a voice.

### The blocker was smaller than the workplan thought

The workplan has said since Phase 4b that this "needs the same cross-module extraction `:quark-brain`
needed — a real piece of work, not a wiring task". Recon before building found that most of the work
was already done years of phases ago:

- `QuantumStateEngine`, `QuantumLauncherState` and **`QuarkReflexPosture { IDLE, SCAN, HAPPY, WARN }`**
  — the exact four states — have always been in **`:core`**, which every module already depends on.
- `environment.isStealthMode` and `quarkBrain.activePosture` were already modelled.
- `toggleStealthMode()` and `dispatchQuarkReflex()` already existed as real mutators.

What could not be reached was the live **instance**, which `:app`'s `QuantumRuntime` object owns. So
this is a **publishing seam, not an extraction** — and Phase 19 had already proved the shape by
moving `SoundEngine` the same way.

### `QuantumStateRuntime`, in `:app-shell`

Deliberately the same shape as `PhosphorHueRuntime`, which solved the identical problem for the
active hue: one process-wide object beneath everything, `:app` feeds it at boot, docked modules read
it with `collectAsState()`. A second use of that seam confirms the pattern rather than inventing a
new one.

Two details that matter:

- **Before `:app` publishes, `masterState` reports defaults rather than null.** A docked module can
  be built and previewed without a booted launcher, and every consumer would otherwise need its own
  null branch. The actuators are no-ops until then for the same reason.
- **`publish()` is idempotent** — republishing the same engine returns early, so a re-entrant boot
  cannot stack a second collector on the same flow.

### `isSpeaking`: the one thing genuinely missing

Nothing in the model carried it. The voice engine knew QUARK was speaking and no surface could ask.
Added to `QuarkBrainState` in `:core`, raised in `QuantumRuntime`'s voice observer at `onStart`,
cleared at `onDone` **and in `stopCurrentSpeech()`** — a stopped utterance never reaches its
`onDone`, and the avatar would otherwise keep her emitter burning for a voice that had stopped.

It is deliberately **not** part of `dispatchQuarkReflex`: a posture change and the act of speaking
are different events with different lifetimes, and the posture persists after she stops. The reflex
now carries `isSpeaking` across rather than resetting it, so a posture raised mid-sentence cannot
silently claim she has finished.

*Inserting a field into the middle of a positional data-class constructor broke two existing
`QuarkBrainState(...)` call sites. The compiler caught both; they are named arguments now, which is
what they should have been.*

### The controls are real actuators

On the Director's call. STATE dispatches a genuine reflex and STEALTH calls `toggleStealthMode()`, so
the avatar is no longer a simulation of the unit running inside the unit.

**SPEAKING is a readout, not a control** — QUARK speaks because she has something to say, and faking
it here would put a second source of truth back on the screen. The row is labelled `LIVE -- SILENT` /
`LIVE -- SPEAKING` so it does not read as a dead toggle.

One consequence had to be handled: making the controls real created a **double-fire**. The avatar was
playing its own chirp on a state change, and the reflex dispatch now carries the cue token so the
engine plays one too. The local chirp is gone — the engine's path is the better one, because a
posture raised anywhere in the OS now sounds, not only one raised while this screen is open.

What stays local is **presentation**: RENDER, FRAMING, AMBIENT, MATERIALISE, PHOSPHOR TINT. Those are
how the avatar is drawn, not what the unit is doing.

### Verified through the LOG channel, not the screen

The avatar's own display is not proof — it reads back from `masterState`, so it changing only shows
the round trip. The proof that it reached the **OS** is the launcher's own LOG channel, read after
driving the avatar's rows:

    > QUARK_BRAIN: [AVATAR_PREVIEW] posture [SCAN]
    > QUARK_BRAIN: [AVATAR_PREVIEW] posture [HAPPY]
    > ENV: Stealth ENGAGED — emission dimmed

And the read direction shows in the same log — `QUARK_BRAIN: [ONLINE] posture [HAPPY]` and
`[VOICE_DONE] posture [IDLE]` are the launcher's own boot flow driving the posture the avatar
displays. Stealth released cleanly from the same row, so the toggle round-trips both ways. Zero
crashes (`grep -v adbd` first — `adbd` echoes the search string into logcat, which produced three
false positives across this session before it was noticed).

Nothing committed.

---

## Phase 22 — QUARK moves into the OS

The avatar track's whole point, finally connected: **the floating trigger opens QUARK herself, in one
tap.** W2 + W3 + W4 of the approved plan.

### W4 — the trigger is her face now

`reference/QUARKIcon.png` — her portrait in a circular HUD badge — replaces the drawn iris rings
that this file had labelled "static placeholder art, NOT the final QUARK mascot" since the day they
were written.

**Phosphor-tinted at draw time, not baked.** The source art measured mean saturation **0.55** — full
colour, and the house style is explicit that icons are themeable with the active phosphor and that
off-palette colour is not introduced. The bitmap is mapped luminance → active hue through a
ColorMatrix using the same Rec.709 weights the plates use, so one asset serves green, amber and cyan
and it re-tints live when the hue changes.

The art survives the treatment: its luminance spans the full 0–255, and at the trigger's **140 px**
footprint (52 dp on the test device) the face, the ring ticks and the QUARK wordmark all stay
legible rather than flattening into a disc. Downscaled 1254 → 256 px: **1795 KB → 131 KB**.

### W2 — the Assistant View IS QUARK

`ui/QuarkProjection.kt` extracts the whole presentation — plate, housing, materialise, emitter — as
a drop-in composable, and `QuarkAssistantActivity` draws it full-screen with the conversation and
command rail over her lower frame. The 132 dp ring-and-iris mark it replaces is **deleted**, not
switched off.

**It takes no state parameters, on purpose.** Posture, speaking and stealth all come from the one
live engine through `QuantumStateRuntime` (B4), so a caller cannot hand it a state that disagrees
with the unit. What a caller chooses is *presentation* — plate set, size, whether the ambient
carrier runs — because that belongs to the surface, not to what she is doing. The dev screen now
calls the same composable, so the two cannot drift.

The enabling step was one line: `:app` did not depend on `:quark-avatar`, which is why CONFIG had
been reaching the avatar by class-name string. `:app` → docked module is the allowed direction and
`:quark-avatar` depends only on `:core` and `:app-shell`, so it adds no cycle.

**One real defect, caught on device.** The first build put the acquisition panel's body text directly
on her lit chest plate, barely readable. Fixed with a vertical CRT-ground scrim — transparent across
her face, opaque by the time it reaches the copy. A gradient rather than a panel deliberately: it is
the falloff language the housing and App Shell already use, so it reads as the screen fading rather
than as a drawn box over her, which the house style forbids.

### W3 — QUARK's settings panel

Everything about *her*, in one place the Operator can find.

**What it replaces, and why that mattered.** The voice controls and the brain's kill switch were
behind a **triple-tap on the title**, described in the source as "visible only if you know to look".
As engineering scaffolding that was fine; as the only route to her voice model it was not. It is a
visible `QUARK ▾` control now, and the panel is grouped by what the Operator is thinking about —
**// MIND** (weights status, `[ MANAGE WEIGHTS ]`, fallback kill switch) then **// VOICE** (speech,
identity, `[ IMPORT VOICE MODEL ]`) — rather than by which subsystem owns each flag.

`forceAcquisition` is new: the model-acquisition panel had been shown only while `!brainLoaded`, so
once she was online there was **no way back to it** — no re-import, no way to see what was on the
device. `[ MANAGE WEIGHTS ]` reopens it.

*Placement was wrong first time:* nested in the title's centre column the panel expanded inside the
header Row and sat across the STOW control. It is a sibling below the header row now, full width.

**A correction to what this session first reported.** Recon initially concluded that the LLM download
route "is not surfaced in any UI" and that the Operator had no way to obtain the 2.59 GB brain. That
was wrong — `ModelAcquisitionPanel` already existed and does it properly on first run, with
progress, offline side-load instructions and error states. The real gap was narrower: no way to
reach it *after* load, and the voice half being hidden.

### Still outstanding

`QuarkModelConfig.DOWNLOAD_URL` is **`""`**. `[ ACQUIRE WEIGHTS ]` cannot download until that is
filled in; `PICK FILE` and `IMPORT FILE` work today. That is a Director decision about where the
weights are served from, not something to guess in code.

### Verified

Trigger → `QuarkAssistantActivity` confirmed by `dumpsys activity`; the panel's rows confirmed
present by `uiautomator`; zero crashes (`grep -v adbd` first). Full gate green: `:core`,
`:app-shell` and `:quark-avatar` tests plus `:app:assembleDebug`. APK **195.11 MB**.

Nothing committed.

---

## Phase 23 — W1: the C1 harness

`tools/c1_field_measure.py`. C1 has stayed open for eight phases because the Fold 6 has never been
connected. This makes the whole pass one command, so the moment it is plugged in the measurement is a
run rather than an afternoon of remembering how it was done.

### What it measures, and why those two things

**The AMBIENT loop's battery cost**, by coulomb counter (`/sys/class/power_supply/battery/
charge_counter`, microamp-hours) over matched windows with AMBIENT on and off. Not `dumpsys battery`:
that reports whole percent, which over five minutes is 0 or 1 and tells you nothing.

**Frame cost** via `dumpsys gfxinfo`, across four scenarios: idle with AMBIENT off, idle with AMBIENT
on, the materialise, and a state change.

Those are the two things the house style actually put at risk. QUARK is specified as "static at rest
(zero idle redraw)" *because* she sits on an always-visible surface on a battery-as-vitality tool,
and AMBIENT is a flagged departure from that rule whose cost decides whether it ships on by default.

### Three things it gets right that are easy to get wrong

**It refuses to fabricate.** On an emulator the battery section runs *not at all* and the report says
why, with a banner stating the run is a harness smoke-test and not C1 results. Detection is
`ro.hardware in (ranchu, goldfish)` OR `ro.build.characteristics` containing "emulator" OR a model
starting `sdk_` — three signals so that any one of them changing does not silently turn a VM into
"real hardware".

**It unplugs the battery.** The device is on USB because adb is on USB, so without
`dumpsys battery unplug` the whole window measures the charger. Restored in a `finally`, because a
device left unplugged reports as discharging until it is rebooted.

**It finds controls by label, never by coordinate.** C1's entire purpose is to run somewhere other
than the 1080x2424 phone everything was tuned on. Hard-coded taps would hit the wrong row on a Fold 6
and the run would confidently report numbers for the wrong thing.

It drives the **avatar dev screen** rather than the Assistant View, deliberately: since Phase 22 both
draw the same `QuarkProjection`, but only the dev screen exposes AMBIENT, so only it can do the A/B.
*That makes CONFIG's dev-preview row load-bearing for measurement* — it should not be retired
without giving this harness another way onto an instrumented surface.

### Smoke test, and one real result that does transfer

Run on the emulator. The harness executed end to end and produced no battery numbers, as designed.

The frame *timings* from a desktop GPU mean nothing for the Fold 6. But one row is not a timing, it
is a **correctness property**, and that one does transfer:

| scenario | frames rendered in 10 s |
|---|---|
| idle, AMBIENT off | **0** |
| idle, AMBIENT on | 601 |

Whether a Compose surface recomposes at all is device-independent. **Zero frames is the
zero-idle-redraw guarantee measured by the platform's own frame counter** — until now it had only
been shown by diffing screenshots, which cannot distinguish "not drawing" from "drawing the same
thing". And 601 frames is ~60/s of continuous redraw, which is precisely why the battery question
needs real hardware rather than an argument.

### One honesty fix the smoke test forced

The first run reported `p50 4950ms` for the AMBIENT-off row — percentiles computed over **zero
frames**. That reads like a five-second frame when it means the opposite. Percentiles are now blanked
whenever `frames == 0` and the row is annotated `no frames drawn`. A harness built to stop bad
numbers reaching this log should not be the thing that puts one in it.

Reports land in `renders/c1/c1-<model>-<timestamp>.md`.

Nothing committed.

---

## Phase 24 — the first real-hardware pass, and what nine phases of emulator work had hidden

The Director sideloaded to the Fold 6 and tested. **This is the first time this project has been run
on the target device**, and it immediately found things the emulator never would have.

That is now a standing rule, recorded in CLAUDE.md: after any major or significant change, build the
APK and hand it over for a Fold 6 pass.

### Fixed: nothing was ever full screen

Android's clock, signal and battery sat on top of the CRT surface on **every screen** — launcher,
QUARK, all nine modules. The cause was uniform and had been there from the start: all eleven
activities called `enableEdgeToEdge()`, and **nothing anywhere ever hid the system bars**.
`enableEdgeToEdge()` only means "draw BEHIND the bars"; it does not remove them.

For this product that is not cosmetic. The house style forbids a drawn bezel precisely so nothing
frames the phosphor, the launcher takes the HOME intent so it IS the surface, and a second OS's
status bar contradicts the whole premise — while also overlapping the App Shell's own nameplate,
which carries the information the Operator actually needs.

`app-shell/FieldUnitDisplay.kt`: one `engageFieldUnitDisplay()` helper, called from all eleven
activities in place of `enableEdgeToEdge()`, plus an `onWindowFocusChanged` hook that re-hides.
Behaviour is **transient, not sticky** — an edge swipe brings the bars back for a few seconds. A
field tool must not trap its Operator away from the system, and the Fold 6 is the Director's daily
phone; same reasoning as the M1 rollback rule. Verified on device: the QUANTUM OS nameplate now sits
at the very top with no clock, signal or battery anywhere.

### Fixed: QUARK could not be looked at — HOLSTER

Since Phase 22 she fills the Assistant View, with the conversation and rail over her lower frame —
permanently. `[ HOLSTER ]` in the header stows the log, the rail, the panels **and the scrim** (which
exists only to keep copy legible, so with no copy it was just dimming her for nothing). `[ DEPLOY ]`
is the way back, and it is in the header for the same reason the system bars are transient: never
strand the Operator.

### Diagnosed: the voice model was never imported, and nothing said so

The sherpa native libraries **are** in the APK for arm64, so that was not it. The Kokoro model is
deliberately not bundled and must be imported once — and selecting QUARK-H2 without it does not
error, it **silently falls back to the placeholder**, which from outside is exactly what "the voice
model did not work" looks like. Until Phase 22 the import control was behind a triple-tap nobody
would find.

The QUARK panel now reports it outright:

    MODEL          NOT IMPORTED
    MISSING: model.onnx · tokens.txt · espeak-ng-data/ · voices.bin
    [ IMPORT VOICE MODEL ]

`--warn` red when not ready, per the rule that red means access-denied rather than decoration. That
row would have answered the question on the spot.

### NOT reproduced: the phosphor sync failure

Reported: the hue did not propagate to CONFIG, MAPS and the other modules — only the launcher and
QUARK followed. **It does not reproduce on the emulator, and I could not find it by reading either.**

Ruled out by inspection: multi-process (there is none — statics are shared), a module holding its
own hue copy (CONFIG's ViewModel exposes `PhosphorHueRuntime.activeHue` itself, not a snapshot), any
module failing to pass `themeColor` (all nine pass it), and nav using different chrome (it collects
the runtime and threads `activeHue` down to `NavUi` and `MapCanvas`).

Ruled out by testing all three paths on device:

| path | result |
|---|---|
| cycle in CONFIG → launcher, MAPS, AUDIO | all followed |
| cycle in the Vitality panel → launcher, CONFIG | both followed |
| cycle, then `am force-stop`, then cold-launch CONFIG | persisted (`phosphor_hue=AMBER` in prefs) and correct |

*One measurement trap worth recording:* MAPS first read as "not synced" because the colour sampler
averaged in Android's location-permission dialog and MapLibre's tiles. Cropping to the nameplate
band showed it green like everything else. The screenshot is the evidence, not the mean.

**So this one is still open and needs the Director's repro:** which surface the hue was changed
FROM, which module showed the wrong colour, and whether that module had been opened before or after
the change. There is a real defect here — it was seen — but guessing at a fix for something that
cannot be reproduced is how a working path gets broken.

### One more thing the Fold 6 pass confirmed for free

The phosphor-tinted trigger badge (Phase 22) re-tints correctly across hues — visible amber in this
session's screenshots after the hue was cycled, having been cyan before.

Nothing committed.

---

## Phase 25 — the THINKING plate loses the furrowed brow

Director's call, 2026-08-22, alongside the Fold 6 audio round: the furrow is out. It reads as
displeasure rather than thought, and QUARK is a peer-grade aide, not a disapproving one.

### This is a revision of SCAN, not a fifth state

Worth stating plainly because the request came in as "the THINKING plate" and the codebase does not
have one under that name. Panel 04 defines **`SCAN (THINKING) — Focused. Analyzing. One moment.`** —
SCAN *is* the thinking state. And the shipped `scan` prompt in `flux2_state_plates.py` says, in so
many words, `brows a fraction drawn in`. So the thing to change was already identified by name in
the prompt that produced it; no new state, no new pipeline.

With the brow ruled out, the thinking has to live in the mouth instead. The Director gave three
directions, and every candidate below states the relaxed brow as an explicit **NOT** — the model
reaches for a furrow unprompted the moment the word "concentration" appears, which is presumably how
it got into the shipped plate in the first place.

### Results

Candidates are `think_a` / `think_b` / `think_c` in `flux2_state_plates.py`, seeds 10_004–10_006,
rendered from the same `QUARK_HOLOGRAMBUST_FRONT.png` source as every other state plate. Comparison
sheet: `renders/state/thinking_candidates_sheet.png`, built by the new `comfy/thinking_contact_sheet.py`
— head-crop only at a fixed crop box, because at full-bust size the entire difference between these
candidates is a few dozen pixels of mouth and a side-by-side of whole figures hides exactly the thing
being judged.

**A — lightly pressed lips, firm straight line. Works at denoise 0.30.** Reads composed and
attentive; clearly distinct from the IDLE source, which has a softer, fuller mouth. The safest of
the three and the closest to what ships today minus the furrow.

**B — questioning half-smirk. Works, but needed denoise 0.42–0.52, not 0.30.** At 0.30 it was
indistinguishable from A — the instruction simply did not land. This is the same restraint/uptake
trade-off HAPPY hit in Phase 12, but pulling in the opposite direction: HAPPY had to be pulled DOWN
to 0.30 to stop the model over-delivering a smile, while B had to be pushed UP because an asymmetric
micro-expression is a smaller ask than the model's default step size. Both d42 and d52 are on the
sheet; d52 is unmistakably a smirk and arguably drifts toward amused rather than considering, which
would put it in HAPPY's register. **Director's pick between d42 and d52 — flagged, not chosen here.**

**C — tongue against the inside of the cheek. NEGATIVE RESULT. Three attempts, no change.**
Rendered at denoise 0.40, 0.52 and — after a full rewrite — 0.55, and the mouth came back centred,
symmetrical and unchanged from the neutral plate every single time. The rewrite (`think_c2`) removed
the word "tongue" entirely and asked only for the visible geometry: lips pushed distinctly off-centre
to her left, left cheek visibly rounded and bulging, right cheek flat, with the softening words
("subtle", "gently") deleted. Still nothing. The only thing 0.55 changed was the neck circuitry,
which drifted asymmetric — i.e. the denoise budget went somewhere, just not to the mouth.

Reading: the model will not act on an instruction about a structure it cannot see, and restating it
as external geometry does not rescue it, because a one-sided closed-mouth bulge is not a shape this
model appears to hold a strong prior for. Pushing denoise further is the wrong lever — it buys
identity drift, not this expression. If the Director wants direction C specifically it needs a
different tool (a posed reference to edit toward, or an inpaint masked to the mouth region), not a
higher number in this script. Recorded rather than quietly dropped so nobody re-runs it in three
months.

### Director's pick, and what shipped

**B at denoise 0.30.** d42 and d52 were rejected in the Director's words as "a villain smirk" — which
is the same register warning this entry already flagged from the other side, confirmed from the
chair. So the shipped plate is the one where the smirk instruction only *partly* lands: lips closed
and set, with a faint asymmetric lift at her right corner. Not the broad half-smirk B was written
for; a considering mouth. It is distinct from IDLE (whose mouth is fuller and softer) and from HAPPY
(which lifts both corners), so the four plates still read apart — checked on the set sheet, not
assumed. **The brows are relaxed and level. The furrow is gone**, which was the entire brief.

Exported to `export/state/scan_keyed.png`, replacing the furrowed plate. The old one is recoverable
from git (commit `bf51aac`) if the Director wants it back.

### The neck-glow concern was measured, and it was wrong

This entry originally flagged, by eye, that candidate A came back with a weaker neck glow than the
shipped plate and that the winner would need a glow-matching re-render. **Measured, that does not
hold**, and the re-render round was not needed. Percentage of strongly-cyan pixels in the neck band
(`G-R > 40`, alpha-masked), and mean `G-R` across that band:

| plate | glow px | mean G−R |
|---|---|---|
| IDLE (shipped) | 20.28% | 34.71 |
| SCAN (shipped, old) | 39.49% | 44.99 |
| HAPPY (shipped) | 39.54% | 42.39 |
| WARN (shipped) | 14.17% | 26.56 |
| **B d30 (shipped now)** | **36.49%** | **41.21** |
| A d30 | 35.09% | 40.60 |
| B d42 / d52 | 29.56% | 37.68 |

B at d30 lands inside the SCAN/HAPPY band. A was never the outlier it looked like. Worth keeping as
a method note: on this plate set, glow differences are visible to the eye at a magnitude the numbers
call negligible, and the numbers are the ones to trust — a re-render round was nearly spent on an
artefact of looking at two images side by side at different moments.

### The shared crop box, recovered — write this down

`chroma_key.py`'s own docstring warns that keying each state independently gives each its own tight
bbox, so the avatar jumps by a few pixels whenever the state changes, and that a shared box is what
makes the set register. **The box the shipped set was keyed with was not recorded anywhere.** It is:

    python chroma_key.py <src.png> <dst.png> --keep-base --box=121,40,900,996

Recovered empirically rather than guessed: the candidate's own natural bbox is `(121, 40, 900, 936)`
— same width and scale as the set, but 60px shorter, because the default `BASE_CUT_FRACTION` of
0.915 cuts at row 936 while the set was cut at 996 (≈ 0.973). Sliding the candidate's alpha mask
against the shipped `scan_keyed.png` over a ±120px search found the registration at **IoU 0.9989**,
which pins the box exactly. The exported plate then measures **IoU 0.9970 against `idle_keyed.png`**
— i.e. it registers with the set. Any future state plate must use this box.

### Installed — and the export was not the last step

Worth writing down because it was nearly missed: the state plates ship as **renamed drawables**, not
as the `*_keyed.png` exports. `quark-avatar/src/main/res/drawable-nodpi/quark_state_*.png` (bust) and
`quark_body_*.png` (body), referenced from `QuarkState.kt:59-73`. Exporting `scan_keyed.png` alone
changes nothing on device.

So both sets were finished:

* **Bust** — new plate copied through to `quark_state_scan.png`. scan/idle silhouette IoU **0.9970**.
* **Body** — this is the **default presentation** (Phase 18) and still had the furrowed SCAN.
  Re-rendered from `QUARK_HOLOGRAM_FRONT.png` with the same `think_b` prompt at denoise 0.30 and
  re-keyed via `body_state_key.py` (its `GENERATED["scan"]` now points at the new render). The
  recomputed shared box came back **(227, 26, 793, 1024) → 566×998** — identical to the installed
  set, so nothing shifted; residual spill **0.000%** on all four; scan/idle IoU **0.9958**.
* Both confirmed present and byte-identical inside `app-debug.apk`. The rebuilt APK is the same byte
  size as the previous one — a PNG-compression coincidence, checked rather than trusted.

The same prompt transferring cleanly across two framings is the Phase 12 claim holding up: the state
prompts name the face and the accent glow, never the framing.

### Not done here

Candidate C is not resolved and is not going to be by this pipeline (see above). Nothing else in the
state set was touched: IDLE, HAPPY and WARN are the shipped plates, unmodified. The AGSL overlay
shader tints the emissive accent live from `PhosphorHueRuntime`, so no per-hue variant of the new
plate exists or is needed.

Comparison sheets: `renders/state/thinking_candidates_sheet.png` (the six candidates) and
`renders/state/state_set_after_thinking.png` (the four shipped plates as they now stand).

---

## Phase 26 — C2, the fold: the framing was not "untuned", it was width-driven

The priority list called this "FRAMING, baseOverhang and the housing fractions are all tuned on one
1080x2424 portrait — never looked at on either Fold screen." Looking at it, the problem is not that
the numbers are untuned for a second screen. **The sizing rule itself only works on a tall narrow
viewport**, and fails structurally on anything else.

`QuarkPlateView` sized QUARK purely off viewport WIDTH: `plateWidth = maxWidth * framingScale`. These
plates are much taller than they are wide, so height is the dependent variable. Computed:

| viewport | BODY @100% plate height |
|---|---|
| 1080x2424 (the tuned screen) | 0.79 of screen — fine |
| ~2160x1856 (Fold 6 inner) | **2.05x screen height** — head off-frame |
| 2424x1080 (landscape) | **3.96x screen height** — torso only |

On the inner display the Operator would have been looking at a chest.

### Clamp the BASE, not the result

The obvious fix — cap the final plate height — was written, then rejected before it shipped: it makes
FRAMING a **dead control** on the inner screen, because both steps hit the same ceiling and render
identically. That is precisely the "four pixel-identical screenshots" defect `QuarkPlateView` already
carries a warning comment about, from when every setting was being silently clamped to the viewport
width. Re-introducing it one line below that warning would have been poor.

So the clamp applies to the base size and FRAMING scales the result:

    baseHeight  = min(maxWidth / aspect, maxHeight * baseHeightFill)
    plateHeight = min(baseHeight * framingScale, maxHeight * 0.99)

`RenderMode.baseHeightFill` is **derived, not eyeballed**: it is what the tuned phone already produces
at FRAMING 100% (BODY 0.786, BUST 0.547), rounded a hair up so float equality cannot make the clamp
bind there. BODY 0.80, BUST 0.56.

### Verified across three viewports, at every reachable setting

Not just the defaults. A first attempt used 0.86/0.76 and **silently clamped BODY@125%** — a framing
the Director can reach from the control — on the very screen it was supposed to leave alone. Caught by
computing all four mode×framing combinations rather than checking the default one:

* **Tuned phone: all four unchanged**, bit for bit.
* **Fold inner / landscape:** QUARK is drawn at the SAME PROPORTION the Director already approved
  (0.80 / 0.56 of height at 100%), not a different composition — and FRAMING still moves her
  (0.80 -> 0.99 on the inner screen).

### Not done, flagged

**No `FoldingFeature` / hinge awareness.** On the inner display QUARK is centred, which puts her
across the hinge. That is a separate problem from the overflow fixed here and a look call on
hardware. Also untouched: the App Shell chrome (nameplate, channel strip, action rail) has had no
adaptive pass at all — C2 named the avatar's framing, and that is what this is.

**Deliberately no `WindowSizeClass` dependency.** The priority list suggested one. A continuous clamp
against the actual measured viewport handles the fold's intermediate and table-top postures, landscape,
and any future aspect, with no new dependency to resolve — size-class buckets would answer a coarser
question than the one that was actually broken. Flagged as a deviation from the written plan.

---

## Phase 27 — identity: the trigger goes hologram, the badge comes on-palette

Two pieces of Director reference art landed this round: `reference/QUARKICON_HOLOGRAM.png` (the
hologram trigger this log has been expecting since Phase 22) and
`reference/QuantumLauncherIcon_reference.png` (the app badge).

### The trigger swap was measured, not judged

`QuarkTriggerService.IrisView` maps the source luminance→hue through a ColorMatrix, so "does this art
work as the trigger" is not a taste question -- it is whether the source's LUMINANCE survives being
collapsed to one channel and shown at 52dp. Both candidates were run through the real matrix at the
deployed size in all three hues:

| source | coverage | luma spread | std | mean saturation |
|---|---|---|---|---|
| current `QUARKIcon` | 76.6% | 176.0 | 58.6 | 0.541 |
| hologram (new) | 65.6% | 170.3 | 56.4 | **0.709** |

Equivalent under tint; the hologram is better at 78px because its ring geometry and bracket ticks are
cleaner while the current art's finer internal detail muds. Installed.

**This log's own prediction was wrong and is corrected here.** Phase 22's priority list said a
hologram source "would drop the tint hack". It does not. The hologram is *more* saturated than the
art it replaces (0.709 vs 0.541) -- hologram blue is itself a saturated colour -- and it must render
green and amber as well as cyan regardless. The luminance→hue mapping stays. The win is legibility.

The previous badge is retained at `export/trigger/quark_trigger_classic_256.png`: the Director wants
a coloured cosmetic/theme pack later, and that art is its natural starting point.

### The badge: flagged first, then rebuilt

The reference is the Director's composition and it is kept -- Q-ring lockup, segmented arcs and
bracket ticks, hexagon ground, circuit traces inside the ring. What could not ship as delivered:
measured mean saturation **0.603**, running cyan->magenta->purple over chrome with an Earth-blue
horizon, against a phosphor-only house rule; app-store gloss against a "used-future" register; a
strapline illegible at icon size; and its own rounded-square badge, which Android would double-mask.
Raised with the Director rather than quietly shipped, per this track's standing rule. Their call:
re-render on-palette.

Also flagged, deliberately not "fixed" in art: the reference strapline reads **QUANTUMOIS PROJECT**,
not QUANTUMOS. The strapline is dropped from the badge, so the typo never ships -- but the source art
still carries it.

### Two stages, because diffusion is not exact

* `comfy/app_icon_repalette.py` -- Flux 2 Klein edit, **importing `graph()` from
  `flux2_state_plates.py`** rather than copying it, so this pipeline still has one graph. Denoise is
  the whole story again: **0.62 left the chrome, the planet, every line of text and the frame
  intact**; 0.88 is what actually re-palettes. Higher is safe here in a way it never is on the
  plates -- there is no facial identity to protect, only a composition.
* `comfy/app_icon_finish.py` -- everything that has to be exact: crop off the render's own badge
  frame, re-derive every pixel as luminance x the **exact** locked token, fit the lockup to the
  66/108dp adaptive safe zone. Result: **0 off-token pixels**, and the Q stays readable under both
  circle and squircle masks down to 48px.

GREEN ships as `ic_launcher_foreground.png`. AMBER and CYAN are exported to `export/icon/` for the
theme pack. The old placeholder vector foreground was deleted -- keeping it would have been a
same-name resource collision against the new nodpi PNG.

Sheets: `renders/icon/app_icon_result.png`, `renders/icon/app_icon_masked_preview.png`,
`renders/trigger_hologram_compare.png`.

### Where ComfyUI belongs, and where it does not

The Director asked whether ComfyUI should be the icon pipeline. Split answer, recorded so it does not
get re-litigated: **yes for illustrative badges** -- app icon, trigger medallion, anything painterly
-- now proven twice. **No for the ~36 UI line glyphs**: those need exact geometry, uniform stroke
weight, legibility at 12-20dp and live retinting from `PhosphorHueRuntime`. Diffusion gives raster
approximations with inconsistent stroke weight and no vector. `QuantumIcon`'s Canvas/Path discipline
stays the route there.

---

## Phase 28 — the badge is centred on what the eye sees, not on its bounding box

Director, end of session: after the strapline and planet came off, the Q sat high with "~20%
emptiness at the bottom".

### The box was already perfectly centred

Measured on the shipped file before touching anything: ink rows **83..348 in a 432 canvas, centre
216 — exactly the canvas centre**. So the obvious reading (the fit routine got the maths wrong) was
wrong, and nudging it by hand would have been fixing a symptom.

What the box did not account for: the bottom third of it is **near-invisible**. The re-palette left
faint low-alpha residue in the region the wordmark and planet used to occupy — hexagon texture and
arc ghosts, real ink to `np.where`, nothing to the eye. The BRIGHT mark spanned only rows **90..276,
centre 183** — 33px above where the numbers said it was centred. The Director was reading the mark;
the code was reading the bounding box.

### Extent and position are now measured separately

`app_icon_finish.py`'s `fit_safe_zone()` uses two thresholds: the **full-ink box sets the SCALE**, so
nothing is clipped and the faint arcs still fit the safe zone, and the **bright-ink centroid sets the
POSITION**, so the lockup lands where it is looked at. Offset clamped so the whole artwork stays on
the canvas.

**One trap worth recording, because the first attempt fell in it.** The bright set has to be measured
**after** the downscale, not before. Measured on the full-size render the bright region looks
centred (bcy/height = 0.504); measured on the 267px art that actually ships it is at 0.376. LANCZOS
averaging the faint hexagon field toward black at ~3x reduction is the difference — most of that
field drops below the threshold while the ring survives intact. Measure the pixels that ship.

Result: bright mark **centre_y 216, delta +0** from the canvas centre, 28% clear above and below.
The full-ink box now sits +32 off centre, which is correct and intended — the faint residue trails
below the mark and some of it falls outside the mask, which is exactly where it should go. All three
hues regenerated, **0 off-token pixels** each; verified in `app-debug.apk` by md5 against the source.
Sheet: `renders/icon/app_icon_centred.png`.

---

## ▶ RESUME HERE — next session

**State.** Three Fold 6 rounds have landed since the last resume block, and the identity/icon work is
now complete end to end. What shipped, newest first:

* **Badge centring** (Phase 28) — above.
* **Fold 6 round 3** — six defects fixed: RADIO's Material controls replaced with the house idiom
  (new shared `SteppedSlider` in `:app-shell`), RADIO's carrier-wave meter no longer drawing outside
  its own canvas (and its needle now shares the arc's pivot), the doubled ring removed from the
  floating trigger, CAM's focus reading moved inside its reticle, CAM's mode dial mounted on the
  console corner as a 3/4 dial, and `Glyph.Infinity` redrawn as a real lemniscate.
* **APPS-grid icon pack** — 25 drawn marks matched by package id, with unrecognised apps falling back
  to their own icon put through the phosphor luminance map. The grid was the last large off-palette
  surface in the OS.
* **The Glyph Sweep** — 40 font-fallback marks across 10 modules replaced with drawn glyphs; verified
  40 → 0 and still 0 after the RADIO restyle.
* **Phases 25–27** — THINKING plate (Director's pick: candidate B at denoise 0.30, both bust and body
  sets), hologram trigger art, app badge re-paletted from the Director's reference.

Full code-side detail lives in `BUILD_LOG.md`'s RESUME HERE; this file keeps the art-track reasoning.

**Everything above is compile-verified and green** (`test` + `assembleDebug`, 86 unit tests, 0
failures) **and none of it is on-device-verified beyond the round that prompted it.** The Fold 6 is
still the judge.

---

### 1. The batched Fold 6 pass — this is the next session's first job

It has been deferred three times now and the backlog of unverified work is the largest it has been.
In rough priority:

1. **RADIO** — does it now read as the same OS as the other modules? And do the stepped bars tune
   and set volume comfortably by drag? They quantise, so they will feel notchier than the Material
   pills did. That is intended; whether 32 segments (carrier) and 20 (volume) is the right grain is a
   feel call only the hand can make.
2. **CAM** — tap to focus and confirm nothing is occluded; check the corner dial clears both the
   SENSOR ISO row and the CORE SYMMETRY label; look at the ∞ tiles in both scales.
3. **The floating trigger against a LIGHT wallpaper as well as a dark one.** The CRT-ground disc is
   now the only thing separating her from the background, since her ring is gone.
4. **The APPS grid** — recognised apps should show a themed mark, unrecognised ones their own
   silhouette in phosphor rather than vendor colour. The failure mode to watch is a very dark icon:
   little luminance to map means little to see. Report any app whose match is plainly wrong; the
   rules are heuristic and trivial to extend once real package ids from the Fold are known.
5. **Icon legibility at 12–20dp across all three hues.** This check finally means something — before
   the sweep it was testing Android's emoji font, not house art.
6. **C1's battery number.** Two house-style departures (the 320ms state crossfade, the AMBIENT loop)
   have been waiting on it for three sessions. Without it that fork rolls again.

### 2. Then, in rough priority

- **The hinge.** C2 fixed the avatar's framing overflow on the inner display, but there is no
  `FoldingFeature` awareness anywhere: QUARK is centred, which puts her across the fold. Separate
  problem from the overflow, and a look call on hardware.
- **The App Shell chrome has had no adaptive pass at all** — nameplate, channel strip, action rail.
  C2 was scoped to the avatar's framing, which is what it named.
- **`QuarkModelConfig.DOWNLOAD_URL` is still `""`.** `[ ACQUIRE WEIGHTS ]` cannot download until it
  is filled in; PICK FILE / IMPORT FILE work today. A Director decision about where ~2.6 GB is
  served from, not something to guess in code.
- **Two dead-vocabulary items.** `Glyph.Shutter` (CAM's shutter is a bespoke Canvas dial) and the
  `AppGlyph` pack's unreachable entries. The first wants a ruling like `ModeToggle` got; the second
  is inherent to a third-party pack and is why it is a separate enum.
- **The QUANTUMOIS typo** is still on `reference/QuantumLauncherIcon_reference.png`. The badge drops
  the strapline so it never ships, but the source art carries it.
- **`QuantumLauncherIconRef_NoText` / `_Hologram`** arrived after the badge was already built from
  the older reference. Both are better sources — no strapline to crop, and the hologram has alpha.
  Re-running the badge from `_NoText` would drop the frame-cropping step entirely. Not done because
  the current badge came out clean; worth an hour if the badge is ever revisited.
- **`AppIconPackRef` has 56 subjects; the pack uses 25.** The rest (Crypto, Stocks, Hotel, Meditation
  …) have no counterpart in a field multi-tool. If the Director wants broader coverage, extend
  `PACKAGE_RULES` from real Fold 6 package ids rather than guessing at categories.

### 3. Standing notes for whoever picks this up

- **Local Gradle works** — `JAVA_HOME=C:\Program Files\AdoptOpenJDK\jdk-17.0.0.20-hotspot`, NOT
  Android Studio's JBR (Java 25, which Gradle 8.9 cannot run on and which fails with a bare,
  unreadable `25.0.2`). Several earlier sessions wrongly concluded local builds were impossible.
- **The geometry previews are NOT Compose renders.** `comfy/glyph_geometry_preview.py` and
  `comfy/app_glyph_preview.py` re-plot the same normalised coordinates in PIL. They are good at
  catching shape mistakes (they caught `Infinity` reading as "oo", `Settings` reading as a sun, and
  `Notes`/`News` being the same rectangle) and prove nothing about how Compose draws them.
- **The state plates ship as RENAMED drawables**, not as the `*_keyed.png` exports:
  `quark-avatar/src/main/res/drawable-nodpi/quark_state_*.png` and `quark_body_*.png`. Exporting a
  keyed plate alone changes nothing on device — this was nearly missed once.
- **The shared crop box for state plates is `--keep-base --box=121,40,900,996`.** It was undocumented
  and had to be recovered by mask alignment. Any future plate must use it.
- **ComfyUI's split:** right for illustrative badges (app icon, trigger medallion), wrong for UI
  glyphs (they need live retinting, exact geometry and uniform stroke weight at 12–40dp). Recorded so
  it is not re-litigated.

### 4. Standing rules

- **Ship the APK after any major or significant change** for a Fold 6 sideload pass -- do not wait to
  be asked. Recorded in `CLAUDE.md`. The first hardware pass found three defects that nine phases of
  emulator verification had missed.
- **Nothing is committed.** The Director commits personally.
- **Flag design forks, do not lock them silently.** Proportion, palette and look calls go to the
  Director. This session alone that rule caught the app-icon reference's palette conflict, the
  denoise pick for the THINKING plate, and the FRAMING behaviour on the fold.
- **Verify with a decisive test, not a plausible fix.** The habit paid for itself repeatedly: the
  "voice is broken" report was latency and not a voice fault; the neck-glow concern dissolved when
  measured; the badge's dead space was a bounding box counting invisible ink, not a centring bug.

*(The former resume list -- triage of the Phase 24 hardware pass, the phosphor-sync repro, the voice
model import, the first C1 run -- is retired: every item in it has since been closed. See Phases
25-28 and `BUILD_LOG.md` for what replaced them.)*
