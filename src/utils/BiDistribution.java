package utils;

import java.util.HashMap;

public class BiDistribution {
	private HashMap<IntPair, Integer> dist;
	private int sum;

	public BiDistribution(int size) {
		dist = new HashMap<IntPair, Integer>(size);
		sum = 0;
	}
	
	public void add(IntPair ip) {
		if ( !dist.containsKey(ip) ) {
			dist.put(ip, 1);
		} else  {
			dist.put(ip, dist.get(ip)+1);
		}

		sum++;
	}

	public void add(IntPair ip, int n) {
		if ( !dist.containsKey(ip) ) {
			dist.put(ip, n);
		} else  {
			dist.put(ip, dist.get(ip)+n);
		}

		sum += n;
	}

	public double getProb(IntPair ip) {
		return ((double) dist.get(ip))/((double) sum);
	}

	public double getLogProb(IntPair ip) {
		return Math.log((double) dist.get(ip)) - Math.log((double) sum);
	}
}
