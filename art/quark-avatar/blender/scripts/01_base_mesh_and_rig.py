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
import os
import addon_utils

addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.humanservice import HumanService
from bl_ext.blender_org.mpfb.services.targetservice import TargetService


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
    macro = TargetService.get_default_macro_info_dict()
    macro['gender'] = 0.0        # 0=female (confirmed against MakeHuman target filenames)
    macro['age'] = 0.5           # young adult
    macro['muscle'] = 0.6        # athletic/toned
    macro['weight'] = 0.4        # lean
    macro['proportions'] = 0.5
    macro['height'] = 0.5
    macro['cupsize'] = 0.5
    macro['firmness'] = 0.6
    macro['race'] = {'asian': 0.2, 'caucasian': 0.6, 'african': 0.2}

    human = HumanService.create_human(
        mask_helpers=True, detailed_helpers=True, extra_vertex_groups=True,
        feet_on_ground=True, scale=0.1,  # 0.1 is MPFB's own "meters" convention -- scale=1.0
        # (tried first) produces a ~17m-tall mesh; confirmed by direct measurement, not assumed.
        macro_detail_dict=macro,
    )
    human.name = "QUARK_Base"
    human.data.name = "QUARK_Base_Mesh"
    return human


# ---------------------------------------------------------------------------------------------
# Measured landmarks (world Z meters) on the mesh `create_and_tune_human()` above produces, via
# this mesh's own rig-joint vertex groups (`joint-pelvis`, `joint-neck`, etc. -- weight > 0.4
# centroid). This mesh's rest pose is a relaxed A-pose (arms angled down), not the old blockout's
# T-pose -- confirmed by rendering and looking, not assumed from the shape-key names.
# ---------------------------------------------------------------------------------------------
Z_ANKLE = 0.074
Z_KNEE = 0.449
Z_HIP = 0.891
Z_SPINE4 = 0.937   # low waist
Z_SPINE1 = 1.251   # upper chest
Z_SHOULDER = 1.343
Z_SCAPULA = 1.371
Z_NECK = 1.408
Z_MOUTH = 1.507

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
):
    """Procedural PBR material -- verbatim from the prior blockout pipeline (mesh-agnostic, works
    on polygon material_index assignment regardless of the underlying mesh's topology)."""
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


def _classify_material_index(co, idx):
    """Region rules approximating the reference sheet's material breakdown, recalibrated against
    THIS mesh's own measured landmarks (see the Z_* constants above) -- the old blockout's
    coordinate thresholds are not transferable to a completely different mesh topology/proportions.
    Face/hands stay real exposed skin only above the neck; per the reference's own "Hand (Palm)"
    close-up the hands are ARMORED (segmented plates), not bare skin, unlike the old blockout's
    treatment -- corrected here."""
    x, y, z = co.x, co.y, co.z
    ax = abs(x)
    r_xy = math.hypot(x, y)

    if z > Z_NECK:  # head / face -- real exposed skin, the whole point of the MPFB swap
        if (Z_MOUTH + 0.06) < z < (Z_MOUTH + 0.075) and r_xy > 0.06:  # headband circuit
            return idx['emissive']
        return idx['skin']

    if (Z_NECK - 0.03) < z <= Z_NECK:  # neck collar seam
        return idx['graphite']

    # Arm territory: this mesh rests in an A-pose (not the old blockout's T-pose), so x and z vary
    # together along the arm -- ax alone is still a reasonable "out on the arm" gate since the
    # torso itself stays well under 0.18m half-width at these heights (measured, not assumed).
    if ax > 0.14 and Z_HIP < z < (Z_SCAPULA + 0.02):
        if Z_SHOULDER - 0.04 < z <= Z_SCAPULA + 0.02 and ax < 0.23:
            return idx['graphite']  # shoulder pauldron
        if z < 0.90:
            return idx['graphite']  # wrist band
        return idx['ceramic']

    if z < (Z_HIP + 0.02):  # leg territory
        if (Z_KNEE - 0.035) < z < (Z_KNEE + 0.035):
            return idx['graphite']  # kneecap
        if Z_ANKLE - 0.01 < z < Z_ANKLE + 0.04:
            return idx['graphite']  # ankle
        if z < Z_ANKLE - 0.01:
            return idx['graphite']  # foot
        return idx['ceramic']

    if Z_HIP - 0.02 < z < Z_HIP + 0.08 and r_xy > 0.075:  # hip wrap
        if Z_HIP + 0.01 < z < Z_HIP + 0.05:
            return idx['metal']
        return idx['graphite']

    # Back = +Y, front = -Y for this mesh (see the orientation note above the constants) --
    # opposite the old blockout's sign convention. Getting this backwards was a real bug caught by
    # rendering and looking (an early pass painted these regions on the front-facing side).
    if y > 0.06 and ax < 0.05 and Z_SPINE4 < z < Z_NECK - 0.03:  # spine conduit (back)
        return idx['emissive']

    if y > 0.05 and 0.05 < ax < 0.13 and Z_SPINE1 < z < Z_SCAPULA + 0.02:  # scapula plates (back)
        return idx['graphite']

    if Z_SPINE1 - 0.03 < z < Z_SPINE1 and y < -0.02:  # chest seam (front)
        return idx['graphite']

    return idx['ceramic']  # default: torso plating


