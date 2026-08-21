"""
QUARK glTF/GLB exporter -- Phase 5, the Option B (real-time 3D via SceneView) track.

Turns the authored `quark_base.blend` into a single skinned GLB that Filament/SceneView can load
on Android. Two things make this non-trivial and they are the whole reason this script exists
rather than a one-line `bpy.ops.export_scene.gltf()`:

1. **`export_apply=True` cannot be combined with shape keys.** `QUARK_Base` carries 12 macro shape
   keys (MakeHuman encodes the entire Director-specified phenotype as a shape-key mix -- height,
   gender, muscle, weight, breast size). Naively calling `shape_key_remove(all=True)` reverts the
   body to MakeHuman's neutral basis and throws the phenotype away. The keys must be BAKED, not
   removed.
2. **Modifiers must be applied, but not the ARMATURE one.** `QUARK_Base` has a MASK ("Hide
   helpers") that -- if not applied -- ships MakeHuman's joint-cube helper geometry into the GLB;
   `QUARK_Armor` has SOLIDIFY+BEVEL without which the plates export as zero-thickness sheets; the
   hair/eyes/brows/lashes carry SUBSURF. But applying ARMATURE would bake the rest pose into the
   vertices and destroy the skinning that is the entire point of shipping a rigged GLB.

`bake_down()` solves both at once: it temporarily hides the ARMATURE modifier, asks the depsgraph
for the fully-evaluated mesh (shape-key mix and every other modifier resolved into real vertex
coordinates), swaps that in as the object's mesh data with `preserve_all_data_layers=True` so the
deform vertex groups survive, then re-creates the ARMATURE modifier. Afterwards nothing is left to
apply and the export runs with `export_apply=False`.

Run headless:
  blender --background quark_base.blend --python 04_export_gltf.py -- [--out PATH] [--no-draco]
"""
import bpy
import os
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ART_DIR = os.path.dirname(SCRIPT_DIR)                      # art/quark-avatar/blender
DEFAULT_OUT = os.path.join(os.path.dirname(ART_DIR), "export", "quark.glb")

RIG_NAME = "QUARK_Rig"


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    out = DEFAULT_OUT
    draco = True
    if "--out" in argv:
        out = argv[argv.index("--out") + 1]
    if "--no-draco" in argv:
        draco = False
    return out, draco


def strip_non_export_objects():
    """Lights and the turnaround camera are authoring-time scene furniture -- the Android side
    lights the scene itself (Filament IBL + LightNodes), so shipping Blender's five area lights
    would double-light everything."""
    removed = []
    for obj in list(bpy.data.objects):
        if obj.type in {'LIGHT', 'CAMERA'}:
            removed.append(obj.name)
            bpy.data.objects.remove(obj, do_unlink=True)
    print(f"[export] dropped non-export objects: {removed}")


def bake_down(obj, rig):
    """Resolve shape keys + every modifier EXCEPT ARMATURE into real mesh data.

    Returns a (name, before, after) report tuple so the caller can print measured vertex counts
    rather than asserting the bake 'probably worked'."""
    before = len(obj.data.vertices)
    n_keys = len(obj.data.shape_keys.key_blocks) if obj.data.shape_keys else 0

    # Record the armature modifier's settings, then hide it so the depsgraph evaluation below
    # does NOT bake the rest-pose deform into the vertex coordinates.
    arm_mods = [m for m in obj.modifiers if m.type == 'ARMATURE']
    arm_specs = [(m.name, m.object, m.use_vertex_groups, m.use_deform_preserve_volume) for m in arm_mods]
    for m in arm_mods:
        m.show_viewport = False

    depsgraph = bpy.context.evaluated_depsgraph_get()
    obj_eval = obj.evaluated_get(depsgraph)
    # preserve_all_data_layers=True is what carries the deform vertex groups (and UVs) through to
    # the new mesh; without it the re-added ARMATURE modifier would have nothing to bind to.
    new_mesh = bpy.data.meshes.new_from_object(
        obj_eval, preserve_all_data_layers=True, depsgraph=depsgraph
    )
    new_mesh.name = obj.data.name + "_baked"

    old_mesh = obj.data
    obj.modifiers.clear()
    obj.data = new_mesh
    if old_mesh.users == 0:
        bpy.data.meshes.remove(old_mesh)

    for name, arm_obj, use_vg, preserve in arm_specs:
        m = obj.modifiers.new(name=name, type='ARMATURE')
        m.object = arm_obj or rig
        m.use_vertex_groups = use_vg
        m.use_deform_preserve_volume = preserve

    after = len(obj.data.vertices)
    return (obj.name, before, after, n_keys, len(arm_specs))


