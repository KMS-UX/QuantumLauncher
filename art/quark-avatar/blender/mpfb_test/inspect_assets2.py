import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)
import sys
mpfb_mods = [m for m in sys.modules.keys() if 'mpfb' in m.lower()]
print("LOADED MPFB MODULES SAMPLE:", mpfb_mods[:10])

from bl_ext.blender_org.mpfb.services.locationservice import LocationService
import os

for asset_type in ["hair", "skins", "eyes", "eyebrows", "eyelashes", "teeth", "clothes", "poses", "proxymeshes", "materials"]:
    try:
        path = LocationService.get_user_data(asset_type)
        exists = os.path.isdir(path) if path else False
        print(f"{asset_type} user path: {path} exists={exists}")
        if exists:
            files = os.listdir(path)
            print(f"  contents ({len(files)}): {files[:25]}")
    except Exception as e:
        print(f"{asset_type} user path ERROR: {e}")
