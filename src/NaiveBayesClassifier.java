import java.io.File;
import java.util.ArrayList;

import reader.CorpusReader;
import reader.Evaluator;
import reader.GenSpamReader;
import reader.PuReader;
import reader.TrecReader;
import utils.Statistics;
import cc.mallet.classify.Classification;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Multinomial;

public class NaiveBayesClassifier {
	private static final int[] NS = null;
	private static final boolean SMOOTHING = true;

	private static CorpusReader corpusReader = null;
	private static String directory;

	private static Statistics stats = new Statistics();

	public static void main(String[] args) {
		String corpus = args[0];
		String subset = args[1];
		int cutoff = Integer.parseInt(args[2]);
		double lambda = Double.parseDouble(args[3]);
		boolean stop = false;

		switch (corpus) {
			case "pu1":
				System.out.println("Subset " + subset + " used.");

				corpusReader = new PuReader(cutoff, NS);
				directory = "data/pu1/" + subset + "/";
				System.out.println("Starting PU1 reader.");
				break;
			case "genspam":
				if ( subset == "lemm_stop" ) {
					subset = "lemm";
					stop = true;
				}

				if ( subset == "stop" ) {
					subset = "bare";
					stop = true;
				}

				System.out.println("Subset " + subset + " used.");

				corpusReader = new GenSpamReader(cutoff, NS, stop);
				directory = "data/genspam/" + subset + "/";
				System.out.println("Starting GenSpam reader.");
				break;
			case "trec07p":
				if ( subset == "lemm_stop" ) {
					subset = "lemm";
					stop = true;
				}

				if ( subset == "stop" ) {
					subset = "bare";
					stop = true;
				}

				System.out.println("Subset " + subset + " used.");

				corpusReader = new TrecReader(cutoff, NS, stop);
				directory = "data/trec07p/";
				System.out.println("Starting TREC 2007 reader.");
				break;
			default:
				System.err.println("Invalid reader type.");
				return;
		}

		if ( corpus.equals("pu1") ) {
			// custom 10-fold cross-validation
			System.out.println("Cross-validation on 10 folds.");

			for ( int i = 1; i <= 10; i++ ) {
				System.out.println("Fold " + i);

				ArrayList<File> trainDirectories = new ArrayList<File>();

				for ( int j = 1; j <= 10; j++ ) {
					if ( j != i ) {
						// add all partj except for current i,
						// which is used for evaluation
						trainDirectories.add(new File(directory + "part" + j + "/"));
					}
				}

				File[] trainDirectoriesArray = new File[trainDirectories.size()];

				for ( int j = 0; j < trainDirectories.size(); j++ ) {
					trainDirectoriesArray[j] = trainDirectories.get(j);
				}

				File testDirectory = new File(directory + "part" + i + "/");

				runNaiveBayes(trainDirectoriesArray, new File[] {testDirectory}, lambda);
			}

			Evaluator evaluator = new Evaluator(lambda,
									(int) stats.getSum("totalSpam"),
									(int) stats.getSum("totalHam"),
									(int) stats.getSum("spamToSpam"),
									(int) stats.getSum("spamToHam"),
									(int) stats.getSum("hamToSpam"),
									(int) stats.getSum("hamToHam"));

			System.out.println(evaluator.getRawStats());
			System.out.println(evaluator.getStats());

			System.out.println();

			System.out.println("Macroaverage spam precision: " + stats.getAverage("sp"));
			System.out.println("Macroaverage spam recall: " + stats.getAverage("sr"));
			System.out.println("Macroaverage weighted accuracy: " + stats.getAverage("wacc"));
			System.out.println("Macroaverage TCR: " + stats.getAverage("tcr"));

			System.out.println("Macro stats: " + stats.getAverage("sp") + " " + stats.getAverage("sr") + " " + stats.getAverage("wacc") + " " + stats.getAverage("tcr"));

			return;
		}

		if ( corpus.equals("genspam") || corpus.equals("trec07p") ) {
			File trainDirectory = new File(directory + "train/");
			File testDirectory = new File(directory + "test/");

			runNaiveBayes(new File[] {trainDirectory}, new File[] {testDirectory}, lambda);

			return;
		}

		/*
		corpusReader.readFeatures(new File[] {new File(directory)});
		corpusReader.computeMutualInfo();
		corpusReader.pruneAlphabet();
		corpusReader.changeAllAlphabets();

		System.out.println("Naive Bayes classifer initialised.");

		NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();

		if ( !SMOOTHING ) {
			nbTrainer.setFeatureMultinomialEstimator(new Multinomial.MLEstimator());
			nbTrainer.setPriorMultinomialEstimator(new Multinomial.MLEstimator());
		}

		InstanceList instances = corpusReader.getFeatureInstances();

		System.out.println("Cross-validation on " + NFOLDS + " folds.");

		CrossValidationIterator iter = new CrossValidationIterator(instances, NFOLDS);
		int i = 1;

		while ( iter.hasNext() ) {
			System.out.println("Fold " + i);
			InstanceList[] split = iter.next();
			nbTrainer.train(split[0]); // each call to train is independent
			NaiveBayes nbClassifier = nbTrainer.getClassifier();

			ArrayList<Classification> results = nbClassifier.classify(split[1]);
			Evaluator evaluator = new Evaluator(LAMBDA, results);
			System.out.println("Evaluator initialised with lambda = " + LAMBDA);

			double tcr = evaluator.getTotalCostRatio();
			stats.put("tcr", tcr);
			System.out.println(" TCR: " + tcr);

			System.out.println();
			i++;
		}

		System.out.println("Average TCR: " + stats.getAverage("tcr"));
		*/
	}

	private static void runNaiveBayes(File[] trainDirectoriesArray, File[] testDirectoriesArray, double lambda) {
		corpusReader.readFeatures(trainDirectoriesArray);
		corpusReader.computeMutualInfo();
		corpusReader.pruneAlphabet();
		corpusReader.changeAllAlphabets();

		corpusReader.readData(testDirectoriesArray);

		System.out.println("Naive Bayes classifer initialised.");

		NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();

		if ( !SMOOTHING ) {
			nbTrainer.setFeatureMultinomialEstimator(new Multinomial.MLEstimator());
			nbTrainer.setPriorMultinomialEstimator(new Multinomial.MLEstimator());
		}

		InstanceList featureInstances = corpusReader.getFeatureInstances();
		InstanceList dataInstances = corpusReader.getDataInstances();

		nbTrainer.train(featureInstances);
		NaiveBayes nbClassifier = nbTrainer.getClassifier();

		ArrayList<Classification> results = nbClassifier.classify(dataInstances);
		Evaluator evaluator = new Evaluator(lambda, results);
		System.out.println("Evaluator initialised with lambda = " + lambda);

		stats.put("tcr", evaluator.getTotalCostRatio());
		stats.put("sp", evaluator.getSpamPrecision());
		stats.put("sr", evaluator.getSpamRecall());
		stats.put("wacc", evaluator.getWeightedAccuracy());

		stats.put("totalSpam", evaluator.getTotalSpam());
		stats.put("totalHam", evaluator.getTotalHam());
		stats.put("spamToSpam", evaluator.getSpamToSpam());
		stats.put("spamToHam", evaluator.getSpamToHam());
		stats.put("hamToSpam", evaluator.getHamToSpam());
		stats.put("hamToHam", evaluator.getHamToHam());

		System.out.println(evaluator.getRawStats());
		System.out.println(evaluator.getStats());

		System.out.println();
	}
}