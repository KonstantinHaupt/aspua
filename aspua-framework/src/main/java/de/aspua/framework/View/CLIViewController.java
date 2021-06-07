package de.aspua.framework.View;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import de.aspua.framework.Controller.ASPUAFrameworkAPI;
import de.aspua.framework.Controller.ControllerInterfaces.IFactoryController;
import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Utils.Constants;

/**
 * Invocation: 
 *  -> mvn exec:java (Default arguments)
 *  -> mvn exec:java -D"exec.args"="[-f <factoryclassname>] [-p <fileName1> <fileName2>]"
 */
public class CLIViewController
{
    /** 
     * Static Scanner-Object because close()-Method closes System.in-Source too.
     * Scanner throws Exceptions if you initialize the scanner-Object twice and try to read in from System.in again.
     */
    private static Scanner scanner = new Scanner(System.in);
    private static String programName = "Example_Thesis_Initial";
    private static String newProgramName = "Example_Thesis_Update";
    private static String factoryClassName = Constants.PACKAGE_PREFIX + "CausalRejectionController.CRFileFactory";

    private static ASPUAFrameworkAPI frameworkAPI;
    private static List<Conflict> conflicts;

    public static void main(String[] args)
    {
        evaluateArguments(args);

        IFactoryController usedFactory = buildFactory();
        frameworkAPI = new ASPUAFrameworkAPI(usedFactory);
        readInProgram(programName);
        readInProgram(newProgramName);

        conflicts = frameworkAPI.detectConflicts();
        while(conflicts != null && !conflicts.isEmpty())
            handleConflicts();
        
        if(conflicts == null)
        {
            System.out.println("There seems to be a problem with computing occuring conflicts in the Update-Sequence.");
            System.out.println("Please check the Application-Logs to get further information.");
        }
        else
        {
            System.out.println("The Update-Sequence doesn't contain any conflicts.");
            System.out.println("Continue with merging the Programs of the Update-Sequence.");
            System.out.println("Should the original ASP-Program be overwritten? [Y/N]");

            boolean overwrite = evaluateOverwrite();
            boolean success;
            if(overwrite)
                success = frameworkAPI.persistUpdatedProgram(programName);
            else
            {
                LocalDateTime dateTime = LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
				String formattedDate = formatter.format(dateTime);
				formattedDate = formattedDate.replace(" ", "_");
				formattedDate = formattedDate.replace(":", "-");

				String fileName = programName + "_" + formattedDate;
                success = frameworkAPI.persistUpdatedProgram(fileName);
            }

            if(success)
            {
                System.out.println("The Update-Process was successfull.");
                scanner.close();
                System.exit(0);
            }
            else
            {
                System.out.println("There was a Problem with merging the Update-Sequence or persisting the ASP-Program!");
                scanner.close();
                System.exit(1);
            }
        }
    }
    
    private static void evaluateArguments(String[] args)
    {
        if(args.length > 0)
        {
            for(int i = 0; i < args.length; i = i+2)
            {
                switch (args[i])
                {
                    case "-p":
                        if(args.length < i+3)
                            failureExit();

                        programName = args[i+1];
                        newProgramName = args[i+2];
                        // Increase argument counter because two arguments were given
                        i++;
                        break;
                    case "-f":
                        if(args.length < i+2)
                            failureExit();

                        factoryClassName = Constants.PACKAGE_PREFIX + args[i+1];
                        break;
                    default:
                        failureExit();
                        break;
                }
            }
        }
    }

