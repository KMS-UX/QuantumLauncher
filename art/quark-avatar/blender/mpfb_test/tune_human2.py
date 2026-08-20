import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.humanservice import HumanService
from bl_ext.blender_org.mpfb.services.targetservice import TargetService

bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)

macro = TargetService.get_default_macro_info_dict()
macro['gender'] = 0.0       # female
macro['age'] = 0.5          # young adult
macro['muscle'] = 0.6       # athletic/toned
macro['weight'] = 0.4       # lean
macro['proportions'] = 0.5
macro['height'] = 0.5       # start point, will measure
macro['cupsize'] = 0.5
macro['firmness'] = 0.6
macro['race'] = {'asian': 0.2, 'caucasian': 0.6, 'african': 0.2}

human = HumanService.create_human(
    mask_helpers=True, detailed_helpers=True, extra_vertex_groups=True,
    feet_on_ground=True, scale=0.1,  # scale=0.1 -> 1 blender unit = 1 meter directly (DECIMETER mode per createhuman.py)
    macro_detail_dict=macro
)

# measure actual height: bounding box top Z in world space
bpy.context.view_layer.update()
verts_world = [human.matrix_world @ v.co for v in human.data.vertices]
min_z = min(v.z for v in verts_world)
max_z = max(v.z for v in verts_world)
print(f"MEASURED_HEIGHT_M: {max_z - min_z:.4f}")
print(f"MIN_Z: {min_z:.4f} MAX_Z: {max_z:.4f}")
print(f"VERT_COUNT: {len(human.data.vertices)}")

bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
print("SAVED")
