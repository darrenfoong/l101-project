package reader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import utils.Distribution;
import utils.IntDoublePair;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public abstract class CorpusReader {
	protected final static short SPAM = 0;
	protected final static short HAM = 1;

	protected final static short SPAM0 = 0;
	protected final static short SPAM1 = 1;
	protected final static short HAM0 = 2;
	protected final static short HAM1 = 3;

	protected final static int CUTOFF = 700;

	protected InstanceList featureInstances;
	protected InstanceList instances;

	protected Distribution cDist = new Distribution(2);
	protected Distribution[] xDists = null;
	protected Distribution[] xcDists = null;

	protected ArrayList<IntDoublePair> mutualInfo = new ArrayList<IntDoublePair>();

	public InstanceList getInstances() {
		return instances;
	}

	public abstract Pipe buildFeaturePipe(Alphabet dataAlphabet);

	public InstanceList readDirectory(File directory, Alphabet dataAlphabet) {
		return readDirectories(new File[] {directory}, dataAlphabet);
	}

	public InstanceList readDirectories(File[] directories, Alphabet dataAlphabet) {;
		FileIterator iter = new FileIterator(directories,
											new TxtFilter(),
											FileIterator.LAST_DIRECTORY);

		InstanceList tempInstances = new InstanceList(buildFeaturePipe(dataAlphabet));
		tempInstances.addThruPipe(iter);
		return tempInstances;
	}

	public void readFeatures() {
		featureInstances = readDirectories(null, null);

		int numFeatures = featureInstances.getDataAlphabet().size();

		for ( int i = 0; i < numFeatures; i++ ) {
			xDists[i] = new Distribution(2);
			xcDists[i] = new Distribution(4);
		}

		Iterator<Instance> iter = featureInstances.iterator();

		while ( iter.hasNext() ) {
			Instance instance = iter.next();
			FeatureVector featureVector = (FeatureVector) instance.getData();

			cDist.add(labelToShort((int) instance.getTarget()));

			for ( int i = 0; i < featureVector.numLocations(); i++ ) {
				int featureID = featureVector.indexAtLocation(i);
				int featureValue = (int) featureVector.valueAtLocation(i);

				xDists[featureID].add(featureValue);
				xcDists[featureID].add(labelValueToShort((int) instance.getTarget(), featureValue));
			}
		}
	}

	public void computeMutualInfo() {
		int numFeatures = featureInstances.getDataAlphabet().size();

		for ( int featureID = 0; featureID < numFeatures; featureID++ ) {
			double spam0 = xcDists[featureID].getProb(SPAM0)
						* (xcDists[featureID].getLogProb(SPAM0) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(0)));
			double spam1 = xcDists[featureID].getProb(SPAM1)
					* (xcDists[featureID].getLogProb(SPAM1) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(1)));
			double ham0 = xcDists[featureID].getProb(HAM0)
					* (xcDists[featureID].getLogProb(HAM0) - (cDist.getLogProb(HAM) + xDists[featureID].getLogProb(0)));
			double ham1 = xcDists[featureID].getProb(HAM1)
					* (xcDists[featureID].getLogProb(HAM1) - (cDist.getLogProb(HAM) + xDists[featureID].getLogProb(1)));

			mutualInfo.add(new IntDoublePair(featureID, spam0 + spam1 + ham0 + ham1));
		}

		Collections.sort(mutualInfo, new Comparator<IntDoublePair>(){
			@Override
			public int compare(IntDoublePair p1, IntDoublePair p2){
				// sort in descending order
				return Double.compare(p2.getY(), p1.getY());
		}});
	}

	public Alphabet pruneAlphabet() {
		Alphabet currentAlphabet = featureInstances.getAlphabet();

		int oldIndices[] = new int[CUTOFF];
		ArrayList<Integer> newFeatures = new ArrayList<Integer>();

		for ( int i = 0; i < CUTOFF; i++ ) {
			oldIndices[i] = mutualInfo.get(i).getX();
			newFeatures.add((Integer) currentAlphabet.lookupObject(oldIndices[i]));
		}

		return new Alphabet(newFeatures.toArray());
	}

	public FeatureVector changeAlphabet(FeatureVector featureVector, Alphabet newAlphabet) {
		ArrayList<Integer> featureIndices = new ArrayList<Integer>();
		ArrayList<Double> featureValues = new ArrayList<Double>();

		for ( int i = 0; i < featureVector.numLocations(); i++ ) {
			// int featureID = featureVector.indexAtLocation(i);
			int featureValue = (int) featureVector.valueAtLocation(i);

			// TODO: danger of error code -1
			int index = newAlphabet.lookupIndex(featureValue);

			if ( index != -1 ) {
				featureIndices.add(index);
				featureValues.add((double) featureValue);
			}
		}

		assert featureIndices.size() == featureValues.size();

		int[] featureIndicesPrim = new int[featureIndices.size()];
		double[] featureValuesPrim = new double[featureValues.size()];

		for ( int i = 0; i < featureIndices.size(); i++ ) {
			featureIndicesPrim[i] = featureIndices.get(i);
			featureValuesPrim[i] = featureValues.get(i);
		}

		return new FeatureVector(newAlphabet, featureIndicesPrim, featureValuesPrim);
	}

	public void readInstances() {
		;
	}

	private short labelToShort(int label) {
		if ( label == SPAM ) {
			return SPAM;
		} else if ( label == HAM ) {
			return HAM;
		} else {
			return -1;
		}
	}

	private short labelValueToShort(int label, int value) {
		if ( label == SPAM ) {
			if ( value ==  0 ) {
				return SPAM0;
			} else if ( value == 1 ) {
				return SPAM1;
			} else {
				return -1;
			}
		} else if ( label == HAM ) {
			if ( value ==  0 ) {
				return HAM0;
			} else if ( value == 1 ) {
				return HAM1;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	private class TxtFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return file.toString().endsWith(".txt");
		}
	}
}