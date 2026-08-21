"""
QUARK base-mesh + rig — Phase 4c of the QUARK 3D avatar pipeline (cosmetic detail pass).

Rebuilds the character on a real MPFB (MakeHuman-for-Blender, `bl_ext.blender_org.mpfb`) human
base mesh instead of the earlier hand-lofted primitive blockout -- a real face (eyes, nose, mouth,
cheekbones, jaw), a real body, and stylized hair, closing the gap the old blockout could never
close procedurally. See `art/quark-avatar/PRODUCTION_LOG.md`'s Phase 4c entry for the full
rationale, the fidelity ceiling this deliberately does not chase (photoreal skin/individually
groomed hair strands), and every measured-not-assumed calibration below.

Reuses verbatim from the prior blockout pipeline (genuinely mesh-agnostic): `make_material()`,
`bake_textures()`, `setup_render()`, `render_turnaround()`, `direction_to_euler()`,
`set_emissive_color()`. Everything else is new: human generation/tuning, eyes, hair, material
region classification (recalibrated against this mesh's own measured landmarks, not the old
blockout's), and rigging (MPFB's own "default" standard rig + auto-weighting, replacing the old
hand-built armature).

Run headless:
  blender --background --python 01_base_mesh_and_rig.py
"""
import bpy
import bmesh
import math
import mathutils
from mathutils.bvhtree import BVHTree
import os
import addon_utils

addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.humanservice import HumanService
from bl_ext.blender_org.mpfb.services.targetservice import TargetService
from bl_ext.blender_org.mpfb.services.locationservice import LocationService


def clear_scene():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for block in (bpy.data.meshes, bpy.data.materials, bpy.data.armatures, bpy.data.images):
        for item in list(block):
            if item.users == 0:
                block.remove(item)


# ---------------------------------------------------------------------------------------------
# Human generation -- tuned to the reference sheet's stated build (167cm, Athletic/Feminine).
# Measured, not assumed: at these exact macro values this mesh comes out 169.46cm tall (max_z
# 1.6682, min_z -0.0264) -- MakeHuman's height slider blends a whole shape-key set together with
# gender/age/muscle/weight rather than moving independently, so a further height-only nudge (tried
# empirically) had zero effect; 1.5% off the sheet's stated height was judged close enough to not
# fight the interpolation system for. All downstream landmark constants below are measured against
# THIS exact tuned mesh -- if these macro values ever change, every constant after this point needs
# remeasuring, not just eyeballing.
# ---------------------------------------------------------------------------------------------
def create_and_tune_human():
    # Director-specified phenotype, transcribed from MPFB's own "New human" panel. These are not
    # hand-picked numbers: each is what MPFB's `createhuman.py` operator computes for the chosen
    # dropdown, so this dict reproduces exactly what that UI would have built.
    #
    #   panel setting            MPFB formula                       value
    #   Gender:      Female      0.5 - phenotype_influence * 0.5    0.0    (influence 1.00)
    #   Age:         Young       fixed                              0.5
    #   Muscle:      Average     (no branch fires -> default)        0.5
    #   Weight:      Minimum     0.5 - phenotype_influence * 0.5    0.0
    #   Height:      Average     (no branch fires -> default)        0.5
    #   Proportions: Average     (no branch fires -> default)        0.5
    #   Race:        Caucasian   single race set to 1.0             1.0 / 0.0 / 0.0
    #   Breast size: Larger      0.5 + breast_influence * 0.5       0.79   (influence 0.58)
    #   Firmness:    More firm   0.5 + breast_influence * 0.5       0.79
    #
    # Note on `proportions`: its enum is literally "Inverted V-shape / Average / V-shape" -- a
    # shoulder-versus-hip WIDTH axis. That is the mechanism behind this pipeline's earlier measured
    # finding that sweeping it does nothing to leg length; it was never a leg control, which is why
    # leg length is handled by detailed targets in `_apply_proportion_targets()` instead.
    macro = TargetService.get_default_macro_info_dict()
    macro['gender'] = 0.0
    macro['age'] = 0.5
    macro['muscle'] = 0.5
    macro['weight'] = 0.0        # Minimum -- markedly leaner than the previous 0.4
    macro['proportions'] = 0.5
    macro['height'] = 0.5
    macro['cupsize'] = 0.79
    macro['firmness'] = 0.79
    macro['race'] = {'asian': 0.0, 'caucasian': 1.0, 'african': 0.0}

    human = HumanService.create_human(
        mask_helpers=True, detailed_helpers=True, extra_vertex_groups=True,
        feet_on_ground=True, scale=0.1,  # 0.1 is MPFB's own "meters" convention -- scale=1.0
        # (tried first) produces a ~17m-tall mesh; confirmed by direct measurement, not assumed.
        macro_detail_dict=macro,
    )
    human.name = "QUARK_Base"
    human.data.name = "QUARK_Base_Mesh"
    _apply_proportion_targets(human)
    return human


# Detailed (non-macro) MakeHuman targets, applied on top of the macro dict above. MPFB ships 1258
# of these; `TargetService.load_target(obj, full_path, weight=...)` takes a real file path, and
# `LocationService.get_mpfb_data("targets")` resolves the directory portably rather than hardcoding
# a user path.
#
# Why these exist: the reference is an IDEALISED figure -- measured off the concept sheet's own
# front view, its legs are ~51% of total height, against ~45% for the untargeted MakeHuman body.
# The obvious lever, the `proportions` macro ("uncommon"<->"idealistic"), turns out to do
# essentially NOTHING to leg length: swept across its full 0.5-1.0 range it moved the measured leg
# fraction by 0.0005 (0.4645 -> 0.4640). That is the same class of finding as this pipeline's
# earlier discovery that the `height` macro has no independent effect -- MakeHuman's macro sliders
# blend whole shape-key sets and several of them simply do not control what their name suggests.
# The detailed `*-scale-vert-*` targets DO work, measured:
#     leg=0.0            -> 159.67cm, leg_fraction 0.450
#     leg=1.0            -> 167.99cm, leg_fraction 0.480
#     leg=1.0 torso=1.0  -> 163.68cm, leg_fraction 0.490
# Chosen: full leg extension plus a partial torso shortening -- keeps overall height essentially on
# the sheet's stated 167cm while taking leg fraction from 0.450 to ~0.485, closing most of the gap
# to the reference's 0.51. The remainder is left rather than forced: pushing torso shortening to
# maximum buys 0.005 more fraction at the cost of 4cm of height, which is the wrong trade against
# an explicitly specified 167cm.
_LEG_TARGETS = (
    "legs/l-upperleg-scale-vert-incr", "legs/r-upperleg-scale-vert-incr",
    "legs/l-lowerleg-scale-vert-incr", "legs/r-lowerleg-scale-vert-incr",
)
_TORSO_TARGET = "torso/torso-scale-vert-decr"


def _apply_proportion_targets(human, leg_weight=1.0, torso_shorten=0.35):
    root = LocationService.get_mpfb_data("targets")
    def _load(fragment, weight):
        path = os.path.join(root, *fragment.split("/")) + ".target.gz"
        if not os.path.isfile(path):
            print(f"WARNING: proportion target missing, skipped: {path}")
            return
        TargetService.load_target(human, path, weight=weight,
                                  name=fragment.rsplit("/", 1)[-1])
    for frag in _LEG_TARGETS:
        _load(frag, leg_weight)
    if torso_shorten > 0.0:
        _load(_TORSO_TARGET, torso_shorten)
    return human


# ---------------------------------------------------------------------------------------------
# Measured landmarks (world Z meters) on the mesh `create_and_tune_human()` above produces, via
# this mesh's own rig-joint vertex groups (`joint-pelvis`, `joint-neck`, etc. -- weight > 0.4
# centroid). This mesh's rest pose is a relaxed A-pose (arms angled down), not the old blockout's
# T-pose -- confirmed by rendering and looking, not assumed from the shape-key names.
# ---------------------------------------------------------------------------------------------
# Only Z_NECK survives, and only because its single remaining consumer -- `_classify_material_index`
# -- runs over the BASE body mesh's own polygons, which is the space this was measured in. The
# other landmarks (ankle/knee/hip/spine/shoulder/scapula/mouth) were deleted rather than left
# lying around: they were consumed only by the armor classifier, which now measures what it needs
# from the evaluated mesh and the fitted rig at runtime (`measure_shell_landmarks`). Leaving stale
# base-space constants next to evaluated-space code is precisely how the two spaces got mixed.
Z_NECK = 1.408

