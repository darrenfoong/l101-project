package reader;

import java.util.ArrayList;

import utils.DoubleDoublePair;

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

	private ArrayList<Classification> results;

	public Evaluator(double lambda, ArrayList<Classification> results) {
		this.lambda = lambda;
		this.results = results;

		processResults();
	}

	public Evaluator(double lambda, int totalSpam, int totalHam, int spamToSpam, int spamToHam, int hamToSpam, int hamToHam) {
		this.lambda = lambda;
		this.totalSpam = totalSpam;
		this.totalHam = totalHam;
		this.spamToSpam = spamToSpam;
		this.spamToHam = spamToHam;
		this.hamToSpam = hamToSpam;
		this.hamToHam = hamToHam;
	}

	private void processResults() {
		totalSpam = 0;
		totalHam = 0;
		spamToSpam = 0;
		spamToHam = 0;
		hamToSpam = 0;
		hamToHam = 0;

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

	private String getBestLabel(Labeling labeling) {
		double spamProb = labeling.valueAtLocation(0);
		double hamProb = labeling.valueAtLocation(1);

		if ( spamProb > lambda * hamProb ) {
			return "spam";
		} else {
			return "ham";
		}
	}

	public int getTotalSpam() {
		return totalSpam;
	}

	public int getTotalHam() {
		return totalHam;
	}

	public int getSpamToSpam() {
		return spamToSpam;
	}

	public int getSpamToHam() {
		return spamToHam;
	}

	public int getHamToSpam() {
		return hamToSpam;
	}

	public int getHamToHam() {
		return hamToHam;
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
		return ((double) spamToSpam)/((double) (spamToSpam + hamToSpam));
	}

	public double getSpamRecall() {
		return ((double) spamToSpam)/((double) (spamToSpam + spamToHam));
	}

	public double getHamPrecision() {
		return ((double) hamToHam)/((double) (hamToHam + spamToHam));
	}

	public double getHamRecall() {
		return ((double) hamToHam)/((double) (hamToHam + hamToSpam));
	}

	public double getWeightedSpamPrecision() {
		return ((double) spamToSpam)/((double) (spamToSpam + lambda*hamToSpam));
	}

	public String getRawStats() {
		String output = "";
		output += "totalSpam: " + totalSpam + "; ";
		output += "totalHam: " + totalHam + "; ";
		output += "spamToSpam: " + spamToSpam + "; ";
		output += "spamToHam: " + spamToHam + "; ";
		output += "hamToSpam: " + hamToSpam + "; ";
		output += "hamToHam: " + hamToHam;
		return output;
	}

	public String getStats() {
		String output = "";
		output += "sp: " + getSpamPrecision() + "; ";
		output += "sr: " + getSpamRecall() + "; ";
		output += "wacc: " + getWeightedAccuracy() + "; ";
		output += "tcr: " + getTotalCostRatio();
		return output;
	}

	public ArrayList<DoubleDoublePair> getRoc(ArrayList<Double> lambdas) {
		ArrayList<DoubleDoublePair> roc = new ArrayList<DoubleDoublePair>();

		double oldLambda = this.lambda;

		for ( double lambda : lambdas ) {
			this.lambda = lambda;
			processResults();
			roc.add(new DoubleDoublePair(getSpamRecall(), getHamRecall()));
		}

		this.lambda = oldLambda;
		processResults();

		return roc;
	}
}
