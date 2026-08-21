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

Ported to the MPFB-based mesh/rig (Phase 4c, `01_base_mesh_and_rig.py`'s rewrite) -- bone names
updated to MPFB's "default" standard rig, and `set_pose_relaxed_idle()` is now a no-op since that
mesh's own rest pose is already the relaxed A-pose this function used to rotate a T-pose into. See
that script's own header and PRODUCTION_LOG.md's Phase 4c entry for the full rationale.
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


ARM_TUCK_DEG = 20.0     # how far to bring the upper arms in from the MPFB rest A-pose


def set_pose_relaxed_idle():
    """Bring the arms IN toward the body from MPFB's rest A-pose.

    Previously a no-op, on the reasoning that the rest pose was already "a relaxed A-pose". Checked
    against the reference sheet's own POSTURE & EMOTION STATES row (cropped and enlarged --
    `renders/ref_poseA.png`): every standing state there (Neutral/Focused/Happy/Warm/Alert/Speaking/
    Stealth) has the arms hanging close to the body in a narrow silhouette, noticeably tighter than
    MPFB's rest A-pose, which splays them ~45 degrees out. The hi-res prototype art agrees. So the
    rest pose is NOT the reference pose, and leaving this as a no-op was reading the sheet too
    loosely.

    Rotating around world Y with the sign flipped per side mirrors correctly (see
    `set_pose_thinking` for the mirror-math note); a negative angle lowers/tucks where the positive
    angle used for Thinking raises."""
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    for side, label in ((1, "L"), (-1, "R")):
        rotate_bone_world(arm_obj, f"upperarm01.{label}", 'Y', ARM_TUCK_DEG * side)
    bpy.ops.object.mode_set(mode='OBJECT')


def set_pose_thinking():
    """Hands raised toward the chin, elbows out and forward — the one posture with a real
    body-language difference from the relaxed-idle base, matching the reference's Thinking
    thumbnail. Bone names ported to MPFB's "default" standard rig (`upperarm01.L/R`,
    `lowerarm01.L/R` -- confirmed by inspecting the actually-created armature, not the rig JSON
    template alone). Angles retuned for this rig's different rest pose (A-pose, not T-pose) and
    confirmed by rendering: 70/-100 degrees reads as a reasonable "hand near chin" gesture on this
    mesh, vs. the old blockout's 65/-110 tuned for its own T-pose rest. The mesh has no per-finger
    bones (fingers are static geometry on the hand), so this brings the hands to the chin/face area
    rather than attempting literal interlocked fingers."""
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    # Mirroring a world-space rotation across the YZ plane flips the sign of Y/Z-axis rotations
    # but PRESERVES X-axis rotation sign (M*Rx(theta)*M^-1 = Rx(theta), for M = diag(-1,1,1)) --
    # so the forearm's X-axis rotation must NOT be scaled by `side` the way the upperarm's Y-axis
    # rotation is. The previous `-100 * side` was the actual cause of the asymmetric "one arm to
    # the chin, the other out to the hip" pose flagged (not fixed) in PRODUCTION_LOG's Phase 4c
    # entry -- confirmed by this sign analysis, not just re-tuned by eye.
    # NOTE: superseded by `set_pose_thinking_ik()` -- kept only for reference. Hand-guessed
    # world-axis rotations could not actually land the hand on the chin (the arm swung out to the
    # side instead), which is the whole reason the IK version below exists.
    #
    # Director's direction: a single hand to the chin, Rodin's "The Thinker", rather than the
    # symmetric two-handed gesture this used to produce.
    #
    # Recorded discrepancy, not silently resolved: the reference sheet's own THINKING (Processing)
    # thumbnail actually shows STEEPLED HANDS TOGETHER at chest height, not a hand on the chin
    # (see `renders/ref_poseB.png`, the enlarged crop). The Director asked for the Thinker pose
    # explicitly, so that is what is built here; the sheet's version is a one-line change back
    # (mirror this to both sides and drop the tuck) if that reading is preferred later.
    #
    # Mirror-math note retained from the earlier fix: reflecting a world-space rotation across the
    # YZ plane flips the sign of Y/Z-axis rotations but PRESERVES X-axis rotations
    # (M*Rx(t)*M^-1 = Rx(t)), which is why only the upperarm's Y angle is scaled by `side`.
    THINK_SIDE, THINK_LABEL = -1, "R"          # right hand goes to the chin
    rotate_bone_world(arm_obj, f"upperarm01.{THINK_LABEL}", 'Y', 62 * THINK_SIDE)
    rotate_bone_world(arm_obj, f"lowerarm01.{THINK_LABEL}", 'X', -104)
    # ...and the supporting arm tucks in across the body rather than hanging in the rest A-pose,
    # which is what the standing-Thinker silhouette reads as.
    rotate_bone_world(arm_obj, "upperarm01.L", 'Y', ARM_TUCK_DEG)
    rotate_bone_world(arm_obj, "lowerarm01.L", 'X', -32)
    bpy.ops.object.mode_set(mode='OBJECT')



def _ik_reach(arm_obj, tip_bone, target_co, chain_count=4, pole_co=None, pole_angle=0.0):
    """Pose an arm chain by INVERSE KINEMATICS to put `tip_bone`'s tail at `target_co`, then bake
    the result into plain bone rotations and remove the rig it needed.

    Hand-authored world-axis rotations were tried first for the Thinker pose and could not land the
    hand on the chin -- the arm swung out to the side instead, because a single world-axis rotation
    per bone cannot express "reach that point" on a chain whose rest orientation is already tilted
    in three axes. IK solves the reach directly, which is what it is for. The constraint and its
    target empty are deleted after `visual_transform_apply()` bakes the solved pose, so the saved
    posture is ordinary bone rotation data with no leftover dependencies."""
    tgt = bpy.data.objects.new("IK_TGT", None)
    bpy.context.collection.objects.link(tgt)
    tgt.location = target_co
    pole = None
    if pole_co is not None:
        pole = bpy.data.objects.new("IK_POLE", None)
        bpy.context.collection.objects.link(pole)
        pole.location = pole_co

    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    pb = arm_obj.pose.bones[tip_bone]
    con = pb.constraints.new('IK')
    con.target = tgt
    con.chain_count = chain_count
    if pole is not None:
        con.pole_target = pole
        con.pole_angle = math.radians(pole_angle)
    bpy.context.view_layer.update()

    bpy.ops.pose.select_all(action='SELECT')
    bpy.ops.pose.visual_transform_apply()
    pb.constraints.remove(con)
    bpy.ops.object.mode_set(mode='OBJECT')
    bpy.data.objects.remove(tgt, do_unlink=True)
    if pole is not None:
        bpy.data.objects.remove(pole, do_unlink=True)


def set_pose_thinking_ik():
    """Rodin's "The Thinker": the right hand comes up to the chin, the left arm tucks across the
    body. Director-specified.

    Discrepancy recorded rather than silently resolved: the reference sheet's own THINKING
    (Processing) thumbnail shows STEEPLED HANDS TOGETHER at chest height, not a hand on the chin
    (see `renders/ref_poseB.png`). The Director asked for the Thinker reading explicitly, so that is
    what is built; the sheet's version is a small change away if preferred.

    The chin target is MEASURED off the evaluated head mesh (front-most low point of the head
    region, ~(0, -0.13, 1.39)) rather than guessed, and offset slightly to the reaching side and
    forward so the knuckles sit under the jaw instead of intersecting it."""
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    # Right hand up under the chin. Target/pole/wrist values are the "D" variant of a rendered
    # A/B/C/D sweep -- each was rendered and looked at, not reasoned about. The earlier attempts are
    # worth recording because each failed for a specific reason:
    #   A/B  wrist reached chin HEIGHT but the hand sat beside the neck, palm to camera -- reads as
    #        a wave, because IK positions the wrist and says nothing about hand ORIENTATION.
    #   C    wrist rotated (Z-55, X-25); fingers turned up toward the face but still short of it.
    #   D    wrist rotated (Z-80, Y+30) with the target pulled in and down -- knuckles land under
    #        the jaw. This is the shipped pose.
    _ik_reach(arm_obj, "lowerarm02.R", (-0.030, -0.150, 1.290), chain_count=4,
              pole_co=(-0.40, -0.40, 0.90))
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    # IK solves the REACH; it cannot express how the hand is turned. Without these two the hand
    # arrives palm-out and the pose reads as "talk to the hand" rather than contemplation.
    rotate_bone_world(arm_obj, "wrist.R", 'Z', -80)
    rotate_bone_world(arm_obj, "wrist.R", 'Y', 30)
    # supporting arm tucked in across the body
    rotate_bone_world(arm_obj, "upperarm01.L", 'Y', ARM_TUCK_DEG)
    rotate_bone_world(arm_obj, "lowerarm01.L", 'X', -38)
    bpy.ops.object.mode_set(mode='OBJECT')


def clear_pose():
    arm_obj = bpy.data.objects.get("QUARK_Rig")
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='POSE')
    bpy.ops.pose.select_all(action='SELECT')
    bpy.ops.pose.transforms_clear()
    bpy.ops.object.mode_set(mode='OBJECT')


