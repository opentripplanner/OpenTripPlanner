package org.opentripplanner.ext.airquality;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.ext.airquality.configuration.DavaOverlayConfig;
import org.opentripplanner.ext.airquality.configuration.IndexVariable;
import org.opentripplanner.ext.airquality.configuration.ParameterType;
import org.opentripplanner.ext.airquality.configuration.RequestParameters;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class parses the settings from data-settings.json data source and formats them based on use-case (input grid data
 * parsing, request parameters)
 *
 * @author Katja Danilova
 */
public class GenericFileConfigurationParser {
	private static final Logger LOG = LoggerFactory.getLogger(GenericFileConfigurationParser.class);

	/**
	 * Parses settings file, verifies the contents and returns the parsed GenericFileConfiguration[]
	 *
	 *
	 * @param node json node
	 * @param path path to file
	 * @return json data parsed into GenericFileConfiguration[]
	 */
	public static DavaOverlayConfig parse(JsonNode node, String path) {
		DavaOverlayConfig configuration = null;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			configuration = objectMapper.treeToValue(node, DavaOverlayConfig.class);
			RequestParameters[] requestParametersList = configuration.getRequestParameters();
			if (!areIndexVariablesValid(Arrays.asList(configuration.getIndexVariables()))) {
				throw new IllegalArgumentException("The settings file has incorrect index variables");
			}

			if (!areRequestParamValid(Arrays.asList(requestParametersList))) {
				throw new IllegalArgumentException("The settings file has incorrect request parameters");
			}

		} catch (JsonProcessingException ex) {
			LOG.error(ex.getMessage());
		}

		return configuration;

	}

	/**
	 * Verifies that the index variables do not have repeating pollutant names
	 *
	 * @param indexVariables list of index variables from json settings file
	 * @return validity of index variables settings
	 */
	private static boolean areIndexVariablesValid(List<IndexVariable> indexVariables) {
		for (IndexVariable indexVariable : indexVariables) {
			if (indexVariables.stream().filter( i -> indexVariable.getName().equals(i.getName())).count() > 1) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Verifies that in the configuration there are exactly 2 corresponding request parameters per one variable name
	 *
	 * @param requestParametersList list of request parameters from json file
	 * @return validity of the request parameters list
	 */
	public static boolean areRequestParamValid(List<RequestParameters> requestParametersList) {
		AtomicBoolean res = new AtomicBoolean(true);
		requestParametersList.stream()
			.collect(Collectors.groupingBy(RequestParameters::getVariable))
			.forEach((k, v) -> {
				if (v.size() != 2)
					res.set(false);
			});
		return res.get();
	}

	/**
	 * Parses the generic file configuration into pairs of RequestParameters, where key = threshold parameter,
	 * value = penalty parameter. These parameters will be filled during request and routing process
	 *
	 * @param davaOverlayConfig settings from data-settings.json
	 * @return map of request parameters
	 */
	public static Map<RequestParameters, RequestParameters> parseConfParam(DavaOverlayConfig davaOverlayConfig) {
		Map<RequestParameters, RequestParameters> requestPairs = new HashMap<>();
		if (davaOverlayConfig == null) {
			return null;
		}

		RequestParameters[] requestParameters = davaOverlayConfig.getRequestParameters();

		Arrays.stream(requestParameters).collect(Collectors.groupingBy(RequestParameters::getVariable))
			.forEach((k, v) -> {
				RequestParameters penalty = null, threshold = null;
				for (RequestParameters fittingParam : v) {
					if (fittingParam.getParameterType().equals(ParameterType.THRESHOLD)) {
						threshold = fittingParam;
					} else if (fittingParam.getParameterType().equals(ParameterType.PENALTY)) {
						penalty = fittingParam;
					}
				}
				requestPairs.put(threshold, penalty);

			});
		return requestPairs;
	}
}
