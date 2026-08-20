import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)

from mpfb.services.locationservice import LocationService
import os

for asset_type in ["hair", "skins", "eyes", "eyebrows", "eyelashes", "teeth", "clothes", "poses", "proxymeshes"]:
    try:
        path = LocationService.get_user_data(asset_type)
        print(f"{asset_type} user path: {path}")
    except Exception as e:
        print(f"{asset_type} user path ERROR: {e}")

# Try system/bundled asset locations
for asset_type in ["hair", "skins", "eyes", "eyebrows", "eyelashes"]:
    try:
        path = LocationService.get_system_data(asset_type)
        print(f"{asset_type} system path: {path}")
        if path and os.path.isdir(path):
            files = os.listdir(path)
            print(f"  contents ({len(files)}): {files[:20]}")
    except Exception as e:
        print(f"{asset_type} system path ERROR: {e}")
