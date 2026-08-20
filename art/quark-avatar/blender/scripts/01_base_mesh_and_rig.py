"""
QUARK base-mesh + rig blockout — Phase 1 of the QUARK 3D avatar pipeline.

Builds a proportion-accurate humanoid blockout (167cm, athletic/feminine) from the
sideview concept sheet's measurements, in a T-pose, with a matching armature.
This is a BLOCKOUT: separate lofted limb forms joined into one object, not a
seamlessly-welded / sculpted mesh. It exists to validate proportions and joint
placement before any detail or retopology pass.

Run headless:
  blender --background --python 01_base_mesh_and_rig.py
"""
import bpy
import bmesh
import math
import mathutils

CM = 0.01  # 1 Blender unit = 1 meter; convert cm inputs to meters

# --- Anthropometric-canon correction (Director decision, 2026-08-20) ---
# Replaces the original Loomis 7.5-head art-canon leg proportions (knee landmark at 33cm from
# the floor) with values derived from real 167cm-stature anthropometric data, per the sanity
# check logged in PRODUCTION_LOG.md and the rendered comparison in 02_anthropometric_compare.py
# (marker-ring renders showing both variants) that the Director reviewed before deciding. Only
# correcting what that research actually found wrong (hip, knee) — ankle height wasn't covered
# by either cited source, so it's left at its original value rather than inventing a figure.
CANON_PELVIS_CM = 78.0        # old pelvis/hip torso landmark, kept only as the tz() reference point
PELVIS_CM = 84.5              # ergonomics table: hip height = 50.6% of 167cm stature
KNEE_CM = 56.4                # average of two cited sources: 61.3cm (ergonomics table, 36.7% of
                               # stature) and 51.4cm (Chumlea clinical regression)
ANKLE_CM = 8.0                # unchanged — no source cited for ankle height
TORSO_SCALE = (167.0 - PELVIS_CM) / (167.0 - CANON_PELVIS_CM)  # compress torso+head so total
                               # stature stays 167cm now that the leg span (floor-to-hip) is longer


def tz(h_cm):
    """Map an old (canon) torso/head z-landmark, in cm, to its corrected position under the
    anthropometric-canon pelvis height — continuous compression anchored at the new pelvis,
    validated visually in 02_anthropometric_compare.py before this was folded in here."""
    return PELVIS_CM + (h_cm - CANON_PELVIS_CM) * TORSO_SCALE


def tz_m(z_m):
    """Same transform as tz(), operating in meters — for _classify_material_index()'s thresholds."""
    return tz(z_m * 100) / 100


def clear_scene():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for block_collection in (bpy.data.meshes, bpy.data.armatures, bpy.data.materials):
        for block in list(block_collection):
            if block.users == 0:
                block_collection.remove(block)


def ring(bm, center, rx, ry, segments, axis='Z'):
    """Create a ring of verts around `center` in the plane perpendicular to `axis` — i.e. for a
    limb whose loft travels along `axis`, this is a real cross-section (has extent in the other
    two dimensions, none along the travel direction itself).
    'Z': legs (loft runs along Z) — varies X/Y, fixed Z.
    'X': arms/fingers (loft runs along +/-X) — varies Y/Z, fixed X.
    'Y': toes (loft runs along +/-Y, one foot's toes separated along X) — varies X/Z, fixed Y.
    Added for toes specifically: reusing 'Z' left rings with zero Z-extent (a near-flat sliver,
    thinner than the voxel grid — caused disconnected islands after the anthropometric-canon
    correction shifted the mesh's bounding box); reusing 'X' left rings with zero X-extent, which
    is exactly the dimension separating one toe from its neighbor, and voxel remeshing fused them
    into one blob instead. Neither existing mode is a real cross-section for a Y-traveling,
    X-separated limb — this one is."""
    verts = []
    for i in range(segments):
        angle = 2 * math.pi * i / segments
        if axis == 'Z':
            x = center[0] + rx * math.cos(angle)
            y = center[1] + ry * math.sin(angle)
            z = center[2]
        elif axis == 'X':  # arms, loft runs along +/-X
            x = center[0]
            y = center[1] + ry * math.sin(angle)
            z = center[2] + rx * math.cos(angle)
        else:  # axis == 'Y' (toes, loft runs along +/-Y)
            x = center[0] + rx * math.cos(angle)
            y = center[1]
            z = center[2] + ry * math.sin(angle)
        verts.append(bm.verts.new((x, y, z)))
    return verts


def _centroid(ring):
    n = len(ring)
    return mathutils.Vector((
        sum(v.co.x for v in ring) / n,
        sum(v.co.y for v in ring) / n,
        sum(v.co.z for v in ring) / n,
    ))


def _cap(bm, ring, outward_dir):
    """Build a cap face and force its normal to point along `outward_dir`, computed
    explicitly rather than guessed — hand-picked winding order was producing inward-facing
    (black, unlit) caps at limb/torso joins."""
    face = bm.faces.new(ring)
    bm.normal_update()
    if face.normal.dot(outward_dir) < 0:
        bmesh.ops.reverse_faces(bm, faces=[face])


