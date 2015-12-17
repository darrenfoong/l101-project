package reader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import utils.Distribution;
import utils.IntDoublePair;
import utils.IntStringPair;
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

	protected final static int CUTOFF = 50;

	protected InstanceList instances;

	protected Distribution cDist = new Distribution(2);
	protected Distribution[] xDists = null;
	protected Distribution[] xcDists = null;

	protected Alphabet newAlphabet;

	protected ArrayList<IntDoublePair> mutualInfo = new ArrayList<IntDoublePair>();

	public InstanceList getInstances() {
		return instances;
	}

	public abstract Pipe buildFeaturePipe(Alphabet dataAlphabet);

	public InstanceList readDirectory(File directory, Alphabet dataAlphabet) {
		return readDirectories(new File[] {directory}, dataAlphabet);
	}

	public InstanceList readDirectories(File[] directories, Alphabet dataAlphabet) {
		FileIterator iter = new FileIterator(directories,
											new TxtFilter(),
											FileIterator.LAST_DIRECTORY);

		InstanceList tempInstances = new InstanceList(buildFeaturePipe(dataAlphabet));
		tempInstances.addThruPipe(iter);
		return tempInstances;
	}

	public void read() {
		System.out.println("Reading features from [todo].");

		instances = readDirectory(new File("data/pu1/bare/part1"), null);

		int numFeatures = instances.getDataAlphabet().size();

		System.out.println(" Number of instances: " + instances.size());
		System.out.println(" Number of features: " + numFeatures);

		xDists = new Distribution[numFeatures];
		xcDists = new Distribution[numFeatures];

		for ( int i = 0; i < numFeatures; i++ ) {
			xDists[i] = new Distribution(2);
			xcDists[i] = new Distribution(4);
		}

		Iterator<Instance> iter = instances.iterator();

		while ( iter.hasNext() ) {
			Instance instance = iter.next();
			FeatureVector featureVector = (FeatureVector) instance.getData();

			cDist.add(labelToShort((instance.getTarget().toString())));

			for ( int i = 0; i < featureVector.numLocations(); i++ ) {
				int featureID = featureVector.indexAtLocation(i);
				int featureValue = (int) featureVector.valueAtLocation(i);

				xDists[featureID].add(featureValue);
				xcDists[featureID].add(labelValueToShort(instance.getTarget().toString(), featureValue));
			}
		}

		System.out.println("Features read. Distributions created.");
	}

	public void computeMutualInfo() {
		System.out.println("Computing mutual information.");

		int numFeatures = instances.getDataAlphabet().size();

		for ( int featureID = 0; featureID < numFeatures; featureID++ ) {
			double spam0 = xcDists[featureID].getProb(SPAM0)
						* (xcDists[featureID].getLogProb(SPAM0) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(0)));
			double spam1 = xcDists[featureID].getProb(SPAM1)
					* (xcDists[featureID].getLogProb(SPAM1) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(1)));
			double ham0 = xcDists[featureID].getProb(HAM0)
					* (xcDists[featureID].getLogProb(HAM0) - (cDist.getLogProb(HAM) + xDists[featureID].getLogProb(0)));
			double ham1 = xcDists[featureID].getProb(HAM1)
					* (xcDists[featureID].getLogProb(HAM1) - (cDist.getLogProb(HAM) + xDists[featureID].getLogProb(1)));

			double mi = spam0 + spam1 + ham0 + ham1;
			assert !Double.isNaN(mi);

			mutualInfo.add(new IntDoublePair(featureID, mi));
		}

		Collections.sort(mutualInfo, new Comparator<IntDoublePair>(){
			@Override
			public int compare(IntDoublePair p1, IntDoublePair p2){
				// sort in descending order
				return Double.compare(p2.getY(), p1.getY());
		}});

		System.out.println("Mutual information computed and sorted.");
	}

	public void pruneAlphabet() {
		System.out.println("Pruning alphabet.");

		Alphabet currentAlphabet = instances.getAlphabet();
		System.out.println(" Current alphabet has " + currentAlphabet.size() + " features.");

		int oldIndices[] = new int[CUTOFF];
		ArrayList<String> newFeatures = new ArrayList<String>();

		for ( int i = 0; i < CUTOFF; i++ ) {
			oldIndices[i] = mutualInfo.get(i).getX();
			newFeatures.add(currentAlphabet.lookupObject(oldIndices[i]).toString());
			// System.out.println(" Mutual info rank " + i + ": " + oldIndices[i] + " " + mutualInfo.get(i).getY());
		}

		newAlphabet = new Alphabet(newFeatures.toArray());
		newAlphabet.stopGrowth();
		// this prevents changes to newAlphabet in subsequent calls to constructor for FeatureVector

		System.out.println(" New alphabet has " + newAlphabet.size() + " features.");
		System.out.println("Alphabet pruned.");
	}

	public FeatureVector changeAlphabet(FeatureVector featureVector) {
		ArrayList<Integer> featureIndices = new ArrayList<Integer>();
		ArrayList<Double> featureValues = new ArrayList<Double>();

		// System.out.println("Old feature vector (" + featureVector.numLocations() + "/" + featureVector.getAlphabet().size() + "):");
		// System.out.println(" " + featureVectorToPrettyString(featureVector));

		for ( int i = 0; i < featureVector.numLocations(); i++ ) {
			int featureValue = (int) featureVector.valueAtLocation(i);

			int oldIndex = (int) featureVector.indexAtLocation(i);
			// TODO: danger of error code -1
			String value = (String) featureVector.getAlphabet().lookupObject(oldIndex);
			int newIndex = newAlphabet.lookupIndex(value);

			if ( newIndex != -1 ) {
				featureIndices.add(newIndex);
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

		FeatureVector newFeatureVector = new FeatureVector(newAlphabet, featureIndicesPrim, featureValuesPrim);

		// System.out.println("New feature vector (" + newFeatureVector.numLocations() + "/" + newFeatureVector.getAlphabet().size() + "):");
		// System.out.println(" " + featureVectorToPrettyString(newFeatureVector));

		return newFeatureVector;
	}

	public void changeAllAlphabets() {
		System.out.println("Changing all alphabets.");

		InstanceList newInstances = new InstanceList(newAlphabet, instances.getTargetAlphabet());

		Iterator<Instance> iter = instances.iterator();

		while ( iter.hasNext() ) {
			Instance instance = iter.next();
			FeatureVector newFeatureVector = changeAlphabet((FeatureVector) instance.getData());
			instance.unLock();
			instance.setData(newFeatureVector);
			instance.lock();
			newInstances.add(instance);
		}

		instances = newInstances;

		System.out.println("All alphabets changed.");
	}

	private String featureVectorToPrettyString(FeatureVector featureVector) {
		ArrayList<IntStringPair> indexValues = new ArrayList<IntStringPair>();

		for ( int i = 0; i < featureVector.numLocations(); i++ ) {
			int index = (int) featureVector.indexAtLocation(i);
			String value = (String) featureVector.getAlphabet().lookupObject(index);
			indexValues.add(new IntStringPair(index, value));
		}

		Collections.sort(indexValues, new Comparator<IntStringPair>(){
			@Override
			public int compare(IntStringPair p1, IntStringPair p2){
				return p1.getY().compareTo(p2.getY());
		}});

		String output = "";

		for ( IntStringPair p : indexValues ) {
			output += p.getY() + "(" + p.getX() + ") ";
		}

		return output;
	}

	private short labelToShort(String label) {
		if ( label.equals("spam") ) {
			return SPAM;
		} else if ( label.equals("ham") ) {
			return HAM;
		} else {
			return -1;
		}
	}

	private short labelValueToShort(String label, int value) {
		if ( label.equals("spam") ) {
			if ( value ==  0 ) {
				return SPAM0;
			} else if ( value == 1 ) {
				return SPAM1;
			} else {
				return -1;
			}
		} else if ( label.equals("ham")) {
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

	private static class TxtFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return file.toString().endsWith(".txt");
		}
	}
}