package classify;

import reader.CorpusReader;
import cc.mallet.classify.Classification;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelVector;

public class NaiveBayesMB extends NaiveBayes {
	private static final long serialVersionUID = 1L;

	private CorpusReader corpusReader;
	private boolean smoothing;

	public NaiveBayesMB(CorpusReader corpusReader, boolean smoothing) {
		super(null, null, null);
		this.corpusReader = corpusReader;
		this.smoothing = smoothing;
	}

	@Override
	public Classification classify(Instance instance) {
		FeatureVector featureVector = (FeatureVector) instance.getData();
		int numFeatures = featureVector.getAlphabet().size();

		double[] logpr = new double[2];

		for ( int c = 0; c < logpr.length; c++ ) {
			logpr[c] = corpusReader.getCDist().getLogProbS(c, smoothing);

			boolean sparseVector[] = new boolean[numFeatures];

			for ( int i = 0; i < featureVector.numLocations(); i++ ) {
				int index = (int) featureVector.indexAtLocation(i);
				sparseVector[index] = true;
			}

			for ( int i = 0; i < numFeatures; i++ ) {
				int value = sparseVector[i] ? 1 : 0;
				logpr[c] += corpusReader.getXCDists()[i].getLogProbS(convert(c, value), smoothing) - corpusReader.getCDist().getLogProbS(c, smoothing);
			}

			logpr[c] = Math.exp(logpr[c]);
		}

		return new Classification(instance, this, new LabelVector((LabelAlphabet) instance.getTargetAlphabet(), logpr));
	}

	private int convert(int c, int value) {
		if ( c == 0 ) {
			if ( value == 0 ) {
				return CorpusReader.SPAM0;
			} else if ( value == 1 ) {
				return CorpusReader.SPAM1;
			} else {
				return -1;
			}
		} else if ( c == 1 ) {
			if ( value == 0 ) {
				return CorpusReader.HAM0;
			} else if ( value == 1 ) {
				return CorpusReader.HAM1;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}
}
