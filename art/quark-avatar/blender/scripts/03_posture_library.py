"""
QUARK posture library — Phase 4, first pass.

Reference (`reference/QUARK_sideview_hologram.png`, "POSTURE & EMOTION STATES" row) defines 8
named states: Neutral(Idle), Focused(Scan), Happy(Positive), Warm(Supportive), Alert(Warn),
Speaking(Active), Thinking(Processing), Stealth(Dimmed). Cropped and inspected each thumbnail
individually (`reference` crops too small to read at full-sheet scale) before building anything —
7 of the 8 are the SAME relaxed standing pose (arms at sides), differing only by accent color and
a couple of state-specific VFX (Speaking's ripple rings, Stealth's dimmed opacity) that belong to
the live AGSL shader layer, not the baked mesh. Only Thinking has a genuinely different body
pose (hands clasped near the chin, elbows out). So this is a 2-pose library, not 8 — matches the
render-path decision's own reasoning ("avoids an N-postures x 3-hues baked-asset explosion").

Accent-color rule (Director decision, this session): every posture uses the live
PhosphorHueRuntime hue EXCEPT Alert, which is always CLAUDE.md's --warn red — these renders use
GREEN (the CLAUDE.md default hue) to represent "whatever hue is active" and RED for Alert, since
the actual hue selection happens live in-app, not baked here. The full 8-token sheet-accurate
palette (Info/Success/Warn/Alert/Stealth/Focus/Calm/Neutral, each its own fixed hex) remains a
later bonus variant, not built by this script.

Run headless (loads the existing baked quark_base.blend rather than rebuilding from scratch):
  blender --background quark_base.blend --python 03_posture_library.py
"""
import bpy
import math
import mathutils
import os

CM = 0.01


def rotate_bone_world(arm_obj, bone_name, axis, angle_deg):
    """Rotate a pose bone (and its children) by a WORLD-space rotation around its own CURRENT
    (already-posed) head position. Bone-local Euler rotation was tried first and produced wrong
    results (the shoulder/upperarm bones' local axes are tilted, not aligned to any world axis —
    confirmed by dumping `bone.matrix_local`'s columns — so a 'local Z rotation' swung the arm
    up-and-around instead of down-and-to-the-side). Working in world space sidesteps that
    entirely: rotating a T-pose arm (pointing along world +/-X) by 90 degrees around world Y
    reliably brings it to point along world -Z (down), regardless of the bone's own local-axis
    convention."""
    pb = arm_obj.pose.bones[bone_name]
    # Current (already-posed) head position, NOT the rest position — matters once a parent bone
    # has already been rotated (e.g. forearm after upperarm has moved): using the rest head here
    # pivots around where the elbow USED to be, not where it now is, and the child bone flies off
    # to the wrong place. Caught by exactly that symptom on the first Thinking-pose attempt.
    head_world = arm_obj.matrix_world @ pb.matrix.translation
    rot = mathutils.Matrix.Rotation(math.radians(angle_deg), 4, axis)
    mat = pb.matrix.copy()
    pivot = mathutils.Matrix.Translation(head_world)
    pivot_inv = mathutils.Matrix.Translation(-head_world)
    pb.matrix = pivot @ rot @ pivot_inv @ mat
    bpy.context.view_layer.update()


def set_pose_relaxed_idle():
    """Arms down at the sides, matching the reference's Neutral/Focused/Happy/Warm/Alert/
    Speaking/Stealth thumbnails — replaces the T-pose used for the turnaround/blockout renders.

    Rotates `upperarm_*`, not `shoulder_*` — first attempt rotated the shoulder (clavicle) bone,
    whose rest head sits at the body centerline (x=0), which swings the whole arm down through
    the torso's center instead of at the actual shoulder-joint offset. `upperarm_*`'s rest head is
    already at the correct shoulder-joint position (`build_armature()` in 01_base_mesh_and_rig.py
    parents it there), so rotating that bone instead pivots the arm from the right place — verified
    by rendering both and comparing (the centerline version had the arms fully hidden behind the
    torso silhouette from straight on; this version reads as a normal arms-at-sides stance)."""
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    for side, label in ((-1, "L"), (1, "R")):
        rotate_bone_world(arm_obj, f"upperarm_{label}", 'Y', 90 * side)
        rotate_bone_world(arm_obj, f"forearm_{label}", 'Y', 12 * side)  # small elbow bend
    bpy.ops.object.mode_set(mode='OBJECT')


