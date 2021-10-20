
package com.gmoutzou.musar.app.video.VideoPlayback;

public class VideoPlaybackShaders
{
    
    public static final String VIDEO_PLAYBACK_VERTEX_SHADER = " \n"
        + "attribute vec4 vertexPosition; \n"
        + "attribute vec4 vertexNormal; \n"
        + "attribute vec2 vertexTexCoord; \n"
        + "varying vec2 texCoord; \n"
        + "varying vec4 normal; \n"
        + "uniform mat4 modelViewProjectionMatrix; \n"
        + "\n"
        + "void main() \n"
        + "{ \n"
        + "   gl_Position = modelViewProjectionMatrix * vertexPosition; \n"
        + "   normal = vertexNormal; \n" + "   texCoord = vertexTexCoord; \n"
        + "} \n";
    
    public static final String VIDEO_PLAYBACK_FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require \n" +
        "precision mediump float; \n" +
        "uniform samplerExternalOES texSamplerOES; \n" +
        "   varying vec2 texCoord;\n" +
        "   varying vec2 texdim0;\n" +
        "   void main()\n\n" +
        "   {\n" +
        "       vec3 keying_color = vec3(0.0823,1,0.1725);\n" +
        "       float thresh = 0.45; // [0, 1.732]\n" +
        "       float slope = 0.1; // [0, 1]\n" +
        "       vec3 input_color = texture2D(texSamplerOES, texCoord).rgb;\n" +
        "       float d = abs(length(abs(keying_color.rgb - input_color.rgb)));\n" +
        "       float edge0 = thresh * (1.0 - slope);\n" +
        "       float alpha = smoothstep(edge0, thresh, d);\n" +
        "       gl_FragColor = vec4(input_color,alpha);\n" +
        "   }";
    
}