def srgb_to_linear(color):
    """`color` args to this function are UI/display hex tokens (CLAUDE.md's GREEN/RED, eventually
    the full 8-token palette) -- sRGB-space values. Blender's node-socket `default_value` is
    interpreted as LINEAR, not sRGB, so feeding a display hex straight in and then multiplying by
    a large emission strength is a gamma mismatch. It's invisible for channel-pure colors (0 and 1
    map to themselves under either curve) -- which is why GREEN (0,1,0) and a BLUE (0,0,1) probe
    both rendered correctly saturated -- but RED = (1.0, 0.11, 0.02) has a non-trivial 0.11 G
    channel that isn't pure zero. At strength 7.0 that scales to 0.77 linear, gamma-ENCODED (not
    decoded) back up to ~0.94 for display -- comparable to R's own clipped 1.0 -- so the accent
    rendered as yellow, not red. Confirmed by direct pixel measurement, not assumed: same
    coordinate read (1.0, 0.94, 0.44) for "red" vs. the expected saturated red, reproduced even
    from a fresh Blender process (rules out a stale-GI-cache theory tried first). Decoding here
    keeps the small secondary channel small after the strength multiply instead of blowing it up."""
    def ch(v):
        return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4
    return tuple(ch(v) for v in color)


def set_emissive_color(color, strength=7.0):
    color = srgb_to_linear(color)
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
    dist = 6.0  # was 2.4 @ effectively-24mm; scaled by the 60/24 lens-change ratio to hold framing
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
        cam_data.lens = 60
        cam_obj = bpy.data.objects.new("PostureCam", cam_data)
        bpy.context.collection.objects.link(cam_obj)
        scene.camera = cam_obj
    else:
        # This script loads the already-saved quark_base.blend, which always has TurnCam as
        # scene.camera by the time this runs -- so the `is None` branch above was dead code and
        # this render was silently inheriting TurnCam's lens (24mm pre-Tier-2) rather than the
        # 35mm this file's own comment claimed. Set explicitly so the intended focal length is
        # actually what's used, regardless of which camera object is active.
        scene.camera.data.lens = 60
    # Tier 2 resolution: scaled by the same ~1.5x as setup_render()'s turnaround resolution.
    scene.render.resolution_x = 1500
    scene.render.resolution_y = 2100

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
    set_pose_thinking_ik()
    render_presentation_shot(os.path.join(out_dir, "thinking_green.png"))
    print("Saved thinking_green")

    clear_pose()
    print("Posture library render pass complete.")


main()
