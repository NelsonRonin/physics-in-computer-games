//  ------------  Vertex- und Fragment-Shaders  -----------------------------------------
import com.jogamp.opengl.*;

public class MyShaders
{

    /*  -----------  Vertex-Shader (Pass-through Shader) ---------- */
    public static String vShader0 =
    "   #version 330                  /* Shader Language Version */   \n" +
    "   in vec4 vPosition, vColor;    /* Vertex-Attribute */          \n" +
    "   out vec4 fColor;              /* Fragment-Farbe */            \n" +
    "   void main()                                                   \n" +
    "   {  gl_Position = vPosition;                                   \n" +
    "      fColor = vColor;                                           \n" +
    "   }";


    /* -----------  Fragment-Shader (Pass-through Shader) ---------  */
    public static String fShader0 =
    "    #version 330                   \n" +
    "    in  vec4 fColor;               \n" +
    "    out vec4 fragColor;            \n" +
    "    void main()                    \n" +
    "    {  fragColor = fColor;         \n" +
    "    }";


    /* -----------  Vertex-Shader mit Vertex-Transformationen  */
    public static String vShader1 =
    "   #version 330                                                              \n" +
    "   uniform mat4 M, P;                       /* Transformations-Matrizen */   \n" +
    "   in vec4 vPosition, vColor, vNormal;      /* Vertex-Attribute */           \n" +
    "   out vec4 fColor;                         /* Fragment-Farbe */             \n" +
    "   void main()                                                               \n" +
    "   {  vec4 vertex = M * vPosition;          /* ModelView-Transformation */   \n" +
    "      gl_Position = P * vertex;             /* Projektion */                 \n" +
    "      fColor = vColor;                                                       \n" +
    "   }";



    /* -----------  Vertex-Shader mit Vertex-Transformationen und Beleuchtung        ------  */
    public static String vShader2 =
    "   #version 330                                         /* Shader Language Version */                \n" +
    "   uniform mat4 M, P;                                   /* Transformations-Matrizen */               \n" +
    "   uniform vec4 lightPosition;                          /* Position Lichtquelle (im Cam.System) */   \n" +
    "   uniform int shadingLevel;                            /* 0 ohne Beleucht, 1 mit Beleucht.     */   \n" +
    "   uniform float ambient;                               /* ambientes Licht */                        \n" +
    "   uniform float diffuse;                               /* diffuse Reflexion */                      \n" +
    "   uniform float specular;                              /* spiegelnde Reflexion */                   \n" +
    "   uniform float specExp;                               /* Shininess (Exponent) */                   \n" +
    "   in vec4 vPosition, vColor, vNormal;                  /* Vertex-Attribute */                       \n" +
    "   out vec4 fColor;                                     /* Fragment-Farbe */                         \n" +
    "   vec3 whiteColor = vec3(1,1,1);                                                                    \n" +

    "  /* ------  spiegelnde Reflexion  --------     */          \n" +
    "  vec3 specularLight(vec3 toLight, vec3 normal, vec3 toEye, \n" +
    "                       float specular, float specExp)       \n" +
    "  {  float Is = 0.0;                                        \n" +
    "     vec3 halfBetween = normalize(toLight + toEye);         \n" +
    "     float cosBeta = dot(halfBetween, normal);              \n" +
    "     if ( cosBeta < 0.0 )                                   \n" +
    "       Is = 0.0;                                            \n" +
    "     else                                                   \n" +
    "       Is = specular * pow(cosBeta,specExp);                \n" +
    "     return  Is * whiteColor;                               \n" +
    "}                                                           \n" +
    "                                                            \n" +

