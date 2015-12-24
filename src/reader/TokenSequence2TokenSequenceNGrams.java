package reader;

import java.util.ArrayList;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequence2TokenSequenceNGrams extends Pipe {
	private static final long serialVersionUID = 1L;

	private int[] ns;

	public TokenSequence2TokenSequenceNGrams(int[] ns) {
		if ( ns != null ) {
			this.ns = ns.clone();
		}
	}

	@Override
	public Instance pipe(Instance inst) {
		if ( ns != null && ns.length != 0 ) {
			TokenSequence ts = (TokenSequence) inst.getData();

			/*
			System.out.println("Current token sequence: ");
			for ( Token t : ts ) {
				System.out.print(t.getText()+" ");
			}
			System.out.println();
			*/

			// TODO: check the "span" thing in PropertyList

			int numTokens = ts.size();

			for ( int i = 0; i < ns.length; i++ ) {
				int n = ns[i];

				ArrayList<Token> ngrams = new ArrayList<Token>();

				for ( int j = 0; j < numTokens-n+1; j++ ) {
					StringBuilder ngramBuilder = new StringBuilder();

					for ( int k = 0; k < n; k++ ) {
						ngramBuilder.append(ts.get(j+k).getText());
						ngramBuilder.append("_");
					}

					//System.out.println("Add " + ngram);
					ngrams.add(new Token(ngramBuilder.toString()));
				}

				ts.addAll(ngrams);
			}
		}

		return inst;
	}
}
