import java.util.ArrayList;

import reader.CorpusReader;
import reader.Evaluator;
import reader.GenSpamReader;
import reader.PuReader;
import reader.TrecReader;
import cc.mallet.classify.Classification;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.InstanceList;

public class NaiveBayesClassifier {
	public static void main(String[] args) {
		final int NFOLDS = 10;
		final double LAMBDA = 1;

		String corpus = args[0];

		CorpusReader corpusReader = null;

		switch (corpus) {
			case "pu1":
				corpusReader = new PuReader();
				System.out.println("Starting PU1 reader.");
				break;
			case "genspam":
				corpusReader = new GenSpamReader();
				System.out.println("Starting GenSpam reader.");
				break;
			case "trec07":
				corpusReader = new TrecReader();
				System.out.println("Starting TREC 2007 reader.");
				break;
			default:
				System.err.println("Invalid reader type.");
				return;
		}

		corpusReader.read();
		corpusReader.computeMutualInfo();
		corpusReader.pruneAlphabet();
		corpusReader.changeAllAlphabets();

		System.out.println("Naive Bayes classifer initialised.");

		NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();
		InstanceList instances = corpusReader.getInstances();

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

			System.out.println(" TCR: " + evaluator.getTotalCostRatio());
			i++;
		}
	}
}