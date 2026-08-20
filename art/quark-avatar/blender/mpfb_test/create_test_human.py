import bpy
import addon_utils
addon_utils.enable('bl_ext.blender_org.mpfb', default_set=True)

# clear scene
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)

bpy.ops.mpfb.create_human()

human = None
for obj in bpy.data.objects:
    if obj.type == 'MESH':
        human = obj
        break

print("OBJECTS:", [o.name for o in bpy.data.objects])
if human:
    print("HUMAN_NAME:", human.name)
    print("VERT_COUNT:", len(human.data.vertices))
    print("MATERIAL_SLOTS:", [m.name if m else None for m in human.data.materials])
    print("VERTEX_GROUPS:", [vg.name for vg in human.vertex_groups][:30])
    print("SHAPE_KEYS:", [sk.name for sk in human.data.shape_keys.key_blocks] if human.data.shape_keys else "NONE")

bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human.blend")
print("SAVED")
