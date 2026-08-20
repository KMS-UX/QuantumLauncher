import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.humanservice import HumanService

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_hair.blend")
human = bpy.data.objects.get("Human")

bpy.ops.object.select_all(action='DESELECT')
bpy.context.view_layer.objects.active = human
human.select_set(True)

HumanService.add_builtin_rig(human, "default", import_weights=True)

print("ALL_OBJECTS_AFTER_RIG:", [(o.name, o.type) for o in bpy.data.objects])
arm = None
for o in bpy.data.objects:
    if o.type == 'ARMATURE':
        arm = o
        break
if arm:
    print("ARM_NAME:", arm.name)
    print("BONE_NAMES:", sorted([b.name for b in arm.data.bones]))

bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_rigged.blend")
print("SAVED_RIGGED")