def loft(bm, rings_list, cap_start=False, cap_end=False):
    """Bridge a list of same-length vertex rings into a tube of quads."""
    segments = len(rings_list[0])
    for a, b in zip(rings_list, rings_list[1:]):
        for i in range(segments):
            v1, v2 = a[i], a[(i + 1) % segments]
            v3, v4 = b[(i + 1) % segments], b[i]
            bm.faces.new((v1, v2, v3, v4))
    if cap_start:
        c0 = _centroid(rings_list[0])
        c1 = _centroid(rings_list[1])
        _cap(bm, rings_list[0], (c0 - c1).normalized())
    if cap_end:
        c_last = _centroid(rings_list[-1])
        c_prev = _centroid(rings_list[-2])
        _cap(bm, rings_list[-1], (c_last - c_prev).normalized())


def build_torso_and_head(bm, seg=14):
    # (height_cm, rx_cm, ry_cm) — rx = half-width (X), ry = half-depth (Y).
    # Neck taper is spread across four intermediate landmarks (133->150cm) instead of one big
    # radius drop, to avoid the pinched/notched look a single sharp step produced.
    landmarks = [
        (tz(78), 17, 11),      # pelvis / hip
        (tz(90), 15, 10),
        (tz(100), 13.5, 9.5),  # waist
        (tz(112), 15, 10),
        (tz(122), 16.5, 11.5),  # bust
        (tz(129), 15.5, 11),
        (tz(135), 13, 9.5),    # shoulder base
        (tz(140.5), 10, 8),    # lower neck
        (tz(145), 7.5, 6.8),   # neck mid
        (tz(150), 6, 6),       # neck top / chin
    ]
    rings_list = [ring(bm, (0, 0, h * CM), rx * CM, ry * CM, seg) for h, rx, ry in landmarks]
    # Capped at BOTH ends: the neck-top used to stay open on the assumption the head sphere
    # would plug it, but the overlap was too thin — from side angles the camera looked straight
    # into the open tube's unlit interior (the real cause of the persistent dark neck wedge, not
    # the arm caps). A fully closed torso avoids that regardless of the head sphere's exact fit.
    loft(bm, rings_list, cap_start=True, cap_end=True)

    # Head as a scaled icosphere, welded to the neck-top ring by proximity (blockout, not welded topology)
    bm_head = bmesh.new()
    bmesh.ops.create_icosphere(bm_head, subdivisions=2, radius=1.0)
    for v in bm_head.verts:
        v.co.x *= 7.2 * CM
        v.co.y *= 8.5 * CM
        v.co.z = v.co.z * 9.5 * CM + tz(159) * CM
    bm_head.verts.ensure_lookup_table()
    vert_map = {v: bm.verts.new(v.co) for v in bm_head.verts}
    for f in bm_head.faces:
        bm.faces.new(vert_map[v] for v in f.verts)
    bm_head.free()
    return rings_list[0]  # pelvis ring, for hip attachment reference


def build_limb(bm, landmarks, side_axis, seg=10):
    """landmarks: list of (pos_cm, rx_cm, ry_cm) along the limb's own local length.
    side_axis 'Z' for legs (loft runs vertically, foot flattens/elongates at the end),
    'X' for arms (loft runs horizontally, hand flattens/widens at the end).
    Capped at both ends: the start end is pushed to overlap the torso volume so the seam
    reads as a blend rather than a hard step; the far end (toe/fingertip) is a small cap."""
    rings_list = [ring(bm, pos, rx, ry, seg, axis=side_axis) for pos, rx, ry in landmarks]
    loft(bm, rings_list, cap_start=True, cap_end=True)


def build_toes(bm, side, foot_center_x, seg=6):
    """Five separate tapered toes branching from the foot, same technique as build_fingers()
    (short overlapping lofts, welded by the voxel remesh) — replaces the single continuous taper
    to one point the foot loft used to end in. Ordered medial (big toe, longest) to lateral
    (little toe, shortest), matching real foot proportions."""
    # (medial_offset_cm, length_cm, base_radius_cm) — medial_offset positive = toward the
    # centerline, converted to a signed world-X offset below (mirrors per leg side).
    #
    # Root-cause note (found after the anthropometric-canon correction exposed it): this used to
    # pass axis='Z' to build_limb(), same as build_legs() — correct for a Z-traveling limb, wrong
    # for toes (which travel along Y): rings had zero Z-extent, relying on the 2-4mm Z-gaps
    # between landmarks for "thickness," which is thinner than the 4mm voxel grid — confirmed by
    # dumping the disconnected fragment's actual vertices (<1mm of Z-extent, a flat pancake).
    # Tried axis='X' next (build_fingers()'s mode) — that fixed the disconnection but broke
    # something else: 'X' mode fixes X (zero X-extent per ring), and X is exactly the dimension
    # separating one toe from its neighbor, so adjacent toes fused into one blob instead (verified
    # via an x-position slice scan: one continuous cluster where Phase 3g's original check found
    # 6 separate ones). Neither mode is a real cross-section for a Y-traveling, X-separated limb —
    # added a proper axis='Y' mode to ring() instead (varies X/Z, fixed Y).
    toe_defs = [
        (-2.0, 3.0, 0.9),   # big toe
        (-0.8, 2.7, 0.8),
        (0.5, 2.3, 0.7),
        (1.8, 1.9, 0.6),
        (3.0, 1.5, 0.65),   # little toe — radius nudged up 0.55->0.65cm as extra margin
    ]
    for medial_off, length, base_r in toe_defs:
        tx = foot_center_x - side * medial_off * CM
        landmarks = [
            ((tx, 5 * CM, 1.2 * CM), base_r * CM, base_r * CM),              # overlap into foot
            ((tx, 9 * CM, 1.0 * CM), base_r * 0.9 * CM, base_r * 0.9 * CM),  # toe base
            ((tx, (9 + length) * CM, 0.6 * CM), 0.3 * CM, 0.3 * CM),         # tip
        ]
        build_limb(bm, landmarks, 'Y', seg)


