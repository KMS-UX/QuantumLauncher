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

## ▶ RESUME HERE — Phase 1: base mesh + rig blockout — DONE, first pass
`blender/scripts/01_base_mesh_and_rig.py` — headless, run via:
```
"C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" --background --python art/quark-avatar/blender/scripts/01_base_mesh_and_rig.py
```
Builds a **proportion-accurate blockout** (167cm, athletic/feminine, T-pose) procedurally from
the color sheet's turnaround + scale-reference measurements: lofted torso/head, two lofted
legs + block feet, two lofted arms + block hands, joined into one `QUARK_Base` object; 5 material
slots seeded from the sheet's locked hex/gloss/metallic values (Ceramic/Synth-Skin/Graphite/Metal
Alloy/Emissive Cyan — whole body currently defaults to Synth-Skin, no panel breakup yet); a
matching `QUARK_Rig` armature (root→pelvis→spine→chest→neck→head, plus L/R
shoulder→upperarm→forearm→hand and hip→thigh→shin→foot chains) parented with automatic weights.
Saves `blender/quark_base.blend`; renders a 5-angle turnaround to `renders/blockout_turnaround/`.

**This is explicitly a blockout, not sculpted detail.** Procedural bmesh scripting cannot
reproduce the concept sheet's photoreal skin/panel fidelity — that needs a human sculpting pass
(or heavier manual Blender work) on top of this base. Flagging per `CLAUDE.md`'s "flag, don't
silently lock" rule rather than overclaiming fidelity.

## Refinement pass (2026-08-20) — issues 1 and 2 fixed, issue 3 partially addressed
Director confirmed the first-pass silhouette/proportions read right; three fixes applied to the
same script:
1. **Neck taper softened** — spread across four intermediate landmarks (129→150cm) instead of
   one sharp radius drop. Fixed.
2. **Feet and hands are now a continuous part of the limb loft** (extra ring landmarks
   flattening/elongating through ankle→heel→toe and wrist→hand→fingertip) instead of separate
   detached blocks. Fixed — both read as an actual foot/hand shape now, welded into the same
   mesh island as the leg/arm.