# -------------------------------------------------------------------------------------------------
# glTF-safe materials
# -------------------------------------------------------------------------------------------------
# MEASURED, not assumed: the first export ran with the authoring materials untouched and the
# re-imported GLB rendered ENTIRELY FLAT WHITE (`renders/glb_verify_front.png`). The GLB JSON
# confirmed why -- QUARK_Ceramic/UnderSuit/Graphite/MetalAlloy all came out with
# `baseColorFactor: None` (i.e. glTF's default 1,1,1,1). Those materials drive Base Color and
# Roughness from Noise/Voronoi/Wave/Geometry node chains, and glTF has no representation for a
# procedural shader graph, so the exporter silently drops the link and ships the socket default.
#
# So the procedural chains are collapsed to constants BEFORE export. The constants are not
# invented: each material's own first RGB node supplies the base colour and its own
# Roughness-linked colour ramp supplies the roughness (averaged over the ramp, since what the
# noise does at runtime is pick a point along it). Clearcoat, metallic and IOR already export
# correctly and are left alone.
#
# This is a stopgap with a known cost, stated plainly: the per-pixel ceramic mottling, brushed-
# metal anisotropy and panel grain are LOST, leaving flat shaded plates. The real fix is a bake
# pass to base-colour/ORM/normal textures -- see PRODUCTION_LOG's Phase 5 entry.
FLATTEN_MATERIALS = (
    "QUARK_Ceramic", "QUARK_UnderSuit", "QUARK_Graphite", "QUARK_MetalAlloy",
)


def flatten_materials_for_gltf():
    report = []
    for name in FLATTEN_MATERIALS:
        mat = bpy.data.materials.get(name)
        if mat is None or not mat.node_tree:
            continue
        nt = mat.node_tree
        bsdf = next((n for n in nt.nodes if n.type == 'BSDF_PRINCIPLED'), None)
        if bsdf is None:
            continue

        rgb_nodes = [n for n in nt.nodes if n.type == 'RGB']
        base = tuple(rgb_nodes[0].outputs[0].default_value) if rgb_nodes else (1.0, 1.0, 1.0, 1.0)

        rough_ramp = next(
            (n for n in nt.nodes if n.type == 'VALTORGB'
             and any(l.to_socket.name == 'Roughness' for l in n.outputs[0].links)),
            None,
        )
        if rough_ramp is not None:
            els = rough_ramp.color_ramp.elements
            rough = sum(e.color[0] for e in els) / len(els)
        else:
            rough = bsdf.inputs['Roughness'].default_value

        for socket_name, value in (('Base Color', base), ('Roughness', rough)):
            sock = bsdf.inputs[socket_name]
            for link in list(sock.links):
                nt.links.remove(link)
            sock.default_value = value

        report.append((name, tuple(round(c, 3) for c in base), round(rough, 3)))
    print(f"[export] flattened procedural materials to glTF constants: {report}")


