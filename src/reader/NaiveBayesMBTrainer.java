package reader;

import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.types.InstanceList;

public class NaiveBayesMBTrainer extends NaiveBayesTrainer {
	private static final long serialVersionUID = 1L;

	private CorpusReader corpusReader;
	private boolean smoothing;

	public NaiveBayesMBTrainer(CorpusReader corpusReader, boolean smoothing) {
		this.corpusReader = corpusReader;
		this.smoothing = smoothing;
	}

	@Override
	public NaiveBayes train(InstanceList featureInstances) {
		return new NaiveBayesMB(corpusReader, smoothing);
	}
}
