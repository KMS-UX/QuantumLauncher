import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.targetservice import TargetService
d = TargetService.get_default_macro_info_dict()
print("DEFAULT_MACRO_DICT:", d)
