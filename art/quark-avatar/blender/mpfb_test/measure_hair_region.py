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
    xs = [p.x for p in pts]; ys = [p.y for p in pts]; zs = [p.z for p in pts]
    return {"n": len(pts), "x": (min(xs), max(xs)), "y": (min(ys), max(ys)), "z": (min(zs), max(zs))}

print("helper-hair:", group_bounds("helper-hair"))
