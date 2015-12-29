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

	public int getCount(int i) {
		return dist[i];
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

	public double getLogProbS(int i, boolean smoothing) {
		if ( smoothing ) {
			return Math.log((double) (dist[i]+1)) - Math.log((double) (sum+dist.length));
		} else {
			return getLogProb(i);
		}
	}
}
