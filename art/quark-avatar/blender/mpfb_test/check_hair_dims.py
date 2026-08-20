import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_hair.blend")
for name in ["Hair_Cap", "Hair_Bun", "Hair_LockL"]:
    obj = bpy.data.objects.get(name)
    if obj:
        print(name, "location:", tuple(obj.location), "scale:", tuple(obj.scale), "dimensions:", tuple(obj.dimensions), "parent:", obj.parent.name if obj.parent else None, "matrix_world translation:", tuple(obj.matrix_world.translation))
