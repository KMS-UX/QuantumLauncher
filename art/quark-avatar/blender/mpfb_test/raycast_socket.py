import bpy, math
from mathutils import Vector

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")
scene = bpy.context.scene

cam_data = bpy.data.cameras.new("RCCam")
cam_obj = bpy.data.objects.new("RCCam", cam_data)
scene.collection.objects.link(cam_obj)
cam_obj.location = (0.0, -1.1, 1.55)
cam_obj.rotation_euler = (math.radians(90), 0, 0)
cam_data.lens = 85
scene.camera = cam_obj
bpy.context.view_layer.update()

render = scene.render
render.resolution_x = 900
render.resolution_y = 1500

import bpy_extras
# left socket hole (character's screen-left) approx pixel from final_face.png (900x1500): (345, 955)
# convert to normalized view coords (0..1, origin bottom-left for view3d, but for camera_view_frame we need NDC)
for (px, py, label) in [(345, 955, "left_socket_onscreen"), (595, 955, "right_socket_onscreen")]:
    ndc_x = px / render.resolution_x
    ndc_y = 1.0 - (py / render.resolution_y)  # flip Y

    frame = cam_data.view_frame(scene=scene)
    # frame corners in camera local space: [top-right, bottom-right, bottom-left, top-left] (roughly)
    top_left = frame[3]
    top_right = frame[0]
    bottom_left = frame[2]
    # bilinear interpolate
    local_point = top_left + (top_right - top_left) * ndc_x + (bottom_left - top_left) * (1 - ndc_y)
    world_point = cam_obj.matrix_world @ local_point
    origin = cam_obj.matrix_world.translation
    direction = (world_point - origin).normalized()

    result, loc, normal, idx, obj, mat = scene.ray_cast(bpy.context.view_layer.depsgraph, origin, direction)
    print(label, "HIT:", result, "LOC:", loc if result else None)
