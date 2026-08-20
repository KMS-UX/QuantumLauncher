import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")

def group_center(name):
    vg = human.vertex_groups.get(name)
    if not vg: return None
    idx = vg.index
    pts = []
    for v in human.data.vertices:
        for g in v.groups:
            if g.group == idx and g.weight > 0.3:
                pts.append(human.matrix_world @ v.co)
    if not pts: return None
    n = len(pts)
    return (sum(p.x for p in pts)/n, sum(p.y for p in pts)/n, sum(p.z for p in pts)/n)

for jn in ["joint-l-eye", "joint-r-eye", "joint-l-eye-target", "joint-r-eye-target"]:
    print(jn, group_center(jn))
