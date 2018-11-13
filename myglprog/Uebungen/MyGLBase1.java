package Uebungen;//  -------------   OpenGL Basis-Funktionen  -------------------
//                                                              E.Gutknecht, August 2016

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec4;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;

import java.nio.FloatBuffer;
import java.util.Stack;

public class MyGLBase1 {

    //  --------------  Globale Daten  -------------------------------------

    private int maxVerts;                                     // max. Anzahl Vertices im Vertex-Array
    private int nVertices = 0;                                // momentane Anzahl Vertices

    private Mat4 M = Mat4.ID;                                 // ModelView-Matrix
    private Mat4 P = Mat4.ID;                                 // Projektions-Matrix

    private Stack<Mat4> MStack = new Stack<>();              // Matrix-Stack


    private int shadingLevel = 0;                             // Beleuchtungs-Stufe 0=aus, 1=ambient u. diffus
    private float[] lightPosition = {0, 0, 10, 1};            // Lichtquelle
    private float ambient = 0.2f;                             // ambientes Licht
    private float diffuse = 0.8f;                             // diffuse Reflexion
    private float specular = 0.0f;                            // diffuse Reflexion
    private float specExp = 0;                                // diffuse Reflexion

    // ------ Identifiers fuer OpenGL-Objekte und Shader-Variablen  ------

    private int vaoId;                                        //  OpenGL VertexArray Object
    private int vertexBufId;                                  //  OpenGL Vertex Buffer
    private int vPositionId, vColorId, vNormalId;             //  Vertex Attribute S
    private int MId, PId;                                     //  Uniform Shader Variables
    private int shadingLevelId, lightPositionId;              // Uniform Shader Variables
    private int ambientId, diffuseId, specularId, specExpId;  // Uniform Shader Variables


    //  --------  Vertex-Array (fuer die Attribute Position, Color, Normal)  ------------

    private FloatBuffer vertexBuf;                            // Vertex-Array

    private int vAttribSize = 4 * Float.SIZE / 8;                 // Anz. Bytes eines Vertex-Attributes
    private int vertexSize = 3 * vAttribSize;                   // Anz. Bytes eines Vertex
    private int bufSize;

    private float[] currentColor = {1, 1, 1, 1};                // aktuelle Farbe fuer Vertices
    private float[] currentNormal = {1, 0, 0, 0};               // aktuelle Normale Vertices


    //  ------------- Konstruktor  ---------------------------


    public MyGLBase1(GL3 gl,
                     int programId,                             // Program-Identifier
                     int maxVerts)                              // max. Anzahl Vertices im Vertex-Array
    {
        this.maxVerts = maxVerts;
        bufSize = maxVerts * vertexSize;
        vertexBuf = Buffers.newDirectFloatBuffer(bufSize);
        setupVertexBuffer(gl, programId, bufSize);             // OpenGL Vertex-Buffer
        setupMatrices(gl, programId);                          // ModelView- und Projektions-Matrix
        setupLightingParms(gl, programId);                     // Beleuchtung
    }

    ;


    //  -------------  Methoden  ---------------------------