# This mesh's front (face/nose) points toward -Y -- confirmed by rendering a camera at y=-1.1
# looking toward +Y and seeing the face lit and centered, not assumed from the old blockout's
# documented convention (which was the *opposite*: that mesh's face pointed toward +Y, so its own
# "front"-labeled camera view actually showed the back/spine-conduit side -- a quirk noted in
# PRODUCTION_LOG.md, NOT a convention to carry forward blindly onto a differently-oriented mesh).
# So here, back = +Y, front = -Y -- the reverse of the signs the old `_classify_material_index()`
# used for its spine-conduit/scapula/chest-seam rules. Confirmed by an early render that put the
# emissive/graphite "back" regions on the front-facing side before this was caught and fixed.


def make_material(
    name, base_color, metallic=0.0, roughness=0.5, emission=None, emission_strength=0.0,
    wear=True, wear_strength=0.35, noise_scale=22.0, noise_strength=0.12, bump_strength=0.04,
    panel_detail=False, panel_scale=9.0, rivet_scale=26.0, brushed=False,
    coat=0.0, coat_roughness=0.05,
):
    """Procedural PBR material -- verbatim from the prior blockout pipeline (mesh-agnostic, works
    on polygon material_index assignment regardless of the underlying mesh's topology).

    `coat` adds a Principled clear-coat layer -- the reference sheet's ceramic reads as a glazed,
    high-gloss shell with a sharp specular highlight, which a bare diffuse+roughness lobe cannot
    produce no matter how low its roughness goes. Used for the armor ceramic; left 0 elsewhere."""
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    nodes.clear()

    output = nodes.new('ShaderNodeOutputMaterial')
    output.location = (600, 0)
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    bsdf.location = (300, 0)
    links.new(bsdf.outputs['BSDF'], output.inputs['Surface'])
    bsdf.inputs['Metallic'].default_value = metallic
    bsdf.inputs['Roughness'].default_value = roughness
    if coat > 0.0 and "Coat Weight" in bsdf.inputs:
        bsdf.inputs["Coat Weight"].default_value = coat
        bsdf.inputs["Coat Roughness"].default_value = coat_roughness

    base_rgba = (*base_color, 1.0)
    if emission is not None and "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*emission, 1.0)
        bsdf.inputs["Emission Strength"].default_value = emission_strength

    if not wear:
        bsdf.inputs['Base Color'].default_value = base_rgba
        return mat

    color_node = nodes.new('ShaderNodeRGB')
    color_node.location = (-600, 200)
    color_node.outputs[0].default_value = base_rgba

    worn_color = tuple(min(1.0, c * 1.35 + 0.05) for c in base_color) + (1.0,)
    wear_color_node = nodes.new('ShaderNodeRGB')
    wear_color_node.location = (-600, 0)
    wear_color_node.outputs[0].default_value = worn_color

    geo = nodes.new('ShaderNodeNewGeometry')
    geo.location = (-900, -200)
    pointiness_ramp = nodes.new('ShaderNodeValToRGB')
    pointiness_ramp.location = (-600, -200)
    pointiness_ramp.color_ramp.elements[0].position = 0.45
    pointiness_ramp.color_ramp.elements[1].position = 0.62
    links.new(geo.outputs['Pointiness'], pointiness_ramp.inputs['Fac'])

    mix_wear = nodes.new('ShaderNodeMixRGB')
    mix_wear.location = (-300, 150)
    mix_wear.inputs['Fac'].default_value = wear_strength
    links.new(pointiness_ramp.outputs['Color'], mix_wear.inputs['Fac'])
    links.new(color_node.outputs[0], mix_wear.inputs['Color1'])
    links.new(wear_color_node.outputs[0], mix_wear.inputs['Color2'])

    noise = nodes.new('ShaderNodeTexNoise')
    noise.location = (-900, -450)
    noise.inputs['Scale'].default_value = noise_scale
    noise.inputs['Detail'].default_value = 3.0
    mottle_ramp = nodes.new('ShaderNodeValToRGB')
    mottle_ramp.location = (-600, -450)
    mottle_ramp.color_ramp.elements[0].color = (1.0 - noise_strength,) * 3 + (1.0,)
    mottle_ramp.color_ramp.elements[1].color = (1.0, 1.0, 1.0, 1.0)
    links.new(noise.outputs['Fac'], mottle_ramp.inputs['Fac'])

    color_so_far = mix_wear.outputs['Color']
    last_bump_input = noise.outputs['Fac']

    coord = None
    if panel_detail or brushed:
        coord = nodes.new('ShaderNodeTexCoord')
        coord.location = (-1200, -750)

    if panel_detail:
        panel_voronoi = nodes.new('ShaderNodeTexVoronoi')
        panel_voronoi.location = (-900, -700)
        panel_voronoi.feature = 'DISTANCE_TO_EDGE'
        panel_voronoi.distance = 'CHEBYCHEV'
        panel_voronoi.inputs['Scale'].default_value = panel_scale
        links.new(coord.outputs['Object'], panel_voronoi.inputs['Vector'])

        seam_ramp = nodes.new('ShaderNodeValToRGB')
        seam_ramp.location = (-600, -700)
        seam_ramp.color_ramp.elements[0].color = (0.0, 0.0, 0.0, 1.0)
        seam_ramp.color_ramp.elements[0].position = 0.02
        seam_ramp.color_ramp.elements[1].color = (1.0, 1.0, 1.0, 1.0)
        seam_ramp.color_ramp.elements[1].position = 0.06
        links.new(panel_voronoi.outputs['Distance'], seam_ramp.inputs['Fac'])

        mix_seam = nodes.new('ShaderNodeMixRGB')
        mix_seam.location = (-300, 400)
        mix_seam.blend_type = 'MULTIPLY'
        mix_seam.inputs['Fac'].default_value = 0.85
        links.new(color_so_far, mix_seam.inputs['Color1'])
        links.new(seam_ramp.outputs['Color'], mix_seam.inputs['Color2'])
        color_so_far = mix_seam.outputs['Color']

        rivet_voronoi = nodes.new('ShaderNodeTexVoronoi')
        rivet_voronoi.location = (-900, -950)
        rivet_voronoi.feature = 'F1'
        rivet_voronoi.inputs['Scale'].default_value = rivet_scale
        links.new(coord.outputs['Object'], rivet_voronoi.inputs['Vector'])

        rivet_ramp = nodes.new('ShaderNodeValToRGB')
        rivet_ramp.location = (-600, -950)
        rivet_ramp.color_ramp.elements[0].color = (1.0, 1.0, 1.0, 1.0)
        rivet_ramp.color_ramp.elements[0].position = 0.05
        rivet_ramp.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)
        rivet_ramp.color_ramp.elements[1].position = 0.09
        links.new(rivet_voronoi.outputs['Distance'], rivet_ramp.inputs['Fac'])

        rivet_color_node = nodes.new('ShaderNodeRGB')
        rivet_color_node.location = (-900, -1150)
        rivet_color_node.outputs[0].default_value = (0.85, 0.87, 0.9, 1.0)

        near_seam_ramp = nodes.new('ShaderNodeValToRGB')
        near_seam_ramp.location = (-600, -1150)
        near_seam_ramp.color_ramp.elements[0].color = (1.0, 1.0, 1.0, 1.0)
        near_seam_ramp.color_ramp.elements[0].position = 0.14
        near_seam_ramp.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)
        near_seam_ramp.color_ramp.elements[1].position = 0.30
        links.new(panel_voronoi.outputs['Distance'], near_seam_ramp.inputs['Fac'])

        rivet_gated = nodes.new('ShaderNodeMixRGB')
        rivet_gated.location = (-300, -1150)
        rivet_gated.blend_type = 'MULTIPLY'
        rivet_gated.inputs['Fac'].default_value = 1.0
        links.new(rivet_ramp.outputs['Color'], rivet_gated.inputs['Color1'])
        links.new(near_seam_ramp.outputs['Color'], rivet_gated.inputs['Color2'])

        mix_rivet_color = nodes.new('ShaderNodeMixRGB')
        mix_rivet_color.location = (-300, 650)
        links.new(rivet_gated.outputs['Color'], mix_rivet_color.inputs['Fac'])
        links.new(color_so_far, mix_rivet_color.inputs['Color1'])
        links.new(rivet_color_node.outputs[0], mix_rivet_color.inputs['Color2'])
        color_so_far = mix_rivet_color.outputs['Color']

        seam_bump = nodes.new('ShaderNodeBump')
        seam_bump.location = (0, -700)
        seam_bump.invert = True
        seam_bump.inputs['Strength'].default_value = 0.12
        links.new(seam_ramp.outputs['Color'], seam_bump.inputs['Height'])
        last_bump_input = ('seam', seam_bump, rivet_gated)

    mix_mottle = nodes.new('ShaderNodeMixRGB')
    mix_mottle.location = (0, 100)
    mix_mottle.blend_type = 'MULTIPLY'
    mix_mottle.inputs['Fac'].default_value = 1.0
    links.new(color_so_far, mix_mottle.inputs['Color1'])
    links.new(mottle_ramp.outputs['Color'], mix_mottle.inputs['Color2'])
    links.new(mix_mottle.outputs['Color'], bsdf.inputs['Base Color'])

    rough_ramp = nodes.new('ShaderNodeValToRGB')
    rough_ramp.location = (-300, -300)
    rough_ramp.color_ramp.elements[0].color = (roughness,) * 3 + (1.0,)
    rough_ramp.color_ramp.elements[1].color = (max(0.05, roughness - 0.3),) * 3 + (1.0,)
    links.new(geo.outputs['Pointiness'], rough_ramp.inputs['Fac'])
    roughness_out = rough_ramp.outputs['Color']

    if brushed:
        brush_mapping = nodes.new('ShaderNodeMapping')
        brush_mapping.location = (-900, -50)
        brush_mapping.inputs['Rotation'].default_value = (0, math.radians(90), 0)
        links.new(coord.outputs['Object'], brush_mapping.inputs['Vector'])

        brush_wave = nodes.new('ShaderNodeTexWave')
        brush_wave.location = (-600, -50)
        brush_wave.wave_type = 'BANDS'
        brush_wave.bands_direction = 'Z'
        brush_wave.inputs['Scale'].default_value = 140.0
        brush_wave.inputs['Distortion'].default_value = 0.0
        links.new(brush_mapping.outputs['Vector'], brush_wave.inputs['Vector'])

        brush_ramp = nodes.new('ShaderNodeValToRGB')
        brush_ramp.location = (-300, -50)
        brush_ramp.color_ramp.elements[0].color = (0.85, 0.85, 0.85, 1.0)
        brush_ramp.color_ramp.elements[1].color = (1.08, 1.08, 1.08, 1.0)
        links.new(brush_wave.outputs['Color'], brush_ramp.inputs['Fac'])

        mix_brush = nodes.new('ShaderNodeMixRGB')
        mix_brush.location = (0, -50)
        mix_brush.blend_type = 'MULTIPLY'
        mix_brush.inputs['Fac'].default_value = 1.0
        links.new(roughness_out, mix_brush.inputs['Color1'])
        links.new(brush_ramp.outputs['Color'], mix_brush.inputs['Color2'])
        roughness_out = mix_brush.outputs['Color']

    links.new(roughness_out, bsdf.inputs['Roughness'])

    bump = nodes.new('ShaderNodeBump')
    bump.location = (0, -450)
    bump.inputs['Strength'].default_value = bump_strength
    links.new(noise.outputs['Fac'], bump.inputs['Height'])

    if panel_detail:
        _, seam_bump_node, rivet_ramp_node = last_bump_input
        links.new(bump.outputs['Normal'], seam_bump_node.inputs['Normal'])
        rivet_bump = nodes.new('ShaderNodeBump')
        rivet_bump.location = (150, -700)
        rivet_bump.inputs['Strength'].default_value = 0.25
        links.new(rivet_ramp_node.outputs['Color'], rivet_bump.inputs['Height'])
        links.new(seam_bump_node.outputs['Normal'], rivet_bump.inputs['Normal'])
        links.new(rivet_bump.outputs['Normal'], bsdf.inputs['Normal'])
    else:
        links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])

    return mat


