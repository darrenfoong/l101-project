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
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.InstanceList;

public class NaiveBayesClassifier {
	private static final int NFOLDS = 10;
	private static final int CUTOFF = 50;
	private static final double LAMBDA = 1;

	private static CorpusReader corpusReader = null;
	private static String directory;

	private static Statistics stats = new Statistics();

	public static void main(String[] args) {
		String corpus = args[0];

		switch (corpus) {
			case "pu1":
				corpusReader = new PuReader(CUTOFF);
				String pu1set = "lemm_stop";

				if ( args.length == 2 ) {
					pu1set = args[1]; // bare, lemm, lemm_stop, stop
					System.out.println("Subset " + pu1set + " used.");
				} else {
					System.out.println("No subset of PU1 selected: subset " + pu1set + " used.");
				}

				directory = "data/pu1/" + pu1set + "/";
				System.out.println("Starting PU1 reader.");
				break;
			case "genspam":
				corpusReader = new GenSpamReader(CUTOFF);
				directory = "data/genspam/";
				System.out.println("Starting GenSpam reader.");
				break;
			case "trec07p":
				corpusReader = new TrecReader(CUTOFF);
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

				runNaiveBayes(trainDirectoriesArray, new File[] {testDirectory});
			}

			System.out.println("Average TCR: " + stats.getAverage("tcr"));

			return;
		}

		if ( corpus.equals("genspam") || corpus.equals("trec07p") ) {
			File trainDirectory = new File(directory + "train/");
			File testDirectory = new File(directory + "test/");

			runNaiveBayes(new File[] {trainDirectory}, new File[] {testDirectory});

			return;
		}

		corpusReader.readFeatures(new File[] {new File(directory)});
		corpusReader.computeMutualInfo();
		corpusReader.pruneAlphabet();
		corpusReader.changeAllAlphabets();

		System.out.println("Naive Bayes classifer initialised.");

		NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();
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
	}

	private static void runNaiveBayes(File[] trainDirectoriesArray, File[] testDirectoriesArray) {
		corpusReader.readFeatures(trainDirectoriesArray);
		corpusReader.computeMutualInfo();
		corpusReader.pruneAlphabet();

		corpusReader.readData(testDirectoriesArray);

		System.out.println("Naive Bayes classifer initialised.");

		NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();
		InstanceList featureInstances = corpusReader.getFeatureInstances();
		InstanceList dataInstances = corpusReader.getDataInstances();

		nbTrainer.train(featureInstances);
		NaiveBayes nbClassifier = nbTrainer.getClassifier();

		ArrayList<Classification> results = nbClassifier.classify(dataInstances);
		Evaluator evaluator = new Evaluator(LAMBDA, results);
		System.out.println("Evaluator initialised with lambda = " + LAMBDA);

		double tcr = evaluator.getTotalCostRatio();
		stats.put("tcr", tcr);
		System.out.println(" TCR: " + tcr);

		System.out.println();
	}
}