package utils;

public class Distribution {
	private int[] dist;
	private int sum;

	public Distribution(int size) {
		dist = new int[size];
		sum = 0;
	}

	public void add(int i) {
		dist[i]++;
		sum++;
	}

	public void add(int i, int n) {
		dist[i] += n;
		sum += n;
	}

	public double getProb(int i) {
		// add-one smoothing
		return ((double) (dist[i]+1))/((double) (sum+dist.length));
	}

	public double getLogProb(int i) {
		// add-one smoothing
		return Math.log((double) (dist[i]+1)) - Math.log((double) (sum+dist.length));
	}
}