3. **The dark "notch" at the neck/shoulder junction — root-caused, not a mesh hole.** Spent real
   effort chasing this as a possible bad-normal/topology bug (rewrote cap-face winding to be
   explicitly computed rather than guessed, tried capped vs. open limb attachment ends, tried
   larger/smaller overlap discs) — none of it changed the artifact, which was the tell. A
   diagnostic close-up with a bright magenta world background confirmed **no background leaks
   through** (i.e. there's no actual hole — the surface is solid), and adding a strong overhead
   light removed the artifact from 4 of 5 turnaround angles (front/¾/back). It's a genuine,
   anatomically-expected concave crease at the collarbone under a sparse 2-light preview rig —
   confirmed by the fact that it only survives in the one viewing angle that stares exactly
   down the crease (the dead-on `left_side`/90° profile shot). Added a 3rd (overhead) and 4th
   (low, profile-angled) light; front/¾/back are now clean, the pure side profile still shows a
   softer version of it. **Not a blocker** — this is a preview-lighting artifact of the 4-light
   diagnostic rig, not something that will exist under the in-app CRT/phosphor shading or a
   real 3-point studio setup. Logged here mainly so a future session doesn't re-chase it as a
   mesh bug.
4. **Not addressed:** a smaller, same-class shadow/seam is still faintly visible at the
   pelvis/inner-thigh junction (same concave-crease-under-sparse-lighting cause, much less
   pronounced). Torso/legs/arms are still separate lofted forms visually touching at the joints,
   **not welded into one continuous quad topology** — fine for this blockout and for driving the
   armature via automatic weights, but a real retopology pass is still needed before this can be
   sculpted or deformed cleanly at the elbow/knee/hip bend limits.
5. No panel/plate breakup yet (the sheet's visible armor-segment lines on shoulders/hips/limbs) —
   whole body is one flat material index currently.

## ▶ RESUME HERE — Phase 2: retopology — DONE
Added `retopologize()` to the same script, inserted right after `build_mesh()` and before
materials/armature. Single pass: Blender's **voxel remesh** (`voxel_size=0.010`, applied as a
baked modifier) directly on the Phase-1 overlapping-forms mesh.

**Verified, not just eyeballed** (via a standalone check script reading the saved `.blend`):
- `disconnected islands=1` — torso/legs/arms/head are now one genuinely connected mesh, not
  separate touching forms.
- `non_manifold_edges=0`, `non_manifold_verts=0` — a real, watertight, deformation-safe solid.
- `tri=0, quad=21326, ngon=0` — output is 100% quads (the voxel remesher does this natively in
  this Blender version; no separate retopology tool needed).
- All 20 armature vertex groups present and an `ARMATURE` modifier on the object — automatic
  weights were re-run after remeshing (the old pre-retopology weights don't carry over, since
  remesh rebuilds vertices from scratch) and succeeded cleanly on the now-manifold mesh.

**A detour worth recording:** first attempt chained voxel remesh → QuadriFlow remesh
(`target_faces=8000`), matching how this kind of pipeline is normally described (voxel pass for
robustness against the overlapping input, QuadriFlow pass for a controlled, even quad budget).
QuadriFlow logged `"mesh needs to be manifold..."` and silently no-op'd despite the input
already passing every manifold/quad check above — left the dense voxel output (43714 faces)
untouched rather than reducing it. Root cause not fully chased down (possibly a Blender 5.2
QuadriFlow regression against voxel-remesh output specifically); dropped the QuadriFlow step
entirely and control face count directly via `voxel_size` instead (0.007→43714 faces,
0.010→21326 faces — used the latter). 21k quads is more than the original 8k target but a
reasonable, workable budget for a base mesh; revisit if a tighter budget matters later (finer
control needs either a working QuadriFlow pass or a Decimate modifier pass on top).

**Side effect:** the Phase-1 collarbone-crease lighting artifact (flagged as benign, not a mesh
bug) reads noticeably softer now that the surface is continuous instead of separate touching
forms — expected, since there's no longer an actual seam boundary at that location for shading
to catch on, just the underlying anatomical concavity.

**Not addressed by this pass:** panel/plate breakup (sheet's hard-surface armor-segment lines) —
still whole-body single material. Topology is even/quad but not hand-authored edge-loop flow
(no deliberate loop convergence at eyes/mouth/joints) — fine for a base/blockout mesh, would
matter more for a facial-animation-grade final mesh.

## ▶ RESUME HERE — Phase 3: texturing/materials — first pass DONE
Added to the same script, running after `retopologize()`:
1. **UV unwrap** (`uv_unwrap()`, Smart UV Project) — the mesh now has a real UV layout; needed
   before any texture painting/baking, not present before this pass.
2. **Panel/plate material breakup** (`_classify_material_index()`) — replaces the Phase-1/2 flat
   single-material assignment. No hand-authored panel-line art exists to follow, so this is a
   geometric stand-in: material assigned per face by height/offset bands matching the same
   landmark measurements the mesh itself was built from (elbow/wrist/knee/ankle/neck = Graphite
   seam bands, shoulder+hip ball-joints = Metal Alloy, head = Synth-Skin, torso/limb plating =
   Ceramic default, a spine strip = the Emissive accent). Visibly reads as a plated figure now,
   not a flat-shaded blob — confirmed in the renders, not just by code review.
3. **Palette comparison rendered, as decided** (`renders/palette_compare/`) — same mesh, same UVs,
   only the Emissive accent material's color swapped between the two frames:
   `sheet_neutral_*` = the hologram sheet's "Neutral" appearance token (#E6F1FF, its own
   idle/rest color) : `phosphor_green_*` = the `CLAUDE.md`-guardrail-compliant collapse to
   phosphor GREEN. Both are clearly visible and distinguishable in the renders — this is the
   actual side-by-side the Director asked to defer the final call to, not a placeholder.

**Honest scope note:** this is still a base/blockout mesh (Phase 2's ~21k-quad voxel-remesh
topology, not hand-authored edge flow) with a rule-based material split, not painted PBR texture
maps (base color/roughness/normal/emissive bitmaps) — that level of texture fidelity needs either
manual texture painting or a bake pass once UVs are laid out (which they now are, so that's
unblocked whenever it's prioritized). The `CLAUDE.md` "phosphor only / one token source" hard-
guardrail conflict (noted in the Decisions section above) is still unresolved and still applies
to whichever palette variant eventually ships — not resolved by rendering both, only visualized.

## ▶ RESUME HERE — Phase 3b: painted texture detail — DONE
Replaced the flat-color materials from the first Phase-3 pass with real procedural PBR node
graphs, then baked them to portable texture files — `art/quark-avatar/textures/`:
- `quark_base_color.png`, `quark_roughness.png`, `quark_emission.png` (1024×1024, on the mesh's
  own UV layout from `uv_unwrap()`).

**What the materials actually do now** (`make_material()`, all 4 physical materials except the
flat emissive accent): a Geometry-node Pointiness signal drives visible **edge wear** — convex
edges (panel corners, joint ridges) read lighter/scuffed and less rough than flat panel faces,
which stay grimier/rougher. A Noise texture layered on top breaks up flat color into subtle tonal
mottling so panels don't read as one digital flat color. A small Bump (fed by the same noise) adds
surface micro-detail. This is a direct, deliberate answer to `CLAUDE.md`'s "used-future" aesthetic
— the Phase-3a materials were pristine flat colors, which reads wrong for a field-worn multi-tool;
this pass is the actual wear/grime the house style calls for. Metal Alloy hardware gets the
strongest wear contrast (brushed/polished reads as most affected by handling); Synth-Skin gets the
least (it's skin, not armor).

**Baking, verified not assumed:** opened all three output PNGs directly — `quark_base_color.png`
shows the UV-unwrapped mesh with real per-region color AND the noise/wear pattern actually baked
in (not flat swatches); `quark_roughness.png` shows the same pointiness-driven variation in
grayscale; `quark_emission.png` is a clean white mask exactly where the spine-conduit accent
geometry lives, black everywhere else — confirms the bake correctly isolated each material's
contribution into the shared UV atlas.

**How baking works here, for whoever touches this next:** every material gets an `Image Texture`
node pointing at the *same* shared image, marked active; Cycles (switched in for the bake, restored
after) then bakes each material's contribution into the correct region of that one shared image
based on per-face `material_index` — no manual atlas-packing needed. The Subsurf modifier is
hidden from render during baking so the bake targets the raw low-poly UV'd cage, not a subdivided
mesh.

**Honest scope note, still true:** normal-map baking was skipped — meaningful normal detail needs
either actual displaced geometry or a proper high-to-low bake source, neither of which exists yet;
the bump node above is a live-render-only effect, not an exported map. This is still a rule-based
material split (no hand-painted panel-line art was available to follow), now with real procedural
wear on top rather than flat colors — a genuine step up in fidelity, not final production texture
art.

**A process takeaway, recorded generally (not just here):** the `blender-3d-modeling` skill this
session used has been updated with the reusable technical lessons from this pipeline — voxel
remesh already outputs all-quad manifold meshes directly in current Blender, QuadriFlow can
silently no-op on valid input rather than erroring (don't trust it blindly, verify actual output),
the Pointiness-driven wear recipe above, the shared-image multi-material baking technique, and the
magenta-background diagnostic for telling a real mesh hole from a lit-but-shadowed concave surface
(see the Phase 1 refinement section above — that's the artifact this test resolved). Worth reading
if a future session hits similar issues in a different project.

## ▶ RESUME HERE — Phase 3c: deeper texture fidelity — DONE
Closed both honest gaps flagged at the end of Phase 3b:

1. **Real normal map, no separate high-poly source needed.** Baked `type='NORMAL',
   normal_space='TANGENT'` — this captures the *final shading normal after the material's own
   Bump-node chain*, not just the low-poly mesh's own smooth normal, so the seam/rivet/noise bump
   detail genuinely exists in `quark_normal.png` as real normal-map data (created `is_data=True`
   so Blender treats it as non-color data, not sRGB). This isn't a technique that needs a
   sculpted high-poly to bake from — it's standard practice for a purely procedural material.
2. **Procedural panel-seam + rivet detail**, replacing the flat material-region split with actual
   hard-surface greeble: a Voronoi `DISTANCE_TO_EDGE` mask darkens + recesses (inverted bump)
   thin groove lines at cell borders (panel seams); a second, finer Voronoi `F1` mask picks out
   small flecks right at each cell's seed point, tinted like a fastener head and given a slight
   raised bump (rivets). Enabled on Ceramic (main plating, `panel_scale=7`) and Metal Alloy
   (hardware, denser `panel_scale=14`/`rivet_scale=34` — real fasteners read finer than plating
   seams); left off Synth-Skin (no rivets on skin) and Graphite (already reads as a seam itself).
3. **AO bake added** (`quark_ao.png`) — standard grounding pass, crevices at seams/joints read as
   occluded regardless of the material's own color.
4. Bumped texture resolution 1024→2048 to keep the finer panel/rivet detail from reading blurry.

**Verified, not eyeballed** (per the standing rule this session set for itself): the panel-seam
cell pattern is clearly visible directly in both the full turnaround renders and the raw
`quark_base_color.png`. Rivets are small by design and hard to confirm by eye at thumbnail scale,
so counted pixels directly instead of assuming: **958 pixels match the rivet-fleck color
signature** (of 4,194,304 total) — present and sparse, as intended, not zero. Only 38 pixels
remain at the *exact* flat ceramic tone with zero wear/mottle/panel contribution, confirming the
procedural layers are genuinely modulating nearly the entire plated surface, not just a few
visible patches.

**Still true / not attempted:** this remains a rule-based procedural stand-in for panel-line art,
not hand-authored seams following real anatomical/mechanical logic — a human hard-surface artist's
pass would place seams and fasteners with actual design intent (access panels, cable routing,
structural logic) rather than a Voronoi cell tessellation. Good enough to read as "plated and worn
hardware" at a distance; not a substitute for that pass if/when the Director wants it.

## ▶ RESUME HERE — Phase 3d: toward hand-authored panel art — DONE
Director asked to keep pushing panel-art fidelity toward hand-authored quality rather than
procedural randomness. Actually re-opened the reference sheet's "Details & Close-Ups" panel
(cropped to `reference/crop_details.png` for repeat inspection) instead of iterating on noise
parameters blind — it shows a handful of large, purposeful plates keyed to real joints (a
shoulder pauldron, a wide hip wrap, a rounded kneecap, a chest seam), not a uniform tessellation.
Three changes, all *placement decisions read off that reference*, not more randomness:

1. **Fewer, larger panels.** `panel_scale` dropped 7→3.2 (Ceramic) and 14→6 (Metal Alloy) — the
   previous pass's dense cell tessellation read as noise standing in for design; matching the
   reference's actual panel count/size reads as deliberate instead.
2. **Rivets gated to seam-adjacency.** Previously scattered uniformly across every panel face;
   now multiplied against a "near seam" mask (reusing the same Voronoi distance-to-edge field)
   so fasteners only appear clustered along a seam line — genuinely how real hardware is placed,
   not randomly across a flat panel. Verified count held (939 px, was 958) — gating changed
   *distribution*, not presence.
3. **Explicit hand-placed plates**, added as their own priority-ordered zones in
   `_classify_material_index()` rather than derived from noise: a shoulder pauldron (graphite,
   straddling the torso/arm boundary, matching the reference's "Shoulder & Arm" close-up), a hip
   wrap (metal hardware ring flanked by a graphite wrap band, matching "Hip & Waist"), a widened
   kneecap plate, and a chest-plate seam line across the upper front torso (matching "Chest /
   Upper Torso"). These coordinates were chosen by looking at the reference, not generated.

**Verified in the renders, not just claimed:** the shoulder pauldron and hip wrap are clearly
visible as distinct large shapes in the turnaround renders (compare to the previous pass's dense
random cracking) — a genuine step toward the reference's actual design language, confirmed by
re-rendering and looking, the same discipline as every other claim in this log.

**Honest ceiling, unchanged:** this is placement-by-hand-reading-the-reference within a still-
procedural generation system (Voronoi cells, not traced curves) — real hand-authored panel-line
art (a human artist placing every seam and fastener with full design intent — access panels,
cable routing, structural logic) is still a different, higher tier than what a headless pipeline
can produce. This pass moved meaningfully closer to that within the constraints of what's
achievable here; it isn't a replacement for that pass if the Director wants the real thing.

## ▶ RESUME HERE — Phase 3e: closing the remaining reference-vs-build gaps — DONE
Went back through the reference's close-up panels one more time for anything still unaddressed
rather than continuing to iterate on what was already built:

1. **Hand knuckle grooves.** The reference's "Hand (Palm)" close-up shows clear knuckle-joint
   gaps between finger segments; the actual mesh geometry is one flat paddle with no finger
   separation at all (a real geometry limitation from Phase 1/2, not fixable by material work).
   Faked the same visual read with three thin graphite bands crossing the hand at fixed
   intervals — texture-only, explicitly not real finger topology. Verified via a close-up render
   (`detail_hand_knuckles.png`, sent to Director) — reads convincingly as knuckle breaks at
   normal viewing distance despite being a flat paddle underneath.
2. **Shoulder-blade panels.** The reference's "Back / Spine Conduit" close-up shows the conduit
   flanked by two curved graphite plates (scapula panels), not bare ceramic either side of it.
   Added as an explicit zone. Clearly visible in the back-view turnaround render.
3. **Brushed-metal directional grain.** The sheet's own material spec calls Metal Alloy
   "brushed/polished" — a directional grain, not the isotropic Noise mottling every other
   material uses. Added a `ShaderNodeTexWave` (BANDS, fixed orientation via Object-space mapping)
   multiplied into roughness, only on Metal Alloy. Verified via close-up
   (`detail_hip_brushed_metal.png`) — genuine anisotropic-looking streaks catching the light,
   not flat isotropic noise.

**All three verified by rendering and looking, not assumed from the code.**

## ▶ RESUME HERE — Phase 3f: real finger geometry, replacing the knuckle-texture fake — DONE
The "keep pushing" direction after Phase 3e's gap-closing pass: fix the hand for real instead of
faking it. Added `build_fingers()` — five separate tapered digits (thumb + 4 fingers, individually
sized/offset/angled) branching from the palm as their own short lofts, each overlapping into the
palm volume the same way every other limb-to-torso join in this file works, letting the voxel
remesh weld them into the mesh automatically. Removed the now-obsolete knuckle-line texture hack
from `_classify_material_index()` — real finger separation reads correctly on its own.

**First attempt silently failed — caught by verification, not assumed working.** At the
established `voxel_size=0.010` (1cm), a close-up render showed the five fingers had blobbed back
into one paddle: 1cm voxels can't resolve ~1.5cm-thick digits with sub-centimeter gaps between
them. Face count barely changed from the pre-finger mesh (21,346 vs. 21,326), which was itself a
tell something was wrong before even rendering. Tightened `voxel_size` to 0.004 (this mesh is a
Blender-side asset for baking/rendering under the hybrid render-path decision, not a real-time
one, so the heavier result — 134,582 faces, up from ~21k — is an acceptable tradeoff for
correctness). Re-verified with a second close-up: **four fingers now clearly visible as separate
tapered volumes with real gaps between them** (thumb out of frame at that angle, not re-shot).
Re-ran the full topology check on the new denser mesh too, not just the render: still exactly
1 island, 0 non-manifold edges, 100% quads (134,582), all 20 armature vertex groups intact —
the retopology guarantees from Phase 2 hold at the new resolution, confirmed rather than assumed.

## ▶ RESUME HERE — Phase 3g: one more push — toes, angular seams, head accents — DONE
Three more additions in the same session, one of which was walked back after honest verification:

1. **Real toe geometry**, mirroring `build_fingers()` — `build_toes()` adds five separate tapered
   digits (big toe longest/most medial → little toe shortest/most lateral) branching from the
   foot, replacing the old single continuous taper to one point. **Render-based verification kept
   failing on camera framing/lighting, not the geometry** — four different close-up attempts
   either mis-framed the shot or caught the toes in deep shadow. Rather than keep burning attempts
   on camera angles, switched to a direct numeric check on the mesh data: sampled vertices in the
   toe region and binned them by X position — **6 separate contiguous clusters** (≈5 toes + one
   edge artifact), confirming real separation exists in the geometry regardless of how hard it was
   to get a clean render angle. Worth remembering: a failed screenshot isn't proof of a failed
   result — check the data directly when the picture won't cooperate.
2. **Angular (Chebyshev-distance) panel seams**, replacing the Euclidean-distance Voronoi used
   since Phase 3d. Rounded/organic cell boundaries read as cracked ceramic; squared-off boundaries
   read closer to an actual designed hard-surface panel line. (Tried Minkowski with a high exponent
   first for the same effect — its `Exponent` input isn't exposed on this node in this Blender
   version, threw a KeyError; Chebyshev gives the same angular character with no extra parameter.)
3. **Headband circuit + ear audio-module, both read off the reference's "Hair & Head Accessories"
   panel** (the head was otherwise a completely bare, zero-detail icosphere). The headband circuit
   (a thin emissive LED line at brow height) works well — confirmed clearly visible in renders.
   **The ear module was walked back after verification showed it wasn't working**: on the metallic
   material, with no environment map for it to reflect, it rendered as a flat black rectangle
   instead of a small sensor node — confirmed via close-up, then diagnosed as a genuine PBR
   consequence (tried brightening the world background first, which is a real, broadly-useful fix
   for metal readability generally, but didn't fix this specific case). More fundamentally, an
   axis-aligned material-region box can only ever produce a rectangle, never the small circular
   node the reference shows — the technique was wrong for this specific job, not just under-tuned.
   Removed rather than kept "fixing" a wrong approach. Not attempting full facial topology either
   — a different, much larger task than this pipeline's scope.

**Also fixed:** the world background brightness bump (item 3's byproduct) makes metallic surfaces
read better everywhere in this pass's renders, not just the one accent that didn't work out.

## ▶ RESUME HERE — anthropometric sanity check — a real finding, not yet acted on
Director asked for a check against real 167cm adult-female anthropometric data before closing the
session. Looked it up rather than relying on memory — cross-checked two independent sources:

- A standard ergonomics table (female 50th-percentile standing dimensions): shoulder height 82.1%
  of stature, hip height 50.6% of stature, knee height 36.7% of stature.
  [RoyMech Human Body Dimensions](https://www.roymech.co.uk/Useful_Tables/Ergonomics/Human_sizes.html)
- The Chumlea knee-height-to-stature clinical regression (widely used, validated formula):
  `height(cm) = 64.19 − 0.04×age + 2.02×knee_height(cm)`. Solved for a 167cm adult (age ~28) gives
  knee height ≈ 51.4cm (≈30.8% of stature).
  [TopEndSports Knee Height Calculator](https://www.topendsports.com/testing/tests/height-knee.htm)

**Scaled to 167cm, both sources put real knee height at roughly 51–61cm from the floor.** The
current mesh's knee landmark is at 33–36cm — built directly off the classic **Loomis 7.5-head
figure-drawing canon** ("6 heads from top = bottom of kneecap"), used explicitly and documented as
such back in Phase 1. That canon is a **stylized artistic convention, not measured population
data** — it deliberately elongates leg proportions for a more heroic/idealized figure, and the gap
here is substantial (33cm built vs. ~51–61cm real), not a rounding difference. Hip height shows a
smaller version of the same pattern: Loomis's own "half of stature" rule puts the built model's
pelvis at ~78cm, which is close to the classic art-canon expectation but ~4–7cm lower than the
ergonomics-table scaled value (~84.5cm).

**Not corrected here — flagged for the Director rather than silently picked**, per this track's
own established discipline: the reference sheet itself is a stylized sci-fi character design, not
a medical scan, so it's a genuine open question whether QUARK's proportions should follow the
idealized figure-drawing canon (matches how character sheets are conventionally built) or be
pulled toward literal population-average anthropometry (matches "real 167cm human" more
literally). Worth a decision before any further leg/torso landmark changes — re-deriving all the
downstream landmark math (hip/knee/ankle/foot, plus the armature bones and every classify-function
z-band tied to those heights) is real work, not a quick tweak, so better to decide direction once
than adjust twice.

## Session close — 2026-08-20

**What shipped today:** Phases 1 through 3g — base mesh + rig blockout, voxel-remesh retopology to
one welded all-quad manifold mesh, UV unwrap, region-based PBR materials with procedural wear,
baked texture maps (base color/roughness/emission/normal/AO), panel-seam + rivet hard-surface
detail read off the reference's close-up panels, real separated finger and toe geometry, and a
working headband circuit accent. All of it verified by rendering and looking — several dead ends
(QuadriFlow's silent no-op, the neck-shadow chase, the blobbed fingers, the black ear-module
rectangle) were caught by that discipline rather than reported as working. Current assets:
`blender/quark_base.blend`, `renders/blockout_turnaround/`, `renders/palette_compare/`,
`textures/quark_{base_color,roughness,emission,normal,ao}.png`.

**Next session: dedicated to detail work.** Scope, in priority order:
1. **Resolve the anthropometric-canon decision above first** — it's upstream of any further body
   landmark changes, so deciding it before touching geometry avoids doing leg work twice.
2. Continue pushing panel/material fidelity if the Director wants more (this session's own
   assessment: getting close to what texture-only techniques can add — real gains from here likely
   need either geometry work beyond retopology, or a human hard-surface artist's pass).
3. Everything still open from Phase 4's original scope remains open too: the posture library (8
   states from the hologram sheet vs. the current 4-state `QuarkReflexPosture`), the real-time
   AGSL overlay shader, and the still-unresolved `CLAUDE.md` phosphor-guardrail-vs-8-token-palette
   conflict.

## ▶ RESUME HERE — Phase 4a: anthropometric-canon correction — DONE, one open gap flagged

Resolved the anthropometric-canon question flagged above. Built `02_anthropometric_compare.py` — a
standalone silhouette-only comparison script (no fingers/toes/panels/bake, flat material, coarser
voxel) that builds both the canon (Loomis-canon knee) and a corrected variant side by side, with
bright marker rings at the actual knee/hip height on each render so the numeric difference is
legible even though the blockout's leg taper has no visible crease at the joint itself. Sourcing
for the corrected values (both already cited in the anthropometric-sanity-check entry above): hip
height 84.5cm (ergonomics table, 50.6% of 167cm stature — the only source cited for hip); knee
height 56.4cm (average of the two cited sources, 61.3cm ergonomics-table and 51.4cm Chumlea
regression, rather than picking one arbitrarily); ankle height left unchanged at 8cm — neither
source covers ankle, so nothing there was "corrected" against no evidence.
Sent the marker-ring renders (`renders/anthro_compare/`) to the Director for an actual side-by-side
before touching production geometry, per this track's own "flag forks, verify don't eyeball"
discipline — **Director chose the anthropometric-corrected variant.**

**Folding into `01_base_mesh_and_rig.py`:** added `tz()`/`tz_m()` — a continuous compression
transform anchored at the new pelvis height, applied to every torso/head landmark so total stature
stays 167cm now that the leg span is longer, and to the material classify function's torso-relative
z-bands (chest seam, shoulder pauldron, neck collar, spine conduit, shoulder-blade panels, head
cutoff, headband). Leg-specific bands (kneecap, hip-wrap) recenter on the actual new landmark
values instead. Every touched function is listed here because all of them had hardcoded absolute
cm values baked in: `build_torso_and_head()`, `build_legs()` (hip/knee recomputed directly;
mid-thigh/calf keep the same *fractional* position along their segment the canon build used,
re-applied to the new segment lengths), `build_arms()` and `build_fingers()` (palm/wrist height
moves with torso compression), `build_armature()` (every bone), and `_classify_material_index()`.

**A real regression, caught by this file's own topology check, not assumed away:** the first full
pipeline run after the landmark changes came back `islands=3`, not the `1` every prior phase
verified. The correction didn't touch toe geometry directly, but shifting the mesh's overall
bounding box moved where an already-marginal feature landed on the voxel remesh's sampling grid.
**Chased through several wrong theories before finding the real one** — worth recording so a future
session doesn't repeat the detour:
1. Deepened the toe's overlap-into-foot ring (7cm→5cm) — no effect, wrong joint.
2. Widened the little toe's radius (0.55→0.65cm) and separately widened just the overlap ring —
   the disconnected fragment grew slightly but stayed disconnected both times: the overlap-to-foot
   weld was never the actual failure.
3. Tightened `voxel_size` 0.004→0.003, assuming an under-resolution problem like the Phase 3f
   finger fix — **made it worse** (9 islands, not fewer). The tell: finer voxels resolve *true*
   gaps more faithfully (that's what fixed Phase 3f's finger-blobbing — separate things staying
   separate); a toe-to-foot join is supposed to *merge*, so a marginal-but-real overlap needs
   width, not resolution. Reverted to 0.004.
4. Widened the toe tip (0.2→0.35cm) — still no effect on the fragment.
5. **Actually dumped the disconnected fragment's raw vertex coordinates** instead of continuing to
   guess from parameter names: <1mm of Z-extent across ~5-6mm of X/Y — a flat pancake, not a tube
   cross-section. That pointed at the real cause: `build_toes()` was passing `axis='Z'` to the
   shared `ring()`/`build_limb()` helpers (same as `build_legs()`), and `ring()`'s `'Z'` mode holds
   Z fixed per ring — correct for a leg that travels along Z, wrong for a toe that travels along Y.
   Toe "thickness" was coming entirely from the 2-4mm Z-gaps between landmarks, below the 4mm
   voxel grid — a pre-existing fragility the canon build's proportions happened to land clear of.
6. Tried `axis='X'` (the mode `build_fingers()` uses successfully) — fixed the disconnection but
   broke something else: `'X'` mode fixes X per ring, and X is exactly the dimension separating
   one toe from its neighbor, so adjacent toes fused into one continuous blob instead (caught by a
   y-slice x-clustering scan: one continuous run where a working build should show gaps).
7. **The actual fix:** added a proper `axis='Y'` mode to `ring()` (varies X/Z, fixed Y) — the one
   that's a real cross-section for a Y-traveling, X-separated limb, which neither pre-existing mode
   was. Re-verified: `islands=1`, `non_manifold_edges=0`, `non_manifold_verts=0`, 100% quads
   (130,036 faces), all 20 armature vertex groups present.

**Toe-separation gap, resolved (with scope relaxed by Director):** the y-slice x-clustering scan
that flagged this as uncertain was checking too close to the toe-base — real toes are webbed near
the base and only separate near the tips, so "one continuous run" there was never actually a
red flag. Director's guidance: shoes will cover the feet in the final render, so silhouette-level
correctness is the actual bar, not per-toe isolation. Got a clean, unobstructed shot by aiming the
camera at the toe tips head-on (`renders/qa_closeup/foot_front.png`) instead of fighting top-down/
bottom-up framing (same class of difficulty Phase 3g logged) — it clearly shows a scalloped, multi-
lobed edge, not a flat paddle. A profile shot (`foot_side3.png`) confirms a normal heel-to-toe
silhouette with the big toe reading as a distinct bump. Good enough for shoe coverage, confirmed by
looking, not assumed.

## ▶ RESUME HERE — Phase 4a QA pass: verifying the correction didn't regress anything else — DONE

The anthropometric correction touched `build_torso_and_head()`, `build_legs()`, `build_arms()`,
`build_fingers()`, `build_armature()`, and every z-band in `_classify_material_index()` — worth
actually checking each rather than assuming the transform math was right just because it compiled
and rendered a plausible-looking front view. Rendered targeted close-ups
(`renders/qa_closeup/`) at each touched region:
- **Kneecap band** — recentered correctly on the new knee height, reads clearly (`hip_knee.png`).
- **Shoulder pauldron + neck collar** — both land correctly at the compressed torso height
  (`shoulder.png`).
- **Chest seam** — correctly placed, panel/rivet detail intact (`chest2.png`; first attempt,
  `chest.png`, accidentally shot the back — this project's camera convention has "front" showing
  the spine-conduit side, confirmed against the last committed pre-session render, not a bug this
  session introduced).
- **Hip wrap** (metal ring + graphite flanks) — correctly placed (`hip_wrap.png`).
- **Headband circuit** — correctly placed at brow height on the (also-compressed) head position
  (`head.png`).
- **Fingers** — attachment height moves with the torso-compression transform (`tz(130)`), unlike
  toes which are floor-anchored and don't move at all; confirmed still cleanly separated and
  correctly attached at the wrist, no regression (`hand.png`, `hand_top.png` — all 5 digits
  distinctly separated, cleaner than the toes).
- **Inner-thigh gap** — checked against the Phase 1 log's old "same-class shadow/seam" flag;
  what's visible from a low camera angle is just the normal ~2cm anatomical gap between two
  separate legs (hip landmarks are 19cm apart center-to-center, ~8.5cm radius each), not a mesh
  defect. Different thing from the old flag, not the same issue persisting.

All seven checked regions confirmed correct. Combined with the earlier topology check (1 island,
manifold, 100% quads) and the toe-separation finding above, the anthropometric correction is fully
verified, not just structurally sound.

**Current assets regenerated at the corrected proportions:** `blender/quark_base.blend`,
`renders/blockout_turnaround/`, `renders/palette_compare/` (materials/textures re-baked at the same
2048px resolution as Phase 3c). `renders/anthro_compare/`, `renders/toe_closeup/`, and
`renders/qa_closeup/` are this session's own working/diagnostic renders, not final production
assets — kept because they're the actual evidence behind the claims above, not because they're
deliverables.

**Committed to git** (`00806c4`, `431dfe2`) — bundles this session's Phase 4a work with the prior
session's still-uncommitted Phase 3g assets, per Director's explicit go-ahead. Not yet pushed to
origin.

## ▶ RESUME HERE — Phase 4 kickoff: phosphor-vs-palette guardrail conflict — RESOLVED

Director chose to start Phase 4 with the palette decision rather than the posture library or the
AGSL shader, since it was a standing design question blocking nothing yet but worth settling before
building more on top of an undecided visual language.

**What actually got decided, and why the framing changed:** presented the existing
`palette_compare` renders as the Phase 3c decision text described them ("8 sheet-accurate tokens
vs. phosphor-collapsed") — but on closer look, that framing didn't match what the renders actually
show. `assign_materials()`'s `glow_color` parameter only ever swaps the emissive accent (spine
conduit + headband); the four physical materials are hardcoded, identical in both renders, and
were never phosphor colors in either variant. So the real choice wasn't "recolor the body or not" —
it was already implicitly decided (the body was always concept-accurate) and just hadn't been
named as a decision. Flagged this gap in the original framing to the Director rather than letting
a decision get made on an inaccurate premise.

**Resolution:** QUARK's four physical materials (Ceramic/Synth-Skin/Graphite/Metal Alloy) are a
**Director-approved exception** to `CLAUDE.md`'s phosphor-only guardrail — real-world material
colors, permanently, matching the concept sheet, because this is a photoreal/holographic character
surface, not a UI screen. The only hue-bearing element is the emissive accent, and it stays
single-sourced from `PhosphorHueRuntime` via the Phase 4 AGSL overlay shader (not yet built) —
which was already the render-path plan (pre-rendered frames stay neutral-value, live tinting
happens at runtime), so this decision confirms rather than changes that architecture. No code
change needed: `main()` already bakes the `sheet_neutral` (`#E6F1FF`) variant as the shipped
default, which is correct under this resolution. `renders/palette_compare/phosphor_green_*` remains
useful as a preview of the live shader's GREEN-hue output, not as an alternate bake.

**Still open, now unblocked:** the 8-state posture library and the real-time AGSL overlay shader
itself (the thing that will actually implement the live accent-tinting this decision assumes)
remain unbuilt. Next session's choice between them, or another priority.
