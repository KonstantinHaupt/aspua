package de.aspua.framework.Unit.Controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.aspua.framework.Controller.CausalRejectionController.SerialController;
import de.aspua.framework.Model.ASP.BaseEntities.ASPAtom;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Utils.Constants;

public class SerialControllerTest
{
    @Test
    public void SerialController_loadAvailableProgramStrings()
    {
        SerialController SerialController = new SerialController();
        File folder = new File(Constants.RESOURCEFOLDER_SERIALIZE);
        File[] files = folder.listFiles();

        boolean isEmptyFolder = (files == null || files.length == 0);
        Map<String,String> loadedFiles = SerialController.loadAvailableProgramStrings();

        assertEquals(isEmptyFolder, loadedFiles.isEmpty());
    }

    @Test
    public void SerialController_loadAvailableParsedPrograms()
    {
        SerialController SerialController = new SerialController();
        File folder = new File(Constants.RESOURCEFOLDER_SERIALIZE);
        File[] files = folder.listFiles();

        boolean isEmptyFolder = (files == null || files.length == 0);
        List<ASPProgram<?,?>> loadedFiles = SerialController.loadAvailableParsedPrograms();

        assertEquals(isEmptyFolder, loadedFiles.isEmpty());
    }

    @Test
    public void SerialController_loadProgram_NonExisting()
    {
        SerialController SerialController = new SerialController();
        assertEquals("",SerialController.loadProgram("Some non-existing serial name"));
    }

    @Test
    public void SerialController_loadProgram_Existing()
    {
        SerialController SerialController = new SerialController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestSerial_LoadProgram");
        SerialController.persist(program, "UnitTestSerial_LoadProgram");
        
        String expectedString = "testLiteral."+ System.lineSeparator();
        assertEquals(expectedString, SerialController.loadProgram("UnitTestSerial_LoadProgram"));

        SerialController.deleteProgram(program);
    }

    @Test
    public void SerialController_persist_InvalidParameter()
    {
        SerialController SerialController = new SerialController();
        assertFalse(SerialController.persist(null, "test"));
    }

    @Test
    public void SerialController_persist_NoName()
    {
        SerialController SerialController = new SerialController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestSerial_NoName");

        assertTrue(SerialController.persist(program, null));

        String expectedString = "testLiteral." + System.lineSeparator();
        assertEquals(expectedString, SerialController.loadProgram(program.getProgramName()));

        SerialController.deleteProgram(program);
    }

    @Test
    public void SerialController_persist_ValidName()
    {
        SerialController SerialController = new SerialController();
        ASPProgram<?, ?> program = this.generateProgram("");
        assertTrue(SerialController.persist(program, "UnitTestSerial_ValidName"));
        
        String expectedString = "testLiteral." + System.lineSeparator();
        assertEquals(expectedString, SerialController.loadProgram("UnitTestSerial_ValidName"));
        
        program.setProgramName("UnitTestSerial_ValidName");
        SerialController.deleteProgram(program);
    }

    @Test
    public void SerialController_delete_InvalidArgument()
    {
        SerialController SerialController = new SerialController();
        assertFalse(SerialController.deleteProgram(null));
        assertFalse(SerialController.deleteProgram(new ASPProgram<>()));
    }

    @Test
    public void SerialController_delete_NonExisting()
    {
        SerialController SerialController = new SerialController();
        ASPProgram<?, ?> program = new ASPProgram<>();
        program.setProgramName("non-existing name");
        assertFalse(SerialController.deleteProgram(program));
    }

    @Test
    public void SerialController_delete_Existing()
    {
        SerialController SerialController = new SerialController();
        ASPProgram<?, ?> program = this.generateProgram("UnitTestSerial_ToDelete");

        assertTrue(SerialController.persist(program, "UnitTestSerial_ToDelete"));
        assertTrue(SerialController.deleteProgram(program));
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
