package pipe;

import java.util.Iterator;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequenceRemoveLongWords extends Pipe {
	private static final long serialVersionUID = 1L;

	private int maxLength;

	public TokenSequenceRemoveLongWords(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public Instance pipe(Instance inst) {
		TokenSequence ts = (TokenSequence) inst.getData();

		Iterator<Token> iter = ts.iterator();

		while ( iter.hasNext() ) {
			if ( iter.next().getText().length() > maxLength ) {
				iter.remove();
			}
		}

		return inst;
	}
}
