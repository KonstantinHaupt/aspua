package de.aspua.framework.Controller.ControllerInterfaces;

import java.util.List;
import java.util.Map;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;

/**
 * Provides an interface to read and write data.
 * It is recommended to use or adjust parameters provided by the {@link Constants}-Class to perform external operations to maintain a single source for all external folder-paths etc.
 * Caution: Different implementations may be sensitive to spaces and formatting in folder paths (/folder/subfolder vs. \folder\subfolder).
 */
public interface IIOController
{
	/**
	 * Persists a {@link ASPProgram}-object under the given name. If the given name is null or an empty string, the {@link ASPProgram#getProgramName()} is used instead.
	 * If there is an exisiting entry with the chosen name, the existing entry will be overwritten.
	 * @param program {@link ASPProgram}-object which is supposed to be persisted
	 * @param newProgramName Name which is used to persist the given object, e.g. a file name
	 * @return True if the object was successfully persisted, false otherwise
	 */
	public abstract boolean persist(ASPProgram<?, ?> program, String newProgramName);

	/**
	 * Deletes the first entry which corresponds to the given {@link ASPProgram}-object.
	 * @param program {@link ASPProgram}-object which is supposed to be deleted from the persisted data
	 * @return True if the object was successfully deleted, false otherwise
	 */
	public abstract boolean deleteProgram(ASPProgram<?, ?> program);

	/**
	 * Loads the entry with the given name. Depending on the implementation, the name for example identifies a file-name.
	 * @param programName Name of the entry which should be loaded
	 * @return String which represents the content of the loaded entry
	*/
	public abstract String loadProgram(String programName);

	/**
	 * Loads all available {@link ASPProgram}-objects from the provided source. May not be supported if an implementation doesn't persist meta-data of Java-Objects.
	 * @return List of loaded {@link ASPProgram}-objects
	 */
	public abstract List<ASPProgram<?, ?>> loadAvailableParsedPrograms();

	/**
	 * Loads all available entries from the provided source.
	 * @return Map of the form <"entryname", "content">. For logic programs, the map for example could contain entries of the form
	 * <pre>
	 * <"exampleName1", "a(x). b :- c(y, z).">
	 * <"exampleName2", "m :- not n. o.">
	 * <pre\>
	 * where exampleName1 and exampleName2 are the entry names with the corresponding content.
	 */
	public abstract Map<String, String> loadAvailableProgramStrings();
}