# ---------------------------------------------------------------------------------------------
# Limb segments are no longer hardcoded here. They used to be a literal table of bone positions
# copied out of one particular generated rig, which went stale the moment the Phase-B proportion
# targets moved the skeleton (measured: `foot.L` head z went 0.068 -> -0.015, `lowerleg01.L`
# 0.447 -> 0.410) -- and, worse, they were in EVALUATED space while the sibling `Z_*` landmark
# constants were in BASE space, so the two rule sets silently disagreed about where the body was.
# `measure_shell_landmarks()` now reads the real rig and the real evaluated mesh at runtime.
#
# Classification by "nearest bone segment + parametric position along it" is still the right idea
# and is retained: the mesh rests in an A-pose, so an arm's height and lateral offset vary TOGETHER
# along its length, and any pure-Z or pure-|x| threshold necessarily misclassifies part of it.
# Values below are side-agnostic (points are folded to |x| before testing).
# ---------------------------------------------------------------------------------------------

# Parametric plate coverage along each limb: (t_start, t_end). The EXCLUDED remainder at each end
# is what becomes a visible dark under-suit gap at the joint -- shoulder, elbow, wrist, hip, knee,
# ankle -- which is exactly how the reference sheet's segmented armor reads ("Armor segments move
# smoothly with the body" on its own rigging panel). The gaps are the design, not missing coverage.
LIMB_PLATE_SPAN = {
    'upperarm': (0.26, 0.88),
    'lowerarm': (0.14, 0.74),
    'upperleg': (0.18, 0.86),
    'lowerleg': (0.13, 0.84),
}

# A point closer than this to a limb segment is treated as belonging to that limb rather than the
# torso -- replaces the old `ax >` torso/arm disambiguation.
LIMB_RADIUS = 0.105


def _nearest_limb(co, segments):
    """Return (limb_name, t, distance) for the closest limb segment, folding x to |x| so one set of
    .L-side bone positions serves both sides. t is the clamped parametric position along the bone."""
    p = mathutils.Vector((abs(co.x), co.y, co.z))
    best = (None, 0.0, 1e9)
    for name, (h, t_) in segments.items():
        a = mathutils.Vector(h)
        b = mathutils.Vector(t_)
        ab = b - a
        denom = ab.dot(ab)
        t = 0.0 if denom == 0 else max(0.0, min(1.0, (p - a).dot(ab) / denom))
        d = (p - (a + ab * t)).length
        if d < best[2]:
            best = (name, t, d)
    return best


def _is_armor_plate(co, lm):
    """True where a hard ceramic armor PLATE sits. Drives real shell geometry
    (`build_armor_shell`), not a flat colour patch on bare skin.

    All landmarks come from `lm` (see `measure_shell_landmarks`) -- measured on the evaluated mesh
    and the fitted rig, in one consistent space."""
    x, z = co.x, co.z
    segs = lm["segments"]

    if z > lm["neck_z"] - 0.035:    # head + neck collar: never plated (face is exposed skin)
        return False

    # Boot. Must be tested BEFORE the limb rule: the foot lies past the lowerleg segment's t=1.0
    # endpoint, so it is still within LIMB_RADIUS of that segment and would otherwise fail the
    # lowerleg span test and come out as a bare dark foot -- which is exactly what the first
    # armored render showed. The reference gives QUARK armoured boots.
    if z < lm["boot_z"]:
        return True

    if segs:
        limb, t, d = _nearest_limb(co, segs)
        if d < LIMB_RADIUS:
            lo, hi = LIMB_PLATE_SPAN[limb]
            # Hands sit past the end of the lowerarm segment; the plate span already excludes them,
            # leaving them as dark under-suit gloves rather than bare skin.
            return lo < t < hi

    # Torso -- bands with deliberate gaps at the under-bust seam and the waist, matching the
    # reference front view's chest / abdomen / pelvis plate stack. Positions are fractions of the
    # measured crotch->neck span, so they track the body instead of assuming absolute heights.
    #
    # The |x| gate is load-bearing, not decorative: the hands sit far out on x but are FARTHER than
    # LIMB_RADIUS from the lowerarm segment (they hang off its t=1.0 end), so they fall through the
    # limb branch entirely. Without this gate their height lands inside the abdomen band and they
    # get plated as though they were torso -- which is exactly what one render showed, ceramic
    # hands instead of dark under-suit gloves. The torso never exceeds ~0.18 half-width.
    if abs(x) > 0.20:
        return False
    f = (z - lm["crotch"]) / lm["span"]
    for key in ("pelvis", "abdomen", "chest"):
        lo, hi = lm[key]
        if lo < f < hi:
            return True
    return False


def _classify_material_index(co, idx):
    """Material for the BODY mesh, which is now the full-coverage dark under-suit the armor plates
    sit on top of -- not the armor itself. Everything below the neck is under-suit; only the head
    is exposed skin.

    This replaces a rule set that painted 'ceramic' directly onto the bare body as its default,
    which is why the render read as a nude figure with grey blotches rather than an armored one:
    the plates had no geometry, and the un-plated remainder was literal bare skin. Confirmed by a
    scaled side-by-side against `reference/QUARK_sideview_color.png` -- see PRODUCTION_LOG."""
    z = co.z
    if z > Z_NECK:              # head / face -- the exposed skin the MPFB swap exists for
        return idx['skin']
    return idx['undersuit']