def build_legs(bm, seg=10):
    # hip/knee corrected per the anthropometric-canon decision above; ankle unchanged (no source
    # cited for it). The two intermediate points (upper-thigh, calf) keep the SAME fractional
    # position along their segment that the canon build used (0.6939 hip->knee for the upper-thigh
    # pair, 0.52 knee->ankle for calf) rather than picking new absolute numbers — re-applied to
    # the new segment lengths, validated in 02_anthropometric_compare.py before landing here.
    hip_start = PELVIS_CM + 4.0  # preserve the canon build's 4cm overlap into the pelvis (82 vs 78)
    knee = KNEE_CM
    ankle = ANKLE_CM
    mid_thigh = hip_start - 0.6939 * (hip_start - knee)
    upper_thigh = hip_start - 0.5 * 0.6939 * (hip_start - knee)
    calf = knee - 0.52 * (knee - ankle)

    for side in (-1, 1):
        x = side * 9.5 * CM
        landmarks = [
            ((x, 0, hip_start * CM), 8.5 * CM, 8.5 * CM),          # hip, overlapping up into the pelvis
            ((x, 0, upper_thigh * CM), 8.3 * CM, 8.3 * CM),
            ((x, 0.5 * CM, mid_thigh * CM), 7.0 * CM, 7.0 * CM),   # mid thigh
            ((x, 0.5 * CM, knee * CM), 5.3 * CM, 5.3 * CM),        # knee
            ((x * 1.02, 0.5 * CM, calf * CM), 5.0 * CM, 5.0 * CM),  # calf
            ((x * 1.05, 0.5 * CM, ankle * CM), 3.4 * CM, 3.4 * CM),   # ankle
            ((x * 1.05, 1 * CM, 4 * CM), 3.6 * CM, 4.5 * CM),     # heel / foot top — starts flattening
            ((x * 1.05, 5 * CM, 2 * CM), 3.2 * CM, 6.0 * CM),     # foot mid — elongating forward
            ((x * 1.05, 9 * CM, 1 * CM), 2.0 * CM, 3.0 * CM),     # toe base — toes branch from here
        ]
        build_limb(bm, landmarks, 'Z', seg)
        build_toes(bm, side, x * 1.05, seg=6)


def build_fingers(bm, side, seg=8):
    """Five separate tapered digits branching from the palm, each its own short loft overlapping
    into the palm volume (same "genuine volumetric overlap, let voxel remesh weld it" approach
    used everywhere else in this file) — replaces the earlier single-taper paddle-to-a-point
    hand, which had no finger separation at all and only faked the look with a texture trick."""
    # (y_offset_cm, start_x_cm, length_cm, base_radius_cm) — thumb shorter, offset toward one edge
    finger_defs = [
        (-3.6, 74.0, 5.5, 1.15),  # thumb
        (-2.3, 76.0, 7.5, 1.05),  # index
        (-0.6, 76.5, 8.2, 1.05),  # middle
        (1.1, 76.0, 7.8, 1.0),    # ring
        (2.8, 75.0, 6.5, 0.95),   # pinky
    ]
    for y_off, start_x, length, base_r in finger_defs:
        x0 = side * start_x * CM
        x_overlap = x0 - side * 1.5 * CM  # pushed back into the palm for a real weld, not a seam
        x_tip = side * (start_x + length) * CM
        palm_z = tz(130) * CM
        landmarks = [
            ((x_overlap, y_off * CM, palm_z), base_r * CM, base_r * 0.85 * CM),
            (((x0 + x_tip) / 2, y_off * CM, palm_z), base_r * 0.75 * CM, base_r * 0.68 * CM),
            ((x_tip, y_off * CM, palm_z), 0.25 * CM, 0.25 * CM),
        ]
        build_limb(bm, landmarks, 'X', seg)


def build_arms(bm, seg=10):
    for side in (-1, 1):
        landmarks = [
            # small anchor disc well inside the torso's wider upper-chest zone (not near the
            # neck taper) so its cap doesn't poke through the torso surface near the neck
            ((side * 11 * CM, 0, tz(130) * CM), 4.5 * CM, 4.5 * CM),
            ((side * 20 * CM, 0, tz(132.5) * CM), 6.2 * CM, 6.2 * CM),
            ((side * 32 * CM, 0, tz(132) * CM), 4.6 * CM, 4.6 * CM),   # upper arm mid
            ((side * 46 * CM, 0, tz(131) * CM), 3.8 * CM, 3.8 * CM),   # elbow
            ((side * 60 * CM, 0, tz(130.3) * CM), 3.3 * CM, 3.3 * CM),  # forearm mid
            ((side * 71 * CM, 0, tz(130) * CM), 2.6 * CM, 2.2 * CM),   # wrist
            ((side * 76 * CM, 0, tz(130) * CM), 2.3 * CM, 4.5 * CM),   # palm — fingers branch from here
        ]
        build_limb(bm, landmarks, 'X', seg)
        build_fingers(bm, side, seg=8)


