package de.aspua.framework.Controller.CausalRejectionController;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ControllerInterfaces.IIOController;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Utils.Constants;

/**
 * Provides IO-operations on serialized Java-Objects (.ser).
 */
public class SerialController implements IIOController
{
	private static Logger LOGGER = LoggerFactory.getLogger(SerialController.class);
	
	@Override
	public boolean persist(ASPProgram<?, ?> program, String newProgramName)
	{
		if(program == null)
		{
			LOGGER.warn("The object to persist is null! Therefore, the object wasn't serialized.");
			return false;
		}

		try
		{
			File serialDir = new File(Constants.RESOURCEFOLDER_SERIALIZE);

			if(!serialDir.exists())
				serialDir.mkdir();

			String filePath;

			if(newProgramName == null || newProgramName.isEmpty())
				filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_SERIALIZE, program.getProgramName() + ".ser");
			else
				filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_SERIALIZE, newProgramName + ".ser");

			File serialFile = new File(filePath);

			if(serialFile.exists())
				LOGGER.info("Overwriting file {} for serializing the ASP-Program {}.", serialFile.getName(), program.getProgramName());
			else
				LOGGER.info("A new file will be created for serializing the ASP-Program {}.", serialFile.getName());

			FileOutputStream fileOut = new FileOutputStream(filePath);
			ObjectOutputStream outStream = new ObjectOutputStream(fileOut);
			outStream.writeObject(program);
			outStream.close();
			fileOut.close();
		} catch (IOException e) {
			LOGGER.error("An error occured while serializing the ASP-Program {}!", program.getProgramName(), e);
			return false;
		}

		return true;
	}

	@Override
	public boolean deleteProgram(ASPProgram<?, ?> program)
	{
		if(program == null || program.getProgramName() == null)
		{
			LOGGER.warn("The program to delete or its name is null/empty! Therefore, no serialized object was deleted.");
			return false;
		}

		try
		{
			String filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_SERIALIZE, program.getProgramName() + ".ser");
			File serialFile = new File(filePath);
	
			if(serialFile.delete())
			{
				LOGGER.info("The serialized File {} was successfully from the directory-folder {}.", program.getProgramName(), new File(Constants.RESOURCEFOLDER_SERIALIZE).getAbsolutePath());
				return true;
			}
			else
			{
				LOGGER.info("The ASP-Program {} couldn't be deleted from the directory-folder {}.", program.getProgramName(), new File(Constants.RESOURCEFOLDER_SERIALIZE).getAbsolutePath());
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("An error occured while deleting the ASP-Program {}!", program.getProgramName(), e);
			return false;
		}
	}

	/**
	 * Loads the serialized file with the given name from the folder specified in {@link Constants#RESOURCEFOLDER_SERIALIZE}.
	 */
	@Override
	public String loadProgram(String programName)
	{
		ASPProgram<?, ?> program = null;

		try
		{
			String filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_SERIALIZE, programName + ".ser");
			File programFile = new File(filePath);

			if(!programFile.exists())
			{
				LOGGER.warn("The requested Program didn't seem to be serialized by the name {} in the folder {}.", programFile.getName(), new File(Constants.RESOURCEFOLDER_SERIALIZE).getAbsolutePath());
				return "";
			}

			FileInputStream fileIn = new FileInputStream(programFile);
			ObjectInputStream inStream = new ObjectInputStream(fileIn);
			program = (ASPProgram<?, ?> ) inStream.readObject();
			inStream.close();
			fileIn.close();
		} catch (IOException e) {
			LOGGER.error("An error occured while deserializing the requested ASP-Program!", e);
			return "";
		} catch (ClassNotFoundException e) {
			LOGGER.error("An error occured while trying to find the file with the requested name!", e);
			return "";
		}

		StringBuilder sb = new StringBuilder();

		for (ASPRule<?> currentRule : program.getRuleSet())
		{
			sb.append(currentRule.toString());
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}

	/**
	 * Loads all available serialized files from the folder specified in {@link Constants#RESOURCEFOLDER_SERIALIZE}.
	 */
	@Override
	public List<ASPProgram<?, ?>> loadAvailableParsedPrograms()
	{
		List<ASPProgram<?, ?>> allPrograms = new ArrayList<>();
		File aspDir = new File(Constants.RESOURCEFOLDER_SERIALIZE);

		if(!aspDir.exists())
		{
			LOGGER.warn("The folder {} doesn't seem to exist! Aborting deserialization.", new File(Constants.RESOURCEFOLDER_SERIALIZE).getAbsolutePath());
			return allPrograms;
		}
		
		FileFilter fileFilter = file -> !file.isDirectory() && file.getName().endsWith(".ser");
		File[] allFiles = aspDir.listFiles(fileFilter);
		LOGGER.info("Found {} serialized files. Continue to deserialize each found file.", allFiles.length);

		for (File currentFile : allFiles)
		{
			try
			{
			FileInputStream fileIn = new FileInputStream(currentFile);
			ObjectInputStream inStream = new ObjectInputStream(fileIn);
			allPrograms.add((ASPProgram<?, ?>) inStream.readObject());

			inStream.close();
			fileIn.close();

			} catch (IOException e) {
				LOGGER.error("An IO-Error occured while trying to deserialize the file {}! The file will be skipped.", currentFile.getName(), e);
				continue;
			} catch (ClassNotFoundException e) {
				LOGGER.error("The file {} couldn't be deserialized because it wasn't found! The file will be skipped.", currentFile.getName(), e);
				continue;
			}
		}
		
		return allPrograms;
	}

	/**
	 * Loads all available serialized files from the folder specified in {@link Constants#RESOURCEFOLDER_SERIALIZE}.
	 * If possible, it is recommended to use {@link #loadAvailableParsedPrograms()}, as this method just calls toString() on each deserialized object.
	 * Therefore, all meta-information that is provided by the {@link ASPProgram}-objects gets lost.
	 */
	@Override
	public Map<String, String> loadAvailableProgramStrings()
	{
		Map<String, String> programStrings = new LinkedHashMap<>();
		for (ASPProgram<?,?> loadedProgram : this.loadAvailableParsedPrograms())
		{
			programStrings.put(loadedProgram.getProgramName(), loadedProgram.toString());	
		}

		return programStrings;
	}
}