def assign_materials(obj, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0):
    ceramic = make_material(
        # #E6E1DD -- the reference sheet's own stated "Primary (Ceramic)" hex. The blue channel was
        # 0.816 (i.e. #E6E1D0), which is measurably more yellow than the sheet specifies and read
        # as a green-grey cast against the neutral lighting rather than the sheet's warm ivory.
        "QUARK_Ceramic", (0.902, 0.882, 0.867), roughness=0.22,
        wear=True, wear_strength=0.06, noise_scale=60.0, noise_strength=0.015, bump_strength=0.004,
        panel_detail=False,
        coat=0.6, coat_roughness=0.04,
    )  # roughness 0.45 -> 0.22 + a clear coat: the reference's ceramic is a glazed, high-gloss
    # shell with a tight specular highlight, which the old near-matte setting could not produce.
    # `panel_detail` is now OFF and wear/noise are near-zero: that procedural voronoi panel-and-
    # rivet pattern existed to FAKE plate seams back when the armor was flat paint on skin. With
    # real shell geometry (build_armor_shell) the seams are actual edges, and the fake pattern only
    # showed up as the dark diagonal smears/cracks across the plates in the first armored render --
    # competing with the real panel lines rather than reinforcing them.
    # Prefer the real MakeHuman skin if `add_makehuman_assets()` already applied one. That call
    # runs first and puts a proper subsurface-scattering skin (with actual texture maps) into the
    # body's material slots; rebuilding the slot list here would silently discard it and put the
    # flat procedural tone back on the face. Falling back to the procedural material keeps the
    # script runnable if the CC0 asset pack was never installed.
    existing = [m for m in obj.data.materials if m is not None]
    if existing:
        synth_skin = existing[0]
        print(f"  using MakeHuman skin material for face: {synth_skin.name}")
    else:
        synth_skin = make_material(
            "QUARK_SynthSkin", (0.867, 0.757, 0.686), roughness=0.5,
            wear_strength=0.05, noise_scale=40.0, noise_strength=0.03, bump_strength=0.01,
        )
        print("  WARNING: no MakeHuman skin found -- falling back to procedural skin tone")
    obj.data.materials.clear()
    # The under-suit: a dark, slightly sheened bodysuit covering EVERYTHING below the neck. This is
    # what the armor plates sit on, and what stops the un-plated remainder from reading as a nude
    # body (the defect the reference comparison exposed). Distinct from `graphite` armor trim.
    undersuit = make_material(
        "QUARK_UnderSuit", (0.098, 0.106, 0.125), roughness=0.55,
        wear_strength=0.12, noise_scale=48.0, noise_strength=0.05, bump_strength=0.02,
    )
    graphite = make_material(
        "QUARK_Graphite", (0.169, 0.176, 0.192), roughness=0.7,
        wear_strength=0.4, noise_scale=25.0, noise_strength=0.15,
    )
    metal_alloy = make_material(
        "QUARK_MetalAlloy", (0.784, 0.8, 0.82), metallic=1.0, roughness=0.3,
        wear_strength=0.55, noise_scale=12.0, noise_strength=0.1, bump_strength=0.02,
        panel_detail=True, panel_scale=6.0, rivet_scale=30.0, brushed=True,
    )
    emissive = make_material(
        "QUARK_Emissive", glow_color, emission=glow_color, emission_strength=glow_strength, wear=False,
    )

    idx = {}
    for key, mat in (
        ('ceramic', ceramic), ('skin', synth_skin), ('undersuit', undersuit),
        ('graphite', graphite), ('metal', metal_alloy), ('emissive', emissive),
    ):
        idx[key] = len(obj.data.materials)
        obj.data.materials.append(mat)

    for poly in obj.data.polygons:
        poly.material_index = _classify_material_index(poly.center, idx)

    return emissive


# ---------------------------------------------------------------------------------------------
# Eyes -- MPFB ships eye SOCKETS (a closed indentation in the head mesh) but no eyeball geometry
# and no bundled eye assets at all (confirmed: `data/3dobjs` has no eye files, and MPFB's own
# `helper-l-eye`/`joint-l-eye` vertex-group centroids do NOT match the visible socket opening --
# tried first, placed the spheres on the forehead, confirmed wrong by rendering and looking). The
# coordinates below are ground-truth: found by raycasting from the actual face-closeup render
# camera through the visible socket pixels in a real render and recording where the ray hits the
# mesh surface, not by trusting any named landmark.
# ---------------------------------------------------------------------------------------------
MH_ASSETS = (
    ("Eyes",      "eyes/high-poly/high-poly"),
    ("Eyebrows",  "eyebrows/eyebrow008/eyebrow008"),
    ("Eyelashes", "eyelashes/eyelashes01/eyelashes01"),
    ("Hair",      "hair/ponytail01/ponytail01"),
)
MH_SKIN = "skins/young_caucasian_female2/young_caucasian_female2"


def add_makehuman_assets(human):
    """Fit real MakeHuman assets (eyes, eyebrows, eyelashes, hair) and a subsurface skin.

    Replaces this pipeline's hand-built primitives: eyeballs were UV spheres positioned by
    raycasting through a rendered image, and hair was a scalp cap plus a bun and two locks made of
    scaled primitives. Those existed because an earlier pass concluded MPFB "ships no default
    eyeball geometry, skin textures, or hair assets (confirmed by searching its installed files)".
    That observation was correct but the inference was wrong: MPFB's asset directory was simply
    EMPTY because the packs had never been downloaded. `makehuman_system_assets` (CC0) provides all
    four, properly fitted to the body by MakeHuman's own system, plus 23 skins.

    Hair is `ponytail01` by Director's choice. Worth recording honestly: the CC0 set contains no
    braided updo -- `braid01` is named for a braid texture but renders as a swept bob. ponytail01
    was chosen not as a braid match but because it pulls the hair back off the face, which is the
    reference's silhouette AND leaves the forehead clear for the emissive circlet.

    Skin uses `skin_type='ENHANCED_SSS'` -- real subsurface scattering, versus the flat procedural
    tone it replaces. `young_caucasian_female2` matches the Director-specified pure-Caucasian
    phenotype."""
    root = LocationService.get_user_data()
    created = []
    for asset_type, fragment in MH_ASSETS:
        path = os.path.join(root, *fragment.split("/")) + ".mhclo"
        if not os.path.isfile(path):
            print(f"WARNING: MakeHuman asset missing, skipped: {path}")
            continue
        try:
            obj = HumanService.add_mhclo_asset(path, human, asset_type=asset_type)
            created.append(obj)
            print(f"  asset {asset_type}: {obj.name if obj else None}")
        except Exception as exc:                                  # noqa: BLE001
            print(f"WARNING: failed to fit {asset_type}: {exc}")

    skin_path = os.path.join(root, *MH_SKIN.split("/")) + ".mhmat"
    if os.path.isfile(skin_path):
        try:
            HumanService.set_character_skin(skin_path, human, skin_type='ENHANCED_SSS')
            print("  skin applied (ENHANCED_SSS)")
        except Exception as exc:                                  # noqa: BLE001
            print(f"WARNING: failed to apply skin: {exc}")
    else:
        print(f"WARNING: skin missing, skipped: {skin_path}")
    return created


def add_eyes(human):
    eye_radius = 0.012
    eye_mat_sclera = bpy.data.materials.new("QUARK_EyeSclera")
    eye_mat_sclera.use_nodes = True
    b = eye_mat_sclera.node_tree.nodes["Principled BSDF"]
    b.inputs['Base Color'].default_value = (0.92, 0.90, 0.87, 1.0)
    b.inputs['Roughness'].default_value = 0.15

    eye_mat_iris = bpy.data.materials.new("QUARK_EyeIris")
    eye_mat_iris.use_nodes = True
    b = eye_mat_iris.node_tree.nodes["Principled BSDF"]
    b.inputs['Base Color'].default_value = (0.30, 0.45, 0.55, 1.0)
    b.inputs['Roughness'].default_value = 0.05

    eye_mat_pupil = bpy.data.materials.new("QUARK_EyePupil")
    eye_mat_pupil.use_nodes = True
    b = eye_mat_pupil.node_tree.nodes["Principled BSDF"]
    b.inputs['Base Color'].default_value = (0.01, 0.01, 0.01, 1.0)
    b.inputs['Roughness'].default_value = 0.05

    eyes = []
    for name, loc in (("Eye_L", (0.035, -0.10, 1.492)), ("Eye_R", (-0.035, -0.10, 1.492))):
        bpy.ops.mesh.primitive_uv_sphere_add(radius=eye_radius, location=loc, segments=24, ring_count=16)
        eye = bpy.context.active_object
        eye.name = name
        eye.data.materials.append(eye_mat_sclera)
        eye.data.materials.append(eye_mat_iris)
        eye.data.materials.append(eye_mat_pupil)
        center = eye.location
        for poly in eye.data.polygons:
            world_center = eye.matrix_world @ poly.center
            d = (world_center - center).normalized()
            forward_dot = -d.y  # -Y is this mesh's forward/front direction
            if forward_dot > 0.97:
                poly.material_index = 2
            elif forward_dot > 0.85:
                poly.material_index = 1
            else:
                poly.material_index = 0
        bpy.ops.object.shade_smooth()
        eye.parent = human
        eyes.append(eye)
    return eyes


