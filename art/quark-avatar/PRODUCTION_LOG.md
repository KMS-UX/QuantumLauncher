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

## ▶ RESUME HERE — Phase 4: posture library, first pass — DONE

Director picked the posture library next. Before building anything, cropped and inspected each of
the reference sheet's 8 "POSTURE & EMOTION STATES" thumbnails individually (full-sheet scale was
too small to read reliably) — a real finding, not an assumption: **7 of the 8 states share the
exact same relaxed standing pose** (arms at sides), differing only by accent color and a couple of
state-specific VFX (Speaking's ripple rings, Stealth's dimmed opacity) that belong to the live
AGSL shader layer, not the baked mesh. Only **Thinking** has a genuinely different body pose
(hands raised toward the chin, elbows out). So the actual deliverable is a **2-pose library**, not
8 separate poses — which is exactly the asset-count reduction the render-path decision's own
reasoning anticipated ("avoids an N-postures x 3-hues baked-asset explosion"), just discovered
concretely rather than assumed.

Accent-color rule for these renders (Director decision, this session): every posture uses the live
`PhosphorHueRuntime` hue except Alert, which is always the fixed `--warn` red — rendered with GREEN
standing in for "whatever hue is active" (the real selection happens live in-app) and RED for
Alert. The full 8-token sheet-accurate palette remains a later bonus variant, not built here.

**New script:** `03_posture_library.py`, loads the existing baked `quark_base.blend` rather than
rebuilding from scratch, poses the `QUARK_Rig` armature, and renders. Ships 3 frames:
`renders/postures/relaxed_idle_green.png` (covers Neutral/Focused/Happy/Warm/Speaking/Stealth),
`relaxed_idle_alert_red.png` (Alert), `thinking_green.png` (Thinking).

**Posing the armature took three real debugging passes, worth recording:**
1. **Bone-local Euler rotation gave nonsense results** — rotating `shoulder_*`'s local Z axis by
   ~78° swung the arm UP overhead, not down to the side. Root cause, found by dumping
   `bone.matrix_local`'s column vectors rather than continuing to guess signs: this bone's local
   X/Z axes are tilted, not aligned to any world axis, so a "local Z rotation" doesn't mean what
   it sounds like. Switched to a world-space technique instead — build a rotation matrix around a
   named *world* axis (Y, for a T-pose arm swinging from horizontal to vertical) and apply it as
   a pivot transform around the bone's head — which sidesteps the local-axis question entirely.
2. **Rotating the wrong bone.** First pass rotated `shoulder_*` (the clavicle), whose rest head
   sits at the body centerline (x=0) — that pivots the *whole* arm chain down through the torso's
   center, leaving it hidden behind the torso silhouette from a straight-on view. `upperarm_*`'s
   rest head is already at the correct shoulder-joint offset (`build_armature()` parents it
   there), so rotating that bone instead was the fix — confirmed by comparing renders of both
   (centerline version: arm invisible from the front; joint version: normal arms-at-sides stance).
