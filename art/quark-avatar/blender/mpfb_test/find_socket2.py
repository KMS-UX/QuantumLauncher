import bpy, bmesh
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")
mesh = human.data

bm = bmesh.new()
bm.from_mesh(mesh)
bm.edges.ensure_lookup_table()

boundary_verts_idx = set()
for e in bm.edges:
    if len(e.link_faces) <= 1:
        boundary_verts_idx.add(e.verts[0].index)
        boundary_verts_idx.add(e.verts[1].index)

pts = []
for vi in boundary_verts_idx:
    v = mesh.vertices[vi]
    w = human.matrix_world @ v.co
    if w.z > 1.35 and abs(w.x) < 0.15:
        pts.append((w.x, w.y, w.z))

print("BOUNDARY_PTS_NEAR_FACE:", len(pts))
left = [p for p in pts if p[0] > 0.005]
right = [p for p in pts if p[0] < -0.005]
def centroid(pts):
    n = len(pts)
    if n == 0: return None
    return (sum(p[0] for p in pts)/n, sum(p[1] for p in pts)/n, sum(p[2] for p in pts)/n)
print("LEFT_SOCKET_CENTROID:", centroid(left), "n=", len(left))
print("RIGHT_SOCKET_CENTROID:", centroid(right), "n=", len(right))
bm.free()
