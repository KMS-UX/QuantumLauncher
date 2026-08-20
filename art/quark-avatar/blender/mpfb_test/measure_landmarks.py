import bpy

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")

joint_groups = [vg.name for vg in human.vertex_groups if vg.name.startswith("joint-")]
print("ALL_JOINT_GROUPS:", sorted(joint_groups))

def group_center_z(name):
    vg = human.vertex_groups.get(name)
    if not vg:
        return None
    idx = vg.index
    zs = []
    xs = []
    for v in human.data.vertices:
        for g in v.groups:
            if g.group == idx and g.weight > 0.4:
                world = human.matrix_world @ v.co
                zs.append(world.z)
                xs.append(world.x)
    if not zs:
        return None
    return (sum(zs)/len(zs), min(zs), max(zs), sum(xs)/len(xs) if xs else 0)

for jn in ["joint-ground", "joint-head", "joint-neck", "joint-l-clavicle", "joint-l-shoulder",
           "joint-l-elbow", "joint-l-wrist", "joint-l-hip", "joint-l-knee", "joint-l-ankle",
           "joint-pelvis", "joint-spine2", "joint-spine3"]:
    r = group_center_z(jn)
    print(f"{jn}: {r}")

print("--- additional ---")
for jn in ["joint-spine-1","joint-spine-2","joint-spine-3","joint-spine-4","joint-l-hand",
           "joint-l-scapula","joint-mouth","joint-l-upper-leg","joint-l-foot-1"]:
    r = group_center_z(jn)
    print(f"{jn}: {r}")
