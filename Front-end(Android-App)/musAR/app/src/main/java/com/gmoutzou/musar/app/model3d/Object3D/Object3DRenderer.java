package com.gmoutzou.musar.app.model3d.Object3D;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import com.gmoutzou.musar.app.model3d.ARApplication.ARAppRenderer;
import com.gmoutzou.musar.app.model3d.ARApplication.ARAppRendererControl;
import com.gmoutzou.musar.app.model3d.ARApplication.ARApplicationSession;
import com.gmoutzou.musar.app.utils.MeshObjectLoader;
import com.gmoutzou.musar.app.utils.Texture;
import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;

import com.gmoutzou.musar.app.utils.CubeShaders;
import com.gmoutzou.musar.app.utils.ARUtils;

public class Object3DRenderer implements GLSurfaceView.Renderer, ARAppRendererControl {
    private static final String LOGTAG = "Object3DRenderer";
    public static final String[] MESH_OBJECT_3DMODELS = {"hercules.obj", "dionysos.obj", "athena.obj"};
    public static final String[] TARGET_NAMES = {"hercules", "dionysos", "athena"};

    private ARApplicationSession vuforiaAppSession;
    private Object3D mActivity;
    private ARAppRenderer mARAppRenderer;

    private Vector<Texture> mTextures;

    private float mObjectRotation;
    private float mNewObjectRotation;
    private float mObjectOrientation;
    private float mScaleFactor;

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    private List<MeshObjectLoader.MeshArrays> meshArrays;

    private float kBuildingScale = 0.012f;

    private boolean mIsActive = false;
    private boolean mModelsAreLoaded = false;
    private boolean mIsModelRotating = false;
    private boolean mGroundPlane = false;

    private static final float OBJECT_SCALE_FLOAT = 0.003f;


    public Object3DRenderer(Object3D activity, ARApplicationSession session) {
        mActivity = activity;
        vuforiaAppSession = session;
        mARAppRenderer = new ARAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);
        meshArrays = new ArrayList<MeshObjectLoader.MeshArrays>();
        mIsModelRotating = false;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive || !mModelsAreLoaded) {
            return;
        }
        mARAppRenderer.render();
    }


    public void setActive(boolean active) {
        mIsActive = active;
        if (mIsActive)
            mARAppRenderer.configureVideoBackground();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        vuforiaAppSession.onSurfaceCreated();
        mARAppRenderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        vuforiaAppSession.onSurfaceChanged(width, height);
        mARAppRenderer.onConfigurationChanged(mIsActive);
        initRendering();
    }

    private void initRendering()

    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (Texture t : mTextures) {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = ARUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        if (!mModelsAreLoaded) {
            for (String meshObject : MESH_OBJECT_3DMODELS) {
                LoadModelTask loadModelTask = new LoadModelTask();
                loadModelTask.execute(meshObject);
            }
        }

        mObjectRotation = 0;
        mNewObjectRotation = 0;
        mObjectOrientation = 90;
        mScaleFactor = OBJECT_SCALE_FLOAT;
    }

    public void updateConfiguration() {
        mARAppRenderer.onConfigurationChanged(mIsActive);
    }

    public void renderFrame(State state, float[] projectionMatrix) {
        mARAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        if (state.getNumTrackableResults() > 0) {
            for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
                TrackableResult result = state.getTrackableResult(tIdx);
                Trackable trackable = result.getTrackable();
                //printUserData(trackable);
                int tIndex = getTargetIndex(trackable.getName());
                mActivity.showInfoLayout(tIndex);
                Matrix44F modelViewMatrix_Vuforia = Tool
                        .convertPose2GLMatrix(result.getPose());
                float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
                float[] modelViewProjection = new float[16];

                if (!mActivity.isExtendedTrackingActive()) {
                    Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, 0.0f);
                    Matrix.scaleM(modelViewMatrix, 0, mScaleFactor,
                            mScaleFactor, mScaleFactor);
                    if (mGroundPlane) {
                        Matrix.rotateM(modelViewMatrix, 0, mObjectOrientation, 1.0f, 0.0f, 0.0f);
                    }
                    Matrix.rotateM(modelViewMatrix, 0, mObjectRotation + mNewObjectRotation, 0.0f, 1.0f, 0.0f);
                } else {
                    Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
                    Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
                            kBuildingScale, kBuildingScale);
                }
                Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

                GLES20.glUseProgram(shaderProgramID);

                if (!mActivity.isExtendedTrackingActive()
                        && tIndex >= 0
                        && tIndex < mTextures.size()
                        && tIndex < meshArrays.size()) {
                    GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                            false, 0, meshArrays.get(tIndex).getVertices());
                    GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                            GLES20.GL_FLOAT, false, 0, meshArrays.get(tIndex).getTexCoords());

                    GLES20.glEnableVertexAttribArray(vertexHandle);
                    GLES20.glEnableVertexAttribArray(textureCoordHandle);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(tIndex).mTextureID[0]);
                    GLES20.glUniform1i(texSampler2DHandle, 0);

                    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                            modelViewProjection, 0);

                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                            meshArrays.get(tIndex).getNumVertices());

                    GLES20.glDisableVertexAttribArray(vertexHandle);
                    GLES20.glDisableVertexAttribArray(textureCoordHandle);
                }

                ARUtils.checkGLError("Render Frame");

            }
        } else {
            mActivity.showInfoLayout(-1);
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    }

    private void printUserData(Trackable trackable) {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }

    private int getTargetIndex(String target) {
        int index = -1;
        for (int i = 0; i < TARGET_NAMES.length; i++) {
            if (target.equalsIgnoreCase(TARGET_NAMES[i])) {
                index = i;
                break;
            }
        }
        return index;
    }

    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;
    }

    public void setModelRotating(boolean modelRotating) {
        mIsModelRotating = modelRotating;

        if (!modelRotating) {
            mObjectRotation += mNewObjectRotation;
            if (mObjectRotation < 0) {
                mObjectRotation += 360;
            }
            mObjectRotation %= 360;
        }
        mNewObjectRotation = 0;
    }

    public void setCurrentRotation(float angle) {
        mNewObjectRotation = angle;
    }

    public void setScale(float scale) {
        mScaleFactor = scale;
    }

    public boolean isGroundPlane() {
        return mGroundPlane;
    }

    public void setGroundPlane(boolean mGroundPlane) {
        this.mGroundPlane = mGroundPlane;
    }

    private class LoadModelTask extends AsyncTask<String, Integer, Boolean> {
        protected Boolean doInBackground(String... params) {
            boolean mModelIsLoaded = false;
            AssetManager assetManager = mActivity.getResources().getAssets();
            InputStream is = null;
            try {
                Log.d(LOGTAG, "Loading <" + params[0] + "> 3D Model...");
                is = assetManager.open("3DObjects"
                        + "/"
                        + params[0]);
                meshArrays.add(MeshObjectLoader.loadModelMeshFromStream(is));
                mModelIsLoaded = true;
            } catch (IOException e) {
                Log.e(LOGTAG, "Error! Unable to load 3D Model");
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    Log.e(LOGTAG, "Error! Unable to close InputStream");
                }
            }
            return mModelIsLoaded;
        }

        protected void onPostExecute(Boolean result) {
            if (result && meshArrays.size() == MESH_OBJECT_3DMODELS.length) {
                boolean load_flag = true;
                for (int i=0; i < meshArrays.size(); i++) {
                    if (meshArrays.get(i) == null) {
                        load_flag = false;
                        break;
                    }
                }
                if (load_flag) {
                    mModelsAreLoaded = true;
                    mActivity.showProgressIndicator(false);
                }
            }
        }
    }

}