def assign_materials(obj, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0):
    ceramic = make_material(
        "QUARK_Ceramic", (0.902, 0.882, 0.816), roughness=0.45,
        wear_strength=0.3, noise_scale=16.0, noise_strength=0.08,
        panel_detail=True, panel_scale=3.2, rivet_scale=22.0,
    )
    synth_skin = make_material(
        "QUARK_SynthSkin", (0.867, 0.757, 0.686), roughness=0.5,
        wear_strength=0.05, noise_scale=40.0, noise_strength=0.03, bump_strength=0.01,
    )  # a real skin tone -- this now covers the actual exposed MPFB face, not a flat placeholder
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
        ('ceramic', ceramic), ('skin', synth_skin), ('graphite', graphite),
        ('metal', metal_alloy), ('emissive', emissive),
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


def setup_render():
    scene = bpy.context.scene
    for engine in ('BLENDER_EEVEE_NEXT', 'BLENDER_EEVEE', 'CYCLES'):
        try:
            scene.render.engine = engine
            break
        except TypeError:
            continue
    scene.render.resolution_x = 900
    scene.render.resolution_y = 1400
    scene.render.film_transparent = False
    scene.world = bpy.data.worlds.get("World") or bpy.data.worlds.new("World")
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs[0].default_value = (0.05, 0.065, 0.05, 1.0)

    sun = bpy.data.lights.new("KeyLight", type='SUN')
    sun.energy = 3.0
    sun_obj = bpy.data.objects.new("KeyLight", sun)
    bpy.context.collection.objects.link(sun_obj)
    sun_obj.rotation_euler = (math.radians(55), 0, math.radians(35))

    fill = bpy.data.lights.new("FillLight", type='SUN')
    fill.energy = 0.8
    fill_obj = bpy.data.objects.new("FillLight", fill)
    bpy.context.collection.objects.link(fill_obj)
    fill_obj.rotation_euler = (math.radians(65), 0, math.radians(-120))

    overhead = bpy.data.lights.new("OverheadLight", type='SUN')
    overhead.energy = 3.0
    overhead_obj = bpy.data.objects.new("OverheadLight", overhead)
    bpy.context.collection.objects.link(overhead_obj)
    overhead_obj.rotation_euler = (math.radians(80), 0, math.radians(0))

    profile = bpy.data.lights.new("ProfileFillLight", type='SUN')
    profile.energy = 1.5
    profile_obj = bpy.data.objects.new("ProfileFillLight", profile)
    bpy.context.collection.objects.link(profile_obj)
    profile_obj.rotation_euler = (math.radians(25), 0, math.radians(90))

    cam_data = bpy.data.cameras.new("TurnCam")
    cam_data.lens = 24
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
    dist = 2.0
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

    emissive_mat = assign_materials(human, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0)
    eyes = add_eyes(human)
    hair = add_hair(human)

    uv_unwrap_if_needed(human)
    texture_dir = os.path.join(repo_root, "art", "quark-avatar", "textures")
    bake_textures(human, texture_dir)

    arm_obj = add_rig_and_weights(human)

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
