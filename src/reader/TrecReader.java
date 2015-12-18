package reader;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;

public class TrecReader extends CorpusReader {
	public TrecReader(int cutoff) {
		super(cutoff);
	}

	@Override
	public Pipe buildFeaturePipe(Alphabet dataAlphabet) {
		// TODO Auto-generated method stub
		return null;
	}
}