# ---------------------------------------------------------------------------------------------
# Hair -- a stylized volume/shape match (cap + gathered bun + two framing locks), not
# strand-by-strand grooming. MPFB owns hair via Blender's native Hair Curves system, but that
# workflow is MPFB's own interactive brush-styling panel, not something scriptable headlessly to a
# specific target shape -- so this uses simple mesh primitives instead, the same technique this
# pipeline already uses for other stylized elements (panel/rivet detail, the emissive headband).
# Sized/positioned empirically against the measured head dome (x in (-0.091,0.091), z in
# (1.52,1.668)) and confirmed by rendering -- an early attempt used a bisect-plane cut to trim a
# full sphere into a dome and got the inner/outer clear side backwards (kept the huge lower portion
# instead of the small cap), caught by a render that showed a giant blob swallowing the whole face;
# fixed by sizing/placing small pieces to sit above the hairline directly, no bisect needed.
# ---------------------------------------------------------------------------------------------
def add_hair(human):
    hair_mat = bpy.data.materials.new("QUARK_Hair")
    hair_mat.use_nodes = True
    bsdf = hair_mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs['Base Color'].default_value = (0.404, 0.310, 0.220, 1.0)
    bsdf.inputs['Roughness'].default_value = 0.42
    nt = hair_mat.node_tree
    noise = nt.nodes.new('ShaderNodeTexNoise')
    noise.inputs['Scale'].default_value = 60.0
    noise.inputs['Detail'].default_value = 4.0
    ramp = nt.nodes.new('ShaderNodeValToRGB')
    ramp.color_ramp.elements[0].color = (0.30, 0.22, 0.15, 1.0)
    ramp.color_ramp.elements[1].color = (0.55, 0.42, 0.30, 1.0)
    nt.links.new(noise.outputs['Fac'], ramp.inputs['Fac'])
    bump = nt.nodes.new('ShaderNodeBump')
    bump.inputs['Strength'].default_value = 0.15
    nt.links.new(noise.outputs['Fac'], bump.inputs['Height'])
    nt.links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    nt.links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])

    def add_ellipsoid(name, loc, radii):
        bpy.ops.mesh.primitive_uv_sphere_add(radius=1.0, location=loc, segments=28, ring_count=18)
        obj = bpy.context.active_object
        obj.name = name
        obj.scale = radii
        obj.data.materials.append(hair_mat)
        bpy.ops.object.shade_smooth()
        obj.parent = human
        return obj

    pieces = []
    pieces.append(add_ellipsoid("Hair_Cap", (0.0, -0.01, 1.595), (0.092, 0.105, 0.062)))
    # back = +Y for this mesh (see orientation note above)
    pieces.append(add_ellipsoid("Hair_Bun", (0.0, 0.07, 1.60), (0.050, 0.050, 0.042)))
    pieces.append(add_ellipsoid("Hair_LockL", (0.082, -0.05, 1.53), (0.016, 0.028, 0.055)))
    pieces.append(add_ellipsoid("Hair_LockR", (-0.082, -0.05, 1.53), (0.016, 0.028, 0.055)))
    return pieces


def uv_unwrap_if_needed(obj):
    if obj.data.uv_layers:
        return
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=0.02)
    bpy.ops.object.mode_set(mode='OBJECT')


def bake_textures(obj, out_dir, resolution=2048):
    """Verbatim from the prior blockout pipeline -- bakes whatever UV-mapped mesh it's given, no
    coupling to mesh topology."""
    scene = bpy.context.scene
    original_engine = scene.render.engine
    scene.render.engine = 'CYCLES'
    scene.cycles.samples = 64
    scene.cycles.use_denoising = False

    subsurf = obj.modifiers.get("Subdivision")
    subsurf_was_visible = subsurf.show_render if subsurf else None
    if subsurf:
        subsurf.show_render = False

    images = {}
    for pass_name in ('base_color', 'roughness', 'emission', 'normal', 'ao'):
        is_normal = pass_name == 'normal'
        images[pass_name] = bpy.data.images.new(
            f"QUARK_{pass_name}", width=resolution, height=resolution, alpha=False,
            is_data=is_normal,
        )

    def set_active_bake_target(img):
        for mat in obj.data.materials:
            nt = mat.node_tree
            node = nt.nodes.get("BakeTarget")
            if node is None:
                node = nt.nodes.new('ShaderNodeTexImage')
                node.name = "BakeTarget"
                node.location = (-1200, 400)
            node.image = img
            for n in nt.nodes:
                n.select = False
            node.select = True
            nt.nodes.active = node

    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)

    set_active_bake_target(images['base_color'])
    bpy.ops.object.bake(type='DIFFUSE', pass_filter={'COLOR'}, margin=8)

    set_active_bake_target(images['roughness'])
    bpy.ops.object.bake(type='ROUGHNESS', margin=8)

    set_active_bake_target(images['emission'])
    bpy.ops.object.bake(type='EMIT', margin=8)

    set_active_bake_target(images['normal'])
    bpy.ops.object.bake(type='NORMAL', normal_space='TANGENT', margin=8)

    set_active_bake_target(images['ao'])
    bpy.ops.object.bake(type='AO', margin=8)

    os.makedirs(out_dir, exist_ok=True)
    for pass_name, img in images.items():
        img.filepath_raw = os.path.join(out_dir, f"quark_{pass_name}.png")
        img.file_format = 'PNG'
        img.save()

    if subsurf:
        subsurf.show_render = subsurf_was_visible
    scene.render.engine = original_engine


def add_rig_and_weights(human):
    """MPFB's own 'default' standard rig + auto-weighting, replacing the old hand-built armature
    (which assumed the old blockout's exact bone/proportions and can't transfer). Confirmed bone
    names by inspecting the actually-created armature, not the JSON rig template alone: the arm
    bones `03_posture_library.py`'s posing needs are `upperarm01.L/R` and `lowerarm01.L/R`."""
    bpy.ops.object.select_all(action='DESELECT')
    bpy.context.view_layer.objects.active = human
    human.select_set(True)
    HumanService.add_builtin_rig(human, "default", import_weights=True)
    for obj in bpy.data.objects:
        if obj.type == 'ARMATURE':
            obj.name = "QUARK_Rig"
            return obj
    return None


def measure_shell_landmarks(human, arm_obj, eval_mesh):
    """Derive every plate-classification landmark from the ACTUAL evaluated mesh and rig, at
    runtime. Nothing here is hardcoded.

    This replaces two sets of module-level constants that were quietly in DIFFERENT COORDINATE
    SPACES, which is a bug that survived several passes only because the two spaces happened to be
    close:
      * `Z_ANKLE`/`Z_KNEE`/.../`Z_NECK` were centroids of `joint-*` vertex groups measured on the
        BASE mesh (shape keys not applied).
      * `LIMB_SEGMENTS` were bone positions read off the rig -- and MPFB fits the rig to the
        EVALUATED body, so those were always in evaluated space.
    Base and evaluated differ by a ~8.5cm downward shift, so the two rule sets disagreed about
    where the body was. Worse, the Phase-B proportion targets stretch the legs and shorten the
    torso NON-uniformly, so no single remap between the spaces can be correct -- an attempt at a
    linear z-remap put the plate bands on visibly wrong anatomy (a crop-top cuirass and blocky
    cut-outs across the hips). Measuring in one space, from the geometry that actually renders,
    removes the whole class of problem and makes the classifier survive future proportion changes
    without any constant being re-derived by hand.

    Torso band positions are expressed as fractions of the crotch->neck span rather than absolute
    heights. The fractions are exactly those the previously-tuned absolute values worked out to,
    so this is a change of reference frame, not a re-tune."""
    zs = [v.co.z for v in eval_mesh.vertices]
    zmin, zmax = min(zs), max(zs)
    height = zmax - zmin

    # Crotch: scanning upward, the first z-slice whose vertices form a single x-run (legs fused).
    crotch = None
    for i in range(60, 141):
        z = zmin + (i / 200.0) * height
        xs = sorted(v.co.x for v in eval_mesh.vertices if abs(v.co.z - z) < height * 0.004)
        if len(xs) < 8:
            continue
        runs = 1
        for a, b in zip(xs, xs[1:]):
            if b - a > 0.035:
                runs += 1
        if runs == 1:
            crotch = z
            break
    if crotch is None:
        crotch = zmin + 0.47 * height
        print("WARNING: crotch detection failed; falling back to 0.47 of height")

    def _bone(name):
        b = arm_obj.data.bones.get(name) if arm_obj else None
        if b is None:
            return None
        return (arm_obj.matrix_world @ b.head_local, arm_obj.matrix_world @ b.tail_local)

    segs = {}
    for key, first, last in (("upperarm", "upperarm01.L", "upperarm02.L"),
                             ("lowerarm", "lowerarm01.L", "lowerarm02.L"),
                             ("upperleg", "upperleg01.L", "upperleg02.L"),
                             ("lowerleg", "lowerleg01.L", "lowerleg02.L")):
        a, b = _bone(first), _bone(last)
        if a and b:
            segs[key] = (a[0], b[1])          # start of the first bone -> end of the second
    neck_b = _bone("neck01")
    neck_z = neck_b[0].z if neck_b else (zmin + 0.82 * height)
    span = (neck_z - crotch) or 1.0

    lm = {
        "zmin": zmin, "height": height, "crotch": crotch, "neck_z": neck_z,
        "segments": segs,
        # (lo, hi) as fractions of the crotch->neck span
        "pelvis": (0.041, 0.246),
        "abdomen": (0.285, 0.490),
        "chest": (0.538, 0.845),
        "boot_z": zmin + 0.088,
        "chest_lo_z": crotch + 0.538 * span,
        "span": span,
    }
    print(f"Landmarks: height={height*100:.1f}cm crotch={crotch:.4f} neck={neck_z:.4f} "
          f"segments={sorted(segs)}")
    return lm