def build_mesh():
    bm = bmesh.new()
    build_torso_and_head(bm)
    build_legs(bm)
    build_arms(bm)
    # No global recalc_face_normals here: cap faces now get an explicitly computed, verifiably
    # correct normal in _cap(); the wall/bridge quads' winding (from loft()'s vertex order) has
    # rendered correctly in every pass so far. A global recalc previously re-flipped the caps
    # back to the wrong (inward-facing, black) orientation its volume heuristic preferred.

    mesh = bpy.data.meshes.new("QUARK_Base_Mesh")
    bm.to_mesh(mesh)
    bm.free()
    mesh.update()

    obj = bpy.data.objects.new("QUARK_Base", mesh)
    bpy.context.collection.objects.link(obj)
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.shade_smooth()

    sub = obj.modifiers.new(name="Subdivision", type='SUBSURF')
    sub.levels = 1
    sub.render_levels = 2
    return obj


def retopologize(obj):
    """Weld the blockout's separate, overlapping lofted forms (torso/legs/arms — touching but
    not sharing topology, per the Phase 1 production log) into one continuous, deformation-ready
    quad mesh via voxel remesh — robust against the self-intersecting/overlapping input a boolean
    union would choke on, and (verified) already outputs an all-quad manifold mesh directly.
    Any pre-existing modifiers (the Phase-1 preview Subsurf) are cleared first since remesh
    rebuilds geometry from scratch and ignores them anyway.
    """
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    for m in list(obj.modifiers):
        obj.modifiers.remove(m)

    # Blender's voxel remesher already outputs an all-quad, manifold mesh directly (verified:
    # 0 non-manifold edges/verts, 0 tris/ngons on the result) — QuadriFlow's own manifold check
    # rejected that same output and silently no-op'd rather than reducing it to target_faces, so
    # face count is controlled directly via voxel_size instead of relying on a QuadriFlow pass.
    # voxel_size tightened 0.010->0.004: at 1cm voxels the ~1.5cm-thick fingers (build_fingers())
    # and the sub-centimeter gaps between them were below the remesh's resolution and blobbed
    # back into one paddle shape — confirmed by a close-up render, not assumed. This mesh is a
    # Blender-side asset for baking/rendering, not a real-time one (the app pipeline uses
    # pre-rendered frames per the hybrid render-path decision), so the heavier face count from
    # finer voxels is an acceptable tradeoff for correctness here.
    # NOT tightened further for the anthropometric-canon correction, despite the little toe
    # coming out disconnected after that change (see build_toes()) — tried 0.003 first and it
    # made things WORSE (9 islands, not fewer), which is the real tell: voxel remeshing resolves
    # true gaps more faithfully at finer resolution (that's what fixed the Phase 3f finger-
    # blobbing problem — separate things staying separate), but a toe-to-foot join is SUPPOSED
    # to merge, so a marginal-but-real overlap there needs width, not resolution. Fixed at the
    # source in build_toes() (wider overlap ring) instead; voxel_size stays 0.004.
    remesh = obj.modifiers.new(name="VoxelRemesh", type='REMESH')
    remesh.mode = 'VOXEL'
    remesh.voxel_size = 0.004
    remesh.adaptivity = 0.0
    remesh.use_smooth_shade = True
    bpy.ops.object.modifier_apply(modifier=remesh.name)

    bpy.ops.object.shade_smooth()
    sub = obj.modifiers.new(name="Subdivision", type='SUBSURF')
    sub.levels = 0
    sub.render_levels = 1