# -------------------------------------------------------------------------------------------------
# Skinning the accessories
# -------------------------------------------------------------------------------------------------
# Also measured: the first GLB shipped hair, eyes, eyebrows, eyelashes, the headband and the spine
# conduit with `skin: None`. They are parented to QUARK_Base / QUARK_Rig as OBJECTS, which the
# Blender viewport honours but glTF's node hierarchy does not reproduce for a skinned parent --
# a skinned mesh deforms its own vertices and its children do not follow. On device the head would
# turn and the hair, eyes and headband would stay behind. Each gets a single full-weight vertex
# group on the bone it rigidly belongs to, which is exactly the rigid-parent behaviour expressed
# in a form glTF can carry.
RIGID_BINDINGS = {
    "QUARK_Base.ponytail01": "head",
    "QUARK_Base.high-poly": "head",
    "QUARK_Base.eyebrow008": "head",
    "QUARK_Base.eyelashes01": "head",
    "QUARK_Headband": "head",
    "QUARK_SpineConduit": "spine03",
}


def rigid_bind(obj, rig, bone_name):
    assert bone_name in rig.data.bones, f"bone {bone_name} missing from {rig.name}"
    vg = obj.vertex_groups.get(bone_name) or obj.vertex_groups.new(name=bone_name)
    vg.add(range(len(obj.data.vertices)), 1.0, 'REPLACE')
    if not any(m.type == 'ARMATURE' for m in obj.modifiers):
        m = obj.modifiers.new(name="Armature", type='ARMATURE')
        m.object = rig
    # The object must be parented to the ARMATURE (not to QUARK_Base) or the exporter writes the
    # skinned mesh under a deforming parent and the transforms compound.
    if obj.parent is not rig:
        world = obj.matrix_world.copy()
        obj.parent = rig
        obj.matrix_parent_inverse = rig.matrix_world.inverted()
        obj.matrix_world = world


def main():
    out_path, draco = parse_args()
    os.makedirs(os.path.dirname(out_path), exist_ok=True)

    rig = bpy.data.objects.get(RIG_NAME)
    assert rig is not None, f"{RIG_NAME} not found in scene"
    assert rig.type == 'ARMATURE'
    print(f"[export] rig {RIG_NAME}: {len(rig.data.bones)} bones")

    strip_non_export_objects()

    print("[export] bake-down report (name, verts_before -> verts_after, shape_keys_baked, armature_mods):")
    for obj in [o for o in bpy.data.objects if o.type == 'MESH']:
        print("   ", bake_down(obj, rig))

    flatten_materials_for_gltf()

    for obj_name, bone_name in RIGID_BINDINGS.items():
        obj = bpy.data.objects.get(obj_name)
        assert obj is not None, f"{obj_name} missing -- RIGID_BINDINGS is stale"
        rigid_bind(obj, rig, bone_name)
    print(f"[export] rigid-bound accessories: {list(RIGID_BINDINGS.items())}")

    for obj in bpy.data.objects:
        if obj.type == 'MESH':
            assert any(m.type == 'ARMATURE' for m in obj.modifiers), \
                f"{obj.name} would export with skin=None and would not follow the rig"

    for obj in bpy.data.objects:
        if obj.type == 'MESH':
            assert obj.data.shape_keys is None, f"{obj.name} still has shape keys"
            leftover = [m.type for m in obj.modifiers if m.type != 'ARMATURE']
            assert not leftover, f"{obj.name} still has modifiers {leftover}"

    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.export_scene.gltf(
        filepath=out_path,
        export_format='GLB',
        export_apply=False,          # everything already baked down above
        export_skins=True,
        export_animations=True,      # no actions authored yet; harmless, future-proofs the call
        export_yup=True,
        export_image_format='AUTO',
        export_draco_mesh_compression_enable=draco,
        export_draco_mesh_compression_level=6,
        use_selection=False,
    )

    size_mb = os.path.getsize(out_path) / (1024 * 1024)
    print(f"[export] WROTE {out_path}  ({size_mb:.2f} MB, draco={draco})")


main()