def build_armor_shell(human, arm_obj):
    """Build QUARK's ceramic armor as REAL, separate shell geometry sitting over the body.

    Why this exists: every pass before this one expressed the armor purely as `poly.material_index`
    on the bare body mesh -- flat colour patches painted onto naked skin. A scaled side-by-side
    against `reference/QUARK_sideview_color.png` (see PRODUCTION_LOG's comparison entry) made the
    consequence unmissable: no plate thickness, no panel edges, no silhouette break, and -- because
    the un-plated default was literal bare skin -- a figure that read as nude with grey smudges on
    it rather than as an armoured synthetic. That is a GEOMETRY gap, not a texture or lighting one;
    no amount of Tier 1-3 render tuning could have closed it, which is exactly why those tiers
    improved the image without moving it toward the reference.

    Technique: duplicate the (already rigged and weighted) body mesh, delete every face that isn't
    a plate region, push the survivors out along their own normals so the shell floats just proud
    of the under-suit, then Solidify for real plate thickness and Bevel the resulting borders so
    each panel catches a specular edge highlight. Built AFTER rigging deliberately -- the duplicate
    inherits MPFB's vertex groups for free, so the plates deform with the body instead of needing a
    second weighting pass; it only needs its own Armature modifier pointing at the same rig."""
    # Build from the EVALUATED body, not `human.data.copy()`. This fixes a real architectural bug,
    # not a cosmetic one:
    #
    # MakeHuman's macros and the proportion targets are all SHAPE KEYS, and a Blender shape key
    # stores ABSOLUTE vertex positions, not deltas from the current base. A copied mesh therefore
    # carried all 16 shape keys (verified: `QUARK_Armor` had 16 non-zero key blocks), so every
    # bmesh edit below -- the smoothing, the standoff offset, the clearance clamp -- was written to
    # base coordinates and then simply OVERRIDDEN at evaluation time by the shape keys' own stored
    # positions. Measured proof: the shell's base z-range was (-0.023, 1.314) while its evaluated
    # range was (-0.118, 1.264). The visible symptom was nipple detail reappearing through the
    # cuirass after the proportion retune despite the dedicated smoothing pass that had previously
    # removed it -- that smoothing had never actually reached the render.
    #
    # `new_from_object` on the evaluated object bakes shape keys AND modifiers down into plain
    # geometry, so the shell has no shape keys and the bmesh edits below are what renders. It also
    # applies MPFB's own "Hide helpers" MASK for free, removing the ~30% of the mesh that is
    # MakeHuman fitting-helper/joint-cube geometry -- the thing that previously leaked a stray
    # ceramic `joint-ground` cube into the shell at the world origin. The explicit `body`-group
    # filter that used to do that job is gone, because the mask now does it upstream.
    dg = bpy.context.evaluated_depsgraph_get()
    mesh = bpy.data.meshes.new_from_object(human.evaluated_get(dg), depsgraph=dg)
    mesh.name = "QUARK_Armor_Mesh"
    shell = bpy.data.objects.new("QUARK_Armor", mesh)
    bpy.context.collection.objects.link(shell)
    shell.matrix_world = human.matrix_world.copy()

    # Classification constants (Z_* and LIMB_SEGMENTS) were measured on the BASE mesh, but the
    # geometry above is now in EVALUATED space, and the two differ by a near-uniform ~8.5cm
    # downward shift (base z-range 0.002..1.668 vs evaluated -0.083..1.582 -- measured, and the
    # offset matches at both ends, so it really is close to a translation). Rather than
    # re-deriving every constant, map each evaluated coordinate back into base space purely for
    # the classification tests; the geometry itself stays in evaluated space.
    lm = measure_shell_landmarks(human, arm_obj, mesh)

    bm = bmesh.new()
    bm.from_mesh(mesh)
    bm.faces.ensure_lookup_table()

    # Relax the surface BEFORE cutting the plates out. An offset copy of a human mesh is still a
    # human mesh: at a 10mm standoff the shell faithfully reproduced nipples, individual toes and
    # knuckles, so the "armour" read as a shrink-wrapped nude rather than moulded plate -- armour
    # is a hard-surface object with its own smooth forms. Laplacian smoothing flattens that fine
    # anatomical detail into plate-like forms; doing it while the surface is still closed keeps it
    # well-behaved, and cutting the plates out afterwards keeps their borders crisp instead of
    # letting the smoothing curl them inward.
    body_bvh = BVHTree.FromBMesh(bm.copy())   # the TRUE body surface, captured pre-smoothing
    for _ in range(12):
        bmesh.ops.smooth_vert(
            bm, verts=bm.verts[:], factor=0.5,
            use_axis_x=True, use_axis_y=True, use_axis_z=True,
        )
    bm.normal_update()

    # Extra, REGION-TARGETED smoothing over the cuirass. Laplacian smoothing erases features by
    # size, and the global pass above is tuned not to destroy limb definition -- but that leaves it
    # far too gentle to remove nipples, which stayed clearly visible through the chest plate. A
    # bigger standoff does NOT fix this: offsetting along normals TRANSLATES a feature outward, it
    # does not erase it (the toes only disappeared because a 30mm offset is large enough for
    # neighbouring toe surfaces to merge into one another -- morphological dilation, not
    # smoothing). Removing a small feature requires actually smoothing it away, so the bust gets
    # its own concentrated pass. The breast form itself is large and survives this, which is
    # correct -- the reference's cuirass is shaped to the bust; it is only the nipple-scale detail
    # that must not read through moulded ceramic.
    chest_verts = [v for v in bm.verts if v.co.z > lm["chest_lo_z"] and abs(v.co.x) < 0.20]
    for _ in range(48):
        bmesh.ops.smooth_vert(
            bm, verts=chest_verts, factor=0.5,
            use_axis_x=True, use_axis_y=True, use_axis_z=True,
        )
    bm.normal_update()

    # Offset the SMOOTHED surface outward along its own normals. The ordering here matters and was
    # got wrong twice before landing:
    #   1st attempt: smooth hard, then a flat +6mm. Laplacian smoothing loses volume (it always
    #      pulls toward the local average), so the shell sank inside the body across every convex
    #      region and rendered as torn patchy islands on the thighs and shins.
    #   2nd attempt: clamp every vertex to >= 6mm outside the ORIGINAL body via a BVH lookup. That
    #      fixed the sinking but re-imprinted every protrusion the smoothing had just removed --
    #      the clamp was fighting the smoothing, so nipples and individual toes came straight back
    #      through the "armour". Confirmed by rendering, not reasoned about.
    # What actually works: offset the smoothed surface by a standoff LARGER than the local feature
    # protrusion, so the plate floats clear of the detail instead of re-acquiring it. The standoff
    # is therefore region-dependent -- a foot's toes stick out far further from a smoothed foot
    # than a nipple does from a smoothed chest, and a limb needs almost none.
    def _standoff(co):
        if co.z < lm["boot_z"]:
            return 0.030        # boot: must clear the toes entirely
        if co.z > lm["chest_lo_z"] and abs(co.x) < 0.20:
            return 0.022        # cuirass: must clear the bust
        return 0.007            # limbs: close-fitting
    for v in bm.verts:
        v.co = v.co + v.normal * _standoff(v.co)
    bm.normal_update()

    # Safety net only: nothing may end up INSIDE the real body. With the standoffs above this
    # should essentially never fire, but a 2mm floor guarantees no skin pokes through a plate on
    # any future proportion change rather than trusting the constants to stay valid.
    MIN_CLEAR = 0.002
    for v in bm.verts:
        hit = body_bvh.find_nearest(v.co)
        if hit[0] is None:
            continue
        loc, nor = hit[0], hit[1]
        if (v.co - loc).dot(nor) < MIN_CLEAR:
            v.co = loc + nor * MIN_CLEAR
    bm.normal_update()

    kill = [f for f in bm.faces if not _is_armor_plate(f.calc_center_median(), lm)]
    if len(kill) >= len(bm.faces):
        bm.free()
        bpy.data.objects.remove(shell)
        print("WARNING: armor shell classified zero plate faces -- shell not built")
        return None
    bmesh.ops.delete(bm, geom=kill, context='FACES')
    bm.normal_update()
    # Float the shell proud of the body. Done per-vertex along the vertex normal (not a Shrinkwrap
    # offset) so it follows the body's own curvature exactly and cannot self-intersect the skin.
    # 4mm float + 6mm thickness = 10mm total proud of the body. The first attempt used 7mm+9mm and
    # read as loose, baggy over-armour (the thigh plates especially looked like trousers rather
    # than a fitted shell) -- the reference's armour is skin-tight, so the total standoff has to
    # stay near the thickness of the plate itself.
    bm.to_mesh(mesh)
    bm.free()

    # Plate thickness + a chamfered border. `offset=1.0` grows outward only, keeping the shell's
    # inner face flush against the body rather than sinking half its thickness into it.
    solid = shell.modifiers.new("Solidify", 'SOLIDIFY')
    solid.thickness = 0.006
    solid.offset = 1.0
    bevel = shell.modifiers.new("Bevel", 'BEVEL')
    bevel.width = 0.0022
    bevel.segments = 2
    bevel.limit_method = 'ANGLE'
    bevel.angle_limit = math.radians(35)

    # Ceramic only -- the shell carries no other material, so clear the inherited slot list and
    # re-point every face at the ceramic index.
    mesh.materials.clear()
    ceramic = bpy.data.materials.get("QUARK_Ceramic")
    mesh.materials.append(ceramic)
    for poly in mesh.polygons:
        poly.material_index = 0

    if arm_obj is not None:
        shell.parent = arm_obj
        mod = shell.modifiers.new("Armature", 'ARMATURE')
        mod.object = arm_obj

    bpy.context.view_layer.objects.active = shell
    bpy.ops.object.shade_smooth()
    print(f"Armor shell built: {len(mesh.polygons)} plate faces")
    return shell