def make_material(
    name, base_color, metallic=0.0, roughness=0.5, emission=None, emission_strength=0.0,
    wear=True, wear_strength=0.35, noise_scale=22.0, noise_strength=0.12, bump_strength=0.04,
    panel_detail=False, panel_scale=9.0, rivet_scale=26.0, brushed=False,
):
    """Procedural PBR material: flat color/roughness (Phase 3's first pass) replaced with
    Pointiness-driven edge wear (worn/scuffed convex edges — the CLAUDE.md "used-future"
    aesthetic explicitly wants visible wear, not pristine surfaces) plus Noise-driven tonal
    mottling and a small bump for surface micro-detail. `wear=False` keeps a flat material
    (used for the emissive accent, where a clean glow reads better than a scuffed one).
    `panel_detail=True` adds procedural panel-seam grooves (Voronoi distance-to-edge, recessed +
    darkened) and small rivet-like flecks (a second, finer Voronoi's F1 distance thresholded near
    each cell's seed point) — there's no hand-authored panel-line art to trace, so this is a
    principled procedural stand-in for actual hard-surface greeble, not just flat material zones.
    `brushed=True` adds tight, consistently-oriented Wave-texture streaks into roughness — the
    sheet calls Metal Alloy "brushed/polished," which means *directional* grain, not the
    isotropic Noise mottling every other material uses; this is the one material where that
    distinction is worth the extra node."""
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

    # worn edges: convex areas (Pointiness) read as scuffed — lighter, less rough
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

    # subtle tonal mottling so flat panels don't read as a flat digital color
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
    last_bump_input = noise.outputs['Fac']  # unioned into the final bump chain below

    coord = None
    if panel_detail or brushed:
        coord = nodes.new('ShaderNodeTexCoord')
        coord.location = (-1200, -750)

    if panel_detail:
        # panel seams: Voronoi's distance-to-edge is near 0 exactly on cell borders — threshold
        # that into a thin mask and use it to darken + recess a groove line.
        panel_voronoi = nodes.new('ShaderNodeTexVoronoi')
        panel_voronoi.location = (-900, -700)
        panel_voronoi.feature = 'DISTANCE_TO_EDGE'
        # Euclidean distance gives organic, rounded cell boundaries — reads as cracked ceramic,
        # not a designed panel line. Chebyshev squares the cells off into straighter, more
        # deliberate-looking seams — closer to how an actual hard-surface panel line would be
        # drawn — with no extra parameters needed (unlike Minkowski's Exponent input, which
        # isn't exposed on this node in this Blender version).
        panel_voronoi.distance = 'CHEBYCHEV'
        panel_voronoi.inputs['Scale'].default_value = panel_scale
        links.new(coord.outputs['Object'], panel_voronoi.inputs['Vector'])

        seam_ramp = nodes.new('ShaderNodeValToRGB')
        seam_ramp.location = (-600, -700)
        seam_ramp.color_ramp.elements[0].color = (0.0, 0.0, 0.0, 1.0)   # on the seam: dark groove
        seam_ramp.color_ramp.elements[0].position = 0.02
        seam_ramp.color_ramp.elements[1].color = (1.0, 1.0, 1.0, 1.0)   # off the seam: unaffected
        seam_ramp.color_ramp.elements[1].position = 0.06
        links.new(panel_voronoi.outputs['Distance'], seam_ramp.inputs['Fac'])

        mix_seam = nodes.new('ShaderNodeMixRGB')
        mix_seam.location = (-300, 400)
        mix_seam.blend_type = 'MULTIPLY'
        mix_seam.inputs['Fac'].default_value = 0.85
        links.new(color_so_far, mix_seam.inputs['Color1'])
        links.new(seam_ramp.outputs['Color'], mix_seam.inputs['Color2'])
        color_so_far = mix_seam.outputs['Color']

        # rivets: a finer Voronoi's F1 distance is near 0 at each cell's seed point — threshold
        # tight around that to get small dots, tinted like a metal fastener head.
        rivet_voronoi = nodes.new('ShaderNodeTexVoronoi')
        rivet_voronoi.location = (-900, -950)
        rivet_voronoi.feature = 'F1'
        rivet_voronoi.inputs['Scale'].default_value = rivet_scale
        links.new(coord.outputs['Object'], rivet_voronoi.inputs['Vector'])

        rivet_ramp = nodes.new('ShaderNodeValToRGB')
        rivet_ramp.location = (-600, -950)
        rivet_ramp.color_ramp.elements[0].color = (1.0, 1.0, 1.0, 1.0)  # at the seed: rivet fleck
        rivet_ramp.color_ramp.elements[0].position = 0.05
        rivet_ramp.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)  # elsewhere: no contribution
        rivet_ramp.color_ramp.elements[1].position = 0.09
        links.new(rivet_voronoi.outputs['Distance'], rivet_ramp.inputs['Fac'])

        rivet_color_node = nodes.new('ShaderNodeRGB')
        rivet_color_node.location = (-900, -1150)
        rivet_color_node.outputs[0].default_value = (0.85, 0.87, 0.9, 1.0)  # metal-alloy-ish fleck

        # gate rivets to only appear near a panel seam — real fasteners run along a seam line,
        # not scattered uniformly across a flat panel; this is what makes them read as designed
        # hardware instead of noise. Reuses the panel Voronoi's own distance-to-edge field.
        near_seam_ramp = nodes.new('ShaderNodeValToRGB')
        near_seam_ramp.location = (-600, -1150)
        near_seam_ramp.color_ramp.elements[0].color = (1.0, 1.0, 1.0, 1.0)  # on/near the seam
        near_seam_ramp.color_ramp.elements[0].position = 0.14
        near_seam_ramp.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)  # mid-panel: gated off
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

        # seams recess (negative-reading bump), rivets raise slightly — chained onto the base
        # noise bump so all three contributions combine into one final shading normal.
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

    # edges are also smoother (worn shiny) than the grimy flat panels
    rough_ramp = nodes.new('ShaderNodeValToRGB')
    rough_ramp.location = (-300, -300)
    rough_ramp.color_ramp.elements[0].color = (roughness,) * 3 + (1.0,)
    rough_ramp.color_ramp.elements[1].color = (max(0.05, roughness - 0.3),) * 3 + (1.0,)
    links.new(geo.outputs['Pointiness'], rough_ramp.inputs['Fac'])
    roughness_out = rough_ramp.outputs['Color']

    if brushed:
        # tight, evenly-spaced, consistently-oriented bands (not noise) = a directional brushed
        # grain; mapped through Object coordinates so the direction is fixed in object space
        # rather than sliding per-face like a plain procedural texture would.
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

    # base bump from the tonal-mottling noise field
    bump = nodes.new('ShaderNodeBump')
    bump.location = (0, -450)
    bump.inputs['Strength'].default_value = bump_strength
    links.new(noise.outputs['Fac'], bump.inputs['Height'])

    if panel_detail:
        # chain: noise bump -> seam groove bump -> rivet raise bump -> BSDF Normal
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


