package de.aspua.framework.Utils;

/**
 * Provides general constant values for consistent usage throughout the framework.
 */
public class Constants
{
    /**
     * Path to ressource folder of {@link de.aspua.framework.Controller.CausalRejectionController.FileController}.
     */
    // Paths for usage in IDE with Maven
    public static String RESOURCEFOLDER_TEXTFILE = BuildPlatformIndependentPath("src/main/resources/ASP-Programs-Textfiles");

    // Suggested Path for deployment in JAR
    // public static final String RESOURCEFOLDER_TEXTFILE = "aspua_files";


    /**
     * Path to ressource folder of {@link de.aspua.framework.Controller.CausalRejectionController.SerialController}.
     */
    // Paths for usage in IDE with Maven
    public static String RESOURCEFOLDER_SERIALIZE = BuildPlatformIndependentPath("src/main/resources/ASP-Programs-Serial");

    // Suggested Path for deployment in JAR
    // public static final String RESOURCEFOLDER_SERIALIZE = "aspua_internal";


    /** 
     * Path for external HTTP-requests in the {@link de.aspua.framework.Controller.CausalRejectionController.ClingoRemoteSolver}.
     */
    public static final String URI_SOLVER = "https://ls1-asp.cs.tu-dortmund.de/enc/index.php";

    /**
     * Ressouce path for framework-banner used for the startup in {@link de.aspua.framework.Controller.ASPUAFrameworkAPI}.
     */
    public static final String RESOURCEFILE_LOAD_BANNER = "framework_banner.txt";
    
    /**
     * Prefix for creating new objects by reflection in {@link de.aspua.framework.View.CLIViewController}.
     * The prefix has to match the specified mainClass-Path of the mojo java:exec goal in the pom.xml.
     */
    public static final String PACKAGE_PREFIX = "de.aspua.framework.Controller.";

    /**
     * Regular Expressions for ASP-Syntax (ELP) which don't tolerate special syntax of HEX-Atoms (e.g. &example["a", b](c)).
     */ 
    public static final String REGEX_PREDICATE = "-?([A-z0-9]+)";
    public static final String REGEX_CONSTANT_VARIABLE = "[A-z0-9]+";
    public static final String REGEX_TERM = "(\\((" + REGEX_CONSTANT_VARIABLE + ",\\s*)*" + REGEX_CONSTANT_VARIABLE + "\\))?";
    public static final String REGEX_LITERAL = REGEX_PREDICATE + REGEX_TERM;
    public static final String REGEX_DEFNEGLITERAL = "not (" + REGEX_LITERAL + ")";

    /**
     * Regex for parsing visual demos like MAMMA-DSCS.
     */
    // public static final String REGEX_PREDICATE = "-?(&[A-z0-9]+\\[[^\\]]+\\]|[A-z0-9]+)";
    // public static final String REGEX_CONSTANT_VARIABLE = "[^,\\(\\)]+";


    /**
     * Replaces path-specific characters such as '/' or '\' depending on the used operation system.
     * @param path Path which is transformed into a plattform-independent path
     * @return The plattform-independent path
     */
    private static String BuildPlatformIndependentPath(String path)
    {
        String[] pathParts = path.split("[/,\\\\]");
        return String.join(System.getProperty("file.separator"), pathParts);
    }
}
