"""
QUARK anthropometric-canon comparison — answers the open question flagged at the end of
PRODUCTION_LOG.md's Phase 3g: build a real side-by-side of the current Loomis-art-canon leg
proportions vs. anthropometrically-corrected proportions, so the Director can decide with the
comparison in front of them rather than from a text description (same "verify don't eyeball"
approach as the earlier palette_compare render).

This is a standalone silhouette-comparison utility, NOT a rebuild of the production asset:
- No fingers/toes, no panel-seam/rivet greeble, no baked textures, no armature — none of that
  affects leg-length silhouette, which is the only thing being decided here. Whichever variant
  the Director picks gets folded into 01_base_mesh_and_rig.py's real landmark set as a follow-up,
  not produced by this script.
- Uses one flat-ish material for the whole mesh so nothing about material fidelity competes for
  attention with the actual proportion question.

Sourcing for the ANTHRO landmarks (both cited in PRODUCTION_LOG.md's anthropometric-sanity-check
entry, scaled to 167cm stature):
  - Hip height: ergonomics table, 50.6% of stature -> 84.5cm. Only source cited for hip, used
    directly.
  - Knee height: two independent sources bracket 51-61cm (ergonomics table 36.7% -> 61.3cm;
    Chumlea clinical regression -> 51.4cm). Used the average of the two, 56.4cm, rather than
    picking one arbitrarily.
  - Ankle height: NOT covered by either cited source, so left unchanged from the canon build
    (8cm) rather than inventing an unverified figure. Only correcting what the log's own research
    actually found wrong.
  - Everything above the hip (torso/head) is uniformly compressed to keep total height at 167cm
    now that the leg span (hip-to-floor) is longer, preserving the torso's own internal
    proportions (bust/waist/shoulder relative spacing) rather than distorting them.

Run headless:
  blender --background --python 02_anthropometric_compare.py
"""
import bpy
import bmesh
import math
import os

CM = 0.01

STATURE_CM = 167.0

CANON = dict(pelvis_z=78.0, hip_start_z=82.0, knee_z=33.0, ankle_z=8.0)
ANTHRO = dict(
    hip_start_z=84.5 + (82.0 - 78.0),  # preserve the same 4cm hip/pelvis overlap as canon
    pelvis_z=84.5,
    knee_z=56.4,
    ankle_z=8.0,  # not corrected — no source cited in the log for ankle height
)


def clear_scene():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for coll in (bpy.data.meshes, bpy.data.armatures, bpy.data.materials):
        for block in list(coll):
            if block.users == 0:
                coll.remove(block)


def ring(bm, center, rx, ry, segments, axis='Z'):
    verts = []
    for i in range(segments):
        angle = 2 * math.pi * i / segments
        if axis == 'Z':
            x = center[0] + rx * math.cos(angle)
            y = center[1] + ry * math.sin(angle)
            z = center[2]
        else:
            x = center[0]
            y = center[1] + ry * math.sin(angle)
            z = center[2] + rx * math.cos(angle)
        verts.append(bm.verts.new((x, y, z)))
    return verts


def _centroid(ring):
    n = len(ring)
    return (sum(v.co.x for v in ring) / n, sum(v.co.y for v in ring) / n, sum(v.co.z for v in ring) / n)


def _cap(bm, ring, outward_dir):
    import mathutils
    face = bm.faces.new(ring)
    bm.normal_update()
    if face.normal.dot(mathutils.Vector(outward_dir)) < 0:
        bmesh.ops.reverse_faces(bm, faces=[face])


def loft(bm, rings_list, cap_start=False, cap_end=False):
    import mathutils
    segments = len(rings_list[0])
    for a, b in zip(rings_list, rings_list[1:]):
        for i in range(segments):
            v1, v2 = a[i], a[(i + 1) % segments]
            v3, v4 = b[(i + 1) % segments], b[i]
            bm.faces.new((v1, v2, v3, v4))
    if cap_start:
        c0 = mathutils.Vector(_centroid(rings_list[0]))
        c1 = mathutils.Vector(_centroid(rings_list[1]))
        _cap(bm, rings_list[0], (c0 - c1).normalized())
    if cap_end:
        c_last = mathutils.Vector(_centroid(rings_list[-1]))
        c_prev = mathutils.Vector(_centroid(rings_list[-2]))
        _cap(bm, rings_list[-1], (c_last - c_prev).normalized())