    "   /* ------  main-function  --------           */                                         \n" +
    "   void main()                                                                             \n" +
    "   {  vec4 vertex = M * vPosition;                      /* ModelView=Transformation */     \n" +
    "      gl_Position = P * vertex;                         /* Projektion */                   \n" +
    "      fColor = vColor;                                                                     \n" +
    "      if ( shadingLevel < 1 )                                                              \n" +
    "        return;                                         /* keine Beleuchtung */            \n" +
    "      vec3 normal = normalize((M * vNormal).xyz);                                          \n" +
    "      vec3 toLight = normalize(lightPosition.xyz - vertex.xyz);                            \n" +
    "      float cosAlpha = dot(toLight, normal);                                               \n" +
    "      if ( cosAlpha < 0.0 )                                                                \n" +
    "      { fColor.rgb = min(ambient * vColor.rgb, whiteColor);                                \n" +
    "        return;                                                                            \n" +
    "      }                                                                                    \n" +
    "      float Id = diffuse * cosAlpha;                   /* Gesetz von Lambert */            \n" +
    "      vec3 diffuseReflected =  (ambient + Id) * vColor.rgb;                                \n" +
    "      vec3 specularReflected = specularLight(toLight, normal, normalize(-vertex.xyz),      \n" +
    "                                             specular, specExp);                           \n" +
    "      vec3 reflectedLight = diffuseReflected + specularReflected;                          \n" +
    "      fColor.rgb = min(reflectedLight, whiteColor);                                        \n" +
    "   }";



    public static int initShaders(GL3 gl,
                                   String vShader,   // Vertex-Shader
                                   String fShader)   // Fragment-Shader
    {
       int vShaderId = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
       int fShaderId = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);


       gl.glShaderSource(vShaderId, 1, new String[] { vShader }, null);
       gl.glCompileShader(vShaderId);                                      // Compile Vertex Shader
       System.out.println("VertexShaderLog:");
       System.out.println(getShaderInfoLog(gl, vShaderId));
       System.out.println();


       gl.glShaderSource(fShaderId, 1, new String[] { fShader }, null);
       gl.glCompileShader(fShaderId);                                     // Compile Fragment Shader
       System.out.println("FragmentShaderLog:");
       System.out.println(getShaderInfoLog(gl, fShaderId));
       System.out.println();

       int programId = gl.glCreateProgram();
       gl.glAttachShader(programId, vShaderId);
       gl.glAttachShader(programId, fShaderId);
       gl.glLinkProgram(programId);                                       // Link Program
       gl.glUseProgram(programId);                                        // Activate Programmable Pipeline
       System.out.println("ProgramInfoLog:");
       System.out.println(getProgramInfoLog(gl, programId));
       System.out.println();
       return programId;
    }


    public static String getProgramInfoLog(GL3 gl, int obj)               // Info- and Error-Messages
    {
       int params[] = new int[1];
       gl.glGetProgramiv(obj, GL3.GL_INFO_LOG_LENGTH, params, 0);         // get log-length
       int logLen = params[0];
       if (logLen <= 0)
         return "";
       byte[] bytes = new byte[logLen + 1];
       int[] retLength = new int[1];
       gl.glGetProgramInfoLog(obj, logLen, retLength, 0, bytes, 0);       // get log-data
       String logMessage = new String(bytes);
       int iend = logMessage.indexOf(0);
       if (iend < 0 ) iend = 0;
       return logMessage.substring(0,iend);
    }


    static public String getShaderInfoLog(GL3 gl, int obj)               // Info- and Error-Messages
    {  int params[] = new int[1];
       gl.glGetShaderiv(obj, GL3.GL_INFO_LOG_LENGTH, params, 0);         // get log-length
       int logLen = params[0];
       if (logLen <= 0)
         return "";
       // Get the log
       byte[] bytes = new byte[logLen + 1];
       int[] retLength = new int[1];
       gl.glGetShaderInfoLog(obj, logLen, retLength, 0, bytes, 0);       // get log-data
       String logMessage = new String(bytes);
       int iend = logMessage.indexOf(0);
       if (iend < 0 ) iend = 0;
       return logMessage.substring(0,iend);
    }

}