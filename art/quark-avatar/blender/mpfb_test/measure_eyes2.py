import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")

def group_bounds(name, min_weight=0.3):
    vg = human.vertex_groups.get(name)
    if not vg: return None
    idx = vg.index
    pts = []
    for v in human.data.vertices:
        for g in v.groups:
            if g.group == idx and g.weight > min_weight:
                pts.append(human.matrix_world @ v.co)
    if not pts: return None
    n = len(pts)
    cx = sum(p.x for p in pts)/n
    cy = sum(p.y for p in pts)/n
    cz = sum(p.z for p in pts)/n
    return (cx, cy, cz, len(pts), min(p.y for p in pts), max(p.y for p in pts))

for jn in ["helper-l-eye", "helper-r-eye"]:
    print(jn, group_bounds(jn))