def build_torso_and_head(bm, mode, seg=14):
    """Same shape as the canon torso, uniformly compressed above the pelvis so total stature
    stays 167cm once the leg span (pelvis-to-floor) changes."""
    canon_landmarks = [
        (78, 17, 11), (90, 15, 10), (100, 13.5, 9.5), (112, 15, 10), (122, 16.5, 11.5),
        (129, 15.5, 11), (135, 13, 9.5), (140.5, 10, 8), (145, 7.5, 6.8), (150, 6, 6),
    ]
    canon_pelvis = CANON['pelvis_z']
    new_pelvis = CANON['pelvis_z'] if mode == 'canon' else ANTHRO['pelvis_z']
    torso_scale = (STATURE_CM - new_pelvis) / (STATURE_CM - canon_pelvis)

    landmarks = [
        (new_pelvis + (h - canon_pelvis) * torso_scale, rx, ry)
        for h, rx, ry in canon_landmarks
    ]
    rings_list = [ring(bm, (0, 0, h * CM), rx * CM, ry * CM, seg) for h, rx, ry in landmarks]
    loft(bm, rings_list, cap_start=True, cap_end=True)

    canon_head_z, head_rx, head_ry, head_rz = 159, 7.2, 8.5, 9.5
    head_z = new_pelvis + (canon_head_z - canon_pelvis) * torso_scale
    bm_head = bmesh.new()
    bmesh.ops.create_icosphere(bm_head, subdivisions=2, radius=1.0)
    for v in bm_head.verts:
        v.co.x *= head_rx * CM
        v.co.y *= head_ry * CM
        v.co.z = v.co.z * head_rz * CM + head_z * CM
    bm_head.verts.ensure_lookup_table()
    vert_map = {v: bm.verts.new(v.co) for v in bm_head.verts}
    for f in bm_head.faces:
        bm.faces.new(vert_map[v] for v in f.verts)
    bm_head.free()
    return new_pelvis, torso_scale


def build_limb(bm, landmarks, side_axis, seg=10):
    rings_list = [ring(bm, pos, rx, ry, seg, axis=side_axis) for pos, rx, ry in landmarks]
    loft(bm, rings_list, cap_start=True, cap_end=True)


def build_legs(bm, mode, seg=10):
    hip_start = CANON['hip_start_z'] if mode == 'canon' else ANTHRO['hip_start_z']
    knee = CANON['knee_z'] if mode == 'canon' else ANTHRO['knee_z']
    ankle = CANON['ankle_z'] if mode == 'canon' else ANTHRO['ankle_z']

    # mid-thigh/calf keep the SAME fractional position along their segment that the canon build
    # used (mid_thigh 0.6939 of the way from hip to knee; calf 0.52 of the way from knee to
    # ankle), just re-applied to the new segment lengths, rather than picking new numbers.
    mid_thigh = hip_start - 0.6939 * (hip_start - knee)
    calf = knee - 0.52 * (knee - ankle)

    for side in (-1, 1):
        x = side * 9.5 * CM
        landmarks = [
            ((x, 0, hip_start * CM), 8.5 * CM, 8.5 * CM),
            ((x, 0, ((hip_start + mid_thigh) / 2) * CM), 8.3 * CM, 8.3 * CM),
            ((x, 0.5 * CM, mid_thigh * CM), 7.0 * CM, 7.0 * CM),
            ((x, 0.5 * CM, knee * CM), 5.3 * CM, 5.3 * CM),
            ((x * 1.02, 0.5 * CM, calf * CM), 5.0 * CM, 5.0 * CM),
            ((x * 1.05, 0.5 * CM, ankle * CM), 3.4 * CM, 3.4 * CM),
            ((x * 1.05, 1 * CM, ankle * 0.5 * CM), 3.6 * CM, 4.5 * CM),
            ((x * 1.05, 5 * CM, ankle * 0.25 * CM), 3.2 * CM, 6.0 * CM),
            ((x * 1.05, 9 * CM, ankle * 0.125 * CM), 2.0 * CM, 3.0 * CM),
        ]
        build_limb(bm, landmarks, 'Z', seg)


def build_arms(bm, torso_scale, new_pelvis, seg=10):
    """Arms hang from the shoulder, which is a torso landmark — shift with the same torso_scale
    transform as the rest of the torso so the arm attaches at the (re-derived) shoulder height
    instead of floating at the old canon height."""
    canon_pelvis = CANON['pelvis_z']

    def tz(h):
        return new_pelvis + (h - canon_pelvis) * torso_scale

    for side in (-1, 1):
        landmarks = [
            ((side * 11 * CM, 0, tz(130) * CM), 4.5 * CM, 4.5 * CM),
            ((side * 20 * CM, 0, tz(132.5) * CM), 6.2 * CM, 6.2 * CM),
            ((side * 32 * CM, 0, tz(132) * CM), 4.6 * CM, 4.6 * CM),
            ((side * 46 * CM, 0, tz(131) * CM), 3.8 * CM, 3.8 * CM),
            ((side * 60 * CM, 0, tz(130.3) * CM), 3.3 * CM, 3.3 * CM),
            ((side * 71 * CM, 0, tz(130) * CM), 2.6 * CM, 2.2 * CM),
            ((side * 76 * CM, 0, tz(130) * CM), 2.3 * CM, 4.5 * CM),
        ]
        build_limb(bm, landmarks, 'X', seg)


def build_mesh(mode):
    bm = bmesh.new()
    new_pelvis, torso_scale = build_torso_and_head(bm, mode)
    build_legs(bm, mode)
    build_arms(bm, torso_scale, new_pelvis)

    mesh = bpy.data.meshes.new(f"QUARK_Compare_{mode}")
    bm.to_mesh(mesh)
    bm.free()
    mesh.update()

    obj = bpy.data.objects.new(f"QUARK_Compare_{mode}", mesh)
    bpy.context.collection.objects.link(obj)
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.shade_smooth()
    return obj


