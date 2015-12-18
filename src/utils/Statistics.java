package utils;

import java.util.ArrayList;
import java.util.HashMap;

public class Statistics {
	private HashMap<String, ArrayList<Double>> statsMap;

	public Statistics() {
		statsMap = new HashMap<String, ArrayList<Double>>();
	}

	public void put(String label, double value) {
		if ( statsMap.containsKey(label) ) {
			statsMap.get(label).add(value);
		} else {
			ArrayList<Double> valueList = new ArrayList<Double>();
			valueList.add(value);
			statsMap.put(label, valueList);
		}
	}

	public double getAverage(String label) {
		if ( statsMap.containsKey(label) ) {
			double sum = 0.0;
			ArrayList<Double> valueList = statsMap.get(label);

			for ( double value : valueList ) {
				sum += value;
			}

			return sum/((double) valueList.size());
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void clear(String label) {
		if ( statsMap.containsKey(label) ) {
			statsMap.get(label).clear();
		}
	}

	public void clearAll() {
		statsMap.clear();
	}
}
