package de.aspua.framework.Unit.Controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.aspua.framework.Controller.CausalRejectionController.FileController;
import de.aspua.framework.Model.ASP.BaseEntities.ASPAtom;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Utils.Constants;

public class FileControllerTest
{
    @Test
    public void FileController_loadAvailableProgramStrings()
    {
        FileController fileController = new FileController();
        File folder = new File(Constants.RESOURCEFOLDER_TEXTFILE);
        File[] files = folder.listFiles();

        boolean isEmptyFolder = (files == null || files.length == 0);
        Map<String,String> loadedFiles = fileController.loadAvailableProgramStrings();

        assertEquals(isEmptyFolder, loadedFiles.isEmpty());
    }

    @Test
    public void FileController_loadProgram_NonExisting()
    {
        FileController fileController = new FileController();
        assertEquals("",fileController.loadProgram("Some non-existing file name"));
    }

    @Test
    public void FileController_loadProgram_Existing()
    {
        FileController fileController = new FileController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestFile_LoadProgram");
        fileController.persist(program, "UnitTestFile_LoadProgram");
        
        String expectedString = "testLiteral."+ System.lineSeparator();
        assertEquals(expectedString, fileController.loadProgram("UnitTestFile_LoadProgram"));

        fileController.deleteProgram(program);
    }

    @Test
    public void FileController_persist_InvalidParameter()
    {
        FileController fileController = new FileController();
        assertFalse(fileController.persist(null, "test"));
    }

    @Test
    public void FileController_persist_NoName()
    {
        FileController fileController = new FileController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestFile_NoName");

        assertTrue(fileController.persist(program, null));

        String expectedString = "testLiteral." + System.lineSeparator();
        assertEquals(expectedString, fileController.loadProgram(program.getProgramName()));

        fileController.deleteProgram(program);
    }

    @Test
    public void FileController_persist_ValidName()
    {
        FileController fileController = new FileController();
        ASPProgram<?, ?> program = this.generateProgram("");
        assertTrue(fileController.persist(program, "UnitTestFile_ValidName"));
        
        String expectedString = "testLiteral." + System.lineSeparator();
        assertEquals(expectedString, fileController.loadProgram("UnitTestFile_ValidName"));
        
        program.setProgramName("UnitTestFile_ValidName");
        fileController.deleteProgram(program);
    }

    @Test
    public void FileController_delete_InvalidArgument()
    {
        FileController fileController = new FileController();
        assertFalse(fileController.deleteProgram(null));
        assertFalse(fileController.deleteProgram(new ASPProgram<>()));
    }

    @Test
    public void FileController_delete_NonExisting()
    {
        FileController fileController = new FileController();
        ASPProgram<?, ?> program = new ASPProgram<>();
        program.setProgramName("non-existing name");
        assertFalse(fileController.deleteProgram(program));
    }

    @Test
    public void FileController_delete_Existing()
    {
        FileController fileController = new FileController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestFile_ToDelete");

        assertTrue(fileController.persist(program, "UnitTestFile_ToDelete"));
        assertTrue(fileController.deleteProgram(program));
    }

    @Test
    public void FileController_exportProgram_Invalid()
    {
        FileController fileController = new FileController();
        assertNull(fileController.exportProgram(null));
    }

    @Test
    public void FileController_exportProgram_Valid()
    {
        FileController fileController = new FileController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestFile_ToExport");
        File exportFile = fileController.exportProgram(program);
        assertNotNull(exportFile);

        exportFile = fileController.exportProgram(new ASPProgram<>());
        assertNotNull(exportFile);
    }

    private ASPProgram<?,?> generateProgram(String name)
    {
        List<ASPRule<ASPLiteral<ASPAtom>>> rules = new ArrayList<>();
        List<ASPLiteral<ASPAtom>> head = new ArrayList<>();
        head.add(new ASPLiteral<ASPAtom>(new ASPAtom("testLiteral")));
        rules.add(new ASPRule<ASPLiteral<ASPAtom>>(head, null));
        ASPProgram<ASPRule<ASPLiteral<ASPAtom>>,ASPLiteral<ASPAtom>> program = new ASPProgram<>(rules);
        program.setProgramName(name);

        return program;
    }
}