    void setupVertexBuffer(GL3 gl, int pgm, int bufSize)             // OpenGL VertexBuffer
    {
        // ----- generate VertexArrayObject  -------------
        int[] vaoIdArray = new int[1];
        gl.glGenVertexArrays(1, vaoIdArray, 0);
        vaoId = vaoIdArray[0];
        gl.glBindVertexArray(vaoId);

        // ----- generate BufferObject  -------------
        int[] bufIdArray = new int[1];
        gl.glGenBuffers(1, bufIdArray, 0);
        vertexBufId = bufIdArray[0];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufId);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, bufSize,           // Speicher allozieren
            null, GL3.GL_STATIC_DRAW);

        vPositionId = defineAttribute(gl, pgm, "vPosition", vertexSize, 0);
        vColorId = defineAttribute(gl, pgm, "vColor", vertexSize, vAttribSize);
        vNormalId = defineAttribute(gl, pgm, "vNormal", vertexSize, 2 * vAttribSize);
    }


    int defineAttribute(GL3 gl, int pgm, String attribName, int vertexSize, int offset) {
        int attribId = gl.glGetAttribLocation(pgm, attribName);
        if (attribId >= 0) {
            gl.glEnableVertexAttribArray(attribId);
            gl.glVertexAttribPointer(attribId, 4, GL3.GL_FLOAT, false, vertexSize, offset);
            System.out.println("Attribute " + attribName + " enabled");
        } else
            System.out.println("Attribute " + attribName + " not enabled");
        return attribId;
    }


    private void setupMatrices(GL3 gl, int pgm) {
        // ----- get shader variable identifiers  -------------
        MId = gl.glGetUniformLocation(pgm, "M");
        PId = gl.glGetUniformLocation(pgm, "P");

        // -----  set uniform variables  -----------------------
        gl.glUniformMatrix4fv(MId, 1, false, Mat4.ID.toArray(), 0);
        gl.glUniformMatrix4fv(PId, 1, false, Mat4.ID.toArray(), 0);
    }

    ;


    private void setupLightingParms(GL3 gl, int pgm) {
        float[] lightPosition = {0, 0, 10, 1};               // Default-Koordinaten der Lichtquelle

        // ----- get shader variable identifiers  -------------
        shadingLevelId = gl.glGetUniformLocation(pgm, "shadingLevel");
        lightPositionId = gl.glGetUniformLocation(pgm, "lightPosition");
        ambientId = gl.glGetUniformLocation(pgm, "ambient");
        diffuseId = gl.glGetUniformLocation(pgm, "diffuse");
        specularId = gl.glGetUniformLocation(pgm, "specular");
        specExpId = gl.glGetUniformLocation(pgm, "specExp");

        // -----  set uniform variables  -----------------------
        gl.glUniform1i(shadingLevelId, shadingLevel);
        gl.glUniform1f(ambientId, ambient);
        gl.glUniform1f(diffuseId, diffuse);
        gl.glUniform1f(specularId, specular);
        gl.glUniform1f(specExpId, specExp);
        gl.glUniformMatrix4fv(lightPositionId, 1, false, lightPosition, 0);
    }

    ;


    //  ----------  oeffentliche Methoden   -------------


    public void setColor(float r, float g, float b)             // aktuelle Vertexfarbe setzen
    {
        currentColor[0] = r;
        currentColor[1] = g;
        currentColor[2] = b;
        currentColor[3] = 1;
    }

    public void setNormal(float x, float y, float z)             // aktuelle Vertexfarbe setzen
    {
        currentNormal[0] = x;
        currentNormal[1] = y;
        currentNormal[2] = z;
        currentNormal[3] = 0;
    }


    public void putVertex(float x, float y, float z)            // Vertex-Daten in Buffer speichern
    {
        vertexBuf.put(x);
        vertexBuf.put(y);
        vertexBuf.put(z);
        vertexBuf.put(1);
        vertexBuf.put(currentColor);                              // Farbe
        vertexBuf.put(currentNormal);                             // Normale
        nVertices++;
    }


    public void copyBuffer(GL3 gl)                              // Vertex-Array in OpenGL-Buffer kopieren
    {
        vertexBuf.rewind();
        if (nVertices > maxVerts)
            throw new IndexOutOfBoundsException();
        gl.glBindVertexArray(vaoId);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufId);
        gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, 0, nVertices * vertexSize, vertexBuf);
    }


    public void rewindBuffer(GL3 gl)                            // Bufferposition zuruecksetzen
    {
        vertexBuf.rewind();
        nVertices = 0;
    }


    public void drawArrays(GL3 gl, int figureType)              // Rendering-Pipeline starten
    {
        gl.glDrawArrays(figureType, 0, nVertices);
    }


    public void drawArrays2(GL3 gl, int figureType, int startVertex,   // Startindex im Array
                            int nVertices)                             // Anzahl Vertices
    {
        gl.glDrawArrays(figureType, startVertex, nVertices);
    }


    public void bindBuffer(GL3 gl)                              // activate Buffer
    {
        gl.glBindVertexArray(vaoId);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufId);
    }


    public void setM(GL3 gl, Mat4 M)                            // ModelView-Matrix
    {
        this.M = M;
        gl.glUniformMatrix4fv(MId, 1, false, M.toArray(), 0);
    }


    public void setP(GL3 gl, Mat4 P)                             // Projektions-Matrix
    {
        this.P = P;
        gl.glUniformMatrix4fv(PId, 1, false, P.toArray(), 0);
    }


    public void multM(GL3 gl, Mat4 A) {
        M = M.postMultiply(A);
        gl.glUniformMatrix4fv(MId, 1, false, M.toArray(), 0);
    }


    public void pushM() {
        MStack.push(M);
    }


    public Mat4 popM(GL3 gl) {
        M = MStack.pop();
        gl.glUniformMatrix4fv(MId, 1, false, M.toArray(), 0);
        return M;
    }


    public void setShadingLevel(GL3 gl, int level)               // 0: ohne Beleuchtung
    {
        gl.glUniform1i(shadingLevelId, level);
    }


    public void setShadingParam(GL3 gl, float ambient, float diffuse)  // Lichtparameter
    {
        this.ambient = ambient;
        this.diffuse = diffuse;
        gl.glUniform1f(ambientId, ambient);
        gl.glUniform1f(diffuseId, diffuse);
    }


    public void setShadingParam2(GL3 gl, float specular, float specExp)  // spiegelnde Reflexion
    {
        this.specular = specular;
        this.specExp = specExp;
        gl.glUniform1f(specularId, specular);
        gl.glUniform1f(specExpId, specExp);
    }


    public void setLightPosition(GL3 gl, float x, float y, float z) {
        lightPosition = new float[]{x, y, z};
        Vec4 tmp = new Vec4(x, y, z, 1);
        tmp = M.transform(tmp);                               // ModelView-Transformation
        gl.glUniform4fv(lightPositionId, 1, tmp.toArray(), 0);
    }


    //  ---------  Abfrage-Methoden ----------

    public float[] getCurrentColor() {
        float[] c = {currentColor[0],
            currentColor[1], currentColor[2]};
        return c;
    }

    public float[] getCurrentNormal() {
        float[] n = {currentNormal[0],
            currentNormal[1], currentNormal[2]};
        return n;
    }

    public Mat4 getM()                                         // ModelView-Matrix
    {
        return M;
    }

    public Mat4 getP()                                         // Projektions-Matrix
    {
        return P;
    }

    public int getShadingLevel() {
        return shadingLevel;
    }

    public float getAmbient() {
        return ambient;
    }

    public float getDiffuse() {
        return diffuse;
    }

    public float[] getLightPosition() {
        return lightPosition;
    }


    public float[] getShadingParam() {
        float[] param = {ambient, diffuse};
        return param;
    }


    public float[] getShadingParam2() {
        float[] param = {specular, specExp};
        return param;
    }

    //  ---------  Zeichenmethoden  ------------------------------

    public void drawAxis(GL3 gl, float a, float b, float c)                   // Koordinatenachsen zeichnen
    {
        rewindBuffer(gl);
        putVertex(0, 0, 0);           // Eckpunkte in VertexArray speichern
        putVertex(a, 0, 0);
        putVertex(0, 0, 0);
        putVertex(0, b, 0);
        putVertex(0, 0, 0);
        putVertex(0, 0, c);
        copyBuffer(gl);
        drawArrays(gl, GL3.GL_LINES);
    }

}