def _measure_head_ring(human, drop_from_crown=0.058):
    """Measure the EVALUATED head's cross-section at brow height.

    Returns (brow_z, y_centre, x_half, y_half). Uses the evaluated (shape-key-applied) mesh because
    that is what actually renders -- see the caller's comment on why base-mesh coordinates are the
    wrong space for world-positioned accent geometry."""
    bpy.context.view_layer.update()
    dg = bpy.context.evaluated_depsgraph_get()
    ev = human.evaluated_get(dg)
    me = ev.to_mesh()
    mw = human.matrix_world
    co = [mw @ v.co for v in me.vertices]
    ev.to_mesh_clear()
    crown = max(c.z for c in co)
    brow_z = crown - drop_from_crown
    band = [c for c in co if abs(c.z - brow_z) < 0.008]
    if not band:                       # never trust a slice to be populated
        band = [c for c in co if abs(c.z - brow_z) < 0.02]
    xs = [c.x for c in band]
    ys = [c.y for c in band]
    x_half = max(abs(min(xs)), abs(max(xs)))
    y_centre = (min(ys) + max(ys)) / 2.0
    y_half = (max(ys) - min(ys)) / 2.0
    return brow_z, y_centre, x_half, y_half


def add_accent_geometry(human, arm_obj, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0):
    """The emissive accent (headband circuit + spine conduit) as real geometry rather than
    material_index assignment on body polygons.

    The old approach coloured whichever body polygons fell inside a Z band, which on a 19k-vertex
    organic mesh produced a thick, ragged, zigzag slab across the forehead -- visible in every
    posture render and unmistakable next to the reference's fine 1-2px circuit line. A polygon
    classifier fundamentally cannot draw a thin clean line on topology that wasn't built for one;
    a small dedicated primitive can, and matches how eyes and hair are already handled here."""
    accent = bpy.data.materials.get("QUARK_Emissive")
    created = []

    # Headband: a thin circlet at the brow, sized and placed from a LIVE measurement of the head
    # rather than baked-in numbers.
    #
    # Two lessons are encoded here. First, the head is an ELLIPSE in cross-section, not a circle --
    # an early attempt used a single 0.083 circular radius and rendered as literally nothing,
    # because the head is far deeper (front-to-back) than it is wide, so the ring sat entirely
    # buried inside the skull. Second, and the reason this is now measured at runtime: the circlet
    # is separate geometry positioned in WORLD space, while the body's shape comes from shape keys
    # (macros + the proportion targets). Base-mesh coordinates and evaluated/rendered coordinates
    # therefore disagree -- the crown sits at z=1.668 on the base mesh but z=1.582 once evaluated,
    # an 8.6cm difference. A hardcoded height measured in the wrong space only *happened* to land
    # on the brow before, and any proportion change silently moves it. Measuring the evaluated mesh
    # here makes the placement survive future retuning instead of needing a manual re-derivation.
    brow_z, y_centre, x_half, y_half = _measure_head_ring(human)
    band_r = 0.003                      # sit ~3mm proud of the skin
    bpy.ops.mesh.primitive_torus_add(
        major_radius=1.0, minor_radius=0.0042, major_segments=72, minor_segments=8,
        location=(0.0, y_centre, brow_z),
    )
    band = bpy.context.active_object
    band.name = "QUARK_Headband"
    band.scale = (x_half + band_r, y_half + band_r, 1.0)
    created.append(band)
    print(f"Headband: z={brow_z:.4f} y={y_centre:.4f} rx={x_half + band_r:.4f} ry={y_half + band_r:.4f}")

    # Spine conduit: a slim raised strip down the back (+Y is this mesh's back -- see the
    # orientation note above the landmark constants). Pushed out to clear the armor shell, which
    # now stands ~10mm proud of the body itself.
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0.0, 0.108, 1.06))
    conduit = bpy.context.active_object
    conduit.name = "QUARK_SpineConduit"
    conduit.scale = (0.010, 0.010, 0.145)
    created.append(conduit)

    for obj in created:
        obj.data.materials.clear()
        obj.data.materials.append(accent)
        for poly in obj.data.polygons:
            poly.material_index = 0
        if arm_obj is not None:
            obj.parent = arm_obj
            mod = obj.modifiers.new("Armature", 'ARMATURE')
            mod.object = arm_obj
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.shade_smooth()
    return created


