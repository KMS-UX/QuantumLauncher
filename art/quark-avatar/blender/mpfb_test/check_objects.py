import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
print("ALL_OBJECTS:", [(o.name, o.type) for o in bpy.data.objects])