    private static void failureExit()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid argument syntax! A command option always has to refer to a following argument!");
        sb.append(System.lineSeparator());
        sb.append("Expected syntax: mvn exec:java -D\"exec.args\"=\"[-f <factoryclassname>] [-p <fileName1> <fileName2>]\"");
        sb.append(System.lineSeparator());
        sb.append("-f <factoryclassname>: Choose the factory which is used for the execution. The factoryclassname has to specify the classname with its subpackage starting from 'de.aspua.framework.Controller'.");
        sb.append(System.lineSeparator());
        sb.append("-p <fileName1> <fileName2>: Choose the programs which will be used for the update process. 'fileName1' determines the older program which is updated by the program 'fileName2'.");
        sb.append(System.lineSeparator());
        sb.append("File endings such as '.txt' can be omitted (Depending on the used IOController of the factory).");
        System.out.println(sb.toString());
        System.exit(1);
    }

    private static void handleConflicts()
    {
        System.out.println(String.format("The Framework detected %s conflict(s) in the Update-Sequence:", conflicts.size()));
        System.out.println("-----------------------------------------");
        for(int i = 0; i < conflicts.size(); i++)
        {
            System.out.println("Conflict Nr. " + i);
            System.out.println(conflicts.get(i).toString());
            System.out.println("-----------------------------------------");
        }

        int conflictNumber = -1;
        while(conflictNumber < 0 || conflictNumber >= conflicts.size())
        {
            try
            {
                System.out.println("Please enter the number of the conflict you would like to solve: ");
                conflictNumber = scanner.nextInt();

                if(conflictNumber < 0 || conflictNumber >= conflicts.size())
                    System.out.println("There isn't a conflict with the entered number!");
            } catch (InputMismatchException e) {
                // Consume invalid input in order to receive new input
                scanner.next();
                System.out.println("Please enter a valid number!");
            }
        }

        Conflict conflictChoice = conflicts.get(conflictNumber);
        String solutionChoice = "";
        int solutionNumber = -1;
        boolean validInput = false;
        while(!validInput)
        {
            System.out.println(String.format("Please enter the number of the solution you would like to apply for conflict %s.", conflictNumber));
            solutionChoice = scanner.next();
            try
            {
                solutionNumber = Integer.parseInt(solutionChoice);

                if(0 <= solutionNumber && solutionNumber < conflictChoice.getSolutions().size())
                    validInput = true;
                else
                {
                    System.out.println("There isn't a solution with the entered number for the chosen conflict!");
                    solutionNumber = -1;
                }
            } catch (NumberFormatException e) {
                if("EXPERTMODE".equals(solutionChoice.toUpperCase()))
                    validInput = true;
                else
                    System.out.println("Invalid option!");
            }
        }

        if(solutionNumber != -1)
            conflicts = frameworkAPI.solveConflict(conflictChoice.getSolutions().get(solutionNumber));
    }

    private static boolean evaluateOverwrite()
    {
        String overwrite = scanner.nextLine();
        
        while(!"Y".equals(overwrite) && !"N".equals(overwrite))
        {
            System.out.println("Please enter 'Y' to overwrite the original ASP-Program and 'N' to create a new file.");
            overwrite = scanner.nextLine();
        }

        return "Y".equals(overwrite);
    }

    private static void readInProgram(String chosenProgram)
    {
        boolean success = frameworkAPI.addToUpdateSequence(chosenProgram);
        if(success)
        {
            System.out.println(String.format("Loaded the ASP-Program '%s' and placed it at the %s.Position of the Update-Sequence.", chosenProgram, frameworkAPI.getUpdateSequence().size()));
            System.out.println(frameworkAPI.getUpdateSequence().get(frameworkAPI.getUpdateSequence().size()-1).toString());
        }
        else
        {
            System.out.println("There was a problem with reading in the initial Program! The application will terminate now.");
            System.exit(1);
        }
    }

    private static IFactoryController buildFactory()
    {
        try
        {
            Class<?> clazz = Class.forName(factoryClassName);
            Constructor<?> ctr = clazz.getConstructor();
            return (IFactoryController) ctr.newInstance();
        }
        catch( ClassNotFoundException e ) {
            System.out.println(String.format("The given Factory-Class '{}' does not exist!", factoryClassName));
            System.out.println(e);
        }
        catch( Exception e ) {
            System.out.println("An error accured during the instantiation of the Factory-Class!");
            System.out.println(e);
        }
        return null;
    }
}