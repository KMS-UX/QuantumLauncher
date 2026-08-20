import bpy, math
from mathutils import Matrix

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_rigged.blend")
arm_obj = bpy.data.objects.get("Human.rig")

def rotate_bone_world(arm_obj, bone_name, axis, angle_deg):
    pb = arm_obj.pose.bones[bone_name]
    head_world = arm_obj.matrix_world @ pb.matrix.translation
    rot = Matrix.Rotation(math.radians(angle_deg), 4, axis)
    mat = pb.matrix.copy()
    pivot = Matrix.Translation(head_world)
    pivot_inv = Matrix.Translation(-head_world)
    pb.matrix = pivot @ rot @ pivot_inv @ mat
    bpy.context.view_layer.update()

bpy.context.view_layer.objects.active = arm_obj
bpy.ops.object.mode_set(mode='POSE')

for side, label in ((1, 'L'), (-1, 'R')):
    rotate_bone_world(arm_obj, f"upperarm01.{label}", 'Y', 70*side)
    rotate_bone_world(arm_obj, f"lowerarm01.{label}", 'X', -100*side)

bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_thinking.blend")
print("POSED_THINKING")
