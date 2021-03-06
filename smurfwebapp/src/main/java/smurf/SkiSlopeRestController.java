package smurf;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/ski_slopes")

public class SkiSlopeRestController {

	private final SkiSlopeRepository skiSlopeRepository;
	private final String API_KEY = "AIzaSyBg-IweUaRNn2s2iJny5Qd7Ep7nKdFoBUQ";

	@Autowired
	SkiSlopeRestController(SkiSlopeRepository skiSlopeRepository) {
		this.skiSlopeRepository = skiSlopeRepository;
	}

	@RequestMapping(method = RequestMethod.GET)
	List<SkiSlope> readSkiSlope() {
		List<SkiSlope> skislopes = this.skiSlopeRepository.findAll();
		return this.addWeatherForList(skislopes);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/slope/{slopeId}")
	SkiSlope getSkiSlope(@PathVariable Long slopeId) {
		SkiSlope skislope = this.skiSlopeRepository.findOne(slopeId);
		return this.addWeather(skislope);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{address}")
	List<SkiSlope> nearestSkiSlopes(@PathVariable String address) {
		GeocodingResult geocode = this.getLongLat(address);
		List<SkiSlope> skislopes = this.skiSlopeRepository.findByLatitudeAndLongitude(geocode.geometry.location.lat,
				geocode.geometry.location.lng);
		this.addWeatherForList(skislopes);
		return skislopes;
	}

	private GeocodingResult getLongLat(String address) {
		try {
			GeoApiContext context = new GeoApiContext().setApiKey(this.API_KEY);
			GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
			return results[0];
		} catch (ApiException | InterruptedException | IOException e) {
			return null;
		}
	}

	private List<SkiSlope> addWeatherForList(List<SkiSlope> skislopes) {
		List <SkiSlope> returningList = new ArrayList<SkiSlope>();
		for (int i=0; i < skislopes.size(); i++) {
			SkiSlope slope = skislopes.get(i);
			returningList.add(this.addWeather(slope));
		}
		return returningList;
	}
	
	private SkiSlope addWeather(SkiSlope skislope) {
		skislope.setTemperature(this.getTemperature(skislope.getAddress()));
		return skislope;
	}

	private String getTemperature(String location) {

		try {
			String baseUrl = "http://query.yahooapis.com/v1/public/yql?q={query}&format={json}";
			String query = "select item from weather.forecast where u='c' AND woeid in (select woeid from geo.places(1) where text=\""
					+ location + "\")";
			String json = "json";
			RestTemplate restTemplate = new RestTemplate();
			String jsonStr = restTemplate.getForObject(baseUrl, String.class, query, json);
			JSONObject jsonObj = new JSONObject(jsonStr);
			return jsonObj.getJSONObject("query").getJSONObject("results").getJSONObject("channel")
					.getJSONObject("item").getJSONObject("condition").getString("temp");
		} catch (JSONException e) {
			return "error";
		}

	}

}
