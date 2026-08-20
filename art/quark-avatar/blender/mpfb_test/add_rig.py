import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.rigservice import RigService
from bl_ext.blender_org.mpfb.services.objectservice import ObjectService

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_hair.blend")
human = bpy.data.objects.get("Human")

bpy.ops.object.select_all(action='DESELECT')
bpy.context.view_layer.objects.active = human
human.select_set(True)

import inspect
print("add_standard_rig sig or class check")