def uv_unwrap(obj):
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=0.02)
    bpy.ops.object.mode_set(mode='OBJECT')


def _classify_material_index(co, idx):
    """Region rules approximating the sheet's material breakdown (Ceramic plating / Synth-Skin /
    Graphite joint seams / Metal Alloy hardware / Emissive Cyan accents) from a face center in
    local mesh coordinates (meters). Plate placement below is deliberately read off the reference
    sheet's "Details & Close-Ups" panel (`reference/crop_details.png`) — the sheet shows a
    handful of large, purposeful plates keyed to real joints (a shoulder pauldron, a hip wrap, a
    kneecap cap, a chest seam), not a uniform tessellation — rather than an evenly-distributed
    procedural split. It's still geometric approximation, not traced vector art, but the
    *placement* decisions below follow what the reference actually shows."""
    x, y, z = co.x, co.y, co.z
    ax = abs(x)
    r_xy = math.hypot(x, y)

    if z > tz_m(1.50):  # head / face
        # Reference's "Hair & Head Accessories" panel calls out a "Headband Circuit" / "Circuit
        # Arc (Thin LED Line)" at brow height — added below, and it reads well (confirmed in
        # render). Also tried a small "Audio Module" node near the ear the same way (a region
        # rule, matching everything else in this function) — that one's dropped: an axis-aligned
        # material-region box can only ever read as a blocky rectangle, and on the metallic
        # material it rendered as a flat black patch with no environment map to reflect (confirmed
        # via close-up, not assumed) rather than a small sensor node. Right call for the seam
        # bands elsewhere in this file, wrong tool for a small circular accent — that needs actual
        # sphere/disc geometry, not a material trick. Not attempting full facial topology either
        # (a different, much larger task — see log).
        if tz_m(1.575) < z < tz_m(1.585) and r_xy > 0.05:  # headband circuit — thin LED line around the head
            return idx['emissive']
        return idx['skin']

    # Shoulder pauldron: the reference's "Shoulder & Arm" close-up shows a large curved graphite
    # plate capping the ball joint, straddling the torso/arm boundary — not just a thin ring.
    if tz_m(1.24) < z < tz_m(1.37) and 0.09 < ax < 0.23:
        return idx['graphite']

    if z > tz_m(1.25) and ax > 0.16:  # out on an arm (past the torso's own width at this height)
        arm_local = ax - 0.10  # roughly distance from the shoulder root, in meters
        if 0.34 < arm_local < 0.40:
            return idx['graphite']  # elbow
        if 0.59 < arm_local < 0.65:
            return idx['graphite']  # wrist
        if arm_local > 0.62:
            # the hand/fingers are now real separated geometry (build_fingers()), not a flat
            # paddle — no texture-only knuckle-line fake needed anymore, the gaps between the
            # actual finger volumes read as separation on their own.
            return idx['skin']  # hand + fingers
        return idx['ceramic']

    # Leg-territory bands recenter on the actual (corrected) knee/hip landmarks, keeping the same
    # absolute half-widths the canon build used — kneecap ±3.5cm, leg-ceiling +2cm above the hip,
    # hip-wrap band shifted by the hip landmark's own delta. Ankle is unchanged (unchanged source).
    leg_ceiling = PELVIS_CM / 100 + 0.02
    knee_m = KNEE_CM / 100
    hip_delta_m = (PELVIS_CM - CANON_PELVIS_CM) / 100

    if z < leg_ceiling:  # leg territory
        if (knee_m - 0.035) < z < (knee_m + 0.035):
            return idx['graphite']  # kneecap — reference shows a distinct rounded cap, not a
            # thin band; widened slightly from the earlier pass to read as a real plate
        if 0.06 < z < 0.11:
            return idx['graphite']  # ankle
        if z < 0.06:
            return idx['graphite']  # foot
        return idx['ceramic']

    # Hip wrap: reference's "Hip & Waist" close-up shows a wide belt-like wrap (metal hardware
    # flanked by graphite), not just a thin metal ring.
    if (0.755 + hip_delta_m) < z < (0.865 + hip_delta_m) and r_xy > 0.075:
        if (0.785 + hip_delta_m) < z < (0.835 + hip_delta_m):
            return idx['metal']  # the ball-joint hardware itself, centered in the wrap
        return idx['graphite']  # the wrap plate flanking it above/below

    if y < -0.06 and ax < 0.05 and tz_m(0.85) < z < tz_m(1.38):  # spine conduit
        return idx['emissive']

    # Shoulder-blade panels: reference's "Back / Spine Conduit" close-up shows the conduit
    # flanked by two curved graphite panels (scapula plates), not bare ceramic either side of it.
    if y < -0.05 and 0.05 < ax < 0.13 and tz_m(1.02) < z < tz_m(1.32):
        return idx['graphite']

    # Chest-plate seam: reference's "Chest / Upper Torso" close-up shows a distinct seam line
    # across the upper front chest, not just flat plating.
    if tz_m(1.155) < z < tz_m(1.185) and y > 0.02:
        return idx['graphite']

    if tz_m(1.39) < z < tz_m(1.42):  # neck collar seam
        return idx['graphite']

    return idx['ceramic']  # default: torso plating