def setup_render():
    scene = bpy.context.scene
    for engine in ('BLENDER_EEVEE_NEXT', 'BLENDER_EEVEE', 'CYCLES'):
        try:
            scene.render.engine = engine
            break
        except TypeError:
            continue
    # Rendering-refinement pass Tier 2: resolution raised ~1.5x (900x1400 -> 1350x2100) now that
    # the lighting/AA quality below is worth resolving properly.
    scene.render.resolution_x = 1350
    scene.render.resolution_y = 2100
    # Real alpha matte -- was False, which is the entire root cause of the "PNGs are not
    # alpha-matted" bug flagged in PRODUCTION_LOG's Phase 4b entry (the shader had to synthesize
    # its own background-color chroma-key mask as a workaround). Explicit RGBA output alongside it
    # so the alpha channel is actually written to the PNG, not just computed and discarded.
    scene.render.film_transparent = True
    scene.render.image_settings.color_mode = 'RGBA'
    # Explicit view transform -- Blender 5.2 defaults to AgX, which deliberately desaturates
    # bright emissives toward white. This is the real, specific cause of the Phase 4b entry's
    # measured "pure (0,1,0) emissive bakes as rgb(155,218,137)" finding (not generic "bloom"),
    # and why the original accent color-key was mathematically unfireable. 'Standard' renders
    # emissive colors at their authored saturation.
    scene.view_settings.view_transform = 'Standard'
    # Studio HDRI environment instead of a flat colour. Gloss is mostly REFLECTED ENVIRONMENT, and
    # `film_transparent` removes the world from the alpha but NOT from reflections -- so with the
    # previous uniform dark world the ceramic's clear coat had nothing but a constant grey to
    # mirror and produced almost no highlight variation, no matter how low its roughness went. The
    # HDRI is `photo_studio_01_2k` from Poly Haven (CC0, no attribution required), committed into
    # the repo so the pipeline is self-contained rather than depending on a machine-local download.
    scene.world = bpy.data.worlds.get("World") or bpy.data.worlds.new("World")
    scene.world.use_nodes = True
    nt = scene.world.node_tree
    nt.nodes.clear()
    out_node = nt.nodes.new("ShaderNodeOutputWorld")
    bg = nt.nodes.new("ShaderNodeBackground")
    hdri_path = os.path.join(
        os.path.dirname(__file__), "..", "..", "hdri", "photo_studio_01_2k.hdr")
    hdri_path = os.path.abspath(hdri_path)
    if os.path.isfile(hdri_path):
        env = nt.nodes.new("ShaderNodeTexEnvironment")
        env.image = bpy.data.images.load(hdri_path)
        nt.links.new(env.outputs["Color"], bg.inputs["Color"])
        bg.inputs["Strength"].default_value = 0.45   # balanced against the area lights below;
        # measured: at 1.0 the plates clipped (mean luminance 0.76, torso 0.99), at 0.45 they sit
        # at 0.52 with real specular variation and no blown highlights.
    else:
        print(f"WARNING: studio HDRI not found, falling back to flat world: {hdri_path}")
        bg.inputs["Color"].default_value = (0.05, 0.065, 0.05, 1.0)
    nt.links.new(bg.outputs["Background"], out_node.inputs["Surface"])

    # Tier 2 quality: EEVEE Next's raytraced AO/soft-shadow path and a higher final-render sample
    # count -- was left at whatever the engine's own defaults were, uneffective for shadows.
    # Confirmed both property names exist on this build (`scene.eevee.bl_rna.properties`) before
    # using them -- `use_gtao` is gone in EEVEE Next; AO now rides on `use_raytracing`.
    scene.eevee.use_raytracing = True
    scene.eevee.use_shadows = True
    scene.eevee.taa_render_samples = 128

    # Tier 2 lighting: was 4 flat SUN lamps (parallel rays -> hard, shadowless-looking edges,
    # no falloff) with no dedicated backlight. Replaced with soft AREA lights (shadow softness
    # comes from `size`, not a separate setting) in a loose 3-point-plus-backlight arrangement.
    # This still has to read reasonably from every turnaround camera angle (the character doesn't
    # rotate -- the camera orbits it across 5 fixed views), which is why this keeps multiple
    # world-space-fixed lights covering different sides rather than a single camera-relative
    # 3-point rig that would only look right from one angle.
    key = bpy.data.lights.new("KeyLight", type='AREA')
    key.energy = 8.4
    key.size = 1.2
    key_obj = bpy.data.objects.new("KeyLight", key)
    bpy.context.collection.objects.link(key_obj)
    key_obj.location = (1.3, -1.6, 2.0)
    key_obj.rotation_euler = (math.radians(55), 0, math.radians(35))

    fill = bpy.data.lights.new("FillLight", type='AREA')
    fill.energy = 2.5
    fill.size = 1.6
    fill_obj = bpy.data.objects.new("FillLight", fill)
    bpy.context.collection.objects.link(fill_obj)
    fill_obj.location = (-1.6, -1.0, 1.4)
    fill_obj.rotation_euler = (math.radians(65), 0, math.radians(-120))

    overhead = bpy.data.lights.new("OverheadLight", type='AREA')
    overhead.energy = 6.3
    overhead.size = 1.8
    overhead_obj = bpy.data.objects.new("OverheadLight", overhead)
    bpy.context.collection.objects.link(overhead_obj)
    overhead_obj.location = (0, 0, 3.2)
    overhead_obj.rotation_euler = (math.radians(80), 0, math.radians(0))

    profile = bpy.data.lights.new("ProfileFillLight", type='AREA')
    profile.energy = 3.15
    profile.size = 1.4
    profile_obj = bpy.data.objects.new("ProfileFillLight", profile)
    bpy.context.collection.objects.link(profile_obj)
    profile_obj.location = (1.8, 0, 1.2)
    profile_obj.rotation_euler = (math.radians(25), 0, math.radians(90))

    # New: a genuine physical backlight -- previously the rim-light effect only existed as the
    # AGSL shader's synthetic edge-gradient hack (QuarkAvatarShader.kt). This gives the shader's
    # rim something real to build on instead of faking the whole effect from nothing.
    back = bpy.data.lights.new("BackLight", type='AREA')
    back.energy = 5.25
    back.size = 1.5
    back_obj = bpy.data.objects.new("BackLight", back)
    bpy.context.collection.objects.link(back_obj)
    back_obj.location = (0, 1.8, 1.8)
    back_obj.rotation_euler = (math.radians(-60), 0, math.radians(180))

    # Tier 2 lens: was 24mm (turnaround) -- a wide-angle focal length that distorts a full-figure
    # portrait subject (perspective stretching toward frame edges/near the camera). Moved to 60mm,
    # a standard portrait/product-shot focal length with negligible distortion, with distance
    # scaled by the same ratio (60/24) to preserve the existing framing/subject size in-frame
    # (pinhole-camera approximation: image size is proportional to focal_length/distance for a
    # fixed sensor width, so distance must scale with focal length to hold framing constant).
    cam_data = bpy.data.cameras.new("TurnCam")
    cam_data.lens = 60
    cam_obj = bpy.data.objects.new("TurnCam", cam_data)
    bpy.context.collection.objects.link(cam_obj)
    scene.camera = cam_obj
    return cam_obj


def direction_to_euler(direction):
    import mathutils
    vec = mathutils.Vector(direction).normalized()
    return vec.to_track_quat('-Z', 'Y').to_euler()


def render_turnaround(cam_obj, out_dir, views=None, prefix="blockout"):
    target_z = 0.84
    cam_z = target_z + 0.15
    dist = 5.0  # was 2.0 @ 24mm lens; scaled by the 60/24 lens-change ratio to hold framing
    if views is None:
        views = {
            "front": 0,
            "three_quarter_left": 45,
            "left_side": 90,
            "back": 180,
            "three_quarter_right": -45,
        }
    for name, deg in views.items():
        rad = math.radians(deg)
        cam_obj.location = (dist * math.sin(rad), -dist * math.cos(rad), cam_z)
        direction = (0 - cam_obj.location[0], 0 - cam_obj.location[1], target_z - cam_obj.location[2])
        cam_obj.rotation_euler = direction_to_euler(direction)
        bpy.context.scene.render.filepath = os.path.join(out_dir, f"{prefix}_{name}.png")
        bpy.ops.render.render(write_still=True)


def set_emissive_color(mat, color, strength):
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    if "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*color, 1.0)
        bsdf.inputs["Emission Strength"].default_value = strength


def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))

    clear_scene()
    human = create_and_tune_human()
    bpy.context.view_layer.objects.active = human
    bpy.ops.object.shade_smooth()

    # Real MakeHuman assets first: `set_character_skin` writes into the body's material slots, so
    # it has to happen BEFORE assign_materials(), which reuses that skin for the face rather than
    # overwriting it with a procedural tone. Supersedes the old hand-built add_eyes()/add_hair().
    add_makehuman_assets(human)
    emissive_mat = assign_materials(human, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0)

    uv_unwrap_if_needed(human)
    texture_dir = os.path.join(repo_root, "art", "quark-avatar", "textures")
    bake_textures(human, texture_dir)

    arm_obj = add_rig_and_weights(human)

    # Armor + accent are built AFTER rigging on purpose: the shell duplicates the already-weighted
    # body mesh, so it inherits MPFB's vertex groups and deforms with the rig without a second
    # weighting pass. See build_armor_shell()'s docstring.
    build_armor_shell(human, arm_obj)
    add_accent_geometry(human, arm_obj, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0)

    cam_obj = setup_render()

    out_dir = os.path.join(repo_root, "art", "quark-avatar", "renders", "blockout_turnaround")
    os.makedirs(out_dir, exist_ok=True)
    render_turnaround(cam_obj, out_dir)

    compare_dir = os.path.join(repo_root, "art", "quark-avatar", "renders", "palette_compare")
    os.makedirs(compare_dir, exist_ok=True)
    compare_views = {"front": 0, "three_quarter_left": 45}
    render_turnaround(cam_obj, compare_dir, views=compare_views, prefix="sheet_neutral")
    set_emissive_color(emissive_mat, (0.0, 1.0, 0.0), 7.0)
    render_turnaround(cam_obj, compare_dir, views=compare_views, prefix="phosphor_green")
    set_emissive_color(emissive_mat, (0.902, 0.945, 1.0), 7.0)

    blend_dir = os.path.join(repo_root, "art", "quark-avatar", "blender")
    os.makedirs(blend_dir, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(blend_dir, "quark_base.blend"))
    print("QUARK MPFB-based base mesh + rig complete.")


main()
