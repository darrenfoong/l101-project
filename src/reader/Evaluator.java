package reader;

import java.util.ArrayList;

import cc.mallet.classify.Classification;
import cc.mallet.types.Labeling;

public class Evaluator {
	private double lambda;

	private int totalSpam;
	private int totalHam;
	private int spamToSpam;
	private int spamToHam;
	private int hamToSpam;
	private int hamToHam;

	public Evaluator(double lambda, ArrayList<Classification> results) {
		this.lambda = lambda;

		for ( Classification result : results ) {
			String targetLabel = result.getInstance().getTarget().toString();
			String classifyLabel = getBestLabel(result.getLabeling());

			if ( targetLabel.equals("spam") ) {
				totalSpam++;
				if ( classifyLabel.equals("spam") ) {
					spamToSpam++;
				} else if ( classifyLabel.equals("ham") ) {
					spamToHam++;
				}
			} else if ( targetLabel.equals("ham") ) {
				totalHam++;
				if ( classifyLabel.equals("spam") ) {
					hamToSpam++;
				} else if ( classifyLabel.equals("ham") ) {
					hamToHam++;
				}
			}
		}
	}

	public String getBestLabel(Labeling labeling) {
		double spamProb = labeling.valueAtLocation(0);
		double hamProb = labeling.valueAtLocation(1);

		if ( spamProb > lambda * hamProb ) {
			return "spam";
		} else {
			return "ham";
		}
	}

	public double getAccuracy() {
		return ((double) (spamToSpam + hamToHam))/((double) (totalSpam + totalHam));
	}

	public double getError() {
		return ((double) (spamToHam + hamToSpam))/((double) (totalSpam + totalHam));
	}

	public double getWeightedAccuracy() {
		return ((double) (spamToSpam + lambda*hamToHam))/((double) (totalSpam + lambda*totalHam));
	}

	public double getWeightedError() {
		return ((double) (spamToHam + lambda*hamToSpam))/((double) (totalSpam + lambda*totalHam));
	}

	public double getWeightedAccuracyBaseline() {
		return ((double) lambda*totalHam)/((double) (lambda*totalHam + totalSpam));
	}

	public double getWeightedErrorBaseline() {
		return ((double) totalSpam)/((double) (lambda*totalHam + totalSpam));
	}

	public double getTotalCostRatio() {
		return ((double) totalSpam)/((double) (lambda*hamToSpam + spamToHam));
	}

	public double getSpamPrecision() {
		return ((double) spamToSpam)/((double) (spamToSpam + spamToHam));
	}

	public double getSpamRecall() {
		return ((double) spamToSpam)/((double) (spamToSpam + hamToSpam));
	}

	public String getStats() {
		String output = "";
		output += "totalSpam: " + totalSpam + "; ";
		output += "totalHam: " + totalHam + "; ";
		output += "spamToSpam: " + spamToSpam + "; ";
		output += "spamToHam: " + spamToHam + "; ";
		output += "hamToSpam: " + hamToSpam + "; ";
		output += "hamToHam: " + hamToHam;
		return output;
	}
}