def assign_materials(obj, glow_color=(0.902, 0.945, 1.0), glow_strength=4.0):
    """glow_color/strength drive only the Emissive Cyan accent material (spine conduit) — this is
    the one knob the two palette-comparison renders (sheet-accurate vs. phosphor-collapsed) swap
    between; the four physical PBR materials are fixed, locked hex values from the concept sheet.
    """
    ceramic = make_material(
        "QUARK_Ceramic", (0.902, 0.882, 0.816), roughness=0.45,
        wear_strength=0.3, noise_scale=16.0, noise_strength=0.08,
        # Reference sheet's close-ups (Chest/Shoulder/Hip/Knee) show a handful of large,
        # deliberate plates, not a busy tessellation — panel_scale dropped 7->3.2 to match
        # (fewer, bigger cells); rivets stay fine-grained but are gated to seam-adjacency only.
        panel_detail=True, panel_scale=3.2, rivet_scale=22.0,
    )  # #E6E1D0 — the main plating, gets full panel-seam + rivet greeble
    synth_skin = make_material(
        "QUARK_SynthSkin", (0.549, 0.678, 0.651), roughness=0.55,
        wear_strength=0.15, noise_scale=30.0, noise_strength=0.05, bump_strength=0.015,
    )  # #8BADA6 approx — subtle: it's skin, not armor plating, no panel lines
    graphite = make_material(
        "QUARK_Graphite", (0.169, 0.176, 0.192), roughness=0.7,
        wear_strength=0.4, noise_scale=25.0, noise_strength=0.15,
    )  # #2B2D31 — already reads as a seam band itself; no additional panel lines needed
    metal_alloy = make_material(
        "QUARK_MetalAlloy", (0.784, 0.8, 0.82), metallic=1.0, roughness=0.3,
        wear_strength=0.55, noise_scale=12.0, noise_strength=0.1, bump_strength=0.02,
        panel_detail=True, panel_scale=6.0, rivet_scale=30.0, brushed=True,
    )  # #C8CCD1 — hardware: pronounced edge-wear, fewer but more deliberate plate breaks,
    # denser gated rivets (real fasteners cluster along a hardware seam), directional brushed
    # grain per the sheet's own "brushed/polished" material spec
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


def bake_textures(obj, out_dir, resolution=2048):
    """Bake the procedural node materials down to real PNG texture maps on the object's UV
    layout — portable assets usable outside this .blend, not just a look that only exists in
    Blender's own node graph. Cycles is required for baking (switched in, then restored).

    NORMAL is baked in tangent space with the material's own Bump-node chain wired in — this
    captures the seam/rivet/noise bump's effect on the shading normal directly, which is a
    legitimate way to get a real detail normal map from a purely procedural material with no
    separate high-poly sculpt to bake from (there isn't one here). AO is a standard grounding
    pass — crevices at seams/joints read as occluded regardless of the material's own color."""
    import os
    scene = bpy.context.scene
    original_engine = scene.render.engine
    scene.render.engine = 'CYCLES'
    scene.cycles.samples = 64
    scene.cycles.use_denoising = False

    subsurf = obj.modifiers.get("Subdivision")
    subsurf_was_visible = subsurf.show_render if subsurf else None
    if subsurf:
        subsurf.show_render = False  # bake against the raw UV'd mesh, not a subdivided cage

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


