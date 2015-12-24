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
		if ( sum == 0 ) {
			return 0;
		} else {
			return ((double) dist[i])/((double) sum);
		}
	}

	public double getLogProb(int i) {
		if ( sum == 0 ) {
			return Double.NEGATIVE_INFINITY;
		} else {
			return Math.log((double) dist[i]) - Math.log((double) sum);
		}
	}
}
