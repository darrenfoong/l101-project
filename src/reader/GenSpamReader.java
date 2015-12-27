package reader;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Alphabet;

public class GenSpamReader extends CorpusReader {
	boolean stop;

	public GenSpamReader(int cutoff, int[] ns, boolean stop) {
		super(cutoff, ns);
		this.stop = stop;
	}

	@Override
	public Pipe buildFeaturePipe(Alphabet dataAlphabet) {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		pipeList.add(new Input2CharSequence("UTF-8"));

		Pattern tokenPattern = Pattern.compile("[\\p{Alpha}'-]+|[\\p{Punct}]+");
		pipeList.add(new CharSequence2TokenSequence(tokenPattern));
		pipeList.add(new TokenSequenceLowercase());

		if ( stop ) {
			pipeList.add(new TokenSequenceRemoveStopwords(new File("data/bnc/top100"), "UTF-8", false, false, false));
		}

		pipeList.add(new TokenSequence2TokenSequenceNGrams(ns));

		if ( dataAlphabet == null ) {
			pipeList.add(new TokenSequence2FeatureSequence());
		} else {
			pipeList.add(new TokenSequence2FeatureSequence(dataAlphabet));
		}

		pipeList.add(new FeatureSequence2FeatureVector(true));

		pipeList.add(new Target2Label());

		return new SerialPipes(pipeList);
	}
}