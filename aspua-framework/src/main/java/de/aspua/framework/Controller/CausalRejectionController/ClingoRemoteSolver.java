package de.aspua.framework.Controller.CausalRejectionController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ControllerInterfaces.ISolverController;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Utils.Constants;

/**
 * Computes answer sets by invoking the Clingo-Solver (https://potassco.org/clingo/).
 * Calls a server-API of the TU-Dortmund to use the solver via HTTP.
 */
public class ClingoRemoteSolver implements ISolverController
{
    private static Logger LOGGER = LoggerFactory.getLogger(ClingoRemoteSolver.class);

	public List<String> computeModels(ASPProgram<?, ?> program)
    {
        List<String> models = new ArrayList<>();
        if(program == null || program.getRuleSet().isEmpty())
        {
            LOGGER.warn("The given Program was null or empty. No answersets were computed.");
            return null;
        }

		HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build();

        try
        {
            // Format rule set string for request
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < program.getRuleSet().size(); i++)
            { 
                sb.append(program.getRuleSet().get(i).toString());
                sb.append(System.lineSeparator());
            }

            String requestData = "rules=" + sb.toString();
            
            // Build request
            URI solverUrl = URI.create(Constants.URI_SOLVER);
            HttpRequest request = HttpRequest.newBuilder(solverUrl)
                    .POST(HttpRequest.BodyPublishers.ofString(requestData))
                    .setHeader("Content-type", "application/x-www-form-urlencoded")
                    .build();
            
            // Send request and wait for response
            LOGGER.info("Send request to Remote-Solver.");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // print status code and body
            LOGGER.info("Received response from Remote-Solver with Status-Code {}.", response.statusCode());

            // Check if the JSON-object contains models
            JSONObject jsonObject = new JSONObject(response.body());
            if(jsonObject.getInt("count") == 0)
            {
                LOGGER.info("The given ASP-Programm doesn't contain any models.");
                return null;
            }
            else
            {
                // Read models from JSON-object
                JSONArray jsonArray = jsonObject.getJSONArray("models");
                for(int i = 0; i < jsonArray.length(); i++)
                    models.add(jsonArray.getString(i));    

                LOGGER.info("The Remote-Solver computed the following models: {} {}", System.lineSeparator(), models.toString());
                return models;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occured while building the request for the Remote-Solver!", e);
            return null;
        } catch (InterruptedException e) {
            LOGGER.error("An error occured while sending the request for the Remote-Solver! The operation was interrupted!", e);
            return null;
        } catch (JSONException e) {
            LOGGER.error("An error occured while trying to parse the Remote-Server response to a JSON-Object!", e);
            return null;
        } catch (IOException e) {
            LOGGER.error("An I/O-Error occured while sending the request to the Remote-Solver!", e);
            return null;
        }
	}
}