3. **Chained rotations need the CURRENT pivot, not the rest pivot.** The Thinking pose's forearm
   bend flew off to the wrong place on the first attempt — traced to `rotate_bone_world()`
   computing its pivot from the bone's *rest* head position, which is stale once the parent
   (`upperarm`) has already been rotated. Fixed by pivoting around `pose_bone.matrix.translation`
   (the bone's current, already-posed head) instead. Also caught an asymmetry bug after that fix —
   only one arm bent correctly — because the forearm's world-X bend angle wasn't mirrored per side
   the way the upperarm's world-Y angle was; fixed by applying the same `* side` sign convention
   to both.

**Verified by rendering and looking, all three, not assumed from the code** (per this track's
standing discipline): relaxed-idle reads as a natural arms-at-sides stance from both a 3/4 front
angle and a profile check (added specifically to catch clipping — none found, the unrotated
clavicle stub doesn't create a visible artifact from any angle checked). Thinking reads as a
distinct, purposeful "hands raised toward the chest/chin" gesture, symmetric across both arms.
Alert's red accent renders correctly on the same relaxed-idle pose, confirming the color-swap
mechanism still works after posing (unaffected by which pose is active, as expected — the emissive
material is independent of the armature).

**Honest scope note:** "hands raised toward the chin" is the achievable target, not literal
interlocked fingers — the rig has no per-finger bones (fingers are static geometry parented to the
hand bone), so individual finger articulation isn't possible without a rig change. Close enough to
the reference's intent at this fidelity level; flagging rather than overclaiming precision.

**Not yet done:** VFX layer (Speaking's ripple rings, Stealth's dimming) — these are Phase 4 AGSL
shader territory per the analysis above, not this script's job. The `.blend` file on disk still
saves in its T-pose (base/rest) state; the posed renders are PNG-only, consistent with the hybrid
render-path architecture (the app consumes rendered frames, not the `.blend` itself).

## Session close — 2026-08-20 (continuation)

**What shipped this continuation:** the anthropometric-canon correction from earlier today, folded
into production and fully QA-verified (panels, hands, feet, topology all confirmed correct post-
correction, not just structurally sound); the toe-separation question resolved by rendering the
actual toe tips instead of relying on an under-scoped numeric check; the phosphor-vs-8-token-
palette guardrail conflict resolved (QUARK's physical materials are a Director-approved exception,
only the emissive accent is hue-driven); and a first-pass 2-pose posture library covering all 8
named states. Three commits (`431dfe2`, `a180de6`, `d0216b1`) on top of the two from the session's
first half. Every claim in this entry and the ones above it was checked by rendering and looking,
or by a direct data check when rendering itself was the hard part (toe islands, panel bands, bone
axes) — several real mistakes were caught this way rather than shipped: the toe ring-axis bug, the
shoulder-vs-upperarm pivot bug, the stale-pivot chained-rotation bug, and the narrower-than-assumed
scope of the original palette-comparison renders.

**Next session:**
1. **AGSL overlay shader** — the piece both the palette resolution and the posture-library VFX
   notes (Speaking's ripple rings, Stealth's dimming) assume exists. Kotlin/Android graphics work,
   a different domain from this session's Blender pipeline — Director explicitly deferred starting
   it today for that reason.
2. Posture library could still use: rendering `relaxed_idle`/`thinking` from the full 5-angle
   turnaround (only one presentation angle exists today) if the app needs more than one viewing
   angle per posture; extending past this 2-pose set if on-device review of these two surfaces a
   need for more real pose variety.
3. Everything else still open from prior sessions: the full 8-token sheet-accurate palette as a
   bonus variant (Director wants to keep this open, not abandoned); continued panel/material
   fidelity if wanted (assessed as near the ceiling of what procedural texture work can add,
   unchanged from the prior session's assessment).

## ▶ RESUME HERE — Phase 4b: AGSL overlay shader — VERIFIED ON A REAL GPU EMULATOR, ALL 6 OPEN QUESTIONS CLOSED

Kotlin/Android graphics work, per the prior session's own deferral. First pass built the module and
went CI-green (2 round-trips: a missing `getValue` import, then the recurring manifest `"--"` bug —
both already recorded, unchanged, in the history below). This continuation is the actual "verified
by rendering and looking" pass this track has always required, made possible by discovering Android
Studio + a full SDK + a GPU-accelerated emulator (`Pixel_10a`, x86_64, real discrete NVIDIA GPU via
`gfxstream`/Vulkan, `renderer=skiagl`) were available locally — not the Fold 6, but a real GPU-backed
Skia rendering path, a legitimate stand-in for AGSL verification. Built a local Gradle 8.9 wrapper
(none was checked into this repo — only `gradle-wrapper.properties` existed; CI installs Gradle via
its own action instead, matching `CLAUDE.md`'s plain `gradle` build/run convention) using JDK 17
(`AdoptOpenJDK-17`, already installed, matching the project's pin) to build, sideload, and drive the
app via `adb`, screenshotting each state and reading the PNGs back directly rather than describing
them secondhand.

**A real, launch-blocking bug found immediately, before any visual check was even possible:** the
CONFIG dev-preview row's `setClassName(...)` launch (added specifically to avoid a Gradle dependency
edge from `:config` onto `:quark-avatar`) was based on a false premise — Android library modules only
end up in the final APK's merged manifest/classes if something in the dependency graph actually
depends on them. `setClassName` is just a string; it can't conjure a class into an APK that never
compiled it in. First launch attempt threw `ActivityNotFoundException`. Fixed by adding the real
`implementation(project(":quark-avatar"))` edge to `config/build.gradle.kts` (the same shape every
other docked module already uses) — the "avoid coupling" reasoning in the first-pass plan was wrong
on the technical merits, not just overcautious.

**Two more real bugs found by actually looking, neither of which a code review would have caught:**
1. **The posture-library PNGs are not alpha-matted**, despite this log's own original description
   ("neutral-value, alpha-matted"). Checked directly (`System.Drawing.Bitmap.GetPixel`, not assumed):
   all three files have `A=255` uniformly, everywhere, including the backdrop — which is instead a
   flat, uniform `(58,67,58)/255` fill (confirmed identical at 8 sample points across all 3 files).
   This meant (a) the avatar rendered with an ugly opaque gray-green box instead of transparency, and
   (b) the rim-glow's alpha-gradient math was silently inert — there was never an alpha discontinuity
   anywhere in the source to detect. A real re-render with alpha is Blender-pipeline work, out of this
   session's scope — fixed instead by synthesizing a subject mask at shader runtime, keying the known
   flat background color (`smoothstep` distance-based, soft edge) and using that mask for both final
   alpha output and the rim-glow's edge detection (replacing the dead alpha-channel taps).
2. **The accent-retint key never fired on the real data.** The original design assumed the posture
   library baked the emissive accent as pure `(0,1,0)` green (matching `03_posture_library.py`'s
   `set_emissive_color(..., (0.0, 1.0, 0.0), ...)` call) and keyed on "G > 2×R and G > 2×B" — but
   measured directly, the brightest green-dominant pixel anywhere in the 1000×1400 render is only
   `rgb(155,218,137)` (dominance 63/255), and the key's own math makes "G > 2×R" mathematically
   impossible once R exceeds ~127 (G caps at 255). An emissive material at high bloom/strength under
   the renderer's tone mapping washes out to a pale near-white-green, not a saturated pure hue. Found
   the real accent cluster by histogram-bucketing dominance across the whole image (~1780 sample
   points at dominance 25–45/255, spatially bounded to a 66×272px band matching the spine conduit's
   actual on-screen position — confirmed spatially, not just by color, so it's the real accent region
   and not noise) and recalibrated the key to `smoothstep(0.07, 0.14, G - max(R,B))` against that
   measured band.

**Rim glow, tuned live once the alpha-mask fix made it meaningful again:** first pass (1.5px offset,
0.35 additive strength) was confirmed genuinely invisible at real display size — checked by cropping
and 4×-upscaling a silhouette-edge region of an actual screenshot, not guessed. Widened to a 6px
offset / 0.9 strength; re-checked the same crop and it now reads as a clean, even, intentional-looking
white rim-light around the whole silhouette, with a crisp mask edge and no fringing/haloing artifact
at the transparency boundary (closes the `CompositingStrategy.Offscreen` question below too — real
alpha transparency was confirmed end-to-end, background genuinely shows the app's true CRT black, not
just "no crash").

**All 6 originally-open questions, closed with photographic evidence (screenshots + pixel-level
sampling), not assumption:**
1. ✅ AGSL compiles and runs — confirmed by every subsequent point below actually rendering.
2. ✅ Transparent compositing works cleanly — confirmed via 4×-upscaled edge crops; no fringing.
   (Caveat: this validates the *mechanism* using a shader-synthesized mask, not the source PNGs' own
   alpha, since that channel turned out to be useless — see bug #1 above.)
3. ✅ Rim glow reads as an intentional rim-light after the width/strength retune (see above) — first
   pass did not, and that finding is preserved above rather than silently overwritten.
4. ✅ Recalibrated accent-key isolates only the spine/headband — confirmed across GREEN/AMBER/CYAN
   with no false-positive tinting anywhere on the body/limbs/head.
5. ✅ Cycling HUE recolors the accent live — confirmed GREEN→AMBER→CYAN, each matching
   `Phosphor.bright()`'s real color, and confirmed the whole app's chrome (nameplate, HOME text, the
   floating QUARK trigger) recolors together, proving `PhosphorHueRuntime.cycleHue()` genuinely
   propagates app-wide from this screen, not just locally.
6. ✅ Stealth-dim confirmed (brightness drops, hue/saturation preserved, matching the engine's own doc
   comment) and Speaking's ripple overlay confirmed rendering (expanding ring, independent of the
   shader). POSTURE cycling (Neutral/Alert/Thinking) confirmed swapping the correct bundled PNG each
   time, including Alert's fixed-red bake correctly staying untouched by the retint (its accent region
   measured at max dominance 9/255, safely below the key threshold — verified, not assumed from the
   design intent alone).

**Fixed and pushed:** `config/build.gradle.kts` (the real dependency edge), and
`QuarkAvatarShader.kt` (background mask synthesis, recalibrated accent key, widened rim glow) — see
BUILD_LOG-style commit trail for the exact diffs. Local `gradle test` + `assembleDebug` both green
before push, same discipline as every CI round-trip.

**Still not done, flagged rather than assumed:** the real Fold 6 (as opposed to a GPU emulator) pass
— color reproduction, actual phosphor-panel look, and physical device performance could still differ;
the emulator is strong evidence, not a substitute for the real hardware this track has always
required as the final word. The CONFIG dev-preview row is still a flagged, temporary stopgap, not a
navigation decision. Real `QuarkReflexPosture`/Stealth-state wiring from `QuantumRuntime.masterState`
remains blocked on the same cross-module constraint noted in the first pass. No Blender/render-pipeline
changes were made this continuation either — the PNGs' non-alpha-matted background is worked around at
shader runtime, not fixed at the source; a real alpha-matted re-render is still open, lower-priority
now that the runtime mask closes the visible gap.

**Next session:** cosmetic detail pass on the base mesh (face/body/hair) against the reference
concept art — see the new session entry below, this is a different, Blender-side thread. The Fold 6
confirmation of this shader work is otherwise the Director's own action, not blocking further work.

---

## ▶ RESUME HERE — Phase 4c: cosmetic detail pass (face/body/hair) — MPFB-based rebuild, verified

The Director asked for the mesh itself brought up toward the reference concept art (real face
details, body features, hair) — none of which pure procedural primitive scripting (this pipeline's
approach through Phase 4b) can produce; a believable human face and hair need real anatomical
topology, which primitives/voxel-remesh blockouts fundamentally can't approximate. Flagged this
honestly before starting (per this track's own "flag, don't silently lock" discipline) — the
Director's response was to install **MPFB** (MakeHuman for Blender,
`bl_ext.blender_org.mpfb`, https://extensions.blender.org/add-ons/mpfb/) specifically to close the
gap, discovered mid-session that Android Studio's local toolchain (used for Phase 4b) also came with
a full Blender 5.2 install already on this machine. Verified this whole pass by rendering and
looking, and separately by round-tripping the new assets through the actual Android app on the
GPU emulator already proven out in Phase 4b — not by trusting the Blender-side renders in isolation.

**Honest fidelity ceiling, stated plainly rather than overclaiming:** this closes the *topology* and
*material* gap dramatically — a real face (eyes, nose, mouth, cheekbones, jaw), a real body, and
actual hair geometry instead of none. It does **not** reach the reference sheet's literal
photoreal/AI-render quality — skin pore detail, individually art-directed flyaway hair strands are a
texture-painting/hair-grooming polish tier beyond a scripted pipeline, and MPFB ships no default
eyeball geometry, skin textures, or hair assets at all (confirmed by searching its installed files,
not assumed) — those had to be built here too, not imported.

**What shipped, `01_base_mesh_and_rig.py` rewritten** (full rationale/measurements in the script's
own comments, not just here):
- **Human generation** — `HumanService.create_human()` tuned to the reference's stated "167cm,
  Athletic/Feminine" build via MakeHuman's macro-detail dict (gender/age/muscle/weight/proportions/
  height/cupsize/firmness, each 0–1). Measured result: 169.46cm (1.5% over spec) — the height slider
  turned out to blend a whole shape-key set together with the other macro axes rather than moving
  independently (confirmed empirically: nudging `height` alone with everything else fixed produced
  *zero* measured change), so this was accepted rather than fought further. `scale=0.1` is MPFB's own
  "real-world meters" convention for `create_human()` — tried `scale=1.0` first, got a 16.9m-tall
  mesh, caught by directly measuring bounding-box Z before assuming anything rendered correctly.
- **19,158-vertex real anatomical topology** replaces the old blockout's lofted-primitive/voxel-remesh
  mesh entirely — real eye sockets, nose, mouth, jaw, cheekbones, ears, proper MakeHuman vertex
  groups and phenotype shape keys. This mesh's rest pose is a relaxed A-pose (arms angled down), not
  the old blockout's T-pose — confirmed by rendering the unposed mesh, not assumed from any shape-key
  name.
- **Eyes** — MPFB has eye *sockets* (a closed indentation) but ships zero eyeball geometry or assets
  (confirmed: `data/3dobjs` has no eye files). Built as simple sclera/iris/pupil-material spheres.
  Positioning took three real attempts, each wrong for a specific, now-recorded reason: (1) MPFB's
  own `helper-l-eye`/`joint-l-eye` vertex-group centroid placed them on the forehead, not the visible
  socket opening; (2) the true socket-hole boundary (found via `bmesh` non-manifold-edge search) gave
  X/Z right but a Y depth that recessed the eyes fully inside the head, invisible; (3) the actual fix
  was raycasting from the real face-closeup render camera through the visible socket pixels in an
  actual rendered image and recording the 3D hit point — ground truth, not a named landmark's
  assumed meaning. Every wrong attempt was caught by rendering and looking at the result, not by
  reasoning about coordinates in the abstract.
- **Hair** — MPFB owns hair via Blender's native Hair Curves system, but MPFB's own hair workflow is
  an interactive brush-styling panel, not something scriptable headlessly toward a specific target
  shape. Built instead as simple mesh primitives (scalp cap + gathered bun + two framing locks) —
  the same stylized-geometry technique this pipeline already uses for panel/rivet detail and the
  emissive headband, explicitly scoped as shape/volume, not strand grooming. First attempt used a
  bmesh bisect-plane to carve a dome cap out of a full sphere and got the inner/outer keep-side
  backwards (kept the huge lower two-thirds instead of the small cap) — a render showed a mushroom-
  sized blob swallowing the whole face before this was caught; fixed by sizing/placing small pieces
  to sit above the measured hairline directly, no bisect needed at all.
- **Material region classification recalibrated**, not ported — the old `_classify_material_index()`'s
  coordinate thresholds were tightly coupled to the old blockout's exact (and very different)
  proportions. Re-measured this mesh's own landmark heights via its `joint-*` rig vertex groups
  (pelvis/neck/shoulder/knee/ankle/etc. centroids) and rebuilt the region rules against those.
  **Two real, confirmed-by-rendering bugs caught and fixed in this pass, not just theorized:**
  1. This mesh's front (face) points toward **-Y**, the *opposite* of the old blockout's convention
     (that mesh's front pointed toward +Y, which is *why* its own "front"-labeled camera view showed
     the back/spine-conduit side — a documented quirk in this log, not a convention to blindly carry
     forward onto a differently-oriented mesh). Copying the old sign convention verbatim first put
     the spine-conduit/scapula/chest-seam regions on the wrong (front-facing) side — an early render
     showed a bright emissive-looking cross pattern on the camera-facing torso instead of the back;
     traced to the sign bug and fixed (all three rules' Y-sign flipped for this mesh).
  2. Per the reference's own "Hand (Palm)" close-up, hands are **armored** (segmented plates), not
     bare skin — corrected from the old blockout's treatment (which mapped hands to the skin
     material) now that "skin" material actually means something real (an exposed human face) rather
     than a placeholder catch-all.
  `make_material()` (the procedural PBR node-graph builder), `bake_textures()`, `setup_render()`,
  `render_turnaround()`, `direction_to_euler()`, and `set_emissive_color()` all carried over verbatim
  — genuinely mesh-agnostic, exactly as anticipated going into this pass.
- **Rig** — MPFB's own "default" standard rig + auto-weighting (`HumanService.add_builtin_rig`),
  replacing the old hand-built armature entirely. Confirmed the actually-created bone names by
  inspecting the real armature, not the rig's JSON template alone: `upperarm01.L/R`/`lowerarm01.L/R`
  for arm posing (a full FACS-style facial bone set also exists on this rig — `levator*`/
  `orbicularis*`/`temporalis*`/etc. — not used this pass, a real opportunity for a future expression
  pass, noted under follow-ups below).

**`03_posture_library.py` ported, not just bone-renamed:**
- Bone names updated to the new rig (`upperarm01.L/R`/`lowerarm01.L/R`).
- `set_pose_relaxed_idle()` is now a **no-op** — this mesh's own rest pose is already the relaxed
  A-pose the old function used to rotate a T-pose *into*; applying the old 90° rotation on top of an
  already-relaxed rest pose would over-rotate. Kept as an explicit empty function (not deleted) so
  the "relaxed idle" concept stays named and documented at its call site.
- `set_pose_thinking()` angles retuned for this rig's different rest pose (70°/-100°, vs. the old
  65°/-110° tuned for a T-pose start) and confirmed by rendering. **Known imperfection, not hidden:**
  the resulting pose is asymmetric — one arm reaches toward the chest/chin as intended, the other
  swings out to the hip rather than mirroring — visible in both the Blender render and the in-app
  screenshot. Reads as a loose "gesturing" pose, not literally broken, but not the clean mirrored
  "hand near chin" the reference implies either. Flagged as a follow-up tuning item, not fixed here.

**Full production pass run for real** (not just the sandbox iteration above): textures re-baked
(`art/quark-avatar/textures/quark_{base_color,roughness,emission,normal,ao}.png`), 5-view turnaround
+ palette-compare renders regenerated, posture library regenerated
(`relaxed_idle_green`/`relaxed_idle_alert_red`/`thinking_green`), `quark_base.blend` resaved.

**Android integration — verified end-to-end on the same GPU emulator Phase 4b proved out, not just
Blender-side:** copied the three regenerated posture PNGs into
`quark-avatar/src/main/res/drawable-nodpi/` (same filenames — no Kotlin/shader code changes needed).
Measured, not assumed, whether `QuarkAvatarShader.kt`'s background-key and accent-key constants still
held against the new renders (flagged as a real open question in the Phase 4b entry above): the
background color measured byte-identical (`58,67,58` — `setup_render()`'s world-background value is
unchanged, carried over verbatim), and the accent's green-dominance range measured within a few
points of the prior calibration, so **no shader changes were needed**. Rebuilt the debug APK,
installed on the emulator, navigated CONFIG → the dev-preview row, and confirmed both NEUTRAL and
THINKING postures render correctly through the full pipeline — real face/hair, clean transparent
background (no fringing, including around the more complex hair silhouette), live CYAN accent retint
matching the persisted hue from the Phase 4b session. The Thinking-pose asymmetry noted above is
visible in-app too, not just in the isolated Blender render — confirms the whole chain is consistent,
not that the Blender output looks different once it reaches the shader.

**What did NOT change:** `QuarkAvatarShader.kt`, `QuarkAvatarScreen.kt`, `QuarkAvatarActivity.kt` —
none of the Kotlin/Android code from Phase 4b needed touching; this pass was purely upstream art
regeneration plus a resource-file swap. `audio/.../QuarkMascot.kt` — untouched, per the standing
"does not replace the existing inline mascot" scope. No HOME instrument-console integration attempted
(still the CONFIG dev-preview row, still explicitly a temporary stopgap, unchanged from Phase 4b).

**Explicit follow-ups, flagged not fixed:**
- Thinking pose's arm asymmetry (above).
- A real alpha-matted re-render (the background-key workaround in the shader still does the real
  work; a native-transparency render would be strictly better but isn't blocking anything visible).
- Hair is a stylized shape/volume match, not groomed strands — the reference's individually-rendered
  hair detail is a different, larger effort (Blender's Hair Curves properly driven, likely still via
  MPFB's interactive tools rather than headless scripting).
- This rig's full FACS-style facial bone set (`levator*`/`orbicularis*`/etc.) is unused — a real,
  concrete opportunity for a future expression pass matching the reference's Neutral/Focused/Warm/
  Alert face variants, which currently only differ by shader-layer accent color, not actual
  expression.
- The `mpfb_test/` sandbox directory (this session's iteration scratch work — test humans, eye/hair
  placement experiments, the exact commands that found each bug above) is left on disk but not
  committed, kept only as this session's own working notes.

**Next session:** Director's call on priority among the follow-ups above, or the Phase 4b shader's
own still-open Fold 6 hardware confirmation (not blocking, but still the real final word per this
track's standing discipline).

## ▶ RESUME HERE — Rendering refinement pass — Tier 1 in progress

Director asked for a general "refine QUARK rendering" pass. Diagnosed three of the standing
follow-ups above as sharing root causes traceable to specific lines, rather than separate loose
ends, before touching anything:

- **Non-alpha-matted PNGs** ← `setup_render()`'s `film_transparent = False`
  (`01_base_mesh_and_rig.py`). One-line fix; retires the shader's chroma-key `subjectMask()`
  workaround entirely once real alpha exists.
- **Washed-out emissive accent** (measured `rgb(155,218,137)` from a baked pure-`(0,1,0)` material,
  per the Phase 4b entry) ← Blender 5.2 defaults to the **AgX** view transform, which deliberately
  desaturates bright emissives toward white; `setup_render()` never sets `view_transform`
  explicitly. This is *why* the original `G > 2×R` key was mathematically unfireable — not generic
  "bloom," a specific settable property. Setting `view_transform = 'Standard'` should restore
  genuine saturated-green output and make the accent key reliable again without inventing a second
  bake pass.
- **Thinking pose's arm asymmetry** ← `rotate_bone_world()` calls in `set_pose_thinking()`
  (`03_posture_library.py`) apply `* side` to both a Y-axis and an X-axis world rotation. Mirroring
  across the YZ plane flips Y/Z-axis rotation sign but *preserves* X-axis rotation sign
  (`M·Rx(θ)·M⁻¹ = Rx(θ)`) — so `'X', -100 * side` is wrong; the right forearm rotates the opposite
  of the intended direction. This is a derivable sign bug, not a tuning miss.

**Ordered plan (this session), verify-by-rendering at each step per this track's standing
discipline:**
1. `setup_render()`: `film_transparent = True`, explicit `RGBA` file output, explicit
   `view_settings.view_transform = 'Standard'`.
2. Re-run `01_base_mesh_and_rig.py` full pipeline; measure the re-baked accent region's actual
   RGB/alpha directly (not assumed) to confirm both fixes landed.
3. Fix the `03_posture_library.py` sign bug; re-render the 3 posture PNGs; confirm Thinking reads
   as a mirrored "hand near chin" pose in both arms this time.
4. `QuarkAvatarShader.kt`: replace the chroma-key `subjectMask()` with `src.a` (now real); recheck
   whether the accent-dominance key constants still need recalibrating against the new
   (unwashed-out) accent color, and simplify/tighten them if so.
5. Copy the 3 regenerated PNGs into `quark-avatar/src/main/res/drawable-nodpi/`; rebuild; verify on
   the GPU emulator proven out in Phase 4b (real alpha edges, correct accent hue cycling, mirrored
   Thinking pose) before calling this done.

**Deferred to a later pass (Tier 2/3 from the planning discussion, not started this session):**
lighting rig (flat 4×`SUN` lamps → 3-point + backlight), lens/distance (24–35mm → 50–85mm
portrait treatment), raytraced shadows/AO/higher samples, output resolution increase, making the
shader's rim-glow offset resolution-derived instead of fixed-pixel, tinting the rim from the live
phosphor hue instead of hardcoded white. None of these are defects — they're a look upgrade —
flagged separately so they don't get bundled into "bug fixing" scope.

## Tier 1 — DONE, all 3 diagnosed root causes fixed and verified end-to-end on the GPU emulator

Steps 1–5 from the plan above executed in order, each checked by rendering/measuring before moving
on (this track's standing discipline), plus **one real bug found only by verifying step 4** that
was not in the original diagnosis — recorded honestly below rather than folded silently into the
plan as if it had been anticipated.

**Steps 1–2 (`setup_render()`):** `film_transparent = True` + explicit `RGBA`, plus
`view_settings.view_transform = 'Standard'`. Re-ran the full `01_base_mesh_and_rig.py` pipeline and
measured the result directly (Blender's own Python, `bpy.data.images` pixel access, not a
screenshot tool) rather than assuming the settings took effect: background corners now read
`(0,0,0,0)` (true alpha) where they were opaque `(58,67,58,255)` before; the palette-compare
accent's peak green-dominance pixel now measures `rgb(0.09, 1.0, 0.09)` — genuinely saturated —
versus the Phase 4b entry's own measured `rgb(155,218,137)/255` washed-out green. A full histogram
scan across the render confirmed a clean separation (body/skin cluster at dominance `[-0.10, 0.03]`,
accent at `[0.54, 0.88]`) with wide margin on both sides, not a fragile few-percent gap.

**Step 3 (posture sign fix):** re-rendered the posture library; the Thinking pose now shows both
arms crossing symmetrically toward the chest (confirmed by rendering — see
`renders/postures/thinking_green.png`), replacing the one-arm-to-hip asymmetry the Phase 4c entry
flagged. Mirror-math check: reflecting a world-space rotation across the YZ plane preserves
X-axis rotation sign and flips Y/Z — the old `-100 * side` on the forearm's X-axis rotation was
the bug; fixed to a plain `-100` shared by both arms.

**Step 4 (shader):** `subjectMask()` replaced with `src.a` (real alpha now exists); accent-key
threshold retuned to the newly-measured dominance gap
(`smoothstep(0.15, 0.35, greenness)`, replacing the old `smoothstep(0.07, 0.14, ...)` that was
tuned for the pre-fix washed-out values).

**A second real, previously-undiscovered bug found while verifying step 4's accent-key change on
the RED (Alert) bake specifically — not something the Tier 1 plan anticipated:** measured the
"red" posture PNG's actual accent pixel at the exact coordinate that reads pure green
`rgb(0.04,1.0,0.04)` in the green posture PNG, expecting saturated red. Got `rgb(1.0, 0.94, 0.44)`
— yellow. First hypothesis (stale EEVEE Next GI/light-probe cache carried over between the two
`render_presentation_shot()` calls in the same script run) was tested directly, not assumed: ran a
**fresh, isolated Blender process** that loaded the blend and rendered only the red variant, with
no prior green render in that process. Same yellow result — ruled out. Tested a pure-BLUE probe
color the same way and it rendered correctly saturated, isolating the bug to the RED constant
specifically, not the pipeline. Root cause: `03_posture_library.py`'s `RED = (1.0, 0.11, 0.02)` is
a CLAUDE.md **display/sRGB** hex token, but Blender's node-socket `default_value` is interpreted
as **linear**. That distinction is invisible for channel-pure colors like `(0,1,0)` or `(0,0,1)`
(0 and 1 map to themselves under either curve — why GREEN and the BLUE probe both looked correct
and nothing here was flagged before now), but RED's non-zero `0.11` G channel, fed as linear and
then multiplied by `emission_strength=7.0`, lands at `0.77` linear — gamma-*encoded* back up to
`~0.94` for display, comparable to R's own clipped `1.0`, reading as yellow instead of red.

**Fix:** added `srgb_to_linear()` (standard sRGB EOTF, per-channel) to `03_posture_library.py`,
applied inside `set_emissive_color()` before the strength multiply — every future accent color fed
through this function (the eventual 8-token palette bonus variant included) gets this correction
automatically, not just RED. Re-rendered: the same accent pixel now measures `rgb(1.0, 0.34, 0.13)`
— a clean saturated red-orange, dominance `0.66` vs. the broken version's `0.02`. Re-verified GREEN
is unaffected (still exactly `(0.04,1.0,0.04)`, as expected since 0/1 are gamma-curve-invariant).

**End-to-end verification, not just Blender-side:** copied all 3 regenerated PNGs into
`quark-avatar/src/main/res/drawable-nodpi/`, rebuilt `:app:assembleDebug`
(`BUILD SUCCESSFUL`), booted the same `Pixel_10a` GPU emulator Phase 4b proved out, installed,
and drove the actual app UI (tap navigation, not an adb shell activity launch — `QuarkAvatarActivity`
is correctly not exported, confirmed by the resulting `SecurityException` when tried) through
HOME → CONFIG → DEV: QUARK AVATAR PREVIEW → cycled POSTURE through NEUTRAL/ALERT/THINKING.
Screenshots pulled and inspected directly (not described secondhand): NEUTRAL shows genuine
transparent background (true app black, no fringing, no gray-green box) with a correctly
CYAN-retinted accent; THINKING shows the now-symmetric crossed-arm pose in-app, matching the
isolated Blender render; ALERT shows a clean red headband post-fix, replacing the yellow this
same click-path showed before the sRGB fix (confirmed both states on-device, not just in renders).

**What did NOT need touching:** `QuarkAvatarActivity.kt`, `QuarkAvatarScreen.kt`,
`config/build.gradle.kts` — Phase 4b's dependency-edge fix and CONFIG dev-preview wiring were
already correct and untouched by this pass.

**Next session:** Tier 2 (lighting rig, lens/portrait framing, raytraced shadows/AO, resolution)
and Tier 3 (shader rim-glow polish) from the original plan, Director's call on priority — or the
still-open Fold 6 hardware confirmation, now more relevant to schedule since Tier 1 changed what
the bundled PNGs actually look like.

## Tier 2 — DONE: lighting rig, portrait lens, raytraced shadows/AO, resolution

The "look upgrade" tier explicitly deferred out of Tier 1's bug-fixing scope. All changes in
`setup_render()` (`01_base_mesh_and_rig.py`) plus lens/distance in both scripts' camera-placement
functions; two real miscalibrations were caught by measuring before locking anything in, not
assumed from the numbers alone.

**Lighting rig:** the 4 flat `SUN` lamps (parallel rays -- hard edges, no falloff, no real shadow
softness) replaced with `AREA` lights (softness comes from `size`, not a separate property) in a
loose 3-point-plus-backlight arrangement, plus a genuine new `BackLight` giving the shader's
rim-glow a real physical light to build on instead of faking the whole effect from nothing. Kept
multiple world-fixed lights (not a single camera-relative 3-point rig) because the turnaround
orbits the camera around a static character across 5 angles -- a strictly camera-relative rig
would only look correct from one of them.

**A real miscalibration caught before shipping, not assumed:** first attempt reused SUN-light-like
energy numbers (400W key, etc.) for the new AREA lights and rendered a nearly-fully-blown-out
white figure with almost no shading definition -- confirmed by rendering and looking, not just by
eyeballing the code. Root cause: `SUN.energy` is irradiance (W/m², distance-independent) but
`AREA.energy` is total radiant power in Watts (falls off with distance/size) -- reusing similar
numbers across the two unit systems was the bug. Fixed by iterating against the already-saved
`quark_base.blend` directly (fast -- no mesh rebuild needed) at a single global energy multiplier,
measuring actual bbox-relative mean luminance and highlight/shadow-side pixel values at each step
(0.15x still clipped a highlight side to pure white; 0.06x gave a clean, un-clipped gradient
between light/shadow sides with real midtone definition) rather than guessing a "looks about
right" value, then baking the converged per-light Watt values into the script.

**Raytraced shadows/AO + samples:** `scene.eevee.use_raytracing = True`, `use_shadows = True`,
`taa_render_samples = 128` -- confirmed these are the real Blender 5.2 EEVEE Next property names
by introspecting `scene.eevee.bl_rna.properties` first rather than guessing (this build's engine
identifier is actually still `'BLENDER_EEVEE'`, not `'BLENDER_EEVEE_NEXT'` -- that enum value
doesn't exist on this build, confirmed by a `TypeError` when tried; EEVEE Next is what
`'BLENDER_EEVEE'` now means in 5.2. `setup_render()`'s existing three-way engine fallback already
handled this correctly by accident, via its own `except TypeError: continue`). `use_gtao` is gone
in EEVEE Next -- AO now rides on `use_raytracing`, not a separate legacy toggle.

**Portrait lens, distortion removed:** 24mm (turnaround) -- a wide-angle focal length that
perspective-stretches a full-figure subject -- moved to 60mm, a standard portrait/product-shot
focal length. Distance scaled by the same 60/24 ratio to hold framing (pinhole-camera model:
image size is proportional to focal_length/distance for fixed sensor width, confirmed exactly via
Blender's own `angle_y` FOV property before and after, not just assumed proportional -- both cases
computed to an identical 0.7083 frame-height fraction). **A second real, previously-undiscovered
bug found in passing while setting this up:** `03_posture_library.py`'s own comment claimed the
posture-library presentation shot used a 35mm lens, but its `if scene.camera is None` fallback
branch that actually set that value was dead code -- this script always loads the already-saved
`quark_base.blend`, which always already has `TurnCam` as `scene.camera` by the time this runs, so
every posture render to date had silently inherited TurnCam's lens (24mm pre-Tier-2) instead.
Fixed by setting the lens explicitly regardless of which camera object is active, not just in the
dead branch.

**A framing regression that measurement disproved, not confirmed:** an initial visual comparison
between an old and a new on-device screenshot looked like the character had shrunk to roughly half
its previous on-screen size after the lens change. Checked directly rather than accepted: a
controlled same-mesh, same-lighting A/B render pair (old lens/distance vs. new, both at the new
1500x2100 resolution, everything else held constant) measured bounding-box heights of 1014px vs.
990px -- a 2% difference, within scan-step noise, not a real regression. The screenshot comparison
that suggested otherwise was an imprecise eyeball comparison across two different capture sessions,
not a genuine defect; recorded here so a future session doesn't re-chase the same false lead.

**Resolution:** raised ~1.5x (turnaround 900x1400 -> 1350x2100; posture library 1000x1400 ->
1500x2100) now that the lighting/AA quality is worth resolving properly.

**Verification, same discipline as Tier 1 -- rendered and measured before shipping, then confirmed
end-to-end in the real app:** re-ran the full `01_base_mesh_and_rig.py` pipeline and
`03_posture_library.py` after each fix; re-checked the accent-key's green-dominance separation
still holds under the new lighting (body cluster `[-0.06, 0.03]`, accent cluster `[0.75, 0.94]`,
still a clean gap around the shader's `smoothstep(0.15, 0.35, ...)` threshold -- no shader change
needed). Copied the 3 regenerated PNGs into `quark-avatar/src/main/res/drawable-nodpi/`, rebuilt
`:app:assembleDebug`, booted the `Pixel_10a` GPU emulator, and drove NEUTRAL/THINKING/ALERT again
through the real CONFIG dev-preview UI: real dimensional shading and soft shadows visible on-device
(not just in the isolated Blender renders), no perspective distortion, live CYAN retint still
correct, the Tier-1 symmetric Thinking pose and corrected red Alert bake both still intact under
the new lighting.

**What did NOT need touching:** the shader (`QuarkAvatarShader.kt`) -- Tier 2 was purely upstream
Blender-side rendering quality plus the two incidental script bugs above; the accent-key/alpha
logic Tier 1 landed still holds under the new lighting without modification.

**Deferred (Tier 3 from the original plan, not started this session):** making the shader's
rim-glow offset resolution-derived instead of fixed-pixel, and tinting the rim from the live
phosphor hue instead of hardcoded white. Now more clearly optional/cosmetic since Tier 2 gave the
rim-glow a real physical backlight to sit on top of rather than being the only source of edge
definition.

**Next session:** Tier 3 shader polish, Director's call on priority -- or the still-open Fold 6
hardware confirmation, now doubly relevant to schedule since both Tier 1 and Tier 2 changed what
the bundled PNGs look like.

## Tier 3 — DONE: rim-glow resolution-derived offset + live-hue tint

The two shader-only polish items deferred out of Tier 1/2, both in `QuarkAvatarShader.kt`. No
Blender-side changes this pass -- pure AGSL, verified the only way this layer can be (rebuild,
install, look at the real running app on the `Pixel_10a` GPU emulator), same as Phase 4b's own
precedent for shader work.

**Resolution-derived rim offset:** the rim's edge-detection taps were a fixed 6px regardless of
the surface's actual render size -- reads thicker on a small surface, thinner on a large one,
instead of a consistent relative width. The `resolution` uniform existed in the shader source
since Phase 4b but was never actually read anywhere. Now the tap radius is
`max(2.0, resolution.y * 0.0045)` -- a fraction of render height, floored so it can't vanish on a
tiny surface.

**Rim detection reformulated, not just re-offset:** the old technique was a symmetric 4-tap
central-difference gradient straddling the silhouette edge -- roughly half of its response came
from taps OUTSIDE the mask, which can never actually reach the screen anyway (the shader's own
`if (mask <= 0.0) return transparent` at the very top already discards every such pixel before the
rim term would apply), so half the gradient's dynamic range was being spent computing a
contribution that gets thrown away. Replaced with an 8-direction (4 axis + 4 diagonal) minimum-
neighbor-mask sample: for a pixel already confirmed inside the mask, this asks "how close is the
nearest background pixel" directly using every tap's full range, rather than differencing inside
and outside taps against each other.

**Rim tint now hue-live, not hardcoded white:** `rgb += rim * float3(1,1,1) * 0.9` became
`rgb += rim * mix(float3(1,1,1), accentColor, 0.7) * 0.9` -- reuses the same `accentColor` uniform
the accent retint already receives, so the rim now tracks whatever phosphor hue (or the fixed
Alert red) is actually live instead of being a hue-independent white line. Kept a white bias in
the mix rather than pure hue so it still reads as a glow, not a flat color wash.

**Verification hit a real infrastructure snag, recorded rather than glossed over:** the GPU
emulator failed to come up on the first relaunch attempt this session -- `adb` reported it
`offline` for over 10 minutes with no forward progress in its own boot log (`WHPX ... operational`
was the last line, then nothing), while two prior sessions (Phase 4b, and this same session's
Tier 1/2 passes) had booted the identical AVD in under a minute each time. Diagnosed rather than
just retried blindly: found stale `hardware-qemu.ini.lock` / `multiinstance.lock` files in the AVD
directory (`~/.android/avd/Pixel_10a.avd/`) left over from a prior `adb emu kill`/relaunch cycle
in this same session, consistent with a lock not being released cleanly. Force-killed both the
`qemu-system-x86_64.exe` and `emulator.exe` processes, removed the two stale lock files, and
relaunched with `-no-snapshot-load` -- booted cleanly on the next attempt. Flagging this here in
case it recurs: this AVD's lock files are worth checking first before assuming a boot hang is
something deeper.

**On-device, both confirmed by screenshot, not assumed from the code:** NEUTRAL/CYAN now shows a
clearly visible, distinctly cyan-tinted rim around the whole silhouette (previously white and, per
Tier 2's own physical backlight, less necessary to even notice); cycling to ALERT shows the same
rim retint to a red/salmon tone tracking the fixed Alert accent color, confirming `accentColor`
genuinely drives the rim in both the live-hue and Alert-fixed-red cases, not just the accent patch
itself.

**What did NOT need touching:** the Blender scripts (`01_base_mesh_and_rig.py`,
`03_posture_library.py`) and the bundled PNGs -- Tier 3 is a pure downstream-rendering change, the
same three posture assets from Tier 2 are still current.

**All three tiers from the original rendering-refinement plan are now done.** Remaining open items
across the whole track: the real Fold 6 hardware confirmation (still the standing final word per
this track's own discipline, not yet done on any tier); a genuinely alpha-matted re-render was
actually delivered by Tier 1 (superseding the older "still open" note from Phase 4b); Thinking
pose's arm asymmetry was fixed by Tier 1; hair as stylized shape/volume vs. groomed strands and the
unused FACS facial bone set remain open per Phase 4c's own follow-up list, untouched by this
rendering-refinement track.

## ▶ RESUME HERE — Armor rebuild: the reference comparison that reframed the whole track

Director asked for a picture-to-picture comparison against the reference concept art. Built one
(`renders/reference_comparison.png` — the sheet's FRONT view cropped and scaled to the same figure
height as our render, so the comparison is like-for-like rather than impressionistic). The result
invalidated the framing of the three preceding tiers.

**What the comparison showed:** the reference is an armoured synthetic in a segmented, high-gloss
ceramic exoshell. Our render was **a nude MakeHuman body with grey patches painted on it**. The
"armour" had never been geometry at any point in this pipeline's history — it was purely
`poly.material_index` assignment on bare skin, and `_classify_material_index()`'s default branch
returned `ceramic`, so every unclassified body polygon was "armour" by fiat while remaining, in
form, naked anatomy. Phase 4c's own entry had flagged a fidelity gap but framed it as a
"texture-painting/hair-grooming polish tier" issue. **That framing was wrong**, and this is worth
recording plainly: the gap was structural. Tiers 1–3 (alpha, gamma, lighting, lens, rim glow) were
all real fixes and all correct, but they were polish on a model that was not the character — which
is exactly why they improved the image measurably without moving it toward the reference.

**What was rebuilt (`build_armor_shell()`, new):** the armour is now real, separate shell geometry
— the rigged body mesh duplicated, non-plate faces deleted, the survivors smoothed, offset clear of
the body, then Solidified for true plate thickness and Bevelled so each panel border catches a
specular edge. Built after rigging deliberately so it inherits MPFB's vertex groups and deforms
with the body (confirmed: plates follow the arms correctly in the re-rendered Thinking pose). The
body mesh underneath became a full-coverage dark **under-suit**, which is what actually resolves
the nudity — previously the un-plated remainder was literal bare skin.

**Region classification rewritten to be skeleton-relative.** The old rules were pure Z/|x| bands,
which cannot work on an A-posed mesh where a limb's height and lateral offset vary together. Plates
are now assigned by nearest bone segment plus parametric position along it (`LIMB_SEGMENTS`,
measured off the real generated rig via `bone.head_local`/`tail_local`, not guessed). The excluded
span at each end of a bone becomes the visible joint gap — shoulder, elbow, wrist, hip, knee, ankle
— which is how the reference's segmented armour reads.

**Five real bugs found by rendering and looking at each step, none of which code review would have
caught. Recorded with their wrong turns intact, per this track's standing discipline:**
1. **Stray ceramic cube at the world origin.** ~30% of an MPFB mesh (5778 of 19158 verts, measured)
   is MakeHuman fitting-helper and joint-cube geometry, invisible on the body only because MPFB
   adds a `Hide helpers` MASK modifier keeping the `body` vertex group. Duplicating the raw mesh
   bypassed that modifier, so the shell inherited all of it; the new boot rule then promoted
   MakeHuman's `joint-ground` cube from invisible-dark to bright white between the feet. Fixed by
   filtering on the same `body` group MPFB's own mask uses — the whole class, not the one cube that
   happened to show.
2. **Ceramic hands.** The hands hang off the lowerarm bone's `t=1.0` end, so they are FARTHER than
   `LIMB_RADIUS` from it and fell through the limb branch entirely — landing in the torso abdomen
   Z-band and getting plated. Fixed with an `|x| < 0.20` torso gate (torso never exceeds ~0.18
   half-width, measured).
3. **Shell sank inside the body.** First smoothing attempt (14 iterations, then a flat +6mm offset)
   ignored that Laplacian smoothing loses volume; the shell rendered as torn patchy islands on the
   thighs and shins.
4. **Nipples and toes read straight through the "armour".** Second attempt fixed the sinking by
   clamping every vertex to >=6mm outside the ORIGINAL body via BVH — which re-imprinted every
   protrusion the smoothing had just removed. The clamp was fighting the smoothing.
5. **A normal offset does not erase a feature, it translates it.** Third attempt raised the chest
   standoff assuming that would bury the nipples; it did not, because offsetting moves a bump
   outward along with the surface. (The toes only vanished at a 30mm standoff because neighbouring
   toe offset-surfaces merge into one another — morphological dilation, not smoothing.) Actually
   removing a small feature requires smoothing it away, so the bust now gets a concentrated
   region-targeted smoothing pass. The breast form itself is large and survives, which is correct:
   the reference's cuirass is shaped to the bust; only nipple-scale detail must not read through.

**Also fixed:** the emissive accent is now dedicated geometry (a brow circlet + spine strip) rather
than a Z-band of body polygons — a polygon classifier cannot draw a thin clean line on organic
topology, which is why it had been rendering as a thick ragged slab across the forehead. Its first
placement used a 0.083 circular radius and rendered as *nothing*, buried inside the skull: the head
is an ellipse at brow height (x half-width 0.0766, y half-depth 0.1056, centred y=-0.057 —
measured, and the circlet height set between the measured eye centres and the measured hairline).
The procedural voronoi panel/rivet pattern was turned OFF on the ceramic — it existed to fake seams
when the armour was paint, and with real geometry it only competed with the true panel lines as
dark diagonal smears. Ceramic gained a clear coat and dropped to roughness 0.22 for the sheet's
glazed look, and its base colour was corrected to the sheet's own stated `#E6E1DD` (the blue
channel was 0.816, i.e. `#E6E1D0` — measurably more yellow than specified, reading as a green-grey
cast).

**Shader recalibrated against the new bake, measured not assumed:** the accent now reads 0.72–0.95
green-dominance (cleaner, being dedicated emissive geometry), but the ceramic plates push a few
body pixels to 0.22 — inside the previous `smoothstep(0.15, 0.35)` ramp, which would have partially
retinted them. Moved to `smoothstep(0.35, 0.55)`: clear of the body maximum, well under the accent
minimum. Verified end-to-end on the `Pixel_10a` GPU emulator — armoured figure renders with correct
transparency, live CYAN accent + rim, and no false tinting anywhere on the body.

**Honest remaining gap to the reference, stated rather than glossed:** this is now unmistakably an
armoured character, but it is not the reference. Still open, roughly by impact:
- **Proportions** — ours reads as a realistic adult build; the reference is idealised (longer legs,
  narrower waist). A MakeHuman macro-tuning question, not a modelling one.
- **Panel borders are stair-stepped**, following mesh topology, where the reference has crisp
  machined lines. This is the real ceiling of the delete-faces-from-a-body-copy technique: plate
  outlines can only ever follow existing edge loops. Genuinely crisp panels need plates modelled as
  their own hard-surface forms (or a boolean cut), which is a substantially larger effort.
- **Face** is a blank mannequin — no eyebrows, flat eyes. The rig's unused FACS bone set and a real
  eyebrow/lash pass remain the open Phase 4c follow-up.
- **Hair** is still a solid blob vs. the reference's braided updo.
- **Accent** is one band vs. the reference's fine circuit tracery, ear module, and brow ornament.
- **Minor:** a faint bump reappears on one breast in the posed Thinking render but not in the rest
  pose — the shell was smoothed after inheriting weights, so its vertices deform very slightly
  differently from the body's. Flagged, not fixed.

**Next session:** Director's call. Highest visual return is probably proportions (cheap, macro
values) then face detail; crisp panel borders are the expensive one.

## ▶ RESUME HERE — Tooling survey + Phase A–D workplan toward the reference

Director's call before resuming: get the right tools first, on the explicit reasoning that a
refinement loop cannot substitute for them. Surveyed what is actually on this machine rather than
assuming, and the survey changed the plan.

**Three findings from the survey:**
1. **MPFB's asset system is entirely empty.** `AssetService.system_assets_pack_is_installed()`
   returns False; `list_mhclo_assets()` returns 0 for clothes/hair/eyebrows/eyelashes/eyes/teeth/
   proxymeshes; the MPFB user-data directory contains no files at all. **This corrects an inference
   in the Phase 4c entry above**, which recorded that "MPFB ships no default eyeball geometry, skin
   textures, or hair assets (confirmed by searching its installed files)". The observation was
   right, the conclusion was wrong: the asset *packs were never downloaded*. Eyes were hand-built
   from spheres and hair from primitives while a complete fitted-asset pipeline sat unused. That is
   the single biggest unforced limitation in this track's history.
2. **Bare Blender otherwise** -- 14 addons, all stock, plus MPFB. `io_curve_svg` IS enabled, which
   opens authoring panel outlines as SVG and importing them as boolean cutters.
3. **Probable cause of the ceramic still not reading as glazed:** the world is a flat dark colour
   (`0.05, 0.065, 0.05`). `film_transparent` removes the world from the *alpha*, not from
   reflections -- so the Tier-2/armor-pass clear coat has a perfectly uniform environment to
   reflect and produces almost no highlight variation. Gloss is mostly reflected environment.
   Untested hypothesis, cheap to falsify with a studio HDRI.

**Tooling decisions (Director):** CC0 asset packs only -- no CC-BY, so no attribution burden in the
shipped app. This rules out Hair 02 (high-poly) and Suits 02 (sci-fi suits) and accepts whatever
the CC0 hair offers. `makehuman_system_assets` (267 MB, CC0) is the key download: hair including
braids, 12 eyebrow sets, 4 eyelash sets, eye colours, skin textures, proxies, teeth. Plus a Poly
Haven studio HDRI (CC0) for the gloss hypothesis.

**Paid hard-surface addons assessed and declined, with reasons:** Hard Ops/BoxCutter, MESHmachine,
MACHIN3tools are the standard recommendations but are modal/interactive tools built for viewport
hand-modelling; this pipeline runs `--background` headless, so they fight the architecture. Quad
Remesher (Exoside) *is* scriptable and would give cleaner topology to boolean against, but built-in
QuadriFlow may suffice -- deferred until booleans demonstrably need it. **Nothing needs to be
bought:** the crisp-panel problem is a technique change, not a tooling gap. Face-deletion makes
plate borders follow existing edge loops (hence the stair-stepping); a boolean cut produces the
exact intersection curve independent of topology, then Bevel gives the machined edge.

**Ordered workplan. Phase B is deliberately NOT last:**
- **A. Tooling.** Install CC0 packs; add studio HDRI; verify assets load *headlessly* through
  MPFB's services -- its asset flow is built around interactive panels, so background-mode loading
  is a real risk to prove before building on it. Test the HDRI gloss hypothesis.
- **B. Proportions.** Retune the MakeHuman macros toward the reference's idealised build (longer
  legs, narrower waist). **This must precede the armour work.** Every constant in
  `01_base_mesh_and_rig.py` -- `LIMB_SEGMENTS`, all `Z_*` landmarks, the head-ellipse measurements
  driving the circlet, the per-region standoffs -- was measured against the current mesh, and the
  script's own header says they must be remeasured if the macro values change. Armour-first would
  mean measuring everything twice.
- **C. Face & hair.** Real eye assets replacing the primitive spheres, eyebrows, eyelashes, a
  fitted braided style replacing the hair blob, a real skin material. Biggest fix to the
  "uncanny mannequin" read.
- **D. Armour v2.** Boolean-cut plates for crisp borders, replacing face-deletion.

**Flagged as genuinely uncertain rather than promised:** CC0 hair may not match the reference's
specific braided updo, and MPFB's headless asset loading is unproven -- both get verified in
Phase A before anything is built on them.

### Phase A — DONE. Both flagged risks resolved; one confirmed, one partly negative.

**`makehuman_system_assets` installed (CC0, 267 MiB).** Download note worth keeping: the two
official mirrors differ by ~100x in throughput. `files2.makehumancommunity.org` delivered ~11 KB/s
(a >6-hour ETA); `files.makehumancommunity.org` delivered ~1.1 MB/s and supports HTTP range
requests, so the partial transfer was resumed cross-mirror with `curl -C -` and finished in ~31s.
Zip verified (517 entries, `testzip()` clean) before installing, since resuming one mirror's
partial against another risks a corrupt archive.

**Headless install works** -- the real risk, since MPFB's asset flow is built around interactive
panels. `AssetService.fix_and_extract_asset_pack_zip(zip, target_dir)` (note: takes a target dir,
not just the zip) into `LocationService.get_user_data()`, then `rescan_pack_metadata()` +
`update_all_asset_lists()`. Result: `system_assets_pack_is_installed() == True`; eyes 2,
eyebrows 12, eyelashes 4, hair 10, teeth 6, clothes 20, skins 23.

**Headless *loading* also works** -- listing is not loading, so this was verified separately.
`HumanService.add_mhclo_asset(path, human, asset_type=...)` successfully fitted eyes (1064 verts),
eyebrows (124), eyelashes (250) and hair (4493) onto a generated human, and
`HumanService.set_character_skin(mhmat, human, skin_type='ENHANCED_SSS')` applied a real
subsurface-scattering skin. All of this replaces hand-built primitive spheres and a procedural
flat skin tone.

**Verified by rendering, not by trusting the vert counts:** `renders/asset_face_test.png` -- a real
face with textured subsurface skin, irises with catchlights, eyebrows, eyelashes and defined lips,
against a Poly Haven studio HDRI (CC0). The difference from the previous blank-mannequin face is
not incremental.

**The negative result, recorded rather than glossed:** the CC0 hair set contains **no braided
updo/bun**. `braid01` is named for a braid texture but renders as a swept bob (see
`renders/hair_options/_hair_options_strip.png`, four candidates rendered for comparison). Closest
structural match to the reference is `ponytail01` -- hair pulled back off the face, which is the
silhouette the reference needs and, importantly, leaves the forehead clear for the emissive
circlet. A true braided updo would need either CC-BY Hair 02 (rejected on licence) or a modelled
hair pass. Director's call pending.

**Gloss hypothesis still untested** -- the HDRI is downloaded and proven to load, but has not yet
been swapped into `setup_render()` in place of the flat dark world, so whether it fixes the ceramic
clear-coat readability remains an open question, not a claim.

### Phase B — DONE (proportions), and it exposed a shape-key bug that had been silently discarding work

**Reference proportions measured, not eyeballed.** Analysed the concept sheet's own front view
(silhouette profile, normalised by figure height, identical code run against our render so
systematic error cancels): the reference's legs separate at ~0.49 of height from the top, ours at
~0.54. Leg length is therefore ~51% of total height in the reference against ~46% in ours -- a
concrete 5-point target rather than "longer legs".

**The `proportions` macro is a dead end, measured.** Swept across its entire 0.5-1.0 range it moved
the leg fraction by 0.0005 (0.4645 -> 0.4640) -- i.e. nothing. This is the same class of finding as
this track's earlier discovery that the `height` macro has no independent effect: MakeHuman's macro
sliders blend whole shape-key sets and several do not control what their names suggest. The fix was
MPFB's **detailed targets** (1258 of them ship with the addon), specifically
`legs/{l,r}-{upper,lower}leg-scale-vert-incr` and `torso/torso-scale-vert-decr`, applied via
`TargetService.load_target(obj, full_path, weight=...)`. Measured results:
`leg=0 -> 159.67cm / 0.450`; `leg=1 -> 167.99cm / 0.480`; `leg=1 torso=1 -> 163.68cm / 0.490`.
Shipped `leg=1.0, torso_shorten=0.35`: **166.5cm, leg fraction 0.485** (re-measured from the render
by the same silhouette method: separation moved 0.54 -> 0.515). About half the gap closed; the rest
is deliberately not forced, since maxing torso shortening buys 0.005 more fraction for 4cm of
height against an explicitly specified 167cm.

**Correction to an earlier entry.** Phase 4c recorded the mesh as "169.46cm (1.5% over spec)". That
measurement included MakeHuman's helper/joint-cube geometry, which extends below the feet. The real
body is 166.59cm base / 166.5cm evaluated -- essentially exactly the sheet's 167cm. There was never
a height discrepancy to explain.

**The real find: a shape-key bug that had been discarding the armour smoothing entirely.**
MakeHuman macros and targets are all SHAPE KEYS, and a Blender shape key stores ABSOLUTE vertex
positions, not deltas. `build_armor_shell()` was cloning `human.data`, so the shell inherited all
16 non-zero key blocks -- and every bmesh edit (smoothing, standoff offset, clearance clamp) was
written into base coordinates that evaluation then overwrote with the shape keys' own stored
positions. Measured proof: the shell's base z-range was (-0.023, 1.314) while its evaluated range
was (-0.118, 1.264). **The nipple-removal smoothing from the armour pass had never actually reached
the render**; it only appeared to work earlier by coincidence of which positions happened to
dominate. Fixed by building the shell from `bpy.data.meshes.new_from_object(evaluated)`, which
bakes shape keys and modifiers into plain geometry -- so the bmesh edits are what renders. Bonus:
that also applies MPFB's "Hide helpers" mask upstream, so the explicit `body`-group filter added
during the armour pass (to stop the stray `joint-ground` cube) is no longer needed and was deleted.

**And a second, older bug it uncovered: the classifier was mixing two coordinate spaces.**
`Z_ANKLE`/`Z_KNEE`/.../`Z_NECK` were `joint-*` vertex-group centroids measured on the BASE mesh,
while `LIMB_SEGMENTS` were bone positions read off the rig -- and MPFB fits the rig to the
EVALUATED body. The two spaces differ by a ~8.5cm shift, so the two rule sets had always disagreed
about where the body was; it survived only because the limb test is distance-based with a generous
radius. The Phase-B targets then made it undeniable, because they stretch legs and shorten torso
NON-uniformly, so no single remap can reconcile the spaces -- an attempted linear z-remap put the
bands on visibly wrong anatomy (a crop-top cuirass, blocky cut-outs across the hips). Replaced both
constant tables with `measure_shell_landmarks()`, which reads the real evaluated mesh and the real
fitted rig at runtime: crotch by connected-component scan, neck and limb segments from bones, and
torso bands expressed as fractions of the measured crotch->neck span (the fractions being exactly
what the previously-tuned absolute values worked out to, so this is a change of reference frame,
not a re-tune). All stale constants deleted rather than left lying around. Verified by rendering:
plate bands now sit correctly on chest / waist gap / hip / thigh / knee gap / shin / boot, with
proper elbow and wrist breaks.

**Also this pass:** the headband circlet is now placed from a live measurement of the evaluated
head (`_measure_head_ring`) instead of a hardcoded z. It had been hardcoded from a BASE-space
measurement and only *happened* to land on the brow; the crown differs by 8.6cm between the two
spaces, so any proportion change would have silently moved it.

**Still open / honestly flagged:** a faint bump remains on one breast; ponytail01 is chosen per
Director but not yet wired into the pipeline (still the procedural hair blob in these renders);
Phase C (assets into the real pipeline) and Phase D (boolean-cut crisp panel borders) not started;
gloss/HDRI hypothesis still untested in `setup_render()`.

### Director phenotype rebuild + Phase C — DONE

Director supplied MPFB "New human" panel settings directly (screenshot) and asked for a rebuild
around them. Rather than transcribe the dropdown labels by eye, read MPFB's own
`ui/new_human/newhuman/operators/createhuman.py` to get the exact formulas, so the macro dict in
`create_and_tune_human()` now reproduces precisely what that UI would have produced:

| panel | formula | value |
|---|---|---|
| Gender Female | `0.5 - phenotype_influence*0.5` (influence 1.00) | 0.0 |
| Age Young | fixed | 0.5 |
| Muscle Average | no branch fires -> default | 0.5 |
| Weight **Minimum** | `0.5 - phenotype_influence*0.5` | **0.0** (was 0.4) |
| Height Average | default | 0.5 |
| Proportions Average | default | 0.5 |
| Race **Caucasian** | single race = 1.0 | **1.0/0/0** (was mixed) |
| Breast size **Larger** | `0.5 + breast_influence*0.5` (0.58) | **0.79** (was 0.5) |
| Firmness **More firm** | `0.5 + breast_influence*0.5` | **0.79** (was 0.6) |

**A mechanism explained, not just observed.** The `proportions` enum is literally
"Inverted V-shape / Average / V-shape" -- a shoulder-versus-hip WIDTH axis. That is *why* the
earlier sweep measured it doing nothing to leg length. It was never a leg control, which retro-
justifies handling leg length through detailed targets instead.

**Measured outcome (Director chose to keep the Phase-B leg targets on top):**
`167.41cm, leg fraction 0.500` -- against the sheet's stated 167cm and the reference's measured
0.51. Best yet; previous build was 166.5cm / 0.475. Without the leg targets the same macros give
160.6cm / 0.465, which is why they were kept.

**The predicted regression happened, and was flagged before it did.** `cupsize 0.5 -> 0.79` plus
firmer re-exposed nipple detail through the cuirass, because the 15mm chest standoff and
26-iteration smoothing had been tuned against the smaller bust. Retuned to 48 iterations / 22mm and
verified on a 2x-upscaled crop of the actual render, not by eye at full-figure scale.

**Phase C landed in the same rebuild:** `add_makehuman_assets()` replaces the hand-built
primitives -- MakeHuman high-poly eyes, eyebrow008, eyelashes01, and `ponytail01` hair (Director's
pick; the CC0 set has no braided updo, and ponytail01 was chosen because it clears the forehead for
the circlet), plus `young_caucasian_female2` skin at `ENHANCED_SSS`. Material ordering matters and
is documented in-code: `set_character_skin` writes into the body's material slots, so it must run
BEFORE `assign_materials()`, which now reuses that skin for the face instead of overwriting it with
the flat procedural tone.

**Gloss hypothesis: CONFIRMED.** Swapping the flat dark world for the Poly Haven `photo_studio_01`
HDRI (CC0, now committed to `art/quark-avatar/hdri/` so the pipeline is self-contained) made the
ceramic finally read as glazed white with real specular variation rather than flat grey. Gloss is
mostly reflected environment, and `film_transparent` removes the world from the alpha but not from
reflections -- so the clear coat previously had nothing but constant grey to mirror. Exposure then
had to be rebalanced, measured rather than eyeballed: at HDRI strength 1.0 the plates clipped
(mean luminance 0.76, torso 0.99). Reducing the area lights barely moved it (0.66 at 35%, 0.65 at
15%) because the HDRI now dominates, so the fix was HDRI strength; settled at strength 0.45 with
lights at 35% -> mean 0.52, no blown highlights.

**Shader recalibrated against the new bake:** body/armour pixels now top out at 0.01 green-dominance
while the accent spans 0.46-0.90 -- a far wider gap than before. The 0.35-0.55 ramp would have left
the dimmest accent pixels partially tinted, so it moved to `smoothstep(0.12, 0.28)`. Posture library
regenerated, copied into `drawable-nodpi`, `:app:assembleDebug` green.

**Still open, unchanged:** stair-stepped plate borders (Phase D, the boolean-cut work -- still the
single biggest remaining gap to the reference); no braided updo in CC0; fine circuit tracery, ear
module and brow ornament absent; on-device emulator verification of this build not yet run.

### Prototype-art evaluation + Option B (real-time 3D) feasibility

**Director-supplied art evaluated for direct AGSL use.** Measured, then tested in the real app:

| file | size | alpha==0 | verdict |
|---|---|---|---|
| QUARK_prototype.png | 200x550 | 61.2% | renders correctly, but subject only 156x512 vs an 864px-wide slot |
| QUARK_HiRes.png | 1024x1536 | 0.0% (colortype 2 = RGB) | no alpha channel at all |
| QUARK_HiRes_draft.png | 560x1536 | 0.0% | opaque dark |
| QUARK_HiRes1.png | 560x1536 | 0.0% | RGBA container, alpha entirely 255 |

**A measurement error of mine, corrected by the app.** An early read of `QUARK_HiRes1.png` reported
"68.5% transparent". That was wrong: Blender's image loader returned `has_data == False` and
`image.pixels` yielded garbage. The in-app render (a white box behind the figure) contradicted the
measurement, and re-reading with an assert on `has_data` -- and finally with a hand-written PNG
IDAT/zlib decoder that removes Blender from the loop entirely -- confirmed 100% opaque. Lesson
recorded: `bpy.data.images.load()` can silently yield empty pixel data; always assert `has_data`.

**Why the art can't drive the accent:** the shader keys green-dominance; all supplied art uses a
CYAN accent (blue-dominance up to 0.196, green-dominance never above 0.024), so the key can never
fire. Confirmed on-device: cycling the hue retinted the shader-generated rim and the app chrome
while the character's own knee accents stayed cyan.

**Option B (real-time 3D) -- FEASIBLE, measured not estimated.** Exported the existing rigged
character to glTF and validated by re-importing:

| variant | size |
|---|---|
| geometry only, Draco | **0.49 MB** |
| animated + 1024^2 textures + Draco | **5.68 MB** |
| animated + 512^2 textures + Draco | **3.92 MB** |

73,852 tris as-authored; textures are ~8.4MB of the unoptimised 11MB, geometry is negligible.
Round-trip re-import confirms 1 armature, 2 skinned meshes (body + armour) and the authored
`QUARK_Idle` action all survive. For scale: the three posture PNGs currently ship at ~3.2MB, so a
3.9MB animated GLB is roughly size-neutral while adding animation and arbitrary pose/angle.

**Export-prep step required (found, not glossed):** `export_apply=True` cannot be combined with
shape keys, but the body carries 16 macro shape keys AND a "Hide helpers" MASK that must be applied
(or the joint-cube helper geometry ships), while the armour needs its Solidify/Bevel applied (or the
plates export with no thickness). A production exporter therefore needs a bake-down pass on
duplicates -- apply modifiers, strip shape keys -- before `export_scene.gltf`.

**Runtime:** SceneView 4.22.0 (`io.github.sceneview:sceneview`), Filament-backed, Compose-native.
Project is compatible as-is: minSdk 33, compileSdk 35, Compose BOM 2024.10.01, JVM 17.

**Architectural consequence worth stating plainly:** Option B would retire the locked
"pre-rendered frames + AGSL overlay" hybrid path. That is not purely a loss -- the accent retint
stops being a fragile colour-key over a baked image and becomes a real emissive material parameter
driven directly from `PhosphorHueRuntime`, which removes the entire class of bug this log has spent
several passes on. Rim glow and Stealth dim likewise become material/fresnel parameters.

**Unmeasured risk, flagged:** Filament ships native `.so` libraries per ABI; their contribution to
APK size has NOT been measured and could exceed the model itself. Also unmeasured: continuous-3D
battery/thermal cost for an always-visible avatar, and Fold 6 behaviour.

### Kotlin 2.2.21 -> 2.4.0 upgrade (unblocks SceneView / Option B)

Adding `io.github.sceneview:sceneview:4.22.0` failed to compile: the library carries **Kotlin 2.4.0
metadata** while the project pinned **Kotlin 2.2.21** (reads up to 2.3.0). Diagnosed with the
`android-gradle-kotlin-jdk-compatibility` skill's four-dimension framework rather than guessing --
confirmed the other three dimensions were already clear (Gradle 8.9 host OK, JDK 17 daemon OK, no
`org.gradle.java.home` pin) so only the Kotlin Gradle Plugin dimension needed moving.

**Changes:** `org.jetbrains.kotlin.android`, `.jvm`, and `.plugin.compose` all 2.2.21 -> **2.4.0**
in lockstep (the Compose plugin version must equal the Kotlin version).

**One real API break, fixed across 12 modules:** Kotlin 2.4 turns `android { kotlinOptions {
jvmTarget = "17" } }` from a deprecation into a hard error. Migrated each module to a top-level
`kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`.

**KSP left at 2.2.21-2.0.5 deliberately.** It works fine under Kotlin 2.4.0 (verified by forcing
`:optics:kspDebugKotlin` to genuinely re-execute with `--rerun-tasks`, not accept a FROM-CACHE
result). Bumping to the current KSP release (2.3.11 -- note KSP moved off Kotlin-prefixed
versioning after 2.2.21-2.0.5) fails with
`'void AndroidComponentsExtension.addKspConfigurations(boolean)'`: it needs a newer AGP than 8.7.2.
Not worth dragging AGP into this when the existing pin works.

**Verified, not assumed:** full `:app:assembleDebug` green; SceneView 4.22.0 then compiles cleanly;
app installed and run on the Pixel_10a emulator with the QUARK screen exercised (posture + hue
cycling both working, accent and rim retinting to AMBER) and `logcat -b crash` clean.

**APK cost measured -- and it corrects an earlier estimate of mine.** I had estimated Filament at
3.07MB (arm64) / 12.40MB (all ABIs) from compressed sizes inside the AARs. The real APK delta is
substantially larger:

| build | APK |
|---|---|
| baseline (Kotlin 2.2.21, no SceneView) | 191.57 MB |
| Kotlin 2.4.0, no SceneView | 199.87 MB (**+8.30 MB** for the upgrade alone) |
| Kotlin 2.4.0 + SceneView | 228.70 MB (**+28.83 MB** for SceneView) |

arm64 Filament libs land at 6.34MB in the APK (`libgltfio-jni` 2.94 + `libfilament-jni` 2.87 +
`libfilament-utils-jni` 0.53) versus the 3.07MB I read from the AAR -- APK packaging/alignment
inflates them, and the AAR scan also missed transitive native deps. **Caveat: these are unminified
DEBUG builds.** A release build with R8 would strip much of the dex growth, so the +8.30MB Kotlin
figure in particular is an upper bound, not the shipping cost. ABI splits or an App Bundle would cut
the ~25MB native portion to a single ABI.

**Current state: Kotlin 2.4.0 upgrade KEPT and committed to the working tree; the SceneView
dependency REVERTED** -- it is pure APK weight until the 3D view is actually implemented, and
Option B is still a pending decision, not a settled one.

### Postures reworked to the reference + first ComfyUI polish test

**Postures.** Cropped and enlarged the reference sheet's own POSTURE & EMOTION STATES row rather
than working from the full-sheet thumbnail. Two findings:
- Every standing state has the arms held CLOSE to the body, a much narrower silhouette than MPFB's
  rest A-pose. `set_pose_relaxed_idle()` had been a deliberate no-op on the reasoning that the rest
  pose was "already relaxed" -- that was reading the sheet too loosely. Now tucks the upper arms in
  by 20 degrees. **Sign verified by render**: the first attempt used the opposite sign and splayed
  the arms WIDER.
- **Recorded discrepancy:** the sheet's THINKING (Processing) thumbnail shows STEEPLED HANDS
  TOGETHER at chest height, NOT a hand on the chin. Director asked for the Rodin "Thinker" reading
  explicitly, so that is what is built; the sheet's version remains a small change away.

**Thinking pose rebuilt with IK.** Hand-authored world-axis rotations could not land the hand on the
chin -- the arm swung out to the side, because one world-axis rotation per bone cannot express
"reach that point" on a chain whose rest orientation is tilted in three axes. Added `_ik_reach()`:
a temporary IK constraint plus target empty, solved, then `visual_transform_apply()` bakes it to
plain bone rotations and the constraint/empty are deleted, so the saved posture has no leftover
dependencies. Chin target measured off the evaluated head mesh (~(0,-0.13,1.39)), not guessed.

Four rendered variants, each failing for a specific recorded reason: **A/B** reached chin HEIGHT
but the hand sat beside the neck palm-out (reads as a wave -- IK positions the wrist and says
nothing about hand ORIENTATION); **C** wrist rotated Z-55/X-25, fingers turned toward the face but
short of it; **D** wrist Z-80/Y+30 with the target pulled in and down -- knuckles under the jaw.
**D shipped.**

**ComfyUI polish workflow -- TESTED END TO END, and it works, with one hard caveat.**
ComfyUI 0.33.2 found running locally on :8188 with its HTTP API open, so the whole loop was driven
from here rather than handed over as a recipe. Available: SDXL base + refiner, Flux 2 Klein.
**No ControlNet models are installed** -- which turns out to be the crux.

Pipeline built: Blender posture render -> composite over a dark studio backdrop at 832x1216 (SDXL
portrait res) -> `LoadImage`/`VAEEncode`/`KSampler`/`VAEDecode` img2img -> retrieve via `/view`.
The alpha matte is exported alongside so it can be re-applied afterwards (img2img returns opaque).

Denoise sweep, all rendered and compared:

| denoise | quality gain | pose | accent |
|---|---|---|---|
| 0.45 | large -- photoreal skin, crisp machined panel seams, blonde hair matching the reference | **DESTROYED** -- Thinker hand gone, arm back at the side | green headband lost |
| 0.32 | moderate | mostly preserved, hand-at-chin softened | **green headband survives** |
| 0.25 | slight | preserved | green headband survives |

**The finding that matters:** there is a direct trade-off between fidelity gain and pose/accent
integrity, and at the denoise needed for reference-level quality (~0.45) img2img re-invents the
pose -- exactly the consistency risk flagged when this workflow was proposed. **A ControlNet
(depth or openpose, SDXL) would break the trade-off**: it locks the pose structurally, allowing
high denoise for quality without drift. That single missing model is the difference between this
workflow being a curiosity and being production-viable, and is the concrete next step.

---

## Phase 5 — SceneView / Option B taken end to end, on device

Director's call this session: park the ControlNet prerequisite and go **experience the SceneView
workflow** instead. So Option B stopped being a paper estimate and became a running build: Blender
→ GLB → Filament → the real app, on the Pixel_10a emulator, with the live phosphor hue driving the
accent. **It works.** Screenshots in `renders/sceneview_device/`.

### The exporter (`blender/scripts/04_export_gltf.py`)

The two blockers flagged last session were real, and `bake_down()` solves both in one pass: hide the
ARMATURE modifier, take the depsgraph-evaluated mesh via
`bpy.data.meshes.new_from_object(..., preserve_all_data_layers=True)`, swap it in as the object's
data, re-create the ARMATURE modifier. That **bakes** the shape-key mix rather than removing it —
`shape_key_remove(all=True)` would have thrown the Director's whole phenotype away, since MakeHuman
encodes height/gender/muscle/weight as a shape-key mix — and applies MASK / SOLIDIFY / BEVEL /
SUBSURF while leaving skinning intact. Export then runs with `export_apply=False`.

Measured bake-down: `QUARK_Armor` 4,769 → 21,224 verts (Solidify+Bevel real), `QUARK_Base`
19,158 → 14,568 (the "Hide helpers" MASK really removed MakeHuman's joint cubes). Output:
**8.53 MB GLB** with Draco, one root node, 8 meshes, 1 skin, 163 joints.

### Two bugs the FIRST GLB had, both found by rendering and looking

1. **Entirely flat white.** The re-imported GLB rendered as a white mannequin
   (`renders/glb_verify_front.png`). Cause, confirmed in the GLB JSON: `QUARK_Ceramic` /
   `UnderSuit` / `Graphite` / `MetalAlloy` drive Base Color and Roughness from
   Noise/Voronoi/Wave/Geometry node chains, and glTF cannot represent a procedural graph — the
   exporter silently drops the link and ships the socket default, i.e. `baseColorFactor` absent =
   white. Fixed with `flatten_materials_for_gltf()`, which reads each material's OWN first RGB node
   and its OWN Roughness-linked ramp so the constants are authored values, not invented ones:
   Ceramic (0.902, 0.882, 0.867) r=0.135, UnderSuit (0.098, 0.106, 0.125) r=0.40, Graphite
   (0.169, 0.176, 0.192) r=0.55, MetalAlloy (0.784, 0.800, 0.820) r=0.30. **Known cost, stated
   plainly:** the ceramic mottling, brushed-metal anisotropy and panel grain are gone. The real fix
   is a bake pass to base-colour / ORM / normal maps.
2. **Hair, eyes, brows, lashes, headband and spine conduit all exported with `skin: None`.** They
   are OBJECT-parented, which the Blender viewport honours but glTF's node hierarchy does not
   reproduce under a *skinned* parent — on device the head would turn and the hair would stay
   behind. Fixed by `rigid_bind()`: one full-weight vertex group on the bone each rigidly belongs to
   (`head`, or `spine03` for the conduit), re-parented to the rig. Re-import now reports an ARMATURE
   modifier on all 8 meshes.

**Not a bug, recorded so it is not re-chased:** the re-import also lists an `Icosphere` (42 verts)
and pulls the world bbox down to z = −1.0. It is not in the GLB — the JSON has exactly 8 meshes
under a single `QUARK_Rig` root. Blender's glTF *importer* creates it as a bone-display shape. The
honest measured figure with it excluded is **1.71 m** tip-to-toe including hair.

### Three corrections to earlier entries in this log

- **"the authored `QUARK_Idle` action survives export" is WRONG.** `quark_base.blend` contains
  **zero actions**, and no script in `blender/scripts/` authors one. That action was made in a
  throwaway session and never saved, so last session's "animated + textures + Draco = 3.92 / 5.68
  MB" figures describe a file that cannot be reproduced from the repo. The current reproducible
  number is 8.53 MB, static.
- **"1 armature, 2 skinned meshes (body + armour)" understated it.** The scene is 8 meshes: body,
  armour, headband, spine conduit, hair, eyes, eyebrows, eyelashes.
- **"the body carries 16 macro shape keys"** — it carries **12**.

### The Android side (`ui/scene/Quark3dView.kt`)

SceneView 4.22.0, `implementation("io.github.sceneview:sceneview:4.22.0")` on `:quark-avatar`.
Notably the API guessed from the skill's canonical example compiled **first try**: `rememberEngine`
/ `rememberModelLoader` / `rememberEnvironmentLoader` / `rememberModelInstance`, then
`SceneView(...) { LightNode(...); ModelNode(modelInstance = …, scaleToUnits = …, centerOrigin = …,
rotation = …) }` with an `onFrame` lambda driving a slow turntable yaw. Defaults cover engine,
environment, camera and gestures — there is very little ceremony.

The GLB ships at `quark-avatar/src/main/assets/models/quark.glb`. `QuarkAvatarScreen` gained a
**RENDER** row toggling `3D (SceneView)` against `2D (baked+AGSL)`, so both paths run in the same
app on the same screen with the same hue and Stealth state. That is the comparison the Director
needs, made concrete rather than argued.

**The architectural claim is now demonstrated, not asserted.** The accent retint is a single
`materialInstance.setParameter("emissiveFactor", r, g, b)` on the shared `QUARK_Emissive` material.
Headband and spine conduit both recolour; nothing else can be caught by accident. The entire
green-dominance colour-key apparatus — and the class of bug it generated — is simply not needed on
this path. Alert's fixed `--warn` red still comes through correctly, because the screen already
decides `accentColor` before either path sees it.

**One measured on-device fix.** The first 3D render showed an AMBER headband as a **white bar with
a faint amber halo** (`renders/sceneview_device/device_3d_amber_accent.png`). `QUARK_Emissive`
carries `KHR_materials_emissive_strength = 7.0`, which Filament multiplies by `emissiveFactor`, so
writing the raw sRGB accent lands at ~7× and clips. `ACCENT_GAIN = 0.3f` restores a readable hue —
verified by switching to CYAN and seeing the spine conduit read cyan
(`device_3d_cyan_back.png`). `scaleToUnits` also went 1.7 → 1.15; at 1.7 the arms and feet ran
off-frame.

### Measured

| | |
|---|---|
| GLB (Draco, 2K textures, static) | 8.53 MB |
| APK, Kotlin 2.4.0 + SceneView + GLB (debug, all ABI) | **237.17 MB** |
| …versus SceneView-without-model last session | 228.70 MB (so the model itself is +8.5 MB) |
| Filament backend on Pixel_10a emulator | OpenGL ES 3.1, feature level 1, no active workarounds |
| frame times, emulator, turntable running | 50th pct **17 ms**, 90th **133 ms**, 95th **400 ms** |

**The frame-time number is not a verdict.** It is a software-GL emulator translating to a host GPU
while the model spins continuously, and the 90th/95th percentiles are dominated by first-frame
shader compilation and asset load. Real-time cost has still **never been measured on hardware**, and
the Fold 6 pass still has not been run. Do not let this table settle the Option A/B decision.

### Open defects on the 3D path, seen on device

- **The eyes are broken in Filament.** They render as detached opaque grey spheres in front of the
  face, and the eyelashes as a dark card. The same GLB re-imported into Blender renders the face
  correctly, so this is not an export bug — it is MakeHuman's high-poly eye being a transparent
  cornea shell over an iris, and Filament shading it opaque. Needs an alpha/blend-mode pass on those
  materials.
- A small red/salmon triangle sits on the right side of the neck at every angle — unclassified
  geometry or a stray material assignment.
- Stair-stepped plate borders (Phase D booleans) are **more** visible in real-time 3D than in the
  Cycles renders. Still the single biggest fidelity gap.
- No idle animation exists to play, because no action exists to export.

### Where this leaves the decision

Option B is no longer hypothetical: it renders, it is hue-driven, it costs ~8.5 MB of model on top
of the ~28.8 MB of SceneView, and it retires the colour-key. Its ceiling is still short of the
reference art, and this build is short of its own ceiling (flat materials, broken eyes, no
animation). Option A (ComfyUI-polished frames) remains gated on the SDXL ControlNet install, which
was deliberately deferred this session and is still the right first move on that track.

**The Director has still not chosen.** Nothing here was committed.

---

## ▶ SESSION CLOSE — next session is dedicated to QUARK rendering

**Working tree state at close:** 39 tracked files modified, 9 untracked additions (the `hdri/`
folder, four Director-supplied reference PNGs, three ComfyUI test outputs, and
`reference_comparison.png`). `:app:assembleDebug` is **green**. **Nothing has been committed** --
the Director commits personally.

### What shipped this session

1. **Reference comparison built** (`renders/reference_comparison.png`) and it reframed the track:
   the render was a nude MakeHuman body with grey patches painted on it, because the armour had
   never been geometry. Tiers 1-3 were real fixes but polish on a model that was not the character.
2. **Armour rebuilt as real shell geometry** + full-coverage under-suit; skeleton-relative plate
   classification; boots; accent as dedicated geometry.
3. **Director's phenotype applied** (Weight Minimum, Larger/More firm at 0.58 influence, pure
   Caucasian), transcribed from MPFB's own operator formulas -> **167.41cm, leg fraction 0.500**
   against the reference's measured 0.51.
4. **Phase A/C assets**: MakeHuman CC0 system assets installed and wired in -- real eyes, eyebrows,
   eyelashes, `ponytail01` hair, `ENHANCED_SSS` skin.
5. **Gloss hypothesis confirmed** -- studio HDRI (Poly Haven CC0, committed to `art/quark-avatar/hdri/`)
   made the ceramic finally read as glazed; exposure rebalanced by measurement.
6. **Kotlin 2.2.21 -> 2.4.0** across 12 modules, unblocking SceneView. Verified by build AND by
   running on the emulator.
7. **Postures reworked to the reference**: arms tucked in; Thinker pose built with IK.
8. **ComfyUI polish workflow proven end to end** against the live local instance.

### Bugs found by rendering and looking, not by reasoning

Shape keys silently discarding every bmesh edit on the armour shell; the classifier mixing BASE and
EVALUATED coordinate spaces; MakeHuman helper geometry leaking a ceramic cube to the world origin;
hands classified as torso; a normal offset translating features rather than erasing them; the
arm-tuck sign inverted; `bpy.data.images.load()` returning silent garbage when `has_data` is False.

### Two corrections to earlier entries in this log

- Phase 4c's "MPFB ships no eyeball geometry, skin textures, or hair assets" -- the observation was
  right, the inference wrong. The asset packs had simply never been downloaded.
- Phase 4c's "169.46cm (1.5% over spec)" -- that measurement included MakeHuman helper geometry.
  The real body is 166.5cm. There was never a height discrepancy.
- (And one of mine, mid-session: an early alpha reading of `QUARK_HiRes1.png` was garbage from a
  failed Blender image load; the in-app white box was the ground truth that caught it.)

### ▶ NEXT SESSION — QUARK RENDERING. Start here.

**The single highest-value action, and it is a prerequisite, not a nice-to-have:**
install an **SDXL ControlNet (depth and/or openpose)** into ComfyUI. The polish workflow is built
and proven, but measured: at denoise ~0.45 the quality reaches toward reference level and the POSE
IS DESTROYED; at 0.32 the pose and the green accent survive but the quality gain is modest. A
ControlNet locks pose structurally and breaks that trade-off. Everything else downstream is gated
on it.

Then, in order:
1. Wire the ControlNet-conditioned workflow (depth pass rendered from Blender as conditioning) and
   re-run the denoise sweep -- expect high denoise to become usable.
2. Re-apply the exported alpha matte to polished frames, and verify the green accent still keys
   (shader band is currently `smoothstep(0.12, 0.28)`; body tops out at 0.01, accent 0.46-0.90).
3. Push the polished postures through to the app and verify on the emulator.

**Still open, unchanged:** stair-stepped plate borders (Phase D booleans -- the biggest remaining
3D-side gap); no braided updo in CC0 hair; fine circuit tracery / ear module / brow ornament absent;
Fold 6 hardware pass never done.

**Decisions still genuinely open (do not treat as settled):** Option B (real-time 3D via SceneView)
vs. the ComfyUI-polished pre-rendered frame path. Measured inputs for that call: SceneView costs
**+28.83MB** APK (debug, all ABIs; arm64-only and R8 would both cut it) and would retire the
colour-key accent hack entirely in favour of a real emissive material parameter -- but its fidelity
ceiling is "very good stylised game character", short of the reference. The ComfyUI path can reach
reference fidelity but only for pre-baked poses. **The Director has not chosen between them.**

---

## ▶ RESUME HERE — after the Phase 5 SceneView session

**The "SESSION CLOSE" block above is superseded.** Its plan (ControlNet first) was deliberately
set aside when the Director asked to experience the SceneView workflow instead. Read the **Phase 5**
entry for current state.

**State of the working tree:** SceneView 4.22.0 is back on `:quark-avatar`, `assets/models/quark.glb`
(8.53 MB) is in the module, `blender/scripts/04_export_gltf.py` and
`ui/scene/Quark3dView.kt` are new, and `QuarkAvatarScreen`/`QuarkAvatarActivity` carry a RENDER
2D↔3D toggle. `:app:assembleDebug` is green, installed and exercised on the Pixel_10a emulator.
**Nothing committed — the Director commits personally.**

**Next, on the 3D path, in rough value order:**
1. Fix the eyes (opaque grey spheres in Filament) — alpha/blend mode on the MakeHuman high-poly eye
   and eyelash materials. Most visible defect by far.
2. Author an idle action in Blender and export it, so the avatar breathes instead of spinning.
   Nothing in the repo authors one today.
3. Bake the procedural materials to base-colour / ORM / normal textures, replacing the flat
   constants `flatten_materials_for_gltf()` currently ships.
4. Track down the red/salmon triangle on the neck.
5. Measure on real hardware — every perf number so far is emulator-only, and the Fold 6 pass has
   never been run.

**Next, on the pre-rendered path:** unchanged — install an SDXL ControlNet (depth and/or openpose)
into the local ComfyUI. Still the gate on everything downstream there.

**The Option A / Option B decision is still the Director's and still open.**
