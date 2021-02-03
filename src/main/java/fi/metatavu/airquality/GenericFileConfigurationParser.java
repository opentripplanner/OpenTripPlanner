package fi.metatavu.airquality;

import com.google.gson.Gson;
import fi.metatavu.airquality.configuration_parsing.GenericFileConfiguration;
import fi.metatavu.airquality.configuration_parsing.ParameterType;
import fi.metatavu.airquality.configuration_parsing.RequestParameters;
import org.opentripplanner.datastore.DataSource;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class parses the settings from settings.json data source and formats them based on use-case (input grid data
 * parsing, request parameters)
 *
 * @author Katja Danilova
 */
public class GenericFileConfigurationParser {

	/**
	 * Parses settings.json data source, verifies the contents and returns the parsed GenericFileConfiguration[]
	 *
	 * @param dataSource dataSource from settings.json file
	 * @return GenericFileConfiguration[] json data parsed into GenericFileConfiguration[]
	 */
	public static GenericFileConfiguration[] parse(DataSource dataSource) {
		if (dataSource.exists()) {
			GenericFileConfiguration[] configurations = new Gson().fromJson(new InputStreamReader(dataSource.asInputStream()),
							GenericFileConfiguration[].class);
			for (GenericFileConfiguration configuration : configurations){
				RequestParameters[] requestParametersList = configuration.getRequestParameters();
				if (!areRequestParamValid(Arrays.asList(requestParametersList))) {
					throw new IllegalArgumentException("The settings file has incorrect request parameters");
				}

			}
			return configurations;
		}
		return null;
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
	 * @param configurationsArray array of all settings from settings.json
	 * @return map of request parameters
	 */
	public static Map<RequestParameters, RequestParameters> parseConfParam(GenericFileConfiguration[] configurationsArray) {
		Map<RequestParameters, RequestParameters> requestPairs = new HashMap<>();
		if (configurationsArray == null) {
			return null;
		}

		for (GenericFileConfiguration genericFileConfiguration : configurationsArray){
			RequestParameters[] requestParameters = genericFileConfiguration.getRequestParameters();

			Arrays.stream(requestParameters)
							.collect(Collectors.groupingBy(RequestParameters::getVariable))
							.forEach((k, v) -> {
								RequestParameters penalty = null, threshold = null;
								for (RequestParameters fittingParam : v) {
									if (fittingParam.getParameterType().equals(ParameterType.THRESHOLD)) {
										threshold = fittingParam;
									} else if (fittingParam.getParameterType().equals(ParameterType.PENALTY)) {
										penalty = fittingParam;
									}

									requestPairs.put(threshold, penalty);
								}
							});
		}
		return requestPairs;
	}
}