def set_pose_thinking():
    """Hands raised toward the chin, elbows out and forward — the one posture with a real
    body-language difference from the relaxed-idle base, matching the reference's Thinking
    thumbnail. The mesh has no per-finger bones (fingers are static geometry on the hand bone),
    so this brings the hands to the chin/face area rather than attempting literal interlocked
    fingers, which isn't achievable with this rig."""
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    for side, label in ((-1, "L"), (1, "R")):
        # Bring the upper arm down most of the way (same rotation as relaxed-idle, smaller angle
        # so the elbow sits out from the body rather than flush against it), then bend sharply at
        # the elbow — rotating the forearm around world X (front-back tilt) swings it from
        # hanging-down to pointing forward-and-up, the way a real elbow bends toward the chest/
        # chin. World Y (the axis used for the shoulder-down rotation) mixes X/Z, which is right
        # for "swing side-to-side down"; world X mixes Y/Z, which is right for "swing forward",
        # and is only meaningful once the forearm itself is pointing mostly along Z (i.e. after
        # the down-swing) — applying it before that swing wouldn't do much, same reasoning as
        # why a T-pose arm barely responds to a world-Y rotation on the shoulder alone at first.
        rotate_bone_world(arm_obj, f"upperarm_{label}", 'Y', 65 * side)
        rotate_bone_world(arm_obj, f"forearm_{label}", 'X', -110 * side)
    bpy.ops.object.mode_set(mode='OBJECT')


def clear_pose():
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    bpy.ops.pose.select_all(action='SELECT')
    bpy.ops.pose.transforms_clear()
    bpy.ops.object.mode_set(mode='OBJECT')


def set_emissive_color(color, strength=7.0):
    mat = bpy.data.materials.get("QUARK_Emissive")
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    if "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*color, 1.0)
        bsdf.inputs["Emission Strength"].default_value = strength


def direction_to_euler(direction):
    vec = mathutils.Vector(direction).normalized()
    return vec.to_track_quat('-Z', 'Y').to_euler()


def render_presentation_shot(out_path, target_z=0.9):
    scene = bpy.context.scene
    cam = scene.camera
    dist = 2.4
    deg = 30  # 3/4-ish angle, matches how the reference examples read (mostly front, slight turn)
    rad = math.radians(deg)
    cam_z = target_z + 0.25
    cam.location = (dist * math.sin(rad), -dist * math.cos(rad), cam_z)
    direction = (0 - cam.location[0], 0 - cam.location[1], target_z - cam.location[2])
    cam.rotation_euler = direction_to_euler(direction)
    scene.render.filepath = out_path
    bpy.ops.render.render(write_still=True)


def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
    out_dir = os.path.join(repo_root, "art", "quark-avatar", "renders", "postures")
    os.makedirs(out_dir, exist_ok=True)

    scene = bpy.context.scene
    if scene.camera is None:
        cam_data = bpy.data.cameras.new("PostureCam")
        cam_data.lens = 35
        cam_obj = bpy.data.objects.new("PostureCam", cam_data)
        bpy.context.collection.objects.link(cam_obj)
        scene.camera = cam_obj
    scene.render.resolution_x = 1000
    scene.render.resolution_y = 1400

    GREEN = (0.0, 1.0, 0.0)       # CLAUDE.md default active hue — stands in for "live PhosphorHueRuntime"
    RED = (1.0, 0.11, 0.02)       # CLAUDE.md --warn, hardcoded exception regardless of active hue

    # relaxed-idle pose covers Neutral/Focused/Happy/Warm/Speaking/Stealth — those 6 states differ
    # from each other only by live accent-color/VFX (shader layer, not this mesh), so rendering
    # each as its own identical-pose, identical-color frame would be 6 duplicate images. One
    # green-accent frame stands in for all 6; the app wires each state to this same asset.
    set_pose_relaxed_idle()
    set_emissive_color(GREEN)
    render_presentation_shot(os.path.join(out_dir, "relaxed_idle_green.png"))
    print("Saved relaxed_idle_green (covers neutral/focused/happy/warm/speaking/stealth)")

    # Alert: same relaxed-idle pose, but the fixed --warn red exception, not the active hue
    set_emissive_color(RED)
    render_presentation_shot(os.path.join(out_dir, "relaxed_idle_alert_red.png"))
    print("Saved relaxed_idle_alert_red")

    # Thinking: the one posture with a real body-language difference
    set_emissive_color(GREEN)
    clear_pose()
    set_pose_thinking()
    render_presentation_shot(os.path.join(out_dir, "thinking_green.png"))
    print("Saved thinking_green")

    clear_pose()
    print("Posture library render pass complete.")


main()