def retopologize(obj):
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    remesh = obj.modifiers.new(name="VoxelRemesh", type='REMESH')
    remesh.mode = 'VOXEL'
    remesh.voxel_size = 0.010  # coarser than the 0.004 production setting — no fingers/toes to
    # resolve here, and this is a silhouette check, not a bake source. Faster.
    remesh.adaptivity = 0.0
    remesh.use_smooth_shade = True
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    bpy.ops.object.shade_smooth()


def assign_simple_material(obj):
    """One flat-ish material for the whole mesh — this comparison is about silhouette/proportion,
    not panel or wear fidelity, so a full material breakup would only distract from the actual
    question being asked."""
    mat = bpy.data.materials.new("QUARK_CompareMat")
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs['Base Color'].default_value = (0.75, 0.78, 0.8, 1.0)
    bsdf.inputs['Roughness'].default_value = 0.5
    obj.data.materials.append(mat)


def add_marker_ring(z_cm, color, name):
    """A thin bright torus at a given height, purely a visual reference line for THIS comparison
    render (not part of the mesh/production asset) — the blockout's leg taper is continuous with
    no crease at the knee, so the silhouette alone doesn't show where each variant's knee height
    actually sits. Makes the numeric landmark difference legible instead of assumed-visible."""
    bpy.ops.mesh.primitive_torus_add(
        major_radius=0.12, minor_radius=0.004, location=(0, 0, z_cm * CM)
    )
    marker = bpy.context.active_object
    marker.name = name
    mat = bpy.data.materials.new(f"{name}_mat")
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs['Base Color'].default_value = (*color, 1.0)
    bsdf.inputs['Emission Color'].default_value = (*color, 1.0) if 'Emission Color' in bsdf.inputs else bsdf.inputs['Base Color'].default_value
    if 'Emission Strength' in bsdf.inputs:
        bsdf.inputs['Emission Strength'].default_value = 2.0
    marker.data.materials.append(mat)
    return marker


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
    scene.world = bpy.data.worlds.get("World") or bpy.data.worlds.new("World")
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs[0].default_value = (0.05, 0.065, 0.05, 1.0)

    for name, energy, rot in (
        ("KeyLight", 3.0, (55, 0, 35)),
        ("FillLight", 0.8, (65, 0, -120)),
        ("OverheadLight", 3.0, (80, 0, 0)),
        ("ProfileFillLight", 1.5, (25, 0, 90)),
    ):
        light = bpy.data.lights.new(name, type='SUN')
        light.energy = energy
        light_obj = bpy.data.objects.new(name, light)
        bpy.context.collection.objects.link(light_obj)
        light_obj.rotation_euler = tuple(math.radians(a) for a in rot)

    cam_data = bpy.data.cameras.new("TurnCam")
    cam_data.lens = 24
    cam_obj = bpy.data.objects.new("TurnCam", cam_data)
    bpy.context.collection.objects.link(cam_obj)
    scene.camera = cam_obj
    return cam_obj


def direction_to_euler(direction):
    import mathutils
    return mathutils.Vector(direction).normalized().to_track_quat('-Z', 'Y').to_euler()


def render_views(cam_obj, out_dir, prefix, views):
    target_z = 0.84
    cam_z = target_z + 0.15
    dist = 2.0
    for name, deg in views.items():
        rad = math.radians(deg)
        cam_obj.location = (dist * math.sin(rad), -dist * math.cos(rad), cam_z)
        direction = (0 - cam_obj.location[0], 0 - cam_obj.location[1], target_z - cam_obj.location[2])
        cam_obj.rotation_euler = direction_to_euler(direction)
        bpy.context.scene.render.filepath = os.path.join(out_dir, f"{prefix}_{name}.png")
        bpy.ops.render.render(write_still=True)


def build_and_render(mode, out_dir):
    clear_scene()
    obj = build_mesh(mode)
    retopologize(obj)
    assign_simple_material(obj)
    for poly in obj.data.polygons:
        poly.material_index = 0

    knee_z = CANON['knee_z'] if mode == 'canon' else ANTHRO['knee_z']
    hip_z = CANON['pelvis_z'] if mode == 'canon' else ANTHRO['pelvis_z']
    add_marker_ring(knee_z, (1.0, 0.15, 0.1), "KneeMarker")   # red = knee height
    add_marker_ring(hip_z, (0.1, 0.6, 1.0), "HipMarker")      # blue = hip height

    cam_obj = setup_render()
    render_views(cam_obj, out_dir, mode, {"front": 0, "three_quarter_left": 45, "left_side": 90})


def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
    out_dir = os.path.join(repo_root, "art", "quark-avatar", "renders", "anthro_compare")
    os.makedirs(out_dir, exist_ok=True)

    print(f"CANON landmarks: {CANON}")
    print(f"ANTHRO landmarks: {ANTHRO}")

    build_and_render('canon', out_dir)
    build_and_render('anthro', out_dir)

    print("Anthropometric comparison render complete ->", out_dir)


main()
