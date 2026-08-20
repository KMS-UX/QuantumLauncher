import bpy
import math

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human.blend")

scene = bpy.context.scene
human = bpy.data.objects.get("Human")

# smooth shading for a fair look at topology quality
bpy.context.view_layer.objects.active = human
bpy.ops.object.shade_smooth()

# basic 3-point-ish lighting
def add_light(name, loc, energy, ltype='AREA', size=2.0):
    light_data = bpy.data.lights.new(name=name, type=ltype)
    light_data.energy = energy
    if ltype == 'AREA':
        light_data.size = size
    obj = bpy.data.objects.new(name, light_data)
    obj.location = loc
    scene.collection.objects.link(obj)
    return obj

add_light("Key", (1.2, -2.0, 2.2), 800)
add_light("Fill", (-1.5, -1.5, 1.5), 300)
add_light("Rim", (0.0, 1.8, 2.5), 400)

# camera framing the head/upper body
cam_data = bpy.data.cameras.new("Cam")
cam_obj = bpy.data.objects.new("Cam", cam_data)
scene.collection.objects.link(cam_obj)
cam_obj.location = (0.0, -2.6, 1.55)
cam_obj.rotation_euler = (math.radians(88), 0, 0)
cam_data.lens = 85
scene.camera = cam_obj

scene.render.engine = 'CYCLES'
scene.cycles.samples = 64
scene.render.resolution_x = 900
scene.render.resolution_y = 1100
scene.render.film_transparent = False
world = bpy.data.worlds.new("W")
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.05,0.05,0.06,1)
scene.world = world

scene.render.filepath = "C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/human_face_test.png"
bpy.ops.render.render(write_still=True)
print("RENDERED")
