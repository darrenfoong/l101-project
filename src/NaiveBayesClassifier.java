import java.util.ArrayList;

import reader.CorpusReader;
import reader.GenSpamReader;
import reader.PuReader;
import reader.TrecReader;
import cc.mallet.classify.Classification;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.InstanceList;

public class NaiveBayesClassifier {
	public static void main(String[] args) {
		final int nfolds = 10;

		String corpus = args[0];

		CorpusReader corpusReader = null;

		switch (corpus) {
			case "pu1":
				corpusReader = new PuReader();
				break;
			case "genspam":
				corpusReader = new GenSpamReader();
				break;
			case "trec07":
				corpusReader = new TrecReader();
				break;
			default: break;
		}

		corpusReader.readFeatures();
		corpusReader.readInstances();

		NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();
		InstanceList instances = corpusReader.getInstances();
		CrossValidationIterator iter = new CrossValidationIterator(instances, nfolds);

		while ( iter.hasNext() ) {
			InstanceList[] split = iter.next();
			nbTrainer.train(split[0]); // each call to train is independent
			NaiveBayes nbClassifier = nbTrainer.getClassifier();

			ArrayList<Classification> results = nbClassifier.classify(split[1]);
			Trial trial = new Trial(nbClassifier, split[1]);
			trial.addAll(results);

			// TODO: get results from trial
		}
	}
}