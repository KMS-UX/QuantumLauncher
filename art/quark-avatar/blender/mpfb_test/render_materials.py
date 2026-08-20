import bpy, math

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_materials.blend")
scene = bpy.context.scene
human = bpy.data.objects.get("Human")
bpy.context.view_layer.objects.active = human
bpy.ops.object.shade_smooth()

def add_light(name, loc, energy, ltype='AREA', size=2.0):
    ld = bpy.data.lights.new(name=name, type=ltype)
    ld.energy = energy
    if ltype == 'AREA': ld.size = size
    o = bpy.data.objects.new(name, ld)
    o.location = loc
    scene.collection.objects.link(o)

add_light("Key", (1.2,-2.2,2.3), 900)
add_light("Fill", (-1.6,-1.6,1.6), 300)
add_light("Rim", (0.0,1.9,2.6), 450)

cam_data = bpy.data.cameras.new("Cam")
cam_obj = bpy.data.objects.new("Cam", cam_data)
scene.collection.objects.link(cam_obj)
cam_data.lens = 50
scene.camera = cam_obj

scene.render.engine = 'CYCLES'
scene.cycles.samples = 96
scene.render.resolution_x = 900
scene.render.resolution_y = 1500
world = bpy.data.worlds.new("W3")
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.05,0.05,0.06,1)
scene.world = world

# full body front
cam_obj.location = (0.0, -3.2, 0.9)
cam_obj.rotation_euler = (math.radians(90), 0, 0)
scene.render.filepath = "C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/mat_front.png"
bpy.ops.render.render(write_still=True)

# back view (spine conduit check)
cam_obj.location = (0.0, 3.2, 0.9)
cam_obj.rotation_euler = (math.radians(90), 0, math.radians(180))
scene.render.filepath = "C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/mat_back.png"
bpy.ops.render.render(write_still=True)

# face closeup
cam_obj.location = (0.0, -1.1, 1.55)
cam_obj.rotation_euler = (math.radians(90), 0, 0)
cam_data.lens = 85
scene.render.filepath = "C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/mat_face.png"
bpy.ops.render.render(write_still=True)

print("RENDERED")
