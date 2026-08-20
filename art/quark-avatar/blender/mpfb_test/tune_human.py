import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.humanservice import HumanService

bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)

macro = {
    'gender': 0.0,   # 0=female per target filenames
    'age': 0.5,      # young adult
    'muscle': 0.6,   # athletic/toned
    'weight': 0.4,   # lean
    'proportions': 0.5,
    'height': 0.5,   # start point, will measure and adjust
    'cupsize': 0.5,
    'firmness': 0.6,
    'race': {'asian': 0.2, 'caucasian': 0.6, 'african': 0.2}
}
bpy.ops.mpfb.create_human(macro_detail_dict=None)  # first check default call still works with explicit dict below