def build_armature():
    bpy.ops.object.armature_add(enter_editmode=True, location=(0, 0, 0))
    arm_obj = bpy.context.active_object
    arm_obj.name = "QUARK_Rig"
    ebones = arm_obj.data.edit_bones
    ebones.remove(ebones[0])  # drop the default bone

    def add_bone(name, head, tail, parent=None):
        b = ebones.new(name)
        b.head = head
        b.tail = tail
        if parent:
            b.parent = ebones[parent]
            b.use_connect = True
        return b

    add_bone("root", (0, 0, 0), (0, 0, 0.05))
    add_bone("pelvis", (0, 0, tz(78) * CM), (0, 0, tz(95) * CM), "root")
    add_bone("spine", (0, 0, tz(95) * CM), (0, 0, tz(122) * CM), "pelvis")
    add_bone("chest", (0, 0, tz(122) * CM), (0, 0, tz(140) * CM), "spine")
    add_bone("neck", (0, 0, tz(140) * CM), (0, 0, tz(150) * CM), "chest")
    add_bone("head", (0, 0, tz(150) * CM), (0, 0, tz(167) * CM), "neck")

    thigh_start = PELVIS_CM + 2.0  # preserve the canon build's +2cm offset from the pelvis landmark

    for side, label in ((-1, "L"), (1, "R")):
        add_bone(f"shoulder_{label}", (0, 0, tz(130) * CM), (side * 20 * CM, 0, tz(132.5) * CM), "chest")
        add_bone(f"upperarm_{label}", (side * 20 * CM, 0, tz(132.5) * CM), (side * 46 * CM, 0, tz(131) * CM), f"shoulder_{label}")
        add_bone(f"forearm_{label}", (side * 46 * CM, 0, tz(131) * CM), (side * 71 * CM, 0, tz(130) * CM), f"upperarm_{label}")
        add_bone(f"hand_{label}", (side * 71 * CM, 0, tz(130) * CM), (side * 84 * CM, 0, tz(130) * CM), f"forearm_{label}")

        hip_x = side * 9.5 * CM
        add_bone(f"thigh_{label}", (hip_x, 0, thigh_start * CM), (hip_x * 1.02, 0.5 * CM, KNEE_CM * CM), "pelvis")
        add_bone(f"shin_{label}", (hip_x * 1.02, 0.5 * CM, KNEE_CM * CM), (hip_x * 1.05, 0.5 * CM, ANKLE_CM * CM), f"thigh_{label}")
        add_bone(f"foot_{label}", (hip_x * 1.05, 0.5 * CM, ANKLE_CM * CM), (hip_x * 1.05, 11 * CM, 0.5 * CM), f"shin_{label}")

    bpy.ops.object.mode_set(mode='OBJECT')
    return arm_obj


def parent_mesh_to_armature(mesh_obj, arm_obj):
    mesh_obj.select_set(True)
    arm_obj.select_set(True)
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.parent_set(type='ARMATURE_AUTO')


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
        # Metal Alloy is metallic=1.0 (pure metal, no diffuse contribution) — with only sun
        # lights and no environment map, it has nothing to reflect except a direct specular
        # hit, so from most angles it renders flat black instead of reading as metal (confirmed
        # via a close-up on the ear-module accent: sharp-edged black rectangle, not shaded metal
        # — a real PBR consequence of the lighting rig, not a mesh defect). Bumped from
        # near-zero so metal surfaces have *something* non-black to reflect; still dark/CRT-toned.
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

    # Third light, mostly overhead: softens the collarbone/neck-shoulder crease's natural
    # concave shadow, which the original 2-light rig rendered too harshly for a preview.
    overhead = bpy.data.lights.new("OverheadLight", type='SUN')
    overhead.energy = 3.0
    overhead_obj = bpy.data.objects.new("OverheadLight", overhead)
    bpy.context.collection.objects.link(overhead_obj)
    overhead_obj.rotation_euler = (math.radians(80), 0, math.radians(0))

    # Fourth light, low and from the side: reaches the same crease specifically for the dead-on
    # profile ("left_side") camera angle, where staring straight down the crease self-shadows
    # regardless of overhead light.
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


def render_turnaround(cam_obj, out_dir, views=None, prefix="blockout"):
    import os
    target_z = 0.84  # mid-figure (total height 1.67m)
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


def direction_to_euler(direction):
    import mathutils
    vec = mathutils.Vector(direction).normalized()
    return vec.to_track_quat('-Z', 'Y').to_euler()


def main():
    import os
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))

    clear_scene()
    mesh_obj = build_mesh()
    retopologize(mesh_obj)
    uv_unwrap(mesh_obj)
    # Default glow = the hologram sheet's "Neutral" appearance token (#E6F1FF, rest/idle) — the
    # sheet-accurate variant of the two the palette decision asked to compare.
    emissive_mat = assign_materials(mesh_obj, glow_color=(0.902, 0.945, 1.0), glow_strength=7.0)

    texture_dir = os.path.join(repo_root, "art", "quark-avatar", "textures")
    bake_textures(mesh_obj, texture_dir)

    arm_obj = build_armature()
    parent_mesh_to_armature(mesh_obj, arm_obj)
    cam_obj = setup_render()

    out_dir = os.path.join(repo_root, "art", "quark-avatar", "renders", "blockout_turnaround")
    os.makedirs(out_dir, exist_ok=True)
    render_turnaround(cam_obj, out_dir)

    # Palette comparison: same mesh/materials, only the accent glow color changes — sheet-accurate
    # (just rendered, above) vs. phosphor-collapsed (GREEN, the CLAUDE.md "phosphor only" default).
    compare_dir = os.path.join(repo_root, "art", "quark-avatar", "renders", "palette_compare")
    os.makedirs(compare_dir, exist_ok=True)
    compare_views = {"front": 0, "three_quarter_left": 45}
    render_turnaround(cam_obj, compare_dir, views=compare_views, prefix="sheet_neutral")
    set_emissive_color(emissive_mat, (0.0, 1.0, 0.0), 7.0)  # phosphor GREEN #00FF00
    render_turnaround(cam_obj, compare_dir, views=compare_views, prefix="phosphor_green")
    set_emissive_color(emissive_mat, (0.902, 0.945, 1.0), 7.0)  # restore sheet-accurate default

    blend_dir = os.path.join(repo_root, "art", "quark-avatar", "blender")
    os.makedirs(blend_dir, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(blend_dir, "quark_base.blend"))
    print("QUARK base mesh + rig blockout complete.")


main()
