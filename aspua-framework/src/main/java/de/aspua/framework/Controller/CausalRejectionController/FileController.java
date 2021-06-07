package de.aspua.framework.Controller.CausalRejectionController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Controller.ControllerInterfaces.IIOController;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Utils.Constants;

/**
 * Provides IO-operations on textfiles (.txt).
 */
public class FileController implements IIOController
{
	private static Logger LOGGER = LoggerFactory.getLogger(FileController.class);
	
	@Override
	public boolean persist(ASPProgram<?, ?> program, String newProgramName)
	{
		if(program == null)
		{
			LOGGER.warn("The object to persist is null! Therefore, no file was created/overwritten.");
			return false;
		}

		try
		{
			File aspDir = new File(Constants.RESOURCEFOLDER_TEXTFILE);

			if(!aspDir.exists())
				aspDir.mkdir();

			String filePath;

			if(newProgramName == null || newProgramName.isEmpty())
			{
				filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_TEXTFILE, program.getProgramName() + ".txt");
			}
			else
			{
				filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_TEXTFILE, newProgramName + ".txt");

				File programFile = new File(filePath);
				if (programFile.createNewFile())
					LOGGER.info("File created: " + programFile.getName());
				else
					LOGGER.warn("A file with the name {} already exists! The file will be overwritten!", programFile.getName());
			}

			FileWriter fileWriter = new FileWriter(filePath);
			StringBuilder sb = new StringBuilder();

			for (ASPRule<?> currentRule : program.getRuleSet())
			{
				sb.append(currentRule.toString());
				sb.append(System.lineSeparator());
			}

			fileWriter.write(sb.toString());
			fileWriter.close();

			return true;

		} catch (IOException e) {
			LOGGER.error("An error occured while creating, locating or writing to the Textfile!", e);
			return false;
		}
	}

	@Override
	public boolean deleteProgram(ASPProgram<?, ?> program)
	{
		if(program == null || program.getProgramName() == null)
		{
			LOGGER.warn("The program to delete or its name is null/empty! Therefore, no file was deleted.");
			return false;
		}

		try
		{
			String filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_TEXTFILE, program.getProgramName() + ".txt");
			File serialFile = new File(filePath);
	
			if(serialFile.delete())
			{
				LOGGER.info("The File {} was successfully from the directory-folder {}.", program.getProgramName(), new File(Constants.RESOURCEFOLDER_TEXTFILE).getAbsolutePath());
				return true;
			}
			else
			{
				LOGGER.info("The File {} couldn't be deleted from the directory-folder {}.", program.getProgramName(), new File(Constants.RESOURCEFOLDER_TEXTFILE).getAbsolutePath());
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("An error occured while deleting the ASP-Program {}!", program.getProgramName(), e);
			return false;
		}
	}

	/**
	 * Loads the first textfile with the given name from the folder specified in {@link Constants#RESOURCEFOLDER_TEXTFILE}.
	 */
	@Override
	public String loadProgram(String filePath)
	{
		if(filePath == null || filePath.isEmpty())
		{
			LOGGER.warn("The given path was null or empty! Therefore, no file was accessed!");
			return "";
		}
		else
			filePath = String.join(System.getProperty("file.separator"), Constants.RESOURCEFOLDER_TEXTFILE, filePath + ".txt");
				
		String line;
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		try
		{
			File te = new File(filePath);
			System.out.println(te.getAbsolutePath());
			FileInputStream inputStream = new FileInputStream(filePath);
			br = new BufferedReader(new InputStreamReader(inputStream));

			while ((line = br.readLine()) != null)
			{
				sb.append(line);
				sb.append(System.lineSeparator());
			}
			LOGGER.info("The File '{}' was successfully loaded.", filePath);

		}catch (FileNotFoundException e) {
			LOGGER.error("There is no Textfile with the name {} in the folder {}!", filePath, new File(Constants.RESOURCEFOLDER_TEXTFILE).getAbsolutePath(), System.lineSeparator(), e);
			return "";
		}catch (IOException e) {
			LOGGER.error("Error occured while reading the file {}!", filePath, System.lineSeparator(), e);
			return "";
		}
		finally {
			try {
				if(br != null)
					br.close();
				}catch (IOException e) {
					LOGGER.error("Error occured while closing the Reader-Instances!", System.lineSeparator(), e);
				}
		}
		return sb.toString();
	}

	/**
	 * NOT SUPPORTED: Textfiles don't contain meta-infos to map content to a {@link ASPProgram}-object.
	 * Use {@link #loadAvailableProgramStrings()} instead and parse the returned strings via 
	 * {@link de.aspua.framework.Controller.ControllerInterfaces.IParserController#parseProgram()}.
	 */
	@Override
	public List<ASPProgram<?, ?>> loadAvailableParsedPrograms() { return null; }

	/**
	 * Loads all available textfiles from the folder specified in {@link Constants#RESOURCEFOLDER_TEXTFILE}.
	 */
	@Override
	public Map<String, String> loadAvailableProgramStrings()
	{
		Map<String, String> programStrings = new LinkedHashMap<>();
		try {
			File folder = new File(Constants.RESOURCEFOLDER_TEXTFILE);
			File[] files = folder.listFiles();

			if(files == null)
			{
				LOGGER.warn("No files were found in the directory '{}'", new File(Constants.RESOURCEFOLDER_TEXTFILE).getAbsolutePath());
				return null;
			}
			
			for(File file : files)
			{
				if(file.isFile() && file.getName().toLowerCase().endsWith(".txt"))
				{
					String programName = file.getName().substring(0, file.getName().length()-4);
					programStrings.put(programName, this.loadProgram(programName));
				}
			}
		  } catch (Exception e) {
			LOGGER.error("An error occured while accessing a file from the directory {}!", new File(Constants.RESOURCEFOLDER_TEXTFILE).getAbsolutePath(), e);
			return null;
		  }

		  return programStrings;
	}

	/**
	 * Writes the rule set of a given {@link ASPProgram}-object to a temporary textfile.
	 * The created file is not persisted as in {@link #persist(ASPProgram, String)}, and will be lost when the application terminates!
	 * @param program {@link ASPProgram}-object that provides the rules which will be written in the textfile
	 * @return A temporary textfile for export operations
	 */
	public File exportProgram(ASPProgram<?, ?> program)
	{
		if(program == null || program.getProgramName() == null)
		{
			LOGGER.warn("The program to export or its name is null/empty! Therefore, no file can be generated.");
			return null;
		}

		try
		{
			File programFile = File.createTempFile("txt", program.getProgramName().replace("\\s", "_"));
			programFile.deleteOnExit();
			FileWriter fileWriter = new FileWriter(programFile);
			StringBuilder sb = new StringBuilder();
	
			for (ASPRule<?> currentRule : program.getRuleSet())
			{
				sb.append(currentRule.toString());
				sb.append(System.lineSeparator());
			}
	
			fileWriter.write(sb.toString());
			fileWriter.close();
	
			return programFile;

		} catch (IOException e) {
			LOGGER.error("An error occured while creating the textfile to export the given ASP-Program {}!", program.getProgramName(), e);
			return null;
		}
	}
}
