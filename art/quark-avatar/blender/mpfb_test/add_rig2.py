import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
from bl_ext.blender_org.mpfb.services.humanservice import HumanService
import inspect
print("SIG:", inspect.signature(HumanService.add_builtin_rig))
