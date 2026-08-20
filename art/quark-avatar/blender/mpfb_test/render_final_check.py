import bpy, math

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_thinking.blend")
scene = bpy.context.scene
human = bpy.data.objects.get("Human")

# restore proper emissive strength on the QUARK_Emissive material
em = bpy.data.materials.get("QUARK_Emissive")
if em:
    bsdf = em.node_tree.nodes.get("Principled BSDF")
    if bsdf and "Emission Strength" in bsdf.inputs:
        bsdf.inputs["Emission Strength"].default_value = 4.0
        bsdf.inputs["Base Color"].default_value = (0.902, 0.945, 1.0, 1.0)

bpy.context.view_layer.objects.active = human
bpy.ops.object.shade_smooth()
for eye in (bpy.data.objects.get("Eye_L"), bpy.data.objects.get("Eye_R")):
    if eye:
        bpy.context.view_layer.objects.active = eye
        bpy.ops.object.shade_smooth()

def add_light(name, loc, energy, ltype='AREA', size=2.0):
    ld = bpy.data.lights.new(name=name, type=ltype)
    ld.energy = energy
    if ltype == 'AREA': ld.size = size
    o = bpy.data.objects.new(name, ld)
    o.location = loc
    scene.collection.objects.link(o)

add_light("Key", (1.2,-2.2,2.3), 500)
add_light("Fill", (-1.6,-1.6,1.6), 200)
add_light("Rim", (0.0,1.9,2.6), 300)

cam_data = bpy.data.cameras.new("Cam")
cam_obj = bpy.data.objects.new("Cam", cam_data)
scene.collection.objects.link(cam_obj)
cam_data.lens = 50
scene.camera = cam_obj

scene.render.engine = 'CYCLES'
scene.cycles.samples = 128
scene.render.resolution_x = 900
scene.render.resolution_y = 1500
world = bpy.data.worlds.new("W4")
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.05,0.05,0.06,1)
scene.world = world

cam_obj.location = (0.0, -3.2, 0.9)
cam_obj.rotation_euler = (math.radians(90), 0, 0)
scene.render.filepath = "C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/thinking_front.png"
bpy.ops.render.render(write_still=True)

cam_obj.location = (0.0, -1.1, 1.55)
cam_obj.rotation_euler = (math.radians(90), 0, 0)
cam_data.lens = 85
scene.render.filepath = "C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/thinking_face.png"
bpy.ops.render.render(write_still=True)

print("RENDERED")
