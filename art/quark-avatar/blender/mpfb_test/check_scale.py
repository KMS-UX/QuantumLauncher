import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")
print("HUMAN_SCALE:", tuple(human.scale))
print("HUMAN_MATRIX_WORLD:", human.matrix_world